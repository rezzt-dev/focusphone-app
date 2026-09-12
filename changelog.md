# CHANGELOG

## SIN PUBLICAR / UNRELEASED

### CASTELLANO

#### ANADIDO
- añadir `FocusColors`, `FocusPalette` y `SurfaceScrim` como fuente unica de color en tiempo de ejecucion del tema, sustituyendo las resoluciones de atributo que devolvian un id de recurso en vez de un color
- añadir `ensureContrast` para garantizar contraste wcag en el tema personalizado cuando el usuario elige un fondo y un primer plano poco legibles
- añadir un scrim de legibilidad adaptativo sobre el fondo de pantalla, con un nuevo slider "legibilidad de texto" en ajustes, para todos los temas
- añadir una vista previa del fondo en ajustes que se toca para elegir la imagen y refleja en vivo el desenfoque y la legibilidad sobre un reloj de muestra
- añadir la opcion "usar el fondo de inicio" en notificaciones y ajustes para reutilizar la misma imagen sin volver a elegirla
- añadir una politica de dependencias sin actualizaciones automaticas: `.github/dependabot.yml` con `open-pull-requests-limit: 0`, `renovate.json` desactivado y el workflow `dependency-policy` que falla las pull requests de bots, de modo que `gradle/libs.versions.toml` solo se actualiza a mano
- añadir analisis estatico de kotlin con ktlint y detekt, sin linea base en ninguno de los dos: `.editorconfig` en la raiz define el formato para el ide y para ktlint, `config/detekt/detekt.yml` registra solo las desviaciones respecto a los valores por defecto de detekt, y la tarea raiz `staticAnalysis` ejecuta ambos
- añadir el paso `staticAnalysis` al workflow `android-ci`, antes de las pruebas y la compilacion, porque no necesita compilar y falla en segundos

#### CAMBIADO
- reorganizar la seccion de fondo de pantalla: el interruptor principal va solo, el modo "mismo fondo en todas las pantallas" muestra una unica fila "imagen", la intensidad del desenfoque solo aparece con el desenfoque activo y se elimina la fila informativa sin accion
- abrir el selector de imagenes directamente cuando elegir es la unica opcion y mostrar "aplicando…" mientras se copia la imagen
- renombrar la accion "editar aplicaciones" a "editar dashboard" en ingles, español, frances y aleman
- reestructurar las pantallas de ajustes (apariencia, inicio, enfoque, notificaciones, sistema y acerca de) alrededor de un `settingsSection` comun que gestiona cabeceras y separadores
- optimizar el rendimiento en tiempo de ejecucion y el proceso de build/ci: lint sin errores en release, dependencias actualizadas, wrapper de gradle versionado y un workflow de ci
- eliminar codigo muerto y simplificar el proyecto: `WidgetConfigActivity`, `ClockStyle`, `WeightGradientTextView`, el subsistema de tamaño de icono inalcanzable y metodos publicos sin uso
- unificar el flujo de desinstalacion tras `LauncherUtils.requestUninstall` y migrar `WidgetPickerActivity` a la activity result api
- mejorar la accesibilidad, los textos i18n y los plurales en es/fr/de, y aplicar las sugerencias `UseKtx` restantes en la capa de preferencias y ui
- bajar `agp` a `9.3.0` en `libs.versions.toml` para restaurar la compatibilidad con la version actual de android studio
- reescribir `README.md` como presentacion tecnica completa del proyecto, con arquitectura, stack, compilacion, pruebas, seguridad e indice de la documentacion, y corregir la url de clonado, el arbol de fuentes y las versiones obsoletas
- dejar de ignorar `.github/` para que los workflows de ci y la configuracion de dependabot viajen con el repositorio, que es la unica forma de que surtan efecto
- documentar la politica de dependencias en `03-entorno-y-compilacion.md`, `11-ci-y-publicacion.md` y `agents.md`, incluyendo el procedimiento manual para subir la version de una biblioteca
- aplicar el formato de ktlint a las 130 fuentes kotlin del proyecto, incluido `AllAppsActivity.kt`, que estaba indentado a 2 espacios frente a los 4 del resto
- documentar el analisis estatico en `README.md`, `agents.md` y la documentacion de desarrolladores (`03`, `10`, `11` y `12`) en castellano e ingles: reglas desactivadas con su motivo, umbrales de complejidad como trinquete y el reparto de trabajo entre android lint, ktlint y detekt

