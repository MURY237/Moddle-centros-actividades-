package com.asir.moodleactividades.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.asir.moodleactividades.ui.theme.Tono
import com.asir.moodleactividades.ui.theme.colores

/**
 * Espaciados de toda la app. Con una escala fija los márgenes cuadran solos entre pantallas
 * y no hay que decidir en cada sitio si van 10, 12 o 14.
 */
object Espacio {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** El margen lateral de todas las pantallas. */
    val lateral = 16.dp
}

/**
 * La cabecera de cada pantalla: título, un subtítulo opcional y sus acciones a la derecha.
 * Va sobre el fondo, sin bloque de color: la jerarquía la da la tipografía.
 */
@Composable
fun CabeceraPantalla(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    alVolver: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(
                start = if (alVolver != null) Espacio.xs else Espacio.lateral + Espacio.xs,
                end = Espacio.xs,
                top = Espacio.m,
                bottom = Espacio.s
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (alVolver != null) {
            IconButton(onClick = alVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitulo.isNullOrBlank()) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        acciones()
    }
}

/**
 * La tarjeta de la app: superficie blanca con borde fino y sin sombra. Con [alPulsar] toda
 * ella es pulsable.
 */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    alPulsar: (() -> Unit)? = null,
    relleno: PaddingValues = PaddingValues(Espacio.l),
    color: Color = MaterialTheme.colorScheme.surface,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val forma = MaterialTheme.shapes.medium
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .then(if (alPulsar != null) Modifier.clickable(onClick = alPulsar) else Modifier),
        shape = forma,
        color = color,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(relleno), content = contenido)
    }
}

@Immutable
data class Metrica(val valor: String, val etiqueta: String, val tono: Tono = Tono.NEUTRO)

/** Unas pocas cifras en fila, separadas por líneas finas: el resumen de lo de abajo. */
@Composable
fun FranjaResumen(metricas: List<Metrica>, modifier: Modifier = Modifier) {
    Tarjeta(modifier = modifier, relleno = PaddingValues(vertical = Espacio.m)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            metricas.forEachIndexed { indice, metrica ->
                if (indice > 0) VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Espacio.s),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = metrica.valor,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (metrica.tono == Tono.NEUTRO) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            metrica.tono.colores().contenido
                        }
                    )
                    Text(
                        text = metrica.etiqueta,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** El estado de algo en una pastilla pequeña: punto de color y texto. */
@Composable
fun EtiquetaEstado(
    texto: String,
    tono: Tono,
    modifier: Modifier = Modifier,
    conPunto: Boolean = true
) {
    val colores = tono.colores()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colores.contenedor)
            .padding(horizontal = Espacio.s, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (conPunto) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(colores.contenido, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = colores.contenido,
            maxLines = 1
        )
    }
}

private fun iconoDe(tono: Tono): ImageVector = when (tono) {
    Tono.EXITO -> Icons.Default.CheckCircle
    Tono.AVISO -> Icons.Default.WarningAmber
    Tono.PELIGRO -> Icons.Default.ErrorOutline
    Tono.INFO, Tono.NEUTRO -> Icons.Default.Info
}

/**
 * Un aviso dentro de la pantalla: sesión caducada, datos sin conexión, algo que revisar.
 * Fondo suave del tono, icono y, si hace falta, una acción y la opción de cerrarlo.
 */
@Composable
fun Aviso(
    texto: String,
    tono: Tono,
    modifier: Modifier = Modifier,
    titulo: String? = null,
    icono: ImageVector = iconoDe(tono),
    accion: String? = null,
    alPulsarAccion: (() -> Unit)? = null,
    alCerrar: (() -> Unit)? = null
) {
    val colores = tono.colores()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colores.contenedor
    ) {
        Row(
            modifier = Modifier.padding(start = Espacio.m, top = Espacio.m, bottom = Espacio.m, end = Espacio.xs),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = colores.contenido,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Espacio.m)
            ) {
                if (!titulo.isNullOrBlank()) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (accion != null && alPulsarAccion != null) {
                    TextButton(
                        onClick = alPulsarAccion,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = Espacio.xs)
                    ) {
                        Text(accion, color = colores.contenido, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (alCerrar != null) {
                IconButton(onClick = alCerrar, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar aviso",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * El rótulo de un grupo dentro de una lista, en versalitas y gris. Con [fondo] se puede
 * usar como cabecera fija de una lista sin que el contenido se transparente por debajo.
 */
@Composable
fun TituloSeccion(
    texto: String,
    modifier: Modifier = Modifier,
    extra: String? = null,
    fondo: Color = MaterialTheme.colorScheme.background
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(fondo)
            .padding(top = Espacio.l, bottom = Espacio.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = texto.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (extra != null) {
            Text(
                text = extra,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Un icono dentro de un cuadrado suave del tono: el acompañante de cada fila de lista. */
@Composable
fun IconoTonal(
    icono: ImageVector,
    tono: Tono,
    modifier: Modifier = Modifier,
    tamano: Dp = 40.dp
) {
    val colores = tono.colores()
    Box(
        modifier = modifier
            .size(tamano)
            .clip(MaterialTheme.shapes.medium)
            .background(colores.contenedor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = colores.contenido,
            modifier = Modifier.size(tamano / 2)
        )
    }
}

/** Progreso en una línea fina, con sus extremos redondeados. */
@Composable
fun BarraProgreso(
    progreso: Float,
    modifier: Modifier = Modifier,
    tono: Tono = Tono.INFO
) {
    LinearProgressIndicator(
        progress = { progreso.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50)),
        color = tono.colores().contenido,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round
    )
}

/** Mientras se carga algo que aún no tiene nada que enseñar. */
@Composable
fun Cargando(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
    }
}

/** Separación vertical estándar entre bloques de una pantalla. */
@Composable
fun Separacion(alto: Dp = Espacio.m) = Spacer(Modifier.height(alto))

/** Contenedor centrado para estados vacíos, errores y cargas. */
@Composable
fun Centrado(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Espacio.xl),
        contentAlignment = Alignment.Center
    ) { contenido() }
}

/** Fila de filtros con desplazamiento lateral y el margen de la pantalla. */
val RellenoFiltros = PaddingValues(horizontal = Espacio.lateral)

/** Espaciado entre filtros. */
val EntreFiltros = Arrangement.spacedBy(Espacio.s)
