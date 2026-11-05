package app.focusphone.domain.util

import app.focusphone.domain.model.CellRect
import app.focusphone.domain.model.HomeGrid
import app.focusphone.domain.model.HomeGridItem

/**
 * Pure text format of the home grid, kept out of the preference layer so it can be unit
 * tested without Android.
 *
 * ```
 * grid|1|4|8
 * clock|0|0|4|2
 * widget|0|2|4|1|57|com.example/.ClockWidget
 * apps|0|3|4|5
 * ```
 *
 * The first line holds the format version and the grid size; every other line is one item:
 * type, x, y, spanX, spanY and, for widgets, the widget ID and its provider. A malformed line
 * is skipped instead of discarding the whole layout; [GridLayoutEngine.normalize] repairs
 * whatever survives.
 */
object HomeGridSerializer {

    private const val VERSION = 1
    private const val HEADER = "grid"
    private const val SEPARATOR = "|"
    private const val LINE_SEPARATOR = "\n"
    private const val HEADER_FIELDS = 4
    private const val ITEM_FIELDS = 5
    private const val WIDGET_FIELDS = 7

    fun serialize(grid: HomeGrid): String = buildString {
        append(listOf(HEADER, VERSION, grid.columns, grid.rows).joinToString(SEPARATOR))
        grid.items.forEach { item ->
            append(LINE_SEPARATOR)
            val rect = item.rect
            val fields = mutableListOf<Any>(item.type.token, rect.x, rect.y, rect.spanX, rect.spanY)
            if (item.type == HomeGridItem.Type.WIDGET) {
                fields += item.widgetId
                fields += item.provider.orEmpty().replace(SEPARATOR, "").replace(LINE_SEPARATOR, "")
            }
            append(fields.joinToString(SEPARATOR))
        }
    }

    /** Parses what [serialize] wrote; null when the header itself is unreadable. */
    fun parse(raw: String): HomeGrid? {
        val lines = raw.split(LINE_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
        val header = lines.firstOrNull()?.split(SEPARATOR)?.takeIf { it.size == HEADER_FIELDS && it[0] == HEADER }
        val numbers = header?.drop(1)?.map { it.toIntOrNull() ?: return null } ?: return null
        val (version, columns, rows) = numbers
        if (version > VERSION || columns < 1 || rows < 1) return null
        return HomeGrid(columns, rows, lines.drop(1).mapNotNull(::parseItem))
    }

    private fun parseItem(line: String): HomeGridItem? {
        val parts = line.split(SEPARATOR)
        val type = HomeGridItem.Type.fromToken(parts[0])
        val expected = if (type == HomeGridItem.Type.WIDGET) WIDGET_FIELDS else ITEM_FIELDS
        if (type == null || parts.size != expected) return null
        val numbers = parts.subList(1, ITEM_FIELDS).map { it.toIntOrNull() ?: return null }
        val rect = CellRect(numbers[0], numbers[1], numbers[2], numbers[3])
        if (rect.x < 0 || rect.y < 0 || rect.spanX < 1 || rect.spanY < 1) return null
        if (type != HomeGridItem.Type.WIDGET) return HomeGridItem(type, rect)

        // The platform never hands out 0 (AppWidgetManager.INVALID_APPWIDGET_ID).
        return parts[5].toIntOrNull()?.takeIf { it > 0 }?.let { widgetId ->
            HomeGridItem(type, rect, widgetId, parts[6].ifEmpty { null })
        }
    }
}
