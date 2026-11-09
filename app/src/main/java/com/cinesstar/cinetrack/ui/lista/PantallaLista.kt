package com.cinesstar.cinetrack.ui.lista

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.contenedor
import com.cinesstar.cinetrack.domain.ContenidoLista
import com.cinesstar.cinetrack.domain.ElementoLista
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.ResumenLista
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.ui.components.Aviso
import com.cinesstar.cinetrack.ui.components.HojaInferior
import com.cinesstar.cinetrack.ui.components.LocalAvisos
import com.cinesstar.cinetrack.ui.components.LocaleEs
import com.cinesstar.cinetrack.ui.components.PantallaSecundaria
import com.cinesstar.cinetrack.ui.components.SymbolIcon
import com.cinesstar.cinetrack.ui.components.conNegrita
import com.cinesstar.cinetrack.ui.components.etiqueta
import com.cinesstar.cinetrack.ui.components.etiquetaSolo
import com.cinesstar.cinetrack.ui.components.icono
import com.cinesstar.cinetrack.ui.listas.DialogoEliminarLista
import com.cinesstar.cinetrack.ui.listas.DialogoRenombrarLista
import com.cinesstar.cinetrack.ui.listas.OpcionMenuLista
import com.cinesstar.cinetrack.ui.listas.textoTotal
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.Dimens
import com.cinesstar.cinetrack.ui.theme.DimensComunes
import com.cinesstar.cinetrack.ui.theme.DimensLista
import com.cinesstar.cinetrack.ui.theme.Sombras
import com.cinesstar.cinetrack.ui.theme.Spacing
import com.cinesstar.cinetrack.ui.theme.TrackingTighter
import com.cinesstar.cinetrack.ui.theme.TrackingWider
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Pantalla "Lista abierta" (design.md §6, CU-13). Desde ella se añaden títulos (CU-15), se quitan (CU-16, con el aviso
 * único M-11), se cambia la prioridad (CU-17), se reordena el ranking (CU-18), se renombra (CU-14) y se elimina la lista
 * (CU-19). Tocar un elemento abre su perfil (CU-13 A3).
 */
