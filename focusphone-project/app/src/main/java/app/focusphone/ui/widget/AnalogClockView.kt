package app.focusphone.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.view.ViewCompat
import app.focusphone.R
import app.focusphone.ui.theme.FocusPalette
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Modern minimalist analog clock drawn on Canvas.
 * Design: outer bezel, 12 radial ticks, balanced hands with tails, hollow pivot cap.
 * Colors adapt dynamically to custom/built-in themes.
 */
class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var hours   = 0
    private var minutes = 0

    // ── Paints ─────────────────────────────────────────────────────

    private val paintBezel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val paintTick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val paintHourHand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style    = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val paintMinuteHand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style    = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val paintCenterOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paintCenterInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // ── Theme colors ────────────────────────────────────────────────

    private var colorPrimary   = 0xFF1A1A1A.toInt()
    private var colorSecondary = 0xFF888888.toInt()
    private var bgColor        = 0xFFFAFAFA.toInt()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateColors()
    }

    /**
     * Resolves the primary/secondary/background colours for the active theme, custom
     * themes included. Called on attach and whenever the host re-themes the view tree.
     */
    fun updateColors() {
        val colors = FocusPalette.of(context)
        colorPrimary = colors.textPrimary
        colorSecondary = colors.textSecondary
        bgColor = colors.background
        invalidate()
    }

    // ── Public API ─────────────────────────────────────────────────

    fun setTime(h: Int, m: Int) {
        hours   = h
        minutes = m
        updateContentDescription()
        invalidate()
    }

    private fun updateContentDescription() {
        val locale = context.resources.configuration.locales[0]
        val desc = if (android.text.format.DateFormat.is24HourFormat(context)) {
            String.format(locale, "%02d:%02d", hours, minutes)
        } else {
            val displayHour = if (hours % 12 == 0) 12 else hours % 12
            val amPm = if (hours >= 12) context.getString(R.string.clock_pm) else context.getString(R.string.clock_am)
            String.format(locale, "%02d:%02d %s", displayHour, minutes, amPm)
        }
        ViewCompat.setStateDescription(this, desc)
        contentDescription = desc
    }

    // ── Drawing ────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        val cx     = width  / 2f
        val cy     = height / 2f
        val radius = min(cx, cy) * 0.88f

        val density = resources.displayMetrics.density

        // 1. Thin outer bezel ring
        paintBezel.color = (colorSecondary and 0x00FFFFFF) or (0x4D shl 24) // ~30% opacity
        paintBezel.strokeWidth = 1.5f * density
        canvas.drawCircle(cx, cy, radius * 0.94f, paintBezel)

        // 2. 12 Radial tick marks
        val tickMajorWidth = 2.5f * density
        val tickMinorWidth = 1.2f * density
        for (i in 0..11) {
            val rad = Math.toRadians(i * 30.0 - 90.0).toFloat()
            val isMajor = (i % 3 == 0)
            val startDist = if (isMajor) radius * 0.80f else radius * 0.86f
            val endDist = radius * 0.90f

            val x1 = cx + startDist * cos(rad)
            val y1 = cy + startDist * sin(rad)
            val x2 = cx + endDist * cos(rad)
            val y2 = cy + endDist * sin(rad)

            paintTick.color = if (isMajor) colorPrimary else (colorSecondary and 0x00FFFFFF) or (0x99 shl 24) // ~60% opacity
            paintTick.strokeWidth = if (isMajor) tickMajorWidth else tickMinorWidth
            canvas.drawLine(x1, y1, x2, y2, paintTick)
        }

        // 3. Hour hand with balanced tail
        val hourRad = Math.toRadians((hours % 12 + minutes / 60.0) * 30.0 - 90.0).toFloat()
        paintHourHand.color       = colorPrimary
        paintHourHand.strokeWidth = radius * 0.05f
        canvas.drawLine(
            cx - radius * 0.12f * cos(hourRad),
            cy - radius * 0.12f * sin(hourRad),
            cx + radius * 0.52f * cos(hourRad),
            cy + radius * 0.52f * sin(hourRad),
            paintHourHand
        )

        // 4. Minute hand with balanced tail
        val minRad = Math.toRadians(minutes * 6.0 - 90.0).toFloat()
        paintMinuteHand.color       = colorPrimary
        paintMinuteHand.strokeWidth = radius * 0.03f
        canvas.drawLine(
            cx - radius * 0.12f * cos(minRad),
            cy - radius * 0.12f * sin(minRad),
            cx + radius * 0.74f * cos(minRad),
            cy + radius * 0.74f * sin(minRad),
            paintMinuteHand
        )

        // 5. Hollow center pivot cap
        paintCenterOuter.color = colorPrimary
        canvas.drawCircle(cx, cy, radius * 0.052f, paintCenterOuter)
        paintCenterInner.color = bgColor
        canvas.drawCircle(cx, cy, radius * 0.026f, paintCenterInner)
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
