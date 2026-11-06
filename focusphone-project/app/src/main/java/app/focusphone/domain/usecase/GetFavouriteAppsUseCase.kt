package app.focusphone.domain.usecase

import app.focusphone.BuildConfig
import app.focusphone.data.model.FavouriteApp
import app.focusphone.domain.repository.FavouriteAppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Observes the favourite apps shown on the home screen.
 *
 * The [ToggleFavouriteAppUseCase.MAX_FAVOURITES] limit is enforced where favourites are
 * written, not here: truncating on read hid stored favourites from every screen, and the
 * next reorder — built from the truncated list — used to delete them.
 */
class GetFavouriteAppsUseCase(
    private val repository: FavouriteAppRepository
) {
    operator fun invoke(): Flow<List<FavouriteApp>> = repository.observeFavourites().map { apps ->
        apps.filter { it.packageName != BuildConfig.APPLICATION_ID }
    }
}
