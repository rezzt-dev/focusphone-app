<div align="center">

# CONTRIBUIR

<sub><a href="./contributing.md">english</a> · castellano · <a href="./readme.es.md">readme</a> · <a href="./documentation/es/desarrolladores/12-contribuir.md">guía completa</a> · <a href="./security.es.md">política de seguridad</a> · <a href="./code_of_conduct.es.md">código de conducta</a></sub>

</div>

---

esta página es la versión corta, y es la que github enlaza desde los formularios de issues
y pull requests. la versión detallada y vinculante es
[`documentation/es/desarrolladores/12-contribuir.md`](./documentation/es/desarrolladores/12-contribuir.md)
([english](./documentation/en/developers/12-contributing.md)): léela antes de tu primera
pull request.

focusphone es un proyecto personal con un historial público. las contribuciones son
bienvenidas y se les exige lo mismo que a los commits del autor: sin línea base de lint, sin
pruebas omitidas, sin comportamiento sin documentar. quien participa sigue el
[código de conducta](./code_of_conduct.es.md).

---

## ANTES DE EMPEZAR

| quieres | haz esto |
|---|---|
| preguntar algo o pedir ayuda | consulta [`.github/support.es.md`](./.github/support.es.md) |
| informar de un error | abre un [informe de error](https://github.com/rezzt-dev/focusphone-app/issues/new?template=bug_report.yml) |
| proponer una funcionalidad | abre una [propuesta](https://github.com/rezzt-dev/focusphone-app/issues/new?template=feature_request.yml) — lee antes los [principios de diseño](./readme.es.md#principios-de-diseño) |
| informar de una vulnerabilidad | **nunca** en un issue público: sigue [`security.es.md`](./security.es.md) (`contacto@rezzt.dev`) |
| cambiar código | el flujo de abajo |

para cualquier cosa mayor que una corrección, abre antes un issue. una propuesta que añade
una pantalla, un interruptor o una notificación tiene que justificarse frente al propósito
de la app, y esa conversación sale más barata antes del código que después.

---

## ENTORNO

| requisito | versión |
|---|---|
| android studio | una versión compatible con agp `9.3.0` |
| jdk | `21` — la toolchain del daemon de gradle, se aprovisiona sola |
| android sdk | compile sdk `36` |
| dispositivo o emulador | android `7.0` (api 24) o posterior |

```bash
git clone https://github.com/rezzt-dev/focusphone-app.git
cd focusphone-app/focusphone-project
./gradlew :app:installDebug
```

en android studio, abre el directorio `focusphone-project/`, **no** la raíz del
repositorio.

---

## RAMAS

| rama | papel |
|---|---|
| `beta-1.X` (hoy `beta-1.5`) | desarrollo activo de la siguiente versión; **las pull requests van contra esta rama** |
| `version-beta` | las versiones estables, que solo se actualiza desde una rama `beta-1.X` |
| `main` | el estado publicado, que se actualiza la última y solo desde `version-beta` |

los cambios fluyen `feature/...` o `fix/...` → `beta-1.X` → `version-beta` → `main`, siempre
con **"create a merge commit"**, nunca squash ni rebase, para que los commits individuales
se conserven junto al resumen de la pull request.

---

## FLUJO

1. haz un **fork** y crea tu rama desde la rama `beta-1.X` activa:
   `git checkout -b feature/tu-funcionalidad`.
2. **escribe el cambio** siguiendo las convenciones de [`agents.md`](./agents.md):
   comentarios y kdoc en inglés, colores nuevos a través de `FocusPalette`, ajustes nuevos
   dentro de una subpantalla de ajustes existente, singletons nuevos registrados en
   `AppContainer`.
3. **cúbrelo** con una prueba unitaria si la lógica es pura, y con una instrumentada si es
   un flujo.
4. **verifica**, desde `focusphone-project/`, con las mismas órdenes que ejecuta la ci:

   ```bash
   ./gradlew staticAnalysis --continue
   ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
   ```

5. **regístralo** en la sección `## SIN PUBLICAR / UNRELEASED` de
   [`changelog.md`](./changelog.md), **en los dos idiomas** (`### CASTELLANO` y
   `### ENGLISH`), en la categoría correcta: `ANADIDO` / `ADDED`, `CAMBIADO` / `CHANGED`,
   `CORREGIDO` / `FIXED`, `ELIMINADO` / `REMOVED`, `SEGURIDAD` / `SECURITY`. una línea por
   ítem, en imperativo, en minúsculas y sin acentos.
6. **abre una pull request** cuya descripción sea el fragmento de changelog que añade tu
   rama. la [plantilla](./.github/pull_request_template.md) ya lo prepara.

---

## FORMATO DE LOS COMMITS

los títulos van en minúsculas, en imperativo, y usan un conjunto cerrado de prefijos:

```
<prefijo> | <resumen corto en imperativo>
```

| prefijo | cuándo |
|---|---|
| `update` | trabajo normal hacia adelante: una funcionalidad, contenido o documento nuevo o mejorado (el prefijo por defecto) |
| `fix` | un error o comportamiento incorrecto detectado durante el desarrollo, sin urgencia |
| `hotfix` | una corrección urgente que afecta a una versión publicada o a un bloqueo crítico |
| `changes` | trabajo estructural o no funcional: reorganizar, renombrar, herramientas, dependencias |
| `changelog` | el único propósito del commit es editar `changelog.md` |

el cuerpo es una lista de markdown no numerada, una línea continua por bloque lógico, con
ficheros, rutas e identificadores entre backticks, que explica el qué y el porqué, no el
diff.

---

## CÓDIGO SENSIBLE

un cambio en cualquiera de estos necesita antes un issue y la revisión explícita del
mantenedor ([`.github/CODEOWNERS`](./.github/CODEOWNERS)), además de pruebas unitarias para
cada caso límite nuevo:

- `focusphone-project/app/src/main/java/app/focusphone/security/` (`PasswordHasher`,
  `PasswordAttemptPolicy`, `SecureStorageHelper`, `EncryptedPrefs`);
- las pantallas y puertas de bloqueo: `ui/detox/DetoxBlockedActivity`,
  `ui/schedule/ScheduleBlockedActivity`, `ui/schedule/ScheduleAccessGate`;
- `service/FocusAccessibilityService` y `service/FocusNotificationService`;
- `AndroidManifest.xml` (permisos, componentes exportados), `res/xml/data_extraction_rules.xml`,
  `res/xml/accessibility_service_config.xml` y `proguard-rules.pro`;
- `.github/workflows/`, `.github/dependabot.yml`, `renovate.json` y
  `gradle/libs.versions.toml`;
- los ficheros legales: `license`, `notice`, `third_party_licenses.md`, `privacy*.md`,
  `permissions*.md`, `disclaimer*.md` y los `assets/*.html` de la app.

las reglas para el código de seguridad están en
[`08-seguridad.md`](./documentation/es/desarrolladores/08-seguridad.md#reglas-al-tocar-seguridad).

---

## REGLAS INNEGOCIABLES

- **sin red.** la app no tiene permiso de internet y no envía nada fuera del dispositivo.
  sin analítica, sin informes de errores, sin configuración remota.
- **sin línea base de lint ni de detekt.** los hallazgos se corrigen en el origen, no se
  registran como deuda aceptada.
- **sin actualizaciones automáticas de dependencias.** `gradle/libs.versions.toml` se edita
  a mano, en un commit que lleva su propia verificación. las pull requests de bots las
  rechaza [`dependency-policy.yml`](./.github/workflows/dependency-policy.yml). una
  dependencia nueva actualiza también [`third_party_licenses.md`](./third_party_licenses.md)
  y la pantalla de licencias de la app.
- **los textos visibles viven en `strings.xml`** y se traducen a `es`, `fr` y `de`.
- **la documentación es bilingüe.** un cambio de comportamiento que un usuario o un
  colaborador pueda observar actualiza `documentation/es` y `documentation/en`, con los
  mismos ficheros, secciones y datos.

la lista completa, con el motivo de cada regla, está en
[`12-contribuir.md`](./documentation/es/desarrolladores/12-contribuir.md#reglas-innegociables).

---

## LICENCIA

focusphone se distribuye con una [licencia no comercial propia](./license), que no es una
licencia de código abierto aprobada por la osi. al abrir una pull request aceptas que tu
contribución quede bajo esos mismos términos.

<!-- TODO(owner): revisión legal de la cláusula de contribución de arriba (no hay cla ni dco). -->

---

<div align="center">

<sub>focusphone — desarrollado por <a href="https://rezzt.dev"><b>rezzt.dev</b></a></sub>

</div>
