package app.focusphone.data.preferences

import android.content.Context
import androidx.core.content.edit

/**
 * Synchronous preferences for home-screen gestures.
 * Uses SharedPreferences so values can be read instantly in touch-event handlers.
 */
object GesturePreferences {

    private const val PREF_FILE = "focusphone_gestures"
    private const val KEY_SWIPE_DOWN_NOTIFICATIONS = "swipe_down_notifications"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    /**
     * Returns whether the swipe-down gesture on the home screen should expand
     * the system notification shade. Defaults to `true`.
     */
    fun isSwipeDownEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SWIPE_DOWN_NOTIFICATIONS, true)

    fun setSwipeDownEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_SWIPE_DOWN_NOTIFICATIONS, enabled) }
}
