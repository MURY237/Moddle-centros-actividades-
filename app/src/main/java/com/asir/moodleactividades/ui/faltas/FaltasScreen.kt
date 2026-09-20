package com.asir.moodleactividades.ui.faltas

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.data.seneca.ExtractorFaltas
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
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    BackHandler(enabled = !estado.navegando, onBack = alVolver)

    if (estado.navegando) {
        NavegadorSeneca(
            alExtraer = { crudo, aMano -> viewModel.procesarPagina(crudo, aMano) },
            sinExito = estado.buscadaSinExito,
            diagnostico = estado.diagnostico,
            alCerrar = viewModel::cerrarSeneca,
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Cabecera(
            total = estado.total,
            injustificadas = estado.injustificadas,
            momento = estado.momento,
            alVolver = alVolver,
            alActualizar = viewModel::abrirSeneca
        )

        if (estado.porAsignatura.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EstadoVacio(
                    icono = Icons.Default.EventBusy,
                    titulo = "Aún no hay faltas guardadas",
                    detalle = "Séneca no ofrece ninguna forma de consultarlo desde fuera, así " +
                        "que la app abre Séneca para que entres tú. Cuando llegues a «Faltas " +
                        "de asistencia», guarda la tabla sola. Tu contraseña no pasa por la app."
                ) {
                    Button(onClick = viewModel::abrirSeneca, shape = RoundedCornerShape(14.dp)) {
                        Text("Entrar en Séneca")
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
    alVolver: () -> Unit,
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
            IconButton(onClick = alVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
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
            IconButton(onClick = alActualizar) {
                Icon(Icons.Default.Refresh, "Volver a leer de Séneca", tint = Color.White)
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
    alExtraer: (String?, Boolean) -> Unit,
    sinExito: Boolean,
    diagnostico: List<String>,
    alCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contenedor = remember { ContenedorWeb() }

    // Dentro de Séneca, atrás navega por el historial de la web antes de salir de ella.
    BackHandler(enabled = true) {
        val web = contenedor.web
        if (web != null && web.canGoBack()) web.goBack() else alCerrar()
    }

    Column(modifier = modifier.fillMaxSize()) {
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
                    text = "Entra y ve a Seguimiento del curso → Faltas de asistencia",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            IconButton(
                onClick = {
                    contenedor.web?.evaluateJavascript(ExtractorFaltas.GUION) {
                        alExtraer(it, true)
                    }
                }
            ) {
                Icon(Icons.Default.Refresh, "Buscar la tabla en esta página", tint = Color.White)
            }
        }

        if (sinExito) {
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
                        text = "Ve a «Faltas de asistencia» y vuelve a pulsar el botón de buscar.",
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

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            factory = { contexto ->
                WebView(contexto).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(vistaWeb: WebView?, url: String?) {
                            // Se prueba en cada página: la mayoría no son la de faltas y el
                            // guion simplemente no encuentra la tabla, que no es un error.
                            vistaWeb?.evaluateJavascript(ExtractorFaltas.GUION) {
                                alExtraer(it, false)
                            }
                        }
                    }
                    loadUrl(INICIO_SENECA)
                    contenedor.web = this
                }
            }
        )
    }
}

/** Guarda la referencia al WebView sin ser estado de Compose: cambiarla no repinta nada. */
private class ContenedorWeb {
    var web: WebView? = null
}
