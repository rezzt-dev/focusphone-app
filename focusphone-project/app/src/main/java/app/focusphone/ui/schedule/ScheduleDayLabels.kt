package app.focusphone.ui.schedule

import java.text.DateFormatSymbols
import java.util.Calendar

/**
 * Localized short weekday labels for the schedule UI.
 *
 * Names and week order come from the device locale through [DateFormatSymbols] and
 * [Calendar.getFirstDayOfWeek], so the day chips read "mon tue wed" in Spanish or
 * "sun mon tue" in the United States without any translated string resources.
 */
object ScheduleDayLabels {

    /** Short label for a `Calendar.DAY_OF_WEEK` value, in the app's lowercase tone. */
    fun shortLabel(dayOfWeek: Int): String =
        DateFormatSymbols.getInstance().shortWeekdays.getOrNull(dayOfWeek)
            ?.lowercase()
            ?.trimEnd('.')
            .orEmpty()

    /** The seven `Calendar.DAY_OF_WEEK` values ordered from the locale's first weekday. */
    fun orderedDays(): List<Int> {
        val first = Calendar.getInstance().firstDayOfWeek
        return (0 until 7).map { offset -> (first - 1 + offset) % 7 + 1 }
    }

    /** Comma-separated labels of [days], in locale week order. */
    fun summarize(days: Set<Int>): String =
        orderedDays().filter { it in days }.joinToString(", ") { shortLabel(it) }
}
