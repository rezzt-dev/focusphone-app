package app.focusphone.ui.widget.library

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import app.focusphone.ui.allapps.AllAppsActivity
import app.focusphone.ui.home.HomeActivity

/**
 * Every tap target of FocusPhone's widgets.
 *
 * A PendingIntent is identified by its intent without the extras, so each target carries
 * a distinct data URI; otherwise two buttons of the same widget would share — and
 * overwrite — one PendingIntent.
 *
 * Apps are never started from here directly: they go through [WidgetLaunchActivity], which
 * applies the profile block, the schedule and the conscious pause like any other launch.
 * System panels (wifi, volume, storage…) are utilities, not distractions, and open as is.
 */
object WidgetIntents {

    private const val SCHEME = "focusphone-widget"
    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    // ── Apps (guarded) ─────────────────────────────────────────────

    /** Opens [packageName] through FocusPhone's launch guards. */
    fun openApp(context: Context, packageName: String, label: String): PendingIntent =
        activity(
            context,
            WidgetLaunchActivity.forApp(context, packageName, label).setData(uri("app", packageName))
        )

    /**
     * Opens whichever app handles [target] — the clock, the calendar, the music player —
     * through FocusPhone's launch guards. The app is resolved when tapped, so a new
     * default app is picked up without redrawing the widget.
     */
    fun openAppFor(context: Context, key: String, target: Intent): PendingIntent =
        activity(context, WidgetLaunchActivity.forTarget(context, target).setData(uri("resolve", key)))

    fun openClock(context: Context): PendingIntent = openAppFor(context, "clock", Intent(AlarmClock.ACTION_SHOW_ALARMS))

    fun openCalendar(context: Context): PendingIntent =
        openAppFor(context, "calendar", Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR))

    fun openMusicPlayer(context: Context): PendingIntent =
        openAppFor(context, "music", Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC))

    // ── FocusPhone screens ─────────────────────────────────────────

    fun openSearch(context: Context, focusSearch: Boolean): PendingIntent = activity(
        context,
        Intent(context, AllAppsActivity::class.java)
            .putExtra(AllAppsActivity.EXTRA_FOCUS_SEARCH, focusSearch)
            // An open drawer is replaced rather than stacked under a second one.
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setData(uri("search", if (focusSearch) "focus" else "browse"))
    )

    fun openHome(context: Context): PendingIntent =
        activity(context, Intent(context, HomeActivity::class.java).setData(uri("home", "main")))

    fun configureWorldClock(context: Context, widgetId: Int): PendingIntent =
        activity(context, WorldClockConfigActivity.intent(context, widgetId).setData(uri("world", widgetId.toString())))

    fun editIntention(context: Context): PendingIntent =
        activity(context, Intent(context, IntentionConfigActivity::class.java).setData(uri("intention", "edit")))

    // ── System panels ──────────────────────────────────────────────

    fun openSystem(context: Context, panel: SystemPanel): PendingIntent =
        activity(context, panel.intent(), panel.ordinal)

    /** System screens the widgets link to, with a fallback for releases without panels. */
    enum class SystemPanel {
        INTERNET,
        BLUETOOTH,
        VOLUME,
        SETTINGS,
        BATTERY,
        STORAGE,
        NOTIFICATION_ACCESS;

        fun intent(): Intent = when (this) {
            INTERNET -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }

            BLUETOOTH -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)

            VOLUME -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_VOLUME)
            } else {
                Intent(Settings.ACTION_SOUND_SETTINGS)
            }

            SETTINGS -> Intent(Settings.ACTION_SETTINGS)

            BATTERY -> Intent(Intent.ACTION_POWER_USAGE_SUMMARY)

            STORAGE -> Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)

            NOTIFICATION_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
    }

    // ── Broadcasts ─────────────────────────────────────────────────

    /** A button handled in the background, such as play/pause or the flashlight. */
    fun action(context: Context, action: String): PendingIntent = WidgetActionReceiver.intent(context, action)

    // ── Internals ──────────────────────────────────────────────────

    private fun uri(kind: String, value: String): Uri = Uri.Builder().scheme(SCHEME).authority(kind).appendPath(value).build()

    private fun activity(context: Context, intent: Intent, requestCode: Int = 0): PendingIntent {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, requestCode, intent, FLAGS)
    }
}
