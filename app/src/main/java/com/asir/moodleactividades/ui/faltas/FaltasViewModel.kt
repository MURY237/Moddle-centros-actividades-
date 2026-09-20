package com.asir.moodleactividades.ui.faltas

import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import com.asir.moodleactividades.data.AlmacenFaltas
import com.asir.moodleactividades.data.seneca.RespuestaJs
import com.asir.moodleactividades.domain.Falta
import com.asir.moodleactividades.domain.FaltasDeAsignatura
import com.asir.moodleactividades.domain.ResumenFaltas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FaltasUiState(
    val faltas: List<Falta> = emptyList(),
    val porAsignatura: List<FaltasDeAsignatura> = emptyList(),
    val momento: Long? = null,
    val navegando: Boolean = false,
    /** Lo que se vio en la última página cuando no había tabla de faltas. */
    val diagnostico: List<String> = emptyList(),
    val buscadaSinExito: Boolean = false,
    val buscandoSolo: Boolean = false
) {
    val total: Int get() = faltas.size
    val injustificadas: Int get() = ResumenFaltas.totalInjustificadas(faltas)
}

class FaltasViewModel(private val almacen: AlmacenFaltas) : ViewModel() {

    private val _estado = MutableStateFlow(FaltasUiState())
    val estado: StateFlow<FaltasUiState> = _estado.asStateFlow()

    init {
        almacen.leer()?.let { guardadas ->
            _estado.value = FaltasUiState(
                faltas = guardadas.faltas,
                porAsignatura = ResumenFaltas.porAsignatura(guardadas.faltas),
                momento = guardadas.momento
            )
        }
    }

    fun abrirSeneca() = _estado.update {
        it.copy(
            navegando = true,
            diagnostico = emptyList(),
            buscadaSinExito = false,
            buscandoSolo = true
        )
    }

    fun cerrarSeneca() = _estado.update { it.copy(navegando = false, buscandoSolo = false) }

    /**
     * Se llama al terminar de cargar cada página. La mayoría no son la de faltas, así que
     * sin tabla no se toca nada: solo se anota lo visto por si hace falta diagnosticar.
     */
    fun procesarPagina(crudo: String?, buscadaAMano: Boolean = false): Boolean {
        val resultado = RespuestaJs.leerExtraccion(crudo)

        if (resultado == null || !resultado.encontrada) {
            if (buscadaAMano) {
                _estado.update {
                    it.copy(
                        diagnostico = resultado?.cabeceras.orEmpty(),
                        buscadaSinExito = true,
                        buscandoSolo = false
                    )
                }
            }
            return false
        }

        almacen.guardar(resultado.faltas)
        _estado.value = FaltasUiState(
            faltas = resultado.faltas,
            porAsignatura = ResumenFaltas.porAsignatura(resultado.faltas),
            momento = System.currentTimeMillis() / 1000,
            navegando = false
        )
        return true
    }

    /** La app está pulsando sola por el menú de Séneca; sirve para avisarlo en pantalla. */
    fun buscandoSolo(activo: Boolean) = _estado.update { it.copy(buscandoSolo = activo) }

    /** Borra las faltas guardadas y la sesión del navegador incrustado. */
    fun desconectar() {
        almacen.borrar()
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        _estado.value = FaltasUiState()
    }
}
