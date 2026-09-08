package app.focusphone.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import app.focusphone.R
import app.focusphone.data.preferences.ConsciousPausePreferences
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.databinding.SettingsSliderRowBinding
import app.focusphone.ui.schedule.AppScheduleActivity
import com.google.android.material.slider.Slider

/**
 * Focus screen: conscious pause (toggle + duration), app schedules and work-mode keywords.
 */
class FocusSettingsActivity : SettingsSubActivity() {

    private var durationSlider: Slider? = null

    override fun getScreenTitleRes(): Int = R.string.focus_settings_title

    override fun buildContent(container: LinearLayout) {
        // ── Conscious Pause ──────────────────────────────────────────
        container.addSectionHeader(R.string.settings_section_conscious_pause)

        val pauseEnabled = ConsciousPausePreferences.isEnabled(this)
        container.addSwitchRow(
            titleResId = R.string.settings_conscious_pause_title,
            subtitleResId = R.string.settings_conscious_pause_desc,
            isChecked = pauseEnabled,
            iconResId = R.drawable.ic_settings_pause,
            onCheckedChanged = { checked ->
                ConsciousPausePreferences.setEnabled(this, checked)
                durationSlider?.isEnabled = checked
            }
        )

        val currentDuration = ConsciousPausePreferences.getDuration(this)
        val durationBinding = SettingsSliderRowBinding.inflate(inflater, container, false)
        durationBinding.ivIcon.setImageResource(R.drawable.ic_settings_timer)
        durationBinding.ivIcon.visibility = View.VISIBLE
        durationBinding.tvLabel.text = getString(R.string.settings_conscious_pause_duration, currentDuration)
        durationBinding.slider.apply {
            valueFrom = 3f
            valueTo = 10f
            stepSize = 1f
            value = currentDuration.toFloat().coerceIn(3f, 10f)
            isEnabled = pauseEnabled
            addOnChangeListener { _, value, _ ->
                val duration = value.toInt()
                ConsciousPausePreferences.setDuration(this@FocusSettingsActivity, duration)
                durationBinding.tvLabel.text = getString(R.string.settings_conscious_pause_duration, duration)
            }
        }
        durationSlider = durationBinding.slider
        container.addView(durationBinding.root)

        container.addView(inflater.inflate(R.layout.settings_divider, container, false))

        // ── App Schedules ────────────────────────────────────────────
        container.addSectionHeader(R.string.schedule_section_apps)

        container.addCategoryRow(
            iconResId = R.drawable.ic_settings_schedule,
            titleResId = R.string.schedule_settings_row_title,
            subtitleResId = R.string.schedule_settings_row_desc,
            onClick = { startActivity(Intent(this, AppScheduleActivity::class.java)) }
        )

        container.addView(inflater.inflate(R.layout.settings_divider, container, false))

        // ── Work Keywords ────────────────────────────────────────────
        container.addSectionHeader(R.string.settings_section_work_profile)

        val keywordsPanel = inflater.inflate(R.layout.settings_work_keywords_panel, container, false)
        val etWorkKeywords = keywordsPanel.findViewById<android.widget.EditText>(R.id.et_work_keywords)
        val btnSave = keywordsPanel.findViewById<View>(R.id.btn_save_work_keywords)

        val currentKeywords = ProfilePreferences.getWorkKeywords(this)
        etWorkKeywords.setText(currentKeywords.joinToString("\n"))

        btnSave.setOnClickListener {
            val raw = etWorkKeywords.text.toString()
            val keywords = raw.split("\n").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
            ProfilePreferences.setWorkKeywords(this, keywords)
            Toast.makeText(this, R.string.allapps_toast_keywords_saved, Toast.LENGTH_SHORT).show()
        }

        container.addView(keywordsPanel)
    }
}
