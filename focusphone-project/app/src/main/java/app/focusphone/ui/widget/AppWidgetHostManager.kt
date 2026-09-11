package app.focusphone.ui.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import app.focusphone.data.preferences.WidgetPreferences
import java.util.Collections
import java.util.IdentityHashMap

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
    private val listeningOwners = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())

    /** Starts listening while at least one lifecycle [owner] is visible. */
    @Synchronized
    fun startListening(owner: Any) {
        if (!listeningOwners.add(owner) || listeningOwners.size > 1) return
        try {
            appWidgetHost.startListening()
        } catch (e: RuntimeException) {
            listeningOwners.remove(owner)
            // TransactionTooLargeException or other binder failures from the system
            Log.e(TAG, "AppWidgetHost.startListening() failed — widgets may not update", e)
        }
    }

    /** Stops listening after the last visible lifecycle [owner] leaves. */
    @Synchronized
    fun stopListening(owner: Any) {
        if (!listeningOwners.remove(owner) || listeningOwners.isNotEmpty()) return
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

    // ── Home widget slot ───────────────────────────────────────────

    /**
     * Makes [widgetId] the home-screen widget, releasing the one it replaces.
     *
     * A released ID lets the old provider receive `onDeleted()` and stop scheduling updates
     * for a widget nobody displays.
     */
    fun assignHomeWidget(context: Context, widgetId: Int) {
        val previous = WidgetPreferences.getWidgetId(context)
        WidgetPreferences.setWidgetId(context, widgetId)
        WidgetPreferences.setWidgetEnabled(context, true)
        if (previous != WidgetPreferences.INVALID_WIDGET_ID && previous != widgetId) {
            deleteAppWidgetId(previous)
        }
    }

    /** Removes the home-screen widget and releases its ID back to the system. */
    fun removeHomeWidget(context: Context) {
        val widgetId = WidgetPreferences.getWidgetId(context)
        WidgetPreferences.clearWidget(context)
        if (widgetId != WidgetPreferences.INVALID_WIDGET_ID) {
            deleteAppWidgetId(widgetId)
        }
    }

    /**
     * Releases every ID allocated to this host except the configured home widget.
     *
     * Cleans up IDs leaked by builds that never released removed or replaced widgets.
     * Skipped while the picker is binding a new widget, whose ID is not saved yet.
     */
    fun releaseOrphanedIds(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (WidgetPickerActivity.isSelectionInProgress) return
        val keep = WidgetPreferences.getWidgetId(context)
        val allocated = try {
            appWidgetHost.appWidgetIds
        } catch (e: RuntimeException) {
            Log.e(TAG, "AppWidgetHost.getAppWidgetIds() failed", e)
            return
        }
        allocated.filter { it != keep }.forEach { orphan ->
            Log.i(TAG, "Releasing orphaned widget id=$orphan")
            deleteAppWidgetId(orphan)
        }
    }

    /**
     * Attempts to bind [widgetId] to [provider].
     *
     * Returns `true` if the bind succeeded (or was already bound). Returns `false` if the user
     * must explicitly grant permission via [AppWidgetManager.ACTION_APPWIDGET_BIND].
     */
    fun bindAppWidgetIdIfAllowed(widgetId: Int, provider: ComponentName): Boolean {
        return try {
            appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, provider)
        } catch (e: RuntimeException) {
            Log.e(TAG, "bindAppWidgetIdIfAllowed($widgetId, $provider) failed", e)
            false
        }
    }

    companion object {
        internal const val HOST_ID = 1024
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
