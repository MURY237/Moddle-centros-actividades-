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
     * Rehace la cuenta atrás. La pantalla la llama al entrar y una vez por minuto mientras
     * está a la vista; en segundo plano no corre nada.
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

    /**
     * Guarda la línea y devuelve null, o devuelve por qué no la guarda. Antes una hora mal
     * escrita se descartaba en silencio y podía quedar guardada una línea sin ninguna salida.
     */
    fun anadir(nombre: String, horasEscritas: String, dias: Set<Int>): String? {
        val trozos = horasEscritas.split(',', ';', ' ', '\n').filter { it.isNotBlank() }
        val horas = trozos.mapNotNull { HorariosBus.parsearHora(it) }
        val malas = trozos.filter { HorariosBus.parsearHora(it) == null }
        when {
            horas.isEmpty() -> return "Escribe al menos una hora, por ejemplo 07:15."
            malas.isNotEmpty() -> return "No se entiende: " + malas.joinToString(", ") { "«${it.trim()}»" }
        }

        val nueva = LineaBus(
            id = System.currentTimeMillis(),
            nombre = nombre.trim().ifBlank { "Mi autobús" },
            horas = horas,
            dias = dias.ifEmpty { LineaBus.LABORABLES }
        )
        guardar(lineas + nueva)
        return null
    }

    fun borrar(id: Long) = guardar(lineas.filterNot { it.id == id })

    private fun guardar(nuevas: List<LineaBus>) {
        lineas = nuevas
        almacen.guardar(nuevas)
        recalcular()
    }
}
