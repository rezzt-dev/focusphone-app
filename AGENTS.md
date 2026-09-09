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
- **SharedPreferences** — stores all user settings (theme, font, profiles, detox timer, conscious pause, custom app labels, work keywords). Synchronous access is intentional so themes can be applied before `setContentView`.

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
│   │   ├── AppSchedulePreferences.kt # Per-app time windows, daily limits, usage counters, schedule password
│   │   ├── ThemePreferences.kt   # Theme, alignment, onboarding, status bar
│   │   ├── FontPreferences.kt    # Font family, clock type, clock size, text size
│   │   ├── ProfilePreferences.kt # Normal / Work / Detox profiles, detox timer & password, blocked apps
│   │   ├── ConsciousPausePreferences.kt # Enable/disable + duration (seconds)
│   │   ├── CustomAppLabelsPreferences.kt # User-renamed app labels
│   │   └── AppFolderPreferences.kt   # App-drawer folders and their expanded state
│   └── repository/
│       ├── AppRepositoryImpl.kt       # Queries installed launcher apps
│       └── FavouriteAppRepositoryImpl.kt # Room + custom label merge; also handles SharedPrefs→Room migration
├── domain/
│   ├── model/
│   │   ├── AppInfo.kt            # Installed app domain model
│   │   ├── AppScheduleRule.kt    # Per-app time window + daily limit, and ScheduleBlockReason
│   │   ├── AppFolder.kt          # App-drawer folder (id, name, member packages)
│   │   ├── ClockType.kt          # DIGITAL | STACKED | WORDS | ANALOG | HIDDEN
│   │   ├── AppFont.kt            # Font enum (system + downloadable)
│   │   └── ToggleResult.kt       # Added | Removed | LimitReached
│   ├── repository/
│   │   ├── AppRepository.kt
│   │   └── FavouriteAppRepository.kt
│   ├── util/
│   │   ├── AppScheduleEvaluator.kt # Pure schedule evaluation + rule serialization
│   │   ├── AppFolderSerializer.kt  # Pure folder serialization, name sanitizing, ordering
│   │   └── FuzzyMatcher.kt       # Subsequence search scoring
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
    │   ├── AllAppsActivity.kt    # Full app drawer with search, folders and long-press menu
    │   ├── AllAppsRow.kt         # Sealed row model: folder header | app
    │   ├── FolderDragCallback.kt # Drag & drop into folders + long-press dispatch
    │   └── adapter/
    │       └── AllAppsAdapter.kt # Two view types (folder header, app row)
    ├── settings/
    │   ├── SettingsActivity.kt            # Category menu that opens sub-screens
    │   ├── SettingsSubActivity.kt         # Abstract base for second-level settings screens
    │   ├── SettingsUiHelper.kt            # Reusable row inflaters (category, switch, value)
    │   ├── AppearanceSettingsActivity.kt  # Theme, custom colours, font, text size, alignment, status bar
    │   ├── HomeSettingsActivity.kt        # Clock type/size, app spacing, widget, home layout
    │   ├── FocusSettingsActivity.kt       # Conscious pause and work-mode keywords
    │   ├── NotificationSettingsActivity.kt# Badges, notification panel, swipe-down gesture
    │   ├── SystemSettingsActivity.kt      # Permissions, accessibility, default launcher, language
    │   ├── AboutSettingsActivity.kt       # Version, tutorial, onboarding, open-source licences
    │   ├── HomeLayoutActivity.kt          # Reorder/resize home-screen components
    │   └── PermissionsActivity.kt         # Required system permissions status
    ├── onboarding/
    │   └── OnboardingActivity.kt # 3-step ViewPager2 onboarding flow
    ├── pause/
    │   └── ConsciousPauseActivity.kt # Breathing countdown before launching target app
    ├── detox/
    │   └── DetoxBlockedActivity.kt   # Full-screen block page with live countdown
    ├── schedule/
    │   ├── AppScheduleActivity.kt     # Password-gated list of scheduled apps
    │   ├── AppScheduleEditActivity.kt # Time window, weekdays and daily limit for one app
    │   ├── ScheduleBlockedActivity.kt # Block page explaining the schedule restriction
    │   ├── ScheduleAccessGate.kt      # In-memory unlock session for the settings
    │   └── ScheduleDayLabels.kt       # Locale-aware weekday labels and week order
    ├── theme/
    │   ├── FocusColors.kt        # WCAG contrast maths (luminance, ratio, ensureContrast)
    │   ├── FocusPalette.kt       # Single source of truth for the active theme's colours
    │   └── SurfaceScrim.kt       # Adaptive scrim between a wallpaper and the content
    └── widget/
        ├── AnalogClockView.kt
        └── WeightGradientTextView.kt
