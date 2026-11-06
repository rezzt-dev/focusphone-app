# FocusPhone — Guía de Estructuración Web

> Documento maestro para la planificación, desarrollo y publicación del sitio web oficial de **FocusPhone**.
> Este fichero define qué información de soporte es necesaria, por qué se necesita y cómo se utiliza para construir una web completa, coherente con la identidad del producto y lista para su distribución pública.

---

## 1. Contexto y Objetivo

FocusPhone es un launcher Android minimalista diseñado para reducir la distracción digital. Su web debe reflejar esa misma filosofía: **limpieza, intencionalidad y calma**.

**Objetivo de esta guía:**
Proporcionar al equipo de desarrollo, diseño y marketing un mapa completo de los ficheros de contenido y planificación necesarios para construir la web sin depender del código fuente de la app.

**Principios de la web:**
- Tono calmado y minimalista (alineado con la app).
- Textos orientados a la acción pero sin agresividad.
- Información legal completa (imprescindible para distribución de APKs).
- SEO optimizado para términos como "minimalist android launcher", "digital detox app", "distraction-free phone".

---

## 2. Estructura de Carpetas Propuesta

```
docs-web/
├── 01-planning/          # Arquitectura, diseño y estrategia
├── 02-content/           # Copy, funcionalidades y FAQ
├── 03-assets/            # Inventario y guía de recursos visuales
├── 04-legal/             # Privacidad, términos y cumplimiento
├── 05-product/           # Descarga, specs y versionado
└── README.md             # Índice y guía de uso de esta estructura
```

> **Convención de numeración:** Los prefijos numéricos (`01-`, `02-`) imponen un orden de lectura lógico. El flujo de trabajo web debe seguir este orden: primero se planifica, luego se escribe, después se producen assets, se revisa lo legal y finalmente se publica.

---

## 3. Ficheros Requeridos — Detalle Completo

### 3.1 Carpeta: `01-planning/`

Contiene todo lo que se necesita **antes de escribir una sola línea de código frontend**.

#### `WEB_STRUCTURE.md`
- **¿Qué es?** Mapa arquitectónico de la web completa.
- **¿Por qué se usa?** Sin este documento, la web crece de forma desordenada, se duplican secciones y se pierden rutas. Define el esqueleto.
- **¿Para qué se usa?**
  - Listado de páginas (Landing, Features, Download, Privacy, FAQ, Changelog).
  - Jerarquía de secciones por página.
  - Definición de rutas URL (`/`, `/features`, `/download`, `/privacy`).
  - Ubicación de CTAs (Call to Action) en cada página.
  - Flujo de navegación esperado del usuario.

#### `DESIGN_GUIDE.md`
- **¿Qué es?** Guía visual y de tono específica para la web.
- **¿Por qué se usa?** La app tiene una identidad muy definida (minimalista, textos en minúsculas, paletas Slate/Navy/Beige/Off-white). La web debe ser una extensión coherente de esa experiencia.
- **¿Para qué se usa?**
  - Paleta de colores web (fondos, textos, acentos, estados hover).
  - Tipografías web (fallbacks si no se usan las mismas de la app).
  - Tono de voz: calmado, directo, sin mayúsculas agresivas, uso consciente del espacio en blanco.
  - Reglas de espaciado, bordes redondeados, y estilo de botones.
  - Referencias visuales o moodboard.

#### `SEO_AND_KEYWORDS.md`
- **¿Qué es?** Estrategia de posicionamiento en buscadores.
- **¿Por qué se usa?** Un producto indie sin presupuesto de ads depende del tráfico orgánico. Este fichero asegura que todo el contenido se escriba con palabras clave objetivo desde el primer borrador.
- **¿Para qué se usa?**
  - Keywords principales y secundarias por página.
  - Meta títulos y meta descripciones (`<title>`, `<meta name="description">`).
  - Etiquetas Open Graph y Twitter Cards para compartir en redes.
  - Estructura de headings (`<h1>`, `<h2>`) recomendada por página.
  - Estrategia de URLs amigables (`/download` vs `/download-apk`).

---

### 3.2 Carpeta: `02-content/`

El **copy real** que el usuario leerá. Debe escribirse siguiendo el `DESIGN_GUIDE.md` y optimizarse con el `SEO_AND_KEYWORDS.md`.

#### `PRODUCT_COPY.md`
- **¿Qué es?** Textos de marketing puros: lo que vende el producto.
- **¿Por qué se usa?** Separar el copy comercial de la documentación técnica permite que un redactor o marketer trabaje sin interferir en la precisión técnica.
- **¿Para qué se usa?**
  - *Hero section:* Headline principal, subtítulo y CTA de la landing.
  - *Elevator pitch:* Frase de 25-30 palabras que resume el valor.
  - *Propuesta de valor:* Por qué FocusPhone es diferente a otros launchers.
  - *Mensajes por perfil:* Textos adaptados al minimalista, al adicto digital en recuperación, y al profesional productivo.
  - *Testimonios/Use cases:* Escenarios narrativos ("reduce tu screen time", "mantén un escritorio limpio").

