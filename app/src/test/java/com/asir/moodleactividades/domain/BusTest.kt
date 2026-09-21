package com.asir.moodleactividades.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BusTest {

    private val lunes = 1
    private val sabado = 6
    private val domingo = 7

    private fun linea(
        horas: List<Int>,
        dias: Set<Int> = LineaBus.LABORABLES
    ) = LineaBus(id = 1, nombre = "Pueblo → Instituto", horas = horas, dias = dias)

    private fun hora(texto: String) = HorariosBus.parsearHora(texto)!!

    @Test
    fun `lee las horas como se escriben en un movil`() {
        assertEquals(465, HorariosBus.parsearHora("07:45"))
        assertEquals(465, HorariosBus.parsearHora("7:45"))
        assertEquals(465, HorariosBus.parsearHora("07.45"))
        assertEquals(465, HorariosBus.parsearHora("0745"))
        assertEquals(0, HorariosBus.parsearHora("00:00"))
        assertEquals(23 * 60 + 59, HorariosBus.parsearHora("23:59"))
    }

    @Test
    fun `rechaza lo que no es una hora`() {
        assertNull(HorariosBus.parsearHora("24:00"))
        assertNull(HorariosBus.parsearHora("07:60"))
        assertNull(HorariosBus.parsearHora("mañana"))
        assertNull(HorariosBus.parsearHora(""))
        assertNull(HorariosBus.parsearHora("7:45:30"))
    }

    @Test
    fun `formatea siempre con dos digitos`() {
        assertEquals("07:45", HorariosBus.formatearHora(465))
        assertEquals("00:05", HorariosBus.formatearHora(5))
    }

    @Test
    fun `la proxima salida es la siguiente de hoy`() {
        val ruta = linea(listOf(hora("07:00"), hora("08:30"), hora("14:00")))

        val proxima = HorariosBus.proxima(ruta, lunes, hora("07:30"))!!

        assertEquals(hora("08:30"), proxima.minutoDelDia)
        assertEquals(0, proxima.diasDeEspera)
        assertEquals(60, proxima.minutosQueFaltan(hora("07:30")))
    }

    @Test
    fun `si ya han salido todas se pasa al siguiente dia con servicio`() {
        val ruta = linea(listOf(hora("07:00"), hora("08:30")))

        val proxima = HorariosBus.proxima(ruta, lunes, hora("20:00"))!!

        assertEquals(hora("07:00"), proxima.minutoDelDia)
        assertEquals(1, proxima.diasDeEspera)
    }

    @Test
    fun `el viernes por la tarde salta al lunes, no al sabado`() {
        val viernes = 5
        val ruta = linea(listOf(hora("07:00")))

        val proxima = HorariosBus.proxima(ruta, viernes, hora("20:00"))!!

        // Tres días de espera: sábado y domingo no hay servicio.
        assertEquals(3, proxima.diasDeEspera)
    }

    @Test
    fun `una linea que solo circula un dia espera a la semana siguiente`() {
        val ruta = linea(listOf(hora("09:00")), dias = setOf(domingo))

        val proxima = HorariosBus.proxima(ruta, domingo, hora("10:00"))!!

        assertEquals(7, proxima.diasDeEspera)
        assertEquals(hora("09:00"), proxima.minutoDelDia)
    }

    @Test
    fun `una salida justo a esta hora ya no cuenta`() {
        val ruta = linea(listOf(hora("08:00"), hora("09:00")))

        val proxima = HorariosBus.proxima(ruta, lunes, hora("08:00"))!!

        assertEquals(hora("09:00"), proxima.minutoDelDia)
    }

    @Test
    fun `sin horas o sin dias no hay proxima salida`() {
        assertNull(HorariosBus.proxima(linea(emptyList()), lunes, 0))
        assertNull(HorariosBus.proxima(linea(listOf(hora("08:00")), dias = emptySet()), lunes, 0))
    }

    @Test
    fun `las salidas de hoy solo aparecen si hoy hay servicio`() {
        val ruta = linea(listOf(hora("08:00")))

        assertEquals(listOf(hora("08:00")), HorariosBus.salidasDeHoy(ruta, lunes))
        assertEquals(emptyList<Int>(), HorariosBus.salidasDeHoy(ruta, sabado))
    }

    @Test
    fun `las horas repetidas o desordenadas no se cuelan`() {
        val ruta = linea(listOf(hora("14:00"), hora("07:00"), hora("07:00")))

        assertEquals(listOf(hora("07:00"), hora("14:00")), ruta.horasOrdenadas)
    }

    @Test
    fun `los dias se describen en corto`() {
        assertEquals("De lunes a viernes", HorariosBus.etiquetaDias(LineaBus.LABORABLES))
        assertEquals("Todos los días", HorariosBus.etiquetaDias((1..7).toSet()))
        assertEquals("S, D", HorariosBus.etiquetaDias(setOf(sabado, domingo)))
        assertEquals("Sin días", HorariosBus.etiquetaDias(emptySet()))
    }
}
