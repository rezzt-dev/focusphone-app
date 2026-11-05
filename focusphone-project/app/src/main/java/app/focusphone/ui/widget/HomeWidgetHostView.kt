package app.focusphone.ui.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.RemoteViews
import android.widget.TextView
import app.focusphone.R
import app.focusphone.ui.theme.FocusPalette
import kotlin.math.abs

/**
 * The view that renders a widget inside FocusPhone.
 *
 * On top of the platform host view it:
 * - replaces the platform error ("couldn't add widget", in the system's colours) with a
 *   quiet message in the active theme;
 * - fades the content in the first time the provider delivers it, instead of popping;
 * - recognises a long press anywhere on the widget without stealing taps or scrolls from
 *   the widget's own controls, so the home screen can offer the widget's options.
 */
class HomeWidgetHostView(context: Context) : AppWidgetHostView(context) {

    /** Called when the user holds the widget; null disables the gesture (e.g. in previews). */
    var onLongPress: ((View) -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private var downX = 0f
    private var downY = 0f
    private var longPressFired = false
    private var hasRevealed = false
    private var revealOnAttach = false

    private val longPressRunnable = Runnable {
        val listener = onLongPress ?: return@Runnable
        longPressFired = true
        // Children that were tracking the touch (a button, a list) must forget it, or they
        // would fire their own click when the finger lifts.
        cancelChildTouches()
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        listener(this)
    }

    // ── Content ────────────────────────────────────────────────────

    override fun updateAppWidget(remoteViews: RemoteViews?) {
        super.updateAppWidget(remoteViews)
        if (remoteViews != null) reveal()
    }

    /** Fades in once per view, the first time the provider's content arrives. */
    private fun reveal() {
        if (hasRevealed) return
        hasRevealed = true
        if (isAttachedToWindow) fadeIn() else revealOnAttach = true
    }

    private fun fadeIn() {
        alpha = 0f
        animate()
            .alpha(1f)
            .setDuration(resources.getInteger(R.integer.fp_screen_enter_duration).toLong())
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (revealOnAttach) {
            revealOnAttach = false
            fadeIn()
        }
    }

    override fun getErrorView(): View {
        val colors = FocusPalette.of(context)
        return TextView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            gravity = Gravity.CENTER
            setText(R.string.widget_error_load)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, ERROR_TEXT_SP)
            setTextColor(colors.textSecondary)
            letterSpacing = ERROR_LETTER_SPACING
        }
    }

    // ── Long press ─────────────────────────────────────────────────

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        trackLongPress(ev)
        return longPressFired || super.onInterceptTouchEvent(ev)
    }

    // Clicks stay with super.onTouchEvent(), which calls performClick(); this override only
    // watches the gesture for a long press.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Reached when no child of the widget wants the touch (blank areas): keep the
        // gesture alive so a long press there still counts.
        trackLongPress(event)
        if (longPressFired) return true
        return super.onTouchEvent(event) || (onLongPress != null && event.actionMasked == MotionEvent.ACTION_DOWN)
    }

    private fun trackLongPress(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                longPressFired = false
                removeCallbacks(longPressRunnable)
                if (onLongPress != null) postDelayed(longPressRunnable, longPressTimeout)
            }

            MotionEvent.ACTION_MOVE -> {
                if (abs(ev.x - downX) > touchSlop || abs(ev.y - downY) > touchSlop) {
                    removeCallbacks(longPressRunnable)
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> removeCallbacks(longPressRunnable)

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> removeCallbacks(longPressRunnable)
        }
    }

    private fun cancelChildTouches() {
        val now = android.os.SystemClock.uptimeMillis()
        val cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        for (i in 0 until childCount) getChildAt(i).dispatchTouchEvent(cancel)
        cancel.recycle()
        cancelLongPress()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(longPressRunnable)
        animate().cancel()
        alpha = 1f
        super.onDetachedFromWindow()
    }

    private companion object {
        const val ERROR_TEXT_SP = 14f
        const val ERROR_LETTER_SPACING = 0.02f
    }
}
