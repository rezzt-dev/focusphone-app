# GUIA DE RELEASES

## ESPAÑOL DE ESPAÑA: USO OBLIGATORIO DE Ñ

- todo texto en castellano debe usar español de españa (`es-ES`) y su ortografia, tambien en titulos y cuerpos de commits, changelogs, pull requests, merges y documentacion
- escribir siempre `ñ` en las palabras que la llevan y `Ñ` cuando corresponda la mayuscula; nunca sustituirlas por `n` o `N`
- la `ñ` es una letra distinta de la `n`; las instrucciones «sin acentos» o «sin tildes» solo afectan a las tildes de las vocales y nunca permiten eliminar la eñe
- ejemplos correctos: `añadir`, `diseño`, `año`, `tamaño`, `contraseña`, `español`, `españa` y `AÑADIDO`; sus formas con `n` son incorrectas cuando se refieren a esas palabras españolas
- conservar literalmente rutas, urls, nombres de archivo, identificadores tecnicos y texto de otros idiomas; esta regla no autoriza renombrarlos ni aplicar sustituciones indiscriminadas
- antes de entregar o publicar texto en castellano, revisar que ninguna palabra haya perdido su `ñ` o `Ñ`; esta regla prevalece sobre cualquier normalizacion a ascii o instruccion generica de eliminar acentos


> como preparar el titulo, la descripcion y el tag de una release de github.
> se aplica cuando una pr de version ya esta fusionada en `main`. lee antes
> `agent-rules.md`: la release no lleva ningun rastro de ia.

---

## 1. REQUISITOS PREVIOS

1. `repo-docs/changelog.md` tiene la version cerrada como `## [x.y.z] - aaaa-mm-dd` (ver `changelog-guide.md`).
2. la pr `version-beta` → `main` esta fusionada con "create a merge commit".
3. `app/build.gradle.kts` tiene `versionName` `x.y.z-beta` (o el sufijo que corresponda) y un `versionCode` mayor que el de la release anterior.
4. el apk esta firmado con la clave de release oficial y existe su `SHA256SUMS`.
5. `android-ci` esta en verde sobre el commit que se va a etiquetar.

---

## 2. TAG

- siempre `v<x.y.z>`, mismo numero que la cabecera del changelog, sin sufijos extra (ejemplo: `v1.4.0`).
- se crea sobre el commit de merge en `main`, nunca sobre una rama de trabajo.
- no se mueve ni se borra un tag ya publicado; si hay un error, se publica una version nueva.

```
git tag -a v<x.y.z> -m "v<x.y.z>"
git push origin v<x.y.z>
```

---

## 3. TITULO

```
v<x.y.z> — <resumen corto en ingles>
```

- en ingles, minusculas y sin acentos (conservando siempre `ñ` y `Ñ`); resume las 2-3 entradas mas relevantes.
- ejemplo: `v1.4.0 — home grid, 20 built-in widgets & bilingual legal docs`.

---

## 4. DESCRIPCION

- **solo en ingles**: la release es la cara publica del proyecto.
- reutiliza el bloque `### ENGLISH` de la version cerrada, con sus encabezados `####` en mayusculas, pero en version corta: de 3 a 6 items por categoria, agrupando cambios relacionados (misma regla de sintesis que `pr-merge-guide.md`, seccion 2).
- se omite la categoria sin items; no se reordenan las categorias.
- cierra con una seccion de verificacion:

```
#### VERIFY

- download `focusphone-<x.y.z>-beta.apk` and `SHA256SUMS` from the assets
- check the checksum with `sha256sum -c SHA256SUMS`
- confirm the signing certificate fingerprint published in `.github/security.md`
```

- sin lineas de atribucion ni enlaces a herramientas de ia.

---

## 5. ACTIVOS

- el apk firmado y `SHA256SUMS`; nada mas (sin keystores, sin `local.properties`, sin mapeos privados).
- si la release se marca como pre-release (versiones beta), se usa `--prerelease`.

---

## 6. COMANDO

```
gh release create v<x.y.z> <apk> SHA256SUMS --title "<titulo>" --notes-file <fichero> --prerelease
```

se entrega el comando sin ejecutar; publicar una release es visible para todos y exige confirmacion (ver `agent-rules.md`).

---

## 7. LISTA DE COMPROBACION

- [ ] el tag coincide con la cabecera del changelog.
- [ ] titulo y descripcion estan en ingles.
- [ ] los items salen del bloque `### ENGLISH` del changelog.
- [ ] el apk y `SHA256SUMS` corresponden a la misma compilacion.
- [ ] no hay atribucion de ia ni datos sensibles.
