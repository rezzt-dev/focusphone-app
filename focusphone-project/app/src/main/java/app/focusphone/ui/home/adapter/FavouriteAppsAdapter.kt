package app.focusphone.ui.home.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.R
import app.focusphone.data.model.FavouriteApp
import app.focusphone.databinding.ItemFavouriteAppBinding
import app.focusphone.data.preferences.ThemePreferences

class FavouriteAppsAdapter(
    private val onAppClick: (FavouriteApp) -> Unit,
    private val onAppLongClick: (FavouriteApp) -> Boolean
) : ListAdapter<FavouriteApp, FavouriteAppsAdapter.ViewHolder>(DIFF_CALLBACK) {

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

    fun updateTypeface(tf: Typeface) {
        typeface = tf
        notifyItemRangeChanged(0, itemCount, PAYLOAD_TYPEFACE)
    }

    fun updateAlignment(gravity: Int) {
        textAlignment = gravity
        notifyItemRangeChanged(0, itemCount, PAYLOAD_ALIGNMENT)
    }

    /** Notification badge counts — call [updateBadgeCounts] whenever the listener service refreshes. */
    private var badgeCounts: Map<String, Int> = emptyMap()

    fun updateBadgeCounts(counts: Map<String, Int>) {
        badgeCounts = counts
        notifyItemRangeChanged(0, itemCount, PAYLOAD_BADGE)
    }

    private var tempReorderList: MutableList<FavouriteApp>? = null

    /** Swap two items visually during drag-and-drop. Tracks state internally. */
    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0) return
        val current = tempReorderList ?: currentList.toMutableList()
        val item = current.removeAt(fromPosition)
        current.add(toPosition, item)
        tempReorderList = current
    }

    override fun submitList(list: List<FavouriteApp>?) {
        tempReorderList = null
        super.submitList(list)
    }

    /** Returns the final reordered list and resets the temporary state. */
    fun commitReorder(): List<FavouriteApp> {
        val result = tempReorderList ?: currentList
        tempReorderList = null
        return result.toList()
    }

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
        val app = getItem(position)
        payloads.forEach { payload ->
            when (payload) {
                PAYLOAD_HEIGHT -> applyHeight(holder)
                PAYLOAD_TYPEFACE -> holder.binding.tvAppName.typeface = typeface
                PAYLOAD_ALIGNMENT -> holder.binding.tvAppName.gravity = textAlignment
                PAYLOAD_BADGE -> applyBadge(holder, app)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        applyHeight(holder)
        holder.bind(getItem(position))
    }

    private fun applyHeight(holder: ViewHolder) {
        if (itemHeight > 0) {
            val params = holder.itemView.layoutParams
            params.height = itemHeight
            holder.itemView.layoutParams = params
        }
    }

    private fun applyBadge(holder: ViewHolder, app: FavouriteApp) {
        val count = badgeCounts[app.packageName] ?: 0
        val label = if (count > 0) "${app.label} ($count)" else app.label
        holder.binding.tvAppName.text = label
        holder.binding.root.contentDescription = holder.binding.root.context.getString(R.string.cd_app_item, label)
    }

    inner class ViewHolder(
        val binding: ItemFavouriteAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: FavouriteApp) {
            val context = binding.root.context
            applyBadge(this, app)
            binding.tvAppName.typeface = typeface
            binding.tvAppName.gravity  = textAlignment

            val activeTheme = ThemePreferences.getTheme(context)
            if (activeTheme == ThemePreferences.THEME_CUSTOM) {
                val fgColor = ThemePreferences.getCustomForegroundColor(context)
                binding.tvAppName.setTextColor(fgColor)
            } else {
                val typedValue = android.util.TypedValue()
                context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                binding.tvAppName.setTextColor(typedValue.data)
            }

            // contentDescription is already set by applyBadge to include the badge count
            binding.root.setOnClickListener     { onAppClick(app) }
            binding.root.setOnLongClickListener { onAppLongClick(app) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FavouriteApp>() {
            override fun areItemsTheSame(old: FavouriteApp, new: FavouriteApp) =
                old.packageName == new.packageName
            override fun areContentsTheSame(old: FavouriteApp, new: FavouriteApp) =
                old == new
        }

        private const val PAYLOAD_TYPEFACE = "typeface"
        private const val PAYLOAD_ALIGNMENT = "alignment"
        private const val PAYLOAD_HEIGHT = "height"
        private const val PAYLOAD_BADGE = "badge"
    }
}
