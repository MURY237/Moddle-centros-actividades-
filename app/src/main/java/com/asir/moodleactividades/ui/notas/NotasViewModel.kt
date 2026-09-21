package com.asir.moodleactividades.ui.notas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asir.moodleactividades.data.ActividadesRepository
import com.asir.moodleactividades.data.Conectividad
import com.asir.moodleactividades.data.net.MoodleException
import com.asir.moodleactividades.domain.FiltroNotas
import com.asir.moodleactividades.domain.NotasDeCurso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class NotasUiState(
    val cargando: Boolean = false,
    val todos: List<NotasDeCurso> = emptyList(),
    val cursos: List<NotasDeCurso> = emptyList(),
    val asignaturas: List<String> = emptyList(),
    val asignatura: String? = null,
    val filtro: FiltroNotas = FiltroNotas.TODAS,
    val error: String? = null
) {
    /** Hay algo en pantalla, pero es de una carga anterior y la última consulta falló. */
    val mostrandoDatosAntiguos: Boolean get() = error != null && todos.isNotEmpty()

    private val visibles: List<NotasDeCurso>
        get() = todos.filter { asignatura == null || it.curso == asignatura }

    val totalEvaluables: Int get() = visibles.sumOf { it.calificaciones.size }
    val totalCalificadas: Int get() = visibles.sumOf { it.calificadas }
}

class NotasViewModel(
    private val repositorio: ActividadesRepository,
    private val conectividad: Conectividad
) : ViewModel() {

    private val _estado = MutableStateFlow(NotasUiState())
    val estado: StateFlow<NotasUiState> = _estado.asStateFlow()

    private var ultimaCarga = 0L

    init {
        // Lo guardado se pinta al instante; si la consulta falla, al menos hay notas en
        // pantalla en vez de un error a secas.
        repositorio.notasGuardadas()?.let { guardadas ->
            _estado.update { aplicarFiltro(it.copy(todos = guardadas.cursos)) }
        }
        refrescar()
    }

    fun cambiarFiltro(filtro: FiltroNotas) = _estado.update { aplicarFiltro(it.copy(filtro = filtro)) }

    fun cambiarAsignatura(asignatura: String?) =
        _estado.update { aplicarFiltro(it.copy(asignatura = asignatura)) }

    private fun aplicarFiltro(estado: NotasUiState): NotasUiState {
        val asignaturas = estado.todos.map { it.curso }.distinct()
        val asignatura = estado.asignatura?.takeIf { it in asignaturas }

        val cursos = estado.todos
            .filter { asignatura == null || it.curso == asignatura }
            .mapNotNull { curso ->
                val visibles = when (estado.filtro) {
                    FiltroNotas.TODAS -> curso.calificaciones
                    FiltroNotas.CALIFICADAS -> curso.calificaciones.filter { it.calificada }
                    FiltroNotas.SIN_CALIFICAR -> curso.calificaciones.filterNot { it.calificada }
                }
                if (visibles.isEmpty()) null else curso.copy(calificaciones = visibles)
            }

        return estado.copy(cursos = cursos, asignaturas = asignaturas, asignatura = asignatura)
    }

    /** Igual que en actividades: se refresca al volver, pero no en cada vistazo. */
    fun refrescarSiConviene() {
        val reciente = System.currentTimeMillis() / 1000 - ultimaCarga < FRESCURA_SEGUNDOS
        if (_estado.value.cargando || reciente) return
        refrescar()
    }

    fun refrescar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            runCatching { repositorio.cargarCalificaciones() }.fold(
                onSuccess = { lista ->
                    ultimaCarga = System.currentTimeMillis() / 1000
                    _estado.update {
                        aplicarFiltro(it.copy(cargando = false, todos = lista, error = null))
                    }
                },
                onFailure = { fallo ->
                    _estado.update { it.copy(cargando = false, error = mensajeDeError(fallo)) }
                }
            )
        }
    }

    private companion object {
        const val FRESCURA_SEGUNDOS = 60L
    }

    private fun mensajeDeError(fallo: Throwable): String = when {
        fallo is MoodleException && fallo.esTokenInvalido -> "Tu sesión ha caducado. Vuelve a iniciar sesión."
        fallo is MoodleException -> fallo.message ?: "Error de Moodle."
        fallo is HttpException && fallo.code() in 401..403 ->
            "El servidor del centro ha rechazado la petición. Espera unos segundos y reinténtalo."
        fallo is HttpException -> "El servidor del centro respondió con un error (${fallo.code()})."
        fallo is IOException && !conectividad.hayInternet() -> "Tu móvil no tiene conexión a internet."
        fallo is IOException ->
            "El Moodle del centro no responde. Tu conexión a internet funciona; el problema está " +
                "en el servidor del centro."
        else -> "No se pudieron cargar las notas: ${fallo.message ?: fallo::class.simpleName}"
    }
}
