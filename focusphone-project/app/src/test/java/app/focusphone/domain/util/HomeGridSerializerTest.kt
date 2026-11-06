package app.focusphone.domain.util

import app.focusphone.domain.model.CellRect
import app.focusphone.domain.model.HomeGrid
import app.focusphone.domain.model.HomeGridItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeGridSerializerTest {

    private val grid = HomeGrid(
        4,
        8,
        listOf(
            HomeGridItem(HomeGridItem.Type.CLOCK, CellRect(0, 0, 4, 2)),
            HomeGridItem(HomeGridItem.Type.WIDGET, CellRect(1, 2, 2, 1), 57, "com.example/.ClockWidget"),
            HomeGridItem(HomeGridItem.Type.APPS, CellRect(0, 3, 4, 5))
        )
    )

    @Test
    fun `round trip keeps every block`() {
        assertEquals(grid, HomeGridSerializer.parse(HomeGridSerializer.serialize(grid)))
    }

    @Test
    fun `a widget without a known provider survives the round trip`() {
        val noProvider = grid.copy(items = listOf(HomeGridItem(HomeGridItem.Type.WIDGET, CellRect(0, 0, 1, 1), 9, null)))
        assertEquals(noProvider, HomeGridSerializer.parse(HomeGridSerializer.serialize(noProvider)))
    }

    @Test
    fun `malformed lines are skipped, not the whole layout`() {
        val raw = """
            grid|1|4|8
            clock|0|0|4|2
            clock|x|0|4|2
            widget|0|2|4|1|0|pkg/.W
            widget|0|2|4|1
            mystery|0|0|1|1
            apps|0|3|4|5
        """.trimIndent()

        val parsed = HomeGridSerializer.parse(raw)!!
        assertEquals(listOf(HomeGridItem.Type.CLOCK, HomeGridItem.Type.APPS), parsed.items.map { it.type })
    }

    @Test
    fun `an unreadable header or a newer version yields nothing`() {
        assertNull(HomeGridSerializer.parse(""))
        assertNull(HomeGridSerializer.parse("clock|0|0|4|2"))
        assertNull(HomeGridSerializer.parse("grid|1|0|8"))
        assertNull(HomeGridSerializer.parse("grid|99|4|8"))
    }

    @Test
    fun `separators in a provider name cannot break the format`() {
        val tricky = grid.copy(items = listOf(HomeGridItem(HomeGridItem.Type.WIDGET, CellRect(0, 0, 1, 1), 3, "a|b\nc/.W")))
        val parsed = HomeGridSerializer.parse(HomeGridSerializer.serialize(tricky))!!
        assertEquals("abc/.W", parsed.items.single().provider)
    }
}
