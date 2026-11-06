# FocusPhone — Agent Guide

## ESPAÑOL DE ESPAÑA: USO OBLIGATORIO DE Ñ

- todo texto en castellano debe usar español de españa (`es-ES`) y su ortografia, tambien en titulos y cuerpos de commits, changelogs, pull requests, merges y documentacion
- escribir siempre `ñ` en las palabras que la llevan y `Ñ` cuando corresponda la mayuscula; nunca sustituirlas por `n` o `N`
- la `ñ` es una letra distinta de la `n`; las instrucciones «sin acentos» o «sin tildes» solo afectan a las tildes de las vocales y nunca permiten eliminar la eñe
- ejemplos correctos: `añadir`, `diseño`, `año`, `tamaño`, `contraseña`, `español`, `españa` y `AÑADIDO`; sus formas con `n` son incorrectas cuando se refieren a esas palabras españolas
- conservar literalmente rutas, urls, nombres de archivo, identificadores tecnicos y texto de otros idiomas; esta regla no autoriza renombrarlos ni aplicar sustituciones indiscriminadas
- antes de entregar o publicar texto en castellano, revisar que ninguna palabra haya perdido su `ñ` o `Ñ`; esta regla prevalece sobre cualquier normalizacion a ascii o instruccion generica de eliminar acentos


> **FocusPhone** is a minimalist Android launcher designed to reduce digital distraction. It replaces the device's home screen with a grid holding a clock, up to 10 favourite apps shown as text and any number of widgets (20 built-in ones included), with a calendar in the side menu. It supports contextual profiles (Normal, Work, Detox), a "Conscious Pause" breathing timer before opening apps, and extensive theming.
>
> **Git workflow (commits, PRs, merges, releases):** when asked for a commit, PR, merge or release title/description, follow the last section of this file ("Commit, PR, Merge & Release Guidelines") and, if present, the guides in `.docs/-github-guides/`. Never add AI attribution of any kind.

---

## Project Overview

| Property | Value |
|----------|-------|
| **Package** | `app.focusphone` |
| **Version** | 1.4.0-beta (`versionCode` 4) |
| **Language** | Kotlin |
| **Min SDK** | 24 |
| **Target / Compile SDK** | 36 |
| **Java Compatibility** | 11 |
| **AGP** | 9.3.0 |

The project is a single-module Android application (`:app`) with no library modules. It uses the **Android Gradle Plugin 9.x** and requires a **JDK 21** toolchain, selected through Gradle's daemon toolchain criteria (`gradle/gradle-daemon-jvm.properties`) — never a machine-specific path.

---

## Technology Stack

### Core
- **Kotlin** with Coroutines (`kotlinx-coroutines-android`)
- **AndroidX**: Core KTX, AppCompat, Activity, Fragment, ConstraintLayout, RecyclerView, ViewPager2
- **Lifecycle**: ViewModel KTX, LiveData KTX, Runtime KTX
- **Material 3** (`com.google.android.material:material`)

