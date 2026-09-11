# CHANGELOG

## SIN PUBLICAR / UNRELEASED

### CASTELLANO

#### ANADIDO
- añadir `FocusColors`, `FocusPalette` y `SurfaceScrim` como fuente unica de color en tiempo de ejecucion del tema, sustituyendo las resoluciones de atributo que devolvian un id de recurso en vez de un color
- añadir `ensureContrast` para garantizar contraste wcag en el tema personalizado cuando el usuario elige un fondo y un primer plano poco legibles
- añadir un scrim de legibilidad adaptativo sobre el fondo de pantalla, con un nuevo slider "legibilidad de texto" en ajustes, para todos los temas

#### CAMBIADO
- renombrar la accion "editar aplicaciones" a "editar dashboard" en ingles, español, frances y aleman
- reestructurar las pantallas de ajustes (apariencia, inicio, enfoque, notificaciones, sistema y acerca de) alrededor de un `settingsSection` comun que gestiona cabeceras y separadores
- optimizar el rendimiento en tiempo de ejecucion y el proceso de build/ci: lint sin errores en release, dependencias actualizadas, wrapper de gradle versionado y un workflow de ci
- eliminar codigo muerto y simplificar el proyecto: `WidgetConfigActivity`, `ClockStyle`, `WeightGradientTextView`, el subsistema de tamaño de icono inalcanzable y metodos publicos sin uso
- unificar el flujo de desinstalacion tras `LauncherUtils.requestUninstall` y migrar `WidgetPickerActivity` a la activity result api
- mejorar la accesibilidad, los textos i18n y los plurales en es/fr/de, y aplicar las sugerencias `UseKtx` restantes en la capa de preferencias y ui
- bajar `agp` a `9.3.0` en `libs.versions.toml` para restaurar la compatibilidad con la version actual de android studio

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

#### ELIMINADO
- `WidgetConfigActivity`, `ClockStyle`, `WeightGradientTextView` y el subsistema de tamaño de icono inalcanzable, junto con `backup_rules.xml` y el `values-night/themes.xml` vacio

#### SEGURIDAD
- unificar el bloqueo de detox y horarios bajo una unica politica de intentos cifrada (5 intentos, 15 min de espera); una contraseña incorrecta ya no finaliza la sesion de detox y la verificacion corre pbkdf2 fuera del hilo principal
- hacer que el almacenamiento seguro reporte resultados tipados (ausente/presente/no disponible/perdido) en vez de fallar en silencio, y dejar de regenerar la clave de keystore sobre un alias existente
- aplicar `data_extraction_rules.xml` para excluir todos los dominios de la transferencia de dispositivo en android 12+, ya que `allowBackup=false` no la cubre

### ENGLISH

#### ADDED
- add `FocusColors`, `FocusPalette` and `SurfaceScrim` as the single source of runtime theme colour, replacing attribute resolutions that returned a resource id instead of a colour
- add `ensureContrast` to guarantee wcag contrast for the custom theme when the user picks a hard-to-read background/foreground pair
- add an adaptive legibility scrim over the wallpaper, with a new "text legibility" slider in settings, for every theme

#### CHANGED
- rename the "edit applications" action to "edit dashboard" across english, spanish, french and german
- restructure the settings screens (appearance, home, focus, notifications, system and about) around a shared `settingsSection` helper that owns headers and separators
- optimize runtime performance and the build/ci pipeline: lint runs clean on release builds, dependencies updated, gradle wrapper jar versioned and a ci workflow added
- remove dead code and simplify the project: `WidgetConfigActivity`, `ClockStyle`, `WeightGradientTextView`, the unreachable icon-size-slider subsystem and unused public methods
- unify the uninstall flow behind `LauncherUtils.requestUninstall` and move `WidgetPickerActivity` to the activity result api
- improve accessibility, i18n text and plurals across es/fr/de, and apply the remaining `UseKtx` suggestions across the preferences layer and ui
- downgrade `agp` to `9.3.0` in `libs.versions.toml` to restore build compatibility with the current android studio release

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

#### REMOVED
- `WidgetConfigActivity`, `ClockStyle`, `WeightGradientTextView` and the unreachable icon-size-slider subsystem, along with `backup_rules.xml` and the empty `values-night/themes.xml`

#### SECURITY
- unify detox and schedule locks under one encrypted attempt policy (5 attempts, 15 min cooldown); a wrong password no longer ends a detox session, and verification now runs pbkdf2 off the main thread
- make secure storage report typed results (absent/present/unavailable/lost) instead of failing silently, and stop regenerating the keystore key over an existing alias
- apply `data_extraction_rules.xml` to exclude every domain from android 12+ device transfer, since `allowBackup=false` doesn't cover it
