# 🔍 Análisis Profundo — FocusPhone v1.0.4

> **Fecha de análisis:** 2026-05-24  
> **Versión analizada:** 1.0.4 (`alfa-1.0.4`)  
> **Scope:** Ciberseguridad + Funcionalidades necesarias para un launcher Android

---

# 📋 PARTE 1: ANÁLISIS DE CIBERSEGURIDAD

## 1.1 Superficie de Ataque

### Permisos y Manifest

| Aspecto | Estado | Detalle |
|---------|--------|---------|
| Permisos peligrosos | ⚠️ **Justificado** | Solo `QUERY_ALL_PACKAGES`. Esencial para un launcher, pero requiere formulario de declaración en Google Play. |
| Componentes exportados | ✅ **Correcto** | Solo `HomeActivity` es `exported="true"` (necesario para ser launcher). Todas las demás Activities son `exported="false"`. |
| Deep Links / App Links | ✅ **Seguro** | No hay intent-filters con `BROWSABLE` ni schemes custom. |
| WebView | ✅ **Inexistente** | No se usa `WebView` en ninguna pantalla. |
| ContentProviders propios | ✅ **Inexistente** | No hay providers propios expuestos. |

### Intents y Comunicaciones

| Hallazgo | Severidad | Archivo / Línea | Detalle |
|----------|-----------|-----------------|---------|
| `file:///android_asset/privacy_policy.html` | 🔶 **Medio** | `SettingsActivity.kt:55` | URI `file://` obsoleto e inseguro. En Android N+ lanza `FileUriExposedException`. Además, cualquier app podría intentar interceptar el intent `text/html`+`file` (aunque fallaría por falta de acceso). |
| Intents de sistema | 🟢 **Bajo** | Varios | `ACTION_UNINSTALL_PACKAGE`, `ACTION_DELETE`, `ACTION_APPLICATION_DETAILS_SETTINGS`, `AlarmClock`, `HOME_SETTINGS` — todos seguros y con datos controlados internamente. |
| `ConsciousPauseActivity` recibe `Intent` parcelable | 🟢 **Bajo** | `HomeActivity.kt:770-773` | Aunque pasar `Intent` dentro de `Intent` es históricamente sensible (StrandHogg), la activity tiene `exported="false"` y el intent interno se obtiene de `packageManager.getLaunchIntentForPackage()`. |

### BroadcastReceivers

| Hallazgo | Estado | Detalle |
|----------|--------|---------|
| `clockReceiver` dinámico | 🟢 **Bajo** | Escucha `ACTION_TIME_TICK` (sistema, inofensivo). Registrado con `RECEIVER_EXPORTED` pero solo procesa `updateClock()`. No recibe datos externos. |
| Receivers estáticos propios | ✅ **Ninguno** | El único receiver en merged manifest es `androidx.profileinstaller.ProfileInstallReceiver` (librería estándar, protegido por permiso `DUMP`). |

---

## 1.2 Almacenamiento y Protección de Datos

### SharedPreferences

| Hallazgo | Severidad | Detalle |
|----------|-----------|---------|
| Modo de acceso | ✅ **Seguro** | Todo usa `MODE_PRIVATE`. No hay `MODE_WORLD_READABLE/WRITEABLE`. |
| Encriptación | 🔴 **Alto** | **Ningún** archivo de preferencias usa `EncryptedSharedPreferences` de AndroidX Security. |
| Contraseña Detox | 🔴 **Alto** | Almacenada como hash SHA-256 **sin salt** en `focusphone_profile`. Vulnerable a rainbow tables y precomputación. |
| Apps bloqueadas | 🔶 **Medio** | Guardadas en `focusphone_profile` como `Set<String>` de package names. Sin encriptar. |
| Custom labels | 🔶 **Medio** | `focusphone_custom_labels` usa `packageName` como clave directa. Sin validación de formato ni encriptación. |

### Room Database

| Aspecto | Estado | Detalle |
|---------|--------|---------|
| SQL Injection | ✅ **Seguro** | Room usa parámetros posicionales (`:packageName`). No hay concatenación de strings SQL. |
| Main thread | ✅ **Seguro** | No se detecta `allowMainThreadQueries()`. |
| Migrations | ⚠️ **Condicional** | `fallbackToDestructiveMigration(true)` solo en `DEBUG`. En release, un bump de versión sin migration provocará crash (`IllegalStateException`). |
| Encriptación | N/A | La DB solo contiene `packageName`, `label`, `sortOrder`. No requiere cifrado para su threat model, pero podría añadirse. |

