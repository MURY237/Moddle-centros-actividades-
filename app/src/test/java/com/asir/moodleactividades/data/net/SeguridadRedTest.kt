package com.asir.moodleactividades.data.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeguridadRedTest {

    private val sitio = "https://educacionadistancia.juntadeandalucia.es/centros/abc/"

    @Test fun `el token va a los archivos del propio moodle`() {
        val url = UrlsMoodle.conToken(
            "https://educacionadistancia.juntadeandalucia.es/centros/abc/webservice/pluginfile.php/12/mod_assign/intro/tema.pdf",
            sitio, "secreto"
        )
        assertEquals(
            "https://educacionadistancia.juntadeandalucia.es/centros/abc/webservice/pluginfile.php/12/mod_assign/intro/tema.pdf?token=secreto",
            url
        )
    }

    @Test fun `y nunca a otro dominio`() {
        assertNull(UrlsMoodle.conToken("https://evil.example.com/tema.pdf", sitio, "secreto"))
        assertNull(UrlsMoodle.conToken("https://juntadeandalucia.es.evil.net/tema.pdf", sitio, "secreto"))
    }

    @Test fun `ni sin cifrar ni a una url rota`() {
        assertNull(UrlsMoodle.conToken("http://educacionadistancia.juntadeandalucia.es/x.pdf", sitio, "secreto"))
        assertNull(UrlsMoodle.conToken("no es una url", sitio, "secreto"))
    }

    @Test fun `un token que ya venia no se duplica`() {
        val url = UrlsMoodle.conToken(
            "https://educacionadistancia.juntadeandalucia.es/f.pdf?forcedownload=1&token=viejo", sitio, "nuevo"
        )!!
        assertEquals(1, Regex("token=").findAll(url).count())
        assertTrue(url.contains("token=nuevo"))
        assertTrue(url.contains("forcedownload=1"))
    }

    @Test fun `el apk solo se baja de las releases de este repositorio`() {
        assertTrue(Actualizaciones.urlPermitida(
            "https://github.com/MURY237/Moddle-centros-actividades-/releases/download/v1.50/actividades-moodle-1.50.apk"
        ))
        assertFalse(Actualizaciones.urlPermitida("http://github.com/MURY237/Moddle-centros-actividades-/releases/download/v1/a.apk"))
        assertFalse(Actualizaciones.urlPermitida("https://github.com/otro/repo/releases/download/v1/a.apk"))
        assertFalse(Actualizaciones.urlPermitida("https://evil.com/MURY237/Moddle-centros-actividades-/releases/download/v1/a.apk"))
        assertFalse(Actualizaciones.urlPermitida("https://github.com.evil.net/MURY237/Moddle-centros-actividades-/releases/download/v1/a.apk"))
    }

    @Test fun `la huella de github se lee y se valida`() {
        val hex = "8df76f9285bdef40106d16a36efd038fc7fbf06e15bfd5334e3fd92c06c9a3a9"
        assertEquals(hex, Actualizaciones.huellaDe("sha256:$hex"))
        assertEquals(hex, Actualizaciones.huellaDe("sha256:" + hex.uppercase()))
        assertNull(Actualizaciones.huellaDe(""))
        assertNull(Actualizaciones.huellaDe("sha256:corta"))
        assertNull(Actualizaciones.huellaDe("md5:$hex"))
    }
}
