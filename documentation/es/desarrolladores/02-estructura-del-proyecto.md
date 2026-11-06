<div align="center">

# ESTRUCTURA DEL PROYECTO

<sub><a href="../README.md">indice</a> · <a href="../../en/developers/02-project-structure.md">english</a> · anterior: <a href="./01-arquitectura.md">arquitectura</a> · siguiente: <a href="./03-entorno-y-compilacion.md">entorno y compilacion</a></sub>

</div>

---

## RAIZ DEL REPOSITORIO

```
focusphone-app/
├── agents.md                # guia operativa para agentes de ia y mantenedores
├── changelog.md             # historial de cambios bilingue por version
├── claude.md                # copia de las reglas de commit, pr, merge y release
├── license                  # licencia de uso no comercial
├── readme.md                # presentacion publica del proyecto
├── security.md              # politica de seguridad y reporte de vulnerabilidades
├── documentation/           # esta documentacion
├── focusphone-project/      # el proyecto gradle de android
└── .github/workflows/       # integracion continua
```

> el proyecto gradle **no** esta en la raiz: vive en `focusphone-project/`. todos los
> comandos de gradle se ejecutan desde ahi.

---

## PROYECTO GRADLE

```
focusphone-project/
├── app/
│   ├── build.gradle.kts          # configuracion del modulo :app
│   ├── proguard-rules.pro        # reglas de r8 para release
│   └── src/
│       ├── main/                 # codigo y recursos de produccion
│       ├── test/                 # pruebas jvm (junit 4, robolectric, mockk, turbine)
│       └── androidTest/          # pruebas instrumentadas (espresso, androidjunit4)
├── gradle/
│   ├── libs.versions.toml        # catalogo de versiones, fuente unica de dependencias
│   ├── gradle-daemon-jvm.properties  # criterio de toolchain: jdk 21
│   └── wrapper/                  # wrapper versionado, incluido el jar
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

---

## PAQUETES DEL CODIGO FUENTE

```
app/src/main/java/app/focusphone/
├── FocusPhoneApp.kt              # clase Application; expone el AppContainer
├── di/
│   └── AppContainer.kt           # grafo manual de dependencias
├── data/
│   ├── local/                    # room: base de datos y dao
│   ├── model/                    # entidades de room
│   ├── preferences/              # 12 objetos de preferencias sincronas
│   └── repository/               # implementaciones de repositorio y monitor de paquetes
├── domain/
│   ├── model/                    # modelos de negocio
│   ├── repository/               # interfaces de repositorio
│   ├── usecase/                  # casos de uso
│   └── util/                     # logica pura sin android
├── security/                     # cifrado, hashing y politica de intentos
├── service/                      # servicios de accesibilidad y de notificaciones
├── ui/
│   ├── base/                     # BaseActivity
│   ├── home/                     # pantalla de inicio
│   ├── appselect/                # selector de favoritos
│   ├── allapps/                  # cajon de aplicaciones y carpetas
│   ├── settings/                 # ajustes y subpantallas
│   ├── schedule/                 # horarios de aplicaciones
│   ├── onboarding/               # bienvenida
│   ├── pause/                    # pausa consciente
│   ├── detox/                    # pantalla de bloqueo de detox
│   ├── theme/                    # paleta, contraste y scrim
│   ├── tutorial/                 # tutorial contextual
│   ├── wallpaper/                # almacenamiento y utilidades de fondo
│   ├── widget/                   # relojes, widget externo y selector
│   └── util/                     # metricas de ventana
└── util/
    └── LauncherUtils.kt          # utilidades de lanzador y desinstalacion
