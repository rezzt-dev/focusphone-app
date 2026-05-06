package app.focusphone.ui.appselect

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import kotlinx.coroutines.launch

class AppSelectActivity : BaseActivity() {

    private lateinit var binding: ActivityAppSelectBinding

    private val viewModel: AppSelectViewModel by viewModels {
        AppSelectViewModel.factory(application as FocusPhoneApp)
    }

    private val adapter = InstalledAppsAdapter { app -> viewModel.onToggle(app) }

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

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.header.setPadding(0, bars.top + dp(16), 0, 0)
            binding.btnDone.setPadding(0, 0, 0, bars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onSearchQuery(s?.toString() ?: "")
            }
        })
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
