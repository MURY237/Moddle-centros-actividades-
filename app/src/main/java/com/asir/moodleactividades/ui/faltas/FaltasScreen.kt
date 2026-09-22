package com.asir.moodleactividades.ui.faltas

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.data.Credenciales
import com.asir.moodleactividades.data.SesionSeneca
import com.asir.moodleactividades.data.seneca.AutoAcceso
import com.asir.moodleactividades.data.seneca.DiagnosticoSeneca
import com.asir.moodleactividades.data.seneca.ExtractorFaltas
import com.asir.moodleactividades.data.seneca.NavegadorFaltas
import com.asir.moodleactividades.data.seneca.RespuestaJs
import com.asir.moodleactividades.data.seneca.ResultadoAcceso
import com.asir.moodleactividades.domain.FaltasDeAsignatura
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.componentes.Etiqueta
import com.asir.moodleactividades.ui.formatearFecha
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

/** La raíz redirige al acceso; una ruta más concreta se rompería si Séneca la cambia. */
private const val INICIO_SENECA = "https://seneca.juntadeandalucia.es/"

@Composable
fun FaltasScreen(
    viewModel: FaltasViewModel,
    sesion: SesionSeneca,
    alVolver: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Como pestaña no hay nada a lo que volver; solo se intercepta el atrás dentro de Séneca.
    BackHandler(enabled = estado.modo == ModoSeneca.NINGUNO && alVolver != null) {
        alVolver?.invoke()
    }

    // Solo el acceso ocupa la pantalla. El refresco de cortesía trabaja en un navegador
    // diminuto detrás de la lista: quitarle al alumno lo que está mirando no es refrescar.
    if (estado.modo == ModoSeneca.VISIBLE) {
        NavegadorSeneca(
            visible = true,
            sesion = sesion,
            alExtraer = { crudo, url, aMano -> viewModel.procesarPagina(crudo, url, aMano) },
            alCargarPagina = viewModel::anotarPagina,
            alNavegar = viewModel::anotarNavegacion,
            alEntrarSolo = viewModel::credencialesGuardadas,
            alFallarElAcceso = viewModel::marcarCredencialesRechazadas,
            alAnotarAcceso = viewModel::anotarAcceso,
            alNecesitarIdentificacion = viewModel::pedirIdentificacion,
            sinExito = estado.buscadaSinExito,
            diagnostico = estado.diagnostico,
            alCerrar = viewModel::cerrarSeneca,
            modifier = modifier
        )
        return
    }

    if (estado.modo == ModoSeneca.OCULTO) {
        NavegadorSeneca(
            visible = false,
            sesion = sesion,
            alExtraer = { crudo, url, aMano -> viewModel.procesarPagina(crudo, url, aMano) },
            alCargarPagina = viewModel::anotarPagina,
            alNavegar = viewModel::anotarNavegacion,
            alEntrarSolo = viewModel::credencialesGuardadas,
            alFallarElAcceso = viewModel::marcarCredencialesRechazadas,
            alAnotarAcceso = viewModel::anotarAcceso,
            alNecesitarIdentificacion = viewModel::pedirIdentificacion,
            sinExito = false,
            diagnostico = emptyList(),
            alCerrar = viewModel::cerrarSeneca
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Cabecera(
            total = estado.total,
            injustificadas = estado.injustificadas,
            momento = estado.momento,
            consultando = estado.modo == ModoSeneca.OCULTO,
            alVolver = alVolver,
            alActualizar = { viewModel.actualizar() }
        )

        if (estado.necesitaAcceso) {
            AvisoSesionCaducada { viewModel.actualizar() }
        }

        if (estado.porAsignatura.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EstadoVacio(
                    icono = Icons.Default.EventBusy,
                    titulo = "Aún no hay faltas guardadas",
                    detalle = "Séneca no ofrece ninguna forma de consultarlo desde fuera, así " +
                        "que la app entra por ti y te enseña aquí lo que encuentra. La " +
                        "primera vez tendrás que identificarte en la web de Séneca."
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = viewModel::actualizar,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Traer mis faltas")
                        }
                        Spacer(Modifier.height(16.dp))
                        TarjetaCuentaSeneca(
                            usuarioGuardado = estado.usuarioGuardado,
                            almacenDisponible = estado.almacenSeguroDisponible,
                            rechazadas = estado.credencialesRechazadas,
                            alGuardar = viewModel::guardarCredenciales,
                            alOlvidar = viewModel::olvidarCredenciales
                        )
                        if (estado.diagnosticoSeneca.paginasVistas > 0) {
                            Spacer(Modifier.height(16.dp))
                            TarjetaDiagnostico(estado.diagnosticoSeneca)
                        }
                    }
                }
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(estado.porAsignatura, key = { it.asignatura }) { asignatura ->
                TarjetaAsignatura(asignatura)
            }
            item {
                Spacer(Modifier.height(8.dp))
                TarjetaCuentaSeneca(
                    usuarioGuardado = estado.usuarioGuardado,
                    almacenDisponible = estado.almacenSeguroDisponible,
                    rechazadas = estado.credencialesRechazadas,
                    alGuardar = viewModel::guardarCredenciales,
                    alOlvidar = viewModel::olvidarCredenciales
                )
                Spacer(Modifier.height(8.dp))
                TarjetaDiagnostico(estado.diagnosticoSeneca)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::desconectar,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LinkOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Borrar faltas y cerrar la sesión de Séneca")
                }
            }
        }
    }
}

