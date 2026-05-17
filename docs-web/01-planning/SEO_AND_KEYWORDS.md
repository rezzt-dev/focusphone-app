# FocusPhone — SEO_AND_KEYWORDS.md

> **Versión:** 1.0.0  
> **Estado:** Aprobado para desarrollo  
> **Última revisión:** 2026-05-15  
> **Propietario:** Contenido + Frontend + Growth  

---

## 1. Propósito y Alcance

Este documento es el **plan de batalla para el tráfico orgánico** de FocusPhone. Sin presupuesto de ads, cada visita debe ser conquistada mediante posicionamiento en buscadores, optimización de compartición social y una arquitectura de contenido que responda exactamente a lo que busca el usuario.

**Regla de oro:** no escribimos contenido y luego "lo optimizamos para SEO". Escribimos contenido SEO desde la primera palabra, porque eso es escribir para el usuario que tiene una intención de búsqueda específica.

---

## 2. Análisis de Contexto y Posicionamiento

### 2.1 Realidad del Mercado

| Factor | Implicación SEO |
|--------|-----------------|
| Producto indie, sin presupuesto de ads | 100% dependiente de tráfico orgánico y referral (GitHub, Reddit, HN). |
| APK fuera de Google Play | Menor autoridad de dominio inicial. Mayor necesidad de confianza vía contenido. |
| Nicho muy específico (minimalist launcher) | Volumen de búsqueda bajo-medio, pero intención de conversión muy alta. |
| Competencia establecida | Niagara, OLauncher, Before Launcher, Ratio, Blank. Necesitamos diferenciarnos por features, no por genéricos. |
| Open source (MIT) | GitHub como fuente de autoridad. Backlinks naturales de listados FOSS. |

### 2.2 Ventajas Diferenciadoras a Potenciar

1. **Conscious Pause** — ningún competidor mayor tiene este feature con este nombre.
2. **Perfiles contextuales (Normal / Work / Detox)** — concepto claro y buscable.
3. **100% offline / sin tracking** — ventaja enorme en una era de preocupación por privacidad.
4. **Open source** — atrae búsquedas de "open source android launcher".
5. **Text-only home screen** — diferenciador visual instantáneo.

---

## 3. Keyword Research

### 3.1 Keywords Principales (Head Terms)

Volumen estimado alto, competencia media-alta. El objetivo es posicionar la landing (`/`) y `/features`.

| Keyword | Volumen Est.* | Dificultad | Página Objetivo | Intención |
|---------|---------------|------------|-----------------|-----------|
| `minimalist android launcher` | Alto | Media | `/` (landing) | Investigación + Comparación |
| `distraction free launcher` | Medio | Media | `/` | Investigación |
| `simple android launcher` | Alto | Alta | `/` | Investigación |
| `best minimalist launcher android` | Medio-Alto | Alta | `/` | Comparación |
| `digital detox app` | Alto | Media-Alta | `/features` | Investigación |
| `open source android launcher` | Medio | Media | `/` + GitHub | Investigación |
| `android launcher no icons` | Bajo-Medio | Baja | `/features` | Investigación + Solución |

> *Volumen estimado relativo (Alto/Medio/Bajo) basado en tendencias del nicho launcher Android 2025-2026.

### 3.2 Keywords Secundarias (Body Terms)

Más específicas, menor volumen individual, pero suman tráfico cualificado.

| Keyword | Página Objetivo | Intención |
|---------|-----------------|-----------|
| `text only launcher android` | `/features` | Solución |
| `launcher with breathing timer` | `/features` | Solución |
| `work profile launcher android` | `/features` | Solución |
| `detox mode app android` | `/features` | Solución |
| `launcher custom fonts android` | `/features` | Solución |
| `android launcher privacy focused` | `/` + `/privacy` | Investigación + Confianza |
| `launcher without ads android` | `/` | Comparación |
| `minimalist phone interface` | `/` | Investigación |
| `focus phone android` | `/` | Marca + Investigación |

### 3.3 Keywords de Larga Cola (Long-Tail)

Bajo volumen, muy alta intención, baja competencia. Objetivo: `/faq`, `/download`, `/changelog`.

