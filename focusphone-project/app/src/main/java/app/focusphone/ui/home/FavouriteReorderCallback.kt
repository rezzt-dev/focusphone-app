package app.focusphone.ui.home

import android.graphics.Canvas
import android.view.HapticFeedbackConstants
import android.view.ViewConfiguration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.data.model.FavouriteApp
import app.focusphone.ui.home.adapter.FavouriteAppsAdapter
import app.focusphone.ui.util.resumePressFeedback
import app.focusphone.ui.util.suspendPressFeedback
import kotlin.math.abs

/**
 * Long press on a home favourite: reorder it, or open its options.
 *
 * [ItemTouchHelper] claims the long press of every row to start a drag, so the row's
 * own long-click listener used to fire at the same time: the options popped up while the
 * row was already lifted, anchored to the corner of the list rather than to the app, and
 * vanished or reordered the list depending on how the finger left the screen. Here a
 * single gesture decides — a press released without travelling past the touch slop is a
 * long press and reports [onLongPress]; one that travels is a reorder and reports the
 * final order through [onReordered]. The same rule the app drawer already follows.
 */
class FavouriteReorderCallback(
    private val adapter: FavouriteAppsAdapter,
    private val onLongPress: (position: Int, holder: RecyclerView.ViewHolder) -> Unit,
    private val onReordered: (List<FavouriteApp>) -> Unit
) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    private var maxTravel = 0f
    private var moved = false
    private var pressedPosition = RecyclerView.NO_POSITION

    /** True from the moment a row is lifted until it settles back into the list. */
    var isDragging: Boolean = false
        private set

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition
        if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
        val didMove = adapter.moveItem(from, to)
        if (didMove) {
            moved = true
            recyclerView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        return didMove
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState != ItemTouchHelper.ACTION_STATE_DRAG || viewHolder == null) return
        isDragging = true
        maxTravel = 0f
        moved = false
        pressedPosition = viewHolder.bindingAdapterPosition
        adapter.beginDrag()
        val view = viewHolder.itemView
        view.suspendPressFeedback()
        view.animate()
            .alpha(LIFTED_ALPHA)
            .scaleX(LIFTED_SCALE)
            .scaleY(LIFTED_SCALE)
            .setDuration(LIFT_DURATION_MS)
            .start()
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
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
            maxTravel = maxOf(maxTravel, abs(dY))
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        val view = viewHolder.itemView
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(LIFT_DURATION_MS)
            .withEndAction { view.resumePressFeedback() }
            .start()

        val finalOrder = adapter.commitReorder()
        isDragging = false
        val slop = ViewConfiguration.get(recyclerView.context).scaledTouchSlop
        when {
            moved -> onReordered(finalOrder)

            // Pressed and released in place: a plain long press.
            maxTravel <= slop && pressedPosition != RecyclerView.NO_POSITION ->
                onLongPress(pressedPosition, viewHolder)
        }
        pressedPosition = RecyclerView.NO_POSITION
    }

    private companion object {
        const val LIFTED_ALPHA = 0.85f
        const val LIFTED_SCALE = 1.04f
        const val LIFT_DURATION_MS = 160L
    }
}
