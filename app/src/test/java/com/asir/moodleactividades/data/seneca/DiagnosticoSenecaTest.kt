package com.asir.moodleactividades.data.seneca

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticoSenecaTest {

    @Test
    fun `la ruta recorta el identificador de sesion`() {
        val url = "https://seneca.juntadeandalucia.es/seneca/nav/d9pSQatiS0m9widARIxcebLsb5"

        assertEquals(
            "seneca.juntadeandalucia.es/seneca/nav/…",
            DiagnosticoSeneca.rutaSegura(url)
        )
    }

    @Test
    fun `una ruta corta se deja entera`() {
        assertEquals(
            "seneca.juntadeandalucia.es",
            DiagnosticoSeneca.rutaSegura("https://seneca.juntadeandalucia.es/")
        )
    }

    @Test
    fun `los parametros no se conservan porque pueden llevar la sesion`() {
        val url = "https://seneca.juntadeandalucia.es/seneca/jsp/x.jsp?token=secreto"

        assertFalse(DiagnosticoSeneca.rutaSegura(url).contains("secreto"))
    }

    @Test
    fun `sin url no revienta`() {
        assertEquals("", DiagnosticoSeneca.rutaSegura(null))
        assertEquals("", DiagnosticoSeneca.rutaSegura(""))
    }

    @Test
    fun `el texto no incluye ningun valor de cookie`() {
        val texto = DiagnosticoSeneca(
            cookiesGuardadas = listOf("JSESSIONID", "SENECA"),
            cookiesVivas = 2,
            urlUltimaPagina = "seneca.juntadeandalucia.es/seneca/nav/…",
            tablaEncontrada = false,
            pidioAcceso = true,
            paginasVistas = 3
        ).comoTexto()

        assertTrue(texto.contains("JSESSIONID"))
        assertTrue(texto.contains("Pidió identificarse: sí"))
        assertTrue(texto.contains("Tabla encontrada: no"))
        assertFalse(texto.contains("="))
    }

    @Test
    fun `sin cookies guardadas lo dice`() {
        assertTrue(DiagnosticoSeneca().comoTexto().contains("ninguna"))
    }
}
