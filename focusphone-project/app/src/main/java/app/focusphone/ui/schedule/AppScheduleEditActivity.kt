package app.focusphone.ui.schedule

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import app.focusphone.R
import app.focusphone.data.preferences.AppSchedulePreferences
import app.focusphone.databinding.SettingsSliderRowBinding
import app.focusphone.domain.model.AppScheduleRule
import app.focusphone.domain.util.AppScheduleEvaluator
import app.focusphone.ui.settings.SettingsSubActivity
import app.focusphone.ui.settings.addSectionHeader
import app.focusphone.ui.settings.addSwitchRow
import app.focusphone.ui.settings.addValueRow
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

/**
 * Editor for a single app's [AppScheduleRule]: allowed time window, days of the week
 * and daily usage allowance.
 *
 * The screen is only reachable through [AppScheduleActivity], and re-checks
 * [ScheduleAccessGate] on every resume so a stale task in recents cannot be used to
 * edit rules without the password.
 */
class AppScheduleEditActivity : SettingsSubActivity() {

    private lateinit var packageNameArg: String
    private var appLabel: String = ""
    private var draft: AppScheduleRule = AppScheduleRule("")

    private var startValueView: TextView? = null
    private var endValueView: TextView? = null
    private var daysPanel: View? = null
    private var limitLabel: TextView? = null
    private var limitSlider: com.google.android.material.slider.Slider? = null

    override fun getScreenTitleRes(): Int = R.string.schedule_edit_title

    override fun onCreate(savedInstanceState: Bundle?) {
        packageNameArg = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        appLabel = intent.getStringExtra(EXTRA_LABEL).orEmpty()
        draft = AppSchedulePreferences.getRule(this, packageNameArg)
            ?: AppScheduleRule(
                packageName = packageNameArg,
                dailyLimitMinutes = 0
            )

        super.onCreate(savedInstanceState)

        if (packageNameArg.isBlank()) {
            finish()
            return
        }
        if (appLabel.isNotBlank()) {
            binding.tvTitle.text = appLabel
        }
    }

    override fun onResume() {
        super.onResume()
        // Defence in depth: the list screen guards the entry point, this guards the task.
        if (!ScheduleAccessGate.isUnlocked()) finish()
    }

    // ── Content ────────────────────────────────────────────────────

    override fun buildContent(container: LinearLayout) {
        buildWindowSection(container)
        container.addView(inflater.inflate(R.layout.settings_divider, container, false))
        buildDailyLimitSection(container)
        buildActions(container)
        updateEnabledState()
    }

    private fun buildWindowSection(container: LinearLayout) {
        container.addSectionHeader(R.string.schedule_section_window)

        container.addSwitchRow(
            titleResId = R.string.schedule_window_switch,
            subtitleResId = R.string.schedule_window_switch_desc,
            isChecked = draft.windowEnabled,
            iconResId = R.drawable.ic_settings_schedule,
            onCheckedChanged = { checked ->
                draft = draft.copy(windowEnabled = checked)
                updateEnabledState()
            }
        )

        startValueView = container.addValueRow(
            titleResId = R.string.schedule_start_time,
            subtitleResId = R.string.schedule_start_time_desc,
            initialValue = AppScheduleEvaluator.formatMinuteOfDay(draft.startMinute),
            iconResId = R.drawable.ic_settings_clock,
            onClick = { pickTime(isStart = true) }
        )

        endValueView = container.addValueRow(
            titleResId = R.string.schedule_end_time,
            subtitleResId = R.string.schedule_end_time_desc,
            initialValue = AppScheduleEvaluator.formatMinuteOfDay(draft.endMinute),
            iconResId = R.drawable.ic_settings_timer,
            onClick = { pickTime(isStart = false) }
        )

        val panel = inflater.inflate(R.layout.settings_schedule_days_panel, container, false)
        buildDayChips(panel.findViewById(R.id.days_container))
        container.addView(panel)
        daysPanel = panel
    }

    /** Builds the seven day toggles in the locale's week order. */
    private fun buildDayChips(chipContainer: LinearLayout) {
        chipContainer.removeAllViews()
        ScheduleDayLabels.orderedDays().forEach { day ->
            val chip = TextView(this).apply {
                text = ScheduleDayLabels.shortLabel(day)
                gravity = Gravity.CENTER
                textSize = 12f
                setBackgroundResource(R.drawable.bg_segmented_item)
                isActivated = draft.days.contains(day)
                setPadding(0, dp(10), 0, dp(10))
                setOnClickListener { toggleDay(day, this) }
            }
            applyChipColors(chip)
            chipContainer.addView(
                chip,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
            )
        }
    }

    /**
     * Toggles a weekday, keeping at least one day selected: a window with no days would
     * silently block the app forever, which is never what the user meant to configure.
     */
    private fun toggleDay(day: Int, chip: TextView) {
        val days = draft.days.toMutableSet()
        if (!days.remove(day)) {
            days.add(day)
        } else if (days.isEmpty()) {
            Toast.makeText(this, R.string.schedule_days_min_one, Toast.LENGTH_SHORT).show()
            return
        }
        draft = draft.copy(days = days)
        chip.isActivated = days.contains(day)
        applyChipColors(chip)
    }

    private fun applyChipColors(chip: TextView) {
        val typedValue = TypedValue()
        if (chip.isActivated) {
            theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        } else {
            theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
        }
        chip.setTextColor(typedValue.data)
    }

