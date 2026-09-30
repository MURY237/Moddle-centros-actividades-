package com.asir.moodleactividades.ui.asistencia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.asir.moodleactividades.domain.SondeoAsistencia
import com.asir.moodleactividades.ui.componentes.Aviso
import com.asir.moodleactividades.ui.componentes.DivisorFila
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.FilaAjuste
import com.asir.moodleactividades.ui.componentes.Tarjeta
import com.asir.moodleactividades.ui.componentes.TituloSeccion
import com.asir.moodleactividades.ui.theme.Tono

@Composable
fun SeccionAsistencia(
    estado: AsistenciaUiState,
    alComprobar: () -> Unit,
    alVerFaltasSeneca: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TituloSeccion("Faltas de asistencia")

        Tarjeta(relleno = PaddingValues(0.dp)) {
            // La vía por Moodle puede no existir, pero las faltas siempre se pueden leer de la
            // sesión que el propio alumno abre en Séneca.
            FilaAjuste(
                titulo = "Ver mis faltas",
                detalle = "Se leen de tu sesión de Séneca y se agrupan por asignatura.",
                icono = Icons.Default.EventBusy,
                tono = Tono.PELIGRO,
                alPulsar = alVerFaltasSeneca
            )
            DivisorFila()
            FilaAjuste(
                titulo = "¿Están también en Moodle?",
                detalle = "Pregunta a tu Moodle si ofrece el módulo de asistencia.",
                icono = Icons.Default.Search,
                tono = Tono.NEUTRO,
                final = {
                    when {
                        estado.cargando -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                        estado.sondeo == null && estado.error == null ->
                            TextButton(onClick = alComprobar) { Text("Comprobar") }
                    }
                }
            )
            if (!estado.cargando && (estado.error != null || estado.sondeo != null)) {
                Resultado(estado, alComprobar)
            }
        }
    }
}

@Composable
private fun Resultado(estado: AsistenciaUiState, alComprobar: () -> Unit) {
    val portapapeles = LocalClipboardManager.current
    Column(
        modifier = Modifier.padding(start = Espacio.l, end = Espacio.l, bottom = Espacio.s),
        verticalArrangement = Arrangement.spacedBy(Espacio.s)
    ) {
        val sondeo = estado.sondeo
        when {
            estado.error != null -> Aviso(titulo = "No se pudo comprobar", texto = estado.error, tono = Tono.PELIGRO)
            sondeo != null && sondeo.disponible -> {
                Aviso(
                    titulo = "Tu centro sí ofrece el módulo de asistencia",
                    texto = "Se han encontrado ${sondeo.funcionesAsistencia.size} funciones. Con esto " +
                        "se podrían leer las faltas por asignatura desde Moodle.",
                    tono = Tono.EXITO
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                        .padding(Espacio.m),
                    verticalArrangement = Arrangement.spacedBy(Espacio.xs)
                ) {
                    sondeo.funcionesAsistencia.forEach { funcion ->
                        Text(funcion, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            sondeo != null -> Aviso(
                titulo = "Tu centro no publica las faltas por Moodle",
                texto = "De las ${sondeo.totalFunciones} funciones que abre a la app, ninguna es del " +
                    "módulo de asistencia. Tus faltas están solo en Séneca.",
                tono = Tono.AVISO
            )
        }
        sondeo?.let {
            Text(
                text = "${it.sitio} · ${it.version.ifBlank { "versión desconocida" }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            TextButton(onClick = alComprobar) {
                Text(if (estado.error != null) "Reintentar" else "Volver a comprobar")
            }
            if (sondeo != null) {
                TextButton(onClick = { portapapeles.setText(AnnotatedString(sondeo.comoTexto())) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Copiar diagnóstico")
                }
            }
        }
    }
}
