# CASOS DE USO — CineTrack (CineSStar)

> Derivados de `contexto.md`, `requisitos.md` y `restricciones.md`. La interfaz de referencia está en `design.md`.
> Actor único: **Usuario** (propietario del dispositivo; no hay cuentas ni roles).
> Cada caso de uso lista flujo principal, flujos alternativos (**A**), flujos de excepción/error (**E**), postcondiciones y los requisitos que cubre.
> Los puntos ambiguos de los requisitos se resuelven con **supuestos** (sección 5, `S-xx`). Hay que confirmarlos o corregirlos antes de implementar.

---

## 1. Convenciones

### 1.1 Vocabulario
- **Perfil**: una película o una serie. Identificado por (nombre normalizado, tipo). Es único.
- **Estado**: `PENDIENTE`, `VIENDO` o `VISTO`. En los datos, `VISTO` se guarda como "sin estado" (ni pendiente ni viendo). En la interfaz se muestra como "Visto".
- **Lista**: `NORMAL`, `PRIORIDAD` o `RANKING`. Puede tener restricción de contenido: `MIXTO` (por defecto), `SOLO_PELICULAS` o `SOLO_SERIES`.
- **Elemento de lista**: relación (lista, perfil). En `RANKING` lleva posición; en `PRIORIDAD` lleva nivel (`ALTA`, `MEDIA`, `BAJA`). Ni posición ni prioridad son atributos del perfil.
- **Puntuación**: opcional. Valores de 0.0 a 10 con un decimal (pasos de 0.1). El 10 no lleva decimales.
- **Nombre normalizado**: nombre en minúsculas, sin tildes ni diacríticos y sin espacios sobrantes. Se usa para duplicados y búsqueda.

### 1.2 Identificadores de requisitos
`requisitos.md` y `restricciones.md` no tienen identificadores. Se numeran aquí para trazabilidad (ver matriz en la sección 4).

| ID | Requisito |
|---|---|
| RF-01 | Añadir una película o serie (perfil específico y único) |
| RF-02 | Crear un nuevo perfil |
| RF-03 | Único atributo obligatorio: nombre. Puntuación, género, comentarios, saga e idioma son opcionales (también sin puntuación en cualquier estado) |
| RF-04 | Indicar al crear si es película o serie |
| RF-05 | Impedir o avisar de perfil duplicado (mismo nombre y tipo) |
| RF-06 | Validar la puntuación (rango y formato); mensaje de error; no se registra una inválida; vacía es válida |
| RF-07 | Asignar uno o varios géneros de una lista predefinida |
| RF-08 | Asignar uno o varios idiomas de una lista predefinida |
| RF-09 | Un perfil pertenece como máximo a una saga; el orden en la saga no se guarda |
| RF-10 | Las sagas quedan registradas: elegir una existente o crear una nueva sin reescribirla |
| RF-11 | Una serie tiene una única puntuación para todo el perfil |
| RF-12 | Una serie en estado "viendo" no guarda progreso |
| RF-13 | Modificar la puntuación: añadirla, cambiarla o quitarla |
| RF-14 | Modificar (añadir, cambiar, borrar) el comentario |
| RF-15 | Cambiar el estado entre pendiente, viendo y ninguno (= visto); nunca es obligatorio puntuar |
| RF-16 | Modificar nombre, género, saga e idioma |
| RF-17 | Pasar a "pendiente" o a "viendo" desde cualquier otro estado |
| RF-18 | Borrar una película o serie (se elimina el perfil completo) |
| RF-19 | Confirmación previa al borrar un perfil |
| RF-20 | Al borrar un perfil, se elimina de todas las listas y de la base de datos |
| RF-21 | Visualizar el perfil |
| RF-22 | Ver el estado en la vista del perfil y en los listados |
| RF-23 | Agrupar por rangos de puntuación, sagas, idioma y géneros; sobre todo, solo películas o solo series |
| RF-24 | Agrupar por estado |
| RF-25 | Cinco rangos de puntuación (0–2.9, 3–4.9, 5–6.9, 7–8.9, 9–10) y grupo "Sin puntuación" |
| RF-26 | Un perfil con varios géneros o idiomas aparece en cada grupo |
| RF-27 | Crear una lista de películas/series |
| RF-28 | Crear una lista de prioridad |
| RF-29 | Crear una lista de ranking |
| RF-30 | Una lista admite películas y series mezcladas |
| RF-31 | Un perfil puede estar en varias listas a la vez |
| RF-32 | Una lista puede restringirse a solo series o solo películas |
| RF-33 | Renombrar una lista |
| RF-34 | Añadir y quitar perfiles de una lista |
| RF-35 | Al quitar el último elemento, un único aviso ofrece borrar la lista; si no acepta, puede borrarla después manualmente |
| RF-36 | Modificar la prioridad de un elemento de una lista de prioridad |
| RF-37 | Reordenar un elemento de una lista de ranking |
| RF-38 | Borrar una lista completa con confirmación; no borra los perfiles |
| RF-39 | Al añadir a prioridad/ranking, el usuario indica la prioridad/posición o el sistema la asigna al final |
| RF-40 | La prioridad tiene tres niveles: alta, media, baja |
| RF-41 | Una lista de ranking no tiene tamaño máximo |
| RF-42 | Visualizar los elementos de una lista (normal, prioridad, ranking) |
| RF-43 | Ver todas las listas con nombre, tipo y número de elementos |
| RF-44 | Buscar por puntuación mínima y máxima (sin puntuación quedan fuera), nombre, género, idioma o saga |
| RF-45 | Combinar varios criterios a la vez |
| RF-46 | La búsqueda por nombre no distingue mayúsculas ni acentos |
| RF-47 | Restringir la búsqueda a películas, series o ambas (por defecto, ambas) |
| RF-48 | Sin resultados: mensaje que lo indica |
| RF-49 | Exportar e importar datos (perfiles y listas) a un archivo como copia de seguridad |
| RNF-01 | Máximo 10 elementos visibles a la vez en cualquier lista; el resto con scroll |
| RNF-02 | La posición en ranking no es atributo del perfil |
| RNF-03 | La prioridad no es atributo del perfil |
| RNF-04 | Puntuación 0.0–10, una unidad y un decimal; el 10 sin decimales |
| RNF-05 | Se admite 0.0 y cualquier valor con un decimal entre 0.0 y 10 |
| RNF-06 | Todas las funciones funcionan sin conexión |
| RNF-07 | Persistencia al cerrar la app o reiniciar el dispositivo |
| RNF-08 | Abrir un perfil, una lista o una búsqueda local responde en < 1 s con hasta 1000 perfiles |
| RNF-09 | Añadir perfil, puntuar y añadir a lista se completan en ≤ 3 pasos |
| RNF-10 | Funciona en Android (versión mínima por definir) |
| RNF-11 | Interfaz adaptable a distintos tamaños de pantalla móvil, en vertical como mínimo |
| RNF-12 | Interfaz en español |
| RES-01 | Solo Android |
| RES-02 | Versión mínima de Android sin definir |
| RES-03 | Ejecución íntegra en local; sin servidores ni internet |
| RES-04 | Datos en el dispositivo (SQLite) |
| RES-05 | Sin API externa de catálogo: el usuario introduce los datos |
| RES-06 | Perfil único por nombre y tipo |
| RES-07 | Ranking y prioridad pertenecen a la lista, no al perfil |
| RES-08 | Una lista no puede contener el mismo perfil dos veces |
| RES-09 | Una lista muestra como máximo 10 elementos a la vez |
| RES-10 | Interfaz en español |
| RES-11 | Coste de infraestructura nulo |

