<div align="center">

# POLÍTICA DE PRIVACIDAD

<sub><a href="./privacy.md">english</a> · castellano · <a href="./readme.es.md">readme</a> · <a href="./permissions.es.md">permisos</a> · <a href="./security.es.md">seguridad</a></sub>

</div>

---

<!-- TODO(owner): revisión legal (rgpd / lopdgdd) antes de dar esta política por definitiva. -->

| | |
|---|---|
| se aplica a | la app android focusphone (`app.focusphone`), versión `1.4.0-beta` y posteriores |
| última actualización | 2026-11-06 |
| autor | Juan García Cazallas ("rezzt-dev") |
| contacto | `contacto@rezzt.dev` |

esta política describe qué hace la app con los datos. no cubre la web
<https://focusphone.rezzt.dev>, que tiene su propia página de privacidad, ni github, que
aloja este repositorio con su [declaración general de privacidad](https://docs.github.com/es/site-policy/privacy-policies/github-general-privacy-statement).
se publica en inglés ([`privacy.md`](./privacy.md)) y en castellano; si difieren, prevalece
la versión en castellano, igual que en la [`license`](./license).

---

## 1. RESUMEN

- **nada sale de tu dispositivo.** la app no declara el permiso `INTERNET`, así que android
  no le deja abrir ninguna conexión de red. no hay servidor, cuenta, sincronización,
  publicidad, analítica, informes de errores ni configuración remota.
- **el autor no recibe ningún dato de la app.** el autor no puede ver quién usa focusphone,
  cómo ni en qué dispositivo.
- **lo que la app guarda, lo guarda en su almacenamiento privado**, y la parte que revela
  cómo usas el móvil se cifra con una clave que custodia el almacén de claves de android.
- **no hay copias de seguridad**: los datos de la app están excluidos de la copia en la
  nube, de `adb backup` y de la transferencia entre dispositivos.

---

## 2. DATOS GUARDADOS EN EL DISPOSITIVO

todo lo siguiente vive en el almacenamiento privado de la app (`/data/data/app.focusphone/`),
que otras apps no pueden leer. "cifrado" significa aes-256/gcm con una clave que genera y
conserva el almacén de claves de android, del que nunca sale.

| dato | dónde | cifrado |
|---|---|---|
| apps favoritas y su orden | base de datos room `focusphone_database` | no |
| tema, tipografía, tamaños de texto, alineación, estilo del reloj, gestos, estado del tutorial | preferencias compartidas | no |
| cuadrícula de inicio (tamaño, bloques, ids de widget y sus celdas) | preferencias compartidas | no |
| ajustes de la pausa consciente y del panel de notificaciones | preferencias compartidas | no |
| ajustes del fondo de pantalla | preferencias compartidas | no |
| imágenes de fondo que eliges | una copia reducida en los ficheros de la app (`wallpapers/`) | no |
| ciudades elegidas para un reloj mundial | preferencias compartidas | no |
| **texto de intención** del widget de intención | preferencias compartidas | **sí** |
| **nombres personalizados de apps** | preferencias compartidas | **sí** |
| **carpetas del cajón** y sus apps | preferencias compartidas | **sí** |
| **perfil activo, temporizador de detox, apps bloqueadas, palabras clave del modo trabajo** | preferencias compartidas | **sí** |
| **horarios de apps, límites diarios y contadores de uso** | preferencias compartidas | **sí** |
| **contraseñas de detox y de horarios** | preferencias compartidas | la contraseña nunca se guarda, solo un resumen pbkdf2-hmac-sha256 (100 000 iteraciones, sal aleatoria de 128 bits) |
| intentos fallidos de contraseña y tiempo de espera | preferencias compartidas | **sí** |

los datos se conservan hasta que los cambias, borras el almacenamiento de la app en los
ajustes de android o desinstalas la app, lo que los elimina todos.

---

## 3. DATOS QUE LA APP LEE PERO NO GUARDA

| dato | para qué | requiere |
|---|---|---|
| la lista de apps instaladas (nombres, nombres de paquete, iconos) | mostrar el cajón, el selector de favoritas y el selector de widgets | `QUERY_ALL_PACKAGES` |
| la app que está en primer plano (solo su nombre de paquete) | aplicar bloqueos de perfil, horarios y límites diarios a apps abiertas fuera de focusphone, y contar el tiempo de uso | el servicio de accesibilidad opcional |
| las notificaciones que muestra android en ese momento | listarlas en el panel lateral y permitir descartarlas; se guardan solo en memoria y se olvidan al retirar el acceso | el acceso a notificaciones opcional |
| lo que suena en las apps de música (título, artista, carátula, estado de reproducción) | mostrarlo en los widgets de música, con controles | el acceso a notificaciones opcional |
| nivel de batería, almacenamiento libre, uso de memoria, próxima alarma y estado de la linterna | mostrarlos en los widgets de sistema (los botones de wi-fi, bluetooth y volumen solo abren los paneles de android) | nada adicional |

el servicio de accesibilidad solo lee el nombre de paquete de la ventana activa. no lee
texto, no hace capturas, no escribe ni pulsa nada por ti.

---

## 4. WIDGETS EN OTROS LAUNCHERS

los 20 widgets de focusphone se pueden colocar en cualquier launcher. android dibuja cada
widget dentro del launcher que lo aloja, así que ese launcher recibe lo que el widget
muestra (por ejemplo tus apps favoritas, tu texto de intención, la hora de las ciudades
elegidas o la canción que suena). focusphone no le envía nada más. lo que ese launcher haga
con ello se rige por su propia política.

---

## 5. PERMISOS

| permiso o servicio | se concede | qué pasa sin él |
|---|---|---|
| `QUERY_ALL_PACKAGES` | al instalar | el launcher no puede listar tus apps |
| `REQUEST_DELETE_PACKAGES` | al instalar | la app no puede abrir el diálogo de desinstalación de android; android siempre pide confirmación |
| `EXPAND_STATUS_BAR` | al instalar | en versiones antiguas de android, deslizar hacia abajo no abre la persiana de notificaciones |
| servicio de accesibilidad | opcional, lo activas tú en los ajustes de android | los bloqueos y horarios solo se aplican a las apps abiertas desde focusphone; los límites diarios no se miden |
| acceso a notificaciones | opcional, lo activas tú en los ajustes de android | sin panel de notificaciones ni widgets de música |

la justificación técnica de cada uno está en [`permissions.es.md`](./permissions.es.md).
puedes retirar los dos servicios opcionales en cualquier momento desde los ajustes de
android.

---

## 6. COPIAS DE SEGURIDAD Y CAMBIO DE DISPOSITIVO

`android:allowBackup="false"` desactiva la copia en la nube de android y `adb backup`, y
`res/xml/data_extraction_rules.xml` excluye todos los dominios de datos de la copia en la
nube y de la transferencia entre dispositivos en android 12 y posteriores. de todos modos,
los valores cifrados no podrían leerse en otro dispositivo, porque la clave del almacén de
claves no puede salir del original. al reinstalar la app o cambiar de móvil tendrás que
configurarla de nuevo.

---

## 7. REGISTROS DE DIAGNÓSTICO

las compilaciones de publicación eliminan los mensajes de registro de depuración e
informativos. los avisos y errores se siguen escribiendo en el registro del sistema de
android y pueden incluir el nombre de paquete de una app cuando falla una operación sobre
ella (por ejemplo, si una app no se puede abrir). el registro del sistema se queda en el
dispositivo, focusphone no lo lee ni lo envía, y otras apps no pueden leerlo.

---

## 8. TUS DERECHOS

como la app no envía datos personales al autor ni a nadie, el autor no tiene ninguno de los
datos de tu app y no puede acceder a ellos, corregirlos ni borrarlos. los controlas tú
directamente: cámbialos en la app, borra el almacenamiento de la app en los ajustes de
android o desinstálala.

si contactas con el autor (por correo o mediante un issue de github), el autor recibe los
datos que decidas enviar —por ejemplo tu dirección de correo, tu usuario de github y lo que
escribas— y los usa solo para responderte y, en los informes de error, para corregir el
fallo. se conservan solo mientras la conversación o el issue lo necesiten. conforme al
reglamento general de protección de datos de la ue (rgpd) y a la ley orgánica 3/2018
(lopdgdd), cuando sean aplicables, puedes solicitar el acceso, la rectificación, la
supresión, la limitación o la portabilidad de esos datos, u oponerte a su tratamiento,
escribiendo a `contacto@rezzt.dev`. también tienes derecho a presentar una reclamación ante
una autoridad de control, en españa la agencia española de protección de datos
(<https://www.aepd.es>).

---

## 9. MENORES

la app no recoge datos de nadie, tampoco de menores. no está diseñada como control parental
ni para supervisar a otra persona (consulta [`disclaimer.es.md`](./disclaimer.es.md)).

---

## 10. CAMBIOS EN ESTA POLÍTICA

los cambios se publican en este fichero, con una nueva fecha de "última actualización", y
se registran en [`changelog.md`](./changelog.md). si una versión futura necesitara alguna
vez enviar datos fuera del dispositivo, esta política se actualizaría antes de publicar esa
versión.

---

<div align="center">

<sub>focusphone — desarrollado por <a href="https://rezzt.dev"><b>rezzt.dev</b></a></sub>

</div>
