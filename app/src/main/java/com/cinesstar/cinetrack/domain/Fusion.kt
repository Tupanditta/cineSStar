package com.cinesstar.cinetrack.domain

/*
 * Reglas de fusión de la importación (CU-22, S-20). La importación añade y nunca borra, sobrescribe ni modifica datos
 * existentes. El plan se calcula sin tocar la base de datos ("en seco", CU-22 paso 3) y se aplica después tal cual
 * dentro de una única transacción (paso 5), así los recuentos de M-19 y M-20 coinciden.
 */

/** Recuentos de M-19 y M-20. */
data class ResumenFusion(
    val perfilesNuevos: Int,
    val perfilesOmitidos: Int,
    val listasNuevas: Int,
    val listasAmpliadas: Int,
)

/** Una lista local, vista por la fusión. [perfiles] son los ids de sus elementos. */
data class ListaLocal(
    val id: Long,
    val nombre: String,
    val tipo: TipoLista,
    val restriccion: Restriccion,
    val perfiles: Set<Long>,
)

/** Lo que ya hay en la base de datos, con las claves de identidad de S-20. */
data class EstadoLocal(
    /** Perfiles por (nombre normalizado, tipo). */
    val perfiles: Map<ClavePerfil, Long> = emptyMap(),
    /** Nombres normalizados de las sagas registradas. */
    val sagas: Set<String> = emptySet(),
    /** Listas por nombre normalizado. */
    val listas: Map<String, ListaLocal> = emptyMap(),
)

/** Lista de la copia que se crea con [nombre] (el suyo o "{nombre} (importada)"…), con todos sus elementos. */
data class ListaNueva(
    val nombre: String,
    val lista: ListaCopia,
)

/** Lista local que se amplía con [elementos] (los que aún no tiene, en el orden de la copia). */
data class Ampliacion(
    val lista: ListaLocal,
    val elementos: List<ElementoCopia>,
)

/** Resultado en seco de fusionar una copia con los datos locales. */
data class PlanFusion(
    val perfilesNuevos: List<PerfilCopia>,
    val perfilesOmitidos: List<PerfilCopia>,
    /** Sagas que no existen todavía, con el nombre de la copia (sin espacios sobrantes). */
    val sagasNuevas: List<String>,
    val listasNuevas: List<ListaNueva>,
    /** Solo las listas que reciben algún elemento. */
    val ampliaciones: List<Ampliacion>,
) {
    val resumen: ResumenFusion
        get() = ResumenFusion(perfilesNuevos.size, perfilesOmitidos.size, listasNuevas.size, ampliaciones.size)
}

object Fusion {
    /**
     * Calcula la fusión de [copia] (ya validada) con [local]:
     * - Perfiles por (nombre normalizado, tipo): si no existe se crea con todos sus datos; si existe se omite entero.
     * - Sagas por nombre normalizado: se reutiliza la existente o se crea.
     * - Listas por nombre normalizado. Con el mismo tipo y restricción se amplía con los perfiles que aún no tiene; si
     *   no coinciden se prueba "{nombre} (importada)", "{nombre} (importada 2)"… y se amplía la primera compatible o se
     *   crea con el primer nombre libre. Así importar dos veces el mismo archivo no crea otra "(importada)"
     *   (idempotencia). Los nombres de otras listas de la copia quedan reservados para ellas.
     */
    fun planificar(
        copia: CopiaSeguridad,
        local: EstadoLocal,
    ): PlanFusion {
        val (omitidos, nuevos) = copia.perfiles.partition { it.clave in local.perfiles }
        val sagasNuevas =
            (copia.sagas + copia.perfiles.mapNotNull { it.saga })
                .map(Normalizador::limpiar)
                .distinctBy(Normalizador::normalizar)
                .filter { Normalizador.normalizar(it) !in local.sagas }
        val reservados = copia.listas.map { Normalizador.normalizar(it.nombre) }.toSet()
        val usados = HashSet<String>()
        val listasNuevas = mutableListOf<ListaNueva>()
        val ampliaciones = mutableListOf<Ampliacion>()
        for (lista in copia.listas) {
            val propio = Normalizador.normalizar(lista.nombre)
            // Primer candidato libre (para crear) o con una lista local compatible (para ampliar).
            val (nombre, existente) =
                candidatos(Normalizador.limpiar(lista.nombre))
                    .map { it to local.listas[Normalizador.normalizar(it)] }
                    .first { (nombre, existente) ->
                        val clave = Normalizador.normalizar(nombre)
                        val disponible = (clave == propio || clave !in reservados) && clave !in usados
                        disponible && (existente == null || (existente.tipo == lista.tipo && existente.restriccion == lista.restriccion))
                    }
            usados += Normalizador.normalizar(nombre)
            if (existente == null) {
                listasNuevas += ListaNueva(nombre, lista)
            } else {
                val faltan = ordenCopia(lista).filter { local.perfiles[it.clave] !in existente.perfiles }
                if (faltan.isNotEmpty()) ampliaciones += Ampliacion(existente, faltan)
            }
        }
        return PlanFusion(nuevos, omitidos, sagasNuevas, listasNuevas, ampliaciones)
    }

    /** "{nombre}", "{nombre} (importada)", "{nombre} (importada 2)"… */
    private fun candidatos(nombre: String): Sequence<String> =
        sequenceOf(nombre, "$nombre $SUFIJO") + generateSequence(2) { it + 1 }.map { "$nombre ${sufijo(it)}" }

    private const val SUFIJO = "(importada)"

    private fun sufijo(n: Int) = "(importada $n)"

    /** Elementos en el orden de la copia: por posición en un ranking y tal como vienen en el resto. */
    fun ordenCopia(lista: ListaCopia): List<ElementoCopia> =
        if (lista.tipo == TipoLista.RANKING) lista.elementos.sortedBy { it.posicion } else lista.elementos
}
