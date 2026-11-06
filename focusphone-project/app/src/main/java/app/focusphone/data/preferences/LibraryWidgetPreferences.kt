package app.focusphone.data.preferences

import android.content.Context
import androidx.core.content.edit
import app.focusphone.security.EncryptedPrefs

/**
 * Settings of FocusPhone's own widgets (the widget library), as opposed to
 * [WidgetPreferences], which stores the widget the home screen hosts.
 *
 * - The intention is text the user wrote, so it is encrypted at rest like every other
 *   user-authored value ([EncryptedPrefs]).
 * - World-clock cities are stored per widget instance and released in [forget].
 * - The style signature remembers which theme the widgets were last painted with.
 */
object LibraryWidgetPreferences {

    private const val PREF_FILE = "focusphone_widget_library"
    private const val SECURE_FILE = "focusphone_widget_library_secure"
    private const val KEY_INTENTION = "intention"
    private const val KEY_STYLE_SIGNATURE = "style_signature"
    private const val KEY_ZONES_PREFIX = "world_zones_"
    private const val ZONE_SEPARATOR = ","

    /** Longest intention accepted; two lines on a 4×1 widget. */
    const val MAX_INTENTION_LENGTH = 80

    /** Cities a world clock shows at most; three columns still read at a glance. */
    const val MAX_WORLD_ZONES = 3

    val DEFAULT_WORLD_ZONES = listOf("Europe/London", "America/New_York", "Asia/Tokyo")

    private val secure = EncryptedPrefs(SECURE_FILE)

    private fun prefs(context: Context) = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    // ── Intention ──────────────────────────────────────────────────

    fun getIntention(context: Context): String = secure.getString(context, KEY_INTENTION, null).orEmpty()

    /** @return false when the text could not be stored securely. */
    fun setIntention(context: Context, text: String): Boolean {
        val clean = sanitizeIntention(text)
        return secure.putString(context, KEY_INTENTION, clean.ifEmpty { null })
    }

    /** One line of plain text: newlines collapse, runs of spaces shrink, the length is capped. */
    internal fun sanitizeIntention(text: String): String =
        text.replace(Regex("\\s+"), " ").trim().take(MAX_INTENTION_LENGTH).trim()

    // ── World clock ────────────────────────────────────────────────

    fun getWorldZones(context: Context, widgetId: Int): List<String> {
        val stored = prefs(context).getString(KEY_ZONES_PREFIX + widgetId, null) ?: return DEFAULT_WORLD_ZONES
        return parseZones(stored).ifEmpty { DEFAULT_WORLD_ZONES }
    }

    fun setWorldZones(context: Context, widgetId: Int, zones: List<String>) {
        prefs(context).edit { putString(KEY_ZONES_PREFIX + widgetId, serializeZones(zones)) }
    }

    internal fun serializeZones(zones: List<String>): String =
        zones.map { it.trim() }.filter { it.isNotEmpty() && ZONE_SEPARATOR !in it }.distinct().take(MAX_WORLD_ZONES)
            .joinToString(ZONE_SEPARATOR)

    internal fun parseZones(raw: String): List<String> =
        raw.split(ZONE_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(MAX_WORLD_ZONES)

    // ── Style ──────────────────────────────────────────────────────

    fun getStyleSignature(context: Context): String? = prefs(context).getString(KEY_STYLE_SIGNATURE, null)

    fun setStyleSignature(context: Context, signature: String) {
        prefs(context).edit { putString(KEY_STYLE_SIGNATURE, signature) }
    }

    // ── Clean-up ───────────────────────────────────────────────────

    /** Drops what was stored for widgets the user removed. */
    fun forget(context: Context, widgetIds: IntArray) {
        prefs(context).edit { widgetIds.forEach { remove(KEY_ZONES_PREFIX + it) } }
    }
}
