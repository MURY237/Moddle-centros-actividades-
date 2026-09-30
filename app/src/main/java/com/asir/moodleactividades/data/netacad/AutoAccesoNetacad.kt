package com.asir.moodleactividades.data.netacad

import com.asir.moodleactividades.data.seneca.AutoAcceso
import java.net.URI
import kotlinx.serialization.Serializable

@Serializable
data class ResultadoAccesoNetacad(
    /** «correo», «clave» o vacío si no había nada que rellenar todavía. */
    val accion: String = "",
    /** Por dónde se mandó: «boton», «formulario» o «intro». */
    val via: String = "",
    /** Cisco dice que el correo o la contraseña no son correctos. */
    val error: Boolean = false,
    /** Cisco pide un segundo paso (código, notificación…): eso lo tiene que hacer el alumno. */
    val mfa: Boolean = false,
    /** La página no es de Cisco: no se ha escrito nada. */
    val ajena: Boolean = false,
    val host: String = ""
)

/**
 * Entra solo en Cisco cuando la sesión de NetAcad caduca, con la cuenta que el alumno
 * guardó cifrada en el móvil. El acceso de Cisco va en dos pasos —primero el correo,
 * luego la contraseña—, así que el guion hace el que toque según lo que haya en pantalla
 * y la app lo vuelve a llamar hasta que la página deja de pedir acceso.
 *
 * Lo que más importa aquí es a quién se le da la contraseña: solo a páginas de cisco.com o
 * netacad.com. Se comprueba en Kotlin antes de inyectar nada ([hostPermitido]) y otra vez
 * dentro del guion, por si la página cambió entre medias.
 */
object AutoAccesoNetacad {

    /** cisco.com, netacad.com y sus subdominios; «ciscoalgo.com» o «cisco.com.otro.net», no. */
    fun hostPermitido(url: String?): Boolean {
        val host = runCatching { URI(url ?: return false).host }.getOrNull()?.lowercase() ?: return false
        if (runCatching { URI(url).scheme }.getOrNull()?.lowercase() != "https") return false
        return DOMINIOS.any { host == it || host.endsWith(".$it") }
    }

    private val DOMINIOS = listOf("cisco.com", "netacad.com")

    fun guion(correo: String, clave: String): String =
        cuerpo(AutoAcceso.comoLiteral(correo), AutoAcceso.comoLiteral(clave))

