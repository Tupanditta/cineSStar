# DESIGN — CineTrack (CineSStar)

> **Fuente de verdad**: el HTML de las 6 pantallas de Google Stitch en `GoogleStitch/stitch_android_web_app_screens/*/code.html` y sus `screen.png`.
> El archivo `cinematic_charcoal_sky/DESIGN.md` de Stitch es orientativo: **varios de sus valores no son los que se ven en las pantallas** (superficies, colores de estado, nombres de la barra inferior). Cuando discrepan, manda el HTML.
> Objetivo: que la app Android sea lo más parecida posible a las capturas. Las únicas desviaciones permitidas están en §12 (por requisitos) y §13 (inconsistencias internas del prototipo).
> Equivalencias: 1 px CSS del prototipo = **1 dp** en Android. Las clases Tailwind se citan entre `código` para poder comprobarlas en el HTML.
> Idioma de toda la interfaz: español.

---

## 1. Identidad visual

- Nombre en la app: **CineTrack**. Junto a él, la píldora **"100% Local"**.
- Tema **solo oscuro**. Fondo carbón `#121316`, nunca negro puro. Acento principal azul cielo `#38BDF8`.
- Elevación por capas tonales (superficies más claras = más arriba). Sombras muy suaves.
- Tipografía **Inter** en todo. Iconos **Material Symbols Outlined** (FILL 0 por defecto; algunos iconos usan FILL 1, se indica en cada caso).
- Esquinas redondeadas (8–12 dp en tarjetas, píldora en botones y chips principales).

---

## 2. Tokens de diseño

### 2.1 Paleta del tema (tailwind.config del prototipo)

El prototipo tiene **dos** `tailwind.config` distintos. Se toma como canónico el de 4 pantallas (Añadir, Buscar, Detalle, Ranking). El de Biblioteca y Listas solo cambia 5 tokens (columna "Variante").

| Token | Canónico | Variante (Biblioteca/Listas) | Uso principal |
|---|---|---|---|
| `background` / `surface` / `surface-dim` | `#121316` | = | Fondo de pantalla |
| `surface-container-lowest` | `#0D0E11` | = | Segmentados de formulario, caja de crítica, chips de idioma, franja de estado |
| `surface-container-low` | `#1B1B1F` | = | Tarjetas de Biblioteca y Ranking, campos de texto, chips de género sin seleccionar |
| `surface-container` | `#1F1F23` | = | Tarjetas (Listas, Detalle, Buscar), chips inactivos |
| `surface-container-high` | `#292A2D` | = | Botones tonales, diálogos, filas internas, chips seleccionados de Buscar |
| `surface-container-highest` | `#323742` | `#343538` | Cajas de icono, insignias de estado, pista de barras |
| `surface-bright` | `#38393C` | = | Toast, pulsado de botones tonales |
| `surface-variant` | `#343538` | = | Etiqueta "Opcional" |
| `on-surface` / `on-background` | `#E3E2E6` | = | Texto principal |
| `on-surface-variant` | `#BDC8D1` | = | Texto secundario, iconos inactivos |
| `outline` | `#87929A` | = | Placeholders, "/ 10", textos terciarios |
| `outline-variant` | `#3E484F` | = | (sin uso visible) |
| `primary` | `#8ED5FF` | = | Texto e iconos de acento (nota grande, enlaces, chips activos de Buscar) |
| `on-primary` | `#00354A` | = | Icono del avatar sobre `primary` |
| `primary-container` | `#38BDF8` | = | **Acento**: botones rellenos, segmento activo, FAB, ranking #1–#3 |
| `on-primary-container` | `#004965` | `#00354A` | Texto sobre `primary-container` |
| `secondary` | `#FFB3B6` | = | Asterisco de obligatorio, errores de validación del formulario, estado Pendiente, lista de prioridad |
| `on-secondary` | `#68001A` | = | Texto del botón "Quitar" en el prototipo; no se usa (contraste 2.3:1, §12) |
| `secondary-container` | `#CC003C` | = | Botón "Quitar", mosaico Prioridad seleccionado, punto de lista |
| `on-secondary-container` | `#FFDCDC` | = | Texto sobre `secondary-container` |
| `tertiary` | `#83D7FF` | `#7DD3FC` | Iconos informativos, barra de series |
| `error` | `#FFB4AB` | = | "Eliminar esta lista", prioridad Alta, botón de confirmación de borrado |
| `on-error` | `#690005` | = | Texto sobre `error` |
| `error-container` | `#93000A` | = | Botón "Eliminar de mi catálogo", fondo de prioridad Alta (30 %) |
| `on-error-container` | `#FFDAD6` | = | Texto sobre `error-container` |

### 2.2 Colores de la paleta Tailwind usados directamente
Algunas pantallas usan colores de Tailwind en vez de tokens. Hay que definirlos como tokens extra:

| Clase | Hex | Uso |
|---|---|---|
| `sky-200` | `#BAE6FD` | Número de la nota en las tarjetas de Biblioteca |
| `sky-300` | `#7DD3FC` | Saga en Biblioteca, insignia `[RANKING]`, nota ★ y prioridad Media en filas, "N obras catalogadas" |
| `sky-400` | `#38BDF8` | Igual que `primary-container` (marca, pestaña activa, botones) |
| `sky-500` | `#0EA5E9` | Prioridad Media (fondo al 20 %), borde del rango seleccionado (30 %) |
| `sky-950` | `#082F49` | Fondo del rango seleccionado (60 %) |
| `slate-950` | `#020617` | Texto sobre `sky-400` en Listas |
| `slate-700` | `#334155` | Borde de diálogos de Listas (60 %) |
| `emerald-500` | `#10B981` | Punto "N listas" del banner de Listas |
| `black` | `#000000` | Scrim de diálogos (75–80 %) y sombras |

### 2.3 Estados de visualización (tal como se pintan)
> El `DESIGN.md` de Stitch propone verde para "Visto". **No se usa en las pantallas.** Se aplica lo siguiente.

| Estado | Insignia en tarjetas (Biblioteca) | Chip en resultados (Buscar) | Icono en selectores |
|---|---|---|---|
| **Viendo** | fondo `surface-container-high`, texto `sky-400` semibold, **punto 6 dp `sky-400` con pulso** | fondo `primary-container` 15 %, texto `primary` | `play_circle` |
| **Visto** | fondo `surface-container-highest`, texto `on-surface` medium, icono `check` 13 dp `sky-400` | fondo `surface-container-highest`, texto `on-surface-variant` | `check_circle` |
| **Pendiente** | fondo `surface-container-highest`, texto `secondary` medium, icono `hourglass_empty` 13 dp | fondo `surface-container-highest`, texto `secondary` (*no aparece en el prototipo; mismo criterio*) | `schedule` (Añadir y contador), `hourglass_top` (Buscar), `bookmark_add` (Detalle) |

Todas: píldora, `label-sm`, padding 8×2 dp.

### 2.4 Prioridad (filas de listas)
Píldora, `label-sm` semibold, padding 6×2 dp:
- **Alta**: fondo `error-container` 30 %, texto `error`.
- **Media**: fondo `sky-500` 20 %, texto `sky-300`.
- **Baja**: fondo `surface-container-highest`, texto `on-surface-variant`.

### 2.5 Tipografía (Inter)

| Token | Tamaño / interlínea | Peso | Tracking | Uso en el prototipo |
|---|---|---|---|---|
| display-lg | 36/44 | 800 | −0.02em | Nota grande del Detalle y del formulario |
| headline-lg-mobile | 24/32 | 700 | −0.01em | Título del Detalle y de la lista abierta |
| headline-md | 20/28 | 600 (se usa en bold) | −0.01em | "CineTrack", títulos de diálogo y de "Añadir nuevo perfil", cifras de contadores, `#n` del ranking |
| title-md | 17/24 | 600 | – | Cabeceras de sección, título de barra secundaria |
| title-sm | 15/22 | 600 | – | Títulos de fila, nombre de pantalla bajo la marca |
| body-md | 15/22 | 400 | – | Campos de texto, crítica |
| body-sm | 13/18 | 400 | – | Descripciones y textos de ayuda |
| label-lg | 15/22 | 600 | 0.01em | Botones principales |
| label-md | 13/18 | 600 | 0.02em | Pestañas, chips, etiquetas de sección |
| label-sm | 12/16 | 700 | 0.04em | Insignias, metadatos, barra inferior |

- **Escala ajustada a petición del usuario**: los estilos de título, cuerpo y etiqueta son 1–2 sp mayores que en el prototipo (10 sp de `label-sm` era ilegible en el móvil). Los `display` y `headline` no cambian. Dos estilos propios: título de la tarjeta de Biblioteca 18/24 600 y etiqueta de los contadores de Biblioteca 10/14 700 (conserva el tamaño del prototipo).
- Etiquetas de sección ("TIPO DE PRODUCCIÓN", "ESTADO DE VISUALIZACIÓN", "GÉNEROS"…): `label-md` (13/18), **MAYÚSCULAS**, `tracking-wider` (0.05em), `on-surface-variant`.
- Insignia de tipo sobre el póster del ranking: 9 sp, mayúsculas, bold.
- `headline-lg` (30/36) solo para tablet.

### 2.6 Radios (Tailwind del prototipo)

| Clase | dp | Uso |
|---|---|---|
| `rounded` | 4 | Chips de género en tarjetas, insignia de tipo sobre póster, chip de nota del ranking, insignias de lista del Detalle |
| `rounded-md` | 6 | Chips de idioma del Detalle, botones de icono de la crítica |
| `rounded-lg` | 8 | Pósters pequeños, cajas de icono 40 dp, botones de diálogo, chips de rango, chips de género de Buscar, filas internas |
| `rounded-xl` | 12 | **Tarjetas**, segmentados, campos de texto, póster del Detalle, botones grandes rectangulares |
| `rounded-2xl` | 16 | Diálogo inferior "Quitar del ranking" |
| `rounded-full` | píldora | Botones "Cancelar"/"Guardar Perfil", chips de tipo, pestañas de Buscar, insignias, avatar, FAB |

### 2.7 Espaciado
Base 4 dp: `space-xs` 4 · `space-sm` 8 · `space-md` 16 · `space-lg` 24 · `space-xl` 32. Margen lateral de pantalla `margin` **16 dp**. El resto de medidas se dan en cada componente.

### 2.8 Sombras (Tailwind por defecto)
En tema oscuro son casi invisibles, pero se replican:
- `shadow-sm`: `0 1 2 rgba(0,0,0,.05)`
- `shadow-md`: `0 4 6 -1 rgba(0,0,0,.1)` + `0 2 4 -2 rgba(0,0,0,.1)`
- `shadow-lg`: `0 10 15 -3 rgba(0,0,0,.1)` + `0 4 6 -4 rgba(0,0,0,.1)`
- `shadow-xl`: `0 20 25 -5 rgba(0,0,0,.1)` + `0 8 10 -6 rgba(0,0,0,.1)`
- `shadow-2xl`: `0 25 50 -12 rgba(0,0,0,.25)`
- `shadow-inner`: `inset 0 2 4 rgba(0,0,0,.05)`
- Barras superior e inferior: `0 1 8 rgba(0,0,0,.04)`.

### 2.9 Movimiento
- Pulsar: `scale(0.95)` en botones y FAB; `scale(0.99)` en tarjetas de lista; `scale(0.98)` en el botón destructivo.
- Transiciones de color 150 ms; toasts con fundido de 300 ms.
- Punto de "Viendo" con animación de pulso (`animate-pulse`); punto de la franja de estado de Biblioteca con onda (`animate-ping`).

---

## 3. Estructura global

### 3.1 Barra de estado
Las capturas dibujan una barra falsa (12:45, wifi_off, sd_card, 92 %, batería) de 28 dp. **No se dibuja**: se usa la barra de estado real de Android con fondo transparente (edge-to-edge) y el mismo color de fondo.

