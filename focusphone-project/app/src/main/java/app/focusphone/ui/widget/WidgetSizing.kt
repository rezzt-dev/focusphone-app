package app.focusphone.ui.widget

import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Pure size maths for the home widget slot.
 *
 * Widget providers describe their size in grid cells (Android 12+) or in dp following the
 * classic launcher formula `70 × n − 30`. FocusPhone has no grid: the slot is always as wide
 * as the screen allows, and only its height is chosen — by default from the provider, then
 * by the user within [MIN_HEIGHT_DP] and a fraction of the window.
 */
object WidgetSizing {

    /** Smallest height the slot accepts; a single row of text still fits. */
    const val MIN_HEIGHT_DP = 64

    /** Step of the height slider, in dp. */
    const val HEIGHT_STEP_DP = 8

    /** Share of the window height a widget may take, so the apps always stay reachable. */
    const val MAX_WINDOW_FRACTION = 0.6f

    private const val CELL_DP = 70
    private const val CELL_GAP_DP = 30

    private const val GRID_COLUMNS = 4
    private const val MIN_CELL_DP = 72f
    private const val MAX_CELL_DP = 120f

    /** Number of grid cells needed for [sizeDp], following the platform launcher formula. */
    fun cellsFor(sizeDp: Float): Int =
        ceil((sizeDp + CELL_GAP_DP) / CELL_DP).toInt().coerceAtLeast(1)

    /**
     * The span shown in the picker ("4 × 2"). [targetCellWidth] and [targetCellHeight] come
     * from Android 12+ providers and win when set; older ones only declare a minimum size.
     */
    fun span(minWidthDp: Float, minHeightDp: Float, targetCellWidth: Int, targetCellHeight: Int): Pair<Int, Int> =
        Pair(
            if (targetCellWidth > 0) targetCellWidth else cellsFor(minWidthDp),
            if (targetCellHeight > 0) targetCellHeight else cellsFor(minHeightDp)
        )

    /** Largest height in dp the slot may take inside a window [windowHeightDp] tall. */
    fun maxHeightDp(windowHeightDp: Float): Int =
        snapDown((windowHeightDp * MAX_WINDOW_FRACTION).toInt()).coerceAtLeast(MIN_HEIGHT_DP)

    /**
     * Size in dp of one cell of a typical launcher grid for a slot [slotWidthDp] wide.
     *
     * Providers design their layouts for real home-screen cells (a phone grid has about four
     * columns), not for the platform's minimum formula, which is far smaller: rendering at
     * the minimum crops or cramps many widgets.
     */
    fun cellDp(slotWidthDp: Float): Float =
        (slotWidthDp / GRID_COLUMNS).coerceIn(MIN_CELL_DP, MAX_CELL_DP)

    /**
     * Height in dp the provider asks for by default: its rows (declared on Android 12+, or
     * derived from its minimum height) at the size of a real launcher cell, never below its
     * minimum height, clamped to what the slot allows.
     */
    fun defaultHeightDp(minHeightDp: Float, targetCellHeight: Int, cellDp: Float, windowHeightDp: Float): Int {
        val rows = if (targetCellHeight > 0) targetCellHeight else cellsFor(minHeightDp)
        val requested = maxOf(rows * cellDp, minHeightDp)
        return clampHeightDp(requested.roundToInt(), windowHeightDp)
    }

    /** Clamps [heightDp] into the range the slot allows inside a window [windowHeightDp] tall. */
    fun clampHeightDp(heightDp: Int, windowHeightDp: Float): Int =
        heightDp.coerceIn(MIN_HEIGHT_DP, maxHeightDp(windowHeightDp))

    /** Rounds [heightDp] to the nearest slider step so stored values always sit on a tick. */
    fun snap(heightDp: Int): Int {
        val offset = heightDp - MIN_HEIGHT_DP
        return MIN_HEIGHT_DP + Math.floorDiv(offset + HEIGHT_STEP_DP / 2, HEIGHT_STEP_DP) * HEIGHT_STEP_DP
    }

    private fun snapDown(heightDp: Int): Int = heightDp - Math.floorMod(heightDp - MIN_HEIGHT_DP, HEIGHT_STEP_DP)
}
