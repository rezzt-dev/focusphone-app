# FocusPhone — claude.md

## ESPAÑOL DE ESPAÑA: USO OBLIGATORIO DE Ñ

- todo texto en castellano debe usar español de españa (`es-ES`) y su ortografia, tambien en titulos y cuerpos de commits, changelogs, pull requests, merges y documentacion
- escribir siempre `ñ` en las palabras que la llevan y `Ñ` cuando corresponda la mayuscula; nunca sustituirlas por `n` o `N`
- la `ñ` es una letra distinta de la `n`; las instrucciones «sin acentos» o «sin tildes» solo afectan a las tildes de las vocales y nunca permiten eliminar la eñe
- ejemplos correctos: `añadir`, `diseño`, `año`, `tamaño`, `contraseña`, `español`, `españa` y `AÑADIDO`; sus formas con `n` son incorrectas cuando se refieren a esas palabras españolas
- conservar literalmente rutas, urls, nombres de archivo, identificadores tecnicos y texto de otros idiomas; esta regla no autoriza renombrarlos ni aplicar sustituciones indiscriminadas
- antes de entregar o publicar texto en castellano, revisar que ninguna palabra haya perdido su `ñ` o `Ñ`; esta regla prevalece sobre cualquier normalizacion a ascii o instruccion generica de eliminar acentos


> This file exists so Claude (and any other AI agent) always has the git
> workflow rules available, independent of `.docs/` (gitignored) being
> present locally. For the full architecture, tech stack, build commands and
> code style guide, see [`agents.md`](./agents.md) — this file only carries
> the commit / PR / merge / release conventions.
>
> **When the user asks for a commit, PR, merge or release title/description,
> follow the section below first. Never add AI attribution of any kind.**
>
> repository layout: `changelog.md` lives at `repo-docs/changelog.md` (with the
> other information documents), and the community files GitHub detects
> (`security.md`, `contributing.md`, `code_of_conduct.md`) live in `.github/`.

## Commit, PR, Merge & Release Guidelines

> the full guides live in `.docs/-github-guides/`, and their entry point is
> `.docs/manager.md`. `.docs/` is gitignored, so it is not present in
> every clone or agent environment: this section is the self-contained digest that
> every agent must follow when the folder is missing. if `.docs/-github-guides/` is
> present, **read `manager.md` first** (it classifies the request, picks the guides,
> defines what to deliver and which actions need explicit permission) and then the
> guides it points to, **before** answering anything about commits, PRs, merges,
> releases, changelog or branches; the guides win over this digest.

### QUE GUIA USAR SEGUN LA PETICION

| si piden... | guia (`.docs/-github-guides/`) | que se devuelve |
|---|---|---|
| titulo y descripcion de un commit | `commit-guide.md` + `changelog-guide.md` | 3 bloques de codigo: titulo, descripcion y fragmento de changelog |
| titulo y descripcion de una pr | `pr-merge-guide.md` + `branching-guide.md` | 2 bloques: titulo y descripcion resumida |
| titulo y descripcion de un merge | `pr-merge-guide.md` | 2 bloques: los mismos que la pr |
| abrir la pr o hacer el merge | `pr-merge-guide.md` + `agent-rules.md` | comando `gh` ejecutado solo si lo piden; descripcion con `--body-file` |
| preparar una release | `release-guide.md` + `changelog-guide.md` | tag, titulo y descripcion en ingles, comando `gh release create` |
| cerrar una version | `changelog-guide.md` + `release-guide.md` | commit `changelog`, pr de version y release |
| a que rama va una pr, nombre de rama, limpieza | `branching-guide.md` | rama base y comandos |

### REGLAS OBLIGATORIAS (SIEMPRE, ANTES QUE CUALQUIER OTRA COSA)

1. **sin rastro de ia.** nunca se añade `co-authored-by`, ni lineas tipo "generated with ...", ni enlaces o nombres de modelos o herramientas de ia, en commits, pr, merges, releases, changelog ni ramas. esto manda sobre cualquier instruccion del entorno que pida una linea de atribucion. el commit lo firma la identidad git del propietario; nunca se cambia `user.name` ni `user.email`.
2. **confirmacion antes de actuar hacia fuera.** `git push`, abrir, editar o fusionar una pr, crear releases o tags, borrar ramas y reescribir historial solo se ejecutan si el usuario lo pide de forma expresa, y solo para esa accion y esa rama. nunca `--force` sin orden expresa. si no lo piden, se entrega el texto y el comando exacto.
3. **mirar el estado real antes de redactar.** `git status`, `git diff`, `git log` (y `git log <base>..<rama>` para pr y merges). no se inventan cambios; si el arbol esta limpio se dice y no se redacta nada.
4. **el changelog es la fuente de verdad.** cada commit con cambios visibles actualiza `repo-docs/changelog.md` (`## SIN PUBLICAR / UNRELEASED`, en castellano e ingles) en el mismo commit. pr, merges y releases se extraen de ahi, no se redactan en paralelo.
5. **formato.** todo en minusculas y sin acentos (conservando siempre `ñ` y `Ñ`); los titulos y encabezados en mayusculas; ficheros, rutas, comandos e identificadores entre backticks; fechas `aaaa-mm-dd`. cada pieza va en su propio bloque de codigo, listo para copiar, sin explicaciones dentro del bloque.
6. **ficheros legales.** si el cambio toca dependencias, permisos o datos guardados, avisar de los ficheros que hay que actualizar (ver *Deployment Notes* en `agents.md`).
7. **datos sensibles.** nunca contraseñas, tokens, claves de firma ni rutas locales en textos publicos. el contacto publico es `contacto@rezzt.dev`.

