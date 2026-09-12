<div align="center">

# DOCUMENTACION DE FOCUSPHONE

<img src="https://img.shields.io/badge/focusphone-documentation-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="focusphone documentation" />

<br>

<strong>documentacion completa del lanzador minimalista focusphone</strong><br>
<strong>complete documentation for the focusphone minimalist launcher</strong>

<br><br>

<img src="https://img.shields.io/badge/version-1.0.6-000000?style=flat-square" alt="version 1.0.6" />
<img src="https://img.shields.io/badge/kotlin-2.0%2B-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="kotlin" />
<img src="https://img.shields.io/badge/min%20sdk-24-3DDC84?style=flat-square&logo=android&logoColor=white" alt="min sdk 24" />
<img src="https://img.shields.io/badge/target%20sdk-36-3DDC84?style=flat-square&logo=android&logoColor=white" alt="target sdk 36" />
<img src="https://img.shields.io/badge/license-non--commercial-orange?style=flat-square" alt="non commercial license" />

</div>

---

## IDIOMAS / LANGUAGES

<table>
<tr>
<td width="50%" valign="top" align="center">

### CASTELLANO

documentacion completa en español, para usuarios y para desarrolladores.

**[abrir la documentacion en castellano →](./es/README.md)**

</td>
<td width="50%" valign="top" align="center">

### ENGLISH

complete documentation in english, for users and for developers.

**[open the documentation in english →](./en/README.md)**

</td>
</tr>
</table>

---

## QUE VAS A ENCONTRAR AQUI

esta carpeta contiene dos documentaciones paralelas, una por idioma, y cada una
se divide en dos recorridos independientes:

<table>
<tr>
<th align="left" width="50%">para usuarios</th>
<th align="left" width="50%">para desarrolladores</th>
</tr>
<tr>
<td valign="top">

no hace falta ningun conocimiento tecnico. se explica que hace la app, como se
instala, como se usa cada gesto, como funcionan los bloqueos y que significa
cada ajuste.

- que es focusphone y para quien es
- instalacion y primeros pasos
- pantalla de inicio y gestos
- aplicaciones, favoritos y carpetas
- perfiles, detox y horarios
- pausa consciente
- personalizacion y apariencia
- notificaciones y widgets
- referencia completa de ajustes
- privacidad y permisos
- preguntas frecuentes

</td>
<td valign="top">

pensado para leer el codigo con contexto. se explica la arquitectura, cada capa,
el sistema de temas, el modelo de seguridad y el proceso de build.

- arquitectura general
- estructura del proyecto
- entorno y compilacion
- capa de datos
- capa de dominio
- capa de presentacion
- sistema de temas y color
- seguridad y cifrado
- servicios del sistema
- pruebas y calidad
- integracion continua y publicacion
- guia de contribucion

</td>
</tr>
</table>

---

## MAPA RAPIDO DEL PROYECTO

```mermaid
flowchart TD
    A["focusphone-app/"] --> B["documentation/"]
    A --> C["focusphone-project/"]
    A --> D["readme.md<br>changelog.md<br>license<br>security.md"]
    A --> E["agents.md<br>claude.md"]

    B --> B1["es/ — castellano"]
    B --> B2["en/ — english"]
    B1 --> B3["usuarios/"]
    B1 --> B4["desarrolladores/"]
    B2 --> B5["users/"]
    B2 --> B6["developers/"]

    C --> C1["app/src/main — codigo de la app"]
    C --> C2["app/src/test — pruebas jvm"]
    C --> C3["app/src/androidTest — pruebas instrumentadas"]
    C --> C4["gradle/ — catalogo de versiones"]
```

---

## CONVENCIONES DE ESTA DOCUMENTACION

<details>
<summary><strong>reglas de formato aplicadas a todos los ficheros</strong></summary>

<br>

| regla | detalle |
|---|---|
| idiomas | cada documento existe en castellano (`es/`) y en ingles (`en/`), con la misma numeracion y el mismo contenido |
| minusculas | todo el contenido esta escrito en minusculas |
| titulos | todos los titulos y encabezados van en mayusculas y sin acentos |
| identificadores | los nombres de clases, ficheros, rutas y comandos se escriben entre comillas invertidas y conservan su capitalizacion real, porque son codigo |
| tablas y diagramas | se usan tablas html y diagramas mermaid, que github renderiza de forma nativa |
| bloques plegables | los detalles largos van dentro de bloques `details` para no romper la lectura |

</details>

<details>
<summary><strong>relacion con la documentacion existente del repositorio</strong></summary>

<br>

| fichero | proposito | audiencia |
|---|---|---|
| `readme.md` (raiz) | presentacion publica del proyecto | visitantes del repositorio |
| `changelog.md` | historial de cambios por version, bilingue | usuarios y desarrolladores |
| `agents.md` / `claude.md` | guia operativa para agentes de ia y para el flujo de commits, pr y releases | agentes y mantenedores |
| `documentation/` | documentacion funcional y tecnica completa | usuarios finales y desarrolladores |

`documentation/` no sustituye a `agents.md`: aquel describe **como se trabaja** en el
repositorio, y este describe **que es y como funciona** el producto.

</details>

---

<div align="center">

<sub>focusphone — desarrollado por <a href="https://rezzt.dev"><b>rezzt.dev</b></a></sub>

</div>
