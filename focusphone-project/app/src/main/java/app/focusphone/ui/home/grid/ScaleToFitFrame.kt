package app.focusphone.ui.home.grid

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * Centres its children and scales down any that need more room than the frame has.
 *
 * Home blocks get whatever size the user gives them on the grid, but a clock style has a
 * natural size of its own (a stacked clock is two lines of 76sp). Text is laid out at its
 * real size — wrapping where it can within the frame's width — and the result is shrunk to
 * fit, instead of being cropped by a small block. Taps keep working on scaled children.
 */
class ScaleToFitFrame @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val innerWidth = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            child.measure(
                MeasureSpec.makeMeasureSpec(innerWidth, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val innerWidth = (right - left - paddingLeft - paddingRight).toFloat()
        val innerHeight = (bottom - top - paddingTop - paddingBottom).toFloat()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            val childLeft = paddingLeft + ((innerWidth - childWidth) / 2f).toInt()
            val childTop = paddingTop + ((innerHeight - childHeight) / 2f).toInt()
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)

            val scale = if (childWidth <= 0 || childHeight <= 0) {
                1f
            } else {
                minOf(1f, innerWidth / childWidth, innerHeight / childHeight).coerceAtLeast(0f)
            }
            child.pivotX = childWidth / 2f
            child.pivotY = childHeight / 2f
            child.scaleX = scale
            child.scaleY = scale
        }
    }
}
