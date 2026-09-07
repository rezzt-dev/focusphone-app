package app.focusphone.domain.util

import app.focusphone.domain.model.AppScheduleRule
import app.focusphone.domain.model.ScheduleBlockReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for the pure schedule-blocking logic.
 *
 * Times are minutes since local midnight; days use `Calendar.DAY_OF_WEEK`
 * (Sunday = 1 … Saturday = 7).
 */
class AppScheduleEvaluatorTest {

    private val monday = Calendar.MONDAY
    private val tuesday = Calendar.TUESDAY
    private val saturday = Calendar.SATURDAY

    private fun rule(
        windowEnabled: Boolean = false,
        start: Int = 9 * 60,
        end: Int = 18 * 60,
        days: Set<Int> = AppScheduleRule.ALL_DAYS,
        limit: Int = 0
    ) = AppScheduleRule(
        packageName = "com.example.app",
        windowEnabled = windowEnabled,
        startMinute = start,
        endMinute = end,
        days = days,
        dailyLimitMinutes = limit
    )

    // ── Inactive rules ─────────────────────────────────────────────

    @Test
    fun `a rule with no restriction never blocks`() {
        assertNull(AppScheduleEvaluator.evaluate(rule(), 12 * 60, monday, 999))
    }

    // ── Time window ────────────────────────────────────────────────

    @Test
    fun `inside the window the app is allowed`() {
        assertNull(AppScheduleEvaluator.evaluate(rule(windowEnabled = true), 10 * 60, monday, 0))
    }

    @Test
    fun `before the window opens the app is blocked`() {
        val reason = AppScheduleEvaluator.evaluate(rule(windowEnabled = true), 8 * 60 + 59, monday, 0)
        assertTrue(reason is ScheduleBlockReason.OutsideWindow)
    }

    @Test
    fun `the window end is exclusive`() {
        val at1800 = AppScheduleEvaluator.evaluate(rule(windowEnabled = true), 18 * 60, monday, 0)
        val at1759 = AppScheduleEvaluator.evaluate(rule(windowEnabled = true), 18 * 60 - 1, monday, 0)
        assertTrue(at1800 is ScheduleBlockReason.OutsideWindow)
        assertNull(at1759)
    }

    @Test
    fun `the block reason carries the configured window`() {
        val reason = AppScheduleEvaluator.evaluate(rule(windowEnabled = true), 0, monday, 0)
        assertEquals(ScheduleBlockReason.OutsideWindow(9 * 60, 18 * 60), reason)
    }

    @Test
    fun `a window is ignored on days that are not selected`() {
        val weekdayRule = rule(windowEnabled = true, days = setOf(monday, tuesday))
        assertNull(AppScheduleEvaluator.evaluate(weekdayRule, 10 * 60, monday, 0))
        assertTrue(
            AppScheduleEvaluator.evaluate(weekdayRule, 10 * 60, saturday, 0)
                is ScheduleBlockReason.OutsideWindow
        )
    }

    @Test
    fun `a window that wraps past midnight covers both sides of the night`() {
        val nightRule = rule(windowEnabled = true, start = 22 * 60, end = 6 * 60)
        assertTrue(AppScheduleEvaluator.isWithinWindow(nightRule, 23 * 60, monday))
        assertTrue(AppScheduleEvaluator.isWithinWindow(nightRule, 2 * 60, monday))
        assertFalse(AppScheduleEvaluator.isWithinWindow(nightRule, 12 * 60, monday))
    }

    @Test
    fun `a wrapped window belongs to the day it starts on`() {
        // Enabled only on Monday: 02:00 on Tuesday is still Monday night's window.
        val nightRule = rule(windowEnabled = true, start = 22 * 60, end = 6 * 60, days = setOf(monday))
        assertTrue(AppScheduleEvaluator.isWithinWindow(nightRule, 23 * 60, monday))
        assertTrue(AppScheduleEvaluator.isWithinWindow(nightRule, 2 * 60, tuesday))
        assertFalse(AppScheduleEvaluator.isWithinWindow(nightRule, 23 * 60, tuesday))
    }

