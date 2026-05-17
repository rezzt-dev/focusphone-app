# FocusPhone — Guía de Descarga e Instalación

> **Versión:** 1.0.4  
> **Requisito mínimo:** Android 7.0 (API 24)  
> **Idiomas disponibles:** Español, English, Français, Deutsch  

---

## 1. Resumen

FocusPhone es un launcher de Android de código abierto diseñado para reducir la distracción digital. Al instalarse, reemplaza tu pantalla de inicio habitual por una interfaz minimalista de solo texto: un reloj, un calendario y hasta 10 aplicaciones favoritas.

Este documento te guía paso a paso para descargar el APK, instalarlo fuera de Google Play y configurarlo como launcher predeterminado.

---

## 2. Requisitos del dispositivo

| Requisito | Detalle |
|-----------|---------|
| **Sistema operativo** | Android 7.0 (Nougat) o superior |
| **API mínima** | 24 |
| **Espacio libre** | ~15 MB de almacenamiento interno (ajustar al tamaño real del APK) |
| **Conexión** | Wi-Fi o datos móviles para la descarga inicial |
| **Permisos especiales** | "Instalar aplicaciones desconocidas" (se solicita durante el proceso) |

> **Nota:** FocusPhone no requiere una cuenta de Google ni acceso a servicios de Play para funcionar. Es 100 % offline tras la instalación.

---

## 3. Descarga del APK

Elige cualquiera de los dos canales oficiales de distribución:

### Canal A — GitHub Releases (recomendado)
1. Visita la página de releases del repositorio:  
   👉 **[github.com/rezzt-devc/focusphone-app/releases](../../releases)**
2. En la última versión publicada, descarga el archivo llamado:  
   `focusphone-v1.0.4-release.apk` (ajustar nombre si cambia)
3. El archivo se guardará en la carpeta **Descargas** de tu dispositivo.

### Canal B — Hosting propio / Landing page
1. Accede a la sección de descargas del sitio oficial:  
   👉 **https://rezzt.dev/focusphone** *(URL de ejemplo — actualizar con el enlace definitivo)*
2. Toca el botón **Descargar APK** y espera a que finalice la descarga.

> **Seguridad:** Ambos canales distribuyen el mismo APK firmado. Puedes verificar la firma comparando el hash SHA-256 publicado en la release de GitHub con el archivo descargado.

---

## 4. Instalación paso a paso

### Paso 1 — Habilitar "Orígenes desconocidos"

Android bloquea por defecto la instalación de aplicaciones fuera de Google Play. Debes autorizar al navegador o gestor de archivos que usaste para descargar el APK.

**En Android 8.0 (API 26) o superior:**
1. Abre **Ajustes** → **Aplicaciones**.
2. Busca y selecciona la app que descargó el APK (normalmente **Chrome**, **Firefox** o **Archivos**).
3. Toca **Instalar aplicaciones desconocidas** (o **Orígenes desconocidos**).
4. Activa el interruptor **Permitir desde esta fuente**.

**En Android 7.0 – 7.1 (API 24 – 25):**
1. Abre **Ajustes** → **Seguridad**.
2. Busca la opción **Orígenes desconocidos**.
3. Actívala y confirma el aviso de seguridad.

> **Importante:** Puedes desactivar esta opción nuevamente después de instalar FocusPhone.

### Paso 2 — Instalar el APK

1. Abre tu gestor de archivos y navega a la carpeta **Descargas**.
2. Localiza el archivo `focusphone-v1.0.4-release.apk`.
3. Toca sobre el archivo. Si aparece un diálogo de seguridad, toca **Instalar de todos modos**.
4. Espera a que aparezca el mensaje **Aplicación instalada**.
5. Toca **Abrir** para lanzar FocusPhone, o **Listo** para salir al escritorio.

