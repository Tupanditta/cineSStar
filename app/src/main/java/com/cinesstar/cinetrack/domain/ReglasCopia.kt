package com.cinesstar.cinetrack.domain

/** Reglas de contenido de la copia: puntuaciones y coherencia de los datos (CU-22 E3). */
object ReglasCopia {
    /** Longitud máxima del comentario (S-06), la misma que en el formulario. */
    const val MAX_COMENTARIO = 10_000

    internal const val DECIMAS_POR_UNIDAD = 10.0

    /** Puntuación de la copia, o null si no tiene; [CopiaException] si no es válida (7.25, 11…). */
    fun puntuacion(perfil: PerfilCopia): Puntuacion? =
        perfil.puntuacion?.let {
            leerPuntuacion(it) ?: throw CopiaException(ProblemaCopia.NoValida(DetalleCopia.PuntuacionInvalida(perfil.nombre)))
        }

    /** Valor de la copia para una puntuación (8.5; el 10 queda como 10.0). */
    fun valor(puntuacion: Puntuacion): Double = puntuacion.decimas / DECIMAS_POR_UNIDAD

    /** El valor se interpreta con las mismas reglas que el formulario (S-04): 7.25 o 10.5 no son válidos. */
    private fun leerPuntuacion(valor: Double): Puntuacion? {
        if (valor.isNaN() || valor.isInfinite()) return null
        val texto = valor.toBigDecimal().stripTrailingZeros().toPlainString()
        return (Puntuacion.leer(texto) as? Puntuacion.Lectura.Valida)?.puntuacion
    }

    /** CU-22 E3: primer dato inconsistente, o null si la copia es coherente. */
    @Suppress("CyclomaticComplexMethod", "ReturnCount") // Una comprobación por regla de E3.
    fun validar(
        copia: CopiaSeguridad,
        generos: Collection<String> = GENEROS_PREDEFINIDOS,
        idiomas: Collection<String> = IDIOMAS_PREDEFINIDOS,
    ): DetalleCopia? {
        val generosValidos = generos.map(Normalizador::normalizar).toSet()
        val idiomasValidos = idiomas.map(Normalizador::normalizar).toSet()
        if (copia.sagas.any { Normalizador.limpiar(it).isEmpty() }) return DetalleCopia.NombreVacio
        val perfiles = HashMap<ClavePerfil, PerfilCopia>()
        for (p in copia.perfiles) {
            if (Normalizador.limpiar(p.nombre).isEmpty() || p.saga?.let { Normalizador.limpiar(it).isEmpty() } == true) {
                return DetalleCopia.NombreVacio
            }
            if (p.puntuacion != null && leerPuntuacion(p.puntuacion) == null) return DetalleCopia.PuntuacionInvalida(p.nombre)
            if ((p.comentario?.length ?: 0) > MAX_COMENTARIO) return DetalleCopia.ComentarioLargo(p.nombre)
            p.generos.firstOrNull { Normalizador.normalizar(it) !in generosValidos }?.let {
                return DetalleCopia.GeneroDesconocido(p.nombre, it)
            }
            p.idiomas.firstOrNull { Normalizador.normalizar(it) !in idiomasValidos }?.let {
                return DetalleCopia.IdiomaDesconocido(p.nombre, it)
            }
            if (perfiles.put(p.clave, p) != null) return DetalleCopia.PerfilDuplicado(p.nombre)
        }
        val listas = HashSet<String>()
        for (l in copia.listas) {
            if (Normalizador.limpiar(l.nombre).isEmpty()) return DetalleCopia.NombreVacio
            if (!listas.add(Normalizador.normalizar(l.nombre))) return DetalleCopia.ListaDuplicada(l.nombre)
            validarElementos(l, perfiles.keys)?.let { return it }
        }
        return null
    }

    @Suppress("ReturnCount")
    private fun validarElementos(
        lista: ListaCopia,
        perfiles: Set<ClavePerfil>,
    ): DetalleCopia? {
        val vistos = HashSet<ClavePerfil>()
        val posiciones = HashSet<Int>()
        for (e in lista.elementos) {
            if (e.clave !in perfiles) return DetalleCopia.ElementoSinPerfil(lista.nombre, e.nombre)
            if (!vistos.add(e.clave)) return DetalleCopia.ElementoRepetido(lista.nombre, e.nombre)
            if (!lista.restriccion.admite(e.tipo)) return DetalleCopia.RestriccionLista(lista.nombre, e.nombre)
            val completo =
                when (lista.tipo) {
                    TipoLista.NORMAL -> true
                    TipoLista.PRIORIDAD -> e.prioridad != null
                    TipoLista.RANKING -> e.posicion != null && e.posicion > 0 && posiciones.add(e.posicion)
                }
            if (!completo) return DetalleCopia.ElementoIncompleto(lista.nombre, e.nombre)
        }
        return null
    }
}
