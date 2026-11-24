package com.asir.moodleactividades.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiasCompletosTest {

    private val itinerario = "Itinerario Personal para la Empleabilidad II"
    private val seguridad = "Seguridad y alta disponibilidad"
    private val servicios = "Servicios de red e Internet"
    private val bases = "Administración de sistemas gestores de bases de datos"

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
        falta("20/11/2026", "8:15 - 9:15", bases)
    )

    private val rellenoDel11 = mapOf(
        "8:15 - 9:15" to bases,
        "9:15 - 10:15" to bases,
        "10:15 - 11:15" to itinerario,
        "11:45 - 12:45" to seguridad,
        "12:45 - 13:45" to seguridad,
        "13:45 - 14:45" to servicios
    )

    @Test
    fun `la fila de dia completo de Seneca se reconoce`() {
        assertTrue(faltas[3].esDiaCompleto)
        assertEquals(listOf(faltas[3]), ResumenFaltas.diasCompletos(faltas))
        assertEquals(3, HorarioClases.diaDeLaSemana("11/11/2026"))
        assertNull(HorarioClases.diaDeLaSemana("Día completo"))
    }

    @Test
    fun `sin rellenar el dia completo sale pero no suma en ninguna asignatura`() {
        val repartidas = ResumenFaltas.repartirDiasCompletos(faltas, DiasCompletos())

        assertEquals(faltas, repartidas)
        assertEquals(listOf(faltas[3]), ResumenFaltas.sinRellenar(faltas, DiasCompletos()))
        assertEquals(2, ResumenFaltas.porAsignatura(repartidas).first { it.asignatura == itinerario }.total)
    }

    @Test
    fun `rellenado suma una falta en cada clase de ese dia con su estado`() {
        val rellenos = DiasCompletos().con("11/11/2026", rellenoDel11)
        val repartidas = ResumenFaltas.repartirDiasCompletos(faltas, rellenos)
        val delDia = repartidas.filter { it.fecha == "11/11/2026" }

        assertEquals(listOf(bases, bases, itinerario, seguridad, seguridad, servicios), delDia.map { it.asignatura })
        assertTrue(delDia.all { it.justificada && it.deDiaCompleto })
        assertEquals(emptyList<Falta>(), ResumenFaltas.sinRellenar(faltas, rellenos))

        val porAsignatura = ResumenFaltas.porAsignatura(repartidas).associate { it.asignatura to it.total }
        assertEquals(3, porAsignatura[bases])
        assertEquals(3, porAsignatura[itinerario])
        assertEquals(4, porAsignatura[seguridad])
        assertEquals(3, porAsignatura[servicios])
    }

    @Test
    fun `un tramo sin clase no suma y el dia cuenta como rellenado`() {
        val rellenos = DiasCompletos().con("11/11/2026", rellenoDel11 + ("8:15 - 9:15" to ""))
        val repartidas = ResumenFaltas.repartirDiasCompletos(faltas, rellenos)

        assertEquals(5, repartidas.count { it.fecha == "11/11/2026" })
        assertEquals(emptyList<Falta>(), ResumenFaltas.sinRellenar(faltas, rellenos))
    }

    @Test
    fun `un tramo que ya tiene su falta ese dia no se cuenta dos veces`() {
        val conPropia = faltas + falta("11/11/2026", "10:15 - 11:15", itinerario, "Justificada")
        val rellenos = DiasCompletos().con("11/11/2026", rellenoDel11)
        val repartidas = ResumenFaltas.repartirDiasCompletos(conPropia, rellenos)

        assertEquals(6, repartidas.count { it.fecha == "11/11/2026" })
        assertEquals(1, repartidas.count { it.fecha == "11/11/2026" && it.asignatura == itinerario })
    }

    @Test
    fun `borrar lo rellenado lo vuelve a dejar pendiente`() {
        val rellenos = DiasCompletos().con("11/11/2026", rellenoDel11).sin("11/11/2026")

        assertEquals(listOf(faltas[3]), ResumenFaltas.sinRellenar(faltas, rellenos))
    }

    @Test
    fun `la propuesta sale de las faltas de ese dia de la semana`() {
        assertEquals(
            mapOf(
                "10:15 - 11:15" to itinerario,
                "11:45 - 12:45" to seguridad,
                "12:45 - 13:45" to seguridad,
                "13:45 - 14:45" to servicios
            ),
            DiasCompletos.sugerencia("11/11/2026", faltas, DiasCompletos())
        )
    }

    @Test
    fun `la propuesta prefiere el ultimo miercoles ya rellenado`() {
        val rellenos = DiasCompletos().con("11/11/2026", rellenoDel11)

        assertEquals(rellenoDel11, DiasCompletos.sugerencia("25/11/2026", faltas, rellenos))
        // Un martes no copia lo de un miércoles.
        assertEquals(emptyMap<String, String>(), DiasCompletos.sugerencia("24/11/2026", faltas, rellenos))
    }

    @Test
    fun `los tramos se ordenan por hora de inicio`() {
        assertEquals(
            listOf("8:15 - 9:15", "10:15 - 11:15", "11:45 - 12:45", "12:45 - 13:45", "13:45 - 14:45"),
            HorarioClases.tramos(faltas)
        )
        assertEquals(6, HorarioClases.tramos(faltas, DiasCompletos().con("11/11/2026", rellenoDel11)).size)
    }
}
