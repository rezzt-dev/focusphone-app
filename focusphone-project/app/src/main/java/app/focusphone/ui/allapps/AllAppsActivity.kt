package app.focusphone.ui.allapps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.FocusPhoneApp
import app.focusphone.R
import app.focusphone.data.preferences.ConsciousPausePreferences
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.ui.pause.ConsciousPauseActivity
import app.focusphone.ui.detox.DetoxBlockedActivity
import app.focusphone.databinding.ActivityAllAppsBinding
import app.focusphone.databinding.ItemAllAppBinding
import app.focusphone.domain.model.AppInfo
import app.focusphone.domain.usecase.GetInstalledAppsUseCase
import app.focusphone.domain.util.normalizeAppName
import app.focusphone.ui.base.BaseActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── ViewModel ──────────────────────────────────────────────────────

class AllAppsViewModel(
  private val getInstalledApps: GetInstalledAppsUseCase
) : ViewModel() {

  private val _allApps  = MutableStateFlow<List<AppInfo>>(emptyList())
  private val _filtered = MutableStateFlow<List<AppInfo>>(emptyList())
  private val _query    = MutableStateFlow("")

  val filteredApps: StateFlow<List<AppInfo>> = _filtered.asStateFlow()
  val isLoading = MutableStateFlow(true)

  private var searchJob: Job? = null

  init { load() }

  fun load() {
    viewModelScope.launch {
      isLoading.value = true
      _allApps.value  = try { getInstalledApps() } catch (_: Exception) { emptyList() }
      applyFilter()
      isLoading.value = false
    }
  }

  fun onQuery(q: String) {
    _query.value = q
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      delay(150)
      applyFilter()
    }
  }

  private fun applyFilter() {
    val q = _query.value.trim().lowercase()
    _filtered.value = if (q.isEmpty()) _allApps.value
    else _allApps.value.filter { it.label.contains(q) }
  }

  companion object {
    fun factory(app: FocusPhoneApp): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          AllAppsViewModel(app.container.getInstalledAppsUseCase) as T
      }
  }
}

// ── Activity ───────────────────────────────────────────────────────

class AllAppsActivity : BaseActivity() {

  private lateinit var binding: ActivityAllAppsBinding
  private val viewModel: AllAppsViewModel by viewModels { AllAppsViewModel.factory(application as FocusPhoneApp) }
  private var activeDialog: androidx.appcompat.app.AlertDialog? = null
  private var searchTextWatcher: TextWatcher? = null
  private var pendingRefresh = false

