package app.focusphone.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import app.focusphone.R
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.preferences.HomeLayoutPreferences
import app.focusphone.data.preferences.WidgetPreferences
import app.focusphone.databinding.ItemSettingsClockTypeBinding
import app.focusphone.databinding.SettingsSliderRowBinding
import app.focusphone.domain.model.ClockType
import app.focusphone.ui.widget.WidgetPickerActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider

/**
 * Home screen settings: clock type, clock size, app spacing, widget and layout.
 */
class HomeSettingsActivity : SettingsSubActivity() {

    private var widgetValueView: TextView? = null
    private var clockSizeSlider: Slider? = null
    private var appSpacingSlider: Slider? = null

    private val widgetPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            updateWidgetStatus()
        }
    }

    override fun getScreenTitleRes(): Int = R.string.home_settings_title

    override fun buildContent(container: LinearLayout) {
        // ── Clock ────────────────────────────────────────────────────
        container.addSectionHeader(R.string.settings_section_clock)

        val clockTypeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(20), dp(4), dp(20), dp(4))
        }
        setupClockTypeSelector(clockTypeContainer)
        container.addView(clockTypeContainer)

        val currentClockSize = FontPreferences.getClockSizePercent(this)
        val clockSizeBinding = SettingsSliderRowBinding.inflate(inflater, container, false)
        clockSizeBinding.ivIcon.setImageResource(R.drawable.ic_settings_clock)
        clockSizeBinding.ivIcon.visibility = View.VISIBLE
        clockSizeBinding.tvLabel.setText(R.string.settings_clock_size)
        clockSizeBinding.slider.apply {
            valueFrom = 0f
            valueTo = 100f
            stepSize = 1f
            value = currentClockSize.toFloat().coerceIn(0f, 100f)
            addOnChangeListener { _, value, _ ->
                FontPreferences.setClockSizePercent(this@HomeSettingsActivity, value.toInt())
            }
        }
        clockSizeSlider = clockSizeBinding.slider
        container.addView(clockSizeBinding.root)

        container.addView(inflater.inflate(R.layout.settings_divider, container, false))

        // ── Layout & Spacing ─────────────────────────────────────────
        container.addSectionHeader(R.string.settings_section_launcher)

        val currentSpacing = HomeLayoutPreferences.getAppSpacingDp(this)
        val spacingBinding = SettingsSliderRowBinding.inflate(inflater, container, false)
        spacingBinding.ivIcon.setImageResource(R.drawable.ic_settings_spacing)
        spacingBinding.ivIcon.visibility = View.VISIBLE
        spacingBinding.tvLabel.text = getString(R.string.settings_app_spacing_dp, currentSpacing)
        spacingBinding.slider.apply {
            valueFrom = 0f
            valueTo = 40f
            stepSize = 1f
            value = currentSpacing.toFloat().coerceIn(0f, 40f)
            addOnChangeListener { _, value, _ ->
                val spacing = value.toInt()
                HomeLayoutPreferences.setAppSpacingDp(this@HomeSettingsActivity, spacing)
                spacingBinding.tvLabel.text = getString(R.string.settings_app_spacing_dp, spacing)
            }
        }
        appSpacingSlider = spacingBinding.slider
        container.addView(spacingBinding.root)

        // Fixed clock at top
        container.addSwitchRow(
            titleResId = R.string.settings_clock_fixed_top,
            subtitleResId = R.string.settings_clock_fixed_top_desc,
            isChecked = HomeLayoutPreferences.isClockFixedTop(this),
            iconResId = R.drawable.ic_settings_clock,
            onCheckedChanged = { checked ->
                HomeLayoutPreferences.setClockFixedTop(this, checked)
            }
        )

        // Widget
        widgetValueView = container.addValueRow(
            titleResId = R.string.settings_widget_title,
            subtitleResId = R.string.settings_widget_settings_desc,
            initialValue = getWidgetStatusText(),
            iconResId = R.drawable.ic_settings_widget,
            onClick = ::onWidgetRowClicked
        )

        // Home layout
        container.addCategoryRow(
            iconResId = R.drawable.ic_settings_layout,
            titleResId = R.string.home_layout_settings_title,
            subtitleResId = R.string.home_layout_settings_desc,
            onClick = { startActivity(Intent(this, HomeLayoutActivity::class.java)) }
        )
    }

    override fun onResume() {
        super.onResume()
        updateWidgetStatus()
    }

    private fun setupClockTypeSelector(container: LinearLayout) {
        val current = FontPreferences.getClockType(this)
        val tiles = mutableListOf<View>()

        ClockType.entries.forEach { type ->
            val item = ItemSettingsClockTypeBinding.inflate(inflater, container, false)
            item.tvPreview.text = type.previewLabel
            val labelRes = when (type) {
                ClockType.DIGITAL -> R.string.settings_clock_digital
                ClockType.STACKED -> R.string.settings_clock_stacked
                ClockType.WORDS -> R.string.settings_clock_words
                ClockType.ANALOG -> R.string.settings_clock_analog
                ClockType.HIDDEN -> R.string.settings_clock_hidden
            }
            item.tvLabel.text = getString(labelRes).lowercase()
            item.root.alpha = if (type == current) 1f else 0.6f
            item.root.setOnClickListener {
                FontPreferences.setClockType(this, type)
                tiles.forEach { it.alpha = 0.6f }
                item.root.alpha = 1f
            }
            container.addView(item.root)
            tiles.add(item.root)
        }
    }

    private fun onWidgetRowClicked() {
        if (WidgetPreferences.isWidgetEnabled(this)) {
            showRemoveWidgetDialog()
        } else {
            widgetPickerLauncher.launch(Intent(this, WidgetPickerActivity::class.java))
        }
    }

    private fun updateWidgetStatus() {
        widgetValueView?.text = getWidgetStatusText()
    }

    private fun getWidgetStatusText(): String =
        getString(
            if (WidgetPreferences.isWidgetEnabled(this)) R.string.settings_widget_remove
            else R.string.settings_widget_none
        ).lowercase()

    private fun showRemoveWidgetDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_widget_title)
            .setMessage(R.string.settings_widget_remove_confirm)
            .setPositiveButton(R.string.settings_widget_remove) { _, _ ->
                WidgetPreferences.clearWidget(this)
                updateWidgetStatus()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
            .let { styleMaterialDialog(it) }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
