# FocusPhone — Features Details

> **Versión:** 1.0.0  
> **Estado:** Borrador para revisión  
> **Última revisión:** 2026-05-17  
> **Propietario:** Contenido + Frontend  
> **Formato:** Inglés (copy de usuario) + español (anotaciones técnicas y notas para frontend)  
> **Dependencias:** `DESIGN_GUIDE.md`, `SEO_AND_KEYWORDS.md`, `PRODUCT_COPY.md`  

---

## Cómo usar este documento

Cada feature se describe en **dos capas**:

1. **Copy de usuario (EN):** El texto que aparecerá en la página `/features`. Debe respetar minúsculas estructurales, tono calm/quiet/intentional, e integrar keywords SEO.
2. **Notas técnicas (ES):** Indicaciones para el equipo frontend sobre qué ilustrar, animar, o priorizar. Incluyen referencias a tokens de diseño, sugerencias de screenshot, y especificaciones de comportamiento.

> **Regla de oro:** Si una sección no tiene una nota visual clara, el frontend no tiene obligación de ilustrarla.

---

## 1. A Home Screen with Nothing to Distract You

> **Keywords objetivo:** `home screen`, `text-only`, `android launcher no icons`, `minimalist phone interface`.  
> **Heading SEO:** `h2` en `/features` (SEO_AND_KEYWORDS.md §5.2).  
> **Sección visual:** Hero de `/features`. Fondo `--surface-primary` o `--surface-elevated`.

---

### 1.1 Text-Only Favourites

**[EN]** your home screen holds up to ten favourite apps. no icons. no notification badges. no widgets fighting for space. just clean text labels in the order you choose.

> **[ES]** **Asset requerido:** Screenshot del home screen de FocusPhone mostrando 5-8 apps favoritas como texto plano.  
> **Especificación visual:**
> - Tema: **Slate** (default).
> - Reloj: **Digital** (default).
> - Alineación: **center**.
> - Fuente: **Poppins**.
> - Ejemplo de apps: `messages · phone · camera · maps · music · calendar · notes · settings`.
> - El reloj debe mostrar una hora estética (ej. `10:08` o `14:32`). Evitar `12:00` o `11:11` por cliché.
> - No debe haber status bar del sistema visible (o debe ser edge-to-edge).
> - **Border-radius:** `16px` en la imagen.
> - **Lazy loading:** `loading="lazy"` si va bajo el fold.
> - **Alt text:** `screenshot of focusphone home screen with slate theme, digital clock, and text-only favourite apps aligned to center`.

> **[ES]** **Nota de contenido:** El número máximo (10) es una regla de negocio (`MAX_FAVOURITES = 10`). El copy usa "up to ten" en lugar de "ten" para no sonar restrictivo.  
> **Alineación con PRODUCT_COPY.md:** Misma anáfora negativa del hero (`no icons. no badges. no widgets.`).

---

### 1.2 Dynamic Reordering

**[EN]** press and hold any favourite to drag it into a new position. the order updates instantly. no menus. no save buttons. your thumb decides.

> **[ES]** **Asset requerido:** Video o GIF corto (máx. 3 segundos, formato WebM) mostrando el gesto de long-press + drag.  
> **Especificación visual:**
> - Se ve un dedo (thumb) presionando una app favorita.
> - La app seleccionada se eleva visualmente (`translateY(-4px)`) y aumenta ligeramente de opacidad o cambia de fondo.
> - Se arrastra a una nueva posición en la lista.
> - Las demás apps se reacomodan con animación suave.
> - **Tokens de animación:** `duration-base` (200ms), `ease-default` (`cubic-bezier(0.4, 0, 0.2, 1)`).
> - **Fallback estático:** Si el video no es viable, usar dos screenshots: antes y después del reordering, con una flecha sutil indicando el movimiento.
> - **Nota de accesibilidad:** Incluir caption "demonstration of drag-to-reorder on the home screen".

> **[ES]** **Nota técnica:** La funcionalidad usa `ItemTouchHelper` con RecyclerView en la app. En web, el video/GIF debe sentirse táctil, no de mouse.  
> **Keyword:** `dynamic reordering` (secundaria).