    @Test
    fun `equal start and end means the whole selected day is allowed`() {
        val allDay = rule(windowEnabled = true, start = 0, end = 0, days = setOf(monday))
        assertTrue(AppScheduleEvaluator.isWithinWindow(allDay, 3 * 60, monday))
        assertTrue(AppScheduleEvaluator.isWithinWindow(allDay, 23 * 60, monday))
        assertFalse(AppScheduleEvaluator.isWithinWindow(allDay, 12 * 60, tuesday))
    }

    @Test
    fun `an empty day set is treated as every day`() {
        val anyDay = rule(windowEnabled = true, days = emptySet())
        assertTrue(AppScheduleEvaluator.isWithinWindow(anyDay, 10 * 60, monday))
        assertTrue(AppScheduleEvaluator.isWithinWindow(anyDay, 10 * 60, saturday))
    }

    @Test
    fun `previousDay wraps sunday back to saturday`() {
        assertEquals(Calendar.SATURDAY, AppScheduleEvaluator.previousDay(Calendar.SUNDAY))
        assertEquals(Calendar.MONDAY, AppScheduleEvaluator.previousDay(Calendar.TUESDAY))
    }

    // ── Daily limit ────────────────────────────────────────────────

    @Test
    fun `usage below the limit is allowed`() {
        assertNull(AppScheduleEvaluator.evaluate(rule(limit = 30), 12 * 60, monday, 29))
    }

    @Test
    fun `usage that reaches the limit blocks the app`() {
        val reason = AppScheduleEvaluator.evaluate(rule(limit = 30), 12 * 60, monday, 30)
        assertEquals(ScheduleBlockReason.DailyLimitReached(30, 30), reason)
    }

    @Test
    fun `the window takes precedence over the daily limit`() {
        val both = rule(windowEnabled = true, limit = 30)
        val reason = AppScheduleEvaluator.evaluate(both, 3 * 60, monday, 999)
        assertTrue(reason is ScheduleBlockReason.OutsideWindow)
    }

    @Test
    fun `inside the window the daily limit is still enforced`() {
        val both = rule(windowEnabled = true, limit = 30)
        val reason = AppScheduleEvaluator.evaluate(both, 10 * 60, monday, 45)
        assertEquals(ScheduleBlockReason.DailyLimitReached(45, 30), reason)
    }

    // ── Serialization ──────────────────────────────────────────────

    @Test
    fun `serialize and parse round-trip preserves every field`() {
        val original = AppScheduleRule(
            packageName = "com.instagram.android",
            windowEnabled = true,
            startMinute = 7 * 60 + 30,
            endMinute = 21 * 60 + 45,
            days = setOf(monday, tuesday, saturday),
            dailyLimitMinutes = 45
        )
        assertEquals(original, AppScheduleEvaluator.parse(AppScheduleEvaluator.serialize(original)))
    }

    @Test
    fun `parse rejects malformed lines`() {
        assertNull(AppScheduleEvaluator.parse(""))
        assertNull(AppScheduleEvaluator.parse("com.example.app|1|540"))
        assertNull(AppScheduleEvaluator.parse("com.example.app|1|abc|1080|2,3|30"))
        assertNull(AppScheduleEvaluator.parse("|1|540|1080|2,3|30"))
    }

    @Test
    fun `parse clamps out-of-range values`() {
        val parsed = AppScheduleEvaluator.parse("com.example.app|1|-60|5000|9,2|-10")
        requireNotNull(parsed)
        assertEquals(0, parsed.startMinute)
        assertEquals(AppScheduleRule.MINUTES_PER_DAY - 1, parsed.endMinute)
        assertEquals(setOf(monday), parsed.days)
        assertEquals(0, parsed.dailyLimitMinutes)
    }

    @Test
    fun `parse restores every day when no valid day is stored`() {
        val parsed = AppScheduleEvaluator.parse("com.example.app|0|540|1080||30")
        assertEquals(AppScheduleRule.ALL_DAYS, parsed?.days)
    }

    // ── Formatting ─────────────────────────────────────────────────

    @Test
    fun `formatMinuteOfDay pads hours and minutes`() {
        assertEquals("00:00", AppScheduleEvaluator.formatMinuteOfDay(0))
        assertEquals("09:05", AppScheduleEvaluator.formatMinuteOfDay(9 * 60 + 5))
        assertEquals("23:59", AppScheduleEvaluator.formatMinuteOfDay(23 * 60 + 59))
    }
}