  private val adapter = AllAppsAdapter(
    onTap = { app ->
      binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
      if (ProfilePreferences.isAppBlocked(this, app.packageName)) {
        startActivity(Intent(this, DetoxBlockedActivity::class.java))
        return@AllAppsAdapter
      }
      val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName) ?: return@AllAppsAdapter
      if (ConsciousPausePreferences.isEnabled(this)) {
        val intent = Intent(this, ConsciousPauseActivity::class.java).apply {
          putExtra("target_intent", launchIntent)
          putExtra("app_label", app.label)
        }
        startActivity(intent)
      } else {
        try { startActivity(launchIntent) }
        catch (_: Exception) { Toast.makeText(this, R.string.error_app_not_found, Toast.LENGTH_SHORT).show() }
      }
    },
    onLongTap = { app ->
      binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
      showAppOptionsDialog(app)
    }
  )

  private fun showRenameDialog(app: AppInfo) {
    val input = android.widget.EditText(this@AllAppsActivity).apply {
      setText(app.label)
      setSelection(text.length)
      setSingleLine()

      val gd = android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        cornerRadius = dp(16).toFloat()

        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        setColor(typedValue.data)

        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
        setStroke(dp(1), typedValue.data)
      }
      background = gd

      val typedValue = android.util.TypedValue()
      theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
      setTextColor(typedValue.data)
      theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
      setHintTextColor(typedValue.data)

      textSize = 15f
      val padH = dp(16)
      val padV = dp(14)
      setPadding(padH, padV, padH, padV)
    }
    val container = android.widget.FrameLayout(this@AllAppsActivity).apply {
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

    activeDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this@AllAppsActivity)
      .setTitle(R.string.customize_name_title)
      .setMessage(getString(R.string.customize_name_message, app.label))
      .setView(container)
      .setPositiveButton(R.string.action_save) { _, _ ->
        val newLabel = input.text.toString().trim().normalizeAppName()
        val prefs = (application as FocusPhoneApp).container.customAppLabelsPreferences
        if (newLabel.isEmpty()) {
          prefs.setCustomLabel(app.packageName, null)
        } else {
          prefs.setCustomLabel(app.packageName, newLabel)
        }
        viewModel.load()
      }
      .setNeutralButton(R.string.action_reset) { _, _ ->
        val prefs = (application as FocusPhoneApp).container.customAppLabelsPreferences
        prefs.setCustomLabel(app.packageName, null)
        viewModel.load()
      }
      .setNegativeButton(R.string.action_cancel, null)
      .create()
    activeDialog?.show()
  }

  private fun showAppOptionsDialog(app: AppInfo) {
    val isBlocked = ProfilePreferences.getBlockedApps(this).contains(app.packageName)
    val blockActionText = if (isBlocked) {
      getString(R.string.allapps_dialog_unblock)
    } else {
      getString(R.string.allapps_dialog_block)
    }
    val options = arrayOf(
      getString(R.string.allapps_dialog_rename),
      blockActionText,
      getString(R.string.allapps_dialog_uninstall)
    )

    activeDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
      .setTitle(app.label)
      .setItems(options) { _, which ->
        when (which) {
          0 -> showRenameDialog(app)
          1 -> {
            ProfilePreferences.setAppBlockedState(this, app.packageName, !isBlocked)
            val toastFormat = if (isBlocked) {
              R.string.allapps_toast_unblocked
            } else {
              R.string.allapps_toast_blocked
            }
            Toast.makeText(
              this,
              getString(toastFormat, app.label),
              Toast.LENGTH_SHORT
            ).show()
          }
          2 -> {
            Log.d("FocusPhone", "AllApps uninstall clicked for ${app.packageName}")
            (application as FocusPhoneApp).container.appRepository.invalidateCache()
            pendingRefresh = true

            val uri = Uri.fromParts("package", app.packageName, null)
            Toast.makeText(this@AllAppsActivity, "uninstalling ${app.label}...", Toast.LENGTH_SHORT).show()
            try {
              startActivity(
                Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                  data = uri
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
              )
            } catch (_: Exception) {
              try {
                startActivity(
                  Intent(Intent.ACTION_DELETE).apply {
                    data = uri
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  }
                )
              } catch (e: Exception) {
                Toast.makeText(this@AllAppsActivity, "could not open uninstall dialog: ${e.message}", Toast.LENGTH_LONG).show()
              }
            }
          }
        }
      }
      .create()
    activeDialog?.show()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityAllAppsBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setupInsets()
    binding.rvApps.layoutManager = LinearLayoutManager(this)
    binding.rvApps.adapter = adapter
    binding.btnClose.setOnClickListener { finish() }
    searchTextWatcher = object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
      override fun afterTextChanged(s: Editable?) = Unit
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        viewModel.onQuery(s?.toString() ?: "")
      }
    }
    binding.etSearch.addTextChangedListener(searchTextWatcher!!)
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.filteredApps.collect {
            adapter.submitList(it)
            val empty = it.isEmpty() && !viewModel.isLoading.value
            binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
            binding.rvApps.visibility = if (empty) View.GONE else View.VISIBLE
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (pendingRefresh) {
      pendingRefresh = false
      viewModel.load()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    activeDialog?.dismiss()
    searchTextWatcher?.let { binding.etSearch.removeTextChangedListener(it) }
  }

  private fun setupInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
      val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      binding.header.setPadding(0, bars.top + dp(12), 0, 0)
      binding.btnClose.setPadding(0, 0, 0, bars.bottom)
      binding.rvApps.setPadding(0, 0, 0, bars.bottom)
      insets
    }
  }

  private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

// ── Adapter ────────────────────────────────────────────────────────

class AllAppsAdapter(
  private val onTap: (AppInfo) -> Unit,
  private val onLongTap: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AllAppsAdapter.VH>(DIFF) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(ItemAllAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
  inner class VH(private val b: ItemAllAppBinding) : RecyclerView.ViewHolder(b.root) {
    fun bind(app: AppInfo) {
      val context = b.root.context
      b.tvName.text = app.label

      val activeTheme = ThemePreferences.getTheme(context)
      if (activeTheme == ThemePreferences.THEME_CUSTOM) {
        val fgColor = ThemePreferences.getCustomForegroundColor(context)
        b.tvName.setTextColor(fgColor)
      } else {
        val typedValue = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        b.tvName.setTextColor(typedValue.data)
      }

      b.root.setOnClickListener { onTap(app) }
      b.root.setOnLongClickListener { onLongTap(app); true }
    }
  }
  companion object {
    private val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
      override fun areItemsTheSame(o: AppInfo, n: AppInfo) = o.packageName == n.packageName
      override fun areContentsTheSame(o: AppInfo, n: AppInfo) = o == n
    }
  }
}
