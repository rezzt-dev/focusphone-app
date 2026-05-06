package app.focusphone.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorInt

/**
 * Custom view that renders "focusphone" with a progressive weight gradient:
 * the first character is drawn in a thin stroke and each successive character
 * receives slightly more stroke width, creating a thin → bold visual effect.
 *
 * No external font dependencies — uses the system sans-serif-light typeface.
 */
class WeightGradientTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val displayText = "focusphone"

    @ColorInt
    private var textColor: Int = Color.WHITE

    private val textSizePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, 26f, resources.displayMetrics
    )

    /** Maximum extra stroke added to the last character (simulates bold weight). */
    private val maxExtraStroke = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 0.9f, resources.displayMetrics
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize     = textSizePx
        letterSpacing = 0.14f
        isSubpixelText = true
    }

    // ── Public API ─────────────────────────────────────────────────

    fun setTextColor(@ColorInt color: Int) {
        textColor = color
        invalidate()
    }

    // ── Drawing ────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        paint.color = textColor

        // Pre-measure character widths to centre the full string
        val charWidths = displayText.map { paint.measureText(it.toString()) }
        val totalWidth  = charWidths.sum()
        var xOffset     = (width - totalWidth) / 2f
        val baseline    = height / 2f - (paint.ascent() + paint.descent()) / 2f

        val lastIndex = displayText.length - 1
        displayText.forEachIndexed { index, char ->
            val progress    = if (lastIndex > 0) index.toFloat() / lastIndex else 1f
            val strokeWidth = progress * maxExtraStroke
            paint.strokeWidth = strokeWidth
            paint.style = if (strokeWidth > 0f) Paint.Style.FILL_AND_STROKE else Paint.Style.FILL
            canvas.drawText(char.toString(), xOffset, baseline, paint)
            xOffset += charWidths[index]
        }
    }

    // ── Measure ────────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (paint.fontSpacing + paddingTop + paddingBottom + 12f).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }
}
