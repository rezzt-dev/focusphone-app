# FocusPhone — Información General del Proyecto

> **FocusPhone** es un launcher Android minimalista diseñado para reducir la distracción digital. Reemplaza la pantalla de inicio del dispositivo con una interfaz limpia y solo texto que muestra un reloj, un calendario y hasta 10 aplicaciones favoritas. Soporta perfiles contextuales (Normal, Work, Detox), un temporizador de "Pausa Consciente" antes de abrir apps, y tematización extensa.

---

## Información General

| Propiedad | Valor |
|-----------|-------|
| **Paquete** | `app.focusphone` |
| **Versión** | 1.0.4 |
| **Lenguaje** | Kotlin |
| **Min SDK** | 24 |
| **Target / Compile SDK** | 36 |
| **Compatibilidad Java** | 11 |
| **AGP** | 9.2.1 |
| **JDK Requerido** | 21 |

El proyecto es una aplicación Android de un solo módulo (`:app`) sin módulos de librería.

---

## Stack Tecnológico

### Core
- **Kotlin** con Coroutines (`kotlinx-coroutines-android`)
- **AndroidX**: Core KTX, AppCompat, Activity, Fragment, ConstraintLayout, RecyclerView, ViewPager2
- **Lifecycle**: ViewModel KTX, LiveData KTX, Runtime KTX
- **Material 3** (`com.google.android.material:material`)

### Almacenamiento de Datos
- **Room 2.8.4** — almacena las apps favoritas del usuario (tabla `favourite_apps`).
- **SharedPreferences** — almacena todas las configuraciones del usuario (tema, fuente, perfiles, temporizador detox, pausa consciente, etiquetas personalizadas). El acceso síncrono es intencional para que los temas se apliquen antes de `setContentView`.

### Arquitectura
- **Inyección de Dependencias Manual** vía `AppContainer` en `FocusPhoneApp`. Hilt se evita intencionalmente en "Fase 1" debido a problemas de compatibilidad de AGP 9.x con el procesamiento de anotaciones.
- **MVVM** con `ViewModel` + `StateFlow` / `SharedFlow`.
- **Repository Pattern** + **Use Cases** (Clean Architecture lite).
- **View Binding** habilitado para todas las pantallas.

---

## Estructura del Proyecto

```
app/src/main/java/app/focusphone/
├── FocusPhoneApp.kt              # Clase Application; contiene AppContainer
├── di/
│   └── AppContainer.kt           # Grafo de DI manual (singletons, repos, use cases)
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt        # Singleton de la base de datos Room
│   │   └── FavouriteAppDao.kt    # DAO de Room
│   ├── model/
│   │   └── FavouriteApp.kt       # Entidad de Room
│   ├── preferences/
│   │   ├── ThemePreferences.kt   # Tema, alineación, onboarding, barra de estado
│   │   ├── FontPreferences.kt    # Familia de fuente, tipo de reloj, tamaño de reloj, tamaño de texto
│   │   ├── ProfilePreferences.kt # Perfiles Normal / Work / Detox, temporizador y contraseña detox, apps bloqueadas
│   │   ├── ConsciousPausePreferences.kt # Habilitar/deshabilitar + duración (segundos)
│   │   └── CustomAppLabelsPreferences.kt # Etiquetas de apps renombradas por el usuario
│   └── repository/
│       ├── AppRepositoryImpl.kt       # Consulta apps instaladas del launcher
│       └── FavouriteAppRepositoryImpl.kt # Room + merge de etiquetas personalizadas; también maneja la migración SharedPrefs→Room
├── domain/
│   ├── model/
│   │   ├── AppInfo.kt            # Modelo de dominio de app instalada
│   │   ├── ClockType.kt          # DIGITAL | STACKED | WORDS | ANALOG | HIDDEN
│   │   ├── AppFont.kt            # Enum de fuentes (sistema + descargables)
│   │   └── ToggleResult.kt       # Added | Removed | LimitReached
│   ├── repository/
│   │   ├── AppRepository.kt
│   │   └── FavouriteAppRepository.kt
│   └── usecase/
│       ├── GetInstalledAppsUseCase.kt
│       ├── GetFavouriteAppsUseCase.kt
│       ├── ToggleFavouriteAppUseCase.kt   # Fuerza MAX_FAVOURITES = 10
│       └── UpdateAppsOrderUseCase.kt
└── ui/
    ├── base/
    │   └── BaseActivity.kt       # Aplicación de tema, edge-to-edge, escala de fuente, transiciones fade
    ├── home/
    │   ├── HomeActivity.kt       # Actividad del launcher; reloj, calendario, drawer, perfiles, reordenamiento
    │   ├── HomeViewModel.kt
    │   └── adapter/
    │       └── FavouriteAppsAdapter.kt
    ├── appselect/
    │   ├── AppSelectActivity.kt  # Lista searchable para elegir apps favoritas
    │   ├── AppSelectViewModel.kt
    │   └── adapter/
    │       └── InstalledAppsAdapter.kt
    ├── allapps/
    │   └── AllAppsActivity.kt    # Drawer completo de apps con búsqueda + menú de bloqueo/renombrar en long-press
    ├── settings/
    │   └── SettingsActivity.kt   # Tema, reloj, fuente, idioma, pausa consciente, tutorial
    ├── onboarding/
    │   └── OnboardingActivity.kt # Flujo de onboarding de 3 pasos con ViewPager2
    ├── pause/
    │   └── ConsciousPauseActivity.kt # Cuenta regresiva de respiración antes de lanzar la app objetivo
    ├── detox/
    │   └── DetoxBlockedActivity.kt   # Pantalla de bloqueo a pantalla completa con cuenta regresiva en vivo
    └── widget/
        ├── AnalogClockView.kt
        └── WeightGradientTextView.kt
```

