package app.focusphone.ui.widget.library

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.storage.StorageManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.IOException

/** Battery charge as the widgets show it. */
data class BatteryInfo(val percent: Int, val charging: Boolean) {
    val full: Boolean get() = charging && percent >= FULL

    private companion object {
        const val FULL = 100
    }
}

/** Used and total bytes of a store (internal storage or memory). */
data class Usage(val usedBytes: Long, val totalBytes: Long) {
    val freeBytes: Long get() = (totalBytes - usedBytes).coerceAtLeast(0L)
    val fraction: Float get() = if (totalBytes <= 0L) 0f else (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
}

/**
 * Reads the device state the system widgets show, and watches the parts that change on
 * their own: the battery and the flashlight.
 *
 * Neither can be watched from the manifest, so they are followed by FocusPhone's process —
 * which, being the launcher, is nearly always alive — and only while a widget that shows
 * them is placed somewhere.
 */
object SystemWidgetMonitor {

    private const val TAG = "SystemWidgetMonitor"
    private const val PERCENT = 100

    private val main = Handler(Looper.getMainLooper())
    private var batteryReceiver: BroadcastReceiver? = null
    private var lastBattery: BatteryInfo? = null
    private var torchCallback: CameraManager.TorchCallback? = null

    @Volatile
    var isTorchOn: Boolean = false
        private set

    // ── Reading ────────────────────────────────────────────────────

    fun battery(context: Context): BatteryInfo {
        val sticky = ContextCompat.registerReceiver(
            context.applicationContext,
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        return batteryFrom(context, sticky)
    }

    private fun batteryFrom(context: Context, intent: Intent?): BatteryInfo {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) {
            level * PERCENT / scale
        } else {
            context.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
        }
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryInfo(percent.coerceIn(0, PERCENT), charging)
    }

    /** Internal storage, measured like the system's own storage screen where possible. */
    fun storage(context: Context): Usage {
        val stat = StatFs(Environment.getDataDirectory().path)
        val free = stat.availableBytes
        val total = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // The advertised size ("128 gb"), not the partition left after the system.
                context.getSystemService(StorageStatsManager::class.java)?.getTotalBytes(StorageManager.UUID_DEFAULT) ?: stat.totalBytes
            } else {
                stat.totalBytes
            }
        } catch (e: IOException) {
            Log.w(TAG, "Total storage unavailable; using the partition size", e)
            stat.totalBytes
        }
        return Usage(usedBytes = (total - free).coerceAtLeast(0L), totalBytes = total)
    }

    fun memory(context: Context): Usage {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(info)
        return Usage(usedBytes = info.totalMem - info.availMem, totalBytes = info.totalMem)
    }

    // ── Flashlight ─────────────────────────────────────────────────

    fun hasTorch(context: Context): Boolean = torchCameraId(context) != null

    fun toggleTorch(context: Context) {
        val manager = context.getSystemService(CameraManager::class.java) ?: return
        val id = torchCameraId(context) ?: return
        try {
            manager.setTorchMode(id, !isTorchOn)
        } catch (e: CameraAccessException) {
            // The camera is in use by another app; the flashlight cannot be driven now.
            Log.w(TAG, "Flashlight unavailable", e)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Camera $id has no flashlight", e)
        }
    }

    private fun torchCameraId(context: Context): String? = try {
        val manager = context.getSystemService(CameraManager::class.java)
        manager?.cameraIdList?.firstOrNull { id ->
            val characteristics = manager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    } catch (e: CameraAccessException) {
        Log.w(TAG, "Cameras unavailable", e)
        null
    }

    // ── Watching ───────────────────────────────────────────────────

    /** Follows the battery and the flashlight exactly while a widget needs them. Call off the main thread. */
    fun sync(context: Context) {
        val app = context.applicationContext
        // The placement checks are binder calls: made here, off the main thread; only the
        // registrations, which must not race each other, are posted to it.
        val battery = FocusWidgetKind.withTrigger(WidgetTrigger.BATTERY).any { it.isPlaced(app) }
        val torch = FocusWidgetKind.withTrigger(WidgetTrigger.TORCH).any { it.isPlaced(app) }
        main.post {
            syncBattery(app, battery)
            syncTorch(app, torch)
        }
    }

    private fun syncBattery(context: Context, needed: Boolean) {
        val current = batteryReceiver
        if (needed && current == null) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    val info = batteryFrom(context, intent)
                    // The broadcast repeats for voltage and temperature; only redraw on news.
                    if (info == lastBattery) return
                    lastBattery = info
                    WidgetUpdater.refresh(context, WidgetTrigger.BATTERY)
                }
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            batteryReceiver = receiver
        } else if (!needed && current != null) {
            context.unregisterReceiver(current)
            batteryReceiver = null
            lastBattery = null
        }
    }

    private fun syncTorch(context: Context, needed: Boolean) {
        val manager = context.getSystemService(CameraManager::class.java) ?: return
        val current = torchCallback
        if (needed && current == null) {
            val torchId = torchCameraId(context) ?: return
            val callback = object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId != torchId || enabled == isTorchOn) return
                    isTorchOn = enabled
                    WidgetUpdater.refresh(context, WidgetTrigger.TORCH)
                }

                override fun onTorchModeUnavailable(cameraId: String) {
                    if (cameraId != torchId || !isTorchOn) return
                    isTorchOn = false
                    WidgetUpdater.refresh(context, WidgetTrigger.TORCH)
                }
            }
            manager.registerTorchCallback(callback, main)
            torchCallback = callback
        } else if (!needed && current != null) {
            manager.unregisterTorchCallback(current)
            torchCallback = null
        }
    }
}
