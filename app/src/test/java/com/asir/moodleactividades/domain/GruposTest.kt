package com.asir.moodleactividades.domain

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GruposTest {

    @Test fun `el codigo se limpia como lo escribe una persona`() {
        assertEquals("AB12CD34", CodigoInvitacion.normalizar(" ab12-cd34 "))
        // O por 0, I y L por 1: es lo que habrá querido decir.
        assertEquals("0011", CodigoInvitacion.normalizar("OoIl"))
    }

    @Test fun `solo vale con ocho simbolos del alfabeto`() {
        assertTrue(CodigoInvitacion.valido("abcd-efgh"))
        assertFalse(CodigoInvitacion.valido("ABCDEFG"))
        assertFalse(CodigoInvitacion.valido("ABCDEFGHJ"))
        // La U no está en el alfabeto de Crockford.
        assertFalse(CodigoInvitacion.valido("ABCDEFGU"))
        assertFalse(CodigoInvitacion.valido("ÁBCDEFGH"))
    }

    @Test fun `el codigo se ensena partido en dos`() {
        assertEquals("ABCD-EFGH", CodigoInvitacion.formatear("abcdefgh"))
    }

    @Test fun `el mes empieza en lunes y rellena semanas completas`() {
        // Diciembre de 2025 empieza en lunes y tiene 31 días: 5 semanas, 4 huecos al final.
        val diciembre = CalendarioExamenes.celdas(YearMonth.of(2025, 12))
        assertEquals(35, diciembre.size)
        assertEquals(LocalDate.of(2025, 12, 1), diciembre.first())
        assertNull(diciembre.last())

        // Noviembre de 2025 empieza en sábado: cinco huecos delante.
        val noviembre = CalendarioExamenes.celdas(YearMonth.of(2025, 11))
        assertTrue(noviembre.take(5).all { it == null })
        assertEquals(LocalDate.of(2025, 11, 1), noviembre[5])
        assertEquals(0, noviembre.size % 7)
    }

    @Test fun `un mes que cuadra justo no lleva relleno`() {
        // Febrero de 2027: empieza en lunes y tiene 28 días.
        val celdas = CalendarioExamenes.celdas(YearMonth.of(2027, 2))
        assertEquals(28, celdas.size)
        assertTrue(celdas.all { it != null })
    }

    private fun examen(id: Long, fecha: String, hora: String? = null, asignatura: String = "Redes") =
        Examen(id = id, autor = "a", asignatura = asignatura, fecha = fecha, hora = hora)

    @Test fun `por dia primero los de hora temprana y los sin hora al final`() {
        val dia = CalendarioExamenes.porDia(
            listOf(
                examen(1, "2025-12-05"),
                examen(2, "2025-12-05", "12:00:00"),
                examen(3, "2025-12-05", "08:30:00"),
                examen(4, "2025-12-09", "10:00:00")
            )
        )
        assertEquals(listOf(3L, 2L, 1L), dia[LocalDate.of(2025, 12, 5)]!!.map { it.id })
        assertEquals(2, dia.size)
    }

    @Test fun `proximos desde hoy incluido y sin lo pasado`() {
        val hoy = LocalDate.of(2025, 12, 5)
        val lista = CalendarioExamenes.proximos(
            listOf(examen(1, "2025-12-04"), examen(2, "2025-12-05"), examen(3, "2026-01-10")), hoy
        )
        assertEquals(listOf(2L, 3L), lista.map { it.id })
        assertEquals(0L, CalendarioExamenes.diasHasta(lista[0], hoy))
    }

    @Test fun `una fecha rota no tumba la lista`() {
        val lista = listOf(examen(1, "no-es-fecha"), examen(2, "2025-12-05"))
        assertEquals(1, CalendarioExamenes.porDia(lista).size)
        assertNull(lista[0].dia)
    }

    private fun mensaje(id: Long) = Mensaje(id = id, autor = "a", texto = "t$id", enviado = "2025-11-20T10:00:00.123456+00:00")

    @Test fun `el chat no repite mensajes que llegan dos veces`() {
        val r = Chat.fusionar(listOf(mensaje(1), mensaje(2)), listOf(mensaje(2), mensaje(3)))
        assertEquals(listOf(1L, 2L, 3L), r.map { it.id })
        assertEquals(3L, Chat.ultimoId(r))
    }

    @Test fun `el chat guarda solo los ultimos`() {
        val muchos = (1L..700L).map { mensaje(it) }
        val r = Chat.fusionar(emptyList(), muchos)
        assertEquals(Chat.MAXIMO_EN_PANTALLA, r.size)
        assertEquals(700L, r.last().id)
    }

    @Test fun `la hora del servidor se entiende con microsegundos`() {
        assertEquals(1763632800L, mensaje(1).momento)
    }

    @Test fun `quien se fue aparece como antiguo miembro`() {
        val miembros = listOf(Miembro(usuario = "u1", apodo = "Ana"))
        assertEquals("Ana", Chat.apodoDe("u1", miembros))
        assertEquals("Antiguo miembro", Chat.apodoDe("u2", miembros))
    }

    @Test fun `los limites son los de la base de datos`() {
        assertFalse(LimitesGrupo.apodoValido("   "))
        assertTrue(LimitesGrupo.apodoValido("a".repeat(30)))
        assertFalse(LimitesGrupo.apodoValido("a".repeat(31)))
        assertFalse(LimitesGrupo.mensajeValido(" \n "))
    }
}
