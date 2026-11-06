package app.focusphone.ui.widget.library

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import app.focusphone.data.preferences.LibraryWidgetPreferences

/**
 * Base of every FocusPhone widget.
 *
 * Providers hold no logic of their own: each one only names its [kind], and drawing is
 * delegated to [WidgetUpdater], which is also what the rest of the app calls when
 * something a widget shows has changed. The system needs one provider class per widget
 * in the manifest, hence the one-line subclasses below.
 */
abstract class FocusWidgetProvider : AppWidgetProvider() {

    abstract val kind: FocusWidgetKind

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdater.renderAsync(context, kind, appWidgetIds, goAsync())
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        // Rings, bars and artwork are drawn for the size the launcher gives the widget.
        WidgetUpdater.renderAsync(context, kind, intArrayOf(appWidgetId), goAsync())
    }

    override fun onEnabled(context: Context) {
        WidgetUpdater.onWidgetsChanged(context)
    }

    override fun onDisabled(context: Context) {
        WidgetUpdater.onWidgetsChanged(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        LibraryWidgetPreferences.forget(context, appWidgetIds)
    }
}

// ── Clock & calendar ───────────────────────────────────────────────
class ClockDigitalWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.CLOCK_DIGITAL
}

class ClockAnalogWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.CLOCK_ANALOG
}

class ClockWorldWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.CLOCK_WORLD
}

class DateTileWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.DATE_TILE
}

class CalendarMonthWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.CALENDAR_MONTH
}

class CalendarWeekWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.CALENDAR_WEEK
}

// ── Music ──────────────────────────────────────────────────────────
class MusicBarWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.MUSIC_BAR
}

class MusicCardWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.MUSIC_CARD
}

class MusicSquareWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.MUSIC_SQUARE
}

// ── Navigation ─────────────────────────────────────────────────────
class SearchPillWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.SEARCH_PILL
}

class SpotlightWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.SPOTLIGHT
}

class FavouritesWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.FAVOURITES
}

// ── System ─────────────────────────────────────────────────────────
class BatteryRingWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.BATTERY_RING
}

class StorageWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.STORAGE
}

class SystemMonitorWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.SYSTEM_MONITOR
}

class QuickTogglesWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.QUICK_TOGGLES
}

class NextAlarmWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.NEXT_ALARM
}

// ── Focus ──────────────────────────────────────────────────────────
class FocusProfileWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.FOCUS_PROFILE
}

class IntentionWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.INTENTION
}

class DayProgressWidget : FocusWidgetProvider() {
    override val kind get() = FocusWidgetKind.DAY_PROGRESS
}
