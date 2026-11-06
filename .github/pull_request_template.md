<!--
  english: the description of a pull request is the changelog fragment the branch adds,
  in both languages. keep the headings below; delete the html comments and any section
  that genuinely does not apply. pull requests target the active `beta-1.X` branch and
  are merged with "create a merge commit".

  castellano: la descripción de una pull request es el fragmento de changelog que añade
  la rama, en los dos idiomas. conserva los encabezados; borra los comentarios html y
  cualquier sección que de verdad no aplique. las pull requests van contra la rama
  `beta-1.X` activa y se fusionan con "create a merge commit".
-->

### CASTELLANO

<!-- copia aquí, literalmente, los ítems que esta rama añade a `## SIN PUBLICAR /
     UNRELEASED` de `changelog.md`, bajo su categoría: ANADIDO / CAMBIADO / CORREGIDO /
     ELIMINADO / SEGURIDAD. omite las categorías vacías. -->

#### ANADIDO
-

### ENGLISH

<!-- the same items, in english, under ADDED / CHANGED / FIXED / REMOVED / SECURITY. -->

#### ADDED
-

---

## VERIFICATION · VERIFICACIÓN

<!-- tick only what you actually ran, from `focusphone-project/`. · marca solo lo que
     hayas ejecutado de verdad, desde `focusphone-project/`. -->

- [ ] `./gradlew staticAnalysis --continue` — ktlint and detekt, no new findings and no baseline added. · sin hallazgos nuevos ni línea base.
- [ ] `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` — passes. · pasa.
- [ ] instrumented tests, if this touches a flow · pruebas instrumentadas, si toca un flujo: `./gradlew :app:connectedDebugAndroidTest`.
- [ ] tested on a device or emulator, as the launcher (home app). · probado en un dispositivo o emulador como launcher.

**devices tested · dispositivos probados:** <!-- e.g. pixel 6a — android 15, emulator api 24 -->

---

## CHECKLIST

- [ ] commit titles follow `<prefix> | <short imperative summary>` with a prefix from `update`, `fix`, `hotfix`, `changes`, `changelog`. · los títulos siguen el formato con un prefijo del conjunto cerrado.
- [ ] comments and kdoc are in english; user-facing strings are in `strings.xml` and translated to `es`, `fr` and `de`. · comentarios en inglés; textos en `strings.xml` traducidos.
- [ ] new colours go through `FocusPalette` / `FocusColors`, never a hardcoded hex. · colores nuevos por `FocusPalette` / `FocusColors`.
- [ ] new singletons are registered in `AppContainer`; new settings live inside an existing settings sub-screen. · singletons en `AppContainer`; ajustes en una subpantalla existente.
- [ ] pure logic is covered by a unit test; a new flow is covered by an instrumented test. · lógica pura con prueba unitaria; flujo nuevo con prueba instrumentada.
- [ ] no new permission, dependency or network call — or the notes explain it, and `permissions*.md`, `privacy*.md` and `third_party_licenses.md` are updated. · sin permisos, dependencias ni red nuevos, o se justifican y se actualizan los documentos legales.
- [ ] sensitive code (see `contributing.md`) has an issue and is flagged for the maintainer's review. · el código sensible tiene issue y pide revisión del mantenedor.
- [ ] `documentation/es` and `documentation/en` are updated, with the same sections, if this changes observable behaviour. · la documentación está actualizada en los dos idiomas.

---

## SCREENSHOTS · CAPTURAS

<!-- required for anything visual: before and after, or a short screen recording. ·
     obligatorias para cualquier cambio visual: antes y después, o una grabación corta. -->

---

## NOTES FOR THE REVIEWER · NOTAS PARA LA REVISIÓN

<!-- anything that is not obvious from the diff. · lo que no se ve en el diff. -->
