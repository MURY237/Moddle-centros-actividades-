package com.asir.moodleactividades.ui.actualizacion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asir.moodleactividades.BuildConfig
import com.asir.moodleactividades.actualizacion.Instalador
import com.asir.moodleactividades.data.net.Actualizacion
import com.asir.moodleactividades.data.net.Actualizaciones
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ActualizacionUiState(
    val disponible: Actualizacion? = null,
    val descargando: Boolean = false,
    val descartada: Boolean = false,
    val error: String? = null,
    val faltaPermiso: Boolean = false
) {
    val visible: Boolean get() = disponible != null && !descartada
}

class ActualizacionViewModel : ViewModel() {

    private val actualizaciones = Actualizaciones(BuildConfig.VERSION_NAME)

    private val _estado = MutableStateFlow(ActualizacionUiState())
    val estado: StateFlow<ActualizacionUiState> = _estado.asStateFlow()

    fun comprobar() {
        viewModelScope.launch {
            val encontrada = withContext(Dispatchers.IO) { actualizaciones.buscar() }
            _estado.update { it.copy(disponible = encontrada) }
        }
    }

    fun descartar() = _estado.update { it.copy(descartada = true) }

    fun instalar(contexto: Context) {
        val actualizacion = _estado.value.disponible ?: return

        if (!Instalador.puedeInstalar(contexto)) {
            _estado.update { it.copy(faltaPermiso = true) }
            Instalador.pedirPermisoDeInstalacion(contexto)
            return
        }

        _estado.update { it.copy(descargando = true, error = null, faltaPermiso = false) }
        viewModelScope.launch {
            val destino = Instalador.archivoDestino(contexto)
            val bajado = withContext(Dispatchers.IO) {
                actualizaciones.descargar(actualizacion.urlApk, destino)
            }

            if (!bajado) {
                _estado.update {
                    it.copy(descargando = false, error = "No se pudo descargar la actualización.")
                }
                return@launch
            }

            val lanzado = Instalador.instalar(contexto, destino)
            _estado.update {
                it.copy(
                    descargando = false,
                    error = if (lanzado) null else "No se pudo abrir el instalador de Android."
                )
            }
        }
    }
}
