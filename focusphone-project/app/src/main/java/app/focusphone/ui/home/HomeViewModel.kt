package app.focusphone.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.focusphone.FocusPhoneApp
import app.focusphone.data.model.FavouriteApp
import app.focusphone.domain.usecase.GetFavouriteAppsUseCase
import app.focusphone.domain.usecase.RemoveFavouriteAppUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    getFavouriteApps: GetFavouriteAppsUseCase,
    private val removeFavouriteApp: RemoveFavouriteAppUseCase,
    private val updateAppsOrder: app.focusphone.domain.usecase.UpdateAppsOrderUseCase
) : ViewModel() {

    val favouriteApps: StateFlow<List<FavouriteApp>> = getFavouriteApps()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun removeFavourite(packageName: String) {
        viewModelScope.launch {
            try {
                removeFavouriteApp(packageName)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to remove favourite $packageName", e)
            }
        }
    }
    fun onAppsReordered(newOrder: List<FavouriteApp>) {
        viewModelScope.launch {
            try {
                updateAppsOrder(newOrder)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to update apps order", e)
            }
        }
    }

    // ── Factory ───────────────────────────────────────────────────

    companion object {
        fun factory(app: FocusPhoneApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(
                        app.container.getFavouriteAppsUseCase,
                        app.container.removeFavouriteAppUseCase,
                        app.container.updateAppsOrderUseCase
                    ) as T
            }
    }
}
