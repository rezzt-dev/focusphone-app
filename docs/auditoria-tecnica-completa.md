# Auditoría técnica completa — FocusPhone

**Proyecto:** `focusphone-project` (`app.focusphone`)
**Rama analizada:** `alfa-1.0.6` (commit `e499f88`)
**Fecha del análisis:** 2026-09-11
**Alcance:** 100 % del código fuente Kotlin (`app/src/main`, `app/src/test`, `app/src/androidTest`), recursos (`res/`), manifiesto, configuración Gradle y configuración XML de servicios.

---

# 0. Cómo usar este documento

Cada hallazgo tiene un **ID estable** (`FP-###`) que no debe reutilizarse ni renumerarse nunca.

**El documento tiene dos mitades con funciones distintas:**

| Sección | Para qué sirve |
|---|---|
| [**§1 Checklist de seguimiento**](#1-checklist-de-seguimiento) | **Se edita.** Es el control del proyecto: qué está hecho, qué falta y quién lo hizo. |
| §2 – §6 (fichas por severidad) | **No se editan.** Descripción, impacto y corrección propuesta de cada hallazgo. |
| §7 – §8 (anexos) | Detalles menores sin ID y criterios de verificación. |

### Flujo de trabajo del agente

1. Abrir [§1 Checklist](#1-checklist-de-seguimiento) y tomar el **primer hallazgo pendiente de la fase activa**. Las fases están ordenadas por riesgo: no adelantar trabajo de fases posteriores.
2. Leer la ficha completa del hallazgo (el ID enlaza a ella) antes de tocar código. Si la ficha incluye `Relacionado con:`, leer también la del hallazgo asociado.
3. Aplicar la corrección. No agrupar en un mismo commit hallazgos de fases distintas.
4. Ejecutar la verificación de la fase ([§8](#8-anexo-b--criterios-de-verificación)).
5. Marcar `- [x]` en la checklist con el hash del commit, y actualizar el contador de la fase y la tabla de progreso global.

**Convención de referencias:** `ruta/al/fichero.kt:línea` — las líneas corresponden al commit `e499f88`. Tras los primeros arreglos los números se desplazarán; en ese caso localizar por nombre de método, no por línea.

### Estado de compilación en el momento del análisis

| Comprobación | Resultado |
|---|---|
| `:app:compileDebugKotlin` | ✅ BUILD SUCCESSFUL |
| `:app:testDebugUnitTest` | ✅ BUILD SUCCESSFUL (14 clases de test) |
| `:app:lintDebug` | ❌ **443 errores, 365 avisos** |
| Warnings del compilador Kotlin | 4 (APIs deprecadas) |

> El proyecto **compila y sus tests unitarios pasan**. Los problemas descritos aquí son defectos de comportamiento en tiempo de ejecución, fugas de recursos, código muerto, conflictos de diseño y deuda de configuración — no errores de compilación.

### Resumen cuantitativo

| Severidad | Nº de hallazgos |
|---|---|
| 🟥 BLOCKER | 3 |
| 🟧 CRÍTICO | 9 |
| 🟨 ALTO | 21 |
| 🟦 MEDIO | 26 |
| ⬜ BAJO | 22 |
| **Total** | **81** |

Además: **66 strings sin usar**, **19 recursos sin usar**, **~750 líneas de código muerto**, **439 errores de traducción** derivados de un único problema de configuración de locales.

---

# 1. Checklist de seguimiento

> **Esta es la sección de control del proyecto.** Los agentes marcan aquí lo que van
> resolviendo. Todo lo demás en el documento es la descripción detallada de cada punto.

### Cómo marcar

| Marca | Significado |
|---|---|
| `- [ ]` | Pendiente |
| `- [x]` | **Resuelto y verificado** — el arreglo está aplicado y la comprobación de la Fase pasa |
| `- [~]` | En curso — alguien está trabajando en ello ahora mismo |
| `- [-]` | Descartado — decisión explícita de no arreglarlo; **obligatorio** añadir el motivo en una línea debajo |

**Reglas:**

1. No marcar `- [x]` sin haber ejecutado los comandos de verificación de la fase correspondiente (ver [§8](#8-anexo-b--criterios-de-verificación)).
2. Al marcar, añadir el hash del commit entre paréntesis al final de la línea: `- [x] **FP-001** — … (a1b2c3d)`.
3. Actualizar el contador de la fase y la tabla de progreso global al cerrar cada bloque.
4. Un hallazgo con `Relacionado con:` en su ficha no debe cerrarse sin revisar el hallazgo asociado.

---

## 1.1 Progreso global

Actualizar esta tabla cada vez que se cierre un bloque de trabajo.

| Fase | Hallazgos | Resueltos | Estado |
|---|---|---|---|
| **Fase 1** — Seguridad y pérdida de datos | 8 | 8 | ✅ Completada |
| **Fase 2** — Fugas de recursos y corrección funcional | 15 | 15 | ✅ Completada |
| **Fase 3** — Coherencia interna | 12 | 12 | ✅ Completada |
| **Fase 4** — Rendimiento | 10 | 10 | ✅ Completada |
| **Fase 5** — Build, calidad y CI | 15 | 15 | ✅ Completada |
| **Fase 6** — Limpieza y código muerto | 11 | 9 | 🟡 En curso |
| **Fase 7** — Localización y accesibilidad | 12 | 0 | ⬜ Pendiente |
| **TOTAL** | **83** | **69** | **83 %** |

**Por severidad:**

| Severidad | Total | Resueltos |
|---|---|---|
| 🟥 BLOCKER | 3 | 3 |
| 🟧 CRÍTICO | 9 | 9 |
| 🟨 ALTO | 21 | 21 |
| 🟦 MEDIO | 26 | 17 |
| ⬜ BAJO | 24 | 19 |

> **Criterio de release:** no se publica ninguna build mientras queden hallazgos 🟥 o 🟧 sin cerrar.

---

## 1.2 Fase 1 — Seguridad y pérdida de datos

*Bloquea cualquier release. Nada se publica hasta que esta fase esté completa.*

**Progreso: 8 / 8**

- [x] 🟥 **FP-001** — [El bloqueo Detox se puede saltar con 5 pulsaciones, sin contraseña](#fp-001--el-bloqueo-detox-se-puede-saltar-con-5-pulsaciones-sin-contraseña) (24052f4)
- [x] 🟥 **FP-002** — [Los fondos de pantalla se pierden al reiniciar el proceso](#fp-002--los-fondos-de-pantalla-se-pierden-al-reiniciar-el-proceso) (24052f4)
- [x] 🟥 **FP-003** — [Pérdida de datos: apps favoritas por encima del límite se borran al reordenar](#fp-003--pérdida-de-datos-apps-favoritas-por-encima-del-límite-se-borran-al-reordenar) (24052f4)
- [x] 🟧 **FP-007** — [Corrupción permanente de las etiquetas personalizadas si el Keystore se invalida](#fp-007--corrupción-permanente-de-las-etiquetas-personalizadas-si-el-keystore-se-invalida) (24052f4)
- [x] 🟧 **FP-008** — [Fallos de cifrado silenciosos provocan pérdida de configuración](#fp-008--fallos-de-cifrado-silenciosos-provocan-pérdida-de-configuración) (24052f4)
- [x] 🟨 **FP-013** — [Dos políticas de bloqueo por contraseña contradictorias y duplicadas](#fp-013--dos-políticas-de-bloqueo-por-contraseña-contradictorias-y-duplicadas) (24052f4)
- [x] 🟨 **FP-014** — [`ProfilePreferences` mezcla almacenamiento cifrado y en claro en el mismo fichero](#fp-014--profilepreferences-mezcla-almacenamiento-cifrado-y-en-claro-en-el-mismo-fichero) (24052f4)
- [x] 🟨 **FP-015** — [`verifyDetoxPassword` no limpia los intentos fallidos al acertar](#fp-015--verifydetoxpassword-no-limpia-los-intentos-fallidos-al-acertar) (24052f4)

---

## 1.3 Fase 2 — Fugas de recursos y corrección funcional

*Defectos que degradan la app con el uso o rompen flujos concretos.*

**Progreso: 15 / 15**

- [x] 🟧 **FP-004** — [Fuga de corrutinas y de `ItemTouchHelper` en el panel de notificaciones](#fp-004--fuga-de-corrutinas-y-de-itemtouchhelper-en-el-panel-de-notificaciones) (edf460c)
- [x] 🟧 **FP-005** — [Fuga de memoria no acotada de `PendingIntent` en el listener de notificaciones](#fp-005--fuga-de-memoria-no-acotada-de-pendingintent-en-el-listener-de-notificaciones) (edf460c)
- [x] 🟧 **FP-006** — [Notificaciones obsoletas persisten tras revocar el permiso](#fp-006--notificaciones-obsoletas-persisten-tras-revocar-el-permiso) (edf460c)
- [x] 🟧 **FP-009** — [Fuga de IDs de widget: el widget nunca se libera del host](#fp-009--fuga-de-ids-de-widget-el-widget-nunca-se-libera-del-host) (edf460c)
- [x] 🟨 **FP-016** — [El `recreate()` por cambio de fondo es código muerto](#fp-016--el-recreate-por-cambio-de-fondo-es-código-muerto) (edf460c)
- [x] 🟨 **FP-018** — [`AppRepositoryImpl`: caché sin sincronización e invalidación incompleta](#fp-018--apprepositoryimpl-caché-sin-sincronización-e-invalidación-incompleta) (edf460c)
- [x] 🟨 **FP-019** — [`AllAppsActivity` no refresca la lista al volver de una instalación](#fp-019--allappsactivity-no-refresca-la-lista-al-volver-de-una-instalación) (edf460c)
- [x] 🟨 **FP-020** — [Diálogos filtrados: `activeDialog` se sobrescribe sin cerrar el anterior](#fp-020--diálogos-filtrados-activedialog-se-sobrescribe-sin-cerrar-el-anterior) (edf460c)
- [x] 🟨 **FP-021** — [Diálogos mostrados desde corrutinas sin comprobar el estado de la Activity](#fp-021--diálogos-mostrados-desde-corrutinas-sin-comprobar-el-estado-de-la-activity) (edf460c)
- [x] 🟨 **FP-022** — [Reordenar favoritas: `ListAdapter` + `notifyItemMoved` produce posiciones incoherentes](#fp-022--reordenar-favoritas-listadapter--notifyitemmoved-produce-posiciones-incoherentes) (edf460c)
- [x] 🟨 **FP-023** — [La pausa consciente lanza la app desde segundo plano (y falla en silencio)](#fp-023--la-pausa-consciente-lanza-la-app-desde-segundo-plano-y-falla-en-silencio) (edf460c)
- [x] 🟨 **FP-024** — [Bucle de sondeo infinito en el onboarding](#fp-024--bucle-de-sondeo-infinito-en-el-onboarding) (edf460c)
- [x] 🟦 **FP-045** — [`ToggleFavouriteAppUseCase` usado como "eliminar": puede volver a añadir la app](#fp-045--togglefavouriteappusecase-usado-como-eliminar-puede-volver-a-añadir-la-app) (edf460c)
- [x] 🟦 **FP-046** — [`AppFolderPreferences` no persiste la deduplicación defensiva](#fp-046--appfolderpreferences-no-persiste-la-deduplicación-defensiva) (edf460c)
- [x] 🟦 **FP-047** — [El bloqueo de detox cacheado puede tener hasta 3 s de retraso](#fp-047--el-bloqueo-de-detox-cacheado-puede-tener-hasta-3-s-de-retraso) (edf460c)

---

## 1.4 Fase 3 — Coherencia interna

*Conflictos entre subsistemas: lógica duplicada con reglas distintas y ajustes sin efecto.*

**Progreso: 12 / 12**

- [x] 🟨 **FP-025** — [Cuatro implementaciones distintas de "¿soy el launcher por defecto?"](#fp-025--cuatro-implementaciones-distintas-de-soy-el-launcher-por-defecto) (2e4989a)
- [x] 🟨 **FP-026** — [`FocusAccessibilityService.isEnabled()` puede devolver falso negativo](#fp-026--focusaccessibilityserviceisenabled-puede-devolver-falso-negativo) (2e4989a)
- [x] 🟨 **FP-030** — [El fondo del panel de notificaciones es un ajuste sin efecto](#fp-030--el-fondo-del-panel-de-notificaciones-es-un-ajuste-sin-efecto) (2e4989a)
- [x] 🟨 **FP-031** — [Vaciar las palabras clave de "trabajo" restaura los valores por defecto](#fp-031--vaciar-las-palabras-clave-de-trabajo-restaura-los-valores-por-defecto) (2e4989a)
- [x] 🟨 **FP-032** — [`getFontSizePt` escribe en disco desde un getter y recupera al valor **máximo**](#fp-032--getfontsizept-escribe-en-disco-desde-un-getter-y-recupera-al-valor-máximo) (2e4989a)
- [x] 🟦 **FP-035** — [Código duplicado íntegro del reloj y del widget entre `HomeActivity` y `HomeLayoutActivity`](#fp-035--código-duplicado-íntegro-del-reloj-y-del-widget-entre-homeactivity-y-homelayoutactivity) (2e4989a)
- [x] 🟦 **FP-036** — [Dos hosts de widget vivos simultáneamente sobre el mismo ID](#fp-036--dos-hosts-de-widget-vivos-simultáneamente-sobre-el-mismo-id) (2e4989a)
- [x] 🟦 **FP-041** — [`HomeLayoutActivity` registra un receptor sin flag de exportación](#fp-041--homelayoutactivity-registra-un-receptor-sin-flag-de-exportación) (2e4989a)
- [x] 🟦 **FP-048** — [Estado de onboarding duplicado y descoordinado](#fp-048--estado-de-onboarding-duplicado-y-descoordinado) (2e4989a)
- [x] 🟦 **FP-049** — [Tres sistemas de "tutorial" paralelos y desalineados](#fp-049--tres-sistemas-de-tutorial-paralelos-y-desalineados) (2e4989a)
- [x] 🟦 **FP-050** — [Los temas oscuros no heredan de `Base.Theme.FocusPhone`](#fp-050--los-temas-oscuros-no-heredan-de-basethemefocusphone) (2e4989a)
- [x] 🟦 **FP-051** — [El estilo `FocusPhone.MaterialAlertDialog` nunca se aplica](#fp-051--el-estilo-focusphonematerialalertdialog-nunca-se-aplica) (2e4989a)

---

## 1.5 Fase 4 — Rendimiento

*Trabajo innecesario en el hilo principal, en cada frame o en cada reanudación.*

**Progreso: 10 / 10**

- [x] 🟨 **FP-017** — [El fondo de pantalla se decodifica en cada `onResume` de cada pantalla](#fp-017--el-fondo-de-pantalla-se-decodifica-en-cada-onresume-de-cada-pantalla) (4e2b296)
- [x] 🟨 **FP-027** — [El servicio de accesibilidad escribe en disco cifrado desde el hilo principal](#fp-027--el-servicio-de-accesibilidad-escribe-en-disco-cifrado-desde-el-hilo-principal) (4e2b296)
- [x] 🟨 **FP-028** — [`FocusPalette.of()` se resuelve en cada frame durante el arrastre](#fp-028--focuspaletteof-se-resuelve-en-cada-frame-durante-el-arrastre) (4e2b296)
- [x] 🟨 **FP-029** — [`NotificationsAdapter` lee preferencias y resuelve la paleta en cada `bind`](#fp-029--notificationsadapter-lee-preferencias-y-resuelve-la-paleta-en-cada-bind) (4e2b296)
- [x] 🟨 **FP-033** — [Los deslizadores escriben en SharedPreferences en cada paso del arrastre](#fp-033--los-deslizadores-escriben-en-sharedpreferences-en-cada-paso-del-arrastre) (4e2b296)
- [x] 🟦 **FP-034** — [`HomeActivity.onCreate` no interrumpe la inicialización al lanzar el onboarding](#fp-034--homeactivityoncreate-no-interrumpe-la-inicialización-al-lanzar-el-onboarding) (4e2b296)
- [x] 🟦 **FP-037** — [`dispatchTouchEvent` alimenta el `GestureDetector` con todos los eventos](#fp-037--dispatchtouchevent-alimenta-el-gesturedetector-con-todos-los-eventos) (4e2b296)
- [x] 🟦 **FP-042** — [`WidgetPickerActivity` carga todos los proveedores en el hilo principal](#fp-042--widgetpickeractivity-carga-todos-los-proveedores-en-el-hilo-principal) (4e2b296)
- [x] 🟦 **FP-052** — [`colorizeViewTree` usa excepciones como flujo de control](#fp-052--colorizeviewtree-usa-excepciones-como-flujo-de-control) (4e2b296)
- [x] ⬜ **FP-081** — [`Overdraw` en todos los layouts de ítem](#fp-081--overdraw-en-todos-los-layouts-de-ítem) (4e2b296)

---

## 1.6 Fase 5 — Build, calidad y CI

*Requisito para poder verificar automáticamente todo lo anterior.*

**Progreso: 15 / 15**

- [x] 🟧 **FP-011** — [`Lint` está desactivado en release, ocultando 443 errores](#fp-011--lint-está-desactivado-en-release-ocultando-443-errores) (eb4ba0e)
- [x] 🟧 **FP-012** — [`BIND_APPWIDGET` declarado: permiso exclusivo de apps de sistema](#fp-012--bind_appwidget-declarado-permiso-exclusivo-de-apps-de-sistema) (eb4ba0e)
- [x] 🟦 **FP-053** — [Errores de lint `ResourceType` en `colorizeViewTree`](#fp-053--errores-de-lint-resourcetype-en-colorizeviewtree) (eb4ba0e)
- [x] 🟦 **FP-054** — [Configuración de locales mal formada: 439 errores de lint](#fp-054--configuración-de-locales-mal-formada-439-errores-de-lint) (eb4ba0e)
- [x] 🟦 **FP-055** — [Traducciones realmente ausentes e inconsistencia de argumentos de formato](#fp-055--traducciones-realmente-ausentes-e-inconsistencia-de-argumentos-de-formato) (eb4ba0e)
- [x] ⬜ **FP-071** — [Dependencias desactualizadas](#fp-071--dependencias-desactualizadas) (eb4ba0e)
- [x] ⬜ **FP-072** — [`versionName` desincronizado con la rama y los commits](#fp-072--versionname-desincronizado-con-la-rama-y-los-commits) (eb4ba0e)
- [x] ⬜ **FP-073** — [`gradle-wrapper.jar` no está versionado](#fp-073--gradle-wrapperjar-no-está-versionado) (eb4ba0e)
- [x] ⬜ **FP-074** — [`org.gradle.java.home` con ruta absoluta local](#fp-074--orggradlejavahome-con-ruta-absoluta-local) (eb4ba0e)
- [x] ⬜ **FP-075** — [APK de release y artefactos de build versionados](#fp-075--apk-de-release-y-artefactos-de-build-versionados) (eb4ba0e)
- [x] ⬜ **FP-076** — [`docs/` está en el `.gitignore` raíz](#fp-076--docs-está-en-el-gitignore-raíz) (eb4ba0e)
- [x] ⬜ **FP-077** — [Tests de plantilla en el paquete equivocado](#fp-077--tests-de-plantilla-en-el-paquete-equivocado) (eb4ba0e)
- [x] 🟧 **FP-010** — [`TestFocusPhoneApp.container` crea un contenedor nuevo en cada acceso](#fp-010--testfocusphoneappcontainer-crea-un-contenedor-nuevo-en-cada-acceso) (eb4ba0e)
- [x] ⬜ **FP-082** — [Error de lint `WrongConstant` en `expandNotificationsPanel`](#fp-082--error-de-lint-wrongconstant-en-expandnotificationspanel) (eb4ba0e)
- [x] ⬜ **FP-083** — `RepeatOnLifecycleWrongUsage`: `HomeActivity` inicia sus observadores desde `onResume` tras el onboarding (eb4ba0e)

---

## 1.7 Fase 6 — Limpieza y código muerto

*Sin riesgo funcional. Hacer después de las fases anteriores para no reintroducir lo eliminado.*

**Progreso: 9 / 11**

- [x] ⬜ **FP-060** — [Clases y ficheros completamente muertos](#fp-060--clases-y-ficheros-completamente-muertos) (b6db552)
- [x] ⬜ **FP-061** — [Los deslizadores de tamaño de `HomeLayoutActivity` son inalcanzables](#fp-061--los-deslizadores-de-tamaño-de-homelayoutactivity-son-inalcanzables) (b6db552)
- [x] ⬜ **FP-062** — [Métodos públicos sin uso](#fp-062--métodos-públicos-sin-uso) (b6db552)
- [x] ⬜ **FP-063** — [Constantes y variables sin usar](#fp-063--constantes-y-variables-sin-usar) (b6db552)
- [x] ⬜ **FP-064** — [Código duplicado literal dentro de `HomeActivity.onResume`](#fp-064--código-duplicado-literal-dentro-de-homeactivityonresume) (b6db552)
- [x] ⬜ **FP-065** — [Ficheros de configuración de backup muertos](#fp-065--ficheros-de-configuración-de-backup-muertos) (b6db552)
- [x] ⬜ **FP-066** — [`values-night/themes.xml` vacío](#fp-066--values-nightthemesxml-vacío) (b6db552)
- [ ] ⬜ **FP-067** — [66 strings sin usar](#fp-067--66-strings-sin-usar)
- [ ] ⬜ **FP-068** — [19 recursos sin usar](#fp-068--19-recursos-sin-usar)
- [x] ⬜ **FP-069** — [APIs deprecadas (avisos del compilador)](#fp-069--apis-deprecadas-avisos-del-compilador) (b6db552)
- [x] ⬜ **FP-070** — [85 avisos `UseKtx`](#fp-070--85-avisos-usektx) (b6db552)

---

## 1.8 Fase 7 — Localización y accesibilidad

*Pulido final de cara al usuario.*

**Progreso: 0 / 12**

- [ ] 🟦 **FP-038** — [`expandNotificationsPanel()` usa reflexión bloqueada en Android moderno](#fp-038--expandnotificationspanel-usa-reflexión-bloqueada-en-android-moderno)
- [ ] 🟦 **FP-039** — [El ancho de los cajones usa `displayMetrics.widthPixels`](#fp-039--el-ancho-de-los-cajones-usa-displaymetricswidthpixels)
- [ ] 🟦 **FP-040** — [El "long press" del editor de layout se dispara a los 200 ms](#fp-040--el-long-press-del-editor-de-layout-se-dispara-a-los-200-ms)
- [ ] 🟦 **FP-043** — [El diálogo de licencias muestra HTML en crudo](#fp-043--el-diálogo-de-licencias-muestra-html-en-crudo)
- [ ] 🟦 **FP-044** — [Los diálogos de creación/renombrado de carpeta pierden lo escrito ante un nombre duplicado](#fp-044--los-diálogos-de-creaciónrenombrado-de-carpeta-pierden-lo-escrito-ante-un-nombre-duplicado)
- [ ] 🟦 **FP-056** — [Los nombres de app se fuerzan a minúsculas y se eliminan los acentos](#fp-056--los-nombres-de-app-se-fuerzan-a-minúsculas-y-se-eliminan-los-acentos)
- [ ] 🟦 **FP-057** — [El reloj ignora el ajuste de 12/24 horas del sistema](#fp-057--el-reloj-ignora-el-ajuste-de-1224-horas-del-sistema)
- [ ] 🟦 **FP-058** — [`AnalogClockView` describe AM/PM incluso en locales de 24 h](#fp-058--analogclockview-describe-ampm-incluso-en-locales-de-24-h)
- [ ] 🟦 **FP-059** — [`PermissionsActivity` muestra un estado siempre "concedido" para `QUERY_ALL_PACKAGES`](#fp-059--permissionsactivity-muestra-un-estado-siempre-concedido-para-query_all_packages)
- [ ] ⬜ **FP-078** — [Faltan `contentDescription` y atributos de accesibilidad](#fp-078--faltan-contentdescription-y-atributos-de-accesibilidad)
- [ ] ⬜ **FP-079** — [Texto embebido en layouts y concatenación en `setText`](#fp-079--texto-embebido-en-layouts-y-concatenación-en-settext)
- [ ] ⬜ **FP-080** — [Candidatos a `plurals` y erratas](#fp-080--candidatos-a-plurals-y-erratas)

---

## 1.9 Verificación obligatoria por fase

Marcar solo cuando la fase entera esté cerrada y las comprobaciones pasen.

- [x] **Fase 1 cerrada** — Detox resiste 6 contraseñas incorrectas · fondo sobrevive a un arranque en frío · reordenar con >10 favoritas no borra ninguna
- [x] **Fase 2 cerrada** — 20 ciclos de home + swipe descarta **una** notificación · 5 ciclos añadir/quitar widget sin acumular IDs (`adb shell dumpsys appwidget`) (edf460c; regresión de widget automatizada en `WidgetSlotTest`)
- [x] **Fase 3 cerrada** — un único helper de "launcher por defecto" · una única política de contraseña · ningún ajuste visible sin efecto (2e4989a)
- [x] **Fase 4 cerrada** — sin decodificación de bitmap por `onResume` · sin lecturas de preferencias en `onBindViewHolder` ni en `onChildDraw`
- [x] **Fase 5 cerrada** — `./gradlew :app:lintDebug` con **0 errores** · `checkReleaseBuilds = true` · clon limpio ejecuta `./gradlew` (eb4ba0e; 149 tests unitarios en verde)
- [ ] **Fase 6 cerrada** — pendiente FP-067/FP-068 (poda de recursos sin usar; lint sigue reportando 133 `UnusedResources`). El resto de la fase está cerrado (b6db552; `compileDebugKotlin` sin warnings, 156/156 tests, `lintDebug` 0 errores)
- [ ] **Fase 7 cerrada** — 0 `MissingTranslation` · 0 `ContentDescription` · 0 `HardcodedText`

---

## 1.10 Registro de decisiones

Anotar aquí toda desviación del documento: hallazgos descartados, alcance ampliado o problemas nuevos
encontrados durante la corrección. Un hallazgo nuevo se numera `FP-082` en adelante y se añade a su fase.

| Fecha | Hallazgo | Decisión | Motivo | Autor |
|---|---|---|---|---|
| 2026-09-11 | FP-001 / FP-013 | Detox adopta la política de Schedule mediante un `PasswordAttemptPolicy` compartido (5 intentos, cooldown de 15 min, nada se borra) | Una única política; la expiración natural del temporizador ya garantiza que nadie queda atrapado | Claude |
| 2026-09-11 | FP-003 | Se eliminan los `.take()` de lectura: una BD heredada con >10 favoritas las muestra todas hasta que el usuario las reduzca | Truncar al leer ocultaba datos y recortar la BD sería pérdida de datos; el límite se aplica al escribir (migración y `ToggleFavouriteAppUseCase`) | Claude |
| 2026-09-11 | FP-008 | `StoredValue` distingue `Unavailable` (clave del Keystore no disponible: nunca se sobrescribe) de `Lost` (payload irrecuperable: se trata como vacío) | Negarse a escribir para siempre dejaría horarios y carpetas inutilizables tras un reseteo del Keystore | Claude |
| 2026-09-11 | FP-008 (alcance ampliado) | `getOrCreateKey()` ya no genera una clave nueva si el alias existe pero su lectura falla | Un fallo transitorio de lectura reemplazaba la clave y destruía todos los valores cifrados | Claude |
| 2026-09-11 | Anexo A | Resueltos: PBKDF2 en el hilo principal y `getRemainingAttempts()` devolviendo 5 durante el cooldown | Consecuencia directa de FP-013 | Claude |
| 2026-09-11 | FP-082 (nuevo) | Añadido a la Fase 5: `WrongConstant` en `expandNotificationsPanel` | Único error de lint no catalogado; sin él la Fase 5 no puede cerrar con 0 errores. La reflexión en sí sigue en FP-038 | Claude |
| 2026-09-11 | FP-076 | Se usa `docs/*` + `!docs/auditoria-tecnica-completa.md`, no `docs/` + negación como proponía la ficha | Git no puede reincluir un fichero si su directorio padre está excluido: la corrección de la ficha no tenía efecto | Claude |
| 2026-09-11 | FP-010 | El contenedor de test se cachea y solo se reconstruye al cambiar un repositorio fake, en lugar de `by lazy` | La `Application` sobrevive a todos los tests; con `by lazy` un `@Before` que inyecta fakes tras un test anterior quedaría ignorado. Se elimina el `onCreate` con comentario engañoso | Claude |
| 2026-09-11 | FP-074 | `org.gradle.java.home` se sustituye por los criterios de toolchain del daemon (`gradle/gradle-daemon-jvm.properties`), no por `kotlin { jvmToolchain(21) }` | La propiedad fijaba la JVM del daemon (jlink para `JdkImageTransform`), no la del compilador; el bytecode sigue apuntando a Java 11 | Claude |
| 2026-09-11 | FP-011 | Sin `lint-baseline.xml` | Tras la fase quedan 0 errores; los avisos restantes son hallazgos de las Fases 6-7 y un baseline los ocultaría | Claude |
| 2026-09-11 | FP-055 | `settings_tutorial_desc` pasa a `formatted="false"` en los cuatro idiomas | La cadena nunca se formatea: el `100%%` español se mostraba literalmente como "100%%". `allapps_folder_caret`/`allapps_new_folder_symbol` ya eran `translatable="false"` | Claude |
| 2026-09-11 | FP-073 | El JAR del wrapper se regenera con la tarea `wrapper` antes de versionarlo (SHA-256 `7a9ce74c…`, idéntico al oficial de Gradle 9.7.1) | El JAR local no coincidía con el oficial de la versión declarada; versionar un binario sin verificar sería un riesgo de cadena de suministro. CI lo valida con `setup-gradle` | Claude |
| 2026-09-11 | FP-072 | `versionCode` 3 / `versionName` 1.0.6, fijados a mano | No hay tags de git y CI trabaja en HEAD desacoplado: derivar la versión de la rama sería frágil | Claude |
| 2026-09-11 | Fase 6 (b6db552) | FP-067/FP-068 quedan fuera de este cierre; el resto de la fase (FP-060 a FP-066, FP-069, FP-070) se cierra en el mismo commit | Podar strings/recursos sin uso en los 4 idiomas es mecánico pero extenso; se hace en un pase propio para no mezclar la eliminación de código muerto con la de recursos | Claude |
| 2026-09-11 | FP-070 (alcance ampliado) | El pase `UseKtx` detectó y corrigió un bug real: `EncryptedPrefs.commitString` usaba `edit(commit = true) { }`, que devuelve `Unit`, no el `Boolean` que la firma promete a sus llamadores | Sin la corrección el código no compilaba; se deja registrado porque no estaba en la ficha original | Claude |
| 2026-09-11 | FP-083 (nuevo) | Añadido a la Fase 5: al terminar el onboarding, `HomeActivity.onResume` hace `recreate()` en lugar de llamar a `initializeHome()` | El diferido de FP-034 lanzaba `repeatOnLifecycle` desde `onResume` (error de lint `RepeatOnLifecycleWrongUsage`); recrear una única vez en la vida de la app mantiene toda la inicialización en `onCreate` | Claude |
| 2026-09-11 | Alcance ampliado | Nuevo workflow `.github/workflows/android-ci.yml` (tests unitarios, `lintDebug`, `assembleRelease` sin firmar) | La fase es "Build, calidad y CI" y su criterio es que un clon limpio ejecute `./gradlew`; CI lo comprueba en cada push | Claude |
---

# 2. 🟥 BLOCKER — Rompen la propuesta de valor del producto

## FP-001 — El bloqueo Detox se puede saltar con 5 pulsaciones, sin contraseña

**Ficheros:**
- `focusphone-project/app/src/main/java/app/focusphone/ui/home/HomeActivity.kt:920-936`
- `focusphone-project/app/src/main/java/app/focusphone/ui/detox/DetoxBlockedActivity.kt:124-138`
- `focusphone-project/app/src/main/java/app/focusphone/data/preferences/ProfilePreferences.kt:100-102`

**Descripción:**
En ambas pantallas de desbloqueo, el manejador del botón "OK" empieza así:

```kotlin
if (ProfilePreferences.isPasswordLockedOut(this)) {
    Toast.makeText(this, getString(R.string.detox_force_disabled), Toast.LENGTH_LONG).show()
    ProfilePreferences.setDetoxPassword(this, null)
    ProfilePreferences.setDetoxEndTime(this, 0L)
    ProfilePreferences.setProfile(this, ProfilePreferences.PROFILE_NORMAL)
    ProfilePreferences.clearFailedAttempts(this)
    onSuccess()          // ← desbloquea sin verificar nada
    dialog.dismiss()
    return@setOnClickListener
}
```

`isPasswordLockedOut()` devuelve `true` cuando hay ≥ 4 intentos fallidos. El mismo bloque se repite en la rama `else` (`attempts >= 4`). Resultado: **el usuario introduce 4 contraseñas cualesquiera incorrectas y, en el quinto intento, la sesión de detox se desactiva completamente y se borra la contraseña**.

**Impacto:** La función central de la aplicación —el bloqueo digital por tiempo— no ofrece ninguna resistencia real. Cualquier usuario con un impulso de desbloqueo lo consigue en menos de 15 segundos. El bloqueo por accesibilidad (`FocusAccessibilityService`) queda igualmente anulado porque lee `cachedProfile`/`cachedIsDetoxActive` de estas mismas preferencias.

**Conflicto de diseño subyacente:** El KDoc de `ProfilePreferences` (líneas 16-18) documenta esto como política deliberada anti-bloqueo ("After 4 failures the detox session is force-disabled"), mientras que `AppSchedulePreferences` (líneas 26-33) documenta explícitamente la política **opuesta** y razona por qué la de Detox sería un "bypass de un minuto". Las dos funciones de bloqueo del producto siguen políticas contradictorias.

**Corrección propuesta:**
Alinear Detox con la política ya razonada de `AppSchedulePreferences`:
1. Sustituir el "force-disable" por un **cooldown** (`LOCKOUT_DURATION_MS`), sin borrar contraseña, `detoxEndTime` ni perfil.
2. Mostrar el tiempo restante de cooldown en el diálogo en lugar de desbloquear.
3. Mantener como salvaguarda anti-bloqueo permanente únicamente la expiración natural del temporizador (`expireDetoxIfNeeded`), que ya garantiza que nadie queda atrapado indefinidamente.
4. Extraer la lógica de intentos/cooldown a un único componente compartido por Detox y Schedule (ver **FP-013**).

---

## FP-002 — Los fondos de pantalla se pierden al reiniciar el proceso

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/settings/AppearanceSettingsActivity.kt:588-597`

**Descripción:**
El selector usa `ActivityResultContracts.PickVisualMedia()` (Android Photo Picker). Después se intenta:

```kotlin
contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
```

**El Photo Picker no concede permisos persistibles.** Esa llamada lanza `SecurityException`, que se captura y solo se registra con `Log.w`. El URI se guarda igualmente en `WallpaperPreferences`, pero el permiso de lectura muere con el proceso.

**Impacto:** El usuario elige un fondo, lo ve correctamente, y tras el siguiente arranque en frío (o reinicio del dispositivo) `WallpaperUtils.loadBitmap()` falla con `SecurityException`, devuelve `null` y **el fondo desaparece sin ningún mensaje**. Afecta a las tres superficies (home, ajustes, notificaciones).

**Corrección propuesta:**
Copiar el bitmap elegido al almacenamiento privado de la app en `handleWallpaperSelected()` y guardar la ruta del fichero local en lugar del `content://` URI:

```kotlin
// pseudo
val dest = File(filesDir, "wallpaper_${target.name.lowercase()}.jpg")
contentResolver.openInputStream(uri)?.use { input -> dest.outputStream().use(input::copyTo) }
WallpaperPreferences.setXxxWallpaperUri(this, dest.toURI().toString())
```

Añadir además:
- Borrado del fichero anterior al sustituirlo (evitar acumulación).
- Reescalado al copiar (reutilizar `MAX_BITMAP_DIMENSION` de `WallpaperUtils`).
- Migración: si el URI guardado es `content://` y falla la lectura, limpiar la preferencia y avisar al usuario en lugar de fallar en silencio.

**Relacionado con:** FP-017 (sin caché de bitmap), FP-030 (fondo de notificaciones muerto).

---

## FP-003 — Pérdida de datos: apps favoritas por encima del límite se borran al reordenar

**Ficheros:**
- `focusphone-project/app/src/main/java/app/focusphone/domain/usecase/GetFavouriteAppsUseCase.kt:16`
- `focusphone-project/app/src/main/java/app/focusphone/data/repository/FavouriteAppRepositoryImpl.kt:26-56`
- `focusphone-project/app/src/main/java/app/focusphone/domain/usecase/UpdateAppsOrderUseCase.kt`
- `focusphone-project/app/src/main/java/app/focusphone/ui/home/HomeActivity.kt:643-646`

**Descripción:**
Cadena de tres eslabones:

1. `migrateFromSharedPreferences()` inserta en Room **todas** las apps del formato antiguo, sin aplicar `MAX_FAVOURITES` (10).
2. `GetFavouriteAppsUseCase` hace `.take(MAX_FAVOURITES)` — la UI solo ve 10; las demás quedan invisibles pero presentes en la BD.
3. Al arrastrar para reordenar, `commitReorder()` devuelve **solo la lista visible**, y `UpdateAppsOrderUseCase` → `replaceAll()` → `dao.replaceAllOrdered()` hace `deleteAll()` + `insertAll()`.

**Impacto:** Las favoritas ocultas se **eliminan permanentemente** en cuanto el usuario reordena la lista una sola vez. Sin aviso, sin deshacer. La misma ruta existe en el "preview" de `HomeLayoutActivity.kt:653-660`.

**Corrección propuesta:**
1. Aplicar el límite en el punto de escritura, no en el de lectura: truncar en `migrateFromSharedPreferences()` a `MAX_FAVOURITES`.
2. Eliminar el `.take()` de `GetFavouriteAppsUseCase` y de `GetInstalledAppsUseCase` una vez garantizada la invariante.
3. Sustituir `replaceAllOrdered` (delete-all + insert-all) por un `@Update` de solo el campo `sortOrder`, para que reordenar nunca pueda borrar filas.

---

# 3. 🟧 CRÍTICO

## FP-004 — Fuga de corrutinas y de `ItemTouchHelper` en el panel de notificaciones

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/home/HomeActivity.kt:146, 192, 1129-1224`

**Descripción:**
`setupNotificationPanel()` se invoca desde `onCreate()` **y desde `onResume()`**. Cada invocación:

- Reasigna `layoutManager` y `adapter` de `rvNotifications`.
- **Crea y adjunta un `ItemTouchHelper` nuevo** (`swipeHelper.attachToRecyclerView(...)`) — los anteriores no se desatan.
- **Lanza una corrutina nueva** `lifecycleScope.launch { repeatOnLifecycle(STARTED) { NotificationRepository.notifications.collect { … } } }`, que vive hasta `onDestroy`.

**Impacto:** Tras N reanudaciones de la home (que en un launcher son decenas o cientos al día) hay N colectores del mismo `StateFlow` y N helpers de swipe. Consecuencias: CPU y memoria crecientes, `submitList` ejecutado N veces por emisión, y **un swipe de descarte dispara `dismissNotification()` N veces**. Además, la variable `enabled` queda capturada en el closure, por lo que los colectores viejos usan un valor obsoleto de la preferencia.

**Corrección propuesta:**
Separar en dos métodos:
- `setupNotificationPanel()` — llamado **solo desde `onCreate`**: RecyclerView, `ItemTouchHelper`, listeners, y una única corrutina de observación que lea `NotificationPanelPreferences.isEnabled(this)` *dentro* del `collect`.
- `refreshNotificationPanelState()` — llamado desde `onResume`: únicamente visibilidad del indicador y `setDrawerLockMode`.

---

## FP-005 — Fuga de memoria no acotada de `PendingIntent` en el listener de notificaciones

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/service/FocusNotificationService.kt:114-127`

**Descripción:**
```kotlin
private val pendingIntents = mutableMapOf<String, PendingIntent>()
```
Es un `HashMap` **estático** (en `companion object`), por lo que vive todo el proceso. Problemas:

1. **No es thread-safe.** Se escribe desde el hilo del listener (`refreshNotifications`) y se lee desde el hilo principal (`HomeActivity.onNotificationClick:1229`). Riesgo de `ConcurrentModificationException`.
2. **Crece sin límite.** `storePendingIntent` se llama para cada notificación activa en cada refresco; `removePendingIntent` solo se llama en `onNotificationRemoved`. Si el listener está desconectado cuando se retira una notificación, la entrada nunca se elimina.
3. `clearPendingIntents()` está definido pero **nunca se llama** desde ningún sitio.
4. Contradice la garantía de privacidad del KDoc de la clase: mantiene referencias a intents de otras apps durante toda la vida del proceso.

**Corrección propuesta:**
- Cambiar a `ConcurrentHashMap` o proteger con `synchronized`.
- Reconciliar el mapa en cada `refreshNotifications()`: eliminar toda clave que no esté en `activeNotifications`.
- Sobrescribir `onListenerDisconnected()` para llamar a `clearPendingIntents()` **y** a `NotificationRepository.clear()`.

---

## FP-006 — Notificaciones obsoletas persisten tras revocar el permiso

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/service/FocusNotificationService.kt`

**Descripción:** No se sobrescribe `onListenerDisconnected()`. Cuando el usuario revoca el acceso a notificaciones (o el sistema desconecta el listener), `NotificationRepository` conserva la última lista en memoria.

**Impacto:** El panel de la home sigue mostrando **contenido de notificaciones antiguas** —títulos y cuerpos de mensajes— indefinidamente, sin permiso vigente. Problema de privacidad y de corrección funcional.

**Corrección propuesta:**
```kotlin
override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    NotificationRepository.clear()
    clearPendingIntents()
}
```

---

## FP-007 — Corrupción permanente de las etiquetas personalizadas si el Keystore se invalida

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/data/preferences/CustomAppLabelsPreferences.kt:53-79`

**Descripción:**
```kotlin
val decrypted = SecureStorageHelper.decrypt(appContext, value)
if (decrypted != null) {
    map[key] = decrypted
} else {
    // Assume legacy plain-text value; migrate transparently
    map[key] = value
    plainValuesToMigrate[key] = value   // ← re-cifra el ciphertext como si fuera texto plano
}
```

`SecureStorageHelper.decrypt` devuelve `null` ante **cualquier** excepción, incluyendo la invalidación de la clave del Keystore (restauración de backup, cambio de bloqueo de pantalla en algunos OEM, borrado de credenciales). En ese caso el ciphertext Base64 se interpreta como etiqueta de texto plano y **se vuelve a cifrar**, haciendo la corrupción irreversible.

**Impacto:** Todos los nombres personalizados de apps se convierten en cadenas Base64 ilegibles, de forma permanente.

**Corrección propuesta:**
1. Distinguir en `SecureStorageHelper` entre "fallo de descifrado" y "clave no disponible" (devolver un sealed result en lugar de `null`).
2. Detectar el formato heredado por forma (una etiqueta legacy no es Base64 válido de longitud ≥ 13 bytes), no por "falló el descifrado".
3. Si la clave no está disponible, **no escribir nada** y degradar mostrando la etiqueta original del sistema.

**Relacionado con:** FP-008.

---

## FP-008 — Fallos de cifrado silenciosos provocan pérdida de configuración

**Ficheros:**
- `focusphone-project/app/src/main/java/app/focusphone/security/EncryptedPrefs.kt:36-42`
- `focusphone-project/app/src/main/java/app/focusphone/security/SecureStorageHelper.kt:36-55`
- `focusphone-project/app/src/main/java/app/focusphone/data/preferences/CustomAppLabelsPreferences.kt:29-41`

**Descripción:**
`EncryptedPrefs.putString()` hace `val encrypted = SecureStorageHelper.encrypt(...) ?: return` — si el cifrado falla, **retorna sin escribir y sin informar**. `SecureStorageHelper.encrypt` captura `Exception` y devuelve `null` sin siquiera un log (a diferencia de `getOrCreateKey`, que sí registra).

De forma simétrica, `decrypt()` fallido hace que `EncryptedPrefs.getString()` devuelva el **valor por defecto**, indistinguible de "no configurado".

**Impacto en cadena:**
- `ProfilePreferences.getProfile()` → `PROFILE_NORMAL` → **detox desactivado en silencio**.
- `ProfilePreferences.getDetoxPassword()` → `null` → contraseña perdida.
- `AppSchedulePreferences.getRules()` → lista vacía → **todas las reglas de horario desaparecen**; la siguiente escritura las sobrescribe definitivamente.
- `AppFolderPreferences.loadFolders()` → sin carpetas → el siguiente `persist()` borra las reales del disco.
- `CustomAppLabelsPreferences.setCustomLabel()` actualiza el `StateFlow` en memoria aunque no se persista → el cambio "funciona" hasta el siguiente arranque.

**Corrección propuesta:**
1. `encrypt`/`decrypt` deben registrar el error y propagar un resultado tipado.
2. `EncryptedPrefs.putString` debe devolver `Boolean` (como ya hace `commitString`) y los llamadores deben tratar el `false`.
3. Los lectores de datos críticos (reglas, carpetas, perfil) deben distinguir "vacío" de "ilegible" y **no sobrescribir** cuando el estado es ilegible.

---

## FP-009 — Fuga de IDs de widget: el widget nunca se libera del host

**Ficheros:**
- `focusphone-project/app/src/main/java/app/focusphone/data/preferences/WidgetPreferences.kt:30-35`
- `focusphone-project/app/src/main/java/app/focusphone/ui/settings/HomeSettingsActivity.kt` (`showRemoveWidgetDialog`)
- `focusphone-project/app/src/main/java/app/focusphone/ui/widget/WidgetPickerActivity.kt:223-228` (`saveWidgetAndFinish`)

**Descripción:**
`WidgetPreferences.clearWidget()` solo borra la clave de SharedPreferences. **Nunca se llama a `AppWidgetHostManager.deleteAppWidgetId()`.** Igualmente, `saveWidgetAndFinish()` sobrescribe `KEY_WIDGET_ID` sin liberar el ID anterior.

**Impacto:**
- Cada ciclo añadir → quitar → añadir deja un ID huérfano asignado al host `1024`.
- El proveedor del widget **nunca recibe `onDeleted()`**, por lo que sigue programando actualizaciones y consumiendo batería para un widget que ya no existe.
- Acumulación indefinida de IDs en el `AppWidgetHost` del sistema.

**Corrección propuesta:**
- `clearWidget(context)` debe recibir y liberar el ID: leer `getWidgetId()`, llamar a `AppWidgetHostManager.getInstance(context).deleteAppWidgetId(id)` y solo después limpiar la preferencia.
- En `saveWidgetAndFinish()`, liberar el ID previo si difiere del nuevo.

---

## FP-010 — `TestFocusPhoneApp.container` crea un contenedor nuevo en cada acceso

**Fichero:** `focusphone-project/app/src/androidTest/java/app/focusphone/TestFocusPhoneApp.kt:16-24`

**Descripción:**
```kotlin
override val container: AppContainer
    get() = AppContainer(context = this, applicationScope = applicationScope, …)
```
La clase base usa `by lazy` (un único contenedor). La sobrescritura usa un getter **sin caché**: cada lectura de `focusPhoneApp.container` construye un `AppContainer` nuevo, con nuevos `CustomAppLabelsPreferences` y `AppFolderPreferences` — y por tanto **`StateFlow`s independientes**.

**Impacto:** En los tests instrumentados, un cambio de carpeta hecho a través de un acceso a `container` es invisible desde el siguiente. Las pruebas de carpetas, etiquetas y drag & drop son intrínsecamente no fiables. Además se ejecuta la migración de `FavouriteAppRepositoryImpl` en cada acceso.

**Nota adicional:** `TestFocusPhoneApp.onCreate()` lleva el comentario *"Skip detox expiration side effects in tests"* pero llama a `super.onCreate()`, que **sí** ejecuta `ProfilePreferences.expireDetoxIfNeeded(this)`. Comentario y código se contradicen.

**Corrección propuesta:**
```kotlin
override val container: AppContainer by lazy {
    AppContainer(this, applicationScope, fakeAppRepository, fakeFavouriteAppRepository)
}
```
y eliminar el comentario engañoso (o implementar realmente el salto).

---

## FP-011 — `Lint` está desactivado en release, ocultando 443 errores

**Fichero:** `focusphone-project/app/build.gradle.kts:60-66`

**Descripción:**
```kotlin
lint { checkReleaseBuilds = false }
```
El comentario justifica la desactivación por un `FileSystemException` en Windows. El efecto real es que **ninguna build de release valida nada**: actualmente hay **443 errores y 365 avisos** de lint, incluido un error de permiso en el manifiesto (**FP-012**) y dos `ResourceType` en `BaseActivity`.

**Corrección propuesta:**
1. Corregir las causas raíz (FP-012, FP-053, FP-054, FP-055).
2. Reactivar `checkReleaseBuilds = true` y crear un `lint-baseline.xml` solo para lo que quede pendiente de forma justificada.
3. El problema de Windows se resuelve con `abortOnError` + ejecución fuera del IDE, no desactivando la comprobación.

---

## FP-012 — `BIND_APPWIDGET` declarado: permiso exclusivo de apps de sistema

**Fichero:** `focusphone-project/app/src/main/AndroidManifest.xml:10`

**Descripción:** `<uses-permission android:name="android.permission.BIND_APPWIDGET" />` es un permiso `signature|privileged`. Una app normal **nunca** lo obtiene. Lint lo marca como **error** (`ProtectedPermissions`).

**Impacto:** El permiso no aporta nada (el flujo de binding ya está resuelto correctamente con `bindAppWidgetIdIfAllowed` + `ACTION_APPWIDGET_BIND` en `WidgetPickerActivity`), pero **aparece en la ficha de Google Play** y puede provocar rechazo en la revisión.

**Corrección propuesta:** Eliminar la línea del manifiesto. Verificar que el flujo de widgets sigue funcionando (no depende de él).

---

# 4. 🟨 ALTO

## FP-013 — Dos políticas de bloqueo por contraseña contradictorias y duplicadas

**Ficheros:** `data/preferences/ProfilePreferences.kt:104-128` vs `data/preferences/AppSchedulePreferences.kt:229-262`

| | Detox | Schedule |
|---|---|---|
| Umbral | 4 intentos | 5 intentos |
| Ventana | 30 min deslizante | Cooldown de 15 min |
| Al superar | **Desactiva la protección** | Enfría, no borra nada |
| Almacenamiento del contador | **SharedPreferences en claro** | `EncryptedPrefs` |
| Limpieza en acierto | ❌ no se limpia | ✅ se limpia |

Son dos implementaciones del mismo concepto con reglas opuestas. **Relacionado con FP-001.**

**Corrección propuesta:** Extraer un `PasswordAttemptPolicy` único (umbral, ventana, cooldown, almacenamiento cifrado) y que Detox y Schedule lo usen.

---

## FP-014 — `ProfilePreferences` mezcla almacenamiento cifrado y en claro en el mismo fichero

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/data/preferences/ProfilePreferences.kt:46-49, 104-128`

**Descripción:**
```kotlin
private val store = EncryptedPrefs(PREF_FILE)                                   // cifrado
private fun prefs(context) = context.getSharedPreferences(PREF_FILE, MODE_PRIVATE) // en claro
```
Ambos apuntan al **mismo fichero** `focusphone_profile`. `recordFailedAttempt`, `getFailedAttempts` y `clearFailedAttempts` escriben `Int`/`Long` en claro; todo lo demás va cifrado.

**Impacto:**
1. El KDoc de la clase (línea 13) afirma que los intentos fallidos están cifrados — **es falso**.
2. El contador de intentos se puede reiniciar trivialmente con acceso al fichero (root/adb), reabriendo el bypass de FP-001 sin límite.
3. `EncryptedPrefs.getString` tiene un `catch (ClassCastException)` (línea 25-31) que existe precisamente por este tipo de mezcla — un parche sobre un problema de diseño.

**Corrección propuesta:** Migrar los tres métodos de intentos a `store` (`getInt`/`putInt`/`getLong`/`putLong` ya existen en `EncryptedPrefs`) y corregir el KDoc.

---

## FP-015 — `verifyDetoxPassword` no limpia los intentos fallidos al acertar

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/data/preferences/ProfilePreferences.kt:84-101`

**Descripción:** A diferencia de `AppSchedulePreferences.verifyPassword()`, el caso `Match` no llama a `clearFailedAttempts()`. Los llamadores lo hacen manualmente *después*, pero cualquier nuevo llamador heredará el contador acumulado y quedará más cerca del bypass de FP-001.

**Corrección propuesta:** Limpiar el contador dentro de `verifyDetoxPassword` en los casos `Match` y `MatchNeedsUpgrade`.

---

## FP-016 — El `recreate()` por cambio de fondo es código muerto

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/base/BaseActivity.kt:553-575`

**Descripción:**
```kotlin
override fun onResume() {
    …
    applyWallpaper()                                   // línea 556 → asigna currentHasWallpaper
    …
    val activeHasWallpaper = hasSurfaceWallpaper()     // línea 565
    if (… || activeHasWallpaper != currentHasWallpaper) { recreate(); return }  // línea 572
```
`applyWallpaper()` (línea 197) ejecuta `currentHasWallpaper = uri != null` de forma síncrona. `hasSurfaceWallpaper()` recalcula exactamente lo mismo. **La condición nunca puede ser `true`.**

**Impacto:** Al activar o desactivar el fondo desde Ajustes, las Activities ya creadas no se recrean, por lo que los fondos y colores resueltos en tiempo de inflado (que dependen de si hay wallpaper) quedan inconsistentes.

**Corrección propuesta:** Capturar `hasSurfaceWallpaper()` **antes** de `applyWallpaper()`, o mover `applyWallpaper()` detrás del bloque de comprobaciones.

---

## FP-017 — El fondo de pantalla se decodifica en cada `onResume` de cada pantalla

**Ficheros:**
- `focusphone-project/app/src/main/java/app/focusphone/ui/base/BaseActivity.kt:556` (`applyWallpaper()` en `onResume`)
- `focusphone-project/app/src/main/java/app/focusphone/ui/wallpaper/WallpaperUtils.kt:44-66`

**Descripción:** No hay caché de bitmap. `BaseActivity.onResume()` llama a `applyWallpaper()`, que lanza una corrutina en `Dispatchers.IO` y decodifica la imagen completa (hasta 1920×1920, `ARGB_8888` ≈ 14 MB) **cada vez que cualquier Activity se reanuda**. En API < 31, además, `blurBitmap()` crea dos bitmaps escalados adicionales y no recicla el intermedio.

**Impacto:** Presión de memoria y GC constante en un launcher, que es la app que más veces se reanuda del sistema. Parpadeo visible del fondo en cada navegación.

**Corrección propuesta:**
- Caché de bitmap a nivel de proceso, con clave `(uri, blurEnabled, blurRadius)`, invalidada al cambiar la preferencia.
- `BitmapFactory.Options.inPreferredConfig = Bitmap.Config.RGB_565` para fondos (no necesitan canal alfa): −50 % de memoria.
- Reciclar el bitmap intermedio en `blurBitmap`.

---

## FP-018 — `AppRepositoryImpl`: caché sin sincronización e invalidación incompleta

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/data/repository/AppRepositoryImpl.kt:19-56`

**Problemas:**
1. `cachedApps` y `cacheTimestamp` no son `@Volatile` y se leen/escriben desde múltiples corrutinas en `Dispatchers.IO` sin sincronización. **Carrera de datos**.
2. **Renombrar una app no surte efecto hasta 30 s.** `AllAppsActivity.showRenameDialog` (línea 415) y `AppSelectActivity.showRenameDialog` (línea 128) llaman a `setCustomLabel()` + `viewModel.load()` pero **no a `invalidateCache()`** — y la caché ya contiene la etiqueta antigua.
3. No hay `BroadcastReceiver` de `ACTION_PACKAGE_ADDED`/`REMOVED`, por lo que instalar una app no se refleja hasta que la caché expira o se recrea la Activity.

**Corrección propuesta:**
- Proteger la caché con un `Mutex` o marcarla `@Volatile`.
- Llamar a `invalidateCache()` en ambos diálogos de renombrado antes de `load()`.
- Registrar un receptor de cambios de paquete a nivel de `AppContainer` que invalide la caché.

---

## FP-019 — `AllAppsActivity` no refresca la lista al volver de una instalación

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/allapps/AllAppsActivity.kt:504-510`

**Descripción:** `onResume()` solo recarga si `pendingRefresh` (que únicamente se activa tras pulsar "desinstalar"). Instalar una app desde Play Store y volver al cajón **no la muestra**.

**Corrección propuesta:** Recargar cuando la caché del repositorio esté vencida, o —mejor— consumir el receptor de cambios de paquete de FP-018.

---

## FP-020 — Diálogos filtrados: `activeDialog` se sobrescribe sin cerrar el anterior

**Ficheros:**
- `focusphone-project/app/src/main/java/app/focusphone/ui/allapps/AllAppsActivity.kt` (9 asignaciones de `activeDialog`)
- `focusphone-project/app/src/main/java/app/focusphone/ui/appselect/AppSelectActivity.kt:47, 118`
- `focusphone-project/app/src/main/java/app/focusphone/ui/schedule/AppScheduleActivity.kt`
- `focusphone-project/app/src/main/java/app/focusphone/ui/settings/AppearanceSettingsActivity.kt`, `AboutSettingsActivity.kt`

**Descripción:** El patrón es siempre `activeDialog = builder.create(); activeDialog?.show()`. Al encadenar diálogos (p. ej. opciones → renombrar, o selector de carpeta → crear carpeta) la referencia al anterior se pierde y `onDestroy` solo cierra el último. Flujos encadenados: `showAppOptionsDialog` → `showFolderChooserDialog` → `showCreateFolderDialog`.

**Impacto:** `WindowLeaked` en logcat y posible fuga de la Activity.

**Corrección propuesta:** Crear un helper en `BaseActivity`:
```kotlin
protected fun showExclusiveDialog(dialog: Dialog) {
    activeDialog?.dismiss()
    activeDialog = dialog
    dialog.show()
}
```
y centralizar el `dismiss()` en `BaseActivity.onDestroy()`.

---

## FP-021 — Diálogos mostrados desde corrutinas sin comprobar el estado de la Activity

**Ficheros:**
- `focusphone-project/app/src/main/java/app/focusphone/ui/home/HomeActivity.kt:1296-1304` (`showAppContextMenu`)
- `focusphone-project/app/src/main/java/app/focusphone/ui/allapps/AllAppsActivity.kt:318-325` (`showAppOptionsDialog`)
- `focusphone-project/app/src/main/java/app/focusphone/ui/schedule/AppScheduleActivity.kt` (`showAppPicker`)

**Descripción:** Todas hacen `lifecycleScope.launch { withContext(IO) { … } ; mostrarDialogo() }`. `lifecycleScope` sobrevive a `onPause`/`onStop`; si el usuario sale mientras se cargan los accesos directos o la lista de apps, el `PopupMenu`/`AlertDialog` se muestra sobre un token de ventana muerto → **`WindowManager$BadTokenException`**.

**Corrección propuesta:** Envolver en `repeatOnLifecycle(Lifecycle.State.STARTED)` o añadir una guarda `if (isFinishing || isDestroyed) return@launch` antes de mostrar.

---

## FP-022 — Reordenar favoritas: `ListAdapter` + `notifyItemMoved` produce posiciones incoherentes

**Ficheros:**
- `focusphone-project/app/src/main/java/app/focusphone/ui/home/HomeActivity.kt:628-646`
- `focusphone-project/app/src/main/java/app/focusphone/ui/settings/HomeLayoutActivity.kt:620-633`
- `focusphone-project/app/src/main/java/app/focusphone/ui/home/adapter/FavouriteAppsAdapter.kt:41-60`

**Descripción:** `FavouriteAppsAdapter` extiende `ListAdapter`, cuya lista interna (`currentList`) **solo cambia vía `submitList`**. `onMove` llama a `notifyItemMoved`, que reordena las vistas pero **no** la lista interna. En el siguiente `onMove` del mismo gesto, `bindingAdapterPosition` se refiere al orden visual, mientras que `tempReorderList` ya fue construido a partir del orden lógico original.

Además, `moveItem()` no valida los índices contra `current.size`:
```kotlin
val item = current.removeAt(fromPosition)   // IndexOutOfBoundsException posible
```

**Impacto:** Arrastres de varias posiciones producen un orden final incorrecto; en casos límite, crash.

**Corrección propuesta:** Aplicar el movimiento a una lista de trabajo y llamar a `submitList(nuevaLista)` con `itemAnimator = null` (ya está desactivado), o sustituir `ListAdapter` por un `RecyclerView.Adapter` con `AsyncListDiffer` gestionado manualmente. En cualquier caso, validar rangos en `moveItem`.

---

## FP-023 — La pausa consciente lanza la app desde segundo plano (y falla en silencio)

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/pause/ConsciousPauseActivity.kt:65-99`

**Descripción:** La cuenta atrás usa `lifecycleScope.launch` sin `repeatOnLifecycle`. Si el usuario pulsa Home durante la pausa, la corrutina sigue corriendo y al terminar llama a `startActivity(targetIntent)` desde segundo plano.

**Impacto:** En Android 10+ el lanzamiento de actividades en segundo plano está bloqueado: la app destino **nunca se abre** y el usuario no recibe ningún aviso (la `Exception` se captura y muestra un toast genérico que tampoco es visible desde segundo plano).

**Corrección propuesta:** Envolver la cuenta atrás en `repeatOnLifecycle(Lifecycle.State.RESUMED)` y descartar el lanzamiento si la Activity no está en primer plano.

---

## FP-024 — Bucle de sondeo infinito en el onboarding

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/onboarding/OnboardingActivity.kt:148-157`

**Descripción:**
```kotlin
repeatOnLifecycle(Lifecycle.State.STARTED) {
    while (true) {
        delay(1000)
        if (!isLauncherVerified) { checkLauncherStatus() }
    }
}
```
El bucle **nunca termina**, ni siquiera tras verificar el launcher: solo deja de hacer la comprobación. Sigue despertando cada segundo mientras la pantalla esté visible.

**Corrección propuesta:** Salir del bucle con `break` en cuanto `isLauncherVerified` sea `true`; `onResume()` ya cubre el caso de volver desde los ajustes del sistema.

---

## FP-025 — Cuatro implementaciones distintas de "¿soy el launcher por defecto?"

**Ficheros:**

| Fichero:línea | Flag usado |
|---|---|
| `ui/home/HomeActivity.kt:277` | `resolveActivity(intent, 0)` |
| `ui/onboarding/OnboardingActivity.kt:163` | `resolveActivity(intent, 0)` |
| `ui/onboarding/OnboardingActivity.kt:340` | `resolveActivity(intent, 0)` (duplicado dentro del ViewHolder) |
| `domain/usecase/GetAppShortcutsUseCase.kt:99` | `resolveActivity(intent, 0)` |
| `ui/settings/PermissionsActivity.kt:74` | `resolveActivity(intent, MATCH_DEFAULT_ONLY)` ✅ |

**Impacto:** Sin `MATCH_DEFAULT_ONLY`, `resolveActivity` puede devolver el *resolver* del sistema o un launcher arbitrario cuando hay varios candidatos y ninguno es el predeterminado. Consecuencia directa: **`GetAppShortcutsUseCase` devuelve lista vacía** (los accesos directos de app desaparecen del menú contextual) y el onboarding puede marcar el paso como verificado incorrectamente. Además, el método está deprecado en API 33+.

**Corrección propuesta:** Un único helper (p. ej. `LauncherUtils.isDefaultLauncher(context)`) que use `MATCH_DEFAULT_ONLY` y la sobrecarga `ResolveInfoFlags` en API ≥ 33. Sustituir los cinco puntos de uso.

---

## FP-026 — `FocusAccessibilityService.isEnabled()` puede devolver falso negativo

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/service/FocusAccessibilityService.kt:282`

**Descripción:**
```kotlin
val expectedService = "${context.packageName}/${FocusAccessibilityService::class.java.canonicalName}"
```
`Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` almacena `ComponentName.flattenToString()`, pero algunos fabricantes usan la **forma corta** (`app.focusphone/.service.FocusAccessibilityService`), que no coincide con la comparación literal.

**Impacto:** La app cree que el servicio está apagado aunque esté activo: muestra avisos incorrectos en `SystemSettingsActivity`, `PermissionsActivity` y `AppScheduleActivity`, y ofrece el diálogo de "activar accesibilidad" de forma repetida.

**Corrección propuesta:** Comparar con `ComponentName.unflattenFromString(entry)` frente a `ComponentName(context, FocusAccessibilityService::class.java)`, que normaliza ambas formas.

---

## FP-027 — El servicio de accesibilidad escribe en disco cifrado desde el hilo principal

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/service/FocusAccessibilityService.kt:198-211, 232-241`

**Descripción:**
- `flushForegroundUsage()` → `AppSchedulePreferences.addUsageSeconds()` → lectura + descifrado + cifrado + escritura de SharedPreferences, en el hilo principal del servicio. Se invoca en cada `TYPE_WINDOW_STATE_CHANGED` y en cada tick de 30 s.
- `refreshCache()` hace **5 operaciones de descifrado** (perfil, detox, bloqueadas, keywords, reglas) cada 3 s como máximo.
- `addUsageSeconds` hace read-modify-write **no atómico**; con el tick del `Handler` y los eventos de accesibilidad concurrentes puede perder contabilización.

**Impacto:** Jank y riesgo de ANR en el servicio de accesibilidad, que es precisamente el componente que debe responder rápido. Contadores de uso diario imprecisos.

**Corrección propuesta:**
- Mover la escritura de contadores a un `CoroutineScope(Dispatchers.IO)` propio del servicio.
- Mantener los contadores en memoria y hacer *flush* periódico (cada 60 s) y en `onUnbind`/`ACTION_SCREEN_OFF`.
- Serializar el acceso con un `Mutex`.

---

## FP-028 — `FocusPalette.of()` se resuelve en cada frame durante el arrastre

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/allapps/FolderDragCallback.kt:186-187`

**Descripción:** `highlight()` se llama desde `onChildDraw()` (60 fps) y termina en `highlightColor()`, que ejecuta `FocusPalette.of(context)` → lectura de SharedPreferences + `obtainStyledAttributes` + cálculos WCAG de contraste, **por cada fotograma**.

**Corrección propuesta:** Resolver el color una sola vez en `onSelectedChanged` (inicio del arrastre) y cachearlo en un campo.

---

## FP-029 — `NotificationsAdapter` lee preferencias y resuelve la paleta en cada `bind`

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/home/adapter/NotificationsAdapter.kt:53-70`

**Descripción:** Cada `bind()` ejecuta `FontPreferences.getTypefaceOrNull(context)` (acceso a disco + `ResourcesCompat.getFont`) y `FocusPalette.of(context)`, y crea un `GradientDrawable` nuevo. `FavouriteAppsAdapter` y `AllAppsAdapter` sí cachean la paleta (`paletteCache`); este no.

**Corrección propuesta:** Replicar el patrón `paletteCache` y cachear el `Typeface` y el `GradientDrawable` base a nivel de adaptador.

---

## FP-030 — El fondo del panel de notificaciones es un ajuste sin efecto

**Ficheros:**
- `focusphone-project/app/src/main/java/app/focusphone/ui/settings/AppearanceSettingsActivity.kt:190-198`
- `focusphone-project/app/src/main/java/app/focusphone/ui/base/BaseActivity.kt:175-181` (`surfaceWallpaperUri`)

**Descripción:** El usuario puede elegir un fondo específico para el panel de notificaciones (`WallpaperTarget.NOTIFICATIONS`). Sin embargo `surfaceWallpaperUri()` solo distingue entre **home** y **settings**:

```kotlin
return if (usesHomeWallpaper()) WallpaperPreferences.getHomeWallpaperUri(this)
       else WallpaperPreferences.getSettingsWallpaperUri(this)
```

`getNotificationsWallpaperUri()` **no se llama desde ninguna ruta de renderizado**. El cajón de notificaciones de `HomeActivity` usa el fondo de home.

**Impacto:** Ajuste visible que no hace absolutamente nada. El usuario selecciona una imagen y ve "configurado" pero nunca aparece.

**Corrección propuesta:** O bien implementar la superficie de notificaciones en `HomeActivity` (aplicar el bitmap a una `ImageView` propia dentro de `drawer_notifications`), o bien **eliminar la opción** del selector junto con `WallpaperTarget.NOTIFICATIONS` y sus preferencias asociadas. Decidir explícitamente; no dejarlo a medias.

---

## FP-031 — Vaciar las palabras clave de "trabajo" restaura los valores por defecto

**Ficheros:**
- `focusphone-project/app/src/main/java/app/focusphone/ui/settings/FocusSettingsActivity.kt:90-95`
- `focusphone-project/app/src/main/java/app/focusphone/data/preferences/ProfilePreferences.kt:152-156`

**Descripción:** `setWorkKeywords(emptySet())` guarda `null`; `getWorkKeywords()` devuelve `DEFAULT_WORK_KEYWORDS` cuando el valor es nulo o vacío. No se puede distinguir "el usuario no ha configurado nada" de "el usuario ha borrado todas las palabras".

**Impacto:** Un usuario que borra la lista para desactivar el bloqueo por palabras clave ve cómo reaparecen las 16 por defecto (instagram, youtube, netflix…) tras reabrir la pantalla, y sus apps siguen bloqueadas en perfil Trabajo.

**Corrección propuesta:** Añadir una clave booleana `work_keywords_configured`. Si está a `true`, respetar el conjunto vacío.

---

## FP-032 — `getFontSizePt` escribe en disco desde un getter y recupera al valor **máximo**

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/data/preferences/FontPreferences.kt:135-146`

**Descripción:**
```kotlin
fun getFontSizePt(context: Context): Int {
    val size = prefs(context).getInt(KEY_FONT_SIZE_PT, 14)
    return if (size in MIN..MAX) size
    else { setFontSizePt(context, MAX_FONT_SIZE_PT); MAX_FONT_SIZE_PT }   // ← 24, no 14
}
```
Dos problemas:
1. Un valor corrupto se recupera al **máximo** (24 pt) en lugar de al **valor por defecto** (14 pt). El usuario ve toda la interfaz gigante sin explicación.
2. El getter escribe en disco. Se invoca desde `BaseActivity.attachBaseContext()` (línea 101), es decir, **antes de `onCreate`, en el hilo principal**, en cada Activity.

**Corrección propuesta:** Recuperar a `DEFAULT_FONT_SIZE_PT = 14`; hacer el getter puro (solo `coerceIn`) y sanear el valor al escribirlo.

---

## FP-033 — Los deslizadores escriben en SharedPreferences en cada paso del arrastre

**Ficheros:**
- `focusphone-project/app/src/main/java/app/focusphone/ui/settings/AppearanceSettingsActivity.kt:118-124, 227-235, 251-260`
- `focusphone-project/app/src/main/java/app/focusphone/ui/settings/HomeSettingsActivity.kt:63-70, 88-97`
- `focusphone-project/app/src/main/java/app/focusphone/ui/settings/FocusSettingsActivity.kt:58-66`

**Descripción:** Todos usan `addOnChangeListener { _, value, _ -> prefs.setXxx(value.toInt()) }` con `stepSize = 1f` y rangos de 0-100. Un único arrastre completo genera hasta **100 escrituras** de SharedPreferences. El de desenfoque, además, lanza `updateWallpaperPreview()` en cada paso, que decodifica el bitmap completo (**FP-017**).

**Corrección propuesta:** Actualizar la etiqueta en `addOnChangeListener` (feedback inmediato) y persistir únicamente en `addOnSliderTouchListener { onStopTrackingTouch }`.

---

# 5. 🟦 MEDIO

## FP-034 — `HomeActivity.onCreate` no interrumpe la inicialización al lanzar el onboarding

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/home/HomeActivity.kt:133-135`

```kotlin
if (!ThemePreferences.isOnboardingComplete(this)) {
    startActivity(Intent(this, OnboardingActivity::class.java))
}
// ← sin return; continúa montando toda la home detrás
```
Se inflan RecyclerViews, se registran observadores, se construye el calendario y se restaura el widget para una pantalla que queda tapada. Coste de arranque innecesario en el primer inicio.

**Corrección:** `return` tras `startActivity`, o mover la decisión a una Activity trampolín.

---

## FP-035 — Código duplicado íntegro del reloj y del widget entre `HomeActivity` y `HomeLayoutActivity`

**Ficheros:** `ui/home/HomeActivity.kt:425-455, 553-600, 1531-1622` vs `ui/settings/HomeLayoutActivity.kt:398-470, 494-598`

**Descripción:** `applyClockType()`, `applyClockScale()`, `updateClock()`, `formatTimeWords()` y `restoreWidget()` están **duplicados literalmente** (~250 líneas) entre ambas Activities, con pequeñas divergencias ya presentes (el preview tiene un estado `clock_type_none` que la home no tiene).

**Impacto:** Cualquier arreglo en el reloj o el widget debe aplicarse dos veces. Ya hay divergencia.

**Corrección propuesta:** Extraer un `ClockRenderer` y un `WidgetSurface` reutilizables que operen sobre un `ViewGroup` recibido por parámetro.

---

## FP-036 — Dos hosts de widget vivos simultáneamente sobre el mismo ID

**Ficheros:** `ui/home/HomeActivity.kt:1568-1620` y `ui/settings/HomeLayoutActivity.kt:494-598`

**Descripción:** Ambas Activities llaman a `AppWidgetHostManager.createView(...)` para el mismo `widgetId` desde el **mismo `AppWidgetHost` singleton**. Si `HomeActivity` no se ha destruido (es `singleTask`, típicamente permanece en la pila) mientras `HomeLayoutActivity` está en primer plano, hay dos `AppWidgetHostView` registradas para el mismo ID.

**Impacto:** Comportamiento indefinido en las actualizaciones del proveedor; el `RemoteViews` puede aplicarse a solo una de las vistas. Lo hemos visto reportado como "el widget del preview no se actualiza".

**Corrección propuesta:** Liberar la vista (`removeView` + anular la referencia) en `onStop()` de cada Activity, no solo en `onDestroy`.

---

## FP-037 — `dispatchTouchEvent` alimenta el `GestureDetector` con todos los eventos

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/home/HomeActivity.kt:399-402`

```kotlin
override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
    ev?.let { gestureDetector.onTouchEvent(it) }
    return super.dispatchTouchEvent(ev)
}
```
Recibe también los eventos dirigidos al `RecyclerView` de favoritas, a los cajones y a los diálogos. Un scroll rápido de la lista puede interpretarse como fling vertical y abrir el panel del sistema.

**Corrección propuesta:** Restringir el detector a la superficie de la home (listener de toque sobre `rootHome`), o comprobar en `onFling` que el punto inicial no cae sobre un hijo desplazable.

---

## FP-038 — `expandNotificationsPanel()` usa reflexión bloqueada en Android moderno

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/home/HomeActivity.kt:390-397`

**Descripción:** Invoca por reflexión `StatusBarManager.expandNotificationsPanel()`. Ese método está en la lista de **interfaces no-SDK bloqueadas** desde Android 12 (API 31). También se usa `Context.STATUS_BAR_SERVICE`, que lint marca como `InlinedApi` (requiere API 33).

**Impacto:** El gesto de deslizar hacia abajo no hace nada en la mayoría de dispositivos actuales, mientras que el ajuste correspondiente (`settings_swipe_down_title`) sigue ofreciéndose como funcional.

**Corrección propuesta:** Documentar el ajuste como "solo en dispositivos compatibles" y detectar el fallo para deshabilitarlo, o sustituirlo por un `AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS` cuando el servicio de accesibilidad esté activo (ruta soportada oficialmente).

---

## FP-039 — El ancho de los cajones usa `displayMetrics.widthPixels`

**Ficheros:**
- `focusphone-project/app/src/main/java/app/focusphone/ui/home/HomeActivity.kt:707-713`
- `focusphone-project/app/src/main/java/app/focusphone/ui/settings/HomeSettingsActivity.kt:181-185`

**Descripción:** `resources.displayMetrics.widthPixels` no refleja correctamente el tamaño de ventana en multiventana ni en plegables con cambio de postura. Los cajones se fijan a ese ancho una sola vez en `onCreate` y nunca se recalculan.

**Corrección propuesta:** Usar `WindowMetricsCalculator`/`WindowManager.currentWindowMetrics` y recalcular en `onConfigurationChanged`.

---

## FP-040 — El "long press" del editor de layout se dispara a los 200 ms

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/settings/HomeLayoutActivity.kt:693`

```kotlin
}.also { handler.postDelayed(it, 200) }
```
El umbral del sistema (`ViewConfiguration.getLongPressTimeout()`) es de 500 ms. Con 200 ms, una pulsación normal ligeramente lenta entra en modo edición y arranca la animación de "wiggle".

**Corrección propuesta:** Usar `ViewConfiguration.getLongPressTimeout()`.

---

## FP-041 — `HomeLayoutActivity` registra un receptor sin flag de exportación

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/settings/HomeLayoutActivity.kt:133`

```kotlin
registerReceiver(timeTickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
```
Sin `ContextCompat.RECEIVER_NOT_EXPORTED` y sin `try/catch`, a diferencia de `HomeActivity.kt:167-176`, que sí lo hace y además captura excepciones para OEMs restrictivos. Con `targetSdk = 36` esto es una inconsistencia peligrosa.

**Corrección propuesta:** Unificar con `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)` envuelto en `try/catch`, en ambos sitios.

---

## FP-042 — `WidgetPickerActivity` carga todos los proveedores en el hilo principal

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/widget/WidgetPickerActivity.kt:85-115`

**Descripción:** `getInstalledProviders()` + `loadLabel(packageManager)` + `loadIcon(...)` para **cada proveedor del dispositivo**, síncronamente en `onCreate`. En un teléfono con muchas apps son cientos de milisegundos de IPC y de carga de drawables.

**Corrección propuesta:** Mover a `Dispatchers.IO` con `lifecycleScope` y mostrar un estado de carga.

---

## FP-043 — El diálogo de licencias muestra HTML en crudo

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/settings/AboutSettingsActivity.kt:122`

```kotlin
text = htmlText ?: getString(R.string.settings_privacy_policy_fallback)
```
Se asigna el contenido de `open_source_licenses.html` **sin parsear**. El diálogo de política de privacidad (línea 150) sí usa `Html.fromHtml`.

**Impacto:** El usuario ve etiquetas `<html><body><h1>…` literales. Además, el fichero se lee desde `assets` en el hilo principal en ambos diálogos.

**Corrección propuesta:** Aplicar `Html.fromHtml(htmlText, FROM_HTML_MODE_LEGACY)` y leer el asset en `Dispatchers.IO`.

---

## FP-044 — Los diálogos de creación/renombrado de carpeta pierden lo escrito ante un nombre duplicado

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/allapps/AllAppsActivity.kt:255-268, 288-300`

**Descripción:**
```kotlin
.setPositiveButton(R.string.action_save) { _, _ ->
    val folder = folderPreferences.createFolder(input.text.toString())
    if (folder == null) { toast(...); return@setPositiveButton }   // el diálogo YA se cerró
```
`AlertDialog` cierra el diálogo antes de invocar el listener; el `return@` no lo impide. El usuario pierde el texto y debe reabrir el diálogo y volver a escribirlo.

**Corrección propuesta:** Sobrescribir el `OnClickListener` del botón tras `show()` para controlar el cierre, y mostrar el error **inline** (como ya hace `AppScheduleActivity.showPasswordDialog`, que es el patrón correcto del proyecto).

---

## FP-045 — `ToggleFavouriteAppUseCase` usado como "eliminar": puede volver a añadir la app

**Ficheros:**
- `focusphone-project/app/src/main/java/app/focusphone/ui/home/HomeViewModel.kt:37-43`
- `focusphone-project/app/src/main/java/app/focusphone/ui/home/HomeActivity.kt:197-236` (`verifyInstalledApps`)

**Descripción:** `HomeViewModel.removeFavourite()` llama a `toggleFavourite()`. Si la app ya no es favorita, el toggle la **añade**. `verifyInstalledApps` itera sobre apps desinstaladas llamando a `removeFavourite` en una corrutina de IO; dos reanudaciones solapadas de la home pueden reinsertar una app desinstalada.

**Corrección propuesta:** Añadir un `RemoveFavouriteAppUseCase` explícito (o exponer `repository.removeFavourite`) y usarlo donde la intención es eliminar.

---

## FP-046 — `AppFolderPreferences` no persiste la deduplicación defensiva

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/data/preferences/AppFolderPreferences.kt:169-183`

**Descripción:** `loadFolders()` elimina paquetes duplicados entre carpetas, pero **no vuelve a escribir el resultado**. El estado corrupto sigue en disco y se deduplica de nuevo en cada arranque, con resultado dependiente del orden de lectura.

**Corrección propuesta:** Si `parsed != deduplicado`, llamar a `persist(deduplicado)`.

---

## FP-047 — El bloqueo de detox cacheado puede tener hasta 3 s de retraso

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/service/FocusAccessibilityService.kt:245-262`

**Descripción:** `cachedIsDetoxActive` se calcula en `refreshCache()` y solo se refresca cada `CACHE_TTL_MS = 3000`. Es un valor **dependiente del tiempo** (`detoxEndTime - now > 0`) tratado como estático.

**Impacto:** Hasta 3 s tras expirar la sesión de detox, las apps siguen bloqueadas; e, inversamente, el servicio no llama nunca a `expireDetoxIfNeeded()`, por lo que el perfil persistido queda en `detox` hasta que `HomeActivity` se reanude.

**Corrección propuesta:** Cachear `detoxEndTime` y `profile` (que sí son estáticos) y evaluar `isDetoxActivePure(profile, endTime, now)` en cada evento.

---

## FP-048 — Estado de onboarding duplicado y descoordinado

**Ficheros:**
- `data/preferences/ThemePreferences.kt:115-122` (`isOnboardingComplete`)
- `data/preferences/TutorialPreferences.kt:47-53` (`isOnboardingFlowCompleted`)

**Descripción:** Hay **dos** banderas de finalización de onboarding. `TutorialPreferences.isOnboardingFlowCompleted()` se **escribe** (`OnboardingActivity.finishOnboarding`) pero **nunca se lee** desde ningún sitio. Su KDoc habla de un "carrusel de 7 pasos" y de un "flujo legacy de 3 pasos", mientras que `OnboardingActivity` tiene **5 páginas**.

**Corrección propuesta:** Eliminar `isOnboardingFlowCompleted`/`setOnboardingFlowCompleted` o convertirla en la única fuente de verdad. Corregir el KDoc.

---

## FP-049 — Tres sistemas de "tutorial" paralelos y desalineados

| Sistema | Ubicación | Nº de pasos |
|---|---|---|
| Onboarding (ViewPager) | `OnboardingActivity.pages` | 5 |
| Tutorial contextual (spotlight) | `HomeActivity.TUTORIAL_STEPS` | 4 |
| Carrusel del diálogo "tutorial" | `AboutSettingsActivity.showTutorialDialog` | 8 |

**Descripción:** Tres conjuntos de contenido, tres modelos de estado y tres estilos visuales que explican las mismas funciones. Existen además 8 strings `tutorial_coach_*` definidas para pasos que **no están en `TUTORIAL_STEPS`** (block, rename, detox, customize) — ver FP-067.

**Corrección propuesta:** Decidir un modelo único de contenido (lista de pasos con icono/título/descripción) y reutilizarlo en los tres presentadores, o eliminar el que sobre.

---

## FP-050 — Los temas oscuros no heredan de `Base.Theme.FocusPhone`

**Fichero:** `focusphone-project/app/src/main/res/values/themes.xml:15-21, 80, 200`

**Descripción:**

| Tema | Parent |
|---|---|
| `Theme.FocusPhone.OffWhite` | `Base.Theme.FocusPhone` ✅ |
| `Theme.FocusPhone.WarmBeige` | `Base.Theme.FocusPhone` ✅ |
| `Theme.FocusPhone.Slate` | `Theme.Material3.Dark.NoActionBar` ❌ |
| `Theme.FocusPhone.Navy` | `Theme.Material3.Dark.NoActionBar` ❌ |

Los dos temas oscuros **no reciben** los atributos comunes definidos en `Base.Theme.FocusPhone`:
- `android:windowLayoutInDisplayCutoutMode = shortEdges`
- `android:fontFamily = sans-serif`

**Impacto:** En dispositivos con muesca, los temas claros dibujan bajo el recorte y los oscuros no. Slate es **el tema por defecto**, así que la mayoría de usuarios ve el comportamiento no intencionado.

**Corrección propuesta:** Crear `Base.Theme.FocusPhone.Dark` (parent `Theme.Material3.Dark.NoActionBar`) con los mismos atributos comunes y hacer que Slate y Navy hereden de él.

---

## FP-051 — El estilo `FocusPhone.MaterialAlertDialog` nunca se aplica

**Fichero:** `focusphone-project/app/src/main/res/values/themes.xml:288-299`

**Descripción:** Se define un `ThemeOverlay` completo (título, cuerpo, botones) pero **ninguno de los cuatro temas declara `materialAlertDialogTheme`**, y ningún `MaterialAlertDialogBuilder` del código pasa un `overrideThemeResId`.

**Impacto:** Todos los diálogos Material usan el estilo por defecto de Material 3. Por eso existe `BaseActivity.styleMaterialDialog()` (línea 613-635), que parchea tipografía y color **después** de `show()` — con el parpadeo visual correspondiente. Los estilos `FocusPhone.DialogTitleTextStyle`, `FocusPhone.DialogBodyTextStyle` y `FocusPhone.DialogButton` son código muerto.

**Corrección propuesta:** Añadir `<item name="materialAlertDialogTheme">@style/FocusPhone.MaterialAlertDialog</item>` a los cuatro temas y reducir `styleMaterialDialog()` a lo estrictamente dinámico (tema personalizado y fuente descargable).

---

## FP-052 — `colorizeViewTree` usa excepciones como flujo de control

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/base/BaseActivity.kt:443-461`

**Descripción:**
```kotlin
val viewId = try { resources.getResourceEntryName(view.id) } catch (e: Exception) { "" }
val skipBg = viewId.startsWith("card_theme") || viewId.contains("preview") || …
```
`getResourceEntryName` lanza `NotFoundException` para todo `View` sin ID (`View.NO_ID`), es decir, para **la mayoría** de los ViewGroups creados en código. Se lanza y captura una excepción por cada nodo del árbol, y después se hacen 11 comparaciones de cadena.

**Corrección propuesta:** Comprobar `view.id != View.NO_ID` antes de resolver el nombre, y sustituir el matching por cadena por una etiqueta explícita (`setTag(R.id.tag_skip_recolor, true)`), siguiendo el patrón ya usado en `SettingsUiHelper.markAsSettingsRow()`.

---

## FP-053 — Errores de lint `ResourceType` en `colorizeViewTree`

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/base/BaseActivity.kt:379-380`

```kotlin
val ta = obtainStyledAttributes(themeAttrs)
val baseSecondary = ta.getColor(1, 0)   // Error: Expected resource of type styleable
val baseBg = ta.getColor(2, 0)
```
Lint no puede verificar el índice contra un `styleable` declarado. Es uno de los dos errores de lint no relacionados con traducciones.

**Corrección propuesta:** Declarar un `<declare-styleable>` en `res/values/attrs.xml`, o anotar el método con `@SuppressLint("ResourceType")` documentando por qué.

---

## FP-054 — Configuración de locales mal formada: 439 errores de lint

**Ficheros:** `res/values/strings.xml:2`, `res/values-en-rGB/strings.xml`

**Descripción:** `values-en-rGB/strings.xml` contiene solo **12 strings** (sobrescrituras de ortografía británica). Como `values/strings.xml` **no declara `tools:locale="en"`**, lint considera que existe un locale "en" incompleto y reporta las **439 strings restantes** como `MissingTranslation`. Es la causa del 99 % de los errores de lint (FP-011).

**Corrección propuesta:**
```xml
<resources xmlns:tools="http://schemas.android.com/tools" tools:locale="en">
```
en `res/values/strings.xml`.

---

## FP-055 — Traducciones realmente ausentes e inconsistencia de argumentos de formato

**Descripción:**

| Locale | String ausente |
|---|---|
| `fr` | `settings_section_system` |
| `es`, `fr`, `de` | `allapps_folder_caret`, `allapps_new_folder_symbol` |

Además, lint reporta `StringFormatCount` en `values-es/strings.xml:193`: **`settings_tutorial_desc` tiene 0 argumentos en español y 1 en la versión base**. Si alguna vez se formatea, la versión española lanzará `IllegalFormatException`.

**Corrección propuesta:**
1. Traducir `settings_section_system` al francés.
2. Marcar `allapps_folder_caret` y `allapps_new_folder_symbol` con `translatable="false"` (son símbolos, no texto).
3. Alinear los argumentos de `settings_tutorial_desc`.

---

## FP-056 — Los nombres de app se fuerzan a minúsculas y se eliminan los acentos

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/domain/util/TextExtensions.kt:16-20`

**Descripción:** `normalizeAppName()` aplica NFD + eliminación de marcas diacríticas + `lowercase()` a **todas** las etiquetas mostradas.

**Impacto en localización:** En los cuatro idiomas soportados esto degrada nombres reales: "Teléfono" → "telefono", "Cámara" → "camara", "Música" → "musica", "Übersetzer" → "ubersetzer". La app se distribuye en `es`, `fr` y `de`, donde los diacríticos son ortografía, no decoración.

**Nota de rendimiento:** `Regex("\\p{InCombiningDiacriticalMarks}+")` se compila en cada llamada; se invoca una vez por app en cada carga del listado.

**Corrección propuesta:**
- Separar **normalización para búsqueda** (sin acentos, minúsculas — usada por `FuzzyMatcher`) de **normalización para visualización** (solo `lowercase(Locale)` si se quiere mantener la estética minimalista).
- Compilar la `Regex` una vez como constante de fichero.

---

## FP-057 — El reloj ignora el ajuste de 12/24 horas del sistema

**Ficheros:** `ui/home/HomeActivity.kt:582-583` y `ui/settings/HomeLayoutActivity.kt:465-466`

```kotlin
val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
```
Formato de 24 h fijo, ignorando `DateFormat.is24HourFormat(context)`. En `en-US` (locale declarado en `locales_config.xml`) el formato esperado es de 12 h con AM/PM.

Además se instancian **dos `SimpleDateFormat` nuevos por minuto** en ambas pantallas.

**Corrección propuesta:** Usar `DateFormat.getTimeFormat(context)` o comprobar `is24HourFormat`, y cachear los formateadores como campos.

---

## FP-058 — `AnalogClockView` describe AM/PM incluso en locales de 24 h

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/widget/AnalogClockView.kt:85-92`

`updateContentDescription()` construye siempre `"%02d:%02d %s"` con AM/PM y usa `String.format` sin `Locale` (lint `DefaultLocale`). Lectores de pantalla en configuración de 24 h reciben una descripción incorrecta.

---

## FP-059 — `PermissionsActivity` muestra un estado siempre "concedido" para `QUERY_ALL_PACKAGES`

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/settings/PermissionsActivity.kt:68-70`

**Descripción:** `QUERY_ALL_PACKAGES` es un permiso *normal* (install-time). `checkSelfPermission` devuelve `PERMISSION_GRANTED` siempre que esté declarado en el manifiesto. El botón "abrir ajustes de la app" tampoco permite revocarlo.

**Impacto:** Fila de permiso engañosa: nunca puede mostrar otra cosa que "concedido", y el botón no lleva a ningún sitio útil.

**Corrección propuesta:** Sustituirla por una comprobación real de funcionalidad (p. ej. `appRepository.getInstalledApps().size > N`) o eliminar la fila.

---

# 6. ⬜ BAJO — Código muerto, deuda y pulido

## FP-060 — Clases y ficheros completamente muertos

| Elemento | Fichero | Líneas |
|---|---|---|
| `WidgetConfigActivity` | `ui/widget/WidgetConfigActivity.kt` | 229 |
| `ClockStyle` (enum completo) | `domain/model/ClockStyle.kt` | 22 |
| `WeightGradientTextView` | `ui/widget/WeightGradientTextView.kt` | 81 |
| `HomeEvent` + `HomeViewModel._events`/`events` | `ui/home/HomeViewModel.kt:33-35, 68-71` | 7 |

`WidgetConfigActivity` está declarada en el manifiesto (marcada como *"legacy, kept for compatibility"*) pero **ningún código la inicia**; duplica el flujo ya resuelto por `WidgetPickerActivity`. `HomeEvent.LimitReached` nunca se emite ni se recoge (el equivalente funcional vive en `AppSelectEvent`).

**Corrección propuesta:** Eliminar los cuatro elementos y la entrada de `WidgetConfigActivity` del `AndroidManifest.xml`.

---

## FP-061 — Los deslizadores de tamaño de `HomeLayoutActivity` son inalcanzables

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/settings/HomeLayoutActivity.kt:893-936, 244-256`

**Descripción:** `showSliderSheet()` **nunca se llama** (`onWrapperClicked` lo comenta explícitamente: *"Size adjustment is no longer offered from this screen"*). Queda muerto todo el subsistema:

- `showSliderSheet()`, `saveSizes()`, `isTouchInsideView()`, el campo `draggedView` (asignado, nunca leído), `selectedComponent`
- `setupSliderSheet()`, `updateComponentWeights()`
- `HomeLayoutPreferences.getClockWeightPercent/setClockWeightPercent/getWidgetWeightPercent/setWidgetWeightPercent`
- Las vistas `slider_section`, `slider_size`, `tv_slider_title`, `tv_slider_value` en `activity_home_layout.xml`

**Corrección propuesta:** Eliminar el subsistema completo, incluidas las vistas del layout y las claves de preferencias (con migración que las borre).

---

## FP-062 — Métodos públicos sin uso

| Método | Fichero |
|---|---|
| `FontPreferences.resolveTypeface()` | `data/preferences/FontPreferences.kt:83-104` |
| `WallpaperPreferences.clearWallpaper()` | `data/preferences/WallpaperPreferences.kt:115-121` |
| `WallpaperPreferences.getRawHomeWallpaperUri()` | `:50-51` |
| `WallpaperPreferences.getRawNotificationsWallpaperUri()` | `:60-61` |
| `WallpaperPreferences.getRawSettingsWallpaperUri()` | `:69-70` |
| `HomeLayoutPreferences.resetToDefaults()` | `data/preferences/HomeLayoutPreferences.kt:110-120` |
| `FocusNotificationService.clearPendingIntents()` | `service/FocusNotificationService.kt:126-128` |
| `TutorialPreferences.isOnboardingFlowCompleted()` | `data/preferences/TutorialPreferences.kt:47-48` |
| `AppScheduleActivity.newIntent()` | `ui/schedule/AppScheduleActivity.kt:381-383` |

**Nota:** `clearWallpaper()` sin uso significa además que **no existe forma de quitar un fondo ya configurado** una vez elegido (solo sustituirlo o desactivar el sistema entero). Es una carencia funcional, no solo código muerto.

**Nota:** `clearPendingIntents()` debe **conservarse** y empezar a usarse (ver FP-005/FP-006), no eliminarse.

---

## FP-063 — Constantes y variables sin usar

| Elemento | Fichero:línea |
|---|---|
| `REASON_WINDOW` | `ui/schedule/ScheduleBlockedActivity.kt:118` |
| `import app.focusphone.BuildConfig` (sin usar) | `data/local/AppDatabase.kt:8` |
| `import android.widget.LinearLayout` (sin usar) | `ui/settings/SettingsActivity.kt:5` |
| `val app = getItem(position)` (asignada, no usada) | `ui/home/adapter/FavouriteAppsAdapter.kt:76` |
| `import android.os.Bundle` (sin usar) | `ui/settings/HomeSettingsActivity.kt`, `FocusSettingsActivity.kt`, `NotificationSettingsActivity.kt` |

---

## FP-064 — Código duplicado literal dentro de `HomeActivity.onResume`

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/home/HomeActivity.kt:186-200`

Los bloques `gravity` y `emptyGravity` son **idénticos carácter a carácter** (misma expresión `when` sobre `ThemePreferences.getAlignment`). Además, `applyClockType()` (llamado antes, línea 177) ya ha fijado `tvEmpty.gravity = CENTER_VERTICAL`, valor que estas líneas sobrescriben — dos rutas compitiendo por el mismo atributo.

**Corrección propuesta:** Calcular la gravedad una sola vez y eliminar la asignación redundante de `applyClockType()`.

---

## FP-065 — Ficheros de configuración de backup muertos

**Ficheros:** `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`

**Descripción:** El manifiesto declara `android:allowBackup="false"` y **no referencia** `android:fullBackupContent` ni `android:dataExtractionRules`. Ambos ficheros XML —con reglas detalladas de inclusión/exclusión— no se aplican jamás. Lint también avisa (`DataExtractionRules`).

**Corrección propuesta:** Decidir: o se habilita el backup y se referencian los ficheros, o se eliminan. Dejar reglas que parecen activas y no lo están es engañoso para cualquier revisión de privacidad.

---

## FP-066 — `values-night/themes.xml` vacío

**Fichero:** `res/values-night/themes.xml` — contiene solo `<resources />` y un comentario. Confirma que la gestión de modo oscuro es manual (`THEME_AUTO`), pero el fichero vacío puede inducir a error. Eliminar o documentar en el README.

---

## FP-067 — 66 strings sin usar

```
app_label, home_menu_desc, settings_alignment_left, settings_alignment_center,
settings_alignment_right, settings_section_security, settings_accessibility_desc,
settings_clock_style_label, settings_clock_thin, settings_clock_light,
settings_clock_regular, settings_clock_bold, settings_language_system_unsupported,
onboarding_get_started, onboarding_step1_title, onboarding_step1_body,
onboarding_step2_title, onboarding_step2_body, onboarding_step2_action,
onboarding_step3_title, onboarding_step3_body, onboarding_step3_action,
onboarding_back, onboarding_step_profiles_title, onboarding_step_profiles_body,
onboarding_step_customize_title, onboarding_step_customize_body,
tutorial_coach_block_title, tutorial_coach_block_desc, tutorial_coach_rename_title,
tutorial_coach_rename_desc, tutorial_coach_detox_title, tutorial_coach_detox_desc,
tutorial_coach_customize_title, tutorial_coach_customize_desc, cd_remove_app,
cd_custom_theme_toggle, cd_language_toggle, pause_hold, detox_remaining_time,
settings_section_tutorial, tutorial_btn_next, tutorial_btn_back, tutorial_btn_finish,
allapps_folder_caret, settings_work_keywords_title, settings_work_keywords_desc,
cd_work_keywords_toggle, settings_section_gestures, settings_section_widget,
settings_widget_desc, settings_widget_add, widget_config_title, home_layout_preview,
home_layout_order, home_layout_sizes, cd_drag_handle, settings_text_size_desc,
settings_alignment_desc, settings_clock_type_desc, settings_clock_size_desc,
settings_app_spacing_desc, settings_home_layout_desc,
settings_conscious_pause_section_desc, settings_work_keywords_section_desc,
settings_notification_badges_section_desc
```

Cada una existe además en `es`, `fr` y `de`: **~264 entradas de traducción muertas**.

Los grupos `onboarding_step1/2/3_*` y `tutorial_coach_{block,rename,detox,customize}_*` son restos de versiones anteriores de los flujos descritos en **FP-049**.

**Corrección propuesta:** Eliminar de los cuatro ficheros de strings. Revisar antes los `tutorial_coach_*`: puede que el contenido deba **reinstaurarse** en `TUTORIAL_STEPS` en lugar de borrarse (decisión de producto).

---

## FP-068 — 19 recursos sin usar

```
color/color_segmented_control_bg      color/color_segmented_text
color/color_switch_thumb              color/color_switch_track
drawable/bg_circle_preview            drawable/bg_component_toggle_active
drawable/bg_component_toggle_inactive drawable/bg_preview_block_selected
drawable/bg_segmented_item_active     drawable/bg_theme_card_selected
drawable/ic_chevron_down              drawable/ic_chevron_up
drawable/ic_menu_hamburger            drawable/ic_settings_work
drawable/ic_tutorial_themes           drawable/shape_breathing_circle
drawable/shape_current_day            layout/item_preview_block
menu/menu_home
```

Lint reporta **131** `UnusedResources` en total (incluye strings). `shape_breathing_circle` sugiere que la animación de respiración de la pausa consciente nunca se implementó visualmente.

---

## FP-069 — APIs deprecadas (avisos del compilador)

| API | Ficheros |
|---|---|
| `Intent.ACTION_UNINSTALL_PACKAGE` | `AllAppsActivity.kt:396`, `AppSelectActivity.kt:62`, `HomeActivity.kt:1333` |
| `Bundle.getParcelable(String)` | `WidgetPickerActivity.kt:51` |
| `PackageManager.resolveActivity(Intent, Int)` | 5 ubicaciones (ver FP-025) |
| `PackageManager.getApplicationInfo(String, Int)` | `FocusNotificationService.kt:64`, `AppScheduleActivity.kt`, `ScheduleBlockedActivity.kt` |
| `overridePendingTransition` | `BaseActivity.kt:125, 132`, `AppearanceSettingsActivity.kt:290` |
| `startActivityForResult`/`onActivityResult` | `HomeLayoutActivity.kt`, `WidgetPickerActivity.kt`, `WidgetConfigActivity.kt` |

Nota de coherencia: `HomeSettingsActivity` usa `registerForActivityResult` para el mismo selector de widget que `HomeLayoutActivity` invoca con `startActivityForResult`. **Dos mecanismos para el mismo flujo.**

---

## FP-070 — 85 avisos `UseKtx`

Lint sugiere reemplazar patrones manuales por extensiones de `androidx.core`:
- `prefs.edit().putX(...).apply()` → `prefs.edit { putX(...) }`
- `Color.parseColor(hex)` → `hex.toColorInt()`
- `Uri.parse(s)` → `s.toUri()`

Afecta a **todos** los ficheros de `data/preferences/`. Mejora legibilidad y seguridad ante olvidos de `apply()`.

---

## FP-071 — Dependencias desactualizadas

Lint reporta **21** `GradleDependency` y **6** `NewerVersionAvailable`. Versiones actuales en `gradle/libs.versions.toml`:

| Librería | Versión declarada |
|---|---|
| `androidx.core:core-ktx` | 1.13.1 |
| `androidx.activity:activity-ktx` | 1.9.3 |
| `androidx.fragment:fragment-ktx` | 1.8.5 |
| `androidx.lifecycle:*` | 2.8.7 |
| `androidx.constraintlayout` | 2.1.4 |
| `kotlinx-coroutines` | 1.8.1 |
| `com.google.android.material` | 1.12.0 |
| `robolectric` | 4.12.2 |
| `mockk` | 1.13.11 |

**Corrección propuesta:** Actualizar en bloque y ejecutar la suite de tests. Prioridad a `material` y `lifecycle` (correcciones de accesibilidad y de fugas).

**Nota de aplicación (Fase 5):** todas las dependencias se actualizaron a su última versión estable, con una excepción: `androidx.core:core-ktx` se fija en **1.18.0** en lugar de 1.19.0. La 1.19.0 exige `compileSdk ≥ 37` (`checkReleaseAarMetadata` falla si no); subir `compileSdk` es un cambio de superficie mucho mayor que una actualización de dependencias (nuevas reglas de lint, requiere instalar la plataforma 37.x en cada máquina y en CI) y queda fuera del alcance de este hallazgo. 1.18.0 es la última versión de `core-ktx` compatible con `compileSdk 36`.

---

## FP-072 — `versionName` desincronizado con la rama y los commits

**Fichero:** `focusphone-project/app/build.gradle.kts:19-20`

```kotlin
versionCode = 2
versionName = "1.0.4"
```
La rama es `alfa-1.0.6` y el historial reciente menciona `alfa-1.0.5` y `alfa-1.0.6`. La pantalla "Acerca de" muestra `BuildConfig.VERSION_NAME`, es decir, **1.0.4 a un usuario que ejecuta 1.0.6**.

**Corrección propuesta:** Sincronizar `versionName` con la rama y elevar `versionCode`. Considerar derivarlo del tag de git para que no vuelva a divergir.

---

## FP-073 — `gradle-wrapper.jar` no está versionado

**Ficheros:** `.gitignore:25` (`*.jar`), `gradle/wrapper/gradle-wrapper.jar`

**Descripción:** El patrón `*.jar` del `.gitignore` excluye el JAR del wrapper. `git ls-files gradle/` solo devuelve `libs.versions.toml` y `gradle-wrapper.properties`.

**Impacto:** Un clon limpio del repositorio **no puede ejecutar `./gradlew`**. Rompe CI y la incorporación de nuevos desarrolladores.

**Corrección propuesta:**
```gitignore
*.jar
!gradle/wrapper/gradle-wrapper.jar
```

---

## FP-074 — `org.gradle.java.home` con ruta absoluta local

**Fichero:** `focusphone-project/gradle.properties:18`

```properties
org.gradle.java.home=/usr/lib/jvm/java-21-openjdk
```
Ruta específica de esta máquina Arch Linux, versionada en el repositorio. Rompe la build en cualquier otro entorno (macOS, Windows, CI).

**Corrección propuesta:** Eliminar la línea y usar el *toolchain* de Gradle (ya está aplicado `foojay-resolver-convention` en `settings.gradle.kts`):
```kotlin
kotlin { jvmToolchain(21) }
```

---

## FP-075 — APK de release y artefactos de build versionados

**Ficheros:**
```
focusphone-project/app/release/app-release.apk
focusphone-project/app/release/output-metadata.json
focusphone-project/app/release/baselineProfiles/0/app-release.dm
focusphone-project/app/release/baselineProfiles/1/app-release.dm
```

`git ls-files` confirma que `output-metadata.json` y los `.dm` están **rastreados**. Artefactos binarios de build en el control de versiones inflan el repositorio en cada release.

**Corrección propuesta:** `git rm --cached` y añadir `app/release/` al `.gitignore`.

---

## FP-076 — `docs/` está en el `.gitignore` raíz

**Fichero:** `.gitignore:39-40`

```gitignore
# Local working documentation and agent skills (not part of the published project)
docs/
```
**Este mismo documento no será versionado.** Si se espera que los agentes de corrección lo consuman desde el repositorio (o desde CI), hay que excluirlo del patrón:
```gitignore
docs/
!docs/auditoria-tecnica-completa.md
```

---

## FP-077 — Tests de plantilla en el paquete equivocado

**Ficheros:**
- `app/src/test/java/dev/rezzt/ExampleUnitTest.kt`
- `app/src/androidTest/java/dev/rezzt/ExampleInstrumentedTest.kt`

Restos del asistente de creación de proyectos, en el paquete `dev.rezzt` en lugar de `app.focusphone`. `ExampleUnitTest` solo comprueba `2 + 2 == 4`.

**Corrección propuesta:** Eliminar ambos y el directorio `dev/rezzt`.

---

## FP-078 — Faltan `contentDescription` y atributos de accesibilidad

**ImageViews sin `contentDescription` ni `importantForAccessibility="no"`** (lint `ContentDescription`):
- `activity_home.xml` → `iv_wallpaper_home`
- `activity_settings_sub.xml`, `activity_settings.xml` → `iv_wallpaper_settings`
- `dialog_tutorial_carousel.xml` → `iv_tutorial_icon`
- `settings_wallpaper_preview.xml` → `iv_wallpaper_preview`

**`ClickableViewAccessibility`** — `setOnTouchListener` sin llamar a `performClick`:
- `HomeLayoutActivity.kt:372` (manejador de arrastre)
- `HomeLayoutActivity.kt:685` (`setupWrapperTouch`)

**`Autofill`** — falta `android:autofillHints` en 6 `EditText`:
`dialog_detox_custom_time.xml:26,47`, `dialog_detox_password_set.xml:28`, `dialog_detox_password_verify.xml:28`, `settings_custom_theme_panel.xml:44,91`

Los campos de contraseña deberían declarar `autofillHints="password"` o `importantForAutofill="no"` explícitamente.

**`SmallSp`** — `item_settings_clock_type.xml:43` usa `10sp` (mínimo recomendado: 11sp).

---

## FP-079 — Texto embebido en layouts y concatenación en `setText`

**`HardcodedText`:**
| Fichero:línea | Texto |
|---|---|
| `dialog_detox_custom_time.xml:43` | `":"` |
| `item_settings_font.xml:47` | `"✓"` |
| `dialog_tutorial_carousel.xml:17` | `"1 / 8"` |
| `dialog_tutorial_carousel.xml:75` | `"←"` |
| `dialog_tutorial_carousel.xml:93` | `"→"` |

**`SetTextI18n`** (concatenación / literales en `setText`):
- `AboutSettingsActivity.kt:208` — `"${currentIndex + 1} / ${slides.size}"`
- `AppearanceSettingsActivity.kt:493` — `item.tvFontSample.text = "focusphone"`
- `HomeLayoutActivity.kt:926, 945` — `"${value.toInt()}%"`
- `ConsciousPauseActivity.kt:70`, `HomeActivity.kt:1471` — formateo de números sin locale

**`DefaultLocale`** — 9 usos de `String.format` sin `Locale` explícito.

---

## FP-080 — Candidatos a `plurals` y erratas

**`PluralsCandidate`** — 6 strings con `%d` seguido de un sustantivo, que deberían ser `<plurals>`:
`values/strings.xml:31, 32, 192, 581, 589, 597` (apps, seconds, characters, attempts, minutes).

**`Typos`:**
- `values/strings.xml:178` — `"ok"` debería ser `"OK"`
- `values-es/strings.xml:508, 509, 512` — `veintidos`, `veintitres`, `veintiseis` deberían llevar tilde (`veintidós`, `veintitrés`, `veintiséis`). **Son entradas de `clock_words_minutes`**, es decir, texto visible en el reloj de tipo "words" en español.
- Los avisos de `values/font_certs.xml` son falsos positivos sobre certificados Base64; ignorar.

**`ObsoleteSdkInt`** — comprobaciones redundantes (minSdk es 24):
- `AboutSettingsActivity.kt:150` (×2) — `SDK_INT >= N`
- `themes.xml:34, 91, 150, 207` — `tools:targetApi="23"` en `windowLightStatusBar`

---

## FP-081 — `Overdraw` en todos los layouts de ítem

12 layouts de ítem pintan `?attr/selectableItemBackground` sobre un tema que ya pinta fondo:
`item_all_app.xml`, `item_all_app_folder.xml`, `item_favourite_app.xml`, `item_installed_app.xml`, `item_notification.xml`, `item_schedule_rule.xml`, `item_settings_category.xml`, `item_settings_font.xml`, `item_settings_switch.xml`, `item_settings_value.xml`, `item_widget_provider.xml`, `settings_divider.xml`.

**Corrección propuesta:** Establecer `android:windowBackground` en los temas y `null` como fondo de los contenedores raíz, o marcar los avisos como aceptados si el ripple es necesario. Impacto real: moderado en dispositivos de gama baja con listas largas.

---

## FP-082 — Error de lint `WrongConstant` en `expandNotificationsPanel`

*Hallazgo añadido durante la Fase 5 (no figuraba en el análisis de `e499f88`).*

**Fichero:** `focusphone-project/app/src/main/java/app/focusphone/ui/home/HomeActivity.kt` (`expandNotificationsPanel`)

```kotlin
val statusBarService = getSystemService(Context.STATUS_BAR_SERVICE)
```
`Context.STATUS_BAR_SERVICE` solo es pública desde API 33 y no forma parte del `@StringDef` de `getSystemService`: lint reporta **`WrongConstant` (error)** y además `InlinedApi` con `minSdk` 24. Era el único error de lint restante tras corregir FP-012, FP-053, FP-054 y FP-055, e impedía el criterio de cierre de la Fase 5 (0 errores).

**Corrección propuesta:** usar el nombre del servicio (`"statusbar"`) como constante privada y documentar la supresión de `WrongConstant` en el método. El problema de fondo —la reflexión bloqueada en Android moderno— sigue abierto en **FP-038** (Fase 7).

**Relacionado con:** FP-038.

---

# 7. Anexo A — Detalles de menor entidad no catalogados

Elementos correctos pero mejorables, sin ID propio. Aplicar solo si no introducen riesgo.

| Ubicación | Observación |
|---|---|
| `AppContainer.kt:44-53` | `appRepository` y `favouriteAppRepository` son `val` **eager**, no `by lazy` como el resto. Fuerzan la construcción de la BD Room y la migración de SharedPreferences en el primer acceso a `container`, normalmente en el hilo principal. |
| `AppContainer.kt` | Uso de nombres completamente cualificados (`app.focusphone.data.preferences.…`) en lugar de imports. Ruido innecesario. |
| `FolderDragCallback.kt:163` | `var bottom = Float.MIN_VALUE` — `Float.MIN_VALUE` es el menor positivo (1.4e-45), no `-∞`. Funciona por accidente; usar `-Float.MAX_VALUE`. |
| `AppSchedulePreferences.kt:255-260` | Al alcanzar `MAX_FAILED_ATTEMPTS` el contador se pone a 0, por lo que `getRemainingAttempts()` devuelve 5 durante el cooldown. Solo se evita mostrar el valor porque `onWrongPassword` comprueba `isLockedOut` antes. |
| `SecureStorageHelper.kt:36, 58` | El parámetro `context` de `encrypt`/`decrypt` no se usa. |
| `PasswordHasher.kt:43-49` | PBKDF2 con 100 000 iteraciones se ejecuta en el hilo principal (desde los diálogos de verificación). En gama baja son 200-500 ms de bloqueo. Mover a `Dispatchers.Default`. |
| `BaseActivity.kt:122-133` | `startActivity(Intent)` está sobrescrito para la transición, pero no `startActivity(Intent, Bundle)` ni `startActivityForResult`. Transiciones inconsistentes. |
| `AppearanceSettingsActivity.kt:284-291` | `relaunchForThemeChange()` llama a `finish()` + `startActivity()` + `overridePendingTransition()`, y tanto `finish()` como `startActivity()` de `BaseActivity` ya llaman a `overridePendingTransition`. Tres invocaciones encadenadas. |
| `AppearanceSettingsActivity.kt:305` | `Color.parseColor("#3A3A3A")` codificado para el borde del círculo de tema personalizado; ignora la paleta. |
| `SystemSettingsActivity.kt:113-122` | Usa `androidx.appcompat.AlertDialog.Builder` mientras el resto de la app usa `MaterialAlertDialogBuilder`. Además llama a `recreate()` tras `setApplicationLocales`, que ya recrea en API < 33. |
| `AboutSettingsActivity.kt:96-101` | `confirmRestartOnboarding` no cierra la pantalla "Acerca de"; al terminar el onboarding el usuario vuelve a ella. |
| `HomeLayoutActivity.kt:601-615` | `observeApps()` usa `lifecycleScope.launch { collect }` sin `repeatOnLifecycle`, a diferencia del resto del proyecto. Colecta el `Flow` de Room en segundo plano. |
| `HomeLayoutActivity.kt:127-137` | Reloj actualizado por dos vías simultáneas: `ACTION_TIME_TICK` y `handler.postDelayed`. Redundante. |
| `HomeLayoutActivity.kt:765-770` | `checkReorder()` llama a `saveOrder()` en cada cruce durante el arrastre: una escritura de preferencias por cruce. |
| `OnboardingActivity.kt:213-231` | `updateUi()` contiene un `when` cuyas tres ramas devuelven `R.string.onboarding_next`, seguido de una reasignación del mismo valor. Lógica condicional muerta. |
| `OnboardingActivity.kt:334-348` | El `ViewHolder` repite la comprobación de launcher por defecto en cada `bind` en lugar de leer `isLauncherVerified` de la Activity. |
| `WidgetPickerActivity.kt:233` | `INVALID_WIDGET_ID = -1` duplica `WidgetPreferences.INVALID_WIDGET_ID`; el valor del framework (`AppWidgetManager.INVALID_APPWIDGET_ID`) es **0**. |
| `accessibility_service_config.xml` | Falta `android:description`, que es lo que el sistema muestra al usuario al conceder el permiso. Además declara `canRetrieveWindowContent="true"` aunque el servicio solo usa `event.packageName` — capacidad excesiva para lo que necesita, y un riesgo en la revisión de Google Play. |
| `ScheduleBlockedActivity.kt` | No aplica `FLAG_SECURE` (a diferencia de `DetoxBlockedActivity`), ni revalida la regla en `onResume`. |
| `ScheduleAccessGate.kt` | Concesión de 5 minutos a nivel de proceso: tras desbloquear los horarios una vez, cualquiera puede reentrar durante ese tiempo. Comportamiento documentado, pero conviene revisarlo. |
| `AllAppsActivity.kt:352-359` | `removeProtection()` borra **todas** las reglas junto con la contraseña, sin deshacer. |
| `AppSelectActivity.kt:152-156` | `onResume` invalida la caché y recarga **siempre**: consulta completa a `PackageManager` + `loadLabel` de todas las apps en cada reanudación. |
| `ConsciousPauseActivity.kt:44-49` | El `OnBackPressedCallback` se deshabilita y redispara inmediatamente: envoltorio sin efecto. |
| `FocusAccessibilityService.kt` | No sobrescribe `onDestroy()`; si el servicio se destruye sin `onUnbind`, `screenReceiver` queda registrado. |

---

# 8. Anexo B — Criterios de verificación

Tras cada fase:

```bash
cd focusphone-project
./gradlew :app:compileDebugKotlin      # sin errores, idealmente sin warnings
./gradlew :app:testDebugUnitTest       # 14 clases de test deben seguir pasando
./gradlew :app:lintDebug               # objetivo final: 0 errores
```

**Comprobaciones manuales obligatorias tras la Fase 1:**
1. Activar Detox 15 min → introducir 6 contraseñas incorrectas → **el bloqueo debe seguir activo**.
2. Elegir un fondo de pantalla → forzar la detención de la app → reabrir → **el fondo debe seguir ahí**.
3. Con > 10 favoritas en la BD, reordenar la lista → **no debe desaparecer ninguna**.

**Comprobaciones manuales tras la Fase 2:**
4. Abrir y cerrar la home 20 veces → deslizar para descartar una notificación → **debe descartarse exactamente una vez**.
5. Añadir un widget → quitarlo → añadirlo → repetir 5 veces → verificar con `adb shell dumpsys appwidget` que **no se acumulan IDs**.

---

*Documento generado a partir de un análisis estático exhaustivo del código fuente, complementado con la ejecución de `compileDebugKotlin`, `testDebugUnitTest` y `lintDebug` sobre el commit `e499f88`.*