#### CORREGIDO
- restaurar los gestos de deslizar en la pantalla de inicio (apertura de cajones y despliegue de notificaciones), rotos tras mover el detector de gestos a un listener que no consumia `ACTION_DOWN`
- dar a el calendario y a ambos cajones laterales un scrim de legibilidad real y resolver sus colores desde `FocusPalette` en vez de `resolveAttribute`
- aplicar los colores del tema personalizado cuando ya esta activo, en vez de ignorarlos por un cortocircuito en `applyTheme()`
- preservar la asignacion de carpetas de aplicaciones entre reinicios y arranques con resultados incompletos del package manager
- confirmar los cambios de carpetas cifrados de forma sincrona y actualizar el estado en memoria solo tras persistir correctamente
- corregir el reordenado de favoritos para que nunca pueda borrar un favorito, y copiar los fondos de pantalla elegidos a almacenamiento privado para que sobrevivan a reinicios
- desplegar notificaciones al deslizar hacia abajo usando la accion de accesibilidad oficial `GLOBAL_ACTION_NOTIFICATIONS`
- mostrar un error en linea al crear o renombrar una carpeta con un nombre duplicado, en vez de cerrar el dialogo silenciosamente
- respetar el formato de 12/24 horas del sistema en el reloj y corregir el informe de estado del permiso `QUERY_ALL_PACKAGES`
- corregir `EncryptedPrefs.commitString`, que devolvia `Unit` en vez del `Boolean` prometido tras la limpieza `UseKtx`
- permitir establecer y ver un fondo con el tema personalizado: `colorizeViewTree` pintaba todos los contenedores de un color opaco que nunca se deshacia y tapaba la imagen; ahora solo repinta los fondos que usaban un color del tema
- evitar que el texto secundario pasara a primario al repintar la vista mas de una vez, recordando el rol de color de cada vista
- no dejar todas las pantallas sin fondo al activar "mismo fondo en todas las pantallas": se parte de la imagen de inicio, y al desactivarlo cada pantalla recupera la suya o conserva la compartida
- aplicar al momento en la propia pantalla de ajustes los cambios de mostrar/ocultar, elegir, quitar, modo compartido, desenfoque y legibilidad, sin recrearla
- descartar las cargas de fondo obsoletas para que una imagen quitada u oculta no reaparezca al terminar de decodificarse
- conservar el destino del selector de imagenes al rotar la pantalla o si el sistema cierra la app mientras se elige
- tintar los interruptores con el tema personalizado, que se trataban como texto y conservaban los colores del tema base
- nombrar `_` las excepciones que se descartan a proposito en trece `catch` que devolvian un repliegue documentado, para que se distingan de un error perdido (detekt `SwallowedException`)
- extraer `styleCalendarDay` de `populateCalendar` en `HomeActivity`, que anidaba seis niveles de bloques para pintar un numero del calendario
- evitar el cierre de la app al cambiar un fondo ya elegido y al abrir cualquier dialogo de material: `FocusPhone.DialogTitleTextStyle` heredaba de un `TextAppearance` sin `layout_width` y el titulo de `m3_alert_dialog` no se podia inflar; ahora los estilos de titulo y cuerpo heredan de `MaterialAlertDialog.Material3.*.Text` y `MaterialAlertDialogThemeTest` lo comprueba para cada tema

