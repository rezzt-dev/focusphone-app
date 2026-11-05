package app.focusphone.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isGone

/**
 * Lays its single child out at a fixed *natural* size and scales it down to fit.
 *
 * Widgets choose their layout from the size they are given, so a preview rendered straight
 * into a small box would show a different (often cropped or "too small") layout than the
 * home screen. Rendering at the real size and scaling the result keeps the preview faithful.
 *
 * The frame is non-interactive: it takes every touch so the widget inside never reacts, and
 * lets the touch fall through to its own parent.
 */
class WidgetScaleFrame @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var naturalWidth = 0
    private var naturalHeight = 0

    /**
     * Called (once per layout that finds it) when the content spills well beyond the box it
     * was rendered in: such a layout was not designed to stand on its own, and a caller may
     * prefer another representation.
     */
    var onContentOverflow: (() -> Unit)? = null

    init {
        clipChildren = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    /** Size in px the child is laid out at before scaling; 0 means "the frame's own size". */
    fun setNaturalSize(widthPx: Int, heightPx: Int) {
        if (widthPx == naturalWidth && heightPx == naturalHeight) return
        naturalWidth = widthPx
        naturalHeight = heightPx
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
        val childWidth = naturalWidth.takeIf { it > 0 } ?: width
        val childHeight = naturalHeight.takeIf { it > 0 } ?: height
        for (i in 0 until childCount) {
            getChildAt(i).measure(
                MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        // Some providers' layouts overflow the box they are given (a fixed-size clock face,
        // for instance). The host must not crop them: the frame scales the whole content in.
        (child as? ViewGroup)?.apply {
            clipChildren = false
            clipToPadding = false
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.layout(0, 0, child.measuredWidth, child.measuredHeight)
            fitContent(child, width, height)
        }
    }

    /**
     * Scales [child] so everything it draws — its own box or, when larger, the bounds of its
     * content — fits the frame, centred.
     */
    private fun fitContent(child: View, width: Int, height: Int) {
        var contentLeft = 0
        var contentTop = 0
        var contentRight = child.width
        var contentBottom = child.height
        if (child is ViewGroup) {
            for (j in 0 until child.childCount) {
                val content = child.getChildAt(j)
                if (content.isGone) continue
                contentLeft = minOf(contentLeft, content.left)
                contentTop = minOf(contentTop, content.top)
                contentRight = maxOf(contentRight, content.right)
                contentBottom = maxOf(contentBottom, content.bottom)
            }
        }
        val contentWidth = contentRight - contentLeft
        val contentHeight = contentBottom - contentTop
        if (contentWidth <= 0 || contentHeight <= 0) return
        if (child.width > 0 && child.height > 0 &&
            (contentWidth > child.width * OVERFLOW_RATIO || contentHeight > child.height * OVERFLOW_RATIO)
        ) {
            onContentOverflow?.let { listener -> post { listener() } }
        }

        val scale = minOf(width.toFloat() / contentWidth, height.toFloat() / contentHeight, 1f)
        child.pivotX = 0f
        child.pivotY = 0f
        child.scaleX = scale
        child.scaleY = scale
        child.translationX = (width - contentWidth * scale) / 2f - contentLeft * scale
        child.translationY = (height - contentHeight * scale) / 2f - contentTop * scale
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = true

    // Not consuming lets the touch reach the parent (e.g. the layout editor's block), which
    // owns the click; this frame never performs one itself.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = false

    private companion object {
        /** Content larger than this share of its box counts as overflowing. */
        const val OVERFLOW_RATIO = 1.25f
    }
}
