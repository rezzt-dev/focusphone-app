package app.focusphone.service

import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import app.focusphone.data.preferences.NotificationBadgePreferences

/**
 * Notification listener that maintains per-app badge counts.
 *
 * Privacy guarantee: only the package name and count are stored.
 * Notification titles, text, or icons are never persisted.
 */
class FocusNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshCounts()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshCounts()
    }

    private fun refreshCounts() {
        try {
            val active = activeNotifications ?: emptyArray()
            val counts = mutableMapOf<String, Int>()
            active.forEach { notification ->
                val pkg = notification.packageName
                counts[pkg] = (counts[pkg] ?: 0) + 1
            }
            NotificationBadgePreferences.setBadgeCounts(this, counts)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh notification counts", e)
        }
    }

    companion object {
        private const val TAG = "FocusNotificationService"

        /** Check whether the user has enabled this notification listener in system settings. */
        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val expected = "${context.packageName}/${FocusNotificationService::class.java.canonicalName}"
            return flat.split(":".toRegex()).any { it.equals(expected, ignoreCase = true) }
        }

        fun openNotificationSettings(context: Context) {
            val intent = android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
