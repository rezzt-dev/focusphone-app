package app.focusphone.ui.widget.library

import android.app.AlarmManager
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.text.format.DateFormat
import app.focusphone.R
import app.focusphone.data.preferences.LibraryWidgetPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Clock and date widgets. The time itself is a `TextClock` or an `AnalogClock`, which the
 * launcher keeps ticking on its own: nothing here runs every minute.
 */
object ClockRenderers {

    /** Large time, the date and the next alarm. */
    val Digital = WidgetRenderer { scene ->
        val style = scene.style
        val context = scene.context
        scene.views(R.layout.widget_clock_digital).apply {
            card(R.id.wg_bg, style)
            textColor(style.textPrimary, R.id.wg_time)
            textColor(style.textSecondary, R.id.wg_ampm, R.id.wg_date, R.id.wg_alarm)
            show(R.id.wg_ampm, !DateFormat.is24HourFormat(context))
            setTextViewText(R.id.wg_date, WidgetDates.longDate(System.currentTimeMillis()))
            // A short widget keeps the time large and drops the alarm line's icon first.
            textSizeSp(R.id.wg_time, if (scene.size.heightDp < SHORT_CLOCK_DP) 44f else 58f)

            val alarm = nextAlarm(context)
            show(R.id.wg_alarm_icon, alarm != null)
            show(R.id.wg_alarm, alarm != null)
            if (alarm != null) {
                tint(R.id.wg_alarm_icon, style.textSecondary)
                setTextViewText(R.id.wg_alarm, WidgetDates.time(context, alarm))
            }
            setOnClickPendingIntent(R.id.wg_root, WidgetIntents.openClock(context))
        }
    }

    /**
     * Analog clock. Android 12 tints the dial and hands to the theme; older releases cannot
     * change an AnalogClock's drawables at runtime, so they pick light or dark hands.
     */
    val Analog = WidgetRenderer { scene ->
        val style = scene.style
        val tintable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val layout = if (tintable || style.isDark) R.layout.widget_clock_analog_on_dark else R.layout.widget_clock_analog_on_light
        scene.views(layout).apply {
            card(R.id.wg_bg, style)
            if (tintable) {
                setColorStateList(R.id.wg_analog, "setDialTintList", ColorStateList.valueOf(style.textTertiary))
                setColorStateList(R.id.wg_analog, "setHourHandTintList", ColorStateList.valueOf(style.textPrimary))
                setColorStateList(R.id.wg_analog, "setMinuteHandTintList", ColorStateList.valueOf(style.textPrimary))
            }
            setOnClickPendingIntent(R.id.wg_root, WidgetIntents.openClock(scene.context))
        }
    }

    /** Up to three cities side by side, each with its offset from local time. */
    val World = WidgetRenderer { scene ->
        val style = scene.style
        val context = scene.context
        val zones = LibraryWidgetPreferences.getWorldZones(context, scene.widgetId)
            .mapNotNull { id -> WorldClockZones.find(id) }
            .ifEmpty { LibraryWidgetPreferences.DEFAULT_WORLD_ZONES.mapNotNull { WorldClockZones.find(it) } }
        val now = System.currentTimeMillis()
        scene.views(R.layout.widget_clock_world).apply {
            card(R.id.wg_bg, style)
            COLUMNS.forEachIndexed { index, column ->
                val zone = zones.getOrNull(index)
                show(column.container, zone != null)
                if (index > 0) show(DIVIDERS[index - 1], zone != null)
                if (zone == null) return@forEachIndexed
                setTextViewText(column.city, zone.city(context))
                setString(column.time, "setTimeZone", zone.id)
                setTextViewText(column.offset, WorldClockZones.offsetLabel(context, zone.id, now))
                textColor(style.textSecondary, column.city)
                textColor(style.textPrimary, column.time)
                textColor(style.textTertiary, column.offset)
            }
            DIVIDERS.forEach { tint(it, style.track) }
            setOnClickPendingIntent(R.id.wg_root, WidgetIntents.configureWorldClock(context, scene.widgetId))
        }
    }

