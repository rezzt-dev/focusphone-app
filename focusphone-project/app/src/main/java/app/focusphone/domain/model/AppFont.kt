package app.focusphone.domain.model

/**
 * Available font families for the launcher app list.
 * @param key         Identifier stored in SharedPreferences.
 * @param displayName Name shown in the Settings UI.
 * @param fontResId   Android font resource ID (downloadable font XML), or null for system fonts.
 */
enum class AppFont(val key: String, val displayName: String, val fontResId: Int?) {
    SYSTEM      ("system",        "System",        null),
    POPPINS     ("poppins",       "Poppins",       app.focusphone.R.font.fp_poppins),
    COMFORTAA   ("comfortaa",     "Comfortaa",     app.focusphone.R.font.fp_comfortaa),
    FIGTREE     ("figtree",       "Figtree",       app.focusphone.R.font.fp_figtree),
    JETBRAINS   ("jetbrains_mono","JetBrains Mono",app.focusphone.R.font.fp_jetbrains_mono),
    ARIAL       ("arial",         "Arial",         null),   // mapped to sans-serif on Android
    SPACE_MONO  ("space_mono",    "Space Mono",    app.focusphone.R.font.fp_space_mono);

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}
