# FocusPhone — Product Copy

> **Versión:** 1.0.0  
> **Estado:** Borrador para revisión  
> **Última revisión:** 2026-05-17  
> **Propietario:** Contenido + Growth  
> **Formato:** Inglés (master) + español (anotaciones de contexto)  

---

## 1. Hero Section — Landing Page (`/`)

> **Objetivo:** Captar atención en < 3 segundos, comunicar la promesa de valor principal y generar el primer click hacia `/download`.  
> **Keywords objetivo:** `minimalist android launcher`, `text-only`, `reduce digital noise`, `distraction free launcher`.  
> **Restricciones de diseño:** Minúsculas estructurales (DESIGN_GUIDE.md §4.3). Excepción SEO: `<title>` y `meta name="description"` usan mayúsculas iniciales.

---

### 1.1 Headline Principal (H1)

**[EN]** your phone, intentionally.

> **[ES]** Frase corta, deliberadamente incompleta gramaticalmente para crear pausa e intención. El adverbio al final rompe la expectativa y refuerza el atributo de marca *intentional*. No se usa mayúscula inicial por coherencia con la UI de la app (DESIGN_GUIDE.md §4.3).  
> **SEO note:** El tag `<title>` de la página usa la variante con mayúsculas: *FocusPhone — Minimalist Android Launcher for Intentional Screen Time* (ver SEO_AND_KEYWORDS.md §4.2).

---

### 1.2 Subtítulo / Supporting Copy

**[EN]** a text-only android launcher designed to reduce digital noise. no icons. no feeds. just what you choose.

> **[ES]** Tres oraciones cortas que escalan en impacto emocional. La primera define la categoría (`text-only android launcher` — keyword SEO). La segunda usa anáfora negativa (`no icons. no feeds.`) para eliminar objeciones. La tercera (`just what you choose`) devuelve el control al usuario, reforzando *trustworthy* e *intentional*.  
> **Palabras clave integradas:** `android launcher`, `text-only`, `reduce digital noise`.

---

### 1.3 CTA Primario

**[EN]** download the apk

> **[ES]** Acción directa, sin artículo indefinido ("the apk", no "the APK" ni "an APK"). Minúsculas por consistencia con todos los botones de la app. El texto es exactamente el mismo que el CTA principal del nav y de la página `/download`.  
> **Destino:** `/download`  
> **Estilo visual:** Botón primario (DESIGN_GUIDE.md §6.1).

---

### 1.4 CTA Secundario

**[EN]** explore features →

> **[ES]** Flecha derecha (`→`, no emoji) indica navegación interna sin salir del sitio. Ofrece una ruta alternativa para usuarios en fase de investigación (intención: * Investigación + Comparación* según SEO_AND_KEYWORDS.md §3.1).  
> **Destino:** `/features`  
> **Estilo visual:** Botón secundario outline o enlace terciario (DESIGN_GUIDE.md §6.1).

---

### 1.5 Meta Copy de Soporte (bajo el hero, opcional)

**[EN]** free and open source. android 7.0+. no account required.

> **[ES]** Tres frases de una línea que eliminan fricciones de conversión: precio (gratis), compatibilidad (Android 7.0+), y barrera de entrada (sin cuenta). Reduce la ansiedad antes del click.  
> **Palabras clave integradas:** `open source`, `android`.

---

## 2. Elevator Pitch

> **Objetivo:** Resumir el valor del producto en 25-30 palabras para READMEs, descripciones de directorios (Product Hunt, AlternativeTo, F-Droid) y tarjetas de compartición social.  
> **Restricción:** Máximo 30 palabras. Debe incluir al menos una keyword principal y un diferenciador único.

---

### 2.1 Pitch Principal

**[EN]** FocusPhone is a minimalist Android launcher with a text-only home screen. Built with conscious pause, contextual profiles, and premium themes to help you reclaim your attention. Free and open source.

