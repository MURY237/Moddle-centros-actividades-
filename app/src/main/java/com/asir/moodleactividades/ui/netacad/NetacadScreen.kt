package com.asir.moodleactividades.ui.netacad

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LinkOff
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.data.SesionNetacad
import com.asir.moodleactividades.domain.EstadoActividad
import com.asir.moodleactividades.domain.FiltroNetacad
import com.asir.moodleactividades.domain.TrabajoNetacad
import com.asir.moodleactividades.ui.componentes.Aviso
import com.asir.moodleactividades.ui.componentes.BarraProgreso
import com.asir.moodleactividades.ui.componentes.CabeceraPantalla
import com.asir.moodleactividades.ui.componentes.DialogoCuentaGuardada
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
import kotlinx.coroutines.delay

/** Lo que la lista de trabajos puede pedir, sin atarla al ViewModel ni al navegador. */
data class AccionesNetacad(
    val volver: () -> Unit = {},
    val actualizar: () -> Unit = {},
    /** Abre NetAcad a la vista: en un trabajo concreto, o en el panel si es null. */
    val abrir: (String?) -> Unit = {},
    val elegirFiltro: (FiltroNetacad) -> Unit = {},
    val elegirCurso: (String?) -> Unit = {},
    val guardarCuenta: (String, String) -> Unit = { _, _ -> },
    val borrarCuenta: () -> Unit = {},
    val desconectar: () -> Unit = {}
)

