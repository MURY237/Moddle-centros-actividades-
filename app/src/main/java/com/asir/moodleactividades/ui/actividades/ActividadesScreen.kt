package com.asir.moodleactividades.ui.actividades

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.domain.Actividad
import com.asir.moodleactividades.domain.EstadoActividad
import com.asir.moodleactividades.domain.FiltroEstado
import com.asir.moodleactividades.domain.RangoTiempo
import com.asir.moodleactividades.domain.TipoActividad
import com.asir.moodleactividades.ui.actualizacion.ActualizacionUiState
import com.asir.moodleactividades.ui.actualizacion.BannerActualizacion
import com.asir.moodleactividades.ui.componentes.Aviso
import com.asir.moodleactividades.ui.componentes.BarraProgreso
import com.asir.moodleactividades.ui.componentes.CabeceraPantalla
import com.asir.moodleactividades.ui.componentes.EntreFiltros
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.componentes.EtiquetaEstado
import com.asir.moodleactividades.ui.componentes.FranjaResumen
import com.asir.moodleactividades.ui.componentes.IconoTonal
import com.asir.moodleactividades.ui.componentes.MenuMas
import com.asir.moodleactividades.ui.componentes.Metrica
import com.asir.moodleactividades.ui.componentes.OpcionMenu
import com.asir.moodleactividades.ui.componentes.SelectorAsignatura
import com.asir.moodleactividades.ui.componentes.Tarjeta
import com.asir.moodleactividades.ui.componentes.TituloSeccion
import com.asir.moodleactividades.ui.componentes.tono
import com.asir.moodleactividades.ui.formatearFecha
import com.asir.moodleactividades.ui.haceCuanto
import com.asir.moodleactividades.ui.textoRelativo
import com.asir.moodleactividades.ui.theme.Tono
import com.asir.moodleactividades.ui.theme.colores

/** Lo que se puede hacer desde la pantalla. Todo con valor por defecto, para las capturas. */
data class AccionesTareas(
    val refrescar: () -> Unit = {},
    val cerrarSesion: () -> Unit = {},
    val abrirNetacad: () -> Unit = {},
    val cambiarFiltro: (FiltroEstado) -> Unit = {},
    val cambiarRango: (RangoTiempo) -> Unit = {},
    val cambiarAsignatura: (String?) -> Unit = {},
    val abrirDetalle: (Actividad) -> Unit = {},
    val instalarActualizacion: () -> Unit = {},
    val descartarActualizacion: () -> Unit = {}
)

