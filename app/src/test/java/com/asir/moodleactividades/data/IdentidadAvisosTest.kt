package com.asir.moodleactividades.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentidadAvisosTest {

    private fun falta(asignatura: String, texto: String, momento: Long = 1_000L) = Aviso(
        momento = momento,
        tipo = TipoAviso.FALTA,
        titulo = "Falta nueva en $asignatura",
        texto = texto
    )

    @Test
    fun `cinco faltas de la misma asignatura y el mismo segundo no repiten clave`() {
        // Es justo lo que llegaba de Séneca de una vez, y lo que cerraba la aplicación.
        val avisos = (1..5).map { falta("Redes", "18/09/2026 · tramo $it · Injustificada") }

        val claves = IdentidadAvisos.unicos(avisos).map { it.id }

        assertEquals(5, claves.toSet().size)
        assertTrue(claves.none { it.isBlank() })
    }

    @Test
    fun `dos avisos identicos se desempatan igualmente`() {
        val repetido = falta("Redes", "18/09/2026 · 8:15 · Injustificada")

        val claves = IdentidadAvisos.unicos(listOf(repetido, repetido)).map { it.id }

        assertEquals(2, claves.toSet().size)
    }

    @Test
    fun `la clave no depende de la posicion en la lista`() {
        val viejo = falta("Redes", "18/09/2026 · 8:15 · Injustificada")
        val nuevo = falta("Bases de datos", "19/09/2026 · 9:15 · Injustificada", momento = 2_000L)

        val solo = IdentidadAvisos.unicos(listOf(viejo)).first().id
        val conOtroEncima = IdentidadAvisos.unicos(listOf(nuevo, viejo))[1].id

        // Si cambiara al llegar otro aviso, la lista se repintaría entera cada vez.
        assertEquals(solo, conOtroEncima)
    }

    @Test
    fun `un aviso que ya trae clave la conserva`() {
        val marcado = falta("Redes", "texto").copy(id = "propia")

        assertEquals("propia", IdentidadAvisos.unicos(listOf(marcado)).first().id)
    }

    @Test
    fun `el texto distingue avisos con el mismo titulo y la misma hora`() {
        val primera = falta("Redes", "18/09/2026 · 8:15 · Injustificada")
        val segunda = falta("Redes", "18/09/2026 · 9:15 · Injustificada")

        assertNotEquals(IdentidadAvisos.de(primera), IdentidadAvisos.de(segunda))
    }

    @Test
    fun `avisos de distinto tipo no comparten clave`() {
        val falta = falta("Redes", "mismo texto")
        val nota = falta.copy(tipo = TipoAviso.NOTA)

        assertNotEquals(IdentidadAvisos.de(falta), IdentidadAvisos.de(nota))
    }

    @Test
    fun `una lista vacia no da problemas`() {
        assertEquals(emptyList<Aviso>(), IdentidadAvisos.unicos(emptyList()))
    }
}
