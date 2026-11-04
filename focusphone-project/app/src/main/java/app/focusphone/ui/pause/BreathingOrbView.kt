package app.focusphone.ui.pause

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import app.focusphone.ui.theme.FocusColors
import kotlin.math.min

/**
 * The breathing guide of the conscious pause: a soft disc that swells while the user
 * breathes in and settles while they breathe out, inside a hairline ring that fills as
 * the pause runs out.
 *
 * The view only draws; the screen owns the clock and feeds [breath] and [progress] every
 * frame. Keeping time outside the view means the pause lasts exactly as long as the user
 * chose even when the system's animations are turned off, which would make any
 * animator-driven countdown finish instantly.
 */
class BreathingOrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density

    private val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = TRACK_WIDTH_DP * density
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = PROGRESS_WIDTH_DP * density
        strokeCap = Paint.Cap.ROUND
    }
    private val arcBounds = RectF()

    private var accent = 0

    /** How full the lungs are, from 0 (empty) to 1 (full). */
    var breath: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (clamped != field) {
                field = clamped
                invalidate()
            }
        }

    /** Share of the pause already elapsed, from 0 to 1. */
    var progress: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (clamped != field) {
                field = clamped
                invalidate()
            }
        }

    /** Paints the orb from the active palette: [accent] for the breath, [track] for the ring. */
    fun setColors(accent: Int, track: Int) {
        this.accent = accent
        trackPaint.color = track
        progressPaint.color = accent
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val ringRadius = min(width, height) / 2f - progressPaint.strokeWidth
        if (ringRadius <= 0f) return

        // The disc never vanishes completely: an empty screen between two breaths would
        // read as the pause having stopped.
        val discRadius = ringRadius * (MIN_DISC + (MAX_DISC - MIN_DISC) * breath)
        discPaint.color = FocusColors.withAlpha(accent, MIN_DISC_ALPHA + (MAX_DISC_ALPHA - MIN_DISC_ALPHA) * breath)
        canvas.drawCircle(cx, cy, discRadius, discPaint)

        canvas.drawCircle(cx, cy, ringRadius, trackPaint)
        if (progress > 0f) {
            arcBounds.set(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius)
            canvas.drawArc(arcBounds, START_ANGLE, FULL_SWEEP * progress, false, progressPaint)
        }
    }

    private companion object {
        const val TRACK_WIDTH_DP = 1f
        const val PROGRESS_WIDTH_DP = 2f
        const val MIN_DISC = 0.46f
        const val MAX_DISC = 0.90f
        const val MIN_DISC_ALPHA = 0.06f
        const val MAX_DISC_ALPHA = 0.16f
        const val START_ANGLE = -90f
        const val FULL_SWEEP = 360f
    }
}