### Backup / Restore

| Aspecto | Severidad | Detalle |
|---------|-----------|---------|
| `allowBackup="true"` | 🔶 **Medio** | Habilitado globalmente. Permite `adb backup` y cloud backup. |
| Exclusiones | ⚠️ **Parcial** | `data_extraction_rules.xml` y `backup_rules.xml` excluyen correctamente `focusphone_profile` (contraseña). |
| Metadatos expuestos | 🔶 **Medio** | El resto de prefs (`focusphone_theme`, `focusphone_custom_labels`, `focusphone_conscious_pause`, etc.) **sí se backupean**. Filtran hábitos digitales, temas, apps favoritas y configuraciones. |

---

## 1.3 Autenticación y Control de Acceso (Modo Detox)

| Hallazgo | Severidad | Archivo / Línea | Detalle |
|----------|-----------|-----------------|---------|
| Hashing sin salt | 🔴 **Alto** | `ProfilePreferences.kt:53-56` | SHA-256 directo. Contraseñas como "1234" o "aaaa" se rompen instantáneamente con tablas precomputadas. |
| Contador de intentos reiniciable | 🔶 **Medio** | `HomeActivity.kt:566-589` | `incorrectAttempts` es una **variable local del diálogo**. Si el usuario cierra y reabre el diálogo, el contador vuelve a 0. Se pueden hacer intentos ilimitados. |
| Complejidad de contraseña | 🔶 **Medio** | `HomeActivity.kt:680` | Solo se valida `length >= 4`. Acepta "aaaa", "1234", "0000". |
| Ausencia de `FLAG_SECURE` | 🔴 **Alto** | Varios | Ninguna Activity usa `WindowManager.LayoutParams.FLAG_SECURE`. Un atacante con acceso físico puede hacer **screenshot o screen recording** mientras el usuario introduce la contraseña Detox. |
| Detox bypass por fuerza cierre | 🔴 **Alto** | `HomeActivity.kt`, `FocusPhoneApp.kt` | No hay un servicio en background ni un Accessibility Service que intercepte lanzamientos. Si se mata el proceso de FocusPhone, el usuario puede abrir cualquier app desde notificaciones, recientes o asistente de voz sin pasar por el bloqueo. |

---

## 1.4 Logging y Fuga de Información

| Hallazgo | Severidad | Archivo / Línea | Detalle |
|----------|-----------|-----------------|---------|
| Log de package names | 🔶 **Medio** | `HomeActivity.kt:794`, `AllAppsActivity.kt:231` | `Log.d("FocusPhone", "uninstall clicked for ${app.packageName}")`. En release, filtra metadatos de apps instaladas a cualquier app con `READ_LOGS` (versiones antiguas) o durante debugging. |
| Logs de credenciales | ✅ **Seguro** | — | No se detecta logging de contraseñas, hashes ni tokens. |
| Stripping de logs en release | ❌ **Ausente** | `proguard-rules.pro` | No hay regla `-assumenosideeffects class android.util.Log { *; }`. Los logs permanecen en release. |

---

## 1.5 Ofuscación (ProGuard / R8)

| Hallazgo | Severidad | Detalle |
|----------|-----------|---------|
| Modelos Room | ✅ **Protegido** | `-keep class app.focusphone.data.model.** { *; }` |
| ViewBinding | ✅ **Protegido** | `-keep class * implements androidx.viewbinding.ViewBinding { *; }` |
| Widgets | ✅ **Protegido** | `-keep class app.focusphone.ui.widget.** { *; }` |
| Clases de seguridad | ❌ **No protegido** | `ProfilePreferences` (maneja hashes, perfiles, bloqueos) **no tiene regla `-keep`**. Ofuscar código de seguridad dificulta auditorías y puede romper futura integración por reflection. |

---

## 1.6 Validación de Entradas

