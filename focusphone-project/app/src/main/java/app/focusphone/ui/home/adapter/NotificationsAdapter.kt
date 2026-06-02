package app.focusphone.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.data.repository.NotificationItem
import app.focusphone.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationsAdapter : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    private var items: List<NotificationItem> = emptyList()

    fun submitList(newItems: List<NotificationItem>) {
        val diff = DiffUtil.calculateDiff(NotificationDiffCallback(items, newItems))
        items = newItems
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class ViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationItem) {
            binding.tvNotificationApp.text = item.appName.lowercase(Locale.getDefault())
            binding.tvNotificationTitle.text = item.title ?: ""
            binding.tvNotificationTitle.visibility =
                if (item.title.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE

            binding.tvNotificationText.text = item.text ?: ""
            binding.tvNotificationText.visibility =
                if (item.text.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE

            binding.tvNotificationTime.text = formatRelativeTime(item.postTime)
        }

        private fun formatRelativeTime(postTime: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - postTime
            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "${mins}m"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hrs = TimeUnit.MILLISECONDS.toHours(diff)
                    "${hrs}h"
                }
                else -> {
                    val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
                    fmt.format(Date(postTime))
                }
            }
        }
    }

    private class NotificationDiffCallback(
        private val oldList: List<NotificationItem>,
        private val newList: List<NotificationItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean =
            oldList[oldPos].key == newList[newPos].key
        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
            oldList[oldPos] == newList[newPos]
    }
}
