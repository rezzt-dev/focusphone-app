package app.focusphone.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import app.focusphone.data.preferences.AppSchedulePreferences
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.domain.model.AppScheduleRule
import app.focusphone.ui.detox.DetoxBlockedActivity
import app.focusphone.ui.schedule.ScheduleBlockedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Accessibility service that intercepts app launches at the system level.
 *
 * When an app that is blocked by the active Work or Detox profile comes to the
 * foreground, the service immediately launches [DetoxBlockedActivity] on top of
 * it, preventing interaction with the blocked application.
 *
 * This closes the bypass vector where a user kills FocusPhone and launches
 * apps from notifications, recents, or voice assistants.
 *
 * The service is also the only component that can measure how long an app stays in
 * the foreground, so it owns the accounting behind the per-app daily usage limits of
 * [AppSchedulePreferences]: it accumulates foreground seconds for apps that have a
 * limit, and re-checks the active rule periodically so a session is interrupted the
 * moment the allowance runs out or the allowed time window closes.
 *
 * To avoid ANRs caused by reading AES-256/GCM encrypted SharedPreferences on every
 * accessibility event, the blocking state is cached in memory and refreshed every
 * [CACHE_TTL_MS].
 */
class FocusAccessibilityService : AccessibilityService() {

    /**
     * In-memory cache of the blocking state to avoid reading encrypted SharedPreferences
     * (AES-256/GCM via Android Keystore) on every accessibility event.
     */
    private var cachedBlockedApps: Set<String> = emptySet()
    private var cachedWorkKeywords: Set<String> = emptySet()
    private var cachedScheduleRules: Map<String, AppScheduleRule> = emptyMap()
    private var cachedDetoxEndTime = 0L
    private var cachedProfile = ""
    private var lastCacheUpdate = 0L
    private var cacheReady = false
    private var cacheRefreshJob: Job? = null
    private var packageWaitingForCache: String? = null

    /** Usage is authoritative in memory while the service lives. */
    private val cachedUsageSeconds = mutableMapOf<String, Int>()
    private val pendingUsageSeconds = mutableMapOf<String, Int>()
    private var cachedUsageDay = 0

    /** Foreground tracking used to accumulate per-app usage against the daily limits. */
    private var foregroundPackage: String? = null
    private var foregroundSince = 0L

    private val handler = Handler(Looper.getMainLooper())
    private val usageTick = Runnable { onUsageTick() }
    private val diskFlushTick = object : Runnable {
        override fun run() {
            flushPendingUsageToDisk()
            handler.postDelayed(this, DISK_FLUSH_INTERVAL_MS)
        }
    }
    private var screenReceiverRegistered = false
    private var released = false

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
    private val usageWriteMutex = Mutex()

