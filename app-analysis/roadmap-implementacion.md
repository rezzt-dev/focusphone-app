# FocusPhone — Roadmap de Implementación Profesional

> **Fecha:** 2026-05-25  
> **Versión base:** 1.0.4  
> **Objetivo:** Cerrar gaps funcionales críticos sin introducir regresiones, manteniendo la arquitectura MVVM + Clean Architecture existente.

---

## Principios Rectores

1. **Cero regresiones en bloqueo de apps.** El `FocusAccessibilityService` y la lógica de `ProfilePreferences` son la razón de ser de la app. Ningún cambio los toca sin tests de cobertura previos.
2. **Feature flags implícitos.** Las nuevas funcionalidades deben poder desactivarse o ignorarse sin romper el flujo existente.
3. **SharedPreferences síncrono para temas/fuentes.** Se mantiene la decisión arquitectónica documentada en `AGENTS.md`. No se introduce DataStore para esos casos.
4. **Idioma del código:** KDoc y comentarios en inglés. UI strings en inglés lowercase (estilo minimalista).

---

## FASE 1: Fundamentos de Accesibilidad y UX Rápida

**Tiempo estimado total: 3–4 días**  
**Riesgo de ruptura: BAJO**  
**Valor para el usuario: ALTO**

Esta fase corrige problemas que afectan a usuarios reales hoy mismo (especialmente usuarios con necesidades de accesibilidad) y son cambios localizados, fáciles de testear.

### 1.1 Corrección del escalado de fuente del sistema (Crítico accesibilidad)

- **Problema:** `BaseActivity.attachBaseContext()` fuerza `config.fontScale = FontPreferences.getFontScale(newBase)`, **anulando** por completo el ajuste de accesibilidad del sistema Android. Un usuario con `fontScale=1.3` en el sistema ve `1.0` a menos que toque el slider de la app.
- **Solución:** El slider de la app debe actuar como **multiplicador relativo** al `fontScale` del sistema, no como valor absoluto.
  - `FontPreferences.getFontScale(context)` → `getFontScaleMultiplier(context)` (default 1.0f).
  - En `attachBaseContext()`: `config.fontScale = systemFontScale * userMultiplier`.
  - Coerce final a `[0.5f, 2.0f]` para evitar valores destructivos.
- **Archivos:** `BaseActivity.kt`, `FontPreferences.kt`, `SettingsActivity.kt` (label del slider).
- **Tests:** Unit test de `FontPreferences` verificando que `multiplier * systemScale` está acotado.
- **Tiempo:** 4–6 horas.

### 1.2 Completar Content Descriptions faltantes

- **Problema:** Faltan `android:contentDescription` en: items de apps en `FavouriteAppsAdapter`/`AllAppsAdapter`, días del calendario generados dinámicamente, botón hamburguesa (drawer), indicadores de página.
- **Solución:**
  - Añadir `contentDescription` programático en adapters: `"${app.label}, open app"`.
  - Calendario dinámico: `"day $dayNumber"`, con `"today, $dayNumber"` para el día actual.
  - Indicadores home/menu: `"home indicator"` / `"menu indicator"`.
- **Archivos:** `FavouriteAppsAdapter.kt`, `AllAppsActivity.kt`, `HomeActivity.kt`.
- **Tiempo:** 3–4 horas.

### 1.3 Relanzar Onboarding desde Settings

- **Problema:** El usuario no puede volver a ver el flujo original de onboarding (Welcome → Set Launcher → Pick Apps) tras completarlo una vez.
- **Solución:**
  - Añadir en `SettingsActivity` un nuevo item "restart onboarding" que:
    1. Llame a `ThemePreferences.setOnboardingComplete(this, false)`.
    2. Luego `recreate()` o `finish()` para que `HomeActivity` lo detecte en el próximo resume.
  - O más limpio: lanzar directamente `OnboardingActivity` con un flag `FORCE_RESTART` y, al finalizar, marcar onboarding como completo de nuevo.
