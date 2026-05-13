package app.focusphone.ui.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.focusphone.BuildConfig
import app.focusphone.R
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.data.preferences.ConsciousPausePreferences
import app.focusphone.databinding.ActivitySettingsBinding
import app.focusphone.databinding.ItemSettingsClockTypeBinding
import app.focusphone.domain.model.ClockType
import app.focusphone.ui.base.BaseActivity

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val inflater by lazy { LayoutInflater.from(this) }
    private var activeDialog: android.app.Dialog? = null
    private var bgTextWatcher: android.text.TextWatcher? = null
    private var fgTextWatcher: android.text.TextWatcher? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()
        binding.btnBack.setOnClickListener { finish() }
        setupThemeSelector()
        setupAlignmentSelector()
        setupClockTypeSelector()
        setupClockSizeSlider()
        setupTextSizeSlider()
        setupStatusBarToggle()
        setupConsciousPause()
        setupLanguageSelector()
        setupTutorial()
        binding.itemSetDefaultLauncher.setOnClickListener {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        }
        binding.tvVersion.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
    }

    // ── Alignment ──────────────────────────────────────────────────

    private fun setupAlignmentSelector() {
        val current = ThemePreferences.getAlignment(this)
        updateAlignmentUI(current)

        binding.btnAlignLeft.setOnClickListener {
            ThemePreferences.setAlignment(this, ThemePreferences.ALIGN_LEFT)
            updateAlignmentUI(ThemePreferences.ALIGN_LEFT)
        }
        binding.btnAlignCenter.setOnClickListener {
            ThemePreferences.setAlignment(this, ThemePreferences.ALIGN_CENTER)
            updateAlignmentUI(ThemePreferences.ALIGN_CENTER)
        }
        binding.btnAlignRight.setOnClickListener {
            ThemePreferences.setAlignment(this, ThemePreferences.ALIGN_RIGHT)
            updateAlignmentUI(ThemePreferences.ALIGN_RIGHT)
        }
    }

    private fun updateAlignmentUI(alignment: String) {
        binding.btnAlignLeft.isActivated = (alignment == ThemePreferences.ALIGN_LEFT)
        binding.btnAlignCenter.isActivated = (alignment == ThemePreferences.ALIGN_CENTER)
        binding.btnAlignRight.isActivated = (alignment == ThemePreferences.ALIGN_RIGHT)
    }

    // ── Insets ─────────────────────────────────────────────────────

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.setPadding(0, bars.top, 0, 0)
            binding.scrollContent.setPadding(0, 0, 0, bars.bottom)
            insets
        }
    }

    // ── Theme ──────────────────────────────────────────────────────

    private fun setupThemeSelector() {
        val currentTheme = ThemePreferences.getTheme(this)
        updateThemeRowUI(currentTheme)
        binding.btnToggleTheme.setOnClickListener {
            showThemeSelectionDialog()
        }

        setupCustomThemePanel()
    }

    private fun applyTheme(theme: String) {
        if (ThemePreferences.getTheme(this) == theme) return
        ThemePreferences.setTheme(this, theme)
        
        // Smooth crossfade transition without white flash
        finish()
        startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun updateThemeRowUI(theme: String) {
        val themeName = when (theme) {
            ThemePreferences.THEME_OFF_WHITE -> getString(R.string.settings_theme_off_white)
            ThemePreferences.THEME_SLATE -> getString(R.string.settings_theme_slate)
            ThemePreferences.THEME_WARM_BEIGE -> getString(R.string.settings_theme_warm_beige)
            ThemePreferences.THEME_NAVY -> getString(R.string.settings_theme_navy)
            ThemePreferences.THEME_CUSTOM -> "custom"
            else -> getString(R.string.settings_theme_slate)
        }
        binding.tvActiveThemeName.text = themeName.lowercase()

        val drawableRes = when (theme) {
            ThemePreferences.THEME_OFF_WHITE -> R.drawable.bg_theme_card_off_white
            ThemePreferences.THEME_SLATE -> R.drawable.bg_theme_card_slate
            ThemePreferences.THEME_WARM_BEIGE -> R.drawable.bg_theme_card_warm_beige
            ThemePreferences.THEME_NAVY -> R.drawable.bg_theme_card_navy
            ThemePreferences.THEME_CUSTOM -> R.drawable.bg_theme_card_slate
            else -> R.drawable.bg_theme_card_slate
        }
        if (theme == ThemePreferences.THEME_CUSTOM) {
            val customColor = ThemePreferences.getCustomBackgroundColor(this)
            val customDrawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(customColor)
                setStroke((1.5f * resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor("#3A3A3A"))
            }
            binding.viewActiveThemeCirclePreview.background = customDrawable
            binding.viewActiveThemeCirclePreview.backgroundTintList = null
        } else {
            binding.viewActiveThemeCirclePreview.setBackgroundResource(drawableRes)
            binding.viewActiveThemeCirclePreview.backgroundTintList = null
        }
    }

    private fun showThemeSelectionDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_theme_select, null)
        activeDialog = android.app.AlertDialog.Builder(this)
            .setView(view)
            .create()

        activeDialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        // Programmatically generate Custom circle with border
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
        view.findViewById<View>(R.id.btn_theme_custom).setOnClickListener {
            applyTheme(ThemePreferences.THEME_CUSTOM)
            activeDialog?.dismiss()
        }

        activeDialog?.show()
    }

    private fun setupCustomThemePanel() {
        // Populate current saved values
        val currentBgHex = ThemePreferences.getCustomBackgroundColorHex(this)
        val currentFgHex = ThemePreferences.getCustomForegroundColorHex(this)

        binding.etCustomBgColor.setText(currentBgHex)
        binding.etCustomFgColor.setText(currentFgHex)

        // Set initial circular previews
        try {
            val bgCol = android.graphics.Color.parseColor(currentBgHex)
            binding.viewCustomBgPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(bgCol)
        } catch (e: Exception) {}

        try {
            val fgCol = android.graphics.Color.parseColor(currentFgHex)
            binding.viewCustomFgPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(fgCol)
        } catch (e: Exception) {}

        // Listen for background hex changes to update circle live and enforce '#'
        bgTextWatcher = object : android.text.TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isUpdating) return
                val raw = s?.toString() ?: ""
                if (!raw.startsWith("#")) {
                    isUpdating = true
                    val fixed = "#" + raw.replace("#", "")
                    binding.etCustomBgColor.setText(fixed)
                    try { binding.etCustomBgColor.setSelection(fixed.length) } catch (e: Exception) {}
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
                    } catch (e: Exception) {}
                }
            }
        }
        binding.etCustomBgColor.addTextChangedListener(bgTextWatcher!!)

        // Listen for foreground hex changes to update circle live and enforce '#'
        fgTextWatcher = object : android.text.TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isUpdating) return
                val raw = s?.toString() ?: ""
                if (!raw.startsWith("#")) {
                    isUpdating = true
                    val fixed = "#" + raw.replace("#", "")
                    binding.etCustomFgColor.setText(fixed)
                    try { binding.etCustomFgColor.setSelection(fixed.length) } catch (e: Exception) {}
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
                    } catch (e: Exception) {}
                }
            }
        }
        binding.etCustomFgColor.addTextChangedListener(fgTextWatcher!!)

        // Expand/collapse logic
        binding.btnToggleCustomTheme.setOnClickListener {
            val isVisible = binding.layoutCustomThemePanel.visibility == View.VISIBLE
            if (isVisible) {
                binding.layoutCustomThemePanel.visibility = View.GONE
                binding.ivCustomThemeToggleIndicator.setImageResource(R.drawable.ic_chevron_down)
            } else {
                binding.layoutCustomThemePanel.visibility = View.VISIBLE
                binding.ivCustomThemeToggleIndicator.setImageResource(R.drawable.ic_chevron_up)
            }
        }

        // Apply action
        binding.btnApplyCustomTheme.setOnClickListener {
            val bgHex = binding.etCustomBgColor.text.toString().trim()
            val fgHex = binding.etCustomFgColor.text.toString().trim()

            if (bgHex.length == 7 && bgHex.startsWith("#") && fgHex.length == 7 && fgHex.startsWith("#")) {
                ThemePreferences.setCustomBackgroundColorHex(this, bgHex)
                ThemePreferences.setCustomForegroundColorHex(this, fgHex)
                applyTheme(ThemePreferences.THEME_CUSTOM)
            }
        }
    }

    // ── Clock type ─────────────────────────────────────────────────

    private fun setupClockTypeSelector() {
        val current = FontPreferences.getClockType(this)
        binding.clockTypeContainer.removeAllViews()
        val tiles = mutableListOf<ItemSettingsClockTypeBinding>()

        ClockType.entries.forEach { type ->
            val item = ItemSettingsClockTypeBinding.inflate(inflater, binding.clockTypeContainer, false)
            item.tvPreview.text = type.previewLabel
            item.tvLabel.text   = type.displayName.lowercase()
            item.root.alpha     = if (type == current) 1f else 0.6f
            item.root.setOnClickListener {
                FontPreferences.setClockType(this, type)
                tiles.forEach { it.root.alpha = 0.6f }
                item.root.alpha = 1f
            }
            binding.clockTypeContainer.addView(item.root)
            tiles.add(item)
        }
    }

    private fun setupClockSizeSlider() {
        val current = FontPreferences.getClockSizePercent(this)
        binding.sliderClockSize.value = current.toFloat()
        binding.sliderClockSize.addOnChangeListener { _, value, _ ->
            FontPreferences.setClockSizePercent(this, value.toInt())
        }
    }

    private fun setupTextSizeSlider() {
        val current = FontPreferences.getFontSizePt(this)
        binding.sliderTextSize.value = current.toFloat()
        updateTextSizeLabel(current)
        binding.sliderTextSize.addOnChangeListener { _, value, _ ->
            val size = value.toInt()
            FontPreferences.setFontSizePt(this, size)
            updateTextSizeLabel(size)
        }
    }

    private fun updateTextSizeLabel(size: Int) {
        binding.tvTextSizeLabel.text = getString(R.string.settings_text_size_pt, size)
    }

    private fun setupStatusBarToggle() {
        binding.switchHideStatusBar.isChecked = ThemePreferences.isStatusBarHidden(this)
        binding.switchHideStatusBar.setOnCheckedChangeListener { _, isChecked ->
            ThemePreferences.setStatusBarHidden(this, isChecked)
            applyStatusBarVisibility()
        }
    }

    // ── Language ───────────────────────────────────────────────────

    /** Language options: tag → display string resource */
    private data class LangOption(val tag: String, val labelResId: Int)

    private val languages = listOf(
        LangOption("",      R.string.settings_language_system),
        LangOption("es",    R.string.settings_language_es),
        LangOption("en-US", R.string.settings_language_en_us),
        LangOption("en-GB", R.string.settings_language_en_gb),
        LangOption("fr",    R.string.settings_language_fr),
        LangOption("de",    R.string.settings_language_de),
    )

    private fun setupLanguageSelector() {
        val currentTag = AppCompatDelegate.getApplicationLocales()
            .toLanguageTags().ifEmpty { "" }
        binding.languageContainer.removeAllViews()

        // Set active language text in the dropdown header in lowercase
        val activeOption = languages.firstOrNull {
            it.tag.equals(currentTag, ignoreCase = true) || (it.tag.isEmpty() && currentTag.isEmpty())
        } ?: languages.first()
        binding.tvActiveLanguageLabel.text = getString(activeOption.labelResId).lowercase()

        // Setup drop-down toggle interaction
        binding.btnToggleLanguage.setOnClickListener {
            val isVisible = binding.languageContainer.visibility == View.VISIBLE
            if (isVisible) {
                binding.languageContainer.visibility = View.GONE
                binding.ivLanguageToggleIndicator.setImageResource(R.drawable.ic_chevron_down)
            } else {
                binding.languageContainer.visibility = View.VISIBLE
                binding.ivLanguageToggleIndicator.setImageResource(R.drawable.ic_chevron_up)
            }
        }

        languages.forEach { lang ->
            val isActive = lang.tag.equals(currentTag, ignoreCase = true) ||
                (lang.tag.isEmpty() && currentTag.isEmpty())

            val row = TextView(this).apply {
                text = getString(lang.labelResId).lowercase()
                textSize = 15f
                setTextColor(
                    if (isActive) getColor(android.R.color.white)
                    else resources.getColor(android.R.color.darker_gray, theme)
                )
                typeface = if (isActive)
                    android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                else
                    android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                setPadding(0, dp(14), 0, dp(14))
                alpha = if (isActive) 1f else 0.6f
                setOnClickListener {
                    if (lang.tag.isEmpty()) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                    } else {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang.tag))
                    }
                }
            }
            binding.languageContainer.addView(row)
        }
    }

    private fun setupConsciousPause() {
        val isEnabled = ConsciousPausePreferences.isEnabled(this)
        binding.switchConsciousPause.isChecked = isEnabled
        
        val currentDuration = ConsciousPausePreferences.getDuration(this)
        binding.sliderConsciousPauseDuration.value = currentDuration.toFloat()
        updateDurationLabel(currentDuration)
        
        binding.sliderConsciousPauseDuration.isEnabled = isEnabled
        
        binding.switchConsciousPause.setOnCheckedChangeListener { _, isChecked ->
            ConsciousPausePreferences.setEnabled(this, isChecked)
            binding.sliderConsciousPauseDuration.isEnabled = isChecked
        }
        
        binding.sliderConsciousPauseDuration.addOnChangeListener { _, value, _ ->
            val duration = value.toInt()
            ConsciousPausePreferences.setDuration(this, duration)
            updateDurationLabel(duration)
        }
    }

    private fun updateDurationLabel(duration: Int) {
        binding.tvConsciousPauseDurationLabel.text = getString(R.string.settings_conscious_pause_duration, duration)
    }

    private fun setupTutorial() {
        binding.itemTutorial.setOnClickListener {
            showTutorialDialog()
        }
    }

    private fun showTutorialDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tutorial_carousel, null)
        activeDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        activeDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvCounter = dialogView.findViewById<TextView>(R.id.tv_slide_counter)
        val ivIcon = dialogView.findViewById<android.widget.ImageView>(R.id.iv_tutorial_icon)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_tutorial_title)
        val tvDesc = dialogView.findViewById<TextView>(R.id.tv_tutorial_desc)
        val layoutDots = dialogView.findViewById<LinearLayout>(R.id.layout_dots)
        val btnPrev = dialogView.findViewById<TextView>(R.id.btn_prev)
        val btnNext = dialogView.findViewById<TextView>(R.id.btn_next)

        val slides = listOf(
            Triple(R.drawable.ic_tutorial_home, R.string.tutorial_slide_1_title, R.string.tutorial_slide_1_desc),
            Triple(R.drawable.ic_add, R.string.tutorial_slide_2_title, R.string.tutorial_slide_2_desc),
            Triple(R.drawable.ic_tutorial_profiles, R.string.tutorial_slide_3_title, R.string.tutorial_slide_3_desc),
            Triple(R.drawable.ic_tutorial_block, R.string.tutorial_slide_4_title, R.string.tutorial_slide_4_desc),
            Triple(R.drawable.ic_tutorial_edit, R.string.tutorial_slide_5_title, R.string.tutorial_slide_5_desc),
            Triple(R.drawable.ic_tutorial_detox, R.string.tutorial_slide_6_title, R.string.tutorial_slide_6_desc),
            Triple(R.drawable.ic_tutorial_pause, R.string.tutorial_slide_7_title, R.string.tutorial_slide_7_desc),
            Triple(R.drawable.ic_tutorial_clock, R.string.tutorial_slide_8_title, R.string.tutorial_slide_8_desc)
        )

        var currentIndex = 0

        fun resolveColorAttr(attrId: Int): Int {
            val typedValue = android.util.TypedValue()
            theme.resolveAttribute(attrId, typedValue, true)
            return typedValue.data
        }

        val primaryColor = resolveColorAttr(android.R.attr.textColorPrimary)
        val secondaryColor = resolveColorAttr(android.R.attr.textColorSecondary)

        fun updateSlide() {
            val (iconRes, titleRes, descRes) = slides[currentIndex]
            
            tvCounter.text = "${currentIndex + 1} / ${slides.size}"
            
            ivIcon.setImageResource(iconRes)
            ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            
            tvTitle.setText(titleRes)
            tvDesc.setText(descRes)

            if (currentIndex == 0) {
                btnPrev.visibility = View.INVISIBLE
            } else {
                btnPrev.visibility = View.VISIBLE
                btnPrev.text = "←"
            }

            if (currentIndex == slides.size - 1) {
                btnNext.text = "✓"
            } else {
                btnNext.text = "→"
            }

            layoutDots.removeAllViews()
            for (i in slides.indices) {
                val dot = View(this)
                val size = (8 * resources.displayMetrics.density).toInt()
                val params = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(
                        (4 * resources.displayMetrics.density).toInt(),
                        0,
                        (4 * resources.displayMetrics.density).toInt(),
                        0
                    )
                }
                dot.layoutParams = params
                
                if (i == currentIndex) {
                    dot.setBackgroundResource(R.drawable.bg_dot_active)
                    dot.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                } else {
                    dot.setBackgroundResource(R.drawable.bg_dot_inactive)
                    dot.backgroundTintList = android.content.res.ColorStateList.valueOf(secondaryColor).withAlpha(76)
                }
                layoutDots.addView(dot)
            }
        }

        btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                updateSlide()
            }
        }

        btnNext.setOnClickListener {
            if (currentIndex < slides.size - 1) {
                currentIndex++
                updateSlide()
            } else {
                activeDialog?.dismiss()
            }
        }

        updateSlide()
        activeDialog?.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        activeDialog?.dismiss()
        bgTextWatcher?.let { binding.etCustomBgColor.removeTextChangedListener(it) }
        fgTextWatcher?.let { binding.etCustomFgColor.removeTextChangedListener(it) }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
