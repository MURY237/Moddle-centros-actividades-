package com.asir.moodleactividades.ui.netacad

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.data.SesionNetacad
import com.asir.moodleactividades.data.netacad.ExtractorNetacad
import com.asir.moodleactividades.data.netacad.RespuestaNetacad
import com.asir.moodleactividades.data.netacad.ResultadoNetacad
import com.asir.moodleactividades.domain.EstadoActividad
import com.asir.moodleactividades.domain.FiltroNetacad
import com.asir.moodleactividades.domain.TrabajoNetacad
import com.asir.moodleactividades.ui.componentes.AnilloProgreso
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.componentes.Etiqueta
import com.asir.moodleactividades.ui.componentes.FilaEstadisticas
import com.asir.moodleactividades.ui.componentes.SelectorAsignatura
import com.asir.moodleactividades.ui.formatearFecha
import com.asir.moodleactividades.ui.haceCuanto
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
import kotlinx.coroutines.delay

private const val ANCHO_OCULTO = 1080
private const val ALTO_OCULTO = 1920
private const val ESPERA_MS = 1500L

/** NetAcad es una aplicación de una sola página: tarda en pintar tras cargar. ~18 s por página. */
private const val MAX_SONDEOS_OCULTO = 12

/** Tope por página a oscuras aunque la página siga navegando sola y reinicie el recuento. */
private const val MAX_TOTAL_OCULTO = 30

/** A la vista se mira un rato tras cada carga; luego se espera a que el alumno navegue. */
private const val MAX_SONDEOS_VISIBLE = 20

/** «Buscar aquí» responde rápido: si en unos segundos no hay nada, no lo va a haber. */
private const val MAX_SONDEOS_MANUAL = 3

/**
 * El OAuth de Cisco pasa por su página de acceso aunque la sesión siga viva, y rebota solo.
 * Solo se da por caducada si el acceso sigue ahí varios sondeos seguidos.
 */
private const val ACCESOS_PARA_RENDIRSE = 3

