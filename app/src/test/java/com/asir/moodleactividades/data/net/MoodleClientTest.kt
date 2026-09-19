package com.asir.moodleactividades.data.net

import org.junit.Assert.assertEquals
import org.junit.Test

class MoodleClientTest {

    @Test
    fun `anade https cuando la url se pega sin esquema`() {
        assertEquals(
            "https://educacionadistancia.juntadeandalucia.es/centros/abc/",
            MoodleClient.normalizarUrl("educacionadistancia.juntadeandalucia.es/centros/abc")
        )
    }

    @Test
    fun `recorta la ruta de la pagina en la que estaba el usuario`() {
        assertEquals(
            "https://moodle.centro.es/",
            MoodleClient.normalizarUrl("https://moodle.centro.es/login/index.php")
        )
        assertEquals(
            "https://moodle.centro.es/",
            MoodleClient.normalizarUrl("https://moodle.centro.es/my/?redirect=0")
        )
    }

    @Test
    fun `respeta el subdirectorio del centro`() {
        assertEquals(
            "https://educacionadistancia.juntadeandalucia.es/centros/abc/",
            MoodleClient.normalizarUrl("https://educacionadistancia.juntadeandalucia.es/centros/abc/my/")
        )
    }

    @Test
    fun `una url vacia no produce esquema suelto`() {
        assertEquals("", MoodleClient.normalizarUrl("   "))
    }
}
