package app.focusphone.ui.widget.library

import android.content.Context
import android.text.format.Formatter
import android.widget.RemoteViews
import app.focusphone.R
import java.text.NumberFormat
import java.util.Locale

/** Battery, storage, the system monitor, quick settings and the next alarm. */
object SystemRenderers {

    val BatteryRing = WidgetRenderer { scene ->
        val style = scene.style
        val battery = SystemWidgetMonitor.battery(scene.context)
        scene.views(R.layout.widget_battery_ring).apply {
            card(R.id.wg_bg, style)
            tint(R.id.wg_ring_track, style.track)
            val ringSide = scene.px((scene.size.squareDp - 2 * ROUND_INSET_DP).toFloat().coerceAtLeast(MIN_RING_DP))
            setImageViewBitmap(R.id.wg_ring, WidgetBitmaps.ring(ringSide, battery.percent / PERCENT))
            tint(R.id.wg_ring, style.accent)
            icon(R.id.wg_status_icon, if (battery.charging) R.drawable.ic_wg_zap else R.drawable.ic_wg_battery, style.textSecondary)
            setTextViewText(R.id.wg_value, percent(battery.percent / PERCENT))
            setTextViewText(R.id.wg_caption, scene.string(batteryCaption(battery)))
            setTextColor(R.id.wg_value, style.textPrimary)
            setTextColor(R.id.wg_caption, style.textSecondary)
            setOnClickPendingIntent(R.id.wg_root, WidgetIntents.openSystem(scene.context, WidgetIntents.SystemPanel.BATTERY))
        }
    }

    val Storage = WidgetRenderer { scene ->
        val style = scene.style
        val context = scene.context
        val storage = SystemWidgetMonitor.storage(context)
        scene.views(R.layout.widget_storage).apply {
            card(R.id.wg_bg, style)
            tint(R.id.wg_icon, style.textSecondary)
            setTextViewText(R.id.wg_value, percent(storage.fraction))
            setTextViewText(R.id.wg_detail, scene.string(R.string.wg_free_format, size(context, storage.freeBytes)))
            textColor(style.textPrimary, R.id.wg_value)
            textColor(style.textSecondary, R.id.wg_caption, R.id.wg_detail)
            bindBar(scene, R.id.wg_bar, STORAGE_BAR_DP, storage.fraction)
            setOnClickPendingIntent(R.id.wg_root, WidgetIntents.openSystem(context, WidgetIntents.SystemPanel.STORAGE))
        }
    }

    val Monitor = WidgetRenderer { scene ->
        val style = scene.style
        val context = scene.context
        val battery = SystemWidgetMonitor.battery(context)
        val storage = SystemWidgetMonitor.storage(context)
        val memory = SystemWidgetMonitor.memory(context)
        val barDp = scene.size.widthDp - 2 * PADDING_DP
        scene.views(R.layout.widget_system_monitor).apply {
            card(R.id.wg_bg, style)
            bindRow(
                scene,
                MonitorRow(R.id.wg_battery_icon, R.id.wg_battery_label, R.id.wg_battery_value, R.id.wg_battery_bar),
                if (battery.charging) R.drawable.ic_wg_battery_charging else R.drawable.ic_wg_battery,
                percent(battery.percent / PERCENT),
                barDp,
                battery.percent / PERCENT
            )
            bindRow(
                scene,
                MonitorRow(R.id.wg_storage_icon, R.id.wg_storage_label, R.id.wg_storage_value, R.id.wg_storage_bar),
                R.drawable.ic_wg_hard_drive,
                scene.string(R.string.wg_free_format, size(context, storage.freeBytes)),
                barDp,
                storage.fraction
            )
            bindRow(
                scene,
                MonitorRow(R.id.wg_memory_icon, R.id.wg_memory_label, R.id.wg_memory_value, R.id.wg_memory_bar),
                R.drawable.ic_wg_memory_stick,
                scene.string(R.string.wg_free_format, size(context, memory.freeBytes)),
                barDp,
                memory.fraction
            )
            setOnClickPendingIntent(R.id.wg_root, WidgetIntents.openSystem(context, WidgetIntents.SystemPanel.SETTINGS))
        }
    }

