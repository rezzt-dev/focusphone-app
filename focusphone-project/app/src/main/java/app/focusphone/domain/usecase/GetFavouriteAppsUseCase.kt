package app.focusphone.domain.usecase

import app.focusphone.BuildConfig
import app.focusphone.data.model.FavouriteApp
import app.focusphone.domain.repository.FavouriteAppRepository
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.map

class GetFavouriteAppsUseCase(
    private val repository: FavouriteAppRepository
) {
    operator fun invoke(): Flow<List<FavouriteApp>> = repository.observeFavourites().map { apps ->
        apps
            .filter { it.packageName != BuildConfig.APPLICATION_ID }
            .take(ToggleFavouriteAppUseCase.MAX_FAVOURITES)
    }
}
