# Changelog

Todos los cambios notables de este proyecto se documentarán en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/), y este proyecto adhiere a [Semantic Versioning](https://semver.org/lang/es/).

---

## [Unreleased]

### Changed
- Documentación del producto ampliada (`DOWNLOAD_AND_INSTALL.md`, `TECH_SPECS.md`).

---

## [1.0.4] — 2026-05-17

> **versionCode:** `2` · **versionName:** `1.0.4`

### Fixed
- Corrección de colores sensibles al tema (`theme-aware colors`) para evitar contrastes incorrectos en ciertas paletas.
- Ajuste del escalado del reloj (`clock scaling`) en pantallas de densidad alta y baja.
- Corrección del ancho del cajón de aplicaciones (`drawer width`) en orientación horizontal.

### Changed
- Eliminación de permisos no esenciales para cumplimiento con políticas de Play Store.
- Mejoras de rendimiento (`perf`) en la carga inicial del launcher.
- Refinamiento de la configuración de firma (`signing`) para builds de release.

---

## [1.0.3] — 2026-05-13

> **Rama:** `alfa-1.0.3`

### Fixed
- Corrección crítica de errores (`critical crash`) que causaban cierres inesperados al cambiar de perfil rápidamente.
- Resolución de bugs de pérdida de datos (`data-loss`) en la persistencia de apps favoritas durante cambios de orientación.
- Correcciones en el ciclo de vida (`lifecycle`) de actividades que provocaban fugas de memoria en el onboarding.

### Changed
- Actualización del icono de la aplicación (`app icon`) con mejor adaptación a temas monocromáticos del sistema.

---

## [1.0.2] — 2026-05-08

> **Rama:** `alfa-1.0.2`

### Added
- **Escalado tipográfico global** (`global typography scaling`): ajuste independiente del tamaño de texto del reloj y de la lista de apps.
- **Válvula de seguridad Detox** (`detox emergency safety-valve`): tras 4 intentos fallidos de password, el modo Detox se desactiva automáticamente para evitar bloqueos permanentes.
- **Temas personalizados** (`custom themes`): soporte para introducir colores hexadecarios personalizados además de las 4 paletas predefinidas.
- **Controles monocromáticos alineados al tema** (`theme-aligned monochrome controls`): iconos y controles del sistema se adaptan al color de tema activo en Android 12+.

### Changed
- Rediseño completo de la interfaz de ajustes premium (`premium settings gui redesign`) con mejor jerarquía visual y agrupación por categorías.

---

## [1.0.1] — 2026-05-06

> **Rama:** `alfa-1.0.1`

### Added
- **Control de visibilidad de la barra de estado** (`status bar visibility controls`): opción para mostrar u ocultar la barra de estado en la pantalla de inicio.
- **Renombrado seguro de apps** (`safe app renaming`): posibilidad de asignar etiquetas personalizadas a las aplicaciones sin afectar el nombre del paquete.

---

## [1.0.0] — 2026-05-06

> **versionCode:** `1` · **versionName:** `1.0.0` · **Lanzamiento inicial**

### Added
- Launcher minimalista de Android con interfaz de solo texto.
- Pantalla de inicio con reloj (5 estilos: digital, apilado, palabras, analógico, oculto), calendario y hasta 10 apps favoritas.
- Cajón de apps completo con búsqueda y menú contextual (bloquear / renombrar).
- Sistema de perfiles contextuales: **Normal**, **Work** y **Detox**.
- Modo Detox con bloqueo total, temporizador y protección por contraseña (SHA-256).
- Pausa consciente (`Conscious Pause`): temporizador de respiración antes de abrir una app.
- 4 temas de color predefinidos (slate, navy, warm beige, off-white) con soporte day/night.
- Fuentes descargables integradas: Poppins, Comfortaa, Figtree, JetBrains Mono, Space Mono.
- Reordenamiento de apps favoritas por arrastre (`drag & drop`).
- Onboarding de 3 pasos con `ViewPager2`.
- Alineación flexible de contenido: izquierda, centro, derecha.
- Persistencia local con Room 2.8.4 (apps favoritas) y SharedPreferences (ajustes).

---

## Notas de versionado

- **Versiones `alfa-x.y.z`**: durante la fase inicial de desarrollo, cada versión se desarrolló en una rama temática (`alfa-1.0.1`, `alfa-1.0.2`, `alfa-1.0.3`) antes de ser fusionada a la rama principal. Estas ramas ya no están activas; el desarrollo continúa sobre la rama principal con versiones semver estándar.
- **Migraciones de datos**: las versiones 1.0.2 y 1.0.3 incluyen migraciones automáticas de SharedPreferences a Room. No se requiere acción manual por parte del usuario.

---

<div align="center">

**[⬅ Volver a la documentación del producto](../)**

</div>
