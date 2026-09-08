package app.focusphone.data.preferences

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
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
        prefs(context).edit().putString(KEY_FONT, font.key).apply()

    /**
     * Returns the [Typeface] for the currently selected font synchronously.
     * For downloadable fonts this relies on the preloaded-font cache.
     * Falls back to sans-serif if the font is not yet available.
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
     * Returns the selected downloadable [Typeface], or **null** for the system font.
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
                        Log.w("FontPreferences", "Failed to resolve downloadable font $resId", e)
                        null
                    }
                } else null
            }
        }
    }

    /**
     * Resolves the selected [AppFont] asynchronously.
     * Callback is always invoked on the **main thread**.
     * For system fonts it fires synchronously.
     * For downloadable fonts it triggers an auto-download if not cached.
     */
    fun resolveTypeface(context: Context, callback: (Typeface) -> Unit) {
        val font = getFont(context)
        val mainHandler = Handler(Looper.getMainLooper())

        when (font) {
            AppFont.SYSTEM -> { callback(Typeface.create("sans-serif", Typeface.BOLD)); return }
            else -> { /* downloadable — fall through */ }
        }

        val resId = font.fontResId ?: run { callback(Typeface.DEFAULT); return }

        ResourcesCompat.getFont(context, resId, object : ResourcesCompat.FontCallback() {
            override fun onFontRetrieved(typeface: Typeface) {
                mainHandler.post { callback(typeface) }
            }
            override fun onFontRetrievalFailed(reason: Int) {
                Log.w("FontPreferences", "Downloadable font retrieval failed, reason=$reason")
                mainHandler.post { callback(Typeface.create("sans-serif", Typeface.BOLD)) }
            }
        }, mainHandler)
    }

    // ── Clock type ────────────────────────────────────────────────

    fun getClockType(context: Context): ClockType {
        val key = prefs(context).getString(KEY_CLOCK, ClockType.DIGITAL.key) ?: ClockType.DIGITAL.key
        return ClockType.fromKey(key)
    }

    fun setClockType(context: Context, type: ClockType) =
        prefs(context).edit().putString(KEY_CLOCK, type.key).apply()

    // ── Clock size ────────────────────────────────────────────────

    fun getClockSize(context: Context): Float {
        val size = prefs(context).getInt(KEY_CLOCK_SIZE, 50)
        return 0.6f + (size / 100f * 1.4f)
    }

    fun getClockSizePercent(context: Context): Int {
        return prefs(context).getInt(KEY_CLOCK_SIZE, 50)
    }

    fun setClockSizePercent(context: Context, percent: Int) =
        prefs(context).edit().putInt(KEY_CLOCK_SIZE, percent).apply()

    // ── Font Size Scale ───────────────────────────────────────────

    private const val KEY_FONT_SIZE_PT = "font_size_pt"
    private const val MIN_FONT_SIZE_PT = 8
    private const val MAX_FONT_SIZE_PT = 24

    fun getFontSizePt(context: Context): Int {
        val size = prefs(context).getInt(KEY_FONT_SIZE_PT, 14)
        return if (size in MIN_FONT_SIZE_PT..MAX_FONT_SIZE_PT) {
            size
        } else {
            // Recover from a corrupted/out-of-range value (e.g. 100) and save the sane default.
            Log.w("FontPreferences", "font_size_pt was out of range ($size); resetting to $MAX_FONT_SIZE_PT")
            setFontSizePt(context, MAX_FONT_SIZE_PT)
            MAX_FONT_SIZE_PT
        }
    }

    fun setFontSizePt(context: Context, size: Int) {
        val coerced = size.coerceIn(MIN_FONT_SIZE_PT, MAX_FONT_SIZE_PT)
        prefs(context).edit().putInt(KEY_FONT_SIZE_PT, coerced).apply()
    }

    fun getFontScale(context: Context): Float {
        val size = getFontSizePt(context)
        return size / 14.0f
    }

    // ── Internal ──────────────────────────────────────────────────

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