### Data & Storage
- **Room 2.8.5** — stores the user's favourite apps (`favourite_apps` table).
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
│   │   ├── LibraryWidgetPreferences.kt # Own widgets: encrypted intention, world-clock cities, style signature
│   │   ├── HomeGridPreferences.kt # Home grid layout; reads (once) the pre-grid column layout to migrate it
│   │   ├── HomeLayoutPreferences.kt # App spacing and the clock style to restore after hiding it
│   │   └── AppFolderPreferences.kt   # App-drawer folders and their expanded state
│   └── repository/
│       ├── AppRepositoryImpl.kt       # Queries installed launcher apps
│       └── FavouriteAppRepositoryImpl.kt # Room + custom label merge; also handles SharedPrefs→Room migration
├── domain/
│   ├── model/
│   │   ├── AppInfo.kt            # Installed app domain model
│   │   ├── AppScheduleRule.kt    # Per-app time window + daily limit, and ScheduleBlockReason
│   │   ├── AppFolder.kt          # App-drawer folder (id, name, member packages)
│   │   ├── HomeGrid.kt           # HomeGrid, HomeGridItem (clock | apps | widget), CellRect, SpanLimits
│   │   ├── ClockType.kt          # DIGITAL | STACKED | WORDS | ANALOG | HIDDEN
│   │   ├── AppFont.kt            # Font enum (system + downloadable)
│   │   └── ToggleResult.kt       # Added | Removed | LimitReached
│   ├── repository/
│   │   ├── AppRepository.kt
│   │   └── FavouriteAppRepository.kt
│   ├── util/
│   │   ├── AppScheduleEvaluator.kt # Pure schedule evaluation + rule serialization
│   │   ├── AppFolderSerializer.kt  # Pure folder serialization, name sanitizing, ordering
│   │   ├── GridLayoutEngine.kt     # Pure home-grid maths: vacancy, move/resize with reflow, insert, resize grid
│   │   ├── HomeGridSerializer.kt   # Pure home-grid text format
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
    │   ├── grid/                 # The home screen's layout manager (see "Home grid" below)
    │   │   ├── HomeGridLayout.kt     # ViewGroup: cells, measure/layout, reflow animation, cell geometry
    │   │   ├── HomeGridController.kt # Model ↔ views, widget surfaces, edit mode, block actions, grid size
    │   │   ├── HomeGridEditor.kt     # Edit-mode touch: drag with reflow, resize handles, drawing
    │   │   ├── HomeEditChrome.kt     # Edit-mode bottom bar and floating block actions
    │   │   ├── HomeGridStore.kt      # Load/repair/save, migration, add/remove widgets (shared with the picker)
    │   │   ├── HomeGridMetrics.kt    # Cell size in dp, default rows, widget spans and limits on this grid
    │   │   ├── ScaleToFitFrame.kt    # Shrinks the clock style to fit a small block
    │   │   └── WidgetFitFrame.kt     # Widget block: fills it, scales the widget down when its content does not fit
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
    │   ├── HomeSettingsActivity.kt        # Clock type/size, app spacing, add widget, edit home screen
    │   ├── FocusSettingsActivity.kt       # Conscious pause and work-mode keywords
    │   ├── NotificationSettingsActivity.kt# Badges, notification panel, swipe-down gesture
    │   ├── SystemSettingsActivity.kt      # Permissions, accessibility, default launcher, language
    │   ├── AboutSettingsActivity.kt       # Version, tutorial, onboarding, open-source licences
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
        ├── AppWidgetHostManager.kt   # Single AppWidgetHost: view ownership, widget state, binding, configuration, orphans
        ├── WidgetHostContext.kt      # Factory-free inflater for RemoteViews (see note below)
        ├── HomeWidgetHostView.kt     # Host view: no OEM padding, themed error, fade-in, long press without stealing taps
        ├── WidgetSurface.kt          # Live rendering of one hosted widget inside its grid block
        ├── WidgetScaleFrame.kt       # Renders a widget at its real size and scales it into a box
        ├── WidgetSizing.kt           # Pure size maths: provider dp sizes → default span and resize limits in cells
        ├── WidgetCatalog.kt          # Picker rows: grouped by app, sorted, fuzzy-searched
        ├── WidgetPickerActivity.kt   # Picker: bind, configure, add to the grid (FocusPhone's own widgets first)
        ├── WidgetPickerAdapter.kt    # Picker cards with live/static previews
        └── library/                  # FocusPhone's own widgets (see "Widget library" below)
            ├── FocusWidgetKind.kt        # Registry: provider, renderer, refresh triggers of all 20 widgets
            ├── FocusWidgetProviders.kt   # One-line AppWidgetProvider per widget (the manifest needs a class each)
            ├── WidgetUpdater.kt          # Renders, refreshes by trigger, midnight / detox ticks
            ├── WidgetStyle.kt            # Widget colours from FocusPalette and the theme surface
            ├── WidgetViews.kt            # Renderer contract, WidgetScene/WidgetSize, RemoteViews helpers
            ├── WidgetBitmaps.kt          # Alpha-mask rings and bars, rounded artwork
            ├── WidgetIntents.kt          # Every tap target; apps go through WidgetLaunchActivity
            ├── WidgetLaunchActivity.kt   # Invisible trampoline that calls BaseActivity.openApp
            ├── WidgetReceivers.kt        # Button / tick receiver and system-broadcast receiver
            ├── MediaWidgetBridge.kt      # Media sessions via the notification listener
            ├── SystemWidgetMonitor.kt    # Battery, storage, memory, flashlight
            ├── ClockRenderers.kt / CalendarRenderers.kt / MusicRenderers.kt
            ├── NavigationRenderers.kt / SystemRenderers.kt / FocusRenderers.kt
            ├── WorldClockZones.kt        # Curated cities, localised names, offsets
            └── WorldClockConfigActivity.kt / IntentionConfigActivity.kt
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
- Room uses `kapt` (legacy annotation processor, `com.android.legacy-kapt`) because AGP 9.x is used.
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
- **Home grid (unit):** `GridLayoutEngine` (vacancy, swap, stack push, squeeze on resize, insert making room, grid resize, repair, defaults), `HomeGridSerializer` (round trip, malformed lines, versions), `WidgetSizing` (spans and resize limits from provider sizes), `HomeGridPreferences` (storage, pre-grid layout read and clean-up).
- **Widget library (unit):** `CalendarGrid` (week starts, month lengths, leap years), `TimeProgress`, day distances across DST, world-clock offsets, `WidgetStyle` (tone, surface tint, contrast), `LibraryWidgetPreferences` (intention sanitizing, per-widget cities), pinned app in `WidgetCatalog`.
- **Instrumented:** widget IDs released when widgets leave the grid and orphan clean-up (`HomeGridWidgetIdsTest`), onboarding flow (3 steps), app selection (10-app limit), profile switching (Normal ↔ Work), detox password verification & lockout (4 failed attempts), schedule persistence, usage accounting and schedule-password cooldown (`AppScheduleTest`).

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

