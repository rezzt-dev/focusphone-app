package app.focusphone.ui.widget

import app.focusphone.domain.model.SpanLimits
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Pure size maths between a widget provider and the cells of the home grid.
 *
 * Providers describe their size in dp (minimum, resize bounds) and, on Android 12+, in
 * cells of a typical launcher grid. FocusPhone turns both into spans of its own grid, from
 * the real size of its cells rather than the platform's `70 × n − 30` formula, because its
 * cells are usually larger than that formula assumes.
 */
object WidgetSizing {

    /**
     * What a provider declares about its size, in dp and cells. Plain values so the maths
     * can be tested without an `AppWidgetProviderInfo`.
     */
    data class ProviderSize(
        val minWidthDp: Float,
        val minHeightDp: Float,
        val minResizeWidthDp: Float = 0f,
        val minResizeHeightDp: Float = 0f,
        val maxResizeWidthDp: Float = 0f,
        val maxResizeHeightDp: Float = 0f,
        val targetCellWidth: Int = 0,
        val targetCellHeight: Int = 0,
        val resizableX: Boolean = true,
        val resizableY: Boolean = true
    )

    /**
     * A provider's minimum may exceed a whole number of cells by a few dp of rounding; that
     * slack is forgiven instead of costing a full extra cell.
     */
    private const val FIT_TOLERANCE_DP = 8f

    /** Number of home cells of [cellDp] needed to show [sizeDp] without cropping. */
    fun cellsFor(sizeDp: Float, cellDp: Float): Int {
        if (cellDp <= 0f) return 1
        return ceil((sizeDp - FIT_TOLERANCE_DP) / cellDp).toInt().coerceAtLeast(1)
    }

    /** Whole home cells of [cellDp] that fit inside [sizeDp]. */
    private fun cellsWithin(sizeDp: Float, cellDp: Float): Int {
        if (cellDp <= 0f) return 1
        return floor((sizeDp + FIT_TOLERANCE_DP) / cellDp).toInt().coerceAtLeast(1)
    }

    /**
     * The span a widget gets when it is added to a grid of [columns] × [rows] cells of
     * [cellWidthDp] × [cellHeightDp]: its declared cells (Android 12+) or, for older
     * providers, the cells its minimum size needs.
     */
    fun defaultSpan(size: ProviderSize, cellWidthDp: Float, cellHeightDp: Float, columns: Int, rows: Int): Pair<Int, Int> {
        val spanX = if (size.targetCellWidth > 0) size.targetCellWidth else cellsFor(size.minWidthDp, cellWidthDp)
        val spanY = if (size.targetCellHeight > 0) size.targetCellHeight else cellsFor(size.minHeightDp, cellHeightDp)
        return spanX.coerceIn(1, columns) to spanY.coerceIn(1, rows)
    }

    /**
     * How far the widget may be resized on the grid, following its `resizeMode` and resize
     * bounds the way a stock launcher does: an axis it cannot be resized on keeps its default
     * span, and the default span always lies within the limits.
     */
    fun limits(size: ProviderSize, cellWidthDp: Float, cellHeightDp: Float, columns: Int, rows: Int): SpanLimits {
        val (spanX, spanY) = defaultSpan(size, cellWidthDp, cellHeightDp, columns, rows)
        val (minX, maxX) = axisLimits(
            resizable = size.resizableX,
            default = spanX,
            minDp = smallest(size.minResizeWidthDp, size.minWidthDp),
            maxDp = size.maxResizeWidthDp,
            cellDp = cellWidthDp,
            cells = columns
        )
        val (minY, maxY) = axisLimits(
            resizable = size.resizableY,
            default = spanY,
            minDp = smallest(size.minResizeHeightDp, size.minHeightDp),
            maxDp = size.maxResizeHeightDp,
            cellDp = cellHeightDp,
            cells = rows
        )
        return SpanLimits(minX, minY, maxX, maxY)
    }

    private fun smallest(resizeDp: Float, minDp: Float): Float = if (resizeDp > 0f) minOf(resizeDp, minDp) else minDp

    private fun axisLimits(resizable: Boolean, default: Int, minDp: Float, maxDp: Float, cellDp: Float, cells: Int): Pair<Int, Int> {
        if (!resizable) return default to default
        val min = cellsFor(minDp, cellDp).coerceIn(1, default)
        val max = if (maxDp > 0f) cellsWithin(maxDp, cellDp).coerceIn(default, cells) else cells
        return min to max
    }
}
