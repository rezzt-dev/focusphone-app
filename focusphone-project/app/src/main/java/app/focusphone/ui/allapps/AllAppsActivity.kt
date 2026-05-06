package app.focusphone.ui.allapps

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.focusphone.FocusPhoneApp
import app.focusphone.R
import app.focusphone.databinding.ActivityAllAppsBinding
import app.focusphone.databinding.ItemAllAppBinding
import app.focusphone.domain.model.AppInfo
import app.focusphone.domain.usecase.GetInstalledAppsUseCase
import app.focusphone.ui.base.BaseActivity
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

    init { load() }

    private fun load() {
        viewModelScope.launch {
            isLoading.value = true
            _allApps.value  = getInstalledApps()
            applyFilter()
            isLoading.value = false
        }
    }

    fun onQuery(q: String) { _query.value = q; applyFilter() }

    private fun applyFilter() {
        val q = _query.value.trim().lowercase()
        _filtered.value = if (q.isEmpty()) _allApps.value
        else _allApps.value.filter { it.label.lowercase().contains(q) }
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

    private val adapter = AllAppsAdapter { app ->
        binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        try {
            startActivity(packageManager.getLaunchIntentForPackage(app.packageName) ?: return@AllAppsAdapter)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.error_app_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = adapter
        binding.btnClose.setOnClickListener { finish() }
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onQuery(s?.toString() ?: "")
            }
        })
        lifecycleScope.launch {
            viewModel.filteredApps.collect { adapter.submitList(it) }
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.header.setPadding(0, bars.top + dp(12), 0, 0)
            binding.btnClose.setPadding(0, 0, 0, bars.bottom)
            insets
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

// ── Adapter ────────────────────────────────────────────────────────

class AllAppsAdapter(
    private val onTap: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AllAppsAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAllAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemAllAppBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(app: AppInfo) {
            b.ivIcon.setImageDrawable(app.icon)
            b.tvName.text = app.label.lowercase()
            b.root.setOnClickListener { onTap(app) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(o: AppInfo, n: AppInfo) = o.packageName == n.packageName
            override fun areContentsTheSame(o: AppInfo, n: AppInfo) = o == n
        }
    }
}
