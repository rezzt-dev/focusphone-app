package app.focusphone.ui.widget.library

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CalendarGridTest {

    private lateinit var originalZone: TimeZone

    @Before
    fun setUp() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    private fun date(year: Int, month: Int, day: Int, hour: Int = 12): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day, hour, 0, 0)
    }.timeInMillis

    @Test
    fun `november 2026 on a monday-first week needs six rows`() {
        // 1 november 2026 is a sunday: six blanks before it when weeks start on monday.
        val month = CalendarGrid.month(date(2026, Calendar.NOVEMBER, 5), Calendar.MONDAY)

        assertEquals(6, month.rows)
        assertEquals(0, month.cells[5].day)
        assertEquals(1, month.cells[6].day)
        assertEquals(30, month.cells[35].day)
        assertEquals(0, month.cells[36].day)
    }

    @Test
    fun `the same month on a sunday-first week needs five rows`() {
        val month = CalendarGrid.month(date(2026, Calendar.NOVEMBER, 5), Calendar.SUNDAY)

        assertEquals(5, month.rows)
        assertEquals(1, month.cells[0].day)
    }

    @Test
    fun `today is the only highlighted day`() {
        val month = CalendarGrid.month(date(2026, Calendar.NOVEMBER, 5), Calendar.MONDAY)

        val today = month.cells.filter { it.isToday }
        assertEquals(listOf(CalendarDay(5, true)), today)
        assertTrue(month.cells[10].isToday)
    }

    @Test
    fun `a february starting on the first weekday fits in four rows`() {
        // 1 february 2026 is a sunday and the month has 28 days.
        val month = CalendarGrid.month(date(2026, Calendar.FEBRUARY, 10), Calendar.SUNDAY)

        assertEquals(4, month.rows)
        assertEquals(CalendarGrid.MAX_ROWS * CalendarGrid.COLUMNS, month.cells.size)
    }

    @Test
    fun `leap years give february its 29th`() {
        val month = CalendarGrid.month(date(2028, Calendar.FEBRUARY, 29), Calendar.MONDAY)

        assertEquals(29, month.cells.filter { it.day > 0 }.size)
        assertTrue(month.cells.single { it.isToday }.day == 29)
    }

    @Test
    fun `the week strip runs from the first weekday and marks today`() {
        val week = CalendarGrid.week(date(2026, Calendar.NOVEMBER, 5), Calendar.MONDAY)

        assertEquals(listOf(2, 3, 4, 5, 6, 7, 8), week.map { it.day })
        assertEquals(3, week.indexOfFirst { it.isToday })
    }

    @Test
    fun `the week strip crosses the end of the month`() {
        val week = CalendarGrid.week(date(2026, Calendar.OCTOBER, 30), Calendar.MONDAY)

        assertEquals(listOf(26, 27, 28, 29, 30, 31, 1), week.map { it.day })
        assertEquals(4, week.indexOfFirst { it.isToday })
    }

    @Test
    fun `weekday order starts on the requested day and wraps`() {
        val mondayFirst = listOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY
        )
        assertEquals(mondayFirst, CalendarGrid.weekdayOrder(Calendar.MONDAY))
        assertEquals(Calendar.SATURDAY, CalendarGrid.weekdayOrder(Calendar.SATURDAY).first())
        assertEquals(Calendar.FRIDAY, CalendarGrid.weekdayOrder(Calendar.SATURDAY).last())
    }
}
