package app.focusphone.ui.home.grid

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.widget.FrameLayout
import androidx.core.view.isGone
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The block a hosted widget lives in on the home grid. The widget always covers the whole
 * block, and when its content needs more height than the block has it is laid out on a
 * larger virtual block and scaled down to fit, instead of being cropped.
 *
 * Widget layouts are written in dp and sp, but the cells they get depend on the device:
 * a narrower screen, a larger display zoom or font size (common on Samsung phones) give
 * shorter cells and taller text. Scaling keeps the widget looking the same everywhere —
 * just smaller — the way a launcher's preview would show it.
 */
class WidgetFitFrame(context: Context) : FrameLayout(context) {

    init {
        clipChildren = false
        clipToPadding = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
        val innerWidth = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        val innerHeight = (height - paddingTop - paddingBottom).coerceAtLeast(0)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.isGone) continue
            val scale = if (child is AppWidgetHostView) fitScale(child, innerWidth, innerHeight) else 1f
            child.measure(
                MeasureSpec.makeMeasureSpec((innerWidth / scale).roundToInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec((innerHeight / scale).roundToInt(), MeasureSpec.EXACTLY)
            )
            child.pivotX = 0f
            child.pivotY = 0f
            child.scaleX = scale
            child.scaleY = scale
        }
    }

    /**
     * How much [child] must shrink for its content to fit [height]: its natural height at
     * [width] against the room there is. A small difference is ignored so the scale does not
     * flicker while the provider redraws for its new size.
     */
    private fun fitScale(child: AppWidgetHostView, width: Int, height: Int): Float {
        if (width <= 0 || height <= 0) return 1f
        child.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val natural = child.measuredHeight
        if (natural <= height) return 1f
        val scale = (height.toFloat() / natural).coerceIn(MIN_SCALE, 1f)
        val current = child.scaleY
        return if (abs(current - scale) < SCALE_TOLERANCE && current < 1f) current else scale
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.isGone) continue
            child.layout(paddingLeft, paddingTop, paddingLeft + child.measuredWidth, paddingTop + child.measuredHeight)
        }
    }

    private companion object {
        /** Below this the text would be unreadable; the rest is cropped as before. */
        const val MIN_SCALE = 0.6f
        const val SCALE_TOLERANCE = 0.01f
    }
}
