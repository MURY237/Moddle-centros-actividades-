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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.data.SesionSeneca
import com.asir.moodleactividades.domain.FaltasDeAsignatura
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
    val desconectar: () -> Unit = {}
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
            desconectar = viewModel::desconectar
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

                if (estado.porAsignatura.isEmpty()) {
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
                    item(key = "titulo-asignaturas") {
                        TituloSeccion("Por asignatura", extra = estado.porAsignatura.size.toString())
                    }
                    items(estado.porAsignatura, key = { it.asignatura }) { asignatura ->
                        TarjetaAsignatura(asignatura)
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
                                falta.tramo,
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
