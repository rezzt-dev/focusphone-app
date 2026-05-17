# FocusPhone — WEB_STRUCTURE.md

> **Versión:** 1.0.0  
> **Estado:** Aprobado para desarrollo  
> **Última revisión:** 2026-05-15  
> **Propietario:** Producto + Frontend  

---

## 1. Propósito y Alcance

Este documento es el **esqueleto arquitectónico** del sitio web oficial de FocusPhone. Define qué páginas existen, qué contiene cada una, cómo se interconectan y por dónde se espera que navegue el usuario.

**Sin este documento:** la web crece de forma desordenada, se duplican secciones, se pierden rutas, los CTAs compiten entre sí y el SEO se fragmenta.

**Con este documento:** el equipo frontend, el redactor y el diseñador trabajan desde una única fuente de verdad estructural.

---

## 2. Principios de Arquitectura

| Principio | Aplicación |
|-----------|------------|
| **Minimalismo funcional** | Cada página resuelve una única intención de usuario. No hay páginas de relleno. |
| **Rutas planas y memorables** | Máximo un nivel de profundidad (`/privacy`, no `/legal/privacy-policy`). |
| **CTA único por intención** | Solo un CTA primario por viewport principal. Los secundarios no compiten visualmente. |
| **Flujo de salida controlado** | Las páginas legales no tienen CTAs de conversión; las de producto sí. |
| **SEO semántico** | Las URLs son palabras clave, los títulos son descriptivos, los headings forman una jerarquía lógica. |

---

## 3. Sitemap Jerárquico

```
/
├── /features
├── /download
├── /faq
├── /changelog
├── /privacy
└── /terms
```

**Total de páginas:** 7  
**Profundidad máxima:** 1 nivel  
**Página raíz (home):** `/`

---

## 4. Detalle de Páginas

### 4.1 Landing Page — `/`

**Intención del usuario:** Entender qué es FocusPhone en < 10 segundos y decidir si descargarlo.

| Propiedad | Valor |
|-----------|-------|
| **Meta title** | `FocusPhone — minimalist android launcher for intentional screen time` |
| **Meta description** | `replace distraction with intention. focusphone is a text-only android launcher with profiles, conscious pause, and premium themes. android 7.0+. free and open source.` |
| **OG image** | `og-home.jpg` (screenshot del home con reloj digital + 3 apps favoritas) |
| **Canonical** | `https://focusphone.app/` |
| **H1** | `your phone, intentionally.` |

#### Jerarquía de Secciones

```
<nav>                         [fijo, sticky, altura 64px]
  logo + nombre               [izquierda]
  features / download / faq   [centro, desktop]
  menú hamburguesa            [móvil]

<main>
  ── Hero ─────────────────────────────────────────────
  H1: your phone, intentionally.
  Subtítulo (H2 visual, párrafo semántico):
    "a text-only android launcher designed to reduce
     digital noise. no icons. no feeds. just what you choose."
  CTA primario:   [download the apk]
  CTA secundario: [see how it works →] (anchor a #how-it-works)
  Asset:          screenshot del HomeActivity con tema slate + reloj stacked

  ── Social Proof / Trust ─────────────────────────────
  3 métricas en línea:
    "100% kotlin" / "zero tracking" / "open source (MIT)"
  Badge: GitHub stars (dinámico, si aplica)

  ── How it works (#how-it-works) ─────────────────────
  H2: three steps to a quieter phone.
  1. install & set as launcher
  2. pick up to 10 favourite apps
  3. choose a profile: normal, work, or detox
  Asset: 3 screenshots en secuencia (onboarding → app select → home)
  CTA: [get focusphone]

  ── Core Features (preview) ──────────────────────────
  H2: built for focus.
  Grid 2×2 o 1 columna en móvil:
    • text-only home screen
    • conscious pause breathing timer
    • contextual profiles (normal / work / detox)
    • premium themes & typography
  Cada ítem: icono lineal + H3 + 1 línea de copy.
  CTA: [explore all features →] → /features

  ── Theming Preview ──────────────────────────────────
  H2: looks like you.
  Visual interactivo (o estático con tabs):
    muestra los 4 temas: slate / navy / warm beige / off-white
  Copy: "swap between carefully crafted palettes or set your own hex colors."
  CTA: [download and try it]

  ── Final CTA Band ───────────────────────────────────
  H2: ready to reclaim your attention?
  CTA primario grande: [download focusphone]
  Copy secundario: "android 7.0+. no account required. no tracking."

<footer>
  columna izquierda:   logo + "© rezzt.dev"
  columna centro:      features / download / faq / changelog
  columna derecha:     privacy / terms / github / contact
```

