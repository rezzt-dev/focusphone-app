package app.focusphone.domain.model

/** Distinct visual clock designs shown on the home screen. */
enum class ClockType(val key: String, val displayName: String, val previewLabel: String) {
    DIGITAL ("digital", "Digital",  "09:41"),
    STACKED ("stacked", "Stacked",  "09\n41"),
    WORDS   ("words",   "Words",    "nine\nforty-one"),
    ANALOG  ("analog",  "Analog",   "◷"),
    HIDDEN  ("hidden",  "Hidden",   "None");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: DIGITAL
    }
}
