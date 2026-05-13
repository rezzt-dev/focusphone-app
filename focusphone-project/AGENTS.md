# FocusPhone — Agent Guide

> **FocusPhone** is a minimalist Android launcher designed to reduce digital distraction. It replaces the device's home screen with a clean, text-only interface showing a clock, a calendar, and up to 10 favourite apps. It supports contextual profiles (Normal, Work, Detox), a "Conscious Pause" breathing timer before opening apps, and extensive theming.

---

## Project Overview

| Property | Value |
|----------|-------|
| **Package** | `app.focusphone` |
| **Version** | 1.0.4 |
| **Language** | Kotlin |
| **Min SDK** | 24 |
| **Target / Compile SDK** | 36 |
| **Java Compatibility** | 11 |
| **AGP** | 9.2.1 |

The project is a single-module Android application (`:app`) with no library modules. It uses the **Android Gradle Plugin 9.x** and requires a **JDK 21** toolchain (path is pinned in `gradle.properties`).

---

## Technology Stack

### Core
- **Kotlin** with Coroutines (`kotlinx-coroutines-android`)
- **AndroidX**: Core KTX, AppCompat, Activity, Fragment, ConstraintLayout, RecyclerView, ViewPager2
- **Lifecycle**: ViewModel KTX, LiveData KTX, Runtime KTX
- **Material 3** (`com.google.android.material:material`)

### Data & Storage
- **Room 2.8.4** — stores the user's favourite apps (`favourite_apps` table).
- **SharedPreferences** — stores all user settings (theme, font, profiles, detox timer, conscious pause, custom app labels). Synchronous access is intentional so themes can be applied before `setContentView`.

### Architecture Patterns
- **Manual Dependency Injection** via `AppContainer` in `FocusPhoneApp`. Hilt is intentionally avoided in "Phase 1" due to AGP 9.x annotation-processing compatibility issues.
- **MVVM** with `ViewModel` + `StateFlow` / `SharedFlow`.
- **Repository Pattern** + **Use Cases** (Clean Architecture lite).
- **View Binding** is enabled for all UI screens.

---

## Project Structure

```
app/src/main/java/app/focusphone/
├── FocusPhoneApp.kt              # Application class; holds AppContainer
├── di/
│   └── AppContainer.kt           # Manual DI graph (singletons, repos, use cases)
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt        # Room database singleton
│   │   └── FavouriteAppDao.kt    # Room DAO
│   ├── model/
│   │   └── FavouriteApp.kt       # Room entity
│   ├── preferences/
│   │   ├── ThemePreferences.kt   # Theme, alignment, onboarding, status bar
│   │   ├── FontPreferences.kt    # Font family, clock type, clock size, text size
│   │   ├── ProfilePreferences.kt # Normal / Work / Detox profiles, detox timer & password, blocked apps
│   │   ├── ConsciousPausePreferences.kt # Enable/disable + duration (seconds)
│   │   └── CustomAppLabelsPreferences.kt # User-renamed app labels
│   └── repository/
│       ├── AppRepositoryImpl.kt       # Queries installed launcher apps
│       └── FavouriteAppRepositoryImpl.kt # Room + custom label merge; also handles SharedPrefs→Room migration
├── domain/
│   ├── model/
│   │   ├── AppInfo.kt            # Installed app domain model
│   │   ├── ClockType.kt          # DIGITAL | STACKED | WORDS | ANALOG | HIDDEN
│   │   ├── AppFont.kt            # Font enum (system + downloadable)
│   │   └── ToggleResult.kt       # Added | Removed | LimitReached
│   ├── repository/
│   │   ├── AppRepository.kt
│   │   └── FavouriteAppRepository.kt
│   └── usecase/
│       ├── GetInstalledAppsUseCase.kt
│       ├── GetFavouriteAppsUseCase.kt
│       ├── ToggleFavouriteAppUseCase.kt   # Enforces MAX_FAVOURITES = 10
│       └── UpdateAppsOrderUseCase.kt
└── ui/
    ├── base/
    │   └── BaseActivity.kt       # Theme application, edge-to-edge, font scale, fade transitions
    ├── home/
    │   ├── HomeActivity.kt       # Launcher activity; clock, calendar, drawer, profiles, reordering
    │   ├── HomeViewModel.kt
    │   └── adapter/
    │       └── FavouriteAppsAdapter.kt
    ├── appselect/
    │   ├── AppSelectActivity.kt  # Searchable list to pick favourite apps
    │   ├── AppSelectViewModel.kt
    │   └── adapter/
    │       └── InstalledAppsAdapter.kt
    ├── allapps/
    │   └── AllAppsActivity.kt    # Full app drawer with search + block/rename long-press menu
    ├── settings/
    │   └── SettingsActivity.kt   # Theme, clock, font, language, conscious pause, tutorial
    ├── onboarding/
    │   └── OnboardingActivity.kt # 3-step ViewPager2 onboarding flow
    ├── pause/
    │   └── ConsciousPauseActivity.kt # Breathing countdown before launching target app
    ├── detox/
    │   └── DetoxBlockedActivity.kt   # Full-screen block page with live countdown
    └── widget/
        ├── AnalogClockView.kt
        └── WeightGradientTextView.kt
```

