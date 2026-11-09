package com.cinesstar.cinetrack.ui.perfilform

import android.app.Application
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.data.repository.CatalogoRepository
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.repository.SagaRepository
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.TipoPerfil
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * T2.29 (S-21, CU-23 A2), parte de cierre del proceso: el borrador se guarda en el SavedStateHandle y un ViewModel
 * nuevo (como tras recrear el proceso) lo recupera entero. La rotación y el segundo plano están en [FormularioPerfilTest].
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class BorradorTest {
    private val db = CineDatabase.construir(ApplicationProvider.getApplicationContext(), nombre = null)

    @After
    fun cerrar() = db.close()

    private fun vm(handle: SavedStateHandle) =
        FormularioPerfilViewModel(handle, PerfilRepository(db), SagaRepository(db), CatalogoRepository(db))

    @Test
    fun elBorradorSobreviveAlCierreDelProceso() {
        val handle = SavedStateHandle()
        val original = vm(handle)
        original.elegirTipo(TipoPerfil.SERIE)
        original.cambiarNombre("Restaurada")
        original.elegirEstado(Estado.VIENDO)
        original.cambiarPuntuacion("7,5")
        original.alternarGenero(3)
        original.alternarIdioma(2)
        original.cambiarComentario("Línea 1\nLínea 2")
        original.abrirEditorSaga()
        original.cambiarTextoSaga("Nueva")
        original.confirmarSagaNueva()
        shadowOf(Looper.getMainLooper()).idle()

        // Tras la muerte del proceso solo queda lo que el sistema guardó del SavedStateHandle.
        val restaurado =
            vm(
                SavedStateHandle(
                    mapOf(
                        FormularioPerfilViewModel.CLAVE_BORRADOR to handle.get<String>(FormularioPerfilViewModel.CLAVE_BORRADOR),
                    ),
                ),
            )
        assertEquals(original.estado.value.borrador, restaurado.estado.value.borrador)
        assertEquals("Nueva", restaurado.estado.value.sagaElegida)
        assertTrue(restaurado.estado.value.borrador.tieneDatos)
    }

    @Test
    fun unBorradorVacioNoTieneDatos() {
        assertFalse(Borrador().tieneDatos)
        assertFalse(Borrador(editorSagaAbierto = true, editorSagaTexto = "x", intentoGuardar = true).tieneDatos)
        assertTrue(Borrador(tipo = TipoPerfil.SERIE).tieneDatos)
    }

    @Test
    fun elComentarioNoSuperaLosDiezMilCaracteres() {
        val vm = vm(SavedStateHandle())
        vm.cambiarComentario("a".repeat(FormularioPerfilViewModel.MAX_COMENTARIO + 50))
        assertEquals(FormularioPerfilViewModel.MAX_COMENTARIO, vm.estado.value.borrador.comentario.length)
    }
}
