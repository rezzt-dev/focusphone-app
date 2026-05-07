package app.focusphone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.focusphone.data.model.FavouriteApp
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteAppDao {
    @Query("SELECT * FROM favourite_apps ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<FavouriteApp>>

    @Query("SELECT * FROM favourite_apps ORDER BY sortOrder ASC")
    suspend fun getAll(): List<FavouriteApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: FavouriteApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<FavouriteApp>)

    @Query("DELETE FROM favourite_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM favourite_apps")
    suspend fun deleteAll()
}
