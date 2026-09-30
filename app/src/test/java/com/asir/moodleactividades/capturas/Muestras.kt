package com.asir.moodleactividades.capturas

import com.asir.moodleactividades.domain.Actividad
import com.asir.moodleactividades.domain.Adjunto
import com.asir.moodleactividades.domain.Clasificador
import com.asir.moodleactividades.domain.EstadoActividad
import com.asir.moodleactividades.domain.TipoActividad
import com.asir.moodleactividades.ui.actividades.ActividadesUiState

/** Datos inventados, con fechas relativas a hoy para que los grupos por plazo salgan llenos. */
object Muestras {

    private const val HORA = 3_600_000L
    private const val DIA = 24 * HORA

    val ahora: Long = System.currentTimeMillis()

    val actividades: List<Actividad> = listOf(
        Actividad(
            id = 1, nombre = "Práctica 4: ACL extendidas en Packet Tracer",
            curso = "Planificación y Administración de Redes", tipo = TipoActividad.TAREA,
            fechaLimite = ahora - 2 * DIA, estado = EstadoActividad.NO_ENTREGADA,
            calificada = false, url = null
        ),
        Actividad(
            id = 2, nombre = "Cuestionario tema 5: RAID y LVM",
            curso = "Administración de Sistemas Operativos", tipo = TipoActividad.CUESTIONARIO,
            fechaLimite = ahora + 5 * HORA, estado = EstadoActividad.PENDIENTE,
            calificada = false, url = null
        ),
        Actividad(
            id = 3, nombre = "Práctica 3: VLAN y enrutamiento entre VLAN",
            curso = "Planificación y Administración de Redes", tipo = TipoActividad.TAREA,
            fechaLimite = ahora + 3 * DIA, estado = EstadoActividad.PENDIENTE,
            calificada = false, url = null,
            descripcion = "<p>Configura la topología del enunciado en <b>Packet Tracer</b>:</p>" +
                "<ul><li>Tres VLAN (10, 20 y 99 de gestión).</li>" +
                "<li>Router-on-a-stick con subinterfaces 802.1Q.</li>" +
                "<li>DHCP en el router para las VLAN de usuarios.</li></ul>" +
                "<p>Entrega el fichero <i>.pkt</i> y una memoria en PDF.</p>",
            adjuntos = listOf(
                Adjunto("enunciado-practica3.pdf", "https://moodle.ejemplo.es/a.pdf", 482_311, "application/pdf"),
                Adjunto("topologia-base.pkt", "https://moodle.ejemplo.es/b.pkt", 91_204, "")
            )
        ),
        Actividad(
            id = 4, nombre = "Foro: dudas sobre Active Directory",
            curso = "Implantación de Sistemas Operativos", tipo = TipoActividad.FORO,
            fechaLimite = ahora + 6 * DIA, estado = EstadoActividad.ENTREGADA,
            calificada = false, url = null
        ),
        Actividad(
            id = 5, nombre = "Consultas con GROUP BY y subconsultas",
            curso = "Gestión de Bases de Datos", tipo = TipoActividad.TAREA,
            fechaLimite = ahora + 12 * DIA, estado = EstadoActividad.ENTREGADA,
            calificada = true, url = null, nota = "8,50"
        ),
        Actividad(
            id = 6, nombre = "Script de copias de seguridad en Bash",
            curso = "Administración de Sistemas Operativos", tipo = TipoActividad.TAREA,
            fechaLimite = ahora + 20 * DIA, estado = EstadoActividad.PENDIENTE,
            calificada = false, url = null
        )
    )

    val tareas = ActividadesUiState(
        nombreUsuario = "Alumno de ASIR",
        nombreSitio = "Moodle Centros",
        todas = actividades,
        secciones = Clasificador.agrupar(actividades, ahora),
        resumen = Clasificador.resumir(actividades),
        asignaturas = actividades.map { it.curso }.distinct().sorted(),
        momentoDatos = ahora
    )
}
