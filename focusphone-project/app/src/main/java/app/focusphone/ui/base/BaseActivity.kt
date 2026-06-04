package app.focusphone.ui.base

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import app.focusphone.FocusPhoneApp
import app.focusphone.R
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.ui.widget.AnalogClockView

/**
 * Base class for all FocusPhone activities.
 *
 * Responsibilities:
 *  - Apply the user-selected theme before [setContentView] (must be first).
 *  - Enable edge-to-edge rendering (transparent status/nav bars).
 *  - Apply consistent fade transitions on every Activity launch.
 */
abstract class BaseActivity : AppCompatActivity() {

    /**
     * Safe accessor for the [FocusPhoneApp] application instance.
     * Throws a clear error message if the application is not a FocusPhoneApp.
     */
    protected val focusPhoneApp: FocusPhoneApp
        get() = application as? FocusPhoneApp
            ?: error("Application must extend FocusPhoneApp")
    private var currentTheme: String? = null
    private var customBgColor: Int? = null
    private var customFgColor: Int? = null
    private var currentFontScale: Float? = null
    private var currentFontFamily: String? = null

    override fun attachBaseContext(newBase: android.content.Context) {
        try {
            val config = android.content.res.Configuration(newBase.resources.configuration)
            val systemFontScale = newBase.resources.configuration.fontScale
            val userMultiplier = FontPreferences.getFontScale(newBase)
            config.fontScale = (systemFontScale * userMultiplier).coerceIn(0.5f, 2.0f)
            val context = newBase.createConfigurationContext(config)
            super.attachBaseContext(context)
        } catch (e: Exception) {
            Log.e("BaseActivity", "Failed to apply font scale configuration", e)
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        currentFontScale = FontPreferences.getFontScale(this)
        currentFontFamily = FontPreferences.getFont(this).key
        applyTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    override fun setContentView(view: android.view.View?) {
        super.setContentView(view)
        applyFontFamily()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyFontFamily()
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
        val theme = ThemePreferences.getEffectiveTheme(this)
        currentTheme = theme
        if (ThemePreferences.getTheme(this) == ThemePreferences.THEME_CUSTOM) {
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
        if (theme != ThemePreferences.THEME_CUSTOM) return

        val bgColor = ThemePreferences.getCustomBackgroundColor(this)
        val fgColor = ThemePreferences.getCustomForegroundColor(this)

        // Set window and content background colors
        window.decorView.setBackgroundColor(bgColor)
        val contentFrame = findViewById<View>(android.R.id.content)
        contentFrame?.setBackgroundColor(bgColor)

        contentFrame?.let { colorizeViewTree(it, bgColor, fgColor, applyBackgrounds = true) }
    }

    protected fun colorizeViewTree(root: View, bgColor: Int, fgColor: Int, applyBackgrounds: Boolean) {
        val themeAttrs = intArrayOf(
            android.R.attr.textColorPrimary,
            android.R.attr.textColorSecondary,
            android.R.attr.colorBackground
        )
        val ta = obtainStyledAttributes(themeAttrs)
        val basePrimary = ta.getColor(0, 0)
        val baseSecondary = ta.getColor(1, 0)
        val baseBg = ta.getColor(2, 0)
        ta.recycle()

        val stack = ArrayDeque<View>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val view = stack.removeFirst()

            when (view) {
                is TextView -> {
                    val currentColor = view.currentTextColor
                    when (currentColor) {
                        baseSecondary -> view.setTextColor((fgColor and 0x00FFFFFF) or (0x8A shl 24))
                        baseBg -> view.setTextColor(bgColor)
                        else -> view.setTextColor(fgColor)
                    }
                    if (view is android.widget.EditText) {
                        view.setHintTextColor((fgColor and 0x00FFFFFF) or (0x66 shl 24))
                    }
                }
                is ImageView -> {
                    val tintList = view.imageTintList
                    if (tintList != null) {
                        val currentTint = tintList.getColorForState(intArrayOf(), basePrimary)
                        when (currentTint) {
                            baseSecondary -> view.imageTintList = ColorStateList.valueOf((fgColor and 0x00FFFFFF) or (0x8A shl 24))
                            else -> view.imageTintList = ColorStateList.valueOf(fgColor)
                        }
                    }
                }
                is com.google.android.material.materialswitch.MaterialSwitch -> {
                    val thumbStates = arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    )
                    val thumbColors = intArrayOf(
                        fgColor,
                        (fgColor and 0x00FFFFFF) or (0x8A shl 24)
                    )
                    view.thumbTintList = ColorStateList(thumbStates, thumbColors)

                    val trackStates = arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    )
                    val trackColors = intArrayOf(
                        (fgColor and 0x00FFFFFF) or (0x7C shl 24),
                        (fgColor and 0x00FFFFFF) or (0x28 shl 24)
                    )
                    view.trackTintList = ColorStateList(trackStates, trackColors)
                }
                is com.google.android.material.slider.Slider -> {
                    view.thumbTintList = ColorStateList.valueOf(fgColor)
                    view.trackActiveTintList = ColorStateList.valueOf(fgColor)
                    view.trackInactiveTintList = ColorStateList.valueOf((fgColor and 0x00FFFFFF) or (0x3C shl 24))
                    // haloColor is private in BaseSlider; halo tint is controlled by colorControlHighlight
                }
                is AnalogClockView -> {
                    view.updateColors()
                }
            }

            if (view is ViewGroup) {
                val viewId = try { resources.getResourceEntryName(view.id) } catch (e: Exception) { "" }
                val skipBg = viewId.startsWith("card_theme") ||
                        viewId.startsWith("view_theme") ||
                        viewId.contains("preview") ||
                        viewId.contains("segmented") ||
                        view === root

                if (applyBackgrounds && !skipBg && view.id != android.R.id.content) {
                    view.setBackgroundColor(bgColor)
                }
                for (i in 0 until view.childCount) {
                    stack.addLast(view.getChildAt(i))
                }
            } else {
                // Leaf view background styling (e.g. dividers)
                val background = view.background
                if (background is android.graphics.drawable.ColorDrawable) {
                    val currentColor = background.color
                    if (currentColor == basePrimary || currentColor == baseSecondary) {
                        view.setBackgroundColor((fgColor and 0x00FFFFFF) or (0x1F shl 24))
                    }
                }
            }
        }
    }

