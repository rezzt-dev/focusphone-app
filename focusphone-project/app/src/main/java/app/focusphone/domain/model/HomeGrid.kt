package app.focusphone.domain.model

/**
 * A rectangle of cells on the home grid: its top-left cell and how many cells it spans.
 * Right and bottom are exclusive, as in [android.graphics.Rect].
 */
data class CellRect(val x: Int, val y: Int, val spanX: Int, val spanY: Int) {

    val right: Int get() = x + spanX
    val bottom: Int get() = y + spanY
    val area: Int get() = spanX * spanY

    fun overlaps(other: CellRect): Boolean =
        x < other.right && other.x < right && y < other.bottom && other.y < bottom

    fun contains(cellX: Int, cellY: Int): Boolean = cellX in x until right && cellY in y until bottom

    fun fitsIn(columns: Int, rows: Int): Boolean =
        x >= 0 && y >= 0 && spanX >= 1 && spanY >= 1 && right <= columns && bottom <= rows

    fun moveTo(newX: Int, newY: Int): CellRect = copy(x = newX, y = newY)
}

/**
 * Smallest and largest span an item accepts, in cells. A widget that is not resizable along
 * an axis has the same minimum and maximum on it.
 */
data class SpanLimits(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int) {

    fun clampX(spanX: Int): Int = spanX.coerceIn(minX, maxOf(minX, maxX))
    fun clampY(spanY: Int): Int = spanY.coerceIn(minY, maxOf(minY, maxY))

    val resizableX: Boolean get() = maxX > minX
    val resizableY: Boolean get() = maxY > minY

    companion object {
        /** No constraint beyond a single cell. */
        val ANY = SpanLimits(1, 1, Int.MAX_VALUE, Int.MAX_VALUE)
    }
}

/** One block on the home screen: the clock, the favourite apps or a hosted widget. */
data class HomeGridItem(
    val type: Type,
    val rect: CellRect,
    val widgetId: Int = NO_WIDGET_ID,
    /** Flattened `ComponentName` of the widget's provider, kept to tell an update from an uninstall. */
    val provider: String? = null
) {

    enum class Type(val token: String) {
        CLOCK("clock"),
        APPS("apps"),
        WIDGET("widget");

        companion object {
            fun fromToken(token: String): Type? = entries.firstOrNull { it.token == token }
        }
    }

    /** Stable identity of the item inside a layout: there is one clock, one app list, one item per widget ID. */
    val key: String get() = keyOf(type, widgetId)

    companion object {
        const val NO_WIDGET_ID = -1

        const val CLOCK_KEY = "clock"
        const val APPS_KEY = "apps"

        fun keyOf(type: Type, widgetId: Int = NO_WIDGET_ID): String = when (type) {
            Type.CLOCK -> CLOCK_KEY
            Type.APPS -> APPS_KEY
            Type.WIDGET -> "widget:$widgetId"
        }
    }
}

/** The whole home screen: a grid of [columns] × [rows] cells and the items placed on it. */
data class HomeGrid(val columns: Int, val rows: Int, val items: List<HomeGridItem>) {

    fun item(key: String): HomeGridItem? = items.firstOrNull { it.key == key }

    fun has(type: HomeGridItem.Type): Boolean = items.any { it.type == type }

    val widgetIds: Set<Int> get() = items.filter { it.type == HomeGridItem.Type.WIDGET }.mapTo(mutableSetOf()) { it.widgetId }

    fun replace(item: HomeGridItem): HomeGrid = copy(items = items.map { if (it.key == item.key) item else it })

    fun without(key: String): HomeGrid = copy(items = items.filterNot { it.key == key })

    companion object {
        const val MIN_COLUMNS = 3
        const val MAX_COLUMNS = 6
        const val MIN_ROWS = 4
        const val MAX_ROWS = 12
        const val DEFAULT_COLUMNS = 4
    }
}
