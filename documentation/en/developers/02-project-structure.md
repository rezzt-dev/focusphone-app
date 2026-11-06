<div align="center">

# PROJECT STRUCTURE

<sub><a href="../README.md">index</a> · <a href="../../es/desarrolladores/02-estructura-del-proyecto.md">castellano</a> · previous: <a href="./01-architecture.md">architecture</a> · next: <a href="./03-environment-and-build.md">environment and build</a></sub>

</div>

---

## REPOSITORY ROOT

```
focusphone-app/
├── agents.md                # operating guide for ai agents and maintainers
├── changelog.md             # bilingual per-version change history
├── claude.md                # copy of the commit, pr, merge and release rules
├── license                  # non-commercial use licence
├── readme.md                # public project presentation
├── security.md              # security policy and vulnerability reporting
├── documentation/           # this documentation
├── focusphone-project/      # the android gradle project
└── .github/workflows/       # continuous integration
```

> the gradle project is **not** at the root: it lives in `focusphone-project/`. every gradle
> command runs from there.

---

## GRADLE PROJECT

```
focusphone-project/
├── app/
│   ├── build.gradle.kts          # :app module configuration
│   ├── proguard-rules.pro        # r8 rules for release
│   └── src/
│       ├── main/                 # production code and resources
│       ├── test/                 # jvm tests (junit 4, robolectric, mockk, turbine)
│       └── androidTest/          # instrumented tests (espresso, androidjunit4)
├── gradle/
│   ├── libs.versions.toml        # version catalog, the single source of dependencies
│   ├── gradle-daemon-jvm.properties  # toolchain criterion: jdk 21
│   └── wrapper/                  # versioned wrapper, jar included
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

---

## SOURCE PACKAGES

```
app/src/main/java/app/focusphone/
├── FocusPhoneApp.kt              # Application class; exposes the AppContainer
├── di/
│   └── AppContainer.kt           # manual dependency graph
├── data/
│   ├── local/                    # room: database and dao
│   ├── model/                    # room entities
│   ├── preferences/              # 12 synchronous preference objects
│   └── repository/               # repository implementations and the package monitor
├── domain/
│   ├── model/                    # business models
│   ├── repository/               # repository interfaces
│   ├── usecase/                  # use cases
│   └── util/                     # pure logic, no android
├── security/                     # encryption, hashing and attempt policy
├── service/                      # accessibility and notification services
├── ui/
│   ├── base/                     # BaseActivity
│   ├── home/                     # home screen
│   ├── appselect/                # favourites selector
│   ├── allapps/                  # app drawer and folders
│   ├── settings/                 # settings and sub-screens
│   ├── schedule/                 # app schedules
│   ├── onboarding/               # onboarding
│   ├── pause/                    # conscious pause
│   ├── detox/                    # detox block screen
│   ├── theme/                    # palette, contrast and scrim
│   ├── tutorial/                 # contextual tutorial
│   ├── wallpaper/                # wallpaper storage and utilities
│   ├── widget/                   # clocks, external widget and picker
│   └── util/                     # window metrics
└── util/
    └── LauncherUtils.kt          # launcher and uninstall utilities
