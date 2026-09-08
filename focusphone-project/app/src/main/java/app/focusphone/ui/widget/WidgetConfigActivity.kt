package app.focusphone.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
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
 * The complete flow is:
 * 1. Allocate a temporary widget ID via the shared [AppWidgetHostManager].
 * 2. Launch [AppWidgetManager.ACTION_APPWIDGET_PICK].
 * 3. **Bind** the returned widget ID to FocusPhone's host (required on modern Android).
 *    If the system denies the implicit bind, launch [AppWidgetManager.ACTION_APPWIDGET_BIND].
 * 4. If the chosen widget requires configuration, forward to its
 *    [AppWidgetManager.ACTION_APPWIDGET_CONFIGURE] activity.
 * 5. On success, persists the widget ID in [WidgetPreferences] and finishes.
 * 6. On cancellation, releases the allocated ID so it does not leak.
 */
class WidgetConfigActivity : BaseActivity() {

    private var allocatedWidgetId: Int = INVALID_WIDGET_ID
    private var pendingWidgetId: Int = INVALID_WIDGET_ID
    private var pendingProvider: AppWidgetProviderInfo? = null

    private val appWidgetManager by lazy { AppWidgetManager.getInstance(this) }

    // ── Lifecycle ──────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate a minimal layout so the window is valid while the picker dialog is shown.
        setContentView(R.layout.activity_widget_config)

        allocatedWidgetId = savedInstanceState?.getInt(KEY_ALLOCATED_WIDGET_ID, INVALID_WIDGET_ID) ?: INVALID_WIDGET_ID
        pendingWidgetId = savedInstanceState?.getInt(KEY_PENDING_WIDGET_ID, INVALID_WIDGET_ID) ?: INVALID_WIDGET_ID

        if (allocatedWidgetId == INVALID_WIDGET_ID) {
            allocatedWidgetId = AppWidgetHostManager.getInstance(this).allocateAppWidgetId()
        }

        launchWidgetPicker(allocatedWidgetId)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_ALLOCATED_WIDGET_ID, allocatedWidgetId)
        outState.putInt(KEY_PENDING_WIDGET_ID, pendingWidgetId)
    }

    // ── Widget picker flow ─────────────────────────────────────────

    private fun launchWidgetPicker(appWidgetId: Int) {
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        try {
            @Suppress("DEPRECATION")
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
            REQUEST_BIND_WIDGET -> handleBindResult(resultCode, data)
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

        // The picker may reuse our allocated ID or hand back a different one.
        // Either way, the allocated ID is no longer needed.
        if (allocatedWidgetId != INVALID_WIDGET_ID && allocatedWidgetId != widgetId) {
            AppWidgetHostManager.getInstance(this).deleteAppWidgetId(allocatedWidgetId)
        }
        allocatedWidgetId = INVALID_WIDGET_ID

        val info = appWidgetManager.getAppWidgetInfo(widgetId)
        if (info == null || info.provider == null) {
            Log.w(TAG, "No AppWidgetProviderInfo or provider for id=$widgetId")
            AppWidgetHostManager.getInstance(this).deleteAppWidgetId(widgetId)
            releaseAndFinish()
            return
        }

        pendingWidgetId = widgetId
        pendingProvider = info

        // ── CRITICAL: Bind widget to our host ──────────────────────
        val bound = AppWidgetHostManager.getInstance(this)
            .bindAppWidgetIdIfAllowed(widgetId, info.provider)

        if (!bound) {
            // System requires explicit user consent to bind this widget
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            }
            try {
                @Suppress("DEPRECATION")
                startActivityForResult(bindIntent, REQUEST_BIND_WIDGET)
                return
            } catch (e: Exception) {
                Log.e(TAG, "Unable to start widget bind activity", e)
                AppWidgetHostManager.getInstance(this).deleteAppWidgetId(widgetId)
                releaseAndFinish()
                return
            }
        }

        // Binding succeeded — proceed to optional configuration
        proceedToConfigurationOrSave(widgetId, info)
    }

    private fun handleBindResult(resultCode: Int, data: Intent?) {
        val widgetId = pendingWidgetId
        val info = pendingProvider

        if (resultCode != RESULT_OK || widgetId == INVALID_WIDGET_ID || info == null || info.provider == null) {
            // User denied the bind — clean up
            if (widgetId != INVALID_WIDGET_ID) {
                AppWidgetHostManager.getInstance(this).deleteAppWidgetId(widgetId)
            }
            releaseAndFinish()
            return
        }

        // Re-verify the bind actually took effect (some OEMs lie)
        val verified = AppWidgetHostManager.getInstance(this)
            .bindAppWidgetIdIfAllowed(widgetId, info.provider)
        if (!verified) {
            Log.w(TAG, "Bind result was OK but verification failed for id=$widgetId")
            AppWidgetHostManager.getInstance(this).deleteAppWidgetId(widgetId)
            releaseAndFinish()
            return
        }

        proceedToConfigurationOrSave(widgetId, info)
    }

    private fun proceedToConfigurationOrSave(widgetId: Int, info: AppWidgetProviderInfo) {
        if (info.configure != null) {
            val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = info.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            try {
                @Suppress("DEPRECATION")
                startActivityForResult(configureIntent, REQUEST_CONFIGURE_WIDGET)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch widget configuration", e)
                // Fall through and save anyway; the widget may work with defaults
            }
        }
        saveWidgetAndFinish(widgetId)
    }

    private fun handleConfigureResult(resultCode: Int, data: Intent?) {
        val widgetId = pendingWidgetId
        if (resultCode == RESULT_OK && widgetId != INVALID_WIDGET_ID) {
            saveWidgetAndFinish(widgetId)
        } else {
            // User cancelled configuration — treat as cancelled
            if (widgetId != INVALID_WIDGET_ID) {
                AppWidgetHostManager.getInstance(this).deleteAppWidgetId(widgetId)
            }
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
            AppWidgetHostManager.getInstance(this).deleteAppWidgetId(allocatedWidgetId)
            allocatedWidgetId = INVALID_WIDGET_ID
        }
        pendingWidgetId = INVALID_WIDGET_ID
        pendingProvider = null
        setResult(RESULT_CANCELED)
        finish()
    }

    // ── Companion ──────────────────────────────────────────────────

    companion object {
        private const val TAG = "WidgetConfigActivity"
        private const val REQUEST_PICK_WIDGET = 1001
        private const val REQUEST_BIND_WIDGET = 1003
        private const val REQUEST_CONFIGURE_WIDGET = 1002
        private const val INVALID_WIDGET_ID = -1

        private const val KEY_ALLOCATED_WIDGET_ID = "allocated_widget_id"
        private const val KEY_PENDING_WIDGET_ID = "pending_widget_id"
    }
}
