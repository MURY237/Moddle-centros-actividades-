package com.asir.moodleactividades.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import android.content.Intent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asir.moodleactividades.BuildConfig
import com.asir.moodleactividades.data.net.SsoLogin

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Actividades Moodle",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "v${BuildConfig.VERSION_NAME} · esquema ${SsoLogin.ESQUEMA_SOLICITADO}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Consulta tus tareas pendientes, entregadas y no entregadas del Moodle de tu centro.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = estado.url,
            onValueChange = viewModel::cambiarUrl,
            label = { Text("URL del Moodle del centro") },
            placeholder = { Text("educacionadistancia.juntadeandalucia.es/centros/...") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = viewModel::entrarConNavegador,
            enabled = estado.url.isNotBlank() && !estado.cargando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar con iDEA/Séneca")
        }
        Text(
            text = "Te llevará al acceso de tu centro en el navegador. La contraseña la escribes allí, " +
                "nunca dentro de esta app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "O accede con las credenciales propias de Moodle:",
            style = MaterialTheme.typography.bodyMedium
        )

        if (estado.modoToken) {
            OutlinedTextField(
                value = estado.token,
                onValueChange = viewModel::cambiarToken,
                label = { Text("Token de servicio móvil") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Genera el token en Moodle: Perfil → Preferencias → Claves de seguridad → " +
                    "«Moodle mobile web service».",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            OutlinedTextField(
                value = estado.usuario,
                onValueChange = viewModel::cambiarUsuario,
                label = { Text("Correo o nombre de usuario") },
                placeholder = { Text("tu correo de Moodle") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = estado.contrasena,
                onValueChange = viewModel::cambiarContrasena,
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
        }

        estado.error?.let { mensaje ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Button(
            onClick = { viewModel.entrar(alEntrar) },
            enabled = estado.puedeEnviar,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (estado.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp).padding(end = 2.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text("Entrar")
        }

        TextButton(onClick = viewModel::alternarModo) {
            Text(
                if (estado.modoToken) "Usar usuario y contraseña"
                else "Mi centro usa iDEA/Séneca: entrar con token"
            )
        }

        Text(
            text = "Tus credenciales no se guardan: solo se almacena el token que devuelve tu Moodle.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
