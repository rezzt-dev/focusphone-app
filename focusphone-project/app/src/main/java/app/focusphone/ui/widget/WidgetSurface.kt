package app.focusphone.ui.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import app.focusphone.R
import app.focusphone.data.preferences.WidgetPreferences
import app.focusphone.ui.theme.FocusPalette
import kotlin.math.roundToInt

/**
 * Owns one visible rendering of FocusPhone's home widget.
 *
 * - [Mode.HOME] renders the live, interactive widget at its real height.
 * - [Mode.PREVIEW] renders the same widget at its home size, scaled down to fit a preview
 *   box, and never reacts to touches.
 *
 * Only one surface at a time can receive a widget's updates (see
 * [AppWidgetHostManager.ownsView]). A surface keeps its view while it still owns it, so
 * coming back to the home screen does not rebuild — and flash — the widget, and rebuilds it
 * only after another surface took it over.
 */
class WidgetSurface(
    context: Context,
    private val mode: Mode,
    private val onVisibilityChanged: () -> Unit = {},
    private val onLongPress: ((View) -> Unit)? = null
) {

    enum class Mode { HOME, PREVIEW }

    private val appContext = context.applicationContext
    private val hostManager = AppWidgetHostManager.getInstance(context)

    private var container: FrameLayout? = null
    private var hostView: HomeWidgetHostView? = null
    private var scaleFrame: WidgetScaleFrame? = null
    private var statusView: TextView? = null
    private var renderedWidgetId = WidgetPreferences.INVALID_WIDGET_ID
    private var renderedInfo: AppWidgetProviderInfo? = null
    private var reportedSize: Pair<Int, Int>? = null

    /** Height in dp the slot shows, overriding the stored one while the user resizes it. */
    private var heightOverrideDp: Int? = null

    /** Resources follow the screen showing the widget (multi-window, font scale). */
    private val resContext: Context get() = container?.context ?: appContext

    /** Restores the configured widget into [target], or renders the empty state. */
    fun restore(target: FrameLayout) {
        if (container !== target) {
            releaseView()
            container = target
        }

        when (val state = hostManager.resolveHomeWidget(appContext)) {
            AppWidgetHostManager.HomeWidget.None -> showEmpty(target)
            is AppWidgetHostManager.HomeWidget.Unavailable -> showUnavailable(target, state.widgetId)
            is AppWidgetHostManager.HomeWidget.Ready -> showReady(target, state.widgetId, state.info)
        }
    }

    /** Detaches this surface so another screen can exclusively render the widget. */
    fun releaseView() {
        detachHostView()
        statusView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        statusView = null
        renderedWidgetId = NO_WIDGET
        renderedInfo = null
        container = null
    }

    /**
     * Shows the widget at [heightDp] without saving it, for live feedback while the user
     * drags a size slider. Pass null to go back to the stored height.
     */
    fun previewHeight(heightDp: Int?) {
        heightOverrideDp = heightDp
        applySize()
    }

    /** Re-reads the stored height and applies it. */
    fun applySize() {
        val view = hostView ?: return
        val info = renderedInfo ?: return
        val heightPx = dpToPx(resolvedHeightDp(info))
        when (mode) {
            Mode.HOME -> view.layoutParams = (view.layoutParams ?: homeParams(heightPx)).apply { height = heightPx }
            Mode.PREVIEW -> scaleFrame?.setNaturalSize(homeWidthPx(), heightPx)
        }
    }

    // ── States ─────────────────────────────────────────────────────

    private fun showReady(target: FrameLayout, widgetId: Int, info: AppWidgetProviderInfo) {
        removeStatus()
        val current = hostView
        val reusable = current != null &&
            widgetId == renderedWidgetId &&
            info.provider == renderedInfo?.provider &&
            hostManager.ownsView(this, widgetId) &&
            current.isInside(target)
        if (reusable) {
            renderedInfo = info
            applySize()
            showContainer(target)
            return
        }

        detachHostView()
        val view = hostManager.createView(this, target.context, widgetId, info)
        if (view == null) {
            Log.w(TAG, "Could not create widget view for id=$widgetId")
            showUnavailable(target, widgetId)
            return
        }
        view.onLongPress = if (mode == Mode.HOME) onLongPress else null
        view.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ -> reportSize(v as AppWidgetHostView) }

        hostView = view
        renderedWidgetId = widgetId
        renderedInfo = info
        reportedSize = null
        try {
            attach(target, view, info)
            showContainer(target)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Could not attach widget view for id=$widgetId", e)
            detachHostView()
            showUnavailable(target, widgetId)
        }
    }

    private fun attach(target: FrameLayout, view: HomeWidgetHostView, info: AppWidgetProviderInfo) {
        val heightPx = dpToPx(resolvedHeightDp(info))
        when (mode) {
            Mode.HOME -> target.addView(view, 0, homeParams(heightPx))

            Mode.PREVIEW -> {
                val frame = scaleFrame ?: WidgetScaleFrame(target.context).also { scaleFrame = it }
                (frame.parent as? ViewGroup)?.removeView(frame)
                frame.addView(view, FrameLayout.LayoutParams(0, 0))
                frame.setNaturalSize(homeWidthPx(), heightPx)
                target.addView(
                    frame,
                    0,
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                )
            }
        }
    }

    /** The provider is missing for now (its app is updating): keep the place, say so quietly. */
    private fun showUnavailable(target: FrameLayout, widgetId: Int) {
        detachHostView()
        renderedWidgetId = widgetId
        renderedInfo = null
        val status = statusView ?: createStatusView(target).also { statusView = it }
        status.setText(R.string.widget_unavailable)
        if (status.parent !== target) {
            (status.parent as? ViewGroup)?.removeView(status)
            target.addView(status, 0)
        }
        target.findViewById<View>(R.id.btn_add_widget)?.visibility = View.GONE
        showContainer(target)
    }

    private fun showEmpty(target: FrameLayout) {
        detachHostView()
        removeStatus()
        renderedWidgetId = NO_WIDGET
        renderedInfo = null
        target.findViewById<View>(R.id.btn_add_widget)?.visibility = View.VISIBLE
        if (mode == Mode.HOME) target.visibility = View.GONE
        onVisibilityChanged()
    }

    private fun showContainer(target: FrameLayout) {
        if (statusView == null) target.findViewById<View>(R.id.btn_add_widget)?.visibility = View.GONE
        target.visibility = View.VISIBLE
        onVisibilityChanged()
    }

    private fun createStatusView(target: FrameLayout): TextView = TextView(target.context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(WidgetSizing.MIN_HEIGHT_DP)
        ).apply { gravity = Gravity.CENTER }
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, STATUS_TEXT_SP)
        setTextColor(FocusPalette.of(target.context).textSecondary)
        letterSpacing = STATUS_LETTER_SPACING
    }

    private fun removeStatus() {
        statusView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        statusView = null
    }

    private fun detachHostView() {
        hostView?.let { view ->
            view.onLongPress = null
            (view.parent as? ViewGroup)?.removeView(view)
        }
        scaleFrame?.let { frame -> (frame.parent as? ViewGroup)?.removeView(frame) }
        hostView = null
        reportedSize = null
        hostManager.releaseViews(this)
    }

    // ── Size ───────────────────────────────────────────────────────

    private fun resolvedHeightDp(info: AppWidgetProviderInfo): Int {
        heightOverrideDp?.let { return WidgetSizing.clampHeightDp(it, WidgetHeights.windowHeightDp(resContext)) }
        return WidgetHeights.currentHeightDp(resContext, info)
    }

    private fun homeParams(heightPx: Int) =
        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)

    /** Width of the widget on the home screen. */
    private fun homeWidthPx(): Int = dpToPx(WidgetHeights.slotWidthDp(resContext).roundToInt())

    /**
     * Tells the provider the size its widget really has, in dp as the API expects.
     *
     * The previous code reported the container size in **pixels**, often before the first
     * layout (0 × 0), so responsive widgets picked their smallest layout or none at all.
     */
    private fun reportSize(view: AppWidgetHostView) {
        if (view.width == 0 || view.height == 0) return
        val density = view.resources.displayMetrics.density
        val widthDp = (view.width / density).roundToInt()
        val heightDp = (view.height / density).roundToInt()
        val size = widthDp to heightDp
        if (size == reportedSize) return
        reportedSize = size
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                view.updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp.toFloat(), heightDp.toFloat())))
            } else {
                @Suppress("DEPRECATION")
                view.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp)
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not report widget size for id=$renderedWidgetId", e)
        }
    }

    private fun View.isInside(target: ViewGroup): Boolean {
        var parent = parent
        while (parent != null) {
            if (parent === target) return true
            parent = parent.parent
        }
        return false
    }

    private fun dpToPx(dp: Int): Int = (dp * resContext.resources.displayMetrics.density).roundToInt()

    private companion object {
        const val TAG = "WidgetSurface"
        const val NO_WIDGET = WidgetPreferences.INVALID_WIDGET_ID
        const val STATUS_TEXT_SP = 14f
        const val STATUS_LETTER_SPACING = 0.02f
    }
}
