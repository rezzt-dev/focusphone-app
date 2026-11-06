<div align="center">

<img src="./focusphone-project/app/src/main/res/mipmap-xxxhdpi/icon_alfa_version.webp" width="112" alt="focusphone" />

# FOCUSPHONE

<strong>a minimalist android launcher that replaces your home screen with text:<br>
a clock, your widgets and only the apps you actually use.</strong>

<sub>english · <a href="./repo-docs/readme.es.md">castellano</a></sub>

<br><br>

<a href="https://github.com/rezzt-dev/focusphone-app/releases"><img src="https://img.shields.io/github/v/release/rezzt-dev/focusphone-app?include_prereleases&label=release&style=for-the-badge&color=000000" alt="latest release" /></a>
<img src="https://img.shields.io/badge/status-beta-orange?style=for-the-badge" alt="status beta" />
<a href="https://github.com/rezzt-dev/focusphone-app/actions/workflows/android-ci.yml"><img src="https://github.com/rezzt-dev/focusphone-app/actions/workflows/android-ci.yml/badge.svg" alt="android-ci" /></a>

<br>

<img src="https://img.shields.io/badge/min%20sdk-24%20(android%207.0)-3DDC84?style=flat-square" alt="min sdk 24" />
<img src="https://img.shields.io/badge/target%20sdk-36-3DDC84?style=flat-square" alt="target sdk 36" />
<img src="https://img.shields.io/badge/kotlin-100%25-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="100% kotlin" />
<img src="https://img.shields.io/badge/permissions-no%20internet-4c1?style=flat-square" alt="no internet permission" />
<a href="./license"><img src="https://img.shields.io/badge/license-non--commercial%20(not%20osi)-orange?style=flat-square" alt="non-commercial license, not osi approved" /></a>

<br><br>

<a href="./documentation/en/README.md"><b>documentation</b></a> ·
<a href="./repo-docs/changelog.md"><b>changelog</b></a> ·
<a href="./repo-docs/privacy.md"><b>privacy</b></a> ·
<a href="./.github/security.md"><b>security</b></a> ·
<a href="./.github/contributing.md"><b>contributing</b></a> ·
<a href="https://focusphone.rezzt.dev"><b>website</b></a>

</div>

---

## TABLE OF CONTENTS

