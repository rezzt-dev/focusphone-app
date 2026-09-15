# FocusPhone — claude.md

> This file exists so Claude (and any other AI agent) always has the git
> workflow rules available, independent of `.docs/` (gitignored) being
> present locally. For the full architecture, tech stack, build commands and
> code style guide, see [`agents.md`](agents.md) — this file only duplicates
> the commit / PR / merge / release conventions.

## Commit, PR, Merge & Release Guidelines

> `.docs/` is gitignored (not present in every clone or agent environment), so the
> full content of `.docs/commit-guidelines.md` and `.docs/pr-merge-release-guidelines.md`
> is duplicated below verbatim. Both `agents.md` and `claude.md` carry the same copy so
> any agent has it regardless of which file it reads first. If `.docs/` is present
> locally, treat it as the source of truth and keep this copy in sync with it.

### `.docs/commit-guidelines.md`

# COMMIT GUIDELINES

> instrucciones para cualquier agente de ia (o persona) que redacte el titulo
> y la descripcion de un commit de git en este repositorio. se aplica siempre
> que se pida algo equivalente a:
>
> *"sacame el titulo y la descripcion para el commit con los cambios
> realizados desde el ultimo commit. fijate en los anteriores commits para
> que tengan el mismo formato."*
>
> y, de forma general, **cada vez que se vaya a crear un commit en este
> repositorio**, se haya pedido explicitamente o no.

---

#### FORMATO FIJO DEL TITULO

```
<prefijo> | <resumen corto en imperativo>
```

el separador es siempre espacio, barra vertical, espacio: ` | `. el
`<prefijo>` es siempre una de estas cinco palabras, en minusculas, y nunca
se combinan ni se inventan otras nuevas:

| prefijo | cuando usarlo |
|---|---|
| **update** | trabajo normal hacia adelante: se añade o mejora una funcionalidad, contenido o documento existente. es el prefijo por defecto. |
| **fix** | se corrige un error o comportamiento incorrecto detectado durante el desarrollo, sin urgencia de publicacion inmediata. |
| **hotfix** | se corrige algo urgente que afecta a una version ya publicada o a un bloqueo critico. debe usarse solo cuando la correccion es realmente urgente. |
| **changes** | cambios estructurales o no funcionales: reorganizar carpetas, renombrar ficheros, tocar configuracion o tooling, dependencias. no anade funcionalidad ni corrige un bug. |
| **changelog** | el unico proposito del commit es editar `changelog.md` (por ejemplo, al cerrar una version). |

ejemplos de titulos correctos:

```
update | añadir vista previa de mapeo de columnas al asistente de importación csv
fix | detección de duplicados fallando en filas con importes negativos
hotfix | fallo al iniciar cuando falta el archivo de perfil de importación
changes | reorganizar la documentación en la subcarpeta project-context
changelog | cerrar la sección de changelog para la versión mvp fase 0
```

---

#### FORMATO DE LA DESCRIPCION (CUERPO)

- el cuerpo es siempre **una lista de markdown no numerada**: cada item empieza
  por `-`. nunca se usan listas numeradas ni parrafos sueltos.
- cada item es un bloque logico en imperativo, escrito como **una sola linea
  continua**, sin saltos de linea internos: no son esteticos ni utiles, deja
  que el cliente de git lo ajuste. no se dejan lineas en blanco entre items.
- usa markdown dentro de cada item: nombres de ficheros, rutas, comandos e
  identificadores siempre entre backticks.
- explica el que y el por que, no el detalle linea a linea.
- cierra con una linea en blanco y la linea de atribucion que corresponda al
  entorno.

---

#### COMO PRESENTAR EL RESULTADO

al responder con el titulo y la descripcion hay que usar **bloques de codigo
separados**: uno para el titulo del commit y otro para el cuerpo del commit (mas
otro para el fragmento del changelog). nunca se juntan el titulo y el cuerpo en
el mismo bloque.

### `.docs/pr-merge-release-guidelines.md`

