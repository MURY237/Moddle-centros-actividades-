package com.asir.moodleactividades.ui.notas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.domain.Calificacion
import com.asir.moodleactividades.domain.FiltroNotas
import com.asir.moodleactividades.domain.NotasDeCurso
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.theme.DegradadoCabecera
import com.asir.moodleactividades.ui.theme.RojoNoEntregada
import com.asir.moodleactividades.ui.theme.RojoNoEntregadaFondo
import com.asir.moodleactividades.ui.theme.RojoNoEntregadaOscuro
import com.asir.moodleactividades.ui.theme.VerdeEntregada
import com.asir.moodleactividades.ui.theme.VerdeEntregadaFondo
import com.asir.moodleactividades.ui.theme.VerdeEntregadaOscuro
import com.asir.moodleactividades.ui.theme.fondoDeEstado

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotasScreen(viewModel: NotasViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {

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
                    .padding(top = 12.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mis notas",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = if (estado.totalEvaluables == 0) {
                            "Calificaciones de todas tus asignaturas"
                        } else {
                            "${estado.totalCalificadas} de ${estado.totalEvaluables} calificadas"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
                IconButton(onClick = viewModel::refrescar, enabled = !estado.cargando) {
                    Icon(Icons.Default.Refresh, "Actualizar", tint = Color.White)
                }
            }
        }

        if (estado.todos.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiltroNotas.entries.forEach { filtro ->
                    FilterChip(
                        selected = estado.filtro == filtro,
                        onClick = { viewModel.cambiarFiltro(filtro) },
                        label = { Text(filtro.etiqueta) },
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                estado.cargando && estado.cursos.isEmpty() ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                estado.error != null && estado.cursos.isEmpty() -> EstadoVacio(
                    icono = Icons.Default.CloudOff,
                    titulo = "No se pudieron cargar las notas",
                    detalle = estado.error.orEmpty(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Button(onClick = viewModel::refrescar, enabled = !estado.cargando) {
                        Text("Reintentar")
                    }
                }

                estado.cursos.isEmpty() -> EstadoVacio(
                    icono = Icons.Default.Grade,
                    titulo = if (estado.todos.isEmpty()) "Todavía sin notas" else "Sin resultados",
                    detalle = if (estado.todos.isEmpty()) {
                        "Cuando tus asignaturas tengan actividades evaluables aparecerán aquí."
                    } else {
                        "Ninguna calificación encaja con este filtro."
                    },
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    estado.cursos.forEach { curso ->
                        stickyHeader(key = curso.curso) {
                            CabeceraCurso(curso)
                        }
                        // Dos actividades del mismo curso pueden llamarse igual: la posición
                        // es lo único que las distingue de verdad.
                        itemsIndexed(
                            curso.calificaciones,
                            key = { indice, item -> "${curso.curso}-$indice-${item.nombre}" }
                        ) { _, calificacion ->
                            TarjetaCalificacion(calificacion)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CabeceraCurso(curso: NotasDeCurso) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = curso.curso,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            curso.total?.let { total ->
                Text(
                    text = "Total: ${total.nota}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(
                text = "${curso.calificadas} de ${curso.calificaciones.size} con nota",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaCalificacion(calificacion: Calificacion) {
    val aprobada = if (calificacion.calificada) calificacion.esAprobada() else null
    val color = when {
        !calificacion.calificada -> MaterialTheme.colorScheme.onSurfaceVariant
        aprobada == true -> VerdeEntregada
        aprobada == false -> RojoNoEntregada
        else -> MaterialTheme.colorScheme.primary
    }
    val fondo = when {
        !calificacion.calificada -> MaterialTheme.colorScheme.surfaceVariant
        aprobada == true -> fondoDeEstado(VerdeEntregadaFondo, VerdeEntregadaOscuro)
        aprobada == false -> fondoDeEstado(RojoNoEntregadaFondo, RojoNoEntregadaOscuro)
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(fondo, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = calificacion.nota.ifBlank { "—" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = calificacion.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(if (calificacion.calificada) calificacion.tipo.etiqueta else "Sin calificar")
                        if (calificacion.notaMaxima > 0) {
                            append(" · sobre ")
                            append(formateaMaxima(calificacion.notaMaxima))
                        }
                        if (calificacion.porcentaje.isNotBlank()) {
                            append(" · ").append(calificacion.porcentaje)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formateaMaxima(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toInt().toString() else valor.toString()

/** Null cuando la nota no es numérica y no se puede decidir si está aprobada. */
private fun Calificacion.esAprobada(): Boolean? {
    val valor = nota.replace(',', '.').filter { it.isDigit() || it == '.' || it == '-' }
        .toDoubleOrNull() ?: return null
    val maxima = notaMaxima.takeIf { it > 0 } ?: 10.0
    return valor >= maxima / 2
}