### 1.3 Catálogo de mensajes (español)
| ID | Tipo | Texto |
|---|---|---|
| M-01 | Error | "El nombre es obligatorio." |
| M-02 | Error | "Ya existe una {película/serie} con ese nombre." |
| M-03 | Error | "Puntuación no válida. Introduce un valor entre 0.0 y 10 con un máximo de un decimal." |
| M-04 | Error | "Ya existe una saga con ese nombre." |
| M-05 | Error | "El nombre de la saga es obligatorio." |
| M-06 | Confirmación | "¿Eliminar «{nombre}»? Se quitará también de todas tus listas. Esta acción no se puede deshacer." |
| M-07 | Error | "El nombre de la lista es obligatorio." |
| M-08 | Error | "Ya existe una lista con ese nombre." |
| M-09 | Error | "Esta lista solo admite {películas/series}." |
| M-10 | Aviso | "«{nombre}» ya está en esta lista." |
| M-11 | Confirmación | "Has quitado el último elemento. ¿Quieres eliminar también la lista «{lista}»?" |
| M-12 | Confirmación | "¿Eliminar la lista «{lista}»? Los títulos no se borrarán." |
| M-13 | Info | "No se han encontrado resultados." |
| M-14 | Error | "La puntuación mínima no puede ser mayor que la máxima." |
| M-15 | Info | "Copia guardada correctamente." |
| M-16 | Error | "No se pudo guardar la copia. Comprueba el espacio disponible y la ubicación elegida." |
| M-17 | Error | "El archivo no es una copia de seguridad válida de CineTrack." |
| M-18 | Error | "La copia es de una versión más reciente de la aplicación y no se puede importar." |
| M-19 | Confirmación | "Se añadirán a tus datos: {n} perfiles nuevos, {m} listas nuevas y {k} listas ampliadas. {p} perfiles ya existían y se omitirán. Tus datos actuales no se borran. ¿Continuar?" |
| M-20 | Info | "Importación completada: {n} perfiles nuevos, {p} omitidos, {m} listas nuevas, {k} listas ampliadas." |
| M-21 | Error | "No se pudo importar. Tus datos no han cambiado." |
| M-22 | Info | "Todavía no hay títulos. Añade el primero." |
| M-23 | Info | "Esta lista está vacía." |
| M-24 | Error | "Posición no válida." |
| M-25 | Error | "Error inesperado. Tus datos no se han modificado." |

---

## 2. Diagrama de casos de uso (resumen)

```
Usuario ─┬─ Perfiles ──── CU-01 Crear perfil
         │                 CU-02 Editar datos del perfil
         │                 CU-03 Puntuar / modificar puntuación
         │                 CU-04 Editar comentario
         │                 CU-05 Cambiar estado
         │                 CU-06 Eliminar perfil
         │                 CU-07 Ver perfil
         │                 CU-08 Ver biblioteca
         │                 CU-09 Agrupar perfiles
         │                 CU-10 Elegir o crear saga   (<<include>> en CU-01 y CU-02)
         ├─ Listas ─────── CU-11 Crear lista
         │                 CU-12 Ver todas las listas
         │                 CU-13 Ver contenido de una lista
         │                 CU-14 Renombrar lista
         │                 CU-15 Añadir perfil a una lista
         │                 CU-16 Quitar perfil de una lista
         │                 CU-17 Cambiar prioridad de un elemento
         │                 CU-18 Reordenar elemento de un ranking
         │                 CU-19 Eliminar lista
         ├─ Buscar ─────── CU-20 Buscar con filtros
         ├─ Datos ──────── CU-21 Exportar copia de seguridad
         │                 CU-22 Importar copia de seguridad
         └─ Sistema ────── CU-23 Arrancar la aplicación
```

---

## 3. Casos de uso

### CU-01 Crear perfil
- **Objetivo**: registrar una película o serie nueva.
- **Precondiciones**: ninguna.
- **Disparador**: el usuario pulsa "Añadir" (botón central de la barra inferior) o "Añadir título".
- **Flujo principal**
  1. El sistema muestra el formulario "Añadir nuevo perfil". Tipo = Película, estado = Pendiente (como en el prototipo), resto vacío. "Guardar perfil" está deshabilitado.
  2. El usuario elige el tipo (película o serie).
  3. El usuario escribe el nombre.
  4. El sistema normaliza el nombre y comprueba que no existe otro perfil con el mismo nombre y tipo. Habilita "Guardar perfil".
  5. El usuario pulsa "Guardar perfil".
  6. El sistema guarda el perfil con los datos introducidos y vuelve atrás con el aviso "Perfil guardado".