/** Presupuesto total del recorrido oculto por página, por si una nunca llega a terminar. */
private const val VIGILANTE_POR_PAGINA_MS = 35_000L

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NetacadScreen(
    viewModel: NetacadViewModel,
    sesion: SesionNetacad,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var confirmarDesconexion by remember { mutableStateOf(false) }

    // Entrar en la pantalla es el momento en que se quiere ver lo último.
    LaunchedEffect(Unit) { viewModel.actualizarSiConviene() }

    // Si una página no termina nunca de cargar, el recorrido a oscuras se quedaría colgado
    // con la barra de «leyendo» puesta para siempre.
    LaunchedEffect(estado.modo) {
        if (estado.modo == ModoNetacad.OCULTO) {
            delay(VIGILANTE_POR_PAGINA_MS * estado.paginas.coerceAtLeast(1) + 10_000L)
            viewModel.terminarRecorrido()
        }
    }

    // Con NetAcad a la vista, «atrás» lo gestiona su navegador; si no, se vuelve a Tareas.
    BackHandler(enabled = estado.modo != ModoNetacad.VISIBLE) { alVolver() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Cabecera(
                estado = estado,
                alVolver = alVolver,
                alRefrescar = viewModel::actualizar,
                alAbrir = { viewModel.abrirVisible() },
                alDesconectar = { confirmarDesconexion = true }
            )

            if (estado.modo == ModoNetacad.OCULTO) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (estado.necesitaAcceso) {
                Aviso(
                    icono = Icons.Default.Info,
                    texto = "La sesión de NetAcad ha caducado. Entra otra vez con tu cuenta de " +
                        "Cisco para seguir actualizando. Lo de abajo es de la última consulta.",
                    boton = "Entrar",
                    color = AmbarPendiente,
                    fondo = fondoDeEstado(AmbarPendienteFondo, AmbarPendienteOscuro)
                ) { viewModel.abrirVisible() }
            }
            if (estado.sinExito) {
                AvisoSinTrabajos(estado.pistas) { viewModel.abrirVisible() }
            }
            if (estado.trabajos.isNotEmpty()) Filtros(estado, viewModel)

            Contenido(estado, viewModel)
        }

        if (estado.modo != ModoNetacad.NINGUNO) {
            // Pasar de oculto a visible estrena navegador: el visible empieza por el panel.
            key(estado.modo == ModoNetacad.VISIBLE) {
                NavegadorNetacad(
                    visible = estado.modo == ModoNetacad.VISIBLE,
                    viewModel = viewModel,
                    sesion = sesion,
                    ultimaLectura = estado.ultimaLectura
                )
            }
        }
    }

    if (confirmarDesconexion) {
        AlertDialog(
            onDismissRequest = { confirmarDesconexion = false },
            icon = { Icon(Icons.Default.LinkOff, contentDescription = null) },
            title = { Text("Desconectar NetAcad") },
            text = {
                Text(
                    "Se cierra la sesión de Cisco en la app y se borran los trabajos " +
                        "guardados y las páginas de curso aprendidas."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarDesconexion = false
                    viewModel.desconectar()
                }) { Text("Desconectar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarDesconexion = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun Cabecera(
    estado: NetacadUiState,
    alVolver: () -> Unit,
    alRefrescar: () -> Unit,
    alAbrir: () -> Unit,
    alDesconectar: () -> Unit
) {
    val resumen = estado.resumen
    val total = resumen.pendientes + resumen.entregadas + resumen.noEntregadas
    val progreso = if (total == 0) 0f else resumen.entregadas.toFloat() / total

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(DegradadoCabecera)
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = alVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver a Tareas", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cisco NetAcad",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = estado.momento?.let { "Actualizado: " + haceCuanto(it).lowercase() }
                            ?: "Sin leer todavía",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
                IconButton(onClick = alRefrescar, enabled = estado.modo == ModoNetacad.NINGUNO) {
                    Icon(Icons.Default.Refresh, "Actualizar", tint = Color.White)
                }
                IconButton(onClick = alAbrir) {
                    Icon(Icons.Default.School, "Abrir NetAcad", tint = Color.White)
                }
                if (estado.configurado || estado.leidoAlgunaVez) {
                    IconButton(onClick = alDesconectar) {
                        Icon(Icons.Default.LinkOff, "Desconectar NetAcad", tint = Color.White)
                    }
                }
            }

            if (estado.trabajos.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnilloProgreso(
                        progreso = progreso,
                        etiquetaCentral = "${(progreso * 100).toInt()}%",
                        subEtiqueta = "hecho",
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
                                resumen.noEntregadas > 0 ->
                                    "Tienes ${resumen.noEntregadas} fuera de plazo"
                                resumen.pendientes > 0 ->
                                    "Te quedan ${resumen.pendientes} por hacer"
                                else -> "Todo al día"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "$total ${if (total == 1) "trabajo" else "trabajos"}" +
                                if (estado.paginas > 0) {
                                    " · ${estado.paginas} " +
                                        if (estado.paginas == 1) "curso" else "cursos"
                                } else {
                                    ""
                                },
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
                    fondoNoEntregada = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun Filtros(estado: NetacadUiState, viewModel: NetacadViewModel) {
    Column(
        modifier = Modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Con un solo curso el selector no filtra nada: solo ocuparía sitio.
        if (estado.cursos.size > 1) {
            SelectorAsignatura(
                asignaturas = estado.cursos,
                seleccionada = estado.curso,
                alElegir = viewModel::elegirCurso,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FiltroNetacad.entries.forEach { filtro ->
                FilterChip(
                    selected = estado.filtro == filtro,
                    onClick = { viewModel.elegirFiltro(filtro) },
                    label = { Text(filtro.etiqueta) },
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Contenido(estado: NetacadUiState, viewModel: NetacadViewModel) {
    when {
        estado.trabajos.isEmpty() && estado.modo == ModoNetacad.OCULTO -> Caja {
            CircularProgressIndicator()
        }

        estado.trabajos.isEmpty() && !estado.leidoAlgunaVez -> Caja {
            EstadoVacio(
                icono = Icons.Default.School,
                titulo = "Conecta tu NetAcad",
                detalle = "Entra con tu cuenta de Cisco y abre la página de calificaciones de " +
                    "cada curso. La app la reconoce sola, la recuerda y a partir de ahí se " +
                    "actualiza sin enseñarte la web."
            ) {
                Button(onClick = { viewModel.abrirVisible() }) { Text("Abrir NetAcad") }
            }
        }

        estado.trabajos.isEmpty() -> Caja {
            EstadoVacio(
                icono = Icons.Default.CheckCircle,
                titulo = "Nada en NetAcad",
                detalle = "En las páginas de curso guardadas no hay ningún trabajo con plazo."
            )
        }

        estado.secciones.isEmpty() -> Caja {
            EstadoVacio(
                icono = Icons.Default.SearchOff,
                titulo = "Sin resultados",
                detalle = "Ningún trabajo encaja con este filtro. Prueba con «Todos»."
            )
        }

        else -> LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            estado.secciones.forEach { seccion ->
                stickyHeader(key = "cabecera-" + seccion.grupo.name) {
                    CabeceraSeccion(seccion.grupo.etiqueta, seccion.trabajos.size)
                }
                items(seccion.trabajos, key = { seccion.grupo.name + "|" + it.id }) { trabajo ->
                    TarjetaTrabajo(trabajo, modifier = Modifier.animateItem()) {
                        viewModel.abrirVisible(trabajo.url)
                    }
                }
            }
        }
    }
}

@Composable
private fun Caja(contenido: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { contenido() }
}

@Composable
private fun CabeceraSeccion(etiqueta: String, cantidad: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = cantidad.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun etiquetaDe(estado: EstadoActividad) = when (estado) {
    EstadoActividad.PENDIENTE -> "Pendiente"
    EstadoActividad.ENTREGADA -> "Hecho"
    EstadoActividad.NO_ENTREGADA -> "Fuera de plazo"
}

/** Los exámenes y cuestionarios se distinguen de las prácticas por el nombre: no hay tipo. */
private fun esEvaluacion(titulo: String): Boolean =
    Regex("(?i)exam|quiz|cuestionario|checkpoint|evaluaci|assessment|test")
        .containsMatchIn(titulo)

@Composable
private fun TarjetaTrabajo(
    trabajo: TrabajoNetacad,
    modifier: Modifier = Modifier,
    alPulsar: () -> Unit
) {
    val color = when (trabajo.estado) {
        EstadoActividad.ENTREGADA -> VerdeEntregada
        EstadoActividad.PENDIENTE -> AmbarPendiente
        EstadoActividad.NO_ENTREGADA -> RojoNoEntregada
    }
    val fondo = when (trabajo.estado) {
        EstadoActividad.ENTREGADA -> fondoDeEstado(VerdeEntregadaFondo, VerdeEntregadaOscuro)
        EstadoActividad.PENDIENTE -> fondoDeEstado(AmbarPendienteFondo, AmbarPendienteOscuro)
        EstadoActividad.NO_ENTREGADA -> fondoDeEstado(RojoNoEntregadaFondo, RojoNoEntregadaOscuro)
    }
    val tieneEnlace = trabajo.url.isNotBlank()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = tieneEnlace, onClick = alPulsar),
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
                    imageVector = if (esEvaluacion(trabajo.titulo)) {
                        Icons.Default.Quiz
                    } else {
                        Icons.AutoMirrored.Filled.Assignment
                    },
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = trabajo.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (tieneEnlace) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (trabajo.curso.isNotBlank()) {
                    Text(
                        text = trabajo.curso,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Etiqueta(etiquetaDe(trabajo.estado), color, fondo)
                    if (trabajo.nota.isNotBlank()) {
                        Etiqueta(
                            texto = "Nota " + trabajo.nota,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fondo = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                }
                Text(
                    text = buildString {
                        append(formatearFecha(trabajo.fechaLimite))
                        // «Venció hace 3 días» en algo ya hecho confunde más que ayuda.
                        if (trabajo.porEntregar) {
                            val relativo = textoRelativo(trabajo.fechaLimite)
                            if (relativo.isNotBlank()) append(" · ").append(relativo)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun Aviso(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    boton: String,
    color: Color,
    fondo: Color,
    alPulsar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = fondo)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icono, contentDescription = null, tint = color)
            Text(
                text = texto,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            )
            TextButton(onClick = alPulsar) { Text(boton) }
        }
    }
}

/**
 * No apareció ningún trabajo en las páginas guardadas. Se enseña lo que sí se vio —cabeceras
 * de tabla y títulos, nunca datos personales— para poder ajustar el lector si Cisco cambia
 * su web, en lugar de tener que adivinar.
 */
@Composable
private fun AvisoSinTrabajos(pistas: List<String>, alAbrir: () -> Unit) {
    Column {
        Aviso(
            icono = Icons.Default.SearchOff,
            texto = "No he encontrado trabajos en tus páginas de curso. Ábrelas en NetAcad " +
                "para que las vuelva a reconocer.",
            boton = "Abrir",
            color = RojoNoEntregada,
            fondo = fondoDeEstado(RojoNoEntregadaFondo, RojoNoEntregadaOscuro),
            alPulsar = alAbrir
        )
        if (pistas.isNotEmpty()) {
            Text(
                text = "Visto en la página: " + pistas.take(6).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
    }
}

/** Guarda el WebView y el recuento de sondeos sin ser estado de Compose. */
private class ContenedorNetacad {
    var web: WebView? = null

    /** Sube con cada carga de página: un sondeo de una página anterior ya no vale. */
    var generacion = 0
    var intentos = 0
    var totales = 0
    var accesosSeguidos = 0
    var visible = false
    var manual = false
}

/** Lo que el sondeo puede hacer, sin atar las funciones de más abajo a Compose. */
private class AccionesNetacad(
    val recibir: (ResultadoNetacad, String?) -> Boolean,
    val pedirAcceso: () -> Unit,
    val siguiente: () -> String?,
    val terminar: () -> Unit,
    val sinTrabajos: (List<String>) -> Unit
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun NavegadorNetacad(
    visible: Boolean,
    viewModel: NetacadViewModel,
    sesion: SesionNetacad,
    ultimaLectura: String
) {
    val contenedor = remember { ContenedorNetacad() }
    contenedor.visible = visible
    var sinTrabajosAqui by remember { mutableStateOf<List<String>?>(null) }

    val acciones = remember(viewModel) {
        AccionesNetacad(
            recibir = { resultado, url ->
                val hubo = viewModel.recibir(resultado, url)
                if (hubo) sinTrabajosAqui = null
                hubo
            },
            pedirAcceso = viewModel::pedirAcceso,
            siguiente = viewModel::siguientePagina,
            terminar = viewModel::terminarRecorrido,
            sinTrabajos = { pistas -> sinTrabajosAqui = pistas }
        )
    }

    // Antes de crear el WebView: si se reponen durante su construcción, la primera petición
    // puede salir sin ellas y NetAcad contesta con el acceso de Cisco.
    val paginaInicial = remember {
        sesion.restaurar()
        viewModel.paginaInicial()
    }

    BackHandler(enabled = visible) {
        val web = contenedor.web
        if (web != null && web.canGoBack()) web.goBack() else viewModel.cerrarNavegador()
    }

    // Al salir, cualquier sondeo pendiente se da por muerto.
    DisposableEffect(Unit) {
        onDispose {
            contenedor.generacion++
            contenedor.web?.stopLoading()
            contenedor.web = null
        }
    }

    Column(
        modifier = if (visible) {
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        } else {
            Modifier
        }
    ) {
        if (visible) {
            BarraNetacad(
                ultimaLectura = ultimaLectura,
                sinTrabajosAqui = sinTrabajosAqui,
                alCerrar = viewModel::cerrarNavegador,
                alRecargar = { contenedor.web?.reload() },
                alBuscarAqui = {
                    sinTrabajosAqui = null
                    empezar(contenedor, acciones, manual = true)
                }
            )
        }

        AndroidView(
            modifier = if (visible) {
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            } else {
                // Oculto no quiere decir diminuto: con 1 dp la página se maqueta en un píxel
                // y los elementos miden cero. Se le da tamaño de móvil y se le quita el sitio.
                Modifier
                    .layout { medible, _ ->
                        val colocable = medible.measure(Constraints.fixed(ANCHO_OCULTO, ALTO_OCULTO))
                        layout(0, 0) { colocable.place(0, 0) }
                    }
                    .alpha(0f)
            },
            factory = { contexto ->
                WebView(contexto).apply {
                    settings.javaScriptEnabled = true
                    // NetAcad guarda su estado en localStorage: sin esto no arranca.
                    settings.domStorageEnabled = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    // El acceso de Cisco rebota entre id.cisco.com y netacad.com.
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(vistaWeb: WebView?, url: String?) {
                            CookieManager.getInstance().flush()
                            sesion.guardar()
                            empezar(contenedor, acciones)
                        }

                        // NetAcad cambia de sección sin cargar página nueva: onPageFinished
                        // no se entera, pero el historial sí.
                        override fun doUpdateVisitedHistory(
                            vistaWeb: WebView?,
                            url: String?,
                            isReload: Boolean
                        ) {
                            empezar(contenedor, acciones)
                        }
                    }
                    contenedor.web = this
                    loadUrl(paginaInicial)
                }
            }
        )
    }
}

@Composable
private fun BarraNetacad(
    ultimaLectura: String,
    sinTrabajosAqui: List<String>?,
    alCerrar: () -> Unit,
    alRecargar: () -> Unit,
    alBuscarAqui: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = alCerrar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cerrar NetAcad")
            }
            Text(
                text = "NetAcad",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = alBuscarAqui) { Text("Buscar aquí") }
            IconButton(onClick = alRecargar) {
                Icon(Icons.Default.Refresh, "Recargar")
            }
        }

        when {
            ultimaLectura.isNotBlank() -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(fondoDeEstado(VerdeEntregadaFondo, VerdeEntregadaOscuro))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerdeEntregada)
                Text(
                    text = "Leídos $ultimaLectura. Abre otro curso o pulsa Listo.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                )
                Button(onClick = alCerrar) { Text("Listo") }
            }

            sinTrabajosAqui != null -> Text(
                text = "No veo trabajos en esta página. Abre la de calificaciones del curso." +
                    if (sinTrabajosAqui.isEmpty()) "" else {
                        " Visto: " + sinTrabajosAqui.take(4).joinToString(" · ")
                    },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(fondoDeEstado(AmbarPendienteFondo, AmbarPendienteOscuro))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            else -> Text(
                // El botón de Google se niega a funcionar dentro de una app, por política
                // suya: mejor avisarlo antes que dejar al alumno atascado en su pantalla.
                text = "Entra con tu correo y contraseña de Cisco (el botón de Google no " +
                    "funciona dentro de apps) y abre las calificaciones de tu curso: la app " +
                    "lo detecta sola.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

/** Arranca una tanda de sondeos para la página actual y deja muertas las anteriores. */
private fun empezar(c: ContenedorNetacad, a: AccionesNetacad, manual: Boolean = false) {
    val web = c.web ?: return
    c.generacion++
    c.intentos = 0
    c.accesosSeguidos = 0
    c.manual = manual
    val generacion = c.generacion
    web.postDelayed({ sondear(c, generacion, a) }, if (manual) 0L else ESPERA_MS)
}

private fun sondear(c: ContenedorNetacad, generacion: Int, a: AccionesNetacad) {
    val web = c.web ?: return
    if (generacion != c.generacion) return

    web.evaluateJavascript(ExtractorNetacad.GUION) { crudo ->
        if (generacion != c.generacion) return@evaluateJavascript
        c.totales++
        val resultado = RespuestaNetacad.leer(crudo) ?: ResultadoNetacad(pagina = "cargando")

        if (resultado.pideAcceso) {
            // A la vista lo resuelve el alumno, y la carga siguiente vuelve a mirar sola.
            if (c.visible) return@evaluateJavascript
            c.accesosSeguidos++
            if (c.accesosSeguidos >= ACCESOS_PARA_RENDIRSE) {
                c.generacion++
                a.pedirAcceso()
            } else {
                web.postDelayed({ sondear(c, generacion, a) }, ESPERA_MS)
            }
            return@evaluateJavascript
        }
        c.accesosSeguidos = 0

        if (a.recibir(resultado, web.url)) {
            if (!c.visible) pasarALaSiguiente(c, a)
            return@evaluateJavascript
        }

        c.intentos++
        val tope = when {
            c.manual -> MAX_SONDEOS_MANUAL
            c.visible -> MAX_SONDEOS_VISIBLE
            else -> MAX_SONDEOS_OCULTO
        }
        val agotado = c.intentos >= tope || (!c.visible && c.totales >= MAX_TOTAL_OCULTO)
        if (!agotado) {
            web.postDelayed({ sondear(c, generacion, a) }, ESPERA_MS)
            return@evaluateJavascript
        }

        if (c.visible) {
            if (c.manual) a.sinTrabajos(resultado.pistas)
        } else {
            pasarALaSiguiente(c, a)
        }
    }
}

private fun pasarALaSiguiente(c: ContenedorNetacad, a: AccionesNetacad) {
    // Corta los sondeos que queden de esta página antes de cambiar de página.
    c.generacion++
    c.totales = 0
    val siguiente = a.siguiente()
    if (siguiente == null) {
        a.terminar()
        return
    }
    c.web?.loadUrl(siguiente)
}
