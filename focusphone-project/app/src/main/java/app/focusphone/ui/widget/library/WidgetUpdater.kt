package app.focusphone.ui.widget.library

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import app.focusphone.data.preferences.LibraryWidgetPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

/**
 * Draws FocusPhone's widgets and keeps them current.
 *
 * Everything that changes what a widget shows calls [refresh] with the matching
 * [WidgetTrigger]; only the kinds that depend on it, and only if one is placed somewhere,
 * are redrawn. Renders run one at a time so a slow one (artwork decoding) can never land
 * after, and overwrite, a newer one.
 */
object WidgetUpdater {

    private const val TAG = "WidgetUpdater"
    private const val MIDNIGHT_SLACK_MS = 2_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val renderLock = Mutex()

    // ── Rendering ──────────────────────────────────────────────────

    /** Renders [ids] from a provider callback, keeping the broadcast alive meanwhile. */
    fun renderAsync(context: Context, kind: FocusWidgetKind, ids: IntArray, pending: BroadcastReceiver.PendingResult?) {
        val app = context.applicationContext
        scope.launch {
            try {
                render(app, kind, ids)
                scheduleMidnight(app)
                SystemWidgetMonitor.sync(app)
            } finally {
                pending?.finish()
            }
        }
    }

    private suspend fun render(context: Context, kind: FocusWidgetKind, ids: IntArray) {
        if (ids.isEmpty()) return
        val manager = AppWidgetManager.getInstance(context)
        val style = WidgetStyle.resolve(context)
        renderLock.withLock {
            ids.forEach { id ->
                try {
                    val size = WidgetSize.from(context, manager.getAppWidgetOptions(id), kind)
                    val views = kind.renderer.render(WidgetScene(context, id, kind, style, size))
                    manager.updateAppWidget(id, views)
                } catch (e: RuntimeException) {
                    // One broken render (an OEM binder failure, an oversized bitmap) must
                    // not stop the others.
                    Log.e(TAG, "Rendering $kind id=$id failed", e)
                }
            }
        }
    }

    // ── Refresh ────────────────────────────────────────────────────

    /** Redraws every placed widget that depends on [trigger]. */
    fun refresh(context: Context, trigger: WidgetTrigger) {
        refresh(context, FocusWidgetKind.withTrigger(trigger))
    }

    fun refresh(context: Context, kinds: Collection<FocusWidgetKind>) {
        val app = context.applicationContext
        scope.launch {
            kinds.forEach { kind -> render(app, kind, kind.widgetIds(app)) }
        }
    }

    fun refreshAll(context: Context) {
        refresh(context, FocusWidgetKind.entries)
    }

    /**
     * Repaints every widget when the theme, its custom colours, the day/night mode or the
     * language changed since they were last drawn. Cheap enough to call on every return
     * to the home screen.
     */
    fun refreshIfStyleChanged(context: Context) {
        val signature = WidgetStyle.signature(context)
        if (signature == LibraryWidgetPreferences.getStyleSignature(context)) return
        LibraryWidgetPreferences.setStyleSignature(context, signature)
        refreshAll(context)
    }

    // ── Scheduling ─────────────────────────────────────────────────

    /** Re-arms the midnight tick and the system monitors after widgets were added or removed. */
    fun onWidgetsChanged(context: Context) {
        val app = context.applicationContext
        scope.launch {
            scheduleMidnight(app)
            SystemWidgetMonitor.sync(app)
        }
    }

    /**
     * Arms a tick just after the next local midnight while a date-dependent widget is
     * placed. Inexact on purpose: a minute late is invisible, a wake-up is not free.
     */
    fun scheduleMidnight(context: Context) {
        val needed = FocusWidgetKind.withTrigger(WidgetTrigger.DATE).any { it.isPlaced(context) }
        val intent = WidgetActionReceiver.intent(context, WidgetActionReceiver.ACTION_MIDNIGHT)
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        if (!needed) {
            alarms.cancel(intent)
            return
        }
        alarms.set(AlarmManager.RTC, nextMidnight(System.currentTimeMillis()) + MIDNIGHT_SLACK_MS, intent)
    }

    /** Arms a one-off refresh of the profile widget for when a detox session ends. */
    fun scheduleProfileTick(context: Context, atMs: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = WidgetActionReceiver.intent(context, WidgetActionReceiver.ACTION_PROFILE_TICK)
        alarms.set(AlarmManager.RTC, atMs + MIDNIGHT_SLACK_MS, intent)
    }

    internal fun nextMidnight(nowMs: Long): Long = Calendar.getInstance().apply {
        timeInMillis = nowMs
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