---

### 1.3 Flexible Alignment

**[EN]** align your favourites to the left, center, or right. pick what feels comfortable for your thumb and your screen size.

> **[ES]** **Asset requerido:** Tres mini-screenshots lado a lado (grid de 3 columnas en desktop, stacked en móvil) mostrando las tres alineaciones.  
> **Especificación visual:**
> - Mismo contenido en los tres (mismas apps, mismo reloj).
> - Left: apps alineadas a la izquierda con margen consistente.
> - Center: apps centradas (default).
> - Right: apps alineadas a la derecha.
> - Etiquetar cada uno con `left`, `center`, `right` en `--text-caption` (`0.75rem`, `--text-tertiary`).
> - **Estilo de contenedor:** Cards con `border: 1px solid var(--border-subtle)` y `border-radius: 16px`.

---

## 2. Five Clock Styles

> **Keywords objetivo:** `clock styles`, `minimalist phone interface`.  
> **Heading SEO:** `h3` bajo `h2: a home screen with nothing to distract you`.  
> **Sección visual:** Grid de cards o carousel. Fondo `--surface-primary`.

---

### 2.1 Digital

**[EN]** clean numbers. precise time. the default clock for a reason.

> **[ES]** **Asset requerido:** Screenshot del home con reloj Digital en tema Slate.  
> **Especificación visual:**
> - Formato: `HH:MM` (24h o 12h según preferencia del sistema).
> - Fuente del reloj: Poppins 600 o Figtree 600 (depende de la fuente global seleccionada).
> - Tamaño grande pero no dominante (`text-hero` o ligeramente menor).
> - **Prioridad:** Este es el reloj por defecto. Debe ser el primero en mostrarse.

---

### 2.2 Stacked

**[EN]** hours above. minutes below. vertical rhythm for a quieter read.

> **[ES]** **Asset requerido:** Screenshot del home con reloj Stacked.  
> **Especificación visual:**
> - Horas arriba, minutos abajo, ambos centrados.
> - Línea de altura ajustada para que parezcan un bloque tipográfico, no dos elementos separados.
> - **Tono:** Este reloj es más "editorial". Funciona bien con fuentes como Poppins o Comfortaa.

---

### 2.3 Words

**[EN]** time written in plain language. ten past three. half past noon. no digits required.

> **[ES]** **Asset requerido:** Screenshot del home con reloj Words mostrando una hora legible.  
> **Especificación visual:**
> - Ejemplo de hora: `twenty past two` o `quarter to eleven`.
> - Texto centrado, posiblemente en dos líneas si es necesario.
> - Fuente recomendada: Figtree o Comfortaa (más orgánica).
> - **Nota de contexto:** Este reloj es el más "humanista" y diferenciador visualmente.

---

### 2.4 Analog

**[EN]** a classic clock face. no numbers, just hands. minimal, elegant, timeless.

> **[ES]** **Asset requerido:** Screenshot del home con reloj Analog.  
> **Especificación visual:**
> - Es un `AnalogClockView` custom (canvas).
> - Mostrar hora tipo `10:08` o `14:32` (manecillas en posición estética, no vertical/horizontal).
> - Sin números en el dial (solo marcas sutiles o nada).
> - Color de las manecillas: `--text-primary`.
> - **Prioridad:** Este es el reloj más "premium". Ideal para mostrar en la variante Navy o Warm Beige.

---

### 2.5 Hidden

**[EN]** no clock at all. for the days when even time feels like pressure.

> **[ES]** **Asset requerido:** Screenshot del home sin reloj, mostrando solo la fecha y las apps favoritas.  
> **Especificación visual:**
> - Debe quedar claro que "falta" el reloj intencionalmente, no que es un bug.
> - La fecha (calendario) sigue visible arriba o debajo.
> - Espacio generoso para que no se sienta vacío.
> - **Tono:** El copy es deliberadamente poético/terapéutico. Este feature apela al perfil Adicto Digital en Recuperación.

