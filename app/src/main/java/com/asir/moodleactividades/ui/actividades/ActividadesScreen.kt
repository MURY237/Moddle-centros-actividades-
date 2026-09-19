package com.asir.moodleactividades.ui.actividades

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asir.moodleactividades.domain.Actividad
import com.asir.moodleactividades.domain.EstadoActividad
import com.asir.moodleactividades.domain.FiltroEstado
import com.asir.moodleactividades.domain.RangoTiempo
import com.asir.moodleactividades.notificaciones.Recordatorios
import com.asir.moodleactividades.notificaciones.RecordatoriosWorker
import com.asir.moodleactividades.ui.actualizacion.ActualizacionViewModel
import com.asir.moodleactividades.ui.actualizacion.BannerActualizacion
import com.asir.moodleactividades.ui.formatearFecha
import com.asir.moodleactividades.ui.textoRelativo
import com.asir.moodleactividades.ui.theme.AmbarPendiente
import com.asir.moodleactividades.ui.theme.AmbarPendienteFondo
import com.asir.moodleactividades.ui.theme.RojoNoEntregada
import com.asir.moodleactividades.ui.theme.RojoNoEntregadaFondo
import com.asir.moodleactividades.ui.theme.VerdeEntregada
import com.asir.moodleactividades.ui.theme.VerdeEntregadaFondo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActividadesScreen(
    viewModel: ActividadesViewModel,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) RecordatoriosWorker.programar(contexto)
    }

    val actualizacionViewModel: ActualizacionViewModel = viewModel()
    val actualizacion by actualizacionViewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        actualizacionViewModel.comprobar()
        Recordatorios.crearCanal(contexto)
        if (Recordatorios.puedeNotificar(contexto)) {
            RecordatoriosWorker.programar(contexto)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis actividades", style = MaterialTheme.typography.titleMedium)
                        if (estado.nombreUsuario.isNotBlank()) {
                            Text(
                                text = estado.nombreUsuario,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refrescar, enabled = !estado.cargando) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                    IconButton(onClick = viewModel::cerrarSesion) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { relleno ->
        Column(modifier = Modifier.padding(relleno).fillMaxSize()) {

            if (actualizacion.visible) {
                BannerActualizacion(
                    estado = actualizacion,
                    alInstalar = { actualizacionViewModel.instalar(contexto) },
                    alDescartar = actualizacionViewModel::descartar
                )
            }

            if (estado.mostrandoDatosAntiguos) {
                AvisoSinConexion(estado, viewModel::refrescar)
            }

            FilaResumen(estado)

            if (estado.asignaturas.size > 1) {
                SelectorAsignatura(estado, viewModel::cambiarAsignatura)
            }

            FilaFiltros(estado, viewModel)

            when {
                estado.cargando && estado.todas.isEmpty() -> Caja {
                    CircularProgressIndicator()
                }

                estado.error != null && estado.todas.isEmpty() -> Caja {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = estado.error.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = viewModel::refrescar, enabled = !estado.cargando) {
                            Text("Reintentar")
                        }
                    }
                }

                estado.secciones.isEmpty() -> Caja {
                    Text(
                        text = "No hay actividades con estos filtros.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    estado.secciones.forEach { seccion ->
                        stickyHeader(key = seccion.grupo.name) {
                            Text(
                                text = "${seccion.grupo.etiqueta} (${seccion.actividades.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = 8.dp)
                            )
                        }
                        items(seccion.actividades, key = { "${seccion.grupo.name}-${it.id}" }) { actividad ->
                            TarjetaActividad(actividad) {
                                actividad.url?.let { enlace ->
                                    runCatching {
                                        contexto.startActivity(Intent(Intent.ACTION_VIEW, enlace.toUri()))
                                    }
                                }
                            }
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
private fun AvisoSinConexion(estado: ActividadesUiState, alReintentar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = AmbarPendienteFondo)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
                    text = "Sin conexión con el centro",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AmbarPendiente
                )
                Text(
                    text = "Estos datos son de ${formatearFecha(estado.momentoDatos)} y pueden " +
                        "estar desactualizados.",
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
private fun FilaResumen(estado: ActividadesUiState) {
    val resumen = estado.resumen
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Contador("Pendientes", resumen.pendientes, AmbarPendiente, AmbarPendienteFondo, Modifier.weight(1f))
        Contador("Entregadas", resumen.entregadas, VerdeEntregada, VerdeEntregadaFondo, Modifier.weight(1f))
        Contador("No entregadas", resumen.noEntregadas, RojoNoEntregada, RojoNoEntregadaFondo, Modifier.weight(1f))
    }
}

@Composable
private fun Contador(
    etiqueta: String,
    valor: Int,
    color: Color,
    fondo: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(fondo, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = valor.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SelectorAsignatura(estado: ActividadesUiState, alElegir: (String?) -> Unit) {
    var desplegado by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        TextButton(onClick = { desplegado = true }) {
            Text(
                text = estado.asignatura ?: "Todas las asignaturas",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = desplegado, onDismissRequest = { desplegado = false }) {
            DropdownMenuItem(
                text = { Text("Todas las asignaturas") },
                onClick = {
                    alElegir(null)
                    desplegado = false
                }
            )
            estado.asignaturas.forEach { asignatura ->
                DropdownMenuItem(
                    text = { Text(asignatura) },
                    onClick = {
                        alElegir(asignatura)
                        desplegado = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FilaFiltros(estado: ActividadesUiState, viewModel: ActividadesViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    label = { Text(filtro.etiqueta) }
                )
            }
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RangoTiempo.entries.forEach { rango ->
                FilterChip(
                    selected = estado.rango == rango,
                    onClick = { viewModel.cambiarRango(rango) },
                    label = { Text(rango.etiqueta) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun TarjetaActividad(actividad: Actividad, alPulsar: () -> Unit) {
    val (color, fondo) = when (actividad.estado) {
        EstadoActividad.ENTREGADA -> VerdeEntregada to VerdeEntregadaFondo
        EstadoActividad.PENDIENTE -> AmbarPendiente to AmbarPendienteFondo
        EstadoActividad.NO_ENTREGADA -> RojoNoEntregada to RojoNoEntregadaFondo
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = alPulsar),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 56.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
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
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = actividad.estado.etiqueta,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(fondo, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    actividad.nota?.let { nota ->
                        Text(
                            text = "Nota: $nota",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = actividad.tipo.etiqueta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = buildString {
                        append(formatearFecha(actividad.fechaLimite))
                        val relativo = textoRelativo(actividad.fechaLimite)
                        if (relativo.isNotBlank()) append(" · ").append(relativo)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
