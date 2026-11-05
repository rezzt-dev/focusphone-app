package app.focusphone.ui.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSizingTest {

    @Test
    fun `cells follow the platform launcher formula`() {
        assertEquals(1, WidgetSizing.cellsFor(40f))
        assertEquals(2, WidgetSizing.cellsFor(110f))
        assertEquals(4, WidgetSizing.cellsFor(250f))
        // Just above a step needs one more cell.
        assertEquals(3, WidgetSizing.cellsFor(111f))
        assertEquals(1, WidgetSizing.cellsFor(0f))
    }

    @Test
    fun `target cells win over the minimum size`() {
        assertEquals(4 to 2, WidgetSizing.span(40f, 40f, targetCellWidth = 4, targetCellHeight = 2))
        assertEquals(4 to 2, WidgetSizing.span(250f, 110f, targetCellWidth = 0, targetCellHeight = 0))
    }

    @Test
    fun `cell size follows a four column grid within sane bounds`() {
        assertEquals(95f, WidgetSizing.cellDp(380f), 0.01f)
        assertEquals(72f, WidgetSizing.cellDp(200f), 0.01f)
        assertEquals(120f, WidgetSizing.cellDp(900f), 0.01f)
    }

    @Test
    fun `default height uses rows at the launcher cell size`() {
        // Declared rows win: 2 rows of 95dp.
        assertEquals(190, WidgetSizing.defaultHeightDp(minHeightDp = 40f, targetCellHeight = 2, cellDp = 95f, windowHeightDp = 800f))
        // Without declared rows they come from the minimum height (110dp -> 2 rows).
        assertEquals(190, WidgetSizing.defaultHeightDp(minHeightDp = 110f, targetCellHeight = 0, cellDp = 95f, windowHeightDp = 800f))
        // Never below the provider's own minimum.
        assertEquals(250, WidgetSizing.defaultHeightDp(minHeightDp = 250f, targetCellHeight = 1, cellDp = 95f, windowHeightDp = 800f))
    }

    @Test
    fun `default height is clamped into the slot range`() {
        val max = WidgetSizing.maxHeightDp(800f)
        assertEquals(max, WidgetSizing.defaultHeightDp(2000f, 0, 95f, 800f))
        assertTrue(max <= 800 * WidgetSizing.MAX_WINDOW_FRACTION)
        assertEquals(WidgetSizing.MIN_HEIGHT_DP, WidgetSizing.defaultHeightDp(10f, 0, 40f, 800f))
    }

    @Test
    fun `max height sits on a slider tick and never below the minimum`() {
        val max = WidgetSizing.maxHeightDp(817f)
        assertEquals(0, (max - WidgetSizing.MIN_HEIGHT_DP) % WidgetSizing.HEIGHT_STEP_DP)
        assertEquals(WidgetSizing.MIN_HEIGHT_DP, WidgetSizing.maxHeightDp(20f))
    }

    @Test
    fun `snap rounds to the nearest slider step`() {
        assertEquals(64, WidgetSizing.snap(64))
        assertEquals(64, WidgetSizing.snap(67))
        assertEquals(72, WidgetSizing.snap(68))
        assertEquals(112, WidgetSizing.snap(110))
        assertEquals(192, WidgetSizing.snap(190))
    }
}
