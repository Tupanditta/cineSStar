# SPRINTS — CineTrack (CineSStar)

> Documentos de referencia: `casosUso.md` (casos de uso `CU-xx`, flujos `A`/`E`, supuestos `S-xx`, mensajes `M-xx`, requisitos `RF/RNF/RES`), `design.md` (interfaz y stack) y `restricciones.md`.
> Proyecto individual: los sprints se dimensionan por tamaño relativo (S, M, L), no por fechas.

---

## 1. Reglas comunes

### 1.1 Definición de "terminado" (DoD) de cada sprint
Un sprint se da por cerrado solo si se cumple **todo** lo siguiente:
1. Todos los tests del sprint (sección de cada sprint) pasan.
2. **Regresión**: todos los tests de los sprints anteriores siguen pasando.
3. No hay avisos de lint (`ktlint`/`detekt`) ni errores de compilación; la app instala y arranca en un emulador.
4. Los textos de interfaz están en español en `strings.xml` (RNF-12) y no hay colores ni medidas fijas fuera de `ui/theme`.
5. Las pantallas del sprint se comparan con su `screen.png` del prototipo y las diferencias están en la lista de excepciones de `design.md` §12.
6. Los supuestos `S-xx` que afectan al sprint están confirmados por el usuario (o la decisión por defecto de `casosUso.md` queda aceptada).
7. Se actualiza `casosUso.md` si un flujo cambia.

### 1.2 Tipos de test
| Código | Tipo | Herramientas | Dónde corre |
|---|---|---|---|
| **U** | Unitario de dominio/ViewModel | JUnit 5, Turbine, MockK | JVM local |
| **D** | Base de datos y repositorios | Room in-memory, `androidx.test` | Emulador o Robolectric |
| **UI** | Interfaz Compose | `compose-ui-test` | Emulador |
| **G** | Visual (golden) | Paparazzi / Roborazzi | JVM local |
| **E** | Extremo a extremo (flujo completo) | `compose-ui-test` + DB real de prueba | Emulador |
| **R** | Rendimiento | `androidx.benchmark` / medición con datos sintéticos | Dispositivo/emulador |
| **M** | Manual en dispositivo | Lista de comprobación | Móvil real |

### 1.3 Convención de identificadores de test
`T{sprint}.{nn}`. Cada test cita qué cubre: `CU-xx Ayy` (flujo alternativo), `CU-xx Eyy` (excepción), `CU-xx FP` (flujo principal), o el requisito (`RF-xx`, `RNF-xx`, `RES-xx`). **Ningún flujo de `casosUso.md` puede quedar sin al menos un test**; la matriz de la sección 4 lo comprueba.

### 1.4 Datos de prueba
- **Fixture pequeño**: 12 perfiles (6 películas, 6 series) con combinaciones de estado, nota (incluidas 0.0, 2.9, 3.0, 4.9, 5.0, 6.9, 7.0, 8.9, 9.0, 10 y sin nota), géneros e idiomas múltiples, 2 sagas, nombres con tildes y mayúsculas mezcladas ("Pádrino", "EL padrino"), 3 listas (una de cada tipo).
- **Fixture grande**: generador determinista de 1000 perfiles, 30 listas, 5000 elementos de lista para pruebas de rendimiento.
- **Casos límite de puntuación** (tabla compartida por tests unitarios e instrumentados):

| Entrada | ¿Válida? | Valor guardado |
|---|---|---|
| `""` | sí | sin puntuación |
| `0`, `0.0`, `0,0` | sí | 0 |
| `7`, `7.0` | sí | 70 |
| `8.5`, `8,5` | sí | 85 |
| `9.9` | sí | 99 |
| `10`, `10.0` | sí | 100 |
| `-0.1`, `-1` | no | — |
| `10.1`, `11`, `100` | no | — |
| `7.25`, `8.55` | no | — |
| `abc`, `7a`, `1e1`, `7 .5`, `.5`, `5.` | no | — |

### 1.5 Pruebas transversales (se ejecutan en todos los sprints desde el S0)
- **TX.01 Sin red (RNF-06, RES-03)**: el `AndroidManifest` **no** declara `INTERNET`; ninguna dependencia hace peticiones de red. Se verifica con una comprobación de build.
- **TX.02 Español (RNF-12, RES-10)**: ninguna cadena visible fuera de `strings.xml`; no hay textos en inglés visibles en las pantallas.
- **TX.03 Sin permisos peligrosos**: el manifiesto no pide almacenamiento ni red; los archivos se eligen con el selector del sistema.
- **TX.04 Regresión**: la suite completa de los sprints anteriores.

---

## 2. Resumen de sprints

| Sprint | Tema | Tamaño | Casos de uso | Pantallas (`design.md`) |
|---|---|---|---|---|
| S0 | Cimientos: proyecto, tema y navegación | M | CU-23 (parcial) | §2, §3 |
| S1 | Dominio y base de datos | L | CU-23, reglas de CU-01/03 | — |
| S2 | Alta y ver perfil | L | CU-01, CU-10, CU-07 | §7, §8 |
| S3 | Modificar y borrar perfil | M | CU-02, CU-03, CU-04, CU-05, CU-06 | §7, §8 |
| S4 | Biblioteca y agrupar | M | CU-08, CU-09 | §4 |
| S5 | Listas (crear, ver, normales) | L | CU-11..CU-16, CU-19 | §5, §6 |
| S6 | Prioridad y ranking | M | CU-17, CU-18 (+ CU-15 ampliado) | §6 |
| S7 | Búsqueda | M | CU-20 | §9 |
| S8 | Copia de seguridad | M | CU-21, CU-22 | §10 |
| S9 | Rendimiento, adaptación y cierre | M | RNF-08, RNF-11, todos | todas |

Cada sprint entrega algo que arranca y se puede usar. El S1 no tiene interfaz; se valida solo con tests.

---

## 3. Sprints

### S0 — Cimientos
**Objetivo**: proyecto Android que compila, con el sistema de diseño aplicado y la navegación entre los 5 destinos vacíos.

**Alcance**
- Proyecto Kotlin + Compose + Material 3, Gradle KTS con catálogo de versiones, `minSdk` decidido (RES-02; propuesto 26).
- Tema oscuro con los tokens de `design.md` §2: colores, tipografía Inter (empaquetada), formas, espaciado.
- Fuente de iconos Material Symbols empaquetada (sin CDN).
- Barra superior y barra inferior con los 5 destinos y el botón central elevado "Añadir" (`design.md` §3).
- Navegación (Biblioteca, Listas, Añadir, Buscar, Ajustes) con pantallas vacías.
- Configuración de lint, formato y ejecución de tests.

