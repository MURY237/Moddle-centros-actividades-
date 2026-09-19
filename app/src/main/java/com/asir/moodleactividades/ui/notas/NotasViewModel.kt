package com.asir.moodleactividades.ui.notas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asir.moodleactividades.data.ActividadesRepository
import com.asir.moodleactividades.data.Conectividad
import com.asir.moodleactividades.data.net.MoodleException
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
    val cursos: List<NotasDeCurso> = emptyList(),
    val error: String? = null
)

class NotasViewModel(
    private val repositorio: ActividadesRepository,
    private val conectividad: Conectividad
) : ViewModel() {

    private val _estado = MutableStateFlow(NotasUiState())
    val estado: StateFlow<NotasUiState> = _estado.asStateFlow()

    init {
        refrescar()
    }

    fun refrescar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            runCatching { repositorio.cargarCalificaciones() }.fold(
                onSuccess = { lista ->
                    _estado.update { it.copy(cargando = false, cursos = lista, error = null) }
                },
                onFailure = { fallo ->
                    _estado.update { it.copy(cargando = false, error = mensajeDeError(fallo)) }
                }
            )
        }
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
