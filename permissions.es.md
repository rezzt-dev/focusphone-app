<div align="center">

# PERMISOS Y COMPONENTES EXPORTADOS

<sub><a href="./permissions.md">english</a> · castellano · <a href="./privacy.es.md">privacidad</a> · <a href="./security.es.md">seguridad</a> · <a href="./readme.es.md">readme</a></sub>

</div>

---

justificación técnica de cada permiso y componente exportado de
`focusphone-project/app/src/main/AndroidManifest.xml`, contrastada con el manifest
combinado de la compilación de publicación `1.4.0-beta` (`aapt2 dump permissions`). está
pensada para la revisión de tiendas (google play) y para auditorías de seguridad; el resumen
para usuarios está en [`privacy.es.md`](./privacy.es.md) y en
[`11-privacidad-y-permisos.md`](./documentation/es/usuarios/11-privacidad-y-permisos.md).

**la app no solicita `INTERNET` ni ningún otro permiso de red**, ni ningún permiso en
tiempo de ejecución ("peligroso"): no aparece ningún diálogo de permisos al abrirla por
primera vez.

---

## 1. PERMISOS DECLARADOS

| permiso | nivel de protección | lo usa | por qué es necesario | sin él |
|---|---|---|---|---|
| `android.permission.QUERY_ALL_PACKAGES` | normal (se concede al instalar) | `AppRepositoryImpl` y `WidgetLaunchActivity`, mediante `PackageManager.queryIntentActivities` para `ACTION_MAIN` / `CATEGORY_LAUNCHER` | un launcher debe listar todas las apps que se pueden abrir; desde android 11 (api 30) la visibilidad de paquetes las oculta si no | el cajón, el selector de favoritas y el selector de widgets solo verían unos pocos paquetes del sistema |
| `android.permission.REQUEST_DELETE_PACKAGES` | normal (se concede al instalar) | `LauncherUtils.requestUninstall`, que lanza `Intent.ACTION_DELETE` | las apps con target api 28+ lo necesitan para abrir el diálogo de desinstalación de android; android siempre pide confirmación al usuario y focusphone nunca desinstala nada por sí misma | la opción "desinstalar" de una app no podría abrir el diálogo del sistema |
| `android.permission.EXPAND_STATUS_BAR` | normal (se concede al instalar) | `HomeActivity.expandNotificationsPanel`, solo como alternativa | el gesto de deslizar hacia abajo usa preferentemente `AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS`; si el servicio de accesibilidad está desactivado recurre a `StatusBarManager.expandNotificationsPanel`, que necesita este permiso y solo funciona en versiones antiguas de android (la llamada está bloqueada desde la api 31) | el gesto no haría nada con el servicio de accesibilidad desactivado |
| `app.focusphone.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | signature | lo añade `androidx.core` al manifest combinado | permite que `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)` mantenga privados los receptores registrados en tiempo de ejecución en versiones de android anteriores a la 13 | no aplica: solo lo define y lo tiene la propia focusphone |

---

## 2. SERVICIOS OPCIONALES QUE ACTIVA EL USUARIO

no son permisos que tenga focusphone: android enlaza el servicio, y solo después de que el
usuario lo active en los ajustes de android. ambos pueden desactivarse en cualquier momento.

### 2.1 SERVICIO DE ACCESIBILIDAD — `FocusAccessibilityService`

| | |
|---|---|
| protegido por | `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"` (solo el sistema puede enlazarlo) |
| configuración | `res/xml/accessibility_service_config.xml`: `accessibilityEventTypes="typeWindowStateChanged\|typeWindowsChanged"`, `canRetrieveWindowContent="true"`, `accessibilityFlags="flagRetrieveInteractiveWindows"` |
| qué lee | el nombre de paquete de la ventana activa (`AccessibilityWindowInfo.root.packageName`) y, si no lo hay, el del evento. `canRetrieveWindowContent` y `flagRetrieveInteractiveWindows` son necesarios para obtener esa lista de ventanas; no se lee el texto de ningún nodo |
| qué hace | muestra `DetoxBlockedActivity` / `ScheduleBlockedActivity` sobre una app bloqueada abierta fuera de focusphone (notificaciones, recientes, accesos sugeridos, asistentes); acumula los segundos en primer plano de cada app para los límites diarios, guardados cifrados por `AppSchedulePreferences`; ejecuta `GLOBAL_ACTION_NOTIFICATIONS` para el gesto de deslizar hacia abajo |
| qué no hace nunca | leer ni guardar el texto de la pantalla, hacer capturas, pulsar o hacer gestos en otras apps, escribir ni enviar datos a ningún sitio (la app no tiene acceso a la red) |
| cuándo se pide | al iniciar una sesión de detox, desde ajustes → sistema y desde la pantalla de horarios; nunca se activa automáticamente |
| sin él | los bloqueos de perfil y los horarios solo se aplican a las apps abiertas desde la propia focusphone (inicio, cajón, widgets); los límites diarios no se miden; el gesto de deslizar hacia abajo recurre a `EXPAND_STATUS_BAR` |

