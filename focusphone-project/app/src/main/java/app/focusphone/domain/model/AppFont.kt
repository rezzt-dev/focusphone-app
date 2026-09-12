package app.focusphone.domain.model

/**
 * Available font families for the launcher app list.
 * @param key         Identifier stored in SharedPreferences.
 * @param displayName Name shown in the Settings UI.
 * @param fontResId   Android font resource ID (downloadable font XML), or null for system fonts.
 */
enum class AppFont(val key: String, val displayName: String, val fontResId: Int?) {
    SYSTEM("system",        "system",        null),
    POPPINS("poppins",       "poppins",       app.focusphone.R.font.fp_poppins),
    COMFORTAA("comfortaa",     "comfortaa",     app.focusphone.R.font.fp_comfortaa),
    JETBRAINS("jetbrains_mono", "jetbrains mono", app.focusphone.R.font.fp_jetbrains_mono);

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}
