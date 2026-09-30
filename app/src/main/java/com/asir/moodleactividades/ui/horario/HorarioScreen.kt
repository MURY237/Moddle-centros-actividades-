package com.asir.moodleactividades.ui.horario

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.ui.bus.BusScreen
import com.asir.moodleactividades.ui.bus.BusViewModel
import com.asir.moodleactividades.ui.componentes.CabeceraPantalla
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.componentes.MenuMas
import com.asir.moodleactividades.ui.componentes.OpcionMenu

/** Las dos cosas que un alumno consulta a diario: a qué clase toca y a qué hora es el bus. */
enum class VistaHorario(val etiqueta: String) {
    CLASES("Clases"),
    BUS("Autobús")
}

data class AccionesHorario(
    val elegirArchivo: () -> Unit = {},
    val quitar: () -> Unit = {},
    val irAPagina: (Int) -> Unit = {}
)

private val TIPOS_ACEPTADOS = arrayOf("application/pdf", "image/*")

@Composable
fun HorarioScreen(
    viewModel: HorarioViewModel,
    busViewModel: BusViewModel,
    documentoBus: HorarioViewModel,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var vista by rememberSaveable { mutableStateOf(VistaHorario.CLASES) }

    val elegirArchivo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::elegir) }

    HorarioContenido(
        estado = estado,
        vista = vista,
        alCambiarVista = { vista = it },
        acciones = AccionesHorario(
            elegirArchivo = { elegirArchivo.launch(TIPOS_ACEPTADOS) },
            quitar = viewModel::quitar,
            irAPagina = viewModel::irAPagina
        ),
        modifier = modifier
    ) {
        BusScreen(viewModel = busViewModel, documento = documentoBus, modifier = Modifier.fillMaxSize())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorarioContenido(
    estado: HorarioUiState,
    vista: VistaHorario,
    alCambiarVista: (VistaHorario) -> Unit,
    acciones: AccionesHorario,
    modifier: Modifier = Modifier,
    contenidoBus: @Composable () -> Unit
) {
    var confirmarQuitar by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        CabeceraPantalla(
            titulo = "Horario",
            subtitulo = if (vista == VistaHorario.BUS) {
                "Tus líneas de autobús"
            } else {
                estado.horario?.nombre ?: "Sin horario guardado"
            }
        ) {
            if (estado.horario != null && vista == VistaHorario.CLASES) {
                MenuMas(
                    listOf(
                        OpcionMenu("Cambiar de archivo", Icons.Default.SwapHoriz, alPulsar = acciones.elegirArchivo),
                        OpcionMenu("Quitar el horario", Icons.Default.Delete, peligrosa = true) {
                            confirmarQuitar = true
                        }
                    )
                )
            }
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Espacio.lateral)
                .padding(bottom = Espacio.m)
        ) {
            VistaHorario.entries.forEachIndexed { indice, opcion ->
                SegmentedButton(
                    selected = vista == opcion,
                    onClick = { alCambiarVista(opcion) },
                    shape = SegmentedButtonDefaults.itemShape(indice, VistaHorario.entries.size),
                    label = { Text(opcion.etiqueta) }
                )
            }
        }

        if (vista == VistaHorario.BUS) {
            contenidoBus()
            return@Column
        }

        Clases(estado, acciones)
    }

    if (confirmarQuitar) {
        AlertDialog(
            onDismissRequest = { confirmarQuitar = false },
            title = { Text("¿Quitar el horario?") },
            text = { Text("Se borrará la copia guardada en la app. El archivo original no se toca.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarQuitar = false
                    acciones.quitar()
                }) { Text("Quitar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarQuitar = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun Clases(estado: HorarioUiState, acciones: AccionesHorario) {
    when {
        estado.horario == null && estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
        }

        estado.horario == null || (estado.pagina == null && estado.error != null) -> EstadoVacio(
            icono = Icons.Default.CalendarMonth,
            titulo = if (estado.error != null) "No se pudo abrir" else "Añade tu horario",
            detalle = estado.error ?: ("Guarda aquí el horario de clase en PDF o como imagen y lo " +
                "tendrás siempre a mano, incluso sin conexión."),
            modifier = Modifier.padding(top = Espacio.xxl)
        ) {
            Button(onClick = acciones.elegirArchivo) {
                Text(if (estado.error != null) "Elegir otro archivo" else "Elegir archivo")
            }
        }

        else -> Column(modifier = Modifier.fillMaxSize().padding(horizontal = Espacio.lateral)) {
            VisorConZoom(estado = estado, modifier = Modifier.weight(1f).fillMaxWidth())
            if (estado.totalPaginas > 1) {
                ControlesDePagina(estado, acciones.irAPagina)
            } else {
                Spacer(Modifier.height(Espacio.l))
            }
        }
    }
}

/**
 * La página o la foto, con zoom de pellizco y doble toque. El desplazamiento se limita al
 * borde de la imagen ampliada: antes se podía arrastrar hasta perderla de vista.
 */
@Composable
internal fun VisorConZoom(estado: HorarioUiState, modifier: Modifier = Modifier) {
    val imagen = remember(estado.pagina) { estado.pagina?.asImageBitmap() }
    // Al cambiar de página se vuelve a ver entera.
    var escala by remember(imagen) { mutableFloatStateOf(1f) }
    var desplazamiento by remember(imagen) { mutableStateOf(Offset.Zero) }
    var tamano by remember { mutableStateOf(IntSize.Zero) }

    fun limitar(propuesto: Offset, escalaActual: Float): Offset {
        val maxX = tamano.width * (escalaActual - 1f) / 2f
        val maxY = tamano.height * (escalaActual - 1f) / 2f
        return Offset(propuesto.x.coerceIn(-maxX, maxX), propuesto.y.coerceIn(-maxY, maxY))
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .onSizeChanged { tamano = it }
            .pointerInput(imagen) {
                detectTapGestures(onDoubleTap = { punto ->
                    if (escala > 1f) {
                        escala = 1f
                        desplazamiento = Offset.Zero
                    } else {
                        escala = 2.5f
                        // Se amplía hacia donde se ha tocado, no hacia el centro.
                        val centro = Offset(tamano.width / 2f, tamano.height / 2f)
                        desplazamiento = limitar((centro - punto) * (escala - 1f), escala)
                    }
                })
            }
            .pointerInput(imagen) {
                detectTransformGestures { _, arrastre, acercamiento, _ ->
                    escala = (escala * acercamiento).coerceIn(1f, 6f)
                    desplazamiento = limitar(desplazamiento + arrastre, escala)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            imagen != null -> Image(
                bitmap = imagen,
                contentDescription = "Horario",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = escala,
                        scaleY = escala,
                        translationX = desplazamiento.x,
                        translationY = desplazamiento.y
                    )
            )
            !estado.cargando -> Text(
                text = "No se pudo mostrar este archivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (estado.cargando) {
            CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
        }

        if (escala > 1f) {
            TextButton(
                onClick = {
                    escala = 1f
                    desplazamiento = Offset.Zero
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(Espacio.s)
            ) {
                Text("Ajustar")
            }
        }
    }
}

@Composable
internal fun ControlesDePagina(estado: HorarioUiState, alIr: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Espacio.s),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { alIr(estado.indicePagina - 1) },
            enabled = estado.indicePagina > 0 && !estado.cargando
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Página anterior")
        }
        Text(
            text = "Página ${estado.indicePagina + 1} de ${estado.totalPaginas}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Espacio.m)
        )
        IconButton(
            onClick = { alIr(estado.indicePagina + 1) },
            enabled = estado.indicePagina < estado.totalPaginas - 1 && !estado.cargando
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Página siguiente")
        }
    }
}