> **[ES]** **(30 palabras)** Estructura: [Qué es] + [Diferenciador visual] + [Tres features clave] + [Beneficio emocional] + [Modelo de negocio/licencia].  
> **Keywords presentes:** `minimalist Android launcher`, `text-only home screen`, `conscious pause`, `open source`.  
> **Diferenciadores incluidos:** Conscious Pause (único en el mercado), perfiles contextuales, text-only.  
> **Nota de uso:** En contextos con límite de caracteres (ej. Twitter bio), usar la versión corta (§2.2).

---

### 2.2 Pitch Corto (versión reducida)

**[EN]** A text-only Android launcher that replaces distraction with intention. Featuring conscious pause, contextual profiles, and premium themes.

> **[ES]** **(19 palabras)** Para espacios muy reducidos donde "free and open source" ya esté implícito en el contexto (ej. descripción corta de GitHub).  
> **Keywords presentes:** `text-only Android launcher`, `distraction`, `conscious pause`, `contextual profiles`.

---

## 3. Propuesta de Valor (Value Proposition)

> **Objetivo:** Responder "¿por qué FocusPhone y no otro launcher?" en la sección intermedia de la landing (aprox. bajo el fold).  
> **Contexto competitivo:** Niagara, OLauncher, Before Launcher, Ratio, Blank (SEO_AND_KEYWORDS.md §2.1).  
> **Estrategia:** No atacar a competidores. Destacar diferenciadores que ningún competidor mayor tiene combinados.

---

### 3.1 Header de Sección

**[EN]** built for focus

> **[ES]** Título de sección en la landing. Actúa como H2 según el heading map de SEO_AND_KEYWORDS.md §5.2. Corto, activo, alineado con el nombre de marca.

---

### 3.2 Diferenciadores Principales

#### Diferenciador 1 — Text-Only Home Screen

**[EN]** no icons. no noise.

**[EN]** your home screen shows up to ten favourite apps as plain text. no colourful icons triggering unconscious taps. no notification badges begging for attention. just the tools you choose, in the order you decide.

> **[ES]** Primer diferenciador visual y emocional. La estructura repite la anáfora negativa del hero (`no icons. no noise.`) para coherencia narrativa. El párrafo traduce el feature técnico (text-only favourites) en beneficio emocional: eliminación de triggers visuales.  
> **Keywords:** `text-only home screen`, `no icons`, `android launcher no icons`.  
> **Alineación con perfiles:** Todos los perfiles se benefician, pero especialmente el Minimalista.

---

#### Diferenciador 2 — Conscious Pause

**[EN]** breathe before you open.

**[EN]** conscious pause is a configurable breathing timer that activates before launching any app. five seconds. ten seconds. you decide. it is the moment between impulse and action — and no other major launcher offers it.

> **[ES]** Este es el **killer feature** diferenciador absoluto (SEO_AND_KEYWORDS.md §2.2). El headline usa imperativo poético (`breathe before you open`) que suena como un mantra. El copy explica la mecánica (temporizador configurable) y luego eleva el beneficio ("el momento entre el impulso y la acción"). La última oración refuerza la exclusividad competitiva.  
> **Keywords:** `conscious pause`, `breathing timer`, `launcher with breathing timer`.  
> **Alineación con perfiles:** Adicto digital en recuperación, Profesional productivo.

---

#### Diferenciador 3 — Contextual Profiles

**[EN]** three modes for three mindsets.

**[EN]** normal lets you use everything. work blocks distracting apps automatically. detox locks everything except FocusPhone itself, with a timer and password protection. switch with a tap.

> **[ES]** Presentación clara de los tres perfiles (Normal / Work / Detox). La estructura es paralela para facilitar escaneo: cada perfil = una oración. El beneficio de Work se describe como automatización (`automatically`). El de Detox como protección (`locks`, `password protection`). La transición (`switch with a tap`) reduce la percepción de fricción.  
> **Keywords:** `work profile launcher android`, `detox mode app android`, `digital detox app`, `blocks apps during work`.  
> **Alineación con perfiles:** Cada perfil mapea directamente a un arquetipo de usuario (ver §4).

---

#### Diferenciador 4 — Privacy-First & Offline

