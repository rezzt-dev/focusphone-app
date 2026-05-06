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
        val all = listOf(binding.cardThemeOffWhite, binding.cardThemeSlate,
               binding.cardThemeWarmBeige, binding.cardThemeNavy)
        all.forEach {
            it.alpha = 0.55f
            it.foreground = null
        }
        val card = when (theme) {
            ThemePreferences.THEME_OFF_WHITE  -> binding.cardThemeOffWhite
            ThemePreferences.THEME_SLATE      -> binding.cardThemeSlate
            ThemePreferences.THEME_WARM_BEIGE -> binding.cardThemeWarmBeige
            ThemePreferences.THEME_NAVY       -> binding.cardThemeNavy
            else                              -> binding.cardThemeSlate
        }
        card.alpha = 1f
        card.foreground = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_theme_card_selected)
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
            item.root.alpha     = if (type == current) 1f else 0.38f
            item.root.setOnClickListener {
                FontPreferences.setClockType(this, type)
                tiles.forEach { it.root.alpha = 0.38f }
                item.root.alpha = 1f
            }
            binding.clockTypeContainer.addView(item.root)
            tiles.add(item)
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
        val rows = mutableListOf<View>()

        languages.forEach { lang ->
            val isActive = lang.tag.equals(currentTag, ignoreCase = true) ||
                (lang.tag.isEmpty() && currentTag.isEmpty())

            val row = TextView(this).apply {
                text = getString(lang.labelResId)
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
            rows.add(row)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
