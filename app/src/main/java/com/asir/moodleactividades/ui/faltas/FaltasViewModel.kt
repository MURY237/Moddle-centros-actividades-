package com.asir.moodleactividades.ui.faltas

import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import com.asir.moodleactividades.data.AlmacenFaltas
import com.asir.moodleactividades.data.Aviso
import com.asir.moodleactividades.data.HistorialAvisos
import com.asir.moodleactividades.data.SesionSeneca
import com.asir.moodleactividades.data.TipoAviso
import com.asir.moodleactividades.data.seneca.DiagnosticoSeneca
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
    val leidoAlgunaVez: Boolean = false,
    /** El intento a oscuras no llegó: hace falta identificarse, pero se pide, no se impone. */
    val necesitaAcceso: Boolean = false,
    val diagnosticoSeneca: DiagnosticoSeneca = DiagnosticoSeneca()
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

    private var intentoExplicito = true

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

        actualizarSiConviene()
    }

    /**
     * Refresco de cortesía: al entrar en la app y al volver a ella. Sin sesión guardada no se
     * hace nada, porque lo único que conseguiría es plantar la pantalla de acceso por sorpresa.
     */
    fun actualizarSiConviene() {
        if (!sesion.hay()) return
        if (_estado.value.modo != ModoSeneca.NINGUNO) return
        // Si ya se sabe que la sesión no vale, insistir solo gasta batería y datos.
        if (_estado.value.necesitaAcceso) return
        if (!caducado(_estado.value.momento)) return
        actualizar(explicito = false)
    }

    private fun caducado(momento: Long?): Boolean {
        if (momento == null) return true
        return System.currentTimeMillis() / 1000 - momento > FRESCURA_SEGUNDOS
    }

    /**
     * Siempre se prueba primero a oscuras. Si la sesión de Séneca sigue viva —lo normal
     * después de la primera vez— el alumno no ve ninguna página web: solo sus faltas.
     */
    /**
     * [explicito] distingue quién ha pedido la consulta. Si la pide el usuario y la sesión ha
     * caducado, tiene sentido enseñarle Séneca para que entre; si es el refresco de cortesía,
     * enseñarlo sería arrebatarle la pantalla mientras mira sus faltas.
     */
    fun actualizar(explicito: Boolean = true) {
        intentoExplicito = explicito
        _estado.update {
            it.copy(
                modo = ModoSeneca.OCULTO,
                diagnostico = emptyList(),
                buscadaSinExito = false,
                necesitaAcceso = false,
                diagnosticoSeneca = DiagnosticoSeneca.de(sesion)
            )
        }
    }

    /**
     * El intento a oscuras no ha llegado a la tabla. Si lo pidió el usuario se le enseña
     * Séneca; si no, se recoge todo y se deja un aviso: nunca se le quita lo que está viendo.
     */
    fun pedirIdentificacion() = _estado.update {
        val anotado = it.copy(diagnosticoSeneca = it.diagnosticoSeneca.copy(pidioAcceso = true))
        when {
            anotado.modo != ModoSeneca.OCULTO -> anotado
            intentoExplicito -> anotado.copy(modo = ModoSeneca.VISIBLE)
            else -> anotado.copy(modo = ModoSeneca.NINGUNO, necesitaAcceso = true)
        }
    }

    /** Qué consiguió pulsar del menú: dice dónde se atasca el recorrido si no llega. */
    fun anotarNavegacion(destino: String) = _estado.update {
        it.copy(diagnosticoSeneca = it.diagnosticoSeneca.copy(ultimoPaso = destino))
    }

    /** Cada página cargada, para poder contar dónde se queda el recorrido cuando falla. */
    fun anotarPagina(url: String?) = _estado.update {
        it.copy(
            diagnosticoSeneca = it.diagnosticoSeneca.copy(
                urlUltimaPagina = DiagnosticoSeneca.rutaSegura(url),
                paginasVistas = it.diagnosticoSeneca.paginasVistas + 1,
                cookiesVivas = sesion.vivasAhora()
            )
        )
    }

    fun cerrarSeneca() {
        // Puede haberse identificado sin llegar a la tabla; esa cookie ya vale para la
        // próxima vez y perderla obligaría a repetir el acceso.
        sesion.guardar()
        _estado.update { it.copy(modo = ModoSeneca.NINGUNO) }
    }

    /**
     * Se llama al terminar de cargar cada página. La mayoría no son la de faltas, así que
     * sin tabla no se toca nada: solo se anota lo visto por si hace falta diagnosticar.
     */
    fun procesarPagina(crudo: String?, url: String? = null, buscadaAMano: Boolean = false): Boolean {
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
        // Volver a esta misma página es lo que permite saltarse el recorrido la próxima vez.
        sesion.guardarUrl(url)
        // La sesión se guarda solo cuando ha servido para algo: así no se conserva una
        // caducada que obligaría a reintentar en balde en la próxima apertura.
        sesion.guardar()

        _estado.value = FaltasUiState(
            faltas = resultado.faltas,
            porAsignatura = ResumenFaltas.porAsignatura(resultado.faltas),
            momento = System.currentTimeMillis() / 1000,
            modo = ModoSeneca.NINGUNO,
            leidoAlgunaVez = true,
            diagnosticoSeneca = _estado.value.diagnosticoSeneca.copy(
                tablaEncontrada = true,
                cookiesGuardadas = sesion.nombres(),
                cookiesVivas = sesion.vivasAhora()
            )
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
        /**
         * Dos minutos. Abrir la app debe traer lo nuevo, pero leer las faltas obliga a
         * recorrer Séneca entero y no tiene sentido repetirlo al cambiar de pestaña.
         */
        const val FRESCURA_SEGUNDOS = 2 * 60L
    }
}