> **[ES]** **Nota de layout para la sección completa:**  
> Los 5 relojes pueden mostrarse como una **grid de 2-3 columnas** en desktop y **carousel horizontal** en móvil. Cada card contiene:
> - Screenshot del reloj.
> - Nombre del reloj (h4).
> - Descripción corta.
> - **Hover:** `translateY(-2px)`, `border-color: var(--text-tertiary)` (DESIGN_GUIDE.md §8.3).

---

## 3. Three Modes for Three Mindsets

> **Keywords objetivo:** `work profile launcher android`, `detox mode app android`, `digital detox app`, `normal profile`.  
> **Heading SEO:** `h2` en `/features`.  
> **Sección visual:** Sección destacada. Fondo `--accent-muted` (Warm Beige atenuado) para crear ritmo visual.

---

### 3.1 Normal Profile

**[EN]** everything available. your phone works the way it always has, just calmer. all apps accessible. no blocks. no friction.

> **[ES]** **Asset requerido:** Screenshot del home en modo Normal con varias apps favoritas.  
> **Especificación visual:**
> - Estado "por defecto". No hay indicadores visuales de bloqueo.
> - El selector de perfil (si es visible en el screenshot) muestra `normal` activo.
> - **Propósito:** Servir como baseline. El usuario debe entender que Normal = libertad total, pero con la interfaz limpia de FocusPhone.

---

### 3.2 Work Profile

**[EN]** focus when it matters. work blocks distracting apps automatically — the ones you choose plus anything matching known distracting keywords. your flow stays protected.

> **[ES]** **Asset requerido:** Dos screenshots comparativos o una ilustración de estado.  
> **Especificación visual:**
> - Screenshot A: intento de abrir una app bloqueada (ej. `social media app`).
> - Screenshot B: el home screen con un pequeño indicador visual (opcional) de que Work está activo.
> - **Nota técnica:** En la app, el bloqueo ocurre al intentar lanzar la app, no en el drawer. El frontend puede representar esto con una transición: tap en app → overlay de bloqueo sutil → vuelta al home.
> - **Keyword:** `work profile launcher android`, `blocks apps during work`.

---

### 3.3 Detox Mode

**[EN]** lock everything. detox blocks every app except FocusPhone itself. set a timer. add a password. your phone becomes a quiet tool until you decide otherwise.

> **[ES]** **Asset requerido:** Screenshot de `DetoxBlockedActivity`.  
> **Especificación visual:**
> - Pantalla completa con fondo oscuro o del tema seleccionado.
> - Mensaje central indicando que Detox está activo.
> - Countdown en vivo (temporizador).
> - Campo de contraseña para salida anticipada (opcionalmente visible).
> - **Tono:** No alarmista. Sereno pero firme. El mensaje es "tu teléfono está en pausa", no "estás castigado".
> - **Keyword:** `detox mode app android`, `digital detox app`.

> **[ES]** **Nota de layout:** Los tres perfiles pueden mostrarse como **tres cards verticales** con iconografía sutil (sin iconos de apps, quizás círculos de color o indicadores de estado). Cada card:
> - Nombre del perfil (h3).
> - Badge de estado: `default`, `productivity`, `deep focus`.
> - Descripción.
> - Mini-screenshot representativo.

---

## 4. Breathe Before You Open

> **Keywords objetivo:** `conscious pause`, `breathing timer`, `launcher with breathing timer`.  
> **Heading SEO:** `h2` en `/features`.  
> **Sección visual:** Sección destacada. Fondo `--surface-elevated` con card grande central.

---

### 4.1 El Flujo de Conscious Pause

**[EN]** conscious pause is a breathing countdown that appears before opening any app. you choose the duration. five seconds. ten. twenty. the screen dims. a gentle timer counts down. you breathe. then, and only then, the app opens.

