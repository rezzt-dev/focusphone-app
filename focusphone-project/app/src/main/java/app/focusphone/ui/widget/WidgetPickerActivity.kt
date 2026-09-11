package app.focusphone.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.focusphone.R
import app.focusphone.databinding.ActivityWidgetPickerBinding
import app.focusphone.ui.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Built-in widget picker for FocusPhone.
 *
 * Displays all installed widget providers using [AppWidgetManager.getInstalledProviders].
 * When the user selects one:
 * 1. Allocates a widget ID.
 * 2. Binds it to FocusPhone's host via [AppWidgetHostManager.bindAppWidgetIdIfAllowed].
 * 3. If the system requires explicit permission, launches [AppWidgetManager.ACTION_APPWIDGET_BIND].
 * 4. If the widget has a configuration activity, forwards to it.
 * 5. Persists the widget ID on success.
 *
 * Using our own picker eliminates the unreliability of the system [AppWidgetManager.ACTION_APPWIDGET_PICK]
 * on heavily skinned Android devices.
 */
class WidgetPickerActivity : BaseActivity() {

    private lateinit var binding: ActivityWidgetPickerBinding
    private lateinit var adapter: WidgetPickerAdapter

    private var allocatedWidgetId: Int = INVALID_WIDGET_ID
    private var pendingProvider: ComponentName? = null

    private val appWidgetManager by lazy { AppWidgetManager.getInstance(this) }

    // ── Lifecycle ──────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isSelectionInProgress = true
        binding = ActivityWidgetPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        allocatedWidgetId = savedInstanceState?.getInt(KEY_ALLOCATED_WIDGET_ID, INVALID_WIDGET_ID) ?: INVALID_WIDGET_ID
        pendingProvider = savedInstanceState?.getParcelable(KEY_PENDING_PROVIDER)

