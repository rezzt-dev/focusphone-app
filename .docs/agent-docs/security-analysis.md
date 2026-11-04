#  **FOCUSPHONE  |  AUDIT REPORT  |  SECURITY ANALYSIS**
> informacion sobre la seguridad de la aplicacion, se ha realizado un analisis.
---
##   **SECURITY ANALYSIS  |  SUMMARY**
- AppDatabase enables `fallbackToDestructiveMigration()` — any schema bump wipes all favourites permanently.
- Repository read-modify-write ops (`removeFavourite`, `replaceAll`) lack Room `@Transaction` — concurrent edits or process death corrupt `sortOrder` and cause partial data loss.
- Backup rules XML files are effectively empty despite `allowBackup=true` — user data is lost on device migration or reinstall.
- `registerReceiver(ACTION_TIME_TICK, RECEIVER_EXPORTED)` in `HomeActivity.onResume` throws `SecurityException` on some OEMs/Android versions, crashing the launcher.
- ViewModels and UseCases fire coroutines without try/catch — any Room I/O or PackageManager error crashes the app.

---
##   **SECURITY ANALYSIS  |  ISSUES**
###    **ISSUES  |  ISS-1 (CRITICAL)**
**Symptom:** All favourite apps permanently deleted on app update after any Room schema change.  
**Root cause:** `fallbackToDestructiveMigration()` used without migrations.
**Steps:**
1. Install app, add favourites.
2. Update app with new DB version.
3. Data wiped.
**Evidence:**  
`AppDatabase.kt:24`
###    **ISSUES  |  ISS-2 (CRITICAL)**
**Symptom:** Corrupted or lost favourites after reorder or process death.  
**Root cause:** Missing `@Transaction` in multi-step DB operations.
**Steps:**
1. Reorder favourites.
2. Kill app mid-operation.
3. Data becomes inconsistent.
**Evidence:**  
`FavouriteAppRepositoryImpl.kt:70-84`
###    **ISSUES  |  ISS-3 (CRITICAL)**
**Symptom:** No data restored after reinstall/device transfer.  
**Root cause:** Empty backup rules.
**Evidence:**  
- `backup_rules.xml`
- `data_extraction_rules.xml`
###    **ISSUES  |  ISS-4 (HIGH)**
**Symptom:** Launcher crashes on resume.  
**Root cause:** `registerReceiver` throws `SecurityException`.
**Evidence:**  
`HomeActivity.kt:95-98`
###    **ISSUES  |  ISS-5 (HIGH)**
**Symptom:** App crashes due to unhandled coroutine exceptions.  
**Root cause:** Missing try/catch in ViewModels.
**Evidence:**  
- `HomeViewModel.kt`
- `AllAppsActivity.kt`
###    **ISSUES  |  ISS-6 (HIGH)**
**Symptom:** Detox mode behaves inconsistently.  
**Root cause:** Side effects inside a read method.
**Evidence:**  
`ProfilePreferences.kt:47-55`
###    **ISSUES  |  ISS-7 (HIGH)**
**Symptom:** Release build crashes with Room instantiation error.  
**Root cause:** Missing ProGuard rules.
**Evidence:**  `proguard-rules.pro`

---
##   **SECURITY ANALYSIS  |  PATCHES**
###    **PATCHES  |  FIX ISS-1**
```kotlin
// REMOVE destructive migration
// .fallbackToDestructiveMigration()
```
###    **PATCHES  |  FIX ISS-2**
```kotlin
@Transaction
suspend fun deleteAndReorder(packageName: String)

@Transaction
suspend fun replaceAllOrdered(apps: List<FavouriteApp>)
```
###    **PATCHES  |  FIX ISS-3**
```xml
<include domain="sharedpref" path="."/>
<include domain="database" path="focusphone_database"/>
```
###    **PATCHES  |  FIX ISS-4**
```kotlin
try {
    ContextCompat.registerReceiver(...)
} catch (_: SecurityException) { }
```
###    **PATCHES  |  FIX ISS-5**
```kotlin
viewModelScope.launch {
    try { ... } catch (_: Exception) { }
}
```
###    **PATCHES  |  FIX ISS-6**
```kotlin
fun isDetoxActive(): Boolean { /* no writes */ }

fun expireDetoxIfNeeded() { /* explicit write */ }
```
###    **PATCHES  |  FIX ISS-7**
```pro
-keep class app.focusphone.data.model.** { *; }
-keep class * extends androidx.room.RoomDatabase
```

---
##   **SECURITY ANALYSIS  |  RISKS & CHECKLIST**
###    **RISKS & CHECKLIST  |  RISKS**
- Removing destructive migration requires proper migrations or app will crash.
- Backup changes apply only to future backups.
- Slight APK size increase from ProGuard rules.
###    **RISKS & CHECKLIST  |  CHECKLIST**
- Never use `fallbackToDestructiveMigration()` in production.
- Always use `@Transaction` for multi-step DB writes.
- Configure backup rules correctly.
- Wrap risky calls in try/catch.
- Avoid side effects in getters.
- Add ProGuard rules for Room.
- Test release builds before publishing.
