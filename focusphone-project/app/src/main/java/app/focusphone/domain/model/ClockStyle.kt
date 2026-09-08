package app.focusphone.domain.model

/**
 * Visual style options for the home screen clock.
 * @param key         Identifier stored in SharedPreferences.
 * @param displayName Shown in Settings.
 * @param textSizeSp  Clock text size in sp.
 * @param fontStyle   Android font family string for the clock.
 */
enum class ClockStyle(
    val key: String,
    val displayName: String,
    val textSizeSp: Float,
    val fontStyle: String
) {
    THIN    ("thin",    "thin",     58f, "sans-serif-thin"),
    LIGHT   ("light",   "light",    54f, "sans-serif-light"),
    REGULAR ("regular", "regular",  48f, "sans-serif"),
    BOLD    ("bold",    "bold",     46f, "sans-serif-medium");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: THIN
    }
}
