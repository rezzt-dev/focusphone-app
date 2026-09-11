package app.focusphone.data.preferences

import androidx.core.content.edit
import android.content.Context

/**
 * Synchronous preferences for the notification panel feature.
 * Uses SharedPreferences so values can be read instantly in touch-event handlers.
 */
object NotificationPanelPreferences {

    private const val PREF_FILE = "focusphone_notification_panel"
    private const val KEY_ENABLED = "notification_panel_enabled"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    /**
     * Returns whether the left-side notification panel is enabled.
     * Defaults to `false`.
     */
    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }
}
