package com.asir.moodleactividades.data.netacad

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoAccesoNetacadTest {

    @Test fun `la contrasena solo se da a Cisco`() {
        assertTrue(AutoAccesoNetacad.hostPermitido("https://id.cisco.com/signin"))
        assertTrue(AutoAccesoNetacad.hostPermitido("https://cisco.com/login"))
        assertTrue(AutoAccesoNetacad.hostPermitido("https://www.netacad.com/login"))
        assertTrue(AutoAccesoNetacad.hostPermitido("https://auth.netacad.com/x?y=1"))
    }

    @Test fun `ni a dominios que se le parecen`() {
        assertFalse(AutoAccesoNetacad.hostPermitido("https://cisco.com.evil.net/login"))
        assertFalse(AutoAccesoNetacad.hostPermitido("https://notcisco.com/login"))
        assertFalse(AutoAccesoNetacad.hostPermitido("https://netacad.com.evil.io/"))
        assertFalse(AutoAccesoNetacad.hostPermitido("https://evil.com/?next=https://id.cisco.com"))
        assertFalse(AutoAccesoNetacad.hostPermitido("https://evil.com#id.cisco.com"))
    }

    @Test fun `ni sin cifrar ni sin direccion`() {
        assertFalse(AutoAccesoNetacad.hostPermitido("http://id.cisco.com/signin"))
        assertFalse(AutoAccesoNetacad.hostPermitido(null))
        assertFalse(AutoAccesoNetacad.hostPermitido(""))
        assertFalse(AutoAccesoNetacad.hostPermitido("no es una url"))
    }

    @Test fun `la cuenta va como literal y no puede escaparse del guion`() {
        val guion = AutoAccesoNetacad.guion("a@b.es", "x\"; alert(1); \"</script>")
        assertTrue(guion.contains("var correo = \"a@b.es\";"))
        assertFalse(guion.contains("</script>"))
        assertFalse(guion.contains("\"; alert(1); \""))
    }

    @Test fun `el guion comprueba el dominio tambien por dentro`() {
        val guion = AutoAccesoNetacad.guion("a@b.es", "c")
        assertTrue(guion.contains("var PERMITIDOS = /(^|\\.)(cisco\\.com|netacad\\.com)$/i;"))
        assertTrue(guion.contains("if (!PERMITIDOS.test(host))"))
    }

    @Test fun `el panel no es un curso`() {
        assertTrue(LectorNetacad.esTituloDeInicio("Mi aprendizaje"))
        assertTrue(LectorNetacad.esTituloDeInicio("My Learning"))
        assertFalse(LectorNetacad.esTituloDeInicio("CCNA: Introduction to Networks"))
        assertTrue(LectorNetacad.esUrlDeInicio("https://www.netacad.com/dashboard"))
        assertTrue(LectorNetacad.esUrlDeInicio("https://www.netacad.com/"))
        assertFalse(LectorNetacad.esUrlDeInicio("https://www.netacad.com/launch?id=abc"))
    }

    @Test fun `las tarjetas de clase del panel no salen como trabajos`() {
        // Lo que se leyó de verdad: la tarjeta del instituto bajo «Mi aprendizaje».
        val leidos = LectorNetacad.leer(
            ResultadoNetacad(
                pagina = "trabajos",
                candidatos = listOf(
                    CandidatoNetacad(titulo = "IES Ciudad Jardín", curso = "Mi aprendizaje", fecha = "16/09/2026"),
                    CandidatoNetacad(titulo = "Modules 1-3 Exam", curso = "CCNA ITN", fecha = "16/10/2026")
                )
            ),
            ahora = 1_790_000_000L
        )
        assertTrue(leidos.map { it.titulo } == listOf("Modules 1-3 Exam"))
    }
}
