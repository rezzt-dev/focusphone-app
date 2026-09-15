package app.focusphone.ui.home.adapter

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.R
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.repository.NotificationItem
import app.focusphone.databinding.ItemNotificationBinding
import app.focusphone.ui.theme.FocusColors
import app.focusphone.ui.theme.FocusPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationsAdapter(
    private val onClick: (NotificationItem) -> Unit = {}
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    private var items: List<NotificationItem> = emptyList()
    private var cachedTypeface: Typeface? = null
    private var cachedPalette: FocusPalette? = null
    private var styleCached = false

    fun submitList(newItems: List<NotificationItem>) {
        val diff = DiffUtil.calculateDiff(NotificationDiffCallback(items, newItems))
        items = newItems
        diff.dispatchUpdatesTo(this)
    }

    fun getItemKey(position: Int): String = items[position].key

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        val context = parent.context
        if (!styleCached) {
            cachedTypeface = FontPreferences.getTypefaceOrNull(context)
            cachedPalette = FocusPalette.of(context)
            styleCached = true
        }
        val typeface = cachedTypeface
        val colors = checkNotNull(cachedPalette)
        return ViewHolder(binding, onClick, typeface, colors)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class ViewHolder(
        private val binding: ItemNotificationBinding,
        private val onClick: (NotificationItem) -> Unit,
        typeface: Typeface?,
        private val colors: FocusPalette
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.tvNotificationApp.typeface = typeface
            binding.tvNotificationTitle.typeface = typeface
            binding.tvNotificationText.typeface = typeface
            binding.tvNotificationTime.typeface = typeface

            val density = binding.root.resources.displayMetrics.density
            binding.cardContainer.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * density
                setColor(FocusColors.withAlpha(this@ViewHolder.colors.background, 0.55f))
                setStroke(
                    (density).toInt().coerceAtLeast(1),
                    FocusColors.withAlpha(this@ViewHolder.colors.textPrimary, 0.10f)
                )
            }
        }

        fun bind(item: NotificationItem) {
            val primaryColor = colors.textPrimary

            // Accent bar: subtle variation derived from the package name so each
            // app feels visually distinct without breaking the minimalist palette.
            val hash = item.packageName.fold(0) { acc, c -> acc + c.code }
            val accentAlpha = 30 + (hash % 50) // 30-80 alpha
            binding.accentBar.setBackgroundColor((primaryColor and 0x00FFFFFF) or (accentAlpha shl 24))

            // Content
            binding.tvNotificationApp.text = item.appName.lowercase(Locale.getDefault())
            binding.tvNotificationTitle.text = item.title ?: ""
            binding.tvNotificationTitle.visibility =
                if (item.title.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE

            binding.tvNotificationText.text = item.text ?: ""
            binding.tvNotificationText.visibility =
                if (item.text.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE

            binding.tvNotificationTime.text = formatRelativeTime(item.postTime)

            val contentParts = listOfNotNull(
                item.appName,
                item.title,
                item.text
            ).filter { it.isNotBlank() }
            binding.root.contentDescription = if (contentParts.isNotEmpty()) {
                contentParts.joinToString(". ")
            } else {
                ""
            }

            binding.root.setOnClickListener { onClick(item) }
        }

        private fun formatRelativeTime(postTime: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - postTime
            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> binding.root.context.getString(R.string.notification_now)

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
                    fmt.format(Date(postTime)).lowercase(Locale.getDefault())
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
