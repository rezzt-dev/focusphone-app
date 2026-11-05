package app.focusphone.domain.util

import app.focusphone.domain.model.CellRect
import app.focusphone.domain.model.HomeGrid
import app.focusphone.domain.model.HomeGridItem
import app.focusphone.domain.model.SpanLimits
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure layout maths of the home grid, the same job `CellLayout` does in a stock launcher:
 * finding free room, moving and resizing an item while the items it would cover make way,
 * fitting a layout onto a grid of another size and adding a new item where there is room.
 *
 * Every operation returns a new [HomeGrid] — or null when the request cannot be satisfied —
 * and never leaves two items overlapping or an item outside the grid.
 */
object GridLayoutEngine {

    /** Rows the clock takes in a fresh layout. */
    const val DEFAULT_CLOCK_ROWS = 2

    /** Smallest clock: two columns so the time fits on one line, one row. */
    val CLOCK_LIMITS = SpanLimits(minX = 2, minY = 1, maxX = Int.MAX_VALUE, maxY = Int.MAX_VALUE)

    /** Smallest favourite list: two columns and two rows still show a couple of names. */
    val APPS_LIMITS = SpanLimits(minX = 2, minY = 2, maxX = Int.MAX_VALUE, maxY = Int.MAX_VALUE)

    private enum class Direction { DOWN, UP, RIGHT, LEFT }

    /** Order in which a person reads the grid: rows top to bottom, then columns. */
    private val READING_ORDER = compareBy<HomeGridItem>({ it.rect.y }, { it.rect.x })

    // ── Checks ─────────────────────────────────────────────────────

    /** true when every item lies inside the grid and no two items share a cell. */
    fun isValid(grid: HomeGrid): Boolean {
        if (grid.items.any { !it.rect.fitsIn(grid.columns, grid.rows) }) return false
        if (grid.items.map { it.key }.distinct().size != grid.items.size) return false
        return grid.items.indices.none { i ->
            (i + 1 until grid.items.size).any { j -> grid.items[i].rect.overlaps(grid.items[j].rect) }
        }
    }

    /** Number of cells no item covers. */
    fun freeCells(grid: HomeGrid): Int = grid.columns * grid.rows - grid.items.sumOf { it.rect.area }

    // ── Free room ──────────────────────────────────────────────────

    /**
     * A free rectangle of [spanX] × [spanY] cells among [occupied] ones: the one closest to
     * [near] when given, otherwise the first in reading order. null when none exists.
     */
    fun findVacant(
        columns: Int,
        rows: Int,
        occupied: List<CellRect>,
        spanX: Int,
        spanY: Int,
        near: CellRect? = null
    ): CellRect? {
        if (spanX < 1 || spanY < 1 || spanX > columns || spanY > rows) return null
        var best: CellRect? = null
        var bestScore = Int.MAX_VALUE
        for (y in 0..rows - spanY) {
            for (x in 0..columns - spanX) {
                val candidate = CellRect(x, y, spanX, spanY)
                if (occupied.any { it.overlaps(candidate) }) continue
                if (near == null) return candidate
                val dx = x - near.x
                val dy = y - near.y
                val score = dx * dx + dy * dy
                if (score < bestScore) {
                    best = candidate
                    bestScore = score
                }
            }
        }
        return best
    }

    // ── Move & resize ──────────────────────────────────────────────

    /**
     * Moves and/or resizes the item [key] to [target]. Items it would cover are moved out of
     * the way — to the nearest free room, or pushed along one direction like a stack — and
     * the arrangement that displaces the others the least wins.
     *
     * With [squeezeLimits] (used when resizing), an item that cannot be moved may instead
     * give up the cells [target] takes, keeping the rest of itself, as long as it stays
     * within its limits: growing the clock shortens the favourite list below it.
     *
     * null when [target] leaves the grid or no arrangement makes room.
     */
    fun place(
        grid: HomeGrid,
        key: String,
        target: CellRect,
        squeezeLimits: ((HomeGridItem) -> SpanLimits)? = null
    ): HomeGrid? {
        val moving = grid.item(key) ?: return null
        if (!target.fitsIn(grid.columns, grid.rows)) return null
        val others = grid.items.filter { it.key != key }
        if (others.none { it.rect.overlaps(target) }) return grid.replace(moving.copy(rect = target))

        val solutions = buildList {
            relocateToNearest(grid, others, target)?.let(::add)
            Direction.entries.forEach { direction -> push(grid, others, target, direction)?.let(::add) }
        }
        val best = solutions.minWithOrNull(
            compareBy<Map<String, CellRect>>({ displacement(others, it) }, { moved(others, it) })
        ) ?: squeezeLimits?.let { squeeze(grid, others, target, it) } ?: return null

        return grid.copy(
            items = grid.items.map { item ->
                if (item.key == key) item.copy(rect = target) else item.copy(rect = best[item.key] ?: item.rect)
            }
        )
    }

