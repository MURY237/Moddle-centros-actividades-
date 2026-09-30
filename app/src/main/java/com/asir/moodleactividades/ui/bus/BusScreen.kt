package com.asir.moodleactividades.ui.bus

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.domain.HorariosBus
import com.asir.moodleactividades.domain.LineaBus
import com.asir.moodleactividades.ui.componentes.CabeceraPantalla
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.componentes.EtiquetaEstado
import com.asir.moodleactividades.ui.componentes.FilaAjuste
import com.asir.moodleactividades.ui.componentes.IconoTonal
import com.asir.moodleactividades.ui.componentes.Tarjeta
import com.asir.moodleactividades.ui.componentes.TituloSeccion
import com.asir.moodleactividades.ui.horario.ControlesDePagina
import com.asir.moodleactividades.ui.horario.HorarioUiState
import com.asir.moodleactividades.ui.horario.HorarioViewModel
import com.asir.moodleactividades.ui.horario.VisorConZoom
import com.asir.moodleactividades.ui.theme.Tono
import com.asir.moodleactividades.ui.theme.colores
import kotlinx.coroutines.delay

data class AccionesBus(
    /** Devuelve null si la guarda, o el motivo por el que no. */
    val anadir: (nombre: String, horas: String, dias: Set<Int>) -> String? = { _, _, _ -> null },
    val borrar: (Long) -> Unit = {},
    val adjuntar: () -> Unit = {},
    val quitarDocumento: () -> Unit = {},
    val irAPagina: (Int) -> Unit = {}
)

@Composable
fun BusScreen(
    viewModel: BusViewModel,
    documento: HorarioViewModel,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val estadoDocumento by documento.estado.collectAsStateWithLifecycle()

    // Mientras se ve, la cuenta atrás va al minuto; al salir de la pantalla se para sola.
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.recalcular()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    val elegirArchivo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(documento::elegir) }

    BusContenido(
        estado = estado,
        documento = estadoDocumento,
        acciones = AccionesBus(
            anadir = viewModel::anadir,
            borrar = viewModel::borrar,
            adjuntar = { elegirArchivo.launch(arrayOf("application/pdf", "image/*")) },
            quitarDocumento = documento::quitar,
            irAPagina = documento::irAPagina
        ),
        modifier = modifier
    )
}