**Tests de aceptación**
| ID | Tipo | Verifica | Resultado esperado |
|---|---|---|---|
| T0.01 | U | Tokens de color: cada token de `design.md` §2.1–§2.4 existe y coincide con su hex | Todos coinciden |
| T0.02 | U | Tipografía: los estilos de `design.md` §2.5 con tamaño, interlínea, peso y tracking; radios de §2.6 | Todos coinciden |
| T0.03 | UI | RES-01/RNF-10: la app arranca en el emulador de `minSdk` y en el de `targetSdk` | Se abre la pantalla Biblioteca sin cierre |
| T0.04 | UI | La barra inferior muestra exactamente "Biblioteca, Listas, Añadir, Buscar, Ajustes" | 5 destinos con esas etiquetas |
| T0.05 | UI | Tocar cada destino cambia de pantalla; el destino activo se resalta | Cada toque muestra su pantalla y resalta solo ese destino |
| T0.06 | UI | El botón "Añadir" abre la pantalla de alta (vacía en este sprint) | Navega a "Añadir Medio" |
| T0.07 | UI | Botón atrás desde una pantalla secundaria vuelve a la anterior; desde Biblioteca sale de la app | Comportamiento esperado |
| T0.08 | G | Capturas de barra superior e inferior comparadas con `screen.png` (tolerancia definida) | Dentro de tolerancia |
| T0.09 | U | Fuente Inter y Symbols cargan desde recursos locales | Sin errores con el dispositivo sin conexión |
| TX.01–TX.03 | — | Ver §1.5 | Pasan |

---

### S1 — Dominio y base de datos
**Objetivo**: reglas de negocio y persistencia correctas, sin interfaz.

**Alcance**
- Clase `Puntuacion` (S-04): parseo, validación, formato, almacenamiento en décimas.
- Normalizador de nombres (minúsculas, sin diacríticos, espacios).
- Entidades y DAO de Room: `Perfil`, `Saga`, `Genero`, `Idioma`, tablas puente, `Lista`, `ListaItem` (`design.md` §16), con claves únicas y `ON DELETE CASCADE`.
- Precarga de géneros e idiomas en la primera ejecución (CU-23).
- Repositorios con transacciones atómicas.
- Generador de fixtures (pequeño y grande).

**Tests de aceptación**
| ID | Tipo | Verifica | Resultado esperado |
|---|---|---|---|
| T1.01 | U | CU-03 / RF-06, RNF-04, RNF-05: toda la tabla de §1.4 | Cada entrada da el resultado y valor de la tabla |
| T1.02 | U | RNF-05: recorrer 0..100 décimas; cada `n/10` se acepta y se formatea con un decimal (el 100 como `10`) | 101 valores aceptados, ninguno con error de coma flotante |
| T1.03 | U | Normalizador: `"  EL  Pádrino "`, `"el padrino"`, `"Él Padrino"` | Misma forma normalizada |
| T1.04 | D | RES-06 / CU-01 E2: insertar dos perfiles con mismo nombre normalizado y tipo | Segundo falla con violación de unicidad |
| T1.05 | D | RES-06: mismo nombre y distinto tipo | Ambos se insertan |
| T1.06 | D | RF-03: perfil con solo nombre y tipo | Se guarda; el resto de campos nulos/vacíos |
| T1.07 | D | RF-03: perfil con estado Pendiente/Viendo/Visto, con y sin puntuación (6 combinaciones) | Todas se guardan |
| T1.08 | D | RF-11/RF-12: un perfil de tipo serie solo tiene una puntuación y ningún campo de progreso | No existe columna de progreso, temporada ni episodio |
| T1.09 | D | RF-07/RF-08/RF-26: varios géneros e idiomas por perfil | Se guardan y recuperan todos |
| T1.10 | D | RF-09: un perfil con una sola saga; reasignar la saga reemplaza la anterior | Máximo una |
| T1.11 | D | RF-10: saga registrada se reutiliza por nombre normalizado; duplicada falla | Se reutiliza / error de unicidad |
| T1.12 | D | CU-23 FP: primera ejecución precarga géneros e idiomas; segunda ejecución no duplica | Lista predefinida completa y sin duplicados |
| T1.13 | D | RES-08 / CU-15 E1: mismo perfil dos veces en la misma lista | Falla por clave primaria |
| T1.14 | D | RF-31: el mismo perfil en varias listas | Permitido |
| T1.15 | D | RF-20 / CU-06 FP: borrar un perfil elimina sus elementos de lista en todas las listas, sus géneros e idiomas | No quedan filas huérfanas |
| T1.16 | D | RF-38 / CU-19: borrar una lista no borra perfiles | Los perfiles permanecen |
| T1.17 | D | RNF-02, RNF-03, RES-07: la posición y la prioridad existen solo en `ListaItem` | `Perfil` no tiene esas columnas; un perfil en dos rankings tiene posiciones distintas |
| T1.18 | D | Atomicidad (CU-01 E4, CU-06 E1): simular fallo a mitad de una transacción | No queda ningún dato parcial |
| T1.19 | D | RNF-07: cerrar y reabrir la base de datos | Los datos persisten |
| T1.20 | U | Restricción de lista: añadir una serie a una lista `SOLO_PELICULAS` y viceversa | Rechazo con error de dominio (M-09) |
| T1.21 | D | Ranking: calcular posiciones tras insertar en medio, al final y tras eliminar | Siempre 1..n contiguo (S-07) |
| T1.22 | D | Prioridad: ordenación Alta → Media → Baja, desempate por antigüedad | Orden correcto |
| T1.23 | D | Cobertura de bordes de rangos de puntuación: 2.9, 3.0, 4.9, 5.0, 6.9, 7.0, 8.9, 9.0, 10 y sin nota → grupo correcto (RF-25) | Cada valor en su grupo |
| TX.01–TX.04 | — | Ver §1.5 | Pasan |

---

### S2 — Alta y ver perfil
**Objetivo**: crear películas y series con todos sus campos y consultarlas.

