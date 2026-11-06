# FocusPhone — auditoría y corrección completa del repositorio

## ESPAÑOL DE ESPAÑA: USO OBLIGATORIO DE Ñ

- todo texto en castellano debe usar español de españa (`es-ES`) y su ortografia, tambien en titulos y cuerpos de commits, changelogs, pull requests, merges y documentacion
- escribir siempre `ñ` en las palabras que la llevan y `Ñ` cuando corresponda la mayuscula; nunca sustituirlas por `n` o `N`
- la `ñ` es una letra distinta de la `n`; las instrucciones «sin acentos» o «sin tildes» solo afectan a las tildes de las vocales y nunca permiten eliminar la eñe
- ejemplos correctos: `añadir`, `diseño`, `año`, `tamaño`, `contraseña`, `español`, `españa` y `AÑADIDO`; sus formas con `n` son incorrectas cuando se refieren a esas palabras españolas
- conservar literalmente rutas, urls, nombres de archivo, identificadores tecnicos y texto de otros idiomas; esta regla no autoriza renombrarlos ni aplicar sustituciones indiscriminadas
- antes de entregar o publicar texto en castellano, revisar que ninguna palabra haya perdido su `ñ` o `Ñ`; esta regla prevalece sobre cualquier normalizacion a ascii o instruccion generica de eliminar acentos


> fichero de trabajo para un agente de ia (claude code) que opera en el clon local del launcher
> (`focusphone-app/`, módulo android en `app/`). contiene todo lo necesario para auditar y
> corregir documentación, aspectos legales, higiene del repositorio y release en una sola pasada.
> **se versiona** junto con `.docs/manager.md` y `.docs/-github-guides/`; los recursos, notas y skills internos permanecen locales. preparar este procedimiento no implica ejecutar la auditoría.
>
> cómo usarlo: abre claude code en la raíz de este repositorio y escribe
> `lee repo-audit-and-fix.md y ejecútalo de principio a fin`.

---

## 0. PROMPT DE LANZAMIENTO

```text
Eres el agente responsable de dejar el repositorio FocusPhone (launcher android) profesional,
completo y consistente con la release pública v1.4-beta, en inglés y castellano.
Sigue este documento fase por fase, en orden. No es asesoría legal: todo texto legal que redactes
se marca para revisión de un abogado.

Reglas de ejecución:
1. Fase 1 es de solo lectura. Al terminarla, haz TODAS las preguntas de la sección 3 en una sola
   interacción (AskUserQuestion, agrupadas) y espera. No preguntes nada más después, salvo bloqueo real.
   Lo que no se responda se resuelve con el valor "por defecto" de la sección 3 y se anota como
   TODO(owner) en el informe final.
2. Lee antes `claude.md`, `agents.md`, `.github/contributing.md`, `.docs/manager.md` y
   `.docs/-github-guides/commit-guide.md` si existen. Son la fuente de verdad: si contradicen este
   documento, gana el repositorio y lo indicas en el informe.
3. Nunca ejecutes sin confirmación expresa: push (menos aún forzado), reescritura de historial,
   mover o borrar tags, crear/editar/publicar releases, cambiar ajustes del repo en GitHub,
   cambiar la rama por defecto. Prepara el comando exacto y lístalo en el informe final.
4. Verifica toda afirmación contra el código y contra GitHub (`gh`). No inventes datos legales,
   técnicos, cifras ni capturas. Si falta un dato: `TODO(owner): <qué falta>`.
5. Trabaja en una rama nueva `changes/repo-audit-and-fix` creada desde la rama oficial acordada.
   Commits pequeños y temáticos según la sección 6. Sin push ni PR hasta que yo lo apruebe.
6. Todo documento nuevo o modificado debe existir en inglés y en castellano con paridad exacta.
7. Termina con el informe de la sección 8.
```

---

## 1. CONTEXTO HISTÓRICO DE LA AUDITORÍA INICIAL

esta tabla conserva la fotografía anterior a las correcciones del proyecto. no describe necesariamente la rama actual. el agente debe **re-verificar cada dato** contra el código, los metadatos y la publicación vigentes antes de actuar. la política actual de documentación es versionar únicamente las nueve guías de gestión y conservar los demás contenidos de `.docs/` como recursos locales.