@Composable
fun BusContenido(
    estado: BusUiState,
    documento: HorarioUiState,
    acciones: AccionesBus,
    modifier: Modifier = Modifier
) {
    var anadiendo by rememberSaveable { mutableStateOf(false) }
    var viendoDocumento by rememberSaveable { mutableStateOf(false) }
    var borrando by rememberSaveable { mutableStateOf<Long?>(null) }

    // El papel de la parada, a pantalla completa: mirarlo es justo para lo que se adjunta.
    if (viendoDocumento && documento.horario != null) {
        BackHandler { viendoDocumento = false }
        Column(modifier = modifier.fillMaxSize()) {
            CabeceraPantalla(
                titulo = "Horario en papel",
                subtitulo = documento.horario.nombre,
                alVolver = { viendoDocumento = false }
            )
            VisorConZoom(
                estado = documento,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Espacio.lateral)
            )
            if (documento.totalPaginas > 1) ControlesDePagina(documento, acciones.irAPagina)
            Spacer(Modifier.size(Espacio.l))
        }
        return
    }

    if (estado.lineas.isEmpty() && !anadiendo && documento.horario == null) {
        EstadoVacio(
            icono = Icons.Default.DirectionsBus,
            titulo = "Sin líneas guardadas",
            detalle = "Apunta las horas de tu autobús y la app te dirá cuánto falta para el " +
                "siguiente. Funciona sin conexión: los horarios los escribes tú.",
            modifier = modifier.padding(top = Espacio.xxl)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { anadiendo = true }) { Text("Añadir una línea") }
                TextButton(onClick = acciones.adjuntar) { Text("O adjunta el horario en papel") }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Espacio.lateral, end = Espacio.lateral, bottom = Espacio.xl),
        verticalArrangement = Arrangement.spacedBy(Espacio.s)
    ) {
        estado.siguiente?.let { siguiente ->
            item(key = "siguiente") { TarjetaSiguiente(siguiente, estado.minutoAhora) }
        }

        if (estado.lineas.isNotEmpty()) {
            item(key = "titulo-lineas") { TituloSeccion("Líneas", extra = estado.lineas.size.toString()) }
        }

        items(estado.lineas, key = { it.linea.id }) { conSalida ->
            TarjetaLinea(
                conSalida = conSalida,
                minutoAhora = estado.minutoAhora,
                alBorrar = { borrando = conSalida.linea.id }
            )
        }

        item(key = "anadir") {
            if (anadiendo) {
                FormularioLinea(
                    alGuardar = { nombre, horas, dias ->
                        acciones.anadir(nombre, horas, dias).also { if (it == null) anadiendo = false }
                    },
                    alCancelar = { anadiendo = false }
                )
            } else {
                OutlinedButton(onClick = { anadiendo = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Espacio.s))
                    Text("Añadir una línea")
                }
            }
        }

        item(key = "titulo-documento") { TituloSeccion("Horario en papel") }

        item(key = "documento") {
            TarjetaDocumento(
                nombreArchivo = documento.horario?.nombre,
                alElegir = acciones.adjuntar,
                alVer = { viendoDocumento = true },
                alQuitar = acciones.quitarDocumento
            )
        }
    }

    borrando?.let { id ->
        val nombre = estado.lineas.firstOrNull { it.linea.id == id }?.linea?.nombre.orEmpty()
        AlertDialog(
            onDismissRequest = { borrando = null },
            title = { Text("¿Borrar la línea?") },
            text = { Text("Se borrará «$nombre» con todas sus horas.") },
            confirmButton = {
                TextButton(onClick = {
                    borrando = null
                    acciones.borrar(id)
                }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { borrando = null }) { Text("Cancelar") } }
        )
    }
}