### 3.2 Barra superior — pantallas principales (Biblioteca, Listas, Buscar)
- Fondo `surface` al 80 % + desenfoque (`backdrop-blur-xl`, 24 dp) + sombra de barra. Fija arriba.
- Fila de contenido de **52 dp** (80 dp del prototipo − 28 de la barra de estado falsa), padding lateral 16, padding inferior 8.
- Izquierda: **Logo** de 32 dp de alto (imagen `CineTrack Logo` del prototipo, empaquetada).
- Sin texto: la marca "CineTrack", la píldora "100% Local" y el nombre de la pantalla se quitaron a petición del usuario (§12). El nombre de la pantalla se mantiene como encabezado para lectores de pantalla.
- Derecha: **avatar** 32 dp circular con icono `person` 18 dp. Canónico: fondo `#38BDF8`, icono `slate-950` (ver variantes en §13). No tiene cuentas detrás: decorativo (o acceso a Ajustes).

### 3.3 Barra superior — pantallas secundarias (Detalle, Lista abierta, Añadir)
- Mismo fondo y altura.
- Izquierda: botón atrás 44×44 con `arrow_back` 24 dp `on-surface`; logo 32 dp; título `title-md` **bold** `on-surface` truncado ("Detalle Película", "Añadir Medio", y en la lista abierta el texto "Lista").
- Derecha: avatar 32 dp (canónico, §13).
- **Estas pantallas no tienen barra inferior.**

### 3.4 Barra de navegación inferior (solo Biblioteca, Listas y Buscar)
- Altura **64 dp** + inset inferior. Fondo `surface` al 90 % + desenfoque 24 dp + sombra de barra.
- 5 destinos repartidos (`justify-around`), padding lateral 4, cada uno mínimo 44×44, icono 22 dp sobre etiqueta `label-sm`:
  1. **Biblioteca** — `video_library`
  2. **Listas** — `bookmarks`
  3. **Añadir** — círculo **48 dp** `#38BDF8` con `add` 26 dp en `slate-950`, desplazado **20 dp hacia arriba** (`-mt-5`), sombra `shadow-lg` negra al 40 %. Etiqueta "Añadir" debajo (2 dp) en `on-surface-variant`.
  4. **Buscar** — `search`
  5. **Ajustes** — `database`
- Activo: icono y etiqueta `#38BDF8`, bold. Inactivo: `on-surface-variant`.
- El contenido deja 96 dp de margen inferior (`pb-24`) para no quedar tapado.

### 3.5 Toast
Canónico (Listas y Ranking): píldora centrada, fondo `surface-bright`, texto `on-surface` `label-md`, icono `check` 18 dp `#38BDF8` a la izquierda, padding 16×10, `shadow-xl`. Posición: 80 dp sobre el borde inferior si hay barra inferior; 24 dp si no. Visible 2,2 s con fundido de 300 ms.
Variante de **éxito al guardar un perfil**: fondo `primary-container`, texto `on-primary-container` bold, 2,8 s.

### 3.6 Diálogos (tres estilos del prototipo)
1. **Formulario** (Nueva Lista, Añadir a lista): centrado, ancho máx. 384 dp, `rounded-xl`, fondo `surface-container-high`, padding 16, separación 16, borde 1 dp `slate-700` 60 %, `shadow-2xl`, scrim negro 75 % con desenfoque 4 dp. Cabecera: icono 22 dp `#38BDF8` + título `headline-md` + botón `close` 20 dp.
2. **Confirmación destructiva de perfil**: centrado, ancho máx. 384, padding 24, `rounded-xl`, fondo `surface-container-high`, scrim `surface-dim` 80 % con desenfoque. Círculo 48 dp `error-container` con `warning` 28 dp `on-error-container` centrado; título `headline-md` bold centrado; texto `body-sm` `on-surface-variant` centrado (nombre en negrita); botones apilados a ancho completo (`rounded-lg`, 10 dp vertical, `title-sm`): primario fondo `error`, texto `on-error` bold; "Cancelar" fondo `surface-container`.
3. **Hoja inferior "Quitar"** (lista abierta): pegada abajo en móvil, `rounded-2xl`, padding 20, fondo `surface-container-high`, `shadow-2xl`, scrim `background` 80 % con desenfoque. Cabecera: círculo 40 dp `secondary-container` 20 % con icono 20 dp `secondary`; título `title-md` bold + nombre del título en `primary` `body-sm` medium. Texto `body-md` `on-surface-variant`. Botones en fila, `flex-1`, `rounded-xl`, 10 dp vertical, `label-md`: "Cancelar" `surface-container-highest`; acción `secondary-container` + `on-secondary-container` semibold (el prototipo usa `on-secondary`, §12).

### 3.7 Campos de formulario
- Fondo `surface-container-low`, `rounded-xl`, `body-md`, placeholder `outline`. **Al enfocar, el fondo pasa a `surface-container`; no hay borde ni anillo** (en el diálogo Nueva Lista: fondo `surface-container-lowest` + anillo 1 dp `#38BDF8` 50 %).
- Desplegables: mismo estilo con icono `expand_more` 20 dp a la derecha.

---

## 4. Pantalla Biblioteca (`biblioteca_de_medios`)

Barra superior principal (§3.2) con "Biblioteca" y barra inferior con Biblioteca activa.

1. ~~Franja de estado~~ ("Base de datos local • {N} entradas" / "Sin telemetría"): **quitada** a petición del usuario (§12). El contenido empieza 8 dp bajo la barra.
2. **Chips de tipo** (scroll horizontal, separación 4, padding 16 lateral y 8 inferior): píldoras 16×8, `label-md`, icono 18 dp a la izquierda:
   - "Todas ({n})" `auto_awesome` · "Solo Películas ({n})" `movie` · "Solo Series ({n})" `tv`.
   - Activo: fondo `#38BDF8`, texto `slate-950` semibold, `shadow-sm`. Inactivo: fondo `surface-container`, texto `on-surface-variant`.
3. **Contadores** (margen inferior 16): contenedor `rounded-xl` fondo `surface-container-low` padding 4, rejilla de 3 con separación 4. Cada celda: `rounded-lg`, fondo `surface-container`, padding 8, centrada:
   - Fila: icono 16 dp + número `headline-md` bold, ambos del color del estado: **Viendo** `sky-400` (`play_circle` FILL 1), **Pendientes** `secondary` (`schedule`), **Vistas** `on-surface` (`check_circle`).
   - Debajo: etiqueta MAYÚSCULAS 10/14 bold (tamaño del prototipo; los contadores no se agrandan) `tracking-wider` `on-surface-variant`.
   - Pulsar una celda filtra por ese estado (en el prototipo son botones).
4. **Filtrar por escala** (margen inferior 16):
   - Cabecera: icono `star` 20 dp `sky-400` + "Filtrar por escala" `title-sm`; a la derecha "5 rangos" `label-sm` `on-surface-variant`.
   - Chips en scroll horizontal (separación 8): `rounded-lg`, padding 10×6, `label-sm`. Seleccionado: fondo `sky-950` 60 %, borde 1 dp `sky-500` 30 %, texto `sky-300`, `shadow-sm`. Resto: fondo `surface-container`, texto `on-surface-variant`.
   - Textos: "9 - 10 • Imprescindibles", "7 - 8.9 • Muy buenas", "5 - 6.9", "3 - 4.9", "0 - 2.9" y, por requisito, "Sin puntuar" al final. Pulsar un chip seleccionado lo deselecciona.
5. **Catálogo de Medios** (padding lateral 16, separación 16):
   - Cabecera: "Catálogo de Medios" `title-md` bold `tracking-tight`; a la derecha píldora "Mostrando {x} de {N}" `label-sm` fondo `surface-container` padding 10×3.
   - **Tarjeta de medio** (§4.1) por perfil.
6. ~~Pie~~ ("Base de datos al día • Almacenamiento local seguro"): **quitado** a petición del usuario (§12).
7. **Selector "Agrupar por"** (requisito, no aparece en el prototipo): ver §4.2.

### 4.1 Tarjeta de medio
- `article`: fila con separación 8, padding 8, `rounded-xl`, fondo **`surface-container-low`**, `shadow-sm`; al pulsar fondo `surface-container`.
- **Icono de tipo** (sustituye al póster, a petición del usuario, §12): columna de 56 dp alineada arriba con una caja 48×48 `rounded-lg` fondo `surface-container-highest` e icono 28 dp `sky-400` (`movie` para películas, `tv` para series, los mismos de los chips de tipo) y debajo "Película"/"Serie" `label-sm` `on-surface-variant`. El contenido conserva el alto del póster (144 dp): la tarjeta mide lo mismo (160 dp) y la columna de datos gana ancho.
- **Columna** (ocupa el resto, distribuida arriba/abajo, padding vertical 2):
  - Fila 1: título 18/24 semibold truncado; a la derecha **chip de nota**: píldora fondo `surface-container-highest`, padding 8×3, `star` 16 dp FILL 1 `sky-400` + valor `label-sm` bold `sky-200`. **Sin nota**: píldora "Sin puntuar" `label-sm` `on-surface-variant` fondo `surface-container` padding 8×2.
  - Fila 2 (2 dp arriba): "Saga: {nombre}" `label-sm` medium `sky-300`, o "Sin saga asociada" `on-surface-variant`. Truncado.
  - Fila 3 (6 dp arriba): chips de género, separación 6, con salto de línea: `rounded`, padding 8×3, fondo `surface-container`, texto `on-surface-variant` `label-sm`.
  - Fila inferior (8 dp arriba), extremos: idioma con `translate` 16 dp + nombre `label-sm` `on-surface-variant` (si hay varios: el primero y "+n"); a la derecha **insignia de estado** (§2.3) con padding 10×3 e iconos de 15 dp (punto de 7 dp).

### 4.2 Agrupar (requisito RF-23..26)
No existe en el prototipo. Para no romper el diseño se añade, entre "Filtrar por escala" y el catálogo, una fila con el mismo estilo que los chips de escala: "Agrupar: Ninguno · Puntuación · Saga · Idioma · Género · Estado". Al agrupar, el catálogo se divide en secciones con cabecera `title-md` bold + recuento en píldora (mismo estilo que "Mostrando x de N"). El ámbito (todo/películas/series) lo dan los chips de tipo.

**Decisiones del S4** (no fijadas por el prototipo):
- La fila es una cabecera "Agrupar por" (icono `category`, mismo estilo que "Filtrar por escala") con los chips debajo.
- Los filtros se combinan: tipo, estado (contadores) y escala se aplican a la vez y después se agrupa. Los recuentos de los chips de tipo y "{N} entradas" son de toda la biblioteca; los contadores, del tipo elegido; "Mostrando {x} de {N}" = perfiles que pasan los filtros / total.
- Si los filtros no dejan ningún perfil se muestra M-13 en el catálogo.
- Orden de los grupos: puntuación de mayor a menor (como los chips) con "Sin puntuación" al final; saga por nombre; género e idioma en el orden del catálogo; estado Pendiente, Viendo, Visto; los "Sin …" siempre al final. Dentro de cada grupo, más recientes primero (S-10).
- Cada cabecera de grupo se pliega al pulsarla (icono `expand_less`/`expand_more`); cambiar de criterio despliega todo.
- Error de lectura (CU-08 E1): icono `error` 48 dp, M-25 y botón "Reintentar" con el estilo del botón "Añadir" del vacío.

---

## 5. Pantalla Listas (`gesti_n_de_listas`)

Barra superior principal con "Listas" y barra inferior con Listas activa.

### 5.1 Pestañas segmentadas (fijas al hacer scroll)
- Contenedor con fondo `surface` 95 % + desenfoque, padding 16 lateral, 4 arriba, 8 abajo.
- Dentro: `rounded-xl`, fondo `surface-container-high`, padding 4, `shadow-inner`.
- Dos botones `flex-1`, `rounded-lg`, padding 8×4, icono 18 dp + `label-md`, separación 4:
  - "**Mis Listas**" `collections_bookmark` + contador en píldora (`label-sm` bold, fondo `slate-950` 20 %).
  - "**Top Ranking**" `hotel_class` + punto 8 dp `#38BDF8`.
  - Activo: fondo `#38BDF8`, texto `slate-950` semibold, `shadow-md`. Inactivo: `on-surface-variant`.
- En la app, "Top Ranking" abre directamente el último ranking visitado (o el primero); ver §13.

