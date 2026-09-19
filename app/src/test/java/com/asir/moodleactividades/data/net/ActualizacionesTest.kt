package com.asir.moodleactividades.data.net

import org.junit.Assert.assertTrue
import org.junit.Test

class ActualizacionesTest {
    @Test
    fun `compara versiones por numero y no alfabeticamente`() {
        assertTrue(Actualizaciones.comparar("1.10", "1.9") > 0)
        assertTrue(Actualizaciones.comparar("1.5", "1.5") == 0)
        assertTrue(Actualizaciones.comparar("1.4", "1.5") < 0)
        assertTrue(Actualizaciones.comparar("2.0", "1.99") > 0)
        assertTrue(Actualizaciones.comparar("1.5.1", "1.5") > 0)
    }
}
