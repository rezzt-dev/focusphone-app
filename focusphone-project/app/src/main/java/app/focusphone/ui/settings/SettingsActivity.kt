package app.focusphone.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.focusphone.R
import app.focusphone.databinding.ActivitySettingsBinding
import app.focusphone.ui.base.BaseActivity

/**
 * Main settings screen. Transformed from a monolithic scrollable form into a
 * clean category menu. Each category opens a dedicated sub-screen.
 */
class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private data class Category(
        @param:DrawableRes val iconResId: Int,
        @param:StringRes val titleResId: Int,
        @param:StringRes val subtitleResId: Int,
        val target: Class<out BaseActivity>
    )

    private val categories = listOf(
        Category(
            R.drawable.ic_palette,
            R.string.settings_category_appearance_title,
            R.string.settings_category_appearance_subtitle,
            AppearanceSettingsActivity::class.java
        ),
        Category(
            R.drawable.ic_home_screen,
            R.string.settings_category_home_title,
            R.string.settings_category_home_subtitle,
            HomeSettingsActivity::class.java
        ),
        Category(
            R.drawable.ic_focus,
            R.string.settings_category_focus_title,
            R.string.settings_category_focus_subtitle,
            FocusSettingsActivity::class.java
        ),
        Category(
            R.drawable.ic_notifications,
            R.string.settings_category_notifications_title,
            R.string.settings_category_notifications_subtitle,
            NotificationSettingsActivity::class.java
        ),
        Category(
            R.drawable.ic_system,
            R.string.settings_category_system_title,
            R.string.settings_category_system_subtitle,
            SystemSettingsActivity::class.java
        ),
        Category(
            R.drawable.ic_info,
            R.string.settings_category_about_title,
            R.string.settings_category_about_subtitle,
            AboutSettingsActivity::class.java
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()
        binding.btnBack.setOnClickListener { finish() }
        renderCategories()
        applyFontFamilyToView(binding.root)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.setPadding(0, bars.top, 0, 0)
            binding.scrollContent.setPadding(0, 0, 0, 0)
            binding.tvDevelopedBy.setPadding(0, 0, 0, bars.bottom)
            insets
        }
    }

    private fun renderCategories() {
        val container = binding.contentContainer
        categories.forEachIndexed { index, category ->
            if (index > 0) {
                container.addView(layoutInflater.inflate(R.layout.settings_divider, container, false))
            }
            container.addCategoryRow(
                iconResId = category.iconResId,
                titleResId = category.titleResId,
                subtitleResId = category.subtitleResId,
                onClick = { openSubScreen(category.target) }
            )
        }
    }

    private fun openSubScreen(target: Class<out BaseActivity>) {
        startActivity(Intent(this, target))
    }
}
