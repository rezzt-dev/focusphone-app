package app.focusphone.ui.allapps

import android.graphics.Canvas
import android.view.HapticFeedbackConstants
import android.view.ViewConfiguration
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.domain.model.AppInfo
import app.focusphone.ui.theme.FocusPalette
import kotlin.math.abs
import androidx.core.graphics.drawable.toDrawable

/**
 * Drag & drop of apps into folders in the "all apps" drawer.
 *
 * The list is always sorted (folders alphabetically, then apps), so a drag never
 * reorders anything: it only picks a drop target. The target is computed from the
 * centre of the dragged row instead of [onMove] so the *whole* region of an expanded
 * folder — its header and its visible apps — accepts the drop, and so releasing over
 * the plain app list can take an app back out of its folder.
 *
 * The same gesture also carries the long press: [ItemTouchHelper] claims the long
 * press of every draggable row, so a press released without travelling further than
 * the touch slop is reported through [onLongPress] and opens the options dialog.
 */
class FolderDragCallback(
    private val rowAt: (Int) -> AllAppsRow?,
    private val onLongPress: (AppInfo) -> Unit,
    private val onDrop: (app: AppInfo, fromFolderId: String?, toFolderId: String?) -> Unit
) : ItemTouchHelper.Callback() {

    private var draggedRow: AllAppsRow.AppRow? = null
    private var hoverFolderId: String? = null
    private var maxTravel = 0f
    private var dragHighlightColor = 0

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        // Folders keep their alphabetical position and are never dragged.
        val draggable = rowAt(viewHolder.bindingAdapterPosition) is AllAppsRow.AppRow
        val dragFlags = if (draggable) ItemTouchHelper.UP or ItemTouchHelper.DOWN else 0
        return makeMovementFlags(dragFlags, 0)
    }

    /** Rows never swap places; the drag only selects a drop target. */
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState != ItemTouchHelper.ACTION_STATE_DRAG || viewHolder == null) return

        draggedRow = rowAt(viewHolder.bindingAdapterPosition) as? AllAppsRow.AppRow
        hoverFolderId = draggedRow?.folderId
        maxTravel = 0f
        // Theme preferences may involve encrypted storage. Resolve them once at the
        // beginning of the gesture instead of for every animation frame.
        dragHighlightColor = ColorUtils.setAlphaComponent(
            FocusPalette.of(viewHolder.itemView.context).accent,
            HIGHLIGHT_ALPHA
        )
        viewHolder.itemView.alpha = DRAGGED_ALPHA
        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        // Ignore the settle animation that follows the release, otherwise the drop
        // target would be recomputed while the row flies back to its slot.
        if (actionState != ItemTouchHelper.ACTION_STATE_DRAG || !isCurrentlyActive) return

        maxTravel = maxOf(maxTravel, abs(dY))

        val centerY = viewHolder.itemView.top + dY + viewHolder.itemView.height / 2f
        // A null result means the point is over the dragged row itself, so the current
        // target is kept instead of being cleared.
        val target = folderIdUnder(recyclerView, viewHolder, centerY)
        if (target != null && target.folderId != hoverFolderId) {
            hoverFolderId = target.folderId
            if (target.folderId != null) {
                recyclerView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
        highlight(recyclerView, hoverFolderId)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.alpha = 1f
        highlight(recyclerView, null)

        val row = draggedRow
        val target = hoverFolderId
        draggedRow = null
        hoverFolderId = null
        if (row == null) return

        val slop = ViewConfiguration.get(recyclerView.context).scaledTouchSlop
        when {
            // Pressed and released in place — a plain long press.
            maxTravel <= slop -> onLongPress(row.app)
            target != row.folderId -> onDrop(row.app, row.folderId, target)
        }
        maxTravel = 0f
    }

    // ── Drop target ────────────────────────────────────────────────

    /** A resolved drop target; a null [folderId] is the root list. */
    private data class DropTarget(val folderId: String?)

    /**
     * Folder owning the row under [centerY]. Returns the root target when the point is
     * past the ends of the list, and null when nothing lies under it — which happens
     * while the dragged row still overlaps its own slot.
     */
    private fun folderIdUnder(
        recyclerView: RecyclerView,
        dragged: RecyclerView.ViewHolder,
        centerY: Float
    ): DropTarget? {
        var found = false
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            if (child === dragged.itemView) continue
            found = true
            if (centerY < child.top || centerY >= child.bottom) continue

            return when (val row = rowAt(recyclerView.getChildAdapterPosition(child))) {
                is AllAppsRow.FolderRow -> DropTarget(row.folder.id)
                is AllAppsRow.AppRow -> DropTarget(row.folderId)
                null -> DropTarget(null)
            }
        }
        // Dropping past the last row (or above the first one) means the root list.
        if (found && isOutsideChildren(recyclerView, dragged, centerY)) return DropTarget(null)
        return null
    }

    /** True when [centerY] sits beyond every laid-out row except the dragged one. */
    private fun isOutsideChildren(
        recyclerView: RecyclerView,
        dragged: RecyclerView.ViewHolder,
        centerY: Float
    ): Boolean {
        var top = Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            if (child === dragged.itemView) continue
            top = minOf(top, child.top.toFloat())
            bottom = maxOf(bottom, child.bottom.toFloat())
        }
        return centerY < top || centerY >= bottom
    }

    /** Tints every visible row of [folderId] so the drop target is obvious. */
    private fun highlight(recyclerView: RecyclerView, folderId: String?) {
        val tint = if (folderId == null) null else dragHighlightColor.toDrawable()
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val row = rowAt(recyclerView.getChildAdapterPosition(child))
            val isTarget = folderId != null && when (row) {
                is AllAppsRow.FolderRow -> row.folder.id == folderId
                is AllAppsRow.AppRow -> row.folderId == folderId
                null -> false
            }
            child.foreground = if (isTarget) tint else null
        }
    }

    private companion object {
        const val DRAGGED_ALPHA = 0.7f
        const val HIGHLIGHT_ALPHA = 0x22
    }
}