# PR, MERGE Y RELEASE GUIDELINES

> instrucciones para cualquier agente de ia (o persona) que redacte el titulo
> y la descripcion de una **pull request**, de un **commit de merge** o de una
> **release de github** en este repositorio, de forma que su contenido lea
> como una extension directa de `changelog.md` en lugar de un formato distinto
> por cada canal. se aplica siempre que se pida algo equivalente a:
>
> *"sacame el titulo y la descripcion para la pull request / el merge / la
> release con los cambios de esta version. usa el mismo formato que
> `changelog.md`."*
>
> y, de forma general, **cada vez que se vaya a abrir una pull request, hacer
> un merge o publicar una release en este repositorio**, se haya pedido
> explicitamente o no.

este documento asume conocido el formato de `changelog.md` (descrito en
`agents.md` y `claude.md`, seccion "formato de las secciones de
`changelog.md`") y el de los commits (`.docs/commit-guidelines.md`). no lo
repite salvo lo estrictamente necesario.

---

#### PRINCIPIO GENERAL

pull requests, merges y releases **no inventan su propio formato**: reusan
literalmente el contenido y las reglas de estilo de `changelog.md`.

- **minusculas y sin acentos** en todo el contenido; los titulos/encabezados
  van en mayusculas y sin acentos.
- **categorias cerradas**, las mismas cinco de `changelog.md` y en el mismo
  orden — nunca, cambios, corregido, eliminado, seguridad
  (added / changed / fixed / removed / security) — omitiendo la que no tenga
  items.
- **una linea por item**, en imperativo, con nombres de ficheros, rutas,
  comandos e identificadores entre backticks.
- las fechas usan el formato `aaaa-mm-dd`.
- las entradas `[online]` / `[offline]` de `changelog.md` (cambios que solo
  afectan a una de las dos versiones de la app) se conservan tal cual en pr,
  merge y release: no se separan ni se traducen a otro marcador.

la fuente de verdad es siempre `changelog.md`: antes de redactar una pr, un
merge o una release, la seccion `## SIN PUBLICAR / UNRELEASED` (o la version
ya cerrada correspondiente) debe estar actualizada. el contenido de pr, merge
y release se **extrae** de ahi, no se redacta desde cero ni en paralelo.

---

#### 1. PULL REQUESTS

hay dos tipos de pr en este repositorio y cada una tiene su propio formato de
titulo; la descripcion es igual para las dos.

##### 1.1 titulo

**pr de trabajo normal** (una rama `feature/...` o `fix/...` contra
`testing-branch`, o cualquier pr que no cierra una version): mismo formato
que el titulo de un commit, definido en `.docs/commit-guidelines.md`:

```
<prefijo> | <resumen corto en imperativo>
```

usando el mismo prefijo (`update`, `fix`, `hotfix`, `changes`, `changelog`) y
las mismas reglas — si la pr agrupa varios commits, el resumen describe el
conjunto, no cada commit por separado.

**pr de publicacion de version** (`testing-branch` → `release-branch` /
`main`, cierra una version de `changelog.md`): sigue el patron ya usado en
los merges anteriores del repositorio (`release version 1.6 - supabase cloud
sync, onyx theme and task detail redesign`):

```
release version <x.y.z> - <resumen corto en imperativo>
```

el resumen corto resume las 2-3 entradas mas relevantes de la version, igual
que en el ejemplo anterior.

##### 1.2 descripcion

la descripcion es el bloque `### CASTELLANO` + `### ENGLISH` de la seccion
correspondiente de `changelog.md` (la version que cierra la pr, o el
subconjunto de `## SIN PUBLICAR / UNRELEASED` que aporta esa pr), pegado tal
cual, con sus encabezados `####` de categoria:

```
### CASTELLANO

#### <categoria>
- <cambio en una linea>

### ENGLISH

#### <category>
- <same change, one line>
```

si la pr solo aporta una parte de `## SIN PUBLICAR / UNRELEASED` (por
ejemplo, varias pr pequenas alimentan la misma version antes de cerrarla),
la descripcion incluye solo los items que introduce esa pr, no la seccion
entera.

no se anaden parrafos sueltos, capturas de pantalla incrustadas como texto,
ni resumenes adicionales fuera de la lista: si hace falta contexto extra (por
ejemplo, capturas de pantalla de cambios visuales, ver `agents.md` seccion
10), se anade **despues** de la lista, nunca sustituyendola.

---

#### 2. COMMIT DE MERGE

github genera el commit de merge a partir del titulo y la descripcion de la
pr automaticamente (`Merge pull request #<n> from <rama>` como primera linea,
seguido del titulo y cuerpo de la pr). por tanto:

- si la pr cumple las secciones 1.1 y 1.2, el commit de merge ya cumple este
  formato sin trabajo adicional.
- usa siempre **"create a merge commit"** al fusionar (nunca squash ni
  rebase): el historial de commits individuales, redactado segun
  `.docs/commit-guidelines.md`, debe conservarse completo junto al resumen de
  la pr en el commit de merge.
- si el merge se hace manualmente por git (`git merge --no-ff`), el mensaje
  del commit de merge se redacta a mano siguiendo el mismo titulo (seccion
  1.1) y el mismo cuerpo (seccion 1.2) que llevaria la pr equivalente.

---

#### 3. RELEASES DE GITHUB

las releases de github son **en ingles unicamente** (no bilingues, a
diferencia de `changelog.md` y de las pr): son la cara publica del proyecto
de cara a usuarios que no necesariamente leen castellano.

##### 3.1 titulo de la release

```
v<x.y.z> — <resumen corto en ingles, minusculas, sin acentos>
```

ejemplo (formato ya usado en `.docs/releases-docs/`):

```
v1.7 — 100% offline, reminders redesign & settings categories
```

##### 3.2 descripcion de la release

el cuerpo reutiliza literalmente el bloque `### ENGLISH` de la version
cerrada en `changelog.md`, con sus categorias en mayusculas y sin acentos,
igual que en el changelog:

```
#### ADDED
- <change in one line>

#### CHANGED
- <change in one line>

#### FIXED
- <change in one line>

#### REMOVED
- <change in one line>

#### SECURITY
- <change in one line>
```

se omite la categoria que no tenga items, igual que en `changelog.md`. no se
reescribe el contenido en parrafos narrativos con negritas ni se reordenan
las categorias: la release debe poder generarse copiando y pegando el bloque
`### ENGLISH` de la version correspondiente.

> nota: los releases anteriores a esta guia (incluidos los de
> `.docs/releases-docs/release-1.7/`) usan un formato narrativo distinto
> (parrafos con titulo en negrita en vez de las cinco categorias). son
> historicos y no se reescriben con esta regla; a partir de esta guia, toda
> release nueva usa el formato de categorias descrito arriba.

##### 3.3 tag

el tag de git asociado a la release es siempre `v<x.y.z>` (mismo numero de
version que la cabecera de `changelog.md`, sin sufijos adicionales).

---

#### FLUJO RESUMIDO

1. durante el desarrollo, cada commit sigue `.docs/commit-guidelines.md` y
   cada cambio relevante se anade a `## SIN PUBLICAR / UNRELEASED` en
   `changelog.md` en su categoria correspondiente.
2. cada pr (normal o de version) usa el titulo y la descripcion definidos en
   la seccion 1, extraidos de `changelog.md`.
3. al fusionar, se usa "create a merge commit" para que el commit de merge
   herede el formato de la pr (seccion 2).
4. al cerrar una version: se renombra `## SIN PUBLICAR / UNRELEASED` a
   `## [x.y.z] - aaaa-mm-dd` en `changelog.md` (commit con prefijo
   `changelog`), se abre la pr de publicacion de version (seccion 1.1), y al
   fusionarla se crea la release de github (seccion 3) con el tag `v<x.y.z>`.
