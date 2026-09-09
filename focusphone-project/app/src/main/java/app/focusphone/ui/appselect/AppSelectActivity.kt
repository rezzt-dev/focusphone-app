package app.focusphone.ui.appselect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import app.focusphone.FocusPhoneApp
import app.focusphone.R
import app.focusphone.databinding.ActivityAppSelectBinding
import app.focusphone.ui.appselect.adapter.InstalledAppsAdapter
import app.focusphone.ui.base.BaseActivity
import app.focusphone.domain.model.AppInfo
import app.focusphone.domain.util.normalizeAppName
import android.view.ViewGroup
import kotlinx.coroutines.launch
import androidx.core.view.updatePadding

class AppSelectActivity : BaseActivity() {

    private lateinit var binding: ActivityAppSelectBinding
    private var activeDialog: androidx.appcompat.app.AlertDialog? = null
    private var searchTextWatcher: android.text.TextWatcher? = null

    private val viewModel: AppSelectViewModel by viewModels {
        AppSelectViewModel.factory(focusPhoneApp)
    }

    private val adapter = InstalledAppsAdapter(
        onToggle = { app -> viewModel.onToggle(app) },
        onLongToggle = { app -> showAppOptionsDialog(app) }
    )

    private fun showAppOptionsDialog(app: AppInfo) {
        val options = arrayOf(
            getString(R.string.allapps_dialog_rename),
            getString(R.string.allapps_dialog_uninstall)
        )

        activeDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(app)
                    1 -> {
                        focusPhoneApp.container.appRepository.invalidateCache()

                        val uri = Uri.fromParts("package", app.packageName, null)
                        Toast.makeText(this@AppSelectActivity, R.string.uninstall_progress, Toast.LENGTH_SHORT).show()
                        try {
                            startActivity(
                                Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                                    data = uri
                                }
                            )
                        } catch (e: Exception) {
                            Log.w("AppSelectActivity", "ACTION_UNINSTALL_PACKAGE failed for ${app.packageName}", e)
                            try {
                                startActivity(
                                    Intent(Intent.ACTION_DELETE).apply {
                                        data = uri
                                    }
                                )
                            } catch (e2: Exception) {
                                Log.e("AppSelectActivity", "ACTION_DELETE also failed for ${app.packageName}", e2)
                                Toast.makeText(this@AppSelectActivity, R.string.error_uninstall_failed, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
            .create()
        activeDialog?.show()
        styleMaterialDialog(activeDialog)
    }

    private fun showRenameDialog(app: AppInfo) {
        val input = android.widget.EditText(this@AppSelectActivity).apply {
            setText(app.label)
            setSelection(text.length)
            setSingleLine()
            filters = arrayOf(android.text.InputFilter.LengthFilter(50))

            val colors = palette
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(colors.surface)
                setStroke(dp(1), colors.divider)
            }

            setTextColor(colors.textPrimary)
            setHintTextColor(colors.textSecondary)

            textSize = 15f
            val padH = dp(16)
            val padV = dp(14)
            setPadding(padH, padV, padH, padV)
        }
        val container = android.widget.FrameLayout(this@AppSelectActivity).apply {
            addView(input, android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dp(24)
                rightMargin = dp(24)
                topMargin = dp(8)
                bottomMargin = dp(8)
            })
        }

        activeDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this@AppSelectActivity)
            .setTitle(R.string.customize_name_title)
            .setMessage(getString(R.string.customize_name_message, app.label))
            .setView(container)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val newLabel = input.text.toString().trim().normalizeAppName()
                val prefs = focusPhoneApp.container.customAppLabelsPreferences
                if (newLabel.isEmpty()) {
                    prefs.setCustomLabel(app.packageName, null)
                } else {
                    prefs.setCustomLabel(app.packageName, newLabel)
                }
                viewModel.loadApps()
            }
            .setNeutralButton(R.string.action_reset) { _, _ ->
                val prefs = focusPhoneApp.container.customAppLabelsPreferences
                prefs.setCustomLabel(app.packageName, null)
                viewModel.loadApps()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
        activeDialog?.show()
        styleMaterialDialog(activeDialog)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        setupRecyclerView()
        setupSearch()
        binding.btnDone.setOnClickListener { finish() }
        observeState()
        observeEvents()
    }

    override fun onResume() {
        super.onResume()
        focusPhoneApp.container.appRepository.invalidateCache()
        viewModel.loadApps()
    }

    override fun onDestroy() {
        activeDialog?.dismiss()
        searchTextWatcher?.let { binding.etSearch.removeTextChangedListener(it) }
        super.onDestroy()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Preserve the horizontal and vertical padding declared in the layout.
            binding.header.updatePadding(top = bars.top + dp(16))
            binding.btnDone.updatePadding(bottom = bars.bottom)
            binding.rvApps.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = adapter
    }

    private fun setupSearch() {
        searchTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onSearchQuery(s?.toString() ?: "")
            }
        }
        binding.etSearch.addTextChangedListener(searchTextWatcher!!)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredApps.collect { apps ->
                    adapter.submitList(apps)
                    binding.tvSelectedCount.text = getString(
                        R.string.appselect_selected_count,
                        viewModel.selectedCount(),
                        viewModel.maxApps()
                    )
                    val empty = apps.isEmpty() && !viewModel.isLoading.value
                    binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
                    binding.rvApps.visibility = if (empty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is AppSelectEvent.LimitReached ->
                            Toast.makeText(
                                this@AppSelectActivity,
                                getString(R.string.appselect_limit_reached, event.limit),
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                }
            }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
