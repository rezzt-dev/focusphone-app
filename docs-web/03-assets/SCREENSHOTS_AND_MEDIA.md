# FocusPhone — Brief de Recursos Visuales

> **Documento tipo:** Inventario + Brief de producción  
> **Destinatario:** Diseñador gráfico, capturador de pantallas, responsable de marketing  
> **Proyecto:** FocusPhone v1.0.4 · `app.focusphone`

---

## 1. Principios generales de captura

FocusPhone es un launcher minimalista y sin distracciones. Por tanto, **todos los recursos visuales deben reflejar calma, orden y intencionalidad**.

- **No saturar:** el producto se vende por lo que *elimina*, no por lo que añade.
- **Contexto real:** usa apps de ejemplo reales (Reloj, Calendario, Mensajes, Teléfono) para que el usuario se identifique.
- **Consistencia temática:** si una captura usa el tema *slate*, mantén ese tema en toda la serie de esa funcionalidad.
- **Dispositivo limpio:** barra de estado limpia (sin notificaciones pendientes, sin batería baja, sin modo avión).

---

## 2. Inventario de Screenshots

### 2.1. Pantalla de inicio — `HomeActivity`

| ID | Descripción | Estado requerido | Tema recomendado |
|----|-------------|------------------|------------------|
| `scr-home-01` | Vista por defecto. Reloj digital centrado, 5 apps favoritas, alineación izquierda. | Reloj: **Digital**. Apps: Reloj, Calendario, Teléfono, Mensajes, Cámara. | slate |
| `scr-home-02` | Mismo estado, tema alternativo para contraste. | Idéntico a `scr-home-01`. | warm beige |
| `scr-home-03` | Reloj analógico, alineación derecha, 10 apps favoritas (límite). | Reloj: **Analógico**. Alineación: **derecha**. | navy |
| `scr-home-04` | Reloj oculto, alineación centro, pocas apps. | Reloj: **Hidden**. 3 apps. | off-white |
| `scr-home-05` | Modo Detox activo. Pantalla de inicio con indicador de perfil Detox. | Perfil: **Detox**. Timer visible (ej. 02:34:10 restantes). | slate |
| `scr-home-06` | Modo Work activo. Apps bloqueadas marcadas sutilmente. | Perfil: **Work**. Alguna app favorita bloqueada (grisada o con indicador). | slate |
| `scr-home-07` | Reordenamiento por arrastre. Captura del gesto de drag & drop. | Arrastrando la 3ª app hacia la 1ª posición. | slate |

**Especificaciones técnicas para Home:**
- **Orientación:** Portrait obligatoria.
- **Reloj:** debe mostrar una hora estética (ej. `10:08`, `14:35`). Evitar `00:00` o `23:59`.
- **Fecha:** usar una fecha realista y coherente entre capturas (ej. `sábado, 17 de mayo`).
- **Barra de estado:** ocultar o mostrar limpia. No mezclar entre capturas de la misma serie.

---

### 2.2. Cajón de apps — `AllAppsActivity`

| ID | Descripción | Estado requerido |
|----|-------------|------------------|
| `scr-drawer-01` | Cajón completo abierto. Lista alfabética de apps con barra de búsqueda vacía. | 15–20 apps visibles. Sin texto en búsqueda. |
| `scr-drawer-02` | Búsqueda activa. Filtrado por "cal" (mostrando Calculadora y Calendario). | Texto de búsqueda: `cal`. |
| `scr-drawer-03` | Menú contextual de app bloqueada. Long-press sobre una app distractora. | Mostrar opciones: *bloquear*, *renombrar*. |

---

### 2.3. Selector de favoritas — `AppSelectActivity`

| ID | Descripción | Estado requerido |
|----|-------------|------------------|
| `scr-select-01` | Lista de apps instaladas con checkboxes. 5 seleccionadas de 10 permitidas. | Indicador visual: "5 / 10" o similar. |
| `scr-select-02` | Límite alcanzado. Toast o indicador de "límite de favoritas alcanzado". | 10 apps marcadas. Intentando marcar la 11ª. |
| `scr-select-03` | Búsqueda en selector. Filtrado de apps por término. | Texto: `mens`. |

---

### 2.4. Ajustes — `SettingsActivity`