#### CTAs Ubicados

| CTA | Texto | Tipo | Destino | Sección |
|-----|-------|------|---------|---------|
| Primario | `download the apk` | Botón (`<a>` con estilo btn) | `/download` | Hero |
| Secundario | `see how it works` | Enlace texto + flecha | `#how-it-works` (anchor) | Hero |
| Terciario | `get focusphone` | Botón | `/download` | How it works |
| Terciario | `explore all features` | Enlace texto + flecha | `/features` | Core Features |
| Primario | `download focusphone` | Botón grande | `/download` | Final CTA Band |

#### Flujo Interno Esperado

```
Entrada (orgánico / redes / GitHub)
    ↓
Hero → scroll → How it works → scroll → Features preview
    ↓ (si convence)                      ↓ (si quiere más)
/download                              /features
    ↓
Conversión (descarga APK)
```

---

### 4.2 Features — `/features`

**Intención del usuario:** Entender en profundidad qué hace la app antes de instalarla.

| Propiedad | Valor |
|-----------|-------|
| **Meta title** | `features — focusphone, the minimalist android launcher` |
| **Meta description** | `discover every feature: text-only home screen, conscious pause, detox profiles, custom themes, typography, and more. built for intentional use.` |
| **Canonical** | `https://focusphone.app/features` |
| **H1** | `everything inside focusphone.` |

#### Jerarquía de Secciones

```
<nav> [igual que home, activo: features]

<main>
  ── Hero ─────────────────────────────────────────────
  H1: everything inside focusphone.
  Subtítulo: "a complete breakdown of what the app does,
             why it matters, and how it looks."

  ── Feature: Home Screen ─────────────────────────────
  H2: a home screen with nothing to distract you.
  H3: text-only favourites
  Copy: "up to 10 apps. no icons. no badges. no notifications.
         just the names you chose, aligned left, center, or right."
  Asset: screenshot HomeActivity con lista de apps favoritas.
  H3: five clock styles
  Copy: "digital, stacked, words, analog, or hidden. each one
         designed to tell time without demanding attention."
  Asset: collage de los 5 tipos de reloj.

  ── Feature: Profiles ────────────────────────────────
  H2: three modes for three mindsets.
  H3: normal
  Copy: "no restrictions. full access to your favourite apps and drawer."
  H3: work
  Copy: "blocks manually selected apps and anything matching
         distracting keywords. stay in the zone."
  H3: detox
  Copy: "blocks every app except focusphone itself.
         set a timer. add a password. recover your attention."
  Asset: screenshot de DetoxBlockedActivity con countdown.
  Nota visual: destacar que detox requiere contraseña SHA-256.

  ── Feature: Conscious Pause ─────────────────────────
  H2: breathe before you open.
  Copy: "a configurable countdown forces a 3-to-10-second pause
         before launching any app. enough to break the reflex."
  Asset: screenshot de ConsciousPauseActivity con círculo de respiración.

  ── Feature: Customization ───────────────────────────
  H2: make it yours.
  H3: four built-in themes
  Copy: "slate, navy, warm beige, off-white. plus custom hex support."
  H3: downloadable fonts
  Copy: "poppins, comfortaa, figtree, jetbrains mono, space mono.
         load them seamlessly without leaving the app."
  H3: alignment & sizing
  Copy: "left, center, or right. adjust clock and text size independently."
  Asset: grid de screenshots con variaciones de tema + fuente.

  ── Feature: App Drawer ──────────────────────────────
  H2: everything else, one swipe away.
  Copy: "swipe left for the full drawer. search instantly.
         long-press to block or rename any app."
  Asset: screenshot AllAppsActivity con search bar activa.

  ── Feature: Security & Privacy ──────────────────────
  H2: your data never leaves your phone.
  Copy: "all storage is local: Room for favourites, SharedPreferences
         for settings. no analytics. no cloud. the detox password is
         hashed with SHA-256 and excluded from backups."
  Asset: iconografía de escudo/candado minimalista.
  Enlace: [read our privacy policy →] → /privacy

  ── Tech Stack (ligero) ──────────────────────────────
  H2: built with modern, lightweight tools.
  Lista: Kotlin 100%, Coroutines, Room 2.8.4, Material 3, MVVM.
  Enlace: [view technical specs →] → GitHub repo / TECH_SPECS.md

  ── Final CTA ────────────────────────────────────────
  H2: convinced?
  CTA: [download focusphone]

<footer> [igual que home]
```