@Composable
fun PantallaLista(
    listaId: Long,
    onBack: () -> Unit,
    onAvatar: () -> Unit,
    onPerfil: (Long) -> Unit,
) {
    val contenedor = LocalContext.current.contenedor
    val vm: ListaViewModel = viewModel(key = "lista_$listaId") { ListaViewModel(listaId, contenedor.listas) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val edicion by vm.edicion.collectAsStateWithLifecycle()
    val avisos = LocalAvisos.current
    val textoNoExiste = stringResource(R.string.aviso_lista_no_existe)
    val textoEliminada = stringResource(R.string.toast_lista_eliminada)
    val textos = TextosEventos.recordar()

    // CU-13 E1: la lista ya no existe → vuelve atrás con un aviso. CU-19 paso 4: "Lista eliminada".
    LaunchedEffect(estado) {
        when (estado) {
            EstadoLista.NoExiste -> avisos.mostrar(Aviso(textoNoExiste))
            EstadoLista.Eliminada -> avisos.mostrar(Aviso(textoEliminada))
            else -> return@LaunchedEffect
        }
        onBack()
    }
    LaunchedEffect(vm) { vm.eventos.collect { avisos.mostrar(Aviso(textos.texto(it))) } }

    val contenido = (estado as? EstadoLista.Lista)?.contenido
    PantallaSecundaria(
        titulo = stringResource(R.string.titulo_lista),
        tag = TagsLista.PANTALLA,
        onBack = onBack,
        onAvatarClick = onAvatar,
    ) {
        if (contenido != null) Contenido(contenido, edicion.reordenando, vm, onPerfil)
    }
    if (contenido != null) Dialogos(contenido, edicion, vm)
}

/** Textos de los toasts, leídos de `strings.xml` en la composición. */
private class TextosEventos(
    val anadido: String,
    val anadidoPuesto: String,
    val quitado: String,
    val renombrada: String,
    val posicion: String,
    val rankingGuardado: String,
    val prioridad: String,
    val elementoNoExiste: String,
    val errores: (ErrorDominio?) -> String,
) {
    fun texto(evento: EventoLista): String =
        when (evento) {
            is EventoLista.Anadido ->
                if (evento.puesto != null) anadidoPuesto.format(evento.nombre, evento.puesto) else anadido.format(evento.nombre)
            EventoLista.Quitado -> quitado
            EventoLista.Renombrada -> renombrada
            EventoLista.PosicionActualizada -> posicion
            EventoLista.RankingGuardado -> rankingGuardado
            EventoLista.PrioridadActualizada -> prioridad
            EventoLista.ElementoNoExiste -> elementoNoExiste
            is EventoLista.Error -> errores(evento.error)
        }

    companion object {
        @Composable
        fun recordar(): TextosEventos {
            val m25 = stringResource(R.string.m25_error_inesperado)
            // Los mensajes de dominio con parámetros (M-10) se formatean aquí con el nombre que trae el error.
            val m09p = stringResource(R.string.m09_lista_solo_peliculas)
            val m09s = stringResource(R.string.m09_lista_solo_series)
            val m10 = stringResource(R.string.m10_ya_en_lista)
            val m24 = stringResource(R.string.m24_posicion_invalida)
            return TextosEventos(
                anadido = stringResource(R.string.toast_anadido),
                anadidoPuesto = stringResource(R.string.toast_anadido_puesto),
                quitado = stringResource(R.string.toast_titulo_quitado),
                renombrada = stringResource(R.string.toast_lista_renombrada),
                posicion = stringResource(R.string.toast_posicion_actualizada),
                rankingGuardado = stringResource(R.string.toast_ranking_guardado),
                prioridad = stringResource(R.string.toast_prioridad_actualizada),
                elementoNoExiste = stringResource(R.string.aviso_elemento_no_existe),
                errores = { error ->
                    when (error) {
                        is ErrorDominio.RestriccionLista ->
                            if (error.admitido == TipoPerfil.PELICULA) m09p else m09s
                        is ErrorDominio.YaEnLista -> m10.format(error.nombrePerfil)
                        ErrorDominio.PosicionInvalida -> m24
                        else -> m25
                    }
                },
            )
        }
    }
}

@Composable
private fun Dialogos(
    contenido: ContenidoLista,
    edicion: EdicionLista,
    vm: ListaViewModel,
) {
    if (edicion.busqueda != null) {
        val candidatos by vm.candidatos.collectAsStateWithLifecycle()
        SelectorTitulos(contenido.lista, edicion, candidatos, vm)
    }
    edicion.quitar?.let { HojaQuitar(contenido.lista, it, vm::confirmarQuitar, vm::cancelarQuitar) }
    if (edicion.avisoVacia) HojaAvisoVacia(contenido.lista.nombre, vm::aceptarAvisoVacia, vm::rechazarAvisoVacia)
    edicion.renombrar?.let {
        DialogoRenombrarLista(
            it,
            edicion.errorRenombrar,
            vm::cambiarNombre,
            vm::guardarRenombrar,
            vm::cerrarRenombrar,
        )
    }
    if (edicion.eliminar) DialogoEliminarLista(contenido.lista.nombre, vm::confirmarEliminar, vm::cancelarEliminar)
}

/**
 * Contenido de la lista. En un ranking las filas se pueden mantener pulsadas y arrastrar (CU-18, con scroll automático
 * cerca de los bordes, A3): mientras se arrastra se pinta un orden provisional y al soltar se guarda el puesto nuevo. Si
 * no se guarda (soltar en el mismo sitio, A2; fallo, E1) se vuelve al orden guardado.
 */
@Composable
private fun Contenido(
    contenido: ContenidoLista,
    reordenando: Boolean,
    vm: ListaViewModel,
    onPerfil: (Long) -> Unit,
) {
    val arriba = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Dimens.TopBarContentHeight
    val abajo = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + DimensLista.ContentBottom
    val restaurar by vm.restaurarOrden.collectAsStateWithLifecycle()
    var arrastre by remember(contenido, restaurar) { mutableStateOf<List<ElementoLista>?>(null) }
    val elementos = arrastre ?: contenido.elementos
    val estadoLista = rememberLazyListState()
    val reordenable =
        rememberReorderableLazyListState(
            estadoLista,
            scrollThresholdPadding = PaddingValues(top = arriba, bottom = abajo),
        ) { desde, hasta ->
            arrastre = OrdenArrastre.mover(arrastre ?: contenido.elementos, desde.key, hasta.key)
        }
    val tipo = contenido.lista.tipo
    LazyColumn(
        state = estadoLista,
        modifier = Modifier.fillMaxSize().testTag(TagsLista.LISTA),
        contentPadding =
            PaddingValues(
                start = Spacing.Margin,
                end = Spacing.Margin,
                top = arriba + DimensLista.CabeceraTop,
                bottom = abajo,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        item(key = "cabecera") { Cabecera(contenido.lista, reordenando, vm) }
        if (elementos.isEmpty()) {
            item(key = "vacia") { Vacia(vm::abrirSelector) }
        } else if (tipo == TipoLista.RANKING) {
            items(elementos, key = { it.perfilId }) { elemento ->
                ReorderableItem(reordenable, key = elemento.perfilId) {
                    val acciones =
                        AccionesFila(
                            onClick = { onPerfil(elemento.perfilId) },
                            onQuitar = { vm.pedirQuitar(elemento) },
                            onSubir = { vm.subir(elemento) },
                            onBajar = { vm.bajar(elemento) },
                        )
                    FilaElemento(
                        tipo,
                        elemento,
                        contenido.lista.total,
                        acciones,
                        Modifier.longPressDraggableHandle(
                            onDragStopped = {
                                val original = contenido.elementos.indexOfFirst { it.perfilId == elemento.perfilId }
                                val nuevo = arrastre?.indexOfFirst { it.perfilId == elemento.perfilId } ?: original
                                if (nuevo == original || nuevo < 0) vm.cancelarArrastre() else vm.mover(elemento.perfilId, nuevo + 1)
                            },
                        ),
                    )
                }
            }
        } else {
            items(elementos, key = { it.perfilId }) { elemento ->
                val acciones =
                    AccionesFila(
                        onClick = { onPerfil(elemento.perfilId) },
                        onQuitar = { vm.pedirQuitar(elemento) },
                        onPrioridad = { vm.cambiarPrioridad(elemento, it) },
                    )
                FilaElemento(tipo, elemento, contenido.lista.total, acciones)
            }
        }
        item(key = "pie") { Pie(vm::pedirEliminar) }
    }
}

/** Orden provisional del arrastre (CU-18): lleva el elemento [desde] al sitio de [hasta] y renumera 1..n. */
internal object OrdenArrastre {
    fun mover(
        elementos: List<ElementoLista>,
        desde: Any,
        hasta: Any,
    ): List<ElementoLista> {
        val origen = elementos.indexOfFirst { it.perfilId == desde }
        val destino = elementos.indexOfFirst { it.perfilId == hasta }
        if (origen < 0 || destino < 0) return elementos
        return elementos
            .toMutableList()
            .apply { add(destino, removeAt(origen)) }
            .mapIndexed { indice, elemento -> elemento.copy(posicion = indice + 1) }
    }
}

/** Lo que se puede hacer desde una fila: abrir el perfil, quitar, ▲▼ (ranking) y cambiar la prioridad. */
private class AccionesFila(
    val onClick: () -> Unit,
    val onQuitar: () -> Unit,
    val onSubir: () -> Unit = {},
    val onBajar: () -> Unit = {},
    val onPrioridad: (Prioridad) -> Unit = {},
)

/** §6.1: insignia de tipo, recuento, renombrar, menú, título, descripción, acciones rápidas y banner de reordenar. */
@Composable
private fun Cabecera(
    lista: ResumenLista,
    reordenando: Boolean,
    vm: ListaViewModel,
) {
    Column(modifier = Modifier.padding(bottom = DimensLista.CabeceraBottom), verticalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
            InsigniaTipo(lista.tipo)
            Box(modifier = Modifier.weight(1f)) { ChipTotal(lista, Modifier) }
            BotonRedondo("edit", stringResource(R.string.cd_renombrar_lista), TagsLista.RENOMBRAR, vm::abrirRenombrar)
            MenuLista(vm::pedirEliminar)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
            Text(
                text = lista.nombre,
                style = CineTheme.typography.headlineLgMobile,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(TagsLista.TITULO),
            )
            Text(
                text =
                    stringResource(
                        when (lista.tipo) {
                            TipoLista.RANKING -> R.string.lista_desc_ranking
                            TipoLista.PRIORIDAD -> R.string.lista_desc_prioridad
                            TipoLista.NORMAL -> R.string.lista_desc_normal
                        },
                    ),
                style = CineTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.Xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        ) {
            if (lista.tipo == TipoLista.RANKING) {
                BotonReordenar(vm::activarReordenar, Modifier.weight(1f).testTag(TagsLista.REORDENAR))
            }
            BotonAnadir(vm::abrirSelector, Modifier.weight(1f).testTag(TagsLista.ANADIR))
        }
        if (reordenando && lista.tipo == TipoLista.RANKING) BannerReordenar(vm::terminarReordenar)
    }
}

/** "Reordenar puestos" (solo ranking): `surface-container-high`, icono `swap_vert` en `primary-container`. */
@Composable
private fun BotonReordenar(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = DimensLista.AccionPaddingH, vertical = DimensLista.AccionPaddingV),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon("swap_vert", null, DimensLista.AccionIcon, MaterialTheme.colorScheme.primaryContainer)
        Text(
            text = stringResource(R.string.lista_reordenar),
            style = CineTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Banner del modo reordenar (§6.1): `info` + texto y "Listo" (toast "Ranking guardado"). */
@Composable
private fun BannerReordenar(onListo: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(DimensLista.BannerPadding)
                .testTag(TagsLista.BANNER_REORDENAR),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon("info", null, DimensLista.BannerIcon, MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(R.string.lista_reordenar_banner),
            style = CineTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.lista_reordenar_listo),
            style = CineTheme.typography.labelSm,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier =
                Modifier
                    .clip(CineShapes.Lg)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(role = Role.Button, onClick = onListo)
                    .padding(horizontal = DimensLista.ListoPaddingH, vertical = DimensLista.ListoPaddingV)
                    .testTag(TagsLista.LISTO),
        )
    }
}

/** Insignia de tipo sin corchetes, en MAYÚSCULAS (§6.1). */
@Composable
private fun InsigniaTipo(tipo: TipoLista) {
    val (fondo, texto) =
        when (tipo) {
            TipoLista.RANKING ->
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = CineAlpha.A20) to
                    MaterialTheme.colorScheme.primaryContainer
            TipoLista.PRIORIDAD ->
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = CineAlpha.A20) to
                    MaterialTheme.colorScheme.secondary
            TipoLista.NORMAL -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
        }
    Text(
        text = stringResource(tipo.etiqueta()).uppercase(LocaleEs),
        style = CineTheme.typography.labelSm,
        letterSpacing = TrackingWider,
        color = texto,
        maxLines = 1,
        modifier =
            Modifier
                .clip(CineShapes.Full)
                .background(fondo)
                .padding(horizontal = DimensLista.InsigniaPaddingH, vertical = DimensLista.InsigniaPaddingV)
                .testTag(TagsLista.TIPO),
    )
}

