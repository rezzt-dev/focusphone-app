package app.focusphone.ui.settings

import android.animation.ObjectAnimator
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import app.focusphone.R
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.preferences.HomeLayoutPreferences
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.data.preferences.WidgetPreferences
import app.focusphone.databinding.ActivityHomeLayoutBinding
import app.focusphone.domain.model.ClockType
import app.focusphone.ui.base.BaseActivity
import app.focusphone.ui.home.adapter.FavouriteAppsAdapter
import app.focusphone.ui.widget.AppWidgetHostManager
import app.focusphone.ui.widget.WidgetPickerActivity
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale
import androidx.core.view.updatePadding

/**
 * Activity that lets the user reorder the three home-screen components
 * (clock, external widget, apps list), toggle their visibility and
 * adjust their relative sizes. Shows a **real, functional preview**
 * of the home screen with live clock, rendered widget and favourite apps.
 */
class HomeLayoutActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeLayoutBinding
    private val handler = Handler(Looper.getMainLooper())

    // Cached state
    private var currentOrder = listOf<HomeLayoutPreferences.Component>()
    private var currentClockEnabled = true
    private var currentWidgetEnabled = true
    private var currentClockWeight = 38
    private var currentWidgetWeight = 25
    private var isEditMode = false
    private var selectedComponent: HomeLayoutPreferences.Component? = null

    // Drag & drop state
    private var draggedView: FrameLayout? = null
    private var dragStartRawY = 0f
    private var longPressRunnable: Runnable? = null
    private val touchSlop by lazy {
        android.view.ViewConfiguration.get(this).scaledTouchSlop
    }

    // Widget
    private var widgetHostView: AppWidgetHostView? = null
    private var lastRestoredWidgetId = WidgetPreferences.INVALID_WIDGET_ID

    // Wiggle
    private val wiggleAnimators = mutableMapOf<View, ObjectAnimator>()

    // Apps
    private val favouriteAppsAdapter = FavouriteAppsAdapter(
        onAppClick = { /* no-op in preview */ },
        onAppLongClick = { false }
    )

    // Clock
    private val clockUpdateRunnable = Runnable {
        updateClock()
        scheduleClockUpdate()
    }
    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateClock()
        }
    }

    // Live container references (updated after every rebuildPreview)
    private var currentClockContainer: FrameLayout? = null
    private var currentWidgetContainer: FrameLayout? = null
    private var currentAppsContainer: FrameLayout? = null

    // ═══════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()
        binding.btnBack.setOnClickListener { finish() }
        binding.btnDone.setOnClickListener { exitEditMode() }

        loadState()
        setupSliderSheet()
        setupVisibilityToggles()
        observeApps()
        updateVisibilityToggles()

        // Defer first build until preview_root has measured height
        binding.previewRoot.post { rebuildPreview() }
    }

    override fun onStart() {
        super.onStart()
        AppWidgetHostManager.getInstance(this).startListening()
        registerReceiver(timeTickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        updateClock()
        scheduleClockUpdate()
        restoreWidget()
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(timeTickReceiver)
        } catch (_: IllegalArgumentException) {
        }
        handler.removeCallbacks(clockUpdateRunnable)
        AppWidgetHostManager.getInstance(this).stopListening()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_WIDGET && resultCode == RESULT_OK) {
            currentWidgetEnabled = true
            WidgetPreferences.setWidgetEnabled(this, true)
            rebuildPreview()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Insets
    // ═══════════════════════════════════════════════════════════════

    private val sliderSection by lazy { findViewById<LinearLayout>(R.id.slider_section) }
    private val toggleClock by lazy { findViewById<LinearLayout>(R.id.toggle_clock) }
    private val toggleWidget by lazy { findViewById<LinearLayout>(R.id.toggle_widget) }
    private val toggleApps by lazy { findViewById<LinearLayout>(R.id.toggle_apps) }
    private val dotClock by lazy { findViewById<View>(R.id.dot_clock) }
    private val dotWidget by lazy { findViewById<View>(R.id.dot_widget) }
    private val dotApps by lazy { findViewById<View>(R.id.dot_apps) }
    private val tvEditHint by lazy { findViewById<TextView>(R.id.tv_edit_hint) }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updatePadding(top = bars.top)
            binding.sliderSheet.setPadding(
                binding.sliderSheet.paddingLeft,
                binding.sliderSheet.paddingTop,
                binding.sliderSheet.paddingRight,
                bars.bottom + dp(8)
            )
            insets
        }
    }

    private fun setupVisibilityToggles() {
        toggleClock.setOnClickListener {
            toggleComponentVisibility(HomeLayoutPreferences.Component.CLOCK)
        }
        toggleWidget.setOnClickListener {
            if (!currentWidgetEnabled) {
                val widgetId = WidgetPreferences.getWidgetId(this)
                if (widgetId == WidgetPreferences.INVALID_WIDGET_ID) {
                    launchWidgetPicker()
                    return@setOnClickListener
                }
            }
            toggleComponentVisibility(HomeLayoutPreferences.Component.WIDGET)
        }
        toggleApps.setOnClickListener {
            // Apps are always visible; give haptic feedback to acknowledge tap
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    private fun updateVisibilityToggles() {
        // Clock
        dotClock.setBackgroundResource(
            if (currentClockEnabled) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
        )
        toggleClock.alpha = if (currentClockEnabled) 1.0f else 0.45f

        // Widget
        dotWidget.setBackgroundResource(
            if (currentWidgetEnabled) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
        )
        toggleWidget.alpha = if (currentWidgetEnabled) 1.0f else 0.45f

        // Apps (always active)
        dotApps.setBackgroundResource(R.drawable.bg_dot_active)
        toggleApps.alpha = 1.0f

        // Update edit hint
        tvEditHint.text = if (isEditMode) getString(R.string.home_layout_drag_hint) else getString(R.string.home_layout_long_press_hint)
    }

    // ═══════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════

    private fun loadState() {
        currentOrder = HomeLayoutPreferences.getOrder(this)
        currentClockEnabled = HomeLayoutPreferences.isClockEnabled(this)
        currentWidgetEnabled = HomeLayoutPreferences.isWidgetEnabled(this)
        currentClockWeight = HomeLayoutPreferences.getClockWeightPercent(this)
        currentWidgetWeight = HomeLayoutPreferences.getWidgetWeightPercent(this)

        // If the clock type is HIDDEN, treat it as disabled here for consistency
        if (FontPreferences.getClockType(this) == ClockType.HIDDEN) {
            currentClockEnabled = false
        }
    }

    private fun saveOrder() {
        HomeLayoutPreferences.setOrder(this, currentOrder)
    }

    private fun saveVisibility() {
        HomeLayoutPreferences.setClockEnabled(this, currentClockEnabled)
        HomeLayoutPreferences.setWidgetEnabled(this, currentWidgetEnabled)
    }

    private fun saveSizes() {
        HomeLayoutPreferences.setClockWeightPercent(this, currentClockWeight)
        HomeLayoutPreferences.setWidgetWeightPercent(this, currentWidgetWeight)
    }

    private fun isComponentEnabled(component: HomeLayoutPreferences.Component): Boolean = when (component) {
        HomeLayoutPreferences.Component.CLOCK -> currentClockEnabled
        HomeLayoutPreferences.Component.WIDGET -> currentWidgetEnabled
        HomeLayoutPreferences.Component.APPS -> true
    }

    // ═══════════════════════════════════════════════════════════════
    // Preview (real home screen replica)
    // ═══════════════════════════════════════════════════════════════

    private fun rebuildPreview() {
        val root = binding.previewRoot
        root.removeAllViews()

        currentClockContainer = null
        currentWidgetContainer = null
        currentAppsContainer = null

        var remainingWeight = 100
        val visibleOrder = currentOrder.filter { component ->
            when (component) {
                HomeLayoutPreferences.Component.CLOCK -> currentClockEnabled || isEditMode
                HomeLayoutPreferences.Component.WIDGET -> currentWidgetEnabled || isEditMode
                HomeLayoutPreferences.Component.APPS -> true
            }
        }

        for (component in visibleOrder) {
            val weight = when (component) {
                HomeLayoutPreferences.Component.CLOCK -> {
                    remainingWeight -= currentClockWeight
                    if (currentClockEnabled) currentClockWeight else 0
                }
                HomeLayoutPreferences.Component.WIDGET -> {
                    remainingWeight -= currentWidgetWeight
                    if (currentWidgetEnabled) currentWidgetWeight else 0
                }
                HomeLayoutPreferences.Component.APPS -> remainingWeight.coerceAtLeast(20)
            }

            val wrapper = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, weight.toFloat()
                )
                tag = component
                clipChildren = false
                setBackgroundResource(R.drawable.bg_component_dashed_border)
                setPadding(dp(8), dp(8), dp(8), dp(8))
            }

            when (component) {
                HomeLayoutPreferences.Component.CLOCK -> {
                    val clockView = layoutInflater.inflate(
                        R.layout.layout_preview_clock, wrapper, false
                    )
                    wrapper.addView(clockView)
                    currentClockContainer = wrapper
                    applyClockType()
                    applyClockScale()
                    updateClock()
                }
                HomeLayoutPreferences.Component.WIDGET -> {
                    val widgetView = layoutInflater.inflate(
                        R.layout.layout_preview_widget, wrapper, false
                    )
                    wrapper.addView(widgetView)
                    currentWidgetContainer = wrapper
                    restoreWidget()
                    wrapper.findViewById<View>(R.id.btn_add_widget)?.setOnClickListener {
                        launchWidgetPicker()
                    }
                }
                HomeLayoutPreferences.Component.APPS -> {
                    val appsView = layoutInflater.inflate(
                        R.layout.layout_preview_apps, wrapper, false
                    )
                    wrapper.addView(appsView)
                    currentAppsContainer = wrapper
                    setupAppsRecycler(appsView)
                }
            }

            // Eye button (visibility toggle) in edit mode
            val eye = ImageButton(this).apply {
                layoutParams = FrameLayout.LayoutParams(dp(40), dp(40)).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.END
                    topMargin = dp(4)
                    marginEnd = dp(4)
                }
                background = null
                scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                setImageResource(
                    if (isComponentEnabled(component)) R.drawable.ic_eye_open
                    else R.drawable.ic_eye_closed
                )
                alpha = 0.9f
                visibility = if (isEditMode) View.VISIBLE else View.GONE
                setOnClickListener { toggleComponentVisibility(component) }
            }
            wrapper.addView(eye)

            // Drag handle (edit mode only)
            if (isEditMode) {
                val dragHandle = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(48), dp(32)).apply {
                        gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                        topMargin = dp(4)
                    }
                    setImageResource(R.drawable.ic_drag_handle)
                    alpha = 0.6f
                    tag = "drag_handle"
                }
                wrapper.addView(dragHandle)

                dragHandle.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            if (!isEditMode) enterEditMode()
                            beginDrag(wrapper, event.rawY)
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            updateDrag(wrapper, event.rawY)
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            endDrag(wrapper)
                            true
                        }
                        else -> false
                    }
                }
            }

            // Dim disabled components in edit mode
            if (isEditMode && !isComponentEnabled(component)) {
                wrapper.alpha = 0.45f
            }

            // Touch handling
            setupWrapperTouch(wrapper, component)

            root.addView(wrapper)
        }

        updateVisibilityToggles()
    }

    // ═══════════════════════════════════════════════════════════════
    // Clock (replicated from HomeActivity)
    // ═══════════════════════════════════════════════════════════════

    private fun applyClockType() {
        val container = currentClockContainer ?: return
        val type = FontPreferences.getClockType(this)
        val isHidden = type == ClockType.HIDDEN

        // In the preview we keep the container visible so "none" can be shown when hidden
        container.visibility = View.VISIBLE
        container.findViewById<View>(R.id.clock_type_digital)?.visibility =
            if (type == ClockType.DIGITAL) View.VISIBLE else View.GONE
        container.findViewById<View>(R.id.clock_type_stacked)?.visibility =
            if (type == ClockType.STACKED) View.VISIBLE else View.GONE
        container.findViewById<View>(R.id.clock_type_words)?.visibility =
            if (type == ClockType.WORDS) View.VISIBLE else View.GONE
        container.findViewById<View>(R.id.clock_type_analog)?.visibility =
            if (type == ClockType.ANALOG) View.VISIBLE else View.GONE
        container.findViewById<View>(R.id.clock_type_none)?.visibility =
            if (isHidden) View.VISIBLE else View.GONE
    }

    private fun applyClockScale() {
        val container = currentClockContainer ?: return
        val scale = FontPreferences.getClockSize(this)

        container.findViewById<TextView>(R.id.tv_time)?.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, 56f * scale
        )
        container.findViewById<TextView>(R.id.tv_date)?.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, 16f * scale
        )
        container.findViewById<TextView>(R.id.tv_stacked_h)?.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, 76f * scale
        )
        container.findViewById<TextView>(R.id.tv_stacked_m)?.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, 76f * scale
        )
        container.findViewById<TextView>(R.id.tv_stacked_date)?.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, 16f * scale
        )
        container.findViewById<TextView>(R.id.tv_words_time)?.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, 34f * scale
        )
        container.findViewById<TextView>(R.id.tv_words_date)?.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, 16f * scale
        )
        container.findViewById<TextView>(R.id.tv_analog_date)?.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, 16f * scale
        )

        container.findViewById<app.focusphone.ui.widget.AnalogClockView>(R.id.view_analog)?.let { view ->
            val params = view.layoutParams
            params.height = (140f * resources.displayMetrics.density * scale).toInt()
            view.layoutParams = params
        }
    }

    private fun updateClock() {
        val container = currentClockContainer ?: return
        val now = Date()
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val dateStr = java.text.SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
        val timeStr = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)

        container.findViewById<TextView>(R.id.tv_time)?.text = timeStr
        container.findViewById<TextView>(R.id.tv_date)?.text = dateStr.lowercase(Locale.getDefault())
        container.findViewById<TextView>(R.id.tv_stacked_h)?.text = String.format("%02d", h)
        container.findViewById<TextView>(R.id.tv_stacked_m)?.text = String.format("%02d", m)
        container.findViewById<TextView>(R.id.tv_stacked_date)?.text = dateStr.lowercase(Locale.getDefault())
        container.findViewById<TextView>(R.id.tv_words_time)?.text = formatTimeWords(h, m)
        container.findViewById<TextView>(R.id.tv_words_date)?.text = dateStr.lowercase(Locale.getDefault())
        container.findViewById<app.focusphone.ui.widget.AnalogClockView>(R.id.view_analog)?.setTime(h, m)
        container.findViewById<TextView>(R.id.tv_analog_date)?.text = dateStr.lowercase(Locale.getDefault())
    }

    private fun formatTimeWords(h: Int, m: Int): String {
        val hours = resources.getStringArray(R.array.clock_words_hours)
        val minutes = resources.getStringArray(R.array.clock_words_minutes)
        val hour = hours[h % 12]
        return "$hour\n${minutes[m]}"
    }

    private fun scheduleClockUpdate() {
        val cal = Calendar.getInstance()
        val sec = cal.get(Calendar.SECOND)
        val delay = (60 - sec) * 1000L
        handler.postDelayed(clockUpdateRunnable, delay)
    }

    // ═══════════════════════════════════════════════════════════════
    // Widget (replicated from HomeActivity)
    // ═══════════════════════════════════════════════════════════════

    private fun restoreWidget() {
        val container = currentWidgetContainer ?: return
        val widgetId = WidgetPreferences.getWidgetId(this)
        val enabled = WidgetPreferences.isWidgetEnabled(this)
        val hostManager = AppWidgetHostManager.getInstance(this)

        if (!enabled || widgetId == WidgetPreferences.INVALID_WIDGET_ID) {
            widgetHostView?.let { container.removeView(it) }
            widgetHostView = null
            lastRestoredWidgetId = WidgetPreferences.INVALID_WIDGET_ID
            container.findViewById<View>(R.id.btn_add_widget)?.visibility = View.VISIBLE
            return
        }

        if (widgetId == lastRestoredWidgetId && widgetHostView != null) {
            container.findViewById<View>(R.id.btn_add_widget)?.visibility = View.GONE
            return
        }

        val info = hostManager.getAppWidgetInfo(widgetId)
        if (info == null || info.provider == null) {
            // Provider may be temporarily unavailable; don't clear preferences aggressively.
            widgetHostView?.let { container.removeView(it) }
            widgetHostView = null
            lastRestoredWidgetId = WidgetPreferences.INVALID_WIDGET_ID
            container.findViewById<View>(R.id.btn_add_widget)?.visibility = View.VISIBLE
            return
        }

        widgetHostView?.let { container.removeView(it) }
        widgetHostView = null

        // Update widget options with current container size for best compatibility
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val options = android.os.Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, container.width)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, container.width)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, container.height)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, container.height)
            }
            appWidgetManager.updateAppWidgetOptions(widgetId, options)
        } catch (e: Exception) {
            Log.w("HomeLayoutActivity", "Failed to update widget options", e)
        }

        var hostView = hostManager.createView(this, widgetId, info)
        if (hostView == null) {
            val repaired = hostManager.bindAppWidgetIdIfAllowed(widgetId, info.provider)
            if (repaired) hostView = hostManager.createView(this, widgetId, info)
            if (hostView == null) {
                Log.w("HomeLayoutActivity", "createView failed for widget id=$widgetId")
                container.findViewById<View>(R.id.btn_add_widget)?.visibility = View.VISIBLE
                return
            }
        }

        hostView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        widgetHostView = hostView
        lastRestoredWidgetId = widgetId

        try {
            container.addView(hostView, 0)
            container.findViewById<View>(R.id.btn_add_widget)?.visibility = View.GONE
        } catch (e: RuntimeException) {
            Log.e("HomeLayoutActivity", "Failed to add widget view", e)
            widgetHostView = null
            lastRestoredWidgetId = WidgetPreferences.INVALID_WIDGET_ID
            container.findViewById<View>(R.id.btn_add_widget)?.visibility = View.VISIBLE
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Apps (real favourite apps)
    // ═══════════════════════════════════════════════════════════════

    private fun observeApps() {
        val app = application as app.focusphone.FocusPhoneApp
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
            app.container.getFavouriteAppsUseCase().collect { apps ->
                favouriteAppsAdapter.submitList(apps)
                val container = currentAppsContainer
                if (container != null) {
                    val rv = container.findViewById<RecyclerView>(R.id.rv_apps)
                    val empty = container.findViewById<TextView>(R.id.tv_empty)
                    if (apps.isEmpty()) {
                        rv.visibility = View.GONE
                        empty.visibility = View.VISIBLE
                    } else {
                        rv.visibility = View.VISIBLE
                        empty.visibility = View.GONE
                        updateAppsItemHeight(apps.size)
                    }
                }
            }
            }
        }
    }

    private fun setupAppsRecycler(appsView: View) {
        val rv = appsView.findViewById<RecyclerView>(R.id.rv_apps)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = favouriteAppsAdapter
        rv.itemAnimator = null
        rv.isNestedScrollingEnabled = false
        rv.overScrollMode = View.OVER_SCROLL_NEVER

        val typeface = FontPreferences.getTypeface(this)
        favouriteAppsAdapter.updateTypeface(typeface)

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                return favouriteAppsAdapter.moveItem(from, to)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    favouriteAppsAdapter.beginDrag()
                    if (!isEditMode) {
                        enterEditMode(rebuild = false)
                    }
                    viewHolder?.itemView?.animate()
                        ?.scaleX(1.06f)?.scaleY(1.06f)
                        ?.translationZ(dpF(12))
                        ?.setDuration(150)?.start()
                    viewHolder?.itemView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.animate()
                    .scaleX(1f).scaleY(1f)
                    .translationZ(0f)
                    .setDuration(200).start()
                val finalOrder = favouriteAppsAdapter.commitReorder()
                lifecycleScope.launch {
                    try {
                        (application as app.focusphone.FocusPhoneApp).container.updateAppsOrderUseCase(finalOrder)
                    } catch (e: Exception) {
                        Log.e("HomeLayoutActivity", "Failed to save apps order", e)
                    }
                }
            }

            override fun isLongPressDragEnabled(): Boolean = true
        })
        touchHelper.attachToRecyclerView(rv)
    }

    private fun updateAppsItemHeight(appCount: Int) {
        val container = currentAppsContainer ?: return
        val available = container.height - container.paddingTop - container.paddingBottom
        val height = if (appCount > 0) (available / appCount).coerceAtMost(dp(80)) else 0
        if (height > 0) {
            favouriteAppsAdapter.itemHeight = height
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Drag & drop
    // ═══════════════════════════════════════════════════════════════

    private fun setupWrapperTouch(wrapper: FrameLayout, component: HomeLayoutPreferences.Component) {
        var startRawY = 0f
        var startRawX = 0f

        wrapper.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startRawY = event.rawY
                    startRawX = event.rawX
                    wrapper.parent.requestDisallowInterceptTouchEvent(true)
                    longPressRunnable = Runnable {
                        if (!isEditMode) enterEditMode()
                    }.also { handler.postDelayed(it, 200) }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (longPressRunnable != null) {
                        val dy = kotlin.math.abs(event.rawY - startRawY)
                        val dx = kotlin.math.abs(event.rawX - startRawX)
                        if (dy > touchSlop || dx > touchSlop) {
                            longPressRunnable?.let { handler.removeCallbacks(it) }
                            longPressRunnable = null
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    longPressRunnable = null
                    wrapper.parent.requestDisallowInterceptTouchEvent(false)
                    if (!isEditMode) {
                        onWrapperClicked(component)
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    longPressRunnable = null
                    wrapper.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            true
        }
    }

    private fun beginDrag(wrapper: FrameLayout, rawY: Float) {
        draggedView = wrapper
        dragStartRawY = rawY
        wrapper.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        wrapper.animate()
            .scaleX(1.03f).scaleY(1.03f)
            .translationZ(dpF(12))
            .setDuration(150).start()
    }

    private fun updateDrag(wrapper: FrameLayout, rawY: Float) {
        val deltaY = rawY - dragStartRawY
        wrapper.translationY = deltaY
        checkReorder(wrapper)
    }

    private fun checkReorder(dragged: FrameLayout) {
        val parent = binding.previewRoot
        val currentIndex = parent.indexOfChild(dragged)
        if (currentIndex < 0) return

        val draggedCenter = dragged.y + dragged.translationY + dragged.height / 2f

        for (i in 0 until parent.childCount) {
            if (i == currentIndex) continue
            val child = parent.getChildAt(i)
            val childCenter = child.y + child.height / 2f

            if ((currentIndex < i && draggedCenter > childCenter) ||
                (currentIndex > i && draggedCenter < childCenter)) {

                val oldTop = dragged.top

                TransitionManager.beginDelayedTransition(parent, AutoTransition().apply {
                    duration = 180
                    interpolator = DecelerateInterpolator()
                })

                val comp = currentOrder[currentIndex]
                val newOrder = currentOrder.toMutableList().apply {
                    removeAt(currentIndex)
                    add(i, comp)
                }
                currentOrder = newOrder
                saveOrder()

                parent.removeView(dragged)
                parent.addView(dragged, i)

                dragged.post {
                    val newTop = dragged.top
                    dragged.translationY += (oldTop - newTop).toFloat()
                }
                break
            }
        }
    }

    private fun endDrag(dragged: FrameLayout) {
        dragged.animate()
            .translationY(0f)
            .scaleX(1f).scaleY(1f)
            .translationZ(0f)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .start()
        binding.previewRoot.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        draggedView = null
    }

    // ═══════════════════════════════════════════════════════════════
    // Edit mode
    // ═══════════════════════════════════════════════════════════════

    private fun enterEditMode(rebuild: Boolean = true) {
        if (isEditMode) return
        isEditMode = true
        selectedComponent = null
        hideSliderSheet()
        binding.btnDone.visibility = View.VISIBLE
        binding.previewRoot.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        if (rebuild) {
            rebuildPreview()
        } else {
            applyEditModeVisuals()
        }
        startWiggle()
    }

    private fun exitEditMode() {
        if (!isEditMode) return
        isEditMode = false
        binding.btnDone.visibility = View.GONE
        stopWiggle()
        rebuildPreview()
    }

    private fun applyEditModeVisuals() {
        for (i in 0 until binding.previewRoot.childCount) {
            val wrapper = binding.previewRoot.getChildAt(i) as? FrameLayout ?: continue
            val component = wrapper.tag as? HomeLayoutPreferences.Component ?: continue

            var eye = wrapper.findViewWithTag<ImageButton>("eye_button")
            if (eye == null) {
                eye = ImageButton(this).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(40), dp(40)).apply {
                        gravity = android.view.Gravity.TOP or android.view.Gravity.END
                        topMargin = dp(4)
                        marginEnd = dp(4)
                    }
                    background = null
                    scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                    alpha = 0.9f
                    tag = "eye_button"
                    setOnClickListener { toggleComponentVisibility(component) }
                }
                wrapper.addView(eye)
            }
            eye.setImageResource(
                if (isComponentEnabled(component)) R.drawable.ic_eye_open
                else R.drawable.ic_eye_closed
            )
            eye.visibility = View.VISIBLE

            wrapper.alpha = if (isComponentEnabled(component)) 1f else 0.45f
        }
    }

    private fun toggleComponentVisibility(component: HomeLayoutPreferences.Component) {
        when (component) {
            HomeLayoutPreferences.Component.CLOCK -> {
                val newEnabled = !currentClockEnabled
                if (newEnabled) {
                    // Restoring visibility — if the type is HIDDEN, switch back to the previous one
                    if (FontPreferences.getClockType(this) == ClockType.HIDDEN) {
                        val previous = HomeLayoutPreferences.getClockTypeBeforeHidden(this)
                        FontPreferences.setClockType(this, previous)
                    }
                } else {
                    // Hiding clock — save current type (if not already hidden) and set HIDDEN
                    val currentType = FontPreferences.getClockType(this)
                    if (currentType != ClockType.HIDDEN) {
                        HomeLayoutPreferences.setClockTypeBeforeHidden(this, currentType)
                    }
                    FontPreferences.setClockType(this, ClockType.HIDDEN)
                }
                currentClockEnabled = newEnabled
            }
            HomeLayoutPreferences.Component.WIDGET -> currentWidgetEnabled = !currentWidgetEnabled
            else -> {}
        }
        saveVisibility()
        rebuildPreview()
    }

    private fun onWrapperClicked(component: HomeLayoutPreferences.Component) {
        if (isEditMode) return
        // Size adjustment is no longer offered from this screen;
        // only allow picking a widget when none is configured.
        if (component == HomeLayoutPreferences.Component.WIDGET &&
            WidgetPreferences.getWidgetId(this) == WidgetPreferences.INVALID_WIDGET_ID) {
            launchWidgetPicker()
        }
    }

    private fun startWiggle() {
        for (i in 0 until binding.previewRoot.childCount) {
            val wrapper = binding.previewRoot.getChildAt(i) as? ViewGroup ?: continue
            val content = wrapper.getChildAt(0) ?: wrapper
            ObjectAnimator.ofFloat(content, View.ROTATION, -1.5f, 1.5f).apply {
                duration = 120
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                start()
                wiggleAnimators[content] = this
            }
        }
    }

    private fun stopWiggle() {
        wiggleAnimators.values.forEach { it.cancel() }
        wiggleAnimators.keys.forEach { it.rotation = 0f }
        wiggleAnimators.clear()
    }

    // ═══════════════════════════════════════════════════════════════
    // Slider sheet
    // ═══════════════════════════════════════════════════════════════

    private fun setupSliderSheet() {
        binding.sliderSize.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            when (selectedComponent) {
                HomeLayoutPreferences.Component.CLOCK -> {
                    currentClockWeight = value.toInt()
                    HomeLayoutPreferences.setClockWeightPercent(this, currentClockWeight)
                }
                HomeLayoutPreferences.Component.WIDGET -> {
                    currentWidgetWeight = value.toInt()
                    HomeLayoutPreferences.setWidgetWeightPercent(this, currentWidgetWeight)
                }
                else -> {}
            }
            binding.tvSliderValue.text = "${value.toInt()}%"
            updateComponentWeights()
        }
    }

    private fun showSliderSheet(component: HomeLayoutPreferences.Component) {
        if (component == HomeLayoutPreferences.Component.APPS) {
            hideSliderSheet()
            return
        }
        selectedComponent = component
        val (value, titleRes) = when (component) {
            HomeLayoutPreferences.Component.CLOCK ->
                currentClockWeight to R.string.home_layout_clock_size
            HomeLayoutPreferences.Component.WIDGET ->
                currentWidgetWeight to R.string.home_layout_widget_size
            else -> return
        }
        binding.tvSliderTitle.text = getString(titleRes).lowercase()
        binding.tvSliderValue.text = "$value%"
        binding.sliderSize.value = value.toFloat()

        if (sliderSection.visibility != View.VISIBLE) {
            sliderSection.visibility = View.VISIBLE
            sliderSection.alpha = 0f
            sliderSection.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }

    private fun hideSliderSheet() {
        selectedComponent = null
        if (sliderSection.visibility == View.VISIBLE) {
            sliderSection.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction { sliderSection.visibility = View.GONE }
                .start()
        }
    }

    private fun updateComponentWeights() {
        val root = binding.previewRoot
        var remainingWeight = 100

        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i) as? FrameLayout ?: continue
            val component = child.tag as? HomeLayoutPreferences.Component ?: continue
            val weight = when (component) {
                HomeLayoutPreferences.Component.CLOCK -> {
                    remainingWeight -= currentClockWeight
                    if (currentClockEnabled) currentClockWeight else 0
                }
                HomeLayoutPreferences.Component.WIDGET -> {
                    remainingWeight -= currentWidgetWeight
                    if (currentWidgetEnabled) currentWidgetWeight else 0
                }
                HomeLayoutPreferences.Component.APPS -> remainingWeight.coerceAtLeast(20)
            }
            val params = child.layoutParams as LinearLayout.LayoutParams
            params.weight = weight.toFloat()
            child.layoutParams = params
        }
        // Recalculate app item heights since the apps container may have changed size
        val appCount = favouriteAppsAdapter.itemCount
        if (appCount > 0) updateAppsItemHeight(appCount)
    }

    // ═══════════════════════════════════════════════════════════════
    // Widget picker launcher
    // ═══════════════════════════════════════════════════════════════

    private fun launchWidgetPicker() {
        @Suppress("DEPRECATION")
        startActivityForResult(
            Intent(this, WidgetPickerActivity::class.java),
            REQUEST_PICK_WIDGET
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun dpF(value: Int): Float =
        value * resources.displayMetrics.density

    private fun isTouchInsideView(event: MotionEvent, view: View): Boolean {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        return event.rawX >= loc[0] && event.rawX <= loc[0] + view.width &&
               event.rawY >= loc[1] && event.rawY <= loc[1] + view.height
    }

    companion object {
        private const val REQUEST_PICK_WIDGET = 3001
    }
}
