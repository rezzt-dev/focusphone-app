# FocusPhone.
<div align="center">

<img src="https://img.shields.io/badge/focusphone-minimalist%20launcher-3DDC84?style=for-the-badge&logo=android&logoColor=white" />

<br>

<strong>Take back control of your screen time with a premium and minimalist launcher</strong>

<br>

<a href="./LICENSE">
  <img src="https://img.shields.io/badge/license-non--commercial-orange.svg" />
</a>
<a href="https://kotlinlang.org">
  <img src="https://img.shields.io/badge/kotlin-2.0+-7F52FF?logo=kotlin&logoColor=white" />
</a>
<a href="https://developer.android.com">
  <img src="https://img.shields.io/badge/platform-android-3DDC84?logo=android&logoColor=white" />
</a>
<a href="#">
  <img src="https://img.shields.io/badge/design-minimalist-000000?style=flat-square" />
</a>

<br>

<a href="#">View Demo</a> •
<a href="./issues">Report a Bug</a> •
<a href="./issues">Request a Feature</a>

</div>

---

## Table Of Contents
- [overview](#overview)
- [features](#features)
- [demo](#demo)
- [technologies](#technologies)
- [installation](#installation)
- [project structure](#project-structure)
- [download](#download)
- [contributing](#contributing)
- [license](#license)
- [author](#author)

---
## Overview
**focusphone** is a native android application designed to simplify your smartphone experience. it combines a clean, distraction-free interface with premium customization options like distinct clock types, custom fonts, and hand-picked color themes. built entirely with modern kotlin and following clean architecture principles, it ensures lightweight performance, zero-flicker style rendering, and offline-first persistence.
### Use Cases
- **digital detox**: reduce daily screen time and mindless scrolling by removing visual app triggers.
- **minimalist lifestyle**: keep a clean, text-only home screen that promotes intentional phone use.
- **quick utility**: access system alarms instantly by tapping anywhere on the main clock.
- **ergonomic customization**: adapt text alignment for comfortable single-handed use on any device size.

---
## Features

### Core Functionality
- **text-only interface**: replaces distracting colorful icons with a beautiful list of essential apps.
- **gesture-based drawer**: swipe left from the home screen to open a full-width menu of other apps or settings.
- **five clock styles**: choose between digital, stacked, words, analog, or hidden clock designs.
- **flexible alignment**: align your main app lists to the left, center, or right for optimal thumb reach.
- **dynamic reordering**: long-press and drag favorite apps on the home screen to instantly sort them.
- **premium themes**: swap between beautifully crafted color palettes (slate, navy, warm beige, and off-white).
- **modern typography**: seamless integration of downloadable fonts (poppins, comfortaa, figtree, jetbrains mono, space mono).
- **app schedules**: allow an app only inside a time window (weekdays included) or for a few minutes a day, protected by a password so limits cannot be undone on impulse.

### technical highlights
- **clean architecture**: distinct separation of concerns dividing data, domain, and presentation layers.
- **zero-flicker loading**: synchronous base preferences loading that prevents flashes of unstyled content.
- **edge-to-edge layout**: fully immersive ui that draws content beautifully under system status and navigation bars.
- **reactive state**: updates propagate instantly across screens using coroutines and stateflow.

---
## Demo

### Workflows
> add application screenshots in this section.
example flow:
`complete onboarding → select favorite apps → customize theme and clock → swipe left to access drawer`

---
## Technologies

### Android Development
- **kotlin**: 100% kotlin codebase.
- **android sdk**: utilizing the latest android apis.
- **jetpack components**:
  - **viewmodel**: ui-related data holder.
  - **flow / coroutines**: reactive state and asynchronous operations.
  - **sharedpreferences**: lightweight local data and sync style persistence.
  - **viewbinding**: safe interaction with views.

### Architecture
- **pattern**: clean architecture / model-view-viewmodel (mvvm).
- **dependency injection**: manual app container injection (`AppContainer`).
- **asynchronous operations**: kotlin coroutines.

---
## Installation

### Prerequisites
- android studio (ladybug or newer).
- jdk 17.
- android sdk api level 26+.

### Local setup
1. **Clone the repository**
```bash
git clone https://github.com/rezzt-devc/focusphone-project.git
cd focusphone-project
```
2. Open in android studio, select "open an existing project" and choose the focusphone-project directory.
3. Sync gradle, allow android studio to download dependencies and index the project.
4. Run the app, connect a device or start an emulator and press run (shift + f10).

---
## Project Structure
```txt
app/src/main/java/app/focusphone/
│
├── data/                  # data layer
│   ├── model/             # data entities
│   ├── preferences/       # local preference sources (theme, fonts, favourites)
│   └── repository/        # data repository implementations
│
├── domain/                # domain business logic
│   ├── model/             # business models (appfont, clocktype)
│   ├── repository/        # repository interfaces
│   └── usecase/           # specialized use cases
│
├── ui/                    # presentation layer
│   ├── allapps/           # device applications drawer screen
│   ├── appselect/         # favorites selection checklist screen
│   ├── base/              # base activity configurations (themes & fonts)
│   ├── home/              # main launcher screen and clocks
│   ├── onboarding/        # setup and welcome screens
│   └── settings/          # style and preference customizers
│
└── focusphoneapp.kt       # application class and dependency container
```

---
## Download
> download the latest release apk directly from the [releases](../../releases) tab on github.

---
## Contributing
contributions are always welcome to make focusphone better.
how to contribute:

1. __fork__ the repository.
2. __create__ a branch (`git checkout -b feature/amazingfeature`).
3. __commit__ your changes (`git commit -m 'add some amazingfeature'`).
4. __push__ to the branch (`git push origin feature/amazingfeature`).
5. __open__ a pull request.

---
## License
this project is licensed under a custom non-commercial license. personal, educational and evaluation use is allowed; commercial use requires prior written permission from the author. see the [license](./LICENSE) file for full details.

---
## Author
__rezzt.dev__
- website: [rezzt.dev](https://rezzt.dev)
- github: [@rezzt-dev](https://github.com/rezzt-dev)

---
<div align="center">
developed by <a href="https://rezzt.dev" type"_blank"><b>rezzt.dev</b></a>
</div>
