package com.asir.moodleactividades.data.seneca

import org.junit.Assert.assertTrue
import org.junit.Test

class NavegadorFaltasTest {

    @Test
    fun `en la pantalla de acceso no se toca el menu`() {
        val guion = NavegadorFaltas.GUION

        // La pantalla de acceso también trae barra de navegación: el recorrido creía estar
        // desplegando el menú una y otra vez y se quedaba sin intentos antes de entrar.
        assertTrue(guion.contains("function pideAcceso"))
        assertTrue(guion.contains("destino: 'acceso'"))

        val corteAcceso = guion.indexOf("destino: 'acceso'")
        val corteMenu = guion.indexOf("abrirMenu(docs[d])")
        assertTrue(corteAcceso in 1 until corteMenu)
    }

    @Test
    fun `el menu tampoco se descarta por medir cero`() {
        assertTrue(NavegadorFaltas.GUION.contains("if (elemento.offsetParent) return true;"))
    }

    @Test
    fun `el acceso no cuenta como paso pulsado`() {
        // Si contara, el recorrido esperaría una recarga que no va a llegar.
        assertTrue(NavegadorFaltas.GUION.contains("{ pulsado: false, destino: 'acceso' }"))
    }
}
