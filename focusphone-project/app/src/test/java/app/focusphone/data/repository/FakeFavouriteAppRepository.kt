package app.focusphone.data.repository

import app.focusphone.data.model.FavouriteApp
import app.focusphone.domain.repository.FavouriteAppRepository
import app.focusphone.domain.util.FavouriteOrdering
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFavouriteAppRepository : FavouriteAppRepository {

    private val apps = mutableListOf<FavouriteApp>()
    private val flow = MutableStateFlow<List<FavouriteApp>>(emptyList())

    override fun observeFavourites(): Flow<List<FavouriteApp>> = flow

    override suspend fun getFavourites(): List<FavouriteApp> = apps.toList()

    override suspend fun addFavourite(app: FavouriteApp) {
        if (apps.none { it.packageName == app.packageName }) {
            apps.add(app.copy(sortOrder = apps.size))
            flow.value = apps.toList()
        }
    }

    override suspend fun removeFavourite(packageName: String) {
        apps.removeAll { it.packageName == packageName }
        // Re-sort
        apps.forEachIndexed { index, app -> apps[index] = app.copy(sortOrder = index) }
        flow.value = apps.toList()
    }

    override suspend fun isFavourite(packageName: String): Boolean =
        apps.any { it.packageName == packageName }

    override suspend fun updateOrder(packageNames: List<String>) {
        val byPackage = apps.associateBy { it.packageName }
        val merged = FavouriteOrdering.merge(apps.map { it.packageName }, packageNames)
        apps.clear()
        apps.addAll(merged.mapIndexed { index, pkg -> byPackage.getValue(pkg).copy(sortOrder = index) })
        flow.value = apps.toList()
    }
}
