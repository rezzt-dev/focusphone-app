# FocusPhone — DESIGN_GUIDE.md

> **Versión:** 1.0.0  
> **Estado:** Aprobado para desarrollo  
> **Última revisión:** 2026-05-15  
> **Propietario:** Diseño + Frontend  

---

## 1. Propósito y Alcance

Este documento es la **constitución visual** del sitio web de FocusPhone. Define cómo se ve, cómo se siente y cómo suena la web antes de que se escriba una sola línea de CSS.

**Regla de oro:** la web no es un folleto de marketing. Es una extensión del launcher. Si un visitante cierra los ojos y solo lee el copy, debe sentir que ya está usando la app.

---

## 2. Filosofía de Diseño

### 2.1 Principios Fundamentales

| Principio | Definición | Aplicación práctica |
|-----------|------------|---------------------|
| **Restricción elegante** | Limitar opciones para maximizar claridad. | Máximo 2 familias tipográficas. Máximo 4 colores por página. Sin degradados complejos. |
| **Respiración** | El espacio en blanco es contenido, no ausencia. | Márgenes generosos. Líneas de texto cortas (60-75 caracteres). Padding mínimo de 64px entre secciones. |
| **Honestidad material** | Sin sombras falsas, sin glassmorphism, sin efectos 3D. | Superficies planas. Bordes redondeados sutiles. Transparencia solo cuando aporta legibilidad. |
| **Bajo contraste calmado** | Evitar el choque visual. Transiciones suaves entre fondo y texto. | No usar negro puro `#000000` ni blanco puro `#FFFFFF` como fondos principales. |

### 2.2 Atributos de Marca (Brand Attributes)

```
calm    ──────────────────────────────  intentional
  │                                          │
  │      ┌─────────────────────┐            │
  │      │                     │            │
  │      │    ○ focusphone     │            │
  │      │                     │            │
  │      └─────────────────────┘            │
  │                                          │
quiet   ──────────────────────────────  trustworthy
```

- **Calm:** colores apagados, animaciones lentas, ausencia de elementos parpadeantes.
- **Intentional:** cada elemento tiene una razón de existir. No hay adornos.
- **Quiet:** la web no grita. susurra.
- **Trustworthy:** información clara, estructura predecible, legal visible.

---

## 3. Paleta de Colores Web

### 3.1 Sistema de Tokens

La paleta web deriva directamente de los 4 temas de la app. Se define como **tokens semánticos** para permitir un futuro modo oscuro/claro sin rediseñar componentes.

#### Tokens Base (siempre presentes)

| Token | Valor Hex | Uso | Nota |
|-------|-----------|-----|------|
| `--base-ink` | `#181818` | Texto principal en fondos claros | No es negro puro. Más suave. |
| `--base-paper` | `#FAFAFA` | Fondo principal claro | No es blanco puro. Evita fatiga. |
| `--base-void` | `#0F0F12` | Fondo principal oscuro (modo oscuro) | Azulado muy sutil. |
| `--base-mist` | `#E8E6E1` | Fondo secundario claro | Warm beige atenuado. |
| `--base-ash` | `#2A2A2E` | Superficie elevada oscura | Cards, nav sticky en dark. |

#### Tokens de Tema (los 4 temas de la app, disponibles como acentos)

| Tema | `--accent` | `--accent-hover` | `--accent-muted` | `--accent-contrast` | Personalidad |
|------|------------|------------------|------------------|---------------------|--------------|
| **Slate** | `#4A5568` | `#3A4556` | `#E2E8F0` | `#FFFFFF` | Neutral, profesional, default |
| **Navy** | `#2C3E50` | `#1F2D3D` | `#D6E4F0` | `#FFFFFF` | Profundo, enfocado, work |
| **Warm Beige** | `#C4A882` | `#B08F6E` | `#F2E8D5` | `#181818` | Cálido, orgánico, detox |
| **Off-White** | `#F5F5F0` | `#EAEAE5` | `#FFFFFF` | `#181818` | Etereo, minimalista extremo |

> **Regla de implementación:** el tema por defecto de la web es **Slate**. Los demás temas se usan como variantes de acento (ej. Warm Beige para la sección Detox en `/features`, Navy para la sección Work).

