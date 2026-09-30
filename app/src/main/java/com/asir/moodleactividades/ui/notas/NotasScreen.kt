package com.asir.moodleactividades.ui.notas

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.domain.Calificacion
import com.asir.moodleactividades.domain.FiltroNotas
import com.asir.moodleactividades.domain.NotasDeCurso
import com.asir.moodleactividades.ui.componentes.Aviso
import com.asir.moodleactividades.ui.componentes.BarraProgreso
import com.asir.moodleactividades.ui.componentes.CabeceraPantalla
import com.asir.moodleactividades.ui.componentes.EntreFiltros
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.componentes.EtiquetaEstado
import com.asir.moodleactividades.ui.componentes.FranjaResumen
import com.asir.moodleactividades.ui.componentes.Metrica
import com.asir.moodleactividades.ui.componentes.SelectorAsignatura
import com.asir.moodleactividades.ui.componentes.Tarjeta
import com.asir.moodleactividades.ui.formatearFecha
import com.asir.moodleactividades.ui.theme.Tono
import com.asir.moodleactividades.ui.theme.colores

/** Lo que la pantalla de notas puede pedir; separado para poder pintarla sin ViewModel. */
data class AccionesNotas(
    val refrescar: () -> Unit = {},
    val cambiarFiltro: (FiltroNotas) -> Unit = {},
    val cambiarAsignatura: (String?) -> Unit = {},
    val abrirEnlace: (String) -> Unit = {}
)

@Composable
fun NotasScreen(
    viewModel: NotasViewModel,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    NotasContenido(
        estado = estado,
        acciones = AccionesNotas(
            refrescar = viewModel::refrescar,
            cambiarFiltro = viewModel::cambiarFiltro,
            cambiarAsignatura = viewModel::cambiarAsignatura,
            abrirEnlace = { enlace ->
                runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, enlace.toUri())) }
            }
        ),
        modifier = modifier
    )
}

@Composable
fun NotasContenido(
    estado: NotasUiState,
    acciones: AccionesNotas,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        CabeceraPantalla(
            titulo = "Notas",
            subtitulo = if (estado.totalEvaluables == 0) {
                "Calificaciones de tus asignaturas"
            } else {
                "${estado.totalCalificadas} de ${estado.totalEvaluables} calificadas"
            }
        ) {
            if (estado.cargando) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            } else {
                IconButton(onClick = acciones.refrescar) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Espacio.xl),
            verticalArrangement = Arrangement.spacedBy(Espacio.m)
        ) {
            if (estado.mostrandoDatosAntiguos) {
                item(key = "sin-conexion") {
                    Aviso(
                        titulo = "Sin actualizar",
                        texto = estado.error.orEmpty(),
                        tono = Tono.AVISO,
                        accion = "Reintentar",
                        alPulsarAccion = acciones.refrescar,
                        modifier = Modifier.padding(horizontal = Espacio.lateral)
                    )
                }
            }

            if (estado.todos.isNotEmpty()) {
                item(key = "resumen") { Resumen(estado) }
                item(key = "filtros") { Filtros(estado, acciones) }
            }

            when {
                estado.cargando && estado.cursos.isEmpty() -> item(key = "cargando") {
                    Box(Modifier.fillMaxWidth().padding(Espacio.xxl), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                    }
                }

                estado.error != null && estado.cursos.isEmpty() -> item(key = "error") {
                    EstadoVacio(
                        icono = Icons.Default.CloudOff,
                        titulo = "No se pudieron cargar las notas",
                        detalle = estado.error.orEmpty()
                    ) {
                        Button(onClick = acciones.refrescar, enabled = !estado.cargando) { Text("Reintentar") }
                    }
                }

                estado.cursos.isEmpty() -> item(key = "vacio") {
                    EstadoVacio(
                        icono = Icons.Default.Grade,
                        titulo = if (estado.todos.isEmpty()) "Todavía sin notas" else "Sin resultados",
                        detalle = if (estado.todos.isEmpty()) {
                            "Cuando tus asignaturas tengan actividades evaluables aparecerán aquí."
                        } else {
                            "Ninguna calificación encaja con estos filtros."
                        }
                    )
                }

                // Dos matrículas pueden resolverse al mismo nombre de curso: la posición entra
                // en la clave para que no se repita.
                else -> itemsIndexed(
                    estado.cursos,
                    key = { indice, curso -> "curso-$indice-${curso.curso}" }
                ) { _, curso ->
                    TarjetaCurso(
                        curso = curso,
                        alAbrir = acciones.abrirEnlace,
                        modifier = Modifier.padding(horizontal = Espacio.lateral)
                    )
                }
            }
        }
    }
}

