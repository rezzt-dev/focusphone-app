# Room entities
-keep class app.focusphone.data.model.** { *; }

# Room database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding { *; }

# Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Custom views and widgets
-keep class app.focusphone.ui.widget.** { *; }
