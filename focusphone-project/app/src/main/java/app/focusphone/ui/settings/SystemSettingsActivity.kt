package app.focusphone.ui.settings

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.focusphone.R
import app.focusphone.service.FocusAccessibilityService

/**
 * System screen: permissions, accessibility service, default launcher and language.
 */
class SystemSettingsActivity : SettingsSubActivity() {

    private var activeDialog: Dialog? = null
    private var accessibilityValueView: TextView? = null

    private data class LangOption(val tag: String, val labelResId: Int)

    private val languages = listOf(
        LangOption("", R.string.settings_language_system),
        LangOption("es", R.string.settings_language_es),
        LangOption("en-US", R.string.settings_language_en_us),
        LangOption("en-GB", R.string.settings_language_en_gb),
        LangOption("fr", R.string.settings_language_fr),
        LangOption("de", R.string.settings_language_de),
    )

    override fun getScreenTitleRes(): Int = R.string.system_settings_title

    override fun buildContent(container: LinearLayout) {
        container.addSectionHeader(R.string.settings_section_system)

        // Permissions
        container.addCategoryRow(
            iconResId = R.drawable.ic_settings_permissions,
            titleResId = R.string.settings_permissions_title,
            subtitleResId = R.string.settings_permissions_desc_short,
            onClick = { startActivity(Intent(this, PermissionsActivity::class.java)) }
        )

        // Accessibility service
        accessibilityValueView = container.addValueRow(
            titleResId = R.string.settings_accessibility_title,
            subtitleResId = R.string.settings_accessibility_section_desc,
            initialValue = getAccessibilityStatusText(),
            iconResId = R.drawable.ic_settings_accessibility,
            onClick = { FocusAccessibilityService.openAccessibilitySettings(this) }
        )

        // Set as default launcher
        container.addValueRow(
            titleResId = R.string.settings_set_default,
            subtitleResId = R.string.settings_set_default_desc_short,
            initialValue = null,
            iconResId = R.drawable.ic_settings_launcher,
            onClick = { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        )

        container.addView(inflater.inflate(R.layout.settings_divider, container, false))

        // Language
        container.addSectionHeader(R.string.settings_section_language)
        container.addValueRow(
            titleResId = R.string.settings_section_language,
            subtitleResId = R.string.settings_language_desc,
            initialValue = getCurrentLanguageLabel(),
            iconResId = R.drawable.ic_settings_language,
            onClick = ::showLanguageDialog
        )
    }

    override fun onResume() {
        super.onResume()
        accessibilityValueView?.text = getAccessibilityStatusText()
    }

    override fun onDestroy() {
        activeDialog?.dismiss()
        super.onDestroy()
    }

    private fun getAccessibilityStatusText(): String =
        getString(
            if (FocusAccessibilityService.isEnabled(this)) R.string.settings_accessibility_active
            else R.string.settings_accessibility_inactive
        ).lowercase()

    private fun getCurrentLanguageLabel(): String {
        val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "" }
        val option = languages.firstOrNull {
            it.tag.equals(currentTag, ignoreCase = true) || (it.tag.isEmpty() && currentTag.isEmpty())
        } ?: languages.first()
        return getString(option.labelResId).lowercase()
    }

    private fun showLanguageDialog() {
        val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "" }
        val labels = languages.map { getString(it.labelResId).lowercase() }.toTypedArray()
        val checkedItem = languages.indexOfFirst {
            it.tag.equals(currentTag, ignoreCase = true) || (it.tag.isEmpty() && currentTag.isEmpty())
        }.coerceAtLeast(0)

        activeDialog = AlertDialog.Builder(this)
            .setTitle(R.string.settings_section_language)
            .setSingleChoiceItems(labels, checkedItem) { dialog, which ->
                val selected = languages[which]
                if (selected.tag != currentTag) {
                    AppCompatDelegate.setApplicationLocales(
                        if (selected.tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                        else LocaleListCompat.forLanguageTags(selected.tag)
                    )
                    recreate()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
        activeDialog?.show()
    }
}
