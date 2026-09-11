package app.focusphone.ui.settings

import android.app.Dialog
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import app.focusphone.R
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.data.preferences.WallpaperPreferences
import app.focusphone.data.preferences.WallpaperTarget
import app.focusphone.databinding.ItemSettingsFontBinding
import app.focusphone.databinding.SettingsCustomThemePanelBinding
import app.focusphone.databinding.SettingsSliderRowBinding
import app.focusphone.databinding.SettingsWallpaperPreviewBinding
import app.focusphone.domain.model.AppFont
import app.focusphone.ui.wallpaper.WallpaperStorage
import app.focusphone.ui.wallpaper.WallpaperUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Appearance screen: theme (with custom colours), font, text size,
 * alignment and status-bar visibility.
 */
class AppearanceSettingsActivity : SettingsSubActivity() {

    private var activeDialog: Dialog? = null
    private var themeValueView: TextView? = null
    private var fontValueView: TextView? = null
    private var customThemeBinding: SettingsCustomThemePanelBinding? = null
    private var bgTextWatcher: TextWatcher? = null
    private var fgTextWatcher: TextWatcher? = null

    private var previewBinding: SettingsWallpaperPreviewBinding? = null
    private var homeValueView: TextView? = null
    private var notificationsValueView: TextView? = null
    private var settingsValueView: TextView? = null
    private var pendingWallpaperTarget: WallpaperTarget? = null

    // Wallpaper section views whose visibility is toggled when the master switch changes.
    private var wallpaperSharedSwitchRoot: View? = null
    private var wallpaperPreviewRoot: View? = null
    private var wallpaperHomeRowRoot: View? = null
    private var wallpaperNotificationsRowRoot: View? = null
    private var wallpaperSettingsRowRoot: View? = null
    private var wallpaperBlurSwitchRoot: View? = null
    private var wallpaperBlurSliderRoot: View? = null
    private var wallpaperReadabilitySliderRoot: View? = null
    private var wallpaperDisabledInfoRoot: View? = null

