package com.asir.moodleactividades.ui.bus

import androidx.lifecycle.ViewModel
import com.asir.moodleactividades.data.AlmacenBus
import com.asir.moodleactividades.domain.HorariosBus
import com.asir.moodleactividades.domain.LineaBus
import com.asir.moodleactividades.domain.ProximaSalida
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

data class LineaConSalida(
    val linea: LineaBus,
    val proxima: ProximaSalida?,
    val salidasDeHoy: List<Int>
)

data class BusUiState(
    val lineas: List<LineaConSalida> = emptyList(),
    val minutoAhora: Int = 0,
    val diaHoy: Int = 1
) {
    /** La que sale antes de todas: es lo que interesa ver nada más abrir. */
    val siguiente: LineaConSalida?
        get() = lineas
            .filter { it.proxima != null }
            .minByOrNull { it.proxima!!.minutosQueFaltan(minutoAhora) }
}

class BusViewModel(private val almacen: AlmacenBus) : ViewModel() {

    private val _estado = MutableStateFlow(BusUiState())
    val estado: StateFlow<BusUiState> = _estado.asStateFlow()

    private var lineas: List<LineaBus> = emptyList()

    init {
        lineas = almacen.leer()
        recalcular()
    }

    /**
     * La cuenta atrás se rehace al volver a la pantalla en vez de con un temporizador: un
     * reloj corriendo en segundo plano gastaría batería para algo que se mira de pasada.
     */
    fun recalcular() {
        val ahora = LocalDateTime.now()
        val minuto = ahora.hour * 60 + ahora.minute
        val dia = ahora.dayOfWeek.value

        _estado.value = BusUiState(
            lineas = lineas.map {
                LineaConSalida(
                    linea = it,
                    proxima = HorariosBus.proxima(it, dia, minuto),
                    salidasDeHoy = HorariosBus.salidasDeHoy(it, dia)
                )
            },
            minutoAhora = minuto,
            diaHoy = dia
        )
    }

    fun anadir(nombre: String, horasEscritas: String, dias: Set<Int>) {
        val horas = horasEscritas.split(',', ' ', '\n')
            .mapNotNull { HorariosBus.parsearHora(it) }
        if (nombre.isBlank() && horas.isEmpty()) return

        val nueva = LineaBus(
            id = System.currentTimeMillis(),
            nombre = nombre.trim().ifBlank { "Mi autobús" },
            horas = horas,
            dias = dias.ifEmpty { LineaBus.LABORABLES }
        )
        guardar(lineas + nueva)
    }

    fun borrar(id: Long) = guardar(lineas.filterNot { it.id == id })

    private fun guardar(nuevas: List<LineaBus>) {
        lineas = nuevas
        almacen.guardar(nuevas)
        recalcular()
    }
}
