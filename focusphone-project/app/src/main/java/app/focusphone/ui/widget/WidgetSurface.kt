package app.focusphone.ui.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
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
import app.focusphone.ui.theme.FocusPalette
import kotlin.math.roundToInt

/**
 * Owns the live rendering of one hosted widget ([widgetId]) inside its block on the home
 * grid. The widget fills the block; the grid decides the block's size.
 *
 * Only one surface at a time can receive a widget's updates (see
 * [AppWidgetHostManager.ownsView]). A surface keeps its view while it still owns it, so
 * coming back to the home screen does not rebuild — and flash — the widget, and rebuilds it
 * only after something else took it over.
 */
class WidgetSurface(
    context: Context,
    val widgetId: Int,
    private val onLongPress: ((View) -> Unit)? = null
) {

    private val appContext = context.applicationContext
    private val hostManager = AppWidgetHostManager.getInstance(context)

    private var container: FrameLayout? = null
    private var hostView: HomeWidgetHostView? = null
    private var statusView: TextView? = null
    private var renderedProvider: ComponentName? = null
    private var reportedSize: Pair<Int, Int>? = null

    /** The provider of the widget as last rendered, or null while it is unavailable. */
    var info: AppWidgetProviderInfo? = null
        private set

    /**
     * Renders the widget into [target]. [provider] is the one the widget was bound to, used
     * to tell an app being updated from an app that was uninstalled. Returns what was found,
     * so the caller can drop a widget whose provider is [AppWidgetHostManager.WidgetState.Gone].
     */
    fun bind(target: FrameLayout, provider: ComponentName?): AppWidgetHostManager.WidgetState {
        if (container !== target) {
            release()
            container = target
        }
        val state = hostManager.resolveWidget(appContext, widgetId, provider)
        when (state) {
            is AppWidgetHostManager.WidgetState.Ready -> showReady(target, state.info)
            AppWidgetHostManager.WidgetState.Unavailable -> showUnavailable(target)
            AppWidgetHostManager.WidgetState.Gone -> release()
        }
        return state
    }

    /** Detaches this surface so the widget can be rendered elsewhere or dropped. */
    fun release() {
        detachHostView()
        removeStatus()
        info = null
        container = null
    }

    // ── States ─────────────────────────────────────────────────────

    private fun showReady(target: FrameLayout, newInfo: AppWidgetProviderInfo) {
        removeStatus()
        val current = hostView
        val reusable = current != null &&
            newInfo.provider == renderedProvider &&
            hostManager.ownsView(this, widgetId) &&
            current.parent === target
        info = newInfo
        if (reusable) return

        detachHostView()
        val view = hostManager.createView(this, target.context, widgetId, newInfo)
        if (view == null) {
            Log.w(TAG, "Could not create widget view for id=$widgetId")
            showUnavailable(target)
            return
        }
        view.onLongPress = onLongPress
        view.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ -> reportSize(v as AppWidgetHostView) }
        try {
            target.addView(view, 0, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            hostView = view
            renderedProvider = newInfo.provider
            reportedSize = null
        } catch (e: RuntimeException) {
            Log.e(TAG, "Could not attach widget view for id=$widgetId", e)
            showUnavailable(target)
        }
    }

    /** The provider is missing for now (its app is updating): keep the place, say so quietly. */
    private fun showUnavailable(target: FrameLayout) {
        detachHostView()
        info = null
        val status = statusView ?: createStatusView(target).also { statusView = it }
        status.setText(R.string.widget_unavailable)
        if (status.parent !== target) {
            (status.parent as? ViewGroup)?.removeView(status)
            target.addView(status, 0)
        }
    }

    private fun createStatusView(target: FrameLayout): TextView = TextView(target.context).apply {
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
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
        hostView = null
        renderedProvider = null
        reportedSize = null
        hostManager.releaseViews(this)
    }

    // ── Size ───────────────────────────────────────────────────────

    /**
     * Tells the provider the size its widget really has, in dp as the API expects, every
     * time the block is resized on the grid, so responsive widgets pick the right layout.
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
            Log.w(TAG, "Could not report widget size for id=$widgetId", e)
        }
    }

    private companion object {
        const val TAG = "WidgetSurface"
        const val STATUS_TEXT_SP = 14f
        const val STATUS_LETTER_SPACING = 0.02f
    }
}
