package com.asir.moodleactividades.ui.bus

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.domain.HorariosBus
import com.asir.moodleactividades.domain.LineaBus
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.horario.ControlesDePagina
import com.asir.moodleactividades.ui.horario.HorarioViewModel
import com.asir.moodleactividades.ui.horario.VisorConZoom
import com.asir.moodleactividades.ui.theme.AmbarPendiente
import com.asir.moodleactividades.ui.theme.AmbarPendienteFondo
import com.asir.moodleactividades.ui.theme.AmbarPendienteOscuro
import com.asir.moodleactividades.ui.theme.VerdeEntregada
import com.asir.moodleactividades.ui.theme.VerdeEntregadaFondo
import com.asir.moodleactividades.ui.theme.VerdeEntregadaOscuro
import com.asir.moodleactividades.ui.theme.fondoDeEstado

@Composable
fun BusScreen(
    viewModel: BusViewModel,
    documento: HorarioViewModel,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val estadoDocumento by documento.estado.collectAsStateWithLifecycle()
    var anadiendo by remember { mutableStateOf(false) }
    var viendoDocumento by remember { mutableStateOf(false) }

    val elegirArchivo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(documento::elegir) }

    val tiposAceptados = arrayOf("application/pdf", "image/*")

    // El papel de la parada, a pantalla completa: mirarlo es justo para lo que se adjunta.
    if (viendoDocumento && estadoDocumento.horario != null) {
        BackHandler { viendoDocumento = false }
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viendoDocumento = false }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver a los horarios")
                }
                Text(
                    text = estadoDocumento.horario?.nombre.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            VisorConZoom(
                estado = estadoDocumento,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            if (estadoDocumento.totalPaginas > 1) {
                ControlesDePagina(estadoDocumento, documento::irAPagina)
            }
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {

        if (estado.lineas.isEmpty() && !anadiendo && estadoDocumento.horario == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EstadoVacio(
                    icono = Icons.Default.DirectionsBus,
                    titulo = "Sin líneas guardadas",
                    detalle = "Apunta las horas de tu autobús y la app te dirá cuánto falta " +
                        "para el siguiente. Funciona sin conexión: los horarios los escribes tú."
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = { anadiendo = true }, shape = RoundedCornerShape(14.dp)) {
                            Text("Añadir una línea")
                        }
                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = { elegirArchivo.launch(tiposAceptados) }) {
                            Text("O adjunta el horario en papel")
                        }
                    }
                }
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            estado.siguiente?.let { siguiente ->
                item { TarjetaSiguiente(siguiente, estado.minutoAhora) }
            }

            items(estado.lineas, key = { it.linea.id }) { conSalida ->
                TarjetaLinea(
                    conSalida = conSalida,
                    minutoAhora = estado.minutoAhora,
                    alBorrar = { viewModel.borrar(conSalida.linea.id) }
                )
            }

            item {
                TarjetaDocumento(
                    nombreArchivo = estadoDocumento.horario?.nombre,
                    alElegir = { elegirArchivo.launch(tiposAceptados) },
                    alVer = { viendoDocumento = true },
                    alQuitar = documento::quitar
                )
            }

            item {
                if (anadiendo) {
                    FormularioLinea(
                        alGuardar = { nombre, horas, dias ->
                            viewModel.anadir(nombre, horas, dias)
                            anadiendo = false
                        },
                        alCancelar = { anadiendo = false }
                    )
                } else {
                    Button(
                        onClick = { anadiendo = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Añadir otra línea")
                    }
                }
            }
        }
    }
}

/** Lo que se quiere ver nada más abrir: cuánto falta para el próximo, en grande. */
@Composable
private fun TarjetaSiguiente(conSalida: LineaConSalida, minutoAhora: Int) {
    val proxima = conSalida.proxima ?: return
    val faltan = proxima.minutosQueFaltan(minutoAhora)

    // Menos de un cuarto de hora es cuando hay que salir corriendo.
    val apurado = proxima.diasDeEspera == 0 && faltan <= 15
    val color = if (apurado) AmbarPendiente else VerdeEntregada
    val fondo = if (apurado) {
        fondoDeEstado(AmbarPendienteFondo, AmbarPendienteOscuro)
    } else {
        fondoDeEstado(VerdeEntregadaFondo, VerdeEntregadaOscuro)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = fondo)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsBus,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    text = HorariosBus.formatearHora(proxima.minutoDelDia),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = conSalida.linea.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = cuantoFalta(faltan, proxima.diasDeEspera),
                    style = MaterialTheme.typography.titleSmall,
                    color = color
                )
            }
        }
    }
}

/**
 * «En 12 min» sirve para decidir si da tiempo; «en 3 h» ya no, y para mañana lo único que
 * importa es la hora, que va aparte.
 */
private fun cuantoFalta(minutos: Int, diasDeEspera: Int): String = when {
    diasDeEspera == 1 -> "Sale mañana"
    diasDeEspera > 1 -> "Sale dentro de $diasDeEspera días"
    minutos <= 0 -> "Sale ahora"
    minutos < 60 -> "Sale en $minutos min"
    minutos % 60 == 0 -> "Sale en ${minutos / 60} h"
    else -> "Sale en ${minutos / 60} h ${minutos % 60} min"
}

@Composable
private fun TarjetaLinea(
    conSalida: LineaConSalida,
    minutoAhora: Int,
    alBorrar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = conSalida.linea.nombre,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = HorariosBus.etiquetaDias(conSalida.linea.dias),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = alBorrar) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Borrar la línea",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (conSalida.salidasDeHoy.isEmpty()) {
                Text(
                    text = "Hoy no hay servicio en esta línea.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                conSalida.salidasDeHoy.forEach { salida ->
                    val pasada = salida <= minutoAhora
                    Text(
                        text = HorariosBus.formatearHora(salida),
                        style = MaterialTheme.typography.labelLarge,
                        // Las que ya han salido se apagan, pero no se ocultan: sirven para
                        // hacerse una idea de la frecuencia de la línea.
                        color = if (pasada) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier
                            .background(
                                if (pasada) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormularioLinea(
    alGuardar: (String, String, Set<Int>) -> Unit,
    alCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var horas by remember { mutableStateOf("") }
    var dias by remember { mutableStateOf(LineaBus.LABORABLES) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Nueva línea", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Trayecto") },
                placeholder = { Text("Pueblo → Instituto") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = horas,
                onValueChange = { horas = it },
                label = { Text("Horas de salida") },
                placeholder = { Text("07:15, 08:30, 14:00") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Sepáralas por comas. Vale «7:15», «07.15» o «0715».",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Días con servicio",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LineaBus.NOMBRES_DIAS.forEachIndexed { indice, etiqueta ->
                    val dia = indice + 1
                    FilterChip(
                        selected = dia in dias,
                        onClick = {
                            dias = if (dia in dias) dias - dia else dias + dia
                        },
                        label = { Text(etiqueta) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { alGuardar(nombre, horas, dias) },
                    enabled = horas.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar")
                }
                TextButton(onClick = alCancelar) { Text("Cancelar") }
            }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "Horario en papel",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = nombreArchivo ?: "Adjunta el PDF o una foto de la parada",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (nombreArchivo == null) {
                TextButton(onClick = alElegir) { Text("Adjuntar") }
            } else {
                TextButton(onClick = alVer) { Text("Ver") }
                IconButton(onClick = alQuitar) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Quitar el documento",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
