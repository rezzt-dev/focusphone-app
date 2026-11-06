package app.focusphone.ui.home.grid

import android.content.ComponentName
import android.content.Context
import android.util.Log
import app.focusphone.data.preferences.HomeGridPreferences
import app.focusphone.domain.model.CellRect
import app.focusphone.domain.model.HomeGrid
import app.focusphone.domain.model.HomeGridItem
import app.focusphone.domain.model.SpanLimits
import app.focusphone.domain.util.GridLayoutEngine
import app.focusphone.ui.widget.AppWidgetHostManager
import kotlin.math.roundToInt

/**
 * The one place the home grid is read and written, shared by the home screen, the widget
 * picker and the settings, so a widget added from any of them lands in the same layout.
 *
 * Reading always returns a valid layout: a stored one is repaired if needed, and the very
 * first read carries over the pre-grid home (clock, one widget, apps in a column).
 *
 * Pass an Activity when one is at hand: on Android 11+ the window it owns gives the exact
 * area of the grid, which spans are computed from.
 */
class HomeGridStore(private val context: Context) {

    private val hostManager = AppWidgetHostManager.getInstance(context)
    private val density = context.resources.displayMetrics.density

    /** The current layout, created on first use. */
    fun load(): HomeGrid {
        val stored = HomeGridPreferences.load(context) ?: return migrate()
        val metrics = HomeGridMetrics.estimate(context, stored.columns, stored.rows)
        val normalized = GridLayoutEngine.normalize(stored, limitsFor(metrics))
        if (normalized != stored) HomeGridPreferences.save(context, normalized)
        return normalized
    }

    fun save(grid: HomeGrid) = HomeGridPreferences.save(context, grid)

    /** IDs of the widgets placed on the home grid; every other ID of the host is an orphan. */
    fun widgetIds(): Set<Int> = load().widgetIds

    /** Providers of the widgets on the grid, to mark them "in use" in the picker. */
    fun providers(): Set<ComponentName> =
        load().items.mapNotNull { item -> item.provider?.let(ComponentName::unflattenFromString) }.toSet()

    /**
     * Limits of each block on a grid measured by [metrics]: widgets follow their provider,
     * the clock and the app list have fixed minimums.
     */
    fun limitsFor(metrics: HomeGridMetrics): (HomeGridItem) -> SpanLimits {
        val cache = mutableMapOf<Int, SpanLimits>()
        return { item ->
            if (item.type != HomeGridItem.Type.WIDGET) {
                GridLayoutEngine.builtInLimits(item.type)
            } else {
                cache.getOrPut(item.widgetId) {
                    hostManager.getAppWidgetInfo(item.widgetId)?.let { metrics.limits(it, density) } ?: SpanLimits.ANY
                }
            }
        }
    }

    /**
     * Places the freshly bound [widgetId] on the grid at its default span, making room by
     * shrinking the app list when needed, and saves the result. Returns null when there is
     * no room even at the widget's smallest size.
     */
    fun addWidget(widgetId: Int, metrics: HomeGridMetrics? = null): HomeGrid? {
        val grid = load()
        val info = hostManager.getAppWidgetInfo(widgetId) ?: return null
        val gridMetrics = metrics ?: HomeGridMetrics.estimate(context, grid.columns, grid.rows)
        val (spanX, spanY) = gridMetrics.defaultSpan(info, density)
        val item = HomeGridItem(HomeGridItem.Type.WIDGET, CellRect(0, 0, spanX, spanY), widgetId, info.provider?.flattenToString())
        val updated = GridLayoutEngine.insert(grid, item, limitsFor(gridMetrics)) ?: return null
        save(updated)
        return updated
    }

    /** Removes the widget [widgetId] from the grid and releases its ID. */
    fun removeWidget(widgetId: Int): HomeGrid {
        val updated = load().without(HomeGridItem.keyOf(HomeGridItem.Type.WIDGET, widgetId))
        save(updated)
        hostManager.deleteAppWidgetId(widgetId)
        return updated
    }

    // ── First run ──────────────────────────────────────────────────

    /**
     * Builds the first grid from the pre-grid home: its blocks in the same order, the clock
     * only if it was shown, and its widget full width at the height the user had given it.
     * On a fresh install this is simply the clock above the favourite list.
     */
    private fun migrate(): HomeGrid {
        val legacy = HomeGridPreferences.readLegacy(context)
        val columns = HomeGrid.DEFAULT_COLUMNS
        val rows = HomeGridMetrics.defaultRows(context, columns)
        val metrics = HomeGridMetrics.estimate(context, columns, rows)
        val blocks = legacy.order.mapNotNull { type ->
            when (type) {
                HomeGridItem.Type.CLOCK -> if (legacy.clockEnabled) HomeGridItem(type, CellRect(0, 0, columns, 1)) else null
                HomeGridItem.Type.APPS -> HomeGridItem(type, CellRect(0, 0, columns, 1))
                HomeGridItem.Type.WIDGET -> legacyWidget(legacy, metrics)
            }
        }.let { found ->
            if (found.any { it.type == HomeGridItem.Type.APPS }) found else found +
                HomeGridItem(HomeGridItem.Type.APPS, CellRect(0, 0, 1, 1))
        }

        val grid = GridLayoutEngine.defaultLayout(columns, rows, blocks, limitsFor(metrics))
        HomeGridPreferences.save(context, grid)
        HomeGridPreferences.clearLegacy(context)
        Log.i(TAG, "Created the home grid ${grid.columns}x${grid.rows} with ${grid.items.size} blocks")
        return grid
    }

    private fun legacyWidget(legacy: HomeGridPreferences.LegacyLayout, metrics: HomeGridMetrics): HomeGridItem? {
        if (legacy.widgetId == HomeGridItem.NO_WIDGET_ID) return null
        val provider = legacy.provider?.let(ComponentName::unflattenFromString)
        val state = hostManager.resolveWidget(context, legacy.widgetId, provider)
        val info = (state as? AppWidgetHostManager.WidgetState.Ready)?.info
        if (state == AppWidgetHostManager.WidgetState.Gone) return null

        val (_, defaultRows) = info?.let { metrics.defaultSpan(it, density) } ?: (metrics.columns to LEGACY_UNAVAILABLE_ROWS)
        val spanY = if (legacy.widgetHeightDp > 0) {
            (legacy.widgetHeightDp / metrics.cellHeightDp).roundToInt().coerceAtLeast(1)
        } else {
            defaultRows
        }
        // The pre-grid widget always spanned the full width of the screen.
        val rect = CellRect(0, 0, metrics.columns, spanY)
        return HomeGridItem(HomeGridItem.Type.WIDGET, rect, legacy.widgetId, info?.provider?.flattenToString() ?: legacy.provider)
    }

    private companion object {
        const val TAG = "HomeGridStore"

        /** Rows kept for a carried-over widget whose provider is momentarily missing. */
        const val LEGACY_UNAVAILABLE_ROWS = 2
    }
}
