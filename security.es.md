<div align="center">

# POLÍTICA DE SEGURIDAD

<sub><a href="./security.md">english</a> · castellano · <a href="./readme.es.md">readme</a> · <a href="./documentation/es/desarrolladores/08-seguridad.md">documentación de seguridad completa</a> · <a href="./permissions.es.md">permisos</a></sub>

<br>

<img src="https://img.shields.io/badge/100%25-offline-4c1?style=flat-square" alt="100% sin conexión" />
<img src="https://img.shields.io/badge/sin-rastreadores-4c1?style=flat-square" alt="sin rastreadores" />
<img src="https://img.shields.io/badge/informes-privados-000000?style=flat-square" alt="informes privados" />

</div>

---

## VERSIONES SOPORTADAS

focusphone está en beta y solo hay una línea soportada: la última versión publicada. no hay
ramas de soporte a largo plazo ni retroportes.

| versión (ajustes → acerca de) | release | soportada |
|---|---|---|
| `1.4.0-beta` | [`v1.4.0`](https://github.com/rezzt-dev/focusphone-app/releases) | sí |
| `1.0.6` compilada con el código de la 1.4 | [`v1.4-beta`](https://github.com/rezzt-dev/focusphone-app/releases/tag/v1.4-beta) | no — mismo código que `1.4.0-beta`, actualiza a ella |
| `1.0.6` compilada con el código de la 1.0 | [`v1.0.0-beta`](https://github.com/rezzt-dev/focusphone-app/releases/tag/v1.0.0-beta) | no — firmada con otra clave, ver abajo |
| cualquier alpha | — | no |

antes de informar, comprueba que el problema sigue ocurriendo en la última release.
`versionName` está en `focusphone-project/app/build.gradle.kts` y la app lo muestra en
**ajustes → acerca de**.

---

## VERIFICAR UN APK

todas las releases desde `v1.4-beta` se firman con el mismo certificado. compruébalo antes
de instalar:

```bash
sha256sum -c SHA256SUMS                     # el fichero adjunto a la release
apksigner verify --print-certs focusphone-*.apk
```

| | |
|---|---|
| esquema de firma | apk signature scheme v2 |
| sha-256 del certificado | `0f:f1:f8:3c:32:e0:5d:c5:4b:c2:2b:a2:8a:13:df:4c:5c:37:ec:79:c4:1d:b3:e1:4a:d7:20:e7:c8:a0:76:2f` |

el apk de `v1.0.0-beta` se firmó con otra clave que ya no está disponible. android no
permite actualizar una app firmada con una clave distinta, así que **si instalaste
`v1.0.0-beta`, desinstálala primero** (se borran sus ajustes, porque focusphone no guarda
copias de seguridad) e instala después la release actual. cualquier apk que diga ser
focusphone y esté firmado con un certificado distinto del de arriba no es una compilación
oficial.

---

## INFORMAR DE UNA VULNERABILIDAD

**no abras un issue público para un problema de seguridad.** usa uno de estos canales
privados:

| canal | cuándo |
|---|---|
| correo a `contacto@rezzt.dev` con el asunto `[focusphone security]` | siempre disponible |
| [informe privado de vulnerabilidades de github](https://github.com/rezzt-dev/focusphone-app/security/advisories/new) | cuando el formulario esté activado en el repositorio; el informe sigue privado hasta que se publique la corrección |

<!-- TODO(owner): activar el informe privado de vulnerabilidades en los ajustes del repositorio; hasta entonces el enlace de arriba no muestra ningún formulario. -->

### QUÉ INCLUIR

- la versión afectada (`versionName` y `versionCode`), cómo se instaló (qué fichero de qué
  release) y la versión de android del dispositivo.
- el componente: una actividad, un servicio, un fichero de preferencias,
  `SecureStorageHelper`, `PasswordHasher`, `PasswordAttemptPolicy`, `ScheduleAccessGate`…
- los pasos para reproducirlo, lo más concretos posible, y qué gana un atacante.
- si hace falta un dispositivo rooteado, la depuración usb activada o la app compilada como
  `debug` para que ocurra.
- registros, trazas o una prueba de concepto, si los tienes.

### QUÉ ESPERAR

| fase | objetivo |
|---|---|
| acuse de recibo del informe | en 72 horas |
| evaluación inicial (aceptado / no es una vulnerabilidad / falta información) | en 7 días |
| corrección en la versión soportada | según la gravedad; los problemas críticos tienen prioridad sobre cualquier funcionalidad |

es un proyecto de un solo mantenedor **sin programa de recompensas**: no hay más
reconocimiento que el crédito en el aviso y en `changelog.md`, y los plazos de arriba son
un compromiso de mejor esfuerzo, no un sla contractual.

---

## DIVULGACIÓN COORDINADA

- las correcciones se desarrollan en privado y se publican junto con un aviso de seguridad
  de github y una entrada `#### SEGURIDAD` en [`changelog.md`](./changelog.md).
- mantén el informe en privado hasta que se publique la corrección, o durante 90 días desde
  el acuse de recibo, lo que ocurra antes.
- quien informa recibe el crédito con el nombre o alias que elija, salvo que pida
  permanecer en el anonimato.
- un informe se trata como describe [`privacy.es.md`](./privacy.es.md#8-tus-derechos): los
  datos que envíes solo se usan para tramitarlo.

---

## ALCANCE

focusphone **no tiene el permiso `INTERNET`**: no hay backend, cuenta, sdk de analítica ni
informes de errores. así que no hay superficie de ataque remota, y el modelo de amenazas es
enteramente local. lo que la app protege está documentado en detalle en
[`08-seguridad.md`](./documentation/es/desarrolladores/08-seguridad.md); en resumen:

| protege frente a | mecanismo |
|---|---|
| otra app leyendo los datos de focusphone | almacenamiento privado de la app más cifrado `AES-256/GCM` respaldado por el almacén de claves de android |
| extraer los ficheros del dispositivo | los valores sensibles no sirven sin la clave del almacén, que nunca sale del dispositivo |
| fugas por copias de seguridad o transferencia entre dispositivos | `allowBackup="false"` y `data_extraction_rules.xml`, que excluye todos los dominios |
| fuerza bruta contra una contraseña de detox o de horarios | `PBKDF2WithHmacSHA256`, 100 000 iteraciones, sal por contraseña y una única política de intentos: 5 fallos y después 15 minutos de rechazo |
| alguien mirando por encima del hombro | `FLAG_SECURE` en la pantalla de bloqueo y en todos los diálogos de contraseña |
| un apk manipulado | el certificado de firma de publicación de arriba |

### DENTRO DEL ALCANCE

- recuperar una contraseña de detox o de horarios, o saltarse un bloqueo sin ella.
- leer, manipular o restablecer las preferencias cifradas desde fuera de la app (otra app,
  un componente exportado, un intent, una copia de seguridad).
- un mal uso del almacén de claves, de `SecureStorageHelper` o de `PasswordHasher`
  (parámetros débiles, reutilización del iv, una clave regenerada sobre un alias existente).
- un componente exportado, intent, widget o camino de los servicios de accesibilidad o de
  notificaciones que permita a un tercero actuar en nombre del usuario (consulta
  [`permissions.es.md`](./permissions.es.md#3-componentes-exportados)).
- una fuga de hábitos del usuario —apps bloqueadas, palabras clave, contadores de uso,
  nombres personalizados, el texto de intención— a través de registros, capturas, copias de
  seguridad o cualquier fichero legible por todos.

### FUERA DEL ALCANCE

son decisiones de diseño conocidas y documentadas, no vulnerabilidades
([`disclaimer.es.md`](./disclaimer.es.md)):

- focusphone **no es administrador del dispositivo**: no impide su propia desinstalación ni
  el cambio de launcher. es un compromiso reforzado, no una jaula.
- cualquier cosa que requiera un dispositivo rooteado, un bootloader desbloqueado, depuración
  usb más acceso físico, o una compilación `debug`.
- borrar los datos de la app o desinstalarla desde los ajustes de android, lo que por diseño
  elimina todos los bloqueos.
- lo que un launcher de terceros haga con el contenido de un widget de focusphone que aloje.
- informes generados solo por un escáner automático, sin un camino de explotación en una
  compilación de publicación sobre un dispositivo no rooteado.
- falta de endurecimiento sin camino de ataque, como no fijar certificados en una app que no
  hace peticiones de red.
- vulnerabilidades del propio android, del software del fabricante o de apps de terceros.

---

## SI VAS A TOCAR CÓDIGO DE SEGURIDAD

cualquier cambio que toque contraseñas, cifrado o pantallas de bloqueo debe seguir las
reglas de [`08-seguridad.md`](./documentation/es/desarrolladores/08-seguridad.md), sección
"reglas al tocar seguridad" —entre ellas: no borrar nunca la configuración protegida ante
intentos fallidos, no tratar nunca un estado `Unavailable` del almacén de claves como una
lista vacía, no ejecutar nunca pbkdf2 en el hilo principal y no regenerar nunca la clave del
almacén sobre un alias existente—. los casos límite nuevos necesitan pruebas unitarias, y la
pull request necesita la revisión del mantenedor ([`.github/CODEOWNERS`](./.github/CODEOWNERS)).

---

<div align="center">

<sub>focusphone — desarrollado por <a href="https://rezzt.dev"><b>rezzt.dev</b></a></sub>

</div>
