package com.asir.moodleactividades.capturas

import com.asir.moodleactividades.data.Aviso
import com.asir.moodleactividades.data.TipoAviso
import com.asir.moodleactividades.domain.Actividad
import com.asir.moodleactividades.domain.Adjunto
import com.asir.moodleactividades.domain.Calificacion
import com.asir.moodleactividades.domain.Clasificador
import com.asir.moodleactividades.domain.EstadoActividad
import com.asir.moodleactividades.domain.Falta
import com.asir.moodleactividades.domain.ResumenFaltas
import com.asir.moodleactividades.domain.TrabajoNetacad
import com.asir.moodleactividades.ui.netacad.NetacadUiState
import com.asir.moodleactividades.ui.faltas.FaltasUiState
import com.asir.moodleactividades.domain.NotasDeCurso
import com.asir.moodleactividades.domain.TipoActividad
import com.asir.moodleactividades.ui.actividades.ActividadesUiState
import com.asir.moodleactividades.domain.HorariosBus
import com.asir.moodleactividades.domain.LineaBus
import com.asir.moodleactividades.ui.avisos.AvisosUiState
import com.asir.moodleactividades.ui.bus.BusUiState
import com.asir.moodleactividades.ui.bus.LineaConSalida
import com.asir.moodleactividades.ui.notas.NotasUiState

/**
 * Datos inventados, con fechas relativas a hoy para que los grupos por plazo salgan llenos.
 * Como en toda la app, los momentos van en segundos desde 1970.
 */
object Muestras {

    const val HORA = 3_600L
    const val DIA = 24 * HORA

    val ahora: Long = System.currentTimeMillis() / 1000

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

    private fun nota(curso: String, nombre: String, nota: String, tipo: TipoActividad = TipoActividad.TAREA,
                     maxima: Double = 10.0, dias: Int = 10, url: String? = "https://moodle.ejemplo.es") =
        Calificacion(curso, nombre, nota, porcentaje = "", notaMaxima = maxima, esTotalDelCurso = false,
            tipo = tipo, fecha = ahora - dias * DIA, url = url)

    private val redes = "Planificación y Administración de Redes"
    private val sistemas = "Administración de Sistemas Operativos"
    private val bases = "Gestión de Bases de Datos"

    val cursos: List<NotasDeCurso> = listOf(
        NotasDeCurso(
            redes,
            total = nota(redes, "Total del curso", "7,35").copy(esTotalDelCurso = true),
            calificaciones = listOf(
                nota(redes, "Práctica 1: subnetting VLSM", "8,50", dias = 30),
                nota(redes, "Práctica 2: STP y EtherChannel", "6,75", dias = 18),
                nota(redes, "Cuestionario OSPF", "4,20", TipoActividad.CUESTIONARIO, dias = 6),
                nota(redes, "Práctica 3: VLAN y enrutamiento entre VLAN", "", url = null)
            )
        ),
        NotasDeCurso(
            sistemas,
            total = null,
            calificaciones = listOf(
                nota(sistemas, "Cuestionario tema 5: RAID y LVM", "92,00", TipoActividad.CUESTIONARIO, maxima = 100.0),
                nota(sistemas, "Script de copias de seguridad en Bash", "Apto")
            )
        ),
        NotasDeCurso(bases, total = null, calificaciones = emptyList())
    )

    val notas = NotasUiState(
        todos = cursos,
        cursos = cursos,
        asignaturas = cursos.map { it.curso }
    )

