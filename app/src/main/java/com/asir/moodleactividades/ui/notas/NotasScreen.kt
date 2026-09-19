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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
                        text = "Calificaciones de todas tus asignaturas",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
                IconButton(onClick = viewModel::refrescar, enabled = !estado.cargando) {
                    Icon(Icons.Default.Refresh, "Actualizar", tint = Color.White)
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
                    titulo = "Todavía sin notas",
                    detalle = "Cuando tus profesores publiquen calificaciones aparecerán aquí.",
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    estado.cursos.forEach { curso ->
                        stickyHeader(key = curso.curso) {
                            CabeceraCurso(curso.curso, curso.total?.nota, curso.total?.porcentaje)
                        }
                        items(
                            curso.calificaciones,
                            key = { "${curso.curso}-${it.nombre}-${it.nota}" }
                        ) { calificacion ->
                            TarjetaCalificacion(calificacion)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CabeceraCurso(nombre: String, notaTotal: String?, porcentaje: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = nombre,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (notaTotal != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "Total: $notaTotal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                if (!porcentaje.isNullOrBlank()) {
                    Text(
                        text = porcentaje,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaCalificacion(calificacion: Calificacion) {
    val aprobada = calificacion.esAprobada()
    val color = when (aprobada) {
        true -> VerdeEntregada
        false -> RojoNoEntregada
        null -> MaterialTheme.colorScheme.primary
    }
    val fondo = when (aprobada) {
        true -> fondoDeEstado(VerdeEntregadaFondo, VerdeEntregadaOscuro)
        false -> fondoDeEstado(RojoNoEntregadaFondo, RojoNoEntregadaOscuro)
        null -> MaterialTheme.colorScheme.primaryContainer
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
                    text = calificacion.nota,
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
                        append(calificacion.tipo.etiqueta)
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
