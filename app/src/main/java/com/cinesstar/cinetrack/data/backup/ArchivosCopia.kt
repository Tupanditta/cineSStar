package com.cinesstar.cinetrack.data.backup

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Acceso a los archivos que el usuario elige con el selector del sistema (Storage Access Framework): la app no pide
 * permisos de almacenamiento (CU-21 E2, TX.03).
 */
interface ArchivosCopia {
    /** Escribe [contenido] entero en [uri], sustituyendo lo que hubiera. Lanza [IOException] si no puede. */
    suspend fun escribir(
        uri: Uri,
        contenido: ByteArray,
    )

    suspend fun leer(uri: Uri): ByteArray

    /** Borra lo que se haya podido escribir en [uri] tras un fallo, para no dejar un archivo a medias (CU-21 E1). */
    suspend fun descartar(uri: Uri)
}

/** [ArchivosCopia] sobre el [ContentResolver]. Las rutas `file://` se tratan como archivos normales. */
class ArchivosSaf(
    private val resolver: ContentResolver,
) : ArchivosCopia {
    override suspend fun escribir(
        uri: Uri,
        contenido: ByteArray,
    ) {
        withContext(Dispatchers.IO) {
            abrirEscritura(uri).use { salida ->
                salida.write(contenido)
                salida.flush()
                (salida as? FileOutputStream)?.fd?.sync()
            }
        }
    }

    override suspend fun leer(uri: Uri): ByteArray = withContext(Dispatchers.IO) { abrirLectura(uri).use(InputStream::readBytes) }

    override suspend fun descartar(uri: Uri) {
        withContext(Dispatchers.IO) {
            runCatching {
                if (uri.scheme == ContentResolver.SCHEME_FILE) {
                    File(requireNotNull(uri.path)).delete()
                } else {
                    DocumentsContract.deleteDocument(resolver, uri)
                }
            }
        }
    }

    private fun abrirEscritura(uri: Uri): OutputStream {
        if (uri.scheme == ContentResolver.SCHEME_FILE) return FileOutputStream(File(requireNotNull(uri.path)))
        // "wt" vacía el archivo antes de escribir; algunos proveedores solo admiten "w".
        return runCatching { resolver.openOutputStream(uri, "wt") }.getOrNull()
            ?: resolver.openOutputStream(uri, "w")
            ?: throw IOException("No se puede escribir en $uri")
    }

    private fun abrirLectura(uri: Uri): InputStream {
        if (uri.scheme == ContentResolver.SCHEME_FILE) return File(requireNotNull(uri.path)).inputStream()
        return resolver.openInputStream(uri) ?: throw IOException("No se puede leer $uri")
    }
}
