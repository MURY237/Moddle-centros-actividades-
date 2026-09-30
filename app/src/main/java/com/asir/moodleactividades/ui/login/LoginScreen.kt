package com.asir.moodleactividades.ui.login

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.BuildConfig
import com.asir.moodleactividades.R
import com.asir.moodleactividades.ui.componentes.Aviso
import com.asir.moodleactividades.ui.componentes.CampoSecreto
import com.asir.moodleactividades.ui.componentes.DivisorFila
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.Tarjeta
import com.asir.moodleactividades.ui.theme.Tono

data class AccionesLogin(
    val cambiarUrl: (String) -> Unit = {},
    val cambiarUsuario: (String) -> Unit = {},
    val cambiarContrasena: (String) -> Unit = {},
    val cambiarToken: (String) -> Unit = {},
    val alternarModo: () -> Unit = {},
    val entrarConNavegador: () -> Unit = {},
    val entrar: () -> Unit = {}
)

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    alEntrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    LaunchedEffect(estado.urlParaAbrir) {
        estado.urlParaAbrir?.let { destino ->
            runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, destino.toUri())) }
            viewModel.navegadorAbierto()
        }
    }

    LoginContenido(
        estado = estado,
        acciones = AccionesLogin(
            cambiarUrl = viewModel::cambiarUrl,
            cambiarUsuario = viewModel::cambiarUsuario,
            cambiarContrasena = viewModel::cambiarContrasena,
            cambiarToken = viewModel::cambiarToken,
            alternarModo = viewModel::alternarModo,
            entrarConNavegador = viewModel::entrarConNavegador,
            entrar = { viewModel.entrar(alEntrar) }
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginContenido(
    estado: LoginUiState,
    acciones: AccionesLogin,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .statusBarsPadding()
            .padding(horizontal = Espacio.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
        }

        Text(
            text = "Actividades Moodle",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = Espacio.l)
        )
        Text(
            text = "Tareas, notas, faltas y horario de tu centro en un solo sitio",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Espacio.xs)
        )

        Spacer(Modifier.height(Espacio.xl))

        Tarjeta(modifier = Modifier.widthIn(max = 480.dp), relleno = PaddingValues(0.dp)) {
            Column(
                modifier = Modifier.padding(Espacio.l),
                verticalArrangement = Arrangement.spacedBy(Espacio.m)
            ) {
                OutlinedTextField(
                    value = estado.url,
                    onValueChange = acciones.cambiarUrl,
                    label = { Text("Moodle del centro") },
                    placeholder = { Text("educacionadistancia.juntadeandalucia.es/…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { acciones.entrarConNavegador() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = acciones.entrarConNavegador,
                    enabled = estado.url.isNotBlank() && !estado.cargando,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Entrar con iDEA/Séneca")
                }

                Text(
                    text = "Se abre el acceso de tu centro en el navegador. La contraseña la escribes " +
                        "allí, nunca en esta app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                estado.error?.let { mensaje -> Aviso(texto = mensaje, tono = Tono.PELIGRO) }
            }

            DivisorFila()

            Column(
                modifier = Modifier.padding(Espacio.l),
                verticalArrangement = Arrangement.spacedBy(Espacio.m)
            ) {
                Text("¿Tu centro no usa iDEA?", style = MaterialTheme.typography.titleSmall)

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !estado.modoToken,
                        onClick = { if (estado.modoToken) acciones.alternarModo() },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        label = { Text("Usuario") }
                    )
                    SegmentedButton(
                        selected = estado.modoToken,
                        onClick = { if (!estado.modoToken) acciones.alternarModo() },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        label = { Text("Token") }
                    )
                }

                val alTerminar = KeyboardActions(onDone = { if (estado.puedeEnviar) acciones.entrar() })
                if (estado.modoToken) {
                    CampoSecreto(
                        valor = estado.token,
                        alCambiar = acciones.cambiarToken,
                        etiqueta = "Token de servicio móvil",
                        acciones = alTerminar
                    )
                    Text(
                        text = "En Moodle: Perfil → Preferencias → Claves de seguridad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextField(
                        value = estado.usuario,
                        onValueChange = acciones.cambiarUsuario,
                        label = { Text("Usuario o correo") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                    CampoSecreto(
                        valor = estado.contrasena,
                        alCambiar = acciones.cambiarContrasena,
                        etiqueta = "Contraseña",
                        acciones = alTerminar
                    )
                }

                OutlinedButton(
                    onClick = acciones.entrar,
                    enabled = estado.puedeEnviar,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (estado.cargando) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(Espacio.s))
                    }
                    Text("Entrar")
                }
            }
        }

        Text(
            text = "Versión ${BuildConfig.VERSION_NAME} · la contraseña de Moodle no se guarda",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = Espacio.xl)
        )
    }
}

