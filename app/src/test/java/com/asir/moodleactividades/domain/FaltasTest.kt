package com.asir.moodleactividades.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaltasTest {

    private fun falta(
        asignatura: String,
        fecha: String = "18/09/2026",
        estado: String = "Injustificada"
    ) = Falta(fecha = fecha, tramo = "8:15 - 9:15", asignatura = asignatura, estado = estado)

    @Test
    fun `distingue justificada de injustificada`() {
        assertTrue(falta("Redes", estado = "Justificada").justificada)
        assertFalse(falta("Redes", estado = "Injustificada").justificada)
        assertFalse(falta("Redes", estado = "INJUSTIFICADA").justificada)
        assertTrue(falta("Redes", estado = " justificada ").justificada)
    }

    @Test
    fun `agrupa las faltas por asignatura`() {
        val resumen = ResumenFaltas.porAsignatura(
            listOf(
                falta("Redes"),
                falta("Redes", estado = "Justificada"),
                falta("Bases de datos")
            )
        )

        assertEquals(2, resumen.size)
        assertEquals("Redes", resumen[0].asignatura)
        assertEquals(2, resumen[0].total)
        assertEquals(1, resumen[0].justificadas)
        assertEquals(1, resumen[0].injustificadas)
    }

    @Test
    fun `la asignatura con mas faltas va primero`() {
        val resumen = ResumenFaltas.porAsignatura(
            listOf(falta("Bases de datos"), falta("Redes"), falta("Redes"))
        )

        assertEquals(listOf("Redes", "Bases de datos"), resumen.map { it.asignatura })
    }

    @Test
    fun `a igualdad de faltas manda el orden alfabetico`() {
        val resumen = ResumenFaltas.porAsignatura(listOf(falta("Redes"), falta("Bases de datos")))

        assertEquals(listOf("Bases de datos", "Redes"), resumen.map { it.asignatura })
    }

    @Test
    fun `dentro de una asignatura van primero las mas recientes`() {
        val resumen = ResumenFaltas.porAsignatura(
            listOf(
                falta("Redes", fecha = "01/09/2026"),
                falta("Redes", fecha = "18/09/2026"),
                falta("Redes", fecha = "10/09/2026")
            )
        )

        assertEquals(
            listOf("18/09/2026", "10/09/2026", "01/09/2026"),
            resumen[0].faltas.map { it.fecha }
        )
    }

    @Test
    fun `una fecha con formato inesperado no rompe el orden`() {
        val resumen = ResumenFaltas.porAsignatura(
            listOf(falta("Redes", fecha = "sin fecha"), falta("Redes", fecha = "18/09/2026"))
        )

        assertEquals(2, resumen[0].total)
        assertEquals("18/09/2026", resumen[0].faltas.first().fecha)
    }

    @Test
    fun `las filas sin asignatura no cuentan`() {
        assertEquals(emptyList<FaltasDeAsignatura>(), ResumenFaltas.porAsignatura(listOf(falta(" "))))
    }

    @Test
    fun `cuenta el total de injustificadas`() {
        val faltas = listOf(
            falta("Redes"),
            falta("Redes", estado = "Justificada"),
            falta("Bases de datos")
        )

        assertEquals(2, ResumenFaltas.totalInjustificadas(faltas))
    }
}