```

> **Colours at runtime:** always go through `FocusPalette.of(context)` (or `BaseActivity.palette` /
> `effectivePalette`). Never `Theme.resolveAttribute(attr, tv, true).data` — it yields a resource id
> rather than a colour whenever the attribute resolves to a `ColorStateList`, and it does not know
> about the custom theme. See `docs/app-analysis/interfaz-tematizacion-y-legibilidad.md`.

> **Window insets:** use `updatePadding(top = …)`, never `setPadding(0, …, 0, 0)`, or the padding a
> layout declares in XML is silently erased.

### Resources
- `res/values/strings.xml` — primary English strings (all lowercase, minimalist tone).
- Localized strings in `values-es`, `values-fr`, `values-de`, `values-en-rGB`.
- `res/values/themes.xml` — 4 built-in themes + `Base.Theme.FocusPhone` + custom widget/dialog styles.
  Each theme also redirects the Material 3 roles it does not define to its own colours, so the M3
  baseline (purple) never leaks into the monochrome UI.
- `res/values-night/themes.xml` — intentionally empty; day/night is handled manually via `THEME_AUTO` resolving to explicit themes.
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
- `AppContainer` accepts optional `appRepositoryOverride` and `favouriteAppRepositoryOverride` constructor parameters to enable fake repository injection in instrumented tests.

### UI Patterns
- Always apply window insets manually (`ViewCompat.setOnApplyWindowInsetsListener`) for edge-to-edge layouts.
- Use `MaterialAlertDialogBuilder` for dialogs with the app's custom dialog theme.
- Text labels displayed to users are typically lowercased.

---

## Testing Strategy

| Type | Location | Runner | Libraries |
|------|----------|--------|-----------|
| Unit tests | `app/src/test/java/app/focusphone/` | JVM (JUnit 4) | MockK, Robolectric, Turbine, kotlinx-coroutines-test |
| Instrumented | `app/src/androidTest/java/app/focusphone/` | AndroidJUnit4 via `TestRunner` | Espresso, ActivityScenario |

**Note:** Legacy placeholder tests remain in `dev.rezzt` and should be removed in a future cleanup.

### Test Infrastructure
- `FakeFavouriteAppRepository` — in-memory fake for unit tests.
- `FakeAppRepository` — returns a fixed app list for instrumented tests.
- `TestFocusPhoneApp` / `TestRunner` — custom application and runner that allow repository substitution before Activities launch.
- `AppContainer` supports constructor injection of fake repositories.

### Coverage (Fase 2)
- **Unit:** `ProfilePreferences` (pure blocking logic, detox expiration, SHA-256→PBKDF2), `ThemePreferences` (theme resolution, auto day/night, hex fallback), `ToggleFavouriteAppUseCase`, `UpdateAppsOrderUseCase`, `FuzzyMatcher`, `AppScheduleEvaluator` (windows, midnight wrap, weekday filtering, daily limits, serialization), `PasswordHasher` (PBKDF2 payloads, legacy upgrade, malformed input), `AppFolderSerializer` (round trip, name sanitizing, corrupted lines, alphabetical order).
- **Instrumented:** Onboarding flow (3 steps), app selection (10-app limit), profile switching (Normal ↔ Work), detox password verification & lockout (4 failed attempts), schedule persistence, usage accounting and schedule-password cooldown (`AppScheduleTest`).

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
- **Work**: blocks manually blocked apps + apps matching user-configurable keywords (`ProfilePreferences.getWorkKeywords()`). Defaults are pre-populated from the former hardcoded list; users can edit them in Settings.
- **Detox**: blocks **all** apps except FocusPhone itself. Requires a password (min 4 chars) to exit early. Auto-reverts to Normal when the timer expires.

### 5. Schedule-Based Blocking (independent of profiles)

Any app can carry an [AppScheduleRule]: an allowed time window (with weekday selection,
windows may wrap past midnight) and/or a daily usage allowance that resets at local
midnight. Schedules are evaluated **in addition to** the active profile — a Normal-profile
device still enforces them — and the pure decision lives in `AppScheduleEvaluator` so it is
fully unit tested. Every in-app launch path calls `BaseActivity.enforceAppSchedule()`;
`FocusAccessibilityService` covers launches that bypass FocusPhone and is the only
component that can measure foreground time, so daily limits need it enabled while time
windows work without it.

### 6. App-Drawer Folders

The "all apps" drawer can group apps into user-created folders. Folders are stored in
`AppFolderPreferences` (AES-256/GCM through `EncryptedPrefs`, like every other user
list) and always rendered **before** the plain apps, sorted alphabetically and
case-insensitively — there is no manual folder order to store. A package belongs to at
most one folder; that invariant is enforced in the preference layer, not in the UI, so
every entry point (drag & drop, the long-press menu, the folder picker) behaves the same.

Two gestures fill a folder: dropping an app onto it, or the "move to folder" entry of
the long-press menu. Both share one gesture pipeline — `FolderDragCallback` claims the
long press of every app row (`ItemTouchHelper` always does), so a press released without
travelling past the touch slop is reported as a long press and opens the options dialog,
while a press that travels resolves a drop target from the centre of the dragged row.
The whole region of an expanded folder accepts a drop, and releasing over the plain app
list takes an app back out of its folder. Searching hides the folder headers and lists
every match flat.

### 7. Auto Day/Night Theme
`ThemePreferences.THEME_AUTO` resolves dynamically to `THEME_SLATE` (dark) when the system is in night mode, or `THEME_OFF_WHITE` (light) otherwise. `BaseActivity` tracks the *effective* resolved theme so that `onResume()` detects system day/night changes automatically.

### 8. Migration from SharedPreferences to Room
`FavouriteAppRepositoryImpl` contains a `migrateFromSharedPreferences()` method that runs once on init, converting the old pipe-delimited string format into Room entities and then deleting the old key.

---

## Security Considerations

### Permissions (AndroidManifest.xml)
| Permission | Reason |
|------------|--------|
| `QUERY_ALL_PACKAGES` | List all installed apps for the app drawer and selector. |
| `BIND_ACCESSIBILITY_SERVICE` | Declared on `FocusAccessibilityService` to intercept app launches system-wide when Detox/Work profiles are active. |
<!-- Only QUERY_ALL_PACKAGES remains; unused permissions removed for Play Store compliance -->

### Detox Password Behavior
- The detox password is hashed with **PBKDF2-HMAC-SHA256** (100 000 iterations, 128-bit random salt) before storage. Legacy SHA-256 hashes (no salt) are detected and transparently upgraded on successful verification.
- After 4 incorrect password attempts within a 30-minute sliding window, detox is **force-disabled** for safety and the password is cleared. The attempt counter is persisted across UI sessions.
- `DetoxBlockedActivity` and password dialogs set `FLAG_SECURE` to prevent screenshots/screen recording while the user is entering credentials or viewing the block screen.
- All values in `focusphone_profile` and `focusphone_custom_labels` are encrypted at rest with **AES-256/GCM** via Android Keystore (`SecureStorageHelper`).

### Schedule Password Behavior
- The schedule password is hashed with the shared `PasswordHasher` (PBKDF2-HMAC-SHA256,
  100 000 iterations, 128-bit random salt), exactly like the detox password.
- Unlike detox, wrong attempts **never** clear the password or the rules: after 5 failures
  the settings enter a 15-minute cooldown (`AppSchedulePreferences.LOCKOUT_DURATION_MS`)
  and `verifyPassword()` refuses every attempt until it expires. Erasing the configuration
  on failed attempts would turn the lock into a trivial bypass, whereas a detox session is
  time-boxed and must never trap the user.
- The password dialogs set `FLAG_SECURE`, and the rule list is not rendered at all until
  the password check succeeds.
- `ScheduleBlockedActivity` intentionally offers no unlock button: rules can only be
  changed from the password-protected settings screen.

### System-Level Blocking (`FocusAccessibilityService`)
- An optional `AccessibilityService` (`FocusAccessibilityService`) intercepts `TYPE_WINDOW_STATE_CHANGED` events. When a blocked app is launched from outside FocusPhone (notifications, recents, voice assistants), the service immediately launches `DetoxBlockedActivity` on top, closing the bypass vector that existed when the process was killed.
- The service is **not** enabled by default; the user is prompted to activate it when starting a Detox session.
- The service also owns per-app usage accounting for daily limits: it accumulates foreground
  seconds, pauses while the screen is off, persists them through `AppSchedulePreferences`,
  and re-checks the active rule every 30 seconds (or exactly when the allowance would run
  out) so a session is interrupted mid-use.

### Backup Policy
- `android:allowBackup="false"` is set globally to prevent `adb backup` and cloud backup from leaking user metadata (favourite apps, themes, custom labels, profile state).

### Custom Theme Colors
- Custom hex colors are validated with a strict regex (`^#[0-9A-Fa-f]{6}$`) before parsing. Invalid values fall back to safe defaults (`#181818` / `#FAFAFA`).

---

## Common Tasks for Agents

### Adding a New Setting
1. Add preference keys to the appropriate `*Preferences.kt` object in `data.preferences`.
2. Expose any repository/use-case changes needed.
3. Choose the correct sub-Activity under `ui.settings` (e.g. `AppearanceSettingsActivity`, `FocusSettingsActivity`) and wire the UI there using the reusable helpers in `SettingsUiHelper.kt`.
4. If the setting affects the theme or font scale, ensure `BaseActivity.onResume()` detects the change and calls `recreate()`.
5. Only add a new top-level category in `SettingsActivity` if the setting introduces a completely new domain; otherwise keep it inside an existing sub-screen.

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