    private fun buildDailyLimitSection(container: LinearLayout) {
        container.addSectionHeader(R.string.schedule_section_limit)

        container.addSwitchRow(
            titleResId = R.string.schedule_limit_switch,
            subtitleResId = R.string.schedule_limit_switch_desc,
            isChecked = draft.hasDailyLimit,
            iconResId = R.drawable.ic_settings_timer,
            onCheckedChanged = { checked ->
                draft = draft.copy(
                    dailyLimitMinutes = if (checked) {
                        limitSliderValue().coerceAtLeast(LIMIT_MIN_MINUTES)
                    } else {
                        0
                    }
                )
                updateEnabledState()
            }
        )

        val sliderBinding = SettingsSliderRowBinding.inflate(inflater, container, false)
        sliderBinding.ivIcon.setImageResource(R.drawable.ic_settings_timer)
        sliderBinding.ivIcon.visibility = View.VISIBLE
        val initialLimit = draft.dailyLimitMinutes
            .takeIf { it > 0 }
            ?: AppScheduleRule.DEFAULT_DAILY_LIMIT_MINUTES
        sliderBinding.tvLabel.text = getString(R.string.schedule_limit_value, initialLimit)
        sliderBinding.slider.apply {
            valueFrom = LIMIT_MIN_MINUTES.toFloat()
            valueTo = LIMIT_MAX_MINUTES.toFloat()
            stepSize = LIMIT_STEP_MINUTES.toFloat()
            value = initialLimit.toFloat()
                .coerceIn(LIMIT_MIN_MINUTES.toFloat(), LIMIT_MAX_MINUTES.toFloat())
            addOnChangeListener { _, sliderValue, _ ->
                val minutes = sliderValue.toInt()
                sliderBinding.tvLabel.text = getString(R.string.schedule_limit_value, minutes)
                if (draft.hasDailyLimit) draft = draft.copy(dailyLimitMinutes = minutes)
            }
        }
        limitSlider = sliderBinding.slider
        limitLabel = sliderBinding.tvLabel
        container.addView(sliderBinding.root)
    }

    private fun limitSliderValue(): Int =
        limitSlider?.value?.toInt() ?: AppScheduleRule.DEFAULT_DAILY_LIMIT_MINUTES

    private fun buildActions(container: LinearLayout) {
        val actions = inflater.inflate(R.layout.settings_schedule_actions, container, false)

        actions.findViewById<View>(R.id.btn_save_rule).setOnClickListener { saveRule() }

        val removeButton = actions.findViewById<View>(R.id.btn_remove_rule)
        removeButton.visibility =
            if (AppSchedulePreferences.getRule(this, packageNameArg) != null) View.VISIBLE else View.GONE
        removeButton.setOnClickListener {
            AppSchedulePreferences.removeRule(this, packageNameArg)
            Toast.makeText(this, R.string.schedule_rule_removed, Toast.LENGTH_SHORT).show()
            finish()
        }

        container.addView(actions)
    }

    /** Greys out the controls that belong to a restriction the user has switched off. */
    private fun updateEnabledState() {
        val windowAlpha = if (draft.windowEnabled) 1f else DISABLED_ALPHA
        startValueView?.rowRoot()?.let { it.alpha = windowAlpha; it.isEnabled = draft.windowEnabled }
        endValueView?.rowRoot()?.let { it.alpha = windowAlpha; it.isEnabled = draft.windowEnabled }
        daysPanel?.let { it.alpha = windowAlpha; it.isEnabled = draft.windowEnabled }

        limitSlider?.isEnabled = draft.hasDailyLimit
        limitLabel?.alpha = if (draft.hasDailyLimit) 1f else DISABLED_ALPHA
    }

    /** The clickable row that owns a value TextView returned by `addValueRow`. */
    private fun TextView.rowRoot(): View? = parent as? View

    private fun pickTime(isStart: Boolean) {
        if (!draft.windowEnabled) return

        val current = if (isStart) draft.startMinute else draft.endMinute
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(if (DateFormat.is24HourFormat(this)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(current / 60)
            .setMinute(current % 60)
            .setTitleText(if (isStart) R.string.schedule_start_time else R.string.schedule_end_time)
            .build()

        picker.addOnPositiveButtonClickListener {
            val minutes = picker.hour * 60 + picker.minute
            if (isStart) {
                draft = draft.copy(startMinute = minutes)
                startValueView?.text = AppScheduleEvaluator.formatMinuteOfDay(minutes)
            } else {
                draft = draft.copy(endMinute = minutes)
                endValueView?.text = AppScheduleEvaluator.formatMinuteOfDay(minutes)
            }
        }
        picker.show(supportFragmentManager, TIME_PICKER_TAG)
    }

    private fun saveRule() {
        if (!draft.isActive) {
            Toast.makeText(this, R.string.schedule_nothing_to_save, Toast.LENGTH_SHORT).show()
            return
        }
        if (draft.windowEnabled && draft.days.isEmpty()) {
            Toast.makeText(this, R.string.schedule_days_min_one, Toast.LENGTH_SHORT).show()
            return
        }

        AppSchedulePreferences.saveRule(this, draft)
        Toast.makeText(this, R.string.schedule_rule_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {

        private const val EXTRA_PACKAGE = "schedule_edit_package"
        private const val EXTRA_LABEL = "schedule_edit_label"
        private const val TIME_PICKER_TAG = "schedule_time_picker"

        private const val LIMIT_MIN_MINUTES = 5
        private const val LIMIT_MAX_MINUTES = 240
        private const val LIMIT_STEP_MINUTES = 5
        private const val DISABLED_ALPHA = 0.4f

        /** Opens the editor for [packageName], displaying [label] as the screen title. */
        fun newIntent(context: Context, packageName: String, label: String): Intent =
            Intent(context, AppScheduleEditActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE, packageName)
                putExtra(EXTRA_LABEL, label)
            }
    }
}
