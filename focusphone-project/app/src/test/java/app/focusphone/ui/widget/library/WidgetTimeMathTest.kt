package app.focusphone.ui.widget.library

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** Day progress, day distances and world-clock offsets: the time maths behind the widgets. */
class WidgetTimeMathTest {

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

    private fun date(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day, hour, minute, 0)
    }.timeInMillis

    // ── Progress ───────────────────────────────────────────────────

    @Test
    fun `thursday noon is half of the day and half of a monday-first week`() {
        val progress = TimeProgress.at(date(2026, Calendar.NOVEMBER, 5), Calendar.MONDAY)

        assertEquals(0.5f, progress.day, DELTA)
        assertEquals(0.5f, progress.week, DELTA)
        assertEquals(4.5f / 30f, progress.month, DELTA)
        assertEquals(308.5f / 365f, progress.year, DELTA)
    }

    @Test
    fun `a sunday-first week is further along on the same thursday`() {
        val progress = TimeProgress.at(date(2026, Calendar.NOVEMBER, 5), Calendar.SUNDAY)

        assertEquals(4.5f / 7f, progress.week, DELTA)
    }

    @Test
    fun `progress starts at zero on the first instant`() {
        val progress = TimeProgress.at(date(2027, Calendar.JANUARY, 1, hour = 0), Calendar.MONDAY)

        assertEquals(0f, progress.day, DELTA)
        assertEquals(0f, progress.month, DELTA)
        assertEquals(0f, progress.year, DELTA)
    }

    @Test
    fun `leap years are measured over 366 days`() {
        val progress = TimeProgress.at(date(2028, Calendar.DECEMBER, 31, hour = 0), Calendar.MONDAY)

        assertEquals(365f / 366f, progress.year, DELTA)
    }

    // ── Day distances ──────────────────────────────────────────────

    @Test
    fun `days between counts calendar days, not 24-hour blocks`() {
        assertEquals(0, WidgetDates.daysBetween(date(2026, Calendar.NOVEMBER, 5, 0, 5), date(2026, Calendar.NOVEMBER, 5, 23, 55)))
        assertEquals(1, WidgetDates.daysBetween(date(2026, Calendar.NOVEMBER, 5, 23, 55), date(2026, Calendar.NOVEMBER, 6, 0, 5)))
        assertEquals(6, WidgetDates.daysBetween(date(2026, Calendar.NOVEMBER, 5), date(2026, Calendar.NOVEMBER, 11)))
    }

    @Test
    fun `a daylight saving change still counts as one day`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"))
        // Clocks go back on 25 october 2026: that day has 25 hours.
        assertEquals(2, WidgetDates.daysBetween(date(2026, Calendar.OCTOBER, 24), date(2026, Calendar.OCTOBER, 26)))
        // ...and forward on 29 march 2026: that day has 23 hours.
        assertEquals(1, WidgetDates.daysBetween(date(2026, Calendar.MARCH, 29, 0, 30), date(2026, Calendar.MARCH, 30, 0, 30)))
    }

    // ── World clock ────────────────────────────────────────────────

    @Test
    fun `offsets are measured against the device zone at that instant`() {
        val now = date(2026, Calendar.NOVEMBER, 5)

        assertEquals(330, WorldClockZones.offsetMinutes("Asia/Kolkata", "UTC", now))
        assertEquals(-300, WorldClockZones.offsetMinutes("America/New_York", "UTC", now))
        assertEquals(0, WorldClockZones.offsetMinutes("Europe/London", "UTC", now))
        // In july London is on summer time.
        assertEquals(60, WorldClockZones.offsetMinutes("Europe/London", "UTC", date(2026, Calendar.JULY, 1)))
    }

    @Test
    fun `offsets read as signed hours with the true minus sign`() {
        assertEquals("+8 h", WorldClockZones.formatOffset(480))
        assertEquals("−6 h", WorldClockZones.formatOffset(-360))
        assertEquals("+5:30 h", WorldClockZones.formatOffset(330))
        assertEquals("+5:45 h", WorldClockZones.formatOffset(345))
        assertEquals("−3:30 h", WorldClockZones.formatOffset(-210))
    }

    @Test
    fun `a zone id falls back to a readable city name`() {
        assertEquals("New York", WorldClockZones.cityFromId("America/New_York"))
        assertEquals("Buenos Aires", WorldClockZones.cityFromId("America/Argentina/Buenos_Aires"))
    }

    @Test
    fun `next midnight is the start of the following day`() {
        assertEquals(date(2026, Calendar.NOVEMBER, 6, hour = 0), WidgetUpdater.nextMidnight(date(2026, Calendar.NOVEMBER, 5, 23, 59)))
        assertEquals(date(2027, Calendar.JANUARY, 1, hour = 0), WidgetUpdater.nextMidnight(date(2026, Calendar.DECEMBER, 31, 0, 0)))
    }

    private companion object {
        const val DELTA = 0.0005f
    }
}