**Alcance**
- Pantalla "Añadir nuevo perfil" (`design.md` §8): tipo, nombre, estado, puntuación (campo, −/+, slider), saga, géneros, idiomas, comentario, botón deshabilitado hasta nombre válido.
- Selector y creación de saga (CU-10).
- Pantalla "Detalle del perfil" básica (`design.md` §7): hero, estado, nota, ficha, comentario (sin edición aún).
- Aviso de descarte al cancelar con datos.

**Tests de aceptación**
| ID | Tipo | Verifica | Resultado esperado |
|---|---|---|---|
| T2.01 | UI | CU-01 FP: escribir solo el nombre y guardar | Perfil creado como película, estado Pendiente, sin puntuación |
| T2.02 | UI | CU-01 FP paso 1: "Guardar perfil" deshabilitado con nombre vacío | Deshabilitado |
| T2.03 | UI | CU-01 E1: nombre solo con espacios | Sigue deshabilitado; M-01 al intentar guardar |
| T2.04 | UI | CU-01 FP: elegir Serie y guardar | Perfil de tipo serie |
| T2.05 | UI | CU-01 E2 / RF-05: crear dos veces "Dune" como película | Segundo intento muestra M-02 y no se guarda |
| T2.06 | UI | CU-01 E2 / RES-06: "Dune" película y "Dune" serie | Ambos se pueden crear |
| T2.07 | UI | CU-01 E2: duplicado detectado ignorando mayúsculas y tildes ("DÚNE") | M-02 |
| T2.08 | UI | CU-01 E5: escribir nombre existente como película, cambiar a serie | M-02 desaparece al cambiar de tipo |
| T2.09 | UI | CU-01 A1: guardar con cada estado (Pendiente, Viendo, Visto) | Estado correcto; Visto = sin estado; no se pide progreso |
| T2.10 | UI | CU-01 A2 / RF-03: guardar con puntuación válida en cada estado | Se guarda en los tres estados |
| T2.11 | UI | CU-01 E3 / RF-06: puntuación `11`, `-1`, `7.25`, `abc` | M-03 en cada caso; no se guarda |
| T2.12 | UI | CU-01 A2: vaciar la puntuación | Perfil sin puntuación guardado |
| T2.13 | UI | CU-03 A3: botones −/+ y límites; −/+ con el campo vacío | 0.1 por pulsación; "−" deshabilitado en 0.0; "+" deshabilitado en 10; desde vacío "+" da 5.1 y "−" da 4.9 (S-05) |
| T2.14 | UI | CU-03 A3: slider y campo se mantienen sincronizados | Ambos muestran el mismo valor |
| T2.15 | UI | CU-01 A3 / RF-07: marcar y desmarcar varios géneros | Se guardan los marcados; contador "N seleccionados" correcto |
| T2.16 | UI | CU-01 A4 / RF-08: varios idiomas | Igual |
| T2.17 | UI | CU-10 FP: elegir una saga existente | Perfil asociado; solo una |
| T2.18 | UI | CU-10 A1: crear saga nueva desde el formulario y reutilizarla en otro perfil | Aparece en el selector sin reescribirla |
| T2.19 | UI | CU-10 E1/E2: saga vacía y duplicada | M-05 y M-04 (con opción de elegir la existente) |
| T2.20 | UI | CU-10 E4: crear saga y cancelar el formulario | La saga no queda registrada |
| T2.21 | UI | CU-01 A6: comentario con saltos de línea y emojis | Se guarda íntegro |
| T2.22 | UI | CU-01 A7: cancelar con datos introducidos | Aviso de descarte; si confirma, no se guarda nada |
| T2.23 | UI | CU-01 E6: doble pulsación rápida en Guardar | Un solo perfil |
| T2.24 | UI | CU-01 E4: fallo de escritura simulado | M-25; ningún dato parcial |
| T2.25 | UI | CU-07 FP: abrir el detalle del perfil creado | Muestra tipo, nombre, estado, nota o "Sin puntuar", saga, géneros, idiomas y comentario |
| T2.26 | UI | CU-07 A1: perfil solo con nombre | Las secciones vacías no rompen el diseño |
| T2.27 | UI | RF-22: el estado se ve en el detalle | Insignia o selector con el estado correcto |
| T2.28 | UI | RNF-09: crear un perfil con nombre y puntuación | ≤ 3 pasos desde el botón "Añadir" hasta guardado |
| T2.29 | UI | CU-23 A2 / S-21: rotar la pantalla y pasar a segundo plano con el formulario a medias | Datos conservados |
| T2.30 | E | RNF-07: crear un perfil, matar la app y reabrir | El perfil sigue |
| T2.31 | G | Pantallas "Añadir perfil" (vacía, con datos, con error) y "Detalle" vs `screen.png` | Dentro de tolerancia |
| TX.01–TX.04 | — | Ver §1.5 | Pasan |

---

### S3 — Modificar y borrar perfil
**Objetivo**: editar cualquier dato de un perfil y eliminarlo con garantías.

**Alcance**: edición de datos (CU-02), puntuación (CU-03), comentario (CU-04), estado (CU-05) y eliminación (CU-06). La eliminación de listas se completa cuando existan las listas (S5); aquí se prueba con datos directos en la base de datos.