**[EN]** your data never leaves your phone.

**[EN]** no analytics. no ads. no remote servers. all your favourites, themes, and passwords live in local storage. FocusPhone works entirely offline, because your attention is not a product.

> **[ES]** Diferenciador de confianza. La estructura usa anáfora negativa triple (`no... no... no...`) seguida de afirmación positiva. El cierre (`your attention is not a product`) es una declaración de principios que refuerza *trustworthy* y genera conexión emocional con usuarios preocupados por privacidad.  
> **Keywords:** `privacy focused`, `no tracking`, `offline`, `data never leaves`.  
> **Alineación con perfiles:** Todos, especialmente usuarios técnicos y minimalistas conscientes.

---

#### Diferenciador 5 — Open Source & Premium Feel

**[EN]** open source. premium feel.

**[EN]** licensed under MIT. built with modern Kotlin and zero bloat. four built-in themes. five clock styles. downloadable fonts. edge-to-edge layout. it is lightweight, transparent, and designed to last.

> **[ES]** Este diferenciador resuelve la tensión entre "gratis" y "calidad". Muchos usuarios asocian "open source" con falta de pulido; este copy refuerza lo contrario listando features premium (temas, relojes, fuentes, edge-to-edge). El cierre (`designed to last`) alinea con la filosofía de minimalismo duradero.  
> **Keywords:** `open source android launcher`, `premium themes`, `custom fonts`, `lightweight`.  
> **Alineación con perfiles:** Profesionales técnicos, minimalistas que valoran calidad sobre cantidad.

---

## 4. Mensajes por Perfil (Profile-Based Messaging)

> **Objetivo:** Adaptar el tono y el ángulo de la propuesta de valor a tres arquetipos de usuario principales. Estos mensajes se usan en: landing (secciones dinámicas o scroll), `/features` (secciones de use case), anuncios dirigidos (Reddit, PH), y email marketing futuro.  
> **Restricción:** Cada mensaje debe sentirse escrito *para* esa persona, no *sobre* el producto.

---

### 4.1 El Minimalista

> **Arquetipo:** Busca belleza en la restricción. Quiere menos, pero mejor. Valora la estética tipográfica, la coherencia visual y la ausencia de elementos innecesarios. No quiere "gestionar" su teléfono; quiere olvidarse de él.  
> **Keywords objetivo:** `minimalist phone interface`, `simple android launcher`, `text only launcher android`, `premium themes`.

#### Headline para este perfil

**[EN]** less is not empty. it is room to breathe.

#### Body copy

**[EN]** you do not need forty apps on your home screen. you need ten that matter. FocusPhone keeps your display clean, your fonts beautiful, and your alignment exact. no badges. no widgets. no noise. just a clock, a calendar, and the few tools you actually use.

> **[ES]** El headline juega con la redefinición del concepto "menos" (no es ausencia, es espacio). El body copy usa la segunda persona directa para crear intimidad. La anáfora final (`no... no... no... just...`) imita la propia estructura minimalista del producto. Se mencionan los elementos visuales concretos (fuentes, alineación, clock, calendar) porque este perfil valora la precisión estética.  
> **Nota de tono:** Evitar lenguaje de "adicción" o "distracción"; este perfil no se identifica como adicto, sino como alguien con gusto refinado.

---

### 4.2 El Adicto Digital en Recuperación

> **Arquetipo:** Reconoce que su relación con el teléfono es problemática. Ha intentado reducir screen time sin éxito. Busca herramientas que le quiten fricción, no que añadan más "productividad" que gestionar. Quiere compasión, no sermones.  
> **Keywords objetivo:** `digital detox app`, `reduce screen time`, `detox mode app android`, `how to reduce screen time android launcher`.

#### Headline para este perfil

**[EN]** your phone does not have to be a trap.

#### Body copy

**[EN]** every colourful icon is a trigger. every notification badge is a demand. FocusPhone removes them entirely. with detox mode, conscious pause, and a text-only interface, your phone stops asking for your attention and starts giving it back. you are not weak for needing help. you are wise for choosing a tool that respects your limits.