| Hallazgo | Severidad | Archivo / Línea | Detalle |
|----------|-----------|-----------------|---------|
| Custom labels sin límite | 🟡 **Bajo** | `AllAppsActivity.kt:180`, `AppSelectActivity.kt:87` | `normalizeAppName()` no limita longitud. Un paste masivo puede saturar UI o memoria. |
| Colores hex incompletos | 🟡 **Bajo** | `SettingsActivity.kt:304-313` | Solo valida `length == 7 && startsWith("#")`. `#GGGGGG` pasaría la validación y fallaría en `Color.parseColor()`. |
| Duración Detox sin límite | 🔶 **Medio** | `HomeActivity.kt:649-652` | `toLongOrNull() ?: 0L` sin límite superior. Un usuario puede introducir 999,999 horas. |
| Migración legacy sin validación | 🟡 **Bajo** | `FavouriteAppRepositoryImpl.kt:26-48` | Lee string pipe-delimited sin validar columnas. Datos corruptos podrían generar entidades parciales. |

---

## 1.7 Resumen de Vulnerabilidades

| Severidad | Cantidad | Hallazgos principales |
|-----------|----------|-----------------------|
| 🔴 **Crítico / Alto** | 4 | SHA-256 sin salt; Sin `EncryptedSharedPreferences`; Sin `FLAG_SECURE` en pantallas de contraseña; Bypass de Detox por cierre del proceso |
| 🔶 **Medio** | 9 | Contador de intentos reiniciable; `allowBackup="true"` filtrando metadatos; Log de package names; Inputs sin límites; Custom labels sin validación de claves; `file://` URI inseguro; Duración detox ilimitada; ProGuard no protege clases de seguridad; `QUERY_ALL_PACKAGES` riesgo Play Store |
| 🟡 **Bajo / Leve** | 5 | Validación hex incompleta; Longitud de labels sin límite; Migración legacy sin validación; Logs no eliminados en release; Toast con datos de app |

---

# 📋 PARTE 2: ANÁLISIS DE FUNCIONALIDADES NECESARIAS

## 2.1 Core del Launcher

| Funcionalidad | Estado | Detalle |
|---------------|--------|---------|
| **Pantalla principal (Home)** | ✅ **Completo** | Reloj (5 tipos: Digital, Stacked, Words, Analog, Hidden), calendario manual, lista text-only de favoritas (máx. 10), empty state, drag & drop para reordenar, edge-to-edge. |
| **Cajón de apps (AllApps)** | ✅ **Funcional** | Lista completa excluyendo FocusPhone, orden alfabético, búsqueda por substring (debounce 150ms). |
| **Selector de favoritas (AppSelect)** | ✅ **Completo** | Toggle inmediato, límite de 10 apps bien gestionado con Toast, renombrado por long-press, contador visual. |
| **Widgets del sistema** | ❌ **No soportado** | No hay `AppWidgetHost`, `AppWidgetManager`, ni contenedores de widgets. Es una decisión de diseño minimalista, pero limita la utilidad como launcher de reemplazo. |
| **Establecer como default** | ⚠️ **Parcial** | El manifest tiene `HOME`/`DEFAULT`/`LAUNCHER` con `priority="1"`. La app detecta si es default (`isDefaultLauncher()`), pero solo redirige a `Settings.ACTION_HOME_SETTINGS`. No fuerza ni guía al usuario paso a paso. |

---

## 2.2 Perfiles y Bloqueo

| Funcionalidad | Estado | Detalle |
|---------------|--------|---------|
| **Perfil Normal** | ✅ **Funciona** | Sin bloqueo. |
| **Perfil Work** | ⚠️ **Parcial** | Bloquea apps marcadas manualmente + una lista **hardcodeada** de keywords (`instagram`, `facebook`, `youtube`, `tiktok`, etc.). **No es configurable** por el usuario. |
| **Perfil Detox** | ⚠️ **Parcial** | Bloquea todas las apps excepto FocusPhone. Tiempo configurable (15m, 30m, 1h, 2h, custom), countdown en vivo, auto-revert a Normal. Password con SHA-256. |
| **Bloqueo real vs visual** | ❌ **Visual únicamente** | El bloqueo solo intercepta toques **dentro de FocusPhone** (`HomeActivity` y `AllAppsActivity`). Si una app se lanza desde notificaciones, multitasking (recientes), asistente de voz o deep links de otra app, **el bloqueo NO aplica**. |
| **Protección ante cierre forzado** | ❌ **Inexistente** | No hay un `Service` en foreground ni un `AccessibilityService` que intercepte lanzamientos a nivel del sistema. Matar el proceso de FocusPhone anula completamente el bloqueo. |
| **Conscious Pause** | ✅ **Completo** | Pantalla de respiración con animación, countdown 3-10s configurable, cancelable, lanza app destino al finalizar. Guarda `remainingSeconds` en `onSaveInstanceState`. |

