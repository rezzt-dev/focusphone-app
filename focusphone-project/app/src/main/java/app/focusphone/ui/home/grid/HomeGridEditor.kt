package app.focusphone.ui.home.grid

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import app.focusphone.R
import app.focusphone.domain.model.CellRect
import app.focusphone.domain.model.HomeGrid
import app.focusphone.domain.model.HomeGridItem
import app.focusphone.domain.model.SpanLimits
import app.focusphone.domain.util.GridLayoutEngine
import app.focusphone.ui.theme.FocusColors
import app.focusphone.ui.theme.FocusPalette
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Gestures and drawing of the home grid, in and out of edit mode.
 *
 * Outside edit mode it only watches for a long press on empty room, which opens edit mode.
 * In edit mode it owns every touch on the grid, as a stock launcher does:
 * - **drag** a block: it lifts and follows the finger; once the target cell has been still
 *   for a moment the other blocks make way ([GridLayoutEngine.place]); releasing drops it
 *   there, or sends it back when there is no room;
 * - **tap** a block to select it: a frame with a handle on each side it can grow along;
 * - **drag a handle** to resize, snapping to cells, within the block's limits, pushing the
 *   other blocks out of the way;
 * - **tap** empty room to clear the selection.
 */
class HomeGridEditor(
    private val grid: HomeGridLayout,
    private val host: Host
) : HomeGridLayout.TouchHandler, HomeGridLayout.Overlay {

    /** What the editor needs from the screen that owns the grid. */
    interface Host {
        val isEditing: Boolean

        /** The committed layout. */
        val layout: HomeGrid
        val palette: FocusPalette

        fun viewFor(key: String): View?
        fun limitsOf(item: HomeGridItem): SpanLimits

        /** A text to draw over a block that shows nothing on its own (a hidden clock), or null. */
        fun placeholderFor(key: String): String?

        /** [fingerDown]: opened by a long press whose gesture may go on as a drag. */
        fun enterEditMode(selectKey: String?, fingerDown: Boolean = false)
        fun onSelectionChanged(key: String?)

        /** Shows [layout] without saving it, sliding every block but [except]. */
        fun preview(layout: HomeGrid, except: View?)

        /** Shows and saves [layout]. */
        fun commit(layout: HomeGrid)

        /** A drag or resize started (true) or ended (false). */
        fun onInteraction(active: Boolean)

        /** The finger left the screen (or the gesture was cancelled) in edit mode. */
        fun onTouchFinished()
    }

    private enum class Gesture { NONE, PRESS_ITEM, PRESS_EMPTY, PRESS_HANDLE, DRAG, RESIZE }

    enum class Handle { LEFT, TOP, RIGHT, BOTTOM }

    private val context = grid.context
    private val resources = context.resources
    private val density = resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val reorderDelay = resources.getInteger(R.integer.fp_grid_reorder_delay).toLong()
    private val liftDuration = resources.getInteger(R.integer.fp_press_in_duration).toLong() * 2
    private val cellInset = resources.getDimension(R.dimen.home_grid_item_inset)
    private val corner = resources.getDimension(R.dimen.home_grid_corner)
    private val handleRadius = resources.getDimension(R.dimen.home_grid_handle_radius)
    private val handleTouch = resources.getDimension(R.dimen.home_grid_handle_touch)

    private var gesture = Gesture.NONE
    private var downX = 0f
    private var downY = 0f
    private var pressedKey: String? = null
    private var pressedHandle: Handle? = null

    /** The block whose frame and handles are shown. */
    var selectedKey: String? = null
        private set

    // Drag
    private var dragKey: String? = null
    private var dragView: View? = null
    private var dragBase: HomeGrid? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var dragTarget: CellRect? = null
    private var dragSolution: HomeGrid? = null
    private var pendingTarget: CellRect? = null
    private val reorderRunnable = Runnable { pendingTarget?.let(::applyDragTarget) }

    // Resize
    private var resizeBase: HomeGrid? = null
    private var resizeLimits: SpanLimits = SpanLimits.ANY
    private var resizeRect: CellRect? = null
    private var resizeSolution: HomeGrid? = null

    private val longPressRunnable = Runnable {
        grid.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        host.enterEditMode(pressedKey, fingerDown = true)
        gesture = if (pressedKey != null) Gesture.PRESS_ITEM else Gesture.PRESS_EMPTY
    }

    val isInteracting: Boolean get() = gesture == Gesture.DRAG || gesture == Gesture.RESIZE

    // ── Selection ──────────────────────────────────────────────────

    fun select(key: String?) {
        if (selectedKey == key) return
        selectedKey = key
        host.onSelectionChanged(key)
        grid.invalidate()
    }

    /**
     * Called when a long press on a block opened edit mode: if the finger then moves, the
     * same gesture becomes a drag of that block.
     */
    fun continueAfterLongPress(key: String) {
        pressedKey = key
        gesture = Gesture.PRESS_ITEM
    }

    /** Drops any gesture in progress, e.g. when edit mode is left from outside. */
    fun reset() {
        grid.removeCallbacks(longPressRunnable)
        if (gesture == Gesture.DRAG) endDrag(commit = false)
        if (gesture == Gesture.RESIZE) endResize()
        gesture = Gesture.NONE
        pressedKey = null
        select(null)
    }

    // ── Touch ──────────────────────────────────────────────────────

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            downX = event.x
            downY = event.y
            if (!host.isEditing) {
                pressedKey = keyAt(event.x, event.y)
                gesture = Gesture.NONE
            }
        }
        // A long press on a block opened edit mode and the finger lifted without dragging:
        // the gesture ends on the block, never reaching onTouchEvent.
        val ended = event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL
        if (host.isEditing && ended && gesture == Gesture.PRESS_ITEM) {
            gesture = Gesture.NONE
            host.onTouchFinished()
        }
        // In edit mode the blocks are not interactive: every touch belongs to the editor.
        return host.isEditing
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = if (host.isEditing) onEditTouch(event) else onIdleTouch(event)

    /** Outside edit mode: only touches no block claimed get here (empty room, blank parts). */
    private fun onIdleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                pressedKey = keyAt(event.x, event.y)
                grid.postDelayed(longPressRunnable, longPressTimeout)
            }

            MotionEvent.ACTION_MOVE -> if (movedBeyond(event, touchSlop.toFloat())) grid.removeCallbacks(longPressRunnable)

            MotionEvent.ACTION_UP -> {
                grid.removeCallbacks(longPressRunnable)
                grid.performClick()
            }

            MotionEvent.ACTION_CANCEL -> grid.removeCallbacks(longPressRunnable)
        }
        return true
    }

    private fun onEditTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> onEditDown(event)

            MotionEvent.ACTION_MOVE -> onEditMove(event)

            MotionEvent.ACTION_UP -> {
                when (gesture) {
                    Gesture.DRAG -> endDrag(commit = true)
                    Gesture.RESIZE -> endResize()
                    Gesture.PRESS_ITEM -> select(pressedKey)
                    Gesture.PRESS_EMPTY -> select(null)
                    else -> Unit
                }
                if (gesture == Gesture.PRESS_ITEM || gesture == Gesture.PRESS_EMPTY) grid.performClick()
                gesture = Gesture.NONE
                host.onTouchFinished()
            }

            MotionEvent.ACTION_CANCEL -> {
                if (gesture == Gesture.DRAG) endDrag(commit = false)
                if (gesture == Gesture.RESIZE) endResize()
                gesture = Gesture.NONE
                host.onTouchFinished()
            }
        }
        return true
    }

    private fun onEditDown(event: MotionEvent) {
        downX = event.x
        downY = event.y
        grid.parent?.requestDisallowInterceptTouchEvent(true)
        val handle = handleAt(event.x, event.y)
        if (handle != null) {
            pressedHandle = handle
            gesture = Gesture.PRESS_HANDLE
            return
        }
        pressedKey = keyAt(event.x, event.y)
        gesture = if (pressedKey != null) Gesture.PRESS_ITEM else Gesture.PRESS_EMPTY
    }

    private fun onEditMove(event: MotionEvent) {
        when (gesture) {
            Gesture.PRESS_ITEM -> if (movedBeyond(event, touchSlop.toFloat())) pressedKey?.let { startDrag(it, event) }
            Gesture.PRESS_HANDLE -> if (movedBeyond(event, touchSlop / 2f)) startResize()
            Gesture.DRAG -> updateDrag(event)
            Gesture.RESIZE -> updateResize(event)
            else -> Unit
        }
    }

    private fun movedBeyond(event: MotionEvent, distance: Float): Boolean =
        abs(event.x - downX) > distance || abs(event.y - downY) > distance

    private fun keyAt(x: Float, y: Float): String? {
        val bounds = RectF()
        return host.layout.items.lastOrNull { grid.boundsOf(it.rect, bounds).contains(x, y) }?.key
    }

    // ── Drag ───────────────────────────────────────────────────────

    private fun startDrag(key: String, event: MotionEvent) {
        val view = host.viewFor(key) ?: return
        val item = host.layout.item(key) ?: return
        gesture = Gesture.DRAG
        dragKey = key
        dragView = view
        dragBase = host.layout
        dragTarget = item.rect
        dragSolution = host.layout
        pendingTarget = item.rect
        dragOffsetX = downX - view.left
        dragOffsetY = downY - view.top
        select(key)
        host.onInteraction(true)
        view.animate()
            .scaleX(LIFT_SCALE)
            .scaleY(LIFT_SCALE)
            .alpha(LIFT_ALPHA)
            .translationZ(LIFT_ELEVATION_DP * density)
            .setDuration(liftDuration)
            .start()
        haptic(HapticFeedbackConstants.LONG_PRESS)
        updateDrag(event)
    }

    private fun updateDrag(event: MotionEvent) {
        val view = dragView ?: return
        val span = dragTarget ?: return
        val left = event.x - dragOffsetX
        val top = event.y - dragOffsetY
        view.translationX = left - view.left
        view.translationY = top - view.top

        // The block snaps by its top-left corner, like an icon in a stock launcher.
        val column = grid.nearestColumnLine(left).coerceIn(0, grid.columns - span.spanX)
        val row = grid.nearestRowLine(top).coerceIn(0, grid.rows - span.spanY)
        val candidate = CellRect(column, row, span.spanX, span.spanY)
        if (candidate != pendingTarget) {
            pendingTarget = candidate
            grid.removeCallbacks(reorderRunnable)
            grid.postDelayed(reorderRunnable, reorderDelay)
        }
        grid.invalidate()
    }

    /** The target cell held still: work out the arrangement and let the other blocks make way. */
    private fun applyDragTarget(candidate: CellRect) {
        val base = dragBase ?: return
        val key = dragKey ?: return
        if (candidate == dragTarget && dragSolution != null) return
        val solution = GridLayoutEngine.place(base, key, candidate)
        dragTarget = candidate
        if (solution != dragSolution) {
            dragSolution = solution
            host.preview(solution ?: base, except = dragView)
            if (solution != null) haptic(HapticFeedbackConstants.CLOCK_TICK)
        }
        grid.invalidate()
    }

    private fun endDrag(commit: Boolean) {
        grid.removeCallbacks(reorderRunnable)
        val view = dragView
        val base = dragBase
        if (commit) pendingTarget?.let(::applyDragTarget)
        val solution = dragSolution.takeIf { commit }

        view?.animate()
            ?.scaleX(1f)
            ?.scaleY(1f)
            ?.alpha(1f)
            ?.translationZ(0f)
            ?.setDuration(liftDuration)
            ?.start()
        if (solution != null && base != null && solution != base) {
            // Lands in its new cell from where the finger let go of it (the commit animates).
            host.commit(solution)
            haptic(confirmFeedback())
        } else {
            if (base != null) host.preview(base, except = view)
            view?.animate()?.translationX(0f)?.translationY(0f)?.setDuration(liftDuration)?.start()
            if (commit && dragSolution == null) haptic(rejectFeedback())
        }

        dragKey = null
        dragView = null
        dragBase = null
        dragTarget = null
        dragSolution = null
        pendingTarget = null
        gesture = Gesture.NONE
        host.onInteraction(false)
        grid.invalidate()
    }

    // ── Resize ─────────────────────────────────────────────────────

    private fun startResize() {
        val key = selectedKey ?: return
        val item = host.layout.item(key) ?: return
        gesture = Gesture.RESIZE
        resizeBase = host.layout
        resizeLimits = host.limitsOf(item)
        resizeRect = item.rect
        resizeSolution = null
        host.onInteraction(true)
    }

    private fun updateResize(event: MotionEvent) {
        val key = selectedKey ?: return
        val base = resizeBase ?: return
        val start = base.item(key)?.rect ?: return
        val handle = pressedHandle ?: return
        val maxX = resizeLimits.maxX.coerceAtMost(grid.columns)
        val maxY = resizeLimits.maxY.coerceAtMost(grid.rows)
        var left = start.x
        var top = start.y
        var right = start.right
        var bottom = start.bottom
        when (handle) {
            Handle.LEFT -> left = bounded(grid.nearestColumnLine(event.x), maxOf(0, right - maxX), right - resizeLimits.minX) ?: return

            Handle.RIGHT ->
                right =
                    bounded(grid.nearestColumnLine(event.x), left + resizeLimits.minX, minOf(grid.columns, left + maxX)) ?: return

            Handle.TOP -> top = bounded(grid.nearestRowLine(event.y), maxOf(0, bottom - maxY), bottom - resizeLimits.minY) ?: return

            Handle.BOTTOM -> bottom = bounded(grid.nearestRowLine(event.y), top + resizeLimits.minY, minOf(grid.rows, top + maxY)) ?: return
        }
        val candidate = CellRect(left, top, right - left, bottom - top)
        if (candidate == resizeRect) return
        // A size with no room for the others is skipped; the frame stays at the last good one.
        val solution = GridLayoutEngine.place(base, key, candidate, squeezeLimits = host::limitsOf) ?: return
        resizeRect = candidate
        resizeSolution = solution
        host.preview(solution, except = null)
        haptic(HapticFeedbackConstants.CLOCK_TICK)
        grid.invalidate()
    }

    private fun bounded(value: Int, min: Int, max: Int): Int? = if (min > max) null else value.coerceIn(min, max)

    private fun endResize() {
        resizeSolution?.let(host::commit)
        resizeBase = null
        resizeRect = null
        resizeSolution = null
        gesture = Gesture.NONE
        host.onInteraction(false)
        grid.invalidate()
    }

    // ── Handles ────────────────────────────────────────────────────

    /** Sides of the selected block it can be resized along, with the centre of each handle. */
    private fun handles(): List<Pair<Handle, Pair<Float, Float>>> {
        val key = selectedKey ?: return emptyList()
        val item = host.layout.item(key) ?: return emptyList()
        val view = host.viewFor(key)
        val bounds = visualBounds(view, item.rect)
        val limits = host.limitsOf(item)
        return buildList {
            if (limits.resizableX) {
                add(Handle.LEFT to (bounds.left to bounds.centerY()))
                add(Handle.RIGHT to (bounds.right to bounds.centerY()))
            }
            if (limits.resizableY) {
                add(Handle.TOP to (bounds.centerX() to bounds.top))
                add(Handle.BOTTOM to (bounds.centerX() to bounds.bottom))
            }
        }
    }

    private fun handleAt(x: Float, y: Float): Handle? = handles()
        .map { (handle, center) -> handle to hypot(x - center.first, y - center.second) }
        .filter { it.second <= handleTouch }
        .minByOrNull { it.second }
        ?.first

    /** Where a block is drawn right now, following its slide; [rect] when it has no view. */
    private fun visualBounds(view: View?, rect: CellRect): RectF {
        val bounds = if (view != null && view.visibility == View.VISIBLE) {
            RectF(
                view.left + view.translationX,
                view.top + view.translationY,
                view.right + view.translationX,
                view.bottom + view.translationY
            )
        } else {
            grid.boundsOf(rect)
        }
        bounds.inset(cellInset, cellInset)
        return bounds
    }

    // ── Drawing ────────────────────────────────────────────────────

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val targetFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val targetStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = TARGET_STROKE_DP * density
    }
    private val frameStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = FRAME_STROKE_DP * density
    }
    private val handleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, LABEL_TEXT_SP, resources.displayMetrics)
    }
    private val labelBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dash = DashPathEffect(floatArrayOf(DASH_DP * density, DASH_DP * density), 0f)
    private val scratch = RectF()

    override fun drawUnder(canvas: Canvas) {
        if (!host.isEditing) return
        val colors = host.palette
        cellPaint.color = FocusColors.withAlpha(colors.textPrimary, CELL_ALPHA)
        // Only free room is tiled; a block lifted by the finger leaves its cells free.
        val occupied = host.layout.items.filter { it.key != dragKey }.map { it.rect }
        for (row in 0 until grid.rows) {
            for (column in 0 until grid.columns) {
                if (occupied.any { it.contains(column, row) }) continue
                grid.boundsOf(CellRect(column, row, 1, 1), scratch)
                scratch.inset(cellInset, cellInset)
                canvas.drawRoundRect(scratch, corner, corner, cellPaint)
            }
        }
    }

    override fun drawOver(canvas: Canvas) {
        if (!host.isEditing) return
        val colors = host.palette
        drawPlaceholders(canvas, colors)
        drawDropTarget(canvas, colors)
        drawSelection(canvas, colors)
        // Blocks sliding into place are moved by their render nodes, which does not redraw
        // the grid; keep drawing while anything is in motion so the frame follows them.
        if (isAnythingMoving()) grid.postInvalidateOnAnimation()
    }

    private fun drawPlaceholders(canvas: Canvas, colors: FocusPalette) {
        labelPaint.color = colors.textSecondary
        host.layout.items.forEach { item ->
            val text = host.placeholderFor(item.key) ?: return@forEach
            val bounds = visualBounds(host.viewFor(item.key), item.rect)
            canvas.drawText(text, bounds.centerX(), bounds.centerY() - (labelPaint.ascent() + labelPaint.descent()) / 2f, labelPaint)
        }
    }

    private fun drawDropTarget(canvas: Canvas, colors: FocusPalette) {
        val target = pendingTarget?.takeIf { gesture == Gesture.DRAG } ?: return
        grid.boundsOf(target, scratch)
        scratch.inset(cellInset, cellInset)
        val fits = dragSolution != null || target != dragTarget
        targetFill.color = FocusColors.withAlpha(colors.accent, if (fits) TARGET_FILL_ALPHA else TARGET_FILL_ALPHA / 2f)
        targetStroke.color = if (fits) colors.accent else colors.textSecondary
        targetStroke.pathEffect = if (fits) null else dash
        canvas.drawRoundRect(scratch, corner, corner, targetFill)
        canvas.drawRoundRect(scratch, corner, corner, targetStroke)
    }

    private fun drawSelection(canvas: Canvas, colors: FocusPalette) {
        if (gesture == Gesture.DRAG) return
        val key = selectedKey ?: return
        val item = host.layout.item(key) ?: return
        val bounds = visualBounds(host.viewFor(key), item.rect)
        frameStroke.color = colors.accent
        canvas.drawRoundRect(bounds, corner, corner, frameStroke)

        handles().forEach { (_, center) ->
            handleFill.color = colors.background
            canvas.drawCircle(center.first, center.second, handleRadius, handleFill)
            canvas.drawCircle(center.first, center.second, handleRadius, frameStroke)
        }

        if (gesture == Gesture.RESIZE) {
            val text = resources.getString(R.string.widget_size_format, item.rect.spanX, item.rect.spanY)
            val padding = LABEL_PADDING_DP * density
            val width = labelPaint.measureText(text) + 2 * padding
            val height = labelPaint.textSize + 2 * padding
            scratch.set(
                bounds.centerX() - width / 2f, bounds.centerY() - height / 2f, bounds.centerX() + width / 2f,
                bounds.centerY() + height / 2f
            )
            labelBackground.color = colors.accent
            canvas.drawRoundRect(scratch, height / 2f, height / 2f, labelBackground)
            labelPaint.color = colors.onAccent
            canvas.drawText(text, scratch.centerX(), scratch.centerY() - (labelPaint.ascent() + labelPaint.descent()) / 2f, labelPaint)
        }
    }

    private fun isAnythingMoving(): Boolean {
        for (i in 0 until grid.childCount) {
            val child = grid.getChildAt(i)
            if (child.translationX != 0f || child.translationY != 0f || child.scaleX != 1f) return true
        }
        return false
    }

    // ── Feedback ───────────────────────────────────────────────────

    private fun haptic(feedback: Int) {
        grid.performHapticFeedback(feedback)
    }

    private fun confirmFeedback(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY

    private fun rejectFeedback(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS

    private companion object {
        const val LIFT_SCALE = 1.04f
        const val LIFT_ALPHA = 0.92f
        const val LIFT_ELEVATION_DP = 12f
        const val CELL_ALPHA = 0.05f
        const val TARGET_FILL_ALPHA = 0.16f
        const val TARGET_STROKE_DP = 1.5f
        const val FRAME_STROKE_DP = 2f
        const val DASH_DP = 6f
        const val LABEL_TEXT_SP = 13f
        const val LABEL_PADDING_DP = 6f
    }
}