> **[ES]** Este es el copy más emocionalmente cargado. El headline reconoce la sensación de atrapamiento sin juzgar. El body copy valida la experiencia del usuario (`every icon is a trigger`) y luego presenta el producto como aliado, no como castigo. El cierre (`you are not weak... you are wise`) es deliberadamente terapéutico; refuerza la autoestima del usuario y reduce la vergüenza asociada a buscar herramientas de control.  
> **Nota de tono:** Evitar palabras como "addict" o "addiction" en el copy visible; usar "recovery" o "habits" si es necesario. El producto ya usa "detox", que es aceptable por su connotación de limpieza, no de enfermedad.

---

### 4.3 El Profesional Productivo

> **Arquetipo:** Gestiona su tiempo activamente. Usa técnicas de productividad (Pomodoro, deep work, time blocking). Quiere un launcher que se integre con su sistema de trabajo, no que lo reemplace. Valora la eficiencia, la automatización y la ausencia de fricción.  
> **Keywords objetivo:** `work profile launcher android`, `distraction free launcher`, `launcher that blocks apps during work`, `best minimalist launcher android`.

#### Headline para este perfil

**[EN]** context switching is the enemy. your launcher can be the defense.

#### Body copy

**[EN]** the work profile blocks distracting apps the moment you enable it. no willpower required. no manual toggling. when the day ends, switch to normal. when you need absolute focus, activate detox. FocusPhone adapts to your schedule so you do not have to adapt to your phone.

> **[ES]** El headline usa lenguaje de productividad establecido (`context switching`, `deep work` implícito). El body copy enfatiza automatización (`no willpower required`, `no manual toggling`) porque este perfil valora sistemas sobre esfuerzo. La estructura paralela al final (`adapts to your schedule so you do not have to adapt to your phone`) es memorable y fácil de citar.  
> **Nota de tono:** Profesional pero no corporativo. Evitar buzzwords como "synergy" o "leverage". Mantener el tono calmado de marca incluso al hablar de productividad.

---

## 5. Testimonios y Casos de Uso (Testimonials & Use Cases)

> **Objetivo:** Proporcionar narrativas realistas que humanicen el producto y permitan a los visitantes verse a sí mismos usando FocusPhone.  
> **Origen:** Escenarios ficticios basados en los tres perfiles definidos. Nombres ficticios. Edades y profesiones representativas del público objetivo.  
> **Formato:** Cada testimonio incluye: nombre, edad, ocupación, cita en primera persona, y el feature específico que menciona.  
> **Restricción:** Las citas deben sonar auténticas (ligeramente imperfectas, no demasiado pulidas) y mencionar un beneficio concreto, no solo elogios genéricos.

---

### 5.1 Escenario: Reducción de Screen Time

**[EN]**

> *"i went from four hours of daily screen time to ninety minutes without trying. there are no icons tempting me. i only open what i need."*
>
> **marco, 29, ux designer**

> **[ES]** Testimonio del perfil **Minimalista/Adicto Digital en transición**. El beneficio es cuantificable (4h → 90min), lo que añade credibilidad. La frase *"without trying"* es clave: reduce la percepción de esfuerzo y refuerza que el producto funciona *por diseño*, no por disciplina. El lenguaje es simple y casi ingenuo, lo que lo hace más creíble.  
> **Feature destacado:** Text-only home screen.  
> **Keyword asociada:** `reduce screen time`.

---

### 5.2 Escenario: Foco Profesional

**[EN]**

> *"work mode is my failsafe. it blocks the apps that break my flow without me having to think about it. i have never trusted myself to do it manually."*
>
> **elena, 34, software engineer**

> **[ES]** Testimonio del perfil **Profesional Productivo**. La palabra *"failsafe"* indica que el producto es parte de un sistema de productividad, no un capricho. La honestidad de la última oración (`i have never trusted myself`) es vulnerable y por tanto muy creíble; valida la necesidad de automatización.  
> **Feature destacado:** Work profile (bloqueo automático de apps distractoras).  
> **Keyword asociada:** `work profile launcher android`, `blocks apps during work`.

