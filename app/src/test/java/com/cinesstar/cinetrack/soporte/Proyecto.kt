package com.cinesstar.cinetrack.soporte

import java.io.File

/** Rutas del repositorio para los tests que contrastan el código con la especificación o el prototipo. */
object Proyecto {
    val raiz: File by lazy {
        System.getProperty("cinetrack.raiz")?.let(::File)
            ?: generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "SOFTWARE/design.md").exists() }
    }

    val design: File get() = File(raiz, "SOFTWARE/design.md")
    val fuentesMain: File get() = File(raiz, "app/src/main/java")
    val recursosMain: File get() = File(raiz, "app/src/main/res")

    fun captura(pantalla: String): File = File(raiz, "GoogleStitch/stitch_android_web_app_screens/$pantalla/screen.png")

    /** Carpeta donde los tests de capturas dejan las imágenes generadas para revisarlas. */
    val capturas: File by lazy {
        File(System.getProperty("cinetrack.capturas") ?: File(raiz, "app/build/capturas").path).apply { mkdirs() }
    }

    /** Líneas de la sección de design.md que empieza por [encabezado] (hasta el siguiente encabezado del mismo nivel o superior). */
    fun seccionDesign(encabezado: String): List<String> {
        val lineas = design.readLines(Charsets.UTF_8)
        val inicio = lineas.indexOfFirst { it.startsWith(encabezado) }
        require(inicio >= 0) { "No se encuentra la sección \"$encabezado\" en design.md" }
        val nivel = encabezado.takeWhile { it == '#' }.length
        val fin =
            (inicio + 1 until lineas.size).firstOrNull { i ->
                val l = lineas[i]
                l.startsWith("#") && l.takeWhile { it == '#' }.length <= nivel
            } ?: lineas.size
        return lineas.subList(inicio + 1, fin)
    }

    /** Filas de las tablas Markdown de una sección (sin cabecera ni separador), con las celdas recortadas. */
    fun filasTabla(lineas: List<String>): List<List<String>> =
        lineas
            .filter { it.trimStart().startsWith("|") }
            .map { fila ->
                fila
                    .trim()
                    .removePrefix("|")
                    .removeSuffix("|")
                    .split("|")
                    .map { it.trim() }
            }.filterNot { celdas -> celdas.all { it.matches(Regex("-+|:?-+:?")) } }
            .drop(1)
}