@Composable
fun NetacadScreen(
    viewModel: NetacadViewModel,
    sesion: SesionNetacad,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Entrar en la pantalla es el momento en que se quiere ver lo último.
    LaunchedEffect(Unit) { viewModel.actualizarSiConviene() }

    // Si una página no termina nunca de cargar, el recorrido a oscuras se quedaría colgado
    // con la barra de «leyendo» puesta para siempre.
    LaunchedEffect(estado.modo) {
        if (estado.modo == ModoNetacad.OCULTO) {
            // Con margen para el acceso de Cisco, que son dos pantallas más.
            delay(VIGILANTE_POR_PAGINA_MS * estado.paginas.coerceAtLeast(1) + 40_000L)
            viewModel.terminarRecorrido()
        }
    }

    // Con NetAcad a la vista, «atrás» lo gestiona su navegador; si no, se vuelve a Tareas.
    BackHandler(enabled = estado.modo != ModoNetacad.VISIBLE) { alVolver() }

    Box(modifier = modifier.fillMaxSize()) {
        NetacadContenido(
            estado = estado,
            acciones = AccionesNetacad(
                volver = alVolver,
                actualizar = viewModel::actualizar,
                abrir = viewModel::abrirVisible,
                elegirFiltro = viewModel::elegirFiltro,
                elegirCurso = viewModel::elegirCurso,
                guardarCuenta = viewModel::guardarCuenta,
                borrarCuenta = viewModel::borrarCuenta,
                desconectar = viewModel::desconectar
            )
        )

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
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NetacadContenido(
    estado: NetacadUiState,
    acciones: AccionesNetacad,
    modifier: Modifier = Modifier
) {
    var confirmarDesconexion by rememberSaveable { mutableStateOf(false) }
    var editandoCuenta by rememberSaveable { mutableStateOf(false) }
    val leyendo = estado.modo == ModoNetacad.OCULTO

    Column(modifier = modifier.fillMaxSize()) {
        CabeceraPantalla(
            titulo = "Cisco NetAcad",
            subtitulo = estado.momento?.let { "Actualizado " + haceCuanto(it, estado.ahora).lowercase() }
                ?: "Sin leer todavía",
            alVolver = acciones.volver
        ) {
            IconButton(onClick = acciones.actualizar, enabled = estado.modo == ModoNetacad.NINGUNO) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
            }
            val hayAlgo = estado.configurado || estado.leidoAlgunaVez || estado.usuarioGuardado.isNotBlank()
            MenuMas(
                buildList {
                    add(OpcionMenu("Abrir NetAcad", Icons.Default.Language) { acciones.abrir(null) })
                    add(OpcionMenu("Cuenta de Cisco", Icons.Default.Key) { editandoCuenta = true })
                    if (hayAlgo) {
                        add(OpcionMenu("Desconectar", Icons.Default.LinkOff, peligrosa = true) {
                            confirmarDesconexion = true
                        })
                    }
                }
            )
        }

        // Una línea fina y no un círculo: la lista de debajo sigue siendo útil mientras lee.
        if (leyendo) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        // Tirar de la lista hacia abajo recarga, como en cualquier app.
        PullToRefreshBox(
            isRefreshing = leyendo,
            onRefresh = acciones.actualizar,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Espacio.xl),
                verticalArrangement = Arrangement.spacedBy(Espacio.s)
            ) {
                if (estado.necesitaAcceso) {
                    item(key = "acceso") {
                        Aviso(
                            titulo = "La sesión de NetAcad ha caducado",
                            texto = "Lo de abajo es de la última consulta." + when {
                                estado.accesoAuto.isNotBlank() -> "\nAcceso automático: " + estado.accesoAuto + "."
                                estado.usuarioGuardado.isBlank() && estado.almacenSeguro ->
                                    "\nGuarda tu cuenta en ⋮ → Cuenta de Cisco y entrará sola."
                                else -> ""
                            },
                            tono = Tono.AVISO,
                            accion = "Entrar",
                            alPulsarAccion = { acciones.abrir(null) },
                            modifier = Modifier.padding(horizontal = Espacio.lateral, vertical = Espacio.xs)
                        )
                    }
                }

                if (estado.sinExito) {
                    item(key = "sin-exito") {
                        // Se enseña lo que sí se vio —cabeceras y títulos, nunca datos personales—
                        // para poder ajustar el lector si Cisco cambia su web.
                        Aviso(
                            titulo = "No he encontrado trabajos",
                            texto = "Abre tus páginas de curso en NetAcad para que las vuelva a reconocer." +
                                if (estado.pistas.isEmpty()) "" else {
                                    "\nVisto en la página: " + estado.pistas.take(6).joinToString(" · ")
                                },
                            tono = Tono.PELIGRO,
                            accion = "Abrir NetAcad",
                            alPulsarAccion = { acciones.abrir(null) },
                            modifier = Modifier.padding(horizontal = Espacio.lateral, vertical = Espacio.xs)
                        )
                    }
                }

                if (estado.trabajos.isNotEmpty()) {
                    item(key = "resumen") { Resumen(estado) }
                    item(key = "filtros") { Filtros(estado, acciones) }
                }

                when {
                    estado.trabajos.isEmpty() && leyendo -> item(key = "cargando") {
                        Box(Modifier.fillMaxWidth().padding(Espacio.xxl), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                        }
                    }

                    estado.trabajos.isEmpty() && !estado.leidoAlgunaVez -> item(key = "conectar") {
                        EstadoVacio(
                            icono = Icons.Default.School,
                            titulo = "Conecta tu NetAcad",
                            detalle = "Entra con tu cuenta de Cisco y abre la página de calificaciones de " +
                                "cada curso. La app la reconoce sola, la recuerda y a partir de ahí se " +
                                "actualiza sin enseñarte la web."
                        ) {
                            Button(onClick = { acciones.abrir(null) }) { Text("Abrir NetAcad") }
                        }
                    }

                    estado.trabajos.isEmpty() -> item(key = "nada") {
                        EstadoVacio(
                            icono = Icons.Default.CheckCircle,
                            titulo = "Nada en NetAcad",
                            detalle = "En las páginas de curso guardadas no hay ningún trabajo con plazo."
                        )
                    }

                    estado.secciones.isEmpty() -> item(key = "sin-resultados") {
                        EstadoVacio(
                            icono = Icons.Default.SearchOff,
                            titulo = "Sin resultados",
                            detalle = "Ningún trabajo encaja con este filtro. Prueba con «Todos»."
                        )
                    }

                    else -> estado.secciones.forEach { seccion ->
                        stickyHeader(key = "cabecera-" + seccion.grupo.name) {
                            TituloSeccion(
                                texto = seccion.grupo.etiqueta,
                                extra = seccion.trabajos.size.toString(),
                                modifier = Modifier.padding(horizontal = Espacio.lateral)
                            )
                        }
                        items(seccion.trabajos, key = { seccion.grupo.name + "|" + it.id }) { trabajo ->
                            TarjetaTrabajo(
                                trabajo = trabajo,
                                ahora = estado.ahora,
                                modifier = Modifier
                                    .padding(horizontal = Espacio.lateral)
                                    .animateItem()
                            ) { acciones.abrir(trabajo.url) }
                        }
                    }
                }
            }
        }
    }

    if (editandoCuenta) {
        DialogoCuentaGuardada(
            titulo = "Entrar solo en Cisco",
            explicacion = "Cuando la sesión de NetAcad caduque, la app entrará sola con esta cuenta. " +
                "Se guarda cifrada en este móvil y solo se escribe en páginas de cisco.com o " +
                "netacad.com. Si Cisco pide un código de verificación, habrá que entrar a mano.",
            etiquetaUsuario = "Correo de Cisco",
            usuarioGuardado = estado.usuarioGuardado,
            almacenSeguro = estado.almacenSeguro,
            rechazada = if (estado.cuentaRechazada) {
                "Cisco rechazó esta cuenta la última vez. Revisa la contraseña: sola no se vuelve " +
                    "a probar, para no bloquearte la cuenta."
            } else {
                null
            },
            alGuardar = { correo, clave ->
                editandoCuenta = false
                acciones.guardarCuenta(correo, clave)
            },
            alBorrar = {
                editandoCuenta = false
                acciones.borrarCuenta()
            },
            alCerrar = { editandoCuenta = false },
            usuarioValido = { it.contains('@') },
            teclado = KeyboardType.Email
        )
    }

    if (confirmarDesconexion) {
        AlertDialog(
            onDismissRequest = { confirmarDesconexion = false },
            icon = { Icon(Icons.Default.LinkOff, contentDescription = null) },
            title = { Text("¿Desconectar NetAcad?") },
            text = {
                Text(
                    "Se cierra la sesión de Cisco en la app y se borran los trabajos guardados, " +
                        "las páginas de curso aprendidas y la cuenta guardada."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarDesconexion = false
                    acciones.desconectar()
                }) { Text("Desconectar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarDesconexion = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun Resumen(estado: NetacadUiState) {
    val resumen = estado.resumen
    val total = resumen.pendientes + resumen.entregadas + resumen.noEntregadas
    val progreso = if (total == 0) 0f else resumen.entregadas.toFloat() / total

    Column(
        modifier = Modifier.padding(horizontal = Espacio.lateral),
        verticalArrangement = Arrangement.spacedBy(Espacio.s)
    ) {
        FranjaResumen(
            listOf(
                Metrica(resumen.pendientes.toString(), "Por hacer", Tono.AVISO),
                Metrica(resumen.entregadas.toString(), "Hechos", Tono.EXITO),
                Metrica(resumen.noEntregadas.toString(), "Fuera de plazo", Tono.PELIGRO)
            )
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            BarraProgreso(progreso, modifier = Modifier.weight(1f), tono = Tono.EXITO)
            Text(
                text = "${(progreso * 100).toInt()} % hecho" +
                    if (estado.paginas > 0) " · ${estado.paginas} " + (if (estado.paginas == 1) "curso" else "cursos") else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Espacio.m)
            )
        }
    }
}

@Composable
private fun Filtros(estado: NetacadUiState, acciones: AccionesNetacad) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        // Con un solo curso el selector no filtra nada: solo ocuparía sitio.
        if (estado.cursos.size > 1) {
            SelectorAsignatura(
                asignaturas = estado.cursos,
                seleccionada = estado.curso,
                alElegir = acciones.elegirCurso,
                modifier = Modifier.padding(horizontal = Espacio.lateral, vertical = Espacio.xs)
            )
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Espacio.lateral),
            horizontalArrangement = EntreFiltros
        ) {
            FiltroNetacad.entries.forEach { filtro ->
                FilterChip(
                    selected = estado.filtro == filtro,
                    onClick = { acciones.elegirFiltro(filtro) },
                    label = { Text(filtro.etiqueta) }
                )
            }
        }
    }
}

