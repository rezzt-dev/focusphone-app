package app.focusphone.domain.usecase

import app.focusphone.data.model.FavouriteApp
import app.focusphone.domain.repository.FavouriteAppRepository

/**
 * Persists a new order for the favourite apps.
 *
 * Only the order changes: a list that omits some favourites (for example one built from a
 * partially displayed list) leaves them in place after the reordered ones.
 */
class UpdateAppsOrderUseCase(
    private val repository: FavouriteAppRepository
) {
    suspend operator fun invoke(apps: List<FavouriteApp>) {
        repository.updateOrder(apps.map { it.packageName })
    }
}
