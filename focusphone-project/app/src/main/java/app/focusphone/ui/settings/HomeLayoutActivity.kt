package app.focusphone.ui.settings

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
import app.focusphone.ui.widget.ClockRenderer
import app.focusphone.ui.widget.WidgetPickerActivity
import app.focusphone.ui.widget.WidgetSurface
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Collections

/**
 * Activity that lets the user reorder the three home-screen components
 * (clock, external widget, apps list) and toggle their visibility. Shows a
 * **real, functional preview**
 * of the home screen with live clock, rendered widget and favourite apps.
 */
class HomeLayoutActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeLayoutBinding
    private val handler = Handler(Looper.getMainLooper())

    // Cached state
    private var currentOrder = listOf<HomeLayoutPreferences.Component>()
    private var currentClockEnabled = true
    private var currentWidgetEnabled = true
    private var isEditMode = false

    // Drag & drop state
    private var dragStartRawY = 0f
    private var longPressRunnable: Runnable? = null
    private val touchSlop by lazy {
        android.view.ViewConfiguration.get(this).scaledTouchSlop
    }

    // Widget
    private lateinit var widgetSurface: WidgetSurface
    private var timeTickReceiverRegistered = false

    // Wiggle
    private val wiggleAnimators = mutableMapOf<View, ObjectAnimator>()

    // Apps
    // Rows in the preview are not tappable: it only shows how the home screen will look.
    private val favouriteAppsAdapter = FavouriteAppsAdapter(onAppClick = { _, _ -> })

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
    private var clockRenderer: ClockRenderer? = null

    private val widgetPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            currentWidgetEnabled = true
            WidgetPreferences.setWidgetEnabled(this, true)
            saveVisibility()
            rebuildPreview()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        widgetSurface = WidgetSurface(this, WidgetSurface.EmptyBehavior.SHOW_ADD_BUTTON)
        setupInsets()
        binding.btnBack.setOnClickListener { finish() }
        binding.btnDone.setOnClickListener { exitEditMode() }

        loadState()
        setupVisibilityToggles()
        observeApps()
        updateVisibilityToggles()

        // Defer first build until preview_root has measured height
        binding.previewRoot.post { rebuildPreview() }
    }

    override fun onStart() {
        super.onStart()
        AppWidgetHostManager.getInstance(this).startListening(this)
        try {
            ContextCompat.registerReceiver(
                this,
                timeTickReceiver,
                IntentFilter(Intent.ACTION_TIME_TICK),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            timeTickReceiverRegistered = true
        } catch (e: RuntimeException) {
            Log.w(TAG, "Cannot register ACTION_TIME_TICK receiver", e)
        }
        updateClock()
        scheduleClockUpdate()
        restoreWidget()
    }

    override fun onStop() {
        widgetSurface.releaseView()
        if (timeTickReceiverRegistered) {
            try {
                unregisterReceiver(timeTickReceiver)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Time receiver was not registered", e)
            } finally {
                timeTickReceiverRegistered = false
            }
        }
        handler.removeCallbacks(clockUpdateRunnable)
        AppWidgetHostManager.getInstance(this).stopListening(this)
        super.onStop()
    }

    // ═══════════════════════════════════════════════════════════════
    // Insets
    // ═══════════════════════════════════════════════════════════════

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
            binding.controlSheet.updatePadding(bottom = bars.bottom + dp(8))
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

    private fun isComponentEnabled(component: HomeLayoutPreferences.Component): Boolean = when (component) {
        HomeLayoutPreferences.Component.CLOCK -> currentClockEnabled
        HomeLayoutPreferences.Component.WIDGET -> currentWidgetEnabled
        HomeLayoutPreferences.Component.APPS -> true
    }

    // ═══════════════════════════════════════════════════════════════
    // Preview (real home screen replica)
    // ═══════════════════════════════════════════════════════════════

    // The drag handle's touch listener calls performClick() on ACTION_UP, but lint's static
    // check does not follow it through the nested `when` branch below (FP-078).
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun rebuildPreview() {
        val root = binding.previewRoot
        widgetSurface.releaseView()
        root.removeAllViews()

        currentClockContainer = null
        currentWidgetContainer = null
        currentAppsContainer = null
        clockRenderer = null

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
                    remainingWeight -= CLOCK_PREVIEW_WEIGHT
                    if (currentClockEnabled) CLOCK_PREVIEW_WEIGHT else 0
                }

                HomeLayoutPreferences.Component.WIDGET -> {
                    remainingWeight -= WIDGET_PREVIEW_WEIGHT
                    if (currentWidgetEnabled) WIDGET_PREVIEW_WEIGHT else 0
                }

                HomeLayoutPreferences.Component.APPS -> remainingWeight.coerceAtLeast(MIN_APPS_PREVIEW_WEIGHT)
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
                    clockRenderer = ClockRenderer(this, wrapper, showHiddenPlaceholder = true)
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
                contentDescription = getString(R.string.cd_toggle_visibility)
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
                    contentDescription = getString(R.string.cd_drag_handle)
                    alpha = 0.6f
                    tag = "drag_handle"
                }
                wrapper.addView(dragHandle)

                dragHandle.setOnTouchListener { v, event ->
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
                            if (event.actionMasked == MotionEvent.ACTION_UP) v.performClick()
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
        clockRenderer?.applyType(FontPreferences.getClockType(this))
    }

    private fun applyClockScale() {
        clockRenderer?.applyScale(FontPreferences.getClockSize(this))
    }

    private fun updateClock() {
        clockRenderer?.render()
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
        widgetSurface.restore(container)
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

    // performClick() is called on ACTION_UP below, but lint's static check does not follow
    // it through the nested `when` branch (FP-078).
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun setupWrapperTouch(wrapper: FrameLayout, component: HomeLayoutPreferences.Component) {
        var startRawY = 0f
        var startRawX = 0f

        wrapper.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startRawY = event.rawY
                    startRawX = event.rawX
                    wrapper.parent.requestDisallowInterceptTouchEvent(true)
                    longPressRunnable = Runnable {
                        if (!isEditMode) enterEditMode()
                    }.also { handler.postDelayed(it, ViewConfiguration.getLongPressTimeout().toLong()) }
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
                        v.performClick()
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
                (currentIndex > i && draggedCenter < childCenter)
            ) {
                val oldTop = dragged.top

                TransitionManager.beginDelayedTransition(
                    parent,
                    AutoTransition().apply {
                        duration = 180
                        interpolator = DecelerateInterpolator()
                    }
                )

                val comp = currentOrder[currentIndex]
                val newOrder = currentOrder.toMutableList().apply {
                    removeAt(currentIndex)
                    add(i, comp)
                }
                currentOrder = newOrder

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
        val feedback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        binding.previewRoot.performHapticFeedback(feedback)
        // Persisted once per gesture rather than on every crossing during the drag.
        saveOrder()
    }

    // ═══════════════════════════════════════════════════════════════
    // Edit mode
    // ═══════════════════════════════════════════════════════════════

    private fun enterEditMode(rebuild: Boolean = true) {
        if (isEditMode) return
        isEditMode = true
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
                    contentDescription = getString(R.string.cd_toggle_visibility)
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
        // Outside edit mode a tap only offers to pick a widget when none is configured.
        if (component == HomeLayoutPreferences.Component.WIDGET &&
            WidgetPreferences.getWidgetId(this) == WidgetPreferences.INVALID_WIDGET_ID
        ) {
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
    // Widget picker launcher
    // ═══════════════════════════════════════════════════════════════

    private fun launchWidgetPicker() {
        widgetPickerLauncher.launch(Intent(this, WidgetPickerActivity::class.java))
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun dpF(value: Int): Float =
        value * resources.displayMetrics.density

    companion object {
        private const val TAG = "HomeLayoutActivity"

        // Share of the preview's height given to each component. The preview only has to
        // convey the order and visibility; the real home screen sizes them by content.
        private const val CLOCK_PREVIEW_WEIGHT = 38
        private const val WIDGET_PREVIEW_WEIGHT = 25
        private const val MIN_APPS_PREVIEW_WEIGHT = 20
    }
}