private fun etiquetaDe(estado: EstadoActividad) = when (estado) {
    EstadoActividad.PENDIENTE -> "Por hacer"
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
    ahora: Long,
    modifier: Modifier = Modifier,
    alPulsar: () -> Unit
) {
    val tono = trabajo.estado.tono()
    val tieneEnlace = trabajo.url.isNotBlank()
    val relativo = if (trabajo.porEntregar) textoRelativo(trabajo.fechaLimite, ahora) else ""

    Tarjeta(
        modifier = modifier,
        alPulsar = if (tieneEnlace) alPulsar else null,
        relleno = PaddingValues(Espacio.m)
    ) {
        Row {
            IconoTonal(
                if (esEvaluacion(trabajo.titulo)) Icons.Default.Quiz else Icons.AutoMirrored.Filled.Assignment,
                tono
            )
            Column(modifier = Modifier.weight(1f).padding(start = Espacio.m)) {
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
                            modifier = Modifier.padding(start = Espacio.xs).size(14.dp)
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
                    modifier = Modifier.padding(top = Espacio.s),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.xs + 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EtiquetaEstado(etiquetaDe(trabajo.estado), tono)
                    if (trabajo.nota.isNotBlank()) {
                        EtiquetaEstado("Nota " + trabajo.nota, Tono.INFO, conPunto = false)
                    }
                }
                Text(
                    // «Venció hace 3 días» en algo ya hecho confunde más que ayuda.
                    text = formatearFecha(trabajo.fechaLimite) + if (relativo.isNotBlank()) " · $relativo" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (trabajo.estado == EstadoActividad.NO_ENTREGADA || relativo.startsWith("Vence")) {
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