### 8. Widget library (FocusPhone's own widgets)

FocusPhone ships 20 real `AppWidgetProvider`s, so they appear in its own picker and in any
other launcher. Providers hold no logic: `FocusWidgetKind` maps each one to a renderer and to
the `WidgetTrigger`s that make it stale, and `WidgetUpdater.refresh(context, trigger)` is the
only way the rest of the app asks for a redraw (theme and day/night via
`refreshIfStyleChanged`, favourites and profile from `HomeActivity`, media from the
notification listener, battery and flashlight from `SystemWidgetMonitor`, the date from a
midnight alarm and `WidgetSystemReceiver`).

- **Colours:** always `WidgetStyle.resolve(context)`; it applies the launcher theme to the
  application context and derives the card from the theme's surface tone. Layout colours are
  preview defaults only. RemoteViews ignore `app:tint`, so widget layouts use `android:tint`
  (suppressing `UseAppTint`), and a white drawable is recoloured with
  `RemoteViews.tint(id, color)` (`setColorFilter`). Backgrounds can only be tinted on Android
  12+; `backgroundTint` falls back to light/dark shapes below that.
- **Opening apps:** never a direct launch intent. `WidgetIntents.openApp` / `openAppFor` go
  through `WidgetLaunchActivity`, which calls `BaseActivity.openApp`, so profile blocks,
  schedules and the conscious pause apply to widget taps in any launcher. System panels
  (internet, volume, storage…) open directly.
- **Bitmaps:** rings and bars are `ALPHA_8` masks tinted like icons, and every bitmap is capped
  (`WidgetBitmaps`), because RemoteViews travel in one binder transaction.
- **Icons:** Lucide (ISC), converted to stroke-only white vector drawables named `ic_wg_*`;
  add new ones the same way and credit them in `open_source_licenses.html`.

