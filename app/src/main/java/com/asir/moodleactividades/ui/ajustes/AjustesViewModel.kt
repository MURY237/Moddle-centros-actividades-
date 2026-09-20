package com.asir.moodleactividades.ui.ajustes

import android.content.Context
import androidx.lifecycle.ViewModel
import com.asir.moodleactividades.data.AjustesAvisos
import com.asir.moodleactividades.data.PreferenciasAvisos
import com.asir.moodleactividades.notificaciones.RecordatoriosWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AjustesViewModel(private val preferencias: PreferenciasAvisos) : ViewModel() {

    private val _estado = MutableStateFlow(preferencias.leer())
    val estado: StateFlow<AjustesAvisos> = _estado.asStateFlow()

    fun cambiarAvisoEntregas(activo: Boolean, contexto: Context) =
        aplicar(_estado.value.copy(avisarEntregas = activo), contexto)

    fun cambiarAvisoNuevas(activo: Boolean, contexto: Context) =
        aplicar(_estado.value.copy(avisarNuevas = activo), contexto)

    fun cambiarAntelacion(horas: Int, contexto: Context) =
        aplicar(_estado.value.copy(antelacionHoras = horas), contexto)

    fun cambiarFrecuencia(veces: Int, contexto: Context) =
        aplicar(_estado.value.copy(comprobacionesDiarias = veces), contexto)

    fun cambiarHora(hora: Int, contexto: Context) =
        aplicar(_estado.value.copy(horaPreferida = hora), contexto)

    private fun aplicar(nuevos: AjustesAvisos, contexto: Context) {
        preferencias.guardar(nuevos)
        _estado.value = nuevos
        // El trabajo en segundo plano lleva dentro el periodo y la hora, así que cada cambio
        // obliga a reprogramarlo.
        RecordatoriosWorker.programar(contexto, nuevos)
    }
}
