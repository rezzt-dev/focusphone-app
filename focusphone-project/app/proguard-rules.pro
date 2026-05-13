# Room entities
-keep class app.focusphone.data.model.** { *; }

# Room database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