@Composable
private fun Cabecera(
    total: Int,
    injustificadas: Int,
    momento: Long?,
    consultando: Boolean,
    alVolver: (() -> Unit)?,
    alActualizar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(DegradadoCabecera)
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(top = 12.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (alVolver != null) {
                IconButton(onClick = alVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                }
            } else {
                Spacer(Modifier.size(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Faltas de asistencia",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = when {
                        total == 0 -> "Sin datos todavía"
                        injustificadas == 0 -> "$total en total, todas justificadas"
                        else -> "$total en total · $injustificadas sin justificar"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f)
                )
                momento?.let {
                    Text(
                        text = "Leídas el ${formatearFecha(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            if (consultando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(Modifier.size(12.dp))
            } else {
                IconButton(onClick = alActualizar) {
                    Icon(Icons.Default.Refresh, "Volver a leer de Séneca", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TarjetaAsignatura(asignatura: FaltasDeAsignatura) {
    var desplegada by remember { mutableStateOf(false) }

    val color = if (asignatura.injustificadas > 0) RojoNoEntregada else VerdeEntregada
    val fondo = if (asignatura.injustificadas > 0) {
        fondoDeEstado(RojoNoEntregadaFondo, RojoNoEntregadaOscuro)
    } else {
        fondoDeEstado(VerdeEntregadaFondo, VerdeEntregadaOscuro)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(fondo, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = asignatura.total.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Text(
                        text = asignatura.asignatura,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (asignatura.injustificadas > 0) {
                            Etiqueta(
                                texto = "${asignatura.injustificadas} sin justificar",
                                color = RojoNoEntregada,
                                fondo = fondoDeEstado(RojoNoEntregadaFondo, RojoNoEntregadaOscuro)
                            )
                        }
                        if (asignatura.justificadas > 0) {
                            Etiqueta(
                                texto = "${asignatura.justificadas} justificadas",
                                color = VerdeEntregada,
                                fondo = fondoDeEstado(VerdeEntregadaFondo, VerdeEntregadaOscuro)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = if (desplegada) "Ocultar los días" else "Ver los días",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { desplegada = !desplegada }
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            if (desplegada) {
                Spacer(Modifier.height(4.dp))
                asignatura.faltas.forEach { falta ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = falta.fecha,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = falta.tramo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = falta.estado,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (falta.justificada) VerdeEntregada else RojoNoEntregada
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun NavegadorSeneca(
    visible: Boolean,
    sesion: SesionSeneca,
    alExtraer: (String?, String?, Boolean) -> Boolean,
    alCargarPagina: (String?) -> Unit,
    alNavegar: (String) -> Unit,
    alEntrarSolo: () -> Credenciales?,
    alFallarElAcceso: () -> Unit,
    alAnotarAcceso: (String) -> Unit,
    alNecesitarIdentificacion: () -> Unit,
    sinExito: Boolean,
    diagnostico: List<String>,
    alCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contenedor = remember { ContenedorWeb() }

    // Antes de crear el WebView: si se reponen dentro de su construcción, la primera
    // petición puede salir sin ellas y Séneca contesta con la pantalla de acceso.
    remember { sesion.restaurar() }

    BackHandler(enabled = true) {
        val web = contenedor.web
        if (visible && web != null && web.canGoBack()) web.goBack() else alCerrar()
    }

    Box(modifier = if (visible) modifier.fillMaxSize() else Modifier.size(1.dp)) {

        Column(modifier = if (visible) Modifier.fillMaxSize() else Modifier) {
            if (visible) {
                BarraSeneca(contenedor, alExtraer, alCerrar)
                if (sinExito) AvisoSinTabla(diagnostico)
            }

            AndroidView(
                modifier = if (visible) {
                    Modifier.fillMaxSize().navigationBarsPadding()
                } else {
                    // Un WebView sin medidas no carga la página, así que oculto no se quita:
                    // se queda en un punto invisible que no tapa ni recoge toques.
                    Modifier.size(1.dp).alpha(0f)
                },
                factory = { contexto ->
                    WebView(contexto).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        // Sin cookies persistentes habría que identificarse en cada apertura.
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(vistaWeb: WebView?, url: String?) {
                                // Cada página nueva estrena presupuesto de intentos: así la
                                // portada tras identificarse vuelve a probar desde cero.
                                contenedor.intentos = 0
                                CookieManager.getInstance().flush()
                                // La cookie de acceso nace en la página que sigue al
                                // formulario, no en la que trae la tabla.
                                sesion.guardar()
                                alCargarPagina(url)
                                buscarFaltas(
                                    contenedor, url, alExtraer, alNavegar, alEntrarSolo,
                                    alFallarElAcceso, alAnotarAcceso, alNecesitarIdentificacion
                                )
                            }
                        }
                        // La referencia se guarda antes de cargar: onPageFinished la necesita.
                        contenedor.web = this
                        // Séneca ata la sesión también a la ruta que genera al entrar, así que
                        // se vuelve a la última página útil en lugar de empezar por la portada.
                        loadUrl(sesion.urlGuardada() ?: INICIO_SENECA)
                    }
                }
            )
        }

    }
}

@Composable
private fun BarraSeneca(
    contenedor: ContenedorWeb,
    alExtraer: (String?, String?, Boolean) -> Boolean,
    alCerrar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DegradadoCabecera)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = alCerrar) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Salir de Séneca", tint = Color.White)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Séneca",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "Si no entra sola, abre el menú (☰) → Faltas de asistencia",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        IconButton(
            onClick = {
                val web = contenedor.web
                web?.evaluateJavascript(ExtractorFaltas.GUION) {
                    alExtraer(it, web.url, true)
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Buscar la tabla en esta página",
                tint = Color.White
            )
        }
    }
}

/**
 * La sesión de Séneca ha caducado. Se avisa y se ofrece entrar, pero no se entra solo: el
 * alumno estaba mirando sus faltas y quitárselas de delante sin pedirlo es peor que no
 * actualizar.
 */
@Composable
private fun AvisoSesionCaducada(alEntrar: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = fondoDeEstado(AmbarPendienteFondo, AmbarPendienteOscuro)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "La sesión de Séneca ha caducado",
                    style = MaterialTheme.typography.titleSmall,
                    color = AmbarPendiente
                )
                Text(
                    text = "Estas faltas son las de la última consulta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmbarPendiente
                )
            }
            TextButton(onClick = alEntrar) {
                Text("Entrar", color = AmbarPendiente)
            }
        }
    }
}

@Composable
private fun AvisoSinTabla(diagnostico: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = fondoDeEstado(AmbarPendienteFondo, AmbarPendienteOscuro)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "En esta página no hay ninguna tabla de faltas",
                style = MaterialTheme.typography.titleSmall,
                color = AmbarPendiente
            )
            Text(
                text = "Abre el menú (☰), entra en Seguimiento del curso → Faltas de " +
                    "asistencia y vuelve a pulsar el botón de buscar.",
                style = MaterialTheme.typography.bodySmall,
                color = AmbarPendiente
            )
            if (diagnostico.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Tablas vistas: " + diagnostico.joinToString(" / "),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = AmbarPendiente
                )
            }
        }
    }
}

/**
 * Séneca caduca la sesión por su cuenta, y sin una cuenta guardada no hay forma de volver a
 * entrar solo. Guardar una contraseña nunca es inocuo, así que aquí se dice lo que implica
 * en vez de esconderlo detrás de un interruptor.
 */
@Composable
private fun TarjetaCuentaSeneca(
    usuarioGuardado: String,
    almacenDisponible: Boolean,
    rechazadas: Boolean,
    alGuardar: (String, String) -> Unit,
    alOlvidar: () -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { abierto = !abierto },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Entrar solo cuando caduque la sesión",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = when {
                            !almacenDisponible -> "No disponible en este móvil"
                            usuarioGuardado.isNotBlank() -> "Cuenta guardada: $usuarioGuardado"
                            else -> "Sin configurar"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (abierto) "Ocultar" else "Ver",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (rechazadas) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Séneca ha rechazado la cuenta guardada. Revísala si no era un " +
                        "despiste suyo: sola no se reintenta, para no bloquearte la cuenta, " +
                        "pero al recargar a mano se vuelve a probar una vez.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RojoNoEntregada
                )
            }

            if (!abierto) return@Column

            Spacer(Modifier.height(12.dp))

            if (!almacenDisponible) {
                Text(
                    text = "El almacén cifrado de Android no está disponible en este " +
                        "dispositivo, y sin él no se guarda ninguna contraseña.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Text(
                text = "La contraseña se guarda cifrada con el almacén de claves de Android " +
                    "y solo se usa para rellenar el formulario de Séneca. Aun así, quedará " +
                    "en este móvil: si lo pierdes, quien lo tenga podría entrar en tu " +
                    "Séneca. Bórrala cuando quieras desde aquí.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = usuario,
                onValueChange = { usuario = it },
                label = { Text("Usuario de Séneca") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = clave,
                onValueChange = { clave = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        alGuardar(usuario, clave)
                        // No se conserva en memoria más de lo imprescindible.
                        usuario = ""
                        clave = ""
                        abierto = false
                    },
                    enabled = usuario.isNotBlank() && clave.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar y entrar")
                }
                if (usuarioGuardado.isNotBlank()) {
                    TextButton(onClick = alOlvidar) {
                        Text("Borrar la cuenta", color = RojoNoEntregada)
                    }
                }
            }
        }
    }
}