```

---

## RESPONSIBILITY OF EACH PACKAGE

<details open>
<summary><strong><code>data/</code> — persistence</strong></summary>

<br>

| file | responsibility |
|---|---|
| `local/AppDatabase.kt` | room database singleton |
| `local/FavouriteAppDao.kt` | dao for the `favourite_apps` table |
| `model/FavouriteApp.kt` | room entity |
| `preferences/AppFolderPreferences.kt` | drawer folders; encrypted; exposes `StateFlow`s |
| `preferences/AppSchedulePreferences.kt` | schedule rules, password and usage counters; encrypted |
| `preferences/ConsciousPausePreferences.kt` | conscious pause enablement and duration |
| `preferences/CustomAppLabelsPreferences.kt` | custom labels; encrypted, with plain-text migration |
| `preferences/FontPreferences.kt` | typeface, clock type and size, app and home text size |
| `preferences/GesturePreferences.kt` | swipe-down gesture |
| `preferences/HomeGridPreferences.kt` | the home grid, one line per block (`HomeGridSerializer`); reads and clears the pre-grid layout once |
| `preferences/HomeLayoutPreferences.kt` | the look of the home blocks: app spacing, clock type before hiding it |
| `preferences/LibraryWidgetPreferences.kt` | built-in widget options: world clock cities, the intention text (encrypted) |
| `preferences/NotificationPanelPreferences.kt` | notification panel enablement |
| `preferences/ProfilePreferences.kt` | profile, detox, password, blocked apps and keywords; encrypted |
| `preferences/ThemePreferences.kt` | theme, custom colours, alignment, status bar, onboarding |
| `preferences/TutorialPreferences.kt` | tutorial progress |
| `preferences/WallpaperPreferences.kt` | per-surface wallpapers, shared mode, blur and legibility |
| `repository/AppRepositoryImpl.kt` | installed-app queries with a brief cache |
| `repository/FavouriteAppRepositoryImpl.kt` | room plus label merging and the preferences migration |
| `repository/NotificationRepository.kt` | active notifications, in memory only |
| `repository/PackageChangeMonitor.kt` | global install / uninstall / update signal |

</details>

<details open>
<summary><strong><code>domain/</code> — business rules</strong></summary>

<br>

| file | responsibility |
|---|---|
| `model/AppFolder.kt` | drawer folder |
| `model/AppFont.kt` | available typefaces |
| `model/AppInfo.kt` | installed app |
| `model/AppScheduleRule.kt` | schedule rule and block reason |
| `model/AppShortcut.kt` | app shortcut |
| `model/ClockType.kt` | the five clock styles |
| `model/HomeGrid.kt` | grid model: `HomeGrid`, `HomeGridItem`, `CellRect`, `SpanLimits` (3-6 columns × 4-12 rows) |
| `model/ToggleResult.kt` | result of toggling a favourite |
| `repository/AppRepository.kt` | installed-apps interface |
| `repository/FavouriteAppRepository.kt` | favourites crud interface |
| `usecase/GetAppShortcutsUseCase.kt` | shortcuts via `LauncherApps` |
| `usecase/GetFavouriteAppsUseCase.kt` | observes favourites |
| `usecase/GetInstalledAppsUseCase.kt` | lists installed apps marking the favourites |
| `usecase/RemoveFavouriteAppUseCase.kt` | removes a favourite idempotently |
| `usecase/ToggleFavouriteAppUseCase.kt` | toggles a favourite and enforces the cap of 10 |
| `usecase/UpdateAppsOrderUseCase.kt` | persists a new order without losing favourites |
| `util/AppFolderSerializer.kt` | folder serialization and name sanitizing |
| `util/AppScheduleEvaluator.kt` | pure schedule evaluation and serialization |
| `util/FavouriteOrdering.kt` | pure reordering rule expressed as a merge |
| `util/FuzzyMatcher.kt` | fuzzy subsequence search |
| `util/GridLayoutEngine.kt` | pure grid maths: free room, move, resize, insert, squeeze, refit |
| `util/HomeGridSerializer.kt` | grid serialization, versioned and tolerant of malformed lines |
| `util/TextExtensions.kt` | name and search-text normalization |

</details>

<details open>
<summary><strong><code>ui/</code> — presentation</strong></summary>

<br>

| package | contents |
|---|---|
| `base` | `BaseActivity`, the base of every screen |
| `home` | `HomeActivity`, `HomeViewModel`, favourites and notifications adapters |
| `home/grid` | `HomeGridLayout`, `HomeGridController`, `HomeGridEditor`, `HomeGridStore`, `HomeGridMetrics`, `HomeEditChrome`, `WidgetFitFrame`, `ScaleToFitFrame` |
| `appselect` | favourites selection with search |
| `allapps` | full drawer, mixed rows, folder dragging and adapter |
| `settings` | category menu, sub-screen base, row helpers and six sub-screens |
| `schedule` | rule list, editor, block screen, access gate and day labels |
| `onboarding` | five-step onboarding |
| `pause` | conscious pause screen |
| `detox` | detox block screen |
| `theme` | `FocusColors`, `FocusPalette`, `SurfaceScrim` |
| `tutorial` | tutorial content and overlay |
| `wallpaper` | copying, downscaling and wallpaper utilities |
| `widget` | widget hosting (`AppWidgetHostManager`, `HomeWidgetHostView`, `WidgetHostContext`, `WidgetSurface`, `WidgetSizing`), the widget picker, the analog clock and the clock renderer |
| `widget/library` | the 20 built-in widgets: `FocusWidgetKind`, providers, renderers, `WidgetStyle`, `WidgetUpdater`, `MediaWidgetBridge`, `WidgetLaunchActivity` and the two configuration screens |
| `util` | window metrics |

</details>

<details open>
<summary><strong><code>security/</code> and <code>service/</code></strong></summary>

<br>

| file | responsibility |
|---|---|
| `security/SecureStorageHelper.kt` | aes-256/gcm backed by the android keystore |
| `security/EncryptedPrefs.kt` | encrypted `SharedPreferences` wrapper with typed results |
| `security/PasswordHasher.kt` | pbkdf2-hmac-sha256 with salt, plus legacy format detection |
| `security/PasswordAttemptPolicy.kt` | shared attempt and cooldown policy |
| `service/FocusAccessibilityService.kt` | system-level blocking and usage accounting |
| `service/FocusNotificationService.kt` | notification listener for the side panel and the music widgets |

</details>

---

## RESOURCES

```
app/src/main/res/
├── anim/            # fp_fade_in, fp_fade_out — BaseActivity transitions
├── color/           # colour selectors for switches, sliders and segments
├── drawable/        # shapes, settings and tutorial icons
├── font/            # downloadable typefaces
├── layout/          # screens, rows, dialogs and panels
├── menu/            # context menus
├── mipmap-*/        # app icons
├── values/          # strings (english), colours, themes, dimens, attrs, ids
├── values-es/       # spanish
├── values-fr/       # french
├── values-de/       # german
├── values-en-rGB/   # british english
└── xml/             # accessibility config, extraction rules, locales
```

<details>
<summary><strong>resource conventions</strong></summary>

<br>

- `values/strings.xml` is the english source, written **in lowercase** with a minimalist,
  calm tone
- **there is no `values-night/`**: light/dark is resolved manually from `THEME_AUTO`
- `values/themes.xml` holds the four built-in themes, `Base.Theme.FocusPhone` and the dialog
  and widget styles; each theme redirects the material 3 roles it does not define to its own
  colours, so the m3 purple baseline never leaks into the monochrome ui
- `res/xml/locales_config.xml` declares the supported languages for android's per-app
  language picker

</details>

---

## TEST CODE

```
app/src/test/java/app/focusphone/          # jvm
├── data/preferences/                      # AppFolder, Font, Gesture, HomeLayout,
│                                          # Profile, Theme, Wallpaper, Widget
├── data/repository/FakeFavouriteAppRepository.kt
├── domain/usecase/                        # Remove, Toggle, UpdateOrder
├── domain/util/                           # AppFolderSerializer, AppScheduleEvaluator,
│                                          # FavouriteOrdering, FuzzyMatcher
├── security/                              # PasswordAttemptPolicy, PasswordHasher
├── service/FocusAccessibilityServiceTest.kt
└── ui/                                    # AllAppsViewModel, FocusColors,
                                           # MaterialAlertDialogTheme, SurfaceScrim

