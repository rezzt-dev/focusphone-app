# FocusPhone — Especificaciones Técnicas

> **Versión:** 1.0.4  
> **Paquete:** `app.focusphone`  
> **Licencia:** MIT  
> **Repositorio:** [github.com/rezzt-devc/focusphone-app](https://github.com/rezzt-devc/focusphone-app)

---

## 1. Resumen ejecutivo

FocusPhone es un launcher Android nativo desarrollado al 100 % en Kotlin. Prioriza el rendimiento, la simplicidad y la privacidad: no requiere cuenta de Google, no incluye anuncios y funciona completamente offline.

El proyecto sigue una arquitectura **MVVM + Clean Architecture lite**, con inyección de dependencias manual y persistencia local vía Room y SharedPreferences. Está diseñado para ser compilado con el ecosistema más reciente de Android (AGP 9.x, SDK 36) manteniendo compatibilidad con dispositivos desde Android 7.0 (2016).

---

## 2. Stack tecnológico

### Core

| Tecnología | Versión / Detalle | Propósito |
|------------|-------------------|-----------|
| **Kotlin** | 100 % del codebase | Lenguaje principal. Coroutines para concurrencia. |
| **Android SDK** | Min 24, Target / Compile 36 | Compatibilidad Android 7.0+ con APIs modernas. |
| **AndroidX Core KTX** | Última estable | Extensiones Kotlin para APIs del framework. |
| **AppCompat & Activity** | AndroidX | Compatibilidad de componentes UI. |
| **Fragment & ConstraintLayout** | AndroidX | Construcción de pantallas y navegación. |
| **RecyclerView & ViewPager2** | AndroidX | Listas de apps y flujo de onboarding. |
| **Lifecycle (ViewModel, LiveData)** | AndroidX | Gestión reactiva del ciclo de vida. |
| **Material 3** | `com.google.android.material` | Sistema de diseño, diálogos, componentes UI. |
| **Kotlin Coroutines** | `kotlinx-coroutines-android` | Operaciones asíncronas y flujos de datos. |
| **StateFlow / SharedFlow** | Kotlin Coroutines | Estados reactivos expuestos por ViewModels. |

### Persistencia

| Tecnología | Versión / Detalle | Propósito |
|------------|-------------------|-----------|
| **Room** | 2.8.4 | Base de datos SQLite para apps favoritas (`favourite_apps`). |
| **SharedPreferences** | Framework Android | Almacenamiento síncrono de ajustes (temas, fuentes, perfiles). |
| **KAPT** | Legacy annotation processor | Procesamiento de anotaciones de Room (requerido por AGP 9.2.1). |

> **Nota:** SharedPreferences se usa intencionalmente en lugar de DataStore porque los temas y fuentes deben leerse de forma síncrona durante `attachBaseContext()` y `onCreate()`, antes de `setContentView`.

---

## 3. Arquitectura

### Diagrama de capas

```
┌─────────────────────────────────────────┐
│           Presentation (UI)             │
│  HomeActivity, SettingsActivity, etc.   │
│  ViewModels ← StateFlow/SharedFlow      │
│  View Binding                           │
├─────────────────────────────────────────┤
│            Domain (Use Cases)           │
│  GetInstalledAppsUseCase                │
│  GetFavouriteAppsUseCase                │
│  ToggleFavouriteAppUseCase              │
│  UpdateAppsOrderUseCase                 │
├─────────────────────────────────────────┤
│              Data Layer                 │
│  AppRepositoryImpl                      │
│  FavouriteAppRepositoryImpl             │
│  Room (AppDatabase, FavouriteAppDao)    │
│  *Preferences (SharedPreferences)       │
└─────────────────────────────────────────┘
```

### Patrones aplicados

| Patrón | Implementación | Justificación |
|--------|---------------|---------------|
| **MVVM** | `ViewModel` + `StateFlow` | Separación clara entre lógica de UI y estado. |
| **Repository Pattern** | Interfaces en `domain.repository`, implementaciones en `data.repository` | Abstracción de la fuente de datos (Room, SharedPrefs, PackageManager). |
| **Use Cases** | Clases en `domain.usecase` | Unidades de negocio atómicas, testables y reutilizables. |
| **DI Manual** | `AppContainer` en `FocusPhoneApp` | Gráfico de dependencias explícito sin necesidad de Hilt/Dagger en esta fase. |
| **View Binding** | Habilitado en `build.gradle.kts` | Referencias type-safe a vistas sin `findViewById`. |

### Flujo de datos típico

1. La UI (Activity) observa un `StateFlow` expuesto por el `ViewModel`.
2. El `ViewModel` invoca un **Use Case** del dominio.
3. El Use Case opera sobre un **Repository**.
4. El Repository decide si leer/escribir en **Room** o **SharedPreferences**.
5. El flujo de datos reactivo actualiza automáticamente la UI.

---

## 4. Requisitos de compilación

| Requisito | Versión / Especificación |
|-----------|--------------------------|
| **Android Studio** | Ladybug o superior |
| **JDK** | 21 (ruta fijada en `gradle.properties`) |
| **Android Gradle Plugin (AGP)** | 9.2.1 |
| **Java Compatibility** | 11 (bytecode target) |
| **Kotlin** | 2.0+ |
| **Gradle** | Compatible con AGP 9.2.1 |

### Configuración del entorno

```bash
# 1. Clonar el repositorio
git clone https://github.com/rezzt-devc/focusphone-app.git
cd focusphone-app/focusphone-project

# 2. Sincronizar dependencias
./gradlew :app:assembleDebug

# 3. Instalar en dispositivo/emulador
./gradlew :app:installDebug
```

### Opciones de build habilitadas

```kotlin
// app/build.gradle.kts
viewBinding = true
buildConfig = true
```

### Build release

```bash
./gradlew :app:assembleRelease
```

- **MinifyEnabled:** `true`
- **ShrinkResources:** `true`
- **ProGuard:** Configuración activa en `proguard-rules.pro`
- **Signing:** Condicional. Requiere definir `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` y `RELEASE_KEY_PASSWORD` en `gradle.properties`.

---

## 5. Estructura del código

```
app/src/main/java/app/focusphone/
├── FocusPhoneApp.kt              # Application class; contiene AppContainer
├── di/
│   └── AppContainer.kt           # Grafo de DI manual (singletons, repos, use cases)
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt        # Singleton de Room
│   │   └── FavouriteAppDao.kt    # DAO de apps favoritas
│   ├── model/
│   │   └── FavouriteApp.kt       # Entidad Room
│   ├── preferences/
│   │   ├── ThemePreferences.kt   # Tema, alineación, onboarding, status bar
│   │   ├── FontPreferences.kt    # Familia de fuente, tipo de reloj, tamaños
│   │   ├── ProfilePreferences.kt # Perfiles Normal/Work/Detox, timer y password
│   │   ├── ConsciousPausePreferences.kt # Activar/desactivar + duración
│   │   └── CustomAppLabelsPreferences.kt # Etiquetas personalizadas de apps
│   └── repository/
│       ├── AppRepositoryImpl.kt       # Consulta apps instaladas (PackageManager)
│       └── FavouriteAppRepositoryImpl.kt # Merge Room + etiquetas + migración legacy
├── domain/
│   ├── model/
│   │   ├── AppInfo.kt            # Modelo de dominio: app instalada
│   │   ├── ClockType.kt          # DIGITAL | STACKED | WORDS | ANALOG | HIDDEN
│   │   ├── AppFont.kt            # Enum de fuentes (sistema + descargables)
│   │   └── ToggleResult.kt       # Added | Removed | LimitReached
│   ├── repository/
│   │   ├── AppRepository.kt
│   │   └── FavouriteAppRepository.kt
│   └── usecase/
│       ├── GetInstalledAppsUseCase.kt
│       ├── GetFavouriteAppsUseCase.kt
│       ├── ToggleFavouriteAppUseCase.kt   # Límite: MAX_FAVOURITES = 10
│       └── UpdateAppsOrderUseCase.kt
└── ui/
    ├── base/
    │   └── BaseActivity.kt       # Aplicación de tema, edge-to-edge, transiciones fade
    ├── home/
    │   ├── HomeActivity.kt       # Launcher principal
    │   ├── HomeViewModel.kt
    │   └── adapter/
    │       └── FavouriteAppsAdapter.kt
    ├── appselect/
    │   ├── AppSelectActivity.kt  # Selector de apps favoritas
    │   ├── AppSelectViewModel.kt
    │   └── adapter/
    │       └── InstalledAppsAdapter.kt
    ├── allapps/
    │   └── AllAppsActivity.kt    # Cajón completo con búsqueda
    ├── settings/
    │   └── SettingsActivity.kt   # Personalización global
    ├── onboarding/
    │   └── OnboardingActivity.kt # Flujo de bienvenida (ViewPager2)
    ├── pause/
    │   └── ConsciousPauseActivity.kt # Temporizador de respiración
    ├── detox/
    │   └── DetoxBlockedActivity.kt   # Pantalla de bloqueo con cuenta atrás
    └── widget/
        ├── AnalogClockView.kt
        └── WeightGradientTextView.kt
```

---

## 6. Decisiones arquitectónicas clave

### 6.1. Inyección de dependencias manual (sin Hilt)

Hilt está planificado para una "Fase 2", pero actualmente se evita debido a incompatibilidades documentadas entre Hilt y AGP 9.x en el procesamiento de anotaciones. `AppContainer` centraliza todas las dependencias y expone factories estáticas en los `ViewModel`:

```kotlin
companion object {
    fun factory(app: FocusPhoneApp): ViewModelProvider.Factory { ... }
}
```

### 6.2. SharedPreferences sobre DataStore para settings

Los ajustes de tema y fuente deben resolverse **síncronamente** antes de `setContentView` para evitar parpadeos (flash of unstyled content). DataStore, al ser asíncrono, no cumple este requisito.

### 6.3. Sin iconos en la pantalla de inicio

El `HomeActivity` muestra únicamente etiquetas de texto para las apps favoritas. Los iconos se muestran únicamente en los selectores (`AppSelectActivity`, `AllAppsActivity`). Esta decisión de producto reduce la carga visual y refuerza el propósito de "minimalismo digital".

### 6.4. Perfiles contextuales

| Perfil | Comportamiento |
|--------|---------------|
| **Normal** | Sin bloqueos. Acceso completo. |
| **Work** | Bloquea apps manualmente marcadas + apps cuyo nombre coincida con `DISTRACTING_KEYWORDS`. |
| **Detox** | Bloquea **todas** las apps excepto FocusPhone. Requiere password SHA-256 para salir. Auto-revierte a Normal al finalizar el timer. |

### 6.5. Migración legacy: SharedPreferences → Room

`FavouriteAppRepositoryImpl` ejecuta `migrateFromSharedPreferences()` una sola vez al inicializar, convirtiendo el antiguo formato pipe-delimited (`app1|app2|app3`) en entidades Room y eliminando la clave legacy.

---

## 7. Seguridad y permisos

### Permisos declarados

| Permiso | Uso |
|---------|-----|
| `QUERY_ALL_PACKAGES` | Listar todas las apps instaladas para el cajón y el selector de favoritas. |

> No se declaran permisos de red, almacenamiento ni sensores. El archivo `focusphone_profile` de SharedPreferences está excluido de copias de seguridad en la nube mediante `data_extraction_rules.xml`.

### Comportamiento del password en Detox

- Almacenamiento: hash **SHA-256** en SharedPreferences.
- Tras **4 intentos fallidos**, el modo Detox se desactiva forzosamente y el password se borra.
- Longitud mínima: 4 caracteres.

---

## 8. Notas para contribuidores

### Convenciones de código

- **Idioma:** Todo código, KDoc y comentarios en **inglés**.
- **Textos de UI:** En minúsculas, tono minimalista y calmado (ej: `"no apps selected.\ntap the menu to add apps."`).
- **Separadores de sección:**
  ```kotlin
  // ── Section Name ──────────────────────────────────────────────────
  ```
- **Todas las Activities** extienden `BaseActivity`.

### Añadir un nuevo ajuste

1. Añade las claves al objeto `*Preferences.kt` correspondiente en `data.preferences`.
2. Expón el cambio en el repository/use-case si es necesario.
3. Cablea la UI en la Activity correspondiente (normalmente `SettingsActivity`).
4. Si afecta a tema o fuente, asegúrate de que `BaseActivity.onResume()` detecte el cambio y llame a `recreate()`.

### Añadir una nueva Activity

1. Extiende `BaseActivity`.
2. Declara en `AndroidManifest.xml`.
3. Usa `ActivityXxxBinding.inflate(layoutInflater)` para View Binding.
4. Aplica insets con `ViewCompat.setOnApplyWindowInsetsListener` para edge-to-edge.
5. Observa flujos con:
   ```kotlin
   lifecycleScope.launch {
       repeatOnLifecycle(Lifecycle.State.STARTED) { ... }
   }
   ```

### Testing

| Tipo | Ubicación | Runner |
|------|-----------|--------|
| Unit tests | `app/src/test/java/app/focusphone/` | JUnit 4 (JVM) |
| Instrumented | `app/src/androidTest/java/dev/rezzt/` | AndroidJUnit4 |

> Los nuevos tests unitarios deben ubicarse bajo el paquete `app.focusphone`. Los tests legacy en `dev.rezzt` están pendientes de migración.

---

## 9. Enlaces y recursos

| Recurso | Enlace |
|---------|--------|
| **Repositorio** | [github.com/rezzt-devc/focusphone-app](https://github.com/rezzt-devc/focusphone-app) |
| **Issues** | [github.com/rezzt-devc/focusphone-app/issues](../../issues) |
| **Releases** | [github.com/rezzt-devc/focusphone-app/releases](../../releases) |
| **Web del autor** | [rezzt.dev](https://rezzt.dev) |
| **Licencia** | [MIT License](../../LICENSE) |

---

<div align="center">

**[⬅ Volver a la documentación del producto](../)**

</div>
