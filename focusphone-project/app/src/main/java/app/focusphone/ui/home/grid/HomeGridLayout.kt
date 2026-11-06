package app.focusphone.ui.home.grid

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import app.focusphone.R
import app.focusphone.domain.model.CellRect
import kotlin.math.roundToInt

/**
 * The home screen's grid: [columns] × [rows] cells that stretch to fill the view, each child
 * occupying the cells its [LayoutParams] name — the role `CellLayout` plays in a stock
 * launcher. It only lays out and draws; what goes where is decided by
 * [HomeGridController], and touches in edit mode are handled by [HomeGridEditor].
 */
class HomeGridLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    /** Cells a child occupies. */
    class LayoutParams(var rect: CellRect) : ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)

    /** Draws edit-mode decorations below and above the blocks. */
    interface Overlay {
        fun drawUnder(canvas: Canvas)
        fun drawOver(canvas: Canvas)
    }

    /** Receives every touch on the grid before its blocks do. */
    interface TouchHandler {
        fun onInterceptTouchEvent(event: MotionEvent): Boolean
        fun onTouchEvent(event: MotionEvent): Boolean
    }

    var columns: Int = 4
        set(value) {
            if (field == value) return
            field = value
            requestLayout()
        }

    var rows: Int = 8
        set(value) {
            if (field == value) return
            field = value
            requestLayout()
        }

    var overlay: Overlay? = null
        set(value) {
            field = value
            invalidate()
        }

    var touchHandler: TouchHandler? = null

    /** Width of one cell in px. */
    val cellWidth: Float get() = (width - paddingLeft - paddingRight).toFloat() / columns

    /** Height of one cell in px. */
    val cellHeight: Float get() = (height - paddingTop - paddingBottom).toFloat() / rows

    /** Called after every layout pass, e.g. to move a floating toolbar with its block. */
    var onLaidOut: (() -> Unit)? = null

    private val animationDurationMs = resources.getInteger(R.integer.fp_grid_reorder_duration).toLong()
    private val reorderInterpolator = DecelerateInterpolator()

    /** On-screen position of each child before a change, for [animateChanges]. */
    private val previousPositions = mutableMapOf<View, Pair<Float, Float>>()
    private var animateNextLayout = false

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
    }

    // ── Layout params ──────────────────────────────────────────────

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean = p is LayoutParams

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams = LayoutParams(CellRect(0, 0, 1, 1))

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams = LayoutParams(CellRect(0, 0, 1, 1))

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams = LayoutParams(CellRect(0, 0, 1, 1))

    override fun shouldDelayChildPressedState(): Boolean = false

    /** Moves [child] to [rect]; nothing happens on screen until the next layout. */
    fun setCells(child: View, rect: CellRect) {
        val params = child.layoutParams as? LayoutParams
        if (params != null && params.rect == rect) return
        if (params != null) params.rect = rect else child.layoutParams = LayoutParams(rect)
        child.requestLayout()
        requestLayout()
    }

    fun cellsOf(child: View): CellRect? = (child.layoutParams as? LayoutParams)?.rect

    // ── Measure & layout ───────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
        val cellW = (width - paddingLeft - paddingRight).toFloat() / columns
        val cellH = (height - paddingTop - paddingBottom).toFloat() / rows
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            val rect = cellsOf(child) ?: continue
            val childW = edge(rect.right, cellW) - edge(rect.x, cellW)
            val childH = edge(rect.bottom, cellH) - edge(rect.y, cellH)
            child.measure(
                MeasureSpec.makeMeasureSpec(childW.coerceAtLeast(0), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(childH.coerceAtLeast(0), MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val cellW = cellWidth
        val cellH = cellHeight
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            val rect = cellsOf(child) ?: continue
            child.layout(
                paddingLeft + edge(rect.x, cellW),
                paddingTop + edge(rect.y, cellH),
                paddingLeft + edge(rect.right, cellW),
                paddingTop + edge(rect.bottom, cellH)
            )
        }
        if (animateNextLayout) runPendingAnimations()
        onLaidOut?.invoke()
    }

    /** Pixel offset of cell line [index]; rounding per line keeps neighbours flush. */
    private fun edge(index: Int, cell: Float): Int = (index * cell).roundToInt()

    // ── Animation ──────────────────────────────────────────────────

    /**
     * Remembers where every child is drawn now, so the next layout slides them from there
     * to their new cells instead of jumping. Children in [except] (the block under the
     * finger) are left alone.
     */
    fun animateChanges(except: View? = null) {
        previousPositions.clear()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child === except || child.visibility == GONE) continue
            previousPositions[child] = (child.left + child.translationX) to (child.top + child.translationY)
        }
        animateNextLayout = true
        // Even when no block changes cell (a block dropped where it was), the pass must run
        // so the blocks settle from where they are drawn.
        requestLayout()
    }

    private fun runPendingAnimations() {
        animateNextLayout = false
        previousPositions.forEach { (child, from) ->
            val dx = from.first - child.left
            val dy = from.second - child.top
            if (dx == 0f && dy == 0f) return@forEach
            // Only the translation is restarted: a lift or drop scaling the same block keeps running.
            child.translationX = dx
            child.translationY = dy
            child.animate()
                .translationX(0f)
                .translationY(0f)
                .setDuration(animationDurationMs)
                .setInterpolator(reorderInterpolator)
                .start()
        }
        previousPositions.clear()
    }

    // ── Geometry ───────────────────────────────────────────────────

    /** Bounds of [rect] in this view's coordinates. */
    fun boundsOf(rect: CellRect, out: RectF = RectF()): RectF {
        val cellW = cellWidth
        val cellH = cellHeight
        out.set(
            (paddingLeft + edge(rect.x, cellW)).toFloat(),
            (paddingTop + edge(rect.y, cellH)).toFloat(),
            (paddingLeft + edge(rect.right, cellW)).toFloat(),
            (paddingTop + edge(rect.bottom, cellH)).toFloat()
        )
        return out
    }

    /** Column under [x], clamped to the grid. */
    fun columnAt(x: Float): Int = ((x - paddingLeft) / cellWidth).toInt().coerceIn(0, columns - 1)

    /** Row under [y], clamped to the grid. */
    fun rowAt(y: Float): Int = ((y - paddingTop) / cellHeight).toInt().coerceIn(0, rows - 1)

    /** Nearest grid line (0..[columns]) to [x]. */
    fun nearestColumnLine(x: Float): Int = ((x - paddingLeft) / cellWidth).roundToInt().coerceIn(0, columns)

    /** Nearest grid line (0..[rows]) to [y]. */
    fun nearestRowLine(y: Float): Int = ((y - paddingTop) / cellHeight).roundToInt().coerceIn(0, rows)

    // ── Drawing ────────────────────────────────────────────────────

    override fun dispatchDraw(canvas: Canvas) {
        overlay?.drawUnder(canvas)
        super.dispatchDraw(canvas)
        overlay?.drawOver(canvas)
    }

    // ── Touch ──────────────────────────────────────────────────────

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = touchHandler?.onInterceptTouchEvent(ev) ?: false

    // The handler calls performClick() for taps (see HomeGridEditor).
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = touchHandler?.onTouchEvent(event) ?: super.onTouchEvent(event)
}