| ID | Descripción | Estado requerido | Tema |
|----|-------------|------------------|------|
| `scr-settings-01` | Pantalla de ajustes completa. Scroll mostrando todas las categorías. | Categorías visibles: tema, reloj, fuente, pausa consciente, perfiles, tutorial. | slate |
| `scr-settings-02` | Selector de tema expandido. Las 4 paletas predefinidas + opción de color personalizado. | Tema seleccionado: **navy** (con checkmark). | — |
| `scr-settings-03` | Selector de tipo de reloj. Los 5 estilos en formato tarjeta o lista. | Seleccionado: **Words**. | slate |
| `scr-settings-04` | Selector de fuente tipográfica. Lista de fuentes descargables. | Seleccionada: **Poppins**. Preview en vivo del nombre de la fuente. | slate |
| `scr-settings-05` | Ajuste de Pausa Consciente. Toggle activo, duración 5 segundos. | Toggle: **ON**. Slider/botones: `5 s`. | slate |
| `scr-settings-06` | Gestión de perfiles. Selector Normal / Work / Detox con descripciones. | Seleccionado: **Work**. | slate |

---

### 2.5. Onboarding — `OnboardingActivity`

| ID | Descripción | Estado requerido |
|----|-------------|------------------|
| `scr-onboarding-01` | Paso 1 de 3. Pantalla de bienvenida con ilustración o ícono grande (`ic_tutorial_home`). | Texto de bienvenida visible. Indicador de paginación: página 1 activa. |
| `scr-onboarding-02` | Paso 2 de 3. Explicación de perfiles (`ic_tutorial_profiles`). | Descripción breve de Normal, Work y Detox. |
| `scr-onboarding-03` | Paso 3 de 3. Explicación de la Pausa Consciente (`ic_tutorial_pause`). | CTA final: *"comenzar"* o *"configurar ahora"*. |

---

### 2.6. Pausa Consciente — `ConsciousPauseActivity`

| ID | Descripción | Estado requerido |
|----|-------------|------------------|
| `scr-pause-01` | Temporizador de respiración activo. Círculo o animación de cuenta regresiva. | Duración: 5 segundos. Mostrar `03` (a mitad de cuenta). |
| `scr-pause-02` | Estado final. Temporizador completado. App a abrir visible. | Texto: *"abriendo mensajes..."* o similar. |

---

### 2.7. Bloqueo Detox — `DetoxBlockedActivity`

| ID | Descripción | Estado requerido |
|----|-------------|------------------|
| `scr-detox-01` | Pantalla de bloqueo completa. Fondo oscuro, cuenta atrás grande. | Timer: `01:45:22` restantes. Sin password visible. |
| `scr-detox-02` | Diálogo de desbloqueo con contraseña. Input de 4+ caracteres. | Campo de texto con asteriscos. Botón: *"desbloquear"*. |
| `scr-detox-03` | Estado post-desbloqueo exitoso (o cambio de perfil). Indicador de vuelta a Normal. | Toast o banner: *"perfil cambiado a normal"*. |

---

### 2.8. Widgets y componentes aislados

| ID | Descripción | Contexto |
|----|-------------|----------|
| `scr-widget-analog` | Reloj analógico personalizado (`AnalogClockView`) en tamaño grande. | Fondo transparente o del tema activo. Hora: `10:10`. |
| `scr-widget-gradient` | Texto con degradado (`WeightGradientTextView`) mostrando "focus". | Aisladamente, para uso en landing page o README. |

---

## 3. Videos Demo

### 3.1. Flujo principal — "Primeros pasos"

| Propiedad | Especificación |
|-----------|---------------|
| **ID** | `vid-demo-01` |
| **Duración** | 30–45 segundos |
| **Resolución** | 1080×1920 (portrait, 9:16) |
| **FPS** | 30 |
| **Dispositivo de grabación** | Emulador Pixel 7 (API 34) o dispositivo físico con pantalla OLED de 6.1"+ |
| **Tema** | slate |
| **Flujo** | 1. Pantalla de inicio limpia → 2. Swipe left para abrir cajón → 3. Tap en *ajustes* → 4. Cambio de tema a *warm beige* → 5. Vuelta a inicio con transición suave. |
| **Audio** | Sin audio. Subtítulos opcionales en inglés/español. |
| **Formato de entrega** | MP4 (H.264) + GIF de 5 segundos (extracto del cambio de tema). |

### 3.2. Flujo de bloqueo — "Detox en acción"

