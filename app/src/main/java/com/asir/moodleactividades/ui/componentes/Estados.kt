package com.asir.moodleactividades.ui.componentes

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.asir.moodleactividades.domain.EstadoActividad
import com.asir.moodleactividades.ui.theme.Tono

/** El mismo tono para el mismo estado en toda la app: Moodle, NetAcad y el calendario. */
fun EstadoActividad.tono(): Tono = when (this) {
    EstadoActividad.ENTREGADA -> Tono.EXITO
    EstadoActividad.PENDIENTE -> Tono.AVISO
    EstadoActividad.NO_ENTREGADA -> Tono.PELIGRO
}

/**
 * Una opción del menú «⋮». Las [peligrosas] (cerrar sesión, borrar) van al final, separadas
 * y en rojo, para que no se pulsen sin querer.
 */
data class OpcionMenu(
    val texto: String,
    val icono: ImageVector,
    val peligrosa: Boolean = false,
    val alPulsar: () -> Unit
)

@Composable
fun MenuMas(opciones: List<OpcionMenu>, descripcion: String = "Más opciones") {
    if (opciones.isEmpty()) return
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = descripcion)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            val normales = opciones.filterNot { it.peligrosa }
            val peligrosas = opciones.filter { it.peligrosa }
            normales.forEach { opcion -> Opcion(opcion) { abierto = false } }
            if (normales.isNotEmpty() && peligrosas.isNotEmpty()) HorizontalDivider()
            peligrosas.forEach { opcion -> Opcion(opcion) { abierto = false } }
        }
    }
}

@Composable
private fun Opcion(opcion: OpcionMenu, alCerrar: () -> Unit) {
    val color = if (opcion.peligrosa) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(
        text = { Text(opcion.texto, color = color) },
        leadingIcon = { Icon(opcion.icono, contentDescription = null, tint = color) },
        onClick = {
            alCerrar()
            opcion.alPulsar()
        }
    )
}
