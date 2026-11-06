package app.focusphone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Favourite app entry stored via Room database.
 */
@Entity(tableName = "favourite_apps")
data class FavouriteApp(
    @PrimaryKey val packageName: String,
    val label: String,
    val sortOrder: Int = 0
)