| tema | hallazgo |
|---|---|
| repositorio | `https://github.com/rezzt-dev/focusphone-app` (público). ramas: `main`, `beta-1.5`, `version-beta`, `version-alfa`. |
| tag / release | `v1.4-beta`, asset `focusphone-v1.4-beta.apk` (4 435 516 bytes), sha256 `0818562f13b4f950cce4b315e7a0956bb051695cbce27e426a5adaba78e119eb`. `prerelease: false`. |
| tag desalineado | el tag apunta a `62d80bf7` = `main` (merge de "alfa1.0.1"), pero el APK contiene código beta (clases `ui.widget.library.*Widget`, `WidgetPickerActivity`). |
| `main` desfasado | solo tiene `README.md` antiguo y código alfa. sin licencia, seguridad, CI, documentación ni changelog. su README dice licencia MIT y una URL de clonado errónea (`rezzt-devc/focusphone-project`). |
| ramas con el trabajo real | `version-alfa` y `beta-1.5` (HEAD local `aa0b9c1`) traen `.github/`, `documentation/{en,es}` (51 ficheros), `changelog.md`, `claude.md`, `agents.md`, `contributing.md`, `code_of_conduct.md`, `security.md`, `license`, `readme.md`, `renovate.json`, `config/detekt/detekt.yml`. |
| `.docs/` versionado | `beta-1.5` y `version-beta` versionan 447 ficheros de `.docs/` (≈75 paquetes `agent-skills` de terceros y análisis internos). `version-alfa` no. el changelog de la release dice que `.docs/` ya no se versiona. |
| versión | `versionName = "1.0.6"`, `versionCode = 3` en `app/build.gradle.kts`; la release y la web dicen 1.4 beta. el "acerca de" de la app mostraría otra versión. |
| licencia | fichero `license`: licencia de uso no comercial, © 2026 Juan García Cazallas ("rezzt-dev"), **solo en castellano**. GitHub no detecta licencia (`licenseInfo: null`). |
| formato de release | tag con sufijo (`v1.4-beta`; la guía exige `v<x.y.z>`), cuerpo narrativo (la guía exige categorías), sin marca de pre-release. |
| changelog | `## SIN PUBLICAR / UNRELEASED` sin cerrar con versión y fecha. |
| metadatos | homepage `https://rezzt.dev/project/?slug=enki` (nombre antiguo); descripción en minúsculas de la era alfa. |
| privacidad (APK) | el manifest del APK no declara `INTERNET`. declara `QUERY_ALL_PACKAGES`, `REQUEST_DELETE_PACKAGES`, `EXPAND_STATUS_BAR` y servicios con `BIND_ACCESSIBILITY_SERVICE` y `BIND_NOTIFICATION_LISTENER_SERVICE`. |
| web | `https://focusphone.rezzt.dev` (repo `focusphone-web`) enlaza a `…/releases/download/v1.4-beta/focusphone-v1.4-beta.apk`, a `…/releases` y a `…#readme` de `main`. **no romper esos enlaces** sin avisar. |
| firma del APK | no verificada: `apksigner` no estaba disponible. hay que comprobarla. |

---

## 2. FASES

### Fase 1 — auditoría (solo lectura)

1. `git fetch --all --tags`; compara `main`, `version-alfa`, `beta-1.5`, `version-beta` y el tag con `git log --oneline --graph --decorate --all -n 40` y `git diff --stat`.
2. Descarga el APK a un directorio nuevo y vacío fuera del repo; trátalo como no confiable. Calcula `sha256sum`, lista el manifest (`aapt2 dump badging` o `apkanalyzer` si hay Android SDK en `~/Android/Sdk`; si no, indícalo), y comprueba `apksigner verify --print-certs`. Averigua de qué commit salió: compara `versionCode`/`versionName`, lista de clases y recursos con cada rama.
3. Inventario real: permisos y servicios del `AndroidManifest.xml` de la rama oficial, dependencias de `gradle/libs.versions.toml`, fuentes y recursos embebidos (`res/font`, `assets/`, `open_source_licenses.html`), iconos Lucide, `data_extraction_rules.xml`, `locales_config`.
4. Estado de `documentation/{en,es}`: paridad de ficheros y secciones, enlaces rotos, y qué falta frente al código (sección 4, fase 5).
5. Busca secretos y datos personales: keystores, tokens, `local.properties`, correos, rutas locales (`git grep -nIE`). Revisa `.idea/` y `.vscode/` versionados.
6. Entrega el informe de hallazgos (confirmados / refutados / nuevos) y pasa a la sección 3.