---

### 5.3 Escenario: Conciencia y Hábitos

**[EN]**

> *"conscious pause made me aware of how many times i opened apps for no reason. now i breathe. half the time, i do not even open them."*
>
> **sam, 26, psychology student**

> **[ES]** Testimonio del perfil **Adicto Digital en Recuperación**. Menciona explícitamente el feature *Conscious Pause*, el diferenciador único. El arco narrativo es: antes (inconsciencia) → durante (herramienta de pausa) → después (comportamiento cambiado). La simplicidad del lenguaje refuerza la autenticidad.  
> **Feature destacado:** Conscious Pause.  
> **Keyword asociada:** `conscious pause`, `breathing timer`.

---

### 5.4 Escenario: Estética y Calma

**[EN]**

> *"my phone finally looks the way i feel: calm. the warm beige theme and poppins typography make even checking the time feel intentional."*
>
> **lina, 31, freelance writer**

> **[ES]** Testimonio del perfil **Minimalista puro**. El beneficio aquí es emocional y estético, no funcional. La metáfora (`looks the way i feel`) es potente porque conecta el producto con la identidad del usuario. Mencionar elementos concretos (`warm beige theme`, `poppins typography`) demuestra que se usan las funciones de personalización premium.  
> **Feature destacado:** Premium themes & downloadable fonts.  
> **Keyword asociada:** `premium themes`, `custom fonts android`, `minimalist phone interface`.

---

### 5.5 Escenario: Uso Familiar / Control Parental

**[EN]**

> *"i set detox mode on my daughter's phone during homework hours. she complains for two minutes, then forgets about it. her grades did the talking after that."*
>
> **david, 42, high school teacher**

> **[ES]** Testimonio de un **caso de uso secundario no planificado**: control parental / uso familiar. El tono es pragmático y ligeramente humorístico (`she complains for two minutes`), lo que lo hace muy humano. El beneficio final (`her grades did the talking`) es el tipo de resultado concreto que genera confianza en nuevos usuarios.  
> **Feature destacado:** Detox mode (con temporizador y protección por contraseña).  
> **Nota:** Este testimonio amplía el público objetivo sin desviar el mensaje principal. Puede usarse en `/features` bajo una sección de "use cases" pero no en el hero.

---

## 6. CTAs Globales y Microcopy

> **Objetivo:** Recopilar todos los textos de acción, estados vacíos, disclaimers y microcopy que aparecen repetidamente en la web.  
> **Restricción:** Todos los CTAs interactivos en minúsculas (DESIGN_GUIDE.md §4.3).

---

### 6.1 CTAs Principales

| Contexto | Copy (EN) | Destino | Notas (ES) |
|----------|-----------|---------|------------|
| Hero primario | `download the apk` | `/download` | CTA dominante en landing y nav. |
| Hero secundario | `explore features →` | `/features` | Flecha indica navegación interna. |
| Features final | `get FocusPhone` | `/download` | Variante del CTA primario para no repetir literalmente. |
| Download página | `download latest release` | Enlace directo APK | Acción explícita en `/download`. |
| Download alternativo | `view all releases ↗` | GitHub releases | Flecha diagonal indica enlace externo. |
| Footer | `read our privacy policy →` | `/privacy` | Enlace terciario. |
| FAQ cierre | `still have questions? contact us →` | `mailto:` o GitHub issues | Mantiene al usuario dentro del flujo de confianza. |

---

### 6.2 Microcopy de Confianza (Trust Signals)

**[EN]** no tracking. no ads. no account required.

> **[ES]** Bloque de tres elementos que se puede ubicar bajo el CTA de descarga o en el footer. Elimina fricciones de conversión comunes en apps que se descargan fuera de Google Play.  
> **Keywords:** `no tracking`, `no ads`, `privacy focused`.

**[EN]** verified by the community. open source on GitHub.

