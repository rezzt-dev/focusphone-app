package app.focusphone.ui.widget.library

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import app.focusphone.ui.theme.FocusColors
import kotlin.math.roundToInt

/** Draws one widget instance. Called off the main thread. */
fun interface WidgetRenderer {
    suspend fun render(scene: WidgetScene): RemoteViews
}

/** Everything a renderer needs to draw one widget instance. */
class WidgetScene(
    val context: Context,
    val widgetId: Int,
    val kind: FocusWidgetKind,
    val style: WidgetStyle,
    val size: WidgetSize
) {
    val packageName: String get() = context.packageName

    fun views(@LayoutRes layout: Int): RemoteViews = RemoteViews(packageName, layout)

    fun px(dp: Float): Int = (dp * context.resources.displayMetrics.density).roundToInt()

    fun string(resId: Int, vararg args: Any): String = context.getString(resId, *args)
}

/** Size of a widget instance in dp, as reported by the launcher hosting it. */
data class WidgetSize(val widthDp: Int, val heightDp: Int) {

    /** The side of the largest square that fits, for round and square widgets. */
    val squareDp: Int get() = minOf(widthDp, heightDp)

    companion object {
        /**
         * Launchers report a range: the width in portrait is the minimum one and the height
         * the maximum one. The orientation the device is in picks the matching pair.
         */
        fun from(context: Context, options: Bundle?, kind: FocusWidgetKind): WidgetSize {
            if (options == null) return WidgetSize(kind.fallbackWidthDp, kind.fallbackHeightDp)
            val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val width = options.getInt(
                if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
            )
            val height = options.getInt(
                if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
            )
            return WidgetSize(
                widthDp = if (width > 0) width else kind.fallbackWidthDp,
                heightDp = if (height > 0) height else kind.fallbackHeightDp
            )
        }
    }
}

// ── RemoteViews helpers ────────────────────────────────────────────

/** Tints a white drawable shown by an ImageView. Works on every supported release. */
fun RemoteViews.tint(@IdRes id: Int, @ColorInt color: Int) {
    setInt(id, "setColorFilter", color)
}

fun RemoteViews.textColor(@ColorInt color: Int, @IdRes vararg ids: Int) {
    ids.forEach { setTextColor(it, color) }
}

fun RemoteViews.show(@IdRes id: Int, visible: Boolean) {
    setViewVisibility(id, if (visible) android.view.View.VISIBLE else android.view.View.GONE)
}

fun RemoteViews.icon(@IdRes id: Int, @DrawableRes icon: Int, @ColorInt color: Int) {
    setImageViewResource(id, icon)
    tint(id, color)
}

fun RemoteViews.textSizeSp(@IdRes id: Int, sp: Float) {
    setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, sp)
}

/**
 * Paints the background of a view with [color]. Android 12 can tint any background; older
 * releases cannot, so they get [lightShape] or [darkShape] — the same shape drawn in the
 * built-in light or dark tone — whichever is closer to [color].
 */
fun RemoteViews.backgroundTint(
    @IdRes id: Int,
    @ColorInt color: Int,
    @DrawableRes lightShape: Int,
    @DrawableRes darkShape: Int
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setInt(id, "setBackgroundResource", lightShape)
        setColorStateList(id, "setBackgroundTintList", ColorStateList.valueOf(color))
    } else {
        setInt(id, "setBackgroundResource", if (FocusColors.isDark(color)) darkShape else lightShape)
    }
}

/** Removes a background set with [backgroundTint]. */
fun RemoteViews.clearBackground(@IdRes id: Int) {
    setInt(id, "setBackgroundResource", 0)
}

/** Paints the card, the shape every widget is drawn on. */
fun RemoteViews.card(@IdRes id: Int, style: WidgetStyle) {
    tint(id, style.card)
}