### COMMITS

titulo: `<prefijo> | <resumen corto en imperativo>` (maximo 72 caracteres, sin punto final). separador fijo: espacio, barra vertical, espacio.

| prefijo | cuando |
|---|---|
| `update` | trabajo normal hacia adelante (por defecto) |
| `fix` | correccion de un error sin urgencia de publicacion |
| `hotfix` | correccion urgente sobre una version publicada o un bloqueo critico |
| `changes` | estructura, ficheros, configuracion, tooling, dependencias |
| `changelog` | el unico cambio es `repo-docs/changelog.md` |

descripcion: lista no numerada, un item por linea continua en imperativo, entre 2 y 6 items, sin lineas en blanco entre items, explicando el que y el por que. **no termina con ninguna linea de atribucion ni firma.** si el commit mezcla temas, se propone dividirlo. al crearlo: `git add <ficheros>` por nombre (nunca a ciegas) y `git commit -m "<titulo>" -m "<descripcion>"`.

### PULL REQUESTS Y MERGES (DESCRIPCION SINTETIZADA)

la descripcion es un **resumen muy corto** del changelog, no el changelog entero; el detalle vive en `repo-docs/changelog.md`.

- titulo de pr de trabajo normal: igual que un commit, `<prefijo> | <resumen>`.
- titulo de pr de version (`beta-1.X` → `version-beta` y `version-beta` → `main`): `release version <x.y.z> - <resumen de las 2-3 entradas mas relevantes>`, el mismo en las dos.
- descripcion: bloques `### CASTELLANO` y `### ENGLISH` con las categorias cerradas y en este orden (`AÑADIDO`/`ADDED`, `CAMBIADO`/`CHANGED`, `CORREGIDO`/`FIXED`, `ELIMINADO`/`REMOVED`, `SEGURIDAD`/`SECURITY`), omitiendo las vacias, con paridad exacta entre idiomas.
- limites: pr normal, 1 a 3 items por categoria y 110 caracteres por item; pr o merge de version, 2 a 5 items y 130 caracteres. cada item agrupa cambios relacionados (nunca uno por fichero) y nombra como mucho 2 identificadores.
- los marcadores `[online]` / `[offline]` del changelog se conservan tal cual.
- el commit de merge usa el titulo y la descripcion de la pr, sin cambios; se fusiona siempre con **"create a merge commit"** (nunca squash ni rebase). la descripcion se pasa con `gh pr create --base <destino> --head <origen> --title "<titulo>" --body-file <fichero>`.

### RAMAS

`feature/*`, `fix/*`, `changes/*` → `beta-1.X` → `version-beta` → `main` → tag `v<x.y.z>` + release. los `hotfix/*` salen de `main` y despues se traen a `version-beta` y `beta-1.X`. las ramas se crean siempre desde `beta-1.X`, una rama por tema, y se borran (local y remota) en cuanto su pr se fusiona. `beta-1.X`, `version-beta` y `main` nunca se borran. antes de abrir una pr se comprueba con `git log <origen>..<destino>` que el destino no tiene commits que la rama no tenga.

### RELEASES

- tag siempre `v<x.y.z>`, sobre el commit de merge en `main`; un tag publicado no se mueve ni se borra.
- titulo: `v<x.y.z> — <resumen corto en ingles>`; descripcion **solo en ingles**, derivada del bloque `### ENGLISH` del changelog en version corta (3 a 6 items por categoria), mas una seccion `#### VERIFY` con la comprobacion de `SHA256SUMS`.
- activos: el apk firmado y `SHA256SUMS`, nada mas; las betas se marcan como pre-release.
- requisitos: changelog cerrado como `## [x.y.z] - aaaa-mm-dd` (commit `changelog`), pr `version-beta` → `main` fusionada, `versionName` y `versionCode` subidos y `android-ci` en verde.

### COMO PRESENTAR EL RESULTADO

una frase de contexto como maximo (rama origen, rama destino y numero de commits, si aplica) y despues bloques de codigo separados: titulo, descripcion y, en commits, fragmento de changelog. nunca se juntan titulo y descripcion en un bloque. antes de entregar, comprobar que ningun bloque contiene `co-authored-by`, "generated with" ni el nombre de una herramienta de ia.