/** Guarda la referencia al WebView sin ser estado de Compose: cambiarla no repinta nada. */
private class ContenedorWeb {
    var web: WebView? = null
    var intentos = 0

    /** Veces que se ha mandado la contraseña: es lo único que puede bloquear la cuenta. */
    var envios = 0

    /** Cerrar el aviso de sesión caducada no manda ninguna contraseña, así que va aparte. */
    var avisosCerrados = 0

    /** Vueltas a la portada cuando la página se queda atascada sin aviso ni formulario. */
    var reinicios = 0
}

/**
 * Busca la tabla en la página actual y, si no está, pulsa la entrada del menú que lleva a
 * ella y lo vuelve a intentar. Cuando lo que hay delante es la pantalla de acceso, entra
 * con la cuenta guardada.
 *
 * El acceso se mira **antes** que el menú, y no después: la pantalla de acceso de Séneca
 * también trae barra de navegación, así que el recorrido creía estar desplegando el menú
 * una y otra vez hasta quedarse sin intentos, y el acceso automático no llegaba a probarse.
 */
private fun buscarFaltas(
    contenedor: ContenedorWeb,
    url: String?,
    alExtraer: (String?, String?, Boolean) -> Boolean,
    alNavegar: (String) -> Unit,
    alEntrarSolo: () -> Credenciales?,
    alFallarElAcceso: () -> Unit,
    alAnotarAcceso: (String) -> Unit,
    alNecesitarIdentificacion: () -> Unit
) {
    val web = contenedor.web ?: return
    val seguir = {
        buscarFaltas(
            contenedor, url, alExtraer, alNavegar, alEntrarSolo,
            alFallarElAcceso, alAnotarAcceso, alNecesitarIdentificacion
        )
    }

    web.evaluateJavascript(ExtractorFaltas.GUION) { crudo ->
        if (alExtraer(crudo, web.url ?: url, false)) return@evaluateJavascript

        // Cambiar el filtro a «Todas» recarga la tabla sin recargar la página.
        if (RespuestaJs.leerExtraccion(crudo)?.ajustado == true &&
            contenedor.intentos < MAX_INTENTOS
        ) {
            contenedor.intentos++
            web.postDelayed(seguir, ESPERA_MS)
            return@evaluateJavascript
        }

        if (contenedor.intentos >= MAX_INTENTOS) {
            alAnotarAcceso("se agotaron los intentos del recorrido")
            alNecesitarIdentificacion()
            return@evaluateJavascript
        }
        contenedor.intentos++

        // El sondeo no lleva la contraseña encima: solo dice qué hay en la página.
        web.evaluateJavascript(AutoAcceso.SONDEO) { sondeo ->
            val visto = RespuestaJs.leerAcceso(sondeo)

            if (visto?.formulario == true || visto?.aviso == true) {
                entrarSolo(
                    contenedor, web, seguir, visto, alEntrarSolo,
                    alFallarElAcceso, alAnotarAcceso
                ) { alNecesitarIdentificacion() }
                return@evaluateJavascript
            }

            web.evaluateJavascript(NavegadorFaltas.GUION) { respuesta ->
                val navegacion = RespuestaJs.leerNavegacion(respuesta)
                alNavegar(navegacion?.destino.orEmpty())
                if (navegacion?.pulsado == true) {
                    // Al desplegar un menú no hay carga de página que avise, así que se
                    // espera un momento y se vuelve a mirar.
                    web.postDelayed(seguir, ESPERA_MS)
                    return@evaluateJavascript
                }

                // Ni tabla, ni acceso, ni menú: la página no es ninguna de las esperadas.
                entrarSolo(
                    contenedor, web, seguir, visto, alEntrarSolo,
                    alFallarElAcceso, alAnotarAcceso
                ) { alNecesitarIdentificacion() }
            }
        }
    }
}

