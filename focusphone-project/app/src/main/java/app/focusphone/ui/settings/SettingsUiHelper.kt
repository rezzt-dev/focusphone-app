package app.focusphone.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.focusphone.R
import app.focusphone.databinding.ItemSettingsCategoryBinding
import app.focusphone.databinding.ItemSettingsSwitchBinding
import app.focusphone.databinding.ItemSettingsValueBinding
import app.focusphone.databinding.SettingsSectionHeaderBinding
import com.google.android.material.slider.Slider

/**
 * Helper functions to inflate the reusable settings row layouts into a parent container.
 *
 * All helpers return the created root [View] so callers can store references if needed
 * (e.g. to update a value label later in [android.app.Activity.onResume]).
 *
 * Rows are grouped with [settingsSection], which owns the header and the separators.
 * Screens therefore never inflate `settings_divider` by hand, which is what previously
 * left some screens with separators between every row and others with none at all.
 */

/** Inflates a section header (e.g. "APPEARANCE"). */
fun LinearLayout.addSectionHeader(@StringRes titleResId: Int): View {
    val binding = SettingsSectionHeaderBinding.inflate(LayoutInflater.from(context), this, false)
    binding.tvHeader.setText(titleResId)
    addView(binding.root)
    return binding.root
}

/**
 * Builds one settings section: an optional header, the rows produced by [content], and a
 * hairline between consecutive rows.
 *
 * Separators are only drawn between two standard rows. A slider, a selector or an inline
 * panel sits flush against its neighbour, because a rule above a slider reads as a divider
 * for a control that has no top edge of its own.
 *
 * @return the group that holds the rows, so callers can show or hide a whole section.
 */
fun LinearLayout.settingsSection(
    @StringRes titleResId: Int? = null,
    content: LinearLayout.() -> Unit
): LinearLayout {
    titleResId?.let { addSectionHeader(it) }

    val group = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    group.content()
    group.insertRowSeparators()
    addView(group)
    return group
}

/** Inserts a hairline between every pair of adjacent standard rows in this group. */
private fun LinearLayout.insertRowSeparators() {
    val inflater = LayoutInflater.from(context)
    var index = 1
    while (index < childCount) {
        val previous = getChildAt(index - 1)
        val current = getChildAt(index)
        if (previous.isSettingsRow() && current.isSettingsRow()) {
            addView(inflater.inflate(R.layout.settings_divider, this, false), index)
            index += 2
        } else {
            index++
        }
    }
}

private fun View.isSettingsRow(): Boolean = getTag(R.id.tag_settings_row) == true

private fun View.markAsSettingsRow(): View = apply {
    setTag(R.id.tag_settings_row, true)
    setTag(R.id.tag_skip_recolor, true)
}

/** Persists a slider value once per drag instead of once per pixel of movement. */
fun Slider.persistOnStop(persist: (Float) -> Unit) {
    addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: Slider) = Unit

        override fun onStopTrackingTouch(slider: Slider) {
            persist(slider.value)
        }
    })
}

/**
 * Inflates a navigable category row:
 *   [icon] Title (bold)
 *          Subtitle
 *                              ›
 */
fun LinearLayout.addCategoryRow(
    @DrawableRes iconResId: Int,
    @StringRes titleResId: Int,
    @StringRes subtitleResId: Int,
    onClick: () -> Unit
): View {
    val binding = ItemSettingsCategoryBinding.inflate(LayoutInflater.from(context), this, false)
    binding.ivIcon.setImageResource(iconResId)
    binding.tvTitle.setText(titleResId)
    binding.tvSubtitle.setText(subtitleResId)
    binding.root.setOnClickListener { onClick() }
    addView(binding.root)
    return binding.root.markAsSettingsRow()
}

/**
 * Inflates a switch row.
 * The listener is invoked **after** the internal state has been updated.
 * Returns the root view of the row so callers can show/hide or disable it.
 */
