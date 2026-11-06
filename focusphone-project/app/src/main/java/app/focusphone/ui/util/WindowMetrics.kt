package app.focusphone.ui.util

import android.app.Activity
import android.os.Build

/**
 * Current width of the window this activity occupies, in pixels.
 *
 * Unlike `resources.displayMetrics.widthPixels`, this reflects the actual window bounds
 * in multi-window mode and on foldables, rather than the full display size (FP-039).
 */
fun Activity.currentWindowWidthPx(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    windowManager.currentWindowMetrics.bounds.width()
} else {
    @Suppress("DEPRECATION")
    resources.displayMetrics.widthPixels
}
