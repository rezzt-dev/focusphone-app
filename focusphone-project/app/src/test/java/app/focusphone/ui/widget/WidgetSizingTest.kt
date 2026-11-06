package app.focusphone.ui.widget

import app.focusphone.domain.model.SpanLimits
import app.focusphone.ui.widget.WidgetSizing.ProviderSize
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSizingTest {

    /** A 4 × 8 grid of 100 dp cells, about what a phone gets. */
    private val cell = 100f
    private val columns = 4
    private val rows = 8

    private fun span(size: ProviderSize) = WidgetSizing.defaultSpan(size, cell, cell, columns, rows)
    private fun limits(size: ProviderSize) = WidgetSizing.limits(size, cell, cell, columns, rows)

    @Test
    fun `cells come from the real cell size with a little tolerance`() {
        assertEquals(1, WidgetSizing.cellsFor(40f, cell))
        assertEquals(1, WidgetSizing.cellsFor(106f, cell))
        assertEquals(2, WidgetSizing.cellsFor(110f, cell))
        assertEquals(3, WidgetSizing.cellsFor(250f, cell))
        assertEquals(1, WidgetSizing.cellsFor(0f, cell))
    }

    @Test
    fun `declared cells win over the minimum size`() {
        assertEquals(4 to 1, span(ProviderSize(minWidthDp = 250f, minHeightDp = 40f, targetCellWidth = 4, targetCellHeight = 1)))
        assertEquals(3 to 2, span(ProviderSize(minWidthDp = 250f, minHeightDp = 180f)))
    }

    @Test
    fun `the default span never exceeds the grid`() {
        assertEquals(4 to 8, span(ProviderSize(minWidthDp = 900f, minHeightDp = 2000f)))
        assertEquals(4 to 1, span(ProviderSize(minWidthDp = 40f, minHeightDp = 40f, targetCellWidth = 6, targetCellHeight = 1)))
    }

    @Test
    fun `a resizable widget goes from its resize minimum to the whole grid`() {
        val size = ProviderSize(minWidthDp = 250f, minHeightDp = 110f, minResizeWidthDp = 180f, minResizeHeightDp = 40f)
        assertEquals(SpanLimits(minX = 2, minY = 1, maxX = 4, maxY = 8), limits(size))
    }

    @Test
    fun `a maximum resize size caps the span but never below the default`() {
        val size = ProviderSize(minWidthDp = 180f, minHeightDp = 40f, maxResizeWidthDp = 300f, maxResizeHeightDp = 60f)
        val result = limits(size)
        assertEquals(3, result.maxX)
        assertEquals(1, result.maxY)
    }

    @Test
    fun `an axis that cannot be resized keeps the default span`() {
        val size = ProviderSize(minWidthDp = 250f, minHeightDp = 110f, resizableX = false, resizableY = true)
        val result = limits(size)
        assertEquals(3, result.minX)
        assertEquals(3, result.maxX)
        assertEquals(false, result.resizableX)
        assertEquals(true, result.resizableY)
    }
}
