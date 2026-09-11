package app.focusphone.ui.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import app.focusphone.R
import app.focusphone.data.preferences.WidgetPreferences

/**
 * Owns one visible rendering of FocusPhone's configured home widget.
 *
 * A surface releases its [AppWidgetHostView] whenever its Activity stops. This guarantees
 * that the home screen and the layout preview never keep two live views for the same ID.
 */
class WidgetSurface(
    context: Context,
    private val emptyBehavior: EmptyBehavior,
    private val onVisibilityChanged: () -> Unit = {}
) {

    enum class EmptyBehavior { HIDE_CONTAINER, SHOW_ADD_BUTTON }

    private val context = context.applicationContext
    private val hostManager = AppWidgetHostManager.getInstance(context)
    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    private var container: FrameLayout? = null
    private var hostView: AppWidgetHostView? = null
    private var restoredWidgetId = WidgetPreferences.INVALID_WIDGET_ID

    /** Restores the configured widget into [target], or renders the empty state. */
    fun restore(target: FrameLayout) {
        if (container !== target) {
            releaseView()
            container = target
        }

        val widgetId = WidgetPreferences.getWidgetId(context)
        val enabled = WidgetPreferences.isWidgetEnabled(context)
        if (!enabled || widgetId == WidgetPreferences.INVALID_WIDGET_ID) {
            showEmpty(target)
            return
        }

        if (widgetId == restoredWidgetId && hostView?.parent === target) {
            showWidget(target)
            return
        }

        releaseView(clearContainer = false)
        val info = hostManager.getAppWidgetInfo(widgetId)
        if (info?.provider == null) {
            showEmpty(target)
            return
        }

        updateOptions(widgetId, target)
        var view = hostManager.createView(target.context, widgetId, info)
        if (view == null && hostManager.bindAppWidgetIdIfAllowed(widgetId, info.provider)) {
            view = hostManager.createView(target.context, widgetId, info)
        }
        if (view == null) {
            Log.w(TAG, "Could not create widget view for id=$widgetId")
            showEmpty(target)
            return
        }

        view.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        hostView = view
        restoredWidgetId = widgetId
        try {
            target.addView(view, 0)
            showWidget(target)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Could not attach widget view for id=$widgetId", e)
            hostView = null
            restoredWidgetId = WidgetPreferences.INVALID_WIDGET_ID
            showEmpty(target)
        }
    }

    /** Detaches this surface so another Activity can exclusively render the widget ID. */
    fun releaseView() = releaseView(clearContainer = true)

    private fun releaseView(clearContainer: Boolean) {
        hostView?.let { view -> (view.parent as? ViewGroup)?.removeView(view) }
        hostView = null
        restoredWidgetId = WidgetPreferences.INVALID_WIDGET_ID
        if (clearContainer) container = null
    }

    private fun showEmpty(target: FrameLayout) {
        releaseView(clearContainer = false)
        target.findViewById<View>(R.id.btn_add_widget)?.visibility = View.VISIBLE
        if (emptyBehavior == EmptyBehavior.HIDE_CONTAINER) target.visibility = View.GONE
        onVisibilityChanged()
    }

    private fun showWidget(target: FrameLayout) {
        target.findViewById<View>(R.id.btn_add_widget)?.visibility = View.GONE
        target.visibility = View.VISIBLE
        onVisibilityChanged()
    }

    private fun updateOptions(widgetId: Int, target: FrameLayout) {
        try {
            appWidgetManager.updateAppWidgetOptions(widgetId, Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, target.width)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, target.width)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, target.height)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, target.height)
            })
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not update widget options for id=$widgetId", e)
        }
    }

    private companion object {
        const val TAG = "WidgetSurface"
    }
}