**Tests de aceptación**
| ID | Tipo | Verifica | Resultado esperado |
|---|---|---|---|
| T3.01 | UI | CU-02 FP: cambiar nombre, géneros, idiomas y saga | Cambios guardados y visibles en el detalle |
| T3.02 | UI | CU-02 A1: quitar la saga | Perfil sin saga |
| T3.03 | UI | CU-02 A2: quitar todos los géneros e idiomas | Perfil sin ellos |
| T3.04 | UI | CU-02 A3/A4: guardar sin cambios y cancelar con cambios | Sin modificaciones |
| T3.05 | UI | CU-02 A5 / S-03: el selector de tipo está deshabilitado | No se puede cambiar |
| T3.06 | UI | CU-02 E1: nombre vacío | M-01 |
| T3.07 | UI | CU-02 E2: renombrar al nombre de otro perfil del mismo tipo | M-02 |
| T3.08 | UI | CU-02 E2: cambiar solo mayúsculas/tildes del propio nombre | Permitido |
| T3.09 | UI | CU-03 FP/RF-13: añadir puntuación a un perfil sin ella | Guardada |
| T3.10 | UI | CU-03 A1: cambiar una existente | Sustituida |
| T3.11 | UI | CU-03 A2: quitar la puntuación con el botón y vaciando el campo | "Sin puntuar" |
| T3.12 | UI | CU-03 E1: valores inválidos de §1.4 | M-03 y el valor anterior no cambia |
| T3.13 | UI | CU-03 A5: puntuar en Pendiente, Viendo y Visto | Siempre permitido |
| T3.14 | UI | CU-03 A4: serie con una sola nota global | No hay controles por temporada/episodio |
| T3.15 | UI | CU-04 FP: añadir comentario | Visible en el detalle |
| T3.16 | UI | CU-04 FP: cambiarlo | Sustituido |
| T3.17 | UI | CU-04 A1: borrarlo por el botón de borrar y vaciando el texto | Sin comentario |
| T3.18 | UI | CU-04 A2/A3/E2: cancelar; texto de 10 000 caracteres; intentar superar el límite | Se descarta; se guarda íntegro; no deja escribir más |
| T3.19 | UI | CU-05 FP/RF-17: transiciones entre los 3 estados (6 transiciones) | Todas posibles y persisten |
| T3.20 | UI | CU-05 A1/A2: tocar el estado activo; Pendiente → Visto sin nota | Sin cambios; permitido |
| T3.21 | UI | CU-05: cambiar estado no modifica la nota ni el comentario | Intactos |
| T3.22 | UI | CU-05 E1: fallo simulado | M-25 y el selector vuelve al estado anterior |
| T3.23 | UI | CU-06 FP / RF-19: eliminar muestra M-06 | Diálogo de confirmación con el nombre |
| T3.24 | UI | CU-06 A1: cancelar la confirmación | No se borra |
| T3.25 | D | CU-06 FP / RF-20: borrar un perfil que está en 3 listas (normal, prioridad, ranking) | Desaparece de las 3 y de la base de datos |
| T3.26 | D | CU-06 FP paso 4 / S-07: borrar un perfil del medio de un ranking | Posiciones contiguas |
| T3.27 | D | CU-06 A3 / S-08: borrar el único perfil de una saga | La saga sigue registrada |
| T3.28 | D | CU-06 A4 / S-09: borrar el único perfil de una lista | Lista vacía conservada; no se lanza M-11 |
| T3.29 | D | CU-06 E1: fallo durante el borrado | No se borra nada |
| T3.30 | UI | CU-07 E1: el perfil se borra mientras se ve | Vuelve atrás con aviso |
| T3.31 | E | RNF-09: puntuar un perfil existente | ≤ 3 pasos |
| T3.32 | G | Detalle con puntuación, sin puntuación, con crítica, estados | Dentro de tolerancia |
| TX.01–TX.04 | — | Ver §1.5 | Pasan |

---

### S4 — Biblioteca y agrupar
**Objetivo**: ver todos los perfiles y organizarlos.

**Alcance**: pantalla Biblioteca (`design.md` §4): contadores, filtro por tipo, filtro por escala, tarjetas de medio, lista con scroll, vacío; agrupación por puntuación, saga, idioma, género y estado.

**Tests de aceptación**
| ID | Tipo | Verifica | Resultado esperado |
|---|---|---|---|
| T4.01 | UI | CU-08 FP / RF-22: el listado muestra el estado de cada perfil | Insignia correcta por perfil |
| T4.02 | UI | CU-08 FP: contadores Viendo/Pendientes/Vistas con el fixture | Coinciden con los datos |
| T4.03 | UI | CU-08 FP / S-10: orden por defecto | Más recientes primero |
| T4.04 | UI | CU-08 A3: biblioteca vacía | M-22 y botón "Añadir" |
| T4.05 | UI | CU-08 A1: filtro Todas / Solo películas / Solo series | Recuentos y resultados correctos |
| T4.06 | UI | CU-08 A2: filtro por escala de puntuación | Resultados del rango; "Sin puntuación" incluye solo perfiles sin nota |
| T4.07 | UI | RNF-01 / RES-09: con 25 perfiles se ven ≤ 10 filas a la vez y se llega al resto con scroll | Cumple |
| T4.08 | UI | CU-08 A4: tocar un perfil abre su detalle | Navega |
| T4.09 | D | CU-09 / RF-25: agrupar por puntuación con los valores límite de §1.4 | 2.9 → 0–2.9; 3.0 → 3–4.9; 8.9 → 7–8.9; 9.0 y 10 → 9–10; sin nota → "Sin puntuación"; 6 grupos máximo |
| T4.10 | D | CU-09 / RF-26: perfil con 3 géneros | Aparece en los 3 grupos de género |
| T4.11 | D | CU-09 / RF-26: perfil con 2 idiomas | Aparece en los 2 grupos de idioma |
| T4.12 | D | CU-09 / S-11: perfiles sin género, idioma o saga | Aparecen en "Sin género/idioma/saga" |
| T4.13 | D | CU-09 / RF-24: agrupar por estado | Pendiente, Viendo, Visto con sus perfiles |
| T4.14 | D | CU-09 / RF-23: cada criterio con ámbito Todo / Solo películas / Solo series | El ámbito filtra correctamente |
| T4.15 | D | CU-09 / S-12: grupos vacíos | No se muestran |
| T4.16 | UI | CU-09 A1/A2: colapsar un grupo; cambiar de criterio manteniendo el ámbito | Funciona |
| T4.17 | UI | CU-09 E1: agrupar con biblioteca vacía | M-22 |
| T4.18 | UI | CU-08 E1: error de lectura simulado | M-25 con "Reintentar" |
| T4.19 | R | RNF-08: abrir la Biblioteca con el fixture grande | < 1 s hasta mostrar la primera pantalla |
| T4.20 | G | Biblioteca con datos, vacía y filtrada vs `screen.png` | Dentro de tolerancia |
| TX.01–TX.04 | — | Ver §1.5 | Pasan |

---

### S5 — Listas (crear, ver y gestionar elementos)
**Objetivo**: listas de los tres tipos, con restricciones, añadir y quitar elementos, y borrado. Las operaciones específicas de prioridad y ranking se completan en S6.

**Alcance**: pantalla Listas (§5), diálogo Nueva Lista, lista abierta (§6), selector "Añadir título", diálogos de confirmación, aviso único al vaciar (CU-16 A1), borrar lista (CU-19), renombrar (CU-14), añadir a lista desde el perfil.