> **[ES]** Refuerzo de confianza para usuarios técnicos que dudan de descargar APKs de sitios web. El enlace debe ir al repositorio público.

---

### 6.3 Estados y Disclaimers

**[EN]** FocusPhone is a launcher, not a system reset. all your apps stay installed.

> **[ES]** Respuesta anticipada a una objeción frecuente (miedo a perder apps). Se usa en FAQ y puede mostrarse como tooltip durante el onboarding web.  
> **Keyword:** `install focusphone`.

**[EN]** Android 7.0+ required. approximately 8 MB.

> **[ES]** Especificaciones técnicas mínimas. El tamaño ligero (`8 MB`) es un argumento de venta implícito contra launchers pesados. Se ubica cerca del botón de descarga.

**[EN]** free forever. no in-app purchases. no subscriptions.

> **[ES]** Elimina el miedo al "freemium". Especialmente importante para un producto indie que compite contra apps con modelos de suscripción.

---

### 6.4 Social Proof Placeholders

**[EN]** trusted by minimalists, developers, and students worldwide.

> **[ES]** Línea de social proof genérica que se puede usar antes de tener métricas reales de descargas. Se actualizará cuando haya datos concretos (ej. "downloaded 10,000+ times").  
> **Nota:** Evitar cifras ficticias. Usar categorías de usuarios hasta tener métricas verificables.

---

## 7. Glosario de Términos de Marca

> **Objetivo:** Garantizar consistencia en cómo se nombran features y conceptos en todos los canales.

| Término | Uso correcto (EN) | Uso incorrecto | Nota (ES) |
|---------|-------------------|----------------|-----------|
| Nombre del producto | `FocusPhone` | `Focusphone`, `Focus Phone`, `focus phone` | Siempre camel case. Marca registrada informal. |
| Feature: pausa | `Conscious Pause` | `conscious pause`, `ConsciousPause`, `breathing timer` | En titulares y primera mención de sección: mayúsculas. En body copy de la app: minúsculas. |
| Feature: perfiles | `Normal`, `Work`, `Detox` | `normal mode`, `work profile mode` | Nombres propios de los perfiles. No añadir "mode" redundante cuando el contexto es claro. |
| Home screen | `text-only home screen` | `homescreen`, `text only homescreen` | Guion en `text-only`. Espacio en `home screen`. |
| Descarga | `the APK` | `the apk file`, `an APK` | En CTAs: `download the apk` (minúsculas). En textos técnicos: `the APK` (mayúsculas, acrónimo). |
| Licencia | `MIT license` | `MIT License`, `Mit license` | Acrónimo en mayúsculas. "license" en minúsculas salvo que inicie oración. |

---

## 8. Checklist de Aplicación

Antes de publicar cualquier texto derivado de este documento, verificar:

- [ ] ¿El copy respeta las **minúsculas estructurales** en CTAs, navegación, headings H1-H4 y body? (Excepción: `<title>`, `<meta>`, proper nouns, acrónimos técnicos).
- [ ] ¿Cada sección incluye al menos **una keyword principal o secundaria** de SEO_AND_KEYWORDS.md?
- [ ] ¿Los testimonios mencionan un **beneficio concreto** (no solo elogios genéricos)?
- [ ] ¿Los CTAs tienen **destino claro** y **estilo visual definido** en DESIGN_GUIDE.md?
- [ ] ¿El tono se mantiene en el cuadrante **calm / intentional / quiet / trustworthy**?
- [ ] ¿No hay **cifras ficticias** de descargas/usuarios en social proof?
- [ ] ¿Las flechas de enlace externo usan `↗` y las internas `→`?
- [ ] ¿El archivo `PRODUCT_COPY.md` está versionado y sincronizado con `DESIGN_GUIDE.md` y `SEO_AND_KEYWORDS.md`?

---

> **Nota para redactores y desarrolladores:**  
> Este documento es la fuente única de verdad para todo copy comercial de FocusPhone. Si un texto de marketing no está aquí, no debe aparecer en la web. Cualquier adición debe pasar por este archivo y actualizar la versión del documento.