> **[ES]** **Asset requerido:** Secuencia de 3-4 screenshots o un video corto (WebM, máx. 4s) mostrando el flujo completo.  
> **Especificación visual - Secuencia:**
> 1. **Trigger:** Usuario toca una app favorita (`messages`).
> 2. **Pantalla de pausa:** Fondo semitransparente o del tema actual. Texto central: `breathe`. Círculo o número que cuenta hacia atrás.
> 3. **Duración:** El número disminuye suavemente (ej. `5 → 4 → 3 → 2 → 1`).
> 4. **Completado:** Fade out de la pantalla de pausa, fade in de la app destino.
>
> **Tokens de animación:**
> - Transición entrada: `opacity 0 → 1`, `duration-slow` (300ms), `ease-out`.
> - Cuenta regresiva: cambio de número cada 1s, sin animación de rebote. Preferible `opacity` cruzada o cambio directo.
> - Transición salida: `opacity 1 → 0`, `duration-base` (200ms).
>
> **Nota de diseño:** El círculo/countdown no debe parecer un cronómetro de ejercicio agresivo. Debe sentirse como una pausa, no una prueba.

---

### 4.2 Estados del Timer

#### Estado: Activo (cuenta regresiva)

**[EN]** breathe. the countdown is running.

> **[ES]** **Asset requerido:** Screenshot de `ConsciousPauseActivity` en estado activo.  
> **Especificación visual:**
> - Texto grande y centrado: `breathe` o el número restante.
> - Fuente: Poppins 500, tamaño grande (`text-hero`).
> - Color: `--text-primary` sobre `--surface-primary` o `--accent-muted`.
> - Opcional: círculo sutil alrededor del número que se reduce proporcionalmente al tiempo (tipo progress indicator, pero mínimo).

---

#### Estado: Cancelación

**[EN]** you can cancel at any time. no guilt. the app stays closed. you stay in control.

> **[ES]** **Asset requerido:** Screenshot del mismo estado activo pero mostrando el área de cancelación (ej. botón `cancel` en la parte inferior, o gesto de back).  
> **Especificación visual:**
> - El botón/área de cancelación debe ser visible pero no dominante.
> - Texto: `cancel` en `--text-tertiary`.
> - Posición: parte inferior de la pantalla, centrado.
> - **Nota UX:** La cancelación debe ser posible pero no tan prominente que anule la intención del feature.

---

#### Estado: Completado

**[EN]** the pause is over. the app opens. you chose to be here.

> **[ES]** **Asset requerido:** No requiere screenshot propio; se muestra como transición.  
> **Especificación visual:**
> - Fade out rápido (`duration-fast`, 150ms).
> - Opcional: texto flash `opening...` que dura 200ms antes de lanzar la app.
> - La app destino entra con la animación estándar del sistema o el fade de FocusPhone.

> **[ES]** **Nota técnica importante:** El frontend de la web **no necesita** ilustrar la interacción con Detox (§4.3) a menos que haya espacio dedicado. Es un edge case. Si se ilustra, usar un screenshot de DetoxBlockedActivity con la cuenta regresiva en vivo.

---

## 5. Make It Yours

> **Keywords objetivo:** `four built-in themes`, `downloadable fonts`, `custom fonts android`, `alignment & sizing`.  
> **Heading SEO:** `h2` en `/features`.  
> **Sección visual:** Grid de cards o galería. Fondo `--surface-primary`.

---

### 5.1 Four Built-In Themes

**[EN]** four palettes designed to feel calm at any hour. slate for neutrality. navy for focus. warm beige for detox. off-white for the extreme minimalists.

> **[ES]** **Asset requerido:** Grid de 4 cards mostrando cada tema con un screenshot del home.  
> **Especificación visual por tema:**
>
> | Tema | Nombre en copy | Color acento | Personalidad | Screenshot ideal |
> |------|----------------|--------------|--------------|------------------|
> | **Slate** | `slate` | `#4A5568` | Neutral, profesional, default | Home con Digital clock, apps centradas |
> | **Navy** | `navy` | `#2C3E50` | Profundo, enfocado, work | Home con Analog clock, apps a la izquierda |
> | **Warm Beige** | `warm beige` | `#C4A882` | Cálido, orgánico, detox | Home con Stacked clock, apps centradas |
> | **Off-White** | `off-white` | `#F5F5F0` | Etereo, minimalista extremo | Home con Hidden clock, apps a la derecha |
>
> **Reglas de visualización:**
> - Cada card tiene fondo `--surface-elevated`, `border-radius: 16px`, `border: 1px solid var(--border-subtle)`.
> - Dentro de cada card: screenshot del home con ese tema + nombre del tema (h4) + descripción corta.
> - **Hover:** `translateY(-2px)`, `border-color: var(--text-tertiary)` (DESIGN_GUIDE.md §8.3).
> - **Prioridad:** Slate primero (default). Warm Beige segundo (más emocional).

