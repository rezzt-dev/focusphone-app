package app.focusphone.ui.home

import androidx.core.content.edit
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.BuildConfig
import app.focusphone.FocusPhoneApp
import app.focusphone.R
import app.focusphone.data.model.FavouriteApp
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.data.preferences.ConsciousPausePreferences
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.data.preferences.GesturePreferences
import app.focusphone.data.preferences.NotificationPanelPreferences
import app.focusphone.data.preferences.HomeLayoutPreferences
import app.focusphone.data.preferences.WidgetPreferences
import app.focusphone.data.preferences.TutorialPreferences
import app.focusphone.data.preferences.WallpaperPreferences
import app.focusphone.data.repository.NotificationRepository
import app.focusphone.data.repository.PackageChangeMonitor
import app.focusphone.ui.home.adapter.NotificationsAdapter
import app.focusphone.ui.pause.ConsciousPauseActivity
import app.focusphone.ui.detox.DetoxBlockedActivity
import app.focusphone.databinding.ActivityHomeBinding
import app.focusphone.domain.model.ClockType
import app.focusphone.service.FocusAccessibilityService
import app.focusphone.service.FocusNotificationService
import app.focusphone.ui.allapps.AllAppsActivity
import app.focusphone.ui.appselect.AppSelectActivity
import app.focusphone.ui.base.BaseActivity
import app.focusphone.ui.home.adapter.FavouriteAppsAdapter
import app.focusphone.ui.onboarding.OnboardingActivity
import app.focusphone.ui.schedule.ScheduleDayLabels
import app.focusphone.ui.settings.SettingsActivity
import app.focusphone.ui.theme.ScrimSurface
import app.focusphone.ui.theme.SurfaceScrim
import app.focusphone.ui.tutorial.TutorialContent
import app.focusphone.ui.util.currentWindowWidthPx
import app.focusphone.ui.tutorial.TutorialOverlayView
import app.focusphone.ui.tutorial.TutorialPage
import app.focusphone.ui.tutorial.TutorialTopic
import app.focusphone.ui.wallpaper.WallpaperStorage
import app.focusphone.ui.wallpaper.WallpaperUtils
import app.focusphone.ui.widget.AppWidgetHostManager
import app.focusphone.ui.widget.ClockRenderer
import app.focusphone.ui.widget.WidgetSurface
import app.focusphone.util.LauncherUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import app.focusphone.ui.theme.FocusColors
import androidx.core.view.isVisible
import androidx.core.view.isNotEmpty
import androidx.core.graphics.drawable.toDrawable

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var gestureDetector: GestureDetector

    private lateinit var clockRenderer: ClockRenderer
    private lateinit var widgetSurface: WidgetSurface

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.factory(focusPhoneApp)
    }

    private val adapter = FavouriteAppsAdapter(
        onAppClick     = ::launchApp,
        onAppLongClick = ::showAppContextMenu
    )

    private val notificationsAdapter = NotificationsAdapter(
        onClick = ::onNotificationClick,
        onDismiss = ::dismissNotification
    )

    private var lastVerifyTime = 0L
    private var appSpacingDecoration: RecyclerView.ItemDecoration? = null
    private var pendingVerify = false
    private var notificationWallpaperGeneration = 0
    private var hasNotificationWallpaper = false
    private var clockReceiverRegistered = false
    private var homeInitialized = false

    /** [PackageChangeMonitor.version] at the last favourites check; -1 before the first. */
    private var verifiedPackageVersion = -1L

    /** Day of the month the calendar grid was last built for, or 0 when never built. */
    private var renderedCalendarDay = 0


    /**
     * A launcher stays resident for days, so the minute tick doubles as the midnight
     * signal: without this, the calendar kept highlighting yesterday until the user left
     * the home screen and came back.
     */
    private val clockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateClock()
            if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) != renderedCalendarDay) {
                populateCalendar()
            }
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        clockRenderer = ClockRenderer(this, binding.clockContainer)
        widgetSurface = WidgetSurface(
            context = this,
            emptyBehavior = WidgetSurface.EmptyBehavior.HIDE_CONTAINER,
            onVisibilityChanged = ::applyClockType
        )

        if (!ThemePreferences.isOnboardingComplete(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            return
        }

        initializeHome()
    }

    /** Completes the expensive launcher setup only when onboarding no longer covers it. */
    private fun initializeHome() {
        if (homeInitialized) return
        setupInsets()
        setupClockTap()
        setupRecyclerView()
        setupDrawerMenu()
        setupGestureDetector()
        setupDrawerPushEffect()
        setupGestureExclusion()
        showLongPressHintIfNeeded()
        observeFavourites()
        setupNotificationPanel()
        setupBackPress()

        binding.indicatorContainer.visibility = View.INVISIBLE
        binding.indicatorContainer.alpha = 0f
        binding.indicatorHome.contentDescription = getString(R.string.cd_home_indicator)
        binding.indicatorMenu.contentDescription = getString(R.string.cd_menu_indicator)
        homeInitialized = true
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!LauncherUtils.isDefaultLauncher(this@HomeActivity)) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (!homeInitialized) {
            if (!ThemePreferences.isOnboardingComplete(this)) return
            // Onboarding has just finished on top of this instance. Recreate instead of
            // initialising here: the observers use repeatOnLifecycle, which must be started
            // from onCreate, and this happens once in the app's lifetime.
            recreate()
            return
        }
        applyNotificationWallpaper()
        try {
            ContextCompat.registerReceiver(
                this,
                clockReceiver,
                IntentFilter(Intent.ACTION_TIME_TICK),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            clockReceiverRegistered = true
        } catch (e: RuntimeException) {
            // Some OEMs restrict ACTION_TIME_TICK.
            Log.w("HomeActivity", "Cannot register ACTION_TIME_TICK receiver", e)
        }
        applyClockType()
        applyClockScale()
        updateClock()
        // A package change since the last check (e.g. an uninstall from the system settings)
        // forces a fresh check instead of waiting for the hourly one.
        val packagesChanged = PackageChangeMonitor.version.value != verifiedPackageVersion
        if (pendingVerify || packagesChanged) {
            pendingVerify = false
            verifyInstalledApps(force = true)
        } else {
            verifyInstalledApps()
        }
        ProfilePreferences.expireDetoxIfNeeded(this)
        updateProfileLabel()
        restoreWidget()
        applyHomeLayout()
        refreshNotificationPanelState()
        adapter.updateTypeface(FontPreferences.getTypeface(this))
        applyAlignment()
        populateCalendar()
        updateGestureExclusionRects()
        applyAppSpacing()

        // Start contextual tutorial if onboarding is done but tutorial not yet completed
        binding.root.post { maybeStartTutorial() }
    }

    private fun verifyInstalledApps(force: Boolean = false) {
        val currentApps = adapter.currentList // snapshot
        if (currentApps.isEmpty()) return

        val now = System.currentTimeMillis()
        if (!force && now - lastVerifyTime < 60 * 60 * 1000L) return
        lastVerifyTime = now
        verifiedPackageVersion = PackageChangeMonitor.version.value

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val pm = packageManager
            currentApps.forEach { app ->
                val exists = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageInfo(app.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0L))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(app.packageName, 0)
                    }
                    true
                } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                    false
                }
                if (!exists) {
                    viewModel.removeFavourite(app.packageName)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val hostManager = AppWidgetHostManager.getInstance(this)
        hostManager.startListening(this)
        // Frees IDs leaked by builds that never released removed or replaced widgets.
        hostManager.releaseOrphanedIds(this)
        restoreWidget()
    }

    override fun onStop() {
        widgetSurface.releaseView()
        AppWidgetHostManager.getInstance(this).stopListening(this)
        super.onStop()
    }

    override fun onPause() {
        super.onPause()
        if (clockReceiverRegistered) {
            try {
                unregisterReceiver(clockReceiver)
            } catch (e: IllegalArgumentException) {
                Log.w("HomeActivity", "Clock receiver was not registered", e)
            } finally {
                clockReceiverRegistered = false
            }
        }
        hintToastJob?.cancel()
        hideRunnable?.let { binding.indicatorContainer.removeCallbacks(it) }
        binding.indicatorContainer.animate().cancel()
    }

    override fun onDestroy() {
        widgetSurface.releaseView()
        super.onDestroy()
    }

    // ── Legibility scrims ──────────────────────────────────────────

    /**
     * The home screen owns three independent surfaces over a single wallpaper, so it places
     * its scrims itself instead of using the base class's full-screen one.
     *
     * The home surface is kept deliberately light — the wallpaper is the point of this
     * screen — while both drawers get the denser panel treatment, because a 12sp weekday
     * letter or a notification subtitle has no chance against an unfiltered photo.
     */
    /** The calendar and the launcher options live in the drawers, behind a panel scrim. */
    override fun contentSurface(): ScrimSurface = ScrimSurface.PANEL

    override fun applyScrim() {
        val hasWallpaper = hasSurfaceWallpaper()
        val colors = palette
        SurfaceScrim.apply(binding.viewScrimHome, this, ScrimSurface.HOME, colors, hasWallpaper)
        SurfaceScrim.apply(binding.viewScrimMenu, this, ScrimSurface.PANEL, colors, hasWallpaper)
        SurfaceScrim.apply(
            binding.viewScrimNotifications,
            this,
            ScrimSurface.PANEL,
            colors,
            hasNotificationWallpaper
        )
    }

    /** Loads the wallpaper selected specifically for the notification drawer. */
    private fun applyNotificationWallpaper() {
        val uri = WallpaperPreferences.getNotificationsWallpaperUri(this)
            ?.takeIf { WallpaperPreferences.isWallpaperEnabled(this) }
        val generation = ++notificationWallpaperGeneration
        hasNotificationWallpaper = uri != null
        binding.drawerNotifications.setBackgroundColor(
            if (uri == null) palette.background else Color.TRANSPARENT
        )

        if (uri == null) {
            lifecycleScope.launch {
                WallpaperUtils.applyWallpaperToImageView(
                    binding.ivWallpaperNotifications,
                    bitmap = null,
                    blurRadius = 0f,
                    blurEnabled = false
                )
                applyScrim()
            }
            return
        }

        val blurEnabled = WallpaperPreferences.isBlurEnabled(this)
        val blurRadius = WallpaperUtils.getBlurRadius(WallpaperPreferences.getBlurIntensity(this))
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                WallpaperStorage.load(this@HomeActivity, uri, blurEnabled, blurRadius)
            }
            if (generation != notificationWallpaperGeneration) return@launch

            val ready = result as? WallpaperStorage.LoadResult.Ready
            val bitmap = ready?.bitmap
            if (result == WallpaperStorage.LoadResult.Lost) {
                Toast.makeText(
                    this@HomeActivity,
                    R.string.wallpaper_access_lost,
                    Toast.LENGTH_LONG
                ).show()
            }
            hasNotificationWallpaper = bitmap != null
            binding.drawerNotifications.setBackgroundColor(
                if (bitmap == null) palette.background else Color.TRANSPARENT
            )
            WallpaperUtils.applyWallpaperToImageView(
                binding.ivWallpaperNotifications,
                bitmap,
                blurRadius,
                blurEnabled && ready?.softwareBlurApplied != true
            )
            applyScrim()
        }
    }

    // ── Insets ─────────────────────────────────────────────────────

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply system-bar padding to the main content container so every component
            // (clock, widget, apps) is safe regardless of user-defined order.
            binding.rootHome.setPadding(0, bars.top, 0, bars.bottom)

            // Drawers need their own insets so they don't overlap status/nav bars, but the
            // padding belongs on the *content* rather than on the drawer frame: the frame
            // also hosts the legibility scrim, and a padded frame left the scrim floating
            // as a visible rectangle inside the drawer instead of covering it edge to edge.
            val drawerInsetH = dp(36)
            val drawerInsetV = dp(40)
            binding.menuContent.setPadding(
                drawerInsetH, bars.top + drawerInsetV, drawerInsetH, bars.bottom + drawerInsetV
            )
            binding.notificationContent.setPadding(
                drawerInsetH, bars.top + drawerInsetV, drawerInsetH, bars.bottom + drawerInsetV
            )

            // Indicator container lives outside the DrawerLayout, so apply a dynamic bottom margin.
            val indicatorParams = binding.indicatorContainer.layoutParams as android.widget.FrameLayout.LayoutParams
            indicatorParams.bottomMargin = bars.bottom + dp(16)
            binding.indicatorContainer.layoutParams = indicatorParams

            insets
        }
    }

    // ── Gestures ───────────────────────────────────────────────────

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 120
            private val SWIPE_VELOCITY_THRESHOLD = 150

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e1.x - e2.x
                val diffY = e1.y - e2.y

                // If any drawer is already open, do not allow fling to open the opposite one.
                // The user must close the current drawer (tap outside or back) before switching.
                val isAnyDrawerOpen = binding.drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START) ||
                        binding.drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.END)
                if (isAnyDrawerOpen) return false

                return when {
                    // Horizontal swipe
                    kotlin.math.abs(diffX) > kotlin.math.abs(diffY) -> {
                        if (kotlin.math.abs(diffX) > SWIPE_THRESHOLD &&
                            kotlin.math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                        ) {
                            when {
                                // Swipe left (finger moving left) — open right menu drawer
                                diffX > 0 -> {
                                    binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.END)
                                    true
                                }
                                // Swipe right (finger moving right) — open left notification drawer
                                diffX < 0 && NotificationPanelPreferences.isEnabled(this@HomeActivity) -> {
                                    binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    // Vertical swipe (down) — expand system notifications
                    diffY < -SWIPE_THRESHOLD && kotlin.math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD -> {
                        expandNotificationsPanel()
                        true
                    }
                    else -> false
                }
            }
        })
        // Only the empty/home surface owns launcher gestures. RecyclerViews, drawers and
        // dialogs keep their flings without leaking them into this detector.
        binding.rootHome.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) binding.rootHome.performClick()
            false
        }
    }

    // "statusbar" is the service name behind Context.STATUS_BAR_SERVICE, which only became
    // public in API 33 and is absent from getSystemService's @StringDef.
    @SuppressLint("WrongConstant")
    private fun expandNotificationsPanel() {
        if (!GesturePreferences.isSwipeDownEnabled(this)) return
        // Preferred, officially supported route: GLOBAL_ACTION_NOTIFICATIONS via our own
        // accessibility service. Falls back to reflection only when that service is not
        // enabled — reflection into StatusBarManager is blocked by the non-SDK interface
        // list on API 31+ and only works, if at all, on older devices (FP-038).
        if (FocusAccessibilityService.expandNotifications()) return
        try {
            val statusBarService = getSystemService(STATUS_BAR_SERVICE_NAME)
            val method = statusBarService?.javaClass?.getMethod("expandNotificationsPanel")
            method?.invoke(statusBarService)
        } catch (e: Exception) {
            Log.w("HomeActivity", "Failed to expand notification panel", e)
        }
    }

    // ── Clock tap → Alarm app ──────────────────────────────────────

    private fun setupClockTap() {
        val action = View.OnClickListener {
            haptic(it)
            try { startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS)) }
            catch (e: Exception) {
                Log.w("HomeActivity", "Cannot open alarm app via ACTION_SHOW_ALARMS", e)
                try { startActivity(Intent(AlarmClock.ACTION_SET_ALARM)) }
                catch (e2: Exception) {
                    Log.w("HomeActivity", "Cannot open alarm app via ACTION_SET_ALARM", e2)
                }
            }
        }
        listOf(
            binding.tvTime, binding.tvDate,
            binding.tvStackedH, binding.tvStackedM, binding.tvStackedDate,
            binding.tvWordsTime, binding.tvWordsDate,
            binding.viewAnalog, binding.tvAnalogDate
        ).forEach { it.setOnClickListener(action) }
        binding.clockContainer.setOnClickListener(action)
    }

    // ── Clock type (Digital / Stacked / Words / Analog) ────────────

    private fun applyClockType() {
        val type = FontPreferences.getClockType(this)
        clockRenderer.applyType(type)

        // Adjust gravity for the RecyclerView and Empty state
        val rvParams = binding.rvFavouriteApps.layoutParams as android.widget.FrameLayout.LayoutParams
        rvParams.gravity = android.view.Gravity.CENTER_VERTICAL
        binding.rvFavouriteApps.layoutParams = rvParams
    }

    /** Aligns the favourite list and its empty-state message with the user's setting. */
    private fun applyAlignment() {
        val gravity = when (ThemePreferences.getAlignment(this)) {
            ThemePreferences.ALIGN_LEFT -> Gravity.START or Gravity.CENTER_VERTICAL
            ThemePreferences.ALIGN_RIGHT -> Gravity.END or Gravity.CENTER_VERTICAL
            else -> Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
        }
        adapter.updateAlignment(gravity)
        val emptyParams = binding.tvEmpty.layoutParams as android.widget.FrameLayout.LayoutParams
        emptyParams.gravity = gravity
        binding.tvEmpty.layoutParams = emptyParams
    }

    // ── Home layout (order, visibility, sizes) ─────────────────────

    private fun applyHomeLayout() {
        val order = HomeLayoutPreferences.getOrder(this)
        val clockEnabled = HomeLayoutPreferences.isClockEnabled(this)
        val widgetEnabled = HomeLayoutPreferences.isWidgetEnabled(this)

        // Determine final visibility for each container
        val clockType = FontPreferences.getClockType(this)
        val clockVisible = clockEnabled && clockType != ClockType.HIDDEN
        val widgetVisible = widgetEnabled && WidgetPreferences.isWidgetEnabled(this)

        // Remove children from root_home and re-add in desired order.
        val root = binding.rootHome
        val clockView = binding.clockContainer
        val widgetView = binding.widgetContainer
        val appsView = binding.appsContainer

        root.removeAllViews()

        for (component in order) {
            when (component) {
                HomeLayoutPreferences.Component.CLOCK -> {
                    clockView.visibility = if (clockVisible) View.VISIBLE else View.GONE
                    root.addView(clockView)
                }
                HomeLayoutPreferences.Component.WIDGET -> {
                    widgetView.visibility = if (widgetVisible) View.VISIBLE else View.GONE
                    root.addView(widgetView)
                }
                HomeLayoutPreferences.Component.APPS -> {
                    appsView.visibility = View.VISIBLE
                    root.addView(appsView)
                }
            }
        }

        adjustRootGravity()
    }

    /**
     * Controls whether the home content behaves as a single centred block
     * (grouped mode) or as independent layers (separated mode).
     *
     * Separated mode is triggered when:
     *   - the user enabled "fixed clock at top" in settings, or
     *   - the screen is empty (no apps and no widget).
     *
     * In separated mode the clock stays at the top with a safe margin and the
     * app list occupies all remaining space, centring itself vertically inside
     * that area.
     */
    private fun adjustRootGravity() {
        val fixedTop = HomeLayoutPreferences.isClockFixedTop(this)
        val noApps = binding.tvEmpty.isVisible
        val noWidget = !binding.widgetContainer.isVisible
        val clockVisible = binding.clockContainer.isVisible

        val isSeparatedMode = fixedTop || (clockVisible && noApps && noWidget)

        val clockParams = binding.clockContainer.layoutParams as? LinearLayout.LayoutParams
            ?: return
        val widgetParams = binding.widgetContainer.layoutParams as? LinearLayout.LayoutParams
            ?: return
        val appsParams = binding.appsContainer.layoutParams as? LinearLayout.LayoutParams
            ?: return

        if (isSeparatedMode) {
            // Clock (and widget) at top; apps fill the rest of the screen.
            binding.rootHome.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            clockParams.topMargin = dp(24)
            clockParams.bottomMargin = dp(8)
            widgetParams.topMargin = dp(8)
            widgetParams.bottomMargin = dp(8)
            appsParams.height = 0
            appsParams.weight = 1f
            appsParams.topMargin = dp(8)
            appsParams.bottomMargin = dp(8)
        } else {
            // Grouped mode: everything wraps and centres as a single block.
            binding.rootHome.gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            clockParams.topMargin = dp(8)
            clockParams.bottomMargin = dp(8)
            widgetParams.topMargin = dp(8)
            widgetParams.bottomMargin = dp(8)
            appsParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            appsParams.weight = 0f
            appsParams.topMargin = dp(8)
            appsParams.bottomMargin = dp(8)
        }

        binding.clockContainer.layoutParams = clockParams
        binding.widgetContainer.layoutParams = widgetParams
        binding.appsContainer.layoutParams = appsParams
    }

    // ── Clock update (called every minute) ────────────────────────

    private fun applyClockScale() {
        clockRenderer.applyScale(FontPreferences.getClockSize(this))
    }

    private fun updateClock() {
        clockRenderer.render()
    }

    // ── RecyclerView ───────────────────────────────────────────────

    private fun setupRecyclerView() {
        binding.rvFavouriteApps.layoutManager = LinearLayoutManager(this)
        binding.rvFavouriteApps.adapter = adapter
        binding.rvFavouriteApps.itemAnimator = null

        val touchHelper = androidx.recyclerview.widget.ItemTouchHelper(
            object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                androidx.recyclerview.widget.ItemTouchHelper.UP or androidx.recyclerview.widget.ItemTouchHelper.DOWN,
                0 // No swipe
            ) {
                override fun onMove(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    target: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ): Boolean {
                    val from = viewHolder.bindingAdapterPosition
                    val to = target.bindingAdapterPosition
                    if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                    return adapter.moveItem(from, to)
                }

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG) {
                        adapter.beginDrag()
                    }
                }

                override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {}

                override fun clearView(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ) {
                    super.clearView(recyclerView, viewHolder)
                    // Drag ended, save new order
                    val finalOrder = adapter.commitReorder()
                    viewModel.onAppsReordered(finalOrder)
                }
                
                override fun isLongPressDragEnabled(): Boolean = true
            }
        )
        touchHelper.attachToRecyclerView(binding.rvFavouriteApps)
        applyAppSpacing()
    }

    private fun applyAppSpacing() {
        val spacingDp = HomeLayoutPreferences.getAppSpacingDp(this)
        val spacingPx = dp(spacingDp)
        val appCount = adapter.itemCount

        if (appCount <= 1 || spacingPx <= 0) {
            appSpacingDecoration?.let { binding.rvFavouriteApps.removeItemDecoration(it) }
            appSpacingDecoration = null
            return
        }

        // Calculate available vertical space for the apps section.
        val rootHeight = binding.rootHome.height - binding.rootHome.paddingTop - binding.rootHome.paddingBottom
        val clockHeight = if (binding.clockContainer.isVisible) binding.clockContainer.height else 0
        val widgetHeight = if (binding.widgetContainer.isVisible) binding.widgetContainer.height else 0
        val availableHeight = rootHeight - clockHeight - widgetHeight

        // Estimate each item's natural height (wrap_content).
        val estimatedItemHeight = if (binding.rvFavouriteApps.isNotEmpty()) {
            binding.rvFavouriteApps.getChildAt(0).height
        } else {
            dp(56)
        }

        // Total spacing budget = available space minus natural item heights.
        // Spacing is added *between* items (count - 1 gaps).
        val maxTotalSpacing = (availableHeight - estimatedItemHeight * appCount).coerceAtLeast(0)
        val maxSpacingPx = (maxTotalSpacing / (appCount - 1)).coerceAtLeast(0)
        val finalSpacingPx = spacingPx.coerceIn(0, maxSpacingPx)

        appSpacingDecoration?.let { binding.rvFavouriteApps.removeItemDecoration(it) }
        val decoration = VerticalSpacingItemDecoration(finalSpacingPx)
        appSpacingDecoration = decoration
        binding.rvFavouriteApps.addItemDecoration(decoration)
    }

    // ── Drawer Menu ────────────────────────────────────────────────

    private fun setupDrawerPushEffect() {
        // Force both drawers to take up the full window width so text centers perfectly.
        // The window's own layout pass (not the display size) is the source of truth, so
        // this stays correct across multi-window resizes and foldable posture changes
        // (FP-039).
        applyDrawerWidths()
        binding.drawerLayout.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left != oldRight - oldLeft) applyDrawerWidths()
        }

        binding.drawerLayout.setScrimColor(Color.TRANSPARENT)
        binding.drawerLayout.drawerElevation = 0f

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                val isLeftDrawer = (drawerView.layoutParams as androidx.drawerlayout.widget.DrawerLayout.LayoutParams).gravity == androidx.core.view.GravityCompat.START
                val moveFactor = if (isLeftDrawer) {
                    drawerView.width * slideOffset // push content to the right
                } else {
                    -(drawerView.width * slideOffset) // push content to the left
                }
                binding.rootHome.translationX = moveFactor
                updateIndicators(slideOffset, isLeftDrawer)
                if (slideOffset > 0f) {
                    showIndicatorsWithTimeout()
                }
            }

            override fun onDrawerOpened(drawerView: View) {
                val isLeftDrawer = (drawerView.layoutParams as androidx.drawerlayout.widget.DrawerLayout.LayoutParams).gravity == androidx.core.view.GravityCompat.START
                // Lock the opposite drawer so the user cannot slide directly to it
                if (isLeftDrawer) {
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, androidx.core.view.GravityCompat.END)
                } else {
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, androidx.core.view.GravityCompat.START)
                }
            }

            override fun onDrawerClosed(drawerView: View) {
                // Reset to home indicator when any drawer is fully closed
                setIndicatorBackground(binding.indicatorNotifications, filled = false)
                setIndicatorBackground(binding.indicatorHome, filled = true)
                setIndicatorBackground(binding.indicatorMenu, filled = false)

                // Restore original lock modes: left follows preference, right is always unlocked
                val panelEnabled = NotificationPanelPreferences.isEnabled(this@HomeActivity)
                binding.drawerLayout.setDrawerLockMode(
                    if (panelEnabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                    androidx.core.view.GravityCompat.START
                )
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, androidx.core.view.GravityCompat.END)
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    private fun applyDrawerWidths() {
        val width = currentWindowWidthPx()
        val menuParams = binding.drawerMenu.layoutParams as androidx.drawerlayout.widget.DrawerLayout.LayoutParams
        menuParams.width = width
        binding.drawerMenu.layoutParams = menuParams

        val notifParams = binding.drawerNotifications.layoutParams as androidx.drawerlayout.widget.DrawerLayout.LayoutParams
        notifParams.width = width
        binding.drawerNotifications.layoutParams = notifParams
    }

    private var hideRunnable: Runnable? = null

    private fun showIndicatorsWithTimeout() {
        hideRunnable?.let { binding.indicatorContainer.removeCallbacks(it) }
        
        if (!binding.indicatorContainer.isVisible) {
            binding.indicatorContainer.visibility = View.VISIBLE
            binding.indicatorContainer.alpha = 0f
            binding.indicatorContainer.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        } else {
            binding.indicatorContainer.animate().cancel()
            binding.indicatorContainer.alpha = 1f
        }
        
        hideRunnable = Runnable {
            binding.indicatorContainer.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    binding.indicatorContainer.visibility = View.INVISIBLE
                }
                .start()
        }
        binding.indicatorContainer.postDelayed(hideRunnable, 2000)
    }

    private fun updateIndicators(slideOffset: Float, isLeftDrawer: Boolean) {
        when {
            isLeftDrawer && slideOffset > 0.5f -> {
                // Notifications panel is open
                setIndicatorBackground(binding.indicatorNotifications, filled = true)
                setIndicatorBackground(binding.indicatorHome, filled = false)
                setIndicatorBackground(binding.indicatorMenu, filled = false)
            }
            !isLeftDrawer && slideOffset > 0.5f -> {
                // Menu drawer is open
                setIndicatorBackground(binding.indicatorNotifications, filled = false)
                setIndicatorBackground(binding.indicatorHome, filled = false)
                setIndicatorBackground(binding.indicatorMenu, filled = true)
            }
            else -> {
                // Home (or transitioning)
                setIndicatorBackground(binding.indicatorNotifications, filled = false)
                setIndicatorBackground(binding.indicatorHome, filled = true)
                setIndicatorBackground(binding.indicatorMenu, filled = false)
            }
        }
    }

    /**
     * Draws a page indicator dot in the active palette.
     *
     * The indicators float above the wallpaper with no scrim of their own, so they are
     * always built from the contrast-checked palette instead of the static drawables —
     * which resolved to a theme colour that could vanish against a photo.
     */
    private fun setIndicatorBackground(view: View, filled: Boolean) {
        val colors = palette
        val sizePx = dp(8)
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setSize(sizePx, sizePx)
            if (filled) {
                setColor(colors.accent)
            } else {
                setColor(Color.TRANSPARENT)
                setStroke(dp(2), FocusColors.withAlpha(colors.accent, 0.55f))
            }
        }
    }

    private fun setupDrawerMenu() {
        binding.tvMenuAllApps.setOnClickListener { 
            binding.drawerLayout.closeDrawers()
            openAllApps() 
        }
        binding.tvMenuSelectApps.setOnClickListener { 
            binding.drawerLayout.closeDrawers()
            openAppSelect() 
        }
        binding.tvMenuSettings.setOnClickListener { 
            binding.drawerLayout.closeDrawers()
            openSettings() 
        }
        binding.tvMenuProfile.setOnClickListener {
            showProfileSelectionDialog()
        }
    }

    private fun updateProfileLabel() {
        val profileKey = ProfilePreferences.getProfile(this)
        val profileNameRes = when (profileKey) {
            ProfilePreferences.PROFILE_WORK -> R.string.profile_work
            ProfilePreferences.PROFILE_DETOX -> R.string.profile_detox
            else -> R.string.profile_normal
        }
        binding.tvMenuProfile.text = getString(R.string.home_profile_label, getString(profileNameRes))
    }

    private fun showProfileSelectionDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_profile_select, null)
        applyDialogSurface(view)
        applyFontFamilyToView(view)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())

        view.findViewById<View>(R.id.btn_profile_normal).setOnClickListener {
            changeProfileWithDetoxCheck(ProfilePreferences.PROFILE_NORMAL) {
                updateProfileLabel()
                dialog.dismiss()
            }
        }
        view.findViewById<View>(R.id.btn_profile_work).setOnClickListener {
            changeProfileWithDetoxCheck(ProfilePreferences.PROFILE_WORK) {
                updateProfileLabel()
                dialog.dismiss()
            }
        }
        view.findViewById<View>(R.id.btn_profile_detox).setOnClickListener {
            dialog.dismiss()
            showDetoxDurationDialog()
        }

        dialog.show()
    }

    private fun changeProfileWithDetoxCheck(newProfile: String, onApproved: () -> Unit) {
        if (ProfilePreferences.isDetoxActive(this)) {
            // Leaving a running detox session always requires its password.
            showDetoxUnlockDialog {
                ProfilePreferences.endDetoxSession(this, newProfile)
                onApproved()
            }
        } else {
            ProfilePreferences.setProfile(this, newProfile)
            onApproved()
        }
    }

    private fun showDetoxDurationDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_detox_duration_select, null)
        applyDialogSurface(view)
        applyFontFamilyToView(view)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())

        val durationMs = arrayOf(
            15 * 60 * 1000L,
            30 * 60 * 1000L,
            60 * 60 * 1000L,
            120 * 60 * 1000L
        )

        fun onDurationSelected(duration: Long) {
            if (FocusAccessibilityService.isEnabled(this)) {
                dialog.dismiss()
                showDetoxSetPasswordDialog(duration) {}
            } else {
                showAccessibilityEnableDialog(
                    onEnable = {
                        dialog.dismiss()
                        FocusAccessibilityService.openAccessibilitySettings(this)
                    },
                    onContinue = {
                        dialog.dismiss()
                        showDetoxSetPasswordDialog(duration) {}
                    }
                )
            }
        }

        view.findViewById<View>(R.id.btn_duration_15m).setOnClickListener { onDurationSelected(durationMs[0]) }
        view.findViewById<View>(R.id.btn_duration_30m).setOnClickListener { onDurationSelected(durationMs[1]) }
        view.findViewById<View>(R.id.btn_duration_1h).setOnClickListener { onDurationSelected(durationMs[2]) }
        view.findViewById<View>(R.id.btn_duration_2h).setOnClickListener { onDurationSelected(durationMs[3]) }
        view.findViewById<View>(R.id.btn_duration_custom).setOnClickListener {
            dialog.dismiss()
            showDetoxCustomTimeDialog()
        }

        dialog.show()
    }

    private fun showAccessibilityEnableDialog(onEnable: () -> Unit, onContinue: () -> Unit) {
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.detox_accessibility_title)
            .setMessage(R.string.detox_accessibility_desc)
            .setPositiveButton(R.string.detox_accessibility_enable) { _, _ -> onEnable() }
            .setNegativeButton(R.string.detox_accessibility_continue) { _, _ -> onContinue() }
            .setCancelable(true)
            .create()
        dialog.show()
        styleMaterialDialog(dialog)
    }

    private fun showDetoxCustomTimeDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_detox_custom_time, null)
        applyDialogSurface(view)
        applyFontFamilyToView(view)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())

        val etHours = view.findViewById<android.widget.EditText>(R.id.et_hours)
        val etMinutes = view.findViewById<android.widget.EditText>(R.id.et_minutes)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btn_ok).setOnClickListener {
            val hours = etHours.text.toString().toLongOrNull() ?: 0L
            val minutes = etMinutes.text.toString().toLongOrNull() ?: 0L
            val totalMinutes = hours * 60 + minutes
            if (totalMinutes in 1..(24 * 60)) {
                dialog.dismiss()
                showDetoxSetPasswordDialog(totalMinutes * 60 * 1000L) {}
            } else {
                Toast.makeText(this, getString(R.string.error_invalid_duration), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showDetoxSetPasswordDialog(durationMs: Long, onSet: () -> Unit) {
        val view = layoutInflater.inflate(R.layout.dialog_detox_password_set, null)
        applyDialogSurface(view)
        applyFontFamilyToView(view)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
            addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        val etPassword = view.findViewById<android.widget.EditText>(R.id.et_detox_password)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btn_ok).setOnClickListener {
            val pwd = etPassword.text.toString()
            if (pwd.length < ProfilePreferences.MIN_PASSWORD_LENGTH) {
                Toast.makeText(this, getString(R.string.error_min_characters), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            it.isEnabled = false
            lifecycleScope.launch {
                // PBKDF2 is deliberately slow; never hash on the main thread.
                val started = withContext(Dispatchers.Default) {
                    ProfilePreferences.startDetoxSession(applicationContext, pwd, durationMs)
                }
                it.isEnabled = true
                if (!started) {
                    Toast.makeText(this@HomeActivity, R.string.error_save_failed, Toast.LENGTH_LONG).show()
                    return@launch
                }
                updateProfileLabel()
                binding.drawerLayout.closeDrawers()
                dialog.dismiss()
                onSet()
            }
        }

        dialog.show()
    }

    // ── Long-press hint ────────────────────────────────────────────

    private var hintToastJob: kotlinx.coroutines.Job? = null

    private fun showLongPressHintIfNeeded() {
        val prefs = getSharedPreferences("focusphone_ui", MODE_PRIVATE)
        if (!prefs.getBoolean("hint_long_press_shown", false)) {
            hintToastJob = lifecycleScope.launch {
                delay(1500)
                if (isFinishing || isDestroyed) return@launch
                Toast.makeText(this@HomeActivity, R.string.home_long_press_hint, Toast.LENGTH_LONG).show()
                prefs.edit { putBoolean("hint_long_press_shown", true) }
            }
        }
    }

    // ── Observe ────────────────────────────────────────────────────

    private fun observeFavourites() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favouriteApps.collect { apps ->
                    val empty = apps.isEmpty()
                    binding.tvEmpty.visibility         = if (empty) View.VISIBLE else View.GONE
                    binding.rvFavouriteApps.visibility = if (empty) View.GONE   else View.VISIBLE

                    // Let items use their natural height (wrap_content) so the whole
                    // group (clock + apps) centres vertically inside root_home.
                    adapter.itemHeight = ViewGroup.LayoutParams.WRAP_CONTENT
                    adapter.submitList(apps)
                    applyAppSpacing()
                    adjustRootGravity()
                }
            }
        }
    }

    // ── Notification Panel ─────────────────────────────────────────

    /**
     * One-time set-up of the notification drawer: list, swipe-to-dismiss and a single
     * observer of [NotificationRepository]. Called from [onCreate] only — running it on
     * every resume stacked a new collector and a new swipe helper each time, so after a
     * day of use one swipe dismissed the same notification dozens of times.
     */
    private fun setupNotificationPanel() {
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = notificationsAdapter
        binding.rvNotifications.itemAnimator = null

        // Swipe right to dismiss individual notifications
        val swipeHelper = androidx.recyclerview.widget.ItemTouchHelper(
            object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                0,
                androidx.recyclerview.widget.ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(
                    viewHolder: RecyclerView.ViewHolder,
                    direction: Int
                ) {
                    val position = viewHolder.bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val key = notificationsAdapter.getItemKey(position)
                        dismissNotification(key)
                    }
                }
            }
        )
        swipeHelper.attachToRecyclerView(binding.rvNotifications)

        binding.btnClearAllNotifications.setOnClickListener {
            clearAllNotifications()
        }

        binding.indicatorNotifications.contentDescription = getString(R.string.cd_notification_indicator)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotificationRepository.notifications.collect { renderNotifications(it) }
            }
        }
        refreshNotificationPanelState()
    }

    /**
     * Applies the notification-panel setting, which can change while the home screen sits
     * in the back stack. Cheap enough to run on every resume.
     */
    private fun refreshNotificationPanelState() {
        val enabled = NotificationPanelPreferences.isEnabled(this)
        binding.indicatorNotifications.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.drawerLayout.setDrawerLockMode(
            if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
            androidx.core.view.GravityCompat.START
        )
        renderNotifications(NotificationRepository.notifications.value)
    }

    /** Reads the setting at render time so a change is never applied with a stale value. */
    private fun renderNotifications(notifications: List<app.focusphone.data.repository.NotificationItem>) {
        if (!NotificationPanelPreferences.isEnabled(this)) {
            notificationsAdapter.submitList(emptyList())
            return
        }
        val empty = notifications.isEmpty()
        binding.tvNotificationsEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rvNotifications.visibility = if (empty) View.GONE else View.VISIBLE
        binding.btnClearAllNotifications.visibility = if (empty) View.GONE else View.VISIBLE
        notificationsAdapter.submitList(notifications)
    }

    private fun dismissNotification(key: String) {
        val intent = Intent(this, FocusNotificationService::class.java).apply {
            action = FocusNotificationService.ACTION_DISMISS
            putExtra(FocusNotificationService.EXTRA_KEY, key)
        }
        startService(intent)
    }

    private fun clearAllNotifications() {
        val intent = Intent(this, FocusNotificationService::class.java).apply {
            action = FocusNotificationService.ACTION_DISMISS_ALL
        }
        startService(intent)
    }

    private fun onNotificationClick(item: app.focusphone.data.repository.NotificationItem) {
        binding.drawerLayout.closeDrawers()

        if (ProfilePreferences.isAppBlocked(this, item.packageName)) {
            startActivity(Intent(this, DetoxBlockedActivity::class.java))
            return
        }

        if (enforceAppSchedule(item.packageName, item.appName)) return

        val launchIntent = packageManager.getLaunchIntentForPackage(item.packageName)
        if (launchIntent == null) {
            Toast.makeText(this, R.string.error_app_not_found, Toast.LENGTH_SHORT).show()
            return
        }

        if (ConsciousPausePreferences.isEnabled(this)) {
            val intent = Intent(this, ConsciousPauseActivity::class.java).apply {
                putExtra("target_intent", launchIntent)
                putExtra("app_label", item.appName)
            }
            startActivity(intent)
            return
        }

        // No conscious pause — try the original notification content intent
        // so the user lands directly in the relevant context (chat, email, etc.).
        val contentIntent = FocusNotificationService.getPendingIntent(item.key)
        if (contentIntent != null) {
            try {
                contentIntent.send()
                return
            } catch (e: Exception) {
                Log.w("HomeActivity", "Failed to send content intent, falling back to launch intent", e)
            }
        }

        try { startActivity(launchIntent) }
        catch (e: Exception) {
            Log.e("HomeActivity", "Failed to launch app ${item.packageName}", e)
            Toast.makeText(this, R.string.error_app_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    // ── Navigation ─────────────────────────────────────────────────

    private fun openAllApps()   = startActivity(Intent(this, AllAppsActivity::class.java))
    private fun openAppSelect() = startActivity(Intent(this, AppSelectActivity::class.java))
    private fun openSettings()  = startActivity(Intent(this, SettingsActivity::class.java))

    // ── App actions ────────────────────────────────────────────────

    private fun launchApp(app: FavouriteApp) {
        haptic(binding.rvFavouriteApps)
        if (ProfilePreferences.isAppBlocked(this, app.packageName)) {
            startActivity(Intent(this, DetoxBlockedActivity::class.java))
            return
        }
        if (enforceAppSchedule(app.packageName, app.label)) return
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        if (ConsciousPausePreferences.isEnabled(this)) {
            val intent = Intent(this, ConsciousPauseActivity::class.java).apply {
                putExtra("target_intent", launchIntent)
                putExtra("app_label", app.label)
            }
            startActivity(intent)
        } else {
            try { startActivity(launchIntent) }
            catch (e: Exception) {
                Log.e("HomeActivity", "Failed to launch app ${app.packageName}", e)
                Toast.makeText(this, R.string.error_app_not_found, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAppContextMenu(app: FavouriteApp): Boolean {
        haptic(binding.rvFavouriteApps)
        lifecycleScope.launch {
            val shortcuts = withContext(Dispatchers.IO) {
                focusPhoneApp.container.getAppShortcutsUseCase(app.packageName)
            }
            // The user may have left while the shortcuts were loading; a popup on a
            // stopped window crashes with BadTokenException.
            if (!canShowUi()) return@launch
            showAppContextMenuWithShortcuts(app, shortcuts)
        }
        return true
    }

    private fun showAppContextMenuWithShortcuts(
        app: FavouriteApp,
        shortcuts: List<app.focusphone.domain.model.AppShortcut>
    ) {
        val popup = PopupMenu(this, binding.rvFavouriteApps, Gravity.CENTER)
        popup.menuInflater.inflate(R.menu.menu_app_context, popup.menu)

        // Append dynamic shortcuts as additional menu items
        shortcuts.forEachIndexed { index, shortcut ->
            val itemId = MENU_ID_SHORTCUT_BASE + index
            popup.menu.add(android.view.Menu.NONE, itemId, android.view.Menu.NONE, shortcut.shortLabel)
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_open       -> launchApp(app)
                R.id.action_remove     -> viewModel.removeFavourite(app.packageName)
                R.id.action_app_info   -> startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .apply { data = Uri.fromParts("package", app.packageName, null) }
                )
                R.id.action_uninstall  -> {
                    if (BuildConfig.DEBUG) {
                        Log.d("FocusPhone", "Home uninstall clicked for ${app.packageName}")
                    }
                    focusPhoneApp.container.appRepository.invalidateCache()
                    pendingVerify = true // force verify on next resume

                    Toast.makeText(this@HomeActivity, R.string.uninstall_progress, Toast.LENGTH_SHORT).show()
                    if (!LauncherUtils.requestUninstall(this@HomeActivity, app.packageName)) {
                        Toast.makeText(this@HomeActivity, R.string.error_uninstall_failed, Toast.LENGTH_LONG).show()
                    }
                }
                else -> {
                    // Shortcut item
                    val shortcutIndex = item.itemId - MENU_ID_SHORTCUT_BASE
                    if (shortcutIndex in shortcuts.indices) {
                        // A shortcut opens the same app, so it obeys the same schedule.
                        if (enforceAppSchedule(app.packageName, app.label)) return@setOnMenuItemClickListener true
                        val launched = focusPhoneApp.container.getAppShortcutsUseCase.launchShortcut(shortcuts[shortcutIndex])
                        if (!launched) {
                            Toast.makeText(this@HomeActivity, R.string.error_app_not_found, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            true
        }
        popup.show()
    }

    // ── Helpers ────────────────────────────────────────────────────

    /**
     * Renders the month grid shown at the top of the launcher menu drawer.
     *
     * Colours come from [palette] rather than from raw theme attributes: the previous
     * implementation read them through `Theme.resolveAttribute(...).data`, which returns a
     * resource id instead of a colour whenever the attribute resolves to a `ColorStateList`,
     * and it ignored the custom theme entirely. Day numbers also used the *secondary* text
     * colour, which is too faint to read against a wallpaper.
     *
     * The week starts on the locale's own first weekday, so the header letters and the grid
     * agree with the rest of the system instead of being hard-coded to Monday.
     */
    private fun populateCalendar() {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val colors = effectivePalette

        // Month title formatted using localized resource string
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val monthName = monthFormat.format(calendar.time).replaceFirstChar { it.lowercase() }
        val year = calendar.get(Calendar.YEAR)
        binding.tvCalendarMonth.text = getString(R.string.calendar_date_format, monthName, currentDay, year)
        binding.tvCalendarMonth.setTextColor(colors.textPrimary)

        // ── Weekday header, in locale week order ─────────────────────
        val orderedDays = ScheduleDayLabels.orderedDays()
        val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val headerViews = listOf(
            binding.tvDay0, binding.tvDay1, binding.tvDay2,
            binding.tvDay3, binding.tvDay4, binding.tvDay5, binding.tvDay6
        )
        headerViews.forEachIndexed { index, textView ->
            val dayOfWeek = orderedDays[index]
            textView.text = ScheduleDayLabels.shortLabel(dayOfWeek).take(1)
            // The column the user is standing in is the one worth finding at a glance.
            textView.setTextColor(
                if (dayOfWeek == todayDayOfWeek) colors.textPrimary else colors.textSecondary
            )
        }

        // ── Grid ─────────────────────────────────────────────────────
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val startOffset = orderedDays.indexOf(firstDayOfWeek).coerceAtLeast(0)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        binding.calendarGrid.removeAllViews()

        val typeface = FontPreferences.getTypefaceOrNull(this)
        val dayTextSizeSp = 14f
        // Size the selection circle from the *scaled* text so it never clips the number at
        // large system font scales or with the app's own text-size multiplier.
        val dayTextSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            dayTextSizeSp,
            resources.displayMetrics
        )
        val cellSize = maxOf(dp(28), (dayTextSizePx * 2f).toInt())

        // Non-today numbers: primary text held back so today still stands out, but far more
        // legible than the old secondary grey.
        val dayColor = colors.primaryAt(0.82f)
        val weekendColor = colors.primaryAt(0.60f)

        var dayCounter = 1
        val totalWeeks = kotlin.math.ceil((startOffset + maxDays) / 7.0).toInt()

        for (week in 0 until totalWeeks) {
            val weekRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                weightSum = 7f
            }

            for (column in 0 until 7) {
                val cellIndex = week * 7 + column
                val cell = android.widget.FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                val dayView = TextView(this).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(cellSize, cellSize).apply {
                        gravity = Gravity.CENTER
                    }
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, dayTextSizeSp)
                }

                val hasDay = cellIndex >= startOffset && dayCounter <= maxDays
                if (hasDay) {
                    val dayOfWeek = orderedDays[column]
                    val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
                    dayView.text = dayCounter.toString()

                    if (dayCounter == currentDay) {
                        dayView.setTextColor(colors.onAccent)
                        dayView.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(colors.accent)
                        }
                        dayView.typeface = Typeface.create(typeface ?: Typeface.DEFAULT, Typeface.BOLD)
                        dayView.contentDescription = getString(R.string.cd_today_indicator, dayCounter)
                    } else {
                        dayView.setTextColor(if (isWeekend) weekendColor else dayColor)
                        dayView.typeface = typeface
                        dayView.contentDescription = getString(R.string.cd_day_indicator, dayCounter)
                    }
                    dayCounter++
                } else {
                    dayView.text = ""
                    dayView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }

                cell.addView(dayView)
                weekRow.addView(cell)
            }
            binding.calendarGrid.addView(weekRow)
        }

        // The day of the month the grid was built for. Used to redraw at midnight.
        renderedCalendarDay = currentDay
    }

    // ── Gesture exclusion (Android 10+ back gesture conflict) ───────

    private fun setupGestureExclusion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        binding.drawerLayout.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateGestureExclusionRects()
        }
    }

    private fun updateGestureExclusionRects() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val width = binding.drawerLayout.width
        val height = binding.drawerLayout.height
        if (width == 0 || height == 0) return
        val exclusionWidth = dp(40)
        val rects = mutableListOf<android.graphics.Rect>()

        // Right edge for menu drawer
        rects.add(android.graphics.Rect(width - exclusionWidth, 0, width, height))

        // Left edge for notification drawer (only when enabled)
        if (NotificationPanelPreferences.isEnabled(this)) {
            rects.add(android.graphics.Rect(0, 0, exclusionWidth, height))
        }

        ViewCompat.setSystemGestureExclusionRects(binding.drawerLayout, rects)
    }

    // ── Widget Host ────────────────────────────────────────────────

    /**
     * Restores the system widget (if any) into [binding.widgetContainer].
     *
     * This method is intentionally lightweight on repeated calls: if the same
     * widget ID is already inflated, it skips recreation to avoid visual flicker.
     * If the provider was uninstalled, the preference is cleared automatically.
     */
    private fun restoreWidget() {
        widgetSurface.restore(binding.widgetContainer)
    }

    private class VerticalSpacingItemDecoration(
        private val spacingPx: Int
    ) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: android.graphics.Rect,
            view: android.view.View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position != RecyclerView.NO_POSITION && position < (parent.adapter?.itemCount ?: 0) - 1) {
                outRect.bottom = spacingPx
            }
        }
    }

    // ── Contextual Tutorial ────────────────────────────────────────────────────────

    private var isTutorialActive = false

    private fun maybeStartTutorial() {
        if (isTutorialActive) return
        if (!ThemePreferences.isOnboardingComplete(this)) return
        if (TutorialPreferences.isTutorialComplete(this)) return

        val step = TutorialPreferences.getCurrentStep(this)
        if (step >= tutorialSteps.size) {
            TutorialPreferences.markTutorialComplete(this)
            return
        }

        isTutorialActive = true
        showTutorialStep(step)
    }

    private fun showTutorialStep(step: Int) {
        if (step >= tutorialSteps.size) {
            TutorialPreferences.markTutorialComplete(this)
            isTutorialActive = false
            return
        }

        val config = tutorialSteps[step]
        val targetView = config.viewProvider(this) ?: run {
            // Skip step if target view is not available
            TutorialPreferences.setCurrentStep(this, step + 1)
            showTutorialStep(step + 1)
            return
        }

        // For the profile step, open the menu drawer first and wait for animation
        if (config.openMenuDrawer) {
            binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.END)
            binding.root.postDelayed({ showTutorialOverlay(step, config, targetView) }, 450)
        } else {
            showTutorialOverlay(step, config, targetView)
        }
    }

    private fun showTutorialOverlay(step: Int, config: TutorialStepConfig, targetView: View) {
        if (isFinishing || isDestroyed) return
        val content = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        TutorialOverlayView.show(
            activityContent = content,
            targetView = targetView,
            titleRes = config.page.titleRes,
            descRes = config.page.bodyRes,
            step = step,
            totalSteps = tutorialSteps.size,
            onDismiss = {
                if (config.openMenuDrawer) {
                    binding.drawerLayout.closeDrawers()
                }
                TutorialPreferences.setCurrentStep(this, step + 1)
                isTutorialActive = false
                // Show next step after a short delay so the UI can settle
                binding.root.postDelayed({ maybeStartTutorial() }, 400)
            }
        )
    }

    private data class TutorialStepConfig(
        val page: TutorialPage,
        val viewProvider: HomeActivity.() -> View?,
        val openMenuDrawer: Boolean = false
    )

    private val tutorialSteps = listOf(
        TutorialStepConfig(
            page = TutorialContent.guidePage(TutorialTopic.HOME),
            viewProvider = { binding.clockContainer }
        ),
        TutorialStepConfig(
            page = TutorialContent.guidePage(TutorialTopic.APPS),
            viewProvider = {
                if (adapter.itemCount > 0 && binding.rvFavouriteApps.isNotEmpty()) {
                    binding.rvFavouriteApps.getChildAt(0)
                } else {
                    binding.tvEmpty
                }
            }
        ),
        TutorialStepConfig(
            page = TutorialContent.guidePage(TutorialTopic.MENU),
            viewProvider = { binding.indicatorMenu }
        ),
        TutorialStepConfig(
            page = TutorialContent.guidePage(TutorialTopic.PROFILE),
            viewProvider = { binding.tvMenuProfile },
            openMenuDrawer = true
        )
    )

    companion object {
        private const val MENU_ID_SHORTCUT_BASE = 10_000
        private const val TAG = "HomeActivity"
        /** Value of Context.STATUS_BAR_SERVICE, usable below API 33. */
        private const val STATUS_BAR_SERVICE_NAME = "statusbar"
    }

    private fun haptic(view: View) = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