fun LinearLayout.addSwitchRow(
    @StringRes titleResId: Int,
    @StringRes subtitleResId: Int,
    isChecked: Boolean,
    @DrawableRes iconResId: Int? = null,
    onCheckedChanged: (Boolean) -> Unit
): View {
    val binding = ItemSettingsSwitchBinding.inflate(LayoutInflater.from(context), this, false)
    if (iconResId != null) {
        binding.ivIcon.setImageResource(iconResId)
        binding.ivIcon.visibility = View.VISIBLE
    }
    binding.tvTitle.setText(titleResId)
    binding.tvSubtitle.setText(subtitleResId)
    binding.switchToggle.isChecked = isChecked
    binding.switchToggle.setOnCheckedChangeListener { _, checked ->
        onCheckedChanged(checked)
    }
    // The whole row is a 64dp target and already looks tappable; without this, only the
    // switch itself responded and every tap on the label was silently dropped.
    binding.root.setOnClickListener { binding.switchToggle.toggle() }
    addView(binding.root)
    return binding.root.markAsSettingsRow()
}

/**
 * Inflates a value row:
 *   [icon] Title (bold)          value   ›
 *          Subtitle
 *
 * [initialValue] may be null, in which case the value label is hidden.
 */
fun LinearLayout.addValueRow(
    @StringRes titleResId: Int,
    @StringRes subtitleResId: Int,
    initialValue: String?,
    @DrawableRes iconResId: Int? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit
): TextView = addValueRowBinding(titleResId, subtitleResId, initialValue, iconResId, showChevron, onClick).tvValue

/** [addValueRow] returning the whole row, for screens that also relabel its title or subtitle. */
fun LinearLayout.addValueRowBinding(
    @StringRes titleResId: Int,
    @StringRes subtitleResId: Int,
    initialValue: String?,
    @DrawableRes iconResId: Int? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit
): ItemSettingsValueBinding {
    val binding = ItemSettingsValueBinding.inflate(LayoutInflater.from(context), this, false)
    if (iconResId != null) {
        binding.ivIcon.setImageResource(iconResId)
        binding.ivIcon.visibility = View.VISIBLE
    }
    binding.tvTitle.setText(titleResId)
    binding.tvSubtitle.setText(subtitleResId)
    if (initialValue == null) {
        binding.tvValue.visibility = View.GONE
    } else {
        binding.tvValue.text = initialValue
    }
    binding.tvChevron.visibility = if (showChevron) View.VISIBLE else View.GONE
    binding.root.setOnClickListener { onClick() }
    addView(binding.root)
    binding.root.markAsSettingsRow()
    return binding
}

/** Inflates a simple clickable info/action row with no chevron (e.g. version). */
fun LinearLayout.addInfoRow(
    @StringRes titleResId: Int,
    @StringRes subtitleResId: Int,
    @DrawableRes iconResId: Int? = null,
    onClick: () -> Unit
): View {
    val binding = ItemSettingsValueBinding.inflate(LayoutInflater.from(context), this, false)
    if (iconResId != null) {
        binding.ivIcon.setImageResource(iconResId)
        binding.ivIcon.visibility = View.VISIBLE
    }
    binding.tvTitle.setText(titleResId)
    binding.tvSubtitle.setText(subtitleResId)
    binding.tvValue.visibility = View.GONE
    binding.tvChevron.visibility = View.GONE
    binding.root.setOnClickListener { onClick() }
    addView(binding.root)
    return binding.root.markAsSettingsRow()
}

/**
 * Enables or disables a settings row, dimming it so the state is visible rather than only
 * discoverable by tapping.
 */
fun View.setRowEnabled(enabled: Boolean) {
    isEnabled = enabled
    alpha = if (enabled) 1f else DISABLED_ALPHA
    if (this is ViewGroup) {
        for (index in 0 until childCount) {
            getChildAt(index).isEnabled = enabled
        }
    }
}

private const val DISABLED_ALPHA = 0.45f