    private val wallpaperPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = pendingWallpaperTarget ?: return@registerForActivityResult
        pendingWallpaperTarget = null
        uri?.let { handleWallpaperSelected(it, target) }
    }

    override fun getScreenTitleRes(): Int = R.string.appearance_settings_title

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
                    FontPreferences.setFontSizePt(this@AppearanceSettingsActivity, size)
                    sizeBinding.tvLabel.text = getString(R.string.settings_text_size_pt, size)
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
        container.addSectionHeader(R.string.settings_section_wallpaper)

        val wallpaperEnabled = WallpaperPreferences.isWallpaperEnabled(this)

        container.addSwitchRow(
            titleResId = R.string.settings_wallpaper_enabled_label,
            subtitleResId = R.string.settings_wallpaper_enabled_desc,
            isChecked = wallpaperEnabled,
            iconResId = R.drawable.ic_settings_wallpaper,
            onCheckedChanged = { checked ->
                WallpaperPreferences.setWallpaperEnabled(this, checked)
                updateWallpaperSectionVisibility()
                applyCustomThemeColors()
                applyWallpaper()
            }
        )

        val useShared = WallpaperPreferences.isUseSharedWallpaper(this)

        wallpaperSharedSwitchRoot = container.addSwitchRow(
            titleResId = R.string.settings_wallpaper_shared_label,
            subtitleResId = R.string.settings_wallpaper_shared_desc,
            isChecked = useShared,
            iconResId = R.drawable.ic_settings_wallpaper,
            onCheckedChanged = { checked ->
                WallpaperPreferences.setUseSharedWallpaper(this, checked)
                updateWallpaperValueLabels()
                updateWallpaperPreview()
            }
        )

        val preview = SettingsWallpaperPreviewBinding.inflate(inflater, container, false)
        previewBinding = preview
        wallpaperPreviewRoot = preview.root
        container.addView(preview.root)

        val (homeValue, homeRoot) = container.addWallpaperValueRow(
            titleResId = R.string.settings_wallpaper_home,
            subtitleResId = if (useShared) R.string.settings_wallpaper_shared_hint else R.string.settings_wallpaper_home_desc,
            initialValue = getWallpaperStatus(WallpaperTarget.HOME),
            iconResId = R.drawable.ic_settings_wallpaper,
            onClick = { launchWallpaperPicker(WallpaperTarget.HOME) }
        )
        homeValueView = homeValue
        wallpaperHomeRowRoot = homeRoot

        val (notificationsValue, notificationsRoot) = container.addWallpaperValueRow(
            titleResId = R.string.settings_wallpaper_notifications,
            subtitleResId = if (useShared) R.string.settings_wallpaper_shared_hint else R.string.settings_wallpaper_notifications_desc,
            initialValue = getWallpaperStatus(WallpaperTarget.NOTIFICATIONS),
            iconResId = R.drawable.ic_settings_wallpaper,
            onClick = { launchWallpaperPicker(WallpaperTarget.NOTIFICATIONS) }
        )
        notificationsValueView = notificationsValue
        wallpaperNotificationsRowRoot = notificationsRoot

        val (settingsValue, settingsRoot) = container.addWallpaperValueRow(
            titleResId = R.string.settings_wallpaper_settings,
            subtitleResId = if (useShared) R.string.settings_wallpaper_shared_hint else R.string.settings_wallpaper_settings_desc,
            initialValue = getWallpaperStatus(WallpaperTarget.SETTINGS),
            iconResId = R.drawable.ic_settings_wallpaper,
            onClick = { launchWallpaperPicker(WallpaperTarget.SETTINGS) }
        )
        settingsValueView = settingsValue
        wallpaperSettingsRowRoot = settingsRoot

        wallpaperBlurSwitchRoot = container.addSwitchRow(
            titleResId = R.string.settings_wallpaper_blur_label,
            subtitleResId = R.string.settings_wallpaper_blur_desc,
            isChecked = WallpaperPreferences.isBlurEnabled(this),
            iconResId = R.drawable.ic_settings_blur,
            onCheckedChanged = { checked ->
                WallpaperPreferences.setBlurEnabled(this, checked)
                updateWallpaperPreview()
            }
        )

        val blurBinding = SettingsSliderRowBinding.inflate(inflater, container, false)
        blurBinding.ivIcon.setImageResource(R.drawable.ic_settings_blur)
        blurBinding.ivIcon.visibility = View.VISIBLE
        val currentBlur = WallpaperPreferences.getBlurIntensity(this)
        blurBinding.tvLabel.text = getString(R.string.settings_wallpaper_blur_intensity, currentBlur)
        blurBinding.slider.apply {
            valueFrom = 0f
            valueTo = 100f
            stepSize = 1f
            value = currentBlur.toFloat().coerceIn(0f, 100f)
            addOnChangeListener { _, value, _ ->
                val intensity = value.toInt()
                WallpaperPreferences.setBlurIntensity(this@AppearanceSettingsActivity, intensity)
                blurBinding.tvLabel.text = getString(R.string.settings_wallpaper_blur_intensity, intensity)
                updateWallpaperPreview()
            }
        }
        wallpaperBlurSliderRoot = blurBinding.root
        container.addView(blurBinding.root)

        // ── Legibility ───────────────────────────────────────────────
        // The single control behind every scrim in the app: it decides how strongly panels
        // are dimmed so text survives a busy photo. Blur already softens the image, so
        // SurfaceScrim reduces the scrim automatically when blur is on.
        val currentReadability = WallpaperPreferences.getReadability(this)
        val readabilityBinding = SettingsSliderRowBinding.inflate(inflater, container, false)
        readabilityBinding.ivIcon.setImageResource(R.drawable.ic_settings_text_size)
        readabilityBinding.ivIcon.visibility = View.VISIBLE
        readabilityBinding.tvLabel.text =
            getString(R.string.settings_wallpaper_readability, currentReadability)
        readabilityBinding.tvDescription.setText(R.string.settings_wallpaper_readability_hint)
        readabilityBinding.tvDescription.visibility = View.VISIBLE
        readabilityBinding.slider.apply {
            valueFrom = 0f
            valueTo = 100f
            stepSize = 5f
            value = currentReadability.toFloat().coerceIn(0f, 100f)
            addOnChangeListener { _, value, _ ->
                val level = value.toInt()
                WallpaperPreferences.setReadability(this@AppearanceSettingsActivity, level)
                readabilityBinding.tvLabel.text =
                    getString(R.string.settings_wallpaper_readability, level)
                // Repaint this screen straight away so the slider previews its own effect.
                applyScrim()
            }
        }
        wallpaperReadabilitySliderRoot = readabilityBinding.root
        container.addView(readabilityBinding.root)

        wallpaperDisabledInfoRoot = container.addInfoRow(
            titleResId = R.string.settings_wallpaper_disabled_info,
            subtitleResId = R.string.settings_wallpaper_disabled_info_desc,
            onClick = { }
        )

        updateWallpaperSectionVisibility()
    }

    override fun onDestroy() {
        activeDialog?.dismiss()
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
        finish()
        startActivity(relaunch)
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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
        activeDialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        activeDialog?.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )

        val customColor = ThemePreferences.getCustomBackgroundColor(this)
        val customDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(customColor)
            setStroke((1.5f * resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor("#3A3A3A"))
        }
        view.findViewById<View>(R.id.view_theme_custom_circle_dialog).background = customDrawable
        view.findViewById<View>(R.id.view_theme_custom_circle_dialog).backgroundTintList = null

        view.findViewById<View>(R.id.btn_theme_off_white).setOnClickListener {
            applyTheme(ThemePreferences.THEME_OFF_WHITE)
            activeDialog?.dismiss()
        }
        view.findViewById<View>(R.id.btn_theme_slate).setOnClickListener {
            applyTheme(ThemePreferences.THEME_SLATE)
            activeDialog?.dismiss()
        }
        view.findViewById<View>(R.id.btn_theme_warm_beige).setOnClickListener {
            applyTheme(ThemePreferences.THEME_WARM_BEIGE)
            activeDialog?.dismiss()
        }
        view.findViewById<View>(R.id.btn_theme_navy).setOnClickListener {
            applyTheme(ThemePreferences.THEME_NAVY)
            activeDialog?.dismiss()
        }
        view.findViewById<View>(R.id.btn_theme_auto).setOnClickListener {
            applyTheme(ThemePreferences.THEME_AUTO)
            activeDialog?.dismiss()
        }
        view.findViewById<View>(R.id.btn_theme_custom).setOnClickListener {
            applyTheme(ThemePreferences.THEME_CUSTOM)
            activeDialog?.dismiss()
        }

        activeDialog?.show()
    }

    private fun setupCustomThemePanel(binding: SettingsCustomThemePanelBinding) {
        val currentBgHex = ThemePreferences.getCustomBackgroundColorHex(this)
        val currentFgHex = ThemePreferences.getCustomForegroundColorHex(this)

        binding.etCustomBgColor.setText(currentBgHex)
        binding.etCustomFgColor.setText(currentFgHex)

        try {
            val bgCol = android.graphics.Color.parseColor(currentBgHex)
            binding.viewCustomBgPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(bgCol)
        } catch (_: Exception) {}

        try {
            val fgCol = android.graphics.Color.parseColor(currentFgHex)
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
                    try { binding.etCustomBgColor.setSelection(fixed.length) } catch (_: Exception) {}
                    isUpdating = false
                    updateBgPreview(fixed)
                    return
                }
                updateBgPreview(raw)
            }

            private fun updateBgPreview(hex: String) {
                if (hex.length == 7) {
                    try {
                        val col = android.graphics.Color.parseColor(hex)
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
                    try { binding.etCustomFgColor.setSelection(fixed.length) } catch (_: Exception) {}
                    isUpdating = false
                    updateFgPreview(fixed)
                    return
                }
                updateFgPreview(raw)
            }

            private fun updateFgPreview(hex: String) {
                if (hex.length == 7) {
                    try {
                        val col = android.graphics.Color.parseColor(hex)
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
        activeDialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        activeDialog?.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )

        val container = view.findViewById<LinearLayout>(R.id.font_options_container)
        container.removeAllViews()

        AppFont.entries.forEachIndexed { index, font ->
            val item = ItemSettingsFontBinding.inflate(inflater, container, false)
            item.tvFontName.text = getFontDisplayName(font).lowercase()
            item.tvFontSample.text = "focusphone"

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
                    activeDialog?.dismiss()
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

        activeDialog?.show()
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
        val shared = WallpaperPreferences.isUseSharedWallpaper(this)
        lifecycleScope.launch {
            val stored = WallpaperStorage.replace(this@AppearanceSettingsActivity, uri) { copy ->
                if (shared) {
                    WallpaperPreferences.setSharedWallpaperUri(this@AppearanceSettingsActivity, copy)
                } else {
                    when (target) {
                        WallpaperTarget.HOME ->
                            WallpaperPreferences.setHomeWallpaperUri(this@AppearanceSettingsActivity, copy)
                        WallpaperTarget.NOTIFICATIONS ->
                            WallpaperPreferences.setNotificationsWallpaperUri(this@AppearanceSettingsActivity, copy)
                        WallpaperTarget.SETTINGS ->
                            WallpaperPreferences.setSettingsWallpaperUri(this@AppearanceSettingsActivity, copy)
                    }
                }
            }
            if (!stored) {
                Toast.makeText(
                    this@AppearanceSettingsActivity,
                    R.string.settings_wallpaper_import_failed,
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            updateWallpaperValueLabels()
            updateWallpaperPreview()
            applyCustomThemeColors()
            applyWallpaper()
        }
    }

    private fun getWallpaperStatus(target: WallpaperTarget): String {
        val uri = WallpaperUtils.getWallpaperUri(this, target)
        return if (uri.isNullOrBlank()) {
            getString(R.string.settings_wallpaper_not_set)
        } else {
            getString(R.string.settings_wallpaper_set)
        }
    }

    private fun updateWallpaperValueLabels() {
        homeValueView?.text = getWallpaperStatus(WallpaperTarget.HOME)
        notificationsValueView?.text = getWallpaperStatus(WallpaperTarget.NOTIFICATIONS)
        settingsValueView?.text = getWallpaperStatus(WallpaperTarget.SETTINGS)
    }

    private fun LinearLayout.addWallpaperValueRow(
        titleResId: Int,
        subtitleResId: Int,
        initialValue: String?,
        iconResId: Int,
        onClick: () -> Unit
    ): Pair<TextView, View> {
        val valueView = addValueRow(
            titleResId = titleResId,
            subtitleResId = subtitleResId,
            initialValue = initialValue,
            iconResId = iconResId,
            onClick = onClick
        )
        val root = getChildAt(childCount - 1)
        return valueView to root
    }

    private fun updateWallpaperSectionVisibility() {
        val enabled = WallpaperPreferences.isWallpaperEnabled(this)
        wallpaperSharedSwitchRoot?.visibility = if (enabled) View.VISIBLE else View.GONE
        wallpaperPreviewRoot?.visibility = if (enabled) View.VISIBLE else View.GONE
        wallpaperHomeRowRoot?.visibility = if (enabled) View.VISIBLE else View.GONE
        wallpaperNotificationsRowRoot?.visibility = if (enabled) View.VISIBLE else View.GONE
        wallpaperSettingsRowRoot?.visibility = if (enabled) View.VISIBLE else View.GONE
        wallpaperBlurSwitchRoot?.visibility = if (enabled) View.VISIBLE else View.GONE
        wallpaperBlurSliderRoot?.visibility = if (enabled) View.VISIBLE else View.GONE
        wallpaperReadabilitySliderRoot?.visibility = if (enabled) View.VISIBLE else View.GONE
        wallpaperDisabledInfoRoot?.visibility = if (enabled) View.GONE else View.VISIBLE
        updateWallpaperPreview()
    }

    private fun updateWallpaperPreview() {
        val preview = previewBinding ?: return

        if (!WallpaperPreferences.isWallpaperEnabled(this)) {
            WallpaperUtils.clearWallpaperEffect(preview.ivWallpaperPreview)
            preview.tvWallpaperPreviewHint.visibility = View.GONE
            return
        }

        val uri = WallpaperUtils.getWallpaperUri(this, WallpaperTarget.HOME)
        val blurEnabled = WallpaperPreferences.isBlurEnabled(this)
        val blurRadius = WallpaperUtils.getBlurRadius(WallpaperPreferences.getBlurIntensity(this))

        if (uri.isNullOrBlank()) {
            WallpaperUtils.clearWallpaperEffect(preview.ivWallpaperPreview)
            preview.tvWallpaperPreviewHint.visibility = View.VISIBLE
            return
        }

        preview.tvWallpaperPreviewHint.visibility = View.GONE
        lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = WallpaperUtils.loadBitmap(this@AppearanceSettingsActivity, uri)
            withContext(Dispatchers.Main) {
                WallpaperUtils.applyWallpaperToImageView(
                    preview.ivWallpaperPreview,
                    bitmap,
                    blurRadius,
                    blurEnabled
                )
            }
        }
    }
}