- **Flujos alternativos**
  - **A1 Elegir estado**: antes del paso 5 el usuario selecciona Pendiente, Viendo o Visto. Si elige Visto, se guarda sin estado. No se pide progreso en ningún caso (RF-12).
  - **A2 Puntuar**: el usuario introduce una puntuación con el campo, los botones − / + o el slider. Pasa por CU-03 (validación). Es válida en cualquier estado (RF-03).
  - **A3 Géneros**: el usuario marca uno o varios géneros de la lista predefinida.
  - **A4 Idiomas**: el usuario marca uno o varios idiomas de la lista predefinida.
  - **A5 Saga**: el usuario elige o crea una saga (CU-10). Máximo una.
  - **A6 Comentario**: el usuario escribe un comentario libre.
  - **A7 Cancelar**: el usuario pulsa "Cancelar" o atrás. Si hay datos introducidos, el sistema pide confirmar el descarte. No se guarda nada.
  - **A8 Añadir a lista desde el alta** *(opcional)*: no es obligatorio; el alta se completa en 1 pantalla y después CU-15.
- **Flujos de excepción**
  - **E1 Nombre vacío o solo espacios**: "Guardar perfil" permanece deshabilitado; al intentar guardar, M-01 en el campo.
  - **E2 Duplicado**: ya existe (nombre normalizado, tipo). Se muestra M-02 en el campo y se impide guardar (S-02). Mismo nombre con distinto tipo es válido.
  - **E3 Puntuación inválida**: M-03; no se guarda hasta corregirla o vaciarla.
  - **E4 Fallo de escritura**: M-25; no se crea ningún dato parcial (transacción atómica).
  - **E5 Cambiar el tipo tras escribir el nombre**: se reevalúa el duplicado con el nuevo tipo.
  - **E6 Doble pulsación en "Guardar"**: se crea un solo perfil.
- **Postcondiciones**: existe el perfil con sus datos; aparece en la biblioteca; persiste tras reiniciar.
- **Cubre**: RF-01..RF-12, RNF-04, RNF-05, RNF-07, RNF-09, RES-05, RES-06.

### CU-02 Editar datos del perfil
- **Objetivo**: modificar nombre, géneros, idiomas y saga de un perfil existente.
- **Precondiciones**: el perfil existe.
- **Disparador**: menú "Editar" en el detalle del perfil.
- **Flujo principal**
  1. El sistema abre el formulario "Editar Medio" con los datos actuales.
  2. El usuario cambia nombre, géneros, idiomas y/o saga. El mismo formulario permite cambiar también estado, puntuación y comentario (con las reglas de CU-05, CU-03 y CU-04) y crear una saga (CU-10).
  3. El usuario pulsa "Guardar".
  4. El sistema valida y guarda; vuelve al detalle con los cambios visibles.
- **Flujos alternativos**
  - **A1 Quitar la saga**: elige "Ninguna / Obra individual"; el perfil queda sin saga.
  - **A2 Quitar todos los géneros o idiomas**: válido; el perfil queda sin ellos.
  - **A3 Sin cambios**: guardar no modifica nada (no se escribe en la base de datos).
  - **A4 Cancelar**: se descartan los cambios (con confirmación "¿Descartar los cambios?" si los hay; sin cambios vuelve directamente).
  - **A5 Cambiar el tipo**: no se permite cambiar película↔serie (S-03).
- **Flujos de excepción**
  - **E1 Nombre vacío**: M-01.
  - **E2 Nuevo nombre ya usado por otro perfil del mismo tipo**: M-02. Cambiar solo mayúsculas/tildes del propio nombre es válido.
  - **E3 Fallo de escritura**: M-25; el perfil queda como estaba.
- **Postcondiciones**: el perfil refleja los cambios; sus listas siguen conteniéndolo con la misma posición/prioridad; la búsqueda por nombre usa el nombre nuevo.
- **Cubre**: RF-16, RF-07..RF-10, RF-05, RF-17.

### CU-03 Puntuar / modificar puntuación
- **Objetivo**: añadir, cambiar o quitar la puntuación de un perfil.
- **Precondiciones**: el perfil existe (o se está creando).
- **Disparador**: botón "Ajustar" en el detalle (abre el editor con campo, slider, "Quitar puntuación" y "Guardar"), o campo de puntuación del formulario.
- **Flujo principal**
  1. El usuario abre el editor de puntuación (valor actual o vacío).
  2. El usuario introduce un valor (teclado numérico, − / +, o slider).
  3. El sistema valida en línea.
  4. El usuario confirma; el sistema guarda y actualiza el detalle y los listados.
- **Reglas de validación** (S-04): se aceptan `""` (sin puntuación), enteros 0..10 y decimales con un solo decimal entre 0.0 y 9.9, con `.` o `,` como separador. `10` y `10.0` equivalen a 10. Se rechazan negativos, mayores que 10, más de un decimal, texto, notación científica y espacios internos. El sistema guarda la puntuación en décimas (entero 0..100) para evitar errores de coma flotante.
- **Flujos alternativos**
  - **A1 Cambiar**: reemplaza el valor anterior.
  - **A2 Quitar**: el usuario pulsa "Quitar puntuación" o vacía el campo; el perfil vuelve a "Sin puntuación".
  - **A3 − / +**: cada pulsación cambia 0.1; en 0.0 el "−" se deshabilita y en 10 el "+" se deshabilita. Desde vacío parten de 5.0: "+" pone 5.1 y "−" pone 4.9 (S-05).
  - **A4 Series**: la puntuación es única para toda la serie (no hay temporadas ni episodios).
  - **A5 Cualquier estado**: se puede puntuar un perfil Pendiente, Viendo o Visto.
- **Flujos de excepción**
  - **E1 Valor inválido**: M-03; el valor anterior no se modifica.
  - **E2 Fallo de escritura**: M-25.
