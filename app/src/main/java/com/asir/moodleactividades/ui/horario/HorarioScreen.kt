package com.asir.moodleactividades.ui.horario

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.ui.bus.BusScreen
import com.asir.moodleactividades.ui.bus.BusViewModel
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.theme.DegradadoCabecera

/** Las dos cosas que un alumno consulta a diario: a qué clase toca y a qué hora es el bus. */
private enum class VistaHorario(val etiqueta: String) {
    CLASES("Clases"),
    BUS("Autobús")
}

@Composable
fun HorarioScreen(
    viewModel: HorarioViewModel,
    busViewModel: BusViewModel,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var vista by remember { mutableStateOf(VistaHorario.CLASES) }

    // La cuenta atrás del bus se rehace al entrar: un temporizador para esto sería gastar
    // batería por algo que se mira de pasada.
    LaunchedEffect(vista) {
        if (vista == VistaHorario.BUS) busViewModel.recalcular()
    }

    val elegirArchivo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::elegir) }

    val tiposAceptados = arrayOf("application/pdf", "image/*")

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
                        text = "Mi horario",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = if (vista == VistaHorario.BUS) {
                            "Horarios de autobús"
                        } else {
                            estado.horario?.nombre ?: "Sin horario guardado"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (estado.horario != null && vista == VistaHorario.CLASES) {
                    IconButton(onClick = { elegirArchivo.launch(tiposAceptados) }) {
                        Icon(Icons.Default.SwapHoriz, "Cambiar horario", tint = Color.White)
                    }
                    IconButton(onClick = viewModel::quitar) {
                        Icon(Icons.Default.Delete, "Quitar horario", tint = Color.White)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VistaHorario.entries.forEach { opcion ->
                FilterChip(
                    selected = vista == opcion,
                    onClick = { vista = opcion },
                    label = { Text(opcion.etiqueta) },
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        if (vista == VistaHorario.BUS) {
            BusScreen(viewModel = busViewModel, modifier = Modifier.fillMaxSize())
            return@Column
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                estado.error != null && estado.horario == null -> EstadoVacio(
                    icono = Icons.Default.CalendarMonth,
                    titulo = "No se pudo abrir",
                    detalle = estado.error.orEmpty(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Button(onClick = { elegirArchivo.launch(tiposAceptados) }) {
                        Text("Elegir otro archivo")
                    }
                }

                estado.horario == null -> EstadoVacio(
                    icono = Icons.Default.CalendarMonth,
                    titulo = "Añade tu horario",
                    detalle = "Guarda aquí el horario de clase en PDF o como imagen y lo " +
                        "tendrás siempre a mano, incluso sin conexión.",
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Button(onClick = { elegirArchivo.launch(tiposAceptados) }) {
                        Text("Elegir archivo")
                    }
                }

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    VisorConZoom(
                        estado = estado,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                    if (estado.totalPaginas > 1) {
                        ControlesDePagina(estado, viewModel::irAPagina)
                    }
                }
            }
        }
    }
}

@Composable
private fun VisorConZoom(estado: HorarioUiState, modifier: Modifier = Modifier) {
    var escala by remember { mutableFloatStateOf(1f) }
    var desplazamientoX by remember { mutableFloatStateOf(0f) }
    var desplazamientoY by remember { mutableFloatStateOf(0f) }

    val horario = estado.horario ?: return
    val imagen = remember(horario.archivo.path, horario.esPdf, estado.pagina) {
        if (horario.esPdf) {
            estado.pagina?.asImageBitmap()
        } else {
            runCatching {
                BitmapFactory.decodeFile(horario.archivo.path)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(imagen) {
                detectTransformGestures { _, arrastre, acercamiento, _ ->
                    escala = (escala * acercamiento).coerceIn(1f, 6f)
                    if (escala > 1f) {
                        desplazamientoX += arrastre.x
                        desplazamientoY += arrastre.y
                    } else {
                        desplazamientoX = 0f
                        desplazamientoY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (imagen == null) {
            Text(
                text = "No se pudo mostrar este archivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Image(
                bitmap = imagen,
                contentDescription = "Horario",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = escala,
                        scaleY = escala,
                        translationX = desplazamientoX,
                        translationY = desplazamientoY
                    )
            )
        }

        if (escala > 1f) {
            TextButton(
                onClick = {
                    escala = 1f
                    desplazamientoX = 0f
                    desplazamientoY = 0f
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
            ) {
                Text("Ajustar")
            }
        }
    }
}

@Composable
private fun ControlesDePagina(estado: HorarioUiState, alIr: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { alIr(estado.indicePagina - 1) },
            enabled = estado.indicePagina > 0
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Página anterior")
        }
        Text(
            text = "Página ${estado.indicePagina + 1} de ${estado.totalPaginas}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        IconButton(
            onClick = { alIr(estado.indicePagina + 1) },
            enabled = estado.indicePagina < estado.totalPaginas - 1
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Página siguiente")
        }
    }
}