- [overview](#overview)
- [features](#features)
- [install](#install)
- [verify the apk](#verify-the-apk)
- [build from source](#build-from-source)
- [repository layout](#repository-layout)
- [architecture and stack](#architecture-and-stack)
- [documentation](#documentation)
- [privacy and security](#privacy-and-security)
- [project status](#project-status)
- [contributing](#contributing)
- [license](#license)

---

## OVERVIEW

most android launchers are designed so that you open apps: colourful icons, red badges and
grids full of suggestions act as constant reminders. **focusphone starts from the opposite
idea: the home screen should be a place you pass through, not a place you stay in.** apps
are shown as text, favourites are capped at ten, and blocking — once configured behind a
password — is designed to hold.

its locks are **a commitment to yourself, not a security tool**: focusphone is not a device
administrator and cannot stop you from uninstalling it or changing launcher. see
[`disclaimer.md`](./repo-docs/disclaimer.md).

### DESIGN PRINCIPLES

| principle | how it is enforced in the code |
|---|---|
| **text over icons** | favourites are labels only; icons exist solely in the app drawer and the pickers |
| **offline by construction** | the app declares no `INTERNET` permission — there is no network layer to audit |
| **blocks that hold** | detox and schedule passwords are pbkdf2-hashed and rate limited (5 attempts, then 15 minutes); a wrong password never clears a rule |
| **one launch path** | every app opening — home, drawer, shortcuts and widgets, even in other launchers — goes through `BaseActivity.openApp` / `openShortcut`, which apply profile, schedule and conscious pause |
| **one source of truth for colour** | every runtime colour goes through `FocusPalette` / `FocusColors`, never `resolveAttribute` |
| **pure logic is testable logic** | schedule evaluation, grid maths, folder serialization, contrast and fuzzy search live in pure, unit-tested classes |

---

## FEATURES

| area | what you get |
|---|---|
| **home screen** | a grid of 3-6 columns by 4-12 rows holding a clock (digital, stacked, in words, analog or hidden), up to 10 favourite apps as text, and any number of widgets; edited in place with drag, resize handles and accessibility actions |
| **widgets** | 20 built-in widgets (clocks, calendar, music, search, favourites, battery, storage, system, quick settings, next alarm, focus profile, intention, progress) that follow your theme and work in any launcher, plus a picker for your other apps' widgets |
| **focus tools** | profiles (normal, work, detox with timer and password), per-app schedules with time windows and daily limits, and a conscious pause — one guided breath before an app opens |
| **drawer** | all apps with fuzzy search, text folders, custom names, shortcuts and uninstall through android's dialog |
| **appearance** | four themes, automatic day/night, a custom theme with a contrast guard, four typefaces (system, poppins, comfortaa, jetbrains mono), separate app and home text sizes (8-24 pt), alignment, and wallpapers with blur and legibility control |
| **notifications** | an optional side panel and a swipe-down gesture for the system shade |
| **languages** | english, british english, spanish, french and german |

the full user guide is in [`documentation/en`](./documentation/en/README.md).

---

## INSTALL

focusphone is distributed as an apk from the
[releases](https://github.com/rezzt-dev/focusphone-app/releases) tab. it is **not on google
play**.

1. download `focusphone-*.apk` (and `SHA256SUMS`) from the latest release.
2. open the apk on your phone and allow "install unknown apps" for the browser or file
   manager you opened it from.
3. press the home button and choose **focusphone** as your home app, or use
   **settings → system → default launcher** inside the app.

requirements: android 7.0 (api 24) or newer; no root, no account, no internet connection.

> **coming from `v1.0.0-beta`?** that build was signed with a different key, so android will
> not update it in place. uninstall it first (its settings are lost, because focusphone keeps
> no backups) and then install the latest release.

---

## VERIFY THE APK

```bash
sha256sum -c SHA256SUMS
apksigner verify --print-certs focusphone-*.apk     # android sdk build-tools
```

| | |
|---|---|
| signature scheme | apk signature scheme v2 |
| signing certificate sha-256 (from `v1.4-beta` on) | `0f:f1:f8:3c:32:e0:5d:c5:4b:c2:2b:a2:8a:13:df:4c:5c:37:ec:79:c4:1d:b3:e1:4a:d7:20:e7:c8:a0:76:2f` |

an apk signed with any other certificate is not an official build. details in
[`security.md`](./.github/security.md#verifying-an-apk).

---

## BUILD FROM SOURCE

| requirement | version |
|---|---|
| android studio | a release compatible with agp `9.3.0` |
| jdk | `21`, provisioned through the gradle daemon toolchain |
| android sdk | compile sdk `36` |

```bash
git clone https://github.com/rezzt-dev/focusphone-app.git
cd focusphone-app/focusphone-project
./gradlew :app:assembleDebug        # build a debug apk
./gradlew :app:installDebug         # install it on a connected device
```

open `focusphone-project/` — **not** the repository root — in android studio. the
verification pipeline, the same one ci runs on every push and pull request:

```bash
./gradlew staticAnalysis --continue
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

release signing is optional and configured through four `RELEASE_*` properties outside
version control; without them `assembleRelease` produces an unsigned apk. see
[`03-environment-and-build.md`](./documentation/en/developers/03-environment-and-build.md).

---

## REPOSITORY LAYOUT

```txt
focusphone-app/
├── focusphone-project/          # the android project (gradle root, single :app module)
├── documentation/               # bilingual user and developer documentation
│   ├── en/{users,developers}/
│   └── es/{usuarios,desarrolladores}/
├── .github/                     # ci, dependency policy, issue and pr templates, and the
│                                # community files: security, contributing, code_of_conduct,
│                                # support (each + .es.md)
├── repo-docs/                   # project information
│   ├── changelog.md             # bilingual changelog, source of truth for releases
│   ├── readme.es.md             # this readme in spanish
│   ├── privacy.md · permissions.md · disclaimer.md   # (each + .es.md)
│   └── third_party_licenses.md  # inventory and texts of third-party licenses
├── agents.md · claude.md        # operating guide for ai agents and maintainers
├── license · notice             # non-commercial license and copyright notice
└── readme.md
```

---

## ARCHITECTURE AND STACK

clean architecture (lite) with mvvm, in a single `:app` module: `data` (room for favourites,
`SharedPreferences` for settings, aes-256/gcm through the android keystore for user content),
`domain` (models, use cases and pure rules such as `GridLayoutEngine` and
`AppScheduleEvaluator`) and `ui` (views + view binding, no compose). dependency injection is
manual, through `AppContainer`.

| area | choice |
|---|---|
| language | kotlin (standard library `2.2.20`), coroutines + `StateFlow` |
| ui | android views, view binding, material components `1.14.0` |
| persistence | room `2.8.5` · `SharedPreferences` · android keystore |
| build | gradle `9.7.1` · agp `9.3.0` · version catalog, updated by hand only |
| quality | android lint and detekt `1.23.8` with no baseline · ktlint `1.8.0` |
| testing | junit4 · mockk · robolectric · turbine · espresso |

details: [architecture](./documentation/en/developers/01-architecture.md) ·
[project structure](./documentation/en/developers/02-project-structure.md) ·
[`agents.md`](./agents.md). third-party components and their licenses:
[`third_party_licenses.md`](./repo-docs/third_party_licenses.md).

---

## DOCUMENTATION

| | english | castellano |
|---|---|---|
| index | [documentation/en](./documentation/en/README.md) | [documentation/es](./documentation/es/README.md) |
| users (12 chapters) | [what is focusphone](./documentation/en/users/01-what-is-focusphone.md) | [que es focusphone](./documentation/es/usuarios/01-que-es-focusphone.md) |
| developers (12 chapters) | [architecture](./documentation/en/developers/01-architecture.md) | [arquitectura](./documentation/es/desarrolladores/01-arquitectura.md) |
| faq | [faq](./documentation/en/users/12-faq.md) | [preguntas frecuentes](./documentation/es/usuarios/12-preguntas-frecuentes.md) |

<!-- TODO(owner): screenshots of the home grid, edit mode and widget picker; none are versioned yet. -->

---

## PRIVACY AND SECURITY

- no `INTERNET` permission, no accounts, no analytics, no crash reporting: nothing leaves
  the device. the full policy is [`privacy.md`](./repo-docs/privacy.md).
- three install-time permissions (`QUERY_ALL_PACKAGES`, `REQUEST_DELETE_PACKAGES`,
  `EXPAND_STATUS_BAR`) and two optional services (accessibility, notification access),
  each justified in [`permissions.md`](./repo-docs/permissions.md).
- sensitive data is encrypted with aes-256/gcm through the android keystore, and excluded
  from backups and device-to-device transfer.
- report vulnerabilities privately, never in a public issue: [`security.md`](./.github/security.md).

---

## PROJECT STATUS

| item | value |
|---|---|
| **current version** | `1.4.0-beta` (`versionCode` 4) — **beta** |
| **package** | `app.focusphone` |
| **distribution** | apk from [github releases](https://github.com/rezzt-dev/focusphone-app/releases), marked as pre-release; not on google play |
| **branches** | `beta-1.X` (development) → `version-beta` (stable versions) → `main` (published) |

beta means the feature set is complete and tested, but the app is still being validated on
more devices and android versions. every change is listed in [`changelog.md`](./repo-docs/changelog.md).

---

## CONTRIBUTING

contributions are welcome. read [`contributing.md`](./.github/contributing.md) (setup, branches,
commit and changelog format, sensitive code) and the
[code of conduct](./.github/code_of_conduct.md). for help, see
[`.github/support.md`](./.github/support.md).

---

## LICENSE

focusphone is distributed under a **custom non-commercial license**
([`license`](./license)): you may view, run, modify and share it for personal, educational,
research or non-commercial evaluation purposes; commercial use needs written permission
from the author. it is **not an open source license approved by the osi**. the spanish text
is the binding one.

third-party components keep their own licenses ([`notice`](./notice),
[`third_party_licenses.md`](./repo-docs/third_party_licenses.md)). contact: `contacto@rezzt.dev`.

---

<div align="center">

<sub>focusphone — developed by <a href="https://rezzt.dev"><b>rezzt.dev</b></a> · <a href="https://github.com/rezzt-dev">@rezzt-dev</a></sub>

</div>