- **Postcondiciones**: puntuación guardada con ≤ 1 decimal o ausente; el perfil cambia de grupo de puntuación (CU-09) y de resultado de búsqueda (CU-20).
- **Cubre**: RF-06, RF-11, RF-13, RF-03, RNF-04, RNF-05, RNF-09.

### CU-04 Editar comentario
- **Objetivo**: añadir, cambiar o borrar la crítica de un perfil.
- **Precondiciones**: el perfil existe.
- **Disparador**: "Mi Crítica y Notas" → editar.
- **Flujo principal**: 1) el usuario abre el editor; 2) escribe o modifica el texto; 3) guarda; 4) el sistema muestra el comentario en el perfil.
- **Alternativos**
  - **A1 Borrar**: pulsa borrar (con confirmación breve) o vacía el texto y guarda; el perfil queda sin comentario.
  - **A2 Cancelar**: se descarta lo escrito.
  - **A3 Texto con saltos de línea, emojis o muy largo**: se guarda íntegro (límite técnico razonable: 10 000 caracteres, S-06).
- **Excepciones**: **E1** fallo de escritura → M-25. **E2** supera el límite → no se permite escribir más.
- **Postcondiciones**: comentario guardado o ausente; el estado y la puntuación no cambian.
- **Cubre**: RF-14, RF-03.

### CU-05 Cambiar estado
- **Objetivo**: marcar un perfil como Pendiente, Viendo o Visto.
- **Precondiciones**: el perfil existe.
- **Disparador**: selector segmentado en el detalle.
- **Flujo principal**: 1) el usuario toca el estado deseado; 2) el sistema lo guarda y lo muestra resaltado y en los listados.
- **Reglas**: desde cualquier estado se puede ir a Pendiente o Viendo (RF-17). "Visto" equivale a ningún estado. Cambiar el estado no exige ni modifica la puntuación. "Viendo" no guarda progreso.
- **Alternativos**
  - **A1 Tocar el estado ya activo**: no cambia nada.
  - **A2 Pendiente → Visto sin puntuación**: válido.
- **Excepciones**: **E1** fallo de escritura → M-25 y el selector vuelve al estado anterior (el selector solo muestra el estado guardado).
- **Postcondiciones**: estado persistente; el perfil cambia de grupo en CU-09 y se refleja en CU-08.
- **Cubre**: RF-15, RF-17, RF-22, RF-12, RF-03.

### CU-06 Eliminar perfil
- **Objetivo**: borrar completamente un perfil.
- **Precondiciones**: el perfil existe.
- **Disparador**: "Eliminar de mi catálogo" en el detalle o en el menú ⋮.
- **Flujo principal**
  1. El sistema muestra la confirmación M-06.
  2. El usuario confirma.
  3. El sistema elimina el perfil, sus relaciones con géneros/idiomas, su comentario y todos sus elementos de lista (normales, de prioridad y de ranking), en una sola transacción.
  4. En cada ranking afectado, las posiciones posteriores suben un puesto y quedan contiguas (S-07).
  5. El sistema vuelve a la pantalla anterior con aviso "Perfil eliminado".
- **Alternativos**
  - **A1 Cancelar**: no cambia nada.
  - **A2 Perfil sin listas**: solo se elimina el perfil.
  - **A3 Sagas**: si la saga queda sin perfiles, se conserva registrada (S-08).
  - **A4 Perfil que era el único de una lista**: la lista queda vacía y se conserva; no se lanza el aviso de CU-16 (S-09).
- **Excepciones**: **E1** fallo de escritura → M-25 y no se borra nada (todo o nada).
- **Postcondiciones**: no queda rastro del perfil en ninguna tabla; no aparece en búsquedas, grupos ni listas; los demás perfiles no cambian.
- **Cubre**: RF-18, RF-19, RF-20, RES-07.

### CU-07 Ver perfil
- **Objetivo**: consultar todos los datos de un perfil.
- **Disparador**: el usuario pulsa un perfil en la biblioteca, una lista o una búsqueda.
- **Flujo principal**: el sistema muestra tipo, nombre, estado, puntuación (o "Sin puntuar"), saga, géneros, idiomas, listas a las que pertenece (con posición o prioridad en cada una) y comentario.
- **Alternativos**
  - **A1 Campos opcionales vacíos**: la sección correspondiente muestra "—" o un texto vacío, sin romper el diseño.
  - **A2 Pertenece a varias listas**: se listan todas.
  - **A3 Pertenece a una saga**: se muestra el nombre y cuántos perfiles tiene; al tocarlo se abre la Biblioteca filtrada por esa saga.
  - **A4 Desde el detalle**: acceso a CU-02, CU-03, CU-04, CU-05, CU-06 y CU-15.
- **Excepciones**: **E1** el perfil ya no existe (borrado desde otra pantalla): se vuelve atrás y se muestra un aviso.
- **Rendimiento**: abre en < 1 s con 1000 perfiles.
- **Cubre**: RF-21, RF-22, RNF-08.

### CU-08 Ver biblioteca
- **Objetivo**: ver el listado de todos los perfiles con su estado.
- **Disparador**: destino "Biblioteca".
- **Flujo principal**: el sistema lista los perfiles (más recientes primero, S-10) con póster/marcador, tipo, nombre, nota, saga, géneros, idioma y estado. Muestra los contadores Viendo/Pendientes/Vistas. Se ven como máximo 10 a la vez; el resto con scroll.
- **Alternativos**
  - **A1 Filtro rápido** por tipo: Todas / Solo películas / Solo series.
  - **A2 Filtro por escala** de puntuación (los cinco rangos y "Sin puntuación").
  - **A3 Biblioteca vacía**: M-22 y botón "Añadir".
  - **A4 Pulsar un perfil**: CU-07.
- **Excepciones**: **E1** error de lectura → M-25 con "Reintentar".
- **Cubre**: RF-22, RNF-01, RNF-08, RNF-11.

