package app.focusphone.ui.home.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.R
import app.focusphone.data.model.FavouriteApp
import app.focusphone.databinding.ItemFavouriteAppBinding
import app.focusphone.domain.util.FavouriteOrdering
import app.focusphone.ui.theme.FocusPalette

/**
 * Favourite apps on the home screen (and in the home-layout preview), with drag & drop.
 *
 * Deliberately not a `ListAdapter`: drag & drop moves rows in place with
 * [notifyItemMoved], while a `ListAdapter`'s list only ever changes through `submitList`.
 * The two drifted apart during a drag — rows could rebind to the wrong app, and the diff
 * against the next database emission replayed moves that had already happened on screen.
 * Here [items] is always exactly what is displayed, and diffs (at most a handful of rows)
 * are computed synchronously against it.
 */
class FavouriteAppsAdapter(
    private val onAppClick: (app: FavouriteApp, row: View) -> Unit
) : RecyclerView.Adapter<FavouriteAppsAdapter.ViewHolder>() {

    private val items = mutableListOf<FavouriteApp>()

    /** Data that arrived during a drag; applied, in the dragged order, when it ends. */
    private var pendingList: List<FavouriteApp>? = null
    private var dragging = false

    /** Live typeface — call [updateTypeface] whenever the user changes the font setting. */
    private var typeface: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
    private var textAlignment: Int = android.view.Gravity.CENTER_HORIZONTAL
    var itemHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT
        set(value) {
            if (field != value) {
                field = value
                notifyItemRangeChanged(0, itemCount, PAYLOAD_HEIGHT)
            }
        }

    /** The rows in the order they are displayed. */
    val currentList: List<FavouriteApp> get() = items.toList()

    /** The row at [position], or null when it is no longer displayed. */
    fun itemAt(position: Int): FavouriteApp? = items.getOrNull(position)

    // Both are called on every return to the home screen; rebinding unchanged rows each
    // time made the list flicker for nothing.
    fun updateTypeface(tf: Typeface) {
        if (tf == typeface) return
        typeface = tf
        notifyItemRangeChanged(0, itemCount, PAYLOAD_TYPEFACE)
    }

    fun updateAlignment(gravity: Int) {
        if (gravity == textAlignment) return
        textAlignment = gravity
        notifyItemRangeChanged(0, itemCount, PAYLOAD_ALIGNMENT)
    }

    /** Displays [list]; while a drag is in progress it is held back until the drag ends. */
    fun submitList(list: List<FavouriteApp>) {
        if (dragging) {
            pendingList = list
            return
        }
        applyList(list)
    }

    private fun applyList(list: List<FavouriteApp>) {
        val old = items.toList()
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = list.size
            override fun areItemsTheSame(oldPosition: Int, newPosition: Int) =
                old[oldPosition].packageName == list[newPosition].packageName
            override fun areContentsTheSame(oldPosition: Int, newPosition: Int) =
                old[oldPosition] == list[newPosition]
        })
        items.clear()
        items.addAll(list)
        diff.dispatchUpdatesTo(this)
    }

    /** Marks the start of a drag so incoming data cannot reshuffle the rows under it. */
    fun beginDrag() {
        dragging = true
    }

    /**
     * Moves one row during a drag. Invalid positions are ignored.
     *
     * @return true when the row moved.
     */
    fun moveItem(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition !in items.indices || toPosition !in items.indices) return false
        if (fromPosition == toPosition) return false
        items.add(toPosition, items.removeAt(fromPosition))
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    /**
     * Ends the drag and returns the order the user left on screen.
     *
     * Data that arrived meanwhile is applied keeping that order, so additions and removals
     * are not lost and nothing jumps back before the new order has been saved.
     */
    fun commitReorder(): List<FavouriteApp> {
        dragging = false
        val ordered = items.toList()
        pendingList?.let { pending ->
            pendingList = null
            val byPackage = pending.associateBy { it.packageName }
            val merged = FavouriteOrdering.merge(
                current = pending.map { it.packageName },
                requested = ordered.map { it.packageName }
            )
            applyList(merged.map { byPackage.getValue(it) })
        }
        return ordered
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFavouriteAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }
        // Apply only the changed properties
        payloads.forEach { payload ->
            when (payload) {
                PAYLOAD_HEIGHT -> applyHeight(holder)
                PAYLOAD_TYPEFACE -> holder.binding.tvAppName.typeface = typeface
                PAYLOAD_ALIGNMENT -> holder.binding.tvAppName.gravity = textAlignment
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        applyHeight(holder)
        holder.bind(items[position])
    }

    private fun applyHeight(holder: ViewHolder) {
        if (itemHeight > 0) {
            val params = holder.itemView.layoutParams
            params.height = itemHeight
            holder.itemView.layoutParams = params
        }
    }

    inner class ViewHolder(
        val binding: ItemFavouriteAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: FavouriteApp) {
            val context = binding.root.context
            binding.tvAppName.text = app.label
            binding.tvAppName.typeface = typeface
            binding.tvAppName.gravity  = textAlignment

            binding.tvAppName.setTextColor(palette(context).textPrimary)

            binding.root.contentDescription = binding.root.context.getString(R.string.cd_app_item, app.label)
            binding.root.setOnClickListener { onAppClick(app, it) }
            // Long press belongs to FavouriteReorderCallback, which tells a press that
            // stays in place (options) from one that travels (reorder).
        }
    }

    /** Resolved once per adapter; the activity is recreated on any theme change. */
    private var paletteCache: FocusPalette? = null

    private fun palette(context: android.content.Context): FocusPalette =
        paletteCache ?: FocusPalette.of(context).also { paletteCache = it }

    companion object {
        private const val PAYLOAD_TYPEFACE = "typeface"
        private const val PAYLOAD_ALIGNMENT = "alignment"
        private const val PAYLOAD_HEIGHT = "height"
    }
}