#### CTAs Ubicados

| CTA | Texto | Tipo | Destino | Sección |
|-----|-------|------|---------|---------|
| Terciario | `read our privacy policy` | Enlace texto | `/privacy` | Security & Privacy |
| Terciario | `view technical specs` | Enlace texto | GitHub repo | Tech Stack |
| Primario | `download focusphone` | Botón | `/download` | Final CTA |

---

### 4.3 Download — `/download`

**Intención del usuario:** Obtener el APK de forma segura e instalarlo.

| Propiedad | Valor |
|-----------|-------|
| **Meta title** | `download focusphone — minimalist launcher apk` |
| **Meta description** | `download the latest focusphone apk for android 7.0+. free, open source, and privacy-first. no google play required.` |
| **Canonical** | `https://focusphone.app/download` |
| **H1** | `download focusphone.` |

#### Jerarquía de Secciones

```
<nav> [igual que home, activo: download]

<main>
  ── Hero ─────────────────────────────────────────────
  H1: download focusphone.
  Versión destacada: "v1.0.4 — latest stable"
  Requisitos: "android 7.0+ (API 24) · ~8 MB"
  CTA primario grande: [download apk v1.0.4]
  Enlace secundario: [view all releases →] → GitHub releases
  Hash opcional: SHA-256 del APK para verificación.

  ── Install Guide (resumen) ──────────────────────────
  H2: how to install.
  Pasos numerados (4 pasos):
    1. enable "install unknown apps" in your browser settings.
    2. download the apk and tap it.
    3. set focusphone as your default launcher.
    4. pick your favourite apps and choose a theme.
  CTA: [read the full guide] → DOWNLOAD_AND_INSTALL.md (o anchor expandido)

  ── Why APK? ─────────────────────────────────────────
  H2: why download outside google play?
  Copy: "focusphone is a launcher. that requires deep system trust.
         distributing directly lets us stay independent, transparent,
         and fully open source. you inspect the code; you install the build."
  Enlace: [view source on github →]

  ── Version History (preview) ────────────────────────
  H2: what changed in v1.0.4?
  Lista de 3-4 bullets de la última versión.
  Enlace: [see full changelog →] → /changelog

  ── Troubleshooting ──────────────────────────────────
  H2: something went wrong?
  3-4 problemas comunes con respuesta corta:
    • "the apk won't install" → check android version and unknown sources.
    • "focusphone won't appear as launcher" → clear defaults of previous launcher.
    • "how do i go back?" → uninstall focusphone; system reverts automatically.
  Enlace: [visit the faq →] → /faq

  ── Final CTA (alternativa) ──────────────────────────
  CTA: [download apk v1.0.4]

<footer> [igual que home]
```

#### CTAs Ubicados

| CTA | Texto | Tipo | Destino | Sección |
|-----|-------|------|---------|---------|
| Primario | `download apk v1.0.4` | Botón grande | APK file (directo) | Hero + Final CTA |
| Secundario | `view all releases` | Enlace texto | GitHub releases | Hero |
| Terciario | `read the full guide` | Enlace texto | DOWNLOAD_AND_INSTALL.md / anchor | Install Guide |
| Terciario | `view source on github` | Enlace texto | GitHub repo | Why APK? |
| Terciario | `see full changelog` | Enlace texto | `/changelog` | Version History |
| Terciario | `visit the faq` | Enlace texto | `/faq` | Troubleshooting |

