package app.focusphone.ui.base

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import app.focusphone.FocusPhoneApp
import app.focusphone.R
import app.focusphone.data.preferences.AppSchedulePreferences
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.data.preferences.WallpaperPreferences
import app.focusphone.ui.schedule.ScheduleBlockedActivity
import app.focusphone.ui.wallpaper.WallpaperUtils
import app.focusphone.ui.widget.AnalogClockView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private var currentHasWallpaper: Boolean = false

    companion object {
        private const val GLOBAL_WALLPAPER_TAG = "fp_global_wallpaper"
    }

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
        currentHasWallpaper = hasAnyWallpaper()
        applyTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    private fun hasAnyWallpaper(): Boolean {
        if (!WallpaperPreferences.isWallpaperEnabled(this)) return false
        return WallpaperPreferences.getHomeWallpaperUri(this) != null ||
                WallpaperPreferences.getNotificationsWallpaperUri(this) != null ||
                WallpaperPreferences.getSettingsWallpaperUri(this) != null
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
        applyWallpaper()
    }

    protected fun applyWallpaper() {
        val homeView = findViewById<ImageView?>(R.id.iv_wallpaper_home)
        val settingsView = findViewById<ImageView?>(R.id.iv_wallpaper_settings)
        val hasDedicatedViews = homeView != null || settingsView != null

        // When wallpapers are disabled, clear every wallpaper surface and let the theme
        // background show through. applyCustomThemeColors() already uses the solid theme
        // background because hasAnyWallpaper() returns false in this state.
        if (!WallpaperPreferences.isWallpaperEnabled(this)) {
            lifecycleScope.launch(Dispatchers.Main) {
                homeView?.let { WallpaperUtils.applyWallpaperToImageView(it, null, 0f, false) }
                settingsView?.let { WallpaperUtils.applyWallpaperToImageView(it, null, 0f, false) }

                if (!hasDedicatedViews) {
                    val contentFrame = findViewById<ViewGroup>(android.R.id.content) ?: return@launch
                    contentFrame.findViewWithTag<ImageView>(GLOBAL_WALLPAPER_TAG)?.let {
                        WallpaperUtils.applyWallpaperToImageView(it, null, 0f, false)
                    }
                }
            }
            return
        }

        val blurEnabled = WallpaperPreferences.isBlurEnabled(this)
        val blurRadius = WallpaperUtils.getBlurRadius(WallpaperPreferences.getBlurIntensity(this))

        lifecycleScope.launch(Dispatchers.IO) {
            val homeBitmap = homeView?.let {
                WallpaperUtils.loadBitmap(this@BaseActivity, WallpaperPreferences.getHomeWallpaperUri(this@BaseActivity))
            }
            val settingsBitmap = settingsView?.let {
                WallpaperUtils.loadBitmap(this@BaseActivity, WallpaperPreferences.getSettingsWallpaperUri(this@BaseActivity))
            }
            val globalBitmap = if (!hasDedicatedViews) {
                WallpaperUtils.loadBitmap(this@BaseActivity, WallpaperPreferences.getSettingsWallpaperUri(this@BaseActivity))
            } else null

            withContext(Dispatchers.Main) {
                homeView?.let { WallpaperUtils.applyWallpaperToImageView(it, homeBitmap, blurRadius, blurEnabled) }
                settingsView?.let { WallpaperUtils.applyWallpaperToImageView(it, settingsBitmap, blurRadius, blurEnabled) }

                if (!hasDedicatedViews) {
                    applyGlobalWallpaper(globalBitmap, blurRadius, blurEnabled)
                }

                // When any wallpaper is active, make sure the content frame is transparent so
                // the global/home/settings wallpaper remains visible behind the activity layout.
                if (hasAnyWallpaper()) {
                    findViewById<View>(android.R.id.content)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            }
        }
    }

    /**
     * Adds a full-screen [ImageView] as the first child of [android.R.id.content] and loads the
     * settings/shared wallpaper into it. This is the fallback for activities that do not declare
     * their own dedicated wallpaper view (e.g. Permissions, AllApps, DetoxBlocked, Onboarding).
     */
    private suspend fun applyGlobalWallpaper(bitmap: Bitmap?, blurRadius: Float, blurEnabled: Boolean) {
        val contentFrame = findViewById<ViewGroup>(android.R.id.content) ?: return
        var imageView = contentFrame.findViewWithTag<ImageView>(GLOBAL_WALLPAPER_TAG)
        if (imageView == null) {
            imageView = ImageView(this).apply {
                tag = GLOBAL_WALLPAPER_TAG
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            contentFrame.addView(imageView, 0)
        }
        WallpaperUtils.applyWallpaperToImageView(imageView, bitmap, blurRadius, blurEnabled)
    }

    protected fun applyCustomThemeColors() {
        val theme = ThemePreferences.getTheme(this)
        if (theme != ThemePreferences.THEME_CUSTOM) return

        val bgColor = ThemePreferences.getCustomBackgroundColor(this)
        val fgColor = ThemePreferences.getCustomForegroundColor(this)

        // Set window background color. The content frame must stay transparent when a wallpaper
        // is active, otherwise the solid background would hide the wallpaper behind the layout.
        window.decorView.setBackgroundColor(bgColor)
        val contentFrame = findViewById<View>(android.R.id.content)
        val hasWallpaper = hasAnyWallpaper()
        currentHasWallpaper = hasWallpaper

        if (hasWallpaper) {
            contentFrame?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        } else {
            contentFrame?.setBackgroundColor(bgColor)
        }

        // When a wallpaper is active, keep container backgrounds transparent so the
        // image stays visible behind the content; only recolour text and controls.
        contentFrame?.let { colorizeViewTree(it, bgColor, fgColor, applyBackgrounds = !hasWallpaper) }
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
                        baseSecondary -> view.setTextColor((fgColor and 0x00FFFFFF) or (0xB3 shl 24))
                        baseBg -> view.setTextColor(bgColor)
                        else -> view.setTextColor(fgColor)
                    }
                    if (view is android.widget.EditText) {
                        view.setHintTextColor((fgColor and 0x00FFFFFF) or (0x80 shl 24))
                    }
                }
                is ImageView -> {
                    val tintList = view.imageTintList
                    if (tintList != null) {
                        val currentTint = tintList.getColorForState(intArrayOf(), basePrimary)
                        when (currentTint) {
                            baseSecondary -> view.imageTintList = ColorStateList.valueOf((fgColor and 0x00FFFFFF) or (0xB3 shl 24))
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
                        (fgColor and 0x00FFFFFF) or (0xB3 shl 24)
                    )
                    view.thumbTintList = ColorStateList(thumbStates, thumbColors)

                    val trackStates = arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    )
                    val trackColors = intArrayOf(
                        (fgColor and 0x00FFFFFF) or (0xB3 shl 24),
                        (bgColor and 0x00FFFFFF) or (0x66 shl 24)
                    )
                    view.trackTintList = ColorStateList(trackStates, trackColors)
                }
                is com.google.android.material.slider.Slider -> {
                    view.thumbTintList = ColorStateList.valueOf(fgColor)
                    view.trackActiveTintList = ColorStateList.valueOf(fgColor)
                    // Use the custom background color for the inactive track so it remains visible
                    // over bright or busy wallpapers (a low-alpha foreground tint disappears).
                    view.trackInactiveTintList = ColorStateList.valueOf((bgColor and 0x00FFFFFF) or (0x66 shl 24))
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
                        viewId.contains("wallpaper") ||
                        viewId == "root_home" ||
                        viewId == "drawer_notifications" ||
                        viewId == "notification_content" ||
                        viewId == "drawer_layout" ||
                        viewId == "content_container" ||
                        viewId == "scroll_content" ||
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
        applyWallpaper()

        // System-wide dynamic theme, font scale or font family change detection on resume
        val activeTheme = ThemePreferences.getEffectiveTheme(this)
        val activeFontScale = FontPreferences.getFontScale(this)
        val activeFontFamily = FontPreferences.getFont(this).key
        val activeHasWallpaper = hasAnyWallpaper()
        val fontScaleChanged = kotlin.math.abs(activeFontScale - (currentFontScale ?: 0f)) > 0.01f
        if (activeTheme != currentTheme || fontScaleChanged || activeFontFamily != currentFontFamily || activeHasWallpaper != currentHasWallpaper) {
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

    // ── Schedule blocking ──────────────────────────────────────────

    /**
     * Shows the schedule block screen when [packageName] may not be used right now.
     *
     * Every in-app launch path calls this before starting an app, so the restriction
     * holds even when the accessibility service is off; the service only adds coverage
     * for launches that never pass through FocusPhone (notifications, recents, voice).
     *
     * @return true when the launch was blocked and the caller must abort.
     */
    protected fun enforceAppSchedule(packageName: String, label: String? = null): Boolean {
        val reason = AppSchedulePreferences.getBlockReason(this, packageName) ?: return false
        startActivity(ScheduleBlockedActivity.newIntent(this, packageName, label, reason))
        return true
    }
}