    /**
     * Pauses usage accounting while the screen is off: an app left in the foreground
     * with a locked device is not being used and must not consume its daily allowance.
     */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    flushForegroundUsage()
                    flushPendingUsageToDisk()
                    cancelUsageTick()
                }

                Intent.ACTION_SCREEN_ON -> {
                    foregroundSince = SystemClock.elapsedRealtime()
                    foregroundPackage?.let { scheduleUsageTickFor(it) }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        refreshCache(force = true)
        handler.removeCallbacks(diskFlushTick)
        handler.postDelayed(diskFlushTick, DISK_FLUSH_INTERVAL_MS)

        // onServiceConnected can fire again after a reconnect; register only once.
        if (!screenReceiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                screenReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            screenReceiverRegistered = true
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (!cacheReady) {
            packageWaitingForCache = packageName
            refreshCache(force = true)
            return
        }
        refreshCacheIfNeeded()

        handleForegroundPackage(packageName)
    }

    /** Handles an event after the encrypted state has been loaded off the main thread. */
    private fun handleForegroundPackage(packageName: String) {
        ensureCurrentUsageDay()

        // A different app came to the front: settle the previous app's usage first.
        if (packageName != foregroundPackage) {
            flushForegroundUsage()
            foregroundPackage = packageName
            foregroundSince = SystemClock.elapsedRealtime()
        }

        // Never block FocusPhone itself.
        if (packageName == this.packageName) {
            cancelUsageTick()
            return
        }

        // Normal profile never blocks by profile, but schedules still apply.
        if (cachedProfile != ProfilePreferences.PROFILE_NORMAL) {
            // Detox blocks everything except FocusPhone. Whether it is active depends on
            // the clock, so it is evaluated on every event from the cached inputs: a cached
            // boolean kept blocking for up to CACHE_TTL_MS after the session had ended.
            val now = System.currentTimeMillis()
            if (ProfilePreferences.isDetoxActivePure(cachedProfile, cachedDetoxEndTime, now)) {
                launchBlockScreen()
                return
            }
            if (cachedProfile == ProfilePreferences.PROFILE_DETOX) {
                // The session has just run out: end it for the whole app, not only here.
                cachedProfile = ProfilePreferences.PROFILE_NORMAL
                serviceScope.launch {
                    ProfilePreferences.expireDetoxIfNeeded(this@FocusAccessibilityService)
                }
            }

            // Work profile blocks manually blocked apps and apps matching keywords.
            if (cachedProfile == ProfilePreferences.PROFILE_WORK) {
                val pkgLower = packageName.lowercase()
                val blockedByKeyword = cachedWorkKeywords.any { pkgLower.contains(it) }
                if (cachedBlockedApps.contains(packageName) || blockedByKeyword) {
                    launchBlockScreen()
                    return
                }
            }
        }

        enforceSchedule(packageName)
    }

    // ── Schedule enforcement ───────────────────────────────────────

    /**
     * Blocks [packageName] when its schedule rule forbids the current moment, and
     * otherwise arms the periodic re-check that enforces the rule mid-session.
     */
    private fun enforceSchedule(packageName: String) {
        val rule = cachedScheduleRules[packageName]
        if (rule == null) {
            cancelUsageTick()
            return
        }

        // Apps that emit window events constantly would otherwise keep resetting the
        // periodic tick and never have their usage written, so settle it here too.
        flushForegroundUsage()

        val reason = AppSchedulePreferences.evaluateRule(
            rule,
            cachedUsageSeconds[packageName] ?: 0
        )
        if (reason != null) {
            cancelUsageTick()
            startActivity(ScheduleBlockedActivity.newIntent(this, packageName, null, reason))
            return
        }

        scheduleUsageTickFor(packageName)
    }

    /**
     * Periodic check while a scheduled app is in the foreground. It persists the time
     * used so far and blocks the app as soon as the allowance or the window ends.
     */
    private fun onUsageTick() {
        val packageName = foregroundPackage ?: return
        if (packageName == this.packageName) return

        flushForegroundUsage()

        val rule = cachedScheduleRules[packageName] ?: return
        val reason = AppSchedulePreferences.evaluateRule(
            rule,
            cachedUsageSeconds[packageName] ?: 0
        )
        if (reason != null) {
            startActivity(ScheduleBlockedActivity.newIntent(this, packageName, null, reason))
            return
        }

        scheduleUsageTickFor(packageName)
    }

    /** Arms the next usage check, waking up exactly when the allowance would run out. */
    private fun scheduleUsageTickFor(packageName: String) {
        cancelUsageTick()
        val rule = cachedScheduleRules[packageName] ?: return
        if (!rule.isActive) return

        val remainingMs = AppSchedulePreferences.getRemainingLimitSeconds(
            rule,
            cachedUsageSeconds[packageName] ?: 0
        )
            ?.let { it * 1000L + 1_000L }
            ?: TICK_INTERVAL_MS
        handler.postDelayed(usageTick, remainingMs.coerceIn(1_000L, TICK_INTERVAL_MS))
    }

    private fun cancelUsageTick() {
        handler.removeCallbacks(usageTick)
    }

    /**
     * Credits the foreground time elapsed since the last flush to the current app and
     * restarts the stopwatch. Apps without a daily limit are not tracked at all, so no
     * usage data is written for them.
     */
    private fun flushForegroundUsage() {
        val packageName = foregroundPackage ?: return
        val now = SystemClock.elapsedRealtime()
        val elapsedSeconds = ((now - foregroundSince) / 1000L).toInt()
        foregroundSince = now

        if (elapsedSeconds <= 0) return
        if (packageName == this.packageName) return
        val rule = cachedScheduleRules[packageName] ?: return
        if (!rule.hasDailyLimit) return

        cachedUsageSeconds[packageName] =
            (cachedUsageSeconds[packageName] ?: 0) + elapsedSeconds
        pendingUsageSeconds[packageName] =
            (pendingUsageSeconds[packageName] ?: 0) + elapsedSeconds
    }

    /**
     * Moves the current in-memory delta to one serialized IO transaction. If encrypted
     * storage is temporarily unavailable, the batch is merged back on the main thread.
     */
    private fun flushPendingUsageToDisk(onComplete: (() -> Unit)? = null) {
        ensureCurrentUsageDay()
        val batch = pendingUsageSeconds.toMap()
        pendingUsageSeconds.clear()
        if (batch.isEmpty()) {
            onComplete?.invoke()
            return
        }

        serviceScope.launch {
            val written = usageWriteMutex.withLock {
                AppSchedulePreferences.addUsageSeconds(this@FocusAccessibilityService, batch)
            }
            withContext(Dispatchers.Main.immediate) {
                if (!written && !released) {
                    batch.forEach { (packageName, seconds) ->
                        pendingUsageSeconds[packageName] =
                            (pendingUsageSeconds[packageName] ?: 0) + seconds
                    }
                }
                onComplete?.invoke()
            }
        }
    }

    /** Drops yesterday's in-memory values at local midnight before any evaluation. */
    private fun ensureCurrentUsageDay() {
        val today = AppSchedulePreferences.currentUsageDayStamp()
        if (cachedUsageDay == today) return
        cachedUsageDay = today
        cachedUsageSeconds.clear()
        pendingUsageSeconds.clear()
    }

    // ── Block screens ──────────────────────────────────────────────

    private fun launchBlockScreen() {
        val intent = Intent(this, DetoxBlockedActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
        }
        startActivity(intent)
    }

    // ── Cache ──────────────────────────────────────────────────────

    /** Refreshes the in-memory cache if the TTL has expired. */
    private fun refreshCacheIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastCacheUpdate < CACHE_TTL_MS) return
        refreshCache(force = false)
    }

    /** Reads encrypted preferences on IO and publishes one coherent snapshot on main. */
    private fun refreshCache(force: Boolean) {
        if (cacheRefreshJob?.isActive == true) return
        if (!force && System.currentTimeMillis() - lastCacheUpdate < CACHE_TTL_MS) return
        val loadUsage = !cacheReady
        cacheRefreshJob = serviceScope.launch {
            val detoxEndTime = ProfilePreferences.getDetoxEndTime(this@FocusAccessibilityService)
            val profile = ProfilePreferences.getProfile(this@FocusAccessibilityService)
            val blockedApps = ProfilePreferences.getBlockedApps(this@FocusAccessibilityService)
            val workKeywords = ProfilePreferences.getWorkKeywords(this@FocusAccessibilityService)
            val rules = AppSchedulePreferences.getRules(this@FocusAccessibilityService)
                .associateBy { it.packageName }
            val usage = if (loadUsage) {
                AppSchedulePreferences.getUsageSecondsSnapshot(this@FocusAccessibilityService)
            } else null

            withContext(Dispatchers.Main.immediate) {
                if (released) return@withContext
                cachedDetoxEndTime = detoxEndTime
                cachedProfile = profile
                cachedBlockedApps = blockedApps
                cachedWorkKeywords = workKeywords
                cachedScheduleRules = rules
                if (!cacheReady) {
                    cachedUsageDay = AppSchedulePreferences.currentUsageDayStamp()
                    cachedUsageSeconds.clear()
                    cachedUsageSeconds.putAll(usage.orEmpty())
                }
                cacheReady = true
                lastCacheUpdate = System.currentTimeMillis()
                packageWaitingForCache?.also {
                    packageWaitingForCache = null
                    handleForegroundPackage(it)
                }
            }
        }
    }

    override fun onInterrupt() {
        // Nothing to clean up.
    }

    override fun onUnbind(intent: Intent?): Boolean {
        release()
        return super.onUnbind(intent)
    }

    /** The service can be destroyed without onUnbind; the receiver must not outlive it. */
    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        if (released) return
        released = true
        flushForegroundUsage()
        cancelUsageTick()
        handler.removeCallbacks(diskFlushTick)
        if (screenReceiverRegistered) {
            unregisterReceiver(screenReceiver)
            screenReceiverRegistered = false
        }
        // Let the final encrypted write finish before cancelling the service scope.
        flushPendingUsageToDisk { serviceScope.cancel() }
    }

    companion object {

        private const val CACHE_TTL_MS = 3000L

        /** How often a scheduled app in the foreground is re-checked and its usage saved. */
        private const val TICK_INTERVAL_MS = 30_000L

        /** Usage writes are batched so accessibility callbacks never touch disk. */
        private const val DISK_FLUSH_INTERVAL_MS = 60_000L

        /**
         * Returns whether this accessibility service is currently enabled by the user.
         */
        fun isEnabled(context: Context): Boolean {
            val expectedService = ComponentName(context, FocusAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            // OEMs may persist either the long form
            // `app.focusphone/app.focusphone.service.FocusAccessibilityService` or the
            // short form `app.focusphone/.service.FocusAccessibilityService`.
            return enabledServices
                .split(':')
                .mapNotNull(ComponentName::unflattenFromString)
                .any { it == expectedService }
        }

        /**
         * Opens the system Accessibility settings so the user can enable the service.
         */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
