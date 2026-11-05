package app.focusphone.ui.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.SparseArray
import androidx.core.util.size
import app.focusphone.data.preferences.HomeLayoutPreferences
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
 * The host delivers a widget's updates to **one** view per ID. The manager therefore
 * remembers which surface created the live view of each ID, so the home screen keeps its
 * view across visits and only rebuilds it after another surface (the layout preview) took
 * the ID over.
 *
 * All host operations are wrapped in try/catch so that widget failures never
 * crash the launcher.
 */
class AppWidgetHostManager private constructor(context: Context) {

    /** State of the home widget slot as a surface should render it. */
    sealed interface HomeWidget {
        /** No widget is assigned. */
        data object None : HomeWidget

        /** A widget is assigned and its provider is available. */
        data class Ready(val widgetId: Int, val info: AppWidgetProviderInfo) : HomeWidget

        /** A widget is assigned but its provider is momentarily missing (e.g. being updated). */
        data class Unavailable(val widgetId: Int) : HomeWidget
    }

    private val appContext = context.applicationContext
    private val appWidgetHost = FocusAppWidgetHost(appContext, HOST_ID) { notifyProvidersChanged() }
    private val appWidgetManager = AppWidgetManager.getInstance(appContext)
    private val listeningOwners = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
    private val viewOwners = SparseArray<Any>()
    private val providerListeners = Collections.newSetFromMap(IdentityHashMap<() -> Unit, Boolean>())

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
    fun getAppWidgetInfo(widgetId: Int): AppWidgetProviderInfo? = try {
        appWidgetManager.getAppWidgetInfo(widgetId)
    } catch (e: RuntimeException) {
        Log.e(TAG, "getAppWidgetInfo($widgetId) failed", e)
        null
    }

    /** Every widget provider that may be placed on a home screen, or an empty list on failure. */
    fun getHomeScreenProviders(): List<AppWidgetProviderInfo> = try {
        appWidgetManager.installedProviders.filter { info ->
            info.provider != null &&
                info.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0 &&
                !(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                        info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_HIDE_FROM_PICKER != 0
                    )
        }
    } catch (e: RuntimeException) {
        Log.e(TAG, "getInstalledProviders() failed", e)
        emptyList()
    }

    /**
     * Creates the live view of [widgetId] on behalf of [owner], which becomes the only
     * surface receiving its updates. Returns null on any system-level failure so the UI can
     * degrade gracefully.
     */
    fun createView(owner: Any, context: Context, widgetId: Int, info: AppWidgetProviderInfo): HomeWidgetHostView? {
        val view = try {
            appWidgetHost.createView(WidgetHostContext(context), widgetId, info) as? HomeWidgetHostView
        } catch (e: RuntimeException) {
            Log.e(TAG, "AppWidgetHost.createView($widgetId) failed", e)
            null
        }
        if (view != null) viewOwners.put(widgetId, owner) else viewOwners.remove(widgetId)
        return view
    }

    /** true while the view [owner] created for [widgetId] still receives its updates. */
    fun ownsView(owner: Any, widgetId: Int): Boolean = viewOwners.get(widgetId) === owner

    /** Forgets [owner] as the renderer of any widget, e.g. when its screen is destroyed. */
    fun releaseViews(owner: Any) {
        for (i in viewOwners.size - 1 downTo 0) {
            if (viewOwners.valueAt(i) === owner) viewOwners.removeAt(i)
        }
    }

    /** Allocates a new widget ID. */
    fun allocateAppWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    /** Releases a widget ID back to the system. */
    fun deleteAppWidgetId(widgetId: Int) {
        viewOwners.remove(widgetId)
        try {
            appWidgetHost.deleteAppWidgetId(widgetId)
        } catch (e: RuntimeException) {
            Log.e(TAG, "AppWidgetHost.deleteAppWidgetId($widgetId) failed", e)
        }
    }

    // ── Provider changes ───────────────────────────────────────────

    /** Registers [listener] to run on the main thread whenever installed providers change. */
    fun addProvidersChangedListener(listener: () -> Unit) {
        providerListeners.add(listener)
    }

    fun removeProvidersChangedListener(listener: () -> Unit) {
        providerListeners.remove(listener)
    }