/** "{n} títulos" (+ " • Solo Películas/Series" si hay restricción). */
@Composable
private fun ChipTotal(
    lista: ResumenLista,
    modifier: Modifier,
) {
    val total = textoTotal(lista.total, Restriccion.MIXTO)
    val texto =
        lista.restriccion.etiquetaSolo()?.let { stringResource(R.string.lista_chip_restriccion, total, stringResource(it)) } ?: total
    Row(
        modifier =
            modifier
                .clip(CineShapes.Full)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = DimensLista.InsigniaPaddingH, vertical = DimensLista.InsigniaPaddingV),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon("check_circle", null, DimensLista.ChipIcon, MaterialTheme.colorScheme.onSurfaceVariant, filled = true)
        Text(
            text = texto,
            style = CineTheme.typography.labelSm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(TagsLista.TOTAL),
        )
    }
}

@Composable
private fun BotonRedondo(
    icono: String,
    descripcion: String,
    tag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(DimensLista.BotonRedondo)
                .shadow(Sombras.Sm, CineShapes.Full)
                .clip(CineShapes.Full)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(role = Role.Button, onClick = onClick)
                .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(icono, descripcion, DimensLista.BotonRedondoIcon, MaterialTheme.colorScheme.onSurface)
    }
}

/** Botón ⋮ de la cabecera: "Eliminar lista" (CU-19). */
@Composable
private fun MenuLista(onEliminar: () -> Unit) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    Box {
        BotonRedondo("more_vert", stringResource(R.string.cd_opciones_lista), TagsLista.MENU) { abierto = true }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            OpcionMenuLista(R.string.eliminar_lista_confirmar, "delete", TagsLista.MENU_ELIMINAR) {
                abierto = false
                onEliminar()
            }
        }
    }
}

