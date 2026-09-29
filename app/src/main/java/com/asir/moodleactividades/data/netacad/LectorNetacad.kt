package com.asir.moodleactividades.data.netacad

import com.asir.moodleactividades.domain.EstadoActividad
import com.asir.moodleactividades.domain.TrabajoNetacad
import java.time.ZoneId

/**
 * Convierte lo que el guion recogió de la página en trabajos de verdad: interpreta la fecha,
 * decide el estado y tira lo que era ruido de la maquetación.
 *
 * Está aparte del guion aposta. Cisco no publica ninguna API para alumnos, así que la única
 * vía es leer la página; pero cuanto menos criterio tenga el JavaScript y más lo tenga esta
 * clase, más parte del recorrido se puede comprobar con pruebas y menos hay que adivinar.
 */
object LectorNetacad {

    /** Va antes que lo de «hecho»: «incomplete» y «no entregado» contienen la otra palabra. */
    private val SIN_HACER = Regex(
        "not started|no iniciad|sin empezar|sin comenzar|no comenzad|" +
            "in progress|en (?:curso|progreso|marcha)|" +
            "incomplet|sin (?:completar|entregar|hacer|realizar)|" +
            "not submitted|no entregad|pendiente|por (?:entregar|hacer)|overdue|vencid|fuera de plazo"
    )

    private val HECHO = Regex(
        "complet|finalizad|terminad|entregad|realizad|submitted|done|" +
            "passed|aprobad|superad|hecho"
    )

    private val PORCENTAJE = Regex("""(\d{1,3})\s*%""")
    // Los topes de los lados evitan que «5/12/2025» se lea como «5 sobre 12»: una fecha
    // colada en la columna de estado daba por hecha una actividad sin tocar.
    private val PUNTOS = Regex("""(?<![\d/])(\d{1,3}(?:[.,]\d+)?)\s*/\s*(\d{1,3}(?:[.,]\d+)?)(?![\d/])""")

    /** Un título más corto que esto es un icono o un resto de maquetación, no un trabajo. */
    private const val TITULO_MINIMO = 3

    /** Tope de cortesía: una página desbocada no debe llenar el almacén de basura. */
    private const val MAXIMO = 400

    fun leer(
        resultado: ResultadoNetacad,
        ahora: Long,
        zona: ZoneId = ZoneId.systemDefault()
    ): List<TrabajoNetacad> {
        val candidatos = resultado.candidatos.take(MAXIMO)
        // El orden día/mes se decide con todas las fechas a la vez: una sola no lo revela.
        val orden = FechaNetacad.detectarOrden(candidatos.map { it.fecha })

        return candidatos
            .mapNotNull { convertir(it, ahora, orden, zona) }
            // Una misma actividad puede salir en la tabla y en una tarjeta: se queda la que
            // más información traiga, que normalmente es la de la tabla.
            .groupBy { it.id }
            .map { (_, repetidos) -> repetidos.maxByOrNull { riqueza(it) }!! }
            .sortedWith(compareBy({ it.fechaLimite ?: Long.MAX_VALUE }, { it.titulo }))
    }

    private fun convertir(
        candidato: CandidatoNetacad,
        ahora: Long,
        orden: FechaNetacad.Orden,
        zona: ZoneId
    ): TrabajoNetacad? {
        val titulo = candidato.titulo.trim()
        if (titulo.length < TITULO_MINIMO) return null

        val fechaLimite = FechaNetacad.parsear(candidato.fecha, ahora, orden, zona)
        val marcado = marcaDeEstado(candidato)

        // Sin fecha y sin estado no hay nada que mostrar: es texto suelto de la página.
        if (fechaLimite == null && marcado == null) return null

        val curso = candidato.curso.trim()
        return TrabajoNetacad(
            id = identidad(curso, titulo),
            titulo = titulo,
            curso = curso,
            fechaLimite = fechaLimite,
            estado = estado(marcado, fechaLimite, ahora),
            nota = candidato.nota.trim(),
            url = candidato.url.trim()
        )
    }

    /** true = la página dice que está hecho, false = dice que no, null = no dice nada. */
    internal fun marcaDeEstado(candidato: CandidatoNetacad): Boolean? {
        val texto = FechaNetacad.normalizar(candidato.estado + " " + candidato.nota)
        if (texto.isBlank()) return null

        if (SIN_HACER.containsMatchIn(texto)) return false
        if (HECHO.containsMatchIn(texto)) return true

        // Una puntuación por encima de cero solo se saca haciéndolo; un cero puede ser tanto
        // un suspenso como un «ni lo has abierto», así que ahí no se afirma nada.
        PORCENTAJE.find(texto)?.let { c ->
            val valor = c.groupValues[1].toIntOrNull() ?: return@let
            if (valor > 0) return true
        }
        PUNTOS.find(texto)?.let { c ->
            val obtenido = c.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@let
            val sobre = c.groupValues[2].replace(',', '.').toDoubleOrNull() ?: return@let
            // Una puntuación de verdad no pasa de su máximo; si lo pasa, eran otros números.
            if (obtenido > 0 && sobre > 0 && obtenido <= sobre) return true
        }
        return null
    }

    internal fun estado(marcado: Boolean?, fechaLimite: Long?, ahora: Long): EstadoActividad = when {
        marcado == true -> EstadoActividad.ENTREGADA
        fechaLimite != null && fechaLimite in 1 until ahora -> EstadoActividad.NO_ENTREGADA
        else -> EstadoActividad.PENDIENTE
    }

    /**
     * NetAcad no da identificadores a las actividades, así que se compone uno con el curso y
     * el título normalizados: es lo único que no cambia entre dos consultas. Sirve para no
     * repetir filas y para saber qué apareció nuevo desde la vez anterior.
     */
    internal fun identidad(curso: String, titulo: String): String =
        FechaNetacad.normalizar(curso) + "|" + FechaNetacad.normalizar(titulo)

    /** Entre dos lecturas de lo mismo gana la que trae fecha, y luego la que trae nota. */
    private fun riqueza(trabajo: TrabajoNetacad): Int =
        (if (trabajo.fechaLimite != null) 4 else 0) +
            (if (trabajo.nota.isNotBlank()) 2 else 0) +
            (if (trabajo.url.isNotBlank()) 1 else 0)
}