### Resources
- `res/values/strings.xml` — primary English strings (all lowercase, minimalist tone).
- Localized strings in `values-es`, `values-fr`, `values-de`, `values-en-rGB`.
- `res/values/themes.xml` — 4 built-in themes + `Base.Theme.FocusPhone` + custom widget/dialog styles.
- `res/values-night/themes.xml` — night variants.
- `res/drawable/` — custom shapes, theme preview circles, tutorial icons.
- `res/anim/fp_fade_in.xml` & `fp_fade_out.xml` — used by `BaseActivity` for consistent Activity transitions.

---

## Build Commands

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release APK (minified + shrinkResources)
./gradlew :app:assembleRelease

# Run unit tests
./gradlew :app:testDebugUnitTest

# Run instrumented tests
./gradlew :app:connectedDebugAndroidTest

# Clean
./gradlew clean
```

### Build Configuration Notes
- `viewBinding = true` and `buildConfig = true` are enabled.
- Release builds use ProGuard (`proguard-rules.pro`) with `isMinifyEnabled = true` and `isShrinkResources = true`.
- Room uses `kapt` (legacy annotation processor) because AGP 9.2.1 is used.
- The project defines a fixed `org.gradle.java.home` pointing to `C:/Program Files/Android/openjdk/jdk-21.0.8`.
- Release signing config is conditional: set `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` in `gradle.properties` (or as project properties) to enable release signing. Without these properties, the release build uses no signing config and can still be assembled for local testing.

---

## Code Style Guidelines

### Language & Tone
- **All code comments and KDoc are written in English.**
- UI text (strings) is written in lowercase English with a minimalist, calm tone (e.g., `"no apps selected.\ntap the menu to add apps."`).
- Some older inline comments may mix Spanish; new code should remain in English.

### Formatting Conventions
- **Section dividers** in classes use a consistent pattern:
  ```kotlin
  // ── Section Name ──────────────────────────────────────────────────
  ```
- Package naming: `app.focusphone.{layer}.{feature}`.
- Activities use `dp(value: Int)` helper methods for pixel conversion.
- All Activities extend `BaseActivity`.

### Dependency Injection
- Do **not** introduce Hilt/Dagger without first resolving AGP 9.x compatibility.
- Register new singletons in `AppContainer` and expose factory methods on ViewModels (`companion object { fun factory(app: FocusPhoneApp): ViewModelProvider.Factory }`).
- `AppContainer` now receives an `applicationScope: CoroutineScope` for long-running background tasks such as data migrations.

### UI Patterns
- Always apply window insets manually (`ViewCompat.setOnApplyWindowInsetsListener`) for edge-to-edge layouts.
- Use `MaterialAlertDialogBuilder` for dialogs with the app's custom dialog theme.
- Text labels displayed to users are typically lowercased.

---

## Testing Strategy

> **Current state:** Testing is minimal. There is only a placeholder unit test and a placeholder instrumented test.

| Type | Location | Runner |
|------|----------|--------|
| Unit tests | `app/src/test/java/app/focusphone/` | JVM (JUnit 4) |
| Instrumented | `app/src/androidTest/java/dev/rezzt/` | AndroidJUnit4 |

**Note:** New unit tests are placed under the application package (`app.focusphone`). Legacy instrumented tests remain in `dev.rezzt` and should be migrated.

### Recommended Testing Approach
- Unit-test Use Cases and Repository logic with mocked repositories.
- UI tests for critical flows: onboarding, app selection, profile switching, detox password verification.
- Because the app queries all installed packages, instrumented tests should run on a device/emulator with a predictable app set.

---

## Key Architectural Decisions

### 1. Manual DI instead of Hilt
`AppContainer` manually wires all singletons. The code explicitly documents that Hilt is planned for "Phase 2" once a compatible version is available for AGP 9.x.

### 2. SharedPreferences for Settings (not DataStore)
Theme and font preferences must be read **synchronously** inside `BaseActivity.attachBaseContext()` and `onCreate()` before `setContentView`. DataStore's async nature makes it unsuitable here.

### 3. No App Icons on the Home Screen
The home screen (`HomeActivity`) displays only text labels for favourite apps. Icons are deliberately omitted to reduce visual noise.

### 4. Profile-Based Blocking
- **Normal**: no blocking.
- **Work**: blocks manually blocked apps + apps matching a hardcoded keyword list (`DISTRACTING_KEYWORDS`).
- **Detox**: blocks **all** apps except FocusPhone itself. Requires a password (min 4 chars) to exit early. Auto-reverts to Normal when the timer expires.

### 5. Migration from SharedPreferences to Room
`FavouriteAppRepositoryImpl` contains a `migrateFromSharedPreferences()` method that runs once on init, converting the old pipe-delimited string format into Room entities and then deleting the old key.

---

## Security Considerations

### Permissions (AndroidManifest.xml)
| Permission | Reason |
|------------|--------|
| `QUERY_ALL_PACKAGES` | List all installed apps for the app drawer and selector. |
<!-- Only QUERY_ALL_PACKAGES remains; unused permissions removed for Play Store compliance -->

### Detox Password Behavior
- The detox password is hashed with **SHA-256** before storage in SharedPreferences (`focusphone_profile`).
- After 4 incorrect password attempts, detox is **force-disabled** for safety and the password is cleared.
- The `focusphone_profile` SharedPreferences file is excluded from cloud backup via `data_extraction_rules.xml`.

### Custom Theme Colors
- Custom hex colors are parsed at runtime with `Color.parseColor()`. Invalid values fall back to safe defaults (`#181818` / `#FAFAFA`).

