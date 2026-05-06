package app.focusphone.data.model

/**
 * Favourite app entry stored via [app.focusphone.data.preferences.FavouriteAppsPreferences].
 * No Room annotations — persistence is SharedPreferences-based in Phase 1.
 */
data class FavouriteApp(
    val packageName: String,
    val label: String,
    val sortOrder: Int = 0
)
