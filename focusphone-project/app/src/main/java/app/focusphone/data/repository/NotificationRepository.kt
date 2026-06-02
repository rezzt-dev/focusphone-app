package app.focusphone.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory repository for active system notifications.
 *
 * Privacy: content is held only in RAM. When the app process is killed the list is
 * cleared and repopulated on the next [FocusNotificationService] connection.
 */
object NotificationRepository {

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    fun update(notifications: List<NotificationItem>) {
        _notifications.value = notifications.sortedByDescending { it.postTime }
    }

    fun remove(key: String) {
        _notifications.value = _notifications.value.filter { it.key != key }
    }

    fun clear() {
        _notifications.value = emptyList()
    }
}

/**
 * Domain model for a single notification.
 *
 * @param key Unique key provided by the system (e.g. "pkg|tag|id").
 * @param packageName Package name of the posting app.
 * @param appName Human-readable app name.
 * @param title Notification title (may be null).
 * @param text Notification body text (may be null).
 * @param postTime Epoch millis when the notification was posted.
 */
data class NotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val postTime: Long
)
