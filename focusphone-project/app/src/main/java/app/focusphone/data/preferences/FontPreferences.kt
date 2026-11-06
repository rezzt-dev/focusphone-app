package app.focusphone.data.preferences

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import app.focusphone.domain.model.AppFont
import app.focusphone.domain.model.ClockType

object FontPreferences {

    private const val PREFS_NAME = "focusphone_prefs"
    private const val KEY_FONT   = "selected_font"
    private const val KEY_CLOCK      = "clock_type"
    private const val KEY_CLOCK_SIZE = "clock_size"

    // ── Font ──────────────────────────────────────────────────────

    fun getFont(context: Context): AppFont {
        val key = prefs(context).getString(KEY_FONT, AppFont.SYSTEM.key) ?: AppFont.SYSTEM.key
        return AppFont.fromKey(key)
    }

    fun setFont(context: Context, font: AppFont) =
        prefs(context).edit { putString(KEY_FONT, font.key) }

    /**
     * Returns the [Typeface] for the currently selected font synchronously.
     * Falls back to sans-serif if the font resource cannot be loaded.
     */
    fun getTypeface(context: Context): Typeface {
        val font = getFont(context)
        return when (font) {
            AppFont.SYSTEM -> Typeface.create("sans-serif", Typeface.BOLD)

            else -> {
                val resId = font.fontResId
                if (resId != null) {
                    try {
                        ResourcesCompat.getFont(context, resId)
                            ?: Typeface.create("sans-serif", Typeface.BOLD)
                    } catch (e: Exception) {
                        Log.w("FontPreferences", "Failed to load font resource $resId, falling back to sans-serif", e)
                        Typeface.create("sans-serif", Typeface.BOLD)
                    }
                } else {
                    Typeface.create("sans-serif", Typeface.BOLD)
                }
            }
        }
    }

    /**
     * Returns the selected bundled [Typeface], or **null** for the system font.
     * Useful when the caller wants to reset to the theme default for SYSTEM.
     */
    fun getTypefaceOrNull(context: Context): Typeface? {
        val font = getFont(context)
        return when (font) {
            AppFont.SYSTEM -> null

            else -> {
                val resId = font.fontResId
                if (resId != null) {
                    try {
                        ResourcesCompat.getFont(context, resId)
                    } catch (e: Exception) {
                        Log.w("FontPreferences", "Failed to resolve font $resId", e)
                        null
                    }
                } else null
            }
        }
    }

    // ── Clock type ────────────────────────────────────────────────

    fun getClockType(context: Context): ClockType {
        val key = prefs(context).getString(KEY_CLOCK, ClockType.DIGITAL.key) ?: ClockType.DIGITAL.key
        return ClockType.fromKey(key)
    }

    fun setClockType(context: Context, type: ClockType) =
        prefs(context).edit { putString(KEY_CLOCK, type.key) }

    // ── Clock size ────────────────────────────────────────────────

    fun getClockSize(context: Context): Float {
        val size = prefs(context).getInt(KEY_CLOCK_SIZE, 50)
        return 0.6f + (size / 100f * 1.4f)
    }

    fun getClockSizePercent(context: Context): Int = prefs(context).getInt(KEY_CLOCK_SIZE, 50)

    fun setClockSizePercent(context: Context, percent: Int) =
        prefs(context).edit { putInt(KEY_CLOCK_SIZE, percent) }

    // ── Font Size Scale ───────────────────────────────────────────

    private const val KEY_FONT_SIZE_PT = "font_size_pt"
    private const val MIN_FONT_SIZE_PT = 8
    private const val MAX_FONT_SIZE_PT = 24
    private const val DEFAULT_FONT_SIZE_PT = 14

    fun getFontSizePt(context: Context): Int {
        val stored = prefs(context).getInt(KEY_FONT_SIZE_PT, DEFAULT_FONT_SIZE_PT)
        val size = sanitizeFontSize(stored)
        if (size != stored) {
            // Keep this getter side-effect free: it runs from attachBaseContext() on every
            // Activity, before onCreate and on the main thread.
            Log.w("FontPreferences", "font_size_pt was out of range ($stored); using $size")
        }
        return size
    }

    fun setFontSizePt(context: Context, size: Int) {
        val coerced = size.coerceIn(MIN_FONT_SIZE_PT, MAX_FONT_SIZE_PT)
        prefs(context).edit { putInt(KEY_FONT_SIZE_PT, coerced) }
    }

    fun getFontScale(context: Context): Float {
        val size = getFontSizePt(context)
        return size / DEFAULT_FONT_SIZE_PT.toFloat()
    }

    // ── Home text size (independent from the app/settings size above) ──

    private const val KEY_HOME_FONT_SIZE_PT = "home_font_size_pt"

    /** Until the user picks a home size it follows the app size, so upgrading changes nothing. */
    fun getHomeFontSizePt(context: Context): Int {
        val p = prefs(context)
        if (!p.contains(KEY_HOME_FONT_SIZE_PT)) return getFontSizePt(context)
        return sanitizeFontSize(p.getInt(KEY_HOME_FONT_SIZE_PT, DEFAULT_FONT_SIZE_PT))
    }

    fun setHomeFontSizePt(context: Context, size: Int) {
        val coerced = size.coerceIn(MIN_FONT_SIZE_PT, MAX_FONT_SIZE_PT)
        prefs(context).edit { putInt(KEY_HOME_FONT_SIZE_PT, coerced) }
    }

    fun getHomeFontScale(context: Context): Float =
        getHomeFontSizePt(context) / DEFAULT_FONT_SIZE_PT.toFloat()

    /** Invalid persisted values recover to the calm default, never to an extreme. */
    internal fun sanitizeFontSize(size: Int): Int =
        if (size in MIN_FONT_SIZE_PT..MAX_FONT_SIZE_PT) size else DEFAULT_FONT_SIZE_PT

    // ── Internal ──────────────────────────────────────────────────

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
