package com.asir.moodleactividades.ui.asistencia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asir.moodleactividades.domain.SondeoAsistencia
import com.asir.moodleactividades.ui.theme.AmbarPendiente
import com.asir.moodleactividades.ui.theme.RojoNoEntregada
import com.asir.moodleactividades.ui.theme.VerdeEntregada

@Composable
fun SeccionAsistencia(
    estado: AsistenciaUiState,
    alComprobar: () -> Unit,
    alVerFaltasSeneca: () -> Unit,
    modifier: Modifier = Modifier
) {
    val portapapeles = LocalClipboardManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Faltas de asistencia",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¿Están tus faltas en Moodle?",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "En los centros de Andalucía las faltas se llevan en Séneca, " +
                                "que no publica ninguna API. Esta comprobación pregunta a tu " +
                                "Moodle si además ofrece el módulo de asistencia.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                when {
                    estado.cargando -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Preguntando al centro…",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    estado.error != null -> Resultado(
                        color = RojoNoEntregada,
                        titulo = "No se pudo comprobar",
                        detalle = estado.error
                    )

                    estado.sondeo != null -> ResultadoDelSondeo(estado.sondeo)

                    else -> Button(
                        onClick = alComprobar,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Comprobar en mi centro")
                    }
                }

                estado.sondeo?.let { sondeo ->
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = alComprobar) { Text("Volver a comprobar") }
                        TextButton(
                            onClick = {
                                portapapeles.setText(AnnotatedString(sondeo.comoTexto()))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.size(6.dp))
                            Text("Copiar diagnóstico")
                        }
                    }
                }

                if (estado.error != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = alComprobar) { Text("Reintentar") }
                }
            }
        }

        // La vía por Moodle puede no existir, pero las faltas siempre se pueden leer de la
        // sesión que el propio alumno abre en Séneca.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Leer las faltas desde Séneca",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "La app abre Séneca para que entres tú y, cuando llegues a la tabla " +
                        "de faltas, la guarda y la agrupa por asignatura. Tu contraseña no " +
                        "pasa por la aplicación.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = alVerFaltasSeneca, shape = RoundedCornerShape(14.dp)) {
                    Text("Ver mis faltas")
                }
            }
        }
    }
}

@Composable
private fun ResultadoDelSondeo(sondeo: SondeoAsistencia) {
    if (sondeo.disponible) {
        Resultado(
            color = VerdeEntregada,
            titulo = "Tu centro sí ofrece el módulo de asistencia",
            detalle = "Se han encontrado ${sondeo.funcionesAsistencia.size} funciones. " +
                "Con esto ya se pueden leer las faltas por asignatura desde la app."
        )
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sondeo.funcionesAsistencia.forEach { funcion ->
                Text(
                    text = funcion,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    } else {
        Resultado(
            color = AmbarPendiente,
            titulo = "Tu centro no publica las faltas por Moodle",
            detalle = "De las ${sondeo.totalFunciones} funciones que abre a la app, ninguna es " +
                "del módulo de asistencia. Tus faltas están en Séneca, y Séneca no ofrece " +
                "ninguna vía oficial para consultarlas desde fuera."
        )
    }
    Spacer(Modifier.height(10.dp))
    Text(
        text = "${sondeo.sitio} · ${sondeo.version.ifBlank { "versión desconocida" }}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun Resultado(
    color: androidx.compose.ui.graphics.Color,
    titulo: String,
    detalle: String
) {
    Column {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            color = color
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = detalle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