#### Tokens Funcionales

| Token | Valor (modo claro) | Valor (modo oscuro) | Uso |
|-------|--------------------|---------------------|-----|
| `--text-primary` | `#181818` | `#E8E6E1` | Headings, párrafos principales |
| `--text-secondary` | `#5A5A5A` | `#9A9A9E` | Subtítulos, meta-información |
| `--text-tertiary` | `#8A8A8A` | `#6A6A6E` | Fechas, captions, labels |
| `--text-inverse` | `#FAFAFA` | `#0F0F12` | Texto sobre acentos |
| `--surface-primary` | `#FAFAFA` | `#0F0F12` | Fondo de página |
| `--surface-elevated` | `#FFFFFF` | `#1E1E22` | Cards, nav sticky, dropdowns |
| `--surface-hover` | `#F0F0EC` | `#25252A` | Estado hover de filas/listas |
| `--border-subtle` | `#E0E0DC` | `#333338` | Líneas divisorias, bordes de cards |
| `--border-focus` | `#4A5568` | `#8A9AAF` | Outline de foco (a11y) |
| `--success` | `#5A8C6A` | `#6AAF7A` | Confirmaciones, checks |
| `--warning` | `#B89A4A` | `#D4B45A` | Alertas suaves |
| `--error` | `#A05050` | `#C07070` | Errores, validaciones |

### 3.2 Reglas de Aplicación de Color

```
┌─────────────────────────────────────────────┐
│  SURFACE PRIMARY (#FAFAFA)                  │
│                                             │
│  ┌─────────────────────────────────────┐    │
│  │ SURFACE ELEVATED (#FFFFFF)          │    │
│  │                                     │    │
│  │  Text Primary (#181818)             │    │
│  │  Text Secondary (#5A5A5A)           │    │
│  │                                     │    │
│  │  [Accent Button] ── Accent (#4A5568)│    │
│  │                                     │    │
│  └─────────────────────────────────────┘    │
│                                             │
│  Border Subtle (#E0E0DC) ─────────────────  │
│                                             │
└─────────────────────────────────────────────┘
```

- **Fondo de página:** siempre `--surface-primary`.
- **Cards / secciones destacadas:** `--surface-elevated` con `border: 1px solid var(--border-subtle)`.
- **Texto sobre fondo claro:** `--text-primary`. Nunca usar `--base-ink` sobre `--surface-elevated` sin verificar contraste.
- **Botones primarios:** `--accent` con `--text-inverse`.
- **Botones secundarios:** transparente con `border: 1px solid var(--border-subtle)` y `--text-primary`.
- **Hover sobre botón primario:** `--accent-hover`.
- **Links:** `--text-primary` con `text-decoration: underline` en hover. Sin color azul por defecto (rompe la paleta).

### 3.3 Modo Oscuro (Dark Mode)

El sitio **debe** soportar modo oscuro vía `prefers-color-scheme: dark`. No hay toggle manual (se alinea con la filosofía de "sin opciones innecesarias").

| Escenario | Fondo | Texto |
|-----------|-------|-------|
| Default | `--base-paper` / `--base-void` | `--text-primary` |
| Hero con tema Navy | `--accent` (Navy) | `--text-inverse` |
| Feature Detox | `--accent-muted` (Warm Beige) | `--base-ink` |

---

## 4. Tipografía Web

### 4.1 Familias Tipográficas

La app usa fuentes descargables (Poppins, Comfortaa, Figtree, JetBrains Mono, Space Mono). En web, se mantiene la misma filosofía pero con fallbacks web-safe para rendimiento.

| Rol | Familia principal | Fallbacks | Uso |
|-----|-------------------|-----------|-----|
| **Display / Headings** | `Poppins` | `system-ui, -apple-system, sans-serif` | H1, H2, números grandes, CTAs |
| **Body / UI** | `Figtree` | `Inter, Roboto, system-ui, sans-serif` | Párrafos, labels, navegación |
| **Monospace** | `JetBrains Mono` | `ui-monospace, SFMono-Regular, monospace` | Código, versiones, hashes |

> **Regla:** Poppins se carga con `font-weight: 400, 500, 600`. Figtree con `400, 500`. JetBrains Mono con `400`. No cargar más pesos de los necesarios.

