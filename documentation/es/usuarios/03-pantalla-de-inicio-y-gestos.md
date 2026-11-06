<div align="center">

# PANTALLA DE INICIO Y GESTOS

<sub><a href="../README.md">indice</a> · <a href="../../en/users/03-home-screen-and-gestures.md">english</a> · anterior: <a href="./02-instalacion-y-primeros-pasos.md">instalacion</a> · siguiente: <a href="./04-aplicaciones-y-carpetas.md">aplicaciones y carpetas</a></sub>

</div>

---

## ANATOMIA DE LA PANTALLA DE INICIO

la pantalla de inicio es una **cuadricula**, como el escritorio de un launcher de serie, con
dos cajones laterales. contiene tres tipos de bloque, y cada uno ocupa un rectangulo de
celdas:

```
┌──────────────────────────────────┐
│ ┌──────────────────────────────┐ │
│ │            09:41             │ │  ← reloj (5 estilos, se toca para la alarma)
│ │       lunes, 12 de mayo      │ │
│ └──────────────────────────────┘ │
│ ┌─────────────┐ ┌──────────────┐ │
│ │   bateria   │ │    musica    │ │  ← widgets (los que quieras, opcionales)
│ └─────────────┘ └──────────────┘ │
│ ┌──────────────────────────────┐ │
│ │           telefono           │ │
│ │           mensajes           │ │  ← favoritos (hasta 10, solo texto)
│ │            camara            │ │
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
   ↑ deslizar derecha    ↑ deslizar izquierda
   notificaciones        menu
```

<table>
<tr>
<th align="left">bloque</th>
<th align="left">que es</th>
<th align="left">se puede quitar</th>
</tr>
<tr>
<td>reloj</td>
<td>la hora y la fecha, en uno de cinco estilos; se encoge para caber en un bloque pequeño en vez de recortarse</td>
<td>si, y se vuelve a añadir desde el modo edicion</td>
</tr>
<tr>
<td>widgets</td>
<td>los widgets que quieras: los 20 propios de focusphone o los de tus otras aplicaciones (consulta <a href="./09-notificaciones-y-widgets.md">notificaciones y widgets</a>)</td>
<td>si</td>
</tr>
<tr>
<td>favoritos</td>
<td>la lista de texto con tus aplicaciones elegidas</td>
<td>no, es el nucleo de la pantalla</td>
</tr>
</table>

| cuadricula | valor |
|---|---|
| columnas | de 3 a 6 (4 por defecto) |
| filas | de 4 a 12 (por defecto, las que hacen las celdas casi cuadradas en tu pantalla) |
| paginas | una: los deslizamientos laterales ya son de los cajones |

> si usabas una version anterior, tu disposicion previa (orden, reloj oculto, el widget y su
> altura) se pasa a la cuadricula automaticamente la primera vez.

---

## MODO EDICION

la disposicion se edita en la propia pantalla de inicio.

**para entrar en el modo edicion**: manten pulsado un hueco libre, un widget o el reloj, o
usa **ajustes → pantalla de inicio → editar pantalla de inicio**. los cajones se bloquean
mientras editas.

| quieres | haz esto |
|---|---|
| mover un bloque | arrastralo; los demas bloques le hacen sitio cuando la celda de destino se queda quieta un momento |
| cambiar el tamaño de un bloque | tocalo para seleccionarlo y tira de las asas de los lados por los que puede crecer; los tamaños se ajustan a celdas enteras |
| usar las acciones de un bloque | tocalo: una barra flotante muestra sus acciones — **ajustes del widget** y **quitar** en un widget, **estilo** y **quitar** en el reloj, **elegir apps** en los favoritos |
| añadir un widget | **widgets** en la barra inferior abre el selector de widgets |
| recuperar el reloj | **añadir reloj** en la barra inferior |
| cambiar el tamaño de la cuadricula | **cuadricula** en la barra inferior, con vista previa en vivo de columnas y filas |
| terminar | **listo** en la barra inferior |

