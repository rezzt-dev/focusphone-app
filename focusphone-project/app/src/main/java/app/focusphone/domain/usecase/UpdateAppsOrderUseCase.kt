package app.focusphone.domain.usecase

import app.focusphone.data.model.FavouriteApp
import app.focusphone.domain.repository.FavouriteAppRepository

class UpdateAppsOrderUseCase(
    private val repository: FavouriteAppRepository
) {
    suspend operator fun invoke(apps: List<FavouriteApp>) {
        val updated = apps.mapIndexed { index, app ->
            app.copy(sortOrder = index)
        }
        repository.replaceAll(updated)
    }
}