    /** Every displaced item goes to the free room closest to where it was, largest first. */
    private fun relocateToNearest(grid: HomeGrid, others: List<HomeGridItem>, target: CellRect): Map<String, CellRect>? {
        val (covered, untouched) = others.partition { it.rect.overlaps(target) }
        val fixed = mutableListOf(target)
        val result = mutableMapOf<String, CellRect>()
        untouched.forEach {
            fixed += it.rect
            result[it.key] = it.rect
        }
        for (item in covered.sortedByDescending { it.rect.area }) {
            val spot = findVacant(grid.columns, grid.rows, fixed, item.rect.spanX, item.rect.spanY, near = item.rect)
                ?: return null
            fixed += spot
            result[item.key] = spot
        }
        return result
    }

    /** Items in the way are pushed along [direction], and push the ones behind them in turn. */
    private fun push(
        grid: HomeGrid,
        others: List<HomeGridItem>,
        target: CellRect,
        direction: Direction
    ): Map<String, CellRect>? {
        val ordered = when (direction) {
            Direction.DOWN -> others.sortedBy { it.rect.y }
            Direction.UP -> others.sortedByDescending { it.rect.bottom }
            Direction.RIGHT -> others.sortedBy { it.rect.x }
            Direction.LEFT -> others.sortedByDescending { it.rect.right }
        }
        val placed = mutableListOf(target)
        val result = mutableMapOf<String, CellRect>()
        for (item in ordered) {
            var rect = item.rect
            while (true) {
                val blockers = placed.filter { it.overlaps(rect) }
                if (blockers.isEmpty()) break
                rect = when (direction) {
                    Direction.DOWN -> rect.moveTo(rect.x, blockers.maxOf { it.bottom })
                    Direction.UP -> rect.moveTo(rect.x, blockers.minOf { it.y } - rect.spanY)
                    Direction.RIGHT -> rect.moveTo(blockers.maxOf { it.right }, rect.y)
                    Direction.LEFT -> rect.moveTo(blockers.minOf { it.x } - rect.spanX, rect.y)
                }
                if (!rect.fitsIn(grid.columns, grid.rows)) return null
            }
            placed += rect
            result[item.key] = rect
        }
        return result
    }

    /** Every item in the way keeps its largest part that [target] does not cover. */
    private fun squeeze(
        grid: HomeGrid,
        others: List<HomeGridItem>,
        target: CellRect,
        limits: (HomeGridItem) -> SpanLimits
    ): Map<String, CellRect>? {
        val (covered, untouched) = others.partition { it.rect.overlaps(target) }
        val fixed = mutableListOf(target)
        val result = mutableMapOf<String, CellRect>()
        untouched.forEach {
            fixed += it.rect
            result[it.key] = it.rect
        }
        for (item in covered.sortedByDescending { it.rect.area }) {
            val own = limits(item)
            val rect = item.rect
            val remainders = listOf(
                CellRect(rect.x, target.bottom, rect.spanX, rect.bottom - target.bottom),
                CellRect(rect.x, rect.y, rect.spanX, target.y - rect.y),
                CellRect(target.right, rect.y, rect.right - target.right, rect.spanY),
                CellRect(rect.x, rect.y, target.x - rect.x, rect.spanY)
            )
            val kept = remainders
                .filter { it.spanX >= own.minX && it.spanY >= own.minY && it.fitsIn(grid.columns, grid.rows) }
                .filter { candidate -> fixed.none { it.overlaps(candidate) } }
                .maxByOrNull { it.area }
                ?: return null
            fixed += kept
            result[item.key] = kept
        }
        return result
    }

    private fun displacement(others: List<HomeGridItem>, solution: Map<String, CellRect>): Int =
        others.sumOf { item ->
            val rect = solution[item.key] ?: item.rect
            abs(rect.x - item.rect.x) + abs(rect.y - item.rect.y)
        }

