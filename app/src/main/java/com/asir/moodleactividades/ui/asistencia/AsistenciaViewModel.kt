package com.asir.moodleactividades.ui.asistencia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asir.moodleactividades.data.ActividadesRepository
import com.asir.moodleactividades.data.net.MoodleException
import com.asir.moodleactividades.domain.SondeoAsistencia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class AsistenciaUiState(
    val cargando: Boolean = false,
    val sondeo: SondeoAsistencia? = null,
    val error: String? = null
)

class AsistenciaViewModel(private val repositorio: ActividadesRepository) : ViewModel() {

    private val _estado = MutableStateFlow(AsistenciaUiState())
    val estado: StateFlow<AsistenciaUiState> = _estado.asStateFlow()

    fun comprobar() {
        if (_estado.value.cargando) return
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            runCatching { repositorio.sondearAsistencia() }.fold(
                onSuccess = { resultado ->
                    _estado.value = AsistenciaUiState(sondeo = resultado)
                },
                onFailure = { fallo ->
                    _estado.value = AsistenciaUiState(error = mensajeDeError(fallo))
                }
            )
        }
    }

    private fun mensajeDeError(fallo: Throwable): String = when {
        fallo is MoodleException && fallo.esTokenInvalido ->
            "Tu sesión ha caducado. Vuelve a iniciar sesión."
        fallo is MoodleException -> fallo.message ?: "Error de Moodle."
        fallo is HttpException ->
            "El servidor del centro respondió con un error (${fallo.code()})."
        fallo is IOException -> "No se pudo contactar con el Moodle del centro."
        else -> "No se pudo comprobar: ${fallo.message ?: fallo::class.simpleName}"
    }
}
