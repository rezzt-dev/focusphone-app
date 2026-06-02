package app.focusphone.ui.home

import android.appwidget.AppWidgetHostView
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
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
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
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
import app.focusphone.data.preferences.NotificationBadgePreferences
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.data.preferences.GesturePreferences
import app.focusphone.data.preferences.NotificationPanelPreferences
import app.focusphone.data.preferences.WidgetPreferences
import app.focusphone.data.repository.NotificationRepository
import app.focusphone.ui.home.adapter.NotificationsAdapter
import app.focusphone.ui.pause.ConsciousPauseActivity
import app.focusphone.ui.detox.DetoxBlockedActivity
import app.focusphone.databinding.ActivityHomeBinding
import app.focusphone.domain.model.ClockType
import app.focusphone.service.FocusAccessibilityService
import app.focusphone.ui.allapps.AllAppsActivity
import app.focusphone.ui.appselect.AppSelectActivity
import app.focusphone.ui.base.BaseActivity
import app.focusphone.ui.home.adapter.FavouriteAppsAdapter
import app.focusphone.ui.onboarding.OnboardingActivity
import app.focusphone.ui.settings.SettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var gestureDetector: GestureDetector

    private var widgetHostView: AppWidgetHostView? = null
    private var lastRestoredWidgetId: Int = WidgetPreferences.INVALID_WIDGET_ID

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.factory(application as FocusPhoneApp)
    }

    private val adapter = FavouriteAppsAdapter(
        onAppClick     = ::launchApp,
        onAppLongClick = ::showAppContextMenu
    )

    private val notificationsAdapter = NotificationsAdapter()

    private var lastAppliedClockType: ClockType? = null
    private var lastVerifyTime = 0L
    private var pendingVerify = false

    private val clockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = updateClock()
    }

    // ── Lifecycle ──────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!ThemePreferences.isOnboardingComplete(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

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
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isDefaultLauncher()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(clockReceiver, IntentFilter(Intent.ACTION_TIME_TICK), Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UNSAFE_CAST")
                registerReceiver(clockReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
            }
        } catch (e: Exception) {
            // Some OEMs restrict ACTION_TIME_TICK; also IllegalArgumentException on old devices
            Log.w("HomeActivity", "Cannot register ACTION_TIME_TICK receiver", e)
        }
        applyClockType()
        applyClockScale()
        updateClock()
        if (pendingVerify) {
            pendingVerify = false
            verifyInstalledApps(force = true)
        } else {
            verifyInstalledApps()
        }
        ProfilePreferences.expireDetoxIfNeeded(this)
        updateProfileLabel()
        restoreWidget()
        setupNotificationPanel()
        adapter.updateTypeface(FontPreferences.getTypeface(this))
        adapter.updateBadgeCounts(NotificationBadgePreferences.getAllBadgeCounts(this))

        val gravity = when (app.focusphone.data.preferences.ThemePreferences.getAlignment(this)) {
            app.focusphone.data.preferences.ThemePreferences.ALIGN_LEFT -> android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            app.focusphone.data.preferences.ThemePreferences.ALIGN_RIGHT -> android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
            else -> android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.CENTER_VERTICAL
        }
        adapter.updateAlignment(gravity)
        val emptyGravity = when (app.focusphone.data.preferences.ThemePreferences.getAlignment(this)) {
            app.focusphone.data.preferences.ThemePreferences.ALIGN_LEFT -> android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            app.focusphone.data.preferences.ThemePreferences.ALIGN_RIGHT -> android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
            else -> android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.CENTER_VERTICAL
        }
        val emptyParams = binding.tvEmpty.layoutParams as android.widget.FrameLayout.LayoutParams
        emptyParams.gravity = emptyGravity
        binding.tvEmpty.layoutParams = emptyParams
        populateCalendar()
        updateGestureExclusionRects()
    }

    private fun verifyInstalledApps(force: Boolean = false) {
        val currentApps = adapter.currentList.toList() // snapshot
        if (currentApps.isEmpty()) return

        val now = System.currentTimeMillis()
        if (!force && now - lastVerifyTime < 60 * 60 * 1000L) return
        lastVerifyTime = now

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
                    viewModel.removeFavourite(app.packageName, app.label)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        app.focusphone.ui.widget.AppWidgetHostManager.getInstance(this).startListening()
        restoreWidget()
    }

    override fun onStop() {
        super.onStop()
        app.focusphone.ui.widget.AppWidgetHostManager.getInstance(this).stopListening()
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(clockReceiver) } catch (e: IllegalArgumentException) {
            Log.w("HomeActivity", "Clock receiver was not registered", e)
        }
        hintToastJob?.cancel()
        hideRunnable?.let { binding.indicatorContainer.removeCallbacks(it) }
        binding.indicatorContainer.animate().cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        widgetHostView?.let { binding.widgetContainer.removeView(it) }
        widgetHostView = null
    }



    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = packageManager.resolveActivity(intent, 0)
        return resolved?.activityInfo?.packageName == packageName
    }

    // ── Insets ─────────────────────────────────────────────────────

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.clockContainer.setPadding(0, bars.top + dp(8), 0, 0)
            binding.drawerMenu.setPadding(dp(32), bars.top + dp(32), dp(32), bars.bottom + dp(32))
            binding.drawerNotifications.setPadding(dp(32), bars.top + dp(32), dp(32), bars.bottom + dp(32))
            binding.indicatorContainer.setPadding(0, 0, 0, bars.bottom)
            binding.appsContainer.setPadding(0, 0, 0, bars.bottom)
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
    }

    private fun expandNotificationsPanel() {
        if (!GesturePreferences.isSwipeDownEnabled(this)) return
        try {
            val statusBarService = getSystemService(Context.STATUS_BAR_SERVICE)
            val method = statusBarService?.javaClass?.getMethod("expandNotificationsPanel")
            method?.invoke(statusBarService)
        } catch (e: Exception) {
            Log.w("HomeActivity", "Failed to expand notification panel", e)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        ev?.let { gestureDetector.onTouchEvent(it) }
        return super.dispatchTouchEvent(ev)
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
        val isHidden = type == ClockType.HIDDEN

        // Only mutate clock sub-views when the type actually changed
        if (type != lastAppliedClockType) {
            lastAppliedClockType = type

            binding.clockContainer.visibility   = if (isHidden) View.GONE else View.VISIBLE
            binding.clockTypeDigital.visibility = if (type == ClockType.DIGITAL) View.VISIBLE else View.GONE
            binding.clockTypeStacked.visibility = if (type == ClockType.STACKED) View.VISIBLE else View.GONE
            binding.clockTypeWords  .visibility = if (type == ClockType.WORDS)   View.VISIBLE else View.GONE
            binding.clockTypeAnalog .visibility = if (type == ClockType.ANALOG)  View.VISIBLE else View.GONE
        }

        // Constraints are always refreshed because widget visibility may change
        // independently of the clock type.
        val constraintSet = androidx.constraintlayout.widget.ConstraintSet()
        constraintSet.clone(binding.rootHome)
        val hasWidget = binding.widgetContainer.visibility == View.VISIBLE
        when {
            hasWidget -> {
                constraintSet.connect(
                    R.id.apps_container,
                    androidx.constraintlayout.widget.ConstraintSet.TOP,
                    R.id.widget_container,
                    androidx.constraintlayout.widget.ConstraintSet.BOTTOM
                )
            }
            isHidden -> {
                constraintSet.connect(
                    R.id.apps_container,
                    androidx.constraintlayout.widget.ConstraintSet.TOP,
                    androidx.constraintlayout.widget.ConstraintSet.PARENT_ID,
                    androidx.constraintlayout.widget.ConstraintSet.TOP
                )
            }
            else -> {
                constraintSet.connect(
                    R.id.apps_container,
                    androidx.constraintlayout.widget.ConstraintSet.TOP,
                    R.id.clock_container,
                    androidx.constraintlayout.widget.ConstraintSet.BOTTOM
                )
            }
        }
        constraintSet.applyTo(binding.rootHome)

        // Adjust gravity for the RecyclerView and Empty state
        val rvParams = binding.rvFavouriteApps.layoutParams as android.widget.FrameLayout.LayoutParams
        rvParams.gravity = android.view.Gravity.CENTER_VERTICAL
        binding.rvFavouriteApps.layoutParams = rvParams

        val emptyParams = binding.tvEmpty.layoutParams as android.widget.FrameLayout.LayoutParams
        emptyParams.gravity = android.view.Gravity.CENTER_VERTICAL
        binding.tvEmpty.layoutParams = emptyParams
    }



    // ── Clock update (called every minute) ────────────────────────

    private fun applyClockScale() {
        val scale = FontPreferences.getClockSize(this)

        // Digital
        binding.tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 56f * scale)
        binding.tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * scale)

        // Stacked
        binding.tvStackedH.setTextSize(TypedValue.COMPLEX_UNIT_SP, 76f * scale)
        binding.tvStackedM.setTextSize(TypedValue.COMPLEX_UNIT_SP, 76f * scale)
        binding.tvStackedDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * scale)

        // Words
        binding.tvWordsTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f * scale)
        binding.tvWordsDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * scale)

        // Analog date
        binding.tvAnalogDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * scale)

        // Analog view height (base 140dp scaled)
        val analogParams = binding.viewAnalog.layoutParams
        analogParams.height = (140f * resources.displayMetrics.density * scale).toInt()
        binding.viewAnalog.layoutParams = analogParams

        binding.clockContainer.requestLayout()
    }

    private fun updateClock() {
        val now  = Date()
        val cal  = Calendar.getInstance()
        val h    = cal.get(Calendar.HOUR_OF_DAY)
        val m    = cal.get(Calendar.MINUTE)
        val dateStr = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
        val timeStr = SimpleDateFormat("HH:mm",        Locale.getDefault()).format(now)

        // Digital
        binding.tvTime.text = timeStr
        binding.tvDate.text = dateStr

        // Stacked
        binding.tvStackedH   .text = String.format("%02d", h)
        binding.tvStackedM   .text = String.format("%02d", m)
        binding.tvStackedDate.text = dateStr

        // Words
        binding.tvWordsTime.text = formatTimeWords(h, m)
        binding.tvWordsDate.text = dateStr.lowercase(Locale.getDefault())

        // Analog
        binding.viewAnalog   .setTime(h, m)
        binding.tvAnalogDate .text = dateStr
    }

    // ── "nine forty-one" format ────────────────────────────────────

    private fun formatTimeWords(h: Int, m: Int): String {
        val hWords = listOf("twelve","one","two","three","four","five",
            "six","seven","eight","nine","ten","eleven")
        val tens = listOf("","","twenty","thirty","forty","fifty")
        val ones = listOf("","one","two","three","four","five","six","seven",
            "eight","nine","ten","eleven","twelve","thirteen","fourteen",
            "fifteen","sixteen","seventeen","eighteen","nineteen")

        val hour = hWords[h % 12]
        return when {
            m == 0        -> "$hour\no'clock"
            m < 20        -> "$hour\n${ones[m]}"
            m % 10 == 0   -> "$hour\n${tens[m / 10]}"
            else          -> "$hour\n${tens[m / 10]}-${ones[m % 10]}"
        }
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
                    adapter.moveItem(from, to)
                    adapter.notifyItemMoved(from, to)
                    return true
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
    }

    // ── Drawer Menu ────────────────────────────────────────────────

    private fun setupDrawerPushEffect() {
        // Force both drawers to take up the full screen width so text centers perfectly
        val menuParams = binding.drawerMenu.layoutParams as androidx.drawerlayout.widget.DrawerLayout.LayoutParams
        menuParams.width = resources.displayMetrics.widthPixels
        binding.drawerMenu.layoutParams = menuParams

        val notifParams = binding.drawerNotifications.layoutParams as androidx.drawerlayout.widget.DrawerLayout.LayoutParams
        notifParams.width = resources.displayMetrics.widthPixels
        binding.drawerNotifications.layoutParams = notifParams

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
                binding.indicatorNotifications.setBackgroundResource(R.drawable.shape_indicator_outline)
                binding.indicatorHome.setBackgroundResource(R.drawable.shape_indicator_filled)
                binding.indicatorMenu.setBackgroundResource(R.drawable.shape_indicator_outline)

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

    private var hideRunnable: Runnable? = null

    private fun showIndicatorsWithTimeout() {
        hideRunnable?.let { binding.indicatorContainer.removeCallbacks(it) }
        
        if (binding.indicatorContainer.visibility != View.VISIBLE) {
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
                binding.indicatorNotifications.setBackgroundResource(R.drawable.shape_indicator_filled)
                binding.indicatorHome.setBackgroundResource(R.drawable.shape_indicator_outline)
                binding.indicatorMenu.setBackgroundResource(R.drawable.shape_indicator_outline)
            }
            !isLeftDrawer && slideOffset > 0.5f -> {
                // Menu drawer is open
                binding.indicatorNotifications.setBackgroundResource(R.drawable.shape_indicator_outline)
                binding.indicatorHome.setBackgroundResource(R.drawable.shape_indicator_outline)
                binding.indicatorMenu.setBackgroundResource(R.drawable.shape_indicator_filled)
            }
            else -> {
                // Home (or transitioning)
                binding.indicatorNotifications.setBackgroundResource(R.drawable.shape_indicator_outline)
                binding.indicatorHome.setBackgroundResource(R.drawable.shape_indicator_filled)
                binding.indicatorMenu.setBackgroundResource(R.drawable.shape_indicator_outline)
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
        applyFontFamilyToView(view)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

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
            showDetoxVerifyPasswordDialog {
                ProfilePreferences.setProfile(this, newProfile)
                onApproved()
            }
        } else {
            ProfilePreferences.setProfile(this, newProfile)
            onApproved()
        }
    }

    private fun showDetoxVerifyPasswordDialog(onSuccess: () -> Unit) {
        val view = layoutInflater.inflate(R.layout.dialog_detox_password_verify, null)
        applyFontFamilyToView(view)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        val etPassword = view.findViewById<android.widget.EditText>(R.id.et_detox_password_verify)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btn_ok).setOnClickListener {
            if (ProfilePreferences.isPasswordLockedOut(this)) {
                Toast.makeText(this, getString(R.string.detox_force_disabled), Toast.LENGTH_LONG).show()
                ProfilePreferences.setDetoxPassword(this, null)
                ProfilePreferences.setDetoxEndTime(this, 0L)
                ProfilePreferences.setProfile(this, ProfilePreferences.PROFILE_NORMAL)
                ProfilePreferences.clearFailedAttempts(this)
                onSuccess()
                dialog.dismiss()
                return@setOnClickListener
            }

            val entered = etPassword.text.toString()
            if (ProfilePreferences.verifyDetoxPassword(this, entered)) {
                ProfilePreferences.clearFailedAttempts(this)
                ProfilePreferences.setDetoxPassword(this, null)
                onSuccess()
                dialog.dismiss()
            } else {
                val attempts = ProfilePreferences.getFailedAttempts(this)
                if (attempts >= 4) {
                    ProfilePreferences.setDetoxPassword(this, null)
                    ProfilePreferences.setDetoxEndTime(this, 0L)
                    ProfilePreferences.setProfile(this, ProfilePreferences.PROFILE_NORMAL)
                    ProfilePreferences.clearFailedAttempts(this)
                    onSuccess()
                    dialog.dismiss()
                    Toast.makeText(
                        this,
                        getString(R.string.detox_force_disabled),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this, getString(R.string.error_incorrect_password), Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun showDetoxDurationDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_detox_duration_select, null)
        applyFontFamilyToView(view)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

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
        applyFontFamilyToView(view)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

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
        applyFontFamilyToView(view)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        val etPassword = view.findViewById<android.widget.EditText>(R.id.et_detox_password)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btn_ok).setOnClickListener {
            val pwd = etPassword.text.toString()
            if (pwd.length >= 4) {
                ProfilePreferences.setDetoxPassword(this, pwd)
                val endTime = System.currentTimeMillis() + durationMs
                ProfilePreferences.setProfile(this, ProfilePreferences.PROFILE_DETOX)
                ProfilePreferences.setDetoxEndTime(this, endTime)
                ProfilePreferences.clearFailedAttempts(this)
                updateProfileLabel()
                binding.drawerLayout.closeDrawers()
                dialog.dismiss()
                onSet()
            } else {
                Toast.makeText(this, getString(R.string.error_min_characters), Toast.LENGTH_SHORT).show()
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
                prefs.edit().putBoolean("hint_long_press_shown", true).apply()
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

                    adapter.submitList(apps) {
                        if (!empty) {
                            val calculateHeight = {
                                val totalHeight = binding.appsContainer.height
                                if (totalHeight > 0) {
                                    val padding = binding.appsContainer.paddingTop + binding.appsContainer.paddingBottom
                                    val availableHeight = totalHeight - padding
                                    val maxItemHeight = dp(80) 
                                    val calculatedHeight = availableHeight / apps.size
                                    adapter.itemHeight = calculatedHeight.coerceAtMost(maxItemHeight)
                                }
                            }
                            
                            if (binding.appsContainer.height > 0) {
                                calculateHeight()
                            } else {
                                binding.appsContainer.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                                    override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int,
                                                                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                                        binding.appsContainer.removeOnLayoutChangeListener(this)
                                        calculateHeight()
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Notification Panel ─────────────────────────────────────────

    private fun setupNotificationPanel() {
        val enabled = NotificationPanelPreferences.isEnabled(this)
        if (enabled) {
            binding.drawerLayout.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_UNLOCKED,
                androidx.core.view.GravityCompat.START
            )
        } else {
            binding.drawerLayout.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                androidx.core.view.GravityCompat.START
            )
        }

        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = notificationsAdapter
        binding.rvNotifications.itemAnimator = null

        binding.indicatorNotifications.contentDescription = getString(R.string.cd_notification_indicator)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotificationRepository.notifications.collect { notifications ->
                    if (enabled) {
                        val empty = notifications.isEmpty()
                        binding.tvNotificationsEmpty.visibility = if (empty) View.VISIBLE else View.GONE
                        binding.rvNotifications.visibility = if (empty) View.GONE else View.VISIBLE
                        notificationsAdapter.submitList(notifications)
                    }
                }
            }
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
                (application as FocusPhoneApp).container.getAppShortcutsUseCase(app.packageName)
            }
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
                R.id.action_remove     -> viewModel.removeFavourite(app.packageName, app.label)
                R.id.action_app_info   -> startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .apply { data = Uri.fromParts("package", app.packageName, null) }
                )
                R.id.action_uninstall  -> {
                    if (BuildConfig.DEBUG) {
                        Log.d("FocusPhone", "Home uninstall clicked for ${app.packageName}")
                    }
                    (application as FocusPhoneApp).container.appRepository.invalidateCache()
                    pendingVerify = true // force verify on next resume

                    val uri = Uri.fromParts("package", app.packageName, null)
                    Toast.makeText(this@HomeActivity, R.string.uninstall_progress, Toast.LENGTH_SHORT).show()
                    try {
                        startActivity(
                            Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                                data = uri
                            }
                        )
                    } catch (e: Exception) {
                        Log.w("HomeActivity", "ACTION_UNINSTALL_PACKAGE failed for ${app.packageName}", e)
                        try {
                            startActivity(
                                Intent(Intent.ACTION_DELETE).apply {
                                    data = uri
                                }
                            )
                        } catch (e2: Exception) {
                            Log.e("HomeActivity", "ACTION_DELETE also failed for ${app.packageName}", e2)
                            Toast.makeText(this@HomeActivity, R.string.error_uninstall_failed, Toast.LENGTH_LONG).show()
                        }
                    }
                }
                else -> {
                    // Shortcut item
                    val shortcutIndex = item.itemId - MENU_ID_SHORTCUT_BASE
                    if (shortcutIndex in shortcuts.indices) {
                        val launched = (application as FocusPhoneApp).container.getAppShortcutsUseCase.launchShortcut(shortcuts[shortcutIndex])
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

    private fun populateCalendar() {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        // Month title formatted using localized resource string
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val monthName = monthFormat.format(calendar.time).replaceFirstChar { it.lowercase() }
        val year = calendar.get(Calendar.YEAR)
        binding.tvCalendarMonth.text = getString(R.string.calendar_date_format, monthName, currentDay, year)

        // Update weekday headers using localized short names (Mon..Sun)
        val weekdays = java.text.DateFormatSymbols.getInstance(Locale.getDefault()).shortWeekdays
        val dayViews = listOf(
            binding.tvDay0, binding.tvDay1, binding.tvDay2,
            binding.tvDay3, binding.tvDay4, binding.tvDay5, binding.tvDay6
        )
        // Calendar.MONDAY=2 ... Calendar.SUNDAY=1
        val calDayIndices = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )
        dayViews.forEachIndexed { index, textView ->
            textView.text = weekdays[calDayIndices[index]]?.take(1)?.lowercase(Locale.getDefault()) ?: ""
        }

        // Set to first day of month to calculate starting weekday offset
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Monday = 2, Tuesday = 3...

        // Monday = 0, Tuesday = 1, ..., Sunday = 6
        val startOffset = (firstDayOfWeek + 5) % 7
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        binding.calendarGrid.removeAllViews()
        
        var dayCounter = 1
        val totalSlots = startOffset + maxDays
        val totalWeeks = kotlin.math.ceil(totalSlots.toDouble() / 7).toInt()
        
        // Get primary and secondary colors from current theme
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val colorPrimary = typedValue.data
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
        val colorSecondary = typedValue.data
        
        for (w in 0 until totalWeeks) {
            val weekRow = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                weightSum = 7f
            }
            
            for (d in 0 until 7) {
                val cellIndex = w * 7 + d
                val cell = android.widget.FrameLayout(this).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }
                
                val dayTextView = android.widget.TextView(this).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        dp(32),
                        dp(32)
                    ).apply {
                        gravity = android.view.Gravity.CENTER
                    }
                    gravity = android.view.Gravity.CENTER
                    includeFontPadding = false
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                }
                
                if (cellIndex >= startOffset && dayCounter <= maxDays) {
                    dayTextView.text = dayCounter.toString()
                    if (dayCounter == currentDay) {
                        val bgValue = android.util.TypedValue()
                        theme.resolveAttribute(android.R.attr.colorBackground, bgValue, true)
                        dayTextView.setTextColor(bgValue.data)
                        dayTextView.setBackgroundResource(R.drawable.shape_current_day)
                        dayTextView.typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)
                        dayTextView.contentDescription = getString(R.string.cd_today_indicator, dayCounter)
                    } else {
                        dayTextView.setTextColor(colorSecondary)
                        dayTextView.setTypeface(null, android.graphics.Typeface.NORMAL)
                        dayTextView.contentDescription = getString(R.string.cd_day_indicator, dayCounter)
                    }
                    dayCounter++
                } else {
                    dayTextView.text = ""
                    dayTextView.contentDescription = ""
                }
                cell.addView(dayTextView)
                weekRow.addView(cell)
            }
            binding.calendarGrid.addView(weekRow)
        }
        applyFontFamilyToView(binding.calendarGrid)
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
        val widgetId = WidgetPreferences.getWidgetId(this)
        val enabled = WidgetPreferences.isWidgetEnabled(this)
        val hostManager = app.focusphone.ui.widget.AppWidgetHostManager.getInstance(this)

        // Nothing configured — ensure container is hidden
        if (!enabled || widgetId == WidgetPreferences.INVALID_WIDGET_ID) {
            if (binding.widgetContainer.visibility != View.GONE) {
                widgetHostView?.let { binding.widgetContainer.removeView(it) }
                widgetHostView = null
                lastRestoredWidgetId = WidgetPreferences.INVALID_WIDGET_ID
                binding.widgetContainer.visibility = View.GONE
                applyClockType()
            }
            return
        }

        // Same widget already restored — nothing to do
        if (widgetId == lastRestoredWidgetId && widgetHostView != null) {
            return
        }

        val info = hostManager.getAppWidgetInfo(widgetId)
        if (info == null) {
            // Provider was uninstalled or disabled — clear the stale preference
            WidgetPreferences.clearWidget(this)
            widgetHostView?.let { binding.widgetContainer.removeView(it) }
            widgetHostView = null
            lastRestoredWidgetId = WidgetPreferences.INVALID_WIDGET_ID
            binding.widgetContainer.visibility = View.GONE
            applyClockType()
            return
        }

        // Remove previous widget before adding the new one
        widgetHostView?.let { binding.widgetContainer.removeView(it) }

        val hostView = hostManager.createView(this, widgetId, info)
        if (hostView == null) {
            // Failed to bind widget view — clear preference to avoid repeated crashes
            WidgetPreferences.clearWidget(this)
            binding.widgetContainer.visibility = View.GONE
            applyClockType()
            return
        }
        widgetHostView = hostView
        lastRestoredWidgetId = widgetId

        binding.widgetContainer.addView(hostView)
        binding.widgetContainer.visibility = View.VISIBLE
        applyClockType()
    }

    companion object {
        private const val MENU_ID_SHORTCUT_BASE = 10_000
    }

    private fun haptic(view: View) = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
