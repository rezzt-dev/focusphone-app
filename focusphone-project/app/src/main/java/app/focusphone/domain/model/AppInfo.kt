package app.focusphone.domain.model

import android.graphics.drawable.Drawable

/**
 * Immutable domain representation of an installed app.
 * [icon] is nullable to avoid loading it when not needed (e.g. home list).
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
    val isFavourite: Boolean = false
)