### 2.2 OYENTE DE NOTIFICACIONES — `FocusNotificationService`

| | |
|---|---|
| protegido por | `android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"` |
| qué lee | las notificaciones activas, para listarlas en el panel lateral; las sesiones multimedia activas (`MediaSessionManager.getActiveSessions`, que exige un oyente activado) para el título, el artista, la carátula y el estado de reproducción de los widgets de música |
| almacenamiento | el contenido de las notificaciones solo se guarda en memoria y se borra en `onListenerDisconnected` / `onDestroy`; los metadatos multimedia se leen cuando hacen falta y no se guardan |
| cuándo se pide | desde ajustes → notificaciones, cuando el usuario activa el panel de notificaciones; nunca se activa automáticamente |
| sin él | no hay panel de notificaciones; los widgets de música muestran un estado "sin acceso" |

---

## 3. COMPONENTES EXPORTADOS

| componente | exportado porque | protección |
|---|---|---|
| `ui.home.HomeActivity` | es el punto de entrada `MAIN` / `HOME` / `LAUNCHER` | `singleTask`, `stateNotNeeded`; el único extra que lee de un intent es el booleano `EXTRA_EDIT_LAYOUT`, que abre el modo edición y no concede nada |
| 20 `AppWidgetProvider` en `ui.widget.library` | android exige que los proveedores estén exportados para que cualquier launcher pueda alojarlos (`APPWIDGET_UPDATE`) | los proveedores no tienen lógica; una actualización solo vuelve a dibujar a partir del estado de focusphone, y cada toque pasa por `WidgetLaunchActivity` (no exportada), que aplica perfil, horario y pausa consciente |
| `ui.widget.library.WidgetSystemReceiver` | para recibir difusiones del sistema que cambian lo que muestran los widgets | solo escucha difusiones protegidas del sistema (`TIME_SET`, `TIMEZONE_CHANGED`, `DATE_CHANGED`, `LOCALE_CHANGED`, `NEXT_ALARM_CLOCK_CHANGED`, `MY_PACKAGE_REPLACED`) y solo provoca un redibujado |
| `service.FocusAccessibilityService` | android lo exige a los servicios de accesibilidad | `BIND_ACCESSIBILITY_SERVICE` |
| `service.FocusNotificationService` | android lo exige a los oyentes de notificaciones | `BIND_NOTIFICATION_LISTENER_SERVICE` |

el resto de actividades y receptores, incluidos `WidgetActionReceiver`,
`WidgetLaunchActivity`, `WorldClockConfigActivity` e `IntentionConfigActivity`, son
`android:exported="false"`.

---

## 4. COPIAS DE SEGURIDAD Y EXTRACCIÓN DE DATOS

`android:allowBackup="false"` junto con `android:dataExtractionRules="@xml/data_extraction_rules"`,
que excluye los dominios `root`, `file`, `database`, `sharedpref` y `external` tanto de
`cloud-backup` como de `device-transfer`. consulta
[`privacy.es.md`](./privacy.es.md#6-copias-de-seguridad-y-cambio-de-dispositivo).

---

## 5. NOTAS PARA GOOGLE PLAY

focusphone se distribuye como apk en las releases de github y no está en google play. si se
publicara allí, harían falta estas declaraciones:

| elemento | política de play | estado |
|---|---|---|
| `QUERY_ALL_PACKAGES` | política de visibilidad de paquetes (apps): la visibilidad amplia debe declararse; los launchers son un caso de uso aceptado | formulario de declaración pendiente en play console |
| servicio de accesibilidad | política de la api de accesibilidad: una app que no es una herramienta de accesibilidad debe mostrar una divulgación destacada en la app y obtener un consentimiento afirmativo antes de activarlo, y declarar el uso en play console | **TODO(owner)**: el texto actual de la app (`detox_accessibility_desc`, `schedule_accessibility_required_desc`) es una explicación breve, no una divulgación destacada completa con consentimiento explícito |
| formulario de seguridad de los datos | declarar "no se recogen datos, no se comparten datos" | coherente con [`privacy.es.md`](./privacy.es.md) |
| url de la política de privacidad | obligatoria para permisos sensibles | **TODO(owner)**: publicar una url estable de [`privacy.es.md`](./privacy.es.md) |

---

<div align="center">

<sub>focusphone — desarrollado por <a href="https://rezzt.dev"><b>rezzt.dev</b></a></sub>

</div>