### CU-09 Agrupar perfiles
- **Objetivo**: ver los perfiles agrupados por un criterio.
- **Disparador**: selector "Agrupar por" en la Biblioteca.
- **Flujo principal**
  1. El usuario elige un ámbito: Todo, Solo películas, Solo series.
  2. El usuario elige el criterio: Puntuación, Saga, Idioma, Género o Estado.
  3. El sistema muestra los grupos con su nombre y recuento, y los perfiles de cada uno.
- **Reglas**
  - Puntuación: cinco grupos fijos `0–2.9`, `3–4.9`, `5–6.9`, `7–8.9`, `9–10` y un grupo adicional "Sin puntuación". Una nota de 2.9 va en el primero, 3.0 en el segundo, 9.0 y 10 en el último.
  - Género e idioma: un perfil con varios aparece en **cada** grupo (RF-26).
  - Perfiles sin género, idioma o saga: grupo "Sin género", "Sin idioma", "Sin saga" (S-11).
  - Estado: Pendiente, Viendo, Visto.
  - Grupos vacíos no se muestran, salvo que se pida lo contrario (S-12).
- **Alternativos**: **A1** grupo colapsable; **A2** cambiar ámbito o criterio mantiene el otro.
- **Excepciones**: **E1** biblioteca vacía → M-22.
- **Cubre**: RF-23, RF-24, RF-25, RF-26, RNF-01.

### CU-10 Elegir o crear saga
- **Objetivo**: asignar una saga (existente o nueva) a un perfil. Incluido en CU-01 y CU-02.
- **Flujo principal**: 1) el usuario abre el selector de saga; 2) el sistema lista las sagas registradas con "Ninguna / Obra individual" arriba; 3) el usuario elige una; 4) el selector se cierra con esa saga.
- **Alternativos**
  - **A1 Crear nueva**: el usuario pulsa "+ Crear nueva saga", escribe el nombre y confirma. La saga se registra y queda seleccionada. A partir de ahí aparece en el selector sin reescribirla (RF-10).
  - **A2 Sin sagas registradas**: el selector solo ofrece "Ninguna" y "Crear nueva saga".
  - **A3 Cambiar de saga**: sustituye a la anterior; el perfil nunca tiene dos.
- **Excepciones**
  - **E1 Nombre de saga vacío**: M-05.
  - **E2 Saga duplicada** (mismo nombre normalizado): M-04; el sistema ofrece seleccionar la existente (S-13).
  - **E3 Cancelar la creación de la saga**: no se registra nada.
  - **E4 Se guarda el perfil**: si el usuario cancela el formulario del perfil tras crear la saga, la saga **no** queda registrada (se registra al guardar el perfil).
- **Cubre**: RF-09, RF-10.

### CU-11 Crear lista
- **Objetivo**: crear una lista normal, de prioridad o de ranking.
- **Disparador**: "+ Nueva Lista".
- **Flujo principal**
  1. El sistema muestra el diálogo "Nueva Lista" (tipo Normal, restricción Mixto).
  2. El usuario escribe el nombre.
  3. El usuario elige el tipo (Normal, Prioridad o Ranking).
  4. El usuario opcionalmente elige la restricción (Mixto, Películas, Series).
  5. El usuario pulsa "Guardar Lista".
  6. El sistema crea la lista vacía, aparece en "Mis Listas" con el aviso "Lista creada con éxito".
- **Reglas**: una lista mixta admite películas y series; la restricción se fija al crear (S-14). El ranking no tiene tamaño máximo.
- **Alternativos**: **A1** cancelar (no se crea nada); **A2** crear varias listas seguidas.
- **Excepciones**
  - **E1 Nombre vacío**: M-07.
  - **E2 Nombre duplicado** (normalizado): M-08 (S-15).
  - **E3 Fallo de escritura**: M-25.
- **Postcondiciones**: lista con nombre, tipo, restricción y 0 elementos.
- **Cubre**: RF-27, RF-28, RF-29, RF-30, RF-32, RF-41.

### CU-12 Ver todas las listas
- **Disparador**: destino "Listas".
- **Flujo principal**: el sistema muestra cada lista con nombre, tipo (`[NORMAL]`, `[PRIORIDAD]`, `[RANKING]`) y número de elementos; si hay restricción, la etiqueta "Restringida: Solo Películas/Series".
- **Alternativos**: **A1** sin listas → estado vacío con "+ Nueva Lista"; **A2** pulsar una lista → CU-13; **A3** menú ⋮ → CU-14 / CU-19.
- **Excepciones**: **E1** error de lectura → M-25.
- **Cubre**: RF-43, RNF-01.

### CU-13 Ver contenido de una lista
- **Disparador**: el usuario pulsa una lista.
- **Flujo principal**: el sistema muestra los elementos con el formato de su tipo: normal (sin orden especial), prioridad (agrupados/ordenados Alta → Media → Baja), ranking (por posición 1..n). Se ven 10 a la vez como máximo; el resto con scroll.
- **Alternativos**
  - **A1 Lista vacía**: M-23 y "Añadir título".
  - **A2 Más de 10**: scroll hasta ver todos, sin paginar ni perder la posición.
  - **A3 Pulsar un elemento**: CU-07.
- **Excepciones**: **E1** la lista ya no existe → volver atrás con aviso.
- **Rendimiento**: < 1 s con 1000 perfiles.
- **Cubre**: RF-42, RNF-01, RNF-02, RNF-03, RNF-08, RES-09.

### CU-14 Renombrar lista
- **Flujo principal**: 1) el usuario elige "Renombrar" en la lista; 2) edita el nombre; 3) guarda; 4) el sistema muestra el nombre nuevo.
- **Alternativos**: **A1** cancelar; **A2** mismo nombre → sin cambios.
- **Excepciones**: **E1** nombre vacío M-07; **E2** duplicado M-08; **E3** fallo → M-25.
- **Postcondiciones**: cambia solo el nombre; tipo, restricción y elementos no cambian.
- **Cubre**: RF-33.

