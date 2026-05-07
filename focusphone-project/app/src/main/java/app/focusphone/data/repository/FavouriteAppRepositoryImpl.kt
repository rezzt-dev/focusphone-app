package app.focusphone.data.repository

import android.content.Context
import app.focusphone.data.local.FavouriteAppDao
import app.focusphone.data.model.FavouriteApp
import app.focusphone.data.preferences.CustomAppLabelsPreferences
import app.focusphone.domain.repository.FavouriteAppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FavouriteAppRepositoryImpl(
    private val dao: FavouriteAppDao,
    private val customLabels: CustomAppLabelsPreferences,
    context: Context
) : FavouriteAppRepository {

    init {
        migrateFromSharedPreferences(context)
    }

    private fun migrateFromSharedPreferences(context: Context) {
        val oldPrefs = context.getSharedPreferences("focusphone_favourites", Context.MODE_PRIVATE)
        val raw = oldPrefs.getString("apps", null)
        if (raw != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val apps = raw.split("\n")
                        .filter { it.isNotBlank() }
                        .mapIndexed { index, row ->
                            val cols = row.split("|")
                            FavouriteApp(
                                packageName = cols[0],
                                label       = cols.getOrElse(1) { cols[0] },
                                sortOrder   = cols.getOrElse(2) { "$index" }.toIntOrNull() ?: index
                            )
                        }
                    dao.insertAll(apps)
                    oldPrefs.edit().remove("apps").apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun observeFavourites(): Flow<List<FavouriteApp>> =
        combine(dao.observeAll(), customLabels.labelsFlow) { list, labels ->
            list.map { app ->
                val custom = labels[app.packageName]
                if (custom != null) app.copy(label = custom) else app
            }
        }

    override suspend fun getFavourites(): List<FavouriteApp> =
        dao.getAll().map { app ->
            val custom = customLabels.getCustomLabel(app.packageName)
            if (custom != null) app.copy(label = custom) else app
        }

    override suspend fun addFavourite(app: FavouriteApp) {
        val current = dao.getAll()
        if (current.none { it.packageName == app.packageName }) {
            dao.insert(app.copy(sortOrder = current.size))
        }
    }

    override suspend fun removeFavourite(packageName: String) {
        dao.delete(packageName)
        val remaining = dao.getAll()
            .mapIndexed { index, app -> app.copy(sortOrder = index) }
        dao.insertAll(remaining)
    }

    override suspend fun isFavourite(packageName: String): Boolean =
        dao.getAll().any { it.packageName == packageName }

    override suspend fun replaceAll(apps: List<FavouriteApp>) {
        dao.deleteAll()
        val ordered = apps.mapIndexed { index, app -> app.copy(sortOrder = index) }
        dao.insertAll(ordered)
    }
}
