package com.asir.moodleactividades.data.netacad

import com.asir.moodleactividades.data.netacad.FechaNetacad.Orden
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FechaNetacadTest {

    private val zona = ZoneId.of("Europe/Madrid")
    private val ahora = LocalDateTime.of(2025, 11, 20, 10, 0).atZone(zona).toEpochSecond()

    private fun dia(texto: String, orden: Orden = Orden.DIA_PRIMERO): LocalDate? =
        FechaNetacad.parsear(texto, ahora, orden, zona)
            ?.let { LocalDate.ofInstant(java.time.Instant.ofEpochSecond(it), zona) }

    @Test fun iso() = assertEquals(LocalDate.of(2025, 12, 5), dia("2025-12-05"))
    @Test fun numericaEspanola() = assertEquals(LocalDate.of(2025, 12, 5), dia("05/12/2025"))
    @Test fun numericaConGuiones() = assertEquals(LocalDate.of(2025, 12, 5), dia("5-12-2025"))
    @Test fun numericaConPuntos() = assertEquals(LocalDate.of(2025, 12, 5), dia("05.12.2025"))
    @Test fun anioDeDosCifras() = assertEquals(LocalDate.of(2025, 12, 5), dia("05/12/25"))

    @Test fun ordenAmericanoCuandoSeIndica() =
        assertEquals(LocalDate.of(2025, 5, 12), dia("05/12/2025", Orden.MES_PRIMERO))

    @Test fun `un numero mayor que doce manda sobre el orden supuesto`() {
        // 25 no puede ser un mes, diga lo que diga el orden general de la pagina.
        assertEquals(LocalDate.of(2025, 12, 25), dia("25/12/2025", Orden.MES_PRIMERO))
    }

    @Test fun mesEnLetraEspanol() = assertEquals(LocalDate.of(2025, 12, 5), dia("5 de diciembre de 2025"))
    @Test fun mesAbreviadoEspanol() = assertEquals(LocalDate.of(2025, 12, 5), dia("5 dic 2025"))
    @Test fun mesEnLetraIngles() = assertEquals(LocalDate.of(2025, 12, 5), dia("Dec 5, 2025"))
    @Test fun mesLargoIngles() = assertEquals(LocalDate.of(2025, 12, 5), dia("December 5th, 2025"))
    @Test fun diaAntesDelMesIngles() = assertEquals(LocalDate.of(2025, 12, 5), dia("5 December 2025"))
    @Test fun conTildes() = assertEquals(LocalDate.of(2025, 3, 5), dia("5 de Marzo de 2025"))

    @Test fun `sin anio se entiende el curso en marcha`() =
        assertEquals(LocalDate.of(2025, 12, 5), dia("5 de diciembre"))

    @Test fun `sin anio una fecha muy atrasada es del curso siguiente`() {
        // Estamos en noviembre de 2025: «10 de febrero» es el febrero que viene, no el pasado.
        assertEquals(LocalDate.of(2026, 2, 10), dia("10 de febrero"))
    }

    @Test fun relativas() {
        assertEquals(LocalDate.of(2025, 11, 20), dia("Due today"))
        assertEquals(LocalDate.of(2025, 11, 21), dia("Vence mañana"))
        assertEquals(LocalDate.of(2025, 11, 19), dia("ayer"))
    }

    @Test fun `sin hora se toma el final del dia`() {
        val cuando = FechaNetacad.parsear("05/12/2025", ahora, Orden.DIA_PRIMERO, zona)!!
        val reloj = LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(cuando), zona)
        assertEquals(23, reloj.hour)
        assertEquals(59, reloj.minute)
    }

    @Test fun `la hora del texto manda`() {
        val cuando = FechaNetacad.parsear("05/12/2025 14:30", ahora, Orden.DIA_PRIMERO, zona)!!
        val reloj = LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(cuando), zona)
        assertEquals(14, reloj.hour)
        assertEquals(30, reloj.minute)
    }

    @Test fun `las doce de la noche en formato ingles`() {
        val cuando = FechaNetacad.parsear("Dec 5, 2025 11:59 PM", ahora, Orden.DIA_PRIMERO, zona)!!
        val reloj = LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(cuando), zona)
        assertEquals(23, reloj.hour)
        assertEquals(59, reloj.minute)
    }

    @Test fun `12 am es medianoche`() {
        val cuando = FechaNetacad.parsear("Dec 5, 2025 12:00 AM", ahora, Orden.DIA_PRIMERO, zona)!!
        assertEquals(0, LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(cuando), zona).hour)
    }

    @Test fun `un texto sin fecha no inventa ninguna`() {
        assertNull(dia("Examen del capítulo 3"))
        assertNull(dia(""))
        assertNull(dia("Sin fecha"))
    }

    @Test fun `una palabra que empieza como un mes no es una fecha`() {
        // «marcador» empieza por «mar»: con el criterio de prefijo se leia como marzo.
        assertNull(dia("Marcador 5 del laboratorio"))
        assertNull(dia("Maya 3 puntos"))
    }

    @Test fun `una fecha invalida no se cuela`() = assertNull(dia("31/02/2025"))

    @Test fun `el orden se deduce del conjunto de fechas`() {
        // Hay un 25 en segunda posicion: solo puede ser un dia, luego es mes primero.
        assertEquals(
            Orden.MES_PRIMERO,
            FechaNetacad.detectarOrden(listOf("01/25/2025", "02/03/2025"))
        )
        assertEquals(
            Orden.DIA_PRIMERO,
            FechaNetacad.detectarOrden(listOf("25/01/2025", "03/02/2025"))
        )
        // Sin ninguna pista se supone el formato español.
        assertEquals(Orden.DIA_PRIMERO, FechaNetacad.detectarOrden(listOf("01/02/2025")))
    }
}
