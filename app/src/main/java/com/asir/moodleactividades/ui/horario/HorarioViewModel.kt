package com.asir.moodleactividades.ui.horario

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asir.moodleactividades.data.AlmacenHorario
import com.asir.moodleactividades.data.HorarioGuardado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HorarioUiState(
    val cargando: Boolean = false,
    val horario: HorarioGuardado? = null,
    val pagina: Bitmap? = null,
    val indicePagina: Int = 0,
    val totalPaginas: Int = 0,
    val error: String? = null
)

class HorarioViewModel(private val almacen: AlmacenHorario) : ViewModel() {

    private val _estado = MutableStateFlow(HorarioUiState())
    val estado: StateFlow<HorarioUiState> = _estado.asStateFlow()

    init {
        almacen.leer()?.let { abrir(it) }
    }

    fun elegir(uri: Uri) {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val guardado = withContext(Dispatchers.IO) { almacen.guardar(uri) }
            if (guardado == null) {
                _estado.update {
                    it.copy(cargando = false, error = "No se pudo leer ese archivo.")
                }
                return@launch
            }
            abrir(guardado)
        }
    }

    fun quitar() {
        almacen.borrar()
        _estado.value = HorarioUiState()
    }

    fun irAPagina(indice: Int) {
        val horario = _estado.value.horario ?: return
        if (!horario.esPdf) return
        cargarPagina(horario, indice)
    }

    private fun abrir(guardado: HorarioGuardado) {
        _estado.update { it.copy(horario = guardado, cargando = guardado.esPdf, error = null) }
        if (guardado.esPdf) cargarPagina(guardado, 0) else _estado.update { it.copy(cargando = false) }
    }

    private fun cargarPagina(horario: HorarioGuardado, indice: Int) {
        _estado.update { it.copy(cargando = true) }
        viewModelScope.launch {
            val pagina = withContext(Dispatchers.IO) { RenderPdf.renderizar(horario.archivo, indice) }
            _estado.update {
                if (pagina == null) {
                    it.copy(
                        cargando = false,
                        error = "No se pudo abrir el PDF. Prueba a guardarlo como imagen."
                    )
                } else {
                    it.copy(
                        cargando = false,
                        pagina = pagina.imagen,
                        indicePagina = indice,
                        totalPaginas = pagina.total,
                        error = null
                    )
                }
            }
        }
    }
}
