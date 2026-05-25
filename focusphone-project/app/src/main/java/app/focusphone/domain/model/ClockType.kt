package app.focusphone.domain.model

/** Distinct visual clock designs shown on the home screen. */
enum class ClockType(val key: String, val displayName: String, val previewLabel: String) {
    DIGITAL ("digital", "digital",  "09:41"),
    STACKED ("stacked", "stacked",  "09\n41"),
    WORDS   ("words",   "words",    "nine\nforty-one"),
    ANALOG  ("analog",  "analog",   "◷"),
    HIDDEN  ("hidden",  "hidden",   "none");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: DIGITAL
    }
}
