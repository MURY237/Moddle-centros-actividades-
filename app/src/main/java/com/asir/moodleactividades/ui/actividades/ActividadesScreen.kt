package com.asir.moodleactividades.ui.actividades

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asir.moodleactividades.BuildConfig
import com.asir.moodleactividades.domain.Actividad
import com.asir.moodleactividades.domain.EstadoActividad
import com.asir.moodleactividades.domain.FiltroEstado
import com.asir.moodleactividades.domain.RangoTiempo
import com.asir.moodleactividades.domain.TipoActividad
import com.asir.moodleactividades.ui.actualizacion.ActualizacionUiState
import com.asir.moodleactividades.ui.actualizacion.BannerActualizacion
import com.asir.moodleactividades.ui.componentes.AnilloProgreso
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.componentes.Etiqueta
import com.asir.moodleactividades.ui.componentes.FilaEstadisticas
import com.asir.moodleactividades.ui.componentes.SelectorAsignatura
import com.asir.moodleactividades.ui.formatearFecha
import com.asir.moodleactividades.ui.textoRelativo
import com.asir.moodleactividades.ui.theme.AmbarPendiente
import com.asir.moodleactividades.ui.theme.AmbarPendienteFondo
import com.asir.moodleactividades.ui.theme.AmbarPendienteOscuro
import com.asir.moodleactividades.ui.theme.DegradadoCabecera
import com.asir.moodleactividades.ui.theme.RojoNoEntregada
import com.asir.moodleactividades.ui.theme.RojoNoEntregadaFondo
import com.asir.moodleactividades.ui.theme.RojoNoEntregadaOscuro
import com.asir.moodleactividades.ui.theme.VerdeEntregada
import com.asir.moodleactividades.ui.theme.VerdeEntregadaFondo
import com.asir.moodleactividades.ui.theme.VerdeEntregadaOscuro
import com.asir.moodleactividades.ui.theme.fondoDeEstado

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActividadesScreen(
    viewModel: ActividadesViewModel,
    actualizacion: ActualizacionUiState,
    alInstalarActualizacion: () -> Unit,
    alDescartarActualizacion: () -> Unit,
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
            alAbrirArchivo = { adjunto, archivo ->
                abrirIntent(viewModel.intentAbrir(adjunto, archivo))
            },
            alCompartirArchivo = { adjunto, archivo ->
                abrirIntent(viewModel.intentCompartir(adjunto, archivo))
            },
            alAbrirEnMoodle = {
                actividad.url?.let { enlace ->
                    abrirIntent(Intent(Intent.ACTION_VIEW, enlace.toUri()))
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {

        Cabecera(
            estado = estado,
            alRefrescar = viewModel::refrescar,
            alCerrarSesion = viewModel::cerrarSesion
        )

        AnimatedVisibility(visible = actualizacion.visible, enter = fadeIn(), exit = fadeOut()) {
            BannerActualizacion(
                estado = actualizacion,
                alInstalar = alInstalarActualizacion,
                alDescartar = alDescartarActualizacion
            )
        }

        AnimatedVisibility(visible = estado.mostrandoDatosAntiguos, enter = fadeIn(), exit = fadeOut()) {
            AvisoSinConexion(estado, viewModel::refrescar)
        }

        Filtros(estado, viewModel)

        when {
            estado.cargando && estado.todas.isEmpty() -> Caja {
                CircularProgressIndicator()
            }

            estado.error != null && estado.todas.isEmpty() -> Caja {
                EstadoVacio(
                    icono = Icons.Default.CloudOff,
                    titulo = "No se pudieron cargar las actividades",
                    detalle = estado.error.orEmpty()
                ) {
                    Button(onClick = viewModel::refrescar, enabled = !estado.cargando) {
                        Text("Reintentar")
                    }
                }
            }

            estado.secciones.isEmpty() -> Caja {
                EstadoVacio(
                    icono = if (estado.todas.isEmpty()) Icons.Default.DoneAll else Icons.Default.SearchOff,
                    titulo = if (estado.todas.isEmpty()) "Nada pendiente" else "Sin resultados",
                    detalle = if (estado.todas.isEmpty()) {
                        "No hay ninguna actividad en tu Moodle ahora mismo."
                    } else {
                        "Ninguna actividad encaja con estos filtros. Prueba con «Todo»."
                    }
                )
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                estado.secciones.forEach { seccion ->
                    stickyHeader(key = seccion.grupo.name) {
                        CabeceraSeccion(seccion.grupo.etiqueta, seccion.actividades.size)
                    }
                    // El id de una tarea y el de un evento de calendario pueden coincidir,
                    // y dos claves iguales rompen la lista.
                    items(
                        seccion.actividades,
                        key = { "${seccion.grupo.name}-${it.tipo.name}-${it.id}" }
                    ) { actividad ->
                        TarjetaActividad(
                            actividad = actividad,
                            modifier = Modifier.animateItem()
                        ) {
                            viewModel.abrirDetalle(actividad)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Caja(contenido: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        content = { contenido() }
    )
}

@Composable
private fun Cabecera(
    estado: ActividadesUiState,
    alRefrescar: () -> Unit,
    alCerrarSesion: () -> Unit
) {
    val resumen = estado.resumen
    val total = resumen.pendientes + resumen.entregadas + resumen.noEntregadas
    val progreso = if (total == 0) 0f else resumen.entregadas.toFloat() / total
    val porcentaje by animateFloatAsState(
        targetValue = progreso * 100,
        animationSpec = tween(700),
        label = "porcentaje"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(DegradadoCabecera)
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Mis actividades",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(
                                    Color.White.copy(alpha = 0.22f),
                                    RoundedCornerShape(7.dp)
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    if (estado.nombreUsuario.isNotBlank()) {
                        Text(
                            text = estado.nombreUsuario,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = alRefrescar, enabled = !estado.cargando) {
                    Icon(Icons.Default.Refresh, "Actualizar", tint = Color.White)
                }
                IconButton(onClick = alCerrarSesion) {
                    Icon(Icons.Default.Logout, "Cerrar sesión", tint = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AnilloProgreso(
                    progreso = progreso,
                    etiquetaCentral = "${porcentaje.toInt()}%",
                    subEtiqueta = "entregado",
                    color = Color.White,
                    colorPista = Color.White.copy(alpha = 0.3f)
                )
                Spacer(Modifier.width(18.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when {
                            total == 0 -> "Sin actividades"
                            resumen.noEntregadas > 0 -> "Tienes ${resumen.noEntregadas} sin entregar"
                            resumen.pendientes > 0 -> "Te quedan ${resumen.pendientes} por entregar"
                            else -> "Todo al día"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "$total ${if (total == 1) "actividad" else "actividades"} en total",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            FilaEstadisticas(
                pendientes = resumen.pendientes,
                entregadas = resumen.entregadas,
                noEntregadas = resumen.noEntregadas,
                colorPendiente = Color.White,
                fondoPendiente = Color.White.copy(alpha = 0.16f),
                colorEntregada = Color.White,
                fondoEntregada = Color.White.copy(alpha = 0.16f),
                colorNoEntregada = Color.White,
                fondoNoEntregada = Color.White.copy(alpha = 0.16f)
            )
        }
    }
}

@Composable
private fun CabeceraSeccion(etiqueta: String, cantidad: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = cantidad.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun AvisoSinConexion(estado: ActividadesUiState, alReintentar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = fondoDeEstado(AmbarPendienteFondo, AmbarPendienteOscuro)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = AmbarPendiente,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Datos sin actualizar",
                    style = MaterialTheme.typography.titleSmall,
                    color = AmbarPendiente
                )
                Text(
                    text = "Son los de ${formatearFecha(estado.momentoDatos)}. ${estado.error.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmbarPendiente
                )
            }
            TextButton(onClick = alReintentar, enabled = !estado.cargando) {
                Text("Reintentar", color = AmbarPendiente)
            }
        }
    }
}

@Composable
private fun Filtros(estado: ActividadesUiState, viewModel: ActividadesViewModel) {
    Column(
        modifier = Modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (estado.asignaturas.isNotEmpty()) {
            SelectorAsignatura(
                asignaturas = estado.asignaturas,
                seleccionada = estado.asignatura,
                alElegir = viewModel::cambiarAsignatura,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FiltroEstado.entries.forEach { filtro ->
                FilterChip(
                    selected = estado.filtroEstado == filtro,
                    onClick = { viewModel.cambiarFiltro(filtro) },
                    label = { Text(filtro.etiqueta) },
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RangoTiempo.entries.forEach { rango ->
                FilterChip(
                    selected = estado.rango == rango,
                    onClick = { viewModel.cambiarRango(rango) },
                    label = { Text(rango.etiqueta) },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
    }
}

private fun iconoDe(tipo: TipoActividad) = when (tipo) {
    TipoActividad.TAREA -> Icons.AutoMirrored.Filled.Assignment
    TipoActividad.CUESTIONARIO -> Icons.Default.Quiz
    TipoActividad.FORO -> Icons.Default.Forum
    TipoActividad.OTRA -> Icons.Default.Event
}

@Composable
private fun TarjetaActividad(
    actividad: Actividad,
    modifier: Modifier = Modifier,
    alPulsar: () -> Unit
) {
    val color = when (actividad.estado) {
        EstadoActividad.ENTREGADA -> VerdeEntregada
        EstadoActividad.PENDIENTE -> AmbarPendiente
        EstadoActividad.NO_ENTREGADA -> RojoNoEntregada
    }
    val fondo = when (actividad.estado) {
        EstadoActividad.ENTREGADA -> fondoDeEstado(VerdeEntregadaFondo, VerdeEntregadaOscuro)
        EstadoActividad.PENDIENTE -> fondoDeEstado(AmbarPendienteFondo, AmbarPendienteOscuro)
        EstadoActividad.NO_ENTREGADA -> fondoDeEstado(RojoNoEntregadaFondo, RojoNoEntregadaOscuro)
    }

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = alPulsar),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(fondo, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconoDe(actividad.tipo),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = actividad.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (actividad.url != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = actividad.curso,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Etiqueta(actividad.estado.etiqueta, color, fondo)
                    if (actividad.adjuntos.isNotEmpty()) {
                        Etiqueta(
                            texto = "${actividad.adjuntos.size} archivo" +
                                if (actividad.adjuntos.size == 1) "" else "s",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fondo = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    actividad.nota?.let { nota ->
                        Etiqueta(
                            texto = "Nota $nota",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fondo = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                }
                Text(
                    text = buildString {
                        append(formatearFecha(actividad.fechaLimite))
                        val relativo = textoRelativo(actividad.fechaLimite)
                        if (relativo.isNotBlank()) append(" · ").append(relativo)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