    // ── Font family ────────────────────────────────────────────────

    /** Apply the currently selected font family to every [TextView] under [android.R.id.content]. */
    protected fun applyFontFamily() {
        val content = findViewById<View>(android.R.id.content) ?: return
        val tf = FontPreferences.getTypefaceOrNull(this)
        applyTypefaceToViewTree(content, tf)
    }

    /** Apply the currently selected font family to every [TextView] under [root]. */
    protected fun applyFontFamilyToView(root: View) {
        val tf = FontPreferences.getTypefaceOrNull(this)
        applyTypefaceToViewTree(root, tf)
    }

    private fun applyTypefaceToViewTree(root: View, typeface: Typeface?) {
        val stack = ArrayDeque<View>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val view = stack.removeFirst()
            if (view is TextView) {
                view.typeface = typeface
            }
            if (view is ViewGroup && view !is androidx.appcompat.widget.Toolbar) {
                for (i in 0 until view.childCount) {
                    stack.addLast(view.getChildAt(i))
                }
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

        // System-wide dynamic theme, font scale or font family change detection on resume
        val activeTheme = ThemePreferences.getEffectiveTheme(this)
        val activeFontScale = FontPreferences.getFontScale(this)
        val activeFontFamily = FontPreferences.getFont(this).key
        if (activeTheme != currentTheme || activeFontScale != currentFontScale || activeFontFamily != currentFontFamily) {
            recreate()
        } else if (ThemePreferences.getTheme(this) == ThemePreferences.THEME_CUSTOM) {
            val bg = ThemePreferences.getCustomBackgroundColor(this)
            val fg = ThemePreferences.getCustomForegroundColor(this)
            if (bg != customBgColor || fg != customFgColor) {
                recreate()
            }
        }

        // Re-apply font family to catch dynamically created views (e.g. calendars, dialogs)
        applyFontFamily()
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

    /**
     * Applies FocusPhone styling (typeface and custom-theme colours) to a
     * MaterialAlertDialog after it has been shown.
     */
    protected fun styleMaterialDialog(dialog: AlertDialog?) {
        dialog ?: return
        val root = dialog.window?.decorView ?: return

        // Apply custom typeface to every TextView inside the dialog
        val typeface = FontPreferences.getTypefaceOrNull(this)
        val stack = ArrayDeque<View>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val view = stack.removeFirst()
            if (view is TextView) {
                typeface?.let { view.typeface = it }
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    stack.addLast(view.getChildAt(i))
                }
            }
        }

        // For custom themes, force background and text colours so the dialog
        // matches the rest of the UI.
        if (ThemePreferences.getTheme(this) == ThemePreferences.THEME_CUSTOM) {
            val bgColor = ThemePreferences.getCustomBackgroundColor(this)
            val fgColor = ThemePreferences.getCustomForegroundColor(this)

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28f * resources.displayMetrics.density
                setColor(bgColor)
            }
            dialog.window?.setBackgroundDrawable(bg)

            colorizeViewTree(root, bgColor, fgColor, applyBackgrounds = false)
        }
    }
}