### 4.2 Escala Tipográfica (Type Scale)

Base: `16px` (1rem)  
Ratio: `1.25` (Major Third)  
Line-height base: `1.6`

| Token | Tamaño | Line-height | Weight | Tracking | Uso |
|-------|--------|-------------|--------|----------|-----|
| `text-hero` | `clamp(2.5rem, 5vw, 4rem)` | `1.1` | 500 | `-0.02em` | H1 de landing |
| `text-h1` | `2.5rem` (40px) | `1.15` | 500 | `-0.01em` | H1 de páginas interiores |
| `text-h2` | `2rem` (32px) | `1.2` | 500 | `-0.01em` | H2 de secciones |
| `text-h3` | `1.5rem` (24px) | `1.3` | 500 | `0` | H3 de features, cards |
| `text-h4` | `1.25rem` (20px) | `1.35` | 500 | `0` | Subtítulos, labels de grupo |
| `text-body` | `1rem` (16px) | `1.6` | 400 | `0` | Párrafos, descripciones |
| `text-body-sm` | `0.875rem` (14px) | `1.5` | 400 | `0` | Captions, meta, footer |
| `text-caption` | `0.75rem` (12px) | `1.4` | 400 | `0.01em` | Fechas, tags, disclaimers |
| `text-mono` | `0.875rem` (14px) | `1.5` | 400 | `0` | Versiones, hashes, código |

### 4.3 Reglas Tipográficas Específicas

#### Minúsculas estructurales

La UI de la app usa minúsculas por filosofía. La web respeta eso en elementos interactivos y cortos:

| Contexto | Caso | Ejemplo |
|----------|------|---------|
| CTAs y botones | minúsculas | `download the apk` |
| Navegación | minúsculas | `features · download · faq` |
| Headings H1-H2 | minúsculas | `your phone, intentionally.` |
| Headings H3-H4 | minúsculas | `text-only favourites` |
| Párrafos body | minúsculas | `focusphone does not collect...` |
| Proper nouns | capitalizado | `FocusPhone`, `GitHub`, `Android` |
| Acrónimos técnicos | mayúsculas | `APK`, `SHA-256`, `MIT`, `GDPR` |

> **Excepción SEO:** el `<title>` y `<meta name="description">` pueden usar mayúsculas iniciales por optimización de CTR en Google.

#### Espaciado entre letras (Tracking)

- Headings grandes (hero, h1): `-0.02em` a `-0.01em` (más denso, más elegante).
- Body: `0` (tracking normal).
- Caption y labels pequeños: `0.01em` a `0.02em` (más aire para legibilidad).

---

## 5. Espaciado y Sistema de Grid

### 5.1 Escala de Espaciado (8-Point Grid)

Todo el espaciado se basa en múltiplos de `8px`.

| Token | Valor | Uso típico |
|-------|-------|------------|
| `space-1` | `4px` | Micro-ajustes, icono + texto |
| `space-2` | `8px` | Padding interno de badges, gaps pequeños |
| `space-3` | `16px` | Padding de botones, gap entre label e input |
| `space-4` | `24px` | Padding de cards, gap entre elementos de lista |
| `space-5` | `32px` | Separación entre subsecciones |
| `space-6` | `48px` | Padding vertical de secciones medianas |
| `space-7` | `64px` | Separación entre secciones principales |
| `space-8` | `96px` | Padding vertical de secciones hero |
| `space-9` | `128px` | Separación máxima, landing page |

### 5.2 Breakpoints

| Nombre | Ancho | Adaptación principal |
|--------|-------|---------------------|
| `sm` | `640px` | Móvil grande. Single column. Nav hamburguesa. |
| `md` | `768px` | Tablet. Grid 2 columnas. Nav parcialmente expandida. |
| `lg` | `1024px` | Desktop pequeño. Grid 3 columnas. Nav completa. |
| `xl` | `1280px` | Desktop estándar. Contenedor max-width activo. |
| `2xl` | `1536px` | Desktop grande. Más aire lateral. |

### 5.3 Contenedor y Márgenes

