package com.asir.moodleactividades.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asir.moodleactividades.data.ActividadesRepository
import com.asir.moodleactividades.data.net.MoodleException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class LoginUiState(
    val url: String = "",
    val usuario: String = "",
    val contrasena: String = "",
    val token: String = "",
    val modoToken: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null,
    val urlParaAbrir: String? = null
) {
    val puedeEnviar: Boolean
        get() = url.isNotBlank() && !cargando &&
            if (modoToken) token.isNotBlank() else usuario.isNotBlank() && contrasena.isNotBlank()
}

class LoginViewModel(private val repositorio: ActividadesRepository) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState(url = repositorio.ultimaUrl()))
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun cambiarUrl(valor: String) = _estado.update { it.copy(url = valor, error = null) }
    fun cambiarUsuario(valor: String) = _estado.update { it.copy(usuario = valor, error = null) }
    fun cambiarContrasena(valor: String) = _estado.update { it.copy(contrasena = valor, error = null) }
    fun cambiarToken(valor: String) = _estado.update { it.copy(token = valor, error = null) }
    fun alternarModo() = _estado.update { it.copy(modoToken = !it.modoToken, error = null) }

    fun entrarConNavegador() {
        val url = _estado.value.url
        if (url.isBlank()) {
            _estado.update { it.copy(error = "Escribe primero la URL del Moodle de tu centro.") }
            return
        }
        _estado.update { it.copy(error = null, urlParaAbrir = repositorio.prepararSso(url)) }
    }

    fun navegadorAbierto() = _estado.update { it.copy(urlParaAbrir = null) }

    fun procesarRespuesta(enlace: String, alCompletar: () -> Unit) {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            runCatching { repositorio.completarSso(enlace) }.fold(
                onSuccess = {
                    _estado.update { it.copy(cargando = false) }
                    alCompletar()
                },
                onFailure = { fallo ->
                    _estado.update { it.copy(cargando = false, error = mensajeDeError(fallo)) }
                }
            )
        }
    }

    fun entrar(alCompletar: () -> Unit) {
        val actual = _estado.value
        if (!actual.puedeEnviar) return

        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val resultado = runCatching {
                if (actual.modoToken) {
                    repositorio.iniciarSesionConToken(actual.url, actual.token)
                } else {
                    repositorio.iniciarSesion(actual.url, actual.usuario.trim(), actual.contrasena)
                }
            }
            resultado.fold(
                onSuccess = {
                    _estado.update { it.copy(cargando = false) }
                    alCompletar()
                },
                onFailure = { fallo ->
                    _estado.update { it.copy(cargando = false, error = mensajeDeError(fallo)) }
                }
            )
        }
    }

    private fun mensajeDeError(fallo: Throwable): String = when {
        fallo is MoodleException && fallo.esLoginInvalido ->
            "Credenciales incorrectas. No todos los centros permiten entrar con el correo: prueba con tu " +
                "nombre de usuario. Si tu centro usa iDEA/Séneca, entra con un token."
        fallo is MoodleException && fallo.codigo == "enablewsdescription" ->
            "El centro tiene desactivados los servicios web de Moodle. Pide a tu administrador que habilite el servicio móvil."
        fallo is MoodleException && fallo.esTokenInvalido ->
            "El token no es válido o ha caducado. Genera uno nuevo desde tu perfil de Moodle."
        fallo is MoodleException -> fallo.message ?: "Error de Moodle."
        fallo is IOException ->
            "No se ha podido contactar con ese servidor. Comprueba que la URL del centro sea correcta."
        else -> "No se pudo iniciar sesión: ${fallo.message ?: fallo::class.simpleName}"
    }
}