#### `FEATURES_DETAILS.md`
- **¿Qué es?** Descripción exhaustiva de cada funcionalidad de la app.
- **¿Por qué se usa?** El equipo frontend necesita saber qué ilustrar y en qué orden. Un feature mal descrito genera secciones vacías o screenshots descontextualizadas.
- **¿Para qué se usa?**
  - Perfiles (Normal, Work, Detox): qué hace cada uno y por qué es útil.
  - Pausa Consciente: flujo del temporizador de respiración antes de abrir apps.
  - Temas y tipografías: descripción visual de las 4 paletas y las fuentes descargables.
  - Relojes: los 5 tipos (Digital, Stacked, Words, Analog, Hidden).
  - Cajón de apps y reordering: cómo el usuario interactúa.
  - Bloqueo Detox: seguridad, countdown y password.
  - Incluye notas del tipo *"esto necesita una animación"*, *"aquí va un screenshot del reloj analog"*.

#### `FAQ.md`
- **¿Qué es?** Preguntas frecuentes y respuestas claras.
- **¿Por qué se usa?** Reduce la carga de soporte y aumenta la confianza del usuario antes de la descarga. Es especialmente importante para un launcher, que modifica el comportamiento fundamental del teléfono.
- **¿Para qué se usa?**
  - "¿Pierdo mis apps al instalar FocusPhone?"
  - "¿Cómo desinstalo el launcher y vuelvo al anterior?"
  - "¿Es seguro el modo Detox? ¿Y si olvido la contraseña?"
  - "¿Por qué la app necesita el permiso QUERY_ALL_PACKAGES?"
  - "¿Funciona en mi móvil?" (Android 7.0+).
  - "¿La app recopila datos personales?"

---

### 3.3 Carpeta: `03-assets/`

No contiene los archivos binarios en sí (eso es trabajo del diseñador), sino el **brief y el inventario** de lo que se debe producir.

#### `SCREENSHOTS_AND_MEDIA.md`
- **¿Qué es?** Lista de todos los recursos visuales necesarios con descripciones detalladas.
- **¿Por qué se usa?** Evita que el equipo de desarrollo frontend se quede esperando a que "alguien haga capturas". Define exactamente qué se necesita.
- **¿Para qué se usa?**
  - Inventario de screenshots por pantalla (Home, Settings, Detox, Onboarding, App Select).
  - Especificaciones de videos demo (duración, dispositivo de grabación, flujo a mostrar).
  - Lista de assets gráficos adicionales: logo en SVG, favicon, iconos para Open Graph, badges de Play Store/Google Play (si aplica en el futuro).
  - Especificaciones técnicas de exportación (formato, resolución, compresión).

#### Subcarpetas sugeridas:
- `download-assets/` → Logos, íconos de marca, archivos fuente del diseñador.
- `screenshots/` → Capturas finales optimizadas para web.

---

### 3.4 Carpeta: `04-legal/`

**Imprescindible** para publicar y distribuir un APK desde un sitio web propio. Sin estos documentos, no hay confianza ni cumplimiento normativo.

#### `PRIVACY_POLICY.md`
- **¿Qué es?** Política de privacidad completa y transparente.
- **¿Por qué se usa?** FocusPhone usa `QUERY_ALL_PACKAGES` y almacena un password localmente. Los usuarios y las tiendas de aplicaciones (incluso futuras) exigen este documento.
- **¿Para qué se usa?**
  - Declaración explícita de que la app no recopila datos personales ni los transmite a servidores externos.
  - Explicación del permiso `QUERY_ALL_PACKAGES`: solo para listar apps instaladas y mostrarlas en el cajón.
  - Tratamiento del password Detox: hash SHA-256 local en SharedPreferences, sin backup en la nube (referenciar `data_extraction_rules.xml`).
  - Uso de Room y SharedPreferences: almacenamiento 100% local y offline.
  - Contacto para ejercer derechos RGPD/CCPA (aunque no se recojan datos, se debe ofrecer un canal).
  - Fecha de última actualización de la política.

#### `TERMS_OF_USE.md`
- **¿Qué es?** Términos y condiciones de uso del software.
- **¿Por qué se usa?** Protege al autor (`rezzt.dev`) limitando la responsabilidad por el uso del launcher y estableciendo las reglas de distribución del APK.
- **¿Para qué se usa?**
  - Licencia de uso (MIT, según el repositorio).
  - Descargo de responsabilidad: el usuario asume el riesgo al establecer FocusPhone como launcher predeterminado.
  - Prohibiciones: redistribución comercial sin autorización, ingeniería inversa maliciosa.
  - Jurisdicción aplicable.

---

### 3.5 Carpeta: `05-product/`

Información operativa que el usuario necesita para **adquirir, instalar y entender** el producto desde un punto de vista práctico.

