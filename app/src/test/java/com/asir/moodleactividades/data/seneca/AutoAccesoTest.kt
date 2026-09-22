package com.asir.moodleactividades.data.seneca

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoAccesoTest {

    @Test
    fun `una comilla no rompe el literal`() {
        assertEquals("\"a\\\"b\"", AutoAcceso.comoLiteral("a\"b"))
    }

    @Test
    fun `una barra invertida se escapa`() {
        assertEquals("\"a\\\\b\"", AutoAcceso.comoLiteral("a\\b"))
    }

    /**
     * Quita los escapes para ver qué comillas quedarían vivas para el intérprete: las de
     * dentro van precedidas de barra y no cierran nada, así que no cuentan.
     */
    private fun comillasQueCierran(literal: String): Int =
        literal.replace("\\\\", "").replace("\\\"", "").count { it == '"' }

    @Test
    fun `no se puede cerrar el guion desde el contenido`() {
        // Sin escapar, esto sacaría el texto del literal y ejecutaría lo que viniera detrás.
        val peligroso = "x\"); alert(1); (\""

        val literal = AutoAcceso.comoLiteral(peligroso)

        assertTrue(literal.startsWith("\""))
        assertTrue(literal.endsWith("\""))
        // Solo las dos de los extremos: ninguna comilla del contenido queda suelta.
        assertEquals(2, comillasQueCierran(literal))
    }

    @Test
    fun `una barra al final no se come la comilla de cierre`() {
        // Con la barra sin escapar, el literal acabaría en \" y seguiría abierto.
        val literal = AutoAcceso.comoLiteral("clave\\")

        assertEquals(2, comillasQueCierran(literal))
    }

    @Test
    fun `los saltos de linea no parten el guion`() {
        assertEquals("\"a\\nb\\rc\"", AutoAcceso.comoLiteral("a\nb\rc"))
    }

    @Test
    fun `se escapan los caracteres que cierran una etiqueta`() {
        val literal = AutoAcceso.comoLiteral("</script>")

        assertFalse(literal.contains("<"))
        assertFalse(literal.contains(">"))
    }

    @Test
    fun `los separadores de linea de unicode tambien se escapan`() {
        val literal = AutoAcceso.comoLiteral("a b c")

        assertFalse(literal.contains(" "))
        assertFalse(literal.contains(" "))
        assertTrue(literal.contains("\\u2028"))
    }

    @Test
    fun `el guion lleva los dos campos y no los deja en claro en el codigo`() {
        val guion = AutoAcceso.guion("alumno", "secreta")

        assertTrue(guion.contains("var usuario = \"alumno\""))
        assertTrue(guion.contains("var clave = \"secreta\""))
        // El guion tiene que seguir siendo capaz de cerrar el aviso de sesión caducada.
        assertTrue(guion.contains("botonCerrar"))
    }

    @Test
    fun `un usuario vacio no genera un literal roto`() {
        assertEquals("\"\"", AutoAcceso.comoLiteral(""))
    }

    @Test
    fun `el guion actua y el sondeo solo mira`() {
        assertTrue(AutoAcceso.guion("alumno", "secreta").contains("var actuar = true"))
        assertTrue(AutoAcceso.SONDEO.contains("var actuar = false"))
    }

    @Test
    fun `el sondeo no lleva ninguna credencial encima`() {
        assertTrue(AutoAcceso.SONDEO.contains("var usuario = \"\""))
        assertTrue(AutoAcceso.SONDEO.contains("var clave = \"\""))
    }

    @Test
    fun `el sondeo distingue el formulario del aviso`() {
        // De eso depende no dar por mala una contraseña que sí vale.
        assertTrue(AutoAcceso.SONDEO.contains("formulario: hayFormulario"))
        assertTrue(AutoAcceso.SONDEO.contains("aviso: hayAviso"))
    }

    @Test
    fun `solo se pulsa lo que es un control, no la palabra del mensaje`() {
        val guion = AutoAcceso.guion("alumno", "secreta")

        // El aviso dice «pulse cerrar para iniciar sesión de nuevo»: esa palabra en negrita
        // no cierra nada, así que el texto suelto nunca puede bastar para pulsar.
        assertTrue(guion.contains("if (!pulsable(nodo)) continue;"))
        assertTrue(guion.contains("function pulsable(elemento)"))
    }
}
