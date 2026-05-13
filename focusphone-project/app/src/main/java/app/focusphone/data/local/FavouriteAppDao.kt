package app.focusphone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.focusphone.data.model.FavouriteApp
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

    @Query("DELETE FROM favourite_apps")
    suspend fun deleteAll()

    @Transaction
    suspend fun deleteAndReorder(packageName: String) {
        delete(packageName)
        val remaining = getAll().mapIndexed { index, app -> app.copy(sortOrder = index) }
        insertAll(remaining)
    }

    @Transaction
    suspend fun replaceAllOrdered(apps: List<FavouriteApp>) {
        deleteAll()
        insertAll(apps)
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
