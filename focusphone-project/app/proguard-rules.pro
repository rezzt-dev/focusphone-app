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

# Security classes — must not be obfuscated for auditability and Keystore stability
-keep class app.focusphone.data.preferences.ProfilePreferences { *; }
-keep class app.focusphone.security.SecureStorageHelper { *; }
-keep class app.focusphone.service.FocusAccessibilityService { *; }

# Strip debug logs in release builds; keep errors and warnings
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int d(...);
}
