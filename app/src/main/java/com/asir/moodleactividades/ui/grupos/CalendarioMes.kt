package com.asir.moodleactividades.ui.grupos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asir.moodleactividades.domain.CalendarioExamenes
import com.asir.moodleactividades.domain.Examen
import com.asir.moodleactividades.ui.theme.RojoNoEntregada
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ESPANOL = Locale.forLanguageTag("es-ES")
private val TITULO_MES = DateTimeFormatter.ofPattern("LLLL yyyy", ESPANOL)
private val INICIALES = listOf("L", "M", "X", "J", "V", "S", "D")

/**
 * Un mes en rejilla, de lunes a domingo. Los días con examen llevan un punto debajo —dos
 * si hay más de uno—, hoy va rodeado y el día elegido, relleno. Tocar un día lo elige y
 * tocarlo otra vez lo suelta, para volver a la lista de próximos.
 */
@Composable
fun CalendarioMes(
    mes: YearMonth,
    examenesPorDia: Map<LocalDate, List<Examen>>,
    hoy: LocalDate,
    seleccionado: LocalDate?,
    alCambiarMes: (YearMonth) -> Unit,
    alElegirDia: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { alCambiarMes(mes.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior")
                }
                Text(
                    text = mes.format(TITULO_MES).replaceFirstChar { it.uppercase(ESPANOL) },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { alCambiarMes(YearMonth.from(hoy)) },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                IconButton(onClick = { alCambiarMes(mes.plusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente")
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)) {
                INICIALES.forEach { inicial ->
                    Text(
                        text = inicial,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            CalendarioExamenes.celdas(mes).chunked(7).forEach { semana ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    semana.forEach { dia ->
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (dia != null) {
                                Dia(
                                    dia = dia,
                                    examenes = examenesPorDia[dia].orEmpty().size,
                                    esHoy = dia == hoy,
                                    elegido = dia == seleccionado,
                                    alPulsar = { alElegirDia(if (dia == seleccionado) null else dia) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dia(dia: LocalDate, examenes: Int, esHoy: Boolean, elegido: Boolean, alPulsar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val nombre = dia.dayOfWeek.getDisplayName(TextStyle.FULL, ESPANOL) + " " + dia.dayOfMonth +
        when (examenes) {
            0 -> ""
            1 -> ", un examen"
            else -> ", $examenes exámenes"
        }

    Column(
        modifier = Modifier
            .padding(2.dp)
            .clip(CircleShape)
            .then(if (elegido) Modifier.background(colores.primary) else Modifier)
            .then(if (esHoy && !elegido) Modifier.border(1.5.dp, colores.primary, CircleShape) else Modifier)
            .clickable(onClick = alPulsar)
            .semantics { contentDescription = nombre }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dia.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (examenes > 0 || esHoy) FontWeight.SemiBold else FontWeight.Normal,
            color = if (elegido) colores.onPrimary else colores.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 1.dp)) {
            repeat(examenes.coerceAtMost(2)) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(if (elegido) colores.onPrimary else RojoNoEntregada, CircleShape)
                )
            }
            // Sin punto se reserva el hueco: si no, los números bailarían de altura.
            if (examenes == 0) Box(modifier = Modifier.size(5.dp))
        }
    }
}