| Keyword | Página Objetivo | Intención |
|---------|-----------------|-----------|
| `how to reduce screen time android launcher` | `/features` + `/faq` | Solución |
| `launcher that blocks apps during work` | `/features` | Solución |
| `how to uninstall focusphone launcher` | `/faq` | Soporte |
| `focusphone apk download` | `/download` | Transacción |
| `minimalist launcher apk download` | `/download` | Transacción |
| `android launcher no google play` | `/download` | Transacción |
| `does focusphone collect data` | `/privacy` | Confianza |
| `open source launcher with themes` | `/features` | Comparación |
| `best launcher for digital detox 2026` | `/` | Comparación |
| `how to set default launcher android` | `/faq` + `/download` | Soporte |

### 3.4 Keywords de Marca

| Keyword | Página Objetivo | Nota |
|---------|-----------------|------|
| `focusphone` | `/` | Dominar la SERP para el nombre propio. |
| `focusphone launcher` | `/` | Variante de marca. |
| `focusphone app` | `/` | Variante de marca. |
| `focusphone download` | `/download` | Intención transaccional directa. |
| `focusphone github` | GitHub repo | Referral desde búsqueda de marca. |

---

## 4. Meta Tags por Página

### 4.1 Meta Tags Generales (Todas las Páginas)

```html
<!-- Charset y viewport -->
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">

<!-- Robots -->
<meta name="robots" content="index, follow, max-image-preview:large">

<!-- Theme Color -->
<meta name="theme-color" content="#FAFAFA" media="(prefers-color-scheme: light)">
<meta name="theme-color" content="#0F0F12" media="(prefers-color-scheme: dark)">

<!-- Canonical (por página, ver sección 4.2) -->
<link rel="canonical" href="https://focusphone.app/PAGE">

<!-- Favicon -->
<link rel="icon" type="image/svg+xml" href="/favicon.svg">
<link rel="apple-touch-icon" href="/apple-touch-icon.png">
```

### 4.2 Meta Tags Específicos por Página

#### Landing Page — `/`

```html
<title>FocusPhone — Minimalist Android Launcher for Intentional Screen Time</title>
<meta name="description" content="Replace distraction with intention. FocusPhone is a text-only Android launcher with profiles, conscious pause, and premium themes. Android 7.0+. Free and open source.">

<!-- Open Graph -->
<meta property="og:type" content="website">
<meta property="og:url" content="https://focusphone.app/">
<meta property="og:title" content="FocusPhone — Minimalist Android Launcher">
<meta property="og:description" content="A text-only launcher designed to reduce digital noise. No icons. No feeds. Just what you choose.">
<meta property="og:image" content="https://focusphone.app/og-home.jpg">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:site_name" content="FocusPhone">

<!-- Twitter Card -->
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="FocusPhone — Minimalist Android Launcher">
<meta name="twitter:description" content="A text-only launcher designed to reduce digital noise. No icons. No feeds. Just what you choose.">
<meta name="twitter:image" content="https://focusphone.app/og-home.jpg">
```

> **Nota sobre OG/Twitter:** la imagen debe ser `1200×630px`, fondo `var(--base-paper)` o `var(--accent-muted)`, con el logo de FocusPhone, el tagline y un screenshot del home. Sin texto excesivo (máximo 20% de la imagen).

#### Features — `/features`

```html
<title>Features — FocusPhone, the Minimalist Android Launcher</title>
<meta name="description" content="Discover every feature: text-only home screen, conscious pause, detox profiles, custom themes, typography, and more. Built for intentional use.">

<meta property="og:type" content="article">
<meta property="og:url" content="https://focusphone.app/features">
<meta property="og:title" content="Features — FocusPhone">
<meta property="og:description" content="A complete breakdown of what FocusPhone does, why it matters, and how it looks.">
<meta property="og:image" content="https://focusphone.app/og-features.jpg">

<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="Features — FocusPhone">
<meta name="twitter:description" content="Conscious pause, detox mode, custom themes, and five clock styles. See every feature.">
<meta name="twitter:image" content="https://focusphone.app/og-features.jpg">
```

#### Download — `/download`

