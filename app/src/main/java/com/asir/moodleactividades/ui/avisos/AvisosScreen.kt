package com.asir.moodleactividades.ui.avisos

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.data.Aviso
import com.asir.moodleactividades.data.TipoAviso
import com.asir.moodleactividades.ui.componentes.CabeceraPantalla
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.componentes.EtiquetaEstado
import com.asir.moodleactividades.ui.componentes.IconoTonal
import com.asir.moodleactividades.ui.componentes.Tarjeta
import com.asir.moodleactividades.ui.componentes.TituloSeccion
import com.asir.moodleactividades.ui.haceCuanto
import com.asir.moodleactividades.ui.theme.Tono

data class AccionesAvisos(
    val vaciar: () -> Unit = {},
    val abrirAjustes: () -> Unit = {},
    val abrirEnlace: (String) -> Unit = {}
)

@Composable
fun AvisosScreen(
    viewModel: AvisosViewModel,
    alAbrirAjustes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    AvisosContenido(
        estado = estado,
        acciones = AccionesAvisos(
            vaciar = viewModel::vaciar,
            abrirAjustes = alAbrirAjustes,
            abrirEnlace = { enlace ->
                runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, enlace.toUri())) }
            }
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AvisosContenido(
    estado: AvisosUiState,
    acciones: AccionesAvisos,
    modifier: Modifier = Modifier,
    ahora: Long = System.currentTimeMillis() / 1000
) {
    var confirmarVaciado by rememberSaveable { mutableStateOf(false) }
    val grupos = remember(estado.avisos, ahora) { agruparPorDia(estado.avisos, ahora) }

    Column(modifier = modifier.fillMaxSize()) {
        CabeceraPantalla(
            titulo = "Avisos",
            subtitulo = when (val cantidad = estado.avisos.size) {
                0 -> "Ninguna notificación guardada"
                1 -> "1 notificación guardada"
                else -> "$cantidad notificaciones guardadas"
            }
        ) {
            if (estado.avisos.isNotEmpty()) {
                IconButton(onClick = { confirmarVaciado = true }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Vaciar el historial")
                }
            }
            // Los ajustes de los avisos viven junto a los avisos: así la barra de abajo
            // tiene sitio para Grupos sin pasar de seis pestañas.
            IconButton(onClick = acciones.abrirAjustes) {
                Icon(Icons.Default.Settings, contentDescription = "Ajustes")
            }
        }

        if (estado.avisos.isEmpty()) {
            EstadoVacio(
                icono = Icons.Default.NotificationsNone,
                titulo = "Sin avisos todavía",
                detalle = "Aquí se guardan las notificaciones que te envía la app: entregas " +
                    "cercanas, actividades nuevas, notas recién publicadas y faltas.",
                modifier = Modifier.padding(top = Espacio.xxl)
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = Espacio.xl),
            verticalArrangement = Arrangement.spacedBy(Espacio.s)
        ) {
            grupos.forEach { (dia, avisos) ->
                stickyHeader(key = "dia-" + dia.name) {
                    TituloSeccion(
                        texto = dia.etiqueta,
                        extra = avisos.size.toString(),
                        modifier = Modifier.padding(horizontal = Espacio.lateral)
                    )
                }
                // La clave tiene que ser única de verdad: con la hora y el título, cinco faltas
                // de la misma asignatura anotadas en el mismo segundo repetían clave y la lista
                // se llevaba por delante la pantalla entera.
                items(avisos, key = { it.id }) { aviso ->
                    TarjetaAviso(
                        aviso = aviso,
                        ahora = ahora,
                        modifier = Modifier.padding(horizontal = Espacio.lateral)
                    ) { aviso.url?.let(acciones.abrirEnlace) }
                }
            }
        }
    }

    if (confirmarVaciado) {
        AlertDialog(
            onDismissRequest = { confirmarVaciado = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
            title = { Text("¿Vaciar el historial?") },
            text = { Text("Se borrarán las ${estado.avisos.size} notificaciones guardadas. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarVaciado = false
                    acciones.vaciar()
                }) { Text("Vaciar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarVaciado = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TarjetaAviso(aviso: Aviso, ahora: Long, modifier: Modifier = Modifier, alPulsar: () -> Unit) {
    val tono = tonoDe(aviso.tipo)

    Tarjeta(
        modifier = modifier,
        alPulsar = if (aviso.url != null) alPulsar else null,
        relleno = PaddingValues(Espacio.m)
    ) {
        Row {
            IconoTonal(iconoDe(aviso.tipo), tono)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Espacio.m)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = aviso.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (aviso.url != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = Espacio.xs).size(14.dp)
                        )
                    }
                }
                Text(
                    text = aviso.texto,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = Espacio.s),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espacio.s)
                ) {
                    EtiquetaEstado(aviso.tipo.etiqueta, tono)
                    Text(
                        text = haceCuanto(aviso.momento, ahora),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun iconoDe(tipo: TipoAviso): ImageVector = when (tipo) {
    TipoAviso.ENTREGA -> Icons.Default.NotificationsActive
    TipoAviso.NUEVA -> Icons.Default.NewReleases
    TipoAviso.NOTA -> Icons.Default.Grade
    TipoAviso.FALTA -> Icons.Default.EventBusy
}

private fun tonoDe(tipo: TipoAviso): Tono = when (tipo) {
    TipoAviso.ENTREGA -> Tono.AVISO
    TipoAviso.NUEVA -> Tono.INFO
    TipoAviso.NOTA -> Tono.EXITO
    TipoAviso.FALTA -> Tono.PELIGRO
}
