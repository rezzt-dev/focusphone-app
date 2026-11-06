package app.focusphone.data.preferences

import android.content.Context
import androidx.core.content.edit
import app.focusphone.domain.model.HomeGrid
import app.focusphone.domain.model.HomeGridItem
import app.focusphone.domain.util.HomeGridSerializer

/**
 * Synchronous storage of the home grid: its size and where each block (clock, favourite
 * apps, widgets) sits. Read before the first frame of the home screen, like every other
 * layout setting, so it stays in SharedPreferences.
 *
 * Also reads — once — the layout of builds before the grid, which had a fixed column of
 * clock, a single widget and the app list, so it can be carried over.
 */
object HomeGridPreferences {

    private const val PREF_FILE = "focusphone_home_grid"
    private const val KEY_LAYOUT = "layout"

    // Layout of builds before the grid.
    private const val LEGACY_LAYOUT_FILE = "focusphone_home_layout"
    private const val LEGACY_WIDGET_FILE = "focusphone_widget"
    private const val LEGACY_KEY_ORDER = "component_order"
    private const val LEGACY_KEY_CLOCK_ENABLED = "clock_enabled"
    private const val LEGACY_KEY_WIDGET_ENABLED = "widget_enabled"
    private const val LEGACY_KEY_WIDGET_ID = "widget_id"
    private const val LEGACY_KEY_PROVIDER = "widget_provider"
    private const val LEGACY_KEY_HEIGHT_DP = "widget_height_dp"
    private val LEGACY_LAYOUT_KEYS = arrayOf(LEGACY_KEY_ORDER, LEGACY_KEY_CLOCK_ENABLED, LEGACY_KEY_WIDGET_ENABLED)

    private fun prefs(context: Context) = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    /** The stored grid, or null when none was saved yet (or it cannot be read). */
    fun load(context: Context): HomeGrid? = prefs(context).getString(KEY_LAYOUT, null)?.let(HomeGridSerializer::parse)

    fun save(context: Context, grid: HomeGrid) {
        prefs(context).edit { putString(KEY_LAYOUT, HomeGridSerializer.serialize(grid)) }
    }

    fun clear(context: Context) {
        prefs(context).edit { remove(KEY_LAYOUT) }
    }

    // ── Legacy layout ──────────────────────────────────────────────

    /** The pre-grid home: blocks top to bottom, which were shown, and its single widget. */
    data class LegacyLayout(
        val order: List<HomeGridItem.Type>,
        val clockEnabled: Boolean,
        val widgetId: Int,
        val provider: String?,
        /** Height the user gave the widget in dp, or 0 for the provider's own. */
        val widgetHeightDp: Int
    )

    fun readLegacy(context: Context): LegacyLayout {
        val layout = context.getSharedPreferences(LEGACY_LAYOUT_FILE, Context.MODE_PRIVATE)
        val widget = context.getSharedPreferences(LEGACY_WIDGET_FILE, Context.MODE_PRIVATE)
        val order = layout.getString(LEGACY_KEY_ORDER, null).orEmpty()
            .split(",")
            .mapNotNull { HomeGridItem.Type.fromToken(it.trim()) }
            .distinct()
            .ifEmpty { listOf(HomeGridItem.Type.CLOCK, HomeGridItem.Type.WIDGET, HomeGridItem.Type.APPS) }
        val widgetShown = layout.getBoolean(LEGACY_KEY_WIDGET_ENABLED, true) &&
            widget.getBoolean(LEGACY_KEY_WIDGET_ENABLED, false)
        return LegacyLayout(
            order = order,
            clockEnabled = layout.getBoolean(LEGACY_KEY_CLOCK_ENABLED, true),
            widgetId = if (widgetShown) widget.getInt(LEGACY_KEY_WIDGET_ID, HomeGridItem.NO_WIDGET_ID) else HomeGridItem.NO_WIDGET_ID,
            provider = widget.getString(LEGACY_KEY_PROVIDER, null),
            widgetHeightDp = widget.getInt(LEGACY_KEY_HEIGHT_DP, 0)
        )
    }

    /**
     * Deletes the pre-grid layout once it has been carried over. A widget that layout kept
     * hidden is not carried over; the orphan clean-up of the widget host releases its ID.
     */
    fun clearLegacy(context: Context) {
        context.getSharedPreferences(LEGACY_LAYOUT_FILE, Context.MODE_PRIVATE).edit { LEGACY_LAYOUT_KEYS.forEach(::remove) }
        context.getSharedPreferences(LEGACY_WIDGET_FILE, Context.MODE_PRIVATE).edit { clear() }
    }
}
