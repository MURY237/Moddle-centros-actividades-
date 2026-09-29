package com.asir.moodleactividades.ui.netacad

import androidx.lifecycle.ViewModel
import com.asir.moodleactividades.data.AlmacenNetacad
import com.asir.moodleactividades.data.Aviso
import com.asir.moodleactividades.data.HistorialAvisos
import com.asir.moodleactividades.data.SesionNetacad
import com.asir.moodleactividades.data.TipoAviso
import com.asir.moodleactividades.data.netacad.LectorNetacad
import com.asir.moodleactividades.data.netacad.ResultadoNetacad
import com.asir.moodleactividades.domain.FiltroNetacad
import com.asir.moodleactividades.domain.ResumenActividades
import com.asir.moodleactividades.domain.ResumenNetacad
import com.asir.moodleactividades.domain.SeccionNetacad
import com.asir.moodleactividades.domain.TrabajoNetacad
import com.asir.moodleactividades.ui.formatearFecha
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * NetAcad solo se enseña cuando hace falta: para entrar con la cuenta de Cisco o para abrir
 * por primera vez la página de un curso. El resto de refrescos ocurre a oscuras.
 */
enum class ModoNetacad { NINGUNO, OCULTO, VISIBLE }

data class NetacadUiState(
    val trabajos: List<TrabajoNetacad> = emptyList(),
    val filtro: FiltroNetacad = FiltroNetacad.TODOS,
    val curso: String? = null,
    val momento: Long? = null,
    val modo: ModoNetacad = ModoNetacad.NINGUNO,
    /** El refresco a oscuras se topó con el acceso de Cisco: hay que entrar a mano. */
    val necesitaAcceso: Boolean = false,
    /** Se recorrieron las páginas guardadas y en ninguna aparecieron trabajos. */
    val sinExito: Boolean = false,
    /** Lo que se vio en la última página sin trabajos: estructura, no datos personales. */
    val pistas: List<String> = emptyList(),
    /** Páginas de curso aprendidas: son las que se visitan en el refresco a oscuras. */
    val paginas: Int = 0,
    /** Confirmación en el navegador visible: «5 trabajos de CCNA1». Vacío si aún nada. */
    val ultimaLectura: String = "",
    val ahora: Long = System.currentTimeMillis() / 1000
) {
    private val delCurso: List<TrabajoNetacad>
        get() {
            val vigentes = ResumenNetacad.alDia(trabajos, ahora)
            return if (curso == null) vigentes else vigentes.filter { it.curso == curso }
        }

    val secciones: List<SeccionNetacad>
        get() = ResumenNetacad.agrupar(ResumenNetacad.filtrar(delCurso, filtro), ahora)

    val resumen: ResumenActividades get() = ResumenNetacad.resumir(delCurso)

    val cursos: List<String> get() = ResumenNetacad.cursos(trabajos)

    val leidoAlgunaVez: Boolean get() = momento != null

    val configurado: Boolean get() = paginas > 0
}