| Propiedad | Especificación |
|-----------|---------------|
| **ID** | `vid-demo-02` |
| **Duración** | 20–30 segundos |
| **Resolución** | 1080×1920 |
| **FPS** | 30 |
| **Dispositivo** | Emulador Pixel 7 (API 34) |
| **Tema** | navy |
| **Flujo** | 1. Inicio en perfil Normal → 2. Abrir ajustes → 3. Activar Detox (30 min) → 4. Intentar abrir Instagram → 5. Aparece `DetoxBlockedActivity` con cuenta regresiva. |
| **Formato** | MP4 (H.264). |

### 3.3. Flujo de intencionalidad — "Pausa Consciente"

| Propiedad | Especificación |
|-----------|---------------|
| **ID** | `vid-demo-03` |
| **Duración** | 15–20 segundos |
| **Resolución** | 1080×1920 |
| **FPS** | 30 |
| **Dispositivo** | Emulador Pixel 7 (API 34) |
| **Tema** | off-white |
| **Flujo** | 1. Tap en app favorita (Mensajes) → 2. Aparece `ConsciousPauseActivity` con cuenta regresiva de 5 s → 3. Animación de respiración → 4. Se abre la app objetivo. |
| **Formato** | MP4 (H.264) + WebM (para web, menor peso). |

---

## 4. Assets Gráficos Adicionales

### 4.1. Logo e identidad

| ID | Descripción | Formato | Especificaciones |
|----|-------------|---------|------------------|
| `asset-logo-svg` | Logo principal de FocusPhone. | SVG | Monocromo + versión en color del tema *slate*. Mínimo 2 variantes: horizontal e icono cuadrado. |
| `asset-logo-png` | Logo para usos donde SVG no es viable. | PNG | 1024×1024 px, fondo transparente. Exportar también a 512×512 y 192×192. |
| `asset-wordmark` | Texto "focusphone" en tipografía de marca. | SVG / PNG | Versión en minúsculas. Al menos 2 colores: negro `#181818` y blanco `#FAFAFA`. |

### 4.2. Favicon y iconos web

| ID | Descripción | Formato | Especificaciones |
|----|-------------|---------|------------------|
| `asset-favicon-ico` | Favicon clásico. | ICO | 16×16, 32×32, 48×48 (multi-res en un solo archivo). |
| `asset-favicon-png` | Favicon PNG para navegadores modernos. | PNG | 32×32, 180×180 (Apple touch icon). Fondo transparente o sólido acorde al tema. |
| `asset-favicon-svg` | Favicon vectorial. | SVG | Escalable, para navegadores que soportan SVG favicon. |

### 4.3. Open Graph (redes sociales / embeds)

| ID | Descripción | Formato | Especificaciones |
|----|-------------|---------|------------------|
| `asset-og-default` | Imagen OG genérica para el sitio web. | PNG / JPG | 1200×630 px. Ratio 1.91:1. Mostrar dispositivo con FocusPhone en pantalla + logo + tagline. |
| `asset-og-twitter` | Variante para Twitter/X card. | PNG / JPG | 1200×600 px (summary_large_image). |
| `asset-og-release` | Imagen OG para anuncios de nueva versión. | PNG / JPG | 1200×630 px. Incluir número de versión y 1–2 features destacadas. |

### 4.4. Badges y sellos (futuro / reserva)

| ID | Descripción | Formato | Especificaciones |
|----|-------------|---------|------------------|
| `asset-badge-playstore` | Badge "Get it on Google Play". | SVG / PNG | Oficial de Google. Incluir solo si se publica en Play Store. |
| `asset-badge-github` | Badge "Download on GitHub". | SVG / PNG | Estilo minimalista, coherente con la estética del sitio. |
| `asset-badge-oss` | Sello "Open Source · MIT License". | SVG / PNG | Para README y landing page. |

### 4.5. Ilustraciones de tutorial (referencia a assets existentes)

El proyecto ya incluye iconos de tutorial en `res/drawable/`:
- `ic_tutorial_home`, `ic_tutorial_block`, `ic_tutorial_clock`, `ic_tutorial_detox`, `ic_tutorial_edit`, `ic_tutorial_pause`, `ic_tutorial_profiles`, `ic_tutorial_themes`.

**Brief para rediseño (si aplica):**
- Estilo: línea fina, stroke 1.5–2 dp, sin relleno.
- Color: adaptable al tema activo (tintado dinámico).
- Tamaño de lienzo: 96×96 dp.
- Formato de entrega: SVG limpio + Vector Drawable XML compatible.