> **Problema común:** Si el sistema muestra "No se puede instalar el paquete", revisa la sección [7. Troubleshooting](#7-troubleshooting) más abajo.

---

## 5. Configuración post-instalación

### Establecer FocusPhone como launcher predeterminado

La primera vez que abras FocusPhone, Android te preguntará si deseas usarlo como pantalla de inicio predeterminada.

1. Toca el botón de inicio (🏠) de tu dispositivo.
2. Aparecerá un diálogo: **¿Con qué aplicación deseas completar la acción?**
3. Selecciona **FocusPhone**.
4. Activa **Usar siempre** o **Establecer como predeterminado**.

**Si el diálogo no aparece automáticamente**, configúralo manualmente:

1. Abre **Ajustes** del sistema → **Aplicaciones** (o **Aplicaciones predeterminadas**).
2. Busca **Aplicación de inicio** / **Launcher predeterminado** / **Pantalla de inicio**.
3. Selecciona **FocusPhone** de la lista.

> **Tip:** Si en algún momento quieres volver temporalmente a tu launcher anterior sin desinstalar FocusPhone, simplemente selecciona otro launcher en ese mismo menú.

### Completar el onboarding

La primera vez que inicies FocusPhone verás una introducción de 3 pasos:

1. **Bienvenida:** conoce la filosofía del launcher.
2. **Selección de apps:** elige hasta 10 aplicaciones favoritas para tu pantalla de inicio.
3. **Personalización:** ajusta tu tema, tipo de reloj y alineación.

Puedes modificar estas preferencias más tarde desde el menú lateral (desliza desde la izquierda) → **Ajustes**.

---

## 6. Desinstalación o rollback al launcher anterior

### Opción A — Desinstalar FocusPhone completamente

1. Abre **Ajustes** del sistema → **Aplicaciones**.
2. Busca **FocusPhone** y tócala.
3. Tapa **Desinstalar** y confirma.

> **Atención:** Si FocusPhone es tu launcher activo, al desinstalarlo Android revertirá automáticamente al launcher del sistema (normalmente Pixel Launcher, One UI Home, etc.).

### Opción B — Cambiar de launcher sin desinstalar

1. Abre **Ajustes** del sistema → **Aplicaciones predeterminadas** → **Aplicación de inicio**.
2. Selecciona tu launcher anterior (ej. **Pixel Launcher**).
3. Ahora puedes desinstalar FocusPhone desde **Ajustes → Aplicaciones** si lo deseas.

---

## 7. Troubleshooting

| Problema | Causa probable | Solución |
|----------|---------------|----------|
| **"No se puede instalar el paquete"** | El APK está corrupto, incompleto o tu versión de Android es inferior a 7.0. | Verifica que descargaste el archivo completo. Comprueba que tu Android sea 7.0+. Descarga nuevamente desde GitHub Releases. |
| **"Instalar aplicaciones desconocidas" no aparece** | Estás en Android 7.x y la opción está en otro menú. | Ve a **Ajustes → Seguridad → Orígenes desconocidos**. |
| **FocusPhone no aparece como opción de launcher** | No has abierto la app al menos una vez, o el filtro de launcher no se registró correctamente. | Abre FocusPhone manualmente desde el cajón de aplicaciones. Luego presiona el botón de inicio. |
| **No puedo desinstalar FocusPhone** | Sigue siendo el launcher predeterminado. | Cambia primero a otro launcher en **Ajustes → Aplicaciones predeterminadas**, luego desinstala. |
| **La app se cierra al abrir** | Conflicto con el launcher del sistema o datos corruptos. | Ve a **Ajustes → Aplicaciones → FocusPhone → Almacenamiento → Borrar caché y datos**. Vuelve a configurar el onboarding. |
| **No veo todas mis apps** | FocusPhone requiere el permiso `QUERY_ALL_PACKAGES` para listar aplicaciones. | Asegúrate de haber aceptado todos los permisos al inicio. Si los rechazaste, ve a **Ajustes → Aplicaciones → FocusPhone → Permisos** y habilita los necesarios. |

---

## 8. Preguntas frecuentes (FAQ)

**¿Es seguro instalar APKs fuera de Google Play?**  
Sí, siempre que descargues desde fuentes oficiales. FocusPhone es código abierto (licencia MIT), se compila públicamente y las releases de GitHub incluyen el APK firmado por el autor.

**¿Perderé mis apps o datos al cambiar de launcher?**  
No. Los launchers solo cambian la capa visual de tu pantalla de inicio. Tus aplicaciones, fotos, contactos y archivos permanecen intactos.

**¿Puedo volver a FocusPhone después de cambiar a otro launcher?**  
Sí. Mientras no lo desinstales, puedes volver a seleccionarlo como predeterminado desde **Ajustes → Aplicaciones predeterminadas**.

**¿FocusPhone consume mucha batería?**  
No. Al ser un launcher minimalista sin widgets complejos ni servicios en segundo plano, su consumo de batería es inferior al de la mayoría de launchers comerciales.

**¿Necesito internet para usar FocusPhone?**  
No. La única conexión requerida es para la descarga inicial. Todas las funciones (reloj, calendario, perfiles, bloqueo detox) funcionan 100 % offline.

---

## 9. Información de soporte

- **Repositorio:** [github.com/rezzt-devc/focusphone-app](https://github.com/rezzt-devc/focusphone-app)
- **Reportar un problema:** [GitHub Issues](../../issues)
- **Web del autor:** [rezzt.dev](https://rezzt.dev)
- **Licencia:** MIT

---

<div align="center">

**[⬅ Volver a la documentación del producto](../)**

</div>
