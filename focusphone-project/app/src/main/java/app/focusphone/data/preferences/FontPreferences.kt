package app.focusphone.data.preferences

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import androidx.core.content.res.ResourcesCompat
import app.focusphone.domain.model.AppFont
import app.focusphone.domain.model.ClockType

object FontPreferences {

    private const val PREFS_NAME = "focusphone_prefs"
    private const val KEY_FONT   = "selected_font"
    private const val KEY_CLOCK  = "clock_type"

    // ── Font ──────────────────────────────────────────────────────

    fun getFont(context: Context): AppFont {
        val key = prefs(context).getString(KEY_FONT, AppFont.SYSTEM.key) ?: AppFont.SYSTEM.key
        return AppFont.fromKey(key)
    }

    fun setFont(context: Context, font: AppFont) =
        prefs(context).edit().putString(KEY_FONT, font.key).apply()

    /**
     * Resolves the selected [AppFont] asynchronously.
     * Callback is always invoked on the **main thread**.
     * For system fonts (no download needed) it fires synchronously.
     * For downloadable fonts it triggers an auto-download if not cached.
     */
    fun resolveTypeface(context: Context, callback: (Typeface) -> Unit) {
        val font = getFont(context)
        val mainHandler = Handler(Looper.getMainLooper())

        // System fonts — instant, no download
        when (font) {
            AppFont.SYSTEM -> { callback(Typeface.create("sans-serif", Typeface.BOLD)); return }
            AppFont.ARIAL  -> { callback(Typeface.SANS_SERIF); return }
            else -> { /* downloadable — fall through */ }
        }

        val resId = font.fontResId ?: run { callback(Typeface.DEFAULT); return }

        ResourcesCompat.getFont(context, resId, object : ResourcesCompat.FontCallback() {
            override fun onFontRetrieved(typeface: Typeface) {
                mainHandler.post { callback(typeface) }
            }
            override fun onFontRetrievalFailed(reason: Int) {
                // Font unavailable (no internet / Play Services). Fall back gracefully.
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

    // ── Internal ──────────────────────────────────────────────────

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