        setupInsets()
        setupRecyclerView()
        loadWidgets()
    }

    override fun onDestroy() {
        isSelectionInProgress = false
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_ALLOCATED_WIDGET_ID, allocatedWidgetId)
        pendingProvider?.let { outState.putParcelable(KEY_PENDING_PROVIDER, it) }
    }

    // ── UI setup ───────────────────────────────────────────────────

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        binding.rvWidgets.layoutManager = LinearLayoutManager(this)
        binding.rvWidgets.itemAnimator = null
        adapter = WidgetPickerAdapter(
            onItemClick = ::onWidgetSelected
        )
        binding.rvWidgets.adapter = adapter
    }

    // ── Load widgets ───────────────────────────────────────────────

    private fun loadWidgets() {
        binding.tvEmpty.setText(R.string.widget_picker_loading)
        binding.tvEmpty.visibility = View.VISIBLE
        binding.rvWidgets.visibility = View.INVISIBLE

        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                val providers = try {
                    appWidgetManager.getInstalledProviders()
                } catch (e: RuntimeException) {
                    Log.e(TAG, "getInstalledProviders() failed", e)
                    emptyList()
                }
                providers.map { info ->
                    WidgetPickerAdapter.Item(
                        provider = info.provider,
                        label = info.loadLabel(packageManager).toString(),
                        icon = info.loadIcon(this@WidgetPickerActivity, resources.displayMetrics.densityDpi),
                        sizeLabel = formatWidgetSize(info.minWidth, info.minHeight)
                    )
                }.sortedBy { it.label.lowercase() }
            }

            binding.tvEmpty.setText(R.string.widget_picker_empty)
            binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            binding.rvWidgets.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
            adapter.submitList(items)
        }
    }

    private fun formatWidgetSize(minWidth: Int, minHeight: Int): String {
        // Android cell size is roughly 70dp
        val cols = (minWidth / 70f).toInt().coerceAtLeast(1)
        val rows = (minHeight / 70f).toInt().coerceAtLeast(1)
        return "$cols x $rows"
    }

    // ── Selection flow ─────────────────────────────────────────────

    private fun onWidgetSelected(provider: ComponentName) {
        // Allocate a fresh ID for this widget
        if (allocatedWidgetId == INVALID_WIDGET_ID) {
            allocatedWidgetId = AppWidgetHostManager.getInstance(this).allocateAppWidgetId()
        }
        pendingProvider = provider

        val bound = AppWidgetHostManager.getInstance(this)
            .bindAppWidgetIdIfAllowed(allocatedWidgetId, provider)

        if (!bound) {
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, allocatedWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
            }
            try {
                @Suppress("DEPRECATION")
                startActivityForResult(bindIntent, REQUEST_BIND_WIDGET)
                return
            } catch (e: Exception) {
                Log.e(TAG, "Unable to start bind activity", e)
                Toast.makeText(this, R.string.error_app_not_found, Toast.LENGTH_SHORT).show()
                releaseAndFinish()
                return
            }
        }

        proceedToConfigurationOrSave()
    }

    private fun proceedToConfigurationOrSave() {
        val widgetId = allocatedWidgetId
        val provider = pendingProvider
        if (widgetId == INVALID_WIDGET_ID || provider == null) {
            releaseAndFinish()
            return
        }

        val info = appWidgetManager.getAppWidgetInfo(widgetId)
        if (info?.configure != null) {
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
                // Fall through and save anyway
            }
        }

        saveWidgetAndFinish(widgetId)
    }

    // ── Activity results ───────────────────────────────────────────

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_BIND_WIDGET -> handleBindResult(resultCode)
            REQUEST_CONFIGURE_WIDGET -> handleConfigureResult(resultCode)
        }
    }

    private fun handleBindResult(resultCode: Int) {
        val widgetId = allocatedWidgetId
        val provider = pendingProvider

        if (resultCode != RESULT_OK || widgetId == INVALID_WIDGET_ID || provider == null) {
            if (widgetId != INVALID_WIDGET_ID) {
                AppWidgetHostManager.getInstance(this).deleteAppWidgetId(widgetId)
            }
            releaseAndFinish()
            return
        }

        // Verify the bind actually took effect
        val verified = AppWidgetHostManager.getInstance(this)
            .bindAppWidgetIdIfAllowed(widgetId, provider)
        if (!verified) {
            Log.w(TAG, "Bind verification failed")
            AppWidgetHostManager.getInstance(this).deleteAppWidgetId(widgetId)
            releaseAndFinish()
            return
        }

        proceedToConfigurationOrSave()
    }

    private fun handleConfigureResult(resultCode: Int) {
        val widgetId = allocatedWidgetId
        if (resultCode == RESULT_OK && widgetId != INVALID_WIDGET_ID) {
            saveWidgetAndFinish(widgetId)
        } else {
            if (widgetId != INVALID_WIDGET_ID) {
                AppWidgetHostManager.getInstance(this).deleteAppWidgetId(widgetId)
            }
            releaseAndFinish()
        }
    }

    // ── Persistence ────────────────────────────────────────────────

    private fun saveWidgetAndFinish(widgetId: Int) {
        // Releases the widget being replaced, if any, so its ID does not leak.
        AppWidgetHostManager.getInstance(this).assignHomeWidget(this, widgetId)
        setResult(RESULT_OK)
        finish()
    }

    private fun releaseAndFinish() {
        if (allocatedWidgetId != INVALID_WIDGET_ID) {
            AppWidgetHostManager.getInstance(this).deleteAppWidgetId(allocatedWidgetId)
            allocatedWidgetId = INVALID_WIDGET_ID
        }
        pendingProvider = null
        setResult(RESULT_CANCELED)
        finish()
    }

    // ── Companion ──────────────────────────────────────────────────

    companion object {
        private const val TAG = "WidgetPickerActivity"
        private const val REQUEST_BIND_WIDGET = 2001
        private const val REQUEST_CONFIGURE_WIDGET = 2002
        private const val INVALID_WIDGET_ID = -1

        private const val KEY_ALLOCATED_WIDGET_ID = "allocated_widget_id"
        private const val KEY_PENDING_PROVIDER = "pending_provider"

        /**
         * true while a picker is alive, i.e. an allocated ID may not be saved yet. The
         * orphan clean-up in [AppWidgetHostManager.releaseOrphanedIds] must leave it alone.
         */
        @Volatile
        var isSelectionInProgress = false
            private set
    }
}
