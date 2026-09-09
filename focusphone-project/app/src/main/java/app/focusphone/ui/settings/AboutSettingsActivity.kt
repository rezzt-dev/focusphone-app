package app.focusphone.ui.settings

import android.app.Dialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import app.focusphone.BuildConfig
import app.focusphone.R
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.data.preferences.TutorialPreferences
import app.focusphone.domain.model.ClockType
import app.focusphone.ui.base.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * About screen: version info, privacy policy, interactive tutorial,
 * onboarding restart and open-source licences.
 */
class AboutSettingsActivity : SettingsSubActivity() {

    private var activeDialog: Dialog? = null

    override fun getScreenTitleRes(): Int = R.string.about_settings_title

    override fun buildContent(container: LinearLayout) {
        container.settingsSection(R.string.settings_section_about) {
            // Version info row (no chevron; a long press opens the privacy policy)
            val versionLabel =
                getString(R.string.settings_version, BuildConfig.VERSION_NAME).lowercase()
            addInfoRow(
                titleResId = R.string.settings_version,
                subtitleResId = R.string.settings_privacy_policy,
                iconResId = R.drawable.ic_settings_version,
                onClick = ::showPrivacyPolicyDialog
            ).apply {
                findViewById<TextView>(R.id.tv_title).text = versionLabel
                setOnLongClickListener {
                    showPrivacyPolicyDialog()
                    true
                }
            }

            addCategoryRow(
                iconResId = R.drawable.ic_settings_tutorial,
                titleResId = R.string.settings_tutorial_title,
                subtitleResId = R.string.settings_tutorial_desc,
                onClick = ::showTutorialDialog
            )

            addCategoryRow(
                iconResId = R.drawable.ic_settings_tutorial,
                titleResId = R.string.settings_tutorial_restart_title,
                subtitleResId = R.string.settings_tutorial_restart_desc,
                onClick = ::confirmRestartTutorial
            )

            addCategoryRow(
                iconResId = R.drawable.ic_settings_onboarding,
                titleResId = R.string.settings_onboarding_title,
                subtitleResId = R.string.settings_onboarding_desc,
                onClick = ::confirmRestartOnboarding
            )

            addCategoryRow(
                iconResId = R.drawable.ic_settings_license,
                titleResId = R.string.settings_open_source,
                subtitleResId = R.string.settings_open_source_desc,
                onClick = ::showOpenSourceLicencesDialog
            )
        }
    }

    private fun confirmRestartTutorial() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_tutorial_restart_title)
            .setMessage(R.string.settings_restart_tutorial_confirm)
            .setPositiveButton(R.string.action_ok) { _, _ ->
                TutorialPreferences.resetTutorial(this)
                finish()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
            .let { styleMaterialDialog(it) }
    }

    private fun confirmRestartOnboarding() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_onboarding_title)
            .setMessage(R.string.settings_restart_onboarding_confirm)
            .setPositiveButton(R.string.action_ok) { _, _ ->
                ThemePreferences.clearOnboardingComplete(this)
                TutorialPreferences.clearAll(this)
                startActivity(Intent(this, app.focusphone.ui.onboarding.OnboardingActivity::class.java))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
            .let { styleMaterialDialog(it) }
    }

    private fun showOpenSourceLicencesDialog() {
        val htmlText = try {
            assets.open("open_source_licenses.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w("AboutSettings", "Failed to load open_source_licenses.html from assets", e)
            null
        }

        val scrollView = android.widget.ScrollView(this).apply {
            setPadding(dp(24), dp(16), dp(24), dp(16))
        }
        val font = FontPreferences.getTypefaceOrNull(this)
        val textView = TextView(this).apply {
            typeface = font
            text = htmlText ?: getString(R.string.settings_privacy_policy_fallback)
            textSize = 14f
            setLineSpacing(1.2f, 1.2f)
        }
        scrollView.addView(textView)

        activeDialog = AlertDialog.Builder(this)
            .setTitle(R.string.settings_open_source)
            .setView(scrollView)
            .setPositiveButton(R.string.action_ok, null)
            .create()
        activeDialog?.show()
    }

    private fun showPrivacyPolicyDialog() {
        val htmlText = try {
            assets.open("privacy_policy.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w("AboutSettings", "Failed to load privacy_policy.html from assets", e)
            null
        }

        val scrollView = android.widget.ScrollView(this).apply {
            setPadding(dp(24), dp(16), dp(24), dp(16))
        }
        val font = FontPreferences.getTypefaceOrNull(this)
        val textView = TextView(this).apply {
            typeface = font
            text = if (htmlText != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(htmlText, android.text.Html.FROM_HTML_MODE_LEGACY)
            } else if (htmlText != null) {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(htmlText)
            } else {
                getString(R.string.settings_privacy_policy_fallback)
            }
            textSize = 14f
            setLineSpacing(1.2f, 1.2f)
        }
        scrollView.addView(textView)

        activeDialog = AlertDialog.Builder(this)
            .setTitle(R.string.settings_privacy_policy)
            .setView(scrollView)
            .setPositiveButton(R.string.action_ok, null)
            .create()
        activeDialog?.show()
    }

    private fun showTutorialDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tutorial_carousel, null)
        applyDialogSurface(dialogView)
        applyFontFamilyToView(dialogView)
        activeDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        activeDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvCounter = dialogView.findViewById<TextView>(R.id.tv_slide_counter)
        val ivIcon = dialogView.findViewById<ImageView>(R.id.iv_tutorial_icon)
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

        val primaryColor = palette.textPrimary
        val secondaryColor = palette.textSecondary

        fun updateSlide() {
            val (iconRes, titleRes, descRes) = slides[currentIndex]

            tvCounter.text = "${currentIndex + 1} / ${slides.size}"
            ivIcon.setImageResource(iconRes)
            ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            tvTitle.setText(titleRes)
            tvDesc.setText(descRes)

            btnPrev.visibility = if (currentIndex == 0) View.INVISIBLE else View.VISIBLE
            btnPrev.text = getString(R.string.tutorial_arrow_prev)
            btnNext.text = if (currentIndex == slides.size - 1) {
                getString(R.string.tutorial_arrow_finish)
            } else {
                getString(R.string.tutorial_arrow_next)
            }

            layoutDots.removeAllViews()
            val size = (8 * resources.displayMetrics.density).toInt()
            for (i in slides.indices) {
                val dot = View(this)
                dot.layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(
                        (4 * resources.displayMetrics.density).toInt(), 0,
                        (4 * resources.displayMetrics.density).toInt(), 0
                    )
                }
                if (i == currentIndex) {
                    dot.setBackgroundResource(R.drawable.bg_dot_active)
                    dot.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                } else {
                    dot.setBackgroundResource(R.drawable.bg_dot_inactive)
                    dot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        (secondaryColor and 0x00FFFFFF) or (76 shl 24)
                    )
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
        activeDialog?.dismiss()
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
