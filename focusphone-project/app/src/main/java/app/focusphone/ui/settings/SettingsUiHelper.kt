package app.focusphone.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.focusphone.databinding.ItemSettingsCategoryBinding
import app.focusphone.databinding.ItemSettingsSwitchBinding
import app.focusphone.databinding.ItemSettingsValueBinding
import app.focusphone.databinding.SettingsSectionHeaderBinding
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Helper functions to inflate the reusable settings row layouts into a parent container.
 *
 * All helpers return the created root [View] so callers can store references if needed
 * (e.g. to update a value label later in [onResume]).
 */

/** Inflates a section header (e.g. "APPEARANCE"). */
fun LinearLayout.addSectionHeader(@StringRes titleResId: Int): View {
    val binding = SettingsSectionHeaderBinding.inflate(LayoutInflater.from(context), this, false)
    binding.tvHeader.setText(titleResId)
    addView(binding.root)
    return binding.root
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
    return binding.root
}

/**
 * Inflates a switch row.
 * The listener is invoked **after** the internal state has been updated.
 */
fun LinearLayout.addSwitchRow(
    @StringRes titleResId: Int,
    @StringRes subtitleResId: Int,
    isChecked: Boolean,
    @DrawableRes iconResId: Int? = null,
    onCheckedChanged: (Boolean) -> Unit
): MaterialSwitch {
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
    addView(binding.root)
    return binding.switchToggle
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
): TextView {
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
    return binding.tvValue
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
    return binding.root
}
