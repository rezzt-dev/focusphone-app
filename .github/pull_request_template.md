<!--
  the description of a pull request is the changelog fragment the branch adds, in both
  languages. keep the headings below; delete the html comments and any section that
  genuinely does not apply.
-->

## SUMMARY

<!-- one or two lines: what this branch changes and why. lowercase, like the rest of
     the repository. -->

---

## CHANGELOG FRAGMENT

<!-- copy here, verbatim, the entries this branch adds to the `## SIN PUBLICAR /
     UNRELEASED` section of `changelog.md`. both languages, under the right category
     (anadido / cambiado / corregido / eliminado). if this branch adds nothing to the
     changelog, say why. -->

### CASTELLANO

-

### ENGLISH

-

---

## VERIFICATION

<!-- tick only what you actually ran, from `focusphone-project/`. -->

- [ ] `./gradlew staticAnalysis` — ktlint and detekt, no new findings and no baseline added.
- [ ] `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` — passes.
- [ ] instrumented tests, if this touches a flow: `./gradlew :app:connectedDebugAndroidTest`.
- [ ] tested on a device or emulator, as the launcher (home app).

**devices tested:** <!-- e.g. pixel 6a — android 15, emulator api 24 -->

---

## CHECKLIST

- [ ] the commit titles follow `<prefix> | <short imperative summary>`, fully lowercase,
      with a prefix from the closed set (`update`, `fix`, `hotfix`, `changes`, `changelog`).
- [ ] comments and kdoc are in english; user-facing strings are in `strings.xml` and
      translated to `es`, `fr` and `de`.
- [ ] new colours go through `FocusPalette` / `FocusColors`, never a hardcoded hex.
- [ ] new singletons are registered in `AppContainer`; new settings live inside an
      existing settings sub-screen.
- [ ] pure logic is covered by a unit test; a new flow is covered by an instrumented test.
- [ ] no new permission, no new dependency and no network call — or the description
      explains and justifies it below.
- [ ] the documentation in `documentation/es` and `documentation/en` is updated if this
      changes behaviour a user or a contributor can observe.

---

## SCREENSHOTS

<!-- required for anything visual: before and after, or a short screen recording. -->

---

## NOTES FOR THE REVIEWER

<!-- anything that is not obvious from the diff: a decision you hesitated on, a
     follow-up you deliberately left out, a migration a user will notice. -->
