package com.asir.moodleactividades.ui.faltas

import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import com.asir.moodleactividades.data.AlmacenFaltas
import com.asir.moodleactividades.data.Aviso
import com.asir.moodleactividades.data.HistorialAvisos
import com.asir.moodleactividades.data.SesionSeneca
import com.asir.moodleactividades.data.TipoAviso
import com.asir.moodleactividades.data.seneca.RespuestaJs
import com.asir.moodleactividades.domain.Falta
import com.asir.moodleactividades.domain.FaltasDeAsignatura
import com.asir.moodleactividades.domain.ResumenFaltas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Séneca solo se enseña cuando hace falta identificarse. Si la sesión sigue viva, todo el
 * recorrido ocurre en un navegador que el alumno no llega a ver.
 */
enum class ModoSeneca { NINGUNO, OCULTO, VISIBLE }

data class FaltasUiState(
    val faltas: List<Falta> = emptyList(),
    val porAsignatura: List<FaltasDeAsignatura> = emptyList(),
    val momento: Long? = null,
    val modo: ModoSeneca = ModoSeneca.NINGUNO,
    /** Lo que se vio en la última página cuando no había tabla de faltas. */
    val diagnostico: List<String> = emptyList(),
    val buscadaSinExito: Boolean = false,
    val leidoAlgunaVez: Boolean = false
) {
    val total: Int get() = faltas.size
    val injustificadas: Int get() = ResumenFaltas.totalInjustificadas(faltas)
}

class FaltasViewModel(
    private val almacen: AlmacenFaltas,
    private val sesion: SesionSeneca,
    private val historial: HistorialAvisos
) : ViewModel() {

    private val _estado = MutableStateFlow(FaltasUiState())
    val estado: StateFlow<FaltasUiState> = _estado.asStateFlow()

    init {
        val guardadas = almacen.leer()
        if (guardadas != null) {
            _estado.value = FaltasUiState(
                faltas = guardadas.faltas,
                porAsignatura = ResumenFaltas.porAsignatura(guardadas.faltas),
                momento = guardadas.momento,
                leidoAlgunaVez = true
            )
        }

        // Al abrir la pestaña se refresca solo, sin pedir nada, siempre que haya una sesión
        // guardada y lo que hay en pantalla se haya quedado viejo.
        if (sesion.hay() && caducado(guardadas?.momento)) actualizar()
    }

    private fun caducado(momento: Long?): Boolean {
        if (momento == null) return true
        return System.currentTimeMillis() / 1000 - momento > FRESCURA_SEGUNDOS
    }

    /**
     * Siempre se prueba primero a oscuras. Si la sesión de Séneca sigue viva —lo normal
     * después de la primera vez— el alumno no ve ninguna página web: solo sus faltas.
     */
    fun actualizar() = _estado.update {
        it.copy(modo = ModoSeneca.OCULTO, diagnostico = emptyList(), buscadaSinExito = false)
    }

    /** Solo cuando el intento a oscuras no llega a la tabla: hay que identificarse. */
    fun pedirIdentificacion() = _estado.update {
        if (it.modo == ModoSeneca.OCULTO) it.copy(modo = ModoSeneca.VISIBLE) else it
    }

    fun cerrarSeneca() = _estado.update { it.copy(modo = ModoSeneca.NINGUNO) }

    /**
     * Se llama al terminar de cargar cada página. La mayoría no son la de faltas, así que
     * sin tabla no se toca nada: solo se anota lo visto por si hace falta diagnosticar.
     */
    fun procesarPagina(crudo: String?, buscadaAMano: Boolean = false): Boolean {
        val resultado = RespuestaJs.leerExtraccion(crudo)

        if (resultado == null || !resultado.encontrada) {
            if (buscadaAMano) {
                _estado.update {
                    it.copy(diagnostico = resultado?.cabeceras.orEmpty(), buscadaSinExito = true)
                }
            }
            return false
        }

        anotarLasNuevas(_estado.value.faltas, resultado.faltas)

        almacen.guardar(resultado.faltas)
        // La sesión se guarda solo cuando ha servido para algo: así no se conserva una
        // caducada que obligaría a reintentar en balde en la próxima apertura.
        sesion.guardar()

        _estado.value = FaltasUiState(
            faltas = resultado.faltas,
            porAsignatura = ResumenFaltas.porAsignatura(resultado.faltas),
            momento = System.currentTimeMillis() / 1000,
            modo = ModoSeneca.NINGUNO,
            leidoAlgunaVez = true
        )
        return true
    }

    /** Lo que llega nuevo se apunta en «Avisos», para enterarse aunque no se mire aquí. */
    private fun anotarLasNuevas(antes: List<Falta>, ahora: List<Falta>) {
        ResumenFaltas.recienPuestas(antes, ahora).forEach { falta ->
            historial.anadir(
                Aviso(
                    momento = System.currentTimeMillis() / 1000,
                    tipo = TipoAviso.FALTA,
                    titulo = "Falta nueva en " + falta.asignatura,
                    texto = falta.fecha + " · " + falta.tramo + " · " + falta.estado
                )
            )
        }
    }

    /** Borra las faltas guardadas y la sesión del navegador incrustado. */
    fun desconectar() {
        almacen.borrar()
        sesion.borrar()
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        _estado.value = FaltasUiState()
    }

    private companion object {
        /** Media hora: lo justo para no repetir la consulta a cada vistazo. */
        const val FRESCURA_SEGUNDOS = 30 * 60L
    }
}
