<div align="center">

# PRIVACIDAD Y PERMISOS

<sub><a href="../README.md">indice</a> · <a href="../../en/users/11-privacy-and-permissions.md">english</a> · anterior: <a href="./10-referencia-de-ajustes.md">referencia de ajustes</a> · siguiente: <a href="./12-preguntas-frecuentes.md">preguntas frecuentes</a></sub>

</div>

---

## RESUMEN EN CINCO PUNTOS

<table>
<tr><td width="4%">1</td><td><strong>focusphone no se conecta a internet.</strong> no hay cuentas, ni sincronizacion, ni servidores, ni actualizaciones en linea dentro de la app.</td></tr>
<tr><td>2</td><td><strong>no hay publicidad ni telemetria.</strong> no se recoge ningun dato de uso, ni anonimo ni identificado.</td></tr>
<tr><td>3</td><td><strong>todo se queda en el dispositivo.</strong> tus favoritos, nombres personalizados, carpetas, reglas y contraseñas no salen de tu movil.</td></tr>
<tr><td>4</td><td><strong>lo sensible esta cifrado.</strong> los datos que revelan como usas el movil se guardan cifrados con una clave que nunca sale del hardware seguro de android.</td></tr>
<tr><td>5</td><td><strong>no hay copia de seguridad.</strong> ni en la nube ni en la transferencia de un movil a otro: los datos de focusphone estan excluidos a proposito.</td></tr>
</table>

---

## PERMISOS QUE DECLARA LA APLICACION

focusphone declara tres permisos en total, y solicita dos servicios opcionales.

<table>
<tr>
<th align="left" width="30%">permiso</th>
<th align="left" width="20%">cuando se concede</th>
<th align="left" width="50%">para que se usa</th>
</tr>
<tr>
<td><strong>ver todas las aplicaciones</strong><br><code>QUERY_ALL_PACKAGES</code></td>
<td>en la instalacion, automaticamente</td>
<td>listar las aplicaciones instaladas en el cajon y en el selector de favoritos. es imprescindible para cualquier lanzador: sin el, no hay nada que mostrar.</td>
</tr>
<tr>
<td><strong>solicitar desinstalaciones</strong><br><code>REQUEST_DELETE_PACKAGES</code></td>
<td>en la instalacion, automaticamente</td>
<td>abrir el dialogo de desinstalacion de android cuando eliges <em>desinstalar</em>. focusphone <strong>no desinstala nada por si mismo</strong>: siempre confirma el sistema.</td>
</tr>
<tr>
<td><strong>desplegar la barra de estado</strong><br><code>EXPAND_STATUS_BAR</code></td>
<td>en la instalacion, automaticamente</td>
<td>el gesto de deslizar hacia abajo para abrir la persiana de notificaciones del sistema.</td>
</tr>
</table>

### SERVICIOS OPCIONALES

<table>
<tr>
<th align="left" width="30%">servicio</th>
<th align="left" width="20%">cuando se pide</th>
<th align="left" width="50%">que hace y que no hace</th>
</tr>
<tr>
<td><strong>servicio de accesibilidad</strong></td>
<td>al iniciar un detox, o desde ajustes → sistema</td>
<td><strong>hace</strong>: detecta que aplicacion pasa a primer plano para bloquearla si toca, cuenta el tiempo de uso para los limites diarios y despliega la persiana de notificaciones.<br><strong>no hace</strong>: no lee el contenido de las pantallas, no escribe texto, no pulsa botones por ti y no envia nada a ningun sitio.</td>
</tr>
<tr>
<td><strong>acceso a notificaciones</strong></td>
<td>al activar el panel de notificaciones</td>
<td><strong>hace</strong>: muestra tus notificaciones en el cajon lateral y permite descartarlas.<br><strong>no hace</strong>: no guarda el contenido en disco, no lo indexa y no lo envia a ningun sitio.</td>
</tr>
</table>

> ambos servicios son **opcionales**. focusphone funciona sin ninguno de los dos; solo
> pierdes el bloqueo desde fuera de la app, los limites diarios de uso y el panel de
> notificaciones.

---

## QUE DATOS GUARDA Y DONDE

todos los datos viven en el almacenamiento privado de la aplicacion, al que ninguna otra
aplicacion puede acceder.

