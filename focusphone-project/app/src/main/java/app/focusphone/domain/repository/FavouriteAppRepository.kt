package app.focusphone.domain.repository

import app.focusphone.data.model.FavouriteApp
import kotlinx.coroutines.flow.Flow

/**
 * CRUD interface for the user's favourite / pinned apps.
 * Backed by Room; changes are emitted reactively via [Flow].
 */
interface FavouriteAppRepository {
    fun observeFavourites(): Flow<List<FavouriteApp>>
    suspend fun getFavourites(): List<FavouriteApp>
    suspend fun addFavourite(app: FavouriteApp)
    suspend fun removeFavourite(packageName: String)
    suspend fun isFavourite(packageName: String): Boolean
    suspend fun replaceAll(apps: List<FavouriteApp>)
}