### 5.2 Contenido de "Mis Listas" (padding 16 lateral, 32 inferior, separación 16)
1. **Banner "Colección Personal"**: `rounded-xl`, fondo `surface-container`, padding 16, `shadow-lg`, borde 1 dp `sky-400` 10 %.
   - Izquierda (máx. 70 % del ancho): icono `auto_awesome` 18 dp + "COLECCIÓN PERSONAL" `label-sm` bold MAYÚSCULAS `tracking-wider` en `sky-400`; título "Organiza tu Cineverso" `headline-md` `on-surface`; texto `body-sm` `on-surface-variant` "Crea rankings rigurosos, listas con priorización de visionado y colecciones temáticas offline."
   - Derecha: caja 48 dp `rounded-xl` `surface-container-highest` `shadow-md` con `movie_filter` 28 dp `sky-400`.
   - Pie (16 dp arriba, `label-sm`): punto 8 dp `emerald-500` + "{n} Listas" `on-surface-variant`; a la derecha "{N} Obras catalogadas" semibold `sky-300`.
2. **Cabecera**: "Colecciones Activas" `title-md`; a la derecha "{x} de {n} mostradas" `label-sm` `on-surface-variant`.
3. **Tarjeta de lista** (una por lista): `rounded-xl`, fondo `surface-container`, padding 16, `shadow-md`; pulsado fondo `surface-container-high` y `scale(0.99)`.
   - Fila superior (separación 8): caja de icono 40 dp `rounded-lg` `surface-container-highest` con icono 22 dp; título `title-sm` truncado; debajo, descripción `body-sm` `on-surface-variant` o chip de restricción. A la derecha, **insignia de tipo** píldora `label-sm` bold padding 8×2, con corchetes:
     - `[RANKING]`: fondo `sky-400` 15 %, texto `sky-300`; icono de caja `format_list_numbered` `sky-400`.
     - `[PRIORIDAD]`: fondo `secondary-container` 20 %, texto `secondary`; icono `flag` `secondary`.
     - `[NORMAL]`: fondo `surface-container-highest`, texto `on-surface-variant`; icono `view_list` `sky-400` (series: `tv` `sky-300`; películas: `theaters` `sky-400`).
   - Chip de restricción (2 dp arriba): "Restringida: Solo Películas" / "Restringida: Solo Series", `label-sm`, `rounded`, padding 6×1, fondo `surface-container-highest`, texto `on-surface`.
   - Fila inferior (16 dp arriba, `label-sm`): icono 16 dp + "{n} títulos" (o "{n} películas"/"{n} series" si la lista está restringida) en `on-surface-variant`; punto separador 4 dp `on-surface-variant` 40 %; dato extra:
     - Ranking: "Completa" no aplica (sin límite); se muestra "#1 {título}" en `sky-400` semibold.
     - Prioridad: píldora "{n} Alta" (`rounded`, fondo `error-container` 30 %, texto `error` semibold).
     - Normal: nada.
   - A la derecha: `more_vert` 18 dp (menú Renombrar / Eliminar). La primera tarjeta del prototipo muestra "Abrir →" en `sky-400`: se usa `more_vert` en todas por coherencia (§13).
4. **Botón "+ Nueva Lista"**: alineado a la derecha al final de las tarjetas (no flota): píldora fondo `#38BDF8`, texto `slate-950` `label-lg` semibold, padding 16×12, icono `playlist_add` 22 dp, `shadow-xl`. Texto literal: "+ Nueva Lista".

### 5.3 Diálogo "Nueva Lista" (estilo formulario, §3.6)
- Cabecera: `create_new_folder` + "Nueva Lista".
- "Nombre de la colección" (`label-sm` `on-surface-variant`) + campo `rounded-lg` fondo `surface-container`, padding 8, placeholder "Ej. Joyas Ocultas, Maratón Fin de Semana..." (`on-surface-variant` 40 %).
- "Tipo de Lista": rejilla de 3 (separación 4) de mosaicos `rounded-lg`, padding 8, fondo `surface-container`, icono 18 dp + `label-sm` bold: **Normal** `view_list`, **Prioridad** `flag`, **Ranking** `military_tech`. Seleccionado: Normal y Ranking fondo `#38BDF8` texto `slate-950`; Prioridad fondo `secondary-container` texto `on-secondary-container`.
- "Restricción de contenido (Opcional)": 3 segmentos **Mixto / Películas / Series**, `rounded-lg`, padding vertical 6, `label-sm`, fondo `surface-container`. Seleccionado: fondo `surface-bright`, texto `on-surface`, anillo 1 dp `sky-400` 40 %.
- Acciones a la derecha (separación 8): "Cancelar" (`rounded-lg`, padding 16×8, fondo `surface-container`, `on-surface-variant` `label-md`) y "Guardar Lista" (fondo `#38BDF8`, `slate-950` bold, `shadow`). Toast "Lista creada con éxito".

**Decisiones del S5** (no fijadas por el prototipo):
- "Mis Listas" ordena las listas de más reciente a más antigua (como S-10 en la Biblioteca). "{x} de {n} mostradas" muestra siempre todas (no hay filtro).
- Las pestañas de §5.1 quedan fijas bajo la barra superior y la lista se desplaza debajo. "Top Ranking" abre el primer ranking de "Mis Listas" (el más reciente); sin rankings, toast "Todavía no tienes ningún ranking.". No se guarda el último ranking visitado (no hay DataStore hasta que haga falta).
- Las tarjetas no tienen descripción (no es un dato de la lista): debajo del título solo va el chip de restricción. Icono de la fila inferior: `theaters` (`live_tv` en listas de solo series).
- Sin listas (CU-12 A1): el banner y, debajo, icono `bookmarks` 48 dp, "Aún no has creado ninguna lista." y "+ Nueva Lista".
- Errores M-07 / M-08: bajo el campo, `body-sm` `secondary` con `error` 14 dp; el diálogo sigue abierto. Un fallo de escritura (M-25) es un toast y tampoco cierra el diálogo.
- "Renombrar" (menú ⋮ de la tarjeta y botón `edit` de la lista abierta) usa el mismo diálogo de formulario con el campo "Nombre de la colección", icono `edit`, título "Renombrar lista" y botón "Guardar". Toast "Lista renombrada"; con el mismo nombre se cierra sin aviso (CU-14 A2).
- "Eliminar" usa el diálogo destructivo (§3.6-2) con título "¿Eliminar la lista?" y el texto de M-12 con el nombre en negrita; botón "Eliminar lista". Toast "Lista eliminada".

---

## 6. Pantalla Lista abierta (`lista_ranking_top_10_hist_rico`)

Barra superior secundaria (§3.3), sin barra inferior. Contenido con padding 16 lateral y 40 inferior.

### 6.1 Cabecera (16 dp arriba, 8 abajo, separación 8)
- Fila 1:
  - Izquierda: insignia de tipo **sin corchetes** en MAYÚSCULAS `tracking-wider` `label-sm`, píldora padding 8×2: RANKING fondo `primary-container` 20 % texto `primary-container`; PRIORIDAD fondo `secondary-container` 20 % texto `secondary`; NORMAL fondo `surface-container-highest` texto `on-surface-variant`. Al lado, chip `surface-container-high` `label-sm` `on-surface-variant` con `check_circle` 13 dp FILL 1: "{n} títulos" (+ " • Solo Películas/Series" si hay restricción).
  - Derecha: dos botones circulares 36 dp fondo `surface-container` `shadow-sm` con iconos 18 dp: `edit` (renombrar) y `more_vert` (Eliminar lista).
- Título `headline-lg-mobile` `tracking-tight`.
- Descripción `body-sm` `on-surface-variant` (4 dp arriba). Texto fijo por tipo: Ranking "Orden por posición personal. La posición solo rige en esta lista."; Prioridad "Ordenada por prioridad: alta, media y baja."; Normal "Colección sin orden especial."
- **Acciones rápidas** (4 dp arriba, separación 8): dos botones `flex-1`, `rounded-xl`, padding 12×8, `label-md`, icono 18 dp:
  - "**Reordenar puestos**" (solo ranking): fondo `surface-container-high`, texto `on-surface`, icono `swap_vert` `primary-container`.
  - "**Añadir título**": fondo `primary-container`, texto `on-primary-container`, icono `add`.
- **Banner de modo reordenar** (aparece al pulsar "Reordenar puestos"): `rounded-xl`, padding 12, fondo `surface-container-highest`, texto `primary` `body-sm`, `info` 18 dp + "Usa las flechas para desplazar puestos ordinales." y botón "**Listo**" (`rounded-lg`, padding 12×4, `primary-container`/`on-primary-container` `label-sm`). Al pulsar Listo: toast "Ranking guardado".

### 6.2 Fila de ranking (lista con separación 8, 8 dp arriba)
- `article`: fila con separación 8, padding 8, `rounded-xl`, fondo `surface-container-low`, `shadow-sm`.
- **Columna de puesto** 32 dp de ancho, centrada:
  - `#1`, `#2`, `#3`: `headline-md` **extrabold** `tracking-tighter` en `primary-container`, con icono 14 dp FILL 1 debajo: `#1` `workspace_premium`, `#2` y `#3` `military_tech`.
  - `#4` en adelante: `headline-md` bold en `on-surface-variant`, sin icono. (Con números de 2–3 cifras se reduce el tamaño para no desbordar 32 dp.)
- **Póster** 56×80 dp (`w-14 h-20`), `rounded-lg`, `shadow-md`, fondo `surface-container`. Insignia "PELÍCULA"/"SERIE" arriba-izquierda: 9 sp bold MAYÚSCULAS, `rounded`, padding 4×0, fondo `surface-dim` 80 % con desenfoque.
- **Columna de datos**:
  - Línea 1: título `title-sm` **bold** truncado.
  - Línea 2: `body-sm` `on-surface-variant` truncado: géneros separados por " • " (en el prototipo: director; ver §12).
  - Línea 3 (4 dp arriba, separación 8): chip de nota `rounded`, padding 6×2, fondo `surface-container-high`, `star` 12 dp FILL 1 `primary-container` + valor `label-sm` `primary` (formato "10.0", "9.8"); sin nota: "Sin puntuar". Después, `label-sm` `on-surface-variant`: "Saga {nombre}" o nada.