/** "Añadir título" (`primary-container` / `on-primary-container`, icono `add`). */
@Composable
private fun BotonAnadir(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = DimensLista.AccionPaddingH, vertical = DimensLista.AccionPaddingV),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon("add", null, DimensLista.AccionIcon, MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
            text = stringResource(R.string.lista_anadir_titulo),
            style = CineTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/**
 * §6.2 / §6.3: fila de un elemento. En ranking, puesto con medalla y flechas ▲▼ (deshabilitadas en los extremos, CU-18
 * A1); en prioridad, la píldora abre el menú Alta/Media/Baja (CU-17). [arrastre] es el asa de pulsación larga.
 */
@Composable
private fun FilaElemento(
    tipo: TipoLista,
    elemento: ElementoLista,
    total: Int,
    acciones: AccionesFila,
    arrastre: Modifier = Modifier,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(Sombras.Sm, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .then(arrastre)
                .clickable(role = Role.Button, onClick = acciones.onClick)
                .padding(DimensLista.FilaPadding)
                .testTag(TagsLista.fila(elemento.perfilId)),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tipo == TipoLista.RANKING) Puesto(elemento)
        Poster(elemento)
        DatosElemento(elemento, acciones.onPrioridad, Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Xs), verticalAlignment = Alignment.CenterVertically) {
            if (tipo == TipoLista.RANKING) {
                val puesto = elemento.posicion ?: 0
                Column {
                    BotonFlecha(
                        "keyboard_arrow_up",
                        stringResource(R.string.cd_subir_puesto),
                        puesto > 1,
                        TagsLista.subir(elemento.perfilId),
                        acciones.onSubir,
                    )
                    BotonFlecha(
                        "keyboard_arrow_down",
                        stringResource(R.string.cd_bajar_puesto),
                        puesto < total,
                        TagsLista.bajar(elemento.perfilId),
                        acciones.onBajar,
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .size(DimensComunes.TouchTarget)
                        .clip(CineShapes.Full)
                        .clickable(role = Role.Button, onClick = acciones.onQuitar)
                        .testTag(TagsLista.quitar(elemento.perfilId)),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(
                    "remove_circle_outline",
                    stringResource(R.string.cd_quitar_de_lista),
                    DimensLista.QuitarIcon,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Flecha ▲/▼ de 28×28 (§6.2); deshabilitada al 30 % y sin efecto en los extremos (CU-18 A1). */
@Composable
private fun BotonFlecha(
    icono: String,
    descripcion: String,
    habilitado: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(DimensLista.Flecha)
                .clip(CineShapes.Rounded)
                .clickable(enabled = habilitado, role = Role.Button, onClick = onClick)
                .alpha(if (habilitado) 1f else CineAlpha.A30)
                .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(icono, descripcion, DimensLista.FlechaIcon, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Columna de puesto: `#1`–`#3` en `primary-container` extrabold con medalla; el resto en `on-surface-variant`. */
@Composable
private fun Puesto(elemento: ElementoLista) {
    val puesto = elemento.posicion ?: 0
    val podio = puesto in 1..PODIO
    Column(
        modifier = Modifier.size(width = DimensLista.PuestoAncho, height = DimensLista.PosterAlto),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.lista_puesto, puesto),
            style =
                when {
                    puesto < DOS_CIFRAS -> CineTheme.typography.headlineMd
                    puesto < TRES_CIFRAS -> CineTheme.typography.titleSm
                    else -> CineTheme.typography.labelSm
                },
            fontWeight = if (podio) FontWeight.ExtraBold else FontWeight.Bold,
            letterSpacing = TrackingTighter,
            color = if (podio) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.testTag(TagsLista.puesto(elemento.perfilId)),
        )
        if (podio) {
            SymbolIcon(
                if (puesto == 1) "workspace_premium" else "military_tech",
                null,
                DimensLista.MedallaIcon,
                MaterialTheme.colorScheme.primaryContainer,
                filled = true,
            )
        }
    }
}

/** Póster 56×80 sin carátula (design.md §12): icono del tipo e insignia "PELÍCULA"/"SERIE". */
@Composable
private fun Poster(elemento: ElementoLista) {
    Box(
        modifier =
            Modifier
                .size(DimensLista.PosterAncho, DimensLista.PosterAlto)
                .shadow(Sombras.Md, CineShapes.Lg)
                .clip(CineShapes.Lg)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(elemento.tipo.icono(), null, DimensLista.PosterIcon, MaterialTheme.colorScheme.surfaceContainerHighest)
        Text(
            text = stringResource(elemento.tipo.etiqueta()).uppercase(LocaleEs),
            style = CineTheme.typography.insigniaPoster,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(DimensLista.PosterInsigniaInset)
                    .clip(CineShapes.Rounded)
                    .background(MaterialTheme.colorScheme.surfaceDim.copy(alpha = CineAlpha.A80))
                    .padding(horizontal = DimensLista.PosterInsigniaPaddingH),
        )
    }
}

@Composable
private fun DatosElemento(
    elemento: ElementoLista,
    onPrioridad: (Prioridad) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = elemento.nombre,
            style = CineTheme.typography.titleSm,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (elemento.generos.isNotEmpty()) {
            Text(
                text = elemento.generos.joinToString(SEPARADOR),
                style = CineTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.padding(top = Spacing.Xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChipNota(elemento)
            elemento.prioridad?.let { MenuPrioridad(elemento.perfilId, it, onPrioridad) }
            elemento.saga?.let {
                Text(
                    text = stringResource(R.string.detalle_saga_nombre, it),
                    style = CineTheme.typography.labelSm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ChipNota(elemento: ElementoLista) {
    Row(
        modifier =
            Modifier
                .clip(CineShapes.Rounded)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = DimensLista.NotaPaddingH, vertical = DimensLista.NotaPaddingV),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val nota = elemento.puntuacion
        if (nota != null) SymbolIcon("star", null, DimensLista.NotaIcon, MaterialTheme.colorScheme.primaryContainer, filled = true)
        Text(
            text = nota?.formatear() ?: stringResource(R.string.sin_puntuar),
            style = CineTheme.typography.labelSm,
            color = if (nota != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** CU-17: la píldora de prioridad abre un menú Alta/Media/Baja (§6.3); elegir el mismo nivel no cambia nada (A1). */
@Composable
private fun MenuPrioridad(
    perfilId: Long,
    actual: Prioridad,
    onElegir: (Prioridad) -> Unit,
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    val descripcion = stringResource(R.string.cd_cambiar_prioridad)
    Box {
        PildoraPrioridad(
            actual,
            Modifier
                .clip(CineShapes.Full)
                .clickable(role = Role.Button, onClickLabel = descripcion) { abierto = true }
                .testTag(TagsLista.prioridad(perfilId)),
        )
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Prioridad.entries.forEach { prioridad ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(prioridad.etiqueta()),
                            style = CineTheme.typography.labelMd,
                            fontWeight = if (prioridad == actual) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    trailingIcon = {
                        if (prioridad == actual) {
                            SymbolIcon("check", null, DimensLista.FlechaIcon, MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = {
                        abierto = false
                        onElegir(prioridad)
                    },
                    modifier = Modifier.testTag(TagsLista.opcionPrioridad(perfilId, prioridad)),
                )
            }
        }
    }
}

/** Píldora de prioridad (design.md §2.4). */
@Composable
private fun PildoraPrioridad(
    prioridad: Prioridad,
    modifier: Modifier = Modifier,
) {
    val colores =
        when (prioridad) {
            Prioridad.ALTA -> CineTheme.colors.prioridadAlta
            Prioridad.MEDIA -> CineTheme.colors.prioridadMedia
            Prioridad.BAJA -> CineTheme.colors.prioridadBaja
        }
    Text(
        text = stringResource(prioridad.etiqueta()),
        style = CineTheme.typography.labelSm,
        fontWeight = FontWeight.SemiBold,
        color = colores.texto,
        maxLines = 1,
        modifier =
            modifier
                .clip(CineShapes.Full)
                .background(colores.fondo)
                .padding(horizontal = DimensLista.PrioridadPaddingH, vertical = DimensLista.PrioridadPaddingV),
    )
}

/** §6.3: lista vacía (M-23) con "Añadir título" (CU-13 A1). */
@Composable
private fun Vacia(onAnadir: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(DimensLista.VaciaPadding)
                .testTag(TagsLista.VACIA),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        SymbolIcon("playlist_add", null, DimensLista.VaciaIcon, MaterialTheme.colorScheme.primaryContainer)
        Text(
            text = stringResource(R.string.m23_lista_vacia),
            style = CineTheme.typography.titleSm,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        BotonAnadir(onAnadir, Modifier.testTag(TagsLista.VACIA_ANADIR))
    }
}

/** §6.4: "Eliminar esta lista" (CU-19). */
@Composable
private fun Pie(onEliminar: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = DimensLista.PieTop), contentAlignment = Alignment.Center) {
        Row(
            modifier =
                Modifier
                    .clip(CineShapes.Xl)
                    .clickable(role = Role.Button, onClick = onEliminar)
                    .padding(horizontal = DimensLista.EliminarPaddingH, vertical = DimensLista.EliminarPaddingV)
                    .testTag(TagsLista.ELIMINAR),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon("delete_sweep", null, DimensLista.EliminarIcon, MaterialTheme.colorScheme.error)
            Text(
                text = stringResource(R.string.lista_eliminar),
                style = CineTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** §6.5: hoja "Quitar" (CU-16 paso 1). */
@Composable
private fun HojaQuitar(
    lista: ResumenLista,
    elemento: ElementoLista,
    onQuitar: () -> Unit,
    onCancelar: () -> Unit,
) {
    HojaListas(
        titulo = stringResource(if (lista.tipo == TipoLista.RANKING) R.string.quitar_titulo_ranking else R.string.quitar_titulo_lista),
        subtitulo = elemento.nombre,
        texto = conNegrita(stringResource(R.string.quitar_texto), lista.nombre),
        accion = stringResource(R.string.quitar_accion),
        cancelar = stringResource(R.string.comun_cancelar),
        onAccion = onQuitar,
        onCancelar = onCancelar,
    )
}

/** §6.5 / CU-16 A1: aviso único M-11 al quitar el último elemento. */
@Composable
private fun HojaAvisoVacia(
    nombreLista: String,
    onEliminar: () -> Unit,
    onMantener: () -> Unit,
) {
    HojaListas(
        titulo = stringResource(R.string.eliminar_lista_confirmar),
        subtitulo = nombreLista,
        texto = conNegrita(stringResource(R.string.m11_ultimo_elemento), nombreLista),
        accion = stringResource(R.string.eliminar_lista_confirmar),
        cancelar = stringResource(R.string.m11_mantener),
        onAccion = onEliminar,
        onCancelar = onMantener,
    )
}

@Composable
private fun HojaListas(
    titulo: String,
    subtitulo: String,
    texto: AnnotatedString,
    accion: String,
    cancelar: String,
    onAccion: () -> Unit,
    onCancelar: () -> Unit,
) {
    HojaInferior(
        accion = accion,
        cancelar = cancelar,
        onAccion = onAccion,
        onCancelar = onCancelar,
        fondoAccion = MaterialTheme.colorScheme.secondaryContainer,
        textoAccion = MaterialTheme.colorScheme.onSecondary,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Sm), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(DimensComunes.SheetIconCircle)
                        .clip(CineShapes.Full)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = CineAlpha.A20)),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon("playlist_remove", null, DimensComunes.SheetIcon, MaterialTheme.colorScheme.secondary)
            }
            Column {
                Text(
                    text = titulo,
                    style = CineTheme.typography.titleMd,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitulo,
                    style = CineTheme.typography.bodySm,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(text = texto, style = CineTheme.typography.bodyMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private const val PODIO = 3

/** Con 2 y 3 cifras el puesto se reduce para no desbordar los 32 dp (§6.2). */
private const val DOS_CIFRAS = 10
private const val TRES_CIFRAS = 100

private const val SEPARADOR = " • "
