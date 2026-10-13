package com.asir.moodleactividades.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AprobadoTest {

    private fun nota(nota: String, maxima: Double = 0.0, porcentaje: String = "") =
        Calificacion("Redes", "Cuestionario ACL", nota, porcentaje, maxima, false, TipoActividad.CUESTIONARIO)

    private fun evaluar(calificacion: Calificacion, ajuste: AjusteNota? = null) =
        Aprobado.evaluar(calificacion, ajuste)

    @Test
    fun `un 1,80 sobre 2 esta aprobado`() {
        val veredicto = evaluar(nota("1,80", maxima = 2.0))

        assertEquals(true, veredicto.aprobada)
        assertEquals(OrigenVeredicto.MAXIMA_MOODLE, veredicto.origen)
        assertEquals(2.0, veredicto.notaMaxima)
    }

    @Test
    fun `un 0,90 sobre 2 esta suspenso`() {
        assertEquals(false, evaluar(nota("0,90", maxima = 2.0)).aprobada)
    }

    @Test
    fun `justo la mitad aprueba`() {
        assertEquals(true, evaluar(nota("1,00", maxima = 2.0)).aprobada)
        assertEquals(true, evaluar(nota("5")).aprobada)
    }

    @Test
    fun `sin nota maxima se supone sobre 10 y se dice que es supuesto`() {
        val veredicto = evaluar(nota("1,80"))

        assertEquals(false, veredicto.aprobada)
        assertEquals(OrigenVeredicto.SUPUESTO, veredicto.origen)
        assertEquals(10.0, veredicto.notaMaxima)
    }

    @Test
    fun `la nota maxima que pone el alumno manda sobre la supuesta`() {
        val veredicto = evaluar(nota("1,80"), AjusteNota(notaMaxima = 2.0))

        assertEquals(true, veredicto.aprobada)
        assertEquals(OrigenVeredicto.MAXIMA_PROPIA, veredicto.origen)
        assertEquals(2.0, veredicto.notaMaxima)
    }

    @Test
    fun `la nota maxima del alumno manda tambien sobre la de Moodle`() {
        assertEquals(true, evaluar(nota("1,80", maxima = 10.0), AjusteNota(notaMaxima = 2.0)).aprobada)
    }

    @Test
    fun `marcarla a mano manda sobre todo`() {
        val aprobada = evaluar(nota("1,80", maxima = 10.0), AjusteNota(aprobada = true))
        val suspensa = evaluar(nota("9", maxima = 10.0), AjusteNota(notaMaxima = 10.0, aprobada = false))

        assertEquals(true, aprobada.aprobada)
        assertEquals(OrigenVeredicto.MANUAL, aprobada.origen)
        assertEquals(false, suspensa.aprobada)
        assertEquals(OrigenVeredicto.MANUAL, suspensa.origen)
    }

    @Test
    fun `sin nota no hay veredicto aunque se haya marcado`() {
        val veredicto = evaluar(nota(""), AjusteNota(notaMaxima = 2.0, aprobada = true))

        assertNull(veredicto.aprobada)
        assertEquals(OrigenVeredicto.DESCONOCIDO, veredicto.origen)
        assertEquals(2.0, veredicto.notaMaxima)
    }

    @Test
    fun `el porcentaje de Moodle tiene en cuenta el minimo`() {
        // Un 6 en una escala de 4 a 10 es un 33 %: suspenso aunque pase de la mitad de 10.
        val veredicto = evaluar(nota("6,00", maxima = 10.0, porcentaje = "33,33 %"))

        assertEquals(false, veredicto.aprobada)
        assertEquals(OrigenVeredicto.PORCENTAJE, veredicto.origen)
    }

    @Test
    fun `una nota con el porcentaje entre parentesis se lee bien`() {
        assertEquals(7.5, Aprobado.valorDe("7,50 (75,00 %)"))
        assertEquals(true, evaluar(nota("7,50 (75,00 %)", maxima = 10.0)).aprobada)
    }

    @Test
    fun `por encima de 10 sin maximo no se inventa nada`() {
        val veredicto = evaluar(nota("92,00"))

        assertNull(veredicto.aprobada)
        assertEquals(OrigenVeredicto.DESCONOCIDO, veredicto.origen)
    }

    @Test
    fun `las notas con palabras`() {
        assertEquals(true, evaluar(nota("Apto")).aprobada)
        assertEquals(false, evaluar(nota("No apto")).aprobada)
        assertEquals(false, evaluar(nota("Insuficiente")).aprobada)
        assertEquals(true, evaluar(nota("Sobresaliente")).aprobada)
        assertEquals(false, evaluar(nota("NO SUPERADO")).aprobada)
        assertEquals(OrigenVeredicto.TEXTO, evaluar(nota("Apto")).origen)
        assertNull(evaluar(nota("B")).aprobada)
    }

    @Test
    fun `la nota maxima escrita`() {
        assertEquals(2.0, Aprobado.maximaEscrita("2"))
        assertEquals(2.5, Aprobado.maximaEscrita(" 2,5 "))
        assertEquals(2.5, Aprobado.maximaEscrita("2.5"))
        assertNull(Aprobado.maximaEscrita("0"))
        assertNull(Aprobado.maximaEscrita("-3"))
        assertNull(Aprobado.maximaEscrita("dos"))
        assertNull(Aprobado.maximaEscrita(""))
    }

    @Test
    fun `las cifras se escriben con coma`() {
        assertEquals("2", Aprobado.cifra(2.0))
        assertEquals("1,5", Aprobado.cifra(1.5))
        assertEquals("1,25", Aprobado.cifra(1.25))
        assertEquals("0,9", Aprobado.cifra(0.9))
    }
}
