package com.asir.moodleactividades.ui.avisos

import androidx.lifecycle.ViewModel
import com.asir.moodleactividades.data.Aviso
import com.asir.moodleactividades.data.HistorialAvisos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AvisosUiState(
    val avisos: List<Aviso> = emptyList(),
    val sinLeer: Int = 0
)

class AvisosViewModel(private val historial: HistorialAvisos) : ViewModel() {

    private val _estado = MutableStateFlow(AvisosUiState())
    val estado: StateFlow<AvisosUiState> = _estado.asStateFlow()

    // El trabajo en segundo plano escribe mientras la app está abierta: sin escuchar, la lista
    // se quedaría congelada hasta el siguiente arranque.
    private val oyente = historial.escuchar { recargar() }

    init {
        recargar()
    }

    fun recargar() {
        _estado.value = AvisosUiState(avisos = historial.leer(), sinLeer = historial.sinLeer())
    }

    /** Al abrir la pestaña se dan por vistos, que es lo que apaga el contador. */
    fun marcarLeidos() {
        historial.marcarLeidos()
        _estado.value = _estado.value.copy(sinLeer = 0)
    }

    fun vaciar() {
        historial.borrar()
        recargar()
    }

    override fun onCleared() {
        historial.dejarDeEscuchar(oyente)
        super.onCleared()
    }
}
