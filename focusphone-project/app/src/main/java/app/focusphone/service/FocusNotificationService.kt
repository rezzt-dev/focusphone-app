package app.focusphone.service

import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import app.focusphone.data.preferences.NotificationBadgePreferences
import app.focusphone.data.repository.NotificationItem
import app.focusphone.data.repository.NotificationRepository

/**
 * Notification listener that maintains per-app badge counts and an in-memory
 * list of active notifications for the notification panel.
 *
 * Privacy guarantee: badge counts are persisted. Notification content (title,
 * text) is kept only in RAM and is never written to disk.
 */
class FocusNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refresh()
    }

    private fun refresh() {
        refreshCounts()
        refreshNotifications()
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

    private fun refreshNotifications() {
        try {
            val active = activeNotifications ?: emptyArray()
            val pm = packageManager
            val items = active.map { sbn ->
                val extras = sbn.notification.extras
                val title = extras.getString("android.title")
                    ?: extras.getCharSequence("android.title")?.toString()
                val text = extras.getCharSequence("android.text")?.toString()
                    ?: extras.getCharSequence("android.bigText")?.toString()

                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
                } catch (e: Exception) {
                    sbn.packageName
                }

                NotificationItem(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    appName = appName,
                    title = title,
                    text = text,
                    postTime = sbn.postTime
                )
            }
            NotificationRepository.update(items)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh notification list", e)
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
