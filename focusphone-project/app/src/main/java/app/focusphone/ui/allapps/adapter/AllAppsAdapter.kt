package app.focusphone.ui.allapps.adapter

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.R
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.databinding.ItemAllAppBinding
import app.focusphone.databinding.ItemAllAppFolderBinding
import app.focusphone.domain.model.AppFolder
import app.focusphone.domain.model.AppInfo
import app.focusphone.ui.allapps.AllAppsRow

/**
 * Renders the "all apps" drawer: folder headers first, then the apps.
 *
 * Long-pressing an *app* is not handled here — it starts a drag through the
 * `ItemTouchHelper` installed by the activity, which opens the options dialog when the
 * finger is released without moving. Folders cannot be dragged (they are always sorted
 * alphabetically), so their long press is a plain listener.
 */
class AllAppsAdapter(
    private val onAppTap: (AppInfo) -> Unit,
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

            // The caret points right when collapsed and down when expanded.
            b.tvCaret.rotation = if (row.expanded) 90f else 0f

            b.tvName.setTextColor(primaryTextColor(context))
            b.tvCount.setTextColor(secondaryTextColor(context))
            b.tvCaret.setTextColor(secondaryTextColor(context))

            b.root.contentDescription = context.getString(R.string.cd_folder_item, row.folder.name)
            b.root.setOnClickListener { onFolderTap(row.folder) }
            b.root.setOnLongClickListener { onFolderLongTap(row.folder); true }
        }
    }

    inner class AppVH(private val b: ItemAllAppBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: AllAppsRow.AppRow) {
            val context = b.root.context
            b.tvName.text = row.app.label
            b.tvName.setTextColor(primaryTextColor(context))

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
            b.root.setOnClickListener { onAppTap(row.app) }
            // No long-click listener: the drag callback owns the long press.
            b.root.setOnLongClickListener(null)
            b.root.isLongClickable = false
        }
    }

    // ── Theme helpers ──────────────────────────────────────────────

    private fun primaryTextColor(context: Context): Int =
        if (ThemePreferences.getTheme(context) == ThemePreferences.THEME_CUSTOM) {
            ThemePreferences.getCustomForegroundColor(context)
        } else {
            resolveAttr(context, android.R.attr.textColorPrimary)
        }

    private fun secondaryTextColor(context: Context): Int =
        if (ThemePreferences.getTheme(context) == ThemePreferences.THEME_CUSTOM) {
            // Custom themes only define one foreground colour; dim it so the caret and
            // the app count stay secondary to the folder name.
            ColorUtils.setAlphaComponent(ThemePreferences.getCustomForegroundColor(context), 0x99)
        } else {
            resolveAttr(context, android.R.attr.textColorSecondary)
        }

    private fun resolveAttr(context: Context, attr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

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