```css
.container {
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
  padding-left: clamp(1rem, 5vw, 3rem);
  padding-right: clamp(1rem, 5vw, 3rem);
}
```

- **Máximo ancho de contenido:** `1200px` (72rem aprox).
- **Márgenes laterales:** fluidos entre `16px` (móvil) y `48px` (desktop grande).
- **Ancho de lectura óptimo:** `65ch` para párrafos de texto largo (privacy, terms, faq).

---

## 6. Componentes UI

### 6.1 Botones

#### Primario

```
┌────────────────────────────┐
│  download the apk          │
└────────────────────────────┘
```

- **Height:** `48px` (mínimo área táctil)
- **Padding horizontal:** `24px`
- **Border-radius:** `12px` (sutil, no pill)
- **Background:** `var(--accent)`
- **Color:** `var(--text-inverse)`
- **Font:** `Poppins 500, 1rem`
- **Text-transform:** lowercase
- **Hover:** `background: var(--accent-hover); transform: translateY(-1px)`
- **Active:** `transform: translateY(0)`
- **Focus:** `outline: 2px solid var(--border-focus); outline-offset: 2px`
- **Transition:** `all 200ms cubic-bezier(0.4, 0, 0.2, 1)`

#### Secundario (Outline)

```
┌────────────────────────────┐
│  view all releases →       │
└────────────────────────────┘
```

- **Height:** `48px`
- **Padding horizontal:** `24px`
- **Border-radius:** `12px`
- **Background:** transparent
- **Border:** `1px solid var(--border-subtle)`
- **Color:** `var(--text-primary)`
- **Hover:** `background: var(--surface-hover); border-color: var(--text-tertiary)`

#### Terciario (Enlace)

```
read our privacy policy →
```

- **Color:** `var(--text-secondary)`
- **Text-decoration:** none (base), `underline` (hover)
- **Hover:** `color: var(--text-primary)`
- **Flecha:** `→` o `↗` (para externos), sin animación exagerada. Solo un `translateX(2px)` en hover si se usa icono.

### 6.2 Navegación

#### Nav Sticky

```
┌─────────────────────────────────────────────────────────┐
│ [logo] focusphone        features  download  faq   [≡]│
└─────────────────────────────────────────────────────────┘
  ↑ fondo: var(--surface-elevated) con backdrop-filter: blur(12px)
  ↑ border-bottom: 1px solid var(--border-subtle)
  ↑ height: 64px
  ↑ z-index: 50
```

- **Backdrop:** `backdrop-filter: blur(12px); background: rgba(var(--surface-elevated-rgb), 0.85)`
- **Links:** `text-body-sm`, `Poppins 500`, lowercase.
- **Link activo:** `color: var(--text-primary)` con una línea inferior de `2px` en `var(--accent)`.
- **Link inactivo:** `color: var(--text-secondary)`.
- **Hover de link:** `color: var(--text-primary)`.

#### Footer

- **Background:** `var(--surface-primary)`
- **Border-top:** `1px solid var(--border-subtle)`
- **Padding vertical:** `64px` arriba, `32px` abajo.
- **Layout:** 3 columnas en desktop, 1 columna apilada en móvil.
- **Links:** `text-body-sm`, `color: var(--text-secondary)`, hover `var(--text-primary)`.

### 6.3 Cards y Secciones Destacadas

```
┌────────────────────────────────────┐
│                                    │
│  [screenshot / asset]              │
│                                    │
├────────────────────────────────────┤
│  conscious pause                   │
│  a configurable countdown...       │
│                                    │
└────────────────────────────────────┘
```

- **Background:** `var(--surface-elevated)`
- **Border:** `1px solid var(--border-subtle)`
- **Border-radius:** `16px`
- **Padding interno:** `24px` o `32px`
- **Sombra:** **NINGUNA.** No se usan box-shadows. La separación se logra con el borde sutil y el fondo diferenciado.

### 6.4 Acordeón (FAQ)

```
┌─────────────────────────────────────────┐
│  ▶ will i lose my apps?              │
└─────────────────────────────────────────┘
```

- **Item cerrado:** `border-bottom: 1px solid var(--border-subtle); padding: 20px 0`
- **Icono:** triángulo/chevron `▶` rotado `90deg` al abrir. Transición `200ms`.
- **Respuesta:** `padding-top: 12px; color: var(--text-secondary)`
- **Hover del item:** `background: var(--surface-hover)` (solo en desktop).

