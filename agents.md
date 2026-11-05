# FocusPhone — Agent Guide

> **FocusPhone** is a minimalist Android launcher designed to reduce digital distraction. It replaces the device's home screen with a clean, text-only interface showing a clock, a calendar, and up to 10 favourite apps. It supports contextual profiles (Normal, Work, Detox), a "Conscious Pause" breathing timer before opening apps, and extensive theming.

---

## Project Overview

| Property | Value |
|----------|-------|
| **Package** | `app.focusphone` |
| **Version** | 1.0.6 |
| **Language** | Kotlin |
| **Min SDK** | 24 |
| **Target / Compile SDK** | 36 |
| **Java Compatibility** | 11 |
| **AGP** | 9.2.1 |

The project is a single-module Android application (`:app`) with no library modules. It uses the **Android Gradle Plugin 9.x** and requires a **JDK 21** toolchain, selected through Gradle's daemon toolchain criteria (`gradle/gradle-daemon-jvm.properties`) — never a machine-specific path.

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
    │   ├── FavouriteReorderCallback.kt # Hold in place = options, hold and move = reorder
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
    │   ├── ConsciousPauseActivity.kt # One guided breath before launching the target app
    │   └── BreathingOrbView.kt       # Draws the breath; the screen owns the clock
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
    ├── util/
    │   ├── PressFeedback.kt      # Parks a row's press animator while a drag owns it
    │   └── WindowMetrics.kt      # Current window width (multi-window / foldable safe)
    └── widget/
        ├── AnalogClockView.kt
        ├── ClockRenderer.kt          # Renders the home clock in its selected style
        ├── AppWidgetHostManager.kt   # Single AppWidgetHost: view ownership, home slot, binding, configuration
        ├── WidgetHostContext.kt      # Factory-free inflater for RemoteViews (see note below)
        ├── HomeWidgetHostView.kt     # Host view: themed error, fade-in, long press without stealing taps
        ├── WidgetSurface.kt          # One rendering of the home widget: HOME (live) or PREVIEW (scaled)
        ├── WidgetScaleFrame.kt       # Renders a widget at its real size and scales it into a box
        ├── WidgetSizing.kt           # Pure size maths: cell spans, default and allowed heights
        ├── WidgetHeights.kt          # Height from preference + provider info for the current screen
        ├── HomeWidgetActions.kt      # Shared options: change, adjust height, widget settings, remove
        ├── WidgetCatalog.kt          # Picker rows: grouped by app, sorted, fuzzy-searched
        ├── WidgetPickerActivity.kt   # Picker: bind, configure, save
        └── WidgetPickerAdapter.kt    # Picker cards with live/static previews
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
- There is no `values-night/`: day/night is handled manually via `THEME_AUTO` resolving to explicit themes.
- `res/drawable/` — custom shapes, theme preview circles, tutorial icons.
- `res/anim/fp_fade_in.xml` & `fp_fade_out.xml` — used by `BaseActivity` for consistent Activity transitions (declared once with `overrideActivityTransition` on API 34+).
- `res/animator/fp_press_feedback.xml` — calm press feedback for text rows; timings live in `res/values/motion.xml`.

> **Hosting widgets:** always create widget views through `AppWidgetHostManager.createView`
> (or `WidgetSurface`), never `AppWidgetHost.createView(activity, …)` directly. RemoteViews inflate
> with the host context's `LayoutInflater`; an Activity's carries AppCompat's factory, which turns
> every provider `TextView` into an AppCompat view reading FocusPhone's attribute IDs against the
> provider's resources, and the widget fails with "can't convert value to dimension".
> `WidgetHostContext` supplies a factory-free inflater. Tree walks that restyle a screen must also
> skip `AppWidgetHostView` subtrees: their content belongs to the provider.

> **Launching apps:** always go through `BaseActivity.openApp` / `openShortcut`. They apply the
> profile block, the schedule and the conscious pause in the right order; never call
> `getLaunchIntentForPackage` + `startActivity` directly from a screen.

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

# Lint (must report 0 errors; release builds fail on any lint error)
./gradlew :app:lintDebug

# Kotlin static analysis: ktlint (format) + detekt (code smells), both must be clean
./gradlew staticAnalysis

# Autofix every formatting violation ktlint can fix on its own
./gradlew :app:ktlintFormat