    val avisos = AvisosUiState(
        avisos = listOf(
            Aviso(ahora - 20 * 60, TipoAviso.NOTA, "Nueva nota: Cuestionario OSPF",
                "Planificación y Administración de Redes · 4,20", "https://moodle.ejemplo.es", id = "1"),
            Aviso(ahora - 3 * HORA, TipoAviso.ENTREGA, "Vence mañana: Práctica 3",
                "VLAN y enrutamiento entre VLAN. Aún no la has entregado.", "https://moodle.ejemplo.es", id = "2"),
            Aviso(ahora - DIA, TipoAviso.FALTA, "Falta en Implantación de Sistemas Operativos",
                "Injustificada · 1.ª hora", id = "3"),
            Aviso(ahora - 3 * DIA, TipoAviso.NUEVA, "Nueva actividad: Script de copias en Bash",
                "Administración de Sistemas Operativos · entrega en 20 días", "https://moodle.ejemplo.es", id = "4"),
            Aviso(ahora - 12 * DIA, TipoAviso.NOTA, "Nueva nota: Práctica 1",
                "Planificación y Administración de Redes · 8,50", id = "5")
        )
    )

    private val faltasDeEjemplo = listOf(
        Falta("22/09/2026", "1.ª hora", "Implantación de Sistemas Operativos", "Injustificada"),
        Falta("15/09/2026", "4.ª hora", "Implantación de Sistemas Operativos", "Justificada"),
        Falta("18/09/2026", "2.ª hora", "Planificación y Administración de Redes", "Justificada"),
        Falta("26/09/2026", "5.ª hora", "Gestión de Bases de Datos", "Injustificada"),
        Falta("25/09/2026", "6.ª hora", "Gestión de Bases de Datos", "Injustificada")
    )

    val faltas = FaltasUiState(
        faltas = faltasDeEjemplo,
        porAsignatura = ResumenFaltas.porAsignatura(faltasDeEjemplo),
        momento = ahora - 2 * HORA,
        leidoAlgunaVez = true,
        usuarioGuardado = "alumno.asir"
    )

    val netacad = NetacadUiState(
        trabajos = listOf(
            TrabajoNetacad("1", "Checkpoint Exam: Ethernet Concepts", "CCNA 1: Introduction to Networks",
                ahora - 3 * DIA, EstadoActividad.NO_ENTREGADA),
            TrabajoNetacad("2", "Packet Tracer - Configure Initial Switch Settings", "CCNA 1: Introduction to Networks",
                ahora + 2 * DIA, EstadoActividad.PENDIENTE, url = "https://www.netacad.com/"),
            TrabajoNetacad("3", "Modules 11 - 13: IP Addressing Exam", "CCNA 1: Introduction to Networks",
                ahora + 9 * DIA, EstadoActividad.PENDIENTE, url = "https://www.netacad.com/"),
            TrabajoNetacad("4", "Modules 1 - 3: Basic Network Connectivity Exam", "CCNA 1: Introduction to Networks",
                ahora - 20 * DIA, EstadoActividad.ENTREGADA, nota = "92%"),
            TrabajoNetacad("5", "Modules 1 - 4: Switching Concepts Exam", "CCNA 2: Switching, Routing",
                ahora + 25 * DIA, EstadoActividad.PENDIENTE)
        ),
        momento = ahora - 40 * 60,
        paginas = 2,
        ahora = ahora
    )

    /** Un lunes a las 7:40, con la ida que sale en diez minutos y la vuelta por la tarde. */
    val bus: BusUiState = run {
        val lunes = 1
        val minuto = 7 * 60 + 40
        val lineas = listOf(
            LineaBus(1, "Pueblo → Instituto", listOf(6 * 60 + 50, 7 * 60 + 20, 7 * 60 + 50, 8 * 60 + 20, 9 * 60 + 15)),
            LineaBus(2, "Instituto → Pueblo", listOf(14 * 60 + 35, 15 * 60 + 10, 17 * 60 + 45, 20 * 60 + 30)),
            LineaBus(3, "Búho del sábado", listOf(23 * 60 + 30), dias = setOf(6))
        )
        BusUiState(
            lineas = lineas.map {
                LineaConSalida(it, HorariosBus.proxima(it, lunes, minuto), HorariosBus.salidasDeHoy(it, lunes))
            },
            minutoAhora = minuto,
            diaHoy = lunes
        )
    }
}