---

## 7. Iconografía

### 7.1 Estilo

- **Tipo:** Iconos lineales (outline), stroke `1.5px` a `2px`.
- **Esquina:** redondeada (`round-cap`, `round-join`).
- **Tamaño base:** `24px` (1.5rem).
- **Color:** `var(--text-secondary)` por defecto, `var(--text-primary)` en hover.

### 7.2 Iconos Requeridos (Inventario Mínimo)

| Icono | Uso | Ubicación |
|-------|-----|-----------|
| Flecha derecha | CTAs internos | Botones, enlaces |
| Flecha diagonal ↗ | Enlaces externos | GitHub, rezzt.dev |
| Menú hamburguesa (3 líneas) | Nav móvil | Header |
| X / Cerrar | Menú móvil expandido, modales | Overlay |
| Chevron | Acordeón FAQ | Lista de preguntas |
| Reloj (abstracto) | Feature clocks | `/features` |
| Escudo / Candado | Privacy, security | `/privacy`, `/features#security` |
| Descarga | Download | `/download` |
| Estrella / Tag | Changelog, versiones | `/changelog` |
| GitHub logo | Enlace a repo | Footer, nav |

> **Fuente recomendada:** [Lucide](https://lucide.dev/) (librería SVG iconos lineales, coherente con el estilo minimalista). Alternativa: [Heroicons](https://heroicons.com/) (outline style).

---

## 8. Movimiento y Microinteracciones

### 8.1 Filosofía del Movimiento

> "El movimiento debe sentirse como respirar, no como anunciar."

- **Propósito:** guiar la atención, confirmar acciones, suavizar transiciones.
- **Prohibido:** animaciones que bloqueen el scroll, parpadeos, rebotes exagerados, animaciones de entrada forzadas en cada elemento.

### 8.2 Tokens de Movimiento

| Token | Valor | Uso |
|-------|-------|-----|
| `duration-fast` | `150ms` | Hover de botones, links, cambios de color |
| `duration-base` | `200ms` | Apertura de acordeón, toggle de menú |
| `duration-slow` | `300ms` | Cambios de página, scroll reveal suave |
| `ease-default` | `cubic-bezier(0.4, 0, 0.2, 1)` | La mayoría de transiciones |
| `ease-out` | `cubic-bezier(0, 0, 0.2, 1)` | Elementos que entran al viewport |

### 8.3 Patrones Específicos

#### Scroll Reveal (Entrada suave)

- **Trigger:** elemento entra un 20% al viewport.
- **Efecto:** `opacity: 0 → 1`, `translateY(16px) → translateY(0)`.
- **Duración:** `duration-slow`.
- **Stagger:** `50ms` entre elementos hermanos (grids de features).
- **Regla:** solo una vez. Al hacer scroll hacia arriba, los elementos permanecen visibles.

#### Hover de Tarjetas

- **Efecto:** `translateY(-2px)`, `border-color: var(--text-tertiary)`.
- **Duración:** `duration-fast`.
- **Sin sombra.** Nunca box-shadow.

#### Transición de Página

- **Tipo:** fade simple.
- **Implementación:** `opacity: 0 → 1` en `body` o contenedor principal al cargar.
- **Duración:** `200ms`.

### 8.4 `prefers-reduced-motion`

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

Todas las animaciones deben respetar esta media query.

---

## 9. Layout y Composición

### 9.1 Estructura de Página Tipo

```
┌──────────────────────────────────────────────┐
│ NAV (sticky, 64px)                           │
├──────────────────────────────────────────────┤
│                                              │
│  HERO                                        │
│  ┌──────────────────────────────────────┐    │
│  │ H1                                   │    │
│  │ Subtítulo                            │    │
│  │ [CTA] [CTA secundario]               │    │
│  └──────────────────────────────────────┘    │
│                                              │
├──────────────────────────────────────────────┤
│                                              │
│  SECCIÓN A (alternancia de fondo)            │
│  ┌──────────┐  ┌──────────────────────┐      │
│  │ Imagen   │  │ H2 + texto           │      │
│  │          │  │                      │      │
│  └──────────┘  └──────────────────────┘      │
│                                              │
├──────────────────────────────────────────────┤
│                                              │
│  SECCIÓN B (fondo diferente o card grid)     │
│  ┌────────────┐ ┌────────────┐               │
│  │ Card 1     │ │ Card 2     │               │
│  └────────────┘ └────────────┘               │
│                                              │
├──────────────────────────────────────────────┤
│                                              │
│  CTA BAND (fondo accent-muted)               │
│  ┌──────────────────────────────────────┐    │
│  │ H2 + CTA primario grande             │    │
│  └──────────────────────────────────────┘    │
│                                              │
├──────────────────────────────────────────────┤
│ FOOTER                                       │
└──────────────────────────────────────────────┘
```

### 9.2 Alternancia de Fondos

Para crear ritmo visual sin usar bordes excesivos:

- Sección 1: `var(--surface-primary)`
- Sección 2: `var(--surface-elevated)`
- Sección 3: `var(--surface-primary)`
- Sección 4: `var(--accent-muted)` (solo para bandas de CTA o features destacadas)

### 9.3 Imágenes y Assets

- **Bordes:** todas las screenshots deben tener `border-radius: 16px`.
- **Sombras:** **ninguna**. La separación se logra con el fondo de la sección.
- **Formato:** WebP con fallback JPG. Screenshots en `800px` de ancho máximo.
- **Lazy loading:** `loading="lazy"` en todas las imágenes bajo el fold.
- **Alt text:** descriptivo y funcional. Ej: "screenshot of focusphone home screen with slate theme and digital clock".

---

## 10. Moodboard y Referencias Visuales

### 10.1 Referencias de Estilo

| Referencia | Qué tomar | Qué evitar |
|------------|-----------|------------|
| **Apple Human Interface** | Claridad, jerarquía tipográfica, aire | Exceso de blanco puro, minimalismo corporativo frío |
| **Linear.app** | Uso del espacio, tipografía Poppins-like, calma | Interacciones complejas, modo oscuro forzado |
| **Muji (sin marca)** | Calidez en la restricción, beige, honestidad material | Contexto físico (no vendemos muebles) |
| **Notion (landing antigua)** | Simplicidad de copy, estructura de secciones | Densidad de información, demasiados CTAs |

### 10.2 Descripción del Mood Visual

> Imagina una habitación a las 7:00 AM. Luz suave, paredes blancas cálidas, una mesa de madera clara. Sobre la mesa hay un teléfono con una pantalla que solo dice "messages · maps · music" y un reloj discreto. No hay notificaciones parpadeando. No hay banners. Solo intención.

**Eso es FocusPhone. Eso debe ser la web.**

### 10.3 Keywords Visuales

```
limpio        ←────→    cálido
  │                        │
  └──→  quiet confidence  ←──┘
  │                        │
texturizado   ←────→    plano
(solo en beige)
```

---

## 11. Accesibilidad (a11y)

### 11.1 Contraste

| Combinación | Ratio mínimo | Estado |
|-------------|--------------|--------|
| `--text-primary` sobre `--surface-primary` | 7:1 (AAA) | ✅ `#181818` / `#FAFAFA` = ~15.8:1 |
| `--text-secondary` sobre `--surface-primary` | 4.5:1 (AA) | ✅ `#5A5A5A` / `#FAFAFA` = ~6.9:1 |
| `--text-inverse` sobre `--accent` (Slate) | 4.5:1 (AA) | ✅ `#FAFAFA` / `#4A5568` = ~5.5:1 |
| `--text-inverse` sobre `--accent` (Beige) | 4.5:1 (AA) | ✅ `#181818` / `#C4A882` = ~5.2:1 |

### 11.2 Focus Visible

- **Outline:** `2px solid var(--border-focus)`
- **Offset:** `2px`
- **Border-radius:** heredado del elemento.
- **Nunca** usar `outline: none` sin reemplazo.

### 11.3 Tamaños Táctiles

- **Mínimo:** `44px × 44px` para cualquier elemento interactivo.
- **Objetivo:** `48px × 48px` (lo usamos en botones).

### 11.4 Tipografía Accesible

- **Tamaño mínimo de fuente:** `16px` para inputs (evita zoom en iOS Safari).
- **Line-height mínimo:** `1.5` para párrafos.
- **Ancho máximo de línea:** `75ch`.

---

## 12. Tokens de Handoff para Desarrolladores

### 12.1 Formato de Entrega

Los tokens se entregan en formato **CSS custom properties** como fuente única de verdad. El proyecto puede consumirlos directamente o transformarlos a Sass/LESS/JS vía script.

```css
/* filepath: tokens.css */
:root {
  /* Base */
  --base-ink: #181818;
  --base-paper: #FAFAFA;
  --base-void: #0F0F12;
  --base-mist: #E8E6E1;
  --base-ash: #2A2A2E;

  /* Accent (Slate default) */
  --accent: #4A5568;
  --accent-hover: #3A4556;
  --accent-muted: #E2E8F0;
  --accent-contrast: #FFFFFF;

  /* Text */
  --text-primary: #181818;
  --text-secondary: #5A5A5A;
  --text-tertiary: #8A8A8A;
  --text-inverse: #FAFAFA;

  /* Surface */
  --surface-primary: #FAFAFA;
  --surface-elevated: #FFFFFF;
  --surface-hover: #F0F0EC;

  /* Border */
  --border-subtle: #E0E0DC;
  --border-focus: #4A5568;

  /* Functional */
  --success: #5A8C6A;
  --warning: #B89A4A;
  --error: #A05050;

  /* Spacing */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 16px;
  --space-4: 24px;
  --space-5: 32px;
  --space-6: 48px;
  --space-7: 64px;
  --space-8: 96px;
  --space-9: 128px;

  /* Typography */
  --font-display: 'Poppins', system-ui, -apple-system, sans-serif;
  --font-body: 'Figtree', 'Inter', Roboto, system-ui, sans-serif;
  --font-mono: 'JetBrains Mono', ui-monospace, SFMono-Regular, monospace;

  /* Motion */
  --duration-fast: 150ms;
  --duration-base: 200ms;
  --duration-slow: 300ms;
  --ease-default: cubic-bezier(0.4, 0, 0.2, 1);
}

@media (prefers-color-scheme: dark) {
  :root {
    --text-primary: #E8E6E1;
    --text-secondary: #9A9A9E;
    --text-tertiary: #6A6A6E;
    --text-inverse: #0F0F12;
    --surface-primary: #0F0F12;
    --surface-elevated: #1E1E22;
    --surface-hover: #25252A;
    --border-subtle: #333338;
    --border-focus: #8A9AAF;
    --accent-muted: #2A3038;
    --success: #6AAF7A;
    --warning: #D4B45A;
    --error: #C07070;
  }
}
```

### 12.2 Checklist de Implementación para Frontend

Antes de considerar la web "lista visualmente", verificar:

- [ ] El modo oscuro funciona sin clases `.dark` manuales (usa `prefers-color-scheme`).
- [ ] Todos los textos interactivos están en minúsculas (excepto proper nouns).
- [ ] No hay box-shadows en ningún componente.
- [ ] No se usa `#000000` ni `#FFFFFF` como fondo principal.
- [ ] Los botones tienen `min-height: 48px`.
- [ ] Los enlaces externos muestran `↗` y abren en nueva pestaña.
- [ ] El focus visible está implementado en todos los elementos interactivos.
- [ ] Las imágenes tienen `border-radius: 16px`.
- [ ] El nav sticky tiene `backdrop-filter: blur(12px)`.
- [ ] Las animaciones respetan `prefers-reduced-motion`.

---

## 13. Historial de Cambios de este Documento

| Versión | Fecha | Autor | Cambios |
|---------|-------|-------|---------|
| 1.0.0 | 2026-05-15 | rezzt.dev | Guía visual inicial. Tokens semánticos completos, sistema 8-point, componentes definidos, modo oscuro integrado, a11y incluida. |

---

> **Nota para desarrolladores:**  
> Este documento es la referencia visual definitiva. Si un color, una fuente o un espaciado no está aquí, no existe en la web de FocusPhone. Cualquier excepción debe ser aprobada y documentada en este archivo.
