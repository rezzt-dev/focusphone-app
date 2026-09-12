<div align="center">

# PREGUNTAS FRECUENTES

<sub><a href="../README.md">indice</a> · <a href="../../en/users/12-faq.md">english</a> · anterior: <a href="./11-privacidad-y-permisos.md">privacidad y permisos</a></sub>

</div>

---

## USO GENERAL

<details>
<summary><strong>¿focusphone desinstala o esconde mis aplicaciones?</strong></summary>

<br>

no. focusphone solo decide **que muestra** en la pantalla de inicio. todas tus
aplicaciones siguen instaladas y accesibles desde el cajon de todas las apps. si vuelves a
tu lanzador anterior, lo encontraras todo exactamente como estaba.

</details>

<details>
<summary><strong>¿por que solo 10 aplicaciones favoritas?</strong></summary>

<br>

porque es el limite que mantiene la pantalla de inicio como una lista corta que se lee de
un vistazo. si cupiesen treinta, volveria a ser una cuadricula de opciones y la aplicacion
perderia su razon de ser. las demas aplicaciones estan a dos gestos de distancia en el
cajon.

</details>

<details>
<summary><strong>¿puedo volver a mi lanzador anterior?</strong></summary>

<br>

si, cuando quieras: ajustes de android → aplicaciones → aplicaciones predeterminadas →
aplicacion de inicio, y elige el que usabas antes. no pierdes nada.

</details>

<details>
<summary><strong>¿focusphone necesita internet?</strong></summary>

<br>

no, en ningun momento. la aplicacion es completamente offline: no tiene cuentas, ni
sincronizacion, ni servidores.

</details>

<details>
<summary><strong>¿hay publicidad o compras dentro de la aplicacion?</strong></summary>

<br>

no hay ni lo uno ni lo otro.

</details>

---

## PANTALLA DE INICIO Y GESTOS

<details>
<summary><strong>los gestos de deslizar no responden</strong></summary>

<br>

comprueba lo siguiente, en este orden:

1. que estas deslizando sobre la pantalla de inicio, no dentro de un cajon ya abierto: con
   un cajon abierto, el gesto no abre el contrario
2. que el gesto es lo bastante largo y rapido: un arrastre muy lento no cuenta como
   deslizamiento
3. para el panel de notificaciones, que este activado en **ajustes → notificaciones** y
   que hayas concedido el acceso a notificaciones
4. para el gesto hacia abajo, que este activado y, preferiblemente, que el servicio de
   accesibilidad este activo

</details>

<details>
<summary><strong>el gesto hacia abajo no abre la persiana del sistema</strong></summary>

<br>

en android 12 y posteriores, el metodo alternativo que usa focusphone cuando no hay
servicio de accesibilidad esta bloqueado por el propio sistema. la solucion es activar el
servicio de accesibilidad en **ajustes → sistema → bloqueo del sistema**: con el, el gesto
usa la via oficial y funciona siempre.

</details>

<details>
<summary><strong>el calendario sigue marcando el dia de ayer</strong></summary>

<br>

no deberia ocurrir: focusphone reconstruye el calendario cuando cambia el dia, aunque la
pantalla de inicio lleve dias abierta. si lo ves, abre y cierra el menu lateral para
forzar la actualizacion, y reportalo como incidencia.

</details>

<details>
<summary><strong>¿puedo tener varias pantallas de inicio?</strong></summary>

<br>

no, y es deliberado. focusphone tiene una unica pantalla, mas dos cajones laterales.

</details>

---

## BLOQUEOS, PERFILES Y HORARIOS

<details>
<summary><strong>he olvidado la contraseña de detox</strong></summary>

<br>

no hay forma de recuperarla, pero **no te quedas atrapado**: toda sesion de detox tiene un
temporizador y termina sola al expirar, aunque reinicies el movil. solo tienes que esperar.
cuando la sesion acabe, podras iniciar una nueva con otra contraseña.

</details>

<details>
<summary><strong>he olvidado la contraseña de los horarios</strong></summary>

<br>

la contraseña de horarios protege la pantalla de configuracion. si la olvidas, las reglas
siguen aplicandose y no podras editarlas desde la aplicacion. como ultimo recurso, borrar
los datos de focusphone desde los ajustes de android elimina toda la configuracion, incluidas
las reglas y la contraseña, pero tambien tus favoritos y ajustes.

</details>

<details>
<summary><strong>he fallado la contraseña varias veces y ahora no me deja ni con la correcta</strong></summary>

<br>

es el comportamiento esperado: tras **5 intentos fallidos** hay una espera de **15 minutos**
en la que se rechaza cualquier intento, incluido el correcto. sin esa espera, probar
contraseñas no tendria ningun coste. espera y vuelve a intentarlo.

nada se ha borrado: ni la contraseña, ni las reglas, ni la sesion.

</details>

<details>
<summary><strong>una aplicacion bloqueada se abre igualmente desde una notificacion</strong></summary>

<br>

eso ocurre cuando el **servicio de accesibilidad** no esta activo. sin el, focusphone solo
puede bloquear las aplicaciones que se abren desde la propia aplicacion. activalo en
**ajustes → sistema → bloqueo del sistema** y la aplicacion quedara bloqueada se abra desde
donde se abra.