**Tests de aceptación**
| ID | Tipo | Verifica | Resultado esperado |
|---|---|---|---|
| T5.01 | UI | CU-11 FP / RF-27: crear una lista Normal | Aparece con tipo `[NORMAL]` y 0 elementos |
| T5.02 | UI | CU-11 FP / RF-28, RF-29: crear una de Prioridad y otra de Ranking | Tipos correctos |
| T5.03 | UI | CU-11 / RF-30: una lista Mixto admite película y serie | Ambas se pueden añadir |
| T5.04 | UI | CU-11 / RF-32: lista "Solo Películas" | Muestra la etiqueta "Restringida: Solo Películas" |
| T5.05 | UI | CU-11 E1/E2: nombre vacío y duplicado (normalizado) | M-07 y M-08 |
| T5.06 | UI | CU-11 A1: cancelar | No se crea nada |
| T5.07 | UI | CU-12 FP / RF-43: pantalla Listas con 3 listas | Muestra nombre, tipo y número de elementos de cada una |
| T5.08 | UI | CU-12 A1: sin listas | Estado vacío con "+ Nueva Lista" |
| T5.09 | UI | CU-13 A1: lista vacía | M-23 y "Añadir título" |
| T5.10 | UI | CU-15 FP: añadir un perfil a una lista normal desde la lista | Aparece y el contador sube a 1 |
| T5.11 | UI | CU-15 FP desde el perfil: "+ Añadir a lista" | Se añade; el perfil muestra la lista |
| T5.12 | UI | CU-15 E1 / RES-08: añadir el mismo perfil dos veces | M-10; un solo elemento |
| T5.13 | UI | CU-15 E2 / RF-32: intentar añadir una serie a una lista de solo películas | No se ofrece o está deshabilitado; si se fuerza, M-09 |
| T5.14 | UI | CU-15 A4 / RF-31: añadir el mismo perfil a 3 listas | Aparece en las 3 |
| T5.15 | UI | CU-15 A3: añadir varios seguidos desde el selector | Todos se añaden sin cerrar |
| T5.16 | UI | CU-15 A5: buscar en el selector "padrino" y "PÁDRINO" | Mismos resultados; sin coincidencias, M-13 |
| T5.17 | UI | RNF-09: añadir un perfil a una lista | ≤ 3 pasos |
| T5.18 | UI | CU-13 / RNF-01, RES-09: una lista con 25 elementos | Se ven ≤ 10 a la vez; scroll hasta el 25 |
| T5.19 | UI | CU-14 FP: renombrar | Nombre nuevo; elementos intactos |
| T5.20 | UI | CU-14 A2/E1/E2: mismo nombre; vacío; duplicado | Sin cambios; M-07; M-08 |
| T5.21 | UI | CU-16 FP: quitar un elemento (con confirmación) | Desaparece de la lista; el perfil sigue en la biblioteca |
| T5.22 | UI | CU-16 A2: cancelar la confirmación | Sin cambios |
| T5.23 | UI | CU-16 A1: quitar el último elemento | Aparece M-11 **una sola vez** |
| T5.24 | UI | CU-16 A1a: aceptar M-11 | La lista se elimina sin pedir segunda confirmación; los perfiles se conservan |
| T5.25 | UI | CU-16 A1b / S-17: rechazar M-11 | La lista queda vacía (M-23); no vuelve a aparecer el aviso mientras siga vacía |
| T5.26 | UI | CU-16 A1b: tras rechazar, borrar la lista manualmente | CU-19 funciona |
| T5.27 | UI | S-17: rechazar M-11, añadir un elemento y quitarlo de nuevo | M-11 aparece otra vez |
| T5.28 | UI | CU-16 con 2 elementos: quitar uno | No aparece M-11 |
| T5.29 | UI | CU-19 FP / RF-38: borrar lista con confirmación M-12 | Se elimina; los perfiles siguen en la biblioteca y en sus otras listas |
| T5.30 | UI | CU-19 A1: cancelar | No se borra |
| T5.31 | D | CU-06 integrado (S3 T3.25): borrar un perfil que está en listas | Se quita de todas las listas |
| T5.32 | UI | CU-13 E1 / CU-16 E1: la lista o elemento ya no existe | Aviso y refresco |
| T5.33 | UI | CU-11 E3 / CU-16 E2: fallo de escritura | M-25; sin cambios |
| T5.34 | E | RNF-07: crear lista con elementos, reiniciar la app | Todo persiste |
| T5.35 | G | Pantalla Listas, lista abierta (normal), diálogos vs `screen.png` | Dentro de tolerancia |
| TX.01–TX.04 | — | Ver §1.5 | Pasan |

---

### S6 — Prioridad y ranking
**Objetivo**: comportamiento específico de las listas de prioridad y de ranking.

**Alcance**: asignar posición/prioridad al añadir (RF-39); cambiar prioridad (CU-17); reordenar ranking con arrastre y flechas ▲▼ (CU-18); filas de ranking con medallas #1–#3 (§6); orden de prioridad.

**Tests de aceptación**
| ID | Tipo | Verifica | Resultado esperado |
|---|---|---|---|
| T6.01 | UI | CU-15 FP / RF-39: añadir a un ranking sin indicar posición | Queda al final (n+1) |
| T6.02 | UI | CU-15 A1: añadir en la posición 2 de un ranking de 5 | Queda en el #2; los demás bajan; posiciones 1..6 |
| T6.03 | UI | CU-15 A1: añadir en la posición n+1 | Queda al final |
| T6.04 | UI | CU-15 E3: posición 0, n+2, texto, vacío | M-24 salvo vacío (= al final); no se añade con las inválidas |
| T6.05 | UI | CU-15 FP / S-16: añadir a prioridad sin elegir | Prioridad Media |
| T6.06 | UI | CU-15 A2 / RF-40: añadir con Alta, Media y Baja | Cada una con su nivel |
| T6.07 | UI | CU-13 / RF-42: lista de prioridad con elementos mezclados | Orden Alta → Media → Baja; desempate por antigüedad |
| T6.08 | UI | CU-13: lista de ranking con 12 elementos | Orden por posición; ≤ 10 visibles a la vez; scroll |
| T6.09 | UI | CU-17 FP / RF-36: cambiar la prioridad de un elemento | La lista se reordena |
| T6.10 | UI | CU-17 A1: elegir el mismo nivel | Sin cambios |
| T6.11 | D | CU-17 A2 / RNF-03 / RES-07: el mismo perfil en dos listas de prioridad con niveles distintos | Cada lista conserva su nivel; el perfil no tiene atributo de prioridad |
| T6.12 | UI | CU-18 FP / RF-37: arrastrar el #5 al #2 | Nuevo orden; posiciones 1..n contiguas |
| T6.13 | UI | CU-18 FP: ▲ y ▼ | Intercambian con el vecino |
| T6.14 | UI | CU-18 A1: ▲ en el #1 y ▼ en el último | Deshabilitados; sin efecto |
| T6.15 | UI | CU-18 A2: arrastrar y soltar en el mismo sitio | Sin cambios |
| T6.16 | UI | CU-18 A3: arrastrar con 25 elementos con scroll automático | Funciona |
| T6.17 | D | CU-18 / RNF-02 / RES-07: reordenar en un ranking no cambia el perfil ni otros rankings | Solo cambia esa lista |
| T6.18 | D | RF-41: ranking con 150 elementos | Se acepta, sin tamaño máximo |
| T6.19 | UI | CU-16 en ranking: quitar el #2 de 5 | Los posteriores suben; 1..4 |
| T6.20 | UI | CU-16 A3 en prioridad: quitar uno | El resto conserva su nivel |
| T6.21 | UI | CU-18 E1: fallo simulado | M-25 y se restaura el orden |
| T6.22 | UI | RNF-09: añadir a ranking al final | ≤ 3 pasos |
| T6.23 | E | RNF-07: reordenar, cerrar y reabrir | Orden persistente |
| T6.24 | G | Ranking (con medallas), prioridad y diálogo de posición vs `screen.png` | Dentro de tolerancia |
| TX.01–TX.04 | — | Ver §1.5 | Pasan |