---

## 2.3 UX y Personalización

| Funcionalidad | Estado | Detalle |
|---------------|--------|---------|
| **Temas** | ✅ **90%** | 4 temas built-in + custom hex. Aplicación centralizada en `BaseActivity`. Falta modo automático día/noche. |
| **Fuentes** | ✅ **100%** | 7 fuentes (5 descargables de Google Fonts vía Play Services). Fallback a `sans-serif` si falla la descarga. Pre-carga en `preloaded_fonts.xml`. |
| **Ajustes (Settings)** | ✅ **95%** | Tema, reloj, tamaño de texto, fuente, idioma, alineación, barra de estado, pausa consciente, tutorial, acerca de. Muy completo. |
| **Onboarding** | ✅ **90%** | 3 pasos (Welcome, SetLauncher, PickApps). Se puede saltar. No hay forma de relanzarlo desde Settings (solo existe tutorial separado). |
| **Widgets propios** | ✅ **100%** | `AnalogClockView` (Canvas) y `WeightGradientTextView`. |
| **Notificaciones / Badges** | ❌ **0%** | Sin `NotificationManager`, sin canales, sin badges, sin acceso programático al shade de notificaciones. |
| **App Shortcuts** | ❌ **0%** | Sin `ShortcutManager`. El long-press en favoritas muestra un menú propio (open, remove, info, uninstall) pero no los shortcuts dinámicos/pinned del sistema. |
| **Gestos y navegación** | ⚠️ **60%** | Drawer desde derecha con `DrawerLayout`. Swipe left detectado con `GestureDetector`. **Posible conflicto** con back gesture del sistema en Android 10+ (no hay `setSystemGestureExclusionRects`). |

---

## 2.4 Accesibilidad e Internacionalización

| Funcionalidad | Estado | Detalle |
|---------------|--------|---------|
| **Content Descriptions** | ⚠️ **Parcial** | Existen en algunos elementos (back button, reloj analógico, selectores), pero faltan en: items de apps en listas, días del calendario, botón hamburguesa, indicadores de página. |
| **Tamaño de fuente del sistema** | ❌ **Roto** | `BaseActivity.attachBaseContext()` fuerza `config.fontScale = FontPreferences.getFontScale(newBase)`, **sobrescribiendo** la escala de accesibilidad del sistema. Un usuario con font scale 1.3 verá 1.0 a menos que ajuste manualmente el slider de la app. |
| **Idiomas** | ⚠️ **Parcial** | Base `en-US` completo (196 strings). `es` casi completo (168). `fr` y `de` muy incompletos (~122 strings, faltan ~74). `en-GB` solo 7 strings (3 overrides de spelling). |

---

## 2.5 Funcionalidades Ausentes Críticas para un Launcher

| Funcionalidad | Impacto | Justificación |
|---------------|---------|---------------|
| **Servicio de bloqueo en background** | 🔴 **Alto** | Sin un `AccessibilityService` o un `ForegroundService`, el bloqueo de Detox/Work se evita simplemente matando la app o usando notificaciones. Para un launcher de "reducción de distracción", esto es una falla funcional crítica. |
| **Widgets del sistema (AppWidgets)** | 🔶 **Medio** | La mayoría de usuarios espera poder añadir al menos un widget del calendario o clima a su home. La ausencia es una barrera de adopción. |
| **App Shortcuts** | 🔶 **Medio** | Android 7.1+ permite shortcuts al hacer long-press en apps. No mostrarlos rompe la paridad con launchers nativos. |
| **Notificaciones / Badges** | 🔶 **Medio** | Un launcher minimalista podría al menos mostrar badges de notificación en los textos de las apps favoritas o permitir acceso al panel de notificaciones con un gesture. |
| **Modo automático día/noche** | 🟡 **Bajo** | Con 4 temas explícitos, el usuario debe cambiar manualmente. Un launcher moderno debería adaptarse al tema del sistema. |
| **Búsqueda difusa (fuzzy)** | 🟡 **Bajo** | La búsqueda actual es substring exacto. Una búsqueda aproximada mejoraría la UX significativamente. |
| **Re-lanzar onboarding** | 🟡 **Bajo** | No hay opción en Settings para repetir el onboarding original. |

