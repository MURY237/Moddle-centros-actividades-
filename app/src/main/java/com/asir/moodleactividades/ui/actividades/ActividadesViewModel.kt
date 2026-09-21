package com.asir.moodleactividades.ui.actividades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asir.moodleactividades.data.ActividadesRepository
import com.asir.moodleactividades.data.Conectividad
import com.asir.moodleactividades.data.DescargaAdjuntos
import com.asir.moodleactividades.data.net.MoodleException
import com.asir.moodleactividades.domain.Actividad
import com.asir.moodleactividades.domain.Adjunto
import com.asir.moodleactividades.domain.Clasificador
import com.asir.moodleactividades.domain.FiltroEstado
import com.asir.moodleactividades.domain.RangoTiempo
import com.asir.moodleactividades.domain.ResumenActividades
import com.asir.moodleactividades.domain.SeccionActividades
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.io.IOException

enum class EstadoDescarga { PENDIENTE, DESCARGANDO, LISTA, ERROR }

data class DescargaUi(
    val estado: EstadoDescarga = EstadoDescarga.PENDIENTE,
    val archivo: File? = null
)

data class ActividadesUiState(
    val cargando: Boolean = false,
    val nombreUsuario: String = "",
    val nombreSitio: String = "",
    val todas: List<Actividad> = emptyList(),
    val secciones: List<SeccionActividades> = emptyList(),
    val resumen: ResumenActividades = ResumenActividades(0, 0, 0),
    val asignaturas: List<String> = emptyList(),
    /** Lo que dice la matrícula, que incluye asignaturas que aún no tienen ni una actividad. */
    val matriculadas: List<String> = emptyList(),
    val asignatura: String? = null,
    val filtroEstado: FiltroEstado = FiltroEstado.TODAS,
    val rango: RangoTiempo = RangoTiempo.MES,
    val error: String? = null,
    val sesionCaducada: Boolean = false,
    val datosDeCache: Boolean = false,
    val momentoDatos: Long? = null,
    val detalle: Actividad? = null,
    val descargas: Map<String, DescargaUi> = emptyMap()
) {
    /** Hay algo que enseñar, pero es una copia guardada y la última consulta al centro falló. */
    val mostrandoDatosAntiguos: Boolean
        get() = datosDeCache && error != null && todas.isNotEmpty()
}

