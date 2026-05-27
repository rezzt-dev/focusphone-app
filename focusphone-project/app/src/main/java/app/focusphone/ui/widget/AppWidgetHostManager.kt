package app.focusphone.ui.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.util.Log

/**
 * Singleton manager for the app's [AppWidgetHost].
 *
 * Android requires that a launcher uses **one** [AppWidgetHost] instance with a
 * stable host ID for the lifetime of the process. Creating multiple hosts or
 * calling [startListening] without error handling can trigger
 * `RuntimeException: system server dead?` (often rooted in
 * [android.os.TransactionTooLargeException]).
 *
 * All host operations are wrapped in try/catch so that widget failures never
 * crash the launcher.
 */
class AppWidgetHostManager private constructor(context: Context) {

    private val appWidgetHost = AppWidgetHost(context.applicationContext, HOST_ID)
    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    /** Start listening for widget update broadcasts. Safe to call multiple times. */
    fun startListening() {
        try {
            appWidgetHost.startListening()
        } catch (e: RuntimeException) {
            // TransactionTooLargeException or other binder failures from the system
            Log.e(TAG, "AppWidgetHost.startListening() failed — widgets may not update", e)
        }
    }

    /** Stop listening. Called from [android.app.Activity.onStop]. */
    fun stopListening() {
        try {
            appWidgetHost.stopListening()
        } catch (e: RuntimeException) {
            Log.e(TAG, "AppWidgetHost.stopListening() failed", e)
        }
    }

    /** Returns the [AppWidgetProviderInfo] for the given widget ID, or null. */
    fun getAppWidgetInfo(widgetId: Int): AppWidgetProviderInfo? {
        return try {
            appWidgetManager.getAppWidgetInfo(widgetId)
        } catch (e: RuntimeException) {
            Log.e(TAG, "getAppWidgetInfo($widgetId) failed", e)
            null
        }
    }

    /**
     * Creates an [AppWidgetHostView] for the given widget.
     * Returns null on any system-level failure so the UI can degrade gracefully.
     */
    fun createView(context: Context, widgetId: Int, info: AppWidgetProviderInfo): AppWidgetHostView? {
        return try {
            appWidgetHost.createView(context, widgetId, info)
        } catch (e: RuntimeException) {
            Log.e(TAG, "AppWidgetHost.createView($widgetId) failed", e)
            null
        }
    }

    /** Allocates a new widget ID. */
    fun allocateAppWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    /** Releases a widget ID back to the system. */
    fun deleteAppWidgetId(widgetId: Int) {
        try {
            appWidgetHost.deleteAppWidgetId(widgetId)
        } catch (e: RuntimeException) {
            Log.e(TAG, "AppWidgetHost.deleteAppWidgetId($widgetId) failed", e)
        }
    }

    companion object {
        private const val HOST_ID = 1024
        private const val TAG = "AppWidgetHostManager"

        @Volatile
        private var instance: AppWidgetHostManager? = null

        fun getInstance(context: Context): AppWidgetHostManager {
            return instance ?: synchronized(this) {
                instance ?: AppWidgetHostManager(context).also { instance = it }
            }
        }
    }
}