---

### S7 — Búsqueda
**Objetivo**: búsqueda combinada con todos los criterios.

**Alcance**: pantalla Buscar (§9): nombre, tipo, rango de puntuación (slider doble), géneros, idioma, saga, estado, botones Limpiar/Aplicar, resultados.

**Tests de aceptación**
| ID | Tipo | Verifica | Resultado esperado |
|---|---|---|---|
| T7.01 | D | CU-20 / RF-46: buscar "padrino", "PADRINO", "pádrino" con el perfil "El Padrino" | Siempre lo encuentra |
| T7.02 | D | CU-20 / RF-44: nombre parcial ("dun") | Coincide con "Dune" |
| T7.03 | D | CU-20 / RF-44: puntuación mínima 7 | Solo ≥ 7.0; sin los perfiles sin nota |
| T7.04 | D | CU-20: puntuación máxima 5 | Solo ≤ 5.0; sin los perfiles sin nota |
| T7.05 | D | CU-20 / RF-44: rango 7.0–10 con valores en los extremos (7.0, 10, 6.9) | Incluye 7.0 y 10, excluye 6.9 |
| T7.06 | D | CU-20 / RF-44: perfiles sin puntuación con rango activo | Quedan fuera |
| T7.07 | D | CU-20 / RF-44: sin rango activo | Los perfiles sin nota sí aparecen |
| T7.08 | D | CU-20: filtro por un género; por dos géneros (OR) | Correcto (S-19) |
| T7.09 | D | CU-20: filtro por idioma; por saga | Correcto |
| T7.10 | D | CU-20 / RF-45: combinar nombre + género + rango + tipo | Solo los que cumplen todos (AND) |
| T7.11 | D | CU-20 / RF-47: tipo Solo películas, Solo series, Todas (por defecto) | Correcto; por defecto ambas |
| T7.12 | D | CU-20 / S-18: filtro por estado (uno y varios) | Correcto |
| T7.13 | D | CU-20 A2: sin criterios | Devuelve todos los perfiles |
| T7.14 | UI | CU-20 E1 / RF-48: criterios sin coincidencias | M-13 |
| T7.15 | UI | CU-20 E2: mínimo 8 y máximo 5 | M-14; no se busca |
| T7.16 | UI | CU-20 E3: valor de puntuación inválido | M-03 |
| T7.17 | D | CU-20 E4: nombre con `%`, `_`, `'`, `"`, `\` | Se trata como texto literal; sin error |
| T7.18 | UI | CU-20 A1: "Limpiar" | Restablece todo (tipo = Todas) |
| T7.19 | UI | CU-20 A3: pulsar un resultado | Abre el detalle |
| T7.20 | UI | RNF-01: más de 10 resultados | Scroll |
| T7.21 | UI | CU-20 FP: el recuento "N coincidencias" coincide con las filas | Coincide |
| T7.22 | R | RNF-08: cada búsqueda combinada con el fixture grande | < 1 s |
| T7.23 | G | Pantalla Buscar (vacía, con filtros, con resultados, sin resultados) vs `screen.png` | Dentro de tolerancia |
| TX.01–TX.04 | — | Ver §1.5 | Pasan |

---

### S8 — Copia de seguridad
**Objetivo**: exportar e importar todos los datos sin pérdidas.

**Alcance**: pantalla Ajustes / Datos Locales (§10): métricas, distribución, exportar, importar; formato JSON versionado; selector de archivos del sistema; validación previa e importación transaccional.

**Tests de aceptación**
| ID | Tipo | Verifica | Resultado esperado |
|---|---|---|---|
| T8.01 | UI | CU-21 FP: exportar con datos | Se genera `cinetrack_backup_AAAA-MM-DD.json`; M-15 |
| T8.02 | U | CU-21 FP: el JSON incluye versión de formato, perfiles (con géneros, idiomas, saga, nota, comentario, estado), sagas y listas con posición/prioridad | Todos los campos presentes |
| T8.03 | U | CU-21 A2: exportar sin datos | Archivo válido y vacío |
| T8.04 | UI | CU-21 A1: cancelar el selector | Sin archivo ni error |
| T8.05 | UI | CU-21 E1: destino no escribible o sin espacio | M-16 y sin archivo corrupto |
| T8.06 | D | **Ida y vuelta** (CU-22 A4): exportar el fixture, vaciar la base de datos, importar | Datos idénticos (perfiles, notas, estados, comentarios, géneros, idiomas, sagas, listas, posiciones y prioridades) |
| T8.07 | D | Ida y vuelta con el fixture grande (1000 perfiles) | Datos idénticos |
| T8.08 | UI | CU-22 FP paso 3 / S-20: aparece M-19 antes de importar, con los recuentos correctos de perfiles nuevos, omitidos, listas nuevas y ampliadas | Confirmación obligatoria; recuentos coinciden con el resultado real |
| T8.09 | UI | CU-22 A1: cancelar en el selector o en M-19 | Datos sin cambios |
| T8.10 | UI | CU-22 E1: importar un archivo que no es JSON, y un JSON de otra app | M-17; sin cambios |
| T8.11 | UI | CU-22 E2: archivo con versión de formato futura | M-18; sin cambios |
| T8.12 | D | CU-22 E3: puntuación inválida (7.25), perfil duplicado dentro del archivo, tipo que viola la restricción de la lista, el mismo perfil dos veces en una lista, elemento que apunta a un perfil inexistente, género o idioma desconocido | M-17 con detalle; **no** se importa nada |
| T8.13 | D | CU-22 E4: fallo forzado a mitad de la fusión | M-21; los datos previos intactos y sin elementos a medias |
| T8.14 | D | CU-22 A2: archivo con formato de versión anterior | Se migra e importa |
| T8.15 | D | CU-22 A3: archivo vacío válido; archivo sin nada nuevo | No cambia ningún dato; M-20 con ceros |
| T8.16 | UI | CU-22 E5: archivo grande | Indicador de progreso; la interfaz no se bloquea |
| T8.17 | D | RF-49: importar un archivo del sprint anterior (compatibilidad de formato, "golden file" versionado en el repositorio) | Se importa correctamente |
| T8.18 | UI | Métricas: títulos, listas, almacenamiento y distribución películas/series coinciden con los datos | Coinciden |
| T8.19 | M | RNF-06: exportar e importar con el modo avión | Funciona |
| T8.20 | M | Exportar a memoria interna y, si existe, a tarjeta SD; reimportar en otro dispositivo | Funciona |
| T8.21 | G | Pantalla Ajustes vs `screen.png` | Dentro de tolerancia |
| T8.22 | D | CU-22 reglas / S-20: base local con perfiles A y B; copia con B, C y D | A y B intactos; C y D añadidos; B contado como omitido |
| T8.23 | D | CU-22 reglas: perfil existente con nota 5.0 y la copia lo trae con 9.0, otro estado y otro comentario | Prevalecen los datos locales (nota 5.0, estado y comentario locales); géneros, idiomas y saga locales sin cambios |
| T8.24 | D | CU-22 reglas: mismo nombre con distinto tipo ("Dune" película local, "Dune" serie en la copia) | Se crea el perfil serie; la película no cambia |
| T8.25 | D | CU-22 reglas: nombre igual salvo mayúsculas/tildes ("EL PÁDRINO" vs "El Padrino") | Detectado como el mismo perfil y omitido |
| T8.26 | D | CU-22 reglas: saga existente por nombre normalizado y saga nueva | La existente se reutiliza (sin duplicar); la nueva se crea; un perfil nuevo queda asociado a la correcta |
| T8.27 | D | CU-22 reglas: lista normal con mismo nombre, tipo y restricción | Se amplía con los perfiles que faltan; los presentes no se duplican (RES-08) |
| T8.28 | D | CU-22 reglas: ranking existente (3 elementos) y copia con el mismo ranking (5 elementos, 2 ya presentes) | Los 3 originales conservan sus posiciones 1–3; los nuevos se añaden como 4 y 5 en el orden de la copia; posiciones contiguas |
| T8.29 | D | CU-22 reglas: lista de prioridad existente ampliada | Los elementos nuevos llevan el nivel de la copia; los existentes no cambian |
| T8.30 | D | CU-22 reglas: lista con mismo nombre pero distinto tipo o distinta restricción | Se crea "{nombre} (importada)"; la local no cambia; si ese nombre existe, "(importada 2)" |
| T8.31 | D | CU-22 reglas: lista de la copia sin coincidencia | Se crea con sus elementos, posiciones y niveles |
| T8.32 | D | CU-22 reglas: elemento de lista de la copia cuyo perfil ya existía localmente (omitido) | Se añade a la lista usando el perfil local |
| T8.33 | D | CU-22 **idempotencia**: importar el mismo archivo dos veces | La segunda vez no cambia nada; M-20 con 0 nuevos |
| T8.34 | D | CU-22: importar el mismo archivo exportado desde la propia base de datos (sin vaciarla) | Sin cambios |
| T8.35 | D | CU-22 FP: el recuento previo de M-19 coincide con el resultado final de M-20 | Coinciden |
| T8.36 | D | CU-22 postcondición: ningún dato previo se borra ni modifica tras una fusión con el fixture | Hash de los datos previos idéntico |
| TX.01–TX.04 | — | Ver §1.5 | Pasan |

---

### S9 — Rendimiento, adaptación y cierre
**Objetivo**: cumplir los requisitos no funcionales con carga real, pulir la fidelidad visual y preparar la versión final.

**Alcance**: medición con 1000 perfiles, tamaños de pantalla, accesibilidad, estados vacíos y de error, fidelidad visual final, ofuscación (R8) y APK/AAB firmado.

**Tests de aceptación**
| ID | Tipo | Verifica | Resultado esperado |
|---|---|---|---|
| T9.01 | R | RNF-08: abrir un perfil con 1000 perfiles almacenados | < 1 s |
| T9.02 | R | RNF-08: abrir una lista con 200 elementos | < 1 s |
| T9.03 | R | RNF-08: búsqueda combinada local | < 1 s |
| T9.04 | R | RNF-08: Biblioteca, agrupar por género con 1000 perfiles | < 1 s |
| T9.05 | R | Scroll de 1000 elementos de la Biblioteca | Sin fotogramas perdidos apreciables (≥ 55 fps de media) |
| T9.06 | R | Arranque en frío con 1000 perfiles | < 2 s (objetivo interno) |
| T9.07 | UI | RNF-11: pantallas en 320 dp, 360 dp, 411 dp y 600 dp de ancho en vertical | Sin cortes, solapes ni scroll horizontal |
| T9.08 | UI | RNF-11: tamaño de fuente del sistema al 130 % y 200 % | Legible; sin textos cortados en los botones clave |
| T9.09 | UI | RNF-11: modo apaisado | No se rompe (soportado o bloqueado a vertical) |
| T9.10 | UI | Accesibilidad: `contentDescription` en iconos; objetivos táctiles ≥ 48 dp; TalkBack recorre las pantallas clave | Cumple |
| T9.11 | UI | Contraste AA de texto sobre superficies | Cumple |
| T9.12 | UI | Todas las pantallas en estado vacío y en estado de error | Mensajes M-22, M-23, M-13 y M-25 correctos |
| T9.13 | E | **Recorrido completo**: crear 3 perfiles → puntuar → comentar → crear una lista de cada tipo → añadir → reordenar → buscar → agrupar → exportar → vaciar → importar → comprobar | Sin errores; datos idénticos |
| T9.14 | E | Cada caso de uso CU-01..CU-23: flujo principal en cadena (suite de humo) | Todos pasan |
| T9.15 | M | RNF-07: reiniciar el dispositivo y comprobar los datos | Intactos |
| T9.16 | M | RNF-10: probar en `minSdk`, una versión intermedia y la última | Funciona en las tres |
| T9.17 | M | RNF-06: modo avión y sin datos móviles durante 1 sesión completa | Todas las funciones operativas |
| T9.18 | U | RES-03 / TX.01: el APK final no tiene permiso `INTERNET` ni librerías de red | Comprobado en el APK de release |
| T9.19 | M | Compilación `release` (R8): repetir T9.13 | Sin diferencias |
| T9.20 | G | **Fidelidad visual final**: todas las pantallas vs `screen.png` | Dentro de tolerancia; diferencias solo las de `design.md` §12 |
| TX.01–TX.04 | — | Ver §1.5 | Pasan |

---

## 4. Cobertura: casos de uso → tests

Cada flujo de `casosUso.md` debe tener al menos un test. Si falta algún flujo al cerrar un sprint, el sprint no está terminado.

| Caso de uso | Sprint | Tests |
|---|---|---|
| CU-01 | S2 | T2.01–T2.12, T2.15, T2.16, T2.21–T2.24, T2.28 |
| CU-02 | S3 | T3.01–T3.08 |
| CU-03 | S2, S3 | T1.01, T1.02, T2.13, T2.14, T3.09–T3.14 |
| CU-04 | S3 | T3.15–T3.18 |
| CU-05 | S3 | T3.19–T3.22 |
| CU-06 | S3, S5 | T3.23–T3.29, T5.31 |
| CU-07 | S2, S3 | T2.25–T2.27, T3.30 |
| CU-08 | S4 | T4.01–T4.08, T4.18, T4.19 |
| CU-09 | S4 | T4.09–T4.17 |
| CU-10 | S2 | T2.17–T2.20 |
| CU-11 | S5 | T5.01–T5.06, T5.33 |
| CU-12 | S5 | T5.07, T5.08 |
| CU-13 | S5, S6 | T5.09, T5.18, T5.32, T6.07, T6.08 |
| CU-14 | S5 | T5.19, T5.20 |
| CU-15 | S5, S6 | T5.10–T5.17, T6.01–T6.06, T6.22 |
| CU-16 | S5, S6 | T5.21–T5.28, T6.19, T6.20 |
| CU-17 | S6 | T6.09–T6.11 |
| CU-18 | S6 | T6.12–T6.17, T6.21 |
| CU-19 | S5 | T5.24, T5.26, T5.29, T5.30 |
| CU-20 | S7 | T7.01–T7.22 |
| CU-21 | S8 | T8.01–T8.05 |
| CU-22 | S8 | T8.06–T8.17, T8.22–T8.36 |
| CU-23 | S0, S1, S2, S9 | T0.03, T1.12, T1.19, T2.29, T9.15, T9.16 |

### Cobertura de requisitos no funcionales y restricciones
| Requisito | Tests |
|---|---|
| RNF-01, RES-09 (máx. 10 visibles) | T4.07, T5.18, T6.08, T7.20 |
| RNF-02, RNF-03, RES-07 | T1.17, T6.11, T6.17 |
| RNF-04, RNF-05 | T1.01, T1.02, T2.13 |
| RNF-06, RES-03 | TX.01, T8.19, T9.17, T9.18 |
| RNF-07 | T1.19, T2.30, T5.34, T6.23, T9.15 |
| RNF-08 | T4.19, T7.22, T9.01–T9.06 |
| RNF-09 | T2.28, T3.31, T5.17, T6.22 |
| RNF-10, RES-01, RES-02 | T0.03, T9.16 |
| RNF-11 | T9.07–T9.09 |
| RNF-12, RES-10 | TX.02 |
| RES-04 | T1.19, T8.06 |
| RES-05 | T2.01 (todos los datos se introducen a mano; no hay llamadas de red, TX.01) |
| RES-06 | T1.04, T1.05, T2.05, T2.06 |
| RES-08 | T1.13, T5.12 |
| RES-11 | TX.01 (sin servidores; coste de infraestructura nulo) |

---

## 5. Orden de ejecución y dependencias

```
S0 ─ S1 ─ S2 ─ S3 ─┬─ S4 ──────────────┐
                   ├─ S5 ─ S6 ─────────┼─ S7 ─ S8 ─ S9
                   └───────────────────┘
