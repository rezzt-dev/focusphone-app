package app.focusphone.ui.widget

import android.content.ComponentName
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.R
import app.focusphone.databinding.ItemWidgetAppHeaderBinding
import app.focusphone.databinding.ItemWidgetProviderBinding
import app.focusphone.ui.theme.FocusColors
import app.focusphone.ui.theme.FocusPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat

/**
 * Adapter for [WidgetPickerActivity]: app headers followed by one card per widget.
 *
 * Each card shows the most faithful preview available, in this order:
 * 1. the provider's `previewLayout` (Android 12+), rendered live at the widget's real size
 *    and scaled into the card — the same thing the user will get on the home screen;
 * 2. the provider's static `previewImage`, decoded off the main thread;
 * 3. the provider's icon.
 */
class WidgetPickerAdapter(
    private val scope: CoroutineScope,
    private val palette: FocusPalette,
    private val homeWidthPx: Int,
    private val onWidgetClick: (WidgetEntry) -> Unit
) : ListAdapter<WidgetPickerRow, RecyclerView.ViewHolder>(DiffCallback) {

    /** Ignores taps while a selection is being processed, so a widget is never added twice. */
    var isInteractionEnabled = true

    private val previewCache = LruCache<String, Drawable>(PREVIEW_CACHE_SIZE)

    /** Providers whose live preview turned out not to stand on its own; they use the image. */
    private val unfitLivePreviews = mutableSetOf<String>()

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is WidgetPickerRow.AppHeader -> TYPE_HEADER
        is WidgetPickerRow.Widget -> TYPE_WIDGET
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(ItemWidgetAppHeaderBinding.inflate(inflater, parent, false))
        } else {
            WidgetHolder(ItemWidgetProviderBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is WidgetPickerRow.AppHeader -> (holder as HeaderHolder).bind(row)
            is WidgetPickerRow.Widget -> (holder as WidgetHolder).bind(row.entry)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        (holder as? WidgetHolder)?.unbind()
    }

    // ── Header ─────────────────────────────────────────────────────

    private inner class HeaderHolder(private val b: ItemWidgetAppHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        init {
            ViewCompat.setAccessibilityHeading(b.root, true)
        }

        fun bind(row: WidgetPickerRow.AppHeader) {
            b.ivAppIcon.setImageDrawable(row.icon)
            b.ivAppIcon.isVisible = row.icon != null
            b.tvAppLabel.text = row.appLabel.lowercase()
            b.tvAppLabel.setTextColor(palette.textSecondary)
            b.tvWidgetCount.text = NumberFormat.getIntegerInstance().format(row.widgetCount)
            b.tvWidgetCount.setTextColor(FocusColors.withAlpha(palette.textSecondary, COUNT_ALPHA))
        }
    }

    // ── Widget card ────────────────────────────────────────────────

    private inner class WidgetHolder(private val b: ItemWidgetProviderBinding) : RecyclerView.ViewHolder(b.root) {

        private var previewJob: Job? = null
        private var boundProvider: ComponentName? = null

        init {
            val context = b.root.context
            b.root.background = GradientDrawable().apply {
                cornerRadius = context.resources.getDimension(R.dimen.widget_card_corner)
                setColor(FocusColors.withAlpha(palette.textPrimary, CARD_FILL_ALPHA))
            }
            b.tvWidgetLabel.setTextColor(palette.textPrimary)
            b.tvWidgetSize.setTextColor(palette.textSecondary)
            b.tvWidgetDescription.setTextColor(palette.textSecondary)
        }

        fun bind(entry: WidgetEntry) {
            boundProvider = entry.provider
            val context = b.root.context
            b.tvWidgetLabel.text = entry.label.lowercase()
            val span = context.getString(R.string.widget_size_format, entry.spanX, entry.spanY)
            b.tvWidgetSize.text = if (entry.inUse) {
                context.getString(R.string.widget_picker_in_use_format, span)
            } else {
                span
            }
            b.tvWidgetSize.setTextColor(if (entry.inUse) palette.accent else palette.textSecondary)
            b.tvWidgetDescription.text = entry.description?.lowercase()
            b.tvWidgetDescription.isVisible = !entry.description.isNullOrBlank()
            b.root.contentDescription = listOfNotNull(entry.label, span, entry.description).joinToString(", ")
            b.root.setOnClickListener {
                if (isInteractionEnabled) onWidgetClick(entry)
            }
            bindPreview(entry)
        }

        fun unbind() {
            previewJob?.cancel()
            previewJob = null
            b.frameLivePreview.onContentOverflow = null
            b.frameLivePreview.removeAllViews()
            b.ivPreview.setImageDrawable(null)
        }

        private fun bindPreview(entry: WidgetEntry) {
            unbind()
            if (bindLivePreview(entry)) return
            bindImagePreview(entry)
        }

        private fun bindImagePreview(entry: WidgetEntry) {
            b.frameLivePreview.isVisible = false
            b.frameLivePreview.removeAllViews()
            b.ivPreview.isVisible = true
            val key = entry.provider.flattenToString()
            previewCache.get(key)?.let {
                showImage(it)
                return
            }
            previewJob = scope.launch {
                val drawable = withContext(Dispatchers.IO) { loadPreviewImage(b.root.context, entry) }
                if (drawable != null) previewCache.put(key, drawable)
                showImage(drawable)
                b.ivPreview.alpha = 0f
                b.ivPreview.animate()
                    .alpha(1f)
                    .setDuration(b.root.resources.getInteger(R.integer.fp_press_out_duration).toLong())
                    .start()
            }
        }

        private fun showImage(drawable: Drawable?) {
            b.ivPreview.alpha = 1f
            b.ivPreview.setImageDrawable(drawable)
        }

        /** Renders the Android 12+ preview layout; false when there is none or it fails. */
        private fun bindLivePreview(entry: WidgetEntry): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
            val layout = entry.info.previewLayout
            val key = entry.provider.flattenToString()
            if (layout == Resources.ID_NULL || key in unfitLivePreviews) return false
            return try {
                val context = b.root.context
                val view = HomeWidgetHostView(WidgetHostContext(context))
                view.setAppWidget(NO_WIDGET_ID, entry.info)
                view.updateAppWidget(RemoteViews(entry.packageName, layout))
                val (widthPx, heightPx) = naturalPreviewSize(context, entry)
                b.frameLivePreview.addView(view, FrameLayout.LayoutParams(widthPx, heightPx))
                b.frameLivePreview.setNaturalSize(widthPx, heightPx)
                b.frameLivePreview.onContentOverflow = {
                    // Only if this holder still shows the same widget.
                    if (unfitLivePreviews.add(key) && boundProvider == entry.provider) bindImagePreview(entry)
                }
                b.frameLivePreview.isVisible = true
                b.ivPreview.isVisible = false
                true
            } catch (e: RuntimeException) {
                Log.w(TAG, "Live preview failed for ${entry.provider}", e)
                b.frameLivePreview.removeAllViews()
                false
            }
        }
    }

    /**
     * The size the widget gets in a real launcher grid, used to render its preview: the
     * layout providers design their previews for. Never below the provider's minimum size,
     * never wider than the home slot.
     */
    private fun naturalPreviewSize(context: Context, entry: WidgetEntry): Pair<Int, Int> {
        val cellPx = WidgetHeights.cellPx(context)
        val widthPx = maxOf(cellPx * entry.spanX, entry.info.minWidth).coerceAtMost(homeWidthPx)
        val heightPx = maxOf(cellPx * entry.spanY, entry.info.minHeight)
        return widthPx to heightPx
    }

    private fun loadPreviewImage(context: Context, entry: WidgetEntry): Drawable? {
        val density = context.resources.displayMetrics.densityDpi
        return try {
            entry.info.loadPreviewImage(context, density) ?: entry.info.loadIcon(context, density)
        } catch (e: RuntimeException) {
            // OutOfMemoryError is not caught on purpose; a provider with a broken resource is.
            Log.w(TAG, "Could not load the preview of ${entry.provider}", e)
            null
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<WidgetPickerRow>() {
        override fun areItemsTheSame(oldItem: WidgetPickerRow, newItem: WidgetPickerRow): Boolean = when {
            oldItem is WidgetPickerRow.AppHeader && newItem is WidgetPickerRow.AppHeader ->
                oldItem.packageName == newItem.packageName

            oldItem is WidgetPickerRow.Widget && newItem is WidgetPickerRow.Widget ->
                oldItem.entry.provider == newItem.entry.provider

            else -> false
        }

        override fun areContentsTheSame(oldItem: WidgetPickerRow, newItem: WidgetPickerRow): Boolean = oldItem == newItem
    }

    private companion object {
        const val TAG = "WidgetPickerAdapter"
        const val TYPE_HEADER = 0
        const val TYPE_WIDGET = 1
        const val NO_WIDGET_ID = -1
        const val PREVIEW_CACHE_SIZE = 48
        const val CARD_FILL_ALPHA = 0.06f
        const val COUNT_ALPHA = 0.7f
    }
}
