package app.focusphone.data.preferences

import android.content.ComponentName
import android.content.Context
import androidx.core.content.edit

/**
 * Synchronous preferences for the home-screen widget slot.
 *
 * Stores at most one widget to preserve the minimalist philosophy: its ID, the provider it
 * was bound to and the height the user gave it. The provider is kept so a widget whose app
 * is merely being updated (its provider briefly disappears) can be told apart from one whose
 * app was uninstalled.
 */
object WidgetPreferences {

    private const val PREF_FILE = "focusphone_widget"
    private const val KEY_WIDGET_ID = "widget_id"
    private const val KEY_WIDGET_ENABLED = "widget_enabled"
    private const val KEY_WIDGET_PROVIDER = "widget_provider"
    private const val KEY_WIDGET_HEIGHT_DP = "widget_height_dp"
    const val INVALID_WIDGET_ID = -1

    /** Height value meaning "use the provider's own default size". */
    const val HEIGHT_AUTO = 0

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    /**
     * Returns the stored app-widget ID or [INVALID_WIDGET_ID] if none is configured.
     */
    fun getWidgetId(context: Context): Int =
        prefs(context).getInt(KEY_WIDGET_ID, INVALID_WIDGET_ID)

    fun isWidgetEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WIDGET_ENABLED, false)

    /** true when a widget is assigned to the home slot. */
    fun hasWidget(context: Context): Boolean =
        isWidgetEnabled(context) && getWidgetId(context) != INVALID_WIDGET_ID

    /** The provider the home widget was bound to, or null for widgets saved by older builds. */
    fun getProvider(context: Context): ComponentName? =
        prefs(context).getString(KEY_WIDGET_PROVIDER, null)?.let(ComponentName::unflattenFromString)

    /** The height chosen by the user in dp, or [HEIGHT_AUTO] to follow the provider's size. */
    fun getHeightDp(context: Context): Int =
        prefs(context).getInt(KEY_WIDGET_HEIGHT_DP, HEIGHT_AUTO)

    fun setHeightDp(context: Context, heightDp: Int) =
        prefs(context).edit { putInt(KEY_WIDGET_HEIGHT_DP, heightDp.coerceAtLeast(HEIGHT_AUTO)) }

    /**
     * Saves [widgetId] as the home widget. A new widget starts at its provider's own size,
     * so the height chosen for the previous one is reset.
     */
    fun setWidget(context: Context, widgetId: Int, provider: ComponentName?) {
        prefs(context).edit {
            putInt(KEY_WIDGET_ID, widgetId)
            putBoolean(KEY_WIDGET_ENABLED, true)
            if (provider != null) putString(KEY_WIDGET_PROVIDER, provider.flattenToString())
            else remove(KEY_WIDGET_PROVIDER)
            remove(KEY_WIDGET_HEIGHT_DP)
        }
    }

    fun clearWidget(context: Context) {
        prefs(context).edit {
            remove(KEY_WIDGET_ID)
            remove(KEY_WIDGET_PROVIDER)
            remove(KEY_WIDGET_HEIGHT_DP)
            putBoolean(KEY_WIDGET_ENABLED, false)
        }
    }
}
