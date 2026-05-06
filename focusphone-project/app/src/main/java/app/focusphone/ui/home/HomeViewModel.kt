package app.focusphone.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.focusphone.FocusPhoneApp
import app.focusphone.data.model.FavouriteApp
import app.focusphone.domain.model.ToggleResult
import app.focusphone.domain.usecase.GetFavouriteAppsUseCase
import app.focusphone.domain.usecase.ToggleFavouriteAppUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    getFavouriteApps: GetFavouriteAppsUseCase,
    private val toggleFavourite: ToggleFavouriteAppUseCase,
    private val updateAppsOrder: app.focusphone.domain.usecase.UpdateAppsOrderUseCase
) : ViewModel() {

    val favouriteApps: StateFlow<List<FavouriteApp>> = getFavouriteApps()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** One-shot events for the UI (e.g. showing a Toast on limit reached). */
    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    fun removeFavourite(packageName: String, label: String) {
        viewModelScope.launch { toggleFavourite(packageName, label) }
    }
    fun onAppsReordered(newOrder: List<FavouriteApp>) {
        viewModelScope.launch { updateAppsOrder(newOrder) }
    }

    // ── Factory ───────────────────────────────────────────────────

    companion object {
        fun factory(app: FocusPhoneApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(
                        app.container.getFavouriteAppsUseCase,
                        app.container.toggleFavouriteAppUseCase,
                        app.container.updateAppsOrderUseCase
                    ) as T
            }
    }
}

/** One-shot UI events emitted by [HomeViewModel]. */
sealed class HomeEvent {
    data class LimitReached(val limit: Int) : HomeEvent()
}
