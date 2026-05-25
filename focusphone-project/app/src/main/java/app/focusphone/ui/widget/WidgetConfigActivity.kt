package app.focusphone.ui.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import app.focusphone.R
import app.focusphone.data.preferences.WidgetPreferences
import app.focusphone.ui.base.BaseActivity

/**
 * Activity that hosts the system widget-picker flow.
 *
 * 1. Allocates a temporary widget ID via [AppWidgetHost].
 * 2. Launches [AppWidgetManager.ACTION_APPWIDGET_PICK].
 * 3. If the chosen widget requires configuration, forwards to its
 *    [AppWidgetManager.ACTION_APPWIDGET_CONFIGURE] activity.
 * 4. On success, persists the widget ID in [WidgetPreferences] and finishes.
 * 5. On cancellation, releases the allocated ID so it does not leak.
 */
class WidgetConfigActivity : BaseActivity() {

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost
    private var allocatedWidgetId: Int = INVALID_WIDGET_ID

    // ── Lifecycle ──────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate a minimal layout so the window is valid while the picker dialog is shown.
        setContentView(R.layout.activity_widget_config)

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, WIDGET_HOST_ID)

        allocatedWidgetId = appWidgetHost.allocateAppWidgetId()
        launchWidgetPicker(allocatedWidgetId)
    }

    // ── Widget picker flow ─────────────────────────────────────────

    private fun launchWidgetPicker(appWidgetId: Int) {
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        try {
            startActivityForResult(pickIntent, REQUEST_PICK_WIDGET)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start widget picker", e)
            Toast.makeText(this, R.string.error_app_not_found, Toast.LENGTH_SHORT).show()
            releaseAndFinish()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_PICK_WIDGET -> handlePickResult(resultCode, data)
            REQUEST_CONFIGURE_WIDGET -> handleConfigureResult(resultCode, data)
        }
    }

    private fun handlePickResult(resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK || data == null) {
            releaseAndFinish()
            return
        }

        val widgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, INVALID_WIDGET_ID)
        if (widgetId == INVALID_WIDGET_ID) {
            releaseAndFinish()
            return
        }

        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetId)

        if (appWidgetInfo?.configure != null) {
            // Widget requires configuration — forward to its config activity
            val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = appWidgetInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            try {
                startActivityForResult(configureIntent, REQUEST_CONFIGURE_WIDGET)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch widget configuration", e)
                // Continue and save the widget even if config fails
            }
        }

        saveWidgetAndFinish(widgetId)
    }

    private fun handleConfigureResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val widgetId = data.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                allocatedWidgetId
            )
            saveWidgetAndFinish(widgetId)
        } else {
            // User cancelled configuration — treat as cancelled
            releaseAndFinish()
        }
    }

    // ── Persistence ────────────────────────────────────────────────

    private fun saveWidgetAndFinish(widgetId: Int) {
        WidgetPreferences.setWidgetId(this, widgetId)
        WidgetPreferences.setWidgetEnabled(this, true)
        setResult(RESULT_OK)
        finish()
    }

    private fun releaseAndFinish() {
        if (allocatedWidgetId != INVALID_WIDGET_ID) {
            try {
                appWidgetHost.deleteAppWidgetId(allocatedWidgetId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete allocated widget ID", e)
            }
            allocatedWidgetId = INVALID_WIDGET_ID
        }
        setResult(RESULT_CANCELED)
        finish()
    }

    // ── Companion ──────────────────────────────────────────────────

    companion object {
        private const val TAG = "WidgetConfigActivity"
        private const val WIDGET_HOST_ID = 1024
        private const val REQUEST_PICK_WIDGET = 1001
        private const val REQUEST_CONFIGURE_WIDGET = 1002
        private const val INVALID_WIDGET_ID = -1
    }
}
