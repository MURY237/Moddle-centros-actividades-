package com.asir.moodleactividades.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class ClasificadorTest {

    private val zona: ZoneId = ZoneId.of("UTC")
    private val ahora = 1_700_000_000L
    private val unDia = 86_400L

    private fun actividad(
        fechaLimite: Long?,
        estado: EstadoActividad
    ) = Actividad(
        id = 1,
        nombre = "Tarea",
        curso = "ASIR",
        tipo = TipoActividad.TAREA,
        fechaLimite = fechaLimite,
        estado = estado,
        calificada = false,
        url = null
    )

    @Test
    fun `una entrega enviada cuenta como entregada aunque el plazo haya vencido`() {
        val estado = Clasificador.estado("submitted", ahora - unDia, ahora)
        assertEquals(EstadoActividad.ENTREGADA, estado)
    }

    @Test
    fun `sin entrega y con plazo vencido es no entregada`() {
        val estado = Clasificador.estado("new", ahora - unDia, ahora)
        assertEquals(EstadoActividad.NO_ENTREGADA, estado)
    }

    @Test
    fun `un borrador sin enviar sigue estando pendiente si queda plazo`() {
        val estado = Clasificador.estado("draft", ahora + unDia, ahora)
        assertEquals(EstadoActividad.PENDIENTE, estado)
    }

    @Test
    fun `sin fecha limite nunca es no entregada`() {
        val estado = Clasificador.estado(null, null, ahora)
        assertEquals(EstadoActividad.PENDIENTE, estado)
    }

    @Test
    fun `los grupos reparten por plazo restante`() {
        assertEquals(GrupoPlazo.SIN_FECHA, Clasificador.grupo(null, ahora, zona))
        assertEquals(GrupoPlazo.VENCIDA, Clasificador.grupo(ahora - unDia, ahora, zona))
        assertEquals(GrupoPlazo.HOY, Clasificador.grupo(ahora + 3600, ahora, zona))
        assertEquals(GrupoPlazo.ESTA_SEMANA, Clasificador.grupo(ahora + 5 * unDia, ahora, zona))
        assertEquals(GrupoPlazo.ESTE_MES, Clasificador.grupo(ahora + 20 * unDia, ahora, zona))
        assertEquals(GrupoPlazo.MAS_ADELANTE, Clasificador.grupo(ahora + 60 * unDia, ahora, zona))
    }

    @Test
    fun `una vencida sin entregar sobrevive a cualquier filtro de rango`() {
        val vencida = actividad(ahora - 40 * unDia, EstadoActividad.NO_ENTREGADA)
        assertTrue(Clasificador.dentroDelRango(vencida, RangoTiempo.HOY, ahora, zona))
        assertTrue(Clasificador.dentroDelRango(vencida, RangoTiempo.SEMANA, ahora, zona))
    }

    @Test
    fun `una entregada cuyo plazo ya paso deja de contar como trabajo por hacer`() {
        val entregadaVencida = actividad(ahora - 2 * unDia, EstadoActividad.ENTREGADA)

        assertFalse(Clasificador.dentroDelRango(entregadaVencida, RangoTiempo.HOY, ahora, zona))
        assertFalse(Clasificador.dentroDelRango(entregadaVencida, RangoTiempo.SEMANA, ahora, zona))
        assertFalse(Clasificador.dentroDelRango(entregadaVencida, RangoTiempo.MES, ahora, zona))
        assertTrue(Clasificador.dentroDelRango(entregadaVencida, RangoTiempo.TODO, ahora, zona))
    }

    @Test
    fun `una tarea sin fecha limite no la esconde ningun rango`() {
        val sinFecha = actividad(null, EstadoActividad.PENDIENTE)

        assertTrue(Clasificador.dentroDelRango(sinFecha, RangoTiempo.HOY, ahora, zona))
        assertTrue(Clasificador.dentroDelRango(sinFecha, RangoTiempo.SEMANA, ahora, zona))
        assertTrue(Clasificador.dentroDelRango(sinFecha, RangoTiempo.MES, ahora, zona))
        assertTrue(Clasificador.dentroDelRango(sinFecha, RangoTiempo.TODO, ahora, zona))
    }

    @Test
    fun `una fecha limite a cero cuenta como no tenerla`() {
        val sinFecha = actividad(0, EstadoActividad.PENDIENTE)

        assertTrue(Clasificador.dentroDelRango(sinFecha, RangoTiempo.SEMANA, ahora, zona))
        assertEquals(GrupoPlazo.SIN_FECHA, Clasificador.grupo(0, ahora, zona))
    }

    @Test
    fun `las tareas sin fecha van a su propio grupo y al final`() {
        val secciones = Clasificador.agrupar(
            listOf(
                actividad(null, EstadoActividad.PENDIENTE),
                actividad(ahora + 3600, EstadoActividad.PENDIENTE)
            ),
            ahora,
            zona
        )

        assertEquals(GrupoPlazo.HOY, secciones.first().grupo)
        assertEquals(GrupoPlazo.SIN_FECHA, secciones.last().grupo)
    }

    @Test
    fun `el filtro de hoy deja pasar lo que vence hoy y nada mas`() {
        val hoy = actividad(ahora + 3600, EstadoActividad.PENDIENTE)
        val manana = actividad(ahora + unDia, EstadoActividad.PENDIENTE)

        assertTrue(Clasificador.dentroDelRango(hoy, RangoTiempo.HOY, ahora, zona))
        assertFalse(Clasificador.dentroDelRango(manana, RangoTiempo.HOY, ahora, zona))
    }

    @Test
    fun `el rango recorta lo que cae fuera del plazo elegido`() {
        val dentro = actividad(ahora + 3 * unDia, EstadoActividad.PENDIENTE)
        val fuera = actividad(ahora + 45 * unDia, EstadoActividad.PENDIENTE)

        assertTrue(Clasificador.dentroDelRango(dentro, RangoTiempo.SEMANA, ahora, zona))
        assertFalse(Clasificador.dentroDelRango(fuera, RangoTiempo.SEMANA, ahora, zona))
        assertTrue(Clasificador.dentroDelRango(fuera, RangoTiempo.TODO, ahora, zona))
    }

    @Test
    fun `solo avisa de lo que sigue sin entregar y vence dentro de la ventana`() {
        val ventana = 2 * unDia
        val lista = listOf(
            actividad(ahora + unDia, EstadoActividad.PENDIENTE),
            actividad(ahora + unDia, EstadoActividad.ENTREGADA),
            actividad(ahora + 5 * unDia, EstadoActividad.PENDIENTE),
            actividad(ahora - unDia, EstadoActividad.NO_ENTREGADA),
            actividad(null, EstadoActividad.PENDIENTE)
        )

        val avisos = Clasificador.porVencer(lista, ahora, ventana)

        assertEquals(1, avisos.size)
        assertEquals(EstadoActividad.PENDIENTE, avisos.single().estado)
        assertEquals(ahora + unDia, avisos.single().fechaLimite)
    }

    @Test
    fun `el resumen cuenta cada estado por separado`() {
        val lista = listOf(
            actividad(ahora + unDia, EstadoActividad.PENDIENTE),
            actividad(ahora + unDia, EstadoActividad.PENDIENTE),
            actividad(ahora - unDia, EstadoActividad.ENTREGADA),
            actividad(ahora - unDia, EstadoActividad.NO_ENTREGADA)
        )
        val resumen = Clasificador.resumir(lista)

        assertEquals(2, resumen.pendientes)
        assertEquals(1, resumen.entregadas)
        assertEquals(1, resumen.noEntregadas)
    }

    @Test
    fun `las secciones salen ordenadas de lo mas urgente a lo mas lejano`() {
        val lista = listOf(
            actividad(ahora + 60 * unDia, EstadoActividad.PENDIENTE),
            actividad(ahora - unDia, EstadoActividad.NO_ENTREGADA),
            actividad(ahora + 3 * unDia, EstadoActividad.PENDIENTE)
        )
        val grupos = Clasificador.agrupar(lista, ahora, zona).map { it.grupo }

        assertEquals(
            listOf(GrupoPlazo.VENCIDA, GrupoPlazo.ESTA_SEMANA, GrupoPlazo.MAS_ADELANTE),
            grupos
        )
    }
}