---

## 2.6 Arquitectura y Estabilidad

| Aspecto | Estado | Detalle |
|---------|--------|---------|
| **MVVM + Clean Architecture** | ✅ **Correcto** | ViewModels con `StateFlow`/`SharedFlow`, Use Cases, Repository pattern. Manual DI vía `AppContainer`. |
| **Lifecycle** | ✅ **Correcto** | Uso apropiado de `lifecycleScope` + `repeatOnLifecycle(Lifecycle.State.STARTED)`. |
| **Manejo de errores** | ⚠️ **Mejorable** | Muchos `catch (_: Exception)` y `catch (e: Exception) { /* toast genérico */ }` que silencian errores. Ej: `AppSelectViewModel.kt:45` usa `catch (_: Exception) { emptyList() }` para `getInstalledApps()`. |
| **Config changes (rotación)** | ⚠️ **Parcial** | `ConsciousPauseActivity` guarda `remainingSeconds` en `onSaveInstanceState`, pero el timer no se reanuda visualmente donde quedó. Otras activities no se han verificado explícitamente para rotación. |
| **Edge-to-edge** | ✅ **Completo** | Todas las Activities extienden `BaseActivity` que llama `enableEdgeToEdge()` y aplican insets manualmente. |
| **Tests** | ❌ **Inexistentes** | Solo hay placeholders. Lógica crítica (bloqueo, detox, reordenamiento, migración) carece de cobertura. |

---

# 📊 Conclusión Ejecutiva

## Ciberseguridad
La app tiene una **superficie de ataque reducida** (sin WebViews, sin providers expuestos, sin receivers peligrosos), pero presenta **fallas graves en la protección de la contraseña Detox**: hashing SHA-256 sin salt, contador de intentos reiniciable por UI, ausencia de `FLAG_SECURE` en pantallas sensibles, y un bypass trivial del bloqueo al cerrar el proceso. Además, los metadatos de uso del usuario se filtran vía backup y logs.

## Funcionalidades
FocusPhone es un **MVP sólido y bien diseñado** para su propósito minimalista. Sin embargo, como **launcher de reemplazo completo**, le faltan funcionalidades críticas: un mecanismo de bloqueo real a nivel de sistema (Accessibility Service), soporte para widgets del sistema, App Shortcuts, notificaciones/badges, y mejoras de accesibilidad (respetar font scale del sistema). La búsqueda es básica y las traducciones a fr/de están significativamente incompletas.

---

## 🛠️ Prioridad de Correcciones Recomendadas

### Seguridad (Alta prioridad)
1. **Migrar a `EncryptedSharedPreferences`** para `focusphone_profile` o, como mínimo, añadir **salt aleatorio** al hash de la contraseña Detox (PBKDF2/Argon2id).
2. **Persistir el contador de intentos fallidos** en `SharedPreferences` con ventana temporal, para evitar reinicio ilimitado cerrando el diálogo.
3. **Añadir `FLAG_SECURE`** a `DetoxBlockedActivity` y a los diálogos de contraseña Detox.
4. **Implementar un `AccessibilityService`** o `ForegroundService` para interceptar lanzamientos de apps a nivel de sistema y evitar bypass por notificaciones/recientes.
5. **Eliminar/condicionar logs** de `packageName` con `if (BuildConfig.DEBUG)`.
6. **Añadir reglas `-keep`** en ProGuard para clases de seguridad (`ProfilePreferences`).

### Funcionalidades (Media/Alta prioridad)
1. **Soporte para widgets del sistema** (`AppWidgetHost` + `AppWidgetManager`).
2. **App Shortcuts** (`ShortcutManager`) al hacer long-press en apps favoritas.
3. **Badges de notificación** sobre los textos de las apps favoritas.
4. **Modo automático día/noche** basado en el tema del sistema.
5. **Respetar `fontScale` del sistema** en lugar de sobrescribirlo en `BaseActivity`.
6. **Completar traducciones** `fr` y `de`.
7. **Añadir tests unitarios** para Use Cases de bloqueo, detox y reordenamiento.
