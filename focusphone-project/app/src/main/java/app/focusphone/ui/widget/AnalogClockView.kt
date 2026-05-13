package app.focusphone.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.view.ViewCompat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Minimal analog clock drawn on Canvas.
 * Design: thin hour/minute hands, small hour-mark dots, no second hand.
 * Colors adapt to the current theme via [android.R.attr.textColorPrimary] and secondary.
 */
class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var hours   = 0
    private var minutes = 0

    // ── Paints ─────────────────────────────────────────────────────

    private val paintHourHand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style    = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val paintMinuteHand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style    = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val paintDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paintCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // ── Theme colours (resolved once in onAttachedToWindow) ────────

    private var colorPrimary   = 0xFF1A1A1A.toInt()
    private var colorSecondary = 0xFF888888.toInt()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val ta1 = context.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        colorPrimary = ta1.getColor(0, colorPrimary)
        ta1.recycle()
        val ta2 = context.obtainStyledAttributes(intArrayOf(android.R.attr.textColorSecondary))
        colorSecondary = ta2.getColor(0, colorSecondary)
        ta2.recycle()
    }

    // ── Public API ─────────────────────────────────────────────────

    fun setTime(h: Int, m: Int) {
        hours   = h
        minutes = m
        updateContentDescription()
        invalidate()
    }

    private fun updateContentDescription() {
        val displayHour = if (hours % 12 == 0) 12 else hours % 12
        val amPm = if (hours >= 12) "PM" else "AM"
        val desc = String.format("%02d:%02d %s", displayHour, minutes, amPm)
        ViewCompat.setStateDescription(this, desc)
        contentDescription = desc
    }

    // ── Drawing ────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        val cx     = width  / 2f
        val cy     = height / 2f
        val radius = min(cx, cy) * 0.88f

        // Hour-mark dots (12 tiny dots at each hour position)
        paintDot.color = colorSecondary
        val dotR = radius * 0.028f
        for (i in 0..11) {
            val rad = Math.toRadians(i * 30.0 - 90.0).toFloat()
            val x = cx + (radius * 0.86f * cos(rad))
            val y = cy + (radius * 0.86f * sin(rad))
            canvas.drawCircle(x, y, dotR, paintDot)
        }

        // Hour hand
        val hourRad = Math.toRadians((hours % 12 + minutes / 60.0) * 30.0 - 90.0).toFloat()
        paintHourHand.color       = colorPrimary
        paintHourHand.strokeWidth = radius * 0.055f
        canvas.drawLine(
            cx, cy,
            cx + radius * 0.50f * cos(hourRad),
            cy + radius * 0.50f * sin(hourRad),
            paintHourHand
        )

        // Minute hand
        val minRad = Math.toRadians(minutes * 6.0 - 90.0).toFloat()
        paintMinuteHand.color       = colorPrimary
        paintMinuteHand.strokeWidth = radius * 0.035f
        canvas.drawLine(
            cx, cy,
            cx + radius * 0.72f * cos(minRad),
            cy + radius * 0.72f * sin(minRad),
            paintMinuteHand
        )

        // Center pivot dot
        paintCenter.color = colorPrimary
        canvas.drawCircle(cx, cy, radius * 0.038f, paintCenter)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)
        // Use the smaller non-zero dimension; default to 200dp if both are 0
        val fallback = (200 * resources.displayMetrics.density).toInt()
        val w = if (wSize > 0) wSize else fallback
        val h = if (hSize > 0) hSize else fallback
        val size = min(w, h)
        setMeasuredDimension(size, size)
    }
}
