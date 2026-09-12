package app.focusphone.ui.tutorial

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import app.focusphone.R

/**
 * A full-screen overlay that highlights a specific view with a circular "spotlight" cut-out
 * and displays an anchored tooltip. Used for the interactive contextual tutorial in
 * [app.focusphone.ui.home.HomeActivity].
 *
 * The overlay is added directly to the Activity's [android.R.id.content] decor view.
 */
class TutorialOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val dimPaint = Paint().apply {
        color = 0xE6000000.toInt() // 90% black
        isAntiAlias = true
    }

    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
        color = 0x66FFFFFF
    }

    private var targetCenterX = 0f
    private var targetCenterY = 0f
    private var targetRadius = 0f
    private var pulseRadius = 0f
    private var pulseAnimator: ValueAnimator? = null

    private lateinit var tooltipView: View
    private var onDismissListener: (() -> Unit)? = null
    private var stepIndex = 0

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setWillNotDraw(false)
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
     * Shows a tutorial step highlighting [targetView] with a circular spotlight and a tooltip.
     *
     * @param targetView The view to highlight.
     * @param titleRes Title string resource for the tooltip.
     * @param descRes Description string resource for the tooltip.
     * @param step Current step index (0-based).
     * @param onDismiss Called when the user taps "got it".
     */
    fun showStep(
        targetView: View,
        titleRes: Int,
        descRes: Int,
        step: Int,
        onDismiss: () -> Unit
    ) {
        this.stepIndex = step
        this.onDismissListener = onDismiss

        // Measure target position relative to this overlay
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val overlayLocation = IntArray(2)
        getLocationOnScreen(overlayLocation)

        val relativeX = (location[0] - overlayLocation[0]).toFloat()
        val relativeY = (location[1] - overlayLocation[1]).toFloat()

        targetCenterX = relativeX + targetView.width / 2f
        targetCenterY = relativeY + targetView.height / 2f
        targetRadius = kotlin.math.max(targetView.width, targetView.height) / 2f + dp(12)
        pulseRadius = targetRadius

        // Setup tooltip content
        tooltipView.findViewById<TextView>(R.id.tv_tooltip_title).setText(titleRes)
        tooltipView.findViewById<TextView>(R.id.tv_tooltip_desc).setText(descRes)

        // Position tooltip
        positionTooltip()

        // Start animations
        startPulseAnimation()
        animateIn()
    }

    private fun positionTooltip() {
        tooltipView.measure(
            MeasureSpec.makeMeasureSpec((resources.displayMetrics.widthPixels * 0.85).toInt(), MeasureSpec.AT_MOST),
            MeasureSpec.UNSPECIFIED
        )

        val tooltipWidth = tooltipView.measuredWidth
        val tooltipHeight = tooltipView.measuredHeight
        val screenHeight = resources.displayMetrics.heightPixels

        // Default: place tooltip below target with horizontal centering
        var tooltipX = (targetCenterX - tooltipWidth / 2f).toInt()
        var tooltipY = (targetCenterY + targetRadius + dp(24)).toInt()

        // If tooltip would go off bottom, place it above the target
        if (tooltipY + tooltipHeight > screenHeight - dp(40)) {
            tooltipY = (targetCenterY - targetRadius - tooltipHeight - dp(24)).toInt()
        }

        // Clamp horizontally
        tooltipX = tooltipX.coerceIn(dp(16), resources.displayMetrics.widthPixels - tooltipWidth - dp(16))

        val params = tooltipView.layoutParams as FrameLayout.LayoutParams
        params.leftMargin = tooltipX
        params.topMargin = tooltipY
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        tooltipView.layoutParams = params
    }

    private fun startPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(targetRadius, targetRadius * 1.4f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = DecelerateInterpolator()
            addUpdateListener { pulseRadius = it.animatedValue as Float }
            start()
        }
    }

    private fun animateIn() {
        alpha = 0f
        animate().alpha(1f).setDuration(300).start()
        tooltipView.animate().alpha(1f).setDuration(400).setStartDelay(200).start()
    }

    private fun dismissWithAnimation() {
        pulseAnimator?.cancel()
        animate()
            .alpha(0f)
            .setDuration(250)
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
        // Draw dim background
        canvas.drawPaint(dimPaint)
        // Draw spotlight hole
        canvas.drawCircle(targetCenterX, targetCenterY, targetRadius, holePaint)
        // Draw pulse ring
        canvas.drawCircle(targetCenterX, targetCenterY, pulseRadius, pulsePaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {

        /**
         * Convenience method to attach a tutorial overlay to an Activity's content view.
         *
         * @param activityContent The root content view (usually [android.R.id.content]).
         * @param targetView The view to highlight.
         * @param titleRes Tooltip title resource.
         * @param descRes Tooltip description resource.
         * @param step Current step index.
         * @param onDismiss Callback when dismissed.
         */
        fun show(
            activityContent: ViewGroup,
            targetView: View,
            titleRes: Int,
            descRes: Int,
            step: Int,
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
                    overlay.showStep(targetView, titleRes, descRes, step, onDismiss)
                }
            }

            return overlay
        }

        private const val TAG_TUTORIAL_OVERLAY = "focusphone_tutorial_overlay"
    }
}
