package app.focusphone.ui.base

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import app.focusphone.R
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.preferences.ThemePreferences

/**
 * Base class for all FocusPhone activities.
 *
 * Responsibilities:
 *  - Apply the user-selected theme before [setContentView] (must be first).
 *  - Enable edge-to-edge rendering (transparent status/nav bars).
 *  - Apply consistent fade transitions on every Activity launch.
 */
abstract class BaseActivity : AppCompatActivity() {
    private var currentTheme: String? = null
    private var customBgColor: Int? = null
    private var customFgColor: Int? = null
    private var currentFontScale: Float? = null

    override fun attachBaseContext(newBase: android.content.Context) {
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.fontScale = FontPreferences.getFontScale(newBase)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        currentFontScale = FontPreferences.getFontScale(this)
        applyTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    override fun startActivity(intent: Intent) {
        super.startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fp_fade_in, R.anim.fp_fade_out)
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fp_fade_in, R.anim.fp_fade_out)
    }

    // ── Theme ──────────────────────────────────────────────────────

    private fun applyTheme() {
        val theme = ThemePreferences.getTheme(this)
        currentTheme = theme
        if (theme == ThemePreferences.THEME_CUSTOM) {
            customBgColor = ThemePreferences.getCustomBackgroundColor(this)
            customFgColor = ThemePreferences.getCustomForegroundColor(this)
        }
        setTheme(ThemePreferences.resolveStyleRes(theme))
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        applyCustomThemeColors()
    }

    private fun applyCustomThemeColors() {
        val theme = ThemePreferences.getTheme(this)
        if (theme == ThemePreferences.THEME_CUSTOM) {
            val bgColor = ThemePreferences.getCustomBackgroundColor(this)
            val fgColor = ThemePreferences.getCustomForegroundColor(this)
            
            // Set window and content background colors
            window.decorView.setBackgroundColor(bgColor)
            val contentFrame = findViewById<android.view.View>(android.R.id.content)
            contentFrame?.setBackgroundColor(bgColor)
            
            // Recursively style all child views with custom background and foreground colors
            contentFrame?.let { applyCustomThemeToView(it, fgColor, bgColor) }
        }
    }

    private fun applyCustomThemeToView(view: android.view.View, fgColor: Int, bgColor: Int) {
        val className = view.javaClass.name
        if (className.contains("ConstraintLayout") || 
            className.contains("LinearLayout") || 
            className.contains("ScrollView") || 
            className.contains("RecyclerView") ||
            view.id == android.R.id.content) {
            
            // Safely skip overriding backgrounds on theme circular cards or segmented controls
            val viewIdHex = try { resources.getResourceEntryName(view.id) } catch (e: Exception) { "" }
            if (!viewIdHex.startsWith("card_theme") && 
                !viewIdHex.startsWith("view_theme") && 
                !viewIdHex.contains("preview") &&
                !viewIdHex.contains("segmented")) {
                view.setBackgroundColor(bgColor)
            }
        }

        if (view is android.widget.TextView) {
            view.setTextColor(fgColor)
            if (view is android.widget.EditText) {
                // Suttle placeholder tint based on custom foreground color
                view.setHintTextColor((fgColor and 0x00FFFFFF) or 0x66000000)
            }
        }

        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                applyCustomThemeToView(view.getChildAt(i), fgColor, bgColor)
            }
        }
    }

    // ── Edge-to-edge ───────────────────────────────────────────────

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun onResume() {
        super.onResume()
        applyStatusBarVisibility()

        // System-wide dynamic theme or font scale change detection on resume
        val activeTheme = ThemePreferences.getTheme(this)
        val activeFontScale = FontPreferences.getFontScale(this)
        if (activeTheme != currentTheme || activeFontScale != currentFontScale) {
            recreate()
        } else if (activeTheme == ThemePreferences.THEME_CUSTOM) {
            val bg = ThemePreferences.getCustomBackgroundColor(this)
            val fg = ThemePreferences.getCustomForegroundColor(this)
            if (bg != customBgColor || fg != customFgColor) {
                recreate()
            }
        }
    }

    protected fun applyStatusBarVisibility() {
        val hideStatusBar = ThemePreferences.isStatusBarHidden(this)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller?.let {
            if (hideStatusBar) {
                it.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                it.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                it.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
        }
    }
}
