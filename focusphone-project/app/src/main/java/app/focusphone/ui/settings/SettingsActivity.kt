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
        setupStatusBarToggle()
        setupConsciousPause()
        setupLanguageSelector()
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
        highlightTheme(ThemePreferences.getTheme(this))
        binding.cardThemeOffWhite.setOnClickListener  { applyTheme(ThemePreferences.THEME_OFF_WHITE)  }
        binding.cardThemeSlate.setOnClickListener     { applyTheme(ThemePreferences.THEME_SLATE)      }
        binding.cardThemeWarmBeige.setOnClickListener { applyTheme(ThemePreferences.THEME_WARM_BEIGE) }
        binding.cardThemeNavy.setOnClickListener      { applyTheme(ThemePreferences.THEME_NAVY)       }
        binding.cardThemeCustom.setOnClickListener    { applyTheme(ThemePreferences.THEME_CUSTOM)     }

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

    private fun highlightTheme(theme: String) {
        val all = listOf(
            binding.cardThemeOffWhite,
            binding.cardThemeSlate,
            binding.cardThemeWarmBeige,
            binding.cardThemeNavy,
            binding.cardThemeCustom
        )
        all.forEach {
            it.alpha = 0.55f
            it.foreground = null
        }

        // Dynamically color the 5th circle with the active custom background color
        val customColor = ThemePreferences.getCustomBackgroundColor(this)
        binding.viewThemeCustomCircle.backgroundTintList = android.content.res.ColorStateList.valueOf(customColor)

        val card = when (theme) {
            ThemePreferences.THEME_OFF_WHITE  -> binding.cardThemeOffWhite
            ThemePreferences.THEME_SLATE      -> binding.cardThemeSlate
            ThemePreferences.THEME_WARM_BEIGE -> binding.cardThemeWarmBeige
            ThemePreferences.THEME_NAVY       -> binding.cardThemeNavy
            ThemePreferences.THEME_CUSTOM     -> binding.cardThemeCustom
            else                              -> binding.cardThemeSlate
        }
        card.alpha = 1f
        card.foreground = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_theme_card_selected)
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
        binding.etCustomBgColor.addTextChangedListener(object : android.text.TextWatcher {
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
        })

        // Listen for foreground hex changes to update circle live and enforce '#'
        binding.etCustomFgColor.addTextChangedListener(object : android.text.TextWatcher {
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
        })

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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
