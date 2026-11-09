package com.cinesstar.cinetrack.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import java.time.LocalDate

/** Lectura y escritura del formato de la copia (CU-21 paso 3, CU-22 paso 2). */
object FormatoCopia {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    fun escribir(copia: CopiaSeguridad): String = json.encodeToString(CopiaSeguridad.serializer(), copia)

    /**
     * CU-22 paso 2: interpreta y valida **completamente** [texto] antes de tocar la base de datos. Los archivos de
     * versiones anteriores se migran (A2). Lanza [CopiaException] con M-17 (no es JSON, no es de CineTrack o tiene
     * datos inconsistentes) o M-18 (versión más reciente).
     */
    fun leer(
        texto: String,
        generos: Collection<String> = GENEROS_PREDEFINIDOS,
        idiomas: Collection<String> = IDIOMAS_PREDEFINIDOS,
    ): CopiaSeguridad {
        val raiz = interpretar(texto) as? JsonObject ?: noValida()
        val copia = decodificar(migrar(raiz, version(raiz)))
        ReglasCopia.validar(copia, generos, idiomas)?.let { throw CopiaException(ProblemaCopia.NoValida(it)) }
        return copia
    }

    /** Errores de sintaxis o de estructura: `SerializationException` es un `IllegalArgumentException`. */
    private fun interpretar(texto: String): JsonElement =
        try {
            Json.parseToJsonElement(texto)
        } catch (e: IllegalArgumentException) {
            throw CopiaException(ProblemaCopia.NoValida()).apply { initCause(e) }
        }

    private fun decodificar(raiz: JsonObject): CopiaSeguridad =
        try {
            json.decodeFromJsonElement(CopiaSeguridad.serializer(), raiz)
        } catch (e: IllegalArgumentException) {
            throw CopiaException(ProblemaCopia.NoValida()).apply { initCause(e) }
        }

    private fun noValida(): Nothing = throw CopiaException(ProblemaCopia.NoValida())

    /** Versión del archivo: M-17 si no es una copia de CineTrack, M-18 si es más reciente que la de la app (E1, E2). */
    private fun version(raiz: JsonObject): Int {
        if ((raiz["app"] as? JsonPrimitive)?.contentOrNull != CopiaSeguridad.APP) noValida()
        val version = (raiz["version"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull ?: noValida()
        if (version > CopiaSeguridad.VERSION_ACTUAL) throw CopiaException(ProblemaCopia.VersionFutura)
        return version.takeIf { it >= 1 } ?: noValida()
    }

    /** CU-22 A2: lleva un archivo de la versión [desde] al formato actual. */
    private fun migrar(
        raiz: JsonObject,
        desde: Int,
    ): JsonObject {
        var actual = raiz
        if (desde < 2) actual = deV1aV2(actual)
        return JsonObject(actual + ("version" to JsonPrimitive(CopiaSeguridad.VERSION_ACTUAL)))
    }

    /** v1 → v2: `puntuacionDecimas` (85) pasa a `puntuacion` (8.5). */
    private fun deV1aV2(raiz: JsonObject): JsonObject {
        val perfiles = raiz["perfiles"] as? JsonArray ?: return raiz
        val migrados =
            perfiles.map { perfil ->
                if (perfil !is JsonObject || "puntuacionDecimas" !in perfil) return@map perfil
                val decimas = perfil["puntuacionDecimas"]
                val puntuacion =
                    when {
                        decimas == null || decimas is JsonNull -> JsonNull
                        decimas is JsonPrimitive && !decimas.isString && decimas.intOrNull != null ->
                            JsonPrimitive(decimas.intOrNull!! / ReglasCopia.DECIMAS_POR_UNIDAD)
                        else -> noValida()
                    }
                JsonObject(perfil - "puntuacionDecimas" + ("puntuacion" to puntuacion))
            }
        return JsonObject(raiz + ("perfiles" to JsonArray(migrados)))
    }

    /** Nombre propuesto para el archivo (CU-21 paso 1): `cinetrack_backup_AAAA-MM-DD.json`. */
    fun nombreArchivo(fecha: LocalDate): String = "cinetrack_backup_$fecha.json"
}