<details>
<summary><strong>cuando no hay sitio</strong></summary>

<br>

un widget nuevo se coloca en orden de lectura. si la cuadricula esta llena, focusphone
encoge primero la lista de favoritos desde su borde superior y despues otros bloques. si aun
asi no hay sitio, te avisa: *no hay sitio en la pantalla de inicio. quita o reduce un bloque
primero.*

</details>

<details>
<summary><strong>editar sin arrastrar</strong></summary>

<br>

en el modo edicion cada bloque ofrece tambien acciones de accesibilidad —mover arriba,
abajo, a la izquierda o a la derecha, ensanchar o estrechar, hacer mas alto o mas bajo—
para cambiar la disposicion con talkback o con acceso por interruptor.

</details>

---

## LOS CINCO ESTILOS DE RELOJ

| estilo | aspecto | comentario |
|---|---|---|
| digital | `09:41` | el clasico, una sola linea con la fecha debajo |
| apilado | `09` sobre `41` | horas y minutos en dos lineas, tipografia muy grande |
| en palabras | `nueve` / `cuarenta y uno` | la hora escrita, la opcion mas tranquila |
| analogico | esfera dibujada | un reloj de agujas minimalista |
| oculto | — | sin reloj; la pantalla se queda solo con los favoritos |

el reloj respeta el formato de 12 o 24 horas configurado en tu sistema, y el tamaño se
ajusta con un deslizador en **ajustes → pantalla de inicio → tamaño del reloj**.

> **tocar el reloj abre la aplicacion de alarmas del sistema.** es el atajo mas usado de
> focusphone: desbloquear, mirar la hora, poner una alarma y volver a guardar el movil.

---

## LOS GESTOS

todos los gestos se hacen sobre la pantalla de inicio, en cualquier punto libre.

```mermaid
flowchart TD
    H["pantalla de inicio"]
    H -->|"deslizar hacia la izquierda"| M["menu lateral derecho"]
    H -->|"deslizar hacia la derecha"| N["panel de notificaciones lateral"]
    H -->|"deslizar hacia abajo"| S["persiana de notificaciones del sistema"]
    H -->|"tocar el reloj"| A["aplicacion de alarmas"]
    H -->|"mantener pulsado un hueco, un widget o el reloj"| E["modo edicion"]
    H -->|"mantener pulsada una app"| C["menu de la aplicacion"]
    H -->|"mantener y arrastrar una app"| R["reordenar la lista"]
```

<table>
<tr>
<th align="left" width="34%">gesto</th>
<th align="left" width="33%">resultado</th>
<th align="left" width="33%">condiciones</th>
</tr>
<tr>
<td><strong>deslizar hacia la izquierda</strong></td>
<td>abre el menu lateral: calendario, todas las apps, editar dashboard, ajustes y perfil</td>
<td>siempre disponible</td>
</tr>
<tr>
<td><strong>deslizar hacia la derecha</strong></td>
<td>abre el panel lateral de notificaciones de focusphone</td>
<td>requiere activar el panel en ajustes y conceder acceso a notificaciones</td>
</tr>
<tr>
<td><strong>deslizar hacia abajo</strong></td>
<td>despliega la persiana de notificaciones de android</td>
<td>requiere activar el gesto en ajustes; funciona mejor con el servicio de accesibilidad activo</td>
</tr>
<tr>
<td><strong>tocar el reloj</strong></td>
<td>abre la aplicacion de alarmas del sistema</td>
<td>siempre disponible, si el reloj esta visible</td>
</tr>
<tr>
<td><strong>mantener pulsado un hueco, un widget o el reloj</strong></td>
<td>entra en el <a href="#modo-edicion">modo edicion</a></td>
<td>siempre disponible</td>
</tr>
<tr>
<td><strong>mantener pulsada una app</strong></td>
<td>abre sus opciones: los accesos directos de la app, quitar de inicio, info de la app, desinstalar</td>
<td>siempre disponible</td>
</tr>
<tr>
<td><strong>mantener y arrastrar una app</strong></td>
<td>cambia el orden de la lista de favoritos</td>
<td>siempre disponible</td>
</tr>
</table>

