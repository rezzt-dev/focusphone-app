package app.focusphone.data.preferences

import android.content.Context
import androidx.core.content.edit

/**
 * Synchronous preferences for the home-screen widget container.
 * Stores at most one widget ID to preserve the minimalist philosophy.
 */
object WidgetPreferences {

    private const val PREF_FILE = "focusphone_widget"
    private const val KEY_WIDGET_ID = "widget_id"
    private const val KEY_WIDGET_ENABLED = "widget_enabled"
    const val INVALID_WIDGET_ID = -1

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    /**
     * Returns the stored app-widget ID or [INVALID_WIDGET_ID] if none is configured.
     */
    fun getWidgetId(context: Context): Int =
        prefs(context).getInt(KEY_WIDGET_ID, INVALID_WIDGET_ID)

    fun setWidgetId(context: Context, id: Int) =
        prefs(context).edit { putInt(KEY_WIDGET_ID, id) }

    fun isWidgetEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WIDGET_ENABLED, false)

    fun setWidgetEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_WIDGET_ENABLED, enabled) }

    fun clearWidget(context: Context) {
        prefs(context).edit {
            remove(KEY_WIDGET_ID)
            putBoolean(KEY_WIDGET_ENABLED, false)
        }
    }
}
