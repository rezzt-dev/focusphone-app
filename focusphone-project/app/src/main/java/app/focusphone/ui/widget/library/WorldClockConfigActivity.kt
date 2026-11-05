package app.focusphone.ui.widget.library

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import app.focusphone.R
import app.focusphone.data.preferences.LibraryWidgetPreferences
import app.focusphone.databinding.ItemSettingsSwitchBinding
import app.focusphone.ui.settings.SettingsSubActivity
import app.focusphone.ui.settings.settingsSection
import java.util.TimeZone

/**
 * Picks the cities of one world-clock widget.
 *
 * The configuration is optional: the widget starts with three well-known cities, so the
 * result is OK from the start and leaving this screen at any point keeps the widget.
 * Every change is saved and drawn at once.
 */
class WorldClockConfigActivity : SettingsSubActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val selected = mutableListOf<String>()

    /** Set while a refused toggle is being undone, so the undo is not handled as a toggle. */
    private var reverting = false

    override fun getScreenTitleRes(): Int = R.string.wg_world_config_title

    override fun onCreate(savedInstanceState: Bundle?) {
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        selected += LibraryWidgetPreferences.getWorldZones(this, widgetId)
        super.onCreate(savedInstanceState)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) finish()
    }

    override fun buildContent(container: LinearLayout) {
        val now = System.currentTimeMillis()
        val local = TimeZone.getDefault().id
        // The device's own zone comes first, even when it is not one of the listed cities.
        val zones = (listOfNotNull(WorldClockZones.find(local)) + WorldClockZones.ALL).distinctBy { it.id }
        container.settingsSection(R.string.wg_world_config_section) {
            zones.forEach { zone -> addView(zoneRow(this, zone, now)) }
        }
    }

    private fun zoneRow(parent: LinearLayout, zone: WorldZone, now: Long): View {
        val row = ItemSettingsSwitchBinding.inflate(LayoutInflater.from(this), parent, false)
        row.tvTitle.text = zone.city(this)
        row.tvSubtitle.text = WorldClockZones.offsetLabel(this, zone.id, now)
        row.switchToggle.isChecked = zone.id in selected
        row.switchToggle.setOnCheckedChangeListener { button, checked ->
            if (reverting) return@setOnCheckedChangeListener
            onToggle(zone.id, checked) {
                reverting = true
                button.isChecked = !checked
                reverting = false
            }
        }
        row.root.setOnClickListener { row.switchToggle.toggle() }
        row.root.setTag(R.id.tag_settings_row, true)
        return row.root
    }

    private fun onToggle(zoneId: String, checked: Boolean, revert: () -> Unit) {
        when {
            checked && zoneId !in selected && selected.size >= LibraryWidgetPreferences.MAX_WORLD_ZONES -> {
                Toast.makeText(this, R.string.wg_world_config_limit, Toast.LENGTH_SHORT).show()
                revert()
                return
            }

            // A world clock with no city would be an empty card; keep the last one.
            !checked && selected.size == 1 && zoneId in selected -> {
                Toast.makeText(this, R.string.wg_world_config_hint, Toast.LENGTH_SHORT).show()
                revert()
                return
            }

            checked -> if (zoneId !in selected) selected += zoneId

            else -> selected -= zoneId
        }
        LibraryWidgetPreferences.setWorldZones(this, widgetId, selected)
        WidgetUpdater.refresh(this, listOf(FocusWidgetKind.CLOCK_WORLD))
    }

    companion object {
        fun intent(context: Context, widgetId: Int): Intent =
            Intent(context, WorldClockConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
    }
}