### CU-15 Añadir perfil a una lista
- **Objetivo**: incluir un perfil existente en una lista. Cumple la regla de ≤ 3 pasos.
- **Disparador**: "Añadir título" dentro de una lista, o "+ Añadir a lista" desde el perfil.
- **Flujo principal (desde la lista)**
  1. El usuario pulsa "Añadir título"; el sistema abre el selector con búsqueda en la biblioteca local.
  2. El usuario busca y toca un perfil.
  3. El sistema lo añade y muestra "«X» añadido a la lista".
  - **Lista normal**: sin datos extra.
  - **Lista de ranking**: se añade al final (posición n+1) salvo que el usuario elija otra posición.
  - **Lista de prioridad**: se añade con la prioridad elegida; si no elige, **Media** (S-16).
- **Flujo principal (desde el perfil)**: 1) "+ Añadir a lista"; 2) el usuario elige la lista (solo se ofrecen las compatibles); 3) en ranking/prioridad indica posición/nivel o acepta el valor por defecto.
- **Alternativos**
  - **A1 Elegir posición** en un ranking: el valor debe estar entre 1 y n+1; los elementos desde esa posición bajan un puesto.
  - **A2 Elegir prioridad** Alta/Media/Baja.
  - **A3 Añadir varios seguidos**: el selector permite añadir varios sin cerrarse.
  - **A4 Perfil en varias listas**: permitido.
  - **A5 Búsqueda en el selector**: insensible a mayúsculas y tildes; si no hay resultados, M-13.
- **Excepciones**
  - **E1 Perfil ya en la lista**: M-10; no se duplica (RES-08).
  - **E2 Tipo no permitido** por la restricción: la lista no se ofrece o el perfil aparece deshabilitado; si llega a intentarse, M-09.
  - **E3 Posición no válida** (0, > n+1, no numérica): M-24; no se añade.
  - **E4 Fallo de escritura**: M-25.
- **Postcondiciones**: el elemento pertenece a la lista; ni la posición ni la prioridad se guardan en el perfil; las posiciones del ranking son contiguas 1..n.
- **Cubre**: RF-31, RF-34, RF-39, RF-40, RF-41, RF-32, RF-30, RNF-02, RNF-03, RNF-09, RES-07, RES-08.

### CU-16 Quitar perfil de una lista
- **Objetivo**: retirar un elemento sin borrar el perfil.
- **Disparador**: botón de quitar en la fila.
- **Flujo principal**
  1. El sistema pide confirmar la retirada.
  2. El usuario confirma.
  3. El sistema quita el elemento. En ranking, los posteriores suben un puesto.
  4. El sistema muestra "Elemento quitado de la lista".
- **Alternativos**
  - **A1 Quitar el último elemento**: tras quitarlo, el sistema muestra **una sola vez** M-11 ("¿Quieres eliminar también la lista?").
    - **A1a Acepta**: se ejecuta CU-19 sin segunda confirmación.
    - **A1b Rechaza**: la lista queda vacía (M-23) y puede borrarse manualmente más tarde con CU-19. El aviso no se repite mientras la lista siga vacía (S-17).
  - **A2 Cancelar**: no cambia nada.
  - **A3 Quitar de prioridad**: el resto conserva su prioridad.
- **Excepciones**: **E1** el elemento ya no existe → se refresca la lista; **E2** fallo → M-25 y el elemento sigue.
- **Postcondiciones**: el perfil sigue existiendo en la biblioteca y en sus otras listas.
- **Cubre**: RF-34, RF-35, RNF-02, RNF-03.

### CU-17 Cambiar prioridad de un elemento
- **Precondiciones**: lista de prioridad con ≥ 1 elemento.
- **Flujo principal**: 1) el usuario toca el chip de prioridad de un elemento; 2) elige Alta, Media o Baja; 3) el sistema guarda y reordena la lista (Alta → Media → Baja; dentro de un nivel, por antigüedad de alta en la lista).
- **Alternativos**: **A1** elegir el mismo nivel: sin cambios; **A2** la prioridad es exclusiva de esta lista: el mismo perfil puede tener otra en otra lista.
- **Excepciones**: **E1** fallo → M-25, la prioridad no cambia.
- **Cubre**: RF-36, RF-40, RNF-03, RES-07.

### CU-18 Reordenar elemento de un ranking
- **Precondiciones**: lista de ranking con ≥ 2 elementos.
- **Flujo principal**: 1) el usuario arrastra una fila a otra posición, o usa ▲/▼; 2) el sistema recalcula las posiciones; 3) la lista muestra #1…#n sin huecos ni repetidos.
- **Alternativos**
  - **A1 ▲ en el #1 / ▼ en el último**: deshabilitados, sin efecto.
  - **A2 Arrastre cancelado** (soltar donde estaba): sin cambios.
  - **A3 Más de 10 elementos**: el arrastre funciona con scroll automático.
- **Excepciones**: **E1** fallo → M-25 y se restaura el orden anterior.
- **Reglas**: la posición pertenece a la lista; reordenar no modifica el perfil ni otras listas.
- **Cubre**: RF-37, RF-41, RNF-02, RES-07.

### CU-19 Eliminar lista
- **Disparador**: menú de la lista, "Eliminar esta lista", o aceptación de M-11.
- **Flujo principal**: 1) el sistema pide confirmación (M-12); 2) el usuario confirma; 3) el sistema borra la lista y sus elementos; 4) vuelve a "Mis Listas" con el aviso "Lista eliminada".
- **Alternativos**: **A1** cancelar; **A2** lista vacía (tras A1b de CU-16): igual flujo.
- **Excepciones**: **E1** fallo → M-25, la lista permanece.
- **Postcondiciones**: los perfiles contenidos **no** se borran y siguen en la biblioteca y en sus demás listas.
- **Cubre**: RF-38, RF-35.

### CU-20 Buscar con filtros
- **Disparador**: destino "Buscar".
- **Flujo principal**
  1. El sistema muestra el formulario con tipo = Todas y sin otros filtros.
  2. El usuario define uno o varios criterios: nombre, puntuación mínima y/o máxima, género(s), idioma, saga, tipo (película/serie/ambas) y, como extensión del prototipo, estado (S-18).
  3. El usuario pulsa "Aplicar filtros" (o los resultados se actualizan al cambiar).
  4. El sistema muestra los perfiles que cumplen **todos** los criterios activos y el recuento.
