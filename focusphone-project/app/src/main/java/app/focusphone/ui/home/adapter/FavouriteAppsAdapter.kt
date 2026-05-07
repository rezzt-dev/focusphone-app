package app.focusphone.ui.home.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
                notifyItemRangeChanged(0, itemCount)
            }
        }

    fun updateTypeface(tf: Typeface) {
        typeface = tf
        // Redraw all visible items with the new font
        notifyItemRangeChanged(0, itemCount)
    }

    fun updateAlignment(gravity: Int) {
        textAlignment = gravity
        notifyItemRangeChanged(0, itemCount)
    }

    private var tempReorderList: MutableList<FavouriteApp>? = null

    /** Swap two items visually during drag-and-drop. Tracks state internally. */
    fun moveItem(fromPosition: Int, toPosition: Int) {
        val current = tempReorderList ?: currentList.toMutableList()
        val item = current.removeAt(fromPosition)
        current.add(toPosition, item)
        tempReorderList = current
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (itemHeight > 0) {
            val params = holder.itemView.layoutParams
            params.height = itemHeight
            holder.itemView.layoutParams = params
        }
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemFavouriteAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: FavouriteApp) {
            val context = binding.root.context
            binding.tvAppName.text     = app.label.lowercase()
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
    }
}