/**
 * Séneca caduca la sesión por su cuenta —lo dice él mismo—, así que cuando aparece su
 * pantalla de acceso se rellena con la cuenta guardada, si la hay.
 *
 * [visto] es lo que el sondeo encontró en la página, sin llevar la contraseña encima. De ahí
 * sale la decisión: dar la cuenta por mala solo cuando lo dice Séneca, y no cuando el
 * recorrido tropieza, porque eso último obliga al alumno a escribirla otra vez para nada.
 *
 * Quitar de en medio el aviso de sesión caducada no gasta intento: ahí no se manda ninguna
 * contraseña. Mandarla sí, y por eso se limita: insistir con una que no vale bloquea la cuenta.
 */
private fun entrarSolo(
    contenedor: ContenedorWeb,
    web: WebView,
    seguir: () -> Unit,
    visto: ResultadoAcceso?,
    alEntrarSolo: () -> Credenciales?,
    alFallarElAcceso: () -> Unit,
    alAnotarAcceso: (String) -> Unit,
    alRendirse: () -> Unit
) {
    val cuenta = alEntrarSolo()
    if (cuenta == null) {
        alAnotarAcceso("no hay cuenta guardada")
        alRendirse()
        return
    }

    val hayFormulario = visto?.formulario == true
    val hayAviso = visto?.aviso == true

    // Que lo diga Séneca es la única señal fiable de que la cuenta no vale.
    if (hayFormulario && visto?.error == true && contenedor.envios > 0) {
        alAnotarAcceso("Séneca dice que los datos no son correctos")
        alFallarElAcceso()
        alRendirse()
        return
    }

    if (contenedor.envios >= MAX_ENVIOS) {
        if (hayFormulario) {
            alAnotarAcceso("enviada $MAX_ENVIOS veces y sigue pidiéndola")
            alFallarElAcceso()
        } else {
            alAnotarAcceso("enviada, pero la página no llega a las faltas")
        }
        alRendirse()
        return
    }

    if (!hayFormulario && !hayAviso) {
        // La página no es ninguna de las esperadas. Volver a la portada devuelve un
        // formulario limpio, que es lo que hace falta para entrar.
        if (contenedor.reinicios < MAX_REINICIOS) {
            contenedor.reinicios++
            contenedor.intentos = 0
            contenedor.avisosCerrados = 0
            alAnotarAcceso("página desconocida: se vuelve a la portada")
            web.loadUrl(INICIO_SENECA)
        } else {
            alAnotarAcceso("no se encontró el formulario de acceso")
            alRendirse()
        }
        return
    }

    web.evaluateJavascript(AutoAcceso.guion(cuenta.usuario, cuenta.clave)) { respuesta ->
        val resultado = RespuestaJs.leerAcceso(respuesta)
        when {
            resultado?.enviado == true -> {
                contenedor.envios++
                alAnotarAcceso("datos enviados (intento ${contenedor.envios})")
                // Enviar el formulario recarga la página: hay que darle tiempo.
                contenedor.intentos = 0
                web.postDelayed(seguir, ESPERA_ACCESO_MS)
            }

            resultado?.cerroAviso == true && contenedor.avisosCerrados < MAX_AVISOS -> {
                contenedor.avisosCerrados++
                alAnotarAcceso("aviso de sesión caducada cerrado")
                contenedor.intentos = 0
                web.postDelayed(seguir, ESPERA_ACCESO_MS)
            }

            else -> {
                alAnotarAcceso("no se pudo rellenar el formulario")
                alRendirse()
            }
        }
    }
}

