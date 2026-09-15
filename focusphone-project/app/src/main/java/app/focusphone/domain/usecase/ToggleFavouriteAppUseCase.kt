package app.focusphone.domain.usecase

import app.focusphone.data.model.FavouriteApp
import app.focusphone.domain.model.ToggleResult
import app.focusphone.domain.repository.FavouriteAppRepository
import app.focusphone.domain.util.normalizeAppName

class ToggleFavouriteAppUseCase(
    private val repository: FavouriteAppRepository
) {
    /**
     * Toggles favourite status for [packageName].
     * Returns [ToggleResult.LimitReached] when trying to add beyond [MAX_FAVOURITES].
     */
    suspend operator fun invoke(packageName: String, label: String): ToggleResult = if (repository.isFavourite(packageName)) {
        repository.removeFavourite(packageName)
        ToggleResult.Removed
    } else {
        val currentCount = repository.getFavourites().size
        if (currentCount >= MAX_FAVOURITES) {
            ToggleResult.LimitReached(MAX_FAVOURITES)
        } else {
            repository.addFavourite(
                FavouriteApp(packageName = packageName, label = label.normalizeAppName(), sortOrder = currentCount)
            )
            ToggleResult.Added
        }
    }

    companion object {
        /** Maximum allowed favourite apps. Enforces intentional, distraction-free selection. */
        const val MAX_FAVOURITES = 10
    }
}
