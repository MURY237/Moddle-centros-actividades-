package com.asir.moodleactividades.data.net

import org.junit.Assert.assertEquals
import org.junit.Test

class TextoMoodleTest {

    @Test
    fun `el espacio duro de Moodle no se cuela como un ampersand`() {
        assertEquals("100,00", notaVisible("100,00&nbsp;/&nbsp;100,00"))
    }

    @Test
    fun `quita las etiquetas con las que Moodle envuelve la nota`() {
        assertEquals("8,50", notaVisible("<span class=\"grade\">8,50 / 10,00</span>"))
    }

    @Test
    fun `conserva la nota cuando viene sin adornos`() {
        assertEquals("7", notaVisible("7"))
        assertEquals("Aprobado", notaVisible("Aprobado"))
    }

    @Test
    fun `descarta cualquier entidad html que no conozca`() {
        assertEquals("9,25", notaVisible("9,25&thinsp;&#8201;/ 10"))
    }

    @Test
    fun `restaura el ampersand escrito como entidad`() {
        assertEquals("Apto & Mención", limpiarHtml("Apto &amp; Mención"))
    }

    @Test
    fun `colapsa los espacios que deja la limpieza`() {
        assertEquals("Bien hecho", limpiarHtml("<b>Bien</b>&nbsp;&nbsp;<i>hecho</i>"))
    }
}
