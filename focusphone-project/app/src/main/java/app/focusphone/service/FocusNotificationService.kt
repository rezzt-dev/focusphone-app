package app.focusphone.service

import android.app.PendingIntent
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import app.focusphone.data.repository.NotificationItem
import app.focusphone.data.repository.NotificationRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Notification listener that maintains an in-memory list of active
 * notifications for the notification panel.
 *
 * Privacy guarantee: notification content (title, text) is kept only in RAM
 * and is never written to disk.
 */
class FocusNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.key?.let { removePendingIntent(it) }
        refresh()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refresh()
    }

    /**
     * Called when access is revoked or the system unbinds the listener. Without a live
     * listener the list can no longer be kept current, so the content of other apps'
     * notifications must not stay on screen (or in memory) either.
     */
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        forgetAll()
    }

    override fun onDestroy() {
        forgetAll()
        super.onDestroy()
    }

    private fun forgetAll() {
        NotificationRepository.clear()
        clearPendingIntents()
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                val key = intent.getStringExtra(EXTRA_KEY)
                if (key != null) {
                    try { cancelNotification(key) } catch (_: Exception) {}
                }
            }
            ACTION_DISMISS_ALL -> {
                try { cancelAllNotifications() } catch (_: Exception) {}
            }
        }
        return START_NOT_STICKY
    }

    private fun refresh() {
        refreshNotifications()
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

                storePendingIntent(sbn.key, sbn.notification.contentIntent)

                NotificationItem(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    appName = appName,
                    title = title,
                    text = text,
                    postTime = sbn.postTime
                )
            }
            // Reconcile: a removal missed while the listener was disconnected must not
            // keep another app's PendingIntent alive for the rest of the process.
            retainPendingIntents(items.mapTo(HashSet()) { it.key })
            NotificationRepository.update(items)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh notification list", e)
        }
    }

    companion object {
        private const val TAG = "FocusNotificationService"

        const val ACTION_DISMISS = "app.focusphone.action.DISMISS_NOTIFICATION"
        const val ACTION_DISMISS_ALL = "app.focusphone.action.DISMISS_ALL_NOTIFICATIONS"
        const val EXTRA_KEY = "notification_key"

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

        // ── PendingIntent cache for opening notifications directly ─────

        /**
         * Content intents of the notifications currently listed. Written from the
         * listener's callbacks and read from the home screen, hence concurrent; it only
         * ever holds entries for notifications that are still active.
         */
        private val pendingIntents = ConcurrentHashMap<String, PendingIntent>()

        fun storePendingIntent(key: String, intent: PendingIntent?) {
            if (intent != null) {
                pendingIntents[key] = intent
            } else {
                pendingIntents.remove(key)
            }
        }

        fun getPendingIntent(key: String): PendingIntent? = pendingIntents[key]

        fun removePendingIntent(key: String) {
            pendingIntents.remove(key)
        }

        /** Drops every entry whose notification is no longer active. */
        private fun retainPendingIntents(activeKeys: Set<String>) {
            pendingIntents.keys.retainAll(activeKeys)
        }

        fun clearPendingIntents() {
            pendingIntents.clear()
        }
    }
}