---

### 5.2 Downloadable Fonts

**[EN]** choose from five typefaces. poppins for clarity. comfortaa for softness. figtree for modern ui. jetbrains mono and space mono for the engineers. plus your system font, always available.

> **[ES]** **Asset requerido:** Muestra tipográfica de cada fuente. No requiere screenshots de la app.  
> **Especificación visual:**
> - Cada fuente se muestra con su nombre en sí misma (ej. la palabra "poppins" escrita en Poppins).
> - Debajo, una frase de muestra: `your phone, intentionally.` en esa fuente.
> - Layout: lista horizontal scrolleable en móvil, grid en desktop.
> - **Nota técnica:** Las fuentes descargables usan `DownloadableFont` de Android. En web, si se quiere mostrar fielmente, se pueden cargar desde Google Fonts (Poppins, Comfortaa, Figtree, JetBrains Mono, Space Mono). Usar `font-display: swap`.

---

### 5.3 Alignment & Sizing

**[EN]** align your favourites left, center, or right. adjust clock size and text size independently. make it comfortable for your eyes and your thumb.

> **[ES]** **Asset requerido:** Misma grid de alineación que en §1.3, pero aquí enfocada en la personalización.  
> **Especificación visual:**
> - Tres mini-screenshots (left, center, right).
> - Opcional: slider visual indicando "small / medium / large" para tamaños de texto y reloj.
> - **Nota:** No es necesario mostrar los controles de settings; solo el resultado visual en el home.

---

## 6. Everything Else, One Swipe Away

> **Keywords objetivo:** `full app drawer`, `search`, `all apps`.  
> **Heading SEO:** `h2` en `/features`.  
> **Sección visual:** Sección con screenshot del drawer. Fondo `--surface-elevated`.

---

### 6.1 Full App Drawer

**[EN]** swipe left from the home screen to open the full drawer. every installed app is there, searchable, scrollable. no hidden menus. no buried settings.

> **[ES]** **Asset requerido:** Screenshot de `AllAppsActivity` (el drawer completo).  
> **Especificación visual:**
> - Lista vertical de apps instaladas, mostradas como texto (nombre de la app).
> - Barra de búsqueda en la parte superior (opcionalmente con texto `search apps`).
> - Fuente: Figtree o Poppins, tamaño body.
> - **Nota:** El drawer en la app muestra iconos pequeños junto al texto (a diferencia del home). El screenshot debe reflejar esto si es parte del diseño actual. Si el drawer es text-only también, especificarlo claramente.
> - **Keyword:** `full app drawer`.

---

### 6.2 Long-Press Menu

**[EN]** press and hold any app in the drawer to rename it or block it. your labels, your rules.

> **[ES]** **Asset requerido:** Screenshot del menú contextual o bottom sheet que aparece al hacer long-press.  
> **Especificación visual:**
> - Opciones visibles: `rename` y `block`.
> - Estilo: bottom sheet de Material con esquinas redondeadas o diálogo centrado.
> - **Nota:** Este es un feature de poder/privacidad. El copy enfatiza control (`your labels, your rules`).

---

## 7. Your Data Never Leaves Your Phone

> **Keywords objetivo:** `privacy`, `data never leaves`, `no tracking`, `offline`.  
> **Heading SEO:** `h2` en `/features`.  
> **Sección visual:** Sección de confianza. Fondo `--surface-primary`. Iconografía de escudo/candado (línea, 1.5px stroke).

---

### 7.1 100% Offline

