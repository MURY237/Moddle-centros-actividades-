package com.asir.moodleactividades.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asir.moodleactividades.data.AjustesAvisos

@Composable
fun AjustesAvisosSeccion(
    ajustes: AjustesAvisos,
    puedeNotificar: Boolean,
    alCambiarEntregas: (Boolean) -> Unit,
    alCambiarNuevas: (Boolean) -> Unit,
    alCambiarNotas: (Boolean) -> Unit,
    alCambiarAntelacion: (Int) -> Unit,
    alCambiarFrecuencia: (Int) -> Unit,
    alCambiarHora: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Notificaciones",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        if (!puedeNotificar) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Android tiene bloqueadas las notificaciones de esta app. " +
                        "Actívalas en los ajustes del sistema para que estos avisos funcionen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        TarjetaInterruptor(
            icono = Icons.Default.NotificationsActive,
            titulo = "Avisar antes de las entregas",
            detalle = "Notifica las tareas sin entregar cuyo plazo se acerca.",
            activo = ajustes.avisarEntregas,
            alCambiar = alCambiarEntregas
        )

        if (ajustes.avisarEntregas) {
            TarjetaOpciones(
                titulo = "Con cuánta antelación",
                opciones = AjustesAvisos.ANTELACIONES,
                seleccionada = ajustes.antelacionHoras,
                etiqueta = { AjustesAvisos.etiquetaAntelacion(it) },
                alElegir = alCambiarAntelacion
            )
        }

        TarjetaInterruptor(
            icono = Icons.Default.NewReleases,
            titulo = "Avisar de actividades nuevas",
            detalle = "Notifica cuando un profesor publica una tarea o un examen.",
            activo = ajustes.avisarNuevas,
            alCambiar = alCambiarNuevas
        )

        TarjetaInterruptor(
            icono = Icons.Default.Grade,
            titulo = "Avisar de las notas publicadas",
            detalle = "Notifica en cuanto un profesor califica una tarea o un examen.",
            activo = ajustes.avisarNotas,
            alCambiar = alCambiarNotas
        )

        TarjetaOpciones(
            titulo = "Cuántas veces comprobar al día",
            opciones = AjustesAvisos.FRECUENCIAS,
            seleccionada = ajustes.comprobacionesDiarias,
            etiqueta = { AjustesAvisos.etiquetaFrecuencia(it) },
            alElegir = alCambiarFrecuencia
        )

        TarjetaOpciones(
            titulo = "Primera comprobación del día",
            opciones = HORAS,
            seleccionada = ajustes.horaPreferida,
            etiqueta = { AjustesAvisos.etiquetaHora(it) },
            alElegir = alCambiarHora
        )

        Text(
            text = "Android decide el momento exacto para ahorrar batería, así que las " +
                "comprobaciones pueden retrasarse unos minutos respecto a la hora elegida.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

private val HORAS = listOf(6, 7, 8, 9, 12, 15, 18, 20, 22)

@Composable
private fun TarjetaInterruptor(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    activo: Boolean,
    alCambiar: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { alCambiar(!activo) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
                    imageVector = icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titulo, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = activo, onCheckedChange = alCambiar)
        }
    }
}

@Composable
private fun TarjetaOpciones(
    titulo: String,
    opciones: List<Int>,
    seleccionada: Int,
    etiqueta: (Int) -> String,
    alElegir: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 14.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                opciones.forEach { opcion ->
                    FilterChip(
                        selected = seleccionada == opcion,
                        onClick = { alElegir(opcion) },
                        label = { Text(etiqueta(opcion)) },
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }
    }
}