app/src/androidTest/java/app/focusphone/   # instrumented
├── TestFocusPhoneApp.kt / TestRunner.kt   # repository substitution
├── FakeAppRepository.kt
├── AppScheduleTest.kt, AppSelectFlowTest.kt, ClockRendererTest.kt,
├── DetoxPasswordTest.kt, FavouriteAppDaoTest.kt, OnboardingFlowTest.kt,
├── ProfilePreferencesStorageTest.kt, ProfileSwitchTest.kt,
└── WallpaperStorageTest.kt, HomeGridWidgetIdsTest.kt
```

full detail in [testing and quality](./10-testing-and-quality.md).

---

## WHERE TO TOUCH, BY TASK

| i want to… | start at |
|---|---|
| add a setting | the matching `*Preferences`, then the `ui/settings` sub-screen |
| add a screen | extend `BaseActivity` and declare it in `AndroidManifest.xml` |
| change a blocking rule | `ProfilePreferences` (profiles) or `AppScheduleEvaluator` (schedules) |
| change colours or contrast | `ui/theme/FocusPalette.kt` and `FocusColors.kt` |
| change drawer behaviour | `ui/allapps/` and `AppFolderPreferences` |
| touch favourites persistence | `FavouriteAppRepositoryImpl` and `FavouriteAppDao` |
| touch system-level blocking | `service/FocusAccessibilityService.kt` |

---

<div align="center">

<sub><a href="./01-architecture.md">← architecture</a> · <a href="../README.md">index</a> · <a href="./03-environment-and-build.md">environment and build →</a></sub>

</div>
