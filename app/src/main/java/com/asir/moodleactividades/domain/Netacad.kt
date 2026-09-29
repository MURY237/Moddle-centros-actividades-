package com.asir.moodleactividades.domain

import kotlinx.serialization.Serializable

/**
 * Un trabajo de Cisco NetAcad: un examen de capítulo, una práctica de Packet Tracer o
 * cualquier otra cosa entregable con fecha.
 *
 * Reutiliza [EstadoActividad] y [GrupoPlazo] a propósito: son los mismos estados y los
 * mismos tramos de plazo que ya usa la pestaña de Moodle, así que las dos listas se leen
 * igual y no hay que aprenderse dos vocabularios distintos.
 *
 * El identificador es texto y no un número porque NetAcad no publica ninguno: se compone
 * con el curso y el título, que es lo único estable entre dos consultas.
 */
@Serializable
data class TrabajoNetacad(
    val id: String,
    val titulo: String,
    val curso: String,
    /** Segundos desde época, como el resto de la app. Null si la página no daba fecha. */
    val fechaLimite: Long?,
    val estado: EstadoActividad,
    /** Lo que ponía la página: «85%», «Aprobado»… Vacío si no venía nota. */
    val nota: String = "",
    val url: String = ""
) {
    val vencido: Boolean get() = estado == EstadoActividad.NO_ENTREGADA

    /** Queda por hacer: ni entregado ni cerrado. Es lo que el alumno viene a ver. */
    val porEntregar: Boolean get() = estado != EstadoActividad.ENTREGADA
}

data class SeccionNetacad(
    val grupo: GrupoPlazo,
    val trabajos: List<TrabajoNetacad>
)

enum class FiltroNetacad(val etiqueta: String, val estado: EstadoActividad?) {
    TODOS("Todos", null),
    PENDIENTES("Pendientes", EstadoActividad.PENDIENTE),
    VENCIDOS("Fuera de plazo", EstadoActividad.NO_ENTREGADA),
    HECHOS("Hechos", EstadoActividad.ENTREGADA)
}

object ResumenNetacad {

    /**
     * Pasa a «fuera de plazo» lo que se leyó pendiente y ya ha vencido. El estado se decide
     * al leer la página, y sin esto un trabajo leído ayer seguiría «pendiente» hoy aunque su
     * plazo hubiera pasado, que es justo cuando más importa verlo, y más aún sin conexión.
     */
    fun alDia(trabajos: List<TrabajoNetacad>, ahora: Long): List<TrabajoNetacad> =
        trabajos.map { trabajo ->
            val limite = trabajo.fechaLimite
            if (trabajo.estado == EstadoActividad.PENDIENTE && limite != null && limite in 1 until ahora) {
                trabajo.copy(estado = EstadoActividad.NO_ENTREGADA)
            } else {
                trabajo
            }
        }

    /**
     * Agrupa por tramo de plazo delegando en [Clasificador], que es el mismo reparto que usa
     * la pestaña de Moodle: lo vencido arriba, luego hoy, la semana, el mes y el resto.
     */
    fun agrupar(
        trabajos: List<TrabajoNetacad>,
        ahora: Long,
        zona: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): List<SeccionNetacad> =
        trabajos
            .sortedWith(compareBy({ it.fechaLimite ?: Long.MAX_VALUE }, { it.titulo }))
            .groupBy { Clasificador.grupo(it.fechaLimite, ahora, zona) }
            .toSortedMap(compareBy { it.ordinal })
            .map { (grupo, lista) -> SeccionNetacad(grupo, lista) }

    fun filtrar(trabajos: List<TrabajoNetacad>, filtro: FiltroNetacad): List<TrabajoNetacad> =
        if (filtro.estado == null) trabajos else trabajos.filter { it.estado == filtro.estado }

    fun cursos(trabajos: List<TrabajoNetacad>): List<String> =
        trabajos.map { it.curso }.filter { it.isNotBlank() }.distinct().sorted()

    fun resumir(trabajos: List<TrabajoNetacad>): ResumenActividades = ResumenActividades(
        pendientes = trabajos.count { it.estado == EstadoActividad.PENDIENTE },
        entregadas = trabajos.count { it.estado == EstadoActividad.ENTREGADA },
        noEntregadas = trabajos.count { it.estado == EstadoActividad.NO_ENTREGADA }
    )

    /**
     * Lo que no estaba en la consulta anterior, para avisar solo de lo nuevo. Solo cuenta en
     * los cursos que ya se conocían: al añadir un curso por primera vez, todos sus trabajos
     * serían «nuevos» y llegaría una avalancha de avisos de cosas que no lo son.
     */
    fun recienPuestos(antes: List<TrabajoNetacad>, ahora: List<TrabajoNetacad>): List<TrabajoNetacad> {
        if (antes.isEmpty()) return emptyList()
        val conocidos = antes.mapTo(mutableSetOf()) { it.id }
        val cursosConocidos = antes.mapTo(mutableSetOf()) { it.curso }
        return ahora.filter { it.curso in cursosConocidos && it.id !in conocidos }
    }

    /**
     * Mezcla una lectura nueva con lo que ya había, curso a curso. Lo de los cursos recién
     * leídos se sustituye entero, para que desaparezca lo que Cisco haya retirado; lo de los
     * demás se conserva, porque no haberlo mirado ahora no quiere decir que ya no exista.
     */
    fun fusionar(previos: List<TrabajoNetacad>, nuevos: List<TrabajoNetacad>): List<TrabajoNetacad> {
        val cursosNuevos = nuevos.mapTo(mutableSetOf()) { it.curso }
        return (previos.filterNot { it.curso in cursosNuevos } + nuevos)
            .distinctBy { it.id }
            .sortedWith(compareBy({ it.fechaLimite ?: Long.MAX_VALUE }, { it.titulo }))
    }

    /** Merece aviso lo que sigue sin hacer y vence dentro de la ventana indicada. */
    fun porVencer(trabajos: List<TrabajoNetacad>, ahora: Long, ventanaSegundos: Long): List<TrabajoNetacad> =
        trabajos.filter { trabajo ->
            val limite = trabajo.fechaLimite ?: return@filter false
            trabajo.porEntregar && limite > ahora && limite - ahora <= ventanaSegundos
        }
}