- **Archivos:** `SettingsActivity.kt`, `ThemePreferences.kt` (añadir `setOnboardingComplete(context, boolean)` sobrecargado), `strings.xml`.
- **Tiempo:** 2–3 horas.

### 1.4 Mejorar manejo de errores silenciados (Crítico estabilidad)

- **Problema:** Múltiples `catch (_: Exception)` que devuelven `emptyList()`, ocultan crashes y hacen debugging imposible.
- **Solución (no invasiva):**
  - En `AppSelectViewModel.kt:45` y `AllAppsViewModel.kt:69`: reemplazar `catch (_: Exception) { emptyList() }` por logging estructurado vía `android.util.Log` en debug, y en release devolver `emptyList()` pero con un evento de error expuesto al UI (opcional: un `SharedFlow<String>` para toast de error).
  - En `BaseActivity.kt`, `HomeActivity.kt` y `AllAppsActivity.kt`: revisar todos los `catch (_: Exception)` y al menos loggear con `Log.e` el tipo de excepción y el mensaje.
- **Archivos:** `AppSelectViewModel.kt`, `AllAppsActivity.kt`, `HomeActivity.kt`, `BaseActivity.kt`.
- **Tiempo:** 3–4 horas.

### 1.5 Búsqueda difusa básica (Fuzzy Search)

- **Problema:** La búsqueda es `String.contains()` exacto. Buscar "fbook" no encuentra "facebook".
- **Solución:** Implementar un algoritmo de **Levenshtein distance** ligero o un **scoring por subsecuencia** (subsequence match con bonus por consecutividad) para los filtros de `AppSelectViewModel` y `AllAppsViewModel`.
  - Algoritmo recomendado: **Fuzzy matching por subsecuencia** (no requiere librerías externas, ~40 líneas de Kotlin puro). Puntuación: +1 por carácter coincidente, +3 por carácter consecutivo. Umbral mínimo para mostrar resultado.
  - Fallback: si no hay resultados fuzzy, mostrar lista vacía (comportamiento actual).
- **Archivos:** Nuevo `domain/util/FuzzyMatcher.kt`, `AppSelectViewModel.kt`, `AllAppsViewModel.kt`.
- **Tests:** Unit tests con casos edge (`"fb" → "facebook"`, `"yt" → "youtube"`, `"zzz" → empty`).
- **Tiempo:** 6–8 horas.

---

## FASE 2: Robustez, Testing y Perfil Work Configurable

**Tiempo estimado total: 5–7 días**  
**Riesgo de ruptura: MEDIO**  
**Valor para el usuario: ALTO**

### 2.1 Suite de tests unitarios para lógica crítica

- **Cobertura objetivo:**
  - `ProfilePreferences`: `isAppBlocked()` con los 3 perfiles, expiración de detox, lógica de intentos fallidos, migración legacy SHA-256 → PBKDF2.
  - `FuzzyMatcher`: todos los casos de matching y no-matching.
  - `ToggleFavouriteAppUseCase`: límite de 10 apps, reordenamiento.
  - `ThemePreferences`: resolución de tema, fallback de colores hex inválidos.
- **Estrategia:** Usar `FakeFavouriteAppRepository.kt` existente como base. Mockear `Context` solo cuando sea inevitable (preferir lógica pura).
- **Archivos:** `app/src/test/java/app/focusphone/...`
- **Tiempo:** 2 días.

### 2.2 Tests instrumentados para flujos críticos

- **Flujos a testear:**
  - Onboarding completo (3 pasos).
  - Selección de apps favoritas, toggle, límite de 10.
  - Cambio de perfil Normal → Work → Detox.
  - Verificación de password de detox (incluyendo lockout tras 4 intentos).
