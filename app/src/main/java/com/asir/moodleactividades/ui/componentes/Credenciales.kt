package com.asir.moodleactividades.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.asir.moodleactividades.ui.theme.Tono

/** Un campo para contraseñas y tokens: oculto por defecto, con el ojo para comprobarlo. */
@Composable
fun CampoSecreto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    acciones: KeyboardActions = KeyboardActions.Default
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Ocultar" else "Mostrar"
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = acciones,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * La cuenta con la que la app entra sola en un servicio cuando caduca la sesión (Séneca,
 * Cisco). Es la misma ventana para los dos: qué implica guardarla, qué hay guardado, si el
 * servicio la rechazó, y los campos. La contraseña nunca se precarga: solo el usuario.
 *
 * Sin almacén cifrado en el móvil no se ofrece guardar nada, porque la contraseña no se
 * guarda nunca en claro.
 */
@Composable
fun DialogoCuentaGuardada(
    titulo: String,
    explicacion: String,
    etiquetaUsuario: String,
    usuarioGuardado: String,
    almacenSeguro: Boolean,
    rechazada: String?,
    alGuardar: (usuario: String, clave: String) -> Unit,
    alBorrar: () -> Unit,
    alCerrar: () -> Unit,
    usuarioValido: (String) -> Boolean = { it.isNotBlank() },
    teclado: KeyboardType = KeyboardType.Text
) {
    var usuario by remember { mutableStateOf(usuarioGuardado) }
    var clave by remember { mutableStateOf("") }
    val valido = usuarioValido(usuario) && clave.isNotEmpty()

    AlertDialog(
        onDismissRequest = alCerrar,
        icon = { Icon(Icons.Default.Key, contentDescription = null) },
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                if (!almacenSeguro) {
                    Text(
                        "Este móvil no deja guardar la contraseña cifrada, así que no se guarda: " +
                            "habrá que entrar a mano cuando caduque la sesión.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    return@Column
                }
                Text(explicacion, style = MaterialTheme.typography.bodyMedium)
                if (usuarioGuardado.isNotBlank()) {
                    EtiquetaEstado("Guardada: $usuarioGuardado", Tono.EXITO)
                }
                if (rechazada != null) {
                    Aviso(texto = rechazada, tono = Tono.PELIGRO)
                }
                OutlinedTextField(
                    value = usuario,
                    onValueChange = { usuario = it.trim() },
                    label = { Text(etiquetaUsuario) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                CampoSecreto(
                    valor = clave,
                    alCambiar = { clave = it },
                    etiqueta = "Contraseña",
                    acciones = KeyboardActions(onDone = { if (valido) alGuardar(usuario, clave) })
                )
            }
        },
        confirmButton = {
            if (almacenSeguro) {
                TextButton(onClick = { alGuardar(usuario, clave) }, enabled = valido) { Text("Guardar") }
            } else {
                TextButton(onClick = alCerrar) { Text("Entendido") }
            }
        },
        dismissButton = {
            Row {
                if (usuarioGuardado.isNotBlank()) {
                    TextButton(onClick = alBorrar) {
                        Text("Quitar cuenta", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = alCerrar) { Text("Cancelar") }
            }
        }
    )
}
