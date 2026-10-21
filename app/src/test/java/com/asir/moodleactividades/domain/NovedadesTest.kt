package com.asir.moodleactividades.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class NovedadesTest {

    private fun mensaje(id: Long, autor: String) = Mensaje(id, "g", autor, "hola $id", "2026-10-21T10:00:00Z")

    private fun examen(id: Long, autor: String, hora: String? = "09:15:00") =
        Examen(id, "g", autor, "Redes", "2026-11-04", hora)

    @Test
    fun `la primera vez no se avisa del historial`() {
        assertTrue(NovedadesGrupo.mensajes(listOf(mensaje(1, "ana")), null, "yo").isEmpty())
    }

    @Test
    fun `solo lo nuevo y de otros`() {
        val nuevos = NovedadesGrupo.mensajes(
            listOf(mensaje(5, "ana"), mensaje(6, "yo"), mensaje(7, "luis"), mensaje(4, "ana")),
            ultimoVisto = 4,
            yo = "yo"
        )
        assertEquals(listOf(5L, 7L), nuevos.map { it.id })
    }

    @Test
    fun `un grupo vacio deja la marca a cero para avisar del primero`() {
        assertEquals(0L, NovedadesGrupo.ultimo(null, emptyList()))
        assertEquals(1, NovedadesGrupo.examenes(listOf(examen(1, "ana")), 0L, "yo").size)
    }

    @Test
    fun `la marca nunca va hacia atras`() {
        assertEquals(9L, NovedadesGrupo.ultimo(9, listOf(3, 5)))
        assertEquals(12L, NovedadesGrupo.ultimo(9, listOf(12, 10)))
    }

    @Test
    fun `cuando es un examen`() {
        assertEquals("Miércoles 4 de noviembre a las 09:15", NovedadesGrupo.cuando(examen(1, "ana")))
        assertEquals("Miércoles 4 de noviembre", NovedadesGrupo.cuando(examen(1, "ana", hora = null)))
    }

    @Test
    fun `la primera comprobacion no espera al dia siguiente`() {
        val ahora = LocalDateTime.of(2026, 10, 21, 10, 30)
        // Primera a las 8 y cada 6 horas: la próxima es a las 14, no mañana a las 8.
        assertEquals(210L, Programacion.minutosHastaLaProxima(ahora, 8, 6))
        // Cada hora: a las 11.
        assertEquals(30L, Programacion.minutosHastaLaProxima(ahora, 8, 1))
        // Una vez al día: mañana a las 8.
        assertEquals(21 * 60L + 30, Programacion.minutosHastaLaProxima(ahora, 8, 24))
        // Antes de la hora elegida, el mismo día.
        assertEquals(60L, Programacion.minutosHastaLaProxima(LocalDateTime.of(2026, 10, 21, 7, 0), 8, 24))
    }

    @Test
    fun `justo a la hora se programa la siguiente`() {
        assertEquals(360L, Programacion.minutosHastaLaProxima(LocalDateTime.of(2026, 10, 21, 8, 0), 8, 6))
    }
}