```html
<title>Download FocusPhone — Minimalist Launcher APK</title>
<meta name="description" content="Download the latest FocusPhone APK for Android 7.0+. Free, open source, and privacy-first. No Google Play required.">

<meta property="og:type" content="website">
<meta property="og:url" content="https://focusphone.app/download">
<meta property="og:title" content="Download FocusPhone APK">
<meta property="og:description" content="Get the latest version for Android 7.0+. No account required. No tracking.">
<meta property="og:image" content="https://focusphone.app/og-download.jpg">

<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="Download FocusPhone APK">
<meta name="twitter:description" content="Android 7.0+. Free. Open source. Privacy-first.">
<meta name="twitter:image" content="https://focusphone.app/og-download.jpg">
```

#### FAQ — `/faq`

```html
<title>FAQ — FocusPhone Launcher Questions & Answers</title>
<meta name="description" content="Common questions about installing, using, and uninstalling FocusPhone. Privacy, detox mode, launcher defaults, and more.">

<meta property="og:type" content="article">
<meta property="og:url" content="https://focusphone.app/faq">
<meta property="og:title" content="FAQ — FocusPhone">
<meta property="og:description" content="Everything you need to know before and after installing FocusPhone.">
<meta property="og:image" content="https://focusphone.app/og-faq.jpg">

<meta name="twitter:card" content="summary">
<meta name="twitter:title" content="FAQ — FocusPhone">
<meta name="twitter:description" content="Questions about installation, privacy, detox mode, and compatibility.">
```

> **Nota:** FAQ usa `summary` (imagen pequeña) porque es más informativa que promocional.

#### Changelog — `/changelog`

```html
<title>Changelog — FocusPhone Version History</title>
<meta name="description" content="See what's new in FocusPhone. Version history, improvements, bug fixes, and upcoming features.">

<meta property="og:type" content="article">
<meta property="og:url" content="https://focusphone.app/changelog">
<meta property="og:title" content="Changelog — FocusPhone">
<meta property="og:description" content="A transparent record of every release. Version 1.0.4 and beyond.">
<meta property="og:image" content="https://focusphone.app/og-changelog.jpg">

<meta name="twitter:card" content="summary">
<meta name="twitter:title" content="Changelog — FocusPhone">
<meta name="twitter:description" content="Transparent version history. See what's new.">
```

#### Privacy Policy — `/privacy`

```html
<title>Privacy Policy — FocusPhone</title>
<meta name="description" content="FocusPhone does not collect, transmit, or sell personal data. All information stays on your device. Read our full privacy policy.">

<meta property="og:type" content="article">
<meta property="og:url" content="https://focusphone.app/privacy">
<meta property="og:title" content="Privacy Policy — FocusPhone">
<meta property="og:description" content="No tracking. No servers. No ads. Your data never leaves your phone.">
<meta property="og:image" content="https://focusphone.app/og-privacy.jpg">

<meta name="twitter:card" content="summary">
<meta name="twitter:title" content="Privacy Policy — FocusPhone">
<meta name="twitter:description" content="No tracking. No servers. No ads. Your data never leaves your phone.">
```

#### Terms of Use — `/terms`

```html
<title>Terms of Use — FocusPhone</title>
<meta name="description" content="Terms and conditions for using FocusPhone. License, disclaimers, and user responsibilities.">

<meta property="og:type" content="article">
<meta property="og:url" content="https://focusphone.app/terms">
<meta property="og:title" content="Terms of Use — FocusPhone">
<meta property="og:description" content="MIT license. User responsibilities. Limitation of liability.">
<meta property="og:image" content="https://focusphone.app/og-default.jpg">

<meta name="twitter:card" content="summary">
<meta name="twitter:title" content="Terms of Use — FocusPhone">
<meta name="twitter:description" content="MIT license. User responsibilities.">
```

---

## 5. Estructura de Headings por Página

### 5.1 Reglas Generales

- **Un solo `<h1>` por página.** Siempre.
- **Jerarquía estricta:** no saltar de `<h2>` a `<h4>`.
- **Keywords en headings:** incluir el término principal de la página en el `<h1>`. Distribuir secundarias en `<h2>`.
- **Longitud:** `<h1>` máximo 60 caracteres. `<h2>` máximo 80.
- **Minúsculas estructurales:** respetar el DESIGN_GUIDE.md (lowercase excepto proper nouns).

### 5.2 Heading Map por Página

#### Landing Page (`/`)