---

### 4.4 FAQ — `/faq`

**Intención del usuario:** Resolver dudas de instalación, privacidad o funcionamiento sin contactar a soporte.

| Propiedad | Valor |
|-----------|-------|
| **Meta title** | `faq — focusphone launcher questions & answers` |
| **Meta description** | `common questions about installing, using, and uninstalling focusphone. privacy, detox mode, launcher defaults, and more.` |
| **Canonical** | `https://focusphone.app/faq` |
| **H1** | `questions & answers.` |

#### Jerarquía de Secciones

```
<nav> [igual que home, activo: faq]

<main>
  ── Hero ─────────────────────────────────────────────
  H1: questions & answers.
  Subtítulo: "everything you need to know before and after installing."
  Buscador interno (opcional, JS): "search questions..."

  ── Categoría: Installation ──────────────────────────
  H2: installation
  Preguntas (acordeón o lista anclada):
    Q: will i lose my apps when i install focusphone?
    A: no. focusphone is a launcher, not a system reset. all your apps
       stay installed. it only changes what you see on the home screen.

    Q: how do i uninstall focusphone and go back?
    A: go to settings → apps → focusphone → uninstall. your previous
       launcher will be restored automatically.

    Q: why does the apk come from a website and not google play?
    A: focusphone is open source and distributed independently.
       this keeps the project transparent and free from store policies
       that limit launcher functionality.

  ── Categoría: Usage ─────────────────────────────────
  H2: using focusphone
    Q: what are the three profiles for?
    A: normal gives you full access. work blocks distracting apps.
       detox blocks everything except focusphone until the timer ends
       or you enter your password.

    Q: what is conscious pause?
    A: a breathing countdown (3-10 seconds) before opening any app.
       it breaks the automatic reflex of tapping icons.

    Q: can i reorder my favourite apps?
    A: yes. long-press and drag any app on the home screen.
       you can add up to 10.

  ── Categoría: Privacy & Security ────────────────────
  H2: privacy & security
    Q: does focusphone collect personal data?
    A: no. all data is stored locally on your device. there are no
       analytics, no ads, and no remote servers.

    Q: why does the app need QUERY_ALL_PACKAGES?
    A: this permission is required to list your installed apps
       in the drawer and in the selector. we do not extract,
       transmit, or analyze your app list.

    Q: is my detox password safe?
    A: yes. it is hashed with SHA-256 before storage and saved in
       a local file excluded from cloud backups.

    Q: what happens if i forget my detox password?
    A: after 4 failed attempts, detox is force-disabled for safety
       and the password is cleared.

  ── Categoría: Compatibility ─────────────────────────
  H2: compatibility
    Q: will focusphone work on my phone?
    A: any device running android 7.0 (API 24) or higher.

  ── No encuentras tu pregunta? ───────────────────────
  H2: still have questions?
  Copy: "open an issue on github or contact the developer directly."
  CTA: [open an issue] → GitHub issues
  CTA: [contact rezzt.dev] → rezzt.dev

<footer> [igual que home]
```

#### CTAs Ubicados

| CTA | Texto | Tipo | Destino | Sección |
|-----|-------|------|---------|---------|
| Terciario | `open an issue` | Botón outline | GitHub issues | Still have questions |
| Terciario | `contact rezzt.dev` | Enlace texto | rezzt.dev | Still have questions |

---

### 4.5 Changelog — `/changelog`

**Intención del usuario:** Verificar que el proyecto está activo, conocer novedades y decidir si actualizar.

| Propiedad | Valor |
|-----------|-------|
| **Meta title** | `changelog — focusphone version history` |
| **Meta description** | `see what's new in focusphone. version history, improvements, bug fixes, and upcoming features.` |
| **Canonical** | `https://focusphone.app/changelog` |
| **H1** | `changelog.` |

#### Jerarquía de Secciones

