package app.focusphone.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.focusphone.R
import app.focusphone.data.preferences.WidgetPreferences
import app.focusphone.databinding.ActivityWidgetPickerBinding
import app.focusphone.ui.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * FocusPhone's own widget picker.
 *
 * Lists every widget that can live on a home screen, grouped by app, with a faithful
 * preview, its grid size and description, and a search field. Selecting one:
 * 1. allocates a widget ID and binds it, asking the user through
 *    [AppWidgetManager.ACTION_APPWIDGET_BIND] when the system requires it;
 * 2. opens the provider's configuration screen when it has a mandatory one;
 * 3. saves the widget as the home widget and finishes with [RESULT_OK].
 *
 * Backing out of the permission or configuration screen releases the ID and returns to the
 * list, so the user can pick something else instead of being thrown out of the picker.
 *
 * Using our own picker eliminates the unreliability of the system [AppWidgetManager.ACTION_APPWIDGET_PICK]
 * on heavily skinned Android devices.
 */
class WidgetPickerActivity : BaseActivity() {

    private lateinit var binding: ActivityWidgetPickerBinding
    private lateinit var adapter: WidgetPickerAdapter

    private val hostManager by lazy { AppWidgetHostManager.getInstance(this) }

    private var allocatedWidgetId: Int = INVALID_WIDGET_ID

    private var entries: List<WidgetEntry> = emptyList()
    private var appIcons: Map<String, Drawable?> = emptyMap()
    private var catalogLoaded = false

