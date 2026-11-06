package app.focusphone.domain.util

import app.focusphone.domain.model.AppScheduleRule
import app.focusphone.domain.model.ScheduleBlockReason

/**
 * Pure evaluation and serialization logic for [AppScheduleRule].
 *
 * Everything here is free of Android framework and `Context` dependencies so the
 * blocking decision — the most safety-critical part of the feature — can be unit
 * tested exhaustively on the JVM.
 */
object AppScheduleEvaluator {

    private const val FIELD_SEPARATOR = "|"
    private const val DAY_SEPARATOR = ","
    private const val FIELD_COUNT = 6

    // ── Blocking decision ──────────────────────────────────────────

    /**
     * Returns the reason why [rule] blocks its app right now, or `null` when the
     * app may be used.
     *
     * @param minuteOfDay minutes elapsed since local midnight (0..1439).
     * @param dayOfWeek `java.util.Calendar.DAY_OF_WEEK` value (Sunday = 1 … Saturday = 7).
     * @param usedMinutesToday minutes the app has already been used today.
     */
    fun evaluate(
        rule: AppScheduleRule,
        minuteOfDay: Int,
        dayOfWeek: Int,
        usedMinutesToday: Int
    ): ScheduleBlockReason? {
        if (!rule.isActive) return null

        if (rule.windowEnabled && !isWithinWindow(rule, minuteOfDay, dayOfWeek)) {
            return ScheduleBlockReason.OutsideWindow(rule.startMinute, rule.endMinute)
        }

        if (rule.hasDailyLimit && usedMinutesToday >= rule.dailyLimitMinutes) {
            return ScheduleBlockReason.DailyLimitReached(usedMinutesToday, rule.dailyLimitMinutes)
        }

        return null
    }

    /**
     * Returns whether [minuteOfDay] on [dayOfWeek] falls inside the allowed window.
     *
     * The window is half-open — `[start, end)` — so a 09:00–18:00 window blocks at
     * exactly 18:00. Windows that wrap past midnight belong to the day they *start*
     * on: with a 22:00–06:00 window enabled only on Friday, 02:00 on Saturday is
     * still allowed because that stretch is the tail of Friday's window.
     */
    fun isWithinWindow(rule: AppScheduleRule, minuteOfDay: Int, dayOfWeek: Int): Boolean {
        val days = rule.days.ifEmpty { AppScheduleRule.ALL_DAYS }

        // start == end means the window covers the whole selected day.
        if (rule.startMinute == rule.endMinute) return days.contains(dayOfWeek)

        return if (rule.startMinute < rule.endMinute) {
            days.contains(dayOfWeek) &&
                minuteOfDay >= rule.startMinute &&
                minuteOfDay < rule.endMinute
        } else {
            (days.contains(dayOfWeek) && minuteOfDay >= rule.startMinute) ||
                (days.contains(previousDay(dayOfWeek)) && minuteOfDay < rule.endMinute)
        }
    }

    /** Previous `Calendar.DAY_OF_WEEK` value, wrapping Sunday (1) back to Saturday (7). */
    fun previousDay(dayOfWeek: Int): Int = if (dayOfWeek <= 1) 7 else dayOfWeek - 1

    // ── Serialization ──────────────────────────────────────────────

    /**
     * Serializes a rule to a single line:
     * `packageName|windowEnabled|startMinute|endMinute|day,day,…|dailyLimitMinutes`
     */
    fun serialize(rule: AppScheduleRule): String = listOf(
        rule.packageName,
        if (rule.windowEnabled) "1" else "0",
        rule.startMinute.toString(),
        rule.endMinute.toString(),
        rule.days.sorted().joinToString(DAY_SEPARATOR),
        rule.dailyLimitMinutes.toString()
    ).joinToString(FIELD_SEPARATOR)

    /**
     * Parses a line produced by [serialize]. Returns `null` for malformed input so a
     * single corrupted entry can be skipped without discarding the whole rule set.
     */
    fun parse(line: String): AppScheduleRule? {
        val parts = line.split(FIELD_SEPARATOR)
        if (parts.size != FIELD_COUNT) return null

        val packageName = parts[0].trim()
        if (packageName.isEmpty()) return null

        val startMinute = parts[2].toIntOrNull() ?: return null
        val endMinute = parts[3].toIntOrNull() ?: return null
        val dailyLimitMinutes = parts[5].toIntOrNull() ?: return null

        val days = parts[4]
            .split(DAY_SEPARATOR)
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()

        return AppScheduleRule(
            packageName = packageName,
            windowEnabled = parts[1] == "1",
            startMinute = startMinute.coerceIn(0, AppScheduleRule.MINUTES_PER_DAY - 1),
            endMinute = endMinute.coerceIn(0, AppScheduleRule.MINUTES_PER_DAY - 1),
            days = days.ifEmpty { AppScheduleRule.ALL_DAYS },
            dailyLimitMinutes = dailyLimitMinutes.coerceAtLeast(0)
        )
    }

    /** Formats [minuteOfDay] as a zero-padded `HH:mm` label. */
    fun formatMinuteOfDay(minuteOfDay: Int): String {
        val safe = minuteOfDay.coerceIn(0, AppScheduleRule.MINUTES_PER_DAY - 1)
        return "%02d:%02d".format(safe / 60, safe % 60)
    }
}