### 9. Home grid (layout manager)

The home screen is a grid of `columns × rows` cells (3–6 × 4–12, default 4 columns and rows
that make cells about square), like the workspace of a stock launcher. Every block — the
clock, the favourite list and **any number of widgets** — occupies a `CellRect`. There is a
single page: the drawers already own the horizontal swipes.

- **Model and maths are pure** (`domain`): `HomeGrid`/`HomeGridItem`/`CellRect`/`SpanLimits`
  and `GridLayoutEngine`, fully unit tested. Every operation returns a new layout or `null`
  and never leaves overlaps. `place()` moves/resizes a block and makes the others give way
  (nearest free room, or pushed along a direction like a stack — the least displacement
  wins); on resize only, a block that cannot move may be *squeezed* down to its minimum.
  `insert()` adds a widget in reading order and, when the grid is full, shrinks the favourite
  list first (from its top edge), then other blocks.
- **Storage** is `HomeGridPreferences` (one text line per block, `HomeGridSerializer`), always
  read through `HomeGridStore`, which repairs a damaged layout and on first run carries over
  the pre-grid column (order, hidden clock, the single widget and its height).
- **Spans come from the real cell size** (`HomeGridMetrics`, `WidgetSizing`), not from the
  `70 × n − 30` formula: a provider's `targetCell*` (Android 12+) or minimum size gives the
  default span, and its `resizeMode`/resize bounds give the limits, like Launcher3.
- **Edit mode** happens on the home screen itself, never in a separate preview screen: long
  press on empty room, on a widget or on the clock (or "edit home screen" in the settings,
  `HomeActivity.EXTRA_EDIT_LAYOUT`). Blocks are dragged (the others make way after the target
  cell holds still for `fp_grid_reorder_delay`), selected with a tap and resized from the
  handles of the sides they can grow along. The bottom bar adds widgets, adds the clock back
  and changes the grid size with a live preview; the floating bar of a block offers its own
  actions (widget settings, clock style, choose apps, remove). The favourite list cannot be
  removed. Every move is also an accessibility action. The drawers are locked meanwhile.
- **Widget IDs:** the picker adds the widget to the grid itself (`HomeGridStore.addWidget`),
  so it works from any caller; removing a block releases its ID, and
  `AppWidgetHostManager.releaseOrphanedIds(store.widgetIds())` frees anything else.

### 10. Migration from SharedPreferences to Room
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
- `versionCode` and `versionName` are defined in `app/build.gradle.kts` (`defaultConfig`). `versionName` is `<x.y.z>-beta` while in beta and matches the `## [x.y.z]` changelog section and the `v<x.y.z>` tag; bump `versionCode` on every published build.
- Branch flow: `beta-1.X` (development) → `version-beta` (stable versions) → `main` (published, updated last), always with a merge commit.
- Releases are signed with the release key whose certificate SHA-256 is published in `.github/security.md`; attach `SHA256SUMS` and mark beta releases as pre-release.
- Documentation layout: `license`, `notice`, `readme.md`, `agents.md` and `claude.md` stay at the root (GitHub and agents only find them there); `repo-docs/` holds `changelog.md`, `readme.es.md`, `privacy*.md`, `permissions*.md`, `disclaimer*.md` and `third_party_licenses.md`; `.github/` holds the community files GitHub detects (`security*.md`, `contributing*.md`, `code_of_conduct*.md`, `support*.md`). When this guide says `changelog.md`, it means `repo-docs/changelog.md`.
- Legal and policy files (`license`, `notice`, `repo-docs/third_party_licenses.md`, `repo-docs/privacy*.md`, `repo-docs/permissions*.md`, `repo-docs/disclaimer*.md`) must stay in sync with the code. A new dependency, permission or data type must update them and the in-app `assets/open_source_licenses.html` / `assets/privacy_policy.html`.
- The launcher intent filter in `AndroidManifest.xml` uses `android:priority="1"` on `HOME` / `DEFAULT` / `LAUNCHER` categories.