    private fun moved(others: List<HomeGridItem>, solution: Map<String, CellRect>): Int =
        others.count { (solution[it.key] ?: it.rect) != it.rect }

    // ── Insert ─────────────────────────────────────────────────────

    /**
     * Adds [item] at the first free room in reading order, at its own span or — failing
     * that — at its minimum span. When the grid is full, resizable items give up rows from
     * their top edge (largest first, never below their minimum) until the new item fits,
     * which is how the favourite list makes room for a widget right under the clock.
     */
    fun insert(grid: HomeGrid, item: HomeGridItem, limits: (HomeGridItem) -> SpanLimits): HomeGrid? {
        if (grid.item(item.key) != null) return null
        val own = limits(item)
        if (own.minX > grid.columns || own.minY > grid.rows) return null
        val spanX = own.clampX(item.rect.spanX).coerceAtMost(grid.columns)
        val spanY = own.clampY(item.rect.spanY).coerceAtMost(grid.rows)
        val sizes = linkedSetOf(spanX to spanY, spanX to own.minY, own.minX to own.minY)

        for ((width, height) in sizes) {
            var current = grid
            while (true) {
                val spot = findVacant(current.columns, current.rows, current.items.map { it.rect }, width, height)
                if (spot != null) return current.copy(items = current.items + item.copy(rect = spot))
                current = shrinkOneRow(current, limits) ?: break
            }
        }
        return null
    }

    /**
     * Takes one row off the top of an item that can lose one, or null when none can. The
     * favourite list goes first — it is the block made to take whatever room is left — and
     * only then the tallest of the others, so a widget the user sized is left alone if it can be.
     */
    private fun shrinkOneRow(grid: HomeGrid, limits: (HomeGridItem) -> SpanLimits): HomeGrid? {
        val candidate = grid.items
            .filter { it.rect.spanY > limits(it).minY }
            .maxWithOrNull(compareBy<HomeGridItem>({ it.type == HomeGridItem.Type.APPS }, { it.rect.spanY }, { it.rect.area }))
            ?: return null
        val rect = candidate.rect
        return grid.replace(candidate.copy(rect = CellRect(rect.x, rect.y + 1, rect.spanX, rect.spanY - 1)))
    }

    // ── Grid size ──────────────────────────────────────────────────

    /**
     * Lays [grid] out again on [columns] × [rows] cells. Positions and spans are scaled in
     * proportion, so blocks that touched keep touching and a full-width block stays full
     * width; spans are kept within each item's limits and any clash is resolved by moving
     * the later item (in reading order) to the nearest free room. null when some item can
     * no longer fit anywhere.
     */
    fun resizeGrid(grid: HomeGrid, columns: Int, rows: Int, limits: (HomeGridItem) -> SpanLimits): HomeGrid? {
        if (columns == grid.columns && rows == grid.rows) return grid
        val scaleX = columns.toFloat() / grid.columns
        val scaleY = rows.toFloat() / grid.rows
        val placed = mutableMapOf<String, CellRect>()

        for (item in grid.items.sortedWith(READING_ORDER)) {
            val own = limits(item)
            if (own.minX > columns || own.minY > rows) return null
            val desired = scaleRect(item.rect, scaleX, scaleY, own, columns, rows)
            val occupied = placed.values.toList()
            val rect = desired.takeIf { spot -> occupied.none { it.overlaps(spot) } }
                ?: findVacant(columns, rows, occupied, desired.spanX, desired.spanY, near = desired)
                ?: findVacant(columns, rows, occupied, own.minX, own.minY, near = desired)
                ?: return null
            placed[item.key] = rect
        }
        return HomeGrid(columns, rows, grid.items.map { it.copy(rect = placed.getValue(it.key)) })
    }

    private fun scaleRect(rect: CellRect, scaleX: Float, scaleY: Float, own: SpanLimits, columns: Int, rows: Int): CellRect {
        val left = (rect.x * scaleX).roundToInt()
        val top = (rect.y * scaleY).roundToInt()
        val spanX = own.clampX(((rect.right * scaleX).roundToInt() - left).coerceAtLeast(1)).coerceAtMost(columns)
        val spanY = own.clampY(((rect.bottom * scaleY).roundToInt() - top).coerceAtLeast(1)).coerceAtMost(rows)
        return CellRect(left.coerceIn(0, columns - spanX), top.coerceIn(0, rows - spanY), spanX, spanY)
    }

    // ── Repair & defaults ──────────────────────────────────────────

