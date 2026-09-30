package com.asir.moodleactividades.ui.actividades

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.asir.moodleactividades.domain.Actividad
import com.asir.moodleactividades.domain.Adjunto
import com.asir.moodleactividades.domain.EstadoActividad
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.EtiquetaEstado
import com.asir.moodleactividades.ui.componentes.IconoTonal
import com.asir.moodleactividades.ui.componentes.Tarjeta
import com.asir.moodleactividades.ui.componentes.TituloSeccion
import com.asir.moodleactividades.ui.componentes.tono
import com.asir.moodleactividades.ui.theme.Tono
import com.asir.moodleactividades.ui.formatearFecha
import com.asir.moodleactividades.ui.textoRelativo
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaDetalleActividad(
    actividad: Actividad,
    descargas: Map<String, DescargaUi>,
    alCerrar: () -> Unit,
    alDescargar: (Adjunto) -> Unit,
    alAbrirArchivo: (Adjunto, File) -> Unit,
    alCompartirArchivo: (Adjunto, File) -> Unit,
    alAbrirEnMoodle: () -> Unit
) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = alCerrar,
        sheetState = estadoHoja,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        DetalleActividadContenido(
            actividad = actividad,
            descargas = descargas,
            alDescargar = alDescargar,
            alAbrirArchivo = alAbrirArchivo,
            alCompartirArchivo = alCompartirArchivo,
            alAbrirEnMoodle = alAbrirEnMoodle
        )
    }
}

/** El contenido de la ficha, aparte de la hoja: así se puede capturar sin abrirla. */
@Composable
fun DetalleActividadContenido(
    actividad: Actividad,
    descargas: Map<String, DescargaUi>,
    alDescargar: (Adjunto) -> Unit = {},
    alAbrirArchivo: (Adjunto, File) -> Unit = { _, _ -> },
    alCompartirArchivo: (Adjunto, File) -> Unit = { _, _ -> },
    alAbrirEnMoodle: () -> Unit = {}
) {
    run {
        // La hoja no desplaza su contenido por sí sola: un enunciado largo con adjuntos se
        // saldría de la pantalla y el botón de Moodle quedaría fuera de alcance.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.xl)
                .padding(bottom = Espacio.xxl)
        ) {
            CabeceraDetalle(actividad)

            Spacer(Modifier.height(Espacio.l))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(Espacio.l))

            Enunciado(actividad.descripcion)

            if (actividad.adjuntos.isNotEmpty()) {
                TituloSeccion(
                    texto = "Documentos adjuntos",
                    extra = actividad.adjuntos.size.toString(),
                    fondo = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(top = Espacio.s)
                )
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    actividad.adjuntos.forEach { adjunto ->
                        FilaAdjunto(
                            adjunto = adjunto,
                            descarga = descargas[adjunto.url] ?: DescargaUi(),
                            alDescargar = { alDescargar(adjunto) },
                            alAbrir = { alAbrirArchivo(adjunto, it) },
                            alCompartir = { alCompartirArchivo(adjunto, it) }
                        )
                    }
                }
            }

            if (actividad.url != null) {
                Spacer(Modifier.height(Espacio.xl))
                Button(
                    onClick = alAbrirEnMoodle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Abrir en Moodle")
                }
            }
        }
    }
}

@Composable
private fun CabeceraDetalle(actividad: Actividad) {
    Text(
        text = actividad.nombre,
        style = MaterialTheme.typography.titleLarge
    )
    Text(
        text = actividad.curso,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )

    Row(
        modifier = Modifier.padding(top = Espacio.m),
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EtiquetaEstado(actividad.estado.etiqueta, actividad.estado.tono())
        EtiquetaEstado(actividad.tipo.etiqueta, Tono.NEUTRO, conPunto = false)
        actividad.nota?.let { nota -> EtiquetaEstado("Nota $nota", Tono.INFO, conPunto = false) }
    }

    Text(
        text = buildString {
            append("Entrega: ").append(formatearFecha(actividad.fechaLimite))
            val relativo = textoRelativo(actividad.fechaLimite)
            if (relativo.isNotBlank()) append(" · ").append(relativo)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp)
    )
}

@Composable
private fun Enunciado(html: String) {
    if (html.isBlank()) {
        Text(
            text = "Esta actividad no trae enunciado escrito en Moodle.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val colorTexto = MaterialTheme.colorScheme.onSurface.toArgb()
    val colorEnlace = MaterialTheme.colorScheme.primary.toArgb()

    // El enunciado llega en HTML de Moodle (listas, negritas, enlaces). Un TextView lo pinta
    // con más fidelidad que reconstruirlo a mano con AnnotatedString.
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { contexto ->
            TextView(contexto).apply {
                textSize = 15f
                setLineSpacing(0f, 1.15f)
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { vista ->
            vista.setTextColor(colorTexto)
            vista.setLinkTextColor(colorEnlace)
            vista.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }
    )
}

@Composable
private fun FilaAdjunto(
    adjunto: Adjunto,
    descarga: DescargaUi,
    alDescargar: () -> Unit,
    alAbrir: (File) -> Unit,
    alCompartir: (File) -> Unit
) {
    Tarjeta(relleno = PaddingValues(Espacio.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoTonal(iconoDeArchivo(adjunto), Tono.INFO)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = adjunto.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (descarga.estado) {
                        EstadoDescarga.DESCARGANDO -> "Descargando…"
                        EstadoDescarga.LISTA -> "Guardado en el móvil"
                        EstadoDescarga.ERROR -> "No se pudo descargar"
                        EstadoDescarga.PENDIENTE -> tamanoLegible(adjunto.tamano)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (descarga.estado == EstadoDescarga.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            when (descarga.estado) {
                EstadoDescarga.DESCARGANDO -> CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )

                EstadoDescarga.LISTA -> {
                    val archivo = descarga.archivo
                    if (archivo == null) {
                        BotonDescarga(alDescargar)
                    } else {
                        IconButton(onClick = { alCompartir(archivo) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        FilledTonalButton(onClick = { alAbrir(archivo) }) {
                            Text("Abrir")
                        }
                    }
                }

                EstadoDescarga.ERROR -> IconButton(onClick = alDescargar) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Reintentar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                EstadoDescarga.PENDIENTE -> BotonDescarga(alDescargar)
            }
        }
    }
}

@Composable
private fun BotonDescarga(alDescargar: () -> Unit) {
    FilledTonalButton(onClick = alDescargar) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text("Descargar")
    }
}

private fun iconoDeArchivo(adjunto: Adjunto): ImageVector {
    val pista = (adjunto.tipo + " " + adjunto.nombre).lowercase(Locale.ROOT)
    return when {
        "pdf" in pista -> Icons.Default.PictureAsPdf
        "image" in pista || Regex("\\.(png|jpe?g|gif|webp|bmp)$").containsMatchIn(pista) ->
            Icons.Default.Image
        "sheet" in pista || "excel" in pista || "csv" in pista -> Icons.Default.TableChart
        else -> Icons.Default.InsertDriveFile
    }
}

/** Moodle da el tamaño en bytes; en pantalla se lee mejor en KB o MB. */
private fun tamanoLegible(bytes: Long): String = when {
    bytes <= 0 -> "Archivo adjunto"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
    else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024))
}
