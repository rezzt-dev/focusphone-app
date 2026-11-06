package app.focusphone.ui.tutorial

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import app.focusphone.R

/**
 * A full-screen overlay that highlights a specific view with a rounded "spotlight" cut-out
 * and displays an anchored tooltip. Used for the interactive contextual tutorial in
 * [app.focusphone.ui.home.HomeActivity].
 *
 * The cut-out hugs the target's own bounds. It used to be a circle as wide as the target's
 * longest side, which for a full-width clock or app list ran off both edges of the screen
 * and lit up half of it. The overlay also swallows every touch outside the tooltip, so the
 * screen underneath cannot be swiped or tapped while a step is explained.
 *
 * The overlay is added directly to the Activity's [android.R.id.content] decor view.
 */
class TutorialOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val density = resources.displayMetrics.density

    private val dimPaint = Paint().apply {
        color = DIM_COLOR
        isAntiAlias = true
    }

    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = PULSE_STROKE_DP * density
        color = Color.WHITE
    }

    private val spotlight = RectF()
    private val pulseBounds = RectF()
    private var cornerRadius = 0f

    /** 0 → 1 along one pulse: the ring grows away from the cut-out while it fades. */
    private var pulseFraction = 0f
    private var pulseAnimator: ValueAnimator? = null

    private lateinit var tooltipView: View
    private var onDismissListener: (() -> Unit)? = null
    private var dismissing = false

    init {
        // The cut-out clears pixels of this view's own layer, so it needs one.
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
        setupTooltip()
    }

    private fun setupTooltip() {
        tooltipView = LayoutInflater.from(context).inflate(R.layout.view_tutorial_tooltip, this, false)
        tooltipView.alpha = 0f
        tooltipView.findViewById<TextView>(R.id.btn_tooltip_action).setOnClickListener {
            dismissWithAnimation()
        }
        addView(tooltipView)
    }

    /**
     * Shows a tutorial step highlighting [targetView] with a spotlight and a tooltip.
     *
     * @param targetView The view to highlight.
     * @param titleRes Title string resource for the tooltip.
     * @param descRes Description string resource for the tooltip.
     * @param onDismiss Called when the user taps "got it".
     */
    fun showStep(
        targetView: View,
        titleRes: Int,
        descRes: Int,
        onDismiss: () -> Unit
    ) {
        this.onDismissListener = onDismiss

        // Measure target position relative to this overlay
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val overlayLocation = IntArray(2)
        getLocationOnScreen(overlayLocation)
        val left = (location[0] - overlayLocation[0]).toFloat()
        val top = (location[1] - overlayLocation[1]).toFloat()

        val padding = SPOTLIGHT_PADDING_DP * density
        val margin = SCREEN_MARGIN_DP * density
        spotlight.set(
            (left - padding).coerceAtLeast(margin),
            (top - padding).coerceAtLeast(margin),
            (left + targetView.width + padding).coerceAtMost(width - margin),
            (top + targetView.height + padding).coerceAtMost(height - margin)
        )
        cornerRadius = minOf(MAX_CORNER_DP * density, spotlight.height() / 2f, spotlight.width() / 2f)

        // Setup tooltip content
        tooltipView.findViewById<TextView>(R.id.tv_tooltip_title).setText(titleRes)
        tooltipView.findViewById<TextView>(R.id.tv_tooltip_desc).setText(descRes)

        positionTooltip()
        startPulseAnimation()
        animateIn()
    }

    private fun positionTooltip() {
        val maxWidth = (width * TOOLTIP_MAX_WIDTH_FRACTION).toInt()
        tooltipView.measure(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
            MeasureSpec.UNSPECIFIED
        )
        val tooltipWidth = tooltipView.measuredWidth
        val tooltipHeight = tooltipView.measuredHeight
        val gap = TOOLTIP_GAP_DP * density
        val margin = SCREEN_MARGIN_DP * density

        // Below the spotlight when it fits, above it otherwise; a target too tall for
        // either keeps the tooltip inside the screen, over the lower part of the target.
        val below = spotlight.bottom + gap
        val above = spotlight.top - gap - tooltipHeight
        val tooltipY = when {
            below + tooltipHeight <= height - margin -> below
            above >= margin -> above
            else -> height - margin - tooltipHeight
        }
        val tooltipX = (spotlight.centerX() - tooltipWidth / 2f)
            .coerceIn(margin, (width - tooltipWidth - margin).coerceAtLeast(margin))

        val params = tooltipView.layoutParams as LayoutParams
        params.leftMargin = tooltipX.toInt()
        params.topMargin = tooltipY.toInt()
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        tooltipView.layoutParams = params
    }

    private fun startPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = PULSE_DURATION_MS
            startDelay = PULSE_START_DELAY_MS
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                pulseFraction = it.animatedValue as Float
                // Without this the ring was computed every frame but never redrawn.
                invalidate()
            }
            start()
        }
    }

    private fun animateIn() {
        alpha = 0f
        animate().alpha(1f).setDuration(FADE_IN_MS).start()
        tooltipView.translationY = TOOLTIP_RISE_DP * density
        tooltipView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(TOOLTIP_IN_MS)
            .setStartDelay(TOOLTIP_DELAY_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun dismissWithAnimation() {
        if (dismissing) return
        dismissing = true
        pulseAnimator?.cancel()
        animate()
            .alpha(0f)
            .setDuration(FADE_OUT_MS)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    (parent as? ViewGroup)?.removeView(this@TutorialOverlayView)
                    onDismissListener?.invoke()
                }
            })
            .start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPaint(dimPaint)
        if (spotlight.isEmpty) return
        canvas.drawRoundRect(spotlight, cornerRadius, cornerRadius, holePaint)

        val grow = PULSE_GROW_DP * density * pulseFraction
        pulseBounds.set(spotlight.left - grow, spotlight.top - grow, spotlight.right + grow, spotlight.bottom + grow)
        pulsePaint.alpha = (PULSE_MAX_ALPHA * (1f - pulseFraction)).toInt()
        canvas.drawRoundRect(pulseBounds, cornerRadius + grow, cornerRadius + grow, pulsePaint)
    }

    /** Swallows touches outside the tooltip: the screen underneath is on hold. */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = true

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
    }

    companion object {

        /**
         * Convenience method to attach a tutorial overlay to an Activity's content view.
         *
         * @param activityContent The root content view (usually [android.R.id.content]).
         * @param targetView The view to highlight.
         * @param titleRes Tooltip title resource.
         * @param descRes Tooltip description resource.
         * @param onDismiss Callback when dismissed.
         */
        fun show(
            activityContent: ViewGroup,
            targetView: View,
            titleRes: Int,
            descRes: Int,
            onDismiss: () -> Unit
        ): TutorialOverlayView {
            // Remove any existing overlay
            val existing = activityContent.findViewWithTag<TutorialOverlayView>(TAG_TUTORIAL_OVERLAY)
            existing?.let { activityContent.removeView(it) }

            val overlay = TutorialOverlayView(activityContent.context)
            overlay.tag = TAG_TUTORIAL_OVERLAY
            activityContent.addView(
                overlay,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Post to ensure layout is ready
            overlay.post {
                if (overlay.isAttachedToWindow) {
                    overlay.showStep(targetView, titleRes, descRes, onDismiss)
                }
            }

            return overlay
        }

        private const val TAG_TUTORIAL_OVERLAY = "focusphone_tutorial_overlay"

        private const val DIM_COLOR = 0xD9000000.toInt() // 85% black
        private const val SPOTLIGHT_PADDING_DP = 10f
        private const val SCREEN_MARGIN_DP = 12f
        private const val MAX_CORNER_DP = 24f
        private const val TOOLTIP_GAP_DP = 20f
        private const val TOOLTIP_RISE_DP = 12f
        private const val TOOLTIP_MAX_WIDTH_FRACTION = 0.85f
        private const val PULSE_STROKE_DP = 2f
        private const val PULSE_GROW_DP = 14f
        private const val PULSE_MAX_ALPHA = 140
        private const val PULSE_DURATION_MS = 1600L
        private const val PULSE_START_DELAY_MS = 300L
        private const val FADE_IN_MS = 300L
        private const val FADE_OUT_MS = 220L
        private const val TOOLTIP_IN_MS = 380L
        private const val TOOLTIP_DELAY_MS = 160L
    }
}
