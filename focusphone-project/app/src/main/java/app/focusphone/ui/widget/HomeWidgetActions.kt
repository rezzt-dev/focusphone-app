package app.focusphone.ui.widget

import android.app.Activity
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import app.focusphone.R
import app.focusphone.data.preferences.WidgetPreferences
import app.focusphone.databinding.SettingsSliderRowBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Everything the user can do with the home widget, shared by the home screen (long press on
 * the widget) and the home settings, so both offer the same options in the same words:
 * change it, adjust its height, open its own settings and remove it.
 *
 * Must be created while the [activity] is being initialised (as a property), because it
 * registers an activity-result launcher.
 *
 * @param showDialog shows a dialog styled like the rest of the screen.
 * @param onWidgetChanged runs after the widget was added, replaced, resized or removed.
 * @param onHeightPreview receives the height being dragged on the slider (null when the
 *   preview ends), so a screen that shows the widget can resize it live.
 */
class HomeWidgetActions(
    private val activity: ComponentActivity,
    private val showDialog: (AlertDialog) -> Unit,
    private val onWidgetChanged: () -> Unit,
    private val onHeightPreview: (Int?) -> Unit = {}
) {

    // Lazy: the actions are built as an Activity property, before it has a base context.
    private val hostManager by lazy { AppWidgetHostManager.getInstance(activity) }

    private val pickerLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) onWidgetChanged()
    }

    /** Opens FocusPhone's widget picker. */
    fun openPicker() {
        pickerLauncher.launch(Intent(activity, WidgetPickerActivity::class.java))
    }

    /** Offers the widget's options, or goes straight to the picker when there is no widget. */
    fun showOptions(anchor: View? = null) {
        val state = hostManager.resolveHomeWidget(activity)
        if (state == AppWidgetHostManager.HomeWidget.None) {
            openPicker()
            return
        }
        val info = (state as? AppWidgetHostManager.HomeWidget.Ready)?.info

        val actions = buildList<Pair<Int, () -> Unit>> {
            add(R.string.widget_option_change to ::openPicker)
            if (info != null) {
                add(R.string.widget_option_height to { showHeightDialog(info) })
                if (isReconfigurable(info)) add(R.string.widget_option_settings to { reconfigure() })
            }
            add(R.string.settings_widget_remove to ::confirmRemove)
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(info?.let(::labelOf) ?: activity.getString(R.string.settings_widget_title))
            .setItems(actions.map { activity.getString(it.first) }.toTypedArray()) { _, which ->
                actions.getOrNull(which)?.second?.invoke()
            }
            .create()
        anchor?.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
        showDialog(dialog)
    }

    /** Asks before removing the widget, then releases it. */
    fun confirmRemove() {
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.settings_widget_title)
            .setMessage(R.string.settings_widget_remove_confirm)
            .setPositiveButton(R.string.settings_widget_remove) { _, _ ->
                // Releases the ID too, so the provider stops updating a widget nobody shows.
                hostManager.removeHomeWidget(activity)
                onWidgetChanged()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
        showDialog(dialog)
    }

    /**
     * Lets the user pick the widget's height on a slider. The dialog sits at the bottom with
     * a light dim so the widget above it can be watched while it resizes.
     */
    fun showHeightDialog(info: AppWidgetProviderInfo) {
        val range = WidgetHeights.sliderRange(activity)
        val initial = WidgetSizing.snap(WidgetHeights.currentHeightDp(activity, info)).coerceIn(range)
        val row = SettingsSliderRowBinding.inflate(activity.layoutInflater)
        row.ivIcon.setImageResource(R.drawable.ic_settings_widget)
        row.ivIcon.visibility = View.VISIBLE
        row.tvLabel.text = activity.getString(R.string.widget_height_label, initial)
        row.slider.apply {
            valueFrom = range.first.toFloat()
            valueTo = range.last.toFloat()
            stepSize = WidgetSizing.HEIGHT_STEP_DP.toFloat()
            value = initial.toFloat()
            setLabelFormatter { activity.getString(R.string.widget_height_value, it.toInt()) }
            addOnChangeListener { _, value, fromUser ->
                row.tvLabel.text = activity.getString(R.string.widget_height_label, value.toInt())
                if (fromUser) onHeightPreview(value.toInt())
            }
        }

        var saved = false
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(labelOf(info))
            .setView(row.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                saved = true
                WidgetPreferences.setHeightDp(activity, row.slider.value.toInt())
                onHeightPreview(null)
                onWidgetChanged()
            }
            .setNeutralButton(R.string.action_reset) { _, _ ->
                saved = true
                WidgetPreferences.setHeightDp(activity, WidgetPreferences.HEIGHT_AUTO)
                onHeightPreview(null)
                onWidgetChanged()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
        dialog.setOnDismissListener { if (!saved) onHeightPreview(null) }
        dialog.window?.apply {
            setGravity(Gravity.BOTTOM)
            setDimAmount(HEIGHT_DIALOG_DIM)
        }
        showDialog(dialog)
    }

    /** Opens the provider's own settings for the current widget. */
    private fun reconfigure() {
        val widgetId = WidgetPreferences.getWidgetId(activity)
        // The result is not needed: the provider pushes its new look on its own.
        if (!hostManager.startConfigureActivity(activity, widgetId, REQUEST_RECONFIGURE)) {
            Toast.makeText(activity, R.string.widget_picker_config_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun isReconfigurable(info: AppWidgetProviderInfo): Boolean =
        info.configure != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE != 0

    private fun labelOf(info: AppWidgetProviderInfo): String =
        info.loadLabel(activity.packageManager)?.toString()?.lowercase()
            ?: activity.getString(R.string.settings_widget_title)

    private companion object {
        const val REQUEST_RECONFIGURE = 0x5718
        const val HEIGHT_DIALOG_DIM = 0.15f
    }
}
