package com.asir.moodleactividades.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiasCompletosTest {

    private val itinerario = "Itinerario Personal para la Empleabilidad II"
    private val seguridad = "Seguridad y alta disponibilidad"
    private val servicios = "Servicios de red e Internet"

    private fun falta(fecha: String, tramo: String, asignatura: String, estado: String = "Injustificada") =
        Falta(fecha = fecha, tramo = tramo, asignatura = asignatura, estado = estado)

    // Los miércoles que salen en la tabla de Pasen, más el día completo del 11/11.
    private val faltas = listOf(
        falta("18/11/2026", "11:45 - 12:45", seguridad),
        falta("18/11/2026", "12:45 - 13:45", seguridad),
        falta("18/11/2026", "13:45 - 14:45", servicios),
        falta("11/11/2026", "Día completo", "", "Justificada"),
        falta("04/11/2026", "10:15 - 11:15", itinerario),
        falta("28/10/2026", "10:15 - 11:15", itinerario),
        falta("28/10/2026", "13:45 - 14:45", servicios),
        falta("20/11/2026", "8:15 - 9:15", "Administración de sistemas gestores de bases de datos")
    )

    @Test
    fun `la fila de dia completo de Seneca se reconoce`() {
        assertTrue(faltas[3].esDiaCompleto)
        assertEquals(3, HorarioClases.diaDeLaSemana("11/11/2026"))
        assertEquals(5, HorarioClases.diaDeLaSemana(" 20/11/2026 "))
        assertNull(HorarioClases.diaDeLaSemana("Día completo"))
    }

    @Test
    fun `el horario se deduce de las faltas de cada dia de la semana`() {
        val horario = HorarioClases.deducir(faltas)

        assertEquals(
            mapOf(
                "10:15 - 11:15" to itinerario,
                "11:45 - 12:45" to seguridad,
                "12:45 - 13:45" to seguridad,
                "13:45 - 14:45" to servicios
            ),
            horario.de(3)
        )
        assertEquals(1, horario.de(5).size)
    }

    @Test
    fun `el dia completo suma una falta a cada clase de ese dia con su estado`() {
        val repartidas = ResumenFaltas.repartirDiasCompletos(faltas, HorarioClases.deducir(faltas))
        val delDia = repartidas.filter { it.fecha == "11/11/2026" }

        assertEquals(
            listOf(itinerario, seguridad, seguridad, servicios),
            delDia.map { it.asignatura }
        )
        assertTrue(delDia.all { it.justificada && it.deDiaCompleto })
        assertEquals(emptyList<Falta>(), ResumenFaltas.sinRepartir(repartidas))

        val porAsignatura = ResumenFaltas.porAsignatura(repartidas).associate { it.asignatura to it.total }
        assertEquals(3, porAsignatura[itinerario])
        assertEquals(4, porAsignatura[seguridad])
        assertEquals(3, porAsignatura[servicios])
    }

    @Test
    fun `un tramo que ya tiene su falta ese dia no se cuenta dos veces`() {
        val conPropia = faltas + falta("11/11/2026", "10:15 - 11:15", itinerario, "Justificada")
        val repartidas = ResumenFaltas.repartirDiasCompletos(conPropia, HorarioClases.deducir(conPropia))

        assertEquals(4, repartidas.count { it.fecha == "11/11/2026" })
        assertEquals(1, repartidas.count { it.fecha == "11/11/2026" && it.asignatura == itinerario })
    }

    @Test
    fun `sin horario de ese dia el dia completo se queda sin repartir`() {
        val jueves = listOf(falta("12/11/2026", "Día completo", ""))
        val repartidas = ResumenFaltas.repartirDiasCompletos(jueves, HorarioClases.deducir(faltas))

        assertEquals(jueves, repartidas)
        assertEquals(jueves, ResumenFaltas.sinRepartir(repartidas))
        assertEquals(emptyList<FaltasDeAsignatura>(), ResumenFaltas.porAsignatura(repartidas))
    }

    @Test
    fun `lo que pone el alumno manda sobre lo deducido y puede quitar un tramo`() {
        val propio = HorarioClases()
            .con(3, "8:15 - 9:15", "Administración de sistemas operativos")
            .con(3, "12:45 - 13:45", "")

        val horario = HorarioClases.deducir(faltas).sobre(propio)

        assertEquals("Administración de sistemas operativos", horario.de(3)["8:15 - 9:15"])
        assertNull(horario.de(3)["12:45 - 13:45"])
        assertEquals(itinerario, horario.de(3)["10:15 - 11:15"])
    }

    @Test
    fun `los tramos se ordenan por hora de inicio`() {
        assertEquals(
            listOf("8:15 - 9:15", "10:15 - 11:15", "11:45 - 12:45", "12:45 - 13:45", "13:45 - 14:45"),
            HorarioClases.tramos(faltas)
        )
    }
}
