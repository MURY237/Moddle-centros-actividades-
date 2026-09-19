package com.asir.moodleactividades.ui.actividades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asir.moodleactividades.data.ActividadesRepository
import com.asir.moodleactividades.data.Conectividad
import com.asir.moodleactividades.data.net.MoodleException
import com.asir.moodleactividades.domain.Actividad
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
import java.io.IOException

data class ActividadesUiState(
    val cargando: Boolean = false,
    val nombreUsuario: String = "",
    val nombreSitio: String = "",
    val todas: List<Actividad> = emptyList(),
    val secciones: List<SeccionActividades> = emptyList(),
    val resumen: ResumenActividades = ResumenActividades(0, 0, 0),
    val asignaturas: List<String> = emptyList(),
    val asignatura: String? = null,
    val filtroEstado: FiltroEstado = FiltroEstado.TODAS,
    val rango: RangoTiempo = RangoTiempo.MES,
    val error: String? = null,
    val sesionCaducada: Boolean = false,
    val datosDeCache: Boolean = false,
    val momentoDatos: Long? = null
) {
    /** Hay algo que enseñar, pero es una copia guardada y la última consulta al centro falló. */
    val mostrandoDatosAntiguos: Boolean
        get() = datosDeCache && error != null && todas.isNotEmpty()
}

class ActividadesViewModel(
    private val repositorio: ActividadesRepository,
    private val conectividad: Conectividad
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

    fun cerrarSesion() {
        repositorio.cerrarSesion()
        _estado.update { it.copy(sesionCaducada = true) }
    }

    fun refrescar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            runCatching { repositorio.cargarActividades() }.fold(
                onSuccess = { lista ->
                    _estado.update {
                        recalcular(
                            it.copy(
                                cargando = false,
                                todas = lista,
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
                        it.copy(cargando = false, error = mensajeDeError(fallo), sesionCaducada = caducada)
                    }
                }
            )
        }
    }

    private fun recalcular(estado: ActividadesUiState): ActividadesUiState {
        val ahora = System.currentTimeMillis() / 1000
        val asignaturas = estado.todas.map { it.curso }.filter { it.isNotBlank() }.distinct().sorted()
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
