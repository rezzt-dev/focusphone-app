package app.focusphone.domain.util

import app.focusphone.domain.model.CellRect
import app.focusphone.domain.model.HomeGrid
import app.focusphone.domain.model.HomeGridItem
import app.focusphone.domain.model.HomeGridItem.Type
import app.focusphone.domain.model.SpanLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridLayoutEngineTest {

    private fun clock(x: Int, y: Int, w: Int, h: Int) = HomeGridItem(Type.CLOCK, CellRect(x, y, w, h))
    private fun apps(x: Int, y: Int, w: Int, h: Int) = HomeGridItem(Type.APPS, CellRect(x, y, w, h))
    private fun widget(id: Int, x: Int, y: Int, w: Int, h: Int) = HomeGridItem(Type.WIDGET, CellRect(x, y, w, h), id, "pkg/.W$id")

    private val anyLimits: (HomeGridItem) -> SpanLimits = { GridLayoutEngine.builtInLimits(it.type) }

    /** The default home: clock on two rows, the favourite list below it. */
    private val home = HomeGrid(4, 8, listOf(clock(0, 0, 4, 2), apps(0, 2, 4, 6)))

    private fun HomeGrid.rect(key: String): CellRect = item(key)!!.rect

    // ── findVacant ─────────────────────────────────────────────────

    @Test
    fun `findVacant returns the first free room in reading order`() {
        val occupied = listOf(CellRect(0, 0, 4, 2), CellRect(0, 2, 2, 1))
        assertEquals(CellRect(2, 2, 2, 1), GridLayoutEngine.findVacant(4, 8, occupied, 2, 1))
    }

    @Test
    fun `findVacant prefers the room nearest to a hint`() {
        val spot = GridLayoutEngine.findVacant(4, 8, emptyList(), 2, 2, near = CellRect(2, 5, 2, 2))
        assertEquals(CellRect(2, 5, 2, 2), spot)
    }

    @Test
    fun `findVacant is null when nothing fits`() {
        assertNull(GridLayoutEngine.findVacant(4, 8, home.items.map { it.rect }, 1, 1))
        assertNull(GridLayoutEngine.findVacant(4, 8, emptyList(), 5, 1))
    }

    // ── place: move ────────────────────────────────────────────────

    @Test
    fun `moving into free room touches nothing else`() {
        val grid = HomeGrid(4, 8, listOf(clock(0, 0, 4, 2), apps(0, 4, 4, 4)))
        val moved = GridLayoutEngine.place(grid, HomeGridItem.CLOCK_KEY, CellRect(0, 2, 4, 2))!!

        assertEquals(CellRect(0, 2, 4, 2), moved.rect(HomeGridItem.CLOCK_KEY))
        assertEquals(CellRect(0, 4, 4, 4), moved.rect(HomeGridItem.APPS_KEY))
    }

    @Test
    fun `dragging the clock below the list swaps them`() {
        val moved = GridLayoutEngine.place(home, HomeGridItem.CLOCK_KEY, CellRect(0, 6, 4, 2))!!

        assertEquals(CellRect(0, 6, 4, 2), moved.rect(HomeGridItem.CLOCK_KEY))
        assertEquals(CellRect(0, 0, 4, 6), moved.rect(HomeGridItem.APPS_KEY))
        assertTrue(GridLayoutEngine.isValid(moved))
    }

    @Test
    fun `blocks in the way are pushed down like a stack`() {
        val grid = HomeGrid(
            4,
            8,
            listOf(widget(1, 0, 0, 4, 1), widget(2, 0, 1, 4, 1), apps(0, 2, 4, 4))
        )
        // Drop widget 2 on top of widget 1: widget 1 makes way.
        val moved = GridLayoutEngine.place(grid, "widget:2", CellRect(0, 0, 4, 1))!!

        assertEquals(CellRect(0, 0, 4, 1), moved.rect("widget:2"))
        assertEquals(CellRect(0, 1, 4, 1), moved.rect("widget:1"))
        assertTrue(GridLayoutEngine.isValid(moved))
    }

    @Test
    fun `a move with no room for the others is refused`() {
        val full = HomeGrid(2, 2, listOf(widget(1, 0, 0, 2, 1), widget(2, 0, 1, 2, 1)))
        assertNull(GridLayoutEngine.place(full, "widget:1", CellRect(0, 0, 2, 2)))
    }

    @Test
    fun `a target outside the grid is refused`() {
        assertNull(GridLayoutEngine.place(home, HomeGridItem.CLOCK_KEY, CellRect(1, 0, 4, 2)))
        assertNull(GridLayoutEngine.place(home, HomeGridItem.CLOCK_KEY, CellRect(0, 7, 4, 2)))
    }

    // ── place: resize ──────────────────────────────────────────────

    @Test
    fun `growing a block pushes a neighbour that has room to move`() {
        val grid = HomeGrid(4, 8, listOf(clock(0, 0, 4, 2), widget(1, 0, 2, 4, 1), apps(0, 4, 4, 4)))
        val grown = GridLayoutEngine.place(grid, HomeGridItem.CLOCK_KEY, CellRect(0, 0, 4, 3))!!

        assertEquals(CellRect(0, 0, 4, 3), grown.rect(HomeGridItem.CLOCK_KEY))
        assertEquals(CellRect(0, 3, 4, 1), grown.rect("widget:1"))
        assertEquals(CellRect(0, 4, 4, 4), grown.rect(HomeGridItem.APPS_KEY))
    }

    @Test
    fun `growing into a block that cannot move squeezes it when resizing`() {
        val grid = HomeGrid(4, 8, listOf(clock(0, 0, 4, 2), apps(0, 3, 4, 5)))
        val target = CellRect(0, 0, 4, 4)

        // A move never shrinks anyone...
        assertNull(GridLayoutEngine.place(grid, HomeGridItem.CLOCK_KEY, target))
        // ...a resize may, down to the other block's minimum.
        val grown = GridLayoutEngine.place(grid, HomeGridItem.CLOCK_KEY, target, squeezeLimits = anyLimits)!!
        assertEquals(target, grown.rect(HomeGridItem.CLOCK_KEY))
        assertEquals(CellRect(0, 4, 4, 4), grown.rect(HomeGridItem.APPS_KEY))
        assertTrue(GridLayoutEngine.isValid(grown))

        // The list keeps at least two rows.
        assertNull(GridLayoutEngine.place(grid, HomeGridItem.CLOCK_KEY, CellRect(0, 0, 4, 7), squeezeLimits = anyLimits))
    }

    @Test
    fun `shrinking leaves the freed cells empty`() {
        val shrunk = GridLayoutEngine.place(home, HomeGridItem.APPS_KEY, CellRect(0, 4, 4, 4))!!
        assertEquals(CellRect(0, 4, 4, 4), shrunk.rect(HomeGridItem.APPS_KEY))
        assertEquals(8, GridLayoutEngine.freeCells(shrunk))
    }

    // ── insert ─────────────────────────────────────────────────────

    @Test
    fun `a widget goes into free room at its own span`() {
        val grid = HomeGrid(4, 8, listOf(clock(0, 0, 4, 2), apps(0, 4, 4, 4)))
        val added = GridLayoutEngine.insert(grid, widget(7, 0, 0, 4, 2), anyLimits)!!

        assertEquals(CellRect(0, 2, 4, 2), added.rect("widget:7"))
    }

    @Test
    fun `a full grid makes room by shrinking the list from its top`() {
        val added = GridLayoutEngine.insert(home, widget(7, 0, 0, 4, 2), anyLimits)!!

        // Right under the clock, where the list used to start.
        assertEquals(CellRect(0, 2, 4, 2), added.rect("widget:7"))
        assertEquals(CellRect(0, 4, 4, 4), added.rect(HomeGridItem.APPS_KEY))
        assertTrue(GridLayoutEngine.isValid(added))
    }

    @Test
    fun `the list gives up room before a taller widget does`() {
        val grid = HomeGrid(4, 9, listOf(clock(0, 0, 4, 2), apps(0, 2, 4, 4), widget(1, 0, 6, 4, 3)))
        val added = GridLayoutEngine.insert(grid, widget(2, 0, 0, 2, 2), anyLimits)!!

        assertEquals(CellRect(0, 6, 4, 3), added.rect("widget:1"))
        assertEquals(CellRect(0, 4, 4, 2), added.rect(HomeGridItem.APPS_KEY))
        assertEquals(CellRect(0, 2, 2, 2), added.rect("widget:2"))
    }

    @Test
    fun `insert never shrinks a block below its minimum`() {
        val tight = HomeGrid(4, 4, listOf(clock(0, 0, 4, 1), apps(0, 1, 4, 3)))
        val added = GridLayoutEngine.insert(tight, widget(7, 0, 0, 4, 3), anyLimits)!!

        // Only one row could be freed (the list keeps two), so the widget takes one row.
        assertEquals(1, added.rect("widget:7").spanY)
        assertEquals(2, added.rect(HomeGridItem.APPS_KEY).spanY)
    }

    @Test
    fun `insert fails when even the minimum size has no room`() {
        val packed = HomeGrid(4, 3, listOf(clock(0, 0, 4, 1), apps(0, 1, 4, 2)))
        val limits: (HomeGridItem) -> SpanLimits = { item ->
            if (item.type == Type.WIDGET) SpanLimits(4, 1, 4, 1) else GridLayoutEngine.builtInLimits(item.type)
        }
        assertNull(GridLayoutEngine.insert(packed, widget(7, 0, 0, 4, 1), limits))
    }

    // ── resizeGrid ─────────────────────────────────────────────────

    @Test
    fun `more columns keep full-width blocks full width`() {
        val wider = GridLayoutEngine.resizeGrid(home, 5, 8, anyLimits)!!

        assertEquals(CellRect(0, 0, 5, 2), wider.rect(HomeGridItem.CLOCK_KEY))
        assertEquals(CellRect(0, 2, 5, 6), wider.rect(HomeGridItem.APPS_KEY))
    }

    @Test
    fun `fewer rows scale blocks without overlaps`() {
        val shorter = GridLayoutEngine.resizeGrid(home, 4, 6, anyLimits)!!

        assertTrue(GridLayoutEngine.isValid(shorter))
        assertEquals(6, shorter.items.sumOf { it.rect.spanY })
    }

    @Test
    fun `a grid too small for a block's minimum is refused`() {
        val limits: (HomeGridItem) -> SpanLimits = { item ->
            if (item.type == Type.WIDGET) SpanLimits(4, 1, 4, 4) else GridLayoutEngine.builtInLimits(item.type)
        }
        val grid = HomeGrid(4, 8, home.items + widget(1, 0, 7, 4, 1)).let {
            GridLayoutEngine.place(it, HomeGridItem.APPS_KEY, CellRect(0, 2, 4, 5))!!
        }
        assertNull(GridLayoutEngine.resizeGrid(grid, 3, 8, limits))
    }

    // ── normalize & defaults ───────────────────────────────────────

    @Test
    fun `normalize repairs overlaps, strays and a missing list`() {
        val broken = HomeGrid(
            4,
            8,
            listOf(clock(0, 0, 4, 2), widget(1, 0, 1, 4, 2), widget(1, 0, 5, 4, 1), widget(2, 3, 7, 4, 3))
        )
        val repaired = GridLayoutEngine.normalize(broken, anyLimits)

        assertTrue(GridLayoutEngine.isValid(repaired))
        assertTrue(repaired.has(Type.APPS))
        assertEquals(1, repaired.items.count { it.key == "widget:1" })
    }

    @Test
    fun `normalize clamps an impossible grid size`() {
        val repaired = GridLayoutEngine.normalize(HomeGrid(40, 1, listOf(apps(0, 0, 4, 2))), anyLimits)
        assertEquals(HomeGrid.MAX_COLUMNS, repaired.columns)
        assertEquals(HomeGrid.MIN_ROWS, repaired.rows)
        assertTrue(GridLayoutEngine.isValid(repaired))
    }

    @Test
    fun `default layout stacks the blocks in order and gives the list the rest`() {
        val grid = GridLayoutEngine.defaultLayout(
            4,
            8,
            listOf(clock(0, 0, 1, 1), widget(3, 0, 0, 2, 1), apps(0, 0, 1, 1)),
            anyLimits
        )

        assertEquals(CellRect(0, 0, 4, 2), grid.rect(HomeGridItem.CLOCK_KEY))
        assertEquals(CellRect(1, 2, 2, 1), grid.rect("widget:3"))
        assertEquals(CellRect(0, 3, 4, 5), grid.rect(HomeGridItem.APPS_KEY))
    }

    @Test
    fun `default layout with a tall widget still fits everything`() {
        val grid = GridLayoutEngine.defaultLayout(
            4,
            6,
            listOf(clock(0, 0, 1, 1), widget(3, 0, 0, 4, 5), apps(0, 0, 1, 1)),
            anyLimits
        )
        assertTrue(GridLayoutEngine.isValid(grid))
        assertNotNull(grid.item("widget:3"))
        assertTrue(grid.rect(HomeGridItem.APPS_KEY).spanY >= GridLayoutEngine.APPS_LIMITS.minY)
    }
}