```
<nav> [igual que home, activo: changelog]

<main>
  ── Hero ─────────────────────────────────────────────
  H1: changelog.
  Subtítulo: "a transparent record of every release."

  ── Versión Actual ───────────────────────────────────
  H2: v1.0.4 (current)
  Fecha: 2026-05-15
  Tags: [stable]
  Secciones:
    • added    — ...
    • improved — ...
    • fixed    — ...
  Enlace: [download this version →] → /download

  ── Historial ────────────────────────────────────────
  H2: previous releases
  Lista cronológica inversa. Cada ítem:
    • versión + fecha
    • 2-3 bullets de cambios clave
    • enlace a release de GitHub (si aplica)

  ── Roadmap (opcional) ───────────────────────────────
  H2: what's next
  Lista de 2-3 items planificados (sin fecha comprometida).
  Ejemplo: "phase 2: hilt integration", "widget support", "quick settings tile".

<footer> [igual que home]
```

#### CTAs Ubicados

| CTA | Texto | Tipo | Destino | Sección |
|-----|-------|------|---------|---------|
| Terciario | `download this version` | Enlace texto | `/download` | Versión Actual |

---

### 4.6 Privacy Policy — `/privacy`

**Intención del usuario:** Cumplir con debida diligencia legal antes de instalar un APK externo.

| Propiedad | Valor |
|-----------|-------|
| **Meta title** | `privacy policy — focusphone` |
| **Meta description** | `focusphone does not collect, transmit, or sell personal data. all information stays on your device. read our full privacy policy.` |
| **Canonical** | `https://focusphone.app/privacy` |
| **H1** | `privacy policy.` |
| **Robots** | `index, follow` (sí, es una señal de confianza pública) |

#### Jerarquía de Secciones

```
<nav> [igual que home]

<main>
  ── Encabezado ───────────────────────────────────────
  H1: privacy policy.
  Fecha de vigencia: 2026-05-15
  Última actualización: 2026-05-15

  ── Resumen Ejecutivo ────────────────────────────────
  H2: the short version
  Copy: "focusphone does not collect, transmit, or sell any personal data.
         everything stays on your device. we have no servers, no analytics,
         and no third-party trackers."

  ── Secciones Legales ────────────────────────────────
  H2: information we do not collect
  H2: local storage (Room & SharedPreferences)
  H2: permissions & usage
    H3: QUERY_ALL_PACKAGES
  H2: detox password security
  H2: data extraction & backups
  H2: third-party services
  H2: your rights (GDPR / CCPA)
  H2: changes to this policy
  H2: contact

<footer> [igual que home]
```

**Nota:** Esta página **no contiene CTAs de conversión** (no hay botón de descarga). El objetivo es puramente informativo y de confianza. El único enlace de salida es al footer general.

---

### 4.7 Terms of Use — `/terms`

**Intención del usuario:** Conocer los términos legales de uso del software.

| Propiedad | Valor |
|-----------|-------|
| **Meta title** | `terms of use — focusphone` |
| **Meta description** | `terms and conditions for using focusphone. license, disclaimers, and user responsibilities.` |
| **Canonical** | `https://focusphone.app/terms` |
| **H1** | `terms of use.` |

#### Jerarquía de Secciones

```
<nav> [igual que home]

<main>
  ── Encabezado ───────────────────────────────────────
  H1: terms of use.
  Fecha de vigencia: 2026-05-15

  ── Secciones Legales ────────────────────────────────
  H2: license (MIT)
  H2: disclaimer of warranty
  H2: limitation of liability
  H2: user responsibilities
    H3: launcher default risk
  H2: redistribution
  H2: governing law
  H2: changes to these terms
  H2: contact

<footer> [igual que home]
```

**Nota:** Al igual que `/privacy`, esta página no tiene CTAs de conversión.

---

## 5. Flujo de Navegación del Usuario

### 5.1 User Journeys Principales

#### Journey A: Descubrimiento → Conversión (el camino feliz)

```
Entrada (Google / redes / GitHub)
    ↓
/ (landing)
    ↓  [scroll + convicción]
#how-it-works
    ↓
/features
    ↓  [decisión de instalar]
/download
    ↓
Descarga APK → Instalación → Conversión completa
```

