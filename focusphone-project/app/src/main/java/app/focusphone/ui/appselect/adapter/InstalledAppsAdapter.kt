package app.focusphone.ui.appselect.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.databinding.ItemInstalledAppBinding
import app.focusphone.domain.model.AppInfo

class InstalledAppsAdapter(
    private val onToggle: (AppInfo) -> Unit
) : ListAdapter<AppInfo, InstalledAppsAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInstalledAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemInstalledAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.tvAppName.text      = app.label
            binding.cbFavourite.isChecked = app.isFavourite
            if (app.icon != null) {
                binding.ivAppIcon.setImageDrawable(app.icon)
            }
            binding.root.setOnClickListener { onToggle(app) }
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