@Composable
fun ActividadesScreen(
    viewModel: ActividadesViewModel,
    actualizacion: ActualizacionUiState,
    alInstalarActualizacion: () -> Unit,
    alDescartarActualizacion: () -> Unit,
    alAbrirNetacad: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    val abrirIntent: (Intent) -> Unit = { intento ->
        // Puede no haber ningún visor instalado para ese tipo de archivo: sin capturarlo,
        // tocar «Abrir» tumbaría la aplicación.
        if (runCatching { contexto.startActivity(intento) }.isFailure) {
            Toast.makeText(
                contexto,
                "No hay ninguna aplicación que pueda abrir este archivo.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    estado.detalle?.let { actividad ->
        HojaDetalleActividad(
            actividad = actividad,
            descargas = estado.descargas,
            alCerrar = viewModel::cerrarDetalle,
            alDescargar = viewModel::descargar,
            alAbrirArchivo = { adjunto, archivo -> abrirIntent(viewModel.intentAbrir(adjunto, archivo)) },
            alCompartirArchivo = { adjunto, archivo -> abrirIntent(viewModel.intentCompartir(adjunto, archivo)) },
            alAbrirEnMoodle = {
                actividad.url?.let { enlace -> abrirIntent(Intent(Intent.ACTION_VIEW, enlace.toUri())) }
            }
        )
    }

    TareasContenido(
        estado = estado,
        actualizacion = actualizacion,
        acciones = AccionesTareas(
            refrescar = viewModel::refrescar,
            cerrarSesion = alCerrarSesion,
            abrirNetacad = alAbrirNetacad,
            cambiarFiltro = viewModel::cambiarFiltro,
            cambiarRango = viewModel::cambiarRango,
            cambiarAsignatura = viewModel::cambiarAsignatura,
            abrirDetalle = viewModel::abrirDetalle,
            instalarActualizacion = alInstalarActualizacion,
            descartarActualizacion = alDescartarActualizacion
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TareasContenido(
    estado: ActividadesUiState,
    actualizacion: ActualizacionUiState,
    acciones: AccionesTareas,
    modifier: Modifier = Modifier
) {
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        CabeceraPantalla(
            titulo = "Tareas",
            subtitulo = subtituloDe(estado)
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
            MenuMas(
                listOf(
                    OpcionMenu("Trabajos de Cisco NetAcad", Icons.Default.School, alPulsar = acciones.abrirNetacad),
                    OpcionMenu("Cerrar sesión", Icons.AutoMirrored.Filled.Logout, peligrosa = true) {
                        confirmarSalida = true
                    }
                )
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Espacio.xl),
            verticalArrangement = Arrangement.spacedBy(Espacio.s)
        ) {
            if (actualizacion.visible) {
                item(key = "actualizacion") {
                    BannerActualizacion(
                        estado = actualizacion,
                        alInstalar = acciones.instalarActualizacion,
                        alDescartar = acciones.descartarActualizacion,
                        modifier = Modifier.padding(horizontal = Espacio.lateral)
                    )
                }
            }

            if (estado.mostrandoDatosAntiguos) {
                item(key = "sin-conexion") {
                    Aviso(
                        titulo = "Sin actualizar",
                        texto = estado.error.orEmpty() +
                            (estado.momentoDatos?.let { " Datos de " + haceCuanto(it).lowercase() + "." } ?: ""),
                        tono = Tono.AVISO,
                        accion = "Reintentar",
                        alPulsarAccion = acciones.refrescar,
                        modifier = Modifier.padding(horizontal = Espacio.lateral)
                    )
                }
            }

            if (estado.todas.isNotEmpty()) {
                item(key = "resumen") { Resumen(estado) }
            }

            item(key = "netacad") { AccesoNetacad(acciones.abrirNetacad) }

            item(key = "filtros") { Filtros(estado, acciones) }

            when {
                estado.cargando && estado.todas.isEmpty() -> item(key = "cargando") {
                    Box(Modifier.fillMaxWidth().padding(Espacio.xxl), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                    }
                }

                estado.error != null && estado.todas.isEmpty() -> item(key = "error") {
                    EstadoVacio(
                        icono = Icons.Default.CloudOff,
                        titulo = "No se pudieron cargar las actividades",
                        detalle = estado.error.orEmpty()
                    ) {
                        Button(onClick = acciones.refrescar, enabled = !estado.cargando) { Text("Reintentar") }
                    }
                }

                estado.secciones.isEmpty() -> item(key = "vacio") { SinResultados(estado) }

                else -> estado.secciones.forEach { seccion ->
                    stickyHeader(key = "seccion-" + seccion.grupo.name) {
                        TituloSeccion(
                            texto = seccion.grupo.etiqueta,
                            extra = seccion.actividades.size.toString(),
                            modifier = Modifier.padding(horizontal = Espacio.lateral)
                        )
                    }
                    // El id de una tarea y el de un evento de calendario pueden coincidir,
                    // y dos claves iguales rompen la lista.
                    items(
                        seccion.actividades,
                        key = { "${seccion.grupo.name}-${it.tipo.name}-${it.id}" }
                    ) { actividad ->
                        TarjetaActividad(
                            actividad = actividad,
                            modifier = Modifier
                                .padding(horizontal = Espacio.lateral)
                                .animateItem()
                        ) { acciones.abrirDetalle(actividad) }
                    }
                }
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            title = { Text("¿Cerrar sesión?") },
            text = {
                Text(
                    "Se borran de este móvil tus tareas, notas, adjuntos descargados y el " +
                        "historial de avisos. Séneca, NetAcad y los grupos siguen conectados: se " +
                        "desconectan desde su pantalla."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    acciones.cerrarSesion()
                }) { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } }
        )
    }
}

private fun subtituloDe(estado: ActividadesUiState): String = when {
    estado.nombreUsuario.isNotBlank() && estado.nombreSitio.isNotBlank() ->
        estado.nombreUsuario + " · " + estado.nombreSitio
    estado.nombreUsuario.isNotBlank() -> estado.nombreUsuario
    else -> estado.nombreSitio
}

@Composable
private fun Resumen(estado: ActividadesUiState) {
    val resumen = estado.resumen
    val total = resumen.pendientes + resumen.entregadas + resumen.noEntregadas
    val progreso = if (total == 0) 0f else resumen.entregadas.toFloat() / total

    Column(
        modifier = Modifier.padding(horizontal = Espacio.lateral),
        verticalArrangement = Arrangement.spacedBy(Espacio.s)
    ) {
        FranjaResumen(
            listOf(
                Metrica(resumen.pendientes.toString(), "Pendientes", Tono.AVISO),
                Metrica(resumen.entregadas.toString(), "Entregadas", Tono.EXITO),
                Metrica(resumen.noEntregadas.toString(), "Sin entregar", Tono.PELIGRO)
            )
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            BarraProgreso(progreso, modifier = Modifier.weight(1f), tono = Tono.EXITO)
            Text(
                text = "${(progreso * 100).toInt()} % entregado",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Espacio.m)
            )
        }
    }
}

/** Los trabajos de Cisco son la otra mitad de lo que hay por entregar: su sitio es aquí. */
@Composable
private fun AccesoNetacad(alPulsar: () -> Unit) {
    Tarjeta(
        alPulsar = alPulsar,
        relleno = PaddingValues(horizontal = Espacio.l, vertical = Espacio.m),
        modifier = Modifier.padding(horizontal = Espacio.lateral)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoTonal(Icons.Default.School, Tono.INFO, tamano = 36.dp)
            Column(modifier = Modifier.weight(1f).padding(horizontal = Espacio.m)) {
                Text("Trabajos de Cisco NetAcad", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Exámenes y prácticas de tus cursos de Cisco",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Filtros(estado: ActividadesUiState, acciones: AccionesTareas) {
    Column(
        modifier = Modifier.padding(top = Espacio.xs),
        verticalArrangement = Arrangement.spacedBy(Espacio.xs)
    ) {
        if (estado.asignaturas.isNotEmpty()) {
            SelectorAsignatura(
                asignaturas = estado.asignaturas,
                seleccionada = estado.asignatura,
                alElegir = acciones.cambiarAsignatura,
                modifier = Modifier.padding(horizontal = Espacio.lateral, vertical = Espacio.xs)
            )
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Espacio.lateral),
            horizontalArrangement = EntreFiltros
        ) {
            FiltroEstado.entries.forEach { filtro ->
                FilterChip(
                    selected = estado.filtroEstado == filtro,
                    onClick = { acciones.cambiarFiltro(filtro) },
                    label = { Text(filtro.etiqueta) }
                )
            }
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Espacio.lateral),
            horizontalArrangement = EntreFiltros
        ) {
            RangoTiempo.entries.forEach { rango ->
                FilterChip(
                    selected = estado.rango == rango,
                    onClick = { acciones.cambiarRango(rango) },
                    label = { Text(rango.etiqueta) }
                )
            }
        }
    }
}

@Composable
private fun SinResultados(estado: ActividadesUiState) {
    // Una asignatura recién creada aparece en el filtro sin tener nada dentro: decir
    // «ninguna actividad encaja» ahí sería engañoso.
    val asignaturaVacia = estado.asignatura != null && estado.todas.none { it.curso == estado.asignatura }
    EstadoVacio(
        icono = if (estado.todas.isEmpty() && !asignaturaVacia) Icons.Default.DoneAll else Icons.Default.SearchOff,
        titulo = when {
            asignaturaVacia -> "Asignatura sin actividades"
            estado.todas.isEmpty() -> "Nada pendiente"
            else -> "Sin resultados"
        },
        detalle = when {
            asignaturaVacia -> "«${estado.asignatura}» todavía no tiene ninguna actividad publicada."
            estado.todas.isEmpty() -> "No hay ninguna actividad en tu Moodle ahora mismo."
            else -> "Ninguna actividad encaja con estos filtros. Prueba con «Todo»."
        }
    )
}

fun iconoDe(tipo: TipoActividad) = when (tipo) {
    TipoActividad.TAREA -> Icons.AutoMirrored.Filled.Assignment
    TipoActividad.CUESTIONARIO -> Icons.Default.Quiz
    TipoActividad.FORO -> Icons.Default.Forum
    TipoActividad.OTRA -> Icons.Default.Event
}

@Composable
fun TarjetaActividad(
    actividad: Actividad,
    modifier: Modifier = Modifier,
    alPulsar: () -> Unit
) {
    val tono = actividad.estado.tono()
    val relativo = textoRelativo(actividad.fechaLimite)
    val urgente = actividad.estado != EstadoActividad.ENTREGADA && relativo.startsWith("Vence")

    Tarjeta(modifier = modifier, alPulsar = alPulsar, relleno = PaddingValues(Espacio.m)) {
        Row {
            IconoTonal(iconoDe(actividad.tipo), tono)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Espacio.m)
            ) {
                Text(
                    text = actividad.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = actividad.curso,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = Espacio.s),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.xs + 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EtiquetaEstado(actividad.estado.etiqueta, tono)
                    actividad.nota?.let { nota -> EtiquetaEstado("Nota $nota", Tono.INFO, conPunto = false) }
                    if (actividad.adjuntos.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                actividad.adjuntos.size.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    text = buildString {
                        append(formatearFecha(actividad.fechaLimite))
                        if (relativo.isNotBlank() && actividad.estado != EstadoActividad.ENTREGADA) {
                            append(" · ").append(relativo)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (urgente || actividad.estado == EstadoActividad.NO_ENTREGADA) {
                        tono.colores().contenido
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = Espacio.s)
                )
            }
        }
    }
}