```
h1: your phone, intentionally.
  h2: a text-only android launcher designed to reduce digital noise
  h2: three steps to a quieter phone
    h3: install & set as launcher
    h3: pick up to 10 favourite apps
    h3: choose a profile
  h2: built for focus
    h3: text-only home screen
    h3: conscious pause breathing timer
    h3: contextual profiles
    h3: premium themes & typography
  h2: looks like you
  h2: ready to reclaim your attention?
```

> **Keywords presentes:** `android launcher`, `text-only`, `reduce digital noise`, `quieter phone`, `focus`, `conscious pause`, `profiles`, `themes`, `typography`.

#### Features (`/features`)

```
h1: everything inside focusphone
  h2: a home screen with nothing to distract you
    h3: text-only favourites
    h3: five clock styles
  h2: three modes for three mindsets
    h3: normal profile
    h3: work profile
    h3: detox mode
  h2: breathe before you open
  h2: make it yours
    h3: four built-in themes
    h3: downloadable fonts
    h3: alignment & sizing
  h2: everything else, one swipe away
  h2: your data never leaves your phone
  h2: built with modern, lightweight tools
  h2: convinced?
```

> **Keywords presentes:** `home screen`, `text-only`, `clock styles`, `work profile`, `detox mode`, `themes`, `fonts`, `privacy`, `data never leaves`.

#### Download (`/download`)

```
h1: download focusphone
  h2: how to install
  h2: why download outside google play
  h2: what changed in v1.0.4
  h2: something went wrong?
```

> **Keywords presentes:** `download focusphone`, `install`, `google play`, `apk`, `android`.

#### FAQ (`/faq`)

```
h1: questions & answers
  h2: installation
    h3: will i lose my apps when i install focusphone?
    h3: how do i uninstall focusphone and go back?
    h3: why does the apk come from a website and not google play?
  h2: using focusphone
    h3: what are the three profiles for?
    h3: what is conscious pause?
    h3: can i reorder my favourite apps?
  h2: privacy & security
    h3: does focusphone collect personal data?
    h3: why does the app need query_all_packages?
    h3: is my detox password safe?
    h3: what happens if i forget my detox password?
  h2: compatibility
    h3: will focusphone work on my phone?
  h2: still have questions?
```

> **Keywords presentes:** `install focusphone`, `uninstall`, `profiles`, `conscious pause`, `privacy`, `personal data`, `query_all_packages`, `detox password`, `compatibility`.

#### Changelog (`/changelog`)

```
h1: changelog
  h2: v1.0.4 (current)
  h2: previous releases
  h2: what's next
```

#### Privacy (`/privacy`)

```
h1: privacy policy
  h2: the short version
  h2: information we do not collect
  h2: local storage (room & sharedpreferences)
  h2: permissions & usage
    h3: query_all_packages
  h2: detox password security
  h2: data extraction & backups
  h2: third-party services
  h2: your rights (gdpr / ccpa)
  h2: changes to this policy
  h2: contact
```

#### Terms (`/terms`)

```
h1: terms of use
  h2: license (mit)
  h2: disclaimer of warranty
  h2: limitation of liability
  h2: user responsibilities
    h3: launcher default risk
  h2: redistribution
  h2: governing law
  h2: changes to these terms
  h2: contact
```

---

## 6. Estrategia de URLs y Arquitectura de Enlaces

### 6.1 URLs Definidas (Canonical)

| Página | URL Canonical | Motivo de elección |
|--------|---------------|-------------------|
| Home | `https://focusphone.app` | Raíz del dominio. Máxima autoridad. |
| Features | `https://focusphone.app/features` | Nombre corto, palabra clave incluida. |
| Download | `https://focusphone.app/download` | Acción clara. Mejor que `/get` o `/apk`. |
| FAQ | `https://focusphone.app/faq` | Estándar de la industria. Reconocible. |
| Changelog | `https://focusphone.app/changelog` | Estándar para desarrolladores y usuarios técnicos. |
| Privacy | `https://focusphone.app/privacy` | Corto. Sin `/policy` redundante. |
| Terms | `https://focusphone.app/terms` | Corto. Sin `/of-use` redundante. |

### 6.2 URLs Rechazadas y Por Qué