---

## Recursos

- `res/values/strings.xml` — cadenas en inglés principal (todas en minúsculas, tono minimalista).
- Localizaciones en `values-es`, `values-fr`, `values-de`, `values-en-rGB`.
- `res/values/themes.xml` — 4 temas integrados + `Base.Theme.FocusPhone` + estilos personalizados de widgets/diálogos.
- `res/values-night/themes.xml` — variantes nocturnas.
- `res/drawable/` — formas personalizadas, círculos de previsualización de tema, iconos de tutorial.
- `res/anim/fp_fade_in.xml` & `fp_fade_out.xml` — usados por `BaseActivity` para transiciones de actividad consistentes.

---

## Comandos de Build

```bash
# APK Debug
./gradlew :app:assembleDebug

# APK Release (minificado + shrinkResources)
./gradlew :app:assembleRelease

# Ejecutar tests unitarios
./gradlew :app:testDebugUnitTest

# Ejecutar tests instrumentados
./gradlew :app:connectedDebugAndroidTest

# Limpiar
./gradlew clean
```

### Notas de Configuración de Build
- `viewBinding = true` y `buildConfig = true` están habilitados.
- Las builds de release usan ProGuard (`proguard-rules.pro`) con `isMinifyEnabled = true` y `isShrinkResources = true`.
- Room usa `kapt` (procesador de anotaciones legacy) porque se utiliza AGP 9.2.1.
- El proyecto define un `org.gradle.java.home` fijo apuntando a `C:/Program Files/Android/openjdk/jdk-21.0.8`.
- La configuración de firma de release es condicional: establece `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, y `RELEASE_KEY_PASSWORD` en `gradle.properties` (o como propiedades del proyecto) para habilitar la firma de release. Sin estas propiedades, la build de release usa sin configuración de firma y puede ensamblarse para testing local.

---

## Guías de Estilo de Código

### Idioma y Tono
- **Todos los comentarios de código y KDoc se escriben en inglés.**
- El texto de la UI (strings) se escribe en inglés en minúsculas con un tono minimalista y calmado (ej. `"no apps selected.\ntap the menu to add apps."`).

### Convenciones de Formato
- **Separadores de sección** en clases usan un patrón consistente:
  ```kotlin
  // ── Section Name ──────────────────────────────────────────────────
  ```
- Nomenclatura de paquetes: `app.focusphone.{layer}.{feature}`.
- Las actividades usan métodos helper `dp(value: Int)` para conversión de píxeles.
- Todas las actividades extienden `BaseActivity`.

### Inyección de Dependencias
- No introducir Hilt/Dagger sin antes resolver la compatibilidad con AGP 9.x.
- Registrar nuevos singletons en `AppContainer` y exponer métodos factory en los ViewModels (`companion object { fun factory(app: FocusPhoneApp): ViewModelProvider.Factory }`).
- `AppContainer` recibe un `applicationScope: CoroutineScope` para tareas en background de larga duración como migraciones de datos.

### Patrones de UI
- Siempre aplicar window insets manualmente (`ViewCompat.setOnApplyWindowInsetsListener`) para layouts edge-to-edge.
- Usar `MaterialAlertDialogBuilder` para diálogos con el tema de diálogo personalizado de la app.
- Las etiquetas de texto mostradas a los usuarios típicamente están en minúsculas.

---

## Estrategia de Testing

> **Estado actual:** El testing es mínimo. Solo existe un test unitario placeholder y un test instrumentado placeholder.

| Tipo | Ubicación | Runner |
|------|-----------|--------|
| Tests unitarios | `app/src/test/java/app/focusphone/` | JVM (JUnit 4) |
| Instrumentados | `app/src/androidTest/java/dev/rezzt/` | AndroidJUnit4 |

**Nota:** Los nuevos tests unitarios se colocan bajo el paquete de la aplicación (`app.focusphone`). Los tests instrumentados legacy permanecen en `dev.rezzt` y deberían migrarse.

### Enfoque de Testing Recomendado
- Tests unitarios para Use Cases y lógica de Repository con repositorios mockeados.
- Tests de UI para flujos críticos: onboarding, selección de apps, cambio de perfil, verificación de contraseña detox.
- Dado que la app consulta todos los paquetes instalados, los tests instrumentados deberían ejecutarse en un dispositivo/emulador con un conjunto de apps predecible.

---

## Decisiones Arquitectónicas Clave

### 1. DI Manual en lugar de Hilt
`AppContainer` conecta manualmente todos los singletons. El código documenta explícitamente que Hilt está planificado para "Fase 2" una vez que haya una versión compatible con AGP 9.x.

### 2. SharedPreferences para Configuraciones (no DataStore)
Las preferencias de tema y fuente deben leerse **síncronamente** dentro de `BaseActivity.attachBaseContext()` y `onCreate()` antes de `setContentView`. La naturaleza asíncrona de DataStore la hace inadecuada aquí.

### 3. Sin Iconos de Apps en la Pantalla de Inicio
La pantalla de inicio (`HomeActivity`) muestra solo etiquetas de texto para las apps favoritas. Los iconos se omiten deliberadamente para reducir el ruido visual.

### 4. Bloqueo Basado en Perfiles
- **Normal**: sin bloqueo.
- **Work**: bloquea apps bloqueadas manualmente + apps que coincidan con una lista de palabras clave hardcodeada (`DISTRACTING_KEYWORDS`).
- **Detox**: bloquea **todas** las apps excepto FocusPhone mismo. Requiere una contraseña (mínimo 4 caracteres) para salir antes de tiempo. Revierte automáticamente a Normal cuando el temporizador expira.

### 5. Migración de SharedPreferences a Room
`FavouriteAppRepositoryImpl` contiene un método `migrateFromSharedPreferences()` que se ejecuta una vez al iniciar, convirtiendo el antiguo formato de string delimitado por pipes en entidades Room y luego eliminando la clave antigua.

---

## Consideraciones de Seguridad

### Permisos (AndroidManifest.xml)
| Permiso | Razón |
|---------|-------|
| `QUERY_ALL_PACKAGES` | Listar todas las apps instaladas para el drawer y el selector. |

### Comportamiento de la Contraseña Detox
- La contraseña detox se hashea con **SHA-256** antes de almacenarse en SharedPreferences (`focusphone_profile`).
- Después de 4 intentos incorrectos de contraseña, el detox se **fuerza a deshabilitar** por seguridad y la contraseña se borra.
- El archivo SharedPreferences `focusphone_profile` se excluye del backup en la nube vía `data_extraction_rules.xml`.

### Colores de Tema Personalizados
- Los colores hex personalizados se parsean en runtime con `Color.parseColor()`. Los valores inválidos caen en valores por defecto seguros (`#181818` / `#FAFAFA`).

