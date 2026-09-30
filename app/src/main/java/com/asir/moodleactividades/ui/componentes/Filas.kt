package com.asir.moodleactividades.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.asir.moodleactividades.ui.theme.Tono

/*
 * Filas para listas de opciones dentro de una Tarjeta sin relleno: ajustes, cuentas, accesos.
 * Van separadas con DivisorFila y comparten márgenes, así todas las listas se leen igual.
 */

/**
 * Una fila con icono, título, detalle y, a la derecha, lo que haga falta. Si se puede pulsar
 * y no trae nada a la derecha, lleva una flecha.
 */
@Composable
fun FilaAjuste(
    titulo: String,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    icono: ImageVector? = null,
    tono: Tono = Tono.INFO,
    alPulsar: (() -> Unit)? = null,
    final: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (alPulsar != null) Modifier.clickable(role = Role.Button, onClick = alPulsar) else Modifier)
            .padding(horizontal = Espacio.l, vertical = Espacio.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icono != null) {
            IconoTonal(icono, tono, tamano = 36.dp)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (icono != null) Espacio.m else 0.dp, end = Espacio.s)
        ) {
            Text(text = titulo, style = MaterialTheme.typography.titleSmall)
            if (!detalle.isNullOrBlank()) {
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        when {
            final != null -> final()
            alPulsar != null -> Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Una opción que se activa o desactiva. Toda la fila cambia el interruptor. */
@Composable
fun FilaInterruptor(
    titulo: String,
    activo: Boolean,
    alCambiar: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    icono: ImageVector? = null,
    tono: Tono = Tono.INFO
) {
    FilaAjuste(
        titulo = titulo,
        detalle = detalle,
        icono = icono,
        tono = tono,
        // toggleable en la fila y el Switch sin acción propia: un solo elemento para TalkBack.
        modifier = modifier.toggleable(value = activo, role = Role.Switch, onValueChange = alCambiar),
        final = { Switch(checked = activo, onCheckedChange = null) }
    )
}

@Composable
fun DivisorFila(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = MaterialTheme.colorScheme.outlineVariant)
}