class ActividadesViewModel(
    private val repositorio: ActividadesRepository,
    private val conectividad: Conectividad,
    private val descargas: DescargaAdjuntos
) : ViewModel() {

    private val _estado = MutableStateFlow(ActividadesUiState())
    val estado: StateFlow<ActividadesUiState> = _estado.asStateFlow()

    init {
        repositorio.sesionGuardada()?.let { sesion ->
            _estado.update {
                it.copy(nombreUsuario = sesion.nombreCompleto, nombreSitio = sesion.nombreSitio)
            }
        }
        repositorio.instantaneaGuardada()?.let { guardada ->
            _estado.update {
                recalcular(
                    it.copy(
                        todas = guardada.actividades,
                        matriculadas = guardada.asignaturas,
                        datosDeCache = true,
                        momentoDatos = guardada.momento
                    )
                )
            }
        }
        refrescar()
    }

    fun cambiarFiltro(filtro: FiltroEstado) =
        _estado.update { recalcular(it.copy(filtroEstado = filtro)) }

    fun cambiarRango(rango: RangoTiempo) =
        _estado.update { recalcular(it.copy(rango = rango)) }

    fun cambiarAsignatura(asignatura: String?) =
        _estado.update { recalcular(it.copy(asignatura = asignatura)) }

    fun abrirDetalle(actividad: Actividad) {
        // El estado de cada adjunto se recalcula al abrir: si la caché conserva el archivo de
        // otra vez, la ficha ya sale con el botón de abrir en lugar del de descargar.
        val estados = actividad.adjuntos.associate { adjunto ->
            val guardado = descargas.yaDescargado(adjunto)
            adjunto.url to DescargaUi(
                estado = if (guardado != null) EstadoDescarga.LISTA else EstadoDescarga.PENDIENTE,
                archivo = guardado
            )
        }
        _estado.update { it.copy(detalle = actividad, descargas = estados) }
    }

    fun cerrarDetalle() = _estado.update { it.copy(detalle = null) }

    fun descargar(adjunto: Adjunto) {
        if (_estado.value.descargas[adjunto.url]?.estado == EstadoDescarga.DESCARGANDO) return
        cambiarDescarga(adjunto, DescargaUi(EstadoDescarga.DESCARGANDO))
        viewModelScope.launch {
            val archivo = runCatching { descargas.descargar(adjunto) }.getOrNull()
            cambiarDescarga(
                adjunto,
                if (archivo == null) {
                    DescargaUi(EstadoDescarga.ERROR)
                } else {
                    DescargaUi(EstadoDescarga.LISTA, archivo)
                }
            )
        }
    }

    fun intentAbrir(adjunto: Adjunto, archivo: File) = descargas.intentAbrir(archivo, adjunto.tipo)

    fun intentCompartir(adjunto: Adjunto, archivo: File) =
        descargas.intentCompartir(archivo, adjunto.tipo)

    private fun cambiarDescarga(adjunto: Adjunto, descarga: DescargaUi) =
        _estado.update { it.copy(descargas = it.descargas + (adjunto.url to descarga)) }

    fun cerrarSesion() {
        repositorio.cerrarSesion()
        _estado.update { it.copy(sesionCaducada = true) }
    }

    /**
     * Al volver a la app se refresca, pero no en cada vistazo: los cortafuegos de centro
     * cortan por exceso de peticiones seguidas y la lista se quedaría vacía.
     */
    fun refrescarSiConviene() {
        val momento = _estado.value.momentoDatos
        val reciente = momento != null &&
            System.currentTimeMillis() / 1000 - momento < FRESCURA_SEGUNDOS
        if (_estado.value.cargando || reciente) return
        refrescar()
    }

    fun refrescar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            runCatching { repositorio.cargarActividades() }.fold(
                onSuccess = { lista ->
                    // La matrícula se guarda junto a las actividades en la misma carga.
                    val matriculadas = repositorio.instantaneaGuardada()?.asignaturas.orEmpty()
                    _estado.update { previo ->
                        recalcular(
                            previo.copy(
                                cargando = false,
                                todas = lista,
                                matriculadas = matriculadas,
                                // La ficha abierta se queda con los datos de la carga anterior
                                // si no se vuelve a buscar en la lista recién traída.
                                detalle = previo.detalle?.let { abierta ->
                                    lista.firstOrNull {
                                        it.id == abierta.id && it.tipo == abierta.tipo
                                    } ?: abierta
                                },
                                error = null,
                                datosDeCache = false,
                                momentoDatos = System.currentTimeMillis() / 1000
                            )
                        )
                    }
                },
                onFailure = { fallo ->
                    val caducada = fallo is MoodleException && fallo.esTokenInvalido
                    if (caducada) repositorio.cerrarSesion()
                    _estado.update {
                        it.copy(
                            cargando = false,
                            error = mensajeDeError(fallo),
                            sesionCaducada = caducada,
                            // Lo que sigue en pantalla es de la carga anterior: sin marcarlo,
                            // un fallo tras una carga correcta no se avisaba por ningún lado.
                            datosDeCache = it.todas.isNotEmpty()
                        )
                    }
                }
            )
        }
    }

    private fun recalcular(estado: ActividadesUiState): ActividadesUiState {
        val ahora = System.currentTimeMillis() / 1000
        // Se unen las dos fuentes: una asignatura recién creada está en la matrícula pero
        // todavía no en ninguna actividad, y aun así tiene que poder elegirse.
        val asignaturas = (estado.todas.map { it.curso } + estado.matriculadas)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val asignatura = estado.asignatura?.takeIf { it in asignaturas }

        val deLaAsignatura = estado.todas.filter { asignatura == null || it.curso == asignatura }
        val visibles = deLaAsignatura
            .filter { estado.filtroEstado.estado == null || it.estado == estado.filtroEstado.estado }
            .filter { Clasificador.dentroDelRango(it, estado.rango, ahora) }

        return estado.copy(
            asignaturas = asignaturas,
            asignatura = asignatura,
            secciones = Clasificador.agrupar(visibles, ahora),
            resumen = Clasificador.resumir(deLaAsignatura)
        )
    }

    private companion object {
        const val FRESCURA_SEGUNDOS = 60L
    }

    private fun mensajeDeError(fallo: Throwable): String = when {
        fallo is MoodleException && fallo.esTokenInvalido -> "Tu sesión ha caducado. Vuelve a iniciar sesión."
        fallo is MoodleException -> fallo.message ?: "Error de Moodle."
        fallo is HttpException && fallo.code() in 401..403 ->
            "El servidor del centro ha rechazado la petición. Suele ser un bloqueo temporal por " +
                "hacer varias consultas seguidas: espera unos segundos y vuelve a intentarlo."
        fallo is HttpException ->
            "El servidor del centro respondió con un error (${fallo.code()}). Inténtalo de nuevo."
        fallo is IOException && !conectividad.hayInternet() ->
            "Tu móvil no tiene conexión a internet."
        fallo is IOException ->
            "El Moodle del centro no responde. Tu conexión a internet funciona; el problema está " +
                "en el servidor del centro."
        else -> "No se pudieron cargar las actividades: ${fallo.message ?: fallo::class.simpleName}"
    }
}
