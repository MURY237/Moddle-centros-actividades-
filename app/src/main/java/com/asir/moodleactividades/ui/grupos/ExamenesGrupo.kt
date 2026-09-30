package com.asir.moodleactividades.ui.grupos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.asir.moodleactividades.domain.CalendarioExamenes
import com.asir.moodleactividades.domain.Chat
import com.asir.moodleactividades.domain.Examen
import com.asir.moodleactividades.domain.LimitesGrupo
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.EtiquetaEstado
import com.asir.moodleactividades.ui.componentes.Tarjeta
import com.asir.moodleactividades.ui.componentes.TituloSeccion
import com.asir.moodleactividades.ui.theme.Tono
import com.asir.moodleactividades.ui.theme.colores
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset

@Composable
internal fun PanelExamenes(
    estado: GruposUiState,
    acciones: AccionesGrupos,
    hoy: LocalDate = LocalDate.now()
) {
    var mes by rememberSaveable { mutableStateOf(YearMonth.from(hoy)) }
    var elegido by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var anadiendo by rememberSaveable { mutableStateOf(false) }
    var borrando by remember { mutableStateOf<Examen?>(null) }
    val porDia = remember(estado.examenes) { CalendarioExamenes.porDia(estado.examenes) }

    val dia = elegido
    val lista = if (dia != null) porDia[dia].orEmpty() else CalendarioExamenes.proximos(estado.examenes, hoy)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = Espacio.lateral, end = Espacio.lateral, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.s)
        ) {
            item(key = "calendario") {
                CalendarioMes(
                    mes = mes,
                    examenesPorDia = porDia,
                    hoy = hoy,
                    seleccionado = elegido,
                    alCambiarMes = { mes = it },
                    alElegirDia = { elegido = it }
                )
            }
            item(key = "titulo") {
                TituloSeccion(
                    texto = if (dia != null) dia.format(DIA_LARGO).conMayuscula() else "Próximos exámenes",
                    extra = if (estado.examenesCargados) lista.size.toString() else null
                )
            }
            when {
                !estado.examenesCargados -> item(key = "cargando") {
                    Box(Modifier.fillMaxWidth().padding(Espacio.xl), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                    }
                }
                lista.isEmpty() -> item(key = "vacio") {
                    Text(
                        text = if (dia != null) "Ningún examen ese día." else "No hay exámenes apuntados por delante.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> items(lista, key = { "examen-" + it.id }) { examen ->
                    TarjetaExamen(
                        examen = examen,
                        hoy = hoy,
                        autor = Chat.apodoDe(examen.autor, estado.miembros),
                        puedeBorrar = estado.puedeBorrar(examen),
                        alBorrar = { borrando = examen }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { anadiendo = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Añadir examen") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(Espacio.l)
        )
    }

    if (anadiendo) {
        DialogoExamen(
            fechaInicial = elegido ?: hoy,
            trabajando = estado.trabajando,
            alCancelar = { anadiendo = false },
            alGuardar = { asignatura, fecha, hora, notas ->
                acciones.anadirExamen(asignatura, fecha, hora, notas) {
                    anadiendo = false
                    // Se lleva al alumno al mes del examen, para que lo vea puesto.
                    mes = YearMonth.from(fecha)
                }
            }
        )
    }

    borrando?.let { examen ->
        AlertDialog(
            onDismissRequest = { borrando = null },
            title = { Text("¿Borrar el examen?") },
            text = { Text("Se borra «${examen.asignatura}» del calendario de todo el grupo.") },
            confirmButton = {
                TextButton(onClick = {
                    borrando = null
                    acciones.borrarExamen(examen)
                }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { borrando = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaExamen(
    examen: Examen,
    hoy: LocalDate,
    autor: String,
    puedeBorrar: Boolean,
    alBorrar: () -> Unit
) {
    val faltan = CalendarioExamenes.diasHasta(examen, hoy)
    val tono = when {
        faltan == null || faltan < 0 -> Tono.NEUTRO
        faltan == 0L -> Tono.PELIGRO
        faltan <= 3 -> Tono.AVISO
        else -> Tono.INFO
    }
    val cuenta = when {
        faltan == null || faltan < 0 -> null
        faltan == 0L -> "Hoy"
        faltan == 1L -> "Mañana"
        else -> "En $faltan días"
    }
    val colores = tono.colores()

    Tarjeta(relleno = PaddingValues(Espacio.m)) {
        Row(verticalAlignment = Alignment.Top) {
            // La fecha como una hoja de calendario: es lo primero que se busca en un examen.
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .background(colores.contenedor, MaterialTheme.shapes.medium)
                    .padding(vertical = Espacio.s),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = examen.dia?.format(MES_CORTO)?.uppercase(ESPANOL)?.trimEnd('.').orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colores.contenido
                )
                Text(
                    text = examen.dia?.dayOfMonth?.toString() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = colores.contenido
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = Espacio.m)) {
                Text(examen.asignatura, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = buildString {
                        examen.dia?.let { append(it.format(DIA_CORTO).conMayuscula()) }
                        examen.horaLocal?.let { append(" · ").append(it.format(HORA)) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (examen.notas.isNotBlank()) {
                    Text(
                        text = examen.notas,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = Espacio.xs)
                    )
                }
                Row(
                    modifier = Modifier.padding(top = Espacio.s),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    cuenta?.let {
                        EtiquetaEstado(it, tono)
                        Spacer(Modifier.width(Espacio.s))
                    }
                    Text(
                        text = "Lo apuntó $autor",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (puedeBorrar) {
                IconButton(onClick = alBorrar) {
                    Icon(Icons.Default.Delete, "Borrar examen", tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoExamen(
    fechaInicial: LocalDate,
    trabajando: Boolean,
    alCancelar: () -> Unit,
    alGuardar: (String, LocalDate, LocalTime?, String) -> Unit
) {
    var asignatura by rememberSaveable { mutableStateOf("") }
    // Guardables: antes se perdían al girar el móvil con el diálogo abierto.
    var fecha by rememberSaveable { mutableStateOf(fechaInicial) }
    var hora by rememberSaveable { mutableStateOf<LocalTime?>(null) }
    var notas by rememberSaveable { mutableStateOf("") }
    var eligiendoFecha by rememberSaveable { mutableStateOf(false) }
    var eligiendoHora by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = alCancelar,
        icon = { Icon(Icons.Default.Event, contentDescription = null) },
        title = { Text("Nuevo examen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                OutlinedTextField(
                    value = asignatura,
                    onValueChange = { if (it.length <= LimitesGrupo.ASIGNATURA) asignatura = it },
                    label = { Text("Asignatura") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { eligiendoFecha = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Espacio.s))
                    Text(fecha.format(DIA_LARGO).conMayuscula())
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { eligiendoHora = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(Espacio.s))
                        Text(hora?.format(HORA) ?: "Sin hora")
                    }
                    if (hora != null) {
                        TextButton(onClick = { hora = null }) { Text("Quitar") }
                    }
                }
                OutlinedTextField(
                    value = notas,
                    onValueChange = { if (it.length <= LimitesGrupo.NOTAS) notas = it },
                    label = { Text("Notas (temas, aula…)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { alGuardar(asignatura, fecha, hora, notas) },
                enabled = LimitesGrupo.asignaturaValida(asignatura) && !trabajando
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = alCancelar) { Text("Cancelar") } }
    )

    if (eligiendoFecha) {
        // El selector trabaja en milisegundos UTC a medianoche: convertir con la zona del
        // móvil movería el día en cuanto la zona no fuese la de Greenwich.
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { eligiendoFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let {
                        fecha = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    eligiendoFecha = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { eligiendoFecha = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estadoFecha)
        }
    }

    if (eligiendoHora) {
        val estadoHora = rememberTimePickerState(
            initialHour = hora?.hour ?: 9,
            initialMinute = hora?.minute ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { eligiendoHora = false },
            title = { Text("Hora del examen") },
            text = { TimeInput(state = estadoHora) },
            confirmButton = {
                TextButton(onClick = {
                    hora = LocalTime.of(estadoHora.hour, estadoHora.minute)
                    eligiendoHora = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { eligiendoHora = false }) { Text("Cancelar") } }
        )
    }
}
