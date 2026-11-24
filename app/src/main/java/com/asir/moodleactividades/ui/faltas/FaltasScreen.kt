package com.asir.moodleactividades.ui.faltas

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.data.SesionSeneca
import com.asir.moodleactividades.domain.Falta
import com.asir.moodleactividades.domain.FaltasDeAsignatura
import com.asir.moodleactividades.domain.HorarioClases
import com.asir.moodleactividades.ui.componentes.Aviso
import com.asir.moodleactividades.ui.componentes.CabeceraPantalla
import com.asir.moodleactividades.ui.componentes.DialogoCuentaGuardada
import com.asir.moodleactividades.ui.componentes.DivisorFila
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.componentes.EtiquetaEstado
import com.asir.moodleactividades.ui.componentes.FilaAjuste
import com.asir.moodleactividades.ui.componentes.FranjaResumen
import com.asir.moodleactividades.ui.componentes.Metrica
import com.asir.moodleactividades.ui.componentes.Tarjeta
import com.asir.moodleactividades.ui.componentes.TituloSeccion
import com.asir.moodleactividades.ui.formatearFecha
import com.asir.moodleactividades.ui.theme.Tono
import com.asir.moodleactividades.ui.theme.colores
import kotlinx.coroutines.delay

/** Lo que la lista de faltas puede pedir, sin atarla al ViewModel ni al navegador. */
data class AccionesFaltas(
    val actualizar: () -> Unit = {},
    val guardarCuenta: (String, String) -> Unit = { _, _ -> },
    val olvidarCuenta: () -> Unit = {},
    val desconectar: () -> Unit = {},
    /** Fecha del día completo y, por tramo, su asignatura; en blanco es «sin clase». */
    val rellenarDia: (String, Map<String, String>) -> Unit = { _, _ -> },
    val borrarRelleno: (String) -> Unit = {}
)

/**
 * Tope del refresco a oscuras. El recorrido completo —acceso, menú y tabla— lleva unos
 * veinte segundos; si pasado esto no ha terminado, es que una página se ha quedado colgada.
 */
private const val TOPE_OCULTO_MS = 120_000L