- **Controles** (separación 4):
  - Columna de dos botones 28×28 `rounded` con `keyboard_arrow_up` / `keyboard_arrow_down` 18 dp `on-surface-variant`. Deshabilitado (subir en #1, bajar en el último): opacidad 30 %.
  - Botón 32 dp circular `remove_circle_outline` 18 dp `on-surface-variant` (al pulsar: `error`). Abre la hoja "Quitar" (§3.6-3).
- Las flechas son siempre visibles (como en el prototipo). Además se permite **mantener pulsado y arrastrar** la fila (sin asa visible). Toast tras mover: "Posición actualizada".

### 6.3 Variantes por tipo de lista
- **Prioridad**: sin columna de puesto ni flechas. En la línea 3, tras la nota, la píldora de prioridad (§2.4); pulsarla abre un menú Alta/Media/Baja. Orden Alta → Media → Baja.
- **Normal**: sin columna de puesto ni flechas; línea 3 solo con nota y saga.
- Lista vacía: caja `rounded-xl` `surface-container-lowest` padding 16 centrada con icono `playlist_add` 32 dp `primary-container`, "Esta lista está vacía" `title-sm` y el botón "Añadir título".
- Se ven como máximo 10 filas a la vez; el resto con scroll (requisito).

### 6.4 Pie de lista (24 dp arriba, separación 16)
- (El aviso con candado "Límite alcanzado (10 de 10)…" **no se implementa**, §12.)
- Botón centrado "**Eliminar esta lista**": texto `error` `label-md`, icono `delete_sweep` 18 dp, `rounded-xl`, padding 16×8, fondo transparente (pulsado `error-container` 20 %). Confirmación con el diálogo destructivo (§3.6-2): "¿Eliminar la lista "{nombre}"?" / "Sus títulos no se borrarán de tu biblioteca personal."

### 6.5 Hoja "Quitar" (§3.6-3)
- Icono `playlist_remove`; título "Quitar del ranking" / "Quitar de la lista"; nombre del título en `primary`.
- Texto: "¿Deseas desvincular este título de "{lista}"? Su ficha y estado en tu biblioteca general permanecerán intactos."
- Botones "Cancelar" y "Quitar". Toast: "Título quitado de la lista (permanece en biblioteca)".
- Si era el último elemento: tras quitarlo aparece el mismo diálogo con el texto "Has quitado el último elemento. ¿Quieres eliminar también la lista "{lista}"?" y botones "Mantener lista" / "Eliminar lista".

### 6.6 Diálogo "Añadir título" (estilo formulario, §3.6-1; prototipo en `gesti_n_de_listas`)
- Título "Añadir a {lista}" `headline-md` + `close`.
- "Buscar en tu biblioteca local" `label-sm` + campo `rounded-lg` fondo `surface-container`, `search` 18 dp, placeholder "Título..." `body-sm`.
- Resultados (alto máx. 192 dp, scroll, separación 4): fila padding 8 `rounded-lg` (pulsado `surface-container`): miniatura 32×44 `rounded`, título `title-sm` truncado, debajo `label-sm` `on-surface-variant` ("Película • {saga o géneros}"), y a la derecha `add_circle` 18 dp `sky-400`. Los perfiles que ya están o que incumplen la restricción aparecen con opacidad 40 % y sin icono.
- En ranking: tras elegir, una fila "Posición: [ n+1 ]" con − / + (mismo estilo que el stepper de §8). En prioridad: 3 segmentos Alta/Media/Baja (Media por defecto).
- Botón "Cerrar" (`rounded-lg`, `surface-container`, `on-surface-variant`).
- Toast: "«{título}» añadido al puesto #{n}" (ranking) o "«{título}» añadido a la lista".

**Decisiones del S5** (la elección de posición o prioridad y el reordenado llegan en el S6):
- En el S5 se añade siempre con los valores por defecto: al final del ranking (RF-39) y prioridad Media (S-16). La fila "Posición" y los segmentos Alta/Media/Baja del selector, las flechas ▲▼, el arrastre, "Reordenar puestos" y el menú de la píldora de prioridad son del S6. La columna de puesto con medallas (§6.2) y la píldora de prioridad (§2.4) ya se muestran.
- Póster sin carátula (§12): caja 56×80 `surface-container` con el icono del tipo 24 dp en `surface-container-highest` y la insignia "PELÍCULA"/"SERIE". La línea 2 son los géneros separados por " • " y se omite si no tiene. El botón "Quitar" mantiene el icono de 18 dp con zona táctil de 48 dp (§11).
- Selector "Añadir título": la búsqueda lleva el icono `search` dentro del campo. Sin resultados (o biblioteca vacía): M-13. Los perfiles no disponibles (ya están o la restricción no los admite) se ven al 40 % y sin `add_circle`, y anuncian "Ya está en la lista" / "No admitido en esta lista"; si se tocan igualmente, toast con M-10 o M-09 y no se añaden (CU-15 E1/E2).
- Hoja "Quitar" y aviso M-11: botón de acción `secondary-container` con texto `on-secondary-container` (§3.6-3). El aviso M-11 usa la misma hoja con el título "Eliminar lista", el nombre de la lista en `primary`, el texto de M-11 y los botones "Mantener lista" / "Eliminar lista".
- La lista abierta lleva un menú ⋮ en la cabecera con "Eliminar lista" (mismo efecto que el pie). Tocar una fila abre el perfil (CU-13 A3).
- Si la lista desaparece mientras está abierta (CU-13 E1), vuelve a "Mis Listas" con el toast "Esta lista ya no existe."; si desaparece el elemento que se iba a quitar (CU-16 E1), toast "Este título ya no está en la lista." y la lista se refresca.

**Decisiones del S6** (prioridad y ranking):
- Selector "Añadir título" en un ranking: bajo la búsqueda, fila "Posición" (`label-sm`) con la ayuda "Vacío = al final (#{n+1})" y, a la derecha, el stepper de §8 (− / campo 64 dp con placeholder "{n+1}" / +, teclado numérico). − / + parten de n+1 y no salen de 1..n+1 (el botón del extremo queda al 40 %). Tocar un título lo añade en esa posición (los demás bajan) y el campo vuelve a vacío. Una posición no válida (0, > n+1, texto) da M-24 en un toast y no añade (CU-15 E3). Así añadir al final sigue siendo de 2 pasos (RNF-09, T6.22).
- Selector en una lista de prioridad: fila "Prioridad" con 3 segmentos Alta / Media / Baja (mismo estilo que la restricción de §5.3), Media por defecto (S-16). El nivel elegido se aplica a todos los que se añadan mientras el selector esté abierto.
- Acciones rápidas del ranking: "Reordenar puestos" y "Añadir título" a partes iguales; en las demás listas solo "Añadir título" a todo el ancho. "Reordenar puestos" muestra el banner de §6.1; cada movimiento se guarda al hacerlo, así que "Listo" solo cierra el banner con el toast "Ranking guardado".
- Flechas ▲▼ siempre visibles (28×28, `rounded`); deshabilitadas al 30 % en el #1 (▲) y en el último (▼), sin efecto (CU-18 A1). Cada movimiento muestra "Posición actualizada".
- Arrastre: mantener pulsada cualquier parte de la fila (sin asa) con la biblioteca `sh.calvin.reorderable`; mientras se arrastra, las filas y los puestos se recolocan de forma provisional y, al soltar, se guarda el puesto nuevo. Cerca del borde superior o inferior la lista se desplaza sola (CU-18 A3). Soltar en el mismo puesto no escribe (A2). Si falla la escritura: M-25 y vuelve el orden guardado (E1). Tocar la fila sin arrastrar sigue abriendo el perfil.
- Píldora de prioridad: al tocarla, menú desplegable (`surface-container-high`) con Alta, Media y Baja (`label-md`; la actual en negrita con `check` 18 dp `primary`). Elegir el mismo nivel no cambia nada (CU-17 A1); otro nivel reordena la lista (Alta → Media → Baja; dentro del nivel, por antigüedad en la lista) con el toast "Prioridad actualizada".

---

## 7. Pantalla Detalle del perfil (`detalle_del_perfil`)

Barra superior secundaria con "Detalle Película" / "Detalle Serie". **Sin barra inferior.** Contenido con padding 16 lateral, 32 inferior y **separación 24** entre bloques.

### 7.1 Hero
- Tarjeta `rounded-xl` fondo `surface-container` `shadow-md`, recorta el contenido.
- **Fondo** de 224 dp de alto (`h-56`) con la carátula a pantalla completa y degradado de abajo arriba: `surface-container` → `surface-container` 60 % → transparente. Sin carátula: fondo `surface-container-high` con el icono `movie`/`tv` 64 dp `surface-container-highest` centrado.
- Arriba-izquierda (8 dp): píldoras `label-sm`, padding 8×2, fondo `surface-container-lowest` 80 % con desenfoque: "Película"/"Serie" (`on-surface`, `tracking-wide`) y, al lado, el estado ("Visto", "Viendo", "Pendiente") en `on-surface-variant` (en el prototipo: "2024 • 166 min", §12).
- Arriba-derecha (8 dp): botón 36 dp circular, mismo fondo, `more_vert` 20 dp: menú **Editar** / **Añadir a lista** / **Eliminar** (desplegable `surface-container-high`, opciones `body-md` con icono 20 dp `edit` / `delete`). "Editar" abre "Editar Medio" (§8, CU-02). "Añadir a lista" abre el selector de listas (§7.5).
- Bloque inferior (padding 16, sin padding superior), fila con separación 16 subida **64 dp** sobre el fondo (`-mt-16`), alineada abajo:
  - **Póster** 112×160 dp (`w-28 h-40`), `rounded-xl`, `shadow-xl`, fondo `surface-container-high`. (La insignia "4K" no se implementa.)
  - Columna (4 dp inferior): título `headline-lg-mobile` bold truncado `leading-tight`; línea `body-sm` `on-surface-variant` truncada con los géneros (en el prototipo "Dir. …"); fila (4 dp arriba): `star` 16 dp FILL 1 + nota `label-md` en `primary`, y "• #{n} en {lista de ranking}" `label-sm` `outline` si el perfil está en algún ranking (mejor posición). Sin nota: "Sin puntuar".

### 7.2 Estado de visualización
- Etiqueta de sección "ESTADO DE VISUALIZACIÓN" (separación 4).
- Rejilla de 3: contenedor `rounded-xl` fondo `surface-container` padding 4 separación 4 `shadow-sm`. Botones `rounded-lg`, padding 8×10, `label-md`, icono 18 dp + texto, separación 6:
  - **Pendiente** `bookmark_add`, **Viendo** `play_circle`, **Visto** `check_circle`.
  - Activo: fondo `primary-container`, texto `on-primary-container` semibold, `shadow-sm`, icono FILL 1. Inactivo: `on-surface-variant`, icono FILL 0.

### 7.3 Tarjeta de nota
- `rounded-xl` fondo `surface-container` padding 16 `shadow-sm`, fila con extremos.
- Izquierda (separación 16):
  - Caja 56 dp `rounded-xl` `surface-container-high` `shadow-inner`: `grade` 24 dp FILL 1 `primary` + "Tu nota" `label-sm` `on-surface-variant`.
  - Columna: nota `display-lg` **extrabold** `primary` `leading-none` + "/ 10" `title-sm` medium `outline` (alineados por la base, separación 4); debajo, la etiqueta del rango `body-sm` `on-surface-variant`: 9–10 "Imprescindible", 7–8.9 "Muy buena", 5–6.9 "Correcta", 3–4.9 "Floja", 0–2.9 "Mala" (propuesta derivada de los chips de Biblioteca). Sin nota: "—" y "Sin puntuar".
- Derecha: botón "**Ajustar**" `rounded-lg` padding 16×8 fondo `surface-container-high` (pulsado `surface-bright`) `label-md` con `edit` 18 dp.
- **Editor desplegable** (bajo la tarjeta, al pulsar Ajustar): `rounded-xl` fondo `surface-container-high` padding 16 `shadow-md`, separación 8:
  - Fila: "Calificar (saltos de 0.1)" `label-md` semibold + valor `title-md` bold `primary`.
  - Slider 0–10 paso 0.1, pista `surface-container` 8 dp `rounded-lg`, color `primary-container`.
  - Marcas `label-sm` `on-surface-variant`: "0.0 (Pésima)", "5.0", "10.0 (Perfecta)".
  - Añadido por requisito (RF-13): enlace "Quitar puntuación" `label-md` `error`.
  - Añadido por CU-03 (confirmar, E1, T3.12): el valor de la cabecera es un **campo editable** (teclado decimal, 64 dp, `title-md` bold `primary`, alineado a la derecha, sin fondo; al enfocar `surface-container`) sincronizado con el slider; con un valor inválido aparece M-03 (`body-sm` `secondary` con `error` 14 dp) y "Guardar" se deshabilita (50 %). Última fila: "Quitar puntuación" a la izquierda y botón "**Guardar**" a la derecha (`rounded-lg`, padding 16×6, `primary-container` / `on-primary-container`, `label-md` bold). Guardar el campo vacío quita la nota. Guardar o quitar cierra el editor; "Ajustar" lo abre y lo cierra.

### 7.4 Ficha Técnica Local
- Tarjeta `rounded-xl` `surface-container` padding 16 separación 16 `shadow-sm`.
- Cabecera: `movie_filter` 20 dp `primary` + "Ficha Técnica Local" `title-md` bold.
- **Colección / Saga** (`label-sm` `on-surface-variant`, separación 4): botón fila `rounded-lg` padding 8 fondo `surface-container-high` (pulsado `surface-bright`): `auto_stories` 20 dp `tertiary`, "Saga {nombre}" `title-sm` semibold y "{n} películas/series guardadas en tu archivo" `body-sm` `on-surface-variant`; `arrow_forward_ios` 20 dp a la derecha. Abre la Biblioteca filtrada por esa saga. Sin saga: el mismo bloque con "Sin saga asociada" y sin flecha.
- **Géneros** (separación 6): chips con salto de línea (separación 4), píldora padding 8×4, fondo `surface-container-high`, texto `on-surface` `label-sm`, icono 14 dp `primary` según el género (§7.8).
- **Idiomas** (el prototipo dice "Idiomas y subtítulos disponibles"; se usa "Idiomas"): chips `rounded-md`, padding 8×4, fondo `surface-container-lowest`, texto `on-surface-variant` `label-sm`, icono `volume_up` 14 dp.

### 7.5 En tus Listas
- Tarjeta igual que §7.4. Cabecera: `folder_special` 20 dp `primary` + "En tus Listas" `title-md` bold; a la derecha botón de texto "**+ Añadir a lista**" `label-md` `primary` con `add` 16 dp.
- Filas (separación 4): `rounded-lg` padding 8 fondo `surface-container-high`, extremos:
  - Izquierda (separación 8): punto 10 dp (ranking `secondary-container`, prioridad `primary`, normal `on-surface-variant`); nombre `title-sm` semibold; debajo `label-sm` `on-surface-variant`: ranking "Posición actual: #{p} de {n} títulos", prioridad "Prioridad {nivel} en esta lista", normal "{n} títulos".
  - Derecha: insignia `rounded` padding 8×2 fondo `surface-container-lowest` texto `primary` `label-sm` semibold: "Ranking #{p}", "Prioridad {Alta/Media/Baja}", "Normal".
- Sin listas: texto `body-sm` `on-surface-variant` "Aún no está en ninguna lista".
- **Añadir a lista** (S5, CU-15 desde el perfil): diálogo de formulario (§3.6-1) "Añadir a lista" con todas las listas (punto del tipo, nombre, "{Tipo} • {n} títulos" y `add_circle` 18 dp `sky-400`). Tocar una lista normal añade el perfil y cierra el diálogo (2 pasos, RNF-09); toast "«{título}» añadido a «{lista}»". Desde el S6, tocar un ranking o una lista de prioridad la despliega (caja `rounded-lg` `surface-container`) con la fila "Posición" o los segmentos de prioridad de §6.6 (por defecto al final y Media) y el botón "Añadir" (`rounded-lg`, `primary-container` / `on-primary-container`, `label-md`); "Añadir" o tocar otra vez la lista la añade y cierra (3 pasos, CU-15 paso 3). Las que ya lo contienen o no lo admiten se ven al 40 % sin icono; tocarlas da M-10 o M-09. Sin listas: "Aún no has creado ninguna lista.". Tocar una fila de "En tus Listas" abre esa lista.

### 7.6 Mi Crítica y Notas
- Tarjeta `rounded-xl` `surface-container` padding 16 separación 8.
- Cabecera: `rate_review` 20 dp `primary` + "Mi Crítica y Notas" `title-md` bold; a la derecha dos botones de icono 18 dp con padding 4 `rounded-md`: `edit_note` y `delete_outline` (pulsado `error`).
- Caja `rounded-xl` padding 16 fondo `surface-container-lowest`: texto `body-md` **cursiva** `leading-relaxed` `on-surface`, entre comillas. Pie (8 dp arriba) `label-sm` `on-surface-variant`: a la derecha "Guardado: {fecha}" (fecha de la última edición). El texto de la izquierda del prototipo ("Visto en Cine Gran Vía (Sala 1)") no se implementa (§12).
- Vacía: "Sin reseña redactada todavía." en `on-surface-variant` cursiva.
- Editar abre una hoja inferior (§3.6-3) con la cabecera de la tarjeta (`rate_review` + "Mi Crítica y Notas"), un campo multilínea del estilo de §8.9 (6 líneas, alto 160–320 dp con desplazamiento interno) y, debajo, el contador "{n} / 10000" `label-sm` `on-surface-variant` a la derecha; botones "Cancelar" (`surface-container-highest`) y "Guardar" (`primary-container` / `on-primary-container`). No deja escribir más de 10 000 caracteres (S-06). Guardar el texto vacío borra la crítica.
- `delete_outline` pide una confirmación breve con el diálogo de §3.6-2: "¿Borrar la crítica?" / "Se borrará tu crítica de este título." / "Borrar" / "Cancelar". Sin crítica, el botón está deshabilitado (40 %).
- `edit_note` y `delete_outline` llevan `contentDescription` ("Editar crítica", "Borrar crítica").

### 7.7 Eliminar
- Bloque (8 dp arriba, separación 8):
  - Botón a ancho completo `rounded-xl` padding 16×14 fondo `error-container` texto `on-error-container` `title-sm` semibold, `delete_forever` 20 dp, `shadow-sm`, pulsado `scale(0.98)`. Texto "**Eliminar de mi catálogo**".
  - Nota centrada `label-sm` `outline`: "Esta acción es irreversible y eliminará calificaciones, notas y registros de tus listas."
- Diálogo destructivo (§3.6-2): "¿Eliminar del catálogo?" / texto de **M-06** con el nombre en negrita ("¿Eliminar «**{nombre}**»? Se quitará también de todas tus listas. Esta acción no se puede deshacer.") / "Sí, eliminar definitivamente" / "Cancelar". El prototipo dice "Se quitará "{nombre}" de tu base de datos local y de las {n} listas personalizadas asociadas."; se usa el catálogo de mensajes (T3.23).
- Al confirmar: se borra (CU-06) y se vuelve atrás con el toast canónico (§3.5) "Perfil eliminado". Si falla, toast con M-25 y el perfil sigue.
- Cambios del detalle que fallan al guardarse (estado, nota, crítica, eliminar): toast canónico con M-25; la pantalla sigue mostrando lo guardado (el selector de estado no cambia).

### 7.8 Iconos de género
Del prototipo: Ciencia Ficción `rocket_launch`, Aventura `explore`, Drama `theater_comedy`. Propuesta para el resto: Acción `sports_martial_arts`, Comedia `sentiment_very_satisfied`, Terror `skull`, Suspense `visibility`, Animación `animation`, Fantasía `auto_fix_high`, Documental `videocam`, Crimen `local_police`, Romance `favorite`, Historia `history_edu`, Thriller `psychology`. Género sin icono asignado: `movie`.

---

## 8. Pantalla Añadir / editar perfil (`a_adir_nuevo_perfil`)

Barra superior secundaria con "Añadir Medio" ("Editar Medio" al editar). **Sin barra inferior.** Formulario con padding 16 lateral, 40 inferior y **separación 24**.

**Al editar (CU-02)** es el mismo formulario con los datos actuales del perfil (también estado, puntuación y comentario): cabecera "Editar perfil" / "Actualiza los datos de tu colección local"; el tipo, deshabilitado (50 %, S-03); el indicador "Disponible" no cuenta el propio perfil (cambiar solo mayúsculas o tildes es válido) y M-01 aparece en cuanto se vacía el nombre. Guardar sin cambios vuelve sin escribir; al guardar, toast de éxito "Cambios guardados" y vuelta al detalle. Cancelar con cambios: "¿Descartar los cambios?" / "Se perderán los cambios que has hecho." / "Descartar" / "Seguir editando".

1. **Cabecera** (4 dp arriba y abajo): "Añadir nuevo perfil" `headline-md` bold + "Registra y califica en tu colección local" `body-sm` `on-surface-variant`. A la derecha botón "**Cancelar**": píldora padding 16×4, fondo `surface-container-high`, `label-md`.
2. **TIPO DE PRODUCCIÓN \***: etiqueta de sección con asterisco `secondary`. Segmentado de 2: contenedor `rounded-xl` fondo `surface-container-lowest` padding 4; botones `rounded-lg` padding vertical 8, `label-lg`, icono 18 dp (`movie`, `tv`), separación 4. Activo: `primary-container` / `on-primary-container` **bold** `shadow-md`. Inactivo: `on-surface-variant`. En edición está deshabilitado (S-03).
3. **Nombre del título \***: etiqueta `label-md` en `on-surface` (no MAYÚSCULAS) con asterisco; a la derecha, mientras se escribe, indicador `label-sm`: "✓ Disponible" (`check_circle` 14 dp, `primary`) o "⊗ Ya registrado" (`cancel` 14 dp, `secondary`).
   - Campo: icono `title` 20 dp `on-surface-variant` a 16 dp de la izquierda, padding izquierdo 44, derecho 16, vertical 8, `rounded-xl`, fondo `surface-container-low` (§3.7), placeholder "Ej. Blade Runner 2049".
   - Ayuda `body-sm` `on-surface-variant` con `info` 14 dp `tertiary`: "El sistema comprueba que no exista ya con este nombre y tipo."
4. **ESTADO DE VISUALIZACIÓN \***: rejilla de 3 (separación 4) de mosaicos `rounded-xl` padding 8×4, columna icono 20 dp + `label-md`, separación 4: **Pendiente** `schedule`, **Viendo** `play_circle`, **Visto** `check_circle`. Activo: fondo `surface-container-high`, texto `primary`, `shadow-sm`. Inactivo: fondo `surface-container-lowest`, texto `on-surface-variant`. **Por defecto: Pendiente.**
5. **Puntuación Personal**: tarjeta `rounded-xl` fondo `surface-container-low` padding 16 separación 8.
   - Cabecera: `star` 22 dp FILL 1 `primary` + "Puntuación Personal" `label-lg` bold; a la derecha etiqueta "Opcional" `label-sm` `rounded` padding 4×2 fondo `surface-variant` texto `on-surface-variant`. **Siempre "Opcional"** (en el prototipo pasa a "Obligatorio *" en Viendo/Visto, §12).
   - Aviso `rounded-lg` padding 4 fondo `surface-container` texto `tertiary` `body-sm` con `lightbulb` 16 dp: "La puntuación es opcional en cualquier estado."
   - Fila (4 dp arriba, extremos, separación 16):
     - **Stepper**: contenedor `rounded-xl` fondo `surface-container-lowest` padding 4 separación 4; botones 40×40 `rounded-lg` fondo `surface-container` (pulsado `surface-bright`) con `remove`/`add` 18 dp; campo central 64 dp centrado `headline-md` bold, placeholder "Ej. 8.5", teclado decimal.
     - Derecha: valor grande `display-lg` extrabold `primary` ("—" si está vacío) + "/ 10" `label-md` `on-surface-variant`.
   - Slider a ancho completo, 0–10 paso 0.1, pista `surface-container` 8 dp, color `#38BDF8`.
   - Mensaje (alto mínimo 20 dp) `body-sm` con icono 14 dp:
     - Neutro, también con el campo vacío (`on-surface-variant`, `info`): "Introduce un valor entre 0.0 y 10 (máx 1 decimal)"
     - Válido (`primary` semibold, `check_circle`): "Puntuación válida."
     - Inválido (**`secondary`**, `error`): el texto de M-03 (el prototipo dice "Debe estar entre 0.0 y 10 con máximo un decimal."; se usa el catálogo de mensajes, T2.11).
   - − / + con el campo vacío parten de **5.0** (como en el prototipo); los límites son 0.0 y 10. El 10 se muestra "10" y el resto con un decimal.
6. **SAGA / UNIVERSO CINEMATOGRÁFICO**: etiqueta de sección; a la derecha enlace "**+ Crear nueva saga**" `label-sm` `primary` con `add` 14 dp.
   - Desplegable (§3.7) con "Ninguna / Obra individual" + sagas registradas.
   - **Caja en línea** al pulsar "Crear nueva saga" (no es un diálogo): `rounded-xl` fondo `surface-container` padding 8, 4 dp arriba; "Nueva saga local" `label-sm` bold `primary`; fila con campo `rounded-lg` fondo `surface-container-lowest` `body-sm` placeholder "Nombre de la saga...", botón "Guardar" (`rounded-lg`, `primary-container`/`on-primary-container`, `label-sm` bold) y `close` 16 dp. Al guardar, la saga queda seleccionada (y se registra al guardar el perfil, CU-10 E4). Debajo de la fila, M-05 o M-04 en `secondary` con `error` 14 dp; con M-04, enlace "Elegir la existente" `label-sm` `primary` (S-13).
7. **GÉNEROS**: etiqueta de sección + contador a la derecha `label-sm` `on-surface-variant` ("0 seleccionados", "1 seleccionado"…). Chips con salto de línea (separación 4): píldora padding 8×4 `label-md`. Sin seleccionar: fondo `surface-container-low`, texto `on-surface-variant`. Seleccionado: fondo `primary-container`, texto `on-primary-container` **bold**, `shadow-sm` (sin ✓). Lista predefinida (primeros del prototipo): Acción, Drama, Comedia, Ciencia Ficción, Terror, Suspense, Animación, Fantasía, Documental, + Crimen, Romance, Historia, Thriller, Aventura.
8. **IDIOMAS DISPONIBLES**: igual que géneros. Lista predefinida: Español, Inglés, Francés, Japonés, Alemán, Coreano, + Italiano, Portugués, Chino.
9. **COMENTARIOS / CRÍTICA PERSONAL (OPCIONAL)**: etiqueta de sección con "(Opcional)" en `outline` peso normal. Área de texto 4 líneas, `rounded-xl`, padding 16, fondo `surface-container-low` (§3.7), sin redimensionar, placeholder "Notas cinematográficas, fotografía, dirección, momentos destacados...".
10. **Guardar Perfil** (16 dp arriba): botón a ancho completo, **píldora**, padding vertical 16, `label-lg` bold, `save` 20 dp.
    - Deshabilitado (nombre vacío, duplicado o nota inválida): fondo `surface-container-highest`, texto `on-surface-variant`, opacidad 50 %, `shadow-md`.
    - Habilitado: fondo `primary-container`, texto `on-primary-container`, `shadow-lg`.
    - Al guardar: toast de éxito (§3.5) "¡Perfil guardado correctamente en CineTrack!" y vuelta atrás.
    - Fallo de escritura (CU-01 E4): M-25 encima del botón, `body-sm` `secondary` con `error` 14 dp.
11. **Cancelar con datos** (botón "Cancelar", atrás de la barra o del sistema): diálogo destructivo (§3.6-2) "¿Descartar el perfil?" / "Se perderán los datos que has introducido." / "Descartar" / "Seguir editando". Sin datos, vuelve atrás directamente.
12. **Slider**: el `<input type="range">` del prototipo ocupa 20 dp de alto (pista 8 dp, pulgar circular 18 dp); la zona táctil sigue siendo de 48 dp (§11).

---

## 9. Pantalla Buscar (`b_squeda_y_copia_de_seguridad`, pestaña "Filtros Cine")

Barra superior principal con "Buscar" y barra inferior con Buscar activa.

### 9.1 Pestañas (padding 16×8, no fijas)
- Contenedor **píldora**, fondo `surface-container`, padding 4, `shadow-inner`, rejilla de 2.
- Botones píldora, padding vertical 8, icono 18 dp + `label-md`: "**Filtros Cine**" `filter_alt`, "**Datos Locales**" `storage`. Activo: `primary-container`/`on-primary-container` `shadow-sm`. Inactivo: `on-surface-variant`.

### 9.2 Filtros (padding 16 lateral, separación 16)
1. **Buscador**: píldora fondo `surface-container-high` padding 16×10 `shadow-md`; `search` 22 dp `primary` (8 dp a su derecha); campo `body-md` placeholder "Buscar por título..." en `on-surface-variant`; botón `cancel` 20 dp a la derecha cuando hay texto. Debajo (4 dp, padding lateral 8, `label-sm`): "Búsqueda insensible a tildes y mayúsculas" `on-surface-variant` y, a la derecha, "{n} coincidencias" en `primary`.
2. **TIPO DE PRODUCCIÓN**: chips píldora padding 16×6 `label-md` `shadow-sm`: "Todas" (por defecto), "Solo Películas", "Solo Series". Activo `primary-container`/`on-primary-container`; inactivo `surface-container`/`on-surface-variant`.
3. **Rango de Calificación**: tarjeta `rounded-xl` `surface-container` padding 16 `shadow-md` separación 8.
   - Cabecera: `star` 20 dp FILL 1 `primary` + "Rango de Calificación" `title-sm`; a la derecha píldora `surface-container-highest` padding 8×2: mínimo `label-md` bold `primary` + "-" `label-sm` + máximo igual.
   - Marcas `label-sm` `on-surface-variant`: 0.0 · 5.0 · 10.0.
   - **Slider de rango con dos pulgares**, paso 0.1, pista `surface-container-highest` 8 dp píldora, tramo y pulgares `primary` (el prototipo tiene un solo pulgar, §12).
   - Pie `label-sm`: izquierda "Desliza para fijar mínimo y máximo" `on-surface-variant`; derecha "★ Excluye títulos sin puntuar" `tertiary` (solo si el rango no es 0.0–10).
   - Por defecto 0.0–10 = filtro inactivo.
4. **GÉNEROS CINEMATOGRÁFICOS**: etiqueta de sección + "Deseleccionar" `label-sm` `primary`. Chips con salto de línea (separación 8, 4 dp arriba): **`rounded-lg`**, padding 12×6, `label-md`, `shadow-sm`. Sin seleccionar `surface-container`/`on-surface-variant`. Seleccionado: fondo `surface-container-high`, texto `primary` y prefijo "✓ ".
5. **Idioma y Saga**: rejilla de 2 (separación 8). Cada uno: tarjeta `rounded-xl` `surface-container` padding 8 `shadow-sm` separación 4; etiqueta `label-sm` `on-surface-variant` con icono 16 dp `tertiary` ("Idioma" `translate`, "Saga / Franquicia" `collections_bookmark`); desplegable `rounded-lg` fondo `surface-container-high` padding 8 `body-sm`, primera opción "Todos los idiomas" / "Cualquier saga".
6. **ESTADO EN TU COLECCIÓN**: rejilla de 3 (separación 8), selección múltiple. Mosaico `rounded-xl` padding 10 `shadow-sm`, icono 20 dp (4 dp abajo) + `label-sm`: Pendiente `hourglass_top`, Viendo `play_circle`, Visto `check_circle`. Activo: `surface-container-high`, texto e icono `primary`, bold. Inactivo: `surface-container`/`on-surface-variant`.
7. **Acciones** (rejilla de 3, separación 8, 8 dp arriba):
   - "Limpiar" (1 columna): `rounded-xl` padding vertical 12, `surface-container-high` `on-surface`, `restart_alt` 18 dp + `label-md`, `shadow-md`.
   - "Aplicar Filtros ({n})" (2 columnas): `rounded-xl` padding 16×12, `primary-container`/`on-primary-container` `label-lg`, `manage_search` 22 dp, `shadow-lg` negra 30 %.
8. **Resultados** (8 dp arriba, separación 8):
   - Cabecera: "Resultados" `title-sm` + "{n} coincidencias" `label-sm` `primary`.
   - **Tarjeta de resultado**: fila separación 16, padding 8, `rounded-xl` `surface-container` `shadow-md`.
     - Póster 64×96 dp `rounded-lg` `shadow-sm`.
     - Columna: fila de chips (4 dp abajo, separación 6) — tipo (píldora padding 6×2 `surface-container-highest` `on-surface` `label-sm`) y nota (píldora `primary-container` 20 % texto `primary` bold "★ 8.8"; sin nota se omite); título `title-sm` truncado; línea `body-sm` `on-surface-variant` truncada "{saga} • {géneros}"; chip de estado (8 dp arriba) según §2.3.
     - `chevron_right` 20 dp `on-surface-variant` con padding 8.
   - Sin resultados: caja centrada con `search_off` 40 dp `on-surface-variant`, "No se han encontrado resultados." `title-sm` y "Prueba a quitar algún filtro." `body-sm`.

**Decisiones del S7** (búsqueda):
- Los resultados se actualizan al cambiar cualquier criterio y cuando cambian los datos (CU-20 paso 3). "Aplicar Filtros ({n})" muestra el recuento actual, cierra el teclado y desplaza la pantalla hasta "Resultados"; la tecla de búsqueda del teclado hace lo mismo.
- Los dos valores de la píldora del rango ("7.0 - 10") son campos editables (`label-md` bold `primary`, 30 dp de ancho, teclado decimal) sincronizados con el slider. Un campo vacío es "sin límite" (0.0 / 10). Un valor no válido da M-03 y uno mínimo mayor que el máximo M-14 (CU-20 E2, E3): en lugar de los resultados aparece una caja `rounded-xl` `surface-container` padding 16 con `error` 20 dp y el mensaje en `error` `body-sm`, sin recuento; no se busca.
- Con el rango en 0.0–10 el filtro está inactivo y los perfiles sin nota aparecen; cualquier otro rango los excluye y muestra "★ Excluye títulos sin puntuar".
- Idioma y saga: desplegable de una opción (menú `surface-container-high`, `body-sm`, la elegida en negrita). Los géneros del catálogo completo (§8.7), varios a la vez (basta uno, S-19); "Deseleccionar" los quita todos. Estado: mosaicos de selección múltiple (S-18).
- Póster de la tarjeta de resultado: caja 64×96 `rounded-lg` `surface-container-highest` con el icono de tipo (`movie`/`tv`) 28 dp `sky-400`, como en la Biblioteca. La línea bajo el título es "{saga} • {géneros separados por comas}" (cada parte solo si existe). Chip de estado según §2.3, sin icono.
- Texto bajo el buscador: "Búsqueda insensible a tildes y mayúsculas" y "{n} coincidencias" (sin "Modo local SQLite", §12). Con la escala tipográfica mayor, las ayudas de buscador y slider pueden ocupar dos líneas en lugar de truncarse.
- La pestaña "Datos Locales" se ve y se puede elegir; su contenido (§10) llegó en el S8.

---

## 10. Datos Locales (pestaña de Buscar) y Ajustes

En el prototipo la copia de seguridad es la **pestaña "Datos Locales" de Buscar**; el destino "Ajustes" no tiene pantalla propia. Se mantiene la pestaña tal cual y el destino **Ajustes** muestra el mismo contenido (barra superior "Ajustes", sin pestañas).

Contenido (padding 16 lateral, separación 16):
1. **Banner de privacidad**: `rounded-xl` `surface-container` padding 16 `shadow-md`, fila separación 8: `verified_user` 24 dp `primary`; "Privacidad Total: Modo 100% Local" `title-sm` + `body-sm` `on-surface-variant` (4 dp arriba) "Tus catálogos, notas, listas y puntuaciones viven únicamente en la base de datos interna de este dispositivo. Cero telemetría externa."
2. **MÉTRICAS DE LA BASE DE DATOS** (etiqueta de sección): rejilla de 3 (separación 8). Tarjeta `rounded-xl` `surface-container` padding 16 `shadow-md` centrada: icono 24 dp (4 dp abajo) + valor `headline-md` bold + etiqueta `label-sm` `on-surface-variant`:
   - `movie_filter` `primary` · {n} · "Títulos Guardados"
   - `playlist_add_check` `tertiary` · {n} · "Listas Creadas"
   - `sd_card` `primary` · {tamaño} · "Almacenamiento"
3. **Distribución de Registros**: tarjeta `rounded-xl` `surface-container` padding 16 `shadow-md`, separación 8. Título `title-sm`. Dos barras (separación 8): fila `label-sm` `on-surface-variant` "Películas ({n} entradas)" + porcentaje bold `primary` / "Series ({n} entradas)" + porcentaje bold `tertiary`; barra de 8 dp píldora, pista `surface-container-highest`, relleno `primary` / `tertiary`. Pie (4 dp arriba) `label-sm` con `sync_saved_locally` 16 dp `primary`: "Última modificación: {hoy/fecha} a las {hh:mm}".
4. **ACCIONES DE RESPALDO LOCAL** (etiqueta de sección, separación 8):
   - **Tarjeta Exportar** (`rounded-xl` `surface-container` padding 16 `shadow-md`, separación 8):
     - Fila: círculo padding 10 `primary-container`/`on-primary-container` con `file_download` 24 dp; "Exportar copia de seguridad" `title-sm` + "Genera un paquete JSON con tus títulos y listas." `body-sm`.
     - Fila `rounded-lg` `surface-container-high` padding 8: nombre de archivo `label-sm` **monoespaciado** `on-surface-variant` ("cinetrack_backup_AAAA-MM-DD.json") y tamaño estimado `label-sm` `tertiary` ("~420 KB").
     - Botón a ancho completo `rounded-xl` padding vertical 12 `primary-container`/`on-primary-container` `label-lg` `shadow-md`, `download_for_offline` 20 dp: "Generar y Guardar Archivo Local".
   - **Tarjeta Importar** (igual): círculo `surface-container-highest`/`on-surface` con `file_upload`; "Importar copia de seguridad" + "Añade a tu colección los títulos y listas de un archivo .JSON. No se borra nada." (la importación **fusiona**, CU-22). Botón `rounded-xl` padding vertical 12 `surface-container-high` `on-surface` `label-lg` `shadow-sm`, `folder_open` 20 dp: "Seleccionar archivo .JSON".
5. **Aviso en línea** tras exportar/importar: `rounded-xl` padding 16 `surface-container-highest` `shadow-xl`, texto `primary` `body-sm`, `check_circle` 22 dp: "Copia guardada con éxito en {carpeta}" / resumen de la importación.

**Decisiones del S8** (copia de seguridad):
- **Formato** (`kotlinx.serialization`, JSON legible con sangría): `{"app": "CineTrack", "version": 2, "fecha", "sagas": [nombres], "perfiles": [...], "listas": [...]}`. Cada perfil lleva nombre, tipo, estado, `puntuacion` (8.5; null sin nota), comentario y su fecha de edición, saga (por nombre), géneros e idiomas (por nombre) y fecha de alta. Cada lista lleva nombre, tipo, restricción, fecha de alta y sus elementos en el orden de su tipo, que apuntan al perfil por nombre y tipo, con `posicion` (ranking), `prioridad` (prioridad) y fecha de alta en la lista. Sin ids internos: así la copia se puede fusionar con otros datos (S-20). La carátula no se exporta (es un archivo interno, no un dato portable).
- **Versiones del formato**: la 1 guardaba la puntuación en décimas (`puntuacionDecimas`: 85); la 2 (actual) la guarda como en la interfaz. Un archivo de una versión anterior se migra al leerlo (CU-22 A2); uno de una versión posterior da M-18. En `app/src/test/resources/copias/` está la copia de referencia de cada versión (T8.14, T8.17): un cambio de formato sube la versión, añade su migración y su copia de referencia, y las anteriores se siguen importando.
- **Validación previa completa** (CU-22 paso 2) antes de tocar la base de datos. M-17 sin detalle si no es JSON, falta `"app": "CineTrack"` o la estructura no es la esperada; M-17 con una segunda línea de detalle (`on-surface-variant`) si un dato es inconsistente: puntuación no válida (mismas reglas que el formulario, S-04), perfil o lista repetidos (nombres normalizados), elemento que apunta a un perfil que no está en la copia, perfil repetido en una lista, restricción de la lista, género o idioma fuera del catálogo, falta la posición o la prioridad, nombre vacío o comentario de más de 10 000 caracteres.
- **Fusión** (CU-22): el plan se calcula en seco para M-19 y se recalcula y aplica dentro de una sola transacción, así M-19 y M-20 coinciden. Lo que se crea conserva las fechas de la copia (ida y vuelta idéntica); los elementos con los que se amplía una lista van detrás de los que ya tenía. Si una lista choca de nombre con otra de distinto tipo o restricción se prueba "{nombre} (importada)", "(importada 2)"…: se amplía la primera compatible o se crea con el primer nombre libre, para que importar dos veces el mismo archivo no cree otra lista (idempotencia).
- **Selector del sistema** (Storage Access Framework): exportar abre `CreateDocument` con el nombre `cinetrack_backup_AAAA-MM-DD.json`; importar abre `OpenDocument` (JSON, texto o binario, porque muchos gestores no marcan los `.json`). Cancelar no hace nada (A1). Si la escritura falla, se borra lo escrito (CU-21 E1).
- **Progreso** (CU-22 E5): mientras se exporta, se lee o se importa, en el sitio del aviso en línea aparece una tarjeta igual con el texto de la operación (`primary` `body-sm`) y una barra de progreso indeterminada de 4 dp (`primary` sobre `surface-container`); los dos botones quedan deshabilitados (opacidad 50 %). El trabajo se hace fuera del hilo principal: se puede seguir navegando. El aviso o el progreso se desplazan a la vista al aparecer.
- **Errores**: el aviso en línea usa el icono `error` 22 dp y texto `error` (M-16, M-17, M-18, M-21); los de éxito, `check_circle` y `primary` (M-15, M-20). Con el selector del sistema no se conoce la carpeta, así que el éxito es M-15 sin carpeta.
- **M-19**: hoja inferior (§3.6-3) con círculo `primary-container` 20 % e icono `file_upload` `primary`, título "Importar copia de seguridad" `title-md` bold, texto de M-19 `body-md` y botones "Cancelar" / "Importar".
- **Métricas**: "Títulos Guardados" y "Listas Creadas" se cuentan en la base de datos; "Almacenamiento" es el tamaño de sus archivos en disco ("571 KB", "1.4 MB"; "—" mientras se lee). Las tres tarjetas tienen la misma altura y su etiqueta reduce el tamaño (mínimo 9 sp) en vez de partir "Almacenamiento". Los porcentajes de la distribución suman 100. "Última modificación" es la de los archivos de la base de datos ("hoy" o la fecha `dd/MM/aaaa`, y la hora `HH:mm`). El tamaño estimado de la tarjeta Exportar es el del archivo que se generaría ahora.
- **Ajustes** muestra el mismo contenido que la pestaña, con 8 dp de separación bajo la barra superior.

---

## 11. Estados, accesibilidad y adaptación

- **Pulsado**: el fondo sube un nivel (`surface-container` → `surface-container-high` → `surface-bright`) y escala según §2.9. **Deshabilitado**: opacidad 30–50 %. **Foco**: §3.7.
- **Vacíos**: biblioteca vacía (icono `video_library` 48 dp, "Todavía no hay títulos. Añade el primero." y botón "Añadir"), lista vacía (§6.3), sin resultados (§9.2-8).
- **Accesibilidad**: `contentDescription` en todos los iconos y en los botones solo-icono; objetivos táctiles ≥ 48 dp (los botones de 28 dp del ranking amplían su zona táctil sin cambiar su tamaño visual); el estado nunca se indica solo por color (siempre icono + texto); respeta el tamaño de fuente del sistema.
- **Orientación**: la app está bloqueada en vertical (`screenOrientation="portrait"`, decisión del usuario en el S9, T9.09). En Android 16 o superior el sistema ignora el bloqueo en pantallas de 600 dp o más (tablets, plegables abiertos); allí se aplica la regla de tablet de abajo.
- **Tamaños**: diseño base 411 dp de ancho (capturas). En < 360 dp las rejillas de 3 mantienen 3 columnas con textos truncados. En tablet (≥ 600 dp) el contenido se centra con ancho máximo 600 dp.

**Decisiones del S9** (adaptación, accesibilidad y cierre):
- **Fuente del sistema grande** (T9.08): las etiquetas de una línea en huecos fijos (barra inferior, pestañas de Buscar, etiquetas de métricas, puesto del ranking) usan `TextoAjustable`: el tamaño del estilo, reducido solo si no cabe, hasta 9 sp reales. Los botones de acción con texto largo ("Generar y Guardar Archivo Local", "Reordenar puestos") pasan a dos líneas en vez de cortarse. La barra inferior tiene alto mínimo de 64 dp (no fijo) para que la etiqueta de "Añadir" no se recorte, y cada destino ocupa como mucho un quinto del ancho. La cabecera "Catálogo de Medios" pasa el recuento "Mostrando…" a la línea siguiente si no caben juntos. Los títulos de tarjetas siguen truncándose con puntos suspensivos.
- **Zonas táctiles** (T9.10): Compose amplía a 48×48 dp la zona táctil de lo que es más pequeño (chips, flechas del ranking) sin cambiar su tamaño visual; la revisión automática lo comprueba en todas las pantallas.
- **Contraste** (T9.11): todas las combinaciones de texto y fondo cumplen AA (4.5:1) salvo la del prototipo para "Quitar" (`on-secondary` sobre `secondary-container`, 2.3:1), que pasa a `on-secondary-container` (§12).
- **Rendimiento** (RNF-08): con 1000 perfiles, abrir un perfil, una lista de 200 elementos, una búsqueda combinada y agrupar por género tardan milisegundos en JVM; el arranque con la base de datos recién abierta, unos 0,25 s en Robolectric. Los fps del scroll y el arranque en frío reales se miden en dispositivo (`SOFTWARE/pruebasManuales.md`).
- **Release**: R8 activado; `assembleRelease` y `bundleRelease` comprueban antes que el manifiesto combinado no pide red y que no hay librerías de red (T9.18). La firma se configura fuera del repositorio (README).

---

## 12. Diferencias con el prototipo por requisitos

| Prototipo | Implementación | Motivo |
|---|---|---|
| Puntuación **obligatoria** en Viendo/Visto ("Obligatorio *", "Puntuación obligatoria en estado Viendo/Visto.") | Siempre opcional; etiqueta "Opcional" fija | RF-03, RF-15 |
| Ranking "Top 10" con límite, aviso con candado "Límite alcanzado (10 de 10)" y "Completa" | Sin límite ni aviso | RF-41. Solo se ven 10 a la vez y el resto con scroll (RNF-01) |
| "Viendo (45m rest.)", "5 Temporadas", "6 series (24 temporadas)" | No se muestra progreso | RF-12 |
| Director ("Dir. Denis Villeneuve"), año, duración "166 min", "4K", "Visto en Cine Gran Vía (Sala 1)" | No existen esos campos: se sustituyen por géneros / estado / nada, según se indica en cada pantalla | No están en los requisitos |
| "Top #1 Cine 2024" en el hero | "#n en {lista}" (mejor puesto del perfil en sus rankings) | Dato real equivalente |
| Slider de Buscar con un solo pulgar y paso 0.5 | Dos pulgares (mínimo y máximo), paso 0.1 | RF-44, RNF-05 |
| Placeholder "Título, director o palabra clave..." | "Buscar por título..." | Solo se busca por nombre (RF-44) |
| Valores del rango de Buscar solo informativos | Mínimo y máximo editables en la píldora, con M-03 / M-14 | CU-20 E2, E3 (T7.15, T7.16) |
| "Idioma Original"; géneros "Thriller Psicológico", "Neo-Noir"…; "Resultados Locales (SQLite)"; "Director • año" en los resultados | "Idioma"; géneros del catálogo (§8.7); "Resultados"; "{saga} • {géneros}" | RF-07, RF-08; sin director ni año; sin jerga técnica |
| "Restaurar copia de seguridad" | "Importar copia de seguridad" (fusiona) | CU-22 / S-20 |
| "Métricas de Base de Datos SQLite", "Almacenamiento (0 KB Ext)", "Distribución de Registros Internos", "Series y Episodios", "Última transacción SQLite: … (Commit OK)" | "Métricas de la base de datos", "Almacenamiento", "Distribución de Registros", "Series", "Última modificación: hoy a las hh:mm" | Sin jerga técnica; no hay almacenamiento externo ni episodios (RF-12) |
| "Genera un paquete JSON estructurado con historial y favoritos."; "Importa colecciones previas desde la memoria interna o tarjeta SD."; `cinetrack_backup_2025.json` | "Genera un paquete JSON con tus títulos y listas."; "Añade a tu colección los títulos y listas de un archivo .JSON. No se borra nada."; `cinetrack_backup_AAAA-MM-DD.json` | No hay historial ni favoritos; la ubicación la elige el selector del sistema (CU-21, CU-22) |
| "SQLite cifrado", "Commit OK", "Modo local SQLite", "Listas sincronizadas", "Resultados Locales (SQLite)" | Textos sin cifrado ni sincronización ni jerga técnica | No se implementa cifrado ni sincronización |
| Carátulas y fotogramas | Carátula opcional elegida de la galería; sin ella, marcador con icono `movie`/`tv` | Sin API de catálogo (RES-05). **Requiere añadir el requisito** |
| Barra de estado dibujada | Barra de estado real del sistema | Es parte del mockup |
| Avatar de usuario | Decorativo / acceso a Ajustes | No hay cuentas |
| Sin opción de quitar la puntuación ni de agrupar | "Quitar puntuación" (§7.3) y "Agrupar" (§4.2) | RF-13, RF-23..26 |
| Editor de nota del detalle solo con slider, que cambia la nota al moverlo | Campo editable + slider, M-03 y botón "Guardar" (§7.3) | CU-03 pasos 2–4 y E1 (T3.12, T3.31) |
| Editar/borrar la crítica con `prompt()` / `confirm()` del navegador | Hoja inferior con contador y diálogo de confirmación (§7.6) | CU-04, S-06 |
| Diálogo de eliminar con "{n} listas personalizadas asociadas" | Texto de M-06 (§7.7) | Catálogo de mensajes (T3.23) |
| Franja "SQLite cifrado local • 60 entradas · Sin telemetría" y pie "Base de datos SQLite al día…" en la Biblioteca | Quitados | Decisión del usuario (S4) |
| Barra superior con "CineTrack", "100% Local" y nombre de la pantalla | Solo logo y avatar | Decisión del usuario (S4) |
| Póster 96×144 en las tarjetas de Biblioteca | Icono de tipo 48 dp (§4.1); la tarjeta mantiene su alto | Decisión del usuario (S4): no hay carátulas y el hueco no aportaba |
| Botón "Quitar" / "Eliminar lista" de la hoja con texto `on-secondary` (2.3:1) | Texto `on-secondary-container` (4.6:1) | Contraste AA (T9.11) |
| Escala tipográfica del prototipo (`label-sm` 10 sp…) | Estilos de título, cuerpo y etiqueta 1–2 sp mayores (§2.5) | Decisión del usuario (S4): textos demasiado pequeños en el móvil. Las capturas G se miden con esta escala |

---

## 13. Inconsistencias internas del prototipo (decisión tomada)

Stitch generó cada pantalla por separado y hay detalles que cambian entre ellas. Se unifican así:

| Elemento | Variantes en el prototipo | Decisión |
|---|---|---|
| Tokens `on-primary-container`, `tertiary`, `tertiary-container`, `on-tertiary-container`, `surface-container-highest` | Dos valores (§2.1) | Valor canónico de 4 de 6 pantallas |
| Color de "CineTrack" | `sky-400` (Biblioteca, Listas) / `primary` (Buscar) | `#38BDF8` |
| Avatar | Relleno `sky-400` + icono `slate-950` (Listas); `sky-400` 20 % + borde 30 % + icono `sky-300` (Biblioteca); `primary` + `on-primary` (Buscar, Detalle, Ranking); `primary-container` (Añadir) | Relleno `#38BDF8` + icono `slate-950` |
| Texto sobre acento | `slate-950` (Listas, Biblioteca) / `on-primary-container` (resto) | `slate-950` en botones de Listas y Biblioteca y en el FAB; `on-primary-container` en el resto (tal cual) |
| Etiqueta del botón "Añadir" de la barra inferior | `on-surface-variant` (Listas, Buscar) / `sky-400` medium + sombra azulada (Biblioteca) | `on-surface-variant`, sombra negra 40 % |
| Título de la barra en la lista abierta | "Detalle Pelicula" (error del prototipo) | "Lista" |
| Ranking dentro de Listas (pestaña "Top Ranking": asa de arrastre, papelera, chip de prioridad) vs. pantalla de ranking (flechas, quitar, medallas) | Dos diseños de fila | Pantalla de ranking (§6). La pestaña "Top Ranking" abre esa pantalla |
| `#4…#10` del ranking | `primary` en el HTML estático / `on-surface-variant` tras renumerar (captura) | `on-surface-variant` |
| Primera tarjeta de lista con "Abrir →" y borde; resto con `more_vert` | Dos estilos | `more_vert` en todas; borde `sky-400` 10 % solo en el banner |
| Toast | Tres estilos (§3.5) | Canónico + variante de éxito al guardar |
| Diálogos | Tres estilos (§3.6) | Cada uno donde lo usa el prototipo |
| Pendiente: icono | `schedule`, `hourglass_empty`, `hourglass_top`, `bookmark_add` | Se respeta el de cada pantalla |
| "Visto" en `DESIGN.md` de Stitch (verde `#34D399`) | No se usa en las pantallas | Estilos de §2.3 |
| Barra inferior en `DESIGN.md` ("Inicio, Buscar, Mi Lista, Estadísticas") | Pantallas: Biblioteca, Listas, Añadir, Buscar, Ajustes | Las de las pantallas |
| ¿Ajustes lleva barra inferior? | §3.4 dice "solo Biblioteca, Listas y Buscar", pero Ajustes es un destino de la barra | Ajustes es pestaña principal: barra superior de marca ("Ajustes") y barra inferior con Ajustes resaltado (S0, T0.05) |
| Elevación del botón "Añadir" (`-mt-5`) | La fila centra la caja de margen (`items-center`), así que el círculo sube 10 dp, no 20 | Se reproduce lo que se ve: 10 dp (S0, T0.08) |

---

# STACK TECNOLÓGICO

## 14. Decisión

**Android nativo con Kotlin + Jetpack Compose (Material 3)** y base de datos local **Room (SQLite)**.

**Por qué**: el prototipo es una app Android Material 3 oscura con tokens propios. En Compose esos tokens se definen una vez (`ColorScheme`, `Typography`, `Shapes` y tokens extra) y cada componente del prototipo se reproduce con sus medidas en dp. Es nativo (rendimiento < 1 s con 1000 perfiles, RNF-08) y funciona 100 % sin conexión (RES-03). Un WebView con Tailwind copiaría el HTML más literalmente, pero perdería gestos, desenfoque y selector de archivos nativos; se descarta.

## 15. Componentes

| Capa | Tecnología | Motivo |
|---|---|---|
| Lenguaje | **Kotlin 2.x** | Estándar Android |
| UI | **Jetpack Compose + Material 3** (BOM estable) | Mismo sistema que el prototipo |
| Tema | `darkColorScheme()` con §2.1, `Typography` §2.5, `Shapes` §2.6, y un `CompositionLocal` `CineColors` con §2.2–§2.4 | Un solo origen de verdad |
| Fuente | **Inter** variable en `res/font/` | Offline (el prototipo la carga de Google Fonts) |
| Iconos | **Material Symbols Outlined** (fuente variable empaquetada, ejes FILL/wght) | El prototipo usa Symbols y FILL 0/1 |
| Logo | Imagen del logo exportada del prototipo como recurso | Barra superior |
| Navegación | **Navigation Compose** (type-safe) + barra inferior propia con destino central elevado | 3 destinos con barra + pantallas secundarias sin barra |
| Arquitectura | **MVVM + UDF**: `ViewModel`, `StateFlow`, repositorios | Mantenible por una persona |
| Inyección | Contenedor manual (`AppContainer`) | Decidido en el S2: pocas dependencias y un solo contenedor; evita el plugin de Hilt. Los tests sustituyen el contenedor |
| Persistencia | **Room** + `Flow` | Requisito de datos locales |
| Búsqueda sin tildes | Columna `nombreBusqueda` normalizada (`java.text.Normalizer`) + `LIKE` con escape | RF-46 |
| Preferencias | **DataStore (Preferences)** | Última lista abierta, avisos |
| Imágenes | **Coil 3** + `PickVisualMedia` (carátula opcional, copiada al almacenamiento interno) | Sin red |
| Reordenar | **sh.calvin.reorderable** 3.1.0 (pulsación larga en `LazyColumn`, con scroll automático) + flechas ▲▼ | §6.2. Añadida en el S6 |
| Desenfoque | **Haze** en barras y diálogos; en Android < 12, color sólido al 95 % | §3.2, §3.4 |
| Copia de seguridad | **kotlinx.serialization** + Storage Access Framework (`CreateDocument` / `OpenDocument`) | Sin permisos de almacenamiento |
| Pruebas | JUnit 5, Turbine, Room in-memory, `compose-ui-test`, **Paparazzi/Roborazzi** (capturas comparadas con `screen.png`) | Ver `sprints.md` |
| Build | Gradle KTS + catálogo de versiones, R8 en release | |
| Calidad | ktlint, detekt | |

**Versión mínima**: `minSdk 26` (Android 8.0); `targetSdk 36`, `compileSdk 37`. Decidido en el S0 (RES-02).

## 16. Modelo de datos (Room)

- `Perfil(id, nombre, nombreBusqueda, tipo{PELICULA,SERIE}, estado{PENDIENTE,VIENDO,VISTO}, puntuacionDecimas: Int?, comentario: String?, comentarioEditadoEn: Instant?, sagaId?, caratulaUri?, creadoEn)` — único `(nombreBusqueda, tipo)`. `VISTO` = "sin estado" del requisito.
- `Saga(id, nombre, nombreBusqueda único)`.
- `Genero(id, nombre)` / `Idioma(id, nombre)` precargados + `PerfilGenero`, `PerfilIdioma`.
- `Lista(id, nombre, nombreBusqueda único, tipo{NORMAL,PRIORIDAD,RANKING}, restriccion{MIXTO,PELICULAS,SERIES}, avisoVaciaMostrado, creadoEn)`.
- `ListaItem(listaId, perfilId, posicion?, prioridad{ALTA,MEDIA,BAJA}?, anadidoEn)` — PK `(listaId, perfilId)`; `ON DELETE CASCADE` en ambas claves. Posición y prioridad viven aquí, no en `Perfil`.
- Rangos de puntuación, grupos y contadores: consultas.

## 17. Estructura del código

```
app/src/main/java/.../cinetrack/
  data/        room (entities, dao, db), backup, repository
  domain/      modelos y reglas (Puntuacion, normalizador, fusión de importación)
  ui/
    theme/     Color, CineColors, Type, Shape, Spacing, Theme
    components/ CineTopBar, CineTopBarSecondary, CineBottomBar, SegmentedTabs, MediaCard,
                RankingRow, ListCard, StatusBadge, ScoreChip, PriorityChip, FilterChip,
                SectionLabel, CineDialog, BottomSheetConfirm, CineToast, ScoreStepper
    biblioteca/ listas/ lista_detalle/ perfil/ perfil_form/ buscar/ datos/
  navigation/
```

## 18. Correspondencia prototipo → Compose

| Prototipo (Tailwind) | Compose |
|---|---|
| `bg-surface-container-*`, `text-primary`… | `MaterialTheme.colorScheme.*` / `CineColors.*` |
| `bg-sky-400/15`, `/20`, `/80` | `color.copy(alpha = 0.15f)` … |
| `rounded`, `-md`, `-lg`, `-xl`, `-2xl`, `-full` | `RoundedCornerShape(4/6/8/12/16.dp)`, `CircleShape` |
| `p-space-md`, `gap-space-sm`, `px-margin` | `padding(16.dp)`, `Arrangement.spacedBy(8.dp)`, `padding(horizontal = 16.dp)` |
| `w-24 h-36` | `Modifier.size(96.dp, 144.dp)` |
| `fixed` + `backdrop-blur-xl` | `Scaffold` con barras propias + `hazeChild` |
| `sticky` (pestañas de Listas) | `stickyHeader` en `LazyColumn` |
| `truncate` | `maxLines = 1, overflow = Ellipsis` |
| `grid grid-cols-3` | `Row` con `Modifier.weight(1f)` |
| Radios ocultos con `peer-checked` | Composables de selección propios |
| `font-variation-settings: 'FILL' 1` | Fuente Symbols con eje FILL = 1 |
| `animate-pulse` / `animate-ping` | `rememberInfiniteTransition` |
| Modal `fixed inset-0` | `Dialog` / `ModalBottomSheet` con scrim propio |

## 19. Criterios de fidelidad

1. Cada pantalla se compara con su `screen.png` a 411 dp de ancho con capturas automáticas; las diferencias solo pueden ser las de §12 y §13.
2. Ningún color, radio ni medida fija fuera de `ui/theme`.
3. Los textos visibles coinciden con los del prototipo salvo los cambiados en §12; todos en `strings.xml`.
4. Cada componente de §17 tiene su captura de referencia en reposo, pulsado, deshabilitado y vacío.
