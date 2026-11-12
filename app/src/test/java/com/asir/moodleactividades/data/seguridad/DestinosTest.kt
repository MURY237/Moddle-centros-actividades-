package com.asir.moodleactividades.data.seguridad

import com.asir.moodleactividades.data.SesionSeneca
import com.asir.moodleactividades.ui.esEnlaceWeb
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A dónde deja ir la app: qué se guarda como página de Séneca y qué enlaces de fuera se abren. */
class DestinosTest {

    @Test
    fun `solo se recuerda una pagina del Seneca de verdad`() {
        assertTrue(SesionSeneca.esDeSeneca("https://seneca.juntadeandalucia.es/seneca/nav/abc/faltas.jsp?rndval=1"))

        // Antes bastaba con empezar por «https://seneca.».
        assertFalse(SesionSeneca.esDeSeneca("https://seneca.ejemplo.com/seneca/"))
        assertFalse(SesionSeneca.esDeSeneca("https://seneca.juntadeandalucia.es.ejemplo.com/"))
        assertFalse(SesionSeneca.esDeSeneca("https://seneca.juntadeandalucia.es@ejemplo.com/"))
        assertFalse(SesionSeneca.esDeSeneca("http://seneca.juntadeandalucia.es/seneca/"))
        assertFalse(SesionSeneca.esDeSeneca("https://www.juntadeandalucia.es/"))
        assertFalse(SesionSeneca.esDeSeneca("javascript:alert(1)"))
        assertFalse(SesionSeneca.esDeSeneca(""))
    }

    @Test
    fun `de lo que manda el servidor solo se abren paginas web`() {
        assertTrue(esEnlaceWeb("https://moodle.ejemplo.es/mod/assign/view.php?id=7"))
        assertTrue(esEnlaceWeb("  http://moodle.ejemplo.es/  "))

        assertFalse(esEnlaceWeb("tel:112"))
        assertFalse(esEnlaceWeb("content://com.otra.app/datos"))
        assertFalse(esEnlaceWeb("file:///data/data/com.asir.moodleactividades/shared_prefs/sesion.xml"))
        assertFalse(esEnlaceWeb("intent://ejemplo#Intent;scheme=https;end"))
        assertFalse(esEnlaceWeb("market://details?id=com.otra.app"))
        assertFalse(esEnlaceWeb("javascript:alert(1)"))
        assertFalse(esEnlaceWeb(""))
        assertFalse(esEnlaceWeb(null))
    }
}
