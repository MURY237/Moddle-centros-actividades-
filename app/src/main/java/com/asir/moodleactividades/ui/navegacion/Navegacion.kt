package com.asir.moodleactividades.ui.navegacion

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow

/**
 * Las pantallas de la app. Las seis primeras son pestañas de la barra inferior; las otras
 * cuelgan de una de ellas y la marcan como activa mientras están abiertas.
 */
enum class Seccion(val etiqueta: String, val icono: ImageVector?, val padre: Seccion? = null) {
    ACTIVIDADES("Tareas", Icons.AutoMirrored.Filled.Assignment),
    NOTAS("Notas", Icons.Default.Grade),
    FALTAS("Faltas", Icons.Default.EventBusy),
    AVISOS("Avisos", Icons.Default.Notifications),
    HORARIO("Horario", Icons.Default.CalendarMonth),
    GRUPOS("Grupos", Icons.Default.Groups),

    /** Los trabajos de Cisco van con las tareas: la barra ya no tiene hueco. */
    NETACAD("NetAcad", null, padre = ACTIVIDADES),

    /** Los ajustes cuelgan de Avisos, que es de lo que tratan casi todos. */
    AJUSTES("Ajustes", null, padre = AVISOS);

    /** La pestaña que se ve activa en la barra. */
    val pestana: Seccion get() = padre ?: this

    companion object {
        val PESTANAS = entries.filter { it.padre == null }
    }
}

@Composable
fun BarraNavegacion(actual: Seccion, avisosSinLeer: Int, alElegir: (Seccion) -> Unit) {
    NavigationBar {
        Seccion.PESTANAS.forEach { seccion ->
            NavigationBarItem(
                selected = actual.pestana == seccion,
                onClick = { alElegir(seccion) },
                icon = {
                    val icono = requireNotNull(seccion.icono)
                    if (seccion == Seccion.AVISOS && avisosSinLeer > 0) {
                        BadgedBox(badge = { Badge { Text(avisosSinLeer.coerceAtMost(99).toString()) } }) {
                            Icon(icono, contentDescription = null)
                        }
                    } else {
                        Icon(icono, contentDescription = null)
                    }
                },
                // Una sola línea siempre. Con la letra grande del sistema, «Horario» y «Grupos»
                // se partían en dos, subían el icono y descuadraban la barra entera.
                label = { Text(seccion.etiqueta, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}
