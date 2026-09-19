package com.asir.moodleactividades.ui.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun AnilloProgreso(
    progreso: Float,
    etiquetaCentral: String,
    subEtiqueta: String,
    color: Color,
    colorPista: Color,
    modifier: Modifier = Modifier,
    diametro: androidx.compose.ui.unit.Dp = 104.dp,
    grosor: androidx.compose.ui.unit.Dp = 10.dp
) {
    val animado by animateFloatAsState(
        targetValue = progreso.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "progreso"
    )

    Box(modifier = modifier.size(diametro), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diametro)) {
            val trazo = Stroke(width = grosor.toPx(), cap = StrokeCap.Round)
            val margen = grosor.toPx() / 2
            val lado = size.minDimension - grosor.toPx()

            drawArc(
                color = colorPista,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(margen, margen),
                size = androidx.compose.ui.geometry.Size(lado, lado),
                style = trazo
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animado,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(margen, margen),
                size = androidx.compose.ui.geometry.Size(lado, lado),
                style = trazo
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = etiquetaCentral,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subEtiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = colorPista,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun Estadistica(
    valor: Int,
    etiqueta: String,
    color: Color,
    fondo: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(fondo, RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = valor.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun Etiqueta(
    texto: String,
    color: Color,
    fondo: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(fondo, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun EstadoVacio(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    detalle: String,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp)
            )
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        accion?.invoke()
    }
}

@Composable
fun FilaEstadisticas(
    pendientes: Int,
    entregadas: Int,
    noEntregadas: Int,
    colorPendiente: Color,
    fondoPendiente: Color,
    colorEntregada: Color,
    fondoEntregada: Color,
    colorNoEntregada: Color,
    fondoNoEntregada: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Estadistica(pendientes, "Pendientes", colorPendiente, fondoPendiente, Modifier.weight(1f))
        Estadistica(entregadas, "Entregadas", colorEntregada, fondoEntregada, Modifier.weight(1f))
        Estadistica(noEntregadas, "Sin entregar", colorNoEntregada, fondoNoEntregada, Modifier.weight(1f))
    }
}