**Métrica de éxito:** % de usuarios que llegan a `/download` desde `/` en < 3 páginas vistas.

#### Journey B: Duda Específica → Resolución

```
Entrada (Google: "focusphone detox password forgotten")
    ↓
/faq#detox-password
    ↓  [si resuelve la duda]
/download
    ↓
Descarga APK
```

**Métrica de éxito:** tiempo en página < 45 segundos + scroll hasta la respuesta.

#### Journey C: Due Diligence Legal → Confianza

```
Entrada (directo o desde /download)
    ↓
/privacy
    ↓  [si convence]
/download
    ↓
Descarga APK
```

**Métrica de éxito:** bounce rate de `/privacy` < 60% (indica que leen y vuelven).

#### Journey D: Retorno (usuario existente)

```
Entrada (bookmark / notificación de release)
    ↓
/changelog
    ↓  [si hay nueva versión]
/download
    ↓
Actualización APK
```

### 5.2 Diagrama de Flujo de Navegación (Texto)

```
                              ┌─────────────┐
                              │   ENTRADA   │
                              │ (SEO/Redes) │
                              └──────┬──────┘
                                     │
           ┌─────────────────────────┼─────────────────────────┐
           │                         │                         │
           ▼                         ▼                         ▼
     ┌──────────┐             ┌──────────┐              ┌──────────┐
     │    /     │             │  /faq    │              │ /privacy │
     │  (home)  │             │ (dudas)  │              │ (legal)  │
     └────┬─────┘             └────┬─────┘              └────┬─────┘
          │                        │                         │
    ┌─────┴─────┐                  │                         │
    ▼           ▼                  ▼                         ▼
┌───────┐   ┌────────┐       ┌──────────┐            ┌──────────┐
│/features│  │/download│      │/download │            │/download │
└────┬───┘   └───┬────┘       └──────────┘            └──────────┘
     │           │
     └─────┬─────┘
           ▼
    ┌────────────┐
    │  DESCARGA  │
    │   (APK)    │
    └────────────┘
```

---

## 6. Estrategia de CTAs

### Jerarquía Visual

| Tipo | Estilo | Uso | Ejemplo |
|------|--------|-----|---------|
| **Primario** | Botón sólido, color de acento (Slate/Navy), texto blanco | Una sola acción por sección principal | `download the apk` |
| **Secundario** | Botón outline o enlace con flecha | Alternativa dentro del mismo contexto | `view all releases →` |
| **Terciario** | Enlace texto plano, sin decoración | Navegación informativa o contextual | `read our privacy policy` |

### Reglas de Convivencia

1. **Un primario por viewport.** Si hay un botón grande de descarga en el hero, no hay otro igual hasta el final de la página.
2. **Los CTAs legales no son de conversión.** `/privacy` y `/terms` no tienen botones de descarga para no romper la intención informativa.
3. **Los CTAs de anclaje no recargan.** `#how-it-works`, `#detox-password`, etc., son smooth-scroll internos.
4. **Los CTAs externos señalan salida.** Todos los enlaces a GitHub, rezzt.dev, etc., llevan icono ↗ y `target="_blank" rel="noopener"`.

---

## 7. Convenciones Técnicas de URL

| Convención | Valor | Ejemplo |
|------------|-------|---------|
| **Protocolo** | HTTPS obligatorio | `https://focusphone.app/` |
| **Trailing slash** | Consistente (sin slash) | `/download`, no `/download/` |
| **Idioma por defecto** | Inglés | Todo el site en `en` como base |
| **Minúsculas** | Siempre | `/faq`, no `/FAQ` |
| **Guiones, no guiones bajos** | Siempre | `/terms-of-use` si se extendiera; actualmente `/terms` |
| **Parámetros prohibidos** | No query strings en rutas principales | ❌ `/download?ref=home` → usar localStorage/UTM en JS |
| **404** | Página propia con enlace a home y a `/faq` | `/404` (implícita) |

### Redirecciones Obligatorias