- **Reglas**
  - Nombre: contiene el texto, sin distinguir mayúsculas ni acentos (`"el padrino"` = `"El Pádrino"`).
  - Rango de puntuación: inclusivo; si solo hay mínimo o solo máximo, el otro extremo es 0.0 / 10. Con rango activo, los perfiles sin puntuación quedan fuera.
  - Varios géneros marcados: basta uno (OR); entre criterios distintos: todos (AND) (S-19).
  - Tipo por defecto: ambos.
- **Alternativos**
  - **A1 Limpiar**: restablece todos los criterios.
  - **A2 Sin criterios**: devuelve todos los perfiles.
  - **A3 Resultado**: pulsar un perfil abre CU-07.
  - **A4 Más de 10 resultados**: scroll.
- **Excepciones**
  - **E1 Sin resultados**: M-13.
  - **E2 Mínimo > máximo**: M-14, no se ejecuta la búsqueda.
  - **E3 Valor de puntuación inválido**: M-03.
  - **E4 Texto con caracteres especiales** (`%`, `_`, `'`, `"`, `\`): se tratan como texto literal, sin errores ni inyección.
- **Rendimiento**: < 1 s con 1000 perfiles.
- **Cubre**: RF-44, RF-45, RF-46, RF-47, RF-48, RNF-01, RNF-08.

### CU-21 Exportar copia de seguridad
- **Disparador**: Ajustes → "Generar y guardar archivo local".
- **Flujo principal**
  1. El sistema propone el nombre `cinetrack_backup_AAAA-MM-DD.json`.
  2. El usuario elige la ubicación con el selector de archivos del sistema.
  3. El sistema escribe un JSON con versión de formato, fecha, perfiles (con géneros, idiomas, saga, nota, comentario, estado), sagas y listas con sus elementos (posición/prioridad).
  4. El sistema muestra M-15.
- **Alternativos**: **A1** cancelar el selector: no se escribe nada ni se muestra error; **A2** sin datos: se exporta un archivo válido vacío.
- **Excepciones**: **E1** sin espacio o ubicación no escribible: M-16; no queda un archivo corrupto a medias; **E2** la app no tiene que pedir permisos de almacenamiento ni de red.
- **Postcondiciones**: los datos de la app no cambian.
- **Cubre**: RF-49, RNF-06, RES-03, RES-04.

### CU-22 Importar copia de seguridad
- **Disparador**: Ajustes → "Seleccionar archivo .JSON".
- **Flujo principal**
  1. El usuario elige un archivo con el selector del sistema.
  2. El sistema lo lee y **valida completamente** antes de tocar la base de datos (formato, versión, referencias, puntuaciones, duplicados internos, géneros e idiomas conocidos, restricciones de lista).
  3. El sistema calcula en seco el resultado de la fusión (S-20) y muestra M-19 con los recuentos.
  4. El usuario confirma.
  5. El sistema aplica la fusión en una sola transacción y muestra M-20.
- **Reglas de fusión (S-20)**: la importación **añade** y nunca borra, sobrescribe ni modifica datos existentes.
  - **Perfiles**: se identifican por (nombre normalizado, tipo). Si no existe, se crea con todos sus datos. Si ya existe, **se omite por completo**: prevalecen los datos locales (nota, estado, comentario, géneros, idiomas y saga no cambian) y se cuenta como "omitido".
  - **Sagas**: se identifican por nombre normalizado; se reutiliza la existente o se crea.
  - **Listas**: se identifican por nombre normalizado.
    - Misma lista (mismo nombre, tipo y restricción): se **amplía** con los perfiles de la copia que aún no estén en ella (ya sean perfiles nuevos o existentes). Ranking: se añaden al final en el orden de la copia. Prioridad: conservan el nivel de la copia. Los elementos ya presentes no cambian de posición ni de nivel.
    - Mismo nombre pero distinto tipo o restricción: se crea una lista nueva llamada "{nombre} (importada)", o "(importada 2)", etc. si ese nombre también existe.
    - Sin coincidencia: se crea la lista con sus elementos, posiciones y niveles.
  - **Idempotencia**: importar dos veces el mismo archivo no cambia nada la segunda vez.
- **Alternativos**
  - **A1 Cancelar en el selector o en la confirmación**: no cambia nada.
  - **A2 Archivo de una versión anterior del formato**: se migra al leerlo.
  - **A3 Archivo vacío válido o sin nada nuevo**: no cambia ningún dato; se informa de ello (M-20 con ceros).
  - **A4 Base de datos local vacía**: todo se crea; el resultado es idéntico al archivo.
- **Excepciones**
  - **E1 No es JSON o no es de CineTrack**: M-17.
  - **E2 Versión más reciente**: M-18.
  - **E3 Contenido inconsistente** (puntuación inválida, perfil duplicado dentro del archivo, elemento de lista que apunta a un perfil inexistente, dos veces el mismo perfil en una lista, tipo que viola la restricción de la lista, género o idioma que no está en la lista predefinida): M-17 con detalle; no se importa nada.
  - **E4 Fallo durante la escritura** (cierre de la app, falta de espacio): M-21 y los datos previos se conservan íntegros (transacción).
  - **E5 Archivo enorme**: se procesa sin bloquear la interfaz (indicador de progreso).
- **Postcondiciones**: todos los datos que existían antes siguen idénticos; se han añadido los perfiles, sagas y elementos de lista nuevos. Con la base de datos local vacía, tras importar un export previo los datos son idénticos a los exportados (ida y vuelta sin pérdidas).
- **Cubre**: RF-49, RNF-06, RNF-07, RES-03.

### CU-23 Arrancar la aplicación
- **Flujo principal**: 1) el sistema abre la base de datos local; 2) en la primera ejecución, crea el esquema y precarga la lista predefinida de géneros e idiomas; 3) muestra la pantalla inicial (Biblioteca).
- **Alternativos**: **A1** arranques posteriores: los datos del usuario están intactos tras cerrar la app o reiniciar el dispositivo; **A2** la app se reanuda tras estar en segundo plano sin perder el formulario en curso (S-21).
- **Excepciones**: **E1** base de datos corrupta o migración fallida: pantalla de error con opción de importar una copia (CU-22); no se borra nada automáticamente.
- **Cubre**: RNF-06, RNF-07, RNF-10, RNF-12, RES-01, RES-03, RES-04, RES-10, RES-11.

---

## 4. Matriz de trazabilidad (requisito → casos de uso)

| Requisito | Casos de uso |
|---|---|
| RF-01, RF-02, RF-04 | CU-01 |
| RF-03 | CU-01, CU-03, CU-05 |
| RF-05 | CU-01, CU-02 |
| RF-06 | CU-01, CU-03 |
| RF-07, RF-08 | CU-01, CU-02 |
| RF-09, RF-10 | CU-10, CU-01, CU-02 |
| RF-11 | CU-01, CU-03 |
| RF-12 | CU-01, CU-05 |
| RF-13 | CU-03 |
| RF-14 | CU-04 |
| RF-15, RF-17 | CU-05 |
| RF-16 | CU-02 |
| RF-18, RF-19, RF-20 | CU-06 |
| RF-21 | CU-07 |
| RF-22 | CU-05, CU-07, CU-08 |
| RF-23, RF-24, RF-25, RF-26 | CU-09 |
| RF-27, RF-28, RF-29, RF-30 | CU-11 |
| RF-31 | CU-15 |
| RF-32 | CU-11, CU-15 |
| RF-33 | CU-14 |
| RF-34 | CU-15, CU-16 |
| RF-35 | CU-16, CU-19 |
| RF-36 | CU-17 |
| RF-37 | CU-18 |
| RF-38 | CU-19 |
| RF-39, RF-40 | CU-15, CU-17 |
| RF-41 | CU-11, CU-15, CU-18 |
| RF-42 | CU-13 |
| RF-43 | CU-12 |
| RF-44..RF-48 | CU-20 |
| RF-49 | CU-21, CU-22 |
| RNF-01, RES-09 | CU-08, CU-09, CU-12, CU-13, CU-20 |
| RNF-02, RNF-03, RES-07 | CU-13, CU-15, CU-16, CU-17, CU-18 |
| RNF-04, RNF-05 | CU-01, CU-03 |
| RNF-06, RES-03 | CU-21, CU-22, CU-23 (y toda la app) |
| RNF-07 | CU-01, CU-22, CU-23 |
| RNF-08 | CU-07, CU-13, CU-20 |
| RNF-09 | CU-01, CU-03, CU-15 |
| RNF-10, RES-01, RES-02 | CU-23 |
| RNF-11 | todas las pantallas |
| RNF-12, RES-10 | todas las pantallas |
| RES-04 | CU-21, CU-22, CU-23 |
| RES-05 | CU-01 |
| RES-06 | CU-01, CU-02 |
| RES-08 | CU-15, CU-22 |
| RES-11 | CU-23 |

---

## 5. Supuestos a confirmar

Los requisitos no cierran estos puntos. Se decidió lo siguiente; las pruebas dependen de ello.

| ID | Tema | Decisión propuesta |
|---|---|---|
| S-01 | Estado "visto" | Se guarda como ausencia de estado; en la interfaz es la tercera opción "Visto". |
| S-02 | Duplicado | Se **impide** (error), no solo se avisa. La comparación usa nombre normalizado + tipo. |
| S-03 | Cambiar tipo de un perfil existente | No permitido (los requisitos no lo piden; el usuario borra y vuelve a crear). |
| S-04 | Formato de puntuación | Acepta `.` o `,`; enteros válidos (`7`); `10` y `10.0` válidos y equivalentes; se guarda en décimas. |
| S-05 | Botones − / + con puntuación vacía | Parten de 5.0, como en el prototipo: "+" pone 5.1 y "−" pone 4.9. |
| S-06 | Longitud del comentario | Máximo 10 000 caracteres. |
| S-07 | Borrar perfil o quitar de ranking | Las posiciones posteriores suben y se mantienen contiguas. |
| S-08 | Saga sin perfiles | Sigue registrada y se puede reutilizar. |
| S-09 | Borrar un perfil que deja una lista vacía | No lanza el aviso de "borrar lista"; ese aviso es solo al quitar manualmente el último elemento. |
| S-10 | Orden por defecto de la biblioteca | Más recientemente añadidos primero. |
| S-11 | Perfiles sin género, idioma o saga al agrupar | Grupo adicional "Sin género / Sin idioma / Sin saga". |
| S-12 | Grupos vacíos | No se muestran. |
| S-13 | Saga duplicada al crear | Error M-04 con opción de seleccionar la existente. |
| S-14 | Cambiar la restricción de una lista existente | No permitido (solo al crear). |
| S-15 | Nombres de lista duplicados | No permitidos (normalizados). |
| S-16 | Prioridad por defecto al añadir | Media. |
| S-17 | "Aviso único" al vaciar una lista | Se muestra una vez cada vez que la lista pasa a vacía; si se rechaza, no se repite hasta que la lista vuelva a tener elementos y a vaciarse. |
| S-18 | Filtro por estado en Buscar | Se incluye (aparece en el prototipo) como extensión, no como requisito. |
| S-19 | Varios géneros en el filtro | OR dentro del criterio, AND entre criterios. |
| S-20 | Importar | **Fusiona** (confirmado por el usuario). Solo añade: los perfiles que ya existen (nombre normalizado + tipo) se omiten y prevalecen los datos locales; las listas del mismo nombre, tipo y restricción se amplían; las de mismo nombre y distinto tipo o restricción se importan como "(importada)". Detalle en CU-22. |
| S-21 | Rotación/segundo plano | El formulario en curso conserva sus datos. |