---

## 5. Especificaciones Técnicas de Exportación

### 5.1. Screenshots estáticas

| Propiedad | Valor |
|-----------|-------|
| **Formato** | PNG (para UI con texto nítido) o WebP (para web, si se requiere menor peso) |
| **Resolución base** | 1080×1920 (equivalente a Pixel 7 en portrait) |
| **Densidad** | 1× (exportar a 2× si se usará en retina/displays densos) |
| **Compresión PNG** | Sin pérdida. Optimizar con `oxipng` o `pngquant` si es para web. |
| **Compresión WebP** | Calidad 90–95 %. |
| **Nombrado** | `scr-{pantalla}-{número}.{ext}` (ej. `scr-home-01.png`) |
| **Fondo** | Capturar con el fondo real del tema. **No añadir sombras, bordes o marcos de dispositivo** salvo que sea para un uso específico de marketing. |

### 5.2. Videos

| Propiedad | Valor |
|-----------|-------|
| **Formato** | MP4 (H.264, perfil High, nivel 4.1) |
| **Resolución** | 1080×1920 |
| **FPS** | 30 |
| **Bitrate** | 8–12 Mbps para MP4. 2–4 Mbps para WebM. |
| **Color** | sRGB. Sin bandas de color (verificar gradientes del tema *slate* y *navy*). |
| **Bordes** | Sin marco de dispositivo. Pura captura de pantalla. |
| **Cursor / toques** | Opcional: añadir indicadores de toque circulares (estilo Material, 48 dp, alpha 30 %) si el video es para tutorial. |

### 5.3. Assets vectoriales

| Propiedad | Valor |
|-----------|-------|
| **Formato maestro** | SVG 1.1 (sin fuentes incrustadas, convertir a trazos si es necesario) |
| **Exportación Android** | Vector Drawable XML (`viewportWidth="24" viewportHeight="24"` para iconos, adaptar para ilustraciones grandes) |
| **Paletas** | Todos los assets deben funcionar en: `#181818` (slate dark), `#FAFAFA` (off-white light), `#3D5A80` (navy), `#E0C097` (warm beige). |

---

## 6. Checklist de producción

- [ ] `scr-home-01` a `scr-home-07` capturados y exportados.
- [ ] `scr-drawer-01` a `scr-drawer-03` capturados y exportados.
- [ ] `scr-select-01` a `scr-select-03` capturados y exportados.
- [ ] `scr-settings-01` a `scr-settings-06` capturados y exportados.
- [ ] `scr-onboarding-01` a `scr-onboarding-03` capturados y exportados.
- [ ] `scr-pause-01` y `scr-pause-02` capturados y exportados.
- [ ] `scr-detox-01` a `scr-detox-03` capturados y exportados.
- [ ] `vid-demo-01`, `vid-demo-02`, `vid-demo-03` grabados y comprimidos.
- [ ] `asset-logo-svg`, `asset-logo-png`, `asset-wordmark` entregados.
- [ ] `asset-favicon-*` entregados.
- [ ] `asset-og-*` entregados.
- [ ] Todos los archivos nombrados según convención y ubicados en `docs-web/03-assets/` (o ruta acordada).

---

## 7. Notas para el capturador / diseñador

1. **Estado del dispositivo:** antes de capturar, ajusta la hora del emulador a `10:08` y la fecha a un sábado de primavera/otoño (evitar días de semana para que la fecha sea estética).
2. **Idioma:** las capturas para la web en inglés deben tener el dispositivo en `en-US`. Las capturas para la web en español deben tenerlo en `es-ES`.
3. **Notificaciones:** desactiva todas las notificaciones del sistema para evitar banners intrusivos.
4. **Batería:** fija el nivel de batería al 100 % en el emulador para evitar el icono de batería baja.
5. **Tema del sistema:** si capturas el modo oscuro, asegúrate de que la barra de navegación y de estado sean coherentes (edge-to-edge).
6. **Consistencia tipográfica:** si una serie de capturas usa **Poppins**, no mezcles con **Comfortaa** en la misma serie.
7. **Apps de ejemplo:** usa siempre el mismo conjunto de apps de ejemplo para mantener coherencia narrativa.

---

<div align="center">

**[⬅ Volver a assets del proyecto](../)**

</div>