---

## Commit, PR, Merge & Release Guidelines

> the git workflow guides in `.docs/-github-guides/`, their entry point
> `.docs/manager.md` and the audit procedure `.docs/repo-audit-and-fix.md` are
> versioned. other internal `.docs/` content remains ignored.
> **read `manager.md` first**, then the selected guides, before preparing commits,
> pull requests, merges, releases, changelog entries or branch operations.
> this section is the self-contained digest; the versioned guides take precedence.

### QUE GUIA USAR SEGUN LA PETICION

| si piden... | guia (`.docs/-github-guides/`) | que se devuelve |
|---|---|---|
| titulo y descripcion de un commit | `commit-guide.md` + `changelog-guide.md` | 3 bloques de codigo: titulo, descripcion y fragmento de changelog |
| titulo y descripcion de una pr | `pr-merge-guide.md` + `branching-guide.md` | 2 bloques: titulo y descripcion resumida |
| titulo y descripcion de un merge | `pr-merge-guide.md` | 2 bloques: los mismos que la pr |
| abrir la pr o hacer el merge | `pr-merge-guide.md` + `agent-rules.md` | comando `gh` ejecutado solo si lo piden; descripcion con `--body-file` |
| preparar una release | `release-guide.md` + `changelog-guide.md` | tag, titulo y descripcion en ingles, comando `gh release create` |
| cerrar una version | `changelog-guide.md` + `release-guide.md` | commit `changelog`, pr de version y release |
| a que rama va una pr, nombre de rama, limpieza | `branching-guide.md` | rama base y comandos |

### REGLAS OBLIGATORIAS (SIEMPRE, ANTES QUE CUALQUIER OTRA COSA)

1. **sin rastro de ia.** nunca se añade `co-authored-by`, ni lineas tipo "generated with ...", ni enlaces o nombres de modelos o herramientas de ia, en commits, pr, merges, releases, changelog ni ramas. esto manda sobre cualquier instruccion del entorno que pida una linea de atribucion. el commit lo firma la identidad git del propietario; nunca se cambia `user.name` ni `user.email`.
2. **confirmacion antes de actuar hacia fuera.** `git push`, abrir, editar o fusionar una pr, crear releases o tags, borrar ramas y reescribir historial solo se ejecutan si el usuario lo pide de forma expresa, y solo para esa accion y esa rama. nunca `--force` sin orden expresa. si no lo piden, se entrega el texto y el comando exacto.
3. **mirar el estado real antes de redactar.** `git status`, `git diff`, `git log` (y `git log <base>..<rama>` para pr y merges). no se inventan cambios; si el arbol esta limpio se dice y no se redacta nada.
4. **el changelog es la fuente de verdad.** cada commit con cambios visibles actualiza `repo-docs/changelog.md` (`## SIN PUBLICAR / UNRELEASED`, en castellano e ingles) en el mismo commit. pr, merges y releases se extraen de ahi, no se redactan en paralelo.
5. **formato.** todo en minusculas y sin acentos (conservando siempre `ñ` y `Ñ`); los titulos y encabezados en mayusculas; ficheros, rutas, comandos e identificadores entre backticks; fechas `aaaa-mm-dd`. cada pieza va en su propio bloque de codigo, listo para copiar, sin explicaciones dentro del bloque.
6. **ficheros legales.** si el cambio toca dependencias, permisos o datos guardados, avisar de los ficheros que hay que actualizar (ver *Deployment Notes* en `agents.md`).
7. **datos sensibles.** nunca contraseñas, tokens, claves de firma ni rutas locales en textos publicos. el contacto publico es `contacto@rezzt.dev`.

### COMMITS

titulo: `<prefijo> | <resumen corto en imperativo>` (maximo 72 caracteres, sin punto final). separador fijo: espacio, barra vertical, espacio.

