package app.focusphone.domain.util

import java.text.Normalizer

/**
 * Normalizes a raw app name for display inside FocusPhone.
 *
 * - Converts to lowercase.
 * - Removes diacritical marks (accents) while preserving base letters.
 *
 * Examples:
 * - "telefono" -> "telefono"
 * - "cafe"     -> "cafe"
 * - "naive"    -> "naive"
 */
fun String.normalizeAppName(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    val withoutDiacritics = normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    return withoutDiacritics.lowercase()
}
