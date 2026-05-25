### Seguridad (Alta prioridad)
1. **Migrar a `EncryptedSharedPreferences`** para `focusphone_profile` o, como mínimo, añadir **salt aleatorio** al hash de la contraseña Detox (PBKDF2/Argon2id).
2. **Persistir el contador de intentos fallidos** en `SharedPreferences` con ventana temporal, para evitar reinicio ilimitado cerrando el diálogo.
3. **Añadir `FLAG_SECURE`** a `DetoxBlockedActivity` y a los diálogos de contraseña Detox.
4. **Implementar un `AccessibilityService`** o `ForegroundService` para interceptar lanzamientos de apps a nivel de sistema y evitar bypass por notificaciones/recientes.
5. **Eliminar/condicionar logs** de `packageName` con `if (BuildConfig.DEBUG)`.
6. **Añadir reglas `-keep`** en ProGuard para clases de seguridad (`ProfilePreferences`).
---

### Funcionalidades (Media/Alta prioridad)
1. **Soporte para widgets del sistema** (`AppWidgetHost` + `AppWidgetManager`).
2. **App Shortcuts** (`ShortcutManager`) al hacer long-press en apps favoritas.
3. **Badges de notificación** sobre los textos de las apps favoritas.
4. **Modo automático día/noche** basado en el tema del sistema.
5. **Respetar `fontScale` del sistema** en lugar de sobrescribirlo en `BaseActivity`.
6. **Completar traducciones** `fr` y `de`.
7. **Añadir tests unitarios** para Use Cases de bloqueo, detox y reordenamiento.
---