package app.focusphone.ui.widget.library

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.focusphone.data.preferences.ProfilePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Buttons and ticks of FocusPhone's widgets. Not exported: every intent it handles is a
 * PendingIntent FocusPhone created itself.
 */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            ACTION_MIDNIGHT -> {
                WidgetUpdater.refresh(app, WidgetTrigger.DATE)
                WidgetUpdater.scheduleMidnight(app)
            }

            ACTION_PROFILE_TICK -> {
                val pending = goAsync()
                scope.launch {
                    try {
                        // Reading the profile alone would still report the finished session.
                        ProfilePreferences.expireDetoxIfNeeded(app)
                        WidgetUpdater.refresh(app, WidgetTrigger.PROFILE)
                    } finally {
                        pending.finish()
                    }
                }
            }

            ACTION_MEDIA_PLAY_PAUSE -> MediaWidgetBridge.playPause(app)

            ACTION_MEDIA_NEXT -> MediaWidgetBridge.skipNext(app)

            ACTION_MEDIA_PREVIOUS -> MediaWidgetBridge.skipPrevious(app)

            ACTION_TORCH_TOGGLE -> SystemWidgetMonitor.toggleTorch(app)

            else -> Log.w(TAG, "Unknown widget action ${intent.action}")
        }
    }

    companion object {
        private const val TAG = "WidgetActionReceiver"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        const val ACTION_MIDNIGHT = "app.focusphone.widget.action.MIDNIGHT"
        const val ACTION_PROFILE_TICK = "app.focusphone.widget.action.PROFILE_TICK"
        const val ACTION_MEDIA_PLAY_PAUSE = "app.focusphone.widget.action.MEDIA_PLAY_PAUSE"
        const val ACTION_MEDIA_NEXT = "app.focusphone.widget.action.MEDIA_NEXT"
        const val ACTION_MEDIA_PREVIOUS = "app.focusphone.widget.action.MEDIA_PREVIOUS"
        const val ACTION_TORCH_TOGGLE = "app.focusphone.widget.action.TORCH_TOGGLE"

        fun intent(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, WidgetActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

/**
 * System events that change what the widgets show. Exported because the system sends
 * them, but each action is a protected broadcast only the system may send.
 */
class WidgetSystemReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                WidgetUpdater.refresh(app, WidgetTrigger.DATE)
                WidgetUpdater.scheduleMidnight(app)
            }

            // Month and weekday names, and the theme signature, are language dependent.
            Intent.ACTION_LOCALE_CHANGED -> WidgetUpdater.refreshIfStyleChanged(app)

            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED -> WidgetUpdater.refresh(app, WidgetTrigger.ALARM)

            // A new build may draw differently; repaint everything once.
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                WidgetUpdater.refreshAll(app)
                WidgetUpdater.onWidgetsChanged(app)
            }
        }
    }
}
