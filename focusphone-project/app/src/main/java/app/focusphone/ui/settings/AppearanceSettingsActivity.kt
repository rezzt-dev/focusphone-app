package app.focusphone.ui.settings

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import app.focusphone.R
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.data.preferences.WallpaperPreferences
import app.focusphone.data.preferences.WallpaperTarget
import app.focusphone.databinding.ItemSettingsFontBinding
import app.focusphone.databinding.ItemSettingsValueBinding
import app.focusphone.databinding.SettingsCustomThemePanelBinding
import app.focusphone.databinding.SettingsSliderRowBinding
import app.focusphone.databinding.SettingsWallpaperPreviewBinding
import app.focusphone.domain.model.AppFont
import app.focusphone.ui.theme.FocusColors
import app.focusphone.ui.theme.ScrimSurface
import app.focusphone.ui.theme.SurfaceScrim
import app.focusphone.ui.wallpaper.WallpaperStorage
import app.focusphone.ui.wallpaper.WallpaperUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Appearance screen: theme (with custom colours), font, text size,
 * alignment and status-bar visibility.
 */
class AppearanceSettingsActivity : SettingsSubActivity() {

    private var themeValueView: TextView? = null
    private var fontValueView: TextView? = null
    private var customThemeBinding: SettingsCustomThemePanelBinding? = null
    private var bgTextWatcher: TextWatcher? = null
    private var fgTextWatcher: TextWatcher? = null

    // ── Wallpaper section state ──

    /** Everything below the master switch; collapsed while the wallpaper is hidden. */
    private var wallpaperDetails: View? = null
    private var previewBinding: SettingsWallpaperPreviewBinding? = null

    /** Home row, relabelled as the single "image" row in shared mode. */
    private var homeWallpaperRow: ItemSettingsValueBinding? = null

    /** Notifications and settings rows, hidden together in shared mode. */
    private var perSurfaceWallpaperRows: View? = null
    private var notificationsValueView: TextView? = null
    private var settingsValueView: TextView? = null
    private var blurSliderRoot: View? = null
    private var pendingWallpaperTarget: WallpaperTarget? = null

    /** Slider positions while dragging, so the preview follows before anything is stored. */
    private var liveBlurIntensity = 0
    private var liveReadability = WallpaperPreferences.DEFAULT_READABILITY

    /** true while a picked image is being copied; further taps are ignored meanwhile. */
    private var wallpaperBusy = false
    private var previewGeneration = 0

