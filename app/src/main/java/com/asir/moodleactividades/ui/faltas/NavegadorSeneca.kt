package com.asir.moodleactividades.ui.faltas

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.asir.moodleactividades.data.Credenciales
import com.asir.moodleactividades.data.SesionSeneca
import com.asir.moodleactividades.data.seneca.AutoAcceso
import com.asir.moodleactividades.data.seneca.ExtractorFaltas
import com.asir.moodleactividades.data.seneca.NavegadorFaltas
import com.asir.moodleactividades.data.seneca.RespuestaJs
import com.asir.moodleactividades.data.seneca.ResultadoAcceso
import com.asir.moodleactividades.ui.componentes.Aviso
import com.asir.moodleactividades.ui.componentes.CabeceraPantalla
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.theme.Tono

/*
 * El navegador de Séneca y el recorrido que hace dentro: buscar la tabla de faltas, llegar a
 * ella por el menú y, si aparece el acceso, entrar con la cuenta guardada. Va aparte de la
 * pantalla porque es otra cosa: la pantalla pinta faltas; esto habla con una web ajena.
 */

/** La raíz redirige al acceso; una ruta más concreta se rompería si Séneca la cambia. */
private const val INICIO_SENECA = "https://seneca.juntadeandalucia.es/"

/** Medidas de un móvil corriente, en píxeles: es como hay que maquetar aunque no se vea. */
private const val ANCHO_OCULTO = 1080
private const val ALTO_OCULTO = 1920

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun NavegadorSeneca(
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

    // Solo a la vista: oculto, el atrás cancelaba el refresco en vez de salir de la pantalla.
    BackHandler(enabled = visible) {
        val web = contenedor.web
        if (web != null && web.canGoBack()) web.goBack() else alCerrar()
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
                    // Oculto no quiere decir diminuto. Con un WebView de 1 dp la página se
                    // maquetaba en un píxel de ancho, los campos del formulario medían cero y
                    // el acceso automático no encontraba nada que rellenar: por eso el
                    // refresco a oscuras acababa siempre pidiendo usuario y contraseña.
                    // Aquí se le da tamaño de móvil de verdad y luego se le quita el sitio.
                    Modifier
                        .layout { medible, _ ->
                            val colocable = medible.measure(
                                Constraints.fixed(ANCHO_OCULTO, ALTO_OCULTO)
                            )
                            layout(0, 0) { colocable.place(0, 0) }
                        }
                        .alpha(0f)
                },
                factory = { contexto ->
                    WebView(contexto).apply {
                        settings.javaScriptEnabled = true
                        // Séneca no necesita leer ficheros del móvil: en Android 10 y anteriores
                        // venía permitido por defecto, y una página podría pedirlos.
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
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
                },
                // Cada refresco estrena navegador: sin destruir el anterior, su proceso de
                // renderizado seguía vivo. Sin la referencia, los reintentos pendientes paran.
                onRelease = { web ->
                    contenedor.web = null
                    web.stopLoading()
                    web.destroy()
                }
            )
        }

    }
}

/** La barra del navegador a la vista: salir, qué hacer si no entra solo y buscar a mano. */
@Composable
private fun BarraSeneca(
    contenedor: ContenedorWeb,
    alExtraer: (String?, String?, Boolean) -> Boolean,
    alCerrar: () -> Unit
) {
    CabeceraPantalla(
        titulo = "Séneca",
        subtitulo = "Si no entra sola: menú (☰) → Faltas de asistencia",
        alVolver = alCerrar
    ) {
        IconButton(
            onClick = {
                val web = contenedor.web
                web?.evaluateJavascript(ExtractorFaltas.GUION) {
                    alExtraer(it, web.url, true)
                }
            }
        ) {
            Icon(Icons.Default.Search, contentDescription = "Buscar la tabla en esta página")
        }
    }
}

@Composable
private fun AvisoSinTabla(diagnostico: List<String>) {
    Aviso(
        titulo = "En esta página no hay ninguna tabla de faltas",
        texto = "Abre el menú (☰), entra en Seguimiento del curso → Faltas de asistencia y " +
            "vuelve a pulsar la lupa." +
            if (diagnostico.isEmpty()) "" else "\nTablas vistas: " + diagnostico.joinToString(" / "),
        tono = Tono.AVISO,
        modifier = Modifier.padding(horizontal = Espacio.lateral, vertical = Espacio.s)
    )
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
            // Sin mensaje de error de Séneca lo más probable no es que la cuenta sea mala,
            // sino que el envío no haya llegado: conviene decir las dos cosas.
            alAnotarAcceso(
                "enviada $MAX_ENVIOS veces y sigue pidiéndola" +
                    if (visto?.propio == true) "" else " (campo de usuario suelto)"
            )
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

    // La contraseña solo va a páginas de la Junta: se mira antes de meterla en ningún guion.
    if (!AutoAcceso.hostPermitido(web.url)) {
        val host = runCatching { java.net.URI(web.url).host }.getOrNull() ?: "desconocida"
        alAnotarAcceso("la página ($host) no es de la Junta, así que no se escribe nada")
        alRendirse()
        return
    }

    web.evaluateJavascript(AutoAcceso.guion(cuenta.usuario, cuenta.clave)) { respuesta ->
        val resultado = RespuestaJs.leerAcceso(respuesta)
        when {
            resultado?.enviado == true -> {
                contenedor.envios++
                alAnotarAcceso(
                    "datos enviados por " + resultado.via.ifBlank { "?" } +
                        " (intento " + contenedor.envios + ")"
                )
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