---

## Notas de Despliegue

- La app se firma y construye como APK vía `./gradlew assembleRelease`.
- La salida está en `app/release/`.
- `versionCode` y `versionName` se definen en `app/build.gradle.kts` (`defaultConfig`).
- El intent filter del launcher en `AndroidManifest.xml` usa `android:priority="1"` en las categorías `HOME` / `DEFAULT` / `LAUNCHER`.

---

## Tareas Comunes para Agentes

### Agregar una Nueva Configuración
1. Agregar las claves de preferencia al objeto `*Preferences.kt` apropiado en `data.preferences`.
2. Exponer los cambios necesarios en repository/use-case.
3. Conectar la UI en la actividad relevante (usualmente `SettingsActivity`).
4. Si la configuración afecta el tema o la escala de fuente, asegurar que `BaseActivity.onResume()` detecte el cambio y llame a `recreate()`.

### Agregar una Nueva Actividad
1. Extender `BaseActivity`.
2. Declararla en `AndroidManifest.xml`.
3. Usar `ActivityXxxBinding.inflate(layoutInflater)` para view binding.
4. Llamar a `setupInsets()` o equivalente para padding edge-to-edge.
5. Observar flows con `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { ... } }`.

### Agregar un Nuevo Use Case
1. Definirlo en `domain.usecase`.
2. Inyectar los repositorios requeridos.
3. Exponerlo a través de `AppContainer`.
4. Pasarlo al ViewModel vía el método `factory()`.

### Modificar la Base de Datos
- Incrementar la versión de `AppDatabase` y proveer una migración, o usar `fallbackToDestructiveMigration()` (actualmente habilitado) solo durante desarrollo.
