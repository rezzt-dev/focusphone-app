package app.focusphone.domain.util

import java.text.Normalizer
import java.util.Locale

private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")

/**
 * Normalizes a raw app name for display inside FocusPhone.
 *
 * Only lowercases the name. Diacritics are preserved: in the app's supported
 * locales (es, fr, de) they are correct spelling, not decoration — stripping
 * them turned "Teléfono" into "telefono" and "Übersetzer" into "ubersetzer".
 */
fun String.normalizeAppName(): String = lowercase(Locale.getDefault())

/**
 * Normalizes text for accent-insensitive search matching: lowercase with
 * diacritical marks removed, so a query like "telefono" still finds "Teléfono".
 */
fun String.normalizeForSearch(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return DIACRITICS_REGEX.replace(normalized, "").lowercase(Locale.getDefault())
}
