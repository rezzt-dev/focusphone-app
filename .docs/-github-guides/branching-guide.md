# GUIA DE RAMAS

## ESPAÑOL DE ESPAÑA: USO OBLIGATORIO DE Ñ

- todo texto en castellano debe usar español de españa (`es-ES`) y su ortografia, tambien en titulos y cuerpos de commits, changelogs, pull requests, merges y documentacion
- escribir siempre `ñ` en las palabras que la llevan y `Ñ` cuando corresponda la mayuscula; nunca sustituirlas por `n` o `N`
- la `ñ` es una letra distinta de la `n`; las instrucciones «sin acentos» o «sin tildes» solo afectan a las tildes de las vocales y nunca permiten eliminar la eñe
- ejemplos correctos: `añadir`, `diseño`, `año`, `tamaño`, `contraseña`, `español`, `españa` y `AÑADIDO`; sus formas con `n` son incorrectas cuando se refieren a esas palabras españolas
- conservar literalmente rutas, urls, nombres de archivo, identificadores tecnicos y texto de otros idiomas; esta regla no autoriza renombrarlos ni aplicar sustituciones indiscriminadas
- antes de entregar o publicar texto en castellano, revisar que ninguna palabra haya perdido su `ñ` o `Ñ`; esta regla prevalece sobre cualquier normalizacion a ascii o instruccion generica de eliminar acentos


> ramas del repositorio, flujo de versiones y que rama es la base de cada pull
> request. sirve para decidir el destino de una pr sin tener que preguntarlo.

---

## 1. RAMAS PERMANENTES

| rama | funcion |
|---|---|
| `main` | codigo publicado; solo recibe versiones cerradas desde `version-beta`; de aqui salen los tags `v<x.y.z>` |
| `version-beta` | version beta estable pendiente de publicar; recibe `beta-1.X` ya cerradas |
| `beta-1.X` | rama de desarrollo de la siguiente version (por ejemplo `beta-1.5`); recibe las ramas de trabajo |
| `version-alfa` | historico de la linea alfa; no recibe cambios nuevos |

---

## 2. RAMAS DE TRABAJO

nombre en minusculas, con barra y guiones:

| prefijo | uso | ejemplo |
|---|---|---|
| `feature/` | funcionalidad nueva | `feature/widget-picker` |
| `fix/` | correccion de error | `fix/schedule-lock-recents` |
| `changes/` | estructura, tooling, documentacion | `changes/repo-docs-legal` |
| `hotfix/` | urgencia sobre una version publicada | `hotfix/crash-on-start` |

- se crean siempre desde `beta-1.X` (los `hotfix/` desde `main`).
- una rama, un tema; si el tema crece, se divide.
- una rama de trabajo se borra (local y remota) en cuanto su pr esta fusionada.

---

## 3. FLUJO DE UNA VERSION

```
feature/* , fix/* , changes/*   →   beta-1.X   →   version-beta   →   main   →   tag v<x.y.z> + release
```

| paso | origen | destino | tipo de pr | titulo |
|---|---|---|---|---|
| 1 | `feature/*`, `fix/*`, `changes/*` | `beta-1.X` | trabajo normal | `<prefijo> \| <resumen>` |
| 2 | `beta-1.X` | `version-beta` | version | `release version <x.y.z> - <resumen>` |
| 3 | `version-beta` | `main` | version | el mismo titulo del paso 2 |
| 4 | `main` | — | release de github | `v<x.y.z> — <resumen en ingles>` |

- los `hotfix/` van a `main` y despues se traen a `version-beta` y `beta-1.X` para no perder la correccion.
- todos los pasos se fusionan con "create a merge commit".
- antes de abrir una pr se comprueba que el destino no tiene commits que la rama no tenga: `git log <origen>..<destino>`. si los tiene, se actualiza la rama primero.

---

## 4. CI

- `android-ci` se ejecuta en push a `main`, `version-beta`, `beta-*` y `alfa-*`.
- no se fusiona una pr con `android-ci` en rojo.

---

## 5. LIMPIEZA

tras fusionar una pr de trabajo:

```
git checkout beta-1.X
git pull --ff-only
git branch -d <rama>
git push origin --delete <rama>
```

`beta-1.X`, `version-beta` y `main` nunca se borran. `beta-1.X` se retira solo cuando su version se ha publicado y existe ya la siguiente.
