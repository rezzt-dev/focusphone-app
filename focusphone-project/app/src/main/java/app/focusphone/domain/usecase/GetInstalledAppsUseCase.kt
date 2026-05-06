package app.focusphone.domain.usecase

import app.focusphone.domain.model.AppInfo
import app.focusphone.domain.repository.AppRepository
import app.focusphone.domain.repository.FavouriteAppRepository

class GetInstalledAppsUseCase(
    private val appRepository: AppRepository,
    private val favouriteRepository: FavouriteAppRepository
) {
    suspend operator fun invoke(): List<AppInfo> {
        val installed  = appRepository.getInstalledApps()
        val favourites = favouriteRepository.getFavourites()
            .take(ToggleFavouriteAppUseCase.MAX_FAVOURITES)
            .map { it.packageName }
            .toSet()
            
        return installed.map { app ->
            app.copy(isFavourite = app.packageName in favourites)
        }
    }
}