- **Estrategia:** Usar `ActivityScenario` + Espresso. El repositorio de apps instaladas debe inyectarse con una lista predecible (mock de `GetInstalledAppsUseCase`).
- **Nota:** Requiere que `AppContainer` permita sustitución de dependencias en test (constructor injection o sobrescritura de factory). Evaluar si es necesario refactor leve de `AppContainer`.
- **Tiempo:** 2–3 días.

### 2.3 Perfil Work con keywords configurables por usuario

- **Problema:** `DISTRACTING_KEYWORDS` es una lista hardcodeada en `ProfilePreferences.kt`. El usuario no puede añadir/quitar keywords ni apps manualmente para Work.
- **Solución:**
  - Añadir a `ProfilePreferences`:
    - `KEY_WORK_KEYWORDS` (lista de strings, separados por `\n`, cifrados igual que blocked_apps).
    - `getWorkKeywords()`, `setWorkKeywords()`.
    - Modificar `isAppBlocked()` para usar la lista personalizada en vez de `DISTRACTING_KEYWORDS` hardcodeada.
  - UI en `AllAppsActivity`: en el diálogo de long-press, añadir opción "block in work mode" (distinguir de block global/detox).
  - UI en `SettingsActivity`: nueva sección "work profile" para gestionar keywords (lista editable simple).
- **Migración:** Mantener `DISTRACTING_KEYWORDS` como valores por defecto pre-poblados la primera vez que se abra la UI de configuración.
- **Archivos:** `ProfilePreferences.kt`, `AllAppsActivity.kt`, `SettingsActivity.kt`, `strings.xml`.
- **Tests:** Unit tests de `isAppBlocked()` con keywords personalizadas.
- **Tiempo:** 2–3 días.

### 2.4 Modo automático día/noche (Auto Theme)

- **Problema:** El usuario debe cambiar tema manualmente. No hay adaptación al tema del sistema.
- **Solución:**
  - Nuevo valor `THEME_AUTO` en `ThemePreferences`.
  - `ThemePreferences.resolveStyleRes()` y `getTheme()` detectan si el tema actual es `THEME_AUTO` y resuelven según `UiModeManager.getNightMode()` o `Configuration.uiMode and UI_MODE_NIGHT_MASK`.
  - `BaseActivity.onResume()` ya detecta cambios de tema y llama `recreate()`, pero hay que asegurar que escuche cambios del sistema (ej. con un `BroadcastReceiver` para `ACTION_UI_MODE_CHANGED` o confiar en `onResume`).
  - En `SettingsActivity`: añadir "auto" al diálogo de selección de tema.
- **Archivos:** `ThemePreferences.kt`, `BaseActivity.kt`, `SettingsActivity.kt`, `themes.xml` (verificar que `values-night/themes.xml` cubra bien los 4 temas built-in).
- **Tiempo:** 4–6 horas.

---

## FASE 3: Features de Paridad con Launcher Nativo

**Tiempo estimado total: 7–10 días**  
**Riesgo de ruptura: MEDIO-ALTO**  
**Valor para el usuario: MEDIO-ALTO**

### 3.1 App Shortcuts (Android 7.1+ / API 25+)

- **Problema:** Long-press en apps favoritas solo muestra menú propio (open, remove, info, uninstall). No muestra los shortcuts dinámicos/pinned del sistema.
- **Solución:**
  - Usar `ShortcutManager` para obtener shortcuts de una app: `getShortcuts()` requiere `android.permission.READ_SHORTCUTS` o `LauncherApps.getShortcuts()` (más apropiado para launchers).
  - La API correcta para launchers de terceros es `LauncherApps.getShortcuts()` con `FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED`. Requiere que la app sea el launcher default.
  - Mostrar shortcuts en un `PopupMenu` o bottom sheet al hacer long-press en favoritas y en AllApps.
  - Lanzar shortcut vía `LauncherApps.startShortcut()`.
