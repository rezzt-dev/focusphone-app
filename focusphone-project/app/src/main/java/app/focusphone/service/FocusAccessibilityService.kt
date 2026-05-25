package app.focusphone.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.ui.detox.DetoxBlockedActivity

/**
 * Accessibility service that intercepts app launches at the system level.
 *
 * When an app that is blocked by the active Work or Detox profile comes to the
 * foreground, the service immediately launches [DetoxBlockedActivity] on top of
 * it, preventing interaction with the blocked application.
 *
 * This closes the bypass vector where a user kills FocusPhone and launches
 * apps from notifications, recents, or voice assistants.
 */
class FocusAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Service is ready; no additional configuration required at runtime.
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        val ownPackage = packageName == this.packageName

        // Never block FocusPhone itself.
        if (ownPackage) return

        // Only act when a blocking profile is active to minimize battery impact.
        if (!ProfilePreferences.isDetoxActive(this) && ProfilePreferences.getProfile(this) != ProfilePreferences.PROFILE_WORK) {
            return
        }

        if (ProfilePreferences.isAppBlocked(this, packageName)) {
            val intent = Intent(this, DetoxBlockedActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        // Nothing to clean up.
    }

    companion object {

        /**
         * Returns whether this accessibility service is currently enabled by the user.
         */
        fun isEnabled(context: Context): Boolean {
            val expectedService = "${context.packageName}/${FocusAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabledServices)
            while (splitter.hasNext()) {
                if (splitter.next().equals(expectedService, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        /**
         * Opens the system Accessibility settings so the user can enable the service.
         */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