### Fase 2 — higiene del repositorio

- `.gitignore`: conservar las excepciones para `.docs/manager.md`, `.docs/repo-audit-and-fix.md` y `.docs/-github-guides/*.md`; ignorar el resto de documentación interna, estado del ide, configuración local, claves y artefactos de build según la política del proyecto.
- revisar los archivos del ide y artefactos versionados por ruta. no retirar del índice las guías de gestión; cualquier limpieza adicional se prepara como un cambio separado y conserva los archivos locales.
- Si `.docs/agent-skills` redistribuye material de terceros sin licencia compatible, **proponer** (sin ejecutar) la purga del historial con `git filter-repo`, indicando el impacto: reescritura de historial, tags y forks. Alternativa: dejar el historial y solo dejar de versionar.
- Asegurar que `claude.md`/`agents.md` duplican las guidelines necesarias para que no dependan de `.docs/`.

### Fase 3 — documentos legales (EN + ES, cada uno con paridad)

formato bilingüe: sigue el criterio ya usado en el repo (`documentation/en` + `documentation/es`). para ficheros raíz, un único fichero con inglés primero y castellano después, separados por encabezados, o `X.md` + `X.es.md` enlazados entre sí. elige uno, justifícalo en una línea y aplícalo a todos.

| fichero | contenido mínimo |
|---|---|
| `license` | traducción fiel al inglés de la licencia no comercial actual, cláusula de prevalencia (propuesta: inglés), garantía/ausencia de garantía, limitación de responsabilidad, terminación, ley aplicable y jurisdicción (preguntar), contacto. Aclarar que **no** es una licencia de código abierto aprobada por la OSI. Evaluar si renombrar a `LICENSE` mejora la detección de GitHub. |
| `NOTICE` y `THIRD_PARTY_LICENSES.md` | cada dependencia (de `libs.versions.toml`), fuente (Poppins, Comfortaa, Figtree, JetBrains Mono, Space Mono: **verifica la licencia de cada una**) e icono (Lucide, ISC): nombre, versión, licencia, URL y texto o enlace. Sincronizar `assets/open_source_licenses.html` con este inventario. |
| `privacy.md` | datos tratados (ninguno sale del dispositivo, sin `INTERNET`, sin telemetría ni cuentas), qué se guarda localmente y cómo se cifra, copias de seguridad y transferencia, cada permiso con su motivo y efecto de denegarlo, derechos del usuario (RGPD / LOPDGDD si aplica), menores, cambios de la política, contacto. Contrastar con `documentation/*/users/11-*` y el manifest. |
| `permissions.md` | justificación técnica de cada permiso sensible para revisión de tiendas y auditoría. |
| `security.md` | versiones soportadas (tabla real), canal privado de informe (preguntar), plazos, divulgación coordinada, alcance. Sin recompensas. Incluir huella del certificado de firma si se verifica. |
| `code_of_conduct.md` | Contributor Covenant 2.1 con contacto de cumplimiento. |
| `contributing.md` | setup, convención de commits, changelog bilingüe, pipeline de verificación, reglas no negociables. |
| `.github/` | plantillas de issue y PR bilingües, `CODEOWNERS`, `SUPPORT.md`; revisar `dependabot.yml`, `renovate.json` y workflows (política de dependencias fijadas a mano). |

### Fase 4 — readme y changelog

- `readme.md` EN + ES: badges reales (versión, SDK, licencia no comercial, CI), características verificadas, instalación del APK, verificación del SHA-256 y de la firma, compilación desde fuente, estructura, enlaces a documentación, privacidad, seguridad y licencia, estado (beta). Sin afirmaciones no verificables ni la palabra "open source" para esta licencia.
- Corregir el README de `main` (URL de clonado, MIT, estructura) o sustituirlo por el de la rama oficial.
- `changelog.md`: cerrar `## SIN PUBLICAR / UNRELEASED` como `## [x.y.z] - aaaa-mm-dd` con la versión acordada (commit con prefijo `changelog`). Comprobar que cada ítem existe en el código. Mantener categorías y estilo de la sección 0.

### Fase 5 — documentación técnica y de usuario (EN + ES)

auditar `documentation/en` y `documentation/es` contra el código y completar lo que falte, con paridad exacta de ficheros, secciones, datos y enlaces:

