package app.focusphone.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.graphics.Color
import android.provider.AlarmClock
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
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
import app.focusphone.FocusPhoneApp
import app.focusphone.R
import app.focusphone.data.model.FavouriteApp
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.data.preferences.ConsciousPausePreferences
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.ui.pause.ConsciousPauseActivity
import app.focusphone.ui.detox.DetoxBlockedActivity
import app.focusphone.databinding.ActivityHomeBinding
import app.focusphone.domain.model.ClockType
import app.focusphone.ui.allapps.AllAppsActivity
import app.focusphone.ui.appselect.AppSelectActivity
import app.focusphone.ui.base.BaseActivity
import app.focusphone.ui.home.adapter.FavouriteAppsAdapter
import app.focusphone.ui.onboarding.OnboardingActivity
import app.focusphone.ui.settings.SettingsActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var gestureDetector: GestureDetector

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.factory(application as FocusPhoneApp)
    }

    private val adapter = FavouriteAppsAdapter(
        onAppClick     = ::launchApp,
        onAppLongClick = ::showAppContextMenu
    )

    private var lastAppliedClockType: ClockType? = null
    private var lastVerifyTime = 0L

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
        showLongPressHintIfNeeded()
        observeFavourites()
        setupBackPress()

        binding.indicatorContainer.visibility = View.INVISIBLE
        binding.indicatorContainer.alpha = 0f
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
            androidx.core.content.ContextCompat.registerReceiver(
                this, clockReceiver, IntentFilter(Intent.ACTION_TIME_TICK),
                androidx.core.content.ContextCompat.RECEIVER_EXPORTED
            )
        } catch (_: SecurityException) {
            // Some OEMs restrict ACTION_TIME_TICK
        }
        applyClockType()
        updateClock()
        verifyInstalledApps()
        ProfilePreferences.expireDetoxIfNeeded(this)
        updateProfileLabel()
        
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
    }

    private fun verifyInstalledApps() {
        val currentApps = adapter.currentList.toList() // snapshot
        if (currentApps.isEmpty()) return

        val now = System.currentTimeMillis()
        if (now - lastVerifyTime < 60 * 60 * 1000L) return
        lastVerifyTime = now

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val pm = packageManager
            currentApps.forEach { app ->
                try {
                    pm.getPackageInfo(app.packageName, 0)
                } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                    viewModel.removeFavourite(app.packageName, app.label)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(clockReceiver) } catch (_: IllegalArgumentException) { }
        hintToastJob?.cancel()
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
            binding.indicatorContainer.setPadding(0, 0, 0, bars.bottom)
            binding.appsContainer.setPadding(0, 0, 0, bars.bottom)
            insets
        }
    }

    // ── Gestures ───────────────────────────────────────────────────

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null) {
                    val diffX = e1.x - e2.x
                    val diffY = e1.y - e2.y
                    if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY)) {
                        if (kotlin.math.abs(diffX) > SWIPE_THRESHOLD && kotlin.math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                // Swipe Left
                                binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.END)
                                return true
                            }
                        }
                    }
                }
                return false
            }
        })
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
            catch (_: Exception) {
                try { startActivity(Intent(AlarmClock.ACTION_SET_ALARM)) }
                catch (_: Exception) { }
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
        if (type == lastAppliedClockType) return
        lastAppliedClockType = type

        val isHidden = type == ClockType.HIDDEN

        binding.clockContainer.visibility   = if (isHidden) View.GONE else View.VISIBLE
        binding.clockTypeDigital.visibility = if (type == ClockType.DIGITAL) View.VISIBLE else View.GONE
        binding.clockTypeStacked.visibility = if (type == ClockType.STACKED) View.VISIBLE else View.GONE
        binding.clockTypeWords  .visibility = if (type == ClockType.WORDS)   View.VISIBLE else View.GONE
        binding.clockTypeAnalog .visibility = if (type == ClockType.ANALOG)  View.VISIBLE else View.GONE

        // Ajustar centrado si el reloj está oculto y aplicar escala
        val scale = FontPreferences.getClockSize(this)
        binding.clockContainer.scaleX = scale
        binding.clockContainer.scaleY = scale

        val constraintSet = androidx.constraintlayout.widget.ConstraintSet()
        constraintSet.clone(binding.rootHome)
        if (isHidden) {
            constraintSet.connect(R.id.apps_container, androidx.constraintlayout.widget.ConstraintSet.TOP, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.TOP)
        } else {
            constraintSet.connect(R.id.apps_container, androidx.constraintlayout.widget.ConstraintSet.TOP, R.id.clock_container, androidx.constraintlayout.widget.ConstraintSet.BOTTOM)
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
        // Force the drawer to take up the full screen width so text centers perfectly
        val params = binding.drawerMenu.layoutParams as androidx.drawerlayout.widget.DrawerLayout.LayoutParams
        params.width = resources.displayMetrics.widthPixels
        binding.drawerMenu.layoutParams = params

        binding.drawerLayout.setScrimColor(Color.TRANSPARENT)
        binding.drawerLayout.drawerElevation = 0f
        
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // Translate the main content left as the drawer slides in from the right
                val moveFactor = -(drawerView.width * slideOffset)
                binding.rootHome.translationX = moveFactor
                updateIndicators(slideOffset)
                if (slideOffset > 0f) {
                    showIndicatorsWithTimeout()
                }
            }

            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
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

    private fun updateIndicators(slideOffset: Float) {
        if (slideOffset > 0.5f) {
            binding.indicatorHome.setBackgroundResource(R.drawable.shape_indicator_outline)
            binding.indicatorMenu.setBackgroundResource(R.drawable.shape_indicator_filled)
        } else {
            binding.indicatorHome.setBackgroundResource(R.drawable.shape_indicator_filled)
            binding.indicatorMenu.setBackgroundResource(R.drawable.shape_indicator_outline)
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
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val etPassword = view.findViewById<android.widget.EditText>(R.id.et_detox_password_verify)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        var incorrectAttempts = 0

        view.findViewById<View>(R.id.btn_ok).setOnClickListener {
            val entered = etPassword.text.toString()
            if (ProfilePreferences.verifyDetoxPassword(this, entered)) {
                ProfilePreferences.setDetoxPassword(this, null)
                onSuccess()
                dialog.dismiss()
            } else {
                incorrectAttempts++
                if (incorrectAttempts >= 4) {
                    ProfilePreferences.setDetoxPassword(this, null)
                    ProfilePreferences.setDetoxEndTime(this, 0L)
                    onSuccess()
                    dialog.dismiss()
                    android.widget.Toast.makeText(
                        this,
                        getString(R.string.detox_force_disabled),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    android.widget.Toast.makeText(this, getString(R.string.error_incorrect_password), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun showDetoxDurationDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_detox_duration_select, null)
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

        view.findViewById<View>(R.id.btn_duration_15m).setOnClickListener {
            dialog.dismiss()
            showDetoxSetPasswordDialog(durationMs[0]) {}
        }
        view.findViewById<View>(R.id.btn_duration_30m).setOnClickListener {
            dialog.dismiss()
            showDetoxSetPasswordDialog(durationMs[1]) {}
        }
        view.findViewById<View>(R.id.btn_duration_1h).setOnClickListener {
            dialog.dismiss()
            showDetoxSetPasswordDialog(durationMs[2]) {}
        }
        view.findViewById<View>(R.id.btn_duration_2h).setOnClickListener {
            dialog.dismiss()
            showDetoxSetPasswordDialog(durationMs[3]) {}
        }
        view.findViewById<View>(R.id.btn_duration_custom).setOnClickListener {
            dialog.dismiss()
            showDetoxCustomTimeDialog()
        }

        dialog.show()
    }

    private fun showDetoxCustomTimeDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_detox_custom_time, null)
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
            if (totalMinutes > 0) {
                dialog.dismiss()
                showDetoxSetPasswordDialog(totalMinutes * 60 * 1000L) {}
            } else {
                android.widget.Toast.makeText(this, getString(R.string.error_invalid_duration), android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showDetoxSetPasswordDialog(durationMs: Long, onSet: () -> Unit) {
        val view = layoutInflater.inflate(R.layout.dialog_detox_password_set, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

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
                updateProfileLabel()
                binding.drawerLayout.closeDrawers()
                dialog.dismiss()
                onSet()
            } else {
                android.widget.Toast.makeText(this, getString(R.string.error_min_characters), android.widget.Toast.LENGTH_SHORT).show()
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
            catch (_: Exception) { Toast.makeText(this, R.string.error_app_not_found, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun showAppContextMenu(app: FavouriteApp): Boolean {
        haptic(binding.rvFavouriteApps)
        val popup = PopupMenu(this, binding.rvFavouriteApps, Gravity.CENTER)
        popup.menuInflater.inflate(R.menu.menu_app_context, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_open     -> launchApp(app)
                R.id.action_remove   -> viewModel.removeFavourite(app.packageName, app.label)
                R.id.action_app_info -> startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .apply { data = Uri.fromParts("package", app.packageName, null) }
                )
            }; true
        }
        popup.show()
        return true
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
            textView.text = weekdays[calDayIndices[index]]?.take(1)?.uppercase(Locale.getDefault()) ?: ""
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
                        topMargin = dp(6)
                        bottomMargin = dp(6)
                    }
                    gravity = android.view.Gravity.CENTER
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
                    } else {
                        dayTextView.setTextColor(colorSecondary)
                        dayTextView.setTypeface(null, android.graphics.Typeface.NORMAL)
                    }
                    dayCounter++
                } else {
                    dayTextView.text = ""
                }
                cell.addView(dayTextView)
                weekRow.addView(cell)
            }
            binding.calendarGrid.addView(weekRow)
        }
    }

    private fun haptic(view: View) = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
