package app.focusphone.ui.widget.library

import android.os.SystemClock
import app.focusphone.R
import app.focusphone.data.preferences.LibraryWidgetPreferences
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.ui.widget.library.SystemRenderers.bindBar
import java.util.Calendar

/** How much of the current day, week, month and year has passed, each 0..1. */
data class TimeProgress(val day: Float, val week: Float, val month: Float, val year: Float) {

    companion object {
        /** Pure so it can be tested across week starts, month lengths and leap years. */
        fun at(nowMs: Long, firstDayOfWeek: Int): TimeProgress {
            fun span(truncate: Calendar.() -> Unit, advance: Calendar.() -> Unit): Float {
                val start = Calendar.getInstance().apply {
                    this.firstDayOfWeek = firstDayOfWeek
                    timeInMillis = nowMs
                    truncate()
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = (start.clone() as Calendar).apply(advance)
                val length = (end.timeInMillis - start.timeInMillis).toFloat()
                return if (length <= 0f) 0f else ((nowMs - start.timeInMillis) / length).coerceIn(0f, 1f)
            }
            return TimeProgress(
                day = span({}, { add(Calendar.DAY_OF_MONTH, 1) }),
                week = span(
                    { add(Calendar.DAY_OF_MONTH, -Math.floorMod(get(Calendar.DAY_OF_WEEK) - firstDayOfWeek, DAYS_PER_WEEK)) },
                    { add(Calendar.DAY_OF_MONTH, DAYS_PER_WEEK) }
                ),
                month = span({ set(Calendar.DAY_OF_MONTH, 1) }, { add(Calendar.MONTH, 1) }),
                year = span({ set(Calendar.DAY_OF_YEAR, 1) }, { add(Calendar.YEAR, 1) })
            )
        }

        private const val DAYS_PER_WEEK = 7
    }
}

/** The focus profile, the daily intention and the day's progress. */
object FocusRenderers {

    val Profile = WidgetRenderer { scene ->
        val style = scene.style
        val context = scene.context
        ProfilePreferences.expireDetoxIfNeeded(context)
        val profile = ProfilePreferences.getProfile(context)
        val detoxEnd = ProfilePreferences.getDetoxEndTime(context)
        val now = System.currentTimeMillis()
        val detox = ProfilePreferences.isDetoxActivePure(profile, detoxEnd, now)
        scene.views(R.layout.widget_focus_profile).apply {
            card(R.id.wg_bg, style)
            tint(R.id.wg_badge_fill, if (detox) style.accent else style.chip)
            icon(R.id.wg_badge_icon, profileIcon(profile), if (detox) style.onAccent else style.textPrimary)
            setTextViewText(R.id.wg_value, scene.string(profileName(profile)))
            setTextColor(R.id.wg_value, style.textPrimary)
            setTextColor(R.id.wg_caption, style.textSecondary)
            setTextColor(R.id.wg_countdown, style.textSecondary)
            show(R.id.wg_caption, !detox)
            show(R.id.wg_countdown, detox)
            if (detox) {
                val base = SystemClock.elapsedRealtime() + (detoxEnd - now)
                setChronometer(R.id.wg_countdown, base, scene.string(R.string.wg_detox_left_format), true)
                setChronometerCountDown(R.id.wg_countdown, true)
                WidgetUpdater.scheduleProfileTick(context, detoxEnd)
            } else {
                setChronometer(R.id.wg_countdown, SystemClock.elapsedRealtime(), null, false)
            }
            setOnClickPendingIntent(R.id.wg_root, WidgetIntents.openHome(context))
        }
    }

    val Intention = WidgetRenderer { scene ->
        val style = scene.style
        val intention = LibraryWidgetPreferences.getIntention(scene.context)
        scene.views(R.layout.widget_intention).apply {
            card(R.id.wg_bg, style)
            setTextViewText(R.id.wg_value, intention.ifEmpty { scene.string(R.string.wg_intention_empty) })
            setTextColor(R.id.wg_value, if (intention.isEmpty()) style.textSecondary else style.textPrimary)
            setTextColor(R.id.wg_caption, style.textTertiary)
            tint(R.id.wg_edit, style.textSecondary)
            setOnClickPendingIntent(R.id.wg_root, WidgetIntents.editIntention(scene.context))
        }
    }

    val DayProgress = WidgetRenderer { scene ->
        val style = scene.style
        val progress = TimeProgress.at(System.currentTimeMillis(), Calendar.getInstance().firstDayOfWeek)
        val barDp = scene.size.widthDp - PROGRESS_CHROME_DP
        scene.views(R.layout.widget_day_progress).apply {
            card(R.id.wg_bg, style)
            listOf(
                Triple(R.id.wg_day_label, R.id.wg_day_value, R.id.wg_day_bar) to progress.day,
                Triple(R.id.wg_week_label, R.id.wg_week_value, R.id.wg_week_bar) to progress.week,
                Triple(R.id.wg_month_label, R.id.wg_month_value, R.id.wg_month_bar) to progress.month,
                Triple(R.id.wg_year_label, R.id.wg_year_value, R.id.wg_year_bar) to progress.year
            ).forEach { (ids, fraction) ->
                val (label, value, bar) = ids
                setTextColor(label, style.textPrimary)
                setTextViewText(value, SystemRenderers.percent(fraction))
                setTextColor(value, style.textSecondary)
                bindBar(scene, bar, barDp, fraction)
            }
        }
    }

    /** Side padding, label and value columns of the progress widget, in dp. */
    private const val PROGRESS_CHROME_DP = 152

    private fun profileName(profile: String): Int = when (profile) {
        ProfilePreferences.PROFILE_WORK -> R.string.profile_work
        ProfilePreferences.PROFILE_DETOX -> R.string.profile_detox
        else -> R.string.profile_normal
    }

    private fun profileIcon(profile: String): Int = when (profile) {
        ProfilePreferences.PROFILE_WORK -> R.drawable.ic_wg_briefcase
        ProfilePreferences.PROFILE_DETOX -> R.drawable.ic_wg_shield
        else -> R.drawable.ic_wg_leaf
    }
}
