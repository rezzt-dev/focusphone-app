package app.focusphone.ui.allapps.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.R
import app.focusphone.databinding.ItemAllAppBinding
import app.focusphone.databinding.ItemAllAppFolderBinding
import app.focusphone.domain.model.AppFolder
import app.focusphone.domain.model.AppInfo
import app.focusphone.ui.allapps.AllAppsRow
import app.focusphone.ui.theme.FocusPalette

/**
 * Renders the "all apps" drawer: folder headers first, then the apps.
 *
 * Long-pressing an *app* is not handled here — it starts a drag through the
 * `ItemTouchHelper` installed by the activity, which opens the options dialog when the
 * finger is released without moving. Folders cannot be dragged (they are always sorted
 * alphabetically), so their long press is a plain listener.
 */
class AllAppsAdapter(
    private val onAppTap: (app: AppInfo, row: View) -> Unit,
    private val onFolderTap: (AppFolder) -> Unit,
    private val onFolderLongTap: (AppFolder) -> Unit
) : ListAdapter<AllAppsRow, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is AllAppsRow.FolderRow -> TYPE_FOLDER
        is AllAppsRow.AppRow -> TYPE_APP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_FOLDER) {
            FolderVH(ItemAllAppFolderBinding.inflate(inflater, parent, false))
        } else {
            AppVH(ItemAllAppBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // A recycled view may still carry the lift and drop highlight of a drag.
        holder.itemView.foreground = null
        holder.itemView.alpha = 1f
        when (val row = getItem(position)) {
            is AllAppsRow.FolderRow -> (holder as FolderVH).bind(row)
            is AllAppsRow.AppRow -> (holder as AppVH).bind(row)
        }
    }

    // ── View holders ───────────────────────────────────────────────

    inner class FolderVH(private val b: ItemAllAppFolderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: AllAppsRow.FolderRow) {
            val context = b.root.context
            b.tvName.text = row.folder.name
            b.tvCount.text = if (row.appCount == 0) {
                context.getString(R.string.allapps_folder_empty)
            } else {
                row.appCount.toString()
            }

            b.ivFolder.setImageResource(
                if (row.expanded) R.drawable.ic_folder_open else R.drawable.ic_folder_closed
            )

            val colors = palette(context)
            b.tvName.setTextColor(colors.textPrimary)
            b.tvCount.setTextColor(colors.textSecondary)
            b.ivFolder.imageTintList = ColorStateList.valueOf(colors.textSecondary)

            b.root.contentDescription = context.getString(R.string.cd_folder_item, row.folder.name)
            b.root.setOnClickListener { onFolderTap(row.folder) }
            b.root.setOnLongClickListener {
                onFolderLongTap(row.folder)
                true
            }
        }
    }

    inner class AppVH(private val b: ItemAllAppBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: AllAppsRow.AppRow) {
            val context = b.root.context
            b.tvName.text = row.app.label
            b.tvName.setTextColor(palette(context).textPrimary)

            // Apps inside a folder are indented so the grouping is readable without
            // any extra chrome.
            val density = context.resources.displayMetrics.density
            val startPadding = if (row.folderId != null) INDENTED_PADDING_DP else ROOT_PADDING_DP
            b.root.setPaddingRelative(
                (startPadding * density).toInt(),
                b.root.paddingTop,
                (ROOT_PADDING_DP * density).toInt(),
                b.root.paddingBottom
            )

            b.root.contentDescription = context.getString(R.string.cd_app_item, row.app.label)
            b.root.setOnClickListener { onAppTap(row.app, it) }
            // No long-click listener: the drag callback owns the long press.
            b.root.setOnLongClickListener(null)
            b.root.isLongClickable = false
        }
    }

    // ── Theme helpers ──────────────────────────────────────────────

    /**
     * Cached for the lifetime of the adapter: the activity — and with it this adapter — is
     * recreated whenever the theme, font or wallpaper changes, so it can never go stale,
     * and row binding stays free of preference reads.
     */
    private var paletteCache: FocusPalette? = null

    private fun palette(context: Context): FocusPalette =
        paletteCache ?: FocusPalette.of(context).also { paletteCache = it }

    companion object {
        private const val TYPE_FOLDER = 0
        private const val TYPE_APP = 1

        private const val ROOT_PADDING_DP = 24
        private const val INDENTED_PADDING_DP = 44

        private val DIFF = object : DiffUtil.ItemCallback<AllAppsRow>() {
            override fun areItemsTheSame(o: AllAppsRow, n: AllAppsRow) = o.id == n.id
            override fun areContentsTheSame(o: AllAppsRow, n: AllAppsRow) = o == n
        }
    }
}