/** Lo que se quiere ver nada más abrir: cuánto falta para el próximo, en grande. */
@Composable
private fun TarjetaSiguiente(conSalida: LineaConSalida, minutoAhora: Int) {
    val proxima = conSalida.proxima ?: return
    val faltan = proxima.minutosQueFaltan(minutoAhora)
    // Menos de un cuarto de hora es cuando hay que salir corriendo.
    val tono = if (proxima.diasDeEspera == 0 && faltan <= 15) Tono.AVISO else Tono.EXITO

    Tarjeta(modifier = Modifier.padding(top = Espacio.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoTonal(Icons.Default.DirectionsBus, tono, tamano = 52.dp)
            Column(modifier = Modifier.weight(1f).padding(start = Espacio.l)) {
                Text(
                    text = "PRÓXIMO AUTOBÚS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = HorariosBus.formatearHora(proxima.minutoDelDia),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = conSalida.linea.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            EtiquetaEstado(cuantoFalta(faltan, proxima.diasDeEspera), tono)
        }
    }
}

/**
 * «En 12 min» sirve para decidir si da tiempo; «en 3 h» ya no, y para mañana lo único que
 * importa es la hora, que va aparte.
 */
private fun cuantoFalta(minutos: Int, diasDeEspera: Int): String = when {
    diasDeEspera == 1 -> "Mañana"
    diasDeEspera > 1 -> "En $diasDeEspera días"
    minutos <= 0 -> "Ahora"
    minutos < 60 -> "En $minutos min"
    minutos % 60 == 0 -> "En ${minutos / 60} h"
    else -> "En ${minutos / 60} h ${minutos % 60} min"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaLinea(
    conSalida: LineaConSalida,
    minutoAhora: Int,
    alBorrar: () -> Unit
) {
    val siguienteHoy = conSalida.proxima?.takeIf { it.diasDeEspera == 0 }?.minutoDelDia

    Tarjeta(relleno = PaddingValues(start = Espacio.l, end = Espacio.xs, top = Espacio.xs, bottom = Espacio.l)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(top = Espacio.s)) {
                Text(text = conSalida.linea.nombre, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = HorariosBus.etiquetaDias(conSalida.linea.dias),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = alBorrar) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar la línea", tint = MaterialTheme.colorScheme.outline)
            }
        }

        if (conSalida.salidasDeHoy.isEmpty()) {
            Text(
                text = "Hoy no hay servicio en esta línea.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Espacio.s)
            )
            return@Tarjeta
        }

        FlowRow(
            modifier = Modifier.padding(top = Espacio.s, end = Espacio.m),
            horizontalArrangement = Arrangement.spacedBy(Espacio.s),
            verticalArrangement = Arrangement.spacedBy(Espacio.s)
        ) {
            conSalida.salidasDeHoy.forEach { salida ->
                // Las que ya han salido se apagan, pero no se ocultan: dan idea de la frecuencia.
                val (texto, fondo) = when {
                    salida == siguienteHoy -> Tono.INFO.colores().let { it.contenido to it.contenedor }
                    salida <= minutoAhora -> MaterialTheme.colorScheme.outline to MaterialTheme.colorScheme.surfaceContainer
                    else -> MaterialTheme.colorScheme.onSurface to MaterialTheme.colorScheme.surfaceContainerHigh
                }
                Text(
                    text = HorariosBus.formatearHora(salida),
                    style = MaterialTheme.typography.labelLarge,
                    color = texto,
                    modifier = Modifier
                        .background(fondo, MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun FormularioLinea(
    alGuardar: (String, String, Set<Int>) -> String?,
    alCancelar: () -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var horas by rememberSaveable { mutableStateOf("") }
    var dias by rememberSaveable { mutableStateOf(LineaBus.LABORABLES.toList()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Tarjeta {
        Text(text = "Nueva línea", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Trayecto") },
            placeholder = { Text("Pueblo → Instituto") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = Espacio.m)
        )
        OutlinedTextField(
            value = horas,
            onValueChange = {
                horas = it
                error = null
            },
            label = { Text("Horas de salida") },
            placeholder = { Text("07:15, 08:30, 14:00") },
            isError = error != null,
            supportingText = { Text(error ?: "Sepáralas por comas. Vale «7:15», «07.15» o «0715».") },
            modifier = Modifier.fillMaxWidth().padding(top = Espacio.s)
        )

        Text(
            text = "Días con servicio",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Espacio.s, bottom = Espacio.xs)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LineaBus.NOMBRES_DIAS.forEachIndexed { indice, etiqueta ->
                val dia = indice + 1
                FilterChip(
                    selected = dia in dias,
                    onClick = { dias = if (dia in dias) dias - dia else dias + dia },
                    label = { Text(etiqueta) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Espacio.m),
            horizontalArrangement = Arrangement.spacedBy(Espacio.s, Alignment.End)
        ) {
            TextButton(onClick = alCancelar) { Text("Cancelar") }
            Button(
                onClick = { error = alGuardar(nombre, horas, dias.toSet()) },
                enabled = horas.isNotBlank()
            ) { Text("Guardar") }
        }
    }
}

/**
 * Muchos horarios de pueblo son un papel en la parada o un PDF del ayuntamiento. Poder
 * adjuntarlo evita tener que copiar a mano decenas de horas, y sirve de respaldo cuando lo
 * escrito se queda corto: los festivos, los refuerzos, las notas al pie.
 */
@Composable
private fun TarjetaDocumento(
    nombreArchivo: String?,
    alElegir: () -> Unit,
    alVer: () -> Unit,
    alQuitar: () -> Unit
) {
    Tarjeta(relleno = PaddingValues(0.dp)) {
        FilaAjuste(
            titulo = nombreArchivo ?: "Adjuntar el horario",
            detalle = if (nombreArchivo == null) "El PDF del ayuntamiento o una foto de la parada" else "Toca para verlo",
            icono = Icons.Default.AttachFile,
            tono = Tono.NEUTRO,
            alPulsar = if (nombreArchivo == null) alElegir else alVer,
            final = if (nombreArchivo == null) {
                null
            } else {
                {
                    IconButton(onClick = alQuitar) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Quitar el documento",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        )
    }
}