| URL Rechazada | Problema | URL Correcta |
|---------------|----------|--------------|
| `/download-apk` | Keyword stuffing en URL. Parece spam. | `/download` |
| `/products/focusphone` | Jerarquía innecesaria. Solo hay un producto. | `/` |
| `/blog/post-1` | Sin estrategia de blog definida. Añade complejidad. | N/A (futuro) |
| `/legal/privacy-policy` | Profundidad innecesaria. | `/privacy` |
| `/faqs` | Plural inconsistente. `/faq` es estándar. | `/faq` |
| `/home` | Redundante con `/`. | `/` |

### 6.3 Estrategia de Enlaces Internos (Internal Linking)

```
Home (/) ───────────────────────┐
    │                           │
    ├──→ /features              │
    │      ├──→ /download       │
    │      ├──→ /privacy        │
    │      └──→ /faq            │
    │                           │
    ├──→ /download ◄────────────┘
    │      ├──→ /faq
    │      ├──→ /changelog
    │      └──→ /privacy
    │
    ├──→ /faq
    │      ├──→ /download
    │      ├──→ /privacy
    │      └──→ /terms
    │
    ├──→ /changelog
    │      └──→ /download
    │
    ├──→ /privacy
    │      └──→ /terms
    │
    └──→ /terms
           └──→ /privacy
```

**Reglas de linking interno:**
1. Desde `/`, enlazar a `/features`, `/download`, `/faq`.
2. Desde `/features`, enlazar a `/download` (CTA final) y `/privacy` (sección security).
3. Desde `/download`, enlazar a `/faq` (troubleshooting) y `/privacy` (confianza).
4. Desde `/faq`, enlazar a `/download` y `/privacy`.
5. `/privacy` y `/terms` se enlazan mutuamente en el footer, nunca en body.
6. `/changelog` solo enlaza a `/download`.

---

## 7. Schema Markup (Structured Data)

### 7.1 SoftwareApplication (Home + Download)

```json
{
  "@context": "https://schema.org",
  "@type": "SoftwareApplication",
  "name": "FocusPhone",
  "applicationCategory": "LauncherApplication",
  "operatingSystem": "Android 7.0+",
  "offers": {
    "@type": "Offer",
    "price": "0",
    "priceCurrency": "USD"
  },
  "softwareVersion": "1.0.4",
  "downloadUrl": "https://focusphone.app/download",
  "author": {
    "@type": "Person",
    "name": "rezzt.dev",
    "url": "https://rezzt.dev"
  },
  "license": "https://opensource.org/licenses/MIT",
  "aggregateRating": {
    "@type": "AggregateRating",
    "ratingValue": "5",
    "ratingCount": "1"
  }
}
```

### 7.2 FAQPage (`/faq`)

```json
{
  "@context": "https://schema.org",
  "@type": "FAQPage",
  "mainEntity": [
    {
      "@type": "Question",
      "name": "Will I lose my apps when I install FocusPhone?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "No. FocusPhone is a launcher, not a system reset. All your apps stay installed. It only changes what you see on the home screen."
      }
    },
    {
      "@type": "Question",
      "name": "Does FocusPhone collect personal data?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "No. All data is stored locally on your device. There are no analytics, no ads, and no remote servers."
      }
    }
  ]
}
```

> **Nota:** Expandir con las 10 preguntas principales del FAQ.

### 7.3 BreadcrumbList (Todas las páginas excepto home)

```json
{
  "@context": "https://schema.org",
  "@type": "BreadcrumbList",
  "itemListElement": [
    {
      "@type": "ListItem",
      "position": 1,
      "name": "Home",
      "item": "https://focusphone.app/"
    },
    {
      "@type": "ListItem",
      "position": 2,
      "name": "Features",
      "item": "https://focusphone.app/features"
    }
  ]
}
```

### 7.4 Organization (Footer / Home)

```json
{
  "@context": "https://schema.org",
  "@type": "Organization",
  "name": "rezzt.dev",
  "url": "https://rezzt.dev",
  "logo": "https://focusphone.app/logo.svg",
  "sameAs": [
    "https://github.com/rezzt-dev"
  ]
}
```

---

## 8. Estrategia de Contenido Off-Page (Link Building)

### 8.1 Perfiles y Directorios a Objetivar

