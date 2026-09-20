package com.asir.moodleactividades.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SondeoAsistenciaTest {

    @Test
    fun `reconoce las funciones del modulo de asistencia`() {
        assertTrue(SondeoAsistencia.esDeAsistencia("mod_attendance_get_sessions"))
        assertTrue(SondeoAsistencia.esDeAsistencia("MOD_ATTENDANCE_ADD_SESSION"))
        assertTrue(SondeoAsistencia.esDeAsistencia("local_asistencia_get_faltas"))
    }

    @Test
    fun `no confunde otras funciones con las de asistencia`() {
        assertFalse(SondeoAsistencia.esDeAsistencia("mod_assign_get_assignments"))
        assertFalse(SondeoAsistencia.esDeAsistencia("core_webservice_get_site_info"))
        assertFalse(SondeoAsistencia.esDeAsistencia("gradereport_user_get_grade_items"))
    }

    @Test
    fun `un sondeo sin funciones de asistencia no esta disponible`() {
        val sondeo = SondeoAsistencia("Centro", "4.1", 120, emptyList())

        assertFalse(sondeo.disponible)
        assertTrue(sondeo.comoTexto().contains("ninguna"))
    }

    @Test
    fun `el diagnostico lista las funciones encontradas`() {
        val sondeo = SondeoAsistencia(
            sitio = "IES Ejemplo",
            version = "Moodle 4.1.2",
            totalFunciones = 130,
            funcionesAsistencia = listOf("mod_attendance_get_sessions")
        )

        assertTrue(sondeo.disponible)
        val texto = sondeo.comoTexto()
        assertTrue(texto.contains("IES Ejemplo"))
        assertTrue(texto.contains("Moodle 4.1.2"))
        assertTrue(texto.contains("mod_attendance_get_sessions"))
    }

    @Test
    fun `sin version conocida el diagnostico lo dice en vez de dejarlo vacio`() {
        val sondeo = SondeoAsistencia("Centro", "", 10, emptyList())

        assertEquals(true, sondeo.comoTexto().contains("desconocido"))
    }
}