- pantalla de inicio en cuadrícula: 3-6 columnas × 4-12 filas, modo edición, acciones de accesibilidad, migración desde la columna fija
- los 20 widgets integrados (lista real extraída del código: `ui/widget/library`), selector de widgets, aviso de "widget no disponible"
- pausa consciente guiada (disco, anillo, tick háptico, minutos del límite diario)
- tamaño de texto de inicio (8-24 pt) separado del de la app
- fondo de pantalla: vista previa, legibilidad, "usar el fondo de inicio", guarda de contraste del tema personalizado
- bloqueo de detox y horarios: 5 intentos y 15 minutos de espera, verificación fuera del hilo principal
- almacenamiento seguro y exclusión de transferencia en Android 12+
- análisis estático (ktlint, detekt), política de dependencias, CI
- capturas: solo si puedes generarlas; si no, `TODO(owner): screenshot <pantalla>`

### Fase 6 — versión y release (solo preparar)

preparar, sin ejecutar:

1. cambio de `versionName`/`versionCode` según lo acordado (sección 3), con nota si exige reconstruir el APK.
2. texto de release en inglés: título `v<x.y.z> — <summary en minúsculas, sin acentos (conservando siempre `ñ` y `Ñ`)>`, cuerpo = bloque `### ENGLISH` del changelog con categorías `ADDED / CHANGED / FIXED / REMOVED / SECURITY`, marcada como pre-release.
3. plan de alineación tag ↔ binario con opciones y riesgos: (a) nuevo tag sobre el commit del que salió el APK; (b) reconstruir y firmar un APK desde el commit del tag nuevo y publicar nueva release; (c) mantener tag actual y documentar. recomendar una.
4. `SHA256SUMS` del APK y nota de verificación de firma.
5. impacto en la web: la web usa el nombre `focusphone-v1.4-beta.apk` y el tag `v1.4-beta`; listar qué cambiaría en `focusphone-web/web/src/data/site.ts`.
6. comandos `gh` exactos listos para pegar (release, edit, upload), sin ejecutarlos.

### Fase 7 — metadatos de GitHub (lista para que los aplique el propietario)

descripción en inglés, homepage `https://focusphone.rezzt.dev`, topics, licencia detectable, social preview, rama por defecto, private vulnerability reporting, discussions (si procede), protección de la rama oficial, comandos `gh api` exactos.

---

## 3. DECISIONES A PREGUNTAR (una sola vez, tras la fase 1)

| # | pregunta | por defecto si no hay respuesta |
|---|---|---|
| 1 | rama oficial y cómo llega su contenido a `main` | `beta-1.5` fusionada en `main` con merge commit, vía PR |
| 2 | esquema de versionado | `versionName = "1.4.0-beta"`, `versionCode` +1; tag `v1.4.0` y release marcada pre-release |
| 3 | tag y APK desalineados | opción (b): reconstruir y firmar desde el commit correcto |
| 4 | idioma y formato de los docs raíz | `X.md` bilingüe (EN primero, ES después) |
| 5 | cláusula de prevalencia | inglés prevalece |
| 6 | jurisdicción y ley aplicable | `TODO(owner)` |
| 7 | correo de contacto para privacidad, seguridad y conducta | `TODO(owner)`; sugerir GitHub private vulnerability reporting |
| 8 | purgar `.docs/agent-skills` del historial | no purgar; solo dejar de versionar |
| 9 | renombrar `license` → `LICENSE` | sí, si GitHub lo detecta mejor; si no, mantener |
| 10 | nombre de la entidad titular | "Juan García Cazallas (rezzt-dev)" |

---

## 4. REGLAS DE ESTILO

- commits, changelog, PR y release: sección 0 y las guidelines del repo. changelog en minúsculas y sin acentos (conservando siempre `ñ` y `Ñ`); fechas `aaaa-mm-dd`.
- documentación y readme: castellano e inglés con acentos normales; mismo orden, mismos títulos traducidos, mismas tablas y cifras.
- nombres de ficheros, rutas, comandos e identificadores entre backticks.
- sin emojis decorativos, sin superlativos de marketing, sin afirmaciones de "seguro" o "infalible".
- los bloqueos de la app son un compromiso personal, no una medida de seguridad contra un atacante: reflejarlo.

---

## 5. COMPROBACIONES (ejecutar y pegar resultados en el informe)

