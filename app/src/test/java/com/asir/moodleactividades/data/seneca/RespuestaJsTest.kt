package com.asir.moodleactividades.data.seneca

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RespuestaJsTest {

    @Test
    fun `una pagina sin resultado no da extraccion`() {
        assertNull(RespuestaJs.leerExtraccion(null))
        assertNull(RespuestaJs.leerExtraccion("null"))
        assertNull(RespuestaJs.leerExtraccion(""))
    }

    @Test
    fun `desenvuelve la cadena que entrega el WebView`() {
        val crudo = "\"{\\\"encontrada\\\":false,\\\"cabeceras\\\":[\\\"fecha | estado\\\"]}\""

        val resultado = RespuestaJs.leerExtraccion(crudo)

        assertEquals(false, resultado?.encontrada)
        assertEquals(listOf("fecha | estado"), resultado?.cabeceras)
    }

    @Test
    fun `lee las faltas de la tabla`() {
        val interior = """
            {"encontrada":true,"faltas":[
              {"fecha":"18/09/2026","tramo":"8:15 - 9:15",
               "asignatura":"Itinerario Personal para la Empleabilidad II",
               "estado":"Injustificada"}
            ]}
        """.trimIndent()

        val resultado = RespuestaJs.leerExtraccion(interior)

        assertTrue(resultado!!.encontrada)
        assertEquals(1, resultado.faltas.size)
        assertEquals("18/09/2026", resultado.faltas[0].fecha)
        assertEquals("Itinerario Personal para la Empleabilidad II", resultado.faltas[0].asignatura)
        assertEquals(false, resultado.faltas[0].justificada)
    }

    @Test
    fun `una respuesta que no es json no rompe nada`() {
        assertNull(RespuestaJs.leerExtraccion("esto no es json"))
    }
}