<table>
<tr>
<th align="left" width="35%">dato</th>
<th align="left" width="30%">como se guarda</th>
<th align="left" width="35%">cifrado</th>
</tr>
<tr><td>aplicaciones favoritas y su orden</td><td>base de datos local</td><td>no, no es informacion sensible por si misma</td></tr>
<tr><td>tema, tipografia, tamaño, alineacion</td><td>preferencias locales</td><td>no</td></tr>
<tr><td>ajustes del reloj, widget y distribucion</td><td>preferencias locales</td><td>no</td></tr>
<tr><td>ajustes del fondo de pantalla</td><td>preferencias locales</td><td>no</td></tr>
<tr><td>imagen de fondo de pantalla</td><td>copia en el almacenamiento privado de la app</td><td>no, pero es inaccesible para otras apps</td></tr>
<tr><td><strong>nombres personalizados de aplicaciones</strong></td><td>preferencias locales</td><td><strong>si</strong>, aes-256/gcm</td></tr>
<tr><td><strong>carpetas del cajon</strong></td><td>preferencias locales</td><td><strong>si</strong>, aes-256/gcm</td></tr>
<tr><td><strong>perfil activo, temporizador de detox y apps bloqueadas</strong></td><td>preferencias locales</td><td><strong>si</strong>, aes-256/gcm</td></tr>
<tr><td><strong>palabras clave del modo trabajo</strong></td><td>preferencias locales</td><td><strong>si</strong>, aes-256/gcm</td></tr>
<tr><td><strong>reglas de horarios y contadores de uso</strong></td><td>preferencias locales</td><td><strong>si</strong>, aes-256/gcm</td></tr>
<tr><td><strong>contraseñas de detox y de horarios</strong></td><td>preferencias locales</td><td><strong>nunca se guarda la contraseña</strong>: solo un resumen criptografico con sal aleatoria</td></tr>
<tr><td>notificaciones mostradas en el panel</td><td>solo en memoria ram</td><td>no llegan a escribirse en disco</td></tr>
</table>

<details>
<summary><strong>que significa "cifrado con aes-256/gcm"</strong></summary>

<br>

el dato se guarda convertido en un texto ilegible. la clave necesaria para volver a leerlo
la genera y custodia el **almacen de claves de android**, un componente respaldado por
hardware en la mayoria de dispositivos modernos: la clave nunca sale de ahi, ni siquiera
focusphone la puede leer, solo puede pedirle que cifre y descifre.

en la practica: si alguien extrae los ficheros de tu movil, obtiene datos ilegibles. y
como la clave no se puede copiar a otro dispositivo, una copia restaurada en otro movil
tampoco serviria de nada.

</details>

<details>
<summary><strong>por que las contraseñas no se guardan</strong></summary>

<br>

lo que se guarda no es tu contraseña, sino el resultado de pasarla por **pbkdf2-hmac-sha256
con 100.000 iteraciones y una sal aleatoria de 128 bits**. de ese resultado no se puede
recuperar la contraseña original, y la sal hace que dos personas con la misma contraseña
tengan resumenes distintos.

las 100.000 iteraciones hacen que cada comprobacion sea deliberadamente lenta, lo que
convierte un ataque por fuerza bruta en algo inviable.

</details>

---

## COPIAS DE SEGURIDAD

focusphone **se excluye de todos los mecanismos de copia**:

| mecanismo | estado | motivo |
|---|---|---|
| copia de seguridad en la nube de android | desactivada | evita que tus favoritos, temas y nombres personalizados salgan del dispositivo |
| `adb backup` | desactivada | mismo motivo |
| transferencia de dispositivo a dispositivo | excluida | desde android 12 esta transferencia no la cubre el ajuste anterior, asi que se excluye explicitamente |

**consecuencia practica**: al cambiar de movil o reinstalar la aplicacion, tendras que
volver a elegir tus favoritos y tus ajustes. es el precio de que tus datos no viajen a
ningun sitio.

---

## LO QUE FOCUSPHONE **NO** PUEDE HACER

| | |
|---|---|
| no puede impedir que lo desinstales | no es un administrador de dispositivos; una sesion de detox no sobrevive a una desinstalacion |
| no puede impedir que cambies de lanzador | siempre puedes volver a tu lanzador anterior desde los ajustes de android |
| no puede bloquear los ajustes del sistema | los ajustes de android quedan siempre accesibles |
| no puede vigilar el movil de otra persona | no envia informes a ningun sitio y no tiene modo administrador |
| no puede leer tus mensajes ni tus contenidos | ni siquiera con el servicio de accesibilidad activo: solo consulta que aplicacion esta en primer plano |

los bloqueos de focusphone son **un compromiso contigo mismo**, reforzado con contraseñas y
esperas para que no se puedan deshacer en un impulso, pero nunca una jaula de la que no se
pueda salir.

---

## PROTECCIONES ADICIONALES

| proteccion | detalle |
|---|---|
| capturas bloqueadas | la pantalla de bloqueo de detox y los dialogos de contraseña no se pueden capturar ni grabar |
| reglas ocultas | la pantalla de horarios no muestra nada, ni siquiera la lista de aplicaciones con reglas, hasta que se introduce la contraseña correcta |
| sin borrado por intentos fallidos | fallar la contraseña nunca borra la configuracion: solo activa una espera de 15 minutos |
| validacion de colores | los colores del tema personalizado se validan antes de usarse, y un valor invalido se sustituye por uno seguro |

---

<div align="center">

<sub><a href="./10-referencia-de-ajustes.md">← referencia de ajustes</a> · <a href="../README.md">indice</a> · <a href="./12-preguntas-frecuentes.md">preguntas frecuentes →</a></sub>

</div>