#### `DOWNLOAD_AND_INSTALL.md`
- **¿Qué es?** Guía de descarga e instalación paso a paso.
- **¿Por qué se usa?** Instalar un APK fuera de Google Play no es trivial para todos los usuarios. Este documento reduce la fricción y el abandono en el funnel de conversión.
- **¿Para qué se usa?**
  - Enlace directo al APK (releases de GitHub o hosting propio).
  - Requisitos del dispositivo: Android 7.0+ (API 24), espacio de almacenamiento.
  - Instrucciones para habilitar "Orígenes desconocidos" / "Instalar aplicaciones desconocidas".
  - Pasos post-instalación: cómo establecer FocusPhone como launcher predeterminado.
  - Guía de desinstalación / rollback al launcher anterior.
  - Troubleshooting básico ("la app no aparece como launcher", "el APK no se instala").

#### `TECH_SPECS.md`
- **¿Qué es?** Especificaciones técnicas del proyecto.
- **¿Por qué se usa?** Atrae a contribuidores open-source y satisface la curiosidad de usuarios técnicos. Refuerza la credibilidad del producto.
- **¿Para qué se usa?**
  - Stack tecnológico: Kotlin 100%, Coroutines, Room 2.8.4, Material 3, MVVM.
  - Arquitectura: Clean Architecture lite, Repository Pattern, AppContainer (DI manual).
  - Requisitos de compilación: Android Studio Ladybug+, JDK 21, AGP 9.2.1.
  - Enlace al repositorio de GitHub.
  - Notas para desarrolladores interesados en contribuir.

#### `CHANGELOG.md`
- **¿Qué es?** Historial de versiones del producto.
- **¿Por qué se usa?** Muestra que el proyecto está activo, bien mantenido y transparente sobre sus mejoras. Es señal de confianza para nuevos usuarios.
- **¿Para qué se usa?**
  - Versión actual (v1.0.4) y número de compilación.
  - Lista de cambios por versión: nuevas funciones, mejoras, correcciones de bugs.
  - Fechas de release.
  - Posibles breaking changes o migraciones necesarias.

---

## 4. Flujo de Trabajo Recomendado

Para que la creación de la web sea eficiente, se recomienda el siguiente orden:

1. **Fase de Planificación:** Revisar y completar `01-planning/`. Hasta que `WEB_STRUCTURE.md` y `DESIGN_GUIDE.md` no estén firmados, no se escribe contenido.
2. **Fase de Contenido:** Escribir `02-content/` basándose en la arquitectura aprobada y optimizando con las keywords definidas.
3. **Fase de Assets:** Producir los recursos visuales listados en `03-assets/SCREENSHOTS_AND_MEDIA.md`.
4. **Fase Legal:** Redactar `04-legal/` con precisión. Estos textos no son creativos; son contractuales.
5. **Fase Producto:** Completar `05-product/` con datos reales de la última release.
6. **Fase de Ensamblaje:** El desarrollador frontend consume estos ficheros para construir la web estática (HTML/CSS/JS, Astro, Next.js, etc.).

---

## 5. Checklist de Publicación

Antes de hacer deploy de la web al dominio público, verificar que existen y están actualizados:

- [ ] `WEB_STRUCTURE.md` — ¿Todas las rutas están definidas?
- [ ] `DESIGN_GUIDE.md` — ¿El frontend implementó la guía visual?
- [ ] `SEO_AND_KEYWORDS.md` — ¿Cada página tiene meta tags únicos?
- [ ] `PRODUCT_COPY.md` — ¿El copy está libre de errores y alineado al tono minimalista?
- [ ] `FEATURES_DETAILS.md` — ¿Todas las funcionalidades actuales están documentadas?
- [ ] `FAQ.md` — ¿Cubre las 10 preguntas más probables?
- [ ] `SCREENSHOTS_AND_MEDIA.md` — ¿Todos los assets fueron producidos y optimizados?
- [ ] `PRIVACY_POLICY.md` — ¿Refleja el comportamiento real de la app (datos locales)?
- [ ] `TERMS_OF_USE.md` — ¿Incluye la licencia MIT y el descargo de responsabilidad?
- [ ] `DOWNLOAD_AND_INSTALL.md` — ¿El enlace al APK funciona y las instrucciones son claras?
- [ ] `TECH_SPECS.md` — ¿Las versiones del stack están actualizadas?
- [ ] `CHANGELOG.md` — ¿La última versión publicada está reflejada?

---

## 6. Notas Finales

- **Mantener esta guía viva:** Cada vez que la app reciba una actualización significativa (nueva funcionalidad, cambio de permisos, nueva versión), estos ficheros deben revisarse.
- **Tono consistente:** Recuerda que FocusPhone no usa mayúsculas agresivas en su UI. La web debe respetar esa calma, aunque por razones de SEO y accesibilidad pueda usar mayúsculas estructurales (ej. en `<h1>`) de forma moderada.
- **Enfoque en la confianza:** Al distribuir un APK fuera de tiendas oficiales, la web debe transmitir máxima transparencia y seguridad. Los documentos legales (`04-legal/`) y el FAQ son tus herramientas más importantes de conversión.
