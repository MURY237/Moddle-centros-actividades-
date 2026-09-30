package com.asir.moodleactividades.ui.acercade

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.asir.moodleactividades.BuildConfig
import com.asir.moodleactividades.R
import com.asir.moodleactividades.data.AjustesAvisos
import com.asir.moodleactividades.data.net.Actualizaciones
import com.asir.moodleactividades.ui.ajustes.AjustesAvisosSeccion
import com.asir.moodleactividades.ui.asistencia.AsistenciaUiState
import com.asir.moodleactividades.ui.asistencia.SeccionAsistencia
import com.asir.moodleactividades.ui.componentes.CabeceraPantalla
import com.asir.moodleactividades.ui.componentes.DivisorFila
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.FilaAjuste
import com.asir.moodleactividades.ui.componentes.Tarjeta
import com.asir.moodleactividades.ui.componentes.TituloSeccion
import com.asir.moodleactividades.ui.theme.Tono

private const val AUTOR = "Mury237"
private const val URL_REPOSITORIO = "https://github.com/${Actualizaciones.REPOSITORIO}"

/** Ajustes de la app: notificaciones, faltas y, al final, qué es y quién la hace. */
@Composable
fun AcercaDeScreen(
    ajustes: AjustesAvisos,
    puedeNotificar: Boolean,
    alCambiarEntregas: (Boolean) -> Unit,
    alCambiarNuevas: (Boolean) -> Unit,
    alCambiarNotas: (Boolean) -> Unit,
    alCambiarAntelacion: (Int) -> Unit,
    alCambiarFrecuencia: (Int) -> Unit,
    alCambiarHora: (Int) -> Unit,
    asistencia: AsistenciaUiState,
    alComprobarAsistencia: () -> Unit,
    alVerFaltasSeneca: () -> Unit,
    modifier: Modifier = Modifier,
    alVolver: (() -> Unit)? = null
) {
    val contexto = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        CabeceraPantalla(titulo = "Ajustes", alVolver = alVolver)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.lateral)
                .padding(bottom = Espacio.xl)
        ) {
            AjustesAvisosSeccion(
                ajustes = ajustes,
                puedeNotificar = puedeNotificar,
                alCambiarEntregas = alCambiarEntregas,
                alCambiarNuevas = alCambiarNuevas,
                alCambiarNotas = alCambiarNotas,
                alCambiarAntelacion = alCambiarAntelacion,
                alCambiarFrecuencia = alCambiarFrecuencia,
                alCambiarHora = alCambiarHora
            )

            SeccionAsistencia(
                estado = asistencia,
                alComprobar = alComprobarAsistencia,
                alVerFaltasSeneca = alVerFaltasSeneca
            )

            TituloSeccion("Acerca de")

            Tarjeta(relleno = PaddingValues(0.dp)) {
                Row(
                    modifier = Modifier.padding(Espacio.l),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(start = Espacio.m)) {
                        Text("Actividades Moodle", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Versión ${BuildConfig.VERSION_NAME} · por $AUTOR",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                DivisorFila()
                FilaAjuste(
                    titulo = "Privacidad",
                    detalle = "La contraseña de Moodle nunca pasa por la app: solo se guarda el " +
                        "token que devuelve el centro. Las de Séneca y Cisco, si decides " +
                        "guardarlas, van cifradas con el almacén de claves del móvil.",
                    icono = Icons.Default.Lock,
                    tono = Tono.EXITO
                )
                DivisorFila()
                FilaAjuste(
                    titulo = "Código fuente",
                    detalle = "github.com/${Actualizaciones.REPOSITORIO}",
                    icono = Icons.Default.Code,
                    tono = Tono.NEUTRO,
                    alPulsar = {
                        runCatching {
                            contexto.startActivity(Intent(Intent.ACTION_VIEW, URL_REPOSITORIO.toUri()))
                        }
                    },
                    final = {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            Text(
                text = "Actividades Moodle no está asociada a Moodle, a Cisco ni a la Consejería " +
                    "de Educación. Es una aplicación independiente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Espacio.s, vertical = Espacio.l)
            )
        }
    }
}
