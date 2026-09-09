package app.focusphone.ui.allapps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.EditText
import android.view.View
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import app.focusphone.BuildConfig
import app.focusphone.FocusPhoneApp
import app.focusphone.R
import app.focusphone.data.preferences.AppFolderPreferences
import app.focusphone.data.preferences.ConsciousPausePreferences
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.ui.pause.ConsciousPauseActivity
import app.focusphone.ui.detox.DetoxBlockedActivity
import app.focusphone.databinding.ActivityAllAppsBinding
import app.focusphone.domain.model.AppFolder
import app.focusphone.domain.model.AppInfo
import app.focusphone.domain.usecase.GetInstalledAppsUseCase
import app.focusphone.domain.util.AppFolderSerializer
import app.focusphone.domain.util.FuzzyMatcher
import app.focusphone.domain.util.normalizeAppName
import app.focusphone.ui.allapps.adapter.AllAppsAdapter
import app.focusphone.ui.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.view.updatePadding

// ── ViewModel ──────────────────────────────────────────────────────

class AllAppsViewModel(
  private val getInstalledApps: GetInstalledAppsUseCase,
  private val folderPreferences: AppFolderPreferences
) : ViewModel() {

  private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
  private val _query   = MutableStateFlow("")

  private val _isLoading = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  /**
   * The rendered drawer: folder headers (alphabetical) with their apps when expanded,
   * followed by every app that belongs to no folder.
   */
  val rows: StateFlow<List<AllAppsRow>> = combine(
    _allApps,
    _query,
    folderPreferences.foldersFlow,
    folderPreferences.expandedFlow
  ) { apps, query, folders, expanded ->
    buildRows(apps, query, folders, expanded)
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private var searchJob: Job? = null

  init { load() }

  fun load() {
    viewModelScope.launch {
      _isLoading.value = true
      val apps = try { getInstalledApps() } catch (e: Exception) {
        Log.e("AllAppsViewModel", "Failed to load installed apps", e)
        emptyList()
      }
      _allApps.value = apps
      // Uninstalled apps must not linger inside folders as invisible members.
      folderPreferences.retainInstalled(apps.mapTo(mutableSetOf()) { it.packageName })
      _isLoading.value = false
    }
  }

  fun onQuery(q: String) {
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      delay(SEARCH_DEBOUNCE_MS)
      _query.value = q
    }
  }

  private fun buildRows(
    apps: List<AppInfo>,
    query: String,
    folders: List<AppFolder>,
    expanded: Set<String>
  ): List<AllAppsRow> {
    val folderByPackage = mutableMapOf<String, String>()
    for (folder in folders) {
      for (packageName in folder.packageNames) folderByPackage[packageName] = folder.id
    }

    val trimmed = query.trim()
    if (trimmed.isNotEmpty()) {
      // While searching, folders step aside and every match is listed flat.
      return apps
        .filter { FuzzyMatcher.matches(trimmed, it.label) }
        .map { AllAppsRow.AppRow(it, folderByPackage[it.packageName]) }
    }

    val appsByPackage = apps.associateBy { it.packageName }
    val rows = mutableListOf<AllAppsRow>()

    for (folder in folders.sortedWith(AppFolderSerializer.NAME_ORDER)) {
      val contents = folder.packageNames
        .mapNotNull { appsByPackage[it] }
        .sortedBy { it.label.lowercase() }

      val isExpanded = folder.id in expanded
      rows += AllAppsRow.FolderRow(folder, contents.size, isExpanded)
      if (isExpanded) {
        rows += contents.map { AllAppsRow.AppRow(it, folder.id) }
      }
    }

    rows += apps
      .filterNot { it.packageName in folderByPackage }
      .map { AllAppsRow.AppRow(it, null) }

    return rows
  }

  companion object {
    private const val SEARCH_DEBOUNCE_MS = 150L

    fun factory(app: FocusPhoneApp): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          AllAppsViewModel(
            app.container.getInstalledAppsUseCase,
            app.container.appFolderPreferences
          ) as T
      }
  }
}

// ── Activity ───────────────────────────────────────────────────────

class AllAppsActivity : BaseActivity() {

  private lateinit var binding: ActivityAllAppsBinding
  private val viewModel: AllAppsViewModel by viewModels { AllAppsViewModel.factory(focusPhoneApp) }
  private var activeDialog: androidx.appcompat.app.AlertDialog? = null
  private var searchTextWatcher: TextWatcher? = null
  private var pendingRefresh = false

