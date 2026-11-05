package app.focusphone.ui.home.grid

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import app.focusphone.R
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.preferences.HomeLayoutPreferences
import app.focusphone.databinding.SettingsSliderRowBinding
import app.focusphone.domain.model.CellRect
import app.focusphone.domain.model.ClockType
import app.focusphone.domain.model.HomeGrid
import app.focusphone.domain.model.HomeGridItem
import app.focusphone.domain.model.SpanLimits
import app.focusphone.domain.util.GridLayoutEngine
import app.focusphone.ui.theme.FocusColors
import app.focusphone.ui.theme.FocusPalette
import app.focusphone.ui.widget.AppWidgetHostManager
import app.focusphone.ui.widget.WidgetPickerActivity
import app.focusphone.ui.widget.WidgetSurface
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * The home screen's layout manager: places the clock, the favourite apps and any number of
 * widgets on [HomeGridLayout], keeps their live widget views, and runs edit mode — moving,
 * resizing, adding and removing blocks and changing the grid size — like the workspace of a
 * stock launcher.
 *
 * Must be created while the [activity] is being initialised (as a property), because it
 * registers an activity-result launcher for the widget picker.
 */
class HomeGridController(
    private val activity: ComponentActivity,
    private val callbacks: Callbacks
) : HomeGridEditor.Host {

    /** What the controller needs from the home screen. */
    interface Callbacks {
        fun showDialog(dialog: AlertDialog)
        fun onEditModeChanged(editing: Boolean)

        /** The blocks moved or changed size (e.g. to spread the favourites again). */
        fun onLayoutApplied()
        fun openClockSettings()
        fun openAppSelect()

        /** The clock style changed from here (a hidden clock was added back): render it again. */
        fun onClockStyleChanged()
    }

    private val store by lazy { HomeGridStore(activity) }
    private val hostManager by lazy { AppWidgetHostManager.getInstance(activity) }
    private val density get() = activity.resources.displayMetrics.density

    private lateinit var grid: HomeGridLayout
    private lateinit var clockView: View
    private lateinit var appsView: View
    private lateinit var chrome: HomeEditChrome
    private lateinit var editor: HomeGridEditor

    private val widgetViews = mutableMapOf<Int, FrameLayout>()
    private val surfaces = mutableMapOf<Int, WidgetSurface>()
    private val editBackgrounds = mutableMapOf<View, Drawable?>()
    private val accessibilityActions = mutableMapOf<View, List<Int>>()
    private var limitsCache: ((HomeGridItem) -> SpanLimits)? = null
    private var attached = false

    /**
     * Edit mode opened by a long press whose finger is still down: the grid shrinks only once
     * it lifts, or the block being dragged would slide away from under it.
     */
    private var scaleWhenTouchEnds = false

    /** Replaced by the stored layout as soon as the grid is attached. */
    override var layout: HomeGrid = HomeGrid(HomeGrid.DEFAULT_COLUMNS, HomeGrid.MIN_ROWS, emptyList())
        private set

    override var isEditing: Boolean = false
        private set

    override val palette: FocusPalette get() = FocusPalette.of(activity)

    private val pickerLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val widgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, HomeGridItem.NO_WIDGET_ID)
            ?: HomeGridItem.NO_WIDGET_ID
        refresh()
        if (widgetId != HomeGridItem.NO_WIDGET_ID) {
            enterEditMode(HomeGridItem.keyOf(HomeGridItem.Type.WIDGET, widgetId))
        }
    }

    // ── Set-up ─────────────────────────────────────────────────────

    /** Takes over [grid], whose children [clockView] and [appsView] are the built-in blocks. */
    fun attach(grid: HomeGridLayout, clockView: View, appsView: View, chromeParent: FrameLayout) {
        this.grid = grid
        this.clockView = clockView
        this.appsView = appsView
        editor = HomeGridEditor(grid, this)
        grid.touchHandler = editor
        grid.overlay = editor
        grid.onLaidOut = ::placeItemBar
        chrome = HomeEditChrome(chromeParent)
        chrome.widgetsButton.setOnClickListener { openPicker() }
        chrome.clockButton.setOnClickListener { addClock() }
        chrome.gridButton.setOnClickListener { showGridDialog() }
        chrome.doneButton.setOnClickListener { exitEditMode() }
        attached = true
        layout = store.load()
        grid.columns = layout.columns
        grid.rows = layout.rows
        syncViews()
    }

    /** Reads the layout again and renders it; cheap enough for every return to the screen. */
    fun refresh() {
        if (!attached) return
        layout = store.load()
        limitsCache = null
        grid.columns = layout.columns
        grid.rows = layout.rows
        syncViews()
        bindWidgets()
        if (isEditing) {
            chrome.applyPalette(palette)
            applyEditDecorations()
            updateChrome()
            updateAccessibilityActions()
        }
        callbacks.onLayoutApplied()
    }

    /** The clock style may have changed: a hidden clock leaves its cells empty. */
    fun onClockTypeApplied() {
        if (!attached) return
        applyClockVisibility()
    }

    /**
     * IDs of the widgets on the grid; every other ID of the widget host is an orphan. Read
     * from storage, so it is right even before the first [refresh] and while the picker has
     * just added a widget.
     */
    fun widgetIds(): Set<Int> = store.widgetIds()

    fun setSystemInsets(top: Int, bottom: Int) {
        if (attached) chrome.setSystemInsets(top, bottom)
    }

    /** Releases every live widget view, when the screen is destroyed. */
    fun release() {
        surfaces.values.forEach { it.release() }
        surfaces.clear()
    }

    // ── Views ──────────────────────────────────────────────────────

    private fun syncViews() {
        val widgetIds = layout.widgetIds
        (widgetViews.keys - widgetIds).forEach { id ->
            surfaces.remove(id)?.release()
            widgetViews.remove(id)?.let(grid::removeView)
        }
        layout.items.forEach { item ->
            val view = viewFor(item.key) ?: createWidgetView(item.widgetId)
            if (view.parent == null) grid.addView(view)
            grid.setCells(view, item.rect)
        }
        applyClockVisibility()
        appsView.visibility = View.VISIBLE
    }

    private fun createWidgetView(widgetId: Int): FrameLayout {
        val inset = activity.resources.getDimensionPixelSize(R.dimen.home_grid_item_inset)
        return FrameLayout(grid.context).apply {
            setPadding(inset, inset, inset, inset)
            clipChildren = false
            clipToPadding = false
        }.also { widgetViews[widgetId] = it }
    }

    private fun applyClockVisibility() {
        val present = layout.has(HomeGridItem.Type.CLOCK)
        val hidden = FontPreferences.getClockType(activity) == ClockType.HIDDEN
        // In edit mode a hidden clock keeps an (empty) block so it can still be moved or removed.
        clockView.visibility = when {
            !present -> View.GONE
            hidden && !isEditing -> View.GONE
            else -> View.VISIBLE
        }
    }

    private fun bindWidgets() {
        val gone = mutableListOf<Int>()
        layout.items.filter { it.type == HomeGridItem.Type.WIDGET }.forEach { item ->
            val container = widgetViews[item.widgetId] ?: return@forEach
            val surface = surfaces.getOrPut(item.widgetId) {
                WidgetSurface(activity, item.widgetId) { onBlockLongPressed(item.key) }
            }
            val provider = item.provider?.let(ComponentName::unflattenFromString)
            if (surface.bind(container, provider) == AppWidgetHostManager.WidgetState.Gone) gone += item.widgetId
        }
        // Widgets whose app was uninstalled are dropped for good, releasing their IDs.
        if (gone.isNotEmpty()) {
            gone.forEach { store.removeWidget(it) }
            layout = store.load()
            syncViews()
        }
    }

    override fun viewFor(key: String): View? {
        val item = layout.item(key) ?: return null
        return when (item.type) {
            HomeGridItem.Type.CLOCK -> clockView
            HomeGridItem.Type.APPS -> appsView
            HomeGridItem.Type.WIDGET -> widgetViews[item.widgetId]
        }
    }

    // ── Editor host ────────────────────────────────────────────────

    override fun limitsOf(item: HomeGridItem): SpanLimits {
        val limits = limitsCache ?: store.limitsFor(currentMetrics()).also { limitsCache = it }
        return limits(item)
    }

    /** Metrics of the grid as laid out on screen, or estimated before the first layout. */
    private fun currentMetrics(): HomeGridMetrics =
        if (grid.width > 0 && grid.height > 0) {
            HomeGridMetrics(layout.columns, layout.rows, grid.cellWidth / density, grid.cellHeight / density)
        } else {
            HomeGridMetrics.estimate(activity, layout.columns, layout.rows)
        }

    override fun placeholderFor(key: String): String? {
        if (key != HomeGridItem.CLOCK_KEY) return null
        if (FontPreferences.getClockType(activity) != ClockType.HIDDEN) return null
        return activity.getString(R.string.home_edit_clock_hidden)
    }

    override fun preview(layout: HomeGrid, except: View?) = applyCells(layout, except)

    override fun commit(layout: HomeGrid) {
        this.layout = layout
        store.save(layout)
        applyCells(layout, except = null)
        updateChrome()
        updateAccessibilityActions()
        callbacks.onLayoutApplied()
    }

    private fun applyCells(target: HomeGrid, except: View?) {
        grid.animateChanges(except)
        target.items.forEach { item ->
            val view = viewFor(item.key) ?: return@forEach
            if (view !== except) grid.setCells(view, item.rect)
        }
    }

    override fun onSelectionChanged(key: String?) = placeItemBar()

    override fun onTouchFinished() {
        if (!scaleWhenTouchEnds) return
        scaleWhenTouchEnds = false
        chrome.root.doOnLayout { if (isEditing) scaleGrid(editing = true) }
    }

    override fun onInteraction(active: Boolean) {
        if (active) chrome.hideItemBar() else placeItemBar()
    }

    // ── Edit mode ──────────────────────────────────────────────────

    /** Long press on a block (widget or clock) outside edit mode. */
    fun onBlockLongPressed(key: String) {
        if (!isEditing) enterEditMode(key, fingerDown = true) else editor.select(key)
        editor.continueAfterLongPress(key)
        // A widget's own controls may have asked to keep the gesture; the grid takes it now.
        grid.requestDisallowInterceptTouchEvent(false)
    }

    override fun enterEditMode(selectKey: String?, fingerDown: Boolean) {
        if (!attached) return
        if (isEditing) {
            editor.select(selectKey)
            return
        }
        isEditing = true
        limitsCache = null
        callbacks.onEditModeChanged(true)
        chrome.applyPalette(palette)
        chrome.show(true)
        applyClockVisibility()
        applyEditDecorations()
        updateChrome()
        updateAccessibilityActions()
        // The bottom bar must be measured to know how much room to leave for it.
        scaleWhenTouchEnds = fingerDown
        if (!fingerDown) chrome.root.doOnLayout { if (isEditing) scaleGrid(editing = true) }
        editor.select(selectKey)
        grid.invalidate()
    }

    fun exitEditMode() {
        if (!isEditing) return
        editor.reset()
        isEditing = false
        scaleWhenTouchEnds = false
        chrome.show(false)
        removeEditDecorations()
        removeAccessibilityActions()
        applyClockVisibility()
        scaleGrid(editing = false)
        callbacks.onEditModeChanged(false)
        callbacks.onLayoutApplied()
        grid.invalidate()
    }

    /** Shrinks the grid towards its top while editing, leaving room for the bottom bar. */
    private fun scaleGrid(editing: Boolean) {
        val scale = if (editing && grid.height > 0) {
            val reserved = chrome.bottomHeight + (EDIT_BOTTOM_GAP_DP * density)
            ((grid.height - reserved) / grid.height).coerceIn(MIN_EDIT_SCALE, 1f)
        } else {
            1f
        }
        grid.pivotX = grid.width / 2f
        grid.pivotY = 0f
        grid.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(activity.resources.getInteger(R.integer.fp_grid_edit_duration).toLong())
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { placeItemBar() }
            .start()
    }

    /** Every block gets a quiet card so its extent is visible, even when it draws nothing. */
    private fun applyEditDecorations() {
        val colors = palette
        val inset = activity.resources.getDimensionPixelSize(R.dimen.home_grid_item_inset)
        val corner = activity.resources.getDimension(R.dimen.home_grid_corner)
        allBlockViews().forEach { view ->
            if (view !in editBackgrounds) editBackgrounds[view] = view.background
            val card = GradientDrawable().apply {
                cornerRadius = corner
                setColor(FocusColors.withAlpha(colors.textPrimary, CARD_FILL_ALPHA))
                setStroke(density.toInt().coerceAtLeast(1), FocusColors.withAlpha(colors.textPrimary, CARD_STROKE_ALPHA))
            }
            view.background = InsetDrawable(card, inset)
        }
    }

    private fun removeEditDecorations() {
        editBackgrounds.forEach { (view, original) -> view.background = original }
        editBackgrounds.clear()
    }

    private fun allBlockViews(): List<View> = layout.items.mapNotNull { viewFor(it.key) }

    private fun updateChrome() {
        if (!isEditing) return
        chrome.clockButton.visibility = if (layout.has(HomeGridItem.Type.CLOCK)) View.GONE else View.VISIBLE
        placeItemBar()
    }

    private fun placeItemBar() {
        if (!attached) return
        val key = editor.selectedKey
        val item = key?.let(layout::item)
        if (!isEditing || item == null || editor.isInteracting) {
            chrome.hideItemBar()
            return
        }
        chrome.showItemBar(titleOf(item), primaryAction(item), removeAction(item), boundsInChrome(item.rect))
    }

    /** Bounds of [rect] in the chrome's coordinates, following the grid's edit-mode scale. */
    private fun boundsInChrome(rect: CellRect): RectF {
        val bounds = grid.boundsOf(rect)
        val gridOrigin = IntArray(2).also(grid::getLocationInWindow)
        val chromeOrigin = IntArray(2).also(chrome.root::getLocationInWindow)
        val scale = grid.scaleX
        return RectF(
            gridOrigin[0] - chromeOrigin[0] + bounds.left * scale,
            gridOrigin[1] - chromeOrigin[1] + bounds.top * scale,
            gridOrigin[0] - chromeOrigin[0] + bounds.right * scale,
            gridOrigin[1] - chromeOrigin[1] + bounds.bottom * scale
        )
    }

    // ── Block actions ──────────────────────────────────────────────

    private fun titleOf(item: HomeGridItem): String = when (item.type) {
        HomeGridItem.Type.CLOCK -> activity.getString(R.string.home_layout_clock)

        HomeGridItem.Type.APPS -> activity.getString(R.string.home_layout_apps)

        HomeGridItem.Type.WIDGET -> surfaces[item.widgetId]?.info?.loadLabel(activity.packageManager)?.lowercase()
            ?: activity.getString(R.string.home_layout_widget)
    }

    private fun primaryAction(item: HomeGridItem): HomeEditChrome.Action? = when (item.type) {
        HomeGridItem.Type.CLOCK -> HomeEditChrome.Action(activity.getString(R.string.home_edit_clock_style)) {
            callbacks.openClockSettings()
        }

        HomeGridItem.Type.APPS -> HomeEditChrome.Action(activity.getString(R.string.home_edit_choose_apps)) {
            callbacks.openAppSelect()
        }

        HomeGridItem.Type.WIDGET -> surfaces[item.widgetId]?.info?.takeIf(::isReconfigurable)?.let {
            HomeEditChrome.Action(activity.getString(R.string.widget_option_settings)) { reconfigure(item.widgetId) }
        }
    }

    /** The favourite list is the one block the home screen cannot do without. */
    private fun removeAction(item: HomeGridItem): HomeEditChrome.Action? {
        if (item.type == HomeGridItem.Type.APPS) return null
        return HomeEditChrome.Action(activity.getString(R.string.home_edit_remove)) { remove(item) }
    }

    private fun remove(item: HomeGridItem) {
        editor.select(null)
        if (item.type == HomeGridItem.Type.WIDGET) {
            surfaces.remove(item.widgetId)?.release()
            widgetViews.remove(item.widgetId)?.let { view ->
                editBackgrounds.remove(view)
                accessibilityActions.remove(view)
                grid.removeView(view)
            }
            // The provider stops updating a widget nobody shows once its ID is released.
            hostManager.deleteAppWidgetId(item.widgetId)
        }
        commit(layout.without(item.key))
        applyClockVisibility()
        grid.announceForAccessibility(activity.getString(R.string.home_edit_removed, titleOf(item)))
    }

    private fun isReconfigurable(info: AppWidgetProviderInfo): Boolean =
        info.configure != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE != 0

    private fun reconfigure(widgetId: Int) {
        // The result is not needed: the provider pushes its new look on its own.
        if (!hostManager.startConfigureActivity(activity, widgetId, REQUEST_RECONFIGURE)) {
            Toast.makeText(activity, R.string.widget_picker_config_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPicker() {
        pickerLauncher.launch(Intent(activity, WidgetPickerActivity::class.java))
    }

    private fun addClock() {
        val clock = HomeGridItem(HomeGridItem.Type.CLOCK, CellRect(0, 0, layout.columns, GridLayoutEngine.DEFAULT_CLOCK_ROWS))
        val updated = GridLayoutEngine.insert(layout, clock, ::limitsOf)
        if (updated == null) {
            Toast.makeText(activity, R.string.home_grid_no_room, Toast.LENGTH_SHORT).show()
            return
        }
        // Adding the clock back is an explicit wish to see it.
        if (FontPreferences.getClockType(activity) == ClockType.HIDDEN) {
            FontPreferences.setClockType(activity, HomeLayoutPreferences.getClockTypeBeforeHidden(activity))
            callbacks.onClockStyleChanged()
        }
        commit(updated)
        applyClockVisibility()
        applyEditDecorations()
        editor.select(HomeGridItem.CLOCK_KEY)
    }

    // ── Grid size ──────────────────────────────────────────────────

    /**
     * Columns and rows on two sliders, applied live to the grid behind the dialog. A size the
     * blocks cannot fit in is refused on the spot; cancelling restores the layout.
     */
    private fun showGridDialog() {
        val original = layout
        var proposed = layout
        val content = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val columnsRow = SettingsSliderRowBinding.inflate(activity.layoutInflater, content, true)
        val rowsRow = SettingsSliderRowBinding.inflate(activity.layoutInflater, content, true)

        fun render() {
            columnsRow.tvLabel.text = activity.getString(R.string.home_grid_columns, proposed.columns)
            rowsRow.tvLabel.text = activity.getString(R.string.home_grid_rows, proposed.rows)
        }

        fun propose(columns: Int, rows: Int): Boolean {
            val resized = GridLayoutEngine.resizeGrid(original, columns, rows, limitsForGrid(columns, rows)) ?: return false
            proposed = resized
            // Shown, not saved: the editor and the floating actions follow the proposal.
            layout = resized
            grid.columns = columns
            grid.rows = rows
            applyCells(resized, except = null)
            grid.invalidate()
            render()
            return true
        }

        listOf(
            columnsRow to (HomeGrid.MIN_COLUMNS..HomeGrid.MAX_COLUMNS to original.columns),
            rowsRow to (HomeGrid.MIN_ROWS..HomeGrid.MAX_ROWS to original.rows)
        ).forEach { (row, config) ->
            val (range, value) = config
            row.ivIcon.visibility = View.GONE
            row.slider.valueFrom = range.first.toFloat()
            row.slider.valueTo = range.last.toFloat()
            row.slider.stepSize = 1f
            row.slider.value = value.toFloat()
        }
        columnsRow.slider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser && !propose(value.toInt(), proposed.rows)) {
                slider.value = proposed.columns.toFloat()
                Toast.makeText(activity, R.string.home_grid_no_room, Toast.LENGTH_SHORT).show()
            }
        }
        rowsRow.slider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser && !propose(proposed.columns, value.toInt())) {
                slider.value = proposed.rows.toFloat()
                Toast.makeText(activity, R.string.home_grid_no_room, Toast.LENGTH_SHORT).show()
            }
        }
        render()

        var saved = false
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.home_grid_title)
            .setView(content)
            .setPositiveButton(R.string.action_save) { _, _ ->
                saved = true
                limitsCache = null
                commit(proposed)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
        dialog.setOnDismissListener {
            if (saved) return@setOnDismissListener
            layout = original
            grid.columns = original.columns
            grid.rows = original.rows
            applyCells(original, except = null)
            grid.invalidate()
        }
        dialog.window?.apply {
            setGravity(Gravity.BOTTOM)
            setDimAmount(GRID_DIALOG_DIM)
        }
        callbacks.showDialog(dialog)
    }

    private fun limitsForGrid(columns: Int, rows: Int): (HomeGridItem) -> SpanLimits =
        if (grid.width > 0 && grid.height > 0) {
            val widthDp = (grid.width - grid.paddingLeft - grid.paddingRight) / density
            val heightDp = (grid.height - grid.paddingTop - grid.paddingBottom) / density
            store.limitsFor(HomeGridMetrics(columns, rows, widthDp / columns, heightDp / rows))
        } else {
            store.limitsFor(HomeGridMetrics.estimate(activity, columns, rows))
        }

    // ── Accessibility ──────────────────────────────────────────────

    /**
     * In edit mode each block offers its moves as accessibility actions, so the layout can be
     * changed without dragging (TalkBack, switch access).
     */
    private fun updateAccessibilityActions() {
        removeAccessibilityActions()
        if (!isEditing) return
        layout.items.forEach { item ->
            val view = viewFor(item.key) ?: return@forEach
            val ids = moves(item).map { (labelRes, rect) ->
                ViewCompat.addAccessibilityAction(view, activity.getString(labelRes)) { _, _ ->
                    val target = layout.item(item.key) ?: return@addAccessibilityAction false
                    val moved = rect(target.rect) ?: return@addAccessibilityAction false
                    val resizing = moved.spanX != target.rect.spanX || moved.spanY != target.rect.spanY
                    val updated = GridLayoutEngine.place(layout, item.key, moved, squeezeLimits = ::limitsOf.takeIf { resizing })
                    if (updated == null) {
                        view.announceForAccessibility(activity.getString(R.string.home_grid_no_room))
                        return@addAccessibilityAction false
                    }
                    commit(updated)
                    true
                }
            }
            accessibilityActions[view] = ids
        }
    }

    private fun moves(item: HomeGridItem): List<Pair<Int, (CellRect) -> CellRect?>> {
        val limits = limitsOf(item)
        return listOf(
            R.string.home_edit_move_up to { r: CellRect -> r.moveTo(r.x, r.y - 1).takeIf { r.y > 0 } },
            R.string.home_edit_move_down to { r: CellRect -> r.moveTo(r.x, r.y + 1).takeIf { r.bottom < layout.rows } },
            R.string.home_edit_move_left to { r: CellRect -> r.moveTo(r.x - 1, r.y).takeIf { r.x > 0 } },
            R.string.home_edit_move_right to { r: CellRect -> r.moveTo(r.x + 1, r.y).takeIf { r.right < layout.columns } },
            R.string.home_edit_wider to { r: CellRect ->
                r.copy(spanX = r.spanX + 1).takeIf { r.right < layout.columns && r.spanX < limits.maxX }
            },
            R.string.home_edit_narrower to { r: CellRect -> r.copy(spanX = r.spanX - 1).takeIf { r.spanX > limits.minX } },
            R.string.home_edit_taller to { r: CellRect ->
                r.copy(spanY = r.spanY + 1).takeIf { r.bottom < layout.rows && r.spanY < limits.maxY }
            },
            R.string.home_edit_shorter to { r: CellRect -> r.copy(spanY = r.spanY - 1).takeIf { r.spanY > limits.minY } }
        )
    }

    private fun removeAccessibilityActions() {
        accessibilityActions.forEach { (view, ids) -> ids.forEach { ViewCompat.removeAccessibilityAction(view, it) } }
        accessibilityActions.clear()
    }

    private companion object {
        const val REQUEST_RECONFIGURE = 0x5718
        const val EDIT_BOTTOM_GAP_DP = 12f
        const val MIN_EDIT_SCALE = 0.7f
        const val CARD_FILL_ALPHA = 0.05f
        const val CARD_STROKE_ALPHA = 0.14f
        const val GRID_DIALOG_DIM = 0.15f
    }
}
