package app.focusphone.ui.widget

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.focusphone.R
import app.focusphone.domain.model.ClockType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Single renderer for the real home clock and the home-layout preview.
 *
 * Both surfaces use the same IDs, typography and localized word arrays. Keeping those
 * rules here prevents the preview from silently drifting away from what Home renders.
 */
class ClockRenderer(
    private val context: Context,
    private val root: ViewGroup,
    private val showHiddenPlaceholder: Boolean = false
) {

    private var appliedType: ClockType? = null

    /** Applies the selected clock variant without touching unrelated home layout state. */
    fun applyType(type: ClockType) {
        if (type == appliedType) return
        appliedType = type

        val hidden = type == ClockType.HIDDEN
        root.findViewById<View>(R.id.clock_container)?.visibility =
            if (hidden && !showHiddenPlaceholder) View.GONE else View.VISIBLE
        root.findViewById<View>(R.id.clock_type_digital)?.visibility = visible(type == ClockType.DIGITAL)
        root.findViewById<View>(R.id.clock_type_stacked)?.visibility = visible(type == ClockType.STACKED)
        root.findViewById<View>(R.id.clock_type_words)?.visibility = visible(type == ClockType.WORDS)
        root.findViewById<View>(R.id.clock_type_analog)?.visibility = visible(type == ClockType.ANALOG)
        root.findViewById<View>(R.id.clock_type_none)?.visibility =
            visible(showHiddenPlaceholder && hidden)
    }

    /** Applies one scale consistently to every clock type. */
    fun applyScale(scale: Float) {
        setTextSize(R.id.tv_time, 56f * scale)
        setTextSize(R.id.tv_date, 16f * scale)
        setTextSize(R.id.tv_stacked_h, 76f * scale)
        setTextSize(R.id.tv_stacked_m, 76f * scale)
        setTextSize(R.id.tv_stacked_date, 16f * scale)
        setTextSize(R.id.tv_words_time, 34f * scale)
        setTextSize(R.id.tv_words_date, 16f * scale)
        setTextSize(R.id.tv_analog_date, 16f * scale)

        root.findViewById<AnalogClockView>(R.id.view_analog)?.let { view ->
            view.layoutParams = view.layoutParams.apply {
                height = (BASE_ANALOG_HEIGHT_DP * context.resources.displayMetrics.density * scale).toInt()
            }
        }
        root.requestLayout()
    }

    /** Renders all clock variants from the same instant. */
    fun render(now: Date = Date()) {
        val locale = Locale.getDefault()
        val calendar = Calendar.getInstance().apply { time = now }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val date = SimpleDateFormat("EEEE, d MMMM", locale).format(now).lowercase(locale)
        val time = SimpleDateFormat("HH:mm", locale).format(now)

        setText(R.id.tv_time, time)
        setText(R.id.tv_date, date)
        setText(R.id.tv_stacked_h, String.format(locale, "%02d", hour))
        setText(R.id.tv_stacked_m, String.format(locale, "%02d", minute))
        setText(R.id.tv_stacked_date, date)
        setText(R.id.tv_words_time, formatTimeWords(hour, minute))
        setText(R.id.tv_words_date, date)
        root.findViewById<AnalogClockView>(R.id.view_analog)?.setTime(hour, minute)
        setText(R.id.tv_analog_date, date)
    }

    private fun formatTimeWords(hour: Int, minute: Int): String {
        val hours = context.resources.getStringArray(R.array.clock_words_hours)
        val minutes = context.resources.getStringArray(R.array.clock_words_minutes)
        return "${hours[hour % 12]}\n${minutes[minute]}"
    }

    private fun setTextSize(id: Int, sizeSp: Float) {
        root.findViewById<TextView>(id)?.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    private fun setText(id: Int, value: String) {
        root.findViewById<TextView>(id)?.text = value
    }

    private fun visible(show: Boolean): Int = if (show) View.VISIBLE else View.GONE

    private companion object {
        const val BASE_ANALOG_HEIGHT_DP = 140f
    }
}
