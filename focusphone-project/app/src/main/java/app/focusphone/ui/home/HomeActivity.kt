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
import app.focusphone.databinding.ActivityHomeBinding
import app.focusphone.domain.model.ClockType
import app.focusphone.ui.allapps.AllAppsActivity
import app.focusphone.ui.appselect.AppSelectActivity
import app.focusphone.ui.base.BaseActivity
import app.focusphone.ui.home.adapter.FavouriteAppsAdapter
import app.focusphone.ui.onboarding.OnboardingActivity
import app.focusphone.ui.settings.SettingsActivity
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
    }

    override fun onResume() {
        super.onResume()
        androidx.core.content.ContextCompat.registerReceiver(
            this, clockReceiver, IntentFilter(Intent.ACTION_TIME_TICK),
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )
        applyClockType()
        updateClock()
        verifyInstalledApps()
        
        val gravity = when (app.focusphone.data.preferences.ThemePreferences.getAlignment(this)) {
            app.focusphone.data.preferences.ThemePreferences.ALIGN_LEFT -> android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            app.focusphone.data.preferences.ThemePreferences.ALIGN_RIGHT -> android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
            else -> android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.CENTER_VERTICAL
        }
        adapter.updateAlignment(gravity)
    }

    private fun verifyInstalledApps() {
        val currentApps = adapter.currentList
        if (currentApps.isEmpty()) return
        
        val pm = packageManager
        currentApps.forEach { app ->
            try {
                pm.getPackageInfo(app.packageName, 0)
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                // App was uninstalled, remove it from favourites
                viewModel.removeFavourite(app.packageName, app.label)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(clockReceiver)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Only swallow back if we ARE the default launcher
        val isDefault = isDefaultLauncher()
        if (!isDefault) { super.onBackPressed() }
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
            binding.drawerMenu.setPadding(0, bars.top, 0, bars.bottom)
            binding.indicatorContainer.setPadding(0, 0, 0, bars.bottom)
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
        val isHidden = type == ClockType.HIDDEN
        
        binding.clockContainer.visibility   = if (isHidden) View.GONE else View.VISIBLE
        binding.clockTypeDigital.visibility = if (type == ClockType.DIGITAL) View.VISIBLE else View.GONE
        binding.clockTypeStacked.visibility = if (type == ClockType.STACKED) View.VISIBLE else View.GONE
        binding.clockTypeWords  .visibility = if (type == ClockType.WORDS)   View.VISIBLE else View.GONE
        binding.clockTypeAnalog .visibility = if (type == ClockType.ANALOG)  View.VISIBLE else View.GONE
        
        // Ajustar centrado si el reloj está oculto
        val constraintSet = androidx.constraintlayout.widget.ConstraintSet()
        constraintSet.clone(binding.rootHome)
        if (isHidden) {
            constraintSet.connect(R.id.apps_container, androidx.constraintlayout.widget.ConstraintSet.TOP, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.TOP)
        } else {
            constraintSet.connect(R.id.apps_container, androidx.constraintlayout.widget.ConstraintSet.TOP, R.id.guideline_top, androidx.constraintlayout.widget.ConstraintSet.BOTTOM)
        }
        constraintSet.applyTo(binding.rootHome)
        
        // Adjust gravity for the RecyclerView and Empty state
        val rvParams = binding.rvFavouriteApps.layoutParams as android.widget.FrameLayout.LayoutParams
        rvParams.gravity = if (isHidden) android.view.Gravity.CENTER_VERTICAL else android.view.Gravity.TOP
        binding.rvFavouriteApps.layoutParams = rvParams
        
        val emptyParams = binding.tvEmpty.layoutParams as android.widget.FrameLayout.LayoutParams
        emptyParams.gravity = if (isHidden) android.view.Gravity.CENTER_VERTICAL else android.view.Gravity.TOP
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
            }

            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
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
    }

    // ── Long-press hint ────────────────────────────────────────────

    private fun showLongPressHintIfNeeded() {
        val prefs = getSharedPreferences("focusphone_ui", MODE_PRIVATE)
        if (!prefs.getBoolean("hint_long_press_shown", false)) {
            binding.rootHome.postDelayed({
                Toast.makeText(this, R.string.home_long_press_hint, Toast.LENGTH_LONG).show()
                prefs.edit().putBoolean("hint_long_press_shown", true).apply()
            }, 1500)
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
                            
                            // Si ya tiene altura, calcular inmediatamente
                            if (binding.appsContainer.height > 0) {
                                calculateHeight()
                            }
                            
                            // Asegurar el cálculo si el layout cambia
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

    // ── Navigation ─────────────────────────────────────────────────

    private fun openAllApps()   = startActivity(Intent(this, AllAppsActivity::class.java))
    private fun openAppSelect() = startActivity(Intent(this, AppSelectActivity::class.java))
    private fun openSettings()  = startActivity(Intent(this, SettingsActivity::class.java))

    // ── App actions ────────────────────────────────────────────────

    private fun launchApp(app: FavouriteApp) {
        haptic(binding.rvFavouriteApps)
        try { startActivity(packageManager.getLaunchIntentForPackage(app.packageName) ?: return) }
        catch (_: Exception) { Toast.makeText(this, R.string.error_app_not_found, Toast.LENGTH_SHORT).show() }
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

    private fun haptic(view: View) = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
