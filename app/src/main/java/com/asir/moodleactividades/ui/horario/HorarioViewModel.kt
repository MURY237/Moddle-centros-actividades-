package com.asir.moodleactividades.ui.horario

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asir.moodleactividades.data.AlmacenHorario
import com.asir.moodleactividades.data.HorarioGuardado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HorarioUiState(
    val cargando: Boolean = false,
    val horario: HorarioGuardado? = null,
    /** Lo que se pinta: la página del PDF o la foto, ya reducida. */
    val pagina: Bitmap? = null,
    val indicePagina: Int = 0,
    val totalPaginas: Int = 0,
    val error: String? = null
)

class HorarioViewModel(private val almacen: AlmacenHorario) : ViewModel() {

    private val _estado = MutableStateFlow(HorarioUiState())
    val estado: StateFlow<HorarioUiState> = _estado.asStateFlow()

    /** La carga en curso: al cambiar o quitar el horario se cancela para que no pise nada. */
    private var carga: Job? = null

    init {
        almacen.leer()?.let { abrir(it) }
    }

    fun elegir(uri: Uri) {
        carga?.cancel()
        _estado.update { it.copy(cargando = true, error = null) }
        carga = viewModelScope.launch {
            val guardado = withContext(Dispatchers.IO) { almacen.guardar(uri) }
            if (guardado == null) {
                _estado.update { it.copy(cargando = false, error = "No se pudo leer ese archivo.") }
                return@launch
            }
            abrir(guardado)
        }
    }

    fun quitar() {
        carga?.cancel()
        almacen.borrar()
        _estado.value = HorarioUiState()
    }

    fun irAPagina(indice: Int) {
        val horario = _estado.value.horario ?: return
        if (!horario.esPdf || indice !in 0 until _estado.value.totalPaginas) return
        cargar(horario, indice)
    }

    private fun abrir(guardado: HorarioGuardado) {
        _estado.update { it.copy(horario = guardado, pagina = null, indicePagina = 0, totalPaginas = 0) }
        cargar(guardado, 0)
    }

    private fun cargar(horario: HorarioGuardado, indice: Int) {
        carga?.cancel()
        _estado.update { it.copy(cargando = true, error = null) }
        carga = viewModelScope.launch {
            val pagina = withContext(Dispatchers.IO) {
                if (horario.esPdf) {
                    RenderPdf.renderizar(horario.archivo, indice)
                } else {
                    RenderPdf.decodificarImagen(horario.archivo)?.let { PaginaPdf(it, 1) }
                }
            }
            _estado.update {
                if (pagina == null) {
                    it.copy(
                        cargando = false,
                        error = if (horario.esPdf) {
                            "No se pudo abrir el PDF. Prueba a guardarlo como imagen."
                        } else {
                            "No se pudo abrir la imagen."
                        }
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