---

## Common Tasks for Agents

### Adding a New Setting
1. Add preference keys to the appropriate `*Preferences.kt` object in `data.preferences`.
2. Expose any repository/use-case changes needed.
3. Wire UI in the relevant Activity (usually `SettingsActivity`).
4. If the setting affects the theme or font scale, ensure `BaseActivity.onResume()` detects the change and calls `recreate()`.

### Adding a New Activity
1. Extend `BaseActivity`.
2. Declare it in `AndroidManifest.xml`.
3. Use `ActivityXxxBinding.inflate(layoutInflater)` for view binding.
4. Call `setupInsets()` or equivalent for edge-to-edge padding.
5. Observe flows with `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { ... } }`.

### Adding a New Use Case
1. Define it in `domain.usecase`.
2. Inject required repositories.
3. Expose it through `AppContainer`.
4. Pass it to the ViewModel via the `factory()` method.

### Modifying the Database
- Increment `AppDatabase` version and provide a migration, or use `fallbackToDestructiveMigration()` (currently enabled) during development only.

---

## Deployment Notes

- The app is signed and built as an APK via `./gradlew assembleRelease`.
- Output is in `app/release/`.
- `versionCode` and `versionName` are defined in `app/build.gradle.kts` (`defaultConfig`).
- The launcher intent filter in `AndroidManifest.xml` uses `android:priority="1"` on `HOME` / `DEFAULT` / `LAUNCHER` categories.
