package app.focusphone.ui.base

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import app.focusphone.FocusPhoneApp
import app.focusphone.R
import app.focusphone.data.preferences.AppSchedulePreferences
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.data.preferences.WallpaperPreferences
import app.focusphone.ui.schedule.ScheduleBlockedActivity
import app.focusphone.ui.theme.FocusColors
import app.focusphone.ui.theme.FocusPalette
import app.focusphone.ui.theme.ScrimSurface
import app.focusphone.ui.theme.SurfaceScrim
import app.focusphone.ui.wallpaper.WallpaperStorage
import app.focusphone.ui.wallpaper.WallpaperUtils
import app.focusphone.ui.widget.AnalogClockView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Base class for all FocusPhone activities.
 *
 * Responsibilities:
 *  - Apply the user-selected theme before [setContentView] (must be first).
 *  - Enable edge-to-edge rendering (transparent status/nav bars).
 *  - Load the wallpaper for this screen and paint an adaptive legibility scrim over it.
 *  - Re-colour the view tree when the user runs a custom (non-style) theme.
 *  - Apply the user font family without flattening the typographic hierarchy.
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
    private var currentReadability: Int = WallpaperPreferences.DEFAULT_READABILITY
    private var paletteCache: FocusPalette? = null

    /**
     * Colours of the active theme, custom themes included. Resolved once per Activity
     * instance; the Activity is recreated whenever the theme changes, so the cache can
     * never go stale.
     */
    protected val palette: FocusPalette
        get() = paletteCache ?: FocusPalette.of(this).also { paletteCache = it }

    companion object {
        private const val TAG = "BaseActivity"
        private const val MS_PER_MINUTE = 60_000L
        private const val GLOBAL_WALLPAPER_TAG = "fp_global_wallpaper"
        private const val GLOBAL_SCRIM_TAG = "fp_global_scrim"

        /**
         * Weight above which a typeface counts as emphasised. Sits between regular (400) and
         * medium (500) so that `sans-serif-medium` — the weight every settings title and
         * section header uses — is preserved, not just true bold.
         */
        private const val EMPHASIS_WEIGHT = 450
        private const val REGULAR_WEIGHT = 400
        private const val MEDIUM_WEIGHT = 500
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
            Log.e(TAG, "Failed to apply font scale configuration", e)
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        currentFontScale = FontPreferences.getFontScale(this)
        currentFontFamily = FontPreferences.getFont(this).key
        currentReadability = WallpaperPreferences.getReadability(this)
        applyTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    override fun setContentView(view: View?) {
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
        paletteCache = null
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        currentHasWallpaper = hasSurfaceWallpaper()
        applyCustomThemeColors()
        // Paint the scrim synchronously so the first frame is already protected; the
        // wallpaper itself then loads off the main thread.
        applyScrim()
        applyWallpaperLegibility()
        applySystemBarContrast()
        applyWallpaper()
    }

    // ── Wallpaper & scrim ──────────────────────────────────────────

    /**
     * Whether this screen renders the *home* wallpaper rather than the settings one.
     *
     * Determined by the presence of a dedicated home wallpaper view in the layout, which
     * keeps the rule in one place instead of duplicating it per Activity.
     */
    private fun usesHomeWallpaper(): Boolean = findViewById<ImageView?>(R.id.iv_wallpaper_home) != null

    /**
     * The wallpaper URI actually displayed by this screen, or null when this screen shows
     * no image. Scrims and background transparency are driven by this — not by "is any
     * wallpaper configured anywhere" — so a home-only wallpaper no longer makes the
     * settings screens behave as if they were wallpapered.
     */
    protected fun surfaceWallpaperUri(): String? {
        if (!WallpaperPreferences.isWallpaperEnabled(this)) return null
        return if (usesHomeWallpaper()) {
            WallpaperPreferences.getHomeWallpaperUri(this)
        } else {
            WallpaperPreferences.getSettingsWallpaperUri(this)
        }
    }

    /** true when an image is painted behind this screen's content. */
    protected fun hasSurfaceWallpaper(): Boolean = surfaceWallpaperUri() != null

    protected fun applyWallpaper() {
        val homeView = findViewById<ImageView?>(R.id.iv_wallpaper_home)
        val settingsView = findViewById<ImageView?>(R.id.iv_wallpaper_settings)
        val hasDedicatedViews = homeView != null || settingsView != null

        val uri = surfaceWallpaperUri()
        currentHasWallpaper = uri != null

        // No wallpaper on this surface: clear every image and let the theme background
        // show through untouched. Scrims collapse to GONE in the same pass.
        if (uri == null) {
            lifecycleScope.launch(Dispatchers.Main) {
                homeView?.let { WallpaperUtils.applyWallpaperToImageView(it, null, 0f, false) }
                settingsView?.let { WallpaperUtils.applyWallpaperToImageView(it, null, 0f, false) }
                if (!hasDedicatedViews) {
                    contentFrame()?.findViewWithTag<ImageView>(GLOBAL_WALLPAPER_TAG)?.let {
                        WallpaperUtils.applyWallpaperToImageView(it, null, 0f, false)
                    }
                }
                applyScrim()
            }
            return
        }

        val blurEnabled = WallpaperPreferences.isBlurEnabled(this)
        val blurRadius = WallpaperUtils.getBlurRadius(WallpaperPreferences.getBlurIntensity(this))

        lifecycleScope.launch(Dispatchers.IO) {
            val result = WallpaperStorage.load(this@BaseActivity, uri)
            val bitmap = (result as? WallpaperStorage.LoadResult.Ready)?.bitmap

            withContext(Dispatchers.Main) {
                if (result == WallpaperStorage.LoadResult.Lost) {
                    // The reference is gone for good; say so instead of dropping it silently.
                    currentHasWallpaper = false
                    Toast.makeText(this@BaseActivity, R.string.wallpaper_access_lost, Toast.LENGTH_LONG).show()
                }
                homeView?.let { WallpaperUtils.applyWallpaperToImageView(it, bitmap, blurRadius, blurEnabled) }
                settingsView?.let { WallpaperUtils.applyWallpaperToImageView(it, bitmap, blurRadius, blurEnabled) }
                if (!hasDedicatedViews) {
                    applyGlobalWallpaper(bitmap, blurRadius, blurEnabled)
                }

                // Keep the content frame transparent so the image behind it stays visible.
                contentFrame()?.setBackgroundColor(Color.TRANSPARENT)
                applyScrim()
                applyWallpaperLegibility()
            }
        }
    }

    /**
     * Positions the legibility scrim for this screen.
     *
     * A layout may declare its own `@id/view_scrim` when the z-order matters; otherwise a
     * full-screen one is inserted directly above the wallpaper. Screens that own several
     * independent surfaces — the home screen and its two drawers — override this and place
     * their scrims themselves.
     */
    protected open fun applyScrim() {
        val declared = findViewById<View?>(R.id.view_scrim)
        val target = declared ?: ensureGlobalScrim() ?: return
        SurfaceScrim.apply(target, this, ScrimSurface.CONTENT, palette, currentHasWallpaper)
    }

    private fun contentFrame(): ViewGroup? = findViewById(android.R.id.content)

    /**
     * Adds a full-screen [ImageView] as the first child of [android.R.id.content] and loads
     * the wallpaper into it. This is the fallback for activities that do not declare their
     * own wallpaper view (app drawer, permissions, block screens, onboarding…).
     */
    private suspend fun applyGlobalWallpaper(bitmap: Bitmap?, blurRadius: Float, blurEnabled: Boolean) {
        val contentFrame = contentFrame() ?: return
        var imageView = contentFrame.findViewWithTag<ImageView>(GLOBAL_WALLPAPER_TAG)
        if (imageView == null) {
            imageView = ImageView(this).apply {
                tag = GLOBAL_WALLPAPER_TAG
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            contentFrame.addView(imageView, 0)
        }
        WallpaperUtils.applyWallpaperToImageView(imageView, bitmap, blurRadius, blurEnabled)
    }

    /**
     * Returns the auto-managed scrim view, creating it directly above the wallpaper so it
     * dims the image without ever covering the content.
     */
    private fun ensureGlobalScrim(): View? {
        val contentFrame = contentFrame() ?: return null
        contentFrame.findViewWithTag<View>(GLOBAL_SCRIM_TAG)?.let { return it }

        val scrim = View(this).apply {
            tag = GLOBAL_SCRIM_TAG
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            visibility = View.GONE
        }
        val wallpaperIndex = contentFrame.findViewWithTag<ImageView>(GLOBAL_WALLPAPER_TAG)
            ?.let { contentFrame.indexOfChild(it) } ?: -1
        contentFrame.addView(scrim, wallpaperIndex + 1)
        return scrim
    }

    /**
     * The surface whose scrim sits behind this screen's own text. Screens with several
     * surfaces name the one that carries their content.
     */
    protected open fun contentSurface(): ScrimSurface = ScrimSurface.CONTENT

    /**
     * The palette to draw with on this screen, accounting for the wallpaper that may be
     * showing through the scrim. Identical to [palette] whenever no wallpaper is visible.
     */
    protected val effectivePalette: FocusPalette
        get() = if (currentHasWallpaper) {
            palette.forScrim(SurfaceScrim.alphaFor(this, contentSurface(), true))
        } else {
            palette
        }

    /**
     * Strengthens supporting text once a wallpaper is showing behind the content.
     *
     * The style themes pick their secondary colour against a solid background; several of
     * them sit right at the WCAG threshold there, so any wallpaper visible through the
     * scrim pushes them under it. This repaints the tree with [effectivePalette] — for
     * every theme, not only the custom one — and is skipped entirely when there is no
     * wallpaper, which is the common case.
     */
    protected fun applyWallpaperLegibility() {
        if (!currentHasWallpaper) return
        if (ThemePreferences.getTheme(this) == ThemePreferences.THEME_CUSTOM) return
        val adjusted = effectivePalette
        if (adjusted == palette) return
        contentFrame()?.let { colorizeViewTree(it, adjusted, applyBackgrounds = false) }
    }

    // ── Custom theme colouring ─────────────────────────────────────

    /**
     * Repaints the view tree for the custom theme, which is defined by two user colours
     * rather than by a style and therefore cannot be resolved at inflation time.
     */
    protected fun applyCustomThemeColors() {
        if (ThemePreferences.getTheme(this) != ThemePreferences.THEME_CUSTOM) return

        val colors = effectivePalette
        window.decorView.setBackgroundColor(colors.background)

        val contentFrame = contentFrame()
        val hasWallpaper = hasSurfaceWallpaper()
        currentHasWallpaper = hasWallpaper

        if (hasWallpaper) {
            contentFrame?.setBackgroundColor(Color.TRANSPARENT)
        } else {
            contentFrame?.setBackgroundColor(colors.background)
        }

        // With a wallpaper active, container backgrounds stay transparent so the image
        // remains visible behind the content; only text and controls are recoloured.
        contentFrame?.let { colorizeViewTree(it, colors, applyBackgrounds = !hasWallpaper) }
    }

    /**
     * Walks [root] and rewrites every colour that came from the *style* theme so it matches
     * the custom [colors] instead.
     *
     * Views are matched against the style's own primary/secondary/background values, which
     * is how a view's original semantic role is recovered after inflation.
     */
    protected fun colorizeViewTree(root: View, colors: FocusPalette, applyBackgrounds: Boolean) {
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
                    when (view.currentTextColor) {
                        baseSecondary -> view.setTextColor(colors.textSecondary)
                        baseBg -> view.setTextColor(colors.background)
                        else -> view.setTextColor(colors.textPrimary)
                    }
                    view.highlightColor = FocusColors.withAlpha(colors.accent, 0.30f)
                    if (view is EditText) {
                        view.setHintTextColor(FocusColors.withAlpha(colors.textSecondary, 0.75f))
                    }
                }

                is ImageView -> {
                    val tintList = view.imageTintList
                    // Stateful tints encode selected/activated semantics that a single
                    // colour cannot express; those views recolour themselves instead.
                    if (tintList != null && !tintList.isStateful) {
                        val currentTint = tintList.getColorForState(intArrayOf(), basePrimary)
                        view.imageTintList = ColorStateList.valueOf(
                            if (currentTint == baseSecondary) colors.textSecondary else colors.textPrimary
                        )
                    }
                }

                is MaterialSwitch -> {
                    val states = arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    )
                    view.thumbTintList = ColorStateList(
                        states,
                        intArrayOf(colors.onAccent, colors.textSecondary)
                    )
                    view.trackTintList = ColorStateList(
                        states,
                        intArrayOf(colors.accent, FocusColors.blend(colors.textPrimary, colors.background, 0.22f))
                    )
                    view.trackDecorationTintList = ColorStateList(
                        states,
                        intArrayOf(colors.accent, colors.divider)
                    )
                }

                is Slider -> {
                    view.thumbTintList = ColorStateList.valueOf(colors.accent)
                    view.trackActiveTintList = ColorStateList.valueOf(colors.accent)
                    // The inactive track must stay visible over bright or busy wallpapers,
                    // where a low-alpha foreground tint would disappear entirely.
                    view.trackInactiveTintList =
                        ColorStateList.valueOf(FocusColors.blend(colors.textPrimary, colors.background, 0.24f))
                    view.haloTintList = ColorStateList.valueOf(FocusColors.withAlpha(colors.accent, 0.20f))
                }

                is AnalogClockView -> view.updateColors()
            }

            if (view is ViewGroup) {
                val viewId = try {
                    resources.getResourceEntryName(view.id)
                } catch (e: Exception) {
                    ""
                }
                val skipBg = viewId.startsWith("card_theme") ||
                        viewId.startsWith("view_theme") ||
                        viewId.contains("preview") ||
                        viewId.contains("segmented") ||
                        viewId.contains("wallpaper") ||
                        viewId.contains("scrim") ||
                        viewId == "root_home" ||
                        viewId == "home_content" ||
                        viewId == "drawer_notifications" ||
                        viewId == "notification_content" ||
                        viewId == "drawer_layout" ||
                        viewId == "content_container" ||
                        viewId == "scroll_content" ||
                        view === root

                if (applyBackgrounds && !skipBg && view.id != android.R.id.content) {
                    view.setBackgroundColor(colors.background)
                }
                for (i in 0 until view.childCount) {
                    stack.addLast(view.getChildAt(i))
                }
            } else {
                // Leaf views: recolour hairline dividers, which are drawn as flat colours.
                val background = view.background
                if (background is ColorDrawable) {
                    val currentColor = background.color
                    when (currentColor) {
                        basePrimary -> view.setBackgroundColor(colors.textPrimary)
                        baseSecondary -> view.setBackgroundColor(colors.textSecondary)
                    }
                }
            }
        }
    }

    // ── Font family ────────────────────────────────────────────────

    /** Apply the currently selected font family to every [TextView] under [android.R.id.content]. */
    protected fun applyFontFamily() {
        val content = contentFrame() ?: return
        applyTypefaceToViewTree(content, FontPreferences.getTypefaceOrNull(this))
    }

    /** Apply the currently selected font family to every [TextView] under [root]. */
    protected fun applyFontFamilyToView(root: View) {
        applyTypefaceToViewTree(root, FontPreferences.getTypefaceOrNull(this))
    }

    /**
     * Swaps the font family of a whole subtree **without flattening its weights**.
     *
     * Assigning `view.typeface = tf` directly would erase every `sans-serif-medium` title
     * and every bold label the moment a custom font was selected, collapsing the entire
     * typographic hierarchy to a single weight. Emphasis is detected from the current
     * typeface and re-applied on top of the new family.
     */
    private fun applyTypefaceToViewTree(root: View, typeface: Typeface?) {
        val stack = ArrayDeque<View>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val view = stack.removeFirst()
            if (view is TextView) {
                view.typeface = mergeEmphasis(view.typeface, typeface)
            }
            if (view is ViewGroup && view !is Toolbar) {
                for (i in 0 until view.childCount) {
                    stack.addLast(view.getChildAt(i))
                }
            }
        }
    }

    /**
     * Returns [target] (or the platform default when null) carrying the emphasis — weight
     * and italic — of [current].
     */
    private fun mergeEmphasis(current: Typeface?, target: Typeface?): Typeface? {
        current ?: return target
        val italic = current.isItalic
        val emphasised = current.isBold ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && current.weight > EMPHASIS_WEIGHT)

        if (!emphasised && !italic) return target

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val weight = if (emphasised) maxOf(current.weight, MEDIUM_WEIGHT) else REGULAR_WEIGHT
            Typeface.create(target ?: Typeface.DEFAULT, weight.coerceIn(100, 1000), italic)
        } else {
            val style = when {
                emphasised && italic -> Typeface.BOLD_ITALIC
                emphasised -> Typeface.BOLD
                else -> Typeface.ITALIC
            }
            Typeface.create(target ?: Typeface.DEFAULT, style)
        }
    }

    // ── Edge-to-edge ───────────────────────────────────────────────

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun onResume() {
        super.onResume()
        applyStatusBarVisibility()
        applySystemBarContrast()

        // System-wide dynamic theme, font scale, font family, wallpaper or legibility
        // changes are detected here and require a full recreate to take effect.
        //
        // The wallpaper state must be compared *before* applyWallpaper() runs: that call
        // records the new state, which used to make this comparison always equal, so
        // screens inflated with (or without) a wallpaper never picked up the change.
        val activeTheme = ThemePreferences.getEffectiveTheme(this)
        val activeFontScale = FontPreferences.getFontScale(this)
        val activeFontFamily = FontPreferences.getFont(this).key
        val activeHasWallpaper = hasSurfaceWallpaper()
        val activeReadability = WallpaperPreferences.getReadability(this)
        val fontScaleChanged = kotlin.math.abs(activeFontScale - (currentFontScale ?: 0f)) > 0.01f

        if (activeTheme != currentTheme ||
            fontScaleChanged ||
            activeFontFamily != currentFontFamily ||
            activeHasWallpaper != currentHasWallpaper
        ) {
            recreate()
            return
        }

        if (ThemePreferences.getTheme(this) == ThemePreferences.THEME_CUSTOM) {
            val bg = ThemePreferences.getCustomBackgroundColor(this)
            val fg = ThemePreferences.getCustomForegroundColor(this)
            if (bg != customBgColor || fg != customFgColor) {
                recreate()
                return
            }
        }

        applyWallpaper()

        if (activeReadability != currentReadability) {
            currentReadability = activeReadability
            applyScrim()
            applyWallpaperLegibility()
        }

        // Re-apply font family to catch dynamically created views (e.g. calendars, dialogs)
        applyFontFamily()
    }

    override fun onDestroy() {
        // A dialog still attached to a destroyed window leaks the Activity.
        openDialogs.forEach { if (it.isShowing) it.dismiss() }
        openDialogs.clear()
        exclusiveDialog = null
        super.onDestroy()
    }

    // ── Dialogs ────────────────────────────────────────────────────

    private val openDialogs = mutableSetOf<Dialog>()
    private var exclusiveDialog: Dialog? = null

    /** Registers [dialog] so it is dismissed with this screen instead of leaking it. */
    protected fun trackDialog(dialog: Dialog) {
        openDialogs.removeAll { !it.isShowing && it !== exclusiveDialog }
        openDialogs += dialog
    }

    /**
     * Shows [dialog] as this screen's single foreground dialog, dismissing the previous
     * one. Chained flows (options → folder picker → new folder) used to overwrite the
     * reference to the dialog still on screen, which then leaked its window.
     */
    protected fun showExclusiveDialog(dialog: Dialog) {
        exclusiveDialog?.takeIf { it !== dialog && it.isShowing }?.dismiss()
        exclusiveDialog = dialog
        trackDialog(dialog)
        dialog.show()
    }

    /** [showExclusiveDialog] for Material dialogs, restyled with the FocusPhone palette. */
    protected fun showStyledDialog(dialog: AlertDialog) {
        showExclusiveDialog(dialog)
        styleMaterialDialog(dialog)
    }

    /** Dismisses the dialog shown through [showExclusiveDialog], if any. */
    protected fun dismissExclusiveDialog() {
        exclusiveDialog?.dismiss()
        exclusiveDialog = null
    }

    /**
     * Whether this screen may still put UI on screen. Work resumed after a suspension
     * (loading shortcuts, the app list…) must check it: `lifecycleScope` outlives
     * `onStop`, and showing a popup on a stopped or dead window crashes with
     * `BadTokenException`.
     */
    protected fun canShowUi(): Boolean =
        !isFinishing && !isDestroyed && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    protected fun applyStatusBarVisibility() {
        val hideStatusBar = ThemePreferences.isStatusBarHidden(this)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller?.let {
            if (hideStatusBar) {
                it.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                it.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                it.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    /**
     * Chooses light or dark status/navigation bar icons from the palette actually in use.
     *
     * The style themes hard-code this per theme, which leaves the custom theme with system
     * icons picked for whichever built-in style it happens to inherit — white icons on a
     * light custom background, for instance. The wallpaper's own tone is deliberately not
     * consulted: it is unknown per pixel, and the scrim tints the area towards the theme
     * background anyway.
     */
    protected fun applySystemBarContrast() {
        val controller = WindowCompat.getInsetsController(window, window.decorView) ?: return
        val lightIcons = !FocusColors.isDark(palette.background)
        controller.isAppearanceLightStatusBars = lightIcons
        controller.isAppearanceLightNavigationBars = lightIcons
    }

    /**
     * Applies FocusPhone styling (typeface and custom-theme colours) to a
     * MaterialAlertDialog after it has been shown.
     */
    protected fun styleMaterialDialog(dialog: AlertDialog?) {
        dialog ?: return
        val root = dialog.window?.decorView ?: return

        applyTypefaceToViewTree(root, FontPreferences.getTypefaceOrNull(this))

        // For custom themes, force background and text colours so the dialog
        // matches the rest of the UI.
        if (ThemePreferences.getTheme(this) == ThemePreferences.THEME_CUSTOM) {
            val colors = palette
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28f * resources.displayMetrics.density
                setColor(colors.background)
            }
            dialog.window?.setBackgroundDrawable(bg)
            colorizeViewTree(root, colors, applyBackgrounds = false)
        }
    }

    /**
     * Paints a dialog's root view as a FocusPhone surface.
     *
     * Dialogs that supply their own layout get their background from `bg_dialog_input`,
     * which resolves theme attributes at inflation time — so a custom theme, whose colours
     * exist only at runtime, was silently ignored and the panel kept the colours of
     * whichever built-in style the custom theme happened to inherit.
     */
    protected fun applyDialogSurface(root: View) {
        val colors = palette
        val density = resources.displayMetrics.density
        root.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f * density
            setColor(colors.background)
            setStroke((1f * density).toInt().coerceAtLeast(1), colors.divider)
        }
    }

    /**
     * Styles a segmented control (the alignment picker) from the palette.
     *
     * @param container the pill-shaped group holding the segments.
     * @param segments the individual segment views, whose `isActivated` flag drives the fill.
     */
    protected fun applySegmentedControlColors(container: View, segments: List<ImageView>) {
        val colors = palette
        val density = resources.displayMetrics.density

        container.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f * density
            setColor(FocusColors.blend(colors.textPrimary, colors.background, 0.08f))
        }

        val states = arrayOf(intArrayOf(android.R.attr.state_activated), intArrayOf())
        segments.forEach { segment ->
            segment.background = android.graphics.drawable.StateListDrawable().apply {
                addState(
                    intArrayOf(android.R.attr.state_activated),
                    GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 8f * density
                        setColor(colors.accent)
                    }
                )
                addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
            }
            segment.imageTintList =
                ColorStateList(states, intArrayOf(colors.onAccent, colors.textSecondary))
        }
    }

    // ── Password locks ─────────────────────────────────────────────

    /** Inline message shown while a password lock is cooling down. */
    protected fun passwordLockoutMessage(remainingMs: Long): String {
        val minutes = ((remainingMs + MS_PER_MINUTE - 1) / MS_PER_MINUTE).toInt().coerceAtLeast(1)
        return resources.getQuantityString(R.plurals.password_locked_out_minutes, minutes, minutes)
    }

    /** Inline message shown after a wrong password that did not start the cooldown. */
    protected fun passwordAttemptsLeftMessage(remaining: Int): String =
        resources.getQuantityString(R.plurals.password_attempts_left, remaining, remaining)

    /**
     * Asks for the detox password and calls [onUnlocked] once it has been verified.
     *
     * Wrong passwords never end the session: they consume attempts from the shared
     * [app.focusphone.security.PasswordAttemptPolicy] and, once the budget is spent, the
     * dialog only reports how long the cooldown still lasts. Errors are shown inline so the
     * dialog stays open, and the deliberately slow PBKDF2 check runs off the main thread.
     */
    protected fun showDetoxUnlockDialog(onUnlocked: () -> Unit) {
        val view = layoutInflater.inflate(R.layout.dialog_detox_password_verify, null)
        applyDialogSurface(view)
        applyFontFamilyToView(view)
        val dialog = AlertDialog.Builder(this).setView(view).create()

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // Passwords are typed here: keep them out of screenshots and screen recordings.
            addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        val etPassword = view.findViewById<EditText>(R.id.et_detox_password_verify)
        val tvError = view.findViewById<TextView>(R.id.tv_password_error)
        val btnOk = view.findViewById<View>(R.id.btn_ok)

        fun showError(message: String) {
            tvError.text = message
            tvError.visibility = View.VISIBLE
        }

        if (ProfilePreferences.isPasswordLockedOut(this)) {
            showError(passwordLockoutMessage(ProfilePreferences.getLockoutRemainingMs(this)))
        }

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener { dialog.dismiss() }

        btnOk.setOnClickListener {
            if (ProfilePreferences.isPasswordLockedOut(this)) {
                showError(passwordLockoutMessage(ProfilePreferences.getLockoutRemainingMs(this)))
                return@setOnClickListener
            }
            val entered = etPassword.text.toString()
            btnOk.isEnabled = false
            lifecycleScope.launch {
                val verified = withContext(Dispatchers.Default) {
                    ProfilePreferences.verifyDetoxPassword(applicationContext, entered)
                }
                btnOk.isEnabled = true
                if (!dialog.isShowing) return@launch
                when {
                    verified -> {
                        dialog.dismiss()
                        onUnlocked()
                    }
                    ProfilePreferences.isPasswordLockedOut(this@BaseActivity) -> {
                        etPassword.text.clear()
                        showError(passwordLockoutMessage(ProfilePreferences.getLockoutRemainingMs(this@BaseActivity)))
                    }
                    else -> {
                        etPassword.text.clear()
                        showError(passwordAttemptsLeftMessage(ProfilePreferences.getRemainingAttempts(this@BaseActivity)))
                    }
                }
            }
        }

        trackDialog(dialog)
        dialog.show()
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