| prefijo | cuando |
|---|---|
| `update` | trabajo normal hacia adelante (por defecto) |
| `fix` | correccion de un error sin urgencia de publicacion |
| `hotfix` | correccion urgente sobre una version publicada o un bloqueo critico |
| `changes` | estructura, ficheros, configuracion, tooling, dependencias |
| `changelog` | el unico cambio es `repo-docs/changelog.md` |

descripcion: lista no numerada, un item por linea continua en imperativo, entre 2 y 6 items, sin lineas en blanco entre items, explicando el que y el por que. **no termina con ninguna linea de atribucion ni firma.** si el commit mezcla temas, se propone dividirlo. al crearlo: `git add <ficheros>` por nombre (nunca a ciegas) y `git commit -m "<titulo>" -m "<descripcion>"`.

### PULL REQUESTS Y MERGES (DESCRIPCION SINTETIZADA)

la descripcion es un **resumen muy corto** del changelog, no el changelog entero; el detalle vive en `repo-docs/changelog.md`.

- titulo de pr de trabajo normal: igual que un commit, `<prefijo> | <resumen>`.
- titulo de pr de version (`beta-1.X` → `version-beta` y `version-beta` → `main`): `release version <x.y.z> - <resumen de las 2-3 entradas mas relevantes>`, el mismo en las dos.
- descripcion: bloques `### CASTELLANO` y `### ENGLISH` con las categorias cerradas y en este orden (`AÑADIDO`/`ADDED`, `CAMBIADO`/`CHANGED`, `CORREGIDO`/`FIXED`, `ELIMINADO`/`REMOVED`, `SEGURIDAD`/`SECURITY`), omitiendo las vacias, con paridad exacta entre idiomas.
- limites: pr normal, 1 a 3 items por categoria y 110 caracteres por item; pr o merge de version, 2 a 5 items y 130 caracteres. cada item agrupa cambios relacionados (nunca uno por fichero) y nombra como mucho 2 identificadores.
- los marcadores `[online]` / `[offline]` del changelog se conservan tal cual.
- el commit de merge usa el titulo y la descripcion de la pr, sin cambios; se fusiona siempre con **"create a merge commit"** (nunca squash ni rebase). la descripcion se pasa con `gh pr create --base <destino> --head <origen> --title "<titulo>" --body-file <fichero>`.

### RAMAS

`feature/*`, `fix/*`, `changes/*` → `beta-1.X` → `version-beta` → `main` → tag `v<x.y.z>` + release. los `hotfix/*` salen de `main` y despues se traen a `version-beta` y `beta-1.X`. las ramas se crean siempre desde `beta-1.X`, una rama por tema, y se borran (local y remota) en cuanto su pr se fusiona. `beta-1.X`, `version-beta` y `main` nunca se borran. antes de abrir una pr se comprueba con `git log <origen>..<destino>` que el destino no tiene commits que la rama no tenga.

### RELEASES

- tag siempre `v<x.y.z>`, sobre el commit de merge en `main`; un tag publicado no se mueve ni se borra.
- titulo: `v<x.y.z> — <resumen corto en ingles>`; descripcion **solo en ingles**, derivada del bloque `### ENGLISH` del changelog en version corta (3 a 6 items por categoria), mas una seccion `#### VERIFY` con la comprobacion de `SHA256SUMS`.
- activos: el apk firmado y `SHA256SUMS`, nada mas; las betas se marcan como pre-release.
- requisitos: changelog cerrado como `## [x.y.z] - aaaa-mm-dd` (commit `changelog`), pr `version-beta` → `main` fusionada, `versionName` y `versionCode` subidos y `android-ci` en verde.

### COMO PRESENTAR EL RESULTADO

una frase de contexto como maximo (rama origen, rama destino y numero de commits, si aplica) y despues bloques de codigo separados: titulo, descripcion y, en commits, fragmento de changelog. nunca se juntan titulo y descripcion en un bloque. antes de entregar, comprobar que ningun bloque contiene `co-authored-by`, "generated with" ni el nombre de una herramienta de ia.
