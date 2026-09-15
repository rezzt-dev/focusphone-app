package app.focusphone.domain.model

/**
 * Time-based access rule for a single application.
 *
 * A rule combines two independent restrictions:
 *  - **Allowed time window** ([windowEnabled]): the app may only be opened between
 *    [startMinute] and [endMinute] on the selected [days].
 *  - **Daily usage limit** ([dailyLimitMinutes]): the app may only be used for a
 *    limited amount of minutes per calendar day. The counter resets at local midnight.
 *
 * Both restrictions can be active at the same time. A rule where neither is active
 * ([isActive] == false) never blocks anything and is dropped when persisted.
 *
 * Minutes are expressed as *minutes since local midnight* (0..1439), so 9:30 is 570.
 * A window where [startMinute] is greater than [endMinute] wraps past midnight
 * (e.g. 22:00 → 06:00). A window where both are equal means "all day".
 *
 * [days] holds `java.util.Calendar.DAY_OF_WEEK` values (Sunday = 1 … Saturday = 7).
 * An empty set is interpreted as "every day".
 */
data class AppScheduleRule(
    val packageName: String,
    val windowEnabled: Boolean = false,
    val startMinute: Int = DEFAULT_START_MINUTE,
    val endMinute: Int = DEFAULT_END_MINUTE,
    val days: Set<Int> = ALL_DAYS,
    val dailyLimitMinutes: Int = 0
) {

    /** Whether this rule restricts anything at all. */
    val isActive: Boolean
        get() = windowEnabled || dailyLimitMinutes > 0

    /** Whether the daily usage limit restriction is enabled. */
    val hasDailyLimit: Boolean
        get() = dailyLimitMinutes > 0

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
        const val DEFAULT_START_MINUTE = 9 * 60
        const val DEFAULT_END_MINUTE = 18 * 60
        const val DEFAULT_DAILY_LIMIT_MINUTES = 30

        /** All `Calendar.DAY_OF_WEEK` values, Sunday (1) through Saturday (7). */
        val ALL_DAYS: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)
    }
}

/**
 * Why an app is currently blocked by its [AppScheduleRule].
 *
 * The reason carries the data needed to explain the block to the user without
 * having to re-read the rule from encrypted storage.
 */
sealed class ScheduleBlockReason {

    /** The app is outside its allowed time window. */
    data class OutsideWindow(
        val startMinute: Int,
        val endMinute: Int
    ) : ScheduleBlockReason()

    /** The daily usage allowance has been consumed. */
    data class DailyLimitReached(
        val usedMinutes: Int,
        val limitMinutes: Int
    ) : ScheduleBlockReason()
}