```

---

## RESPONSABILIDAD DE CADA PAQUETE

<details open>
<summary><strong><code>data/</code> — persistencia</strong></summary>

<br>

| fichero | responsabilidad |
|---|---|
| `local/AppDatabase.kt` | singleton de la base de datos room |
| `local/FavouriteAppDao.kt` | dao de la tabla `favourite_apps` |
| `model/FavouriteApp.kt` | entidad de room |
| `preferences/AppFolderPreferences.kt` | carpetas del cajon; cifrado; expone `StateFlow` |
| `preferences/AppSchedulePreferences.kt` | reglas de horario, contraseña y contadores de uso; cifrado |
| `preferences/ConsciousPausePreferences.kt` | activacion y duracion de la pausa consciente |
| `preferences/CustomAppLabelsPreferences.kt` | nombres personalizados; cifrado, con migracion desde texto plano |
| `preferences/FontPreferences.kt` | tipografia, tipo y tamaño de reloj, tamaño de texto de la app y de la home |
| `preferences/GesturePreferences.kt` | gesto de deslizar hacia abajo |
| `preferences/HomeGridPreferences.kt` | la cuadricula de inicio, una linea por bloque (`HomeGridSerializer`); lee y limpia una vez la disposicion anterior a la cuadricula |
| `preferences/HomeLayoutPreferences.kt` | el aspecto de los bloques de inicio: espaciado entre apps, tipo de reloj antes de ocultarlo |
| `preferences/LibraryWidgetPreferences.kt` | opciones de los widgets propios: ciudades del reloj mundial, el texto de intencion (cifrado) |
| `preferences/NotificationPanelPreferences.kt` | activacion del panel de notificaciones |
| `preferences/ProfilePreferences.kt` | perfil, detox, contraseña, apps bloqueadas y palabras clave; cifrado |
| `preferences/ThemePreferences.kt` | tema, colores personalizados, alineacion, barra de estado, bienvenida |
| `preferences/TutorialPreferences.kt` | progreso del tutorial |
| `preferences/WallpaperPreferences.kt` | fondos por superficie, modo compartido, desenfoque y legibilidad |
| `repository/AppRepositoryImpl.kt` | consulta de aplicaciones instaladas con cache breve |
| `repository/FavouriteAppRepositoryImpl.kt` | room mas fusion de etiquetas y migracion desde preferencias |
| `repository/NotificationRepository.kt` | notificaciones activas, solo en memoria |
| `repository/PackageChangeMonitor.kt` | señal global de instalacion, desinstalacion o actualizacion |

</details>

<details open>
<summary><strong><code>domain/</code> — reglas de negocio</strong></summary>

<br>

| fichero | responsabilidad |
|---|---|
| `model/AppFolder.kt` | carpeta del cajon |
| `model/AppFont.kt` | tipografias disponibles |
| `model/AppInfo.kt` | aplicacion instalada |
| `model/AppScheduleRule.kt` | regla de horario y motivo de bloqueo |
| `model/AppShortcut.kt` | acceso directo de aplicacion |
| `model/ClockType.kt` | los cinco estilos de reloj |
| `model/HomeGrid.kt` | modelo de la cuadricula: `HomeGrid`, `HomeGridItem`, `CellRect`, `SpanLimits` (3-6 columnas × 4-12 filas) |
| `model/ToggleResult.kt` | resultado de alternar un favorito |
| `repository/AppRepository.kt` | interfaz de aplicaciones instaladas |
| `repository/FavouriteAppRepository.kt` | interfaz crud de favoritos |
| `usecase/GetAppShortcutsUseCase.kt` | accesos directos via `LauncherApps` |
| `usecase/GetFavouriteAppsUseCase.kt` | observa los favoritos |
| `usecase/GetInstalledAppsUseCase.kt` | lista instaladas marcando las favoritas |
| `usecase/RemoveFavouriteAppUseCase.kt` | elimina un favorito de forma idempotente |
| `usecase/ToggleFavouriteAppUseCase.kt` | alterna un favorito y aplica el limite de 10 |
| `usecase/UpdateAppsOrderUseCase.kt` | persiste un nuevo orden sin perder favoritos |
| `util/AppFolderSerializer.kt` | serializacion y saneado de nombres de carpeta |
| `util/AppScheduleEvaluator.kt` | evaluacion pura de horarios y su serializacion |
| `util/FavouriteOrdering.kt` | regla pura de reordenacion como fusion |
| `util/FuzzyMatcher.kt` | busqueda difusa por subsecuencia |
| `util/GridLayoutEngine.kt` | matematica pura de la cuadricula: hueco libre, mover, redimensionar, insertar, encoger, reajustar |
| `util/HomeGridSerializer.kt` | serializacion de la cuadricula, versionada y tolerante a lineas mal formadas |
| `util/TextExtensions.kt` | normalizacion de nombres y de texto para buscar |

</details>

<details open>
<summary><strong><code>ui/</code> — presentacion</strong></summary>

<br>

| paquete | contenido |
|---|---|
| `base` | `BaseActivity`, base de todas las pantallas |
| `home` | `HomeActivity`, `HomeViewModel`, adaptadores de favoritos y notificaciones |
| `home/grid` | `HomeGridLayout`, `HomeGridController`, `HomeGridEditor`, `HomeGridStore`, `HomeGridMetrics`, `HomeEditChrome`, `WidgetFitFrame`, `ScaleToFitFrame` |
| `appselect` | seleccion de favoritos con busqueda |
| `allapps` | cajon completo, filas mixtas, arrastre a carpetas y adaptador |
| `settings` | menu de categorias, base de subpantallas, ayudantes de filas y seis subpantallas |
| `schedule` | lista de reglas, editor, pantalla de bloqueo, puerta de acceso y etiquetas de dias |
| `onboarding` | bienvenida de cinco pasos |
| `pause` | pantalla de pausa consciente |
| `detox` | pantalla de bloqueo de detox |
| `theme` | `FocusColors`, `FocusPalette`, `SurfaceScrim` |
| `tutorial` | contenido y superposicion del tutorial |
| `wallpaper` | copia, escalado y utilidades de fondo |
| `widget` | alojamiento de widgets (`AppWidgetHostManager`, `HomeWidgetHostView`, `WidgetHostContext`, `WidgetSurface`, `WidgetSizing`), el selector de widgets, el reloj analogico y el renderizador de reloj |
| `widget/library` | los 20 widgets propios: `FocusWidgetKind`, proveedores, renderizadores, `WidgetStyle`, `WidgetUpdater`, `MediaWidgetBridge`, `WidgetLaunchActivity` y las dos pantallas de configuracion |
| `util` | metricas de ventana |

</details>

<details open>
<summary><strong><code>security/</code> y <code>service/</code></strong></summary>

<br>

| fichero | responsabilidad |
|---|---|
| `security/SecureStorageHelper.kt` | aes-256/gcm respaldado por el keystore de android |
| `security/EncryptedPrefs.kt` | envoltorio de `SharedPreferences` cifrado con resultado tipado |
| `security/PasswordHasher.kt` | pbkdf2-hmac-sha256 con sal, y deteccion del formato heredado |
| `security/PasswordAttemptPolicy.kt` | politica compartida de intentos y espera |
| `service/FocusAccessibilityService.kt` | bloqueo a nivel de sistema y contabilidad de uso |
| `service/FocusNotificationService.kt` | escucha de notificaciones para el panel lateral y los widgets de musica |

</details>

---

## RECURSOS

```
app/src/main/res/
├── anim/            # fp_fade_in, fp_fade_out — transiciones de BaseActivity
├── color/           # selectores de color para interruptores, deslizadores y segmentos
├── drawable/        # formas, iconos de ajustes y del tutorial
├── font/            # tipografias descargables
├── layout/          # pantallas, filas, dialogos y paneles
├── menu/            # menus contextuales
├── mipmap-*/        # iconos de la aplicacion
├── values/          # strings (ingles), colores, temas, dimensiones, atributos, ids
├── values-es/       # castellano
├── values-fr/       # frances
├── values-de/       # aleman
├── values-en-rGB/   # ingles britanico
└── xml/             # configuracion de accesibilidad, reglas de extraccion, locales
```

<details>
<summary><strong>convenciones de recursos</strong></summary>

<br>

- `values/strings.xml` es la fuente en ingles, escrita **en minusculas** y con tono
  minimalista y tranquilo
- **no existe `values-night/`**: el claro/oscuro se resuelve a mano desde `THEME_AUTO`
- `values/themes.xml` contiene los cuatro temas integrados, `Base.Theme.FocusPhone` y los
  estilos de dialogo y de widgets; cada tema redirige los roles de material 3 que no define
  a sus propios colores, para que la linea base morada de m3 no se cuele en la interfaz
  monocroma
- `res/xml/locales_config.xml` declara los idiomas soportados para el selector de idioma
  por aplicacion de android

</details>

---

## CODIGO DE PRUEBAS

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

app/src/androidTest/java/app/focusphone/   # instrumentadas
├── TestFocusPhoneApp.kt / TestRunner.kt   # sustitucion de repositorios
├── FakeAppRepository.kt
├── AppScheduleTest.kt, AppSelectFlowTest.kt, ClockRendererTest.kt,
├── DetoxPasswordTest.kt, FavouriteAppDaoTest.kt, OnboardingFlowTest.kt,
├── ProfilePreferencesStorageTest.kt, ProfileSwitchTest.kt,
└── WallpaperStorageTest.kt, HomeGridWidgetIdsTest.kt
```

detalle completo en [pruebas y calidad](./10-pruebas-y-calidad.md).

---

## DONDE TOCAR SEGUN LA TAREA

| quiero… | empieza por |
|---|---|
| añadir un ajuste | el `*Preferences` correspondiente, luego la subpantalla de `ui/settings` |
| añadir una pantalla | extiende `BaseActivity` y declarala en `AndroidManifest.xml` |
| cambiar una regla de bloqueo | `ProfilePreferences` (perfiles) o `AppScheduleEvaluator` (horarios) |
| cambiar colores o contraste | `ui/theme/FocusPalette.kt` y `FocusColors.kt` |
| cambiar el comportamiento del cajon | `ui/allapps/` y `AppFolderPreferences` |
| tocar la persistencia de favoritos | `FavouriteAppRepositoryImpl` y `FavouriteAppDao` |
| tocar el bloqueo del sistema | `service/FocusAccessibilityService.kt` |

---

<div align="center">

<sub><a href="./01-arquitectura.md">← arquitectura</a> · <a href="../README.md">indice</a> · <a href="./03-entorno-y-compilacion.md">entorno y compilacion →</a></sub>

</div>
