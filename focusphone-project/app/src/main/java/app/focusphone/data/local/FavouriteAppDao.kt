package app.focusphone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.focusphone.data.model.FavouriteApp
import app.focusphone.domain.util.FavouriteOrdering
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteAppDao {
    @Query("SELECT * FROM favourite_apps ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<FavouriteApp>>

    @Query("SELECT * FROM favourite_apps ORDER BY sortOrder ASC")
    suspend fun getAll(): List<FavouriteApp>

    @Query("SELECT COUNT(*) FROM favourite_apps")
    suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM favourite_apps WHERE packageName = :packageName)")
    suspend fun exists(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: FavouriteApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<FavouriteApp>)

    @Query("DELETE FROM favourite_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("UPDATE favourite_apps SET sortOrder = :sortOrder WHERE packageName = :packageName")
    suspend fun updateSortOrder(packageName: String, sortOrder: Int)

    @Transaction
    suspend fun deleteAndReorder(packageName: String) {
        delete(packageName)
        getAll().forEachIndexed { index, app -> updateSortOrder(app.packageName, index) }
    }

    /**
     * Applies [requestedOrder] by rewriting `sortOrder` only.
     *
     * No row is ever deleted here: favourites the request does not mention keep their
     * relative order after the requested ones (see [FavouriteOrdering.merge]).
     */
    @Transaction
    suspend fun applyOrder(requestedOrder: List<String>) {
        val merged = FavouriteOrdering.merge(getAll().map { it.packageName }, requestedOrder)
        merged.forEachIndexed { index, packageName -> updateSortOrder(packageName, index) }
    }

    /**
     * Atomic insert that assigns the next available sortOrder.
     * Uses REPLACE so duplicates are overwritten safely.
     */
    @Transaction
    suspend fun insertIfNotExists(app: FavouriteApp) {
        if (!exists(app.packageName)) {
            val nextOrder = count()
            insert(app.copy(sortOrder = nextOrder))
        }
    }
}
