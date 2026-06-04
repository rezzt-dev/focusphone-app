package app.focusphone.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.ui.detox.DetoxBlockedActivity

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
    private var cachedIsDetoxActive = false
    private var cachedProfile = ""
    private var lastCacheUpdate = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        refreshCache()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        val ownPackage = packageName == this.packageName

        // Never block FocusPhone itself.
        if (ownPackage) return

        refreshCacheIfNeeded()

        // Normal profile never blocks.
        if (cachedProfile == ProfilePreferences.PROFILE_NORMAL) return

        // Detox blocks everything except FocusPhone.
        if (cachedIsDetoxActive) {
            launchBlockScreen()
            return
        }

        // Work profile blocks manually blocked apps and apps matching keywords.
        if (cachedProfile == ProfilePreferences.PROFILE_WORK) {
            val pkgLower = packageName.lowercase()
            val blockedByKeyword = cachedWorkKeywords.any { pkgLower.contains(it) }
            if (cachedBlockedApps.contains(packageName) || blockedByKeyword) {
                launchBlockScreen()
            }
        }
    }

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

    /** Refreshes the in-memory cache if the TTL has expired. */
    private fun refreshCacheIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastCacheUpdate < CACHE_TTL_MS) return
        refreshCache()
    }

    /** Force-refreshes the cache by reading encrypted preferences once. */
    private fun refreshCache() {
        lastCacheUpdate = System.currentTimeMillis()
        cachedIsDetoxActive = ProfilePreferences.isDetoxActive(this)
        cachedProfile = ProfilePreferences.getProfile(this)
        cachedBlockedApps = ProfilePreferences.getBlockedApps(this)
        cachedWorkKeywords = ProfilePreferences.getWorkKeywords(this)
    }

    override fun onInterrupt() {
        // Nothing to clean up.
    }

    companion object {

        private const val CACHE_TTL_MS = 3000L

        /**
         * Returns whether this accessibility service is currently enabled by the user.
         */
        fun isEnabled(context: Context): Boolean {
            val expectedService = "${context.packageName}/${FocusAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabledServices)
            while (splitter.hasNext()) {
                if (splitter.next().equals(expectedService, ignoreCase = true)) {
                    return true
                }
            }
            return false
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