    /**
     * Repairs a layout read from storage: duplicates are dropped, items are pulled inside
     * the grid and moved off one another, and the favourite list — the one block the home
     * screen cannot do without — is added back when missing.
     */
    fun normalize(grid: HomeGrid, limits: (HomeGridItem) -> SpanLimits): HomeGrid {
        val columns = grid.columns.coerceIn(HomeGrid.MIN_COLUMNS, HomeGrid.MAX_COLUMNS)
        val rows = grid.rows.coerceIn(HomeGrid.MIN_ROWS, HomeGrid.MAX_ROWS)
        val kept = mutableListOf<HomeGridItem>()
        for (item in grid.items.distinctBy { it.key }) {
            val own = limits(item)
            val spanX = own.clampX(item.rect.spanX).coerceAtMost(columns)
            val spanY = own.clampY(item.rect.spanY).coerceAtMost(rows)
            val desired = CellRect(item.rect.x.coerceIn(0, columns - spanX), item.rect.y.coerceIn(0, rows - spanY), spanX, spanY)
            val occupied = kept.map { it.rect }
            val rect = desired.takeIf { spot -> occupied.none { it.overlaps(spot) } }
                ?: findVacant(columns, rows, occupied, spanX, spanY, near = desired)
                ?: continue
            kept += item.copy(rect = rect)
        }
        val repaired = HomeGrid(columns, rows, kept)
        if (repaired.has(HomeGridItem.Type.APPS)) return repaired
        val apps = HomeGridItem(HomeGridItem.Type.APPS, CellRect(0, 0, columns, rows))
        return insert(repaired, apps, limits) ?: repaired
    }

    /**
     * A first layout: the blocks of [order] stacked top to bottom, the clock on
     * [DEFAULT_CLOCK_ROWS] rows, each widget at its own span and centred, and the favourite
     * list filling what is left. Widgets that do not fit the stack are added afterwards
     * with [insert], which makes room by shrinking the list.
     */
    fun defaultLayout(
        columns: Int,
        rows: Int,
        order: List<HomeGridItem>,
        limits: (HomeGridItem) -> SpanLimits
    ): HomeGrid {
        val clockRows = if (rows >= HomeGrid.MIN_ROWS + DEFAULT_CLOCK_ROWS) DEFAULT_CLOCK_ROWS else 1
        val widgets = order.filter { it.type == HomeGridItem.Type.WIDGET }
        val fixedRows = order.sumOf { item ->
            when (item.type) {
                HomeGridItem.Type.CLOCK -> clockRows
                HomeGridItem.Type.APPS -> 0
                HomeGridItem.Type.WIDGET -> limits(item).clampY(item.rect.spanY).coerceAtMost(rows)
            }
        }
        val stackWidgets = rows - fixedRows >= APPS_LIMITS.minY
        val stacked = if (stackWidgets) order else order - widgets.toSet()
        val appsRows = rows - stacked.sumOf { item ->
            when (item.type) {
                HomeGridItem.Type.CLOCK -> clockRows
                HomeGridItem.Type.APPS -> 0
                HomeGridItem.Type.WIDGET -> limits(item).clampY(item.rect.spanY).coerceAtMost(rows)
            }
        }

        var y = 0
        val items = stacked.map { item ->
            val rect = when (item.type) {
                HomeGridItem.Type.CLOCK -> CellRect(0, y, columns, clockRows)

                HomeGridItem.Type.APPS -> CellRect(0, y, columns, appsRows)

                HomeGridItem.Type.WIDGET -> {
                    val own = limits(item)
                    val spanX = own.clampX(item.rect.spanX).coerceAtMost(columns)
                    CellRect((columns - spanX) / 2, y, spanX, own.clampY(item.rect.spanY).coerceAtMost(rows))
                }
            }
            y += rect.spanY
            item.copy(rect = rect)
        }
        var grid = HomeGrid(columns, rows, items)
        if (!stackWidgets) {
            widgets.forEach { widget -> insert(grid, widget, limits)?.let { grid = it } }
        }
        return grid
    }

    /** Limits of the built-in blocks; widgets get theirs from their provider. */
    fun builtInLimits(type: HomeGridItem.Type): SpanLimits = when (type) {
        HomeGridItem.Type.CLOCK -> CLOCK_LIMITS
        HomeGridItem.Type.APPS -> APPS_LIMITS
        HomeGridItem.Type.WIDGET -> SpanLimits.ANY
    }
}