    private fun notifyProvidersChanged() {
        providerListeners.toList().forEach { it() }
    }

    // ── Home widget slot ───────────────────────────────────────────

    /**
     * Makes [widgetId] the home-screen widget, releasing the one it replaces.
     *
     * Choosing a widget is an explicit wish to see it, so the widget block of the home
     * layout is switched back on too: it used to stay hidden when it had been turned off
     * in the layout editor, and the new widget never appeared.
     *
     * A released ID lets the old provider receive `onDeleted()` and stop scheduling updates
     * for a widget nobody displays.
     */
    fun assignHomeWidget(context: Context, widgetId: Int) {
        val previous = WidgetPreferences.getWidgetId(context)
        WidgetPreferences.setWidget(context, widgetId, getAppWidgetInfo(widgetId)?.provider)
        HomeLayoutPreferences.setWidgetEnabled(context, true)
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
     * Resolves what the home slot should show.
     *
     * A provider that is missing because its app was uninstalled clears the slot for good.
     * One that is missing while its app is still installed — mid-update, or on an SD card
     * being mounted — is reported as [HomeWidget.Unavailable] and keeps its place, so an
     * app update never silently removes the user's widget.
     */
    fun resolveHomeWidget(context: Context): HomeWidget {
        if (!WidgetPreferences.hasWidget(context)) return HomeWidget.None
        val widgetId = WidgetPreferences.getWidgetId(context)
        val info = getAppWidgetInfo(widgetId)
        if (info?.provider != null) return HomeWidget.Ready(widgetId, info)

        val provider = WidgetPreferences.getProvider(context)
        if (provider != null && isPackageInstalled(context, provider.packageName)) {
            return HomeWidget.Unavailable(widgetId)
        }
        Log.i(TAG, "Home widget id=$widgetId lost its provider $provider; clearing the slot")
        removeHomeWidget(context)
        return HomeWidget.None
    }

    /** The provider of the home widget, when one is assigned and available. */
    fun homeWidgetInfo(context: Context): AppWidgetProviderInfo? =
        (resolveHomeWidget(context) as? HomeWidget.Ready)?.info

    private fun isPackageInstalled(context: Context, packageName: String): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
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
    fun bindAppWidgetIdIfAllowed(widgetId: Int, provider: ComponentName): Boolean = try {
        appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, provider)
    } catch (e: RuntimeException) {
        Log.e(TAG, "bindAppWidgetIdIfAllowed($widgetId, $provider) failed", e)
        false
    }

    /** true once [widgetId] is bound to a provider, however the bind was granted. */
    fun isBound(widgetId: Int): Boolean = getAppWidgetInfo(widgetId)?.provider != null

    /**
     * Opens the provider's configuration screen for [widgetId].
     *
     * Goes through [AppWidgetHost.startAppWidgetConfigureActivityForResult], which launches
     * the screen on the host's behalf: configuration activities are often not exported,
     * and starting them with a plain intent failed with a `SecurityException`.
     *
     * @return false when the screen could not be opened.
     */
    fun startConfigureActivity(activity: Activity, widgetId: Int, requestCode: Int): Boolean = try {
        appWidgetHost.startAppWidgetConfigureActivityForResult(activity, widgetId, 0, requestCode, null)
        true
    } catch (e: RuntimeException) {
        // ActivityNotFoundException and SecurityException are both RuntimeExceptions.
        Log.w(TAG, "Could not open the configuration of widget id=$widgetId", e)
        false
    }

    /** Host whose views are [HomeWidgetHostView]s and which reports provider changes. */
    private class FocusAppWidgetHost(
        context: Context,
        hostId: Int,
        private val onProvidersChanged: () -> Unit
    ) : AppWidgetHost(context, hostId) {

        override fun onCreateView(
            context: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?
        ): AppWidgetHostView = HomeWidgetHostView(context)

        override fun onProvidersChanged() {
            onProvidersChanged.invoke()
        }
    }

    companion object {
        internal const val HOST_ID = 1024
        private const val TAG = "AppWidgetHostManager"

        @Volatile
        private var instance: AppWidgetHostManager? = null

        fun getInstance(context: Context): AppWidgetHostManager = instance ?: synchronized(this) {
            instance ?: AppWidgetHostManager(context).also { instance = it }
        }
    }
}