<details>
<summary><strong>por que no se puede abrir un cajon estando el otro abierto</strong></summary>

<br>

si ya hay un cajon abierto, el gesto de deslizar no abre el contrario: primero hay que
cerrar el actual (tocando fuera o con el boton de atras). es deliberado, para evitar que
un gesto rapido acabe en el cajon equivocado.

</details>

---

## EL MENU LATERAL

se abre deslizando hacia la izquierda. contiene, de arriba abajo:

| elemento | que hace |
|---|---|
| calendario del mes | muestra el mes actual con el dia de hoy marcado; se actualiza solo al cambiar de dia |
| todas las apps | abre el cajon completo de aplicaciones, con buscador y carpetas |
| editar dashboard | abre el selector de favoritos |
| ajustes de focus | abre la pantalla de ajustes |
| perfil seleccionado | muestra el perfil activo y, al tocarlo, permite cambiarlo |

<details>
<summary><strong>el calendario</strong></summary>

<br>

es un calendario de solo lectura: muestra el mes en curso con los nombres de los dias
en el orden e idioma de tu sistema, y el dia de hoy resaltado. no se puede navegar a
otros meses ni crear eventos, a proposito: es una referencia rapida, no una agenda.

como el lanzador puede quedarse abierto durante dias, el calendario se reconstruye solo
cuando cambia el dia, sin que tengas que salir y volver a entrar.

</details>

---

## LA LISTA DE FAVORITOS

- muestra **solo texto**, sin iconos, con el nombre que tu elijas para cada aplicacion
- admite un maximo de **10 aplicaciones**
- se alinea a la izquierda, al centro o a la derecha (**ajustes → apariencia → alineacion**),
  algo util para usar el movil con una sola mano
- el espacio entre aplicaciones es ajustable (**ajustes → pantalla de inicio → espaciado**)
- si esta vacia, muestra el mensaje *aun no hay apps. toca aqui para elegir las pocas que de
  verdad necesitas.*, y al tocarlo se abre directamente el selector de apps

<details>
<summary><strong>reordenar la lista</strong></summary>

<br>

manten pulsada una aplicacion y, sin levantar el dedo, arrastrala a su nueva posicion. el
nuevo orden se guarda automaticamente. reordenar **nunca** elimina una aplicacion de la
lista: si el reordenado no incluye alguna de tus favoritas, esa aplicacion se conserva
detras de las demas.

</details>

<details>
<summary><strong>el menu de cada aplicacion</strong></summary>

<br>

manteniendo pulsada una aplicacion sin arrastrarla se abre un dialogo con su nombre (un
toque ya la abre, asi que no hay opcion "abrir"):

| opcion | que hace |
|---|---|
| los accesos directos de la app | los atajos que publica la aplicacion (por ejemplo "nuevo mensaje"), que se abren con las mismas comprobaciones de perfil, horario y pausa consciente que la app |
| quitar de inicio | la saca de los favoritos, sin desinstalarla |
| info de la app | abre la ficha de la aplicacion en los ajustes de android |
| desinstalar | lanza el dialogo de desinstalacion del sistema |

</details>

---

## EL INDICADOR DE PAGINA

en la parte inferior aparecen dos puntos muy discretos cuando arrastras lateralmente:
indican si estas en la pantalla de inicio o en el menu. desaparecen en cuanto sueltas.

---

<div align="center">

<sub><a href="./02-instalacion-y-primeros-pasos.md">← instalacion</a> · <a href="../README.md">indice</a> · <a href="./04-aplicaciones-y-carpetas.md">aplicaciones y carpetas →</a></sub>

</div>