#### ELIMINADO
- `WidgetConfigActivity`, `ClockStyle`, `WeightGradientTextView` y el subsistema de tamaño de icono inalcanzable, junto con `backup_rules.xml` y el `values-night/themes.xml` vacio
- codigo muerto detectado por detekt: el callback `onDismiss` de `NotificationsAdapter`, que nunca se invocaba porque el deslizamiento llama a `dismissNotification` directamente; la constante `TAG` sin uso de `HomeActivity`; y el parametro `totalSteps` de `TutorialOverlayView.showStep`

#### SEGURIDAD
- unificar el bloqueo de detox y horarios bajo una unica politica de intentos cifrada (5 intentos, 15 min de espera); una contraseña incorrecta ya no finaliza la sesion de detox y la verificacion corre pbkdf2 fuera del hilo principal
- hacer que el almacenamiento seguro reporte resultados tipados (ausente/presente/no disponible/perdido) en vez de fallar en silencio, y dejar de regenerar la clave de keystore sobre un alias existente
- aplicar `data_extraction_rules.xml` para excluir todos los dominios de la transferencia de dispositivo en android 12+, ya que `allowBackup=false` no la cubre

### ENGLISH

#### ADDED
- add `FocusColors`, `FocusPalette` and `SurfaceScrim` as the single source of runtime theme colour, replacing attribute resolutions that returned a resource id instead of a colour
- add `ensureContrast` to guarantee wcag contrast for the custom theme when the user picks a hard-to-read background/foreground pair
- add an adaptive legibility scrim over the wallpaper, with a new "text legibility" slider in settings, for every theme
- add a tappable wallpaper preview in settings that picks the image and follows blur and legibility live over a sample clock
- add a "use the home wallpaper" option for notifications and settings to reuse the same image without picking it again
- add a dependency policy with no automated updates: `.github/dependabot.yml` with `open-pull-requests-limit: 0`, `renovate.json` disabled and a `dependency-policy` workflow that fails bot pull requests, so `gradle/libs.versions.toml` is only ever updated by hand
- add kotlin static analysis with ktlint and detekt, neither with a baseline: `.editorconfig` at the repository root defines the format for both the ide and ktlint, `config/detekt/detekt.yml` records only the deviations from detekt's defaults, and the root `staticAnalysis` task runs both
- add a `staticAnalysis` step to the `android-ci` workflow, ahead of the tests and the build, because it needs no compilation and fails in seconds

#### CHANGED
- reorganize the wallpaper section: the master switch stands alone, "same wallpaper everywhere" shows a single "image" row, blur intensity only appears while blur is on, and the inert info row is gone
- open the image picker straight away when choosing is the only option, and show "applying…" while the image is copied
- rename the "edit applications" action to "edit dashboard" across english, spanish, french and german
- restructure the settings screens (appearance, home, focus, notifications, system and about) around a shared `settingsSection` helper that owns headers and separators
- optimize runtime performance and the build/ci pipeline: lint runs clean on release builds, dependencies updated, gradle wrapper jar versioned and a ci workflow added
- remove dead code and simplify the project: `WidgetConfigActivity`, `ClockStyle`, `WeightGradientTextView`, the unreachable icon-size-slider subsystem and unused public methods
- unify the uninstall flow behind `LauncherUtils.requestUninstall` and move `WidgetPickerActivity` to the activity result api
- improve accessibility, i18n text and plurals across es/fr/de, and apply the remaining `UseKtx` suggestions across the preferences layer and ui
- downgrade `agp` to `9.3.0` in `libs.versions.toml` to restore build compatibility with the current android studio release
- rewrite `README.md` as a complete technical presentation of the project, with architecture, stack, build, testing, security and a documentation index, fixing the wrong clone url, the stale source tree and the outdated versions
- stop ignoring `.github/` so the ci workflows and the dependabot configuration ship with the repository, the only way for them to take effect
- document the dependency policy in `03-environment-and-build.md`, `11-ci-and-release.md` and `agents.md`, including the manual procedure for bumping a library version
- apply ktlint's formatting to all 130 kotlin sources, including `AllAppsActivity.kt`, which was indented with 2 spaces against the project's 4
- document static analysis in `README.md`, `agents.md` and the developer documentation (`03`, `10`, `11` and `12`) in both spanish and english: every disabled rule with its reason, the complexity thresholds as a ratchet, and how the work is split between android lint, ktlint and detekt

