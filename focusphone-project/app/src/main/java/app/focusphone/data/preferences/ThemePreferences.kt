package app.focusphone.data.preferences

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.annotation.StyleRes
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import app.focusphone.R

/**
 * Synchronous theme preference using SharedPreferences.
 * Must be synchronous so that [BaseActivity] can apply the theme
 * before [setContentView] is called — DataStore (async) is not suitable here.
 */
object ThemePreferences {

    private const val PREF_FILE    = "focusphone_theme"
    private const val KEY_THEME    = "selected_theme"
    private const val KEY_ONBOARDING = "onboarding_complete"

    // ── Theme identifiers ──────────────────────────────────────────
    const val THEME_OFF_WHITE  = "off_white"
    const val THEME_SLATE      = "slate"
    const val THEME_WARM_BEIGE = "warm_beige"
    const val THEME_NAVY       = "navy"
    const val THEME_CUSTOM     = "custom"
    const val THEME_AUTO       = "auto"

    private const val KEY_CUSTOM_BG = "custom_bg_color"
    private const val KEY_CUSTOM_FG = "custom_fg_color"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    // ── Theme ──────────────────────────────────────────────────────

    fun getTheme(context: Context): String =
        prefs(context).getString(KEY_THEME, THEME_SLATE) ?: THEME_SLATE

    fun setTheme(context: Context, theme: String) =
        prefs(context).edit { putString(KEY_THEME, theme) }

    /**
     * Resolves the effective theme identifier.
     * If the user selected [THEME_AUTO], returns a concrete theme based on the
     * current system night mode: [THEME_SLATE] for dark, [THEME_OFF_WHITE] for light.
     */
    fun getEffectiveTheme(context: Context): String {
        val theme = getTheme(context)
        if (theme != THEME_AUTO) return theme

        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            THEME_SLATE
        } else {
            THEME_OFF_WHITE
        }
    }

    fun getCustomBackgroundColorHex(context: Context): String =
        prefs(context).getString(KEY_CUSTOM_BG, "#181818") ?: "#181818"

    fun setCustomBackgroundColorHex(context: Context, hex: String) =
        prefs(context).edit { putString(KEY_CUSTOM_BG, hex) }

    fun getCustomForegroundColorHex(context: Context): String =
        prefs(context).getString(KEY_CUSTOM_FG, "#FAFAFA") ?: "#FAFAFA"

    fun setCustomForegroundColorHex(context: Context, hex: String) =
        prefs(context).edit { putString(KEY_CUSTOM_FG, hex) }

    fun getCustomBackgroundColor(context: Context): Int {
        val hex = getCustomBackgroundColorHex(context)
        return try {
            hex.toColorInt()
        } catch (_: Exception) {
            Log.w("ThemePreferences", "Invalid custom background hex '$hex', falling back to default")
            "#181818".toColorInt()
        }
    }

    fun getCustomForegroundColor(context: Context): Int {
        val hex = getCustomForegroundColorHex(context)
        return try {
            hex.toColorInt()
        } catch (_: Exception) {
            Log.w("ThemePreferences", "Invalid custom foreground hex '$hex', falling back to default")
            "#FAFAFA".toColorInt()
        }
    }

    @StyleRes
    fun resolveStyleRes(theme: String): Int = when (theme) {
        THEME_OFF_WHITE  -> R.style.Theme_FocusPhone_OffWhite
        THEME_SLATE      -> R.style.Theme_FocusPhone_Slate
        THEME_WARM_BEIGE -> R.style.Theme_FocusPhone_WarmBeige
        THEME_NAVY       -> R.style.Theme_FocusPhone_Navy
        else             -> R.style.Theme_FocusPhone_Slate
    }

    @StyleRes
    fun resolveStyleRes(context: Context): Int =
        resolveStyleRes(getEffectiveTheme(context))

    // ── Alignment ──────────────────────────────────────────────────

    private const val KEY_ALIGNMENT = "app_alignment"
    const val ALIGN_CENTER = "center"
    const val ALIGN_LEFT   = "left"
    const val ALIGN_RIGHT  = "right"

    fun getAlignment(context: Context): String =
        prefs(context).getString(KEY_ALIGNMENT, ALIGN_CENTER) ?: ALIGN_CENTER

    fun setAlignment(context: Context, alignment: String) =
        prefs(context).edit { putString(KEY_ALIGNMENT, alignment) }

    // ── Onboarding ────────────────────────────────────────────────

    fun isOnboardingComplete(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDING, false)

    fun markOnboardingComplete(context: Context) =
        prefs(context).edit { putBoolean(KEY_ONBOARDING, true) }

    fun clearOnboardingComplete(context: Context) =
        prefs(context).edit { putBoolean(KEY_ONBOARDING, false) }

    // ── Status Bar ────────────────────────────────────────────────

    private const val KEY_STATUS_BAR = "hide_status_bar"

    fun isStatusBarHidden(context: Context): Boolean =
        prefs(context).getBoolean(KEY_STATUS_BAR, false)

    fun setStatusBarHidden(context: Context, hide: Boolean) =
        prefs(context).edit { putBoolean(KEY_STATUS_BAR, hide) }
}