    /** Flashlight (a real toggle), then shortcuts to the internet, bluetooth and volume panels and settings. */
    val Toggles = WidgetRenderer { scene ->
        val style = scene.style
        val context = scene.context
        val torchOn = SystemWidgetMonitor.isTorchOn
        scene.views(R.layout.widget_quick_toggles).apply {
            card(R.id.wg_bg, style)
            listOf(
                Toggle(R.id.wg_torch, R.id.wg_torch_fill, R.id.wg_torch_icon),
                Toggle(R.id.wg_internet, R.id.wg_internet_fill, R.id.wg_internet_icon),
                Toggle(R.id.wg_bluetooth, R.id.wg_bluetooth_fill, R.id.wg_bluetooth_icon),
                Toggle(R.id.wg_volume, R.id.wg_volume_fill, R.id.wg_volume_icon),
                Toggle(R.id.wg_settings, R.id.wg_settings_fill, R.id.wg_settings_icon)
            ).forEach { toggle ->
                val active = toggle.button == R.id.wg_torch && torchOn
                tint(toggle.fill, if (active) style.accent else style.chip)
                tint(toggle.icon, if (active) style.onAccent else style.textPrimary)
            }
            setImageViewResource(R.id.wg_torch_icon, if (torchOn) R.drawable.ic_wg_flashlight else R.drawable.ic_wg_flashlight_off)
            // Without a flashlight the remaining four buttons share the width.
            show(R.id.wg_torch_slot, SystemWidgetMonitor.hasTorch(context))
            setOnClickPendingIntent(R.id.wg_torch, WidgetIntents.action(context, WidgetActionReceiver.ACTION_TORCH_TOGGLE))
            setOnClickPendingIntent(R.id.wg_internet, WidgetIntents.openSystem(context, WidgetIntents.SystemPanel.INTERNET))
            setOnClickPendingIntent(R.id.wg_bluetooth, WidgetIntents.openSystem(context, WidgetIntents.SystemPanel.BLUETOOTH))
            setOnClickPendingIntent(R.id.wg_volume, WidgetIntents.openSystem(context, WidgetIntents.SystemPanel.VOLUME))
            setOnClickPendingIntent(R.id.wg_settings, WidgetIntents.openSystem(context, WidgetIntents.SystemPanel.SETTINGS))
        }
    }

    val NextAlarm = WidgetRenderer { scene ->
        val style = scene.style
        val context = scene.context
        val alarm = ClockRenderers.nextAlarm(context)
        scene.views(R.layout.widget_next_alarm).apply {
            card(R.id.wg_bg, style)
            tint(R.id.wg_badge_fill, style.chip)
            icon(
                R.id.wg_badge_icon,
                if (alarm !=
                    null
                ) R.drawable.ic_wg_alarm_clock else R.drawable.ic_wg_alarm_clock_off,
                style.textPrimary
            )
            if (alarm != null) {
                setTextViewText(R.id.wg_value, WidgetDates.time(context, alarm))
                setTextViewText(R.id.wg_caption, WidgetDates.relativeDay(context, alarm, System.currentTimeMillis()))
                textSizeSp(R.id.wg_value, ALARM_TIME_SP)
            } else {
                setTextViewText(R.id.wg_value, scene.string(R.string.wg_alarm_none))
                setTextViewText(R.id.wg_caption, "")
                textSizeSp(R.id.wg_value, ALARM_NONE_SP)
            }
            show(R.id.wg_caption, alarm != null)
            setTextColor(R.id.wg_value, style.textPrimary)
            setTextColor(R.id.wg_caption, style.textSecondary)
            setOnClickPendingIntent(R.id.wg_root, WidgetIntents.openClock(context))
        }
    }

    // ── Shared parts ───────────────────────────────────────────────

    private const val PERCENT = 100f
    private const val ROUND_INSET_DP = 14
    private const val MIN_RING_DP = 48f
    private const val PADDING_DP = 18
    private const val STORAGE_BAR_DP = 120
    private const val ALARM_TIME_SP = 22f
    private const val ALARM_NONE_SP = 16f

    private class MonitorRow(val icon: Int, val label: Int, val value: Int, val bar: Int)

    private class Toggle(val button: Int, val fill: Int, val icon: Int)

    private fun RemoteViews.bindRow(scene: WidgetScene, row: MonitorRow, iconRes: Int, value: String, barDp: Int, fraction: Float) {
        val style = scene.style
        icon(row.icon, iconRes, style.textSecondary)
        setTextColor(row.label, style.textPrimary)
        setTextViewText(row.value, value)
        setTextColor(row.value, style.textSecondary)
        bindBar(scene, row.bar, barDp, fraction)
    }

    /** Paints a bar's track and draws its filled part at the width it will be shown at. */
    internal fun RemoteViews.bindBar(scene: WidgetScene, barId: Int, widthDp: Int, fraction: Float) {
        tint(trackOf(barId), scene.style.track)
        val bitmap = WidgetBitmaps.bar(scene.px(widthDp.toFloat().coerceAtLeast(1f)), scene.px(BAR_HEIGHT_DP), fraction)
        setImageViewBitmap(barId, bitmap)
        tint(barId, scene.style.accent)
    }

    private const val BAR_HEIGHT_DP = 6f

    /** Every bar's track is its id's "_track" sibling. */
    private fun trackOf(barId: Int): Int = when (barId) {
        R.id.wg_bar -> R.id.wg_bar_track
        R.id.wg_battery_bar -> R.id.wg_battery_bar_track
        R.id.wg_storage_bar -> R.id.wg_storage_bar_track
        R.id.wg_memory_bar -> R.id.wg_memory_bar_track
        R.id.wg_day_bar -> R.id.wg_day_bar_track
        R.id.wg_week_bar -> R.id.wg_week_bar_track
        R.id.wg_month_bar -> R.id.wg_month_bar_track
        else -> R.id.wg_year_bar_track
    }

    private fun batteryCaption(battery: BatteryInfo): Int = when {
        battery.full -> R.string.wg_battery_full
        battery.charging -> R.string.wg_battery_charging
        else -> R.string.wg_battery
    }

    internal fun percent(fraction: Float): String =
        NumberFormat.getPercentInstance(Locale.getDefault()).apply { maximumFractionDigits = 0 }.format(fraction.toDouble())

    private fun size(context: Context, bytes: Long): String = Formatter.formatShortFileSize(context, bytes).lowercase(Locale.getDefault())
}
