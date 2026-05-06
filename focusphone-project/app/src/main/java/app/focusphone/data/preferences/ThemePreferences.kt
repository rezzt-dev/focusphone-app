package app.focusphone.data.preferences

import android.content.Context
import androidx.annotation.StyleRes
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

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    // ── Theme ──────────────────────────────────────────────────────

    fun getTheme(context: Context): String =
        prefs(context).getString(KEY_THEME, THEME_SLATE) ?: THEME_SLATE

    fun setTheme(context: Context, theme: String) =
        prefs(context).edit().putString(KEY_THEME, theme).apply()

    @StyleRes
    fun resolveStyleRes(theme: String): Int = when (theme) {
        THEME_OFF_WHITE  -> R.style.Theme_FocusPhone_OffWhite
        THEME_SLATE      -> R.style.Theme_FocusPhone_Slate
        THEME_WARM_BEIGE -> R.style.Theme_FocusPhone_WarmBeige
        THEME_NAVY       -> R.style.Theme_FocusPhone_Navy
        else             -> R.style.Theme_FocusPhone_Slate
    }

    // ── Alignment ──────────────────────────────────────────────────
    
    private const val KEY_ALIGNMENT = "app_alignment"
    const val ALIGN_CENTER = "center"
    const val ALIGN_LEFT   = "left"
    const val ALIGN_RIGHT  = "right"

    fun getAlignment(context: Context): String =
        prefs(context).getString(KEY_ALIGNMENT, ALIGN_CENTER) ?: ALIGN_CENTER

    fun setAlignment(context: Context, alignment: String) =
        prefs(context).edit().putString(KEY_ALIGNMENT, alignment).apply()

    // ── Onboarding ────────────────────────────────────────────────

    fun isOnboardingComplete(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDING, false)

    fun markOnboardingComplete(context: Context) =
        prefs(context).edit().putBoolean(KEY_ONBOARDING, true).apply()

    // ── Status Bar ────────────────────────────────────────────────

    private const val KEY_STATUS_BAR = "hide_status_bar"

    fun isStatusBarHidden(context: Context): Boolean =
        prefs(context).getBoolean(KEY_STATUS_BAR, false)

    fun setStatusBarHidden(context: Context, hide: Boolean) =
        prefs(context).edit().putBoolean(KEY_STATUS_BAR, hide).apply()
}
