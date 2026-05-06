package app.focusphone.data.repository

import app.focusphone.data.model.FavouriteApp
import app.focusphone.data.preferences.FavouriteAppsPreferences
import app.focusphone.domain.repository.FavouriteAppRepository
import kotlinx.coroutines.flow.Flow

class FavouriteAppRepositoryImpl(
    private val prefs: FavouriteAppsPreferences
) : FavouriteAppRepository {

    override fun observeFavourites(): Flow<List<FavouriteApp>> = prefs.appsFlow

    override suspend fun getFavourites(): List<FavouriteApp> = prefs.getAll()

    override suspend fun addFavourite(app: FavouriteApp) = prefs.add(app)

    override suspend fun removeFavourite(packageName: String) = prefs.remove(packageName)

    override suspend fun isFavourite(packageName: String): Boolean = prefs.contains(packageName)

    override suspend fun replaceAll(apps: List<FavouriteApp>) = prefs.replaceAll(apps)
}
