package app.focusphone.ui.home.grid

import android.app.Activity
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowInsets
import app.focusphone.R
import app.focusphone.domain.model.HomeGrid
import app.focusphone.domain.model.SpanLimits
import app.focusphone.ui.widget.WidgetSizing
import kotlin.math.roundToInt

/**
 * Size of one cell of the home grid, in dp, and the spans a widget gets on it.
 *
 * The home screen builds it from the grid it has laid out; screens that do not show the
 * grid (the widget picker, the first run) use [estimate], which derives the grid's area
 * from the window the same way the home screen fills it.
 */
class HomeGridMetrics(
    val columns: Int,
    val rows: Int,
    val cellWidthDp: Float,
    val cellHeightDp: Float
) {

    /** The span a newly added widget gets. */
    fun defaultSpan(info: AppWidgetProviderInfo, density: Float): Pair<Int, Int> =
        WidgetSizing.defaultSpan(providerSize(info, density), cellWidthDp, cellHeightDp, columns, rows)

    /** How far a widget may be resized, following its provider. */
    fun limits(info: AppWidgetProviderInfo, density: Float): SpanLimits =
        WidgetSizing.limits(providerSize(info, density), cellWidthDp, cellHeightDp, columns, rows)

    companion object {

        /** Tallest grid an automatic row count may produce; squarer cells read better. */
        private const val MAX_AUTO_ROWS = 9

        /**
         * Below Android 11 the configuration's screen height already leaves out the navigation
         * bar but still counts the status bar, assumed to be this tall.
         */
        private const val LEGACY_STATUS_BAR_DP = 24

        /** Estimates the metrics of a [columns] × [rows] grid from the window of [context]. */
        fun estimate(context: Context, columns: Int, rows: Int): HomeGridMetrics {
            val (widthDp, heightDp) = gridAreaDp(context)
            return HomeGridMetrics(columns, rows, widthDp / columns, heightDp / rows)
        }

        /** Rows that make cells about square for [columns] columns on this screen. */
        fun defaultRows(context: Context, columns: Int): Int {
            val (widthDp, heightDp) = gridAreaDp(context)
            val rows = (heightDp / (widthDp / columns)).roundToInt()
            return rows.coerceIn(HomeGrid.MIN_ROWS, MAX_AUTO_ROWS)
        }

        /** Area in dp the home grid covers: the window without system bars or the grid's padding. */
        private fun gridAreaDp(context: Context): Pair<Float, Float> {
            val resources = context.resources
            val density = resources.displayMetrics.density
            val paddingH = 2 * resources.getDimension(R.dimen.home_grid_padding_horizontal) / density
            val paddingV = 2 * resources.getDimension(R.dimen.home_grid_padding_vertical) / density
            val activity = context.findActivity()
            val (windowW, windowH) = if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val metrics = activity.windowManager.currentWindowMetrics
                val bars = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                val bounds = metrics.bounds
                (bounds.width() - bars.left - bars.right) / density to (bounds.height() - bars.top - bars.bottom) / density
            } else {
                val config = resources.configuration
                config.screenWidthDp.toFloat() to (config.screenHeightDp - LEGACY_STATUS_BAR_DP).toFloat()
            }
            return (windowW - paddingH).coerceAtLeast(1f) to (windowH - paddingV).coerceAtLeast(1f)
        }

        private tailrec fun Context.findActivity(): Activity? = when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }

        /** What the provider declares; `AppWidgetProviderInfo` sizes are already in pixels. */
        fun providerSize(info: AppWidgetProviderInfo, density: Float): WidgetSizing.ProviderSize {
            val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            return WidgetSizing.ProviderSize(
                minWidthDp = info.minWidth / density,
                minHeightDp = info.minHeight / density,
                minResizeWidthDp = info.minResizeWidth / density,
                minResizeHeightDp = info.minResizeHeight / density,
                maxResizeWidthDp = if (modern) info.maxResizeWidth / density else 0f,
                maxResizeHeightDp = if (modern) info.maxResizeHeight / density else 0f,
                targetCellWidth = if (modern) info.targetCellWidth else 0,
                targetCellHeight = if (modern) info.targetCellHeight else 0,
                resizableX = info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0,
                resizableY = info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0
            )
        }
    }
}
