package app.focusphone.ui.settings

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import app.focusphone.R
import app.focusphone.data.preferences.GesturePreferences
import app.focusphone.data.preferences.NotificationPanelPreferences
import app.focusphone.service.FocusNotificationService

/**
 * Notifications screen: notification access, notification panel toggle and
 * swipe-down gesture toggle.
 */
class NotificationSettingsActivity : SettingsSubActivity() {

    private var notificationAccessValueView: TextView? = null

    override fun getScreenTitleRes(): Int = R.string.notification_settings_title

    override fun buildContent(container: LinearLayout) {
        container.settingsSection(R.string.settings_section_notifications) {
            // Notification access (system permission)
            notificationAccessValueView = addValueRow(
                titleResId = R.string.settings_notification_access_title,
                subtitleResId = R.string.settings_notification_access_desc,
                initialValue = getNotificationAccessStatusText(),
                iconResId = R.drawable.ic_settings_badge,
                onClick = {
                    FocusNotificationService.openNotificationSettings(this@NotificationSettingsActivity)
                }
            )

            addSwitchRow(
                titleResId = R.string.settings_notification_panel_title,
                subtitleResId = R.string.settings_notification_panel_desc_short,
                isChecked = NotificationPanelPreferences.isEnabled(this@NotificationSettingsActivity),
                iconResId = R.drawable.ic_settings_panel,
                onCheckedChanged = { checked ->
                    NotificationPanelPreferences.setEnabled(this@NotificationSettingsActivity, checked)
                }
            )

            addSwitchRow(
                titleResId = R.string.settings_swipe_down_title,
                subtitleResId = R.string.settings_swipe_down_desc,
                isChecked = GesturePreferences.isSwipeDownEnabled(this@NotificationSettingsActivity),
                iconResId = R.drawable.ic_settings_swipe,
                onCheckedChanged = { checked ->
                    GesturePreferences.setSwipeDownEnabled(this@NotificationSettingsActivity, checked)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        notificationAccessValueView?.text = getNotificationAccessStatusText()
    }

    private fun getNotificationAccessStatusText(): String =
        getString(
            if (FocusNotificationService.isEnabled(this)) R.string.settings_notification_access_active
            else R.string.settings_notification_access_inactive
        ).lowercase()
}