| Plataforma | Tipo | Prioridad | Acción |
|------------|------|-----------|--------|
| **GitHub** | Repositorio | Crítica | README optimizado con keywords, releases taggeados, topics: `android`, `launcher`, `minimalist`, `digital-detox`, `kotlin`. |
| **Product Hunt** | Lanzamiento | Alta | Lanzar v1.0.4 con copy SEO-friendly. Responder comentarios. |
| **AlternativeTo** | Directorio | Alta | Añadir FocusPhone con tags correctos. Solicitar reviews. |
| **F-Droid** | Repositorio FOSS | Alta | Submit para inclusión. Backlink de alta autoridad en nicho. |
| **Reddit (r/minimalism, r/digitalminimalism, r/androidapps)** | Comunidad | Media | Participación genuina, no spam. AMA del desarrollador. |
| **Hacker News** | Comunidad técnica | Media | Show HN cuando haya una release significativa. |
| **LibHunt / Awesome-Android** | Listados curados | Media | Pull request para inclusión en listados awesome. |
| **Medium / Dev.to** | Blog técnico | Baja | Artículo guest post sobre "building a launcher in 2026". |

### 8.2 Estrategia de Anchor Text

| Destino | Anchor Text Ideal | Anchor Text a Evitar |
|---------|-------------------|----------------------|
| `/` | `FocusPhone`, `minimalist Android launcher` | `click here`, `this app`, `website` |
| `/download` | `download FocusPhone`, `get the APK` | `here`, `download link` |
| `/privacy` | `privacy policy`, `how we handle data` | `here`, `read more` |
| GitHub | `source code on GitHub`, `MIT license` | `GitHub`, `repo` (demasiado genérico) |

---

## 9. Sitemap.xml y robots.txt

### 9.1 robots.txt

```
User-agent: *
Allow: /
Disallow: /404

Sitemap: https://focusphone.app/sitemap.xml
```

### 9.2 sitemap.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>https://focusphone.app/</loc>
    <lastmod>2026-05-15</lastmod>
    <changefreq>weekly</changefreq>
    <priority>1.0</priority>
  </url>
  <url>
    <loc>https://focusphone.app/features</loc>
    <lastmod>2026-05-15</lastmod>
    <changefreq>monthly</changefreq>
    <priority>0.9</priority>
  </url>
  <url>
    <loc>https://focusphone.app/download</loc>
    <lastmod>2026-05-15</lastmod>
    <changefreq>weekly</changefreq>
    <priority>0.9</priority>
  </url>
  <url>
    <loc>https://focusphone.app/faq</loc>
    <lastmod>2026-05-15</lastmod>
    <changefreq>monthly</changefreq>
    <priority>0.7</priority>
  </url>
  <url>
    <loc>https://focusphone.app/changelog</loc>
    <lastmod>2026-05-15</lastmod>
    <changefreq>weekly</changefreq>
    <priority>0.6</priority>
  </url>
  <url>
    <loc>https://focusphone.app/privacy</loc>
    <lastmod>2026-05-15</lastmod>
    <changefreq>yearly</changefreq>
    <priority>0.3</priority>
  </url>
  <url>
    <loc>https://focusphone.app/terms</loc>
    <lastmod>2026-05-15</lastmod>
    <changefreq>yearly</changefreq>
    <priority>0.3</priority>
  </url>