@Composable
private fun Resumen(estado: NotasUiState) {
    // Se cuentan las de las asignaturas elegidas, sin el filtro de estado: el resumen es de
    // la asignatura, no de lo que quepa en pantalla.
    val calificaciones = estado.todos
        .filter { estado.asignatura == null || it.curso == estado.asignatura }
        .flatMap { it.calificaciones }
        .filter { it.calificada }
    val aprobadas = calificaciones.count { it.esAprobada() == true }
    val suspensas = calificaciones.count { it.esAprobada() == false }

    FranjaResumen(
        listOf(
            Metrica(calificaciones.size.toString(), "Con nota", Tono.INFO),
            Metrica(aprobadas.toString(), "Aprobadas", Tono.EXITO),
            Metrica(suspensas.toString(), "Suspensas", Tono.PELIGRO)
        ),
        modifier = Modifier.padding(horizontal = Espacio.lateral)
    )
}

@Composable
private fun Filtros(estado: NotasUiState, acciones: AccionesNotas) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        SelectorAsignatura(
            asignaturas = estado.asignaturas,
            seleccionada = estado.asignatura,
            alElegir = acciones.cambiarAsignatura,
            modifier = Modifier.padding(horizontal = Espacio.lateral, vertical = Espacio.xs)
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Espacio.lateral),
            horizontalArrangement = EntreFiltros
        ) {
            FiltroNotas.entries.forEach { filtro ->
                FilterChip(
                    selected = estado.filtro == filtro,
                    onClick = { acciones.cambiarFiltro(filtro) },
                    label = { Text(filtro.etiqueta) }
                )
            }
        }
    }
}

/** Una asignatura: su cabecera con el total y, debajo, cada actividad evaluable en una fila. */
@Composable
private fun TarjetaCurso(curso: NotasDeCurso, alAbrir: (String) -> Unit, modifier: Modifier = Modifier) {
    Tarjeta(modifier = modifier, relleno = PaddingValues(0.dp)) {
        Column(
            modifier = Modifier.padding(Espacio.l),
            verticalArrangement = Arrangement.spacedBy(Espacio.s)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = curso.curso,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                curso.total?.takeIf { it.calificada }?.let { total ->
                    EtiquetaEstado(
                        texto = "Total " + total.nota,
                        tono = tonoDe(total),
                        conPunto = false,
                        modifier = Modifier.padding(start = Espacio.s)
                    )
                }
            }
            if (curso.calificaciones.isEmpty()) {
                Text(
                    text = "Todavía sin actividades evaluables",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BarraProgreso(
                        progreso = curso.calificadas.toFloat() / curso.calificaciones.size,
                        tono = Tono.INFO,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${curso.calificadas} de ${curso.calificaciones.size} con nota",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Espacio.m)
                    )
                }
            }
        }
        curso.calificaciones.forEach { calificacion ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FilaCalificacion(calificacion) { calificacion.url?.let(alAbrir) }
        }
    }
}

@Composable
private fun FilaCalificacion(calificacion: Calificacion, alPulsar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = calificacion.url != null, role = Role.Button, onClick = alPulsar)
            .padding(horizontal = Espacio.l, vertical = Espacio.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = calificacion.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (calificacion.url != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = Espacio.xs).size(14.dp)
                    )
                }
            }
            Text(
                text = detalleDe(calificacion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        CasillaNota(calificacion, modifier = Modifier.padding(start = Espacio.m))
    }
}

@Composable
private fun CasillaNota(calificacion: Calificacion, modifier: Modifier = Modifier) {
    val colores = tonoDe(calificacion).colores()
    val texto = calificacion.nota.ifBlank { "—" }
    Box(
        modifier = modifier
            .widthIn(min = 52.dp)
            .background(colores.contenedor, MaterialTheme.shapes.small)
            .padding(horizontal = Espacio.s, vertical = Espacio.xs + 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            // Una nota sobre 100 o un «Apto» no caben con el cuerpo grande.
            style = if (texto.length > 5) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
            color = colores.contenido,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

private fun detalleDe(calificacion: Calificacion): String = buildString {
    append(if (calificacion.calificada) calificacion.tipo.etiqueta else "Sin calificar")
    if (calificacion.notaMaxima > 0) append(" · sobre ").append(formateaMaxima(calificacion.notaMaxima))
    if (calificacion.porcentaje.isNotBlank()) append(" · ").append(calificacion.porcentaje)
    calificacion.fecha?.let { append(" · ").append(formatearFecha(it)) }
}

private fun tonoDe(calificacion: Calificacion): Tono = when {
    !calificacion.calificada -> Tono.NEUTRO
    else -> when (calificacion.esAprobada()) {
        true -> Tono.EXITO
        false -> Tono.PELIGRO
        null -> Tono.INFO
    }
}

private fun formateaMaxima(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toInt().toString() else valor.toString()

/** Null cuando la nota no es numérica y no se puede decidir si está aprobada. */
internal fun Calificacion.esAprobada(): Boolean? {
    val valor = nota.replace(',', '.').filter { it.isDigit() || it == '.' || it == '-' }
        .toDoubleOrNull() ?: return null
    val maxima = notaMaxima.takeIf { it > 0 } ?: 10.0
    return valor >= maxima / 2
}