```bash
# enlaces relativos de todos los markdown
git ls-files '*.md' | while read f; do grep -oE '\]\((\./|\.\./)?[^)#h][^)]*\)' "$f" \
  | sed -E 's/^\]\(|\)$//' | while read l; do
    [ -e "$(dirname "$f")/$l" ] || echo "ROTO: $f -> $l"; done; done

# paridad en/es en documentation/
diff <(cd documentation/en && find . -type f | sed -E 's#/[^/]*$##' | sort | uniq -c) \
     <(cd documentation/es && find . -type f | sed -E 's#/[^/]*$##' | sort | uniq -c)

# acentos o mayúsculas en items del changelog (debe salir vacío en las listas)
grep -nP '^- .*[áéíóúñÁÉÍÓÚ]' changelog.md

# permisos reales vs documentados
grep -n 'uses-permission\|<service\|permission=' app/src/main/AndroidManifest.xml

# verificación del proyecto (si se tocó código o recursos)
./gradlew staticAnalysis --continue --stacktrace \
  && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease --stacktrace

# APK publicado
sha256sum focusphone-v1.4-beta.apk   # esperado: 0818562f13b4f950cce4b315e7a0956bb051695cbce27e426a5adaba78e119eb
apksigner verify --print-certs focusphone-v1.4-beta.apk
```

---

## 6. PLAN DE COMMITS (rama `changes/repo-audit-and-fix`)

orden sugerido; ajusta al estado real del repo.

1. `changes | conservar las guias de gestion y revisar los archivos locales del ide`
2. `update | añadir la licencia bilingüe, NOTICE y THIRD_PARTY_LICENSES`
3. `update | añadir la política de privacidad y la justificación de permisos`
4. `update | completar security, code of conduct y contributing en inglés y castellano`
5. `update | añadir plantillas de issue y pr bilingües y SUPPORT`
6. `update | completar la documentación de usuario y desarrollador de la v1.4 en inglés y castellano`
7. `update | rehacer el readme bilingüe con datos verificados`
8. `changes | alinear versionName y versionCode con la release` (solo si se acordó)
9. `changelog | cerrar la sección de changelog para la versión <x.y.z>`

cada cuerpo: lista `-` de una línea por ítem, qué y por qué, sin firmas ni líneas de atribución automática.

---

## 7. DEFINICIÓN DE HECHO

- [ ] ningún hallazgo de la sección 1 queda sin resolver, propuesto o anotado como `TODO(owner)`
- [ ] `license`, `NOTICE`, `THIRD_PARTY_LICENSES.md`, `privacy.md`, `permissions.md`, `security.md`, `code_of_conduct.md`, `contributing.md` y `readme.md` existen en EN y ES con paridad
- [ ] el README no dice MIT ni "open source" y las URLs de clonado son correctas
- [ ] `documentation/{en,es}` cubre cuadrícula, widgets, pausa guiada, tamaño de texto, fondo, bloqueo de 5 intentos y almacenamiento seguro
- [ ] las comprobaciones de la sección 5 pasan o sus fallos están explicados
- [ ] las nueve guías de gestión están versionadas; los demás recursos locales y archivos del ide siguen la política de exclusión vigente
- [ ] `changelog.md` cerrado con versión y fecha, bilingüe, con las cinco categorías
- [ ] propuesta de release en inglés, formato de categorías, pre-release, plan de tag ↔ APK y `SHA256SUMS` listos
- [ ] ninguna acción prohibida de la sección 0 se ha ejecutado sin confirmación
- [ ] los enlaces que usa la web siguen funcionando o el cambio está documentado

---

## 8. INFORME FINAL (formato de entrega)

1. **resumen**: qué se hizo, en cinco líneas como máximo.
2. **hallazgos**: tabla confirmado / refutado / nuevo, con evidencia (comando o ruta).
3. **ficheros**: creados, modificados y eliminados del índice.
4. **decisiones aplicadas**: las respondidas y las resueltas por defecto.
5. **`TODO(owner)`**: lista numerada de datos que faltan.
6. **para revisión legal**: puntos concretos que debe revisar un abogado.
7. **acciones pendientes de mi confirmación**: comandos exactos (push, PR, tag, release, ajustes de GitHub, purga de historial).
8. **título y descripción de la PR** en el formato del repositorio, en bloques de código separados.
9. **cambios necesarios en `focusphone-web`**: ficheros y valores concretos.
