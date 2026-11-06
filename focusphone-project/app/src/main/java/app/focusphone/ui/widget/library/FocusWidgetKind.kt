package app.focusphone.ui.widget.library

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log

/** Events after which a widget has something new to show. */
enum class WidgetTrigger {
    /** The calendar day changed (midnight, time set, time zone, locale). */
    DATE,

    /** Favourite apps were added, removed, renamed or reordered. */
    FAVOURITES,

    /** The focus profile changed or a detox session started or ended. */
    PROFILE,

    /** The playing media session, its track or its playback state changed. */
    MEDIA,

    /** Battery level or charging state changed. */
    BATTERY,

    /** The flashlight was switched on or off. */
    TORCH,

    /** The next scheduled alarm changed. */
    ALARM,

    /** The daily intention was edited. */
    INTENTION
}

/**
 * Every widget FocusPhone ships, with the provider that publishes it, the renderer that
 * draws it and the events that make it stale.
 *
 * The enum is the single registry: refreshing "everything that shows the battery" or
 * "every widget, because the theme changed" is a filter over it, never a hand-kept list.
 */
enum class FocusWidgetKind(
    val providerClass: Class<out FocusWidgetProvider>,
    val renderer: WidgetRenderer,
    val triggers: Set<WidgetTrigger>,
    /** Size used before the host reports one, in dp. */
    val fallbackWidthDp: Int,
    val fallbackHeightDp: Int
) {
    // ── Clock & calendar ───────────────────────────────────────────
    CLOCK_DIGITAL(ClockDigitalWidget::class.java, ClockRenderers.Digital, setOf(WidgetTrigger.DATE, WidgetTrigger.ALARM), 320, 180),
    CLOCK_ANALOG(ClockAnalogWidget::class.java, ClockRenderers.Analog, emptySet(), 170, 170),
    CLOCK_WORLD(ClockWorldWidget::class.java, ClockRenderers.World, setOf(WidgetTrigger.DATE), 320, 96),
    DATE_TILE(DateTileWidget::class.java, ClockRenderers.DateTile, setOf(WidgetTrigger.DATE), 170, 170),
    CALENDAR_MONTH(CalendarMonthWidget::class.java, CalendarRenderers.Month, setOf(WidgetTrigger.DATE), 320, 290),
    CALENDAR_WEEK(CalendarWeekWidget::class.java, CalendarRenderers.Week, setOf(WidgetTrigger.DATE), 320, 96),

    // ── Music ──────────────────────────────────────────────────────
    MUSIC_BAR(MusicBarWidget::class.java, MusicRenderers.Bar, setOf(WidgetTrigger.MEDIA), 320, 96),
    MUSIC_CARD(MusicCardWidget::class.java, MusicRenderers.Card, setOf(WidgetTrigger.MEDIA), 320, 180),
    MUSIC_SQUARE(MusicSquareWidget::class.java, MusicRenderers.Square, setOf(WidgetTrigger.MEDIA), 170, 170),

    // ── Navigation ─────────────────────────────────────────────────
    SEARCH_PILL(SearchPillWidget::class.java, NavigationRenderers.SearchPill, emptySet(), 320, 80),
    SPOTLIGHT(SpotlightWidget::class.java, NavigationRenderers.Spotlight, setOf(WidgetTrigger.FAVOURITES), 320, 180),
    FAVOURITES(FavouritesWidget::class.java, NavigationRenderers.Favourites, setOf(WidgetTrigger.FAVOURITES), 170, 220),

    // ── System ─────────────────────────────────────────────────────
    BATTERY_RING(BatteryRingWidget::class.java, SystemRenderers.BatteryRing, setOf(WidgetTrigger.BATTERY), 170, 170),
    STORAGE(StorageWidget::class.java, SystemRenderers.Storage, setOf(WidgetTrigger.BATTERY), 170, 170),
    SYSTEM_MONITOR(SystemMonitorWidget::class.java, SystemRenderers.Monitor, setOf(WidgetTrigger.BATTERY), 320, 180),
    QUICK_TOGGLES(QuickTogglesWidget::class.java, SystemRenderers.Toggles, setOf(WidgetTrigger.TORCH), 320, 88),
    NEXT_ALARM(NextAlarmWidget::class.java, SystemRenderers.NextAlarm, setOf(WidgetTrigger.ALARM, WidgetTrigger.DATE), 170, 88),

    // ── Focus ──────────────────────────────────────────────────────
    FOCUS_PROFILE(FocusProfileWidget::class.java, FocusRenderers.Profile, setOf(WidgetTrigger.PROFILE), 170, 170),
    INTENTION(IntentionWidget::class.java, FocusRenderers.Intention, setOf(WidgetTrigger.INTENTION), 320, 96),
    DAY_PROGRESS(DayProgressWidget::class.java, FocusRenderers.DayProgress, setOf(WidgetTrigger.DATE), 320, 180);

    fun component(context: Context): ComponentName = ComponentName(context, providerClass)

    /** IDs of this widget placed on any home screen, in any launcher. */
    fun widgetIds(context: Context): IntArray = try {
        AppWidgetManager.getInstance(context).getAppWidgetIds(component(context))
    } catch (e: RuntimeException) {
        Log.w(TAG, "getAppWidgetIds($name) failed", e)
        IntArray(0)
    }

    fun isPlaced(context: Context): Boolean = widgetIds(context).isNotEmpty()

    companion object {
        private const val TAG = "FocusWidgetKind"

        fun withTrigger(trigger: WidgetTrigger): List<FocusWidgetKind> = entries.filter { trigger in it.triggers }

        fun forProvider(className: String): FocusWidgetKind? = entries.firstOrNull { it.providerClass.name == className }
    }
}