**[EN]** no servers. no analytics. no ads. all your favourites, themes, and settings live in local storage on your device. FocusPhone works entirely offline.

> **[ES]** **Asset requerido:** Icono de escudo o candado (outline, stroke 1.5px, estilo Lucide). No requiere screenshot de la app.  
> **Especificación visual:**
> - Icono centrado o alineado a la izquierda junto al texto.
> - Color: `--text-secondary` por defecto, `--text-primary` en hover.
> - **Nota:** Esta sección es más "informativa" que "visual". El frontend puede usar una card con icono + texto, sin screenshot.

---

### 7.2 Detox Password Security

**[EN]** your detox password is hashed with sha-256 before storage. after four failed attempts, detox disables itself for safety. your willpower has a backup.

> **[ES]** **Asset requerido:** Screenshot del campo de contraseña en `DetoxBlockedActivity`.  
> **Especificación visual:**
> - Campo de entrada de contraseña (puntos o asteriscos).
> - Texto contextual: `enter detox password`.
> - **Nota de seguridad:** No mostrar la contraseña real ni el hash, obviamente.
> - **Keyword:** `detox password`, `sha-256`.

---

### 7.3 Permission Transparency

**[EN]** we only request `query_all_packages` — so we can list your apps in the drawer. that is it. no location. no contacts. no camera.

> **[ES]** **Asset requerido:** Lista visual de permisos. Check verde para `query_all_packages`, grises o ausentes para el resto.  
> **Especificación visual:**
> - Tabla o lista simple.
> - `query_all_packages`: check + label.
> - `location`, `contacts`, `camera`: cruz o ausente + label.
> - **Nota:** Esto refuerza la confianza de forma muy directa. Especialmente importante dado que el APK se descarga fuera de Google Play.

---

## 8. Mapa de Assets Visuales Requeridos

> **Resumen ejecutivo para diseñadores y frontend.** Lista priorizada de todos los screenshots, videos e ilustraciones mencionados en este documento.

### 8.1 Screenshots de la App (obligatorios)

| # | Asset | Tema | Reloj | Sección | Prioridad |
|---|-------|------|-------|---------|-----------|
| 1 | Home screen (favourites) | Slate | Digital | Hero `/features` | Crítica |
| 2 | Home screen alineaciones | Slate | Digital | §1.3 | Alta |
| 3 | Reordering (video/GIF) | Slate | Digital | §1.2 | Alta |
| 4 | Reloj Digital | Slate | Digital | §2.1 | Crítica |
| 5 | Reloj Stacked | Warm Beige | Stacked | §2.2 | Media |
| 6 | Reloj Words | Off-White | Words | §2.3 | Media |
| 7 | Reloj Analog | Navy | Analog | §2.4 | Alta |
| 8 | Reloj Hidden | Off-White | Hidden | §2.5 | Media |
| 9 | Perfil Normal | Slate | Digital | §3.1 | Media |
| 10 | Perfil Work (bloqueo) | Navy | Analog | §3.2 | Alta |
| 11 | Perfil Detox (pantalla bloqueo) | Warm Beige | Stacked | §3.3 | Alta |
| 12 | Conscious Pause activo | Slate | Digital | §4.2 | Crítica |
| 13 | Conscious Pause cancelación | Slate | Digital | §4.2 | Media |
| 14 | Tema Slate | Slate | Digital | §5.1 | Crítica |
| 15 | Tema Navy | Navy | Analog | §5.1 | Alta |
| 16 | Tema Warm Beige | Warm Beige | Stacked | §5.1 | Alta |
| 17 | Tema Off-White | Off-White | Hidden | §5.1 | Media |
| 18 | App drawer | Slate | Digital | §6.1 | Alta |
| 19 | Long-press menu | Slate | Digital | §6.2 | Media |
| 20 | Detox password input | Warm Beige | Stacked | §7.2 | Media |

### 8.2 Ilustraciones e Iconos (obligatorios)