    private fun cuerpo(correo: String, clave: String): String = """
        (function() {
          var correo = $correo;
          var clave = $clave;
          var PERMITIDOS = /(^|\.)(cisco\.com|netacad\.com)${'$'}/i;

          function limpio(elemento) {
            return (elemento && elemento.textContent || '').replace(/\s+/g, ' ').trim();
          }

          function clave2(texto) {
            return (texto || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
          }

          function documentos() {
            var lista = [document];
            for (var nivel = 0; nivel < lista.length && nivel < 12; nivel++) {
              var marcos = [];
              try {
                marcos = [].concat(
                  [].slice.call(lista[nivel].getElementsByTagName('iframe')),
                  [].slice.call(lista[nivel].getElementsByTagName('frame'))
                );
              } catch (e) { marcos = []; }
              for (var m = 0; m < marcos.length; m++) {
                try {
                  var dentro = marcos[m].contentDocument;
                  if (dentro && lista.indexOf(dentro) < 0) lista.push(dentro);
                } catch (e) { /* de otro origen: ni se ve ni se toca */ }
              }
            }
            return lista;
          }

          function hostDe(doc) {
            try { return (doc.location && doc.location.hostname || '').toLowerCase(); } catch (e) { return ''; }
          }

          function visible(elemento) {
            try {
              if (elemento.disabled || elemento.hidden) return false;
              var caja = elemento.getBoundingClientRect();
              if (caja.width > 0 && caja.height > 0) return true;
              if (elemento.offsetParent) return true;
              var estilo = elemento.ownerDocument.defaultView.getComputedStyle(elemento);
              return estilo.display !== 'none' && estilo.visibility !== 'hidden';
            } catch (e) { return true; }
          }

          function atributos(elemento) {
            var nombres = ['id', 'name', 'class', 'aria-label', 'title', 'placeholder', 'autocomplete', 'value', 'data-se', 'type'];
            var junto = '';
            for (var i = 0; i < nombres.length; i++) {
              try { var v = elemento.getAttribute(nombres[i]); if (v) junto += ' ' + v; } catch (e) {}
            }
            return clave2(junto);
          }

          /**
           * La página de Cisco está hecha con un framework que guarda el valor del campo
           * por su cuenta: escribir en .value no le llega y el formulario sale vacío. Con el
           * «setter» nativo y el evento «input», el framework se entera como si se tecleara.
           */
          function escribir(campo, texto) {
            try { campo.focus(); } catch (e) {}
            var ventana = campo.ownerDocument.defaultView;
            var descriptor = null;
            try { descriptor = Object.getOwnPropertyDescriptor(ventana.HTMLInputElement.prototype, 'value'); } catch (e) {}
            if (descriptor && descriptor.set) descriptor.set.call(campo, texto); else campo.value = texto;
            ['input', 'change', 'keyup', 'blur'].forEach(function (nombre) {
              try {
                campo.dispatchEvent(new ventana.Event(nombre, { bubbles: true }));
              } catch (e) {
                try {
                  var evento = campo.ownerDocument.createEvent('HTMLEvents');
                  evento.initEvent(nombre, true, true);
                  campo.dispatchEvent(evento);
                } catch (e2) {}
              }
            });
          }

          function entradas(doc) {
            try { return [].slice.call(doc.getElementsByTagName('input')); } catch (e) { return []; }
          }

          function campoClave(doc) {
            var lista = entradas(doc);
            for (var i = 0; i < lista.length; i++) {
              if ((lista[i].type || '').toLowerCase() === 'password' && visible(lista[i])) return lista[i];
            }
            return null;
          }

          function campoCorreo(doc) {
            var lista = entradas(doc);
            var suplente = null;
            for (var i = 0; i < lista.length; i++) {
              var entrada = lista[i];
              var tipo = (entrada.type || 'text').toLowerCase();
              if (tipo !== 'email' && tipo !== 'text') continue;
              if (!visible(entrada) || entrada.readOnly) continue;
              var pistas = atributos(entrada);
              if (/search|buscar|busqueda|filtro/.test(pistas)) continue;
              if (tipo === 'email' || /identifier|email|e-mail|correo|user|usuario|login|username/.test(pistas)) return entrada;
              if (!suplente) suplente = entrada;
            }
            return suplente;
          }

          // Lo que nunca se pulsa, aunque diga «login»: volver, recuperar la contraseña o
          // entrar con otra cuenta (Google, Apple…) llevarían a otro sitio.
          var PROHIBIDO = /back|atras|volver|reset|forgot|olvid|setup|recuper|google|apple|microsoft|facebook|linkedin|github|sso|sign ?up|registr|crear|create|cancel|idioma|language|english|espanol/;
          var ADELANTE = /next|siguiente|continu|log ?in|sign ?in|iniciar|entrar|acceder|verify|verific|submit|enviar|aceptar/;

          function boton(doc, campo) {
            var sitio = (campo && campo.form) || doc;
            var nodos;
            try {
              nodos = sitio.querySelectorAll('button, input[type=submit], input[type=button], [role=button], a');
            } catch (e) { return null; }
            for (var i = 0; i < nodos.length; i++) {
              var nodo = nodos[i];
              if (!visible(nodo)) continue;
              var texto = clave2(limpio(nodo)) + ' ' + atributos(nodo);
              if (PROHIBIDO.test(texto)) continue;
              if (ADELANTE.test(texto)) return nodo;
            }
            // Un formulario con un único botón de enviar no necesita que diga nada.
            if (campo && campo.form) {
              var envio = campo.form.querySelector('button[type=submit], input[type=submit]');
              if (envio && visible(envio) && !PROHIBIDO.test(clave2(limpio(envio)) + ' ' + atributos(envio))) return envio;
            }
            return null;
          }

          function intro(campo) {
            ['keydown', 'keypress', 'keyup'].forEach(function (nombre) {
              try {
                var ventana = campo.ownerDocument.defaultView;
                campo.dispatchEvent(new ventana.KeyboardEvent(nombre, { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true }));
              } catch (e) {}
            });
          }

          function enviar(doc, campo) {
            var pulsable = boton(doc, campo);
            if (pulsable) { pulsable.click(); return 'boton'; }
            if (campo.form && campo.form.requestSubmit) {
              try { campo.form.requestSubmit(); return 'formulario'; } catch (e) {}
            }
            intro(campo);
            return 'intro';
          }

          /** El aviso de Cisco cuando la cuenta no vale: la única razón para dejar de intentarlo. */
          function hayError(doc) {
            var nodos;
            try {
              nodos = doc.querySelectorAll('[role=alert], [aria-live], [class*=error], [class*=Error], [class*=alert], [class*=invalid]');
            } catch (e) { return false; }
            for (var i = 0; i < nodos.length; i++) {
              if (!visible(nodos[i])) continue;
              var texto = clave2(limpio(nodos[i]));
              if (!texto) continue;
              if (/incorrect|invalid|wrong|not valid|unable to sign in|could not sign|failed|no (es|son) (valid|correct)|no coincide|erron|bloquead|locked/.test(texto)) return true;
            }
            return false;
          }

          /** Código, notificación en el móvil o autenticador: eso no lo puede hacer la app. */
          function pideMfa(doc) {
            var lista = entradas(doc);
            for (var i = 0; i < lista.length; i++) {
              var entrada = lista[i];
              if ((entrada.type || '').toLowerCase() === 'password' || !visible(entrada)) continue;
              if (/one-time-code|otp|verification|verificacion|mfa|totp|passcode/.test(atributos(entrada))) return true;
            }
            try {
              var cuerpo = clave2(limpio(doc.body));
              return /okta verify|push notification|notificacion push|verification code|codigo de verificacion|authenticator app|aplicacion de autenticacion|two-factor|dos pasos|2fa/.test(cuerpo);
            } catch (e) { return false; }
          }

          var docs = documentos();
          for (var d = 0; d < docs.length; d++) {
            var doc = docs[d];
            var host = hostDe(doc);
            if (!PERMITIDOS.test(host)) {
              // Una página que no es de Cisco no recibe nada, ni el correo.
              if (campoClave(doc) || campoCorreo(doc)) return JSON.stringify({ ajena: true, host: host });
              continue;
            }
            if (hayError(doc)) return JSON.stringify({ error: true, host: host });
            if (pideMfa(doc)) return JSON.stringify({ mfa: true, host: host });

            var campo = campoClave(doc);
            if (campo) {
              // Si el correo está en el mismo formulario y vacío, se rellena también.
              var delCorreo = campoCorreo(doc);
              if (delCorreo && delCorreo.form === campo.form && !delCorreo.value) escribir(delCorreo, correo);
              escribir(campo, clave);
              return JSON.stringify({ accion: 'clave', via: enviar(doc, campo), host: host });
            }

            var correoCampo = campoCorreo(doc);
            if (correoCampo) {
              escribir(correoCampo, correo);
              return JSON.stringify({ accion: 'correo', via: enviar(doc, correoCampo), host: host });
            }
          }
          return JSON.stringify({ accion: '', host: docs.length ? hostDe(docs[0]) : '' });
        })();
    """.trimIndent()
}