    /** Weekday, day of the month and month on a square tile. */
    val DateTile = WidgetRenderer { scene ->
        val style = scene.style
        val context = scene.context
        val now = System.currentTimeMillis()
        scene.views(R.layout.widget_date_tile).apply {
            card(R.id.wg_bg, style)
            setTextViewText(R.id.wg_weekday, WidgetDates.weekday(now))
            setTextViewText(R.id.wg_day, WidgetDates.dayOfMonth(now))
            setTextViewText(R.id.wg_month, WidgetDates.monthYear(now))
            textColor(style.textSecondary, R.id.wg_weekday, R.id.wg_month)
            textColor(style.textPrimary, R.id.wg_day)
            setOnClickPendingIntent(R.id.wg_root, WidgetIntents.openCalendar(context))
        }
    }

    private const val SHORT_CLOCK_DP = 150

    private class Column(val container: Int, val city: Int, val time: Int, val offset: Int)

    private val COLUMNS = listOf(
        Column(R.id.wg_col_1, R.id.wg_city_1, R.id.wg_zone_time_1, R.id.wg_offset_1),
        Column(R.id.wg_col_2, R.id.wg_city_2, R.id.wg_zone_time_2, R.id.wg_offset_2),
        Column(R.id.wg_col_3, R.id.wg_city_3, R.id.wg_zone_time_3, R.id.wg_offset_3)
    )
    private val DIVIDERS = intArrayOf(R.id.wg_divider_1, R.id.wg_divider_2)

    /** Trigger time of the next alarm the user set, or null when there is none. */
    internal fun nextAlarm(context: Context): Long? = try {
        context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime
    } catch (_: SecurityException) {
        null
    }
}

/** Lower-case, locale-aware date texts shared by the clock and calendar widgets. */
object WidgetDates {

    private fun locale(): Locale = Locale.getDefault()

    private fun format(pattern: String, timeMs: Long, zone: TimeZone = TimeZone.getDefault()): String {
        val best = DateFormat.getBestDateTimePattern(locale(), pattern)
        return SimpleDateFormat(best, locale()).apply { timeZone = zone }.format(Date(timeMs)).lowercase(locale())
    }

    /** "thursday, 5 november" in the order the language uses. */
    fun longDate(timeMs: Long): String = format("EEEEdMMMM", timeMs)

    fun weekday(timeMs: Long): String = format("EEEE", timeMs)

    fun dayOfMonth(timeMs: Long): String = format("d", timeMs)

    fun monthYear(timeMs: Long): String = format("MMMMyyyy", timeMs)

    fun month(timeMs: Long): String = SimpleDateFormat("LLLL", locale()).format(Date(timeMs)).lowercase(locale())

    fun year(timeMs: Long): String = SimpleDateFormat("yyyy", locale()).format(Date(timeMs))

    /** The time of day in the user's 12/24-hour setting. */
    fun time(context: Context, timeMs: Long): String = DateFormat.getTimeFormat(context).format(Date(timeMs)).lowercase(locale())

    /** "today", "tomorrow" or the weekday of [timeMs], seen from [nowMs]. */
    fun relativeDay(context: Context, timeMs: Long, nowMs: Long): String = when (daysBetween(nowMs, timeMs)) {
        0 -> context.getString(R.string.wg_alarm_today)
        1 -> context.getString(R.string.wg_alarm_tomorrow)
        else -> weekday(timeMs)
    }

    /** Whole calendar days from [fromMs] to [toMs] in the default time zone; DST days count as one. */
    internal fun daysBetween(fromMs: Long, toMs: Long): Int =
        Math.floorDiv(startOfDay(toMs) - startOfDay(fromMs) + HALF_DAY_MS, DAY_MS).toInt()

    internal fun startOfDay(timeMs: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timeMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private const val DAY_MS = 86_400_000L
    private const val HALF_DAY_MS = DAY_MS / 2
}
