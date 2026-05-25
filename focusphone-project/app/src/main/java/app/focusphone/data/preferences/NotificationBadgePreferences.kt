package app.focusphone.data.preferences

import android.content.Context

/**
 * Synchronous preference manager for notification badge counts.
 *
 * Stores a simple map of package name → notification count.
 * Data is refreshed by [FocusNotificationService] and read by UI adapters.
 *
 * Only the count is stored; no notification content is persisted,
 * preserving user privacy.
 */
object NotificationBadgePreferences {

    private const val PREF_FILE = "focusphone_notification_badges"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    /** Replace the entire badge map. Called by the notification listener service. */
    fun setBadgeCounts(context: Context, counts: Map<String, Int>) {
        val editor = prefs(context).edit()
        editor.clear()
        counts.forEach { (pkg, count) ->
            if (count > 0) editor.putInt(pkg, count)
        }
        editor.apply()
    }

    /** Get the badge count for a single package. */
    fun getBadgeCount(context: Context, packageName: String): Int {
        return prefs(context).getInt(packageName, 0)
    }

    /** Get all badge counts as a map. */
    fun getAllBadgeCounts(context: Context): Map<String, Int> {
        return prefs(context).all.mapNotNull { (key, value) ->
            if (value is Int && value > 0) key to value else null
        }.toMap()
    }

    /** Clear all badge counts. */
    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
