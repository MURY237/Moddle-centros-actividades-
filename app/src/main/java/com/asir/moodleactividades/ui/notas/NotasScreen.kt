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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.asir.moodleactividades.domain.Aprobado
import com.asir.moodleactividades.domain.Calificacion
import com.asir.moodleactividades.domain.FiltroNotas
import com.asir.moodleactividades.domain.NotasDeCurso
import com.asir.moodleactividades.domain.OrigenVeredicto
import com.asir.moodleactividades.domain.Veredicto
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
    val abrirDetalle: (Calificacion) -> Unit = {}
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
            abrirDetalle = viewModel::abrirDetalle
        ),
        modifier = modifier
    )

    estado.detalle?.let { calificacion ->
        HojaDetalleNota(
            calificacion = calificacion,
            ajuste = estado.ajustes[calificacion.clave],
            veredicto = estado.veredicto(calificacion),
            alAjustar = { viewModel.ajustar(calificacion, it) },
            alCerrar = viewModel::cerrarDetalle,
            alAbrirEnMoodle = {
                calificacion.url?.let { enlace ->
                    runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, enlace.toUri())) }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

        // Tirar de la lista hacia abajo recarga, como en cualquier app.
        PullToRefreshBox(
            isRefreshing = estado.cargando,
            onRefresh = acciones.refrescar,
            modifier = Modifier.fillMaxSize()
        ) {
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
                            veredicto = estado::veredicto,
                            alAbrir = acciones.abrirDetalle,
                            modifier = Modifier.padding(horizontal = Espacio.lateral)
                        )
                    }
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
    val veredictos = calificaciones.map { estado.veredicto(it).aprobada }
    val aprobadas = veredictos.count { it == true }
    val suspensas = veredictos.count { it == false }

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
private fun TarjetaCurso(
    curso: NotasDeCurso,
    veredicto: (Calificacion) -> Veredicto,
    alAbrir: (Calificacion) -> Unit,
    modifier: Modifier = Modifier
) {
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
                        tono = tonoDe(total, veredicto(total)),
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
            FilaCalificacion(calificacion, veredicto(calificacion)) { alAbrir(calificacion) }
        }
    }
}

/** Tocarla abre su ficha: ahí se ve sobre cuánto es y se puede corregir el resultado. */
@Composable
private fun FilaCalificacion(calificacion: Calificacion, veredicto: Veredicto, alPulsar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = alPulsar)
            .padding(horizontal = Espacio.l, vertical = Espacio.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = calificacion.nombre,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detalleDe(calificacion, veredicto),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        CasillaNota(calificacion, veredicto, modifier = Modifier.padding(start = Espacio.m))
    }
}

@Composable
internal fun CasillaNota(calificacion: Calificacion, veredicto: Veredicto, modifier: Modifier = Modifier) {
    val colores = tonoDe(calificacion, veredicto).colores()
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

private fun detalleDe(calificacion: Calificacion, veredicto: Veredicto): String = buildString {
    append(if (calificacion.calificada) calificacion.tipo.etiqueta else "Sin calificar")
    veredicto.notaMaxima?.let { maxima ->
        append(" · sobre ").append(Aprobado.cifra(maxima))
        // Que se vea cuándo el máximo es una suposición: es justo lo que hay que corregir.
        when (veredicto.origen) {
            OrigenVeredicto.SUPUESTO -> append(" (supuesto)")
            OrigenVeredicto.MAXIMA_PROPIA -> append(" (tuyo)")
            else -> Unit
        }
    }
    if (veredicto.origen == OrigenVeredicto.MANUAL) append(" · marcada a mano")
    if (calificacion.porcentaje.isNotBlank()) append(" · ").append(calificacion.porcentaje)
    calificacion.fecha?.let { append(" · ").append(formatearFecha(it)) }
}

internal fun tonoDe(calificacion: Calificacion, veredicto: Veredicto): Tono = when {
    !calificacion.calificada -> Tono.NEUTRO
    else -> when (veredicto.aprobada) {
        true -> Tono.EXITO
        false -> Tono.PELIGRO
        null -> Tono.INFO
    }
}
