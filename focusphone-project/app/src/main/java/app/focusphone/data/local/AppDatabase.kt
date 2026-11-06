package app.focusphone.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.focusphone.data.model.FavouriteApp

@Database(entities = [FavouriteApp::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favouriteAppDao(): FavouriteAppDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase = instance ?: synchronized(this) {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "focusphone_database"
            )
            // Favourite apps can be re-selected by the user, so destructive migration
            // is acceptable in both debug and release during early development.
            builder.fallbackToDestructiveMigration(true)
            val created = builder.build()
            instance = created
            created
        }
    }
}
