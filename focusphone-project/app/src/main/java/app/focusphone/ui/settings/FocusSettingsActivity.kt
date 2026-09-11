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
    private var durationRow: View? = null

    override fun getScreenTitleRes(): Int = R.string.focus_settings_title

    override fun buildContent(container: LinearLayout) {
        // ── Conscious Pause ──────────────────────────────────────────
        container.settingsSection(R.string.settings_section_conscious_pause) {
            val pauseEnabled = ConsciousPausePreferences.isEnabled(this@FocusSettingsActivity)
            addSwitchRow(
                titleResId = R.string.settings_conscious_pause_title,
                subtitleResId = R.string.settings_conscious_pause_desc,
                isChecked = pauseEnabled,
                iconResId = R.drawable.ic_settings_pause,
                onCheckedChanged = { checked ->
                    ConsciousPausePreferences.setEnabled(this@FocusSettingsActivity, checked)
                    // Dim the whole row, not just the slider: an enabled-looking label above
                    // a dead control reads as a bug.
                    durationRow?.setRowEnabled(checked)
                    durationSlider?.isEnabled = checked
                }
            )

            val currentDuration = ConsciousPausePreferences.getDuration(this@FocusSettingsActivity)
            val durationBinding = SettingsSliderRowBinding.inflate(inflater, this, false)
            durationBinding.ivIcon.setImageResource(R.drawable.ic_settings_timer)
            durationBinding.ivIcon.visibility = View.VISIBLE
            durationBinding.tvLabel.text =
                getString(R.string.settings_conscious_pause_duration, currentDuration)
            durationBinding.slider.apply {
                valueFrom = 3f
                valueTo = 10f
                stepSize = 1f
                value = currentDuration.toFloat().coerceIn(3f, 10f)
                isEnabled = pauseEnabled
                addOnChangeListener { _, value, _ ->
                    val duration = value.toInt()
                    durationBinding.tvLabel.text =
                        getString(R.string.settings_conscious_pause_duration, duration)
                }
                persistOnStop { value ->
                    ConsciousPausePreferences.setDuration(this@FocusSettingsActivity, value.toInt())
                }
            }
            durationSlider = durationBinding.slider
            durationRow = durationBinding.root
            addView(durationBinding.root)
            durationBinding.root.setRowEnabled(pauseEnabled)
        }

        // ── App Schedules ────────────────────────────────────────────
        container.settingsSection(R.string.schedule_section_apps) {
            addCategoryRow(
                iconResId = R.drawable.ic_settings_schedule,
                titleResId = R.string.schedule_settings_row_title,
                subtitleResId = R.string.schedule_settings_row_desc,
                onClick = {
                    startActivity(Intent(this@FocusSettingsActivity, AppScheduleActivity::class.java))
                }
            )
        }

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