@Composable
fun FaltasScreen(
    viewModel: FaltasViewModel,
    sesion: SesionSeneca,
    alVolver: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Como pestaña no hay nada a lo que volver; dentro de Séneca el atrás es del navegador.
    BackHandler(enabled = estado.modo != ModoSeneca.VISIBLE && alVolver != null) {
        alVolver?.invoke()
    }

    LaunchedEffect(estado.modo) {
        if (estado.modo == ModoSeneca.OCULTO) {
            delay(TOPE_OCULTO_MS)
            viewModel.terminarPorTiempo()
        }
    }

    // Solo el acceso ocupa la pantalla. El refresco de cortesía trabaja en un navegador
    // invisible detrás de la lista: quitarle al alumno lo que está mirando no es refrescar.
    if (estado.modo != ModoSeneca.NINGUNO) {
        val visible = estado.modo == ModoSeneca.VISIBLE
        // Pasar de oculto a visible estrena navegador, con sus contadores de intentos a cero.
        key(visible) {
            NavegadorSeneca(
                visible = visible,
                sesion = sesion,
                alExtraer = { crudo, url, aMano -> viewModel.procesarPagina(crudo, url, aMano) },
                alCargarPagina = viewModel::anotarPagina,
                alNavegar = viewModel::anotarNavegacion,
                alEntrarSolo = viewModel::credencialesGuardadas,
                alFallarElAcceso = viewModel::marcarCredencialesRechazadas,
                alAnotarAcceso = viewModel::anotarAcceso,
                alNecesitarIdentificacion = viewModel::pedirIdentificacion,
                sinExito = visible && estado.buscadaSinExito,
                diagnostico = if (visible) estado.diagnostico else emptyList(),
                alCerrar = viewModel::cerrarSeneca,
                modifier = modifier
            )
        }
        if (visible) return
    }

    FaltasContenido(
        estado = estado,
        alVolver = alVolver,
        acciones = AccionesFaltas(
            actualizar = { viewModel.actualizar() },
            guardarCuenta = viewModel::guardarCredenciales,
            olvidarCuenta = viewModel::olvidarCredenciales,
            desconectar = viewModel::desconectar,
            rellenarDia = viewModel::rellenarDia,
            borrarRelleno = viewModel::borrarRelleno
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaltasContenido(
    estado: FaltasUiState,
    acciones: AccionesFaltas,
    modifier: Modifier = Modifier,
    alVolver: (() -> Unit)? = null
) {
    var editandoCuenta by rememberSaveable { mutableStateOf(false) }
    var viendoDiagnostico by rememberSaveable { mutableStateOf(false) }
    var confirmarDesconexion by rememberSaveable { mutableStateOf(false) }
    /** La fecha del día completo que se está rellenando. */
    var rellenando by rememberSaveable { mutableStateOf<String?>(null) }
    val consultando = estado.modo == ModoSeneca.OCULTO

    Column(modifier = modifier.fillMaxSize()) {
        CabeceraPantalla(
            titulo = "Faltas",
            subtitulo = estado.momento?.let { "Leídas el " + formatearFecha(it) } ?: "De tu sesión de Séneca",
            alVolver = alVolver
        ) {
            if (consultando) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            } else {
                IconButton(onClick = acciones.actualizar) {
                    Icon(Icons.Default.Refresh, contentDescription = "Volver a leer de Séneca")
                }
            }
        }

        // Tirar de la lista hacia abajo recarga, como en cualquier app.
        PullToRefreshBox(
            isRefreshing = consultando,
            onRefresh = acciones.actualizar,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Espacio.lateral, end = Espacio.lateral, bottom = Espacio.xl),
                verticalArrangement = Arrangement.spacedBy(Espacio.s)
            ) {
                if (estado.necesitaAcceso) {
                    item(key = "caducada") {
                        // Se avisa y se ofrece entrar, pero no se entra solo: el alumno estaba
                        // mirando sus faltas y quitárselas de delante sin pedirlo es peor.
                        Aviso(
                            titulo = "La sesión de Séneca ha caducado",
                            texto = "Estas faltas son las de la última consulta." +
                                // Aquí y no escondido en el diagnóstico: cuando el acceso automático
                                // falla, es lo único que dice por qué.
                                estado.diagnosticoSeneca.acceso.takeIf { it.isNotBlank() }
                                    ?.let { "\nAcceso automático: $it." }.orEmpty(),
                            tono = Tono.AVISO,
                            accion = "Entrar",
                            alPulsarAccion = acciones.actualizar
                        )
                    }
                }

                // Con solo días completos sin rellenar no hay nada por asignatura, pero sí faltas.
                if (estado.faltas.isEmpty()) {
                    item(key = "vacio") {
                        EstadoVacio(
                            icono = Icons.Default.EventBusy,
                            titulo = "Aún no hay faltas guardadas",
                            detalle = "Séneca no ofrece ninguna forma de consultarlas desde fuera, así " +
                                "que la app entra por ti y te enseña aquí lo que encuentra. La primera " +
                                "vez tendrás que identificarte en la web de Séneca."
                        ) {
                            Button(onClick = acciones.actualizar, enabled = !consultando) { Text("Traer mis faltas") }
                        }
                    }
                } else {
                    item(key = "resumen") {
                        FranjaResumen(
                            listOf(
                                Metrica(estado.total.toString(), "Total"),
                                Metrica((estado.total - estado.injustificadas).toString(), "Justificadas", Tono.EXITO),
                                Metrica(estado.injustificadas.toString(), "Sin justificar", Tono.PELIGRO)
                            ),
                            modifier = Modifier.padding(top = Espacio.xs)
                        )
                    }
                    if (estado.sinRellenar.isNotEmpty()) {
                        item(key = "sin-rellenar") {
                            val dias = estado.sinRellenar
                            Aviso(
                                titulo = if (dias.size == 1) "1 día completo sin rellenar" else "${dias.size} días completos sin rellenar",
                                texto = "Séneca no dice a qué clases faltaste. Rellena qué asignaturas tuviste " +
                                    "ese día y se sumarán a cada una.",
                                tono = Tono.AVISO,
                                accion = "Rellenar",
                                alPulsarAccion = { rellenando = dias.first().fecha }
                            )
                        }
                    }
                    if (estado.diasCompletos.isNotEmpty()) {
                        item(key = "titulo-dias") {
                            TituloSeccion("Días completos", extra = estado.diasCompletos.size.toString())
                        }
                        item(key = "dias") {
                            Tarjeta(relleno = PaddingValues(0.dp)) {
                                estado.diasCompletos.forEachIndexed { indice, dia ->
                                    if (indice > 0) DivisorFila()
                                    FilaDiaCompleto(
                                        falta = dia,
                                        clases = estado.rellenos.de(dia.fecha),
                                        alPulsar = { rellenando = dia.fecha }
                                    )
                                }
                            }
                        }
                    }
                    if (estado.porAsignatura.isNotEmpty()) {
                        item(key = "titulo-asignaturas") {
                            TituloSeccion("Por asignatura", extra = estado.porAsignatura.size.toString())
                        }
                        items(estado.porAsignatura, key = { it.asignatura }) { asignatura ->
                            TarjetaAsignatura(asignatura)
                        }
                    }
                }

                item(key = "titulo-seneca") { TituloSeccion("Séneca") }
                item(key = "seneca") {
                    Tarjeta(relleno = PaddingValues(0.dp)) {
                        FilaAjuste(
                            titulo = "Entrar solo cuando caduque",
                            detalle = when {
                                !estado.almacenSeguroDisponible -> "No disponible en este móvil"
                                estado.credencialesRechazadas -> "Séneca rechazó la cuenta guardada"
                                estado.usuarioGuardado.isNotBlank() -> "Cuenta guardada: " + estado.usuarioGuardado
                                else -> "Sin configurar"
                            },
                            icono = Icons.Default.Key,
                            tono = if (estado.credencialesRechazadas) Tono.PELIGRO else Tono.INFO,
                            alPulsar = { editandoCuenta = true }
                        )
                        if (estado.diagnosticoSeneca.paginasVistas > 0 || estado.leidoAlgunaVez) {
                            DivisorFila()
                            FilaAjuste(
                                titulo = "Qué vio la app en Séneca",
                                detalle = "Para saber dónde se atasca si no llega a las faltas",
                                icono = Icons.Default.Info,
                                tono = Tono.NEUTRO,
                                alPulsar = { viendoDiagnostico = true }
                            )
                        }
                        if (estado.leidoAlgunaVez || estado.usuarioGuardado.isNotBlank()) {
                            DivisorFila()
                            FilaAjuste(
                                titulo = "Desconectar Séneca",
                                detalle = "Borra las faltas, la sesión y la cuenta guardada",
                                icono = Icons.Default.LinkOff,
                                tono = Tono.PELIGRO,
                                alPulsar = { confirmarDesconexion = true }
                            )
                        }
                    }
                }
            }
        }
    }

    if (editandoCuenta) {
        DialogoCuentaGuardada(
            titulo = "Entrar solo en Séneca",
            explicacion = "Cuando caduque la sesión, la app entrará sola con esta cuenta. La " +
                "contraseña se guarda cifrada en este móvil y solo se escribe en páginas de " +
                "juntadeandalucia.es. Si pierdes el móvil, quien lo tenga podría entrar en tu " +
                "Séneca: bórrala cuando quieras desde aquí.",
            etiquetaUsuario = "Usuario de Séneca",
            usuarioGuardado = estado.usuarioGuardado,
            almacenSeguro = estado.almacenSeguroDisponible,
            rechazada = if (estado.credencialesRechazadas) {
                "Séneca rechazó esta cuenta. Sola no se vuelve a probar, para no bloquearte " +
                    "la cuenta; al recargar a mano se prueba una vez más."
            } else {
                null
            },
            alGuardar = { usuario, clave ->
                editandoCuenta = false
                acciones.guardarCuenta(usuario, clave)
            },
            alBorrar = {
                editandoCuenta = false
                acciones.olvidarCuenta()
            },
            alCerrar = { editandoCuenta = false }
        )
    }

    rellenando?.let { fecha ->
        estado.diasCompletos.firstOrNull { it.fecha == fecha }?.let { dia ->
            DialogoDiaCompleto(
                falta = dia,
                inicial = estado.inicialDe(fecha),
                yaRellenado = estado.rellenos.relleno(fecha),
                tramos = estado.tramos,
                opciones = estado.asignaturas,
                alGuardar = { clases ->
                    acciones.rellenarDia(fecha, clases)
                    rellenando = null
                },
                alBorrar = {
                    acciones.borrarRelleno(fecha)
                    rellenando = null
                },
                alCerrar = { rellenando = null }
            )
        }
    }

    if (viendoDiagnostico) {
        DialogoDiagnostico(estado.diagnosticoSeneca.comoTexto()) { viendoDiagnostico = false }
    }

    if (confirmarDesconexion) {
        AlertDialog(
            onDismissRequest = { confirmarDesconexion = false },
            icon = { Icon(Icons.Default.LinkOff, contentDescription = null) },
            title = { Text("¿Desconectar Séneca?") },
            text = {
                Text(
                    "Se borran las faltas guardadas, la sesión de Séneca en la app y la cuenta " +
                        "guardada. La de NetAcad no se toca."
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
private fun TarjetaAsignatura(asignatura: FaltasDeAsignatura) {
    var desplegada by rememberSaveable(asignatura.asignatura) { mutableStateOf(false) }
    val tono = if (asignatura.injustificadas > 0) Tono.PELIGRO else Tono.EXITO
    val colores = tono.colores()

    Tarjeta(relleno = PaddingValues(0.dp), modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { desplegada = !desplegada }
                .padding(Espacio.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // El número de faltas en el sitio del icono: es lo primero que se busca.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colores.contenedor, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = asignatura.total.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = colores.contenido
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = Espacio.m)) {
                Text(
                    text = asignatura.asignatura,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = Espacio.xs),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.xs + 2.dp)
                ) {
                    if (asignatura.injustificadas > 0) {
                        EtiquetaEstado("${asignatura.injustificadas} sin justificar", Tono.PELIGRO)
                    }
                    if (asignatura.justificadas > 0) {
                        val n = asignatura.justificadas
                        EtiquetaEstado(if (n == 1) "1 justificada" else "$n justificadas", Tono.EXITO)
                    }
                }
            }
            Icon(
                if (desplegada) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (desplegada) "Ocultar los días" else "Ver los días",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (desplegada) {
            asignatura.faltas.forEach { falta ->
                DivisorFila()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Espacio.l, vertical = Espacio.s + 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(falta.fecha, style = MaterialTheme.typography.bodyMedium)
                        if (falta.tramo.isNotBlank()) {
                            Text(
                                if (falta.deDiaCompleto) falta.tramo + " · día completo" else falta.tramo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    EtiquetaEstado(
                        texto = falta.estado.ifBlank { if (falta.justificada) "Justificada" else "Injustificada" },
                        tono = if (falta.justificada) Tono.EXITO else Tono.PELIGRO,
                        conPunto = false
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaDiaCompleto(falta: Falta, clases: Map<String, String>?, alPulsar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = alPulsar)
            .padding(horizontal = Espacio.l, vertical = Espacio.s + 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = Espacio.s)) {
            Text(nombreDelDia(falta.fecha) + falta.fecha, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = when {
                    clases == null -> "Sin rellenar: toca para poner las clases"
                    clases.isEmpty() -> "Rellenado: sin clases ese día"
                    else -> (if (clases.size == 1) "1 clase: " else "${clases.size} clases: ") +
                        clases.values.distinct().joinToString(", ")
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (clases == null) Tono.AVISO.colores().contenido else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        EtiquetaEstado(
            texto = falta.estado.ifBlank { if (falta.justificada) "Justificada" else "Injustificada" },
            tono = if (falta.justificada) Tono.EXITO else Tono.PELIGRO,
            conPunto = false
        )
    }
}

/**
 * Qué clase había en cada tramo de un día completo. Se abre con lo ya guardado o con una
 * propuesta sacada de las otras faltas, pero nada cuenta hasta pulsar «Guardar».
 */
@Composable
private fun DialogoDiaCompleto(
    falta: Falta,
    inicial: Map<String, String>,
    yaRellenado: Boolean,
    tramos: List<String>,
    opciones: List<String>,
    alGuardar: (Map<String, String>) -> Unit,
    alBorrar: () -> Unit,
    alCerrar: () -> Unit
) {
    var clases by remember(falta.fecha) { mutableStateOf(inicial) }
    var otraPara by remember { mutableStateOf<String?>(null) }
    val todas = (opciones + clases.values.filter { it.isNotBlank() }).distinct().sorted()

    AlertDialog(
        onDismissRequest = alCerrar,
        icon = { Icon(Icons.Default.EventBusy, contentDescription = null) },
        title = { Text("Día completo · " + nombreDelDia(falta.fecha) + falta.fecha) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Espacio.xs)
            ) {
                Text(
                    "Elige la clase que tenías en cada tramo. Al guardar, cada una cuenta como una falta " +
                        (if (falta.justificada) "justificada" else "sin justificar") + " en su asignatura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!yaRellenado && inicial.isNotEmpty()) {
                    Text(
                        "Viene propuesto según tus otras faltas de ese día de la semana: revísalo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Tono.AVISO.colores().contenido
                    )
                }
                if (tramos.isEmpty()) {
                    Text(
                        "Aún no se conocen las horas de clase: salen de tus otras faltas.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                tramos.forEach { tramo ->
                    FilaTramo(
                        tramo = tramo,
                        asignatura = clases[tramo]?.takeIf { it.isNotBlank() },
                        opciones = todas,
                        alElegir = { clases = clases + (tramo to it) },
                        alPedirOtra = { otraPara = tramo }
                    )
                }
                if (yaRellenado) {
                    TextButton(onClick = alBorrar) {
                        Text("Dejar sin rellenar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        // Se guardan todos los tramos: uno en blanco es «sin clase», no «sin decidir».
        confirmButton = {
            TextButton(onClick = { alGuardar(tramos.associateWith { clases[it].orEmpty() }) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } }
    )

    otraPara?.let { tramo ->
        DialogoOtraAsignatura(
            alAceptar = { nombre ->
                clases = clases + (tramo to nombre)
                otraPara = null
            },
            alCerrar = { otraPara = null }
        )
    }
}

@Composable
private fun FilaTramo(
    tramo: String,
    asignatura: String?,
    opciones: List<String>,
    alElegir: (String) -> Unit,
    alPedirOtra: () -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            tramo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            TextButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    asignatura ?: "Sin clase",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (asignatura == null) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                DropdownMenuItem(
                    text = { Text("Sin clase") },
                    onClick = {
                        abierto = false
                        alElegir("")
                    }
                )
                opciones.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            abierto = false
                            alElegir(opcion)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Otra asignatura…") },
                    onClick = {
                        abierto = false
                        alPedirOtra()
                    }
                )
            }
        }
    }
}

/** Para una asignatura a la que nunca se ha faltado, que por eso no está en la lista. */
@Composable
private fun DialogoOtraAsignatura(alAceptar: (String) -> Unit, alCerrar: () -> Unit) {
    var texto by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Otra asignatura") },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                singleLine = true,
                label = { Text("Nombre, como sale en Séneca") }
            )
        },
        confirmButton = {
            TextButton(onClick = { alAceptar(texto.trim()) }, enabled = texto.isNotBlank()) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } }
    )
}

/** «Miércoles » para «11/11/2026»; vacío si la fecha no se entiende. */
private fun nombreDelDia(fecha: String): String =
    HorarioClases.diaDeLaSemana(fecha)?.let { NOMBRES_DIAS[it - 1] + " " }.orEmpty()

private val NOMBRES_DIAS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

/**
 * Séneca no se puede probar desde fuera de un móvil con sesión, así que la app cuenta qué vio
 * en su último intento. Sin valores de sesión ni datos del alumno: solo nombres de cookies,
 * cuántas había y hasta dónde llegó el recorrido.
 */
@Composable
private fun DialogoDiagnostico(texto: String, alCerrar: () -> Unit) {
    val portapapeles = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = alCerrar,
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text("Qué vio la app en Séneca") },
        text = {
            SelectionContainer {
                Text(
                    text = texto,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } },
        dismissButton = {
            TextButton(onClick = { portapapeles.setText(AnnotatedString(texto)) }) { Text("Copiar") }
        }
    )
}
