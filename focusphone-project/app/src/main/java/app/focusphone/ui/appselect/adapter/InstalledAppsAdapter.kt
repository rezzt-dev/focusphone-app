package app.focusphone.ui.appselect.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.R
import app.focusphone.databinding.ItemInstalledAppBinding
import app.focusphone.domain.model.AppInfo

class InstalledAppsAdapter(
    private val onToggle: (AppInfo) -> Unit,
    private val onLongToggle: ((AppInfo) -> Unit)? = null
) : ListAdapter<AppInfo, InstalledAppsAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInstalledAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onToggle, onLongToggle)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemInstalledAppBinding,
        private val onToggle: (AppInfo) -> Unit,
        private val onLongToggle: ((AppInfo) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.tvAppName.text = app.label
            val resId = if (app.isFavourite) R.drawable.ic_check else R.drawable.ic_add
            binding.ivSelectIndicator.setImageResource(resId)
            binding.ivSelectIndicator.alpha = if (app.isFavourite) 1.0f else 0.4f

            binding.root.setOnClickListener { onToggle(app) }
            binding.root.setOnLongClickListener {
                onLongToggle?.invoke(app)
                true
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(old: AppInfo, new: AppInfo) =
                old.packageName == new.packageName

            override fun areContentsTheSame(old: AppInfo, new: AppInfo) =
                old.isFavourite == new.isFavourite && old.label == new.label
        }
    }
}
