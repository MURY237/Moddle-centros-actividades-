package com.asir.moodleactividades.ui.avisos

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.data.Aviso
import com.asir.moodleactividades.data.TipoAviso
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.haceCuanto
import com.asir.moodleactividades.ui.theme.AmbarPendiente
import com.asir.moodleactividades.ui.theme.AmbarPendienteFondo
import com.asir.moodleactividades.ui.theme.AmbarPendienteOscuro
import com.asir.moodleactividades.ui.theme.DegradadoCabecera
import com.asir.moodleactividades.ui.theme.RojoNoEntregada
import com.asir.moodleactividades.ui.theme.RojoNoEntregadaFondo
import com.asir.moodleactividades.ui.theme.RojoNoEntregadaOscuro
import com.asir.moodleactividades.ui.theme.VerdeEntregada
import com.asir.moodleactividades.ui.theme.VerdeEntregadaFondo
import com.asir.moodleactividades.ui.theme.VerdeEntregadaOscuro
import com.asir.moodleactividades.ui.theme.fondoDeEstado

@Composable
fun AvisosScreen(
    viewModel: AvisosViewModel,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {

        Cabecera(cantidad = estado.avisos.size, alVaciar = viewModel::vaciar)

        if (estado.avisos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EstadoVacio(
                    icono = Icons.Default.NotificationsNone,
                    titulo = "Sin avisos todavía",
                    detalle = "Aquí se guardan las notificaciones que te envía la app: entregas " +
                        "cercanas, actividades nuevas y notas recién publicadas."
                )
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(estado.avisos, key = { "${it.momento}-${it.titulo}" }) { aviso ->
                TarjetaAviso(aviso) {
                    aviso.url?.let { enlace ->
                        runCatching {
                            contexto.startActivity(Intent(Intent.ACTION_VIEW, enlace.toUri()))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Cabecera(cantidad: Int, alVaciar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(DegradadoCabecera)
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Avisos",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = when (cantidad) {
                        0 -> "Ninguna notificación guardada"
                        1 -> "1 notificación guardada"
                        else -> "$cantidad notificaciones guardadas"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }
            if (cantidad > 0) {
                IconButton(onClick = alVaciar) {
                    Icon(Icons.Default.DeleteSweep, "Vaciar el historial", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TarjetaAviso(aviso: Aviso, alPulsar: () -> Unit) {
    val color = colorDe(aviso.tipo)
    val fondo = fondoDe(aviso.tipo)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = aviso.url != null, onClick = alPulsar),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(fondo, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconoDe(aviso.tipo),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = aviso.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (aviso.url != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = aviso.texto,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = aviso.tipo.etiqueta,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        modifier = Modifier
                            .background(fondo, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    Text(
                        text = haceCuanto(aviso.momento),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun iconoDe(tipo: TipoAviso): ImageVector = when (tipo) {
    TipoAviso.ENTREGA -> Icons.Default.NotificationsActive
    TipoAviso.NUEVA -> Icons.Default.NewReleases
    TipoAviso.NOTA -> Icons.Default.Grade
    TipoAviso.FALTA -> Icons.Default.EventBusy
}

private fun colorDe(tipo: TipoAviso): Color = when (tipo) {
    TipoAviso.ENTREGA -> AmbarPendiente
    TipoAviso.NUEVA -> AmbarPendiente
    TipoAviso.NOTA -> VerdeEntregada
    TipoAviso.FALTA -> RojoNoEntregada
}

@Composable
private fun fondoDe(tipo: TipoAviso): Color = when (tipo) {
    TipoAviso.NOTA -> fondoDeEstado(VerdeEntregadaFondo, VerdeEntregadaOscuro)
    TipoAviso.FALTA -> fondoDeEstado(RojoNoEntregadaFondo, RojoNoEntregadaOscuro)
    else -> fondoDeEstado(AmbarPendienteFondo, AmbarPendienteOscuro)
}
