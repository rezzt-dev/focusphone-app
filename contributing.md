<div align="center">

# CONTRIBUTING

<sub>english · <a href="./contributing.es.md">castellano</a> · <a href="./readme.md">readme</a> · <a href="./documentation/en/developers/12-contributing.md">full guide</a> · <a href="./security.md">security policy</a> · <a href="./code_of_conduct.md">code of conduct</a></sub>

</div>

---

this page is the short version, and it is what github links from the issue and pull
request forms. the binding, detailed version is
[`documentation/en/developers/12-contributing.md`](./documentation/en/developers/12-contributing.md)
([castellano](./documentation/es/desarrolladores/12-contribuir.md)) — read it before your
first pull request.

focusphone is a personal project with a public history. contributions are welcome, and
they are held to the same bar as the author's own commits: no lint baseline, no skipped
tests, no undocumented behaviour. everyone taking part follows the
[code of conduct](./code_of_conduct.md).

---

## BEFORE YOU START

| you want to | do this |
|---|---|
| ask a question or get help | see [`.github/support.md`](./.github/support.md) |
| report a bug | open a [bug report](https://github.com/rezzt-dev/focusphone-app/issues/new?template=bug_report.yml) |
| propose a feature | open a [feature request](https://github.com/rezzt-dev/focusphone-app/issues/new?template=feature_request.yml) — read the [design principles](./readme.md#design-principles) first |
| report a vulnerability | **never** in a public issue: follow [`security.md`](./security.md) (`contacto@rezzt.dev`) |
| change code | the workflow below |

for anything larger than a fix, open an issue first. a proposal that adds a screen, a
toggle or a notification has to justify itself against the point of the app, and that
conversation is cheaper before the code than after it.

---

## SETUP

| requirement | version |
|---|---|
| android studio | a release compatible with agp `9.3.0` |
| jdk | `21` — the gradle daemon toolchain, auto-provisioned |
| android sdk | compile sdk `36` |
| device or emulator | android `7.0` (api 24) or newer |

```bash
git clone https://github.com/rezzt-dev/focusphone-app.git
cd focusphone-app/focusphone-project
./gradlew :app:installDebug
```

in android studio, open the `focusphone-project/` directory — **not** the repository root.

---

## BRANCHES

| branch | role |
|---|---|
| `beta-1.X` (today `beta-1.5`) | active development of the next version; **pull requests target this branch** |
| `version-beta` | the stable versions, updated only from a `beta-1.X` branch |
| `main` | the published state, updated last, only from `version-beta` |

changes flow `feature/...` or `fix/...` → `beta-1.X` → `version-beta` → `main`, always with
**"create a merge commit"**, never squash or rebase, so the individual commits survive
next to the pull request summary.

---

## WORKFLOW

1. **fork** and branch from the active `beta-1.X` branch:
   `git checkout -b feature/your-feature`.
2. **write the change** following the conventions in
   [`agents.md`](./agents.md) — comments and kdoc in english, new colours through
   `FocusPalette`, new settings inside an existing settings sub-screen, new singletons
   registered in `AppContainer`.
3. **cover it** with a unit test when the logic is pure, and an instrumented test when it
   is a flow.
4. **verify**, from `focusphone-project/` — the same commands ci runs:

   ```bash
   ./gradlew staticAnalysis --continue
   ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
   ```

5. **record it** in the `## SIN PUBLICAR / UNRELEASED` section of
   [`changelog.md`](./changelog.md), **in both languages** (`### CASTELLANO` and
   `### ENGLISH`), under the right category: `ANADIDO` / `ADDED`, `CAMBIADO` / `CHANGED`,
   `CORREGIDO` / `FIXED`, `ELIMINADO` / `REMOVED`, `SEGURIDAD` / `SECURITY`. one line per
   item, imperative, lowercase, no accents.
6. **open a pull request** whose description is the changelog fragment your branch adds.
   the [template](./.github/pull_request_template.md) is filled in for you.

---

## COMMIT FORMAT

titles are lowercase, imperative, and use a closed set of prefixes:

```
<prefix> | <short imperative summary>
```

| prefix | when |
|---|---|
| `update` | normal forward work: a new or improved feature, content or document (the default) |
| `fix` | a bug or wrong behaviour found during development, not urgent |
| `hotfix` | an urgent fix affecting a published version or a critical block |
| `changes` | structural or non-functional work: reorganizing, renaming, tooling, dependencies |
| `changelog` | the commit's only purpose is editing `changelog.md` |

the body is an unordered markdown list, one continuous line per logical block, with
files, paths and identifiers in backticks — explaining the what and the why, not the
diff.

---

## SENSITIVE CODE

a change to any of these needs an issue first and the maintainer's explicit review
([`.github/CODEOWNERS`](./.github/CODEOWNERS)), plus unit tests for every new edge case:

- `focusphone-project/app/src/main/java/app/focusphone/security/` (`PasswordHasher`,
  `PasswordAttemptPolicy`, `SecureStorageHelper`, `EncryptedPrefs`);
- the block screens and gates: `ui/detox/DetoxBlockedActivity`,
  `ui/schedule/ScheduleBlockedActivity`, `ui/schedule/ScheduleAccessGate`;
- `service/FocusAccessibilityService` and `service/FocusNotificationService`;
- `AndroidManifest.xml` (permissions, exported components), `res/xml/data_extraction_rules.xml`,
  `res/xml/accessibility_service_config.xml` and `proguard-rules.pro`;
- `.github/workflows/`, `.github/dependabot.yml`, `renovate.json` and
  `gradle/libs.versions.toml`;
- the legal files: `license`, `notice`, `third_party_licenses.md`, `privacy*.md`,
  `permissions*.md`, `disclaimer*.md` and the in-app `assets/*.html`.

the rules for security code are in
[`08-security.md`](./documentation/en/developers/08-security.md#rules-when-touching-security).

---

## NON-NEGOTIABLE RULES

- **no network.** the app has no internet permission and sends nothing off the device.
  no analytics, no crash reporting, no remote config.
- **no lint or detekt baseline.** findings are fixed at the source, not recorded as
  accepted debt.
- **no automated dependency updates.** `gradle/libs.versions.toml` is edited by hand, in
  a commit that carries its own verification. bot pull requests are rejected by
  [`dependency-policy.yml`](./.github/workflows/dependency-policy.yml). a new dependency
  also updates [`third_party_licenses.md`](./third_party_licenses.md) and the in-app
  licenses screen.
- **user-facing strings live in `strings.xml`** and are translated to `es`, `fr` and `de`.
- **documentation is bilingual.** a behaviour change that a user or a contributor can
  observe updates both `documentation/es` and `documentation/en`, with the same files,
  sections and data.

the full list, with the reasoning behind each one, is in
[`12-contributing.md`](./documentation/en/developers/12-contributing.md#non-negotiable-rules).

---

## LICENCE

focusphone is distributed under a [custom non-commercial licence](./license), which is not
an osi-approved open source licence. by opening a pull request you agree that your
contribution is licensed under those same terms.

<!-- TODO(owner): legal review of the contribution clause above (no cla/dco is in place). -->

---

<div align="center">

<sub>focusphone — developed by <a href="https://rezzt.dev"><b>rezzt.dev</b></a></sub>

</div>