    private val wallpaperPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = pendingWallpaperTarget ?: return@registerForActivityResult
        pendingWallpaperTarget = null
        uri?.let { handleWallpaperSelected(it, target) }
    }

    override fun getScreenTitleRes(): Int = R.string.appearance_settings_title

    override fun onCreate(savedInstanceState: Bundle?) {
        // The picker can outlive this instance (rotation, process death while choosing);
        // without the target its result used to be dropped silently.
        pendingWallpaperTarget = savedInstanceState?.getString(STATE_PENDING_TARGET)
            ?.let { name -> WallpaperTarget.entries.firstOrNull { it.name == name } }
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingWallpaperTarget?.let { outState.putString(STATE_PENDING_TARGET, it.name) }
    }

    override fun buildContent(container: LinearLayout) {
        // ── Theme ────────────────────────────────────────────────────
        container.settingsSection(R.string.settings_section_appearance) {
            themeValueView = addValueRow(
                titleResId = R.string.settings_theme_label,
                subtitleResId = R.string.settings_theme_desc,
                initialValue = getThemeDisplayName(
                    ThemePreferences.getTheme(this@AppearanceSettingsActivity)
                ).lowercase(),
                iconResId = R.drawable.ic_settings_theme,
                onClick = ::showThemeSelectionDialog
            )

            // Custom theme panel (shown only when the custom theme is active)
            val customBinding = SettingsCustomThemePanelBinding.inflate(inflater, this, false)
            customBinding.root.setTag(R.id.tag_skip_recolor, true)
            customThemeBinding = customBinding
            setupCustomThemePanel(customBinding)
            addView(customBinding.root)
            updateCustomThemePanelVisibility()
        }

        // ── Font ─────────────────────────────────────────────────────
        container.settingsSection(R.string.settings_section_font) {
            fontValueView = addValueRow(
                titleResId = R.string.settings_font_label,
                subtitleResId = R.string.settings_font_desc,
                initialValue = getFontDisplayName(
                    FontPreferences.getFont(this@AppearanceSettingsActivity)
                ).lowercase(),
                iconResId = R.drawable.ic_settings_font,
                onClick = ::showFontSelectionDialog
            )
        }

        // ── Text size, alignment and status bar ──────────────────────
        // Previously headed "appearance" a second time, which read as a duplicated section.
        container.settingsSection(R.string.settings_section_text_layout) {
            val currentSize = FontPreferences.getFontSizePt(this@AppearanceSettingsActivity)
            val sizeBinding = SettingsSliderRowBinding.inflate(inflater, this, false)
            sizeBinding.ivIcon.setImageResource(R.drawable.ic_settings_text_size)
            sizeBinding.ivIcon.visibility = View.VISIBLE
            sizeBinding.tvLabel.text = getString(R.string.settings_text_size_pt, currentSize)
            sizeBinding.slider.apply {
                valueFrom = 8f
                valueTo = 24f
                stepSize = 1f
                value = currentSize.toFloat().coerceIn(8f, 24f)
                addOnChangeListener { _, value, _ ->
                    val size = value.toInt()
                    sizeBinding.tvLabel.text = getString(R.string.settings_text_size_pt, size)
                }
                persistOnStop { value ->
                    FontPreferences.setFontSizePt(this@AppearanceSettingsActivity, value.toInt())
                }
            }
            addView(sizeBinding.root)

            val alignmentView = inflater.inflate(R.layout.settings_alignment_selector, this, false)
            setupAlignmentSelector(alignmentView)
            addView(alignmentView)

            addSwitchRow(
                titleResId = R.string.settings_hide_status_bar,
                subtitleResId = R.string.settings_hide_status_bar_desc,
                isChecked = ThemePreferences.isStatusBarHidden(this@AppearanceSettingsActivity),
                iconResId = R.drawable.ic_settings_status_bar,
                onCheckedChanged = { checked ->
                    ThemePreferences.setStatusBarHidden(this@AppearanceSettingsActivity, checked)
                    applyStatusBarVisibility()
                }
            )
        }

        // ── Wallpaper ────────────────────────────────────────────────
        // The master switch sits alone: hiding the wallpaper collapses everything else,
        // while the chosen images stay stored for when it is shown again.
        container.settingsSection(R.string.settings_section_wallpaper) {
            addSwitchRow(
                titleResId = R.string.settings_wallpaper_enabled_label,
                subtitleResId = R.string.settings_wallpaper_enabled_desc,
                isChecked = WallpaperPreferences.isWallpaperEnabled(this@AppearanceSettingsActivity),
                iconResId = R.drawable.ic_settings_wallpaper,
                onCheckedChanged = { checked ->
                    WallpaperPreferences.setWallpaperEnabled(this@AppearanceSettingsActivity, checked)
                    updateWallpaperSection()
                    applyWallpaper()
                }
            )
        }
        wallpaperDetails = container.settingsSection { buildWallpaperDetails() }
        updateWallpaperSection()
    }

    override fun onDestroy() {
        bgTextWatcher?.let { customThemeBinding?.etCustomBgColor?.removeTextChangedListener(it) }
        fgTextWatcher?.let { customThemeBinding?.etCustomFgColor?.removeTextChangedListener(it) }
        super.onDestroy()
    }

    // ── Theme ──────────────────────────────────────────────────────

    private fun applyTheme(theme: String) {
        if (ThemePreferences.getTheme(this) == theme) return
        ThemePreferences.setTheme(this, theme)
        relaunchForThemeChange()
    }

    /**
     * Restarts this screen so the new theme is applied before `setContentView`, which is the
     * only point at which a style theme can take effect.
     */
    private fun relaunchForThemeChange() {
        val relaunch = Intent(this, AppearanceSettingsActivity::class.java)
        // BaseActivity.finish() and startActivity() already apply the fade transition.
        finish()
        startActivity(relaunch)
    }

    private fun getThemeDisplayName(theme: String): String = when (theme) {
        ThemePreferences.THEME_OFF_WHITE -> getString(R.string.settings_theme_off_white)
        ThemePreferences.THEME_SLATE -> getString(R.string.settings_theme_slate)
        ThemePreferences.THEME_WARM_BEIGE -> getString(R.string.settings_theme_warm_beige)
        ThemePreferences.THEME_NAVY -> getString(R.string.settings_theme_navy)
        ThemePreferences.THEME_CUSTOM -> getString(R.string.settings_theme_custom)
        ThemePreferences.THEME_AUTO -> getString(R.string.settings_theme_auto)
        else -> getString(R.string.settings_theme_slate)
    }

    private fun updateCustomThemePanelVisibility() {
        val isCustom = ThemePreferences.getTheme(this) == ThemePreferences.THEME_CUSTOM
        customThemeBinding?.root?.visibility = if (isCustom) View.VISIBLE else View.GONE
    }

    private fun showThemeSelectionDialog() {
        val view = inflater.inflate(R.layout.dialog_theme_select, null)
        applyDialogSurface(view)
        applyFontFamilyToView(view)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawable(
            android.graphics.Color.TRANSPARENT.toDrawable()
        )

        val customColor = ThemePreferences.getCustomBackgroundColor(this)
        val customDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(customColor)
            setStroke((1.5f * resources.displayMetrics.density).toInt(), "#3A3A3A".toColorInt())
        }
        view.findViewById<View>(R.id.view_theme_custom_circle_dialog).background = customDrawable
        view.findViewById<View>(R.id.view_theme_custom_circle_dialog).backgroundTintList = null

        view.findViewById<View>(R.id.btn_theme_off_white).setOnClickListener {
            applyTheme(ThemePreferences.THEME_OFF_WHITE)
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_theme_slate).setOnClickListener {
            applyTheme(ThemePreferences.THEME_SLATE)
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_theme_warm_beige).setOnClickListener {
            applyTheme(ThemePreferences.THEME_WARM_BEIGE)
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_theme_navy).setOnClickListener {
            applyTheme(ThemePreferences.THEME_NAVY)
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_theme_auto).setOnClickListener {
            applyTheme(ThemePreferences.THEME_AUTO)
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_theme_custom).setOnClickListener {
            applyTheme(ThemePreferences.THEME_CUSTOM)
            dialog.dismiss()
        }

        showExclusiveDialog(dialog)
    }

    private fun setupCustomThemePanel(binding: SettingsCustomThemePanelBinding) {
        val currentBgHex = ThemePreferences.getCustomBackgroundColorHex(this)
        val currentFgHex = ThemePreferences.getCustomForegroundColorHex(this)

        binding.etCustomBgColor.setText(currentBgHex)
        binding.etCustomFgColor.setText(currentFgHex)

        try {
            val bgCol = currentBgHex.toColorInt()
            binding.viewCustomBgPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(bgCol)
        } catch (_: Exception) {}

        try {
            val fgCol = currentFgHex.toColorInt()
            binding.viewCustomFgPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(fgCol)
        } catch (_: Exception) {}

        bgTextWatcher = object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                val raw = s?.toString() ?: ""
                if (!raw.startsWith("#")) {
                    isUpdating = true
                    val fixed = "#" + raw.replace("#", "")
                    binding.etCustomBgColor.setText(fixed)
                    try {
                        binding.etCustomBgColor.setSelection(fixed.length)
                    } catch (_: Exception) {}
                    isUpdating = false
                    updateBgPreview(fixed)
                    return
                }
                updateBgPreview(raw)
            }

            private fun updateBgPreview(hex: String) {
                if (hex.length == 7) {
                    try {
                        val col = hex.toColorInt()
                        binding.viewCustomBgPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(col)
                    } catch (_: Exception) {}
                }
            }
        }
        binding.etCustomBgColor.addTextChangedListener(bgTextWatcher!!)

        fgTextWatcher = object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                val raw = s?.toString() ?: ""
                if (!raw.startsWith("#")) {
                    isUpdating = true
                    val fixed = "#" + raw.replace("#", "")
                    binding.etCustomFgColor.setText(fixed)
                    try {
                        binding.etCustomFgColor.setSelection(fixed.length)
                    } catch (_: Exception) {}
                    isUpdating = false
                    updateFgPreview(fixed)
                    return
                }
                updateFgPreview(raw)
            }

            private fun updateFgPreview(hex: String) {
                if (hex.length == 7) {
                    try {
                        val col = hex.toColorInt()
                        binding.viewCustomFgPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(col)
                    } catch (_: Exception) {}
                }
            }
        }
        binding.etCustomFgColor.addTextChangedListener(fgTextWatcher!!)

        binding.btnApplyCustomTheme.setOnClickListener {
            val bgHex = binding.etCustomBgColor.text.toString().trim()
            val fgHex = binding.etCustomFgColor.text.toString().trim()
            if (!isValidHexColor(bgHex) || !isValidHexColor(fgHex)) {
                // Silently doing nothing left the user tapping a dead button.
                Toast.makeText(this, R.string.settings_custom_theme_invalid, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            ThemePreferences.setCustomBackgroundColorHex(this, bgHex)
            ThemePreferences.setCustomForegroundColorHex(this, fgHex)
            // Always relaunch: when the custom theme is *already* selected only the two
            // colours changed, and applyTheme() short-circuits on an unchanged theme id.
            relaunchForThemeChange()
        }
    }

    private fun isValidHexColor(hex: String): Boolean =
        hex.matches(Regex("^#[0-9A-Fa-f]{6}$"))

    // ── Font ───────────────────────────────────────────────────────

    private fun getFontDisplayName(font: AppFont): String = when (font) {
        AppFont.SYSTEM -> getString(R.string.settings_font_system)
        else -> font.displayName
    }

    private fun showFontSelectionDialog() {
        val current = FontPreferences.getFont(this)
        val view = inflater.inflate(R.layout.dialog_font_select, null)
        applyDialogSurface(view)
        applyFontFamilyToView(view)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawable(
            android.graphics.Color.TRANSPARENT.toDrawable()
        )

        val container = view.findViewById<LinearLayout>(R.id.font_options_container)
        container.removeAllViews()

        AppFont.entries.forEachIndexed { index, font ->
            val item = ItemSettingsFontBinding.inflate(inflater, container, false)
            item.tvFontName.text = getFontDisplayName(font).lowercase()
            item.tvFontSample.text = getString(R.string.settings_font_sample_text)

            if (font == AppFont.SYSTEM) {
                item.tvFontSample.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            } else {
                font.fontResId?.let { resId ->
                    try {
                        ResourcesCompat.getFont(this, resId)?.let {
                            item.tvFontSample.typeface = it
                        }
                    } catch (e: Exception) {
                        Log.w("AppearanceSettings", "Failed to load font preview for resId=$resId", e)
                    }
                }
            }

            val isActive = font == current
            item.tvCheck.visibility = if (isActive) View.VISIBLE else View.INVISIBLE
            item.root.alpha = if (isActive) 1f else 0.6f

            item.root.setOnClickListener {
                if (FontPreferences.getFont(this) != font) {
                    FontPreferences.setFont(this, font)
                    dialog.dismiss()
                    recreate()
                }
            }

            container.addView(item.root)

            if (index < AppFont.entries.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (1 * resources.displayMetrics.density).toInt()
                    ).apply {
                        marginStart = (16 * resources.displayMetrics.density).toInt()
                        marginEnd = (16 * resources.displayMetrics.density).toInt()
                    }
                    setBackgroundColor(resources.getColor(android.R.color.darker_gray, theme))
                    alpha = 0.1f
                }
                container.addView(divider)
            }
        }

        showExclusiveDialog(dialog)
    }

    // ── Alignment ──────────────────────────────────────────────────

    private fun setupAlignmentSelector(view: View) {
        val current = ThemePreferences.getAlignment(this)
        val btnLeft = view.findViewById<ImageView>(R.id.btn_align_left)
        val btnCenter = view.findViewById<ImageView>(R.id.btn_align_center)
        val btnRight = view.findViewById<ImageView>(R.id.btn_align_right)

        // Rebuild the pill from the palette so the selected segment is correct under every
        // theme — the XML selectors resolve to the style theme and miss custom colours.
        (btnLeft.parent as? View)?.let {
            applySegmentedControlColors(it, listOf(btnLeft, btnCenter, btnRight))
        }

        fun updateUI(alignment: String) {
            btnLeft.isActivated = (alignment == ThemePreferences.ALIGN_LEFT)
            btnCenter.isActivated = (alignment == ThemePreferences.ALIGN_CENTER)
            btnRight.isActivated = (alignment == ThemePreferences.ALIGN_RIGHT)
        }

        updateUI(current)

        btnLeft.setOnClickListener {
            ThemePreferences.setAlignment(this, ThemePreferences.ALIGN_LEFT)
            updateUI(ThemePreferences.ALIGN_LEFT)
        }
        btnCenter.setOnClickListener {
            ThemePreferences.setAlignment(this, ThemePreferences.ALIGN_CENTER)
            updateUI(ThemePreferences.ALIGN_CENTER)
        }
        btnRight.setOnClickListener {
            ThemePreferences.setAlignment(this, ThemePreferences.ALIGN_RIGHT)
            updateUI(ThemePreferences.ALIGN_RIGHT)
        }
    }

    // ── Wallpaper ────────────────────────────────────────────────────

    /**
     * Builds everything below the master switch: a tappable preview, the shared-mode
     * switch, one row per screen (a single row in shared mode), blur and legibility.
     */
    private fun LinearLayout.buildWallpaperDetails() {
        val activity = this@AppearanceSettingsActivity
        liveBlurIntensity = WallpaperPreferences.getBlurIntensity(activity)
        liveReadability = WallpaperPreferences.getReadability(activity)

        val preview = SettingsWallpaperPreviewBinding.inflate(inflater, this, false)
        preview.root.setTag(R.id.tag_skip_recolor, true)
        preview.root.setOnClickListener { onWallpaperRowClicked(WallpaperTarget.HOME) }
        stylePreview(preview)
        previewBinding = preview
        addView(preview.root)

        addSwitchRow(
            titleResId = R.string.settings_wallpaper_shared_label,
            subtitleResId = R.string.settings_wallpaper_shared_desc,
            isChecked = WallpaperPreferences.isUseSharedWallpaper(activity),
            iconResId = R.drawable.ic_settings_wallpaper,
            onCheckedChanged = ::setSharedWallpaperMode
        )

        homeWallpaperRow = addValueRowBinding(
            titleResId = R.string.settings_wallpaper_home,
            subtitleResId = R.string.settings_wallpaper_home_desc,
            initialValue = getWallpaperStatus(WallpaperTarget.HOME),
            iconResId = R.drawable.ic_settings_wallpaper,
            onClick = { onWallpaperRowClicked(WallpaperTarget.HOME) }
        )

        // One block with its own separators, so hiding it in shared mode leaves no stray
        // hairline behind.
        val perSurface = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        perSurface.addRowDivider()
        notificationsValueView = perSurface.addValueRow(
            titleResId = R.string.settings_wallpaper_notifications,
            subtitleResId = R.string.settings_wallpaper_notifications_desc,
            initialValue = getWallpaperStatus(WallpaperTarget.NOTIFICATIONS),
            iconResId = R.drawable.ic_settings_wallpaper,
            onClick = { onWallpaperRowClicked(WallpaperTarget.NOTIFICATIONS) }
        )
        perSurface.addRowDivider()
        settingsValueView = perSurface.addValueRow(
            titleResId = R.string.settings_wallpaper_settings,
            subtitleResId = R.string.settings_wallpaper_settings_desc,
            initialValue = getWallpaperStatus(WallpaperTarget.SETTINGS),
            iconResId = R.drawable.ic_settings_wallpaper,
            onClick = { onWallpaperRowClicked(WallpaperTarget.SETTINGS) }
        )
        addView(perSurface)
        perSurfaceWallpaperRows = perSurface
        addRowDivider()

        val blurEnabled = WallpaperPreferences.isBlurEnabled(activity)
        addSwitchRow(
            titleResId = R.string.settings_wallpaper_blur_label,
            subtitleResId = R.string.settings_wallpaper_blur_desc,
            isChecked = blurEnabled,
            iconResId = R.drawable.ic_settings_blur,
            onCheckedChanged = { checked ->
                WallpaperPreferences.setBlurEnabled(activity, checked)
                blurSliderRoot?.visibility = if (checked) View.VISIBLE else View.GONE
                onWallpaperLookChanged()
            }
        )

        // The intensity only matters while blur is on, so it is shown only then.
        val blurBinding = SettingsSliderRowBinding.inflate(inflater, this, false)
        blurBinding.ivIcon.setImageResource(R.drawable.ic_settings_blur)
        blurBinding.ivIcon.visibility = View.VISIBLE
        blurBinding.tvLabel.text = getString(R.string.settings_wallpaper_blur_intensity, liveBlurIntensity)
        blurBinding.slider.apply {
            valueFrom = 0f
            valueTo = 100f
            stepSize = 1f
            value = liveBlurIntensity.toFloat().coerceIn(0f, 100f)
            addOnChangeListener { _, value, _ ->
                liveBlurIntensity = value.toInt()
                blurBinding.tvLabel.text = getString(R.string.settings_wallpaper_blur_intensity, liveBlurIntensity)
                updatePreviewEffects()
            }
            persistOnStop { value ->
                WallpaperPreferences.setBlurIntensity(activity, value.toInt())
                onWallpaperLookChanged()
            }
        }
        blurBinding.root.visibility = if (blurEnabled) View.VISIBLE else View.GONE
        blurSliderRoot = blurBinding.root
        addView(blurBinding.root)

        // ── Legibility ──
        // The single control behind every scrim in the app: it decides how strongly panels
        // are dimmed so text survives a busy photo. Blur already softens the image, so
        // SurfaceScrim reduces the scrim automatically when blur is on.
        val readabilityBinding = SettingsSliderRowBinding.inflate(inflater, this, false)
        readabilityBinding.ivIcon.setImageResource(R.drawable.ic_settings_text_size)
        readabilityBinding.ivIcon.visibility = View.VISIBLE
        readabilityBinding.tvLabel.text = getString(R.string.settings_wallpaper_readability, liveReadability)
        readabilityBinding.tvDescription.setText(R.string.settings_wallpaper_readability_hint)
        readabilityBinding.tvDescription.visibility = View.VISIBLE
        readabilityBinding.slider.apply {
            valueFrom = 0f
            valueTo = 100f
            stepSize = 5f
            value = liveReadability.toFloat().coerceIn(0f, 100f)
            addOnChangeListener { _, value, _ ->
                liveReadability = value.toInt()
                readabilityBinding.tvLabel.text = getString(R.string.settings_wallpaper_readability, liveReadability)
                updatePreviewEffects()
            }
            persistOnStop { value ->
                WallpaperPreferences.setReadability(activity, value.toInt())
                refreshWallpaperLegibility()
            }
        }
        addView(readabilityBinding.root)
    }

    private fun LinearLayout.addRowDivider() {
        addView(inflater.inflate(R.layout.settings_divider, this, false))
    }

    /**
     * Paints the preview frame from the palette — its XML shape resolves theme attributes,
     * which miss the custom theme — and clips the image to its rounded corners.
     */
    private fun stylePreview(preview: SettingsWallpaperPreviewBinding) {
        val density = resources.displayMetrics.density
        preview.root.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f * density
            setColor(FocusColors.blend(palette.textPrimary, palette.background, 0.06f))
            setStroke(
                (1.5f * density).toInt().coerceAtLeast(1),
                FocusColors.withAlpha(palette.textSecondary, 0.6f),
                6f * density,
                4f * density
            )
        }
        preview.root.clipToOutline = true
        preview.tvWallpaperPreviewClock.setTextColor(palette.textPrimary)
        preview.tvWallpaperPreviewClock.text = DateFormat.getTimeFormat(this).format(Date())
    }

    private fun updateWallpaperSection() {
        val enabled = WallpaperPreferences.isWallpaperEnabled(this)
        wallpaperDetails?.visibility = if (enabled) View.VISIBLE else View.GONE
        updateWallpaperRows()
        if (enabled) updateWallpaperPreview()
    }

    /** Relabels the rows for the current mode and refreshes their "set / not set" values. */
    private fun updateWallpaperRows() {
        val shared = WallpaperPreferences.isUseSharedWallpaper(this)
        homeWallpaperRow?.apply {
            tvTitle.setText(if (shared) R.string.settings_wallpaper_image else R.string.settings_wallpaper_home)
            tvSubtitle.setText(
                if (shared) R.string.settings_wallpaper_shared_row_desc else R.string.settings_wallpaper_home_desc
            )
            tvValue.text = getWallpaperStatus(WallpaperTarget.HOME)
        }
        perSurfaceWallpaperRows?.visibility = if (shared) View.GONE else View.VISIBLE
        notificationsValueView?.text = getWallpaperStatus(WallpaperTarget.NOTIFICATIONS)
        settingsValueView?.text = getWallpaperStatus(WallpaperTarget.SETTINGS)
    }

    private fun getWallpaperStatus(target: WallpaperTarget): String =
        if (WallpaperUtils.getWallpaperUri(this, target) == null) {
            getString(R.string.settings_wallpaper_not_set)
        } else {
            getString(R.string.settings_wallpaper_set)
        }

    private fun launchWallpaperPicker(target: WallpaperTarget) {
        pendingWallpaperTarget = target
        wallpaperPicker.launch(
            androidx.activity.result.PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                .build()
        )
    }

    /**
     * Stores a picked image. The Photo Picker grant dies with the process, so the image is
     * copied into private storage rather than referenced by its `content://` URI.
     */
    private fun handleWallpaperSelected(uri: Uri, target: WallpaperTarget) {
        wallpaperBusy = true
        showPreviewMessage(R.string.settings_wallpaper_applying)
        lifecycleScope.launch {
            val stored = WallpaperStorage.replace(this@AppearanceSettingsActivity, uri) { copy ->
                WallpaperPreferences.assignWallpaper(this@AppearanceSettingsActivity, target, copy)
            }
            wallpaperBusy = false
            if (isDestroyed) return@launch
            if (!stored) {
                Toast.makeText(
                    this@AppearanceSettingsActivity,
                    R.string.settings_wallpaper_import_failed,
                    Toast.LENGTH_LONG
                ).show()
                updateWallpaperPreview()
                return@launch
            }
            onWallpaperChanged()
        }
    }

    /**
     * Opens the picker straight away when choosing is the only thing to do; otherwise
     * offers to change the image, reuse the home one on this screen, or remove it.
     */
    private fun onWallpaperRowClicked(target: WallpaperTarget) {
        if (wallpaperBusy) return
        val shared = WallpaperPreferences.isUseSharedWallpaper(this)
        val current = WallpaperUtils.getWallpaperUri(this, target)
        val homeUri = WallpaperPreferences.getHomeWallpaperUri(this)
            ?.takeIf { !shared && target != WallpaperTarget.HOME && it != current }

        val actions = buildList<Pair<String, () -> Unit>> {
            val chooseLabel = if (current == null) R.string.settings_wallpaper_choose else R.string.settings_wallpaper_change
            add(getString(chooseLabel) to { launchWallpaperPicker(target) })
            homeUri?.let { uri -> add(getString(R.string.settings_wallpaper_use_home) to { reuseWallpaper(target, uri) }) }
            if (current != null) add(getString(R.string.settings_wallpaper_remove) to { removeWallpaper(target) })
        }
        if (actions.size == 1) {
            actions.single().second()
            return
        }

        val title = when {
            shared -> R.string.settings_wallpaper_image
            target == WallpaperTarget.HOME -> R.string.settings_wallpaper_home
            target == WallpaperTarget.NOTIFICATIONS -> R.string.settings_wallpaper_notifications
            else -> R.string.settings_wallpaper_settings
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setItems(actions.map { it.first }.toTypedArray()) { _, which -> actions[which].second() }
            .create()
            .also { showStyledDialog(it) }
    }

    /** Shows the image already stored as [uri] on [target] too, without copying it again. */
    private fun reuseWallpaper(target: WallpaperTarget, uri: String) = updateWallpapers {
        WallpaperPreferences.assignWallpaper(this, target, uri)
    }

    /** Forgets the image of [target] (every surface in shared mode) and deletes its copy. */
    private fun removeWallpaper(target: WallpaperTarget) = updateWallpapers {
        WallpaperPreferences.assignWallpaper(this, target, null)
    }

    private fun setSharedWallpaperMode(enabled: Boolean) = updateWallpapers {
        WallpaperPreferences.setSharedMode(this, enabled)
    }

    /** Applies a change to the stored references, then refreshes the section and this screen. */
    private fun updateWallpapers(change: () -> Unit) {
        lifecycleScope.launch {
            WallpaperStorage.update(this@AppearanceSettingsActivity, change)
            if (isDestroyed) return@launch
            onWallpaperChanged()
        }
    }

    private fun onWallpaperChanged() {
        updateWallpaperRows()
        updateWallpaperPreview()
        applyWallpaper()
    }

    /** Blur changed: the preview and this screen's own background are redrawn with it. */
    private fun onWallpaperLookChanged() {
        updateWallpaperPreview()
        refreshWallpaperLegibility()
        applyWallpaper()
    }

    /** Loads the home image (the shared one in shared mode) into the preview. */
    private fun updateWallpaperPreview() {
        val preview = previewBinding ?: return
        val generation = ++previewGeneration
        if (wallpaperBusy) return

        val uri = WallpaperUtils.getWallpaperUri(this, WallpaperTarget.HOME)
        if (uri == null) {
            showPreviewMessage(R.string.settings_wallpaper_preview_hint)
            return
        }

        val blurEnabled = WallpaperPreferences.isBlurEnabled(this)
        val blurRadius = WallpaperUtils.getBlurRadius(liveBlurIntensity)
        lifecycleScope.launch {
            val result = WallpaperStorage.load(this@AppearanceSettingsActivity, uri, blurEnabled, blurRadius)
            if (generation != previewGeneration || wallpaperBusy || isDestroyed) return@launch

            val ready = result as? WallpaperStorage.LoadResult.Ready
            if (ready == null) {
                showPreviewMessage(R.string.settings_wallpaper_preview_hint)
                if (result == WallpaperStorage.LoadResult.Lost) updateWallpaperRows()
                return@launch
            }
            WallpaperUtils.applyWallpaperToImageView(
                preview.ivWallpaperPreview,
                ready.bitmap,
                blurRadius,
                blurEnabled && !ready.softwareBlurApplied
            )
            preview.tvWallpaperPreviewHint.visibility = View.GONE
            preview.tvWallpaperPreviewClock.visibility = View.VISIBLE
            updatePreviewEffects()
        }
    }

    /**
     * Applies the slider positions to the preview while they are being dragged: the home
     * scrim always, and the blur too where it is a render effect (Android 12+). Older
     * devices blur in software and pick the new radius up when the drag ends.
     */
    private fun updatePreviewEffects() {
        val preview = previewBinding ?: return
        if (preview.ivWallpaperPreview.visibility != View.VISIBLE) return
        val blurEnabled = WallpaperPreferences.isBlurEnabled(this)

        val alpha = SurfaceScrim.alphaFor(ScrimSurface.HOME, liveReadability, blurEnabled, liveBlurIntensity)
        preview.viewWallpaperPreviewScrim.apply {
            setBackgroundColor(palette.background)
            this.alpha = alpha
            visibility = if (alpha > 0.01f) View.VISIBLE else View.GONE
        }
        WallpaperUtils.updateLiveBlur(
            preview.ivWallpaperPreview,
            WallpaperUtils.getBlurRadius(liveBlurIntensity),
            blurEnabled
        )
    }

    private fun showPreviewMessage(@StringRes message: Int) {
        val preview = previewBinding ?: return
        WallpaperUtils.clearWallpaperEffect(preview.ivWallpaperPreview)
        preview.viewWallpaperPreviewScrim.visibility = View.GONE
        preview.tvWallpaperPreviewClock.visibility = View.GONE
        preview.tvWallpaperPreviewHint.setText(message)
        preview.tvWallpaperPreviewHint.visibility = View.VISIBLE
    }

    private companion object {
        const val STATE_PENDING_TARGET = "pending_wallpaper_target"
    }
}
