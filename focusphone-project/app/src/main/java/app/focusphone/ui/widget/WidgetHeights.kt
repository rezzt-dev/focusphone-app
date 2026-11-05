package app.focusphone.ui.widget

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import app.focusphone.R
import app.focusphone.data.preferences.WidgetPreferences
import kotlin.math.roundToInt

/**
 * Resolves the home widget's height from the stored preference and the provider's own
 * size, so the home screen, the layout preview and the size slider always agree.
 */
object WidgetHeights {

    /** Height of the window in dp, the reference for the slot's maximum height. */
    fun windowHeightDp(context: Context): Float = context.resources.configuration.screenHeightDp.toFloat()

    /** Width in dp of the home widget slot: the window minus the slot's side padding. */
    fun slotWidthDp(context: Context): Float {
        val resources = context.resources
        val paddingDp = 2 * resources.getDimension(R.dimen.home_widget_side_padding) / resources.displayMetrics.density
        return resources.configuration.screenWidthDp - paddingDp
    }

    /** Size in px of one launcher cell for this screen, see [WidgetSizing.cellDp]. */
    fun cellPx(context: Context): Int =
        (WidgetSizing.cellDp(slotWidthDp(context)) * context.resources.displayMetrics.density).roundToInt()

    /** The height the provider asks for, clamped to what the slot allows. */
    fun defaultHeightDp(context: Context, info: AppWidgetProviderInfo): Int {
        val density = context.resources.displayMetrics.density
        val targetRows = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) info.targetCellHeight else 0
        // AppWidgetProviderInfo sizes are already converted to pixels by the system.
        return WidgetSizing.defaultHeightDp(
            minHeightDp = info.minHeight / density,
            targetCellHeight = targetRows,
            cellDp = WidgetSizing.cellDp(slotWidthDp(context)),
            windowHeightDp = windowHeightDp(context)
        )
    }

    /** The height the widget shows at: the user's choice, or the provider's default. */
    fun currentHeightDp(context: Context, info: AppWidgetProviderInfo): Int {
        val stored = WidgetPreferences.getHeightDp(context)
        return if (stored == WidgetPreferences.HEIGHT_AUTO) {
            defaultHeightDp(context, info)
        } else {
            WidgetSizing.clampHeightDp(stored, windowHeightDp(context))
        }
    }

    /** Slider bounds: from the slot minimum up to the window's share, on slider ticks. */
    fun sliderRange(context: Context): IntRange =
        WidgetSizing.MIN_HEIGHT_DP..WidgetSizing.maxHeightDp(windowHeightDp(context))
}