class NetacadViewModel(
    private val almacen: AlmacenNetacad,
    private val sesion: SesionNetacad,
    private val historial: HistorialAvisos
) : ViewModel() {

    private val _estado = MutableStateFlow(NetacadUiState())
    val estado: StateFlow<NetacadUiState> = _estado.asStateFlow()

    /** Lo leído en el recorrido a oscuras que está en marcha, página a página. */
    private var acumulado: List<TrabajoNetacad> = emptyList()
    private val cola = ArrayDeque<String>()
    private var ultimasPistas: List<String> = emptyList()

    /** Dónde abrir el navegador visible: un trabajo concreto, o el panel si es null. */
    private var paginaVisible: String? = null

    init {
        val guardados = almacen.leer()
        _estado.value = NetacadUiState(
            trabajos = guardados?.trabajos.orEmpty(),
            momento = guardados?.momento,
            paginas = sesion.urls().size
        )
    }

    private fun ahora() = System.currentTimeMillis() / 1000

    /**
     * Refresco pedido por el alumno. Sin ninguna página de curso aprendida no hay nada que
     * recorrer a oscuras, así que se abre NetAcad para que entre y abra su curso.
     */
    fun actualizar() {
        val paginas = sesion.urls()
        if (paginas.isEmpty()) {
            abrirVisible()
            return
        }
        acumulado = emptyList()
        ultimasPistas = emptyList()
        cola.clear()
        cola.addAll(paginas)
        _estado.update {
            it.copy(
                modo = ModoNetacad.OCULTO,
                necesitaAcceso = false,
                sinExito = false,
                pistas = emptyList(),
                ahora = ahora()
            )
        }
    }

    /** Al volver a la app: solo si hay algo que recorrer y la última lectura ya es vieja. */
    fun actualizarSiConviene() {
        // La hora de referencia también envejece: con la app abierta días, «hoy» dejaría de serlo.
        _estado.update { it.copy(ahora = ahora()) }
        val actual = _estado.value
        if (actual.modo != ModoNetacad.NINGUNO) return
        if (!sesion.hay() || sesion.urls().isEmpty()) return
        // Tras un fallo de acceso no se reintenta solo: acabaría igual, y a oscuras.
        if (actual.necesitaAcceso) return
        val momento = actual.momento
        if (momento != null && ahora() - momento < REFRESCO_MINIMO_S) return
        actualizar()
    }

    /**
     * Abre NetAcad a la vista. Con [url] va directo a ese trabajo: dentro de la app está la
     * sesión abierta, y en el navegador del móvil podría no estarlo.
     */
    fun abrirVisible(url: String? = null) {
        paginaVisible = url?.takeIf { it.startsWith("https://") }
        // Abrirlo a mano en mitad de un refresco a oscuras corta ese recorrido; lo que ya
        // hubiera leído vale igual y no se tira.
        if (_estado.value.modo == ModoNetacad.OCULTO && acumulado.isNotEmpty()) {
            guardar(ResumenNetacad.fusionar(_estado.value.trabajos, acumulado))
        }
        acumulado = emptyList()
        cola.clear()
        _estado.update {
            it.copy(
                modo = ModoNetacad.VISIBLE,
                necesitaAcceso = false,
                sinExito = false,
                ultimaLectura = ""
            )
        }
    }

    fun cerrarNavegador() {
        cola.clear()
        _estado.update { it.copy(modo = ModoNetacad.NINGUNO, ahora = ahora()) }
    }

    /** La primera página a cargar: la cola en el recorrido oculto, el panel en el visible. */
    fun paginaInicial(): String =
        if (_estado.value.modo == ModoNetacad.OCULTO) {
            cola.removeFirstOrNull() ?: SesionNetacad.INICIO
        } else {
            paginaVisible ?: SesionNetacad.INICIO
        }

    /** Siguiente página del recorrido oculto; null cuando ya no quedan. */
    fun siguientePagina(): String? = cola.removeFirstOrNull()

    /**
     * Lo que devolvió el guion en una página. Devuelve true si había trabajos, y entonces ya
     * no hace falta seguir mirando esa página.
     */
    fun recibir(resultado: ResultadoNetacad, url: String?): Boolean {
        val leidos = LectorNetacad.leer(resultado, ahora())
        if (leidos.isEmpty()) {
            if (resultado.pistas.isNotEmpty()) ultimasPistas = resultado.pistas
            return false
        }

        // Es una página de curso con trabajos: se aprende para el próximo refresco a oscuras.
        sesion.guardarUrl(url)

        when (_estado.value.modo) {
            ModoNetacad.OCULTO -> acumulado = ResumenNetacad.fusionar(acumulado, leidos)
            ModoNetacad.VISIBLE -> {
                guardar(ResumenNetacad.fusionar(_estado.value.trabajos, leidos))
                val cursos = ResumenNetacad.cursos(leidos)
                _estado.update {
                    it.copy(
                        paginas = sesion.urls().size,
                        ultimaLectura = "${leidos.size} " +
                            (if (leidos.size == 1) "trabajo" else "trabajos") +
                            (if (cursos.isEmpty()) "" else " de " + cursos.joinToString(", "))
                    )
                }
            }
            ModoNetacad.NINGUNO -> Unit
        }
        return true
    }

    /** El recorrido a oscuras se topó con el acceso de Cisco: se para y se pide entrar. */
    fun pedirAcceso() {
        if (_estado.value.modo != ModoNetacad.OCULTO) return
        cola.clear()
        // Lo que sí se leyera antes de toparse con el acceso vale igual.
        if (acumulado.isNotEmpty()) guardar(ResumenNetacad.fusionar(_estado.value.trabajos, acumulado))
        _estado.update { it.copy(modo = ModoNetacad.NINGUNO, necesitaAcceso = true) }
    }

    /** Se acabaron las páginas del recorrido oculto. */
    fun terminarRecorrido() {
        if (_estado.value.modo != ModoNetacad.OCULTO) return
        val hubo = acumulado.isNotEmpty()
        if (hubo) guardar(ResumenNetacad.fusionar(_estado.value.trabajos, acumulado))
        _estado.update {
            it.copy(
                modo = ModoNetacad.NINGUNO,
                sinExito = !hubo,
                pistas = if (hubo) emptyList() else ultimasPistas,
                ahora = ahora()
            )
        }
    }

    fun elegirFiltro(filtro: FiltroNetacad) = _estado.update { it.copy(filtro = filtro) }

    fun elegirCurso(curso: String?) = _estado.update { it.copy(curso = curso) }

    /** Cierra la sesión de NetAcad y de Cisco y olvida todo lo leído y aprendido. */
    fun desconectar() {
        cola.clear()
        acumulado = emptyList()
        sesion.borrar()
        almacen.borrar()
        _estado.value = NetacadUiState()
    }

    private fun guardar(trabajos: List<TrabajoNetacad>) {
        val antes = _estado.value.trabajos
        val momento = ahora()
        almacen.guardar(trabajos, momento)
        avisarDeNuevos(antes, trabajos, momento)
        _estado.update {
            it.copy(
                trabajos = trabajos,
                momento = momento,
                ahora = momento,
                // Si el curso elegido ya no existe, el filtro dejaría la lista vacía sin motivo.
                curso = it.curso?.takeIf { elegido -> trabajos.any { t -> t.curso == elegido } }
            )
        }
    }

    private fun avisarDeNuevos(antes: List<TrabajoNetacad>, ahora: List<TrabajoNetacad>, momento: Long) {
        ResumenNetacad.recienPuestos(antes, ahora)
            .take(MAX_AVISOS)
            .forEach { trabajo ->
                historial.anadir(
                    Aviso(
                        momento = momento,
                        tipo = TipoAviso.NUEVA,
                        titulo = "NetAcad: " + trabajo.titulo,
                        texto = listOf(trabajo.curso, formatearFecha(trabajo.fechaLimite))
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        url = trabajo.url.ifBlank { null }
                    )
                )
            }
    }

    private companion object {
        /** Media hora: volver a la app cada rato no debe recorrer NetAcad cada vez. */
        const val REFRESCO_MINIMO_S = 30 * 60L

        /** Un curso que publica veinte cosas de golpe no debe llenar el historial. */
        const val MAX_AVISOS = 10
    }
}