- **Consideraciones de seguridad:** Solo obtener shortcuts si `isDefaultLauncher()` es true. Si no lo es, mostrar el menú actual como fallback.
- **Archivos:** Nuevo `domain/usecase/GetAppShortcutsUseCase.kt`, `HomeActivity.kt`, `AllAppsActivity.kt`, `AppContainer.kt`.
- **Tests:** Mock de `LauncherApps` en instrumented tests.
- **Tiempo:** 2–3 días.

### 3.2 Badges de Notificación (Notification Dots / Count)

- **Problema:** No hay acceso al estado de notificaciones, ni badges visuales.
- **Solución:**
  - Implementar un `NotificationListenerService` para recibir eventos de notificación del sistema (`NotificationListenerService`).
  - Mantener un mapa en memoria (y cacheado en SharedPreferences cifrado si es necesario) de `packageName → count`.
  - Mostrar un pequeño indicador (círculo o número) junto al texto de cada app favorita en `HomeActivity` y `AllAppsActivity`.
  - **Diseño minimalista:** Dado el estilo text-only de FocusPhone, el badge debe ser sutil (un punto pequeño o un número entre paréntesis, ej: `"messages (3)"`).
- **Permisos:** `BIND_NOTIFICATION_LISTENER_SERVICE` en manifest. El usuario debe habilitarlo manualmente en Settings del sistema.
- **Privacidad:** No almacenar contenido de notificaciones, solo contador por package. El servicio debe documentar qué datos procesa.
- **Archivos:** Nuevo `service/FocusNotificationService.kt`, `data/preferences/NotificationBadgePreferences.kt`, `HomeActivity.kt`, `AllAppsActivity.kt`, `FavouriteAppsAdapter.kt`, `AllAppsAdapter.kt`.
- **Tests:** Instrumented tests difíciles (requieren permiso de notificación). Al menos unit tests del contador.
- **Tiempo:** 3–4 días.

### 3.3 Gestos de navegación sin conflicto (Android 10+)

- **Problema:** El swipe left desde el borde derecho para abrir el drawer puede entrar en conflicto con el back gesture del sistema.
- **Solución:**
  - Implementar `setSystemGestureExclusionRects()` en `HomeActivity` para reservar una zona del borde derecho (ej. 40dp desde la derecha, altura completa) para el drawer swipe.
  - Alternativa: usar `DrawerLayout` con `drawerLayout.setDrawerLockMode()` adecuado y `androidx.core.view.ViewCompat.setSystemGestureExclusionRects()`.
- **Archivos:** `HomeActivity.kt`.
- **Tiempo:** 2–3 horas.

---

## FASE 4: Features Avanzadas (Mayor Complejidad)

**Tiempo estimado total: 10–15 días**  
**Riesgo de ruptura: ALTO**  
**Valor para el usuario: MEDIO (alto para ciertos segmentos)**

### 4.1 Widgets del Sistema (AppWidgets)

- **Problema:** No hay soporte para widgets. Es una barrera de adopción para usuarios que dependen de widgets de calendario, clima, etc.
- **Solución:**
  - Implementar `AppWidgetHost` y `AppWidgetHostView` en una Activity de configuración dedicada (`WidgetConfigActivity`).
  - Añadir un contenedor de widget en `HomeActivity` (encima del reloj o debajo, configurable). Solo **1 widget** para mantener la filosofía minimalista.
  - Persistir el widget seleccionado en `ThemePreferences` o nueva `WidgetPreferences`.
  - Manejar el ciclo de vida del `AppWidgetHost` (startListening/stopListening en resume/pause).
- **Riesgos:**
  - Los widgets pueden romper el diseño edge-to-edge.
  - Algunos widgets asumen launcher nativo y pueden comportarse mal.
  - `AppWidgetHost` requiere cuidado con memory leaks.