</urlset>
```

> **Prioridades:** Home y Download son `0.9-1.0` (máxima autoridad). FAQ `0.7` (contenido valioso). Changelog `0.6` (actualización frecuente). Legal `0.3` (necesario pero no prioritario para ranking).

---

## 10. Core Web Vitals y Rendimiento SEO

### 10.1 Objetivos

| Métrica | Objetivo | Prioridad |
|---------|----------|-----------|
| **LCP** (Largest Contentful Paint) | `< 2.5s` | Crítica |
| **FID/INP** (Interaction to Next Paint) | `< 200ms` | Alta |
| **CLS** (Cumulative Layout Shift) | `< 0.1` | Crítica |
| **TTFB** (Time to First Byte) | `< 600ms` | Alta |
| **FCP** (First Contentful Paint) | `< 1.8s` | Media |

### 10.2 Acciones Técnicas

- **Imágenes:** WebP con fallback JPG. Dimensiones explícitas en HTML (`width` + `height`) para evitar CLS.
- **Fuentes:** `font-display: swap`. Subset de caracteres si es posible.
- **CSS:** Critical CSS inline para el hero. Resto cargado asíncrono.
- **JS:** Mínimo indispensable. Sin frameworks pesados para una web estática. Astro/Next.js con SSR estático preferido.
- **Hosting:** CDN con edge locations (Cloudflare, Vercel, Netlify).

---

## 11. KPIs y Métricas de Seguimiento

### 11.1 Métricas de Tráfico (Google Analytics / Plausible)

| KPI | Objetivo a 3 meses | Objetivo a 6 meses | Cómo medir |
|-----|--------------------|--------------------|------------|
| Sesiones orgánicas/mes | 500 | 2,000 | Canal: Organic Search |
| Keywords posicionadas (top 20) | 15 | 40 | Google Search Console |
| CTR medio en SERP | 3% | 5% | Google Search Console |
| Posición media "minimalist android launcher" | > 30 | 11-20 | Rank tracker manual |
| Tráfico de marca ("focusphone") | 20% del total | 35% del total | GA4 / Plausible |

### 11.2 Métricas de Conversión

| KPI | Objetivo | Cómo medir |
|-----|----------|------------|
| Tasa de conversión `/` → `/download` | > 15% | Evento: click en "download the apk" |
| Tasa de conversión `/download` → descarga real | > 40% | Evento: click en enlace de APK |
| Bounce rate en `/` | < 45% | GA4 / Plausible |
| Tiempo en página `/features` | > 90s | GA4 / Plausible |

### 11.3 Métricas de Confianza

| KPI | Objetivo | Cómo medir |
|-----|----------|------------|
| Visitas a `/privacy` desde `/download` | > 10% | Path analysis |
| Bounce rate en `/privacy` | < 60% | GA4 / Plausible |
| Retorno a `/download` desde `/privacy` | > 30% | Path analysis |

---

## 12. Calendario de Acciones SEO (Primeros 90 Días)

| Semana | Acción | Responsable | Entregable |
|--------|--------|-------------|------------|
| 1 | Implementar meta tags y schema markup | Frontend | Tags en todas las páginas |
| 1 | Configurar Google Search Console + GA4 | Growth | Propiedades verificadas |
| 2 | Optimizar README de GitHub para SEO | Growth | README con keywords y links |
| 2 | Generar sitemap.xml y robots.txt | Frontend | Archivos en producción |
| 3 | Lanzar en Product Hunt | Growth | Página de PH publicada |
| 3 | Añadir a AlternativeTo | Growth | Perfil activo |
| 4 | Submit a F-Droid | Growth | Issue/PR en F-Droid |
| 4-8 | Participación en Reddit (r/digitalminimalism, r/androidapps) | Growth | 2-3 posts/comentarios semanales |
| 8-12 | Artículo técnico en Dev.to / Medium | Growth | 1 artículo publicado |
| 12 | Revisión de métricas y ajuste de keywords | Growth | Reporte SEO v1 |

---

## 13. Glosario SEO

| Término | Definición |
|---------|------------|
| **SERP** | Search Engine Results Page. La página de resultados de Google. |
| **CTR** | Click-Through Rate. Porcentaje de impresiones que resultan en clic. |
| **Canonical** | URL oficial que los motores de búsqueda deben indexar. |
| **Schema Markup** | Código estructurado (JSON-LD) que ayuda a los motores a entender el contenido. |
| **Long-tail** | Keywords específicas de bajo volumen pero alta intención. |
| **Backlink** | Enlace desde otro sitio web hacia el nuestro. Factor de autoridad. |
| **Anchor text** | Texto clickeable de un enlace. |
| **CLS** | Cumulative Layout Shift. Métrica de estabilidad visual. |
| **LCP** | Largest Contentful Paint. Métrica de velocidad de carga del elemento principal. |
| **INP** | Interaction to Next Paint. Métrica de responsividad. |

---

## 14. Historial de Cambios de este Documento

| Versión | Fecha | Autor | Cambios |
|---------|-------|-------|---------|
| 1.0.0 | 2026-05-15 | rezzt.dev | Estrategia SEO inicial. Keyword research completo, meta tags por página, schema markup, sitemap, internal linking, calendario de 90 días. |

---

> **Nota para desarrolladores y redactores:**  
> Este documento es un contrato de contenido. Cada texto que se escriba para la web debe poder rastrear su origen hasta una keyword, una intención de usuario y una página objetivo definidos aquí. Si no está en este documento, no es prioridad SEO.
