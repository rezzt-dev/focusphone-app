package app.focusphone.ui.widget

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.R

/**
 * Adapter for [WidgetPickerActivity]. Displays installed widget providers
 * with their icon, human-readable label and cell size.
 */
class WidgetPickerAdapter(
    private val onItemClick: (ComponentName) -> Unit
) : ListAdapter<WidgetPickerAdapter.Item, WidgetPickerAdapter.ViewHolder>(DiffCallback()) {

    data class Item(
        val provider: ComponentName,
        val label: String,
        val icon: Drawable?,
        val sizeLabel: String
    )

    // ── ViewHolder ─────────────────────────────────────────────────

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_widget_icon)
        val tvLabel: TextView = itemView.findViewById(R.id.tv_widget_label)
        val tvSize: TextView = itemView.findViewById(R.id.tv_widget_size)
    }

    // ── ListAdapter ────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_widget_provider, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvLabel.text = item.label.lowercase()
        holder.tvSize.text = item.sizeLabel
        holder.ivIcon.setImageDrawable(item.icon)
        holder.itemView.setOnClickListener { onItemClick(item.provider) }
    }

    // ── DiffUtil ───────────────────────────────────────────────────

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean = oldItem.provider == newItem.provider

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean = oldItem == newItem
    }
}