| # | Asset | Estilo | Uso |
|---|-------|--------|-----|
| 1 | Escudo / candado | Outline 1.5px, Lucide | §7.1 Privacidad |
| 2 | Lista de permisos | Iconos check/cross simples | §7.3 Transparencia |
| 3 | Muestras tipográficas | Texto en cada fuente | §5.2 Fonts |

### 8.3 Videos / Animaciones (deseables)

| # | Asset | Formato | Duración | Uso |
|---|-------|---------|----------|-----|
| 1 | Reordering drag-and-drop | WebM / GIF | 2-3s | §1.2 |
| 2 | Conscious Pause flujo completo | WebM | 4-5s | §4.1 |
| 3 | Transición entre perfiles | WebM | 1-2s | §3 |

> **[ES]** **Notas de producción:**
> - Todos los screenshots deben exportarse en **WebP con fallback JPG**.
> - Ancho máximo: `800px`.
> - `border-radius: 16px` en todas las imágenes.
> - **Sin sombras.** Ninguna box-shadow (DESIGN_GUIDE.md §6.3).
> - Lazy loading en todo asset bajo el fold.
> - Alt text descriptivo y funcional en cada imagen.

---

## 9. Tokens de Movimiento por Feature

> **Referencia rápida para implementadores.** Todos los valores derivados de `DESIGN_GUIDE.md` §8.

| Feature | Animación | Duración | Easing | Notas |
|---------|-----------|----------|--------|-------|
| Reordering | Drag + reacomodo | `duration-base` (200ms) | `ease-default` | Sin rebote. Reacomodo suave de items. |
| Conscious Pause entrada | Fade in | `duration-slow` (300ms) | `ease-out` | Opacity 0→1. No scale. |
| Conscious Pause countdown | Cambio de número | 1s (fijo) | N/A | Cambio directo o crossfade mínimo. |
| Conscious Pause salida | Fade out | `duration-fast` (150ms) | `ease-default` | Opacity 1→0. |
| Profile switching | Cambio de estado | `duration-base` (200ms) | `ease-default` | Transición de indicador visual. |
| Card hover | Elevación sutil | `duration-fast` (150ms) | `ease-default` | `translateY(-2px)` + border-color. |
| Scroll reveal | Entrada suave | `duration-slow` (300ms) | `ease-out` | Opacity 0→1, translateY(16px)→0. Stagger 50ms. |
| Nav sticky | Backdrop blur | `duration-fast` (150ms) | `ease-default` | `backdrop-filter: blur(12px)` al hacer scroll. |

> **[ES]** **Importante:** Todas las animaciones deben respetar `prefers-reduced-motion: reduce` (DESIGN_GUIDE.md §8.4).

---

## 10. Checklist de Implementación para Frontend

Antes de dar por completada la página `/features`, verificar:

- [ ] Todos los screenshots de la §8.1 están producidos y comprimidos (WebP + fallback).
- [ ] Las imágenes tienen `border-radius: 16px` y **ninguna** box-shadow.
- [ ] El tema Slate + Digital está presente en el hero y como primer elemento de cada grid.
- [ ] Las cards de themes usan hover consistente (`translateY(-2px)`, border-color).
- [ ] Los videos/GIFs (si existen) tienen `playsinline`, `muted`, `loop`, y controles ocultos.
- [ ] Los alt text describen funcionalidad, no solo "screenshot".
- [ ] Las secciones alternan fondos según `DESIGN_GUIDE.md` §9.2 (surface-primary → surface-elevated → surface-primary...).
- [ ] La sección de perfiles (§3) usa fondo diferenciado (`--accent-muted` Warm Beige) para crear ritmo.
- [ ] Los headings siguen el heading map de `SEO_AND_KEYWORDS.md` §5.2.
- [ ] Todas las animaciones respetan `prefers-reduced-motion`.
- [ ] Los enlaces internos usan `→`; los externos usan `↗` y `target="_blank"`.

---

> **Nota para desarrolladores y diseñadores:**  
> Este documento es el contrato visual y de contenido para `/features`. Si un asset, una animación o un texto no está aquí, no es requisito para el MVP de la página. Cualquier adición debe ser aprobada y documentada en este archivo.