  private val folderPreferences: AppFolderPreferences
    get() = focusPhoneApp.container.appFolderPreferences

  private val adapter = AllAppsAdapter(
    onAppTap = { app -> launchApp(app) },
    onFolderTap = { folder ->
      binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
      folderPreferences.toggleExpanded(folder.id)
    },
    onFolderLongTap = { folder ->
      binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
      showFolderOptionsDialog(folder)
    }
  )

  // ── App launching ────────────────────────────────────────────────

  private fun launchApp(app: AppInfo) {
    binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    if (ProfilePreferences.isAppBlocked(this, app.packageName)) {
      startActivity(Intent(this, DetoxBlockedActivity::class.java))
      return
    }
    if (enforceAppSchedule(app.packageName, app.label)) return
    val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName) ?: return
    if (ConsciousPausePreferences.isEnabled(this)) {
      startActivity(
        Intent(this, ConsciousPauseActivity::class.java).apply {
          putExtra("target_intent", launchIntent)
          putExtra("app_label", app.label)
        }
      )
    } else {
      try { startActivity(launchIntent) }
      catch (e: Exception) {
        Log.e("AllAppsActivity", "Failed to launch app ${app.packageName}", e)
        Toast.makeText(this, R.string.error_app_not_found, Toast.LENGTH_SHORT).show()
      }
    }
  }

  // ── Folder actions ───────────────────────────────────────────────

  /**
   * Applies a drop: onto a folder it moves the app there, onto the plain list it takes
   * the app out of the folder it came from.
   */
  private fun onAppDropped(app: AppInfo, fromFolderId: String?, toFolderId: String?) {
    when {
      toFolderId != null -> {
        val folder = folderPreferences.getFolder(toFolderId) ?: return
        folderPreferences.moveAppToFolder(app.packageName, toFolderId)
        toast(getString(R.string.allapps_toast_moved_to_folder, app.label, folder.name))
      }
      fromFolderId != null -> removeFromFolder(app, folderPreferences.getFolder(fromFolderId))
    }
  }

  private fun removeFromFolder(app: AppInfo, folder: AppFolder?) {
    folderPreferences.removeAppFromFolders(app.packageName)
    folder ?: return
    toast(getString(R.string.allapps_toast_removed_from_folder, app.label, folder.name))
  }

  /** Folder picker shown from the app long-press menu. */
  private fun showFolderChooserDialog(app: AppInfo) {
    val currentFolderId = folderPreferences.folderOf(app.packageName)?.id
    val folders = folderPreferences.getFolders().filter { it.id != currentFolderId }
    val labels = folders.map { it.name } + getString(R.string.allapps_folder_choose_new)

    activeDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
      .setTitle(getString(R.string.allapps_folder_choose_title, app.label))
      .setItems(labels.toTypedArray()) { _, which ->
        val folder = folders.getOrNull(which)
        if (folder == null) {
          showCreateFolderDialog(appToMove = app)
        } else {
          folderPreferences.moveAppToFolder(app.packageName, folder.id)
          toast(getString(R.string.allapps_toast_moved_to_folder, app.label, folder.name))
        }
      }
      .setNegativeButton(R.string.action_cancel, null)
      .create()
    showStyled()
  }

  /**
   * Creates a folder. When [appToMove] is set the app lands in the new folder, which is
   * what happens when "new folder…" is chosen from the folder picker.
   */
  private fun showCreateFolderDialog(appToMove: AppInfo? = null) {
    val input = styledInput(prefill = "", hint = getString(R.string.allapps_folder_name_hint))

    activeDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
      .setTitle(R.string.allapps_new_folder)
      .setView(inputContainer(input))
      .setPositiveButton(R.string.action_save) { _, _ ->
        val folder = folderPreferences.createFolder(input.text.toString())
        if (folder == null) {
          toast(getString(R.string.allapps_folder_name_taken))
          return@setPositiveButton
        }
        appToMove?.let { folderPreferences.moveAppToFolder(it.packageName, folder.id) }
        toast(getString(R.string.allapps_toast_folder_created, folder.name))
      }
      .setNegativeButton(R.string.action_cancel, null)
      .create()
    showStyled()
  }

  private fun showFolderOptionsDialog(folder: AppFolder) {
    val options = arrayOf(
      getString(R.string.allapps_folder_rename_title),
      getString(R.string.allapps_folder_delete_title)
    )

    activeDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
      .setTitle(folder.name)
      .setItems(options) { _, which ->
        when (which) {
          0 -> showRenameFolderDialog(folder)
          1 -> showDeleteFolderDialog(folder)
        }
      }
      .create()
    showStyled()
  }

  private fun showRenameFolderDialog(folder: AppFolder) {
    val input = styledInput(prefill = folder.name, hint = getString(R.string.allapps_folder_name_hint))

    activeDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
      .setTitle(R.string.allapps_folder_rename_title)
      .setView(inputContainer(input))
      .setPositiveButton(R.string.action_save) { _, _ ->
        if (!folderPreferences.renameFolder(folder.id, input.text.toString())) {
          toast(getString(R.string.allapps_folder_name_taken))
        }
      }
      .setNegativeButton(R.string.action_cancel, null)
      .create()
    showStyled()
  }

  private fun showDeleteFolderDialog(folder: AppFolder) {
    activeDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
      .setTitle(R.string.allapps_folder_delete_title)
      .setMessage(getString(R.string.allapps_folder_delete_message, folder.name))
      .setPositiveButton(R.string.allapps_folder_delete_confirm) { _, _ ->
        folderPreferences.deleteFolder(folder.id)
        toast(getString(R.string.allapps_toast_folder_deleted, folder.name))
      }
      .setNegativeButton(R.string.action_cancel, null)
      .create()
    showStyled()
  }

  // ── App options ──────────────────────────────────────────────────

  private fun showAppOptionsDialog(app: AppInfo) {
    lifecycleScope.launch {
      val shortcuts = withContext(Dispatchers.IO) {
        focusPhoneApp.container.getAppShortcutsUseCase(app.packageName)
      }
      showAppOptionsDialogWithShortcuts(app, shortcuts)
    }
  }

  private fun showAppOptionsDialogWithShortcuts(
    app: AppInfo,
    shortcuts: List<app.focusphone.domain.model.AppShortcut>
  ) {
    val isBlocked = ProfilePreferences.getBlockedApps(this).contains(app.packageName)
    val currentFolder = folderPreferences.folderOf(app.packageName)

    // Options are paired with their action so adding one never shifts an index.
    val actions = mutableListOf<Pair<String, () -> Unit>>()

    actions += getString(R.string.allapps_dialog_rename) to { showRenameDialog(app) }
    actions += getString(R.string.allapps_dialog_move_to_folder) to { showFolderChooserDialog(app) }
    if (currentFolder != null) {
      actions += getString(R.string.allapps_dialog_remove_from_folder) to {
        removeFromFolder(app, currentFolder)
      }
    }
    actions += getString(
      if (isBlocked) R.string.allapps_dialog_unblock else R.string.allapps_dialog_block
    ) to { toggleBlocked(app, isBlocked) }
    actions += getString(R.string.allapps_dialog_uninstall) to { uninstall(app) }
    shortcuts.forEach { shortcut ->
      actions += shortcut.shortLabel to { launchShortcut(app, shortcut) }
    }

    activeDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
      .setTitle(app.label)
      .setItems(actions.map { it.first }.toTypedArray()) { _, which ->
        actions.getOrNull(which)?.second?.invoke()
      }
      .create()
    showStyled()
  }

  private fun toggleBlocked(app: AppInfo, isBlocked: Boolean) {
    ProfilePreferences.setAppBlockedState(this, app.packageName, !isBlocked)
    val toastFormat = if (isBlocked) {
      R.string.allapps_toast_unblocked
    } else {
      R.string.allapps_toast_blocked
    }
    toast(getString(toastFormat, app.label))
  }

  private fun uninstall(app: AppInfo) {
    if (BuildConfig.DEBUG) {
      Log.d("FocusPhone", "AllApps uninstall clicked for ${app.packageName}")
    }
    focusPhoneApp.container.appRepository.invalidateCache()
    pendingRefresh = true

    val uri = Uri.fromParts("package", app.packageName, null)
    toast(getString(R.string.uninstall_progress))
    try {
      startActivity(Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply { data = uri })
    } catch (e: Exception) {
      Log.w("AllAppsActivity", "ACTION_UNINSTALL_PACKAGE failed for ${app.packageName}", e)
      try {
        startActivity(Intent(Intent.ACTION_DELETE).apply { data = uri })
      } catch (e2: Exception) {
        Log.e("AllAppsActivity", "ACTION_DELETE also failed for ${app.packageName}", e2)
        Toast.makeText(this, R.string.error_uninstall_failed, Toast.LENGTH_LONG).show()
      }
    }
  }

  private fun launchShortcut(app: AppInfo, shortcut: app.focusphone.domain.model.AppShortcut) {
    // A shortcut opens the same app, so it obeys the same schedule.
    if (enforceAppSchedule(app.packageName, app.label)) return
    val launched = focusPhoneApp.container.getAppShortcutsUseCase.launchShortcut(shortcut)
    if (!launched) {
      Toast.makeText(this, R.string.error_app_not_found, Toast.LENGTH_SHORT).show()
    }
  }

  private fun showRenameDialog(app: AppInfo) {
    val input = styledInput(prefill = app.label, hint = null)

    activeDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
      .setTitle(R.string.customize_name_title)
      .setMessage(getString(R.string.customize_name_message, app.label))
      .setView(inputContainer(input))
      .setPositiveButton(R.string.action_save) { _, _ ->
        val newLabel = input.text.toString().trim().normalizeAppName()
        val prefs = focusPhoneApp.container.customAppLabelsPreferences
        prefs.setCustomLabel(app.packageName, newLabel.ifEmpty { null })
        viewModel.load()
      }
      .setNeutralButton(R.string.action_reset) { _, _ ->
        focusPhoneApp.container.customAppLabelsPreferences.setCustomLabel(app.packageName, null)
        viewModel.load()
      }
      .setNegativeButton(R.string.action_cancel, null)
      .create()
    showStyled()
  }

  // ── Dialog helpers ───────────────────────────────────────────────

  /** Single-line input styled like the rest of the app (rounded outline, theme colours). */
  private fun styledInput(prefill: String, hint: String?): EditText =
    EditText(this).apply {
      setText(prefill)
      setSelection(text.length)
      setSingleLine()
      hint?.let { this.hint = it }
      filters = arrayOf(android.text.InputFilter.LengthFilter(INPUT_MAX_LENGTH))

      val colors = palette
      background = android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        cornerRadius = dp(16).toFloat()
        // Opaque fill: a text field must stay solid even when a wallpaper shows behind
        // the dialog, and a hairline outline reads better than a full-strength border.
        setColor(colors.surface)
        setStroke(dp(1), colors.divider)
      }

      setTextColor(colors.textPrimary)
      setHintTextColor(colors.textSecondary)

      textSize = 15f
      setPadding(dp(16), dp(14), dp(16), dp(14))
    }

  private fun inputContainer(input: EditText): android.widget.FrameLayout =
    android.widget.FrameLayout(this).apply {
      addView(
        input,
        android.widget.FrameLayout.LayoutParams(
          android.view.ViewGroup.LayoutParams.MATCH_PARENT,
          android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
          leftMargin = dp(24)
          rightMargin = dp(24)
          topMargin = dp(8)
          bottomMargin = dp(8)
        }
      )
    }

  private fun showStyled() {
    activeDialog?.show()
    styleMaterialDialog(activeDialog)
  }

  private fun toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

  // ── Lifecycle ────────────────────────────────────────────────────

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityAllAppsBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setupInsets()

    binding.rvApps.layoutManager = LinearLayoutManager(this)
    binding.rvApps.adapter = adapter
    ItemTouchHelper(
      FolderDragCallback(
        // Read from the adapter, not the flow: the displayed list is what the
        // drag coordinates refer to while a diff is still being applied.
        rowAt = { position -> adapter.currentList.getOrNull(position) },
        onLongPress = { app -> showAppOptionsDialog(app) },
        onDrop = { app, from, to -> onAppDropped(app, from, to) }
      )
    ).attachToRecyclerView(binding.rvApps)

    binding.btnClose.setOnClickListener { finish() }
    binding.btnNewFolder.setOnClickListener { showCreateFolderDialog() }

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
        viewModel.rows.collect { rows ->
          adapter.submitList(rows)
          val empty = rows.isEmpty() && !viewModel.isLoading.value
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
    activeDialog?.dismiss()
    searchTextWatcher?.let { binding.etSearch.removeTextChangedListener(it) }
    super.onDestroy()
  }

  private fun setupInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
      val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      // updatePadding, not setPadding: these views declare their own horizontal and
      // vertical padding in XML, and overwriting all four edges pushed the search field
      // and the close button flush against the screen edges.
      binding.header.updatePadding(top = bars.top + dp(12))
      binding.btnClose.updatePadding(bottom = bars.bottom)
      binding.rvApps.updatePadding(bottom = bars.bottom)
      insets
    }
  }

  private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

  private companion object {
    const val INPUT_MAX_LENGTH = 50
  }
}