# Clean
./gradlew clean
```

### Build Configuration Notes
- `viewBinding = true` and `buildConfig = true` are enabled.
- Release builds use ProGuard (`proguard-rules.pro`) with `isMinifyEnabled = true` and `isShrinkResources = true`.
- Room uses `kapt` (legacy annotation processor) because AGP 9.2.1 is used.
- The Gradle daemon runs on a JDK 21 chosen by `gradle/gradle-daemon-jvm.properties` (regenerate with `./gradlew updateDaemonJvm --jvm-version=21`); do not reintroduce `org.gradle.java.home`.
- Lint runs on release builds (`checkReleaseBuilds = true`, `abortOnError = true`) and there is no lint baseline: fix new errors at the source.
- CI (`.github/workflows/android-ci.yml`) runs `staticAnalysis` (ktlint + detekt) first, then unit tests, `lintDebug` and an unsigned `assembleRelease`, on every push and pull request.
- **Dependencies are updated by hand.** There is no automated update of `gradle/libs.versions.toml`: `.github/dependabot.yml` sets `open-pull-requests-limit: 0` for the `gradle` and `github-actions` ecosystems, `renovate.json` is `"enabled": false`, and `.github/workflows/dependency-policy.yml` fails any pull request authored by `dependabot[bot]` / `renovate[bot]` or coming from a `dependabot/*` / `renovate/*` branch. To bump a library, edit the `[versions]` block by hand, run `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease`, add the changelog entry and commit with the `changes` prefix (see `documentation/en/developers/03-environment-and-build.md`, *manual dependency updates*). Never merge a bot pull request or bypass that guard.
- The Gradle wrapper JAR is versioned; release outputs under `app/release/` are not.
- Release signing config is conditional: set `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` in `gradle.properties` (or as project properties) to enable release signing. Without these properties, the release build uses no signing config and can still be assembled for local testing.

---

## Code Style Guidelines

### Language & Tone
- **All code comments and KDoc are written in English.**
- UI text (strings) is written in lowercase English with a minimalist, calm tone (e.g., `"no apps selected.\ntap the menu to add apps."`).
- Some older inline comments may mix Spanish; new code should remain in English.

### Formatting Conventions
Formatting is **enforced, not conventional**: ktlint reads `.editorconfig` at the repository root and CI fails on any violation. Run `./gradlew :app:ktlintFormat` before committing and the rest of this section takes care of itself.

- 4-space indent, LF endings, UTF-8, final newline, no trailing whitespace, **140-column limit**.
- No wildcard imports; imports in lexicographic order.
- **No trailing commas.** `ij_kotlin_allow_trailing_comma*` are `false`, so ktlint keeps them out.
- Four rules are switched off on purpose, each with its reason written in `.editorconfig`: `function-signature`, `class-signature`, `argument-list-wrapping` and `multiline-if-else` (where a signature or an argument list breaks is an authored decision), plus `no-multi-spaces` (the codebase aligns assignments into columns, e.g. `versionCode   = 3`).
- The style is ktlint's `intellij_idea`, which matches the IDE's "Kotlin official" formatter that `kotlin.code.style=official` in `gradle.properties` already selects — so *Reformat Code* in Android Studio and `ktlintFormat` agree.
- **Section dividers** in classes use a consistent pattern:
  ```kotlin
  // ── Section Name ──────────────────────────────────────────────────
  ```
- Package naming: `app.focusphone.{layer}.{feature}`.
- Activities use `dp(value: Int)` helper methods for pixel conversion.
- All Activities extend `BaseActivity`.

### Static Analysis
Three tools, each with a job the others do not do, and **none of them has a baseline file**: a finding is fixed at the source, or the rule is turned off in config with the reason written down next to it.

| Tool | Covers | Config | Task |
|---|---|---|---|
| Android Lint | Android/API correctness, resources, manifest | `app/build.gradle.kts`, `lint { }` | `:app:lintDebug` (also runs in `assembleRelease`) |
| ktlint | Kotlin formatting | `.editorconfig` (repository root) | `:app:ktlintCheck` / `:app:ktlintFormat` |
| detekt | Kotlin code smells and complexity | `config/detekt/detekt.yml` | `:app:detekt` |

- `./gradlew staticAnalysis` (root task) runs ktlint and detekt together; that is what CI runs.
- The detekt config is `buildUponDefaultConfig = true`, so it only records **deviations** from detekt's defaults. Never paste the full default config into it — new rules from an upgrade would be silently dropped. `config.validation = true` makes a stale rule name fail the build instead of being ignored.
- Several complexity thresholds (`LongMethod` 130, `CyclomaticComplexMethod` 40, `LargeClass` 1800…) are deliberately set just above the worst case in `main` today, so they work as a **ratchet**: the existing screens are tolerated, nothing may grow past them. Lower them when a screen is split; never raise them to make a new finding go away.
- `TooGenericExceptionCaught` is off (a launcher must survive whatever an OEM system service throws), but `SwallowedException` stays on: every `catch` must log, rethrow, or name its binding `_` / `ignored` / `expected` to say the exception is dropped on purpose.
- detekt's bundled `formatting` ruleset (its copy of the ktlint rules) is **not** added as a dependency: formatting belongs to ktlint alone, or the two engines report everything twice and can disagree after an upgrade.
- The ktlint engine version is pinned in `gradle/libs.versions.toml` (`ktlint`), separately from the Gradle plugin (`ktlintGradle`), so the formatter never moves on its own and rewrites files CI previously accepted.

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


### Test Infrastructure
- `FakeFavouriteAppRepository` — in-memory fake for unit tests.
- `FakeAppRepository` — returns a fixed app list for instrumented tests.
- `TestFocusPhoneApp` / `TestRunner` — custom application and runner that allow repository substitution before Activities launch. The test app keeps a single `AppContainer` (rebuilt only when a fake is swapped), mirroring production.
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
component that can measure foreground time, so daily limits need it enabled. Time
windows work without it only for launches made from FocusPhone itself; launches from
Recents and suggested shortcuts require the accessibility service for every rule.

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
- Wrong passwords follow the shared `PasswordAttemptPolicy` (the same one schedules use): after 5 failures every attempt — even the correct password — is refused for a 15-minute cooldown. Nothing is ever cleared on failure; a detox session only ends early with the right password, and always ends on its own when its timer expires, so nobody can be trapped. The counter is encrypted and persisted across process restarts.
- Password hashing (PBKDF2) always runs off the main thread.
- `DetoxBlockedActivity` and password dialogs set `FLAG_SECURE` to prevent screenshots/screen recording while the user is entering credentials or viewing the block screen.
- All values in `focusphone_profile` and `focusphone_custom_labels` are encrypted at rest with **AES-256/GCM** via Android Keystore (`SecureStorageHelper`).

### Schedule Password Behavior
- The schedule password is hashed with the shared `PasswordHasher` (PBKDF2-HMAC-SHA256,
  100 000 iterations, 128-bit random salt), exactly like the detox password.
- Wrong attempts **never** clear the password or the rules: after 5 failures
  the settings enter a 15-minute cooldown (`AppSchedulePreferences.LOCKOUT_DURATION_MS`)
  and `verifyPassword()` refuses every attempt until it expires. Erasing the configuration
  on failed attempts would turn the lock into a trivial bypass. Detox follows the same
  `PasswordAttemptPolicy`.

### Encrypted storage failures
- `EncryptedPrefs.read()` distinguishes `Absent`, `Present`, `Unavailable` (Keystore key
  temporarily unusable — never overwrite) and `Lost` (payload can never be decrypted —
  treated as absent so the user can rebuild). Writers of user-authored lists (rules,
  folders, blocked apps) refuse to run while a value is `Unavailable`.
- Custom labels detect the legacy plain-text format by shape
  (`SecureStorageHelper.looksEncrypted`), never by a failed decryption.

### Wallpapers
- Picked images are copied (downscaled, EXIF-rotated) into `filesDir/wallpapers` by
  `WallpaperStorage`; preferences store the `file://` URI of the copy. The Photo Picker
  grant dies with the process, so a `content://` URI must never be stored.
- Assign or remove an image only through `WallpaperPreferences.assignWallpaper` (it honours
  the shared-wallpaper mode) inside `WallpaperStorage.replace`/`update`, which delete the
  copies no preference refers to any more. Switch the shared mode with
  `WallpaperPreferences.setSharedMode` (never the raw flag): it seeds the shared slot from the
  image on screen and hands it back to each screen when turned off.
- `BaseActivity.applyWallpaper()` can be called on a live screen after any wallpaper change:
  it settles the content background, scrim and colours in place. `colorizeViewTree` only
  repaints backgrounds drawn in a theme colour and remembers each view's colour role, so it
  is safe to run repeatedly — never paint every container opaque again.
- The password dialogs set `FLAG_SECURE`, and the rule list is not rendered at all until
  the password check succeeds.
- `ScheduleBlockedActivity` intentionally offers no unlock button: rules can only be
  changed from the password-protected settings screen.

### System-Level Blocking (`FocusAccessibilityService`)
- An optional `AccessibilityService` (`FocusAccessibilityService`) intercepts `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOWS_CHANGED` events and checks the active window's package. When a blocked app is launched from outside FocusPhone (notifications, recents, suggested shortcuts, voice assistants), the service launches the appropriate block screen on top.
- The service is **not** enabled by default; the user is prompted to activate it when starting a Detox session.
- The service also owns per-app usage accounting for daily limits: it accumulates foreground
  seconds, pauses while the screen is off, persists them through `AppSchedulePreferences`,
  and re-checks the active rule every 30 seconds (or exactly when the allowance would run
  out) so a session is interrupted mid-use.

### Backup Policy
- `android:allowBackup="false"` is set globally to prevent `adb backup` and cloud backup from leaking user metadata (favourite apps, themes, custom labels, profile state).
- From Android 12 `allowBackup` no longer covers device-to-device transfer, so `res/xml/data_extraction_rules.xml` (referenced by `android:dataExtractionRules`) excludes every domain from both cloud backup and device transfer. Encrypted values are bound to a Keystore key that never leaves the device, so a restored copy could not be read anyway.

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

---

## Commit, PR, Merge & Release Guidelines

> `.docs/` is gitignored (not present in every clone or agent environment), so the
> full content of `.docs/commit-guidelines.md` and `.docs/pr-merge-release-guidelines.md`
> is duplicated below verbatim. Both `agents.md` and `claude.md` carry the same copy so
> any agent has it regardless of which file it reads first. If `.docs/` is present
> locally, treat it as the source of truth and keep this copy in sync with it.

### `.docs/commit-guidelines.md`

# COMMIT GUIDELINES

> instrucciones para cualquier agente de ia (o persona) que redacte el titulo
> y la descripcion de un commit de git en este repositorio. se aplica siempre
> que se pida algo equivalente a:
>
> *"sacame el titulo y la descripcion para el commit con los cambios
> realizados desde el ultimo commit. fijate en los anteriores commits para
> que tengan el mismo formato."*
>
> y, de forma general, **cada vez que se vaya a crear un commit en este
> repositorio**, se haya pedido explicitamente o no.

---

#### FORMATO FIJO DEL TITULO

```
<prefijo> | <resumen corto en imperativo>
```

el separador es siempre espacio, barra vertical, espacio: ` | `. el
`<prefijo>` es siempre una de estas cinco palabras, en minusculas, y nunca
se combinan ni se inventan otras nuevas:

| prefijo | cuando usarlo |
|---|---|
| **update** | trabajo normal hacia adelante: se añade o mejora una funcionalidad, contenido o documento existente. es el prefijo por defecto. |
| **fix** | se corrige un error o comportamiento incorrecto detectado durante el desarrollo, sin urgencia de publicacion inmediata. |
| **hotfix** | se corrige algo urgente que afecta a una version ya publicada o a un bloqueo critico. debe usarse solo cuando la correccion es realmente urgente. |
| **changes** | cambios estructurales o no funcionales: reorganizar carpetas, renombrar ficheros, tocar configuracion o tooling, dependencias. no anade funcionalidad ni corrige un bug. |
| **changelog** | el unico proposito del commit es editar `changelog.md` (por ejemplo, al cerrar una version). |

ejemplos de titulos correctos:

```
update | añadir vista previa de mapeo de columnas al asistente de importación csv
fix | detección de duplicados fallando en filas con importes negativos
hotfix | fallo al iniciar cuando falta el archivo de perfil de importación
changes | reorganizar la documentación en la subcarpeta project-context
changelog | cerrar la sección de changelog para la versión mvp fase 0
```

---

#### FORMATO DE LA DESCRIPCION (CUERPO)

- el cuerpo es siempre **una lista de markdown no numerada**: cada item empieza
  por `-`. nunca se usan listas numeradas ni parrafos sueltos.
- cada item es un bloque logico en imperativo, escrito como **una sola linea
  continua**, sin saltos de linea internos: no son esteticos ni utiles, deja
  que el cliente de git lo ajuste. no se dejan lineas en blanco entre items.
- usa markdown dentro de cada item: nombres de ficheros, rutas, comandos e
  identificadores siempre entre backticks.
- explica el que y el por que, no el detalle linea a linea.
- cierra con una linea en blanco y la linea de atribucion que corresponda al
  entorno.

---

#### COMO PRESENTAR EL RESULTADO

al responder con el titulo y la descripcion hay que usar **bloques de codigo
separados**: uno para el titulo del commit y otro para el cuerpo del commit (mas
otro para el fragmento del changelog). nunca se juntan el titulo y el cuerpo en
el mismo bloque.

### `.docs/pr-merge-release-guidelines.md`

# PR, MERGE Y RELEASE GUIDELINES

> instrucciones para cualquier agente de ia (o persona) que redacte el titulo
> y la descripcion de una **pull request**, de un **commit de merge** o de una
> **release de github** en este repositorio, de forma que su contenido lea
> como una extension directa de `changelog.md` en lugar de un formato distinto
> por cada canal. se aplica siempre que se pida algo equivalente a:
>
> *"sacame el titulo y la descripcion para la pull request / el merge / la
> release con los cambios de esta version. usa el mismo formato que
> `changelog.md`."*
>
> y, de forma general, **cada vez que se vaya a abrir una pull request, hacer
> un merge o publicar una release en este repositorio**, se haya pedido
> explicitamente o no.

este documento asume conocido el formato de `changelog.md` (descrito en
`agents.md` y `claude.md`, seccion "formato de las secciones de
`changelog.md`") y el de los commits (`.docs/commit-guidelines.md`). no lo
repite salvo lo estrictamente necesario.

---

#### PRINCIPIO GENERAL

pull requests, merges y releases **no inventan su propio formato**: reusan
literalmente el contenido y las reglas de estilo de `changelog.md`.

- **minusculas y sin acentos** en todo el contenido; los titulos/encabezados
  van en mayusculas y sin acentos.
- **categorias cerradas**, las mismas cinco de `changelog.md` y en el mismo
  orden — nunca, cambios, corregido, eliminado, seguridad
  (added / changed / fixed / removed / security) — omitiendo la que no tenga
  items.
- **una linea por item**, en imperativo, con nombres de ficheros, rutas,
  comandos e identificadores entre backticks.
- las fechas usan el formato `aaaa-mm-dd`.
- las entradas `[online]` / `[offline]` de `changelog.md` (cambios que solo
  afectan a una de las dos versiones de la app) se conservan tal cual en pr,
  merge y release: no se separan ni se traducen a otro marcador.

la fuente de verdad es siempre `changelog.md`: antes de redactar una pr, un
merge o una release, la seccion `## SIN PUBLICAR / UNRELEASED` (o la version
ya cerrada correspondiente) debe estar actualizada. el contenido de pr, merge
y release se **extrae** de ahi, no se redacta desde cero ni en paralelo.

---

#### 1. PULL REQUESTS

hay dos tipos de pr en este repositorio y cada una tiene su propio formato de
titulo; la descripcion es igual para las dos.

##### 1.1 titulo

**pr de trabajo normal** (una rama `feature/...` o `fix/...` contra
`testing-branch`, o cualquier pr que no cierra una version): mismo formato
que el titulo de un commit, definido en `.docs/commit-guidelines.md`:

```
<prefijo> | <resumen corto en imperativo>
```

usando el mismo prefijo (`update`, `fix`, `hotfix`, `changes`, `changelog`) y
las mismas reglas — si la pr agrupa varios commits, el resumen describe el
conjunto, no cada commit por separado.

**pr de publicacion de version** (`testing-branch` → `release-branch` /
`main`, cierra una version de `changelog.md`): sigue el patron ya usado en
los merges anteriores del repositorio (`release version 1.6 - supabase cloud
sync, onyx theme and task detail redesign`):

```
release version <x.y.z> - <resumen corto en imperativo>
```

el resumen corto resume las 2-3 entradas mas relevantes de la version, igual
que en el ejemplo anterior.

##### 1.2 descripcion

la descripcion es el bloque `### CASTELLANO` + `### ENGLISH` de la seccion
correspondiente de `changelog.md` (la version que cierra la pr, o el
subconjunto de `## SIN PUBLICAR / UNRELEASED` que aporta esa pr), pegado tal
cual, con sus encabezados `####` de categoria:

```
### CASTELLANO

#### <categoria>
- <cambio en una linea>

### ENGLISH

#### <category>
- <same change, one line>
```

si la pr solo aporta una parte de `## SIN PUBLICAR / UNRELEASED` (por
ejemplo, varias pr pequenas alimentan la misma version antes de cerrarla),
la descripcion incluye solo los items que introduce esa pr, no la seccion
entera.

no se anaden parrafos sueltos, capturas de pantalla incrustadas como texto,
ni resumenes adicionales fuera de la lista: si hace falta contexto extra (por
ejemplo, capturas de pantalla de cambios visuales, ver `agents.md` seccion
10), se anade **despues** de la lista, nunca sustituyendola.

---

#### 2. COMMIT DE MERGE

github genera el commit de merge a partir del titulo y la descripcion de la
pr automaticamente (`Merge pull request #<n> from <rama>` como primera linea,
seguido del titulo y cuerpo de la pr). por tanto:

- si la pr cumple las secciones 1.1 y 1.2, el commit de merge ya cumple este
  formato sin trabajo adicional.
- usa siempre **"create a merge commit"** al fusionar (nunca squash ni
  rebase): el historial de commits individuales, redactado segun
  `.docs/commit-guidelines.md`, debe conservarse completo junto al resumen de
  la pr en el commit de merge.
- si el merge se hace manualmente por git (`git merge --no-ff`), el mensaje
  del commit de merge se redacta a mano siguiendo el mismo titulo (seccion
  1.1) y el mismo cuerpo (seccion 1.2) que llevaria la pr equivalente.

---

#### 3. RELEASES DE GITHUB

las releases de github son **en ingles unicamente** (no bilingues, a
diferencia de `changelog.md` y de las pr): son la cara publica del proyecto
de cara a usuarios que no necesariamente leen castellano.

##### 3.1 titulo de la release

```
v<x.y.z> — <resumen corto en ingles, minusculas, sin acentos>
```

ejemplo (formato ya usado en `.docs/releases-docs/`):

```
v1.7 — 100% offline, reminders redesign & settings categories
```

##### 3.2 descripcion de la release

el cuerpo reutiliza literalmente el bloque `### ENGLISH` de la version
cerrada en `changelog.md`, con sus categorias en mayusculas y sin acentos,
igual que en el changelog:

```
#### ADDED
- <change in one line>

#### CHANGED
- <change in one line>

#### FIXED
- <change in one line>

#### REMOVED
- <change in one line>

#### SECURITY
- <change in one line>
```

se omite la categoria que no tenga items, igual que en `changelog.md`. no se
reescribe el contenido en parrafos narrativos con negritas ni se reordenan
las categorias: la release debe poder generarse copiando y pegando el bloque
`### ENGLISH` de la version correspondiente.

> nota: los releases anteriores a esta guia (incluidos los de
> `.docs/releases-docs/release-1.7/`) usan un formato narrativo distinto
> (parrafos con titulo en negrita en vez de las cinco categorias). son
> historicos y no se reescriben con esta regla; a partir de esta guia, toda
> release nueva usa el formato de categorias descrito arriba.

##### 3.3 tag

el tag de git asociado a la release es siempre `v<x.y.z>` (mismo numero de
version que la cabecera de `changelog.md`, sin sufijos adicionales).

---

#### FLUJO RESUMIDO

1. durante el desarrollo, cada commit sigue `.docs/commit-guidelines.md` y
   cada cambio relevante se anade a `## SIN PUBLICAR / UNRELEASED` en
   `changelog.md` en su categoria correspondiente.
2. cada pr (normal o de version) usa el titulo y la descripcion definidos en
   la seccion 1, extraidos de `changelog.md`.
3. al fusionar, se usa "create a merge commit" para que el commit de merge
   herede el formato de la pr (seccion 2).
4. al cerrar una version: se renombra `## SIN PUBLICAR / UNRELEASED` a
   `## [x.y.z] - aaaa-mm-dd` en `changelog.md` (commit con prefijo
   `changelog`), se abre la pr de publicacion de version (seccion 1.1), y al
   fusionarla se crea la release de github (seccion 3) con el tag `v<x.y.z>`.
