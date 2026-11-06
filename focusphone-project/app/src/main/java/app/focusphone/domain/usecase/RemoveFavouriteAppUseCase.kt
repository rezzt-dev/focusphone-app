package app.focusphone.domain.usecase

import app.focusphone.domain.repository.FavouriteAppRepository

/**
 * Removes an app from the favourites.
 *
 * Use this — not [ToggleFavouriteAppUseCase] — wherever the intent is to remove: a toggle
 * adds the app back when it is no longer a favourite, which let overlapping clean-ups of
 * uninstalled apps re-insert them.
 */
class RemoveFavouriteAppUseCase(
    private val repository: FavouriteAppRepository
) {
    /** Removes [packageName]; does nothing when it is not a favourite. */
    suspend operator fun invoke(packageName: String) {
        repository.removeFavourite(packageName)
    }
}