```
- S1 debe terminar antes de cualquier pantalla con datos.
- S4 y S5 pueden hacerse en cualquier orden tras S3. S6 requiere S5.
- S7 requiere S2 y la normalización de S1; se deja después de S5/S6 para poder buscar también al añadir a listas.
- S8 requiere que existan todos los datos (perfiles y listas), por eso va después de S6.
- S9 va el último; el rendimiento se vigila desde S4 con el fixture grande.

## 6. Riesgos y decisiones abiertas
| Riesgo / decisión | Impacto | Acción |
|---|---|---|
| Supuestos S-01..S-21 de `casosUso.md` sin confirmar | Cambian el resultado esperado de varios tests | Confirmar antes del S1 los que afectan al modelo (S-02, S-03, S-04, S-14, S-15) y antes de cada sprint los demás |
| Versión mínima de Android (RES-02) | Disponibilidad de APIs (blur, fuentes) | Decidir en el S0 |
| Carátulas de pósters (`design.md` §12) | Cambia el modelo (campo de imagen), CU-01/02 y el copiado de ficheros | Decidir antes del S2 |
| Arrastre en `LazyColumn` con scroll automático | Puede ser frágil | Mantener siempre los botones ▲▼ como alternativa |
| Desenfoque de barras en Android < 12 | Diferencia visual | Superficie sólida al 95 % como alternativa (G excepciones) |
| Importación por fusión (S-20) | Reglas de fusión complejas (listas, rankings, conflictos); un perfil existente se omite aunque la copia sea más reciente | Reglas fijadas en CU-22 y cubiertas por T8.22–T8.36; el usuario ve los recuentos antes de confirmar (M-19) |