- **Archivos:** Nueva `ui/widget/WidgetContainerActivity.kt` o integración en `HomeActivity`, `WidgetPreferences.kt`, `AndroidManifest.xml`.
- **Tests:** Mínimos; dependen demasiado del sistema.
- **Tiempo:** 5–7 días.

### 4.2 Panel de notificaciones accesible por gesture

- **Problema:** No hay acceso programático al shade de notificaciones.
- **Solución:**
  - Swipe down en el área superior del home para expandir notificaciones vía `StatusBarManager.expandNotificationsPanel()` (requiere permiso `EXPAND_STATUS_BAR`, disponible desde API 16, o reflection como fallback).
  - O más simple: detectar swipe down en `HomeActivity` y usar `WindowInsetsControllerCompat.show(Type.statusBars())` seguido de un intent implícito si es posible.
  - Alternativa robusta: un gesto configurable (swipe down en cualquier parte vacía del home) que simule el pulso en la status bar.
- **Archivos:** `HomeActivity.kt` (gesture detector).
- **Tiempo:** 1–2 días.

---

## Orden de Ejecución Recomendado

| Orden | Feature | Fase | Tiempo | Bloquea a... |
|-------|---------|------|--------|-------------|
| 1 | 1.1 Corrección font scale | Fase 1 | 4–6h | — |
| 2 | 1.4 Manejo de errores | Fase 1 | 3–4h | 2.1 (tests con logging) |
| 3 | 1.2 Content descriptions | Fase 1 | 3–4h | — |
| 4 | 1.3 Relanzar onboarding | Fase 1 | 2–3h | — |
| 5 | 1.5 Fuzzy search | Fase 1 | 6–8h | — |
| 6 | 2.1 Tests unitarios | Fase 2 | 2 días | 2.2, 2.3 |
| 7 | 2.3 Work keywords configurable | Fase 2 | 2–3 días | — |
| 8 | 2.4 Auto day/night | Fase 2 | 4–6h | — |
| 9 | 2.2 Tests instrumentados | Fase 2 | 2–3 días | — |
| 10 | 3.3 Gestos navegación | Fase 3 | 2–3h | — |
| 11 | 3.1 App Shortcuts | Fase 3 | 2–3 días | — |
| 12 | 3.2 Badges notificación | Fase 3 | 3–4 días | — |
| 13 | 4.2 Panel notificaciones gesture | Fase 4 | 1–2 días | — |
| 14 | 4.1 Widgets sistema | Fase 4 | 5–7 días | — |

**Tiempo total estimado:** ~20–28 días de trabajo efectivo (enfoque profesional, code review propio, tests incluidos).

---

## Notas sobre Vulnerabilidades y Seguridad

- **Nunca** modificar `ProfilePreferences.kt` sin mantener la encriptación AES-256/GCM vía `SecureStorageHelper`.
- **Nunca** almacenar contraseñas de detox en texto plano. El hash PBKDF2 actual es correcto y no debe tocarse.
- **Nunca** añadir permisos de red sin justificación. FocusPhone es offline-first.
- **Nunca** activar `android:allowBackup="true"`. Debe permanecer `false`.
- El `FocusAccessibilityService` actual es la línea de defensa más importante. Cualquier refactor debe preservar la lógica de `TYPE_WINDOW_STATE_CHANGED` y el lanzamiento de `DetoxBlockedActivity`.

---

## Checklist de Entrega por Fase

Antes de declarar una fase terminada:

- [ ] `./gradlew :app:assembleDebug` compila sin warnings críticos.
- [ ] `./gradlew :app:testDebugUnitTest` pasa al 100%.
- [ ] Se ha probado rotación de pantalla en las Activities modificadas.
- [ ] Se ha probado el cambio de tema en caliente (light/dark/custom).
- [ ] Se ha verificado que el bloqueo Detox/Work sigue funcionando (con y sin Accessibility Service activo).
- [ ] Los strings nuevos están en `values/strings.xml` (inglés) y marcados como `translatable` donde aplique.
