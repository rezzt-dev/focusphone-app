package app.focusphone.ui.widget.library

import android.widget.RemoteViews
import app.focusphone.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** One day of a calendar grid. [day] is 0 for the blank cells around the month. */
data class CalendarDay(val day: Int, val isToday: Boolean)

/**
 * Pure calendar maths for the month and week widgets, independent of Android views so it
 * can be unit tested for every first-day-of-week and month length.
 */
object CalendarGrid {

    const val COLUMNS = 7
    const val MAX_ROWS = 6

    /**
     * The month of [todayMs] laid out in weeks starting on [firstDayOfWeek]
     * ([Calendar.SUNDAY]..[Calendar.SATURDAY]). Always [MAX_ROWS] × [COLUMNS] cells; [rows]
     * says how many of them the month needs.
     */
    fun month(todayMs: Long, firstDayOfWeek: Int): Month {
        val calendar = Calendar.getInstance().apply { timeInMillis = todayMs }
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val length = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val lead = leadingBlanks(calendar.get(Calendar.DAY_OF_WEEK), firstDayOfWeek)
        val cells = List(MAX_ROWS * COLUMNS) { index ->
            val day = index - lead + 1
            if (day in 1..length) CalendarDay(day, day == today) else CalendarDay(0, false)
        }
        val rows = (lead + length + COLUMNS - 1) / COLUMNS
        return Month(cells, rows)
    }

    /** The seven days of the week containing [todayMs], starting on [firstDayOfWeek]. */
    fun week(todayMs: Long, firstDayOfWeek: Int): List<CalendarDay> {
        val calendar = Calendar.getInstance().apply { timeInMillis = todayMs }
        val back = leadingBlanks(calendar.get(Calendar.DAY_OF_WEEK), firstDayOfWeek)
        calendar.add(Calendar.DAY_OF_MONTH, -back)
        return List(COLUMNS) { index ->
            CalendarDay(calendar.get(Calendar.DAY_OF_MONTH), index == back).also { calendar.add(Calendar.DAY_OF_MONTH, 1) }
        }
    }

    /** The weekdays in column order, as [Calendar] constants. */
    fun weekdayOrder(firstDayOfWeek: Int): List<Int> = List(COLUMNS) { (firstDayOfWeek - 1 + it) % COLUMNS + 1 }

    /** Cells before day 1 when the week starts on [firstDayOfWeek]. */
    internal fun leadingBlanks(dayOfWeek: Int, firstDayOfWeek: Int): Int = Math.floorMod(dayOfWeek - firstDayOfWeek, COLUMNS)

    data class Month(val cells: List<CalendarDay>, val rows: Int)
}

/** The month grid and the week strip. */
object CalendarRenderers {

    val Month = WidgetRenderer { scene ->
        val style = scene.style
        val now = System.currentTimeMillis()
        val firstDay = Calendar.getInstance().firstDayOfWeek
        val month = CalendarGrid.month(now, firstDay)
        scene.views(R.layout.widget_calendar_month).apply {
            card(R.id.wg_bg, style)
            setTextViewText(R.id.wg_month_name, WidgetDates.month(now))
            setTextViewText(R.id.wg_year, WidgetDates.year(now))
            textColor(style.textPrimary, R.id.wg_month_name)
            textColor(style.textTertiary, R.id.wg_year)
            bindWeekdays(this, MONTH_HEADS, firstDay, style)
            month.cells.forEachIndexed { index, day -> bindDay(this, MONTH_CELLS[index], day, style) }
            MONTH_ROWS.forEachIndexed { row, id -> show(id, row < month.rows) }
            setOnClickPendingIntent(R.id.wg_root, WidgetIntents.openCalendar(scene.context))
        }
    }

    val Week = WidgetRenderer { scene ->
        val style = scene.style
        val now = System.currentTimeMillis()
        val firstDay = Calendar.getInstance().firstDayOfWeek
        scene.views(R.layout.widget_calendar_week).apply {
            card(R.id.wg_bg, style)
            bindWeekdays(this, WEEK_HEADS, firstDay, style)
            CalendarGrid.week(now, firstDay).forEachIndexed { index, day -> bindDay(this, WEEK_CELLS[index], day, style) }
            setOnClickPendingIntent(R.id.wg_root, WidgetIntents.openCalendar(scene.context))
        }
    }

    private fun bindWeekdays(views: RemoteViews, ids: IntArray, firstDay: Int, style: WidgetStyle) {
        val narrow = SimpleDateFormat("EEEEE", Locale.getDefault())
        val calendar = Calendar.getInstance()
        CalendarGrid.weekdayOrder(firstDay).forEachIndexed { index, weekday ->
            calendar.set(Calendar.DAY_OF_WEEK, weekday)
            views.setTextViewText(ids[index], narrow.format(calendar.time).lowercase(Locale.getDefault()))
            views.setTextColor(ids[index], style.textTertiary)
        }
    }

    private fun bindDay(views: RemoteViews, id: Int, day: CalendarDay, style: WidgetStyle) {
        views.setTextViewText(id, if (day.day > 0) day.day.toString() else "")
        if (day.isToday) {
            views.backgroundTint(id, style.accent, R.drawable.wg_dot_light, R.drawable.wg_dot_dark)
            views.setTextColor(id, style.onAccent)
        } else {
            views.clearBackground(id)
            views.setTextColor(id, style.textPrimary)
        }
    }

    private val MONTH_ROWS = intArrayOf(R.id.wg_row_0, R.id.wg_row_1, R.id.wg_row_2, R.id.wg_row_3, R.id.wg_row_4, R.id.wg_row_5)

    private val MONTH_HEADS = intArrayOf(
        R.id.wg_head_0, R.id.wg_head_1, R.id.wg_head_2, R.id.wg_head_3, R.id.wg_head_4, R.id.wg_head_5, R.id.wg_head_6
    )
    private val WEEK_HEADS = MONTH_HEADS

    private val WEEK_CELLS = intArrayOf(
        R.id.wg_cell_0, R.id.wg_cell_1, R.id.wg_cell_2, R.id.wg_cell_3, R.id.wg_cell_4, R.id.wg_cell_5, R.id.wg_cell_6
    )

    private val MONTH_CELLS = intArrayOf(
        R.id.wg_cell_0, R.id.wg_cell_1, R.id.wg_cell_2, R.id.wg_cell_3, R.id.wg_cell_4, R.id.wg_cell_5, R.id.wg_cell_6,
        R.id.wg_cell_7, R.id.wg_cell_8, R.id.wg_cell_9, R.id.wg_cell_10, R.id.wg_cell_11, R.id.wg_cell_12, R.id.wg_cell_13,
        R.id.wg_cell_14, R.id.wg_cell_15, R.id.wg_cell_16, R.id.wg_cell_17, R.id.wg_cell_18, R.id.wg_cell_19, R.id.wg_cell_20,
        R.id.wg_cell_21, R.id.wg_cell_22, R.id.wg_cell_23, R.id.wg_cell_24, R.id.wg_cell_25, R.id.wg_cell_26, R.id.wg_cell_27,
        R.id.wg_cell_28, R.id.wg_cell_29, R.id.wg_cell_30, R.id.wg_cell_31, R.id.wg_cell_32, R.id.wg_cell_33, R.id.wg_cell_34,
        R.id.wg_cell_35, R.id.wg_cell_36, R.id.wg_cell_37, R.id.wg_cell_38, R.id.wg_cell_39, R.id.wg_cell_40, R.id.wg_cell_41
    )
}