| Origen | Destino | Tipo | Motivo |
|--------|---------|------|--------|
| `www.focusphone.app/*` | `focusphone.app/*` | 301 | Canonical sin www |
| `http://focusphone.app/*` | `https://focusphone.app/*` | 301 | SSL obligatorio |
| `/install` | `/download` | 301 | Sinónimo común |
| `/help` | `/faq` | 301 | Sinónimo común |

---

## 8. Estructura de Navegación Compartida

### Desktop (> 1024 px)

```
[logo] focusphone          features    download    faq         [☰]
                                                    (activo subrayado)
```

### Tablet (768–1024 px)

```
[logo] focusphone          features    download    [☰]
```

### Mobile (< 768 px)

```
[logo] focusphone          [☰]
(overlay:
  features
  download
  faq
  ──────────
  changelog
  privacy
  terms
  github ↗
)
```

### Footer (todos los dispositivos)

```
┌─────────────────────────────────────────────────────────┐
│  [logo] focusphone                                      │
│  a minimalist android launcher by rezzt.dev             │
│                                                         │
│  product          resources          legal              │
│  ─────────        ─────────          ─────              │
│  features         changelog          privacy            │
│  download         faq                terms              │
│                   github ↗                              │
│                                                         │
│  © 2026 rezzt.dev · licensed under MIT                  │
└─────────────────────────────────────────────────────────┘
```

---

## 9. Dependencias entre Documentos

Este archivo es la base. Los siguientes documentos del repositorio `docs-web/` dependen de él:

| Documento | Dependencia con WEB_STRUCTURE.md |
|-----------|----------------------------------|
| `DESIGN_GUIDE.md` | Debe respetar las jerarquías de sección aquí definidas (H1, H2, H3) para aplicar estilos tipográficos. |
| `SEO_AND_KEYWORDS.md` | Cada página listada aquí requiere su propio set de keywords, title y description. |
| `PRODUCT_COPY.md` | El copy del hero, los feature blocks y los CTAs deben alinearse a las secciones y textos definidos aquí. |
| `FEATURES_DETAILS.md` | Debe expandir los H2/H3 de `/features` con descripciones técnicas y notas de asset. |
| `SCREENSHOTS_AND_MEDIA.md` | Cada asset mencionado en las secciones de esta estructura debe listarse en el inventario de media. |
| `PRIVACY_POLICY.md` | La estructura de `/privacy` aquí definida debe coincidir con el documento legal. |
| `TERMS_OF_USE.md` | La estructura de `/terms` aquí definida debe coincidir con el documento legal. |
| `DOWNLOAD_AND_INSTALL.md` | El paso a paso de `/download` debe derivar de este documento. |
| `CHANGELOG.md` | El historial de `/changelog` debe coincidir con el contenido del producto. |

---

## 10. Glosario de Términos Arquitectónicos

| Término | Significado en este documento |
|---------|------------------------------|
| **CTA** | Call to Action. Elemento interactivo que solicita una acción al usuario. |
| **Hero** | Sección superior de una página, visible sin scroll. Contiene el H1 y el CTA primario. |
| **Viewport** | Área visible del navegador en un momento dado. |
| **Smooth-scroll** | Desplazamiento animado hacia un anclaje interno (`#id`). |
| **Canonical** | URL oficial que los motores de búsqueda deben indexar. |
| **OG** | Open Graph. Metadatos para previsualización en redes sociales. |
| **User Journey** | Secuencia de páginas y acciones que realiza un usuario para cumplir un objetivo. |

---

## 11. Historial de Cambios de este Documento

| Versión | Fecha | Autor | Cambios |
|---------|-------|-------|---------|
| 1.0.0 | 2026-05-15 | rezzt.dev | Estructura inicial aprobada. 7 páginas definidas, sitemap plano, CTAs mapeados, 4 user journeys documentados. |

---

> **Nota para desarrolladores:**  
> Este documento es de solo lectura para el equipo de frontend. Cualquier cambio de ruta, adición de página o modificación de jerarquía requiere actualizar este archivo primero y notificar al equipo de contenido y SEO.