    private val bindLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> handleBindResult(result.resultCode) }

    // ── Lifecycle ──────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isSelectionInProgress = true
        binding = ActivityWidgetPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        allocatedWidgetId = savedInstanceState?.getInt(KEY_ALLOCATED_WIDGET_ID, INVALID_WIDGET_ID) ?: INVALID_WIDGET_ID

        setupInsets()
        setupHeader()
        setupRecyclerView()
        binding.btnClose.setOnClickListener { finishCancelled() }
        loadWidgets()
    }

    override fun onDestroy() {
        // Leaving with the back gesture mid-selection must not leak the allocated ID.
        if (isFinishing) releasePendingId()
        isSelectionInProgress = false
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_ALLOCATED_WIDGET_ID, allocatedWidgetId)
    }

    // ── UI setup ───────────────────────────────────────────────────

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    private fun setupHeader() {
        styleSearchField(binding.etSearch)
        binding.etSearch.doAfterTextChanged { submitRows() }
        binding.progress.setIndicatorColor(palette.accent)
    }

    private fun setupRecyclerView() {
        binding.rvWidgets.layoutManager = LinearLayoutManager(this)
        binding.rvWidgets.itemAnimator = null
        adapter = WidgetPickerAdapter(
            scope = lifecycleScope,
            palette = palette,
            homeWidthPx = homeWidthPx(),
            onWidgetClick = ::onWidgetSelected
        )
        binding.rvWidgets.adapter = adapter
    }

    private fun homeWidthPx(): Int =
        (WidgetHeights.slotWidthDp(this) * resources.displayMetrics.density).roundToInt()

    // ── Catalog ────────────────────────────────────────────────────

    private fun loadWidgets() {
        showState(R.string.widget_picker_loading, loading = true)
        lifecycleScope.launch {
            val currentProvider = hostManager.homeWidgetInfo(this@WidgetPickerActivity)?.provider
            val (loadedEntries, icons) = withContext(Dispatchers.IO) { buildCatalog(currentProvider) }
            entries = loadedEntries
            appIcons = icons
            catalogLoaded = true
            submitRows()
            if (binding.rvWidgets.isVisible) {
                binding.rvWidgets.alpha = 0f
                binding.rvWidgets.animate()
                    .alpha(1f)
                    .setDuration(resources.getInteger(R.integer.fp_screen_enter_duration).toLong())
                    .start()
            }
        }
    }

    private fun buildCatalog(currentProvider: ComponentName?): Pair<List<WidgetEntry>, Map<String, Drawable?>> {
        val pm = packageManager
        val density = resources.displayMetrics.density
        val appLabels = mutableMapOf<String, String>()
        val icons = mutableMapOf<String, Drawable?>()

        val catalog = hostManager.getHomeScreenProviders().mapNotNull { info ->
            try {
                val packageName = info.provider.packageName
                val appLabel = appLabels.getOrPut(packageName) {
                    loadAppLabelAndIcon(pm, packageName, icons)
                }
                val (spanX, spanY) = WidgetSizing.span(
                    minWidthDp = info.minWidth / density,
                    minHeightDp = info.minHeight / density,
                    targetCellWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) info.targetCellWidth else 0,
                    targetCellHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) info.targetCellHeight else 0
                )
                WidgetEntry(
                    info = info,
                    provider = info.provider,
                    label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { appLabel },
                    description = loadDescription(info),
                    appLabel = appLabel,
                    spanX = spanX,
                    spanY = spanY,
                    inUse = info.provider == currentProvider
                )
            } catch (e: RuntimeException) {
                // One broken provider must not empty the whole picker.
                Log.w(TAG, "Skipping widget provider ${info.provider}", e)
                null
            }
        }
        return catalog to icons
    }

    private fun loadAppLabelAndIcon(pm: PackageManager, packageName: String, icons: MutableMap<String, Drawable?>): String =
        try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            icons[packageName] = pm.getApplicationIcon(appInfo)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            icons[packageName] = null
            packageName
        }

    private fun loadDescription(info: AppWidgetProviderInfo): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return info.loadDescription(this)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun submitRows() {
        if (!catalogLoaded) return
        val query = binding.etSearch.text?.toString().orEmpty()
        val rows = WidgetCatalog.rows(entries, query, appIcons, pinnedPackage = packageName)
        adapter.submitList(rows)
        when {
            entries.isEmpty() -> showState(R.string.widget_picker_empty)

            rows.isEmpty() -> showState(R.string.widget_picker_no_results)

            else -> {
                binding.stateContainer.isVisible = false
                binding.rvWidgets.isVisible = true
            }
        }
    }

    private fun showState(@StringRes messageRes: Int, loading: Boolean = false) {
        binding.tvState.setText(messageRes)
        binding.progress.isVisible = loading
        binding.stateContainer.isVisible = true
        binding.rvWidgets.isVisible = false
    }

    // ── Selection flow ─────────────────────────────────────────────

    private fun onWidgetSelected(entry: WidgetEntry) {
        if (allocatedWidgetId != INVALID_WIDGET_ID) return
        setSelectionBusy(true)
        val widgetId = try {
            hostManager.allocateAppWidgetId()
        } catch (e: RuntimeException) {
            Log.e(TAG, "Could not allocate a widget id", e)
            abortSelection(R.string.widget_picker_add_failed)
            return
        }
        allocatedWidgetId = widgetId

        if (hostManager.bindAppWidgetIdIfAllowed(widgetId, entry.provider)) {
            proceedAfterBind()
            return
        }

        val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, entry.provider)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, entry.info.profile)
        }
        try {
            bindLauncher.launch(bindIntent)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Unable to start bind activity", e)
            abortSelection(R.string.widget_picker_add_failed)
        }
    }

    private fun handleBindResult(resultCode: Int) {
        val widgetId = allocatedWidgetId
        if (widgetId == INVALID_WIDGET_ID) return
        // The bind is checked against the system instead of re-binding: when the user allows
        // it "only this time", a second bindAppWidgetIdIfAllowed() returns false even though
        // the widget is bound, and the widget used to be thrown away.
        if (hostManager.isBound(widgetId)) {
            proceedAfterBind()
        } else {
            abortSelection(if (resultCode == RESULT_OK) R.string.widget_picker_add_failed else null)
        }
    }

    private fun proceedAfterBind() {
        val widgetId = allocatedWidgetId
        val info = hostManager.getAppWidgetInfo(widgetId)
        if (info == null) {
            abortSelection(R.string.widget_picker_add_failed)
            return
        }
        if (!needsConfiguration(info)) {
            saveWidgetAndFinish(widgetId)
            return
        }
        if (!hostManager.startConfigureActivity(this, widgetId, REQUEST_CONFIGURE)) {
            abortSelection(R.string.widget_picker_config_failed)
        }
    }

    /**
     * A configuration screen is shown unless the provider marks it optional (Android 12+),
     * in which case it stays reachable later from the widget's options.
     */
    private fun needsConfiguration(info: AppWidgetProviderInfo): Boolean {
        if (info.configure == null) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val optional = AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL or
            AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE
        return info.widgetFeatures and optional != optional
    }

    // The configuration screen can only be started through AppWidgetHost, which reports
    // back here; the Activity Result API has no contract for it.
    @Deprecated("Required by AppWidgetHost.startAppWidgetConfigureActivityForResult")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CONFIGURE) return
        val widgetId = allocatedWidgetId
        if (widgetId == INVALID_WIDGET_ID) return
        if (resultCode == RESULT_OK) saveWidgetAndFinish(widgetId) else abortSelection(null)
    }

    // ── Outcome ────────────────────────────────────────────────────

    private fun saveWidgetAndFinish(widgetId: Int) {
        // Releases the widget being replaced, if any, so its ID does not leak.
        hostManager.assignHomeWidget(this, widgetId)
        allocatedWidgetId = INVALID_WIDGET_ID
        setResult(RESULT_OK)
        finish()
    }

    /** Releases the pending ID and returns to the list, optionally explaining why. */
    private fun abortSelection(@StringRes messageRes: Int?) {
        releasePendingId()
        setSelectionBusy(false)
        messageRes?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
    }

    private fun finishCancelled() {
        releasePendingId()
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun releasePendingId() {
        val widgetId = allocatedWidgetId
        // Never release the widget the home screen is showing.
        if (widgetId != INVALID_WIDGET_ID && widgetId != WidgetPreferences.getWidgetId(this)) {
            hostManager.deleteAppWidgetId(widgetId)
        }
        allocatedWidgetId = INVALID_WIDGET_ID
    }

    private fun setSelectionBusy(busy: Boolean) {
        adapter.isInteractionEnabled = !busy
        binding.rvWidgets.animate()
            .alpha(if (busy) BUSY_ALPHA else 1f)
            .setDuration(resources.getInteger(R.integer.fp_press_out_duration).toLong())
            .start()
    }

    // ── Companion ──────────────────────────────────────────────────

    companion object {
        private const val TAG = "WidgetPickerActivity"
        private const val INVALID_WIDGET_ID = WidgetPreferences.INVALID_WIDGET_ID
        private const val REQUEST_CONFIGURE = 0x5717
        private const val BUSY_ALPHA = 0.5f

        private const val KEY_ALLOCATED_WIDGET_ID = "allocated_widget_id"

        /**
         * true while a picker is alive, i.e. an allocated ID may not be saved yet. The
         * orphan clean-up in [AppWidgetHostManager.releaseOrphanedIds] must leave it alone.
         */
        @Volatile
        var isSelectionInProgress = false
            private set
    }
}