/**
 * El recorrido puede necesitar tres pulsaciones —hamburguesa, apartado y entrada— y cada una
 * cuenta como intento, así que el tope tiene que dar margen para todas y alguna repetición.
 */
private const val MAX_INTENTOS = 8
private const val ESPERA_MS = 1200L

/**
 * Dos por apertura: la primera puede irse a un formulario a medio cargar. Más no, porque
 * insistir con una contraseña que no vale termina bloqueando la cuenta del alumno.
 */
private const val MAX_ENVIOS = 2

/** Cerrar avisos no manda contraseñas, pero tampoco puede quedarse en bucle. */
private const val MAX_AVISOS = 3

/** Un único reinicio: si la portada limpia tampoco trae el formulario, no lo va a traer. */
private const val MAX_REINICIOS = 1
private const val ESPERA_ACCESO_MS = 2500L

/**
 * Séneca no se puede probar desde fuera de un móvil con sesión, así que la app cuenta qué vio
 * en su último intento. Sin valores de sesión ni datos del alumno: solo nombres de cookies,
 * cuántas había y hasta dónde llegó el recorrido.
 */
@Composable
private fun TarjetaDiagnostico(diagnostico: DiagnosticoSeneca) {
    var abierto by remember { mutableStateOf(false) }
    val portapapeles = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { abierto = !abierto },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = "Qué vio la app en Séneca",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (abierto) "Ocultar" else "Ver",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (abierto) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = diagnostico.comoTexto(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = {
                        portapapeles.setText(AnnotatedString(diagnostico.comoTexto()))
                    }
                ) {
                    Text("Copiar")
                }
            }
        }
    }
}