</details>

<details>
<summary><strong>el limite diario de minutos no descuenta tiempo</strong></summary>

<br>

los limites diarios necesitan el **servicio de accesibilidad**: es el unico componente que
puede medir cuanto tiempo pasa una aplicacion en primer plano. sin el, la franja horaria
funciona pero el contador de minutos no avanza.

</details>

<details>
<summary><strong>¿por que la pantalla de bloqueo por horario no tiene boton de desbloquear?</strong></summary>

<br>

porque un horario es una configuracion permanente, no una sesion. ofrecer un desbloqueo en
la propia pantalla de bloqueo seria poner la salida justo en el momento de mayor tentacion.
para cambiar una regla hay que ir a **ajustes → enfoque → horarios de aplicaciones** e
introducir la contraseña.

</details>

<details>
<summary><strong>el perfil de trabajo bloquea una aplicacion que yo no queria bloquear</strong></summary>

<br>

casi siempre es por una **palabra clave** demasiado generica. revisa
**ajustes → enfoque → palabras clave del modo trabajo**, quita la palabra que esta
provocando la coincidencia y, si aun quieres bloquear alguna aplicacion concreta, marcala
a mano desde el cajon de todas las apps.

</details>

<details>
<summary><strong>¿se puede usar focusphone para controlar el movil de otra persona?</strong></summary>

<br>

no esta pensado para eso. no hay modo administrador, ni informes remotos, ni proteccion
frente a la desinstalacion. focusphone es una herramienta para imponerse limites a uno
mismo.

</details>

---

## APARIENCIA

<details>
<summary><strong>he puesto un tema personalizado y el texto casi no se lee</strong></summary>

<br>

focusphone ajusta automaticamente el color del texto hasta alcanzar un contraste minimo con
el fondo, asi que la interfaz nunca queda del todo ilegible. si aun asi no te convence,
elige colores mas separados entre si: un fondo oscuro con texto claro, o al reves.

</details>

<details>
<summary><strong>he puesto un fondo de pantalla y no veo bien el texto</strong></summary>

<br>

sube el deslizador **legibilidad del texto** en
**ajustes → apariencia → fondo de pantalla**, y activa el **desenfoque**. ambos ajustes se
aplican al instante y la vista previa te enseña el resultado sin salir de la pantalla.

</details>

<details>
<summary><strong>el fondo de pantalla ha desaparecido despues de reiniciar</strong></summary>

<br>

no deberia: focusphone copia la imagen a su almacenamiento privado precisamente para que
sobreviva a los reinicios. si ocurre, vuelve a elegirla y reportalo como incidencia.

</details>

<details>
<summary><strong>¿puedo cambiar el formato de 12 o 24 horas del reloj?</strong></summary>

<br>

si, pero desde los ajustes de android: focusphone respeta el formato del sistema para no
tener dos sitios donde configurar lo mismo.

</details>

---

## DATOS Y ACTUALIZACIONES

<details>
<summary><strong>¿se guardan mis ajustes al cambiar de movil?</strong></summary>

<br>

no. focusphone esta excluido a proposito de la copia de seguridad en la nube y de la
transferencia entre dispositivos. tendras que volver a configurarlo en el movil nuevo. es
el precio de que tus datos no viajen a ningun sitio.

</details>

<details>
<summary><strong>¿pierdo mi configuracion al actualizar la aplicacion?</strong></summary>

<br>

no. una actualizacion normal conserva todos los datos. solo se pierden si desinstalas la
aplicacion o borras sus datos desde los ajustes de android.

</details>

<details>
<summary><strong>¿como se actualiza focusphone?</strong></summary>

<br>

descargando el apk de la version nueva desde la pestaña de releases del repositorio e
instalandolo encima del anterior. la aplicacion no comprueba actualizaciones por su cuenta,
porque no se conecta a internet.

</details>

---

## PROBLEMAS Y REPORTES

<details>
<summary><strong>la aplicacion se comporta de forma extraña despues de una actualizacion</strong></summary>

<br>

prueba, en este orden:

1. cerrar focusphone por completo desde las aplicaciones recientes y volver a abrirlo
2. reiniciar el movil
3. revisar que los permisos opcionales siguen concedidos: algunos fabricantes desactivan
   los servicios de accesibilidad tras una actualizacion

</details>

<details>
<summary><strong>¿donde reporto un error o propongo una funcion?</strong></summary>

<br>

en la pestaña de **issues** del repositorio de github. si es un error, ayuda mucho indicar:

- la version de focusphone (ajustes → acerca de → version)
- el modelo de movil y la version de android
- los pasos exactos para reproducirlo
- si el servicio de accesibilidad estaba activo o no

</details>

<details>
<summary><strong>quiero entender como funciona por dentro</strong></summary>

<br>

toda la documentacion tecnica esta en la seccion para desarrolladores, empezando por
**[arquitectura](../desarrolladores/01-arquitectura.md)**.

</details>

---

<div align="center">

<sub><a href="./11-privacidad-y-permisos.md">← privacidad y permisos</a> · <a href="../README.md">indice</a></sub>

</div>
