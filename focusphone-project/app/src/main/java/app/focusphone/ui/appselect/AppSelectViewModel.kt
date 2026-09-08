package app.focusphone.ui.appselect

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.focusphone.FocusPhoneApp
import app.focusphone.domain.model.AppInfo
import app.focusphone.domain.model.ToggleResult
import app.focusphone.domain.usecase.GetInstalledAppsUseCase
import app.focusphone.domain.usecase.ToggleFavouriteAppUseCase
import app.focusphone.domain.util.FuzzyMatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppSelectViewModel(
    private val getInstalledApps: GetInstalledAppsUseCase,
    private val toggleFavourite: ToggleFavouriteAppUseCase
) : ViewModel() {

    private val _allApps   = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _query     = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(true)
    private val _filtered  = MutableStateFlow<List<AppInfo>>(emptyList())

    val isLoading: StateFlow<Boolean>       = _isLoading.asStateFlow()
    val filteredApps: StateFlow<List<AppInfo>> = _filtered.asStateFlow()

    private val _events = MutableSharedFlow<AppSelectEvent>()
    val events: SharedFlow<AppSelectEvent> = _events.asSharedFlow()

    private var searchJob: Job? = null

    init { loadApps() }

    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _allApps.value   = try { getInstalledApps() } catch (e: Exception) {
                Log.e("AppSelectViewModel", "Failed to load installed apps", e)
                emptyList()
            }
            applyFilter()
            _isLoading.value = false
        }
    }

    fun onSearchQuery(query: String) {
        _query.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(150)
            applyFilter()
        }
    }

    fun onToggle(app: AppInfo) {
        viewModelScope.launch {
            val result = try { toggleFavourite(app.packageName, app.label) } catch (e: Exception) {
                Log.e("AppSelectViewModel", "Failed to toggle favourite for ${app.packageName}", e)
                return@launch
            }
            when (result) {
                is ToggleResult.Added -> {
                    _allApps.update { list ->
                        list.map { if (it.packageName == app.packageName) it.copy(isFavourite = true) else it }
                    }
                    applyFilter()
                }
                is ToggleResult.Removed -> {
                    _allApps.update { list ->
                        list.map { if (it.packageName == app.packageName) it.copy(isFavourite = false) else it }
                    }
                    applyFilter()
                }
                is ToggleResult.LimitReached -> {
                    _events.emit(AppSelectEvent.LimitReached(result.limit))
                }
            }
        }
    }

    fun selectedCount(): Int = _allApps.value.count { it.isFavourite }

    fun maxApps(): Int = ToggleFavouriteAppUseCase.MAX_FAVOURITES

    private fun applyFilter() {
        val q = _query.value.trim()
        _filtered.value = if (q.isEmpty()) _allApps.value
        else _allApps.value.filter { FuzzyMatcher.matches(q, it.label) }
    }

    // ── Factory ───────────────────────────────────────────────────

    companion object {
        fun factory(app: FocusPhoneApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppSelectViewModel(
                        app.container.getInstalledAppsUseCase,
                        app.container.toggleFavouriteAppUseCase
                    ) as T
            }
    }
}

sealed class AppSelectEvent {
    data class LimitReached(val limit: Int) : AppSelectEvent()
}
