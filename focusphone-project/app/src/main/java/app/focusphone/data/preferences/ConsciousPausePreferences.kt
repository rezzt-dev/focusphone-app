package app.focusphone.data.preferences

import android.content.Context
import androidx.core.content.edit

/**
 * Synchronous preference manager for Conscious Pause configurations.
 */
object ConsciousPausePreferences {

    private const val PREF_FILE    = "focusphone_conscious_pause"
    private const val KEY_ENABLED  = "pause_enabled"
    private const val KEY_DURATION = "pause_duration" // in seconds, from 3 to 10

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false) // Default is disabled so it is opt-in

    fun setEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }

    fun getDuration(context: Context): Int =
        prefs(context).getInt(KEY_DURATION, 4) // Default is 4 seconds

    fun setDuration(context: Context, duration: Int) =
        prefs(context).edit { putInt(KEY_DURATION, duration) }
}