#### FIXED
- restore home screen swipe gestures (drawers and notification expansion), broken after the gesture detector moved to a touch listener that never consumed `ACTION_DOWN`
- give the calendar and both side drawers a real legibility scrim and resolve their colours through `FocusPalette` instead of `resolveAttribute`
- apply custom theme colours when the custom theme is already active, instead of ignoring them due to a short-circuit in `applyTheme()`
- preserve folder assignments across restarts and boots with incomplete package manager results
- commit encrypted folder changes synchronously and only update in-memory state after a successful write
- make favourite reordering never able to delete a favourite, and copy picked wallpapers into private storage so they survive restarts
- expand notifications on swipe-down using the official `GLOBAL_ACTION_NOTIFICATIONS` accessibility action
- show an inline error when creating or renaming a folder with a duplicate name instead of silently dismissing the dialog
- honour the system's 12/24-hour clock setting and fix the `QUERY_ALL_PACKAGES` permission status report
- fix `EncryptedPrefs.commitString`, which returned `Unit` instead of the promised `Boolean` after the `UseKtx` cleanup
- allow setting and seeing a wallpaper under the custom theme: `colorizeViewTree` filled every container with an opaque colour that was never undone and hid the image; it now only repaints backgrounds drawn in a theme colour
- stop secondary text turning primary when the view tree is repainted more than once, by remembering each view's colour role
- stop "same wallpaper everywhere" from blanking every screen: it starts from the home image, and turning it off gives each screen back its own image or keeps the shared one
- apply show/hide, choose, remove, shared mode, blur and legibility changes immediately on the settings screen itself, without recreating it
- drop stale wallpaper loads so a removed or hidden image never reappears when its decode finishes
- keep the image picker's target across rotation or process death while choosing
- tint switches under the custom theme, which were matched as text and kept the base theme's colours
- name `_` the exceptions that are dropped on purpose in thirteen `catch` blocks that returned a documented fallback, so they read differently from a lost error (detekt `SwallowedException`)
- extract `styleCalendarDay` out of `HomeActivity.populateCalendar`, which nested six levels of blocks to paint a single calendar number
- stop the app crashing when changing an already chosen wallpaper and when opening any material dialog: `FocusPhone.DialogTitleTextStyle` inherited from a `TextAppearance` with no `layout_width`, so the `m3_alert_dialog` title could not be inflated; the title and body styles now inherit from `MaterialAlertDialog.Material3.*.Text` and `MaterialAlertDialogThemeTest` checks it for every theme

#### REMOVED
- `WidgetConfigActivity`, `ClockStyle`, `WeightGradientTextView` and the unreachable icon-size-slider subsystem, along with `backup_rules.xml` and the empty `values-night/themes.xml`
- dead code found by detekt: the `onDismiss` callback of `NotificationsAdapter`, never invoked because the swipe handler calls `dismissNotification` directly; the unused `TAG` constant in `HomeActivity`; and the `totalSteps` parameter of `TutorialOverlayView.showStep`

#### SECURITY
- unify detox and schedule locks under one encrypted attempt policy (5 attempts, 15 min cooldown); a wrong password no longer ends a detox session, and verification now runs pbkdf2 off the main thread
- make secure storage report typed results (absent/present/unavailable/lost) instead of failing silently, and stop regenerating the keystore key over an existing alias
- apply `data_extraction_rules.xml` to exclude every domain from android 12+ device transfer, since `allowBackup=false` doesn't cover it
