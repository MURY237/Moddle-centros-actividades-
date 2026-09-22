package com.asir.moodleactividades.data.seneca

/**
 * Séneca caduca la sesión por su cuenta —lo dice él mismo: «se ha agotado el tiempo de
 * sesión»—, así que ninguna cookie guardada evita volver a entrar. Este guion hace lo que
 * haría el alumno: rellenar el formulario y enviarlo, o cerrar el aviso que lo tapa.
 *
 * Los datos no se pegan en el guion tal cual, sino codificados como JSON, para que una
 * comilla o una barra en la contraseña no rompan el código ni se escapen del literal.
 */
object AutoAcceso {

    fun guion(usuario: String, clave: String): String =
        cuerpo(comoLiteral(usuario), comoLiteral(clave), actuar = true)

    /**
     * El mismo reconocimiento, pero sin tocar nada ni llevar la contraseña encima. Sirve
     * para saber si lo que hay delante es de verdad el formulario de acceso antes de dar
     * por mala una contraseña: acusarla sin motivo obliga al alumno a escribirla otra vez.
     */
    val SONDEO: String = cuerpo("\"\"", "\"\"", actuar = false)

    private fun cuerpo(usuario: String, clave: String, actuar: Boolean): String = """
        (function() {
          var usuario = $usuario;
          var clave = $clave;
          var actuar = $actuar;

          function limpio(elemento) {
            var texto = (elemento.textContent || '').replace(/\s+/g, ' ').trim().toLowerCase();
            return texto.normalize('NFD').replace(/[̀-ͯ]/g, '');
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
                } catch (e) { /* de otro origen */ }
              }
            }
            return lista;
          }

          /**
           * Visible es «no escondido a propósito». La caja a cero no basta para descartar:
           * un navegador estrecho deja a cero cosas que están perfectamente en la página, y
           * con el criterio antiguo no se encontraba ni el formulario ni el menú.
           */
          function visible(elemento) {
            try {
              var caja = elemento.getBoundingClientRect();
              if (caja.width > 0 && caja.height > 0) return true;
              if (elemento.offsetParent) return true;
              if (elemento.getClientRects && elemento.getClientRects().length > 0) return true;
              var ventana = elemento.ownerDocument.defaultView;
              var estilo = ventana.getComputedStyle(elemento);
              return estilo.display !== 'none' && estilo.visibility !== 'hidden';
            } catch (e) { return true; }
          }

          // Escribir en .value no basta: muchas páginas solo se enteran por los eventos.
          function escribir(campo, texto) {
            campo.focus();
            campo.value = texto;
            ['input', 'change', 'keyup'].forEach(function (nombre) {
              try {
                var evento = campo.ownerDocument.createEvent('HTMLEvents');
                evento.initEvent(nombre, true, true);
                campo.dispatchEvent(evento);
              } catch (e) { /* el navegador no deja crearlo: se sigue igual */ }
            });
          }

          function atributos(elemento) {
            var nombres = ['id', 'class', 'aria-label', 'title', 'alt', 'name', 'value'];
            var junto = '';
            for (var i = 0; i < nombres.length; i++) {
              try {
                var valor = elemento.getAttribute(nombres[i]);
                if (valor) junto += ' ' + valor;
              } catch (e) { /* hay nodos sin atributos */ }
            }
            return junto.toLowerCase();
          }

          /** Un texto suelto no se pulsa: para cerrar algo hace falta un control de verdad. */
          function pulsable(elemento) {
            var etiqueta = (elemento.tagName || '').toLowerCase();
            if (etiqueta === 'button' || etiqueta === 'a' || etiqueta === 'input') return true;
            if (etiqueta === 'img' || etiqueta === 'i') return true;
            try {
              if (elemento.getAttribute('onclick')) return true;
              if (elemento.getAttribute('role') === 'button') return true;
            } catch (e) { /* hay nodos sin atributos */ }
            return !!elemento.onclick;
          }

          /**
           * El aspa del aviso de sesión caducada, no la palabra «cerrar» del mensaje: el
           * texto dice «pulse cerrar para iniciar sesión de nuevo», y esa palabra en negrita
           * no es un botón. Pulsarla no hacía nada y el aviso se quedaba puesto para siempre.
           */
          function botonCerrar(doc) {
            var nodos;
            try {
              nodos = doc.querySelectorAll('button, a, input, img, i, span, div');
            } catch (e) { return null; }

            var suplente = null;
            for (var i = 0; i < nodos.length; i++) {
              var nodo = nodos[i];
              if (!visible(nodo)) continue;
              var caja;
              try { caja = nodo.getBoundingClientRect(); } catch (e) { continue; }
              // Un bloque grande es el propio aviso, no su aspa: pulsarlo no lo cierra.
              if (caja.width > 120 || caja.height > 120) continue;

              var pistas = atributos(nodo);
              var texto = limpio(nodo);
              var porAtributo = /close|cerrar|dismiss|aceptar/.test(pistas);
              var esAspa = texto === 'x' || texto === '×' || texto === '✕';

              if ((porAtributo || esAspa) && pulsable(nodo)) return nodo;
              // Si nada resulta pulsable se prueba con esto, que al menos es del aviso.
              if (porAtributo || esAspa) { if (!suplente) suplente = nodo; continue; }
              if (!pulsable(nodo)) continue;
              if (texto === 'cerrar' || texto === 'aceptar' || texto === 'continuar') return nodo;
            }
            return suplente;
          }

          function esBuscador(entrada) {
            var pistas = atributos(entrada) + ' ' +
              (entrada.getAttribute('placeholder') || '').toLowerCase();
            return /buscar|search|filtro|busqueda/.test(pistas);
          }

          /**
           * El campo de usuario es el que acompaña a la contraseña en su mismo formulario, y
           * el último de los que van antes que ella. Antes se cogía el primer campo de texto
           * de toda la página: una caja de búsqueda de la barra de arriba aparece antes en el
           * HTML, así que el usuario se escribía ahí y el de verdad se mandaba vacío. Séneca
           * devolvía el formulario otra vez, sin error, y parecía que la cuenta no valía.
           */
          function campos(doc) {
            var entradas;
            try { entradas = doc.getElementsByTagName('input'); } catch (e) { return null; }

            var clave = null;
            for (var i = 0; i < entradas.length; i++) {
              if ((entradas[i].type || '').toLowerCase() !== 'password') continue;
              if (!visible(entradas[i])) continue;
              clave = entradas[i];
              break;
            }
            if (!clave) return null;

            var candidatas = clave.form ? clave.form.getElementsByTagName('input') : entradas;
            var usuarioCampo = null;
            for (var j = 0; j < candidatas.length; j++) {
              var entrada = candidatas[j];
              // Solo cuenta lo que va antes de la contraseña; lo de después es otra cosa.
              if (entrada === clave) break;
              var tipo = (entrada.type || '').toLowerCase();
              if (tipo !== 'text' && tipo !== 'email' && tipo !== 'tel') continue;
              if (!visible(entrada)) continue;
              if (esBuscador(entrada)) continue;
              usuarioCampo = entrada;
            }
            if (!usuarioCampo) return null;
            return { usuario: usuarioCampo, clave: clave, propio: !!clave.form };
          }

          /** Pulsa Intro en el campo: hay formularios que solo se mandan así. */
          function intro(campo) {
            var nombres = ['keydown', 'keypress', 'keyup'];
            for (var i = 0; i < nombres.length; i++) {
              try {
                var evento = campo.ownerDocument.createEvent('Events');
                evento.initEvent(nombres[i], true, true);
                evento.keyCode = 13;
                evento.which = 13;
                evento.key = 'Enter';
                campo.dispatchEvent(evento);
              } catch (e) { /* el navegador no deja crearlo */ }
            }
          }

          /** Devuelve por dónde se mandó, que es lo que dice si el envío llegó a alguna parte. */
          function enviar(doc, par) {
            // Primero un control de verdad. El botón de Séneca no siempre es un <button>:
            // puede ser un enlace o una imagen, y buscando solo botones no se encontraba.
            var botones;
            try {
              botones = doc.querySelectorAll(
                'input[type=submit], input[type=button], input[type=image], button, a, [role=button]'
              );
            } catch (e) { botones = []; }
            for (var i = 0; i < botones.length; i++) {
              if (!visible(botones[i])) continue;
              var pista = limpio(botones[i]) + ' ' + atributos(botones[i]);
              if (!/entrar|acceder|iniciar|enviar|login|aceptar/.test(pista)) continue;
              botones[i].click();
              return 'boton';
            }

            var formulario = par.clave.form;
            if (formulario) {
              // requestSubmit respeta la validación y el onsubmit del formulario; submit se
              // los salta, y si Séneca prepara algo ahí el acceso se iría sin ello.
              try {
                if (formulario.requestSubmit) { formulario.requestSubmit(); return 'formulario'; }
              } catch (e) { /* se sigue con el clásico */ }
              try { formulario.submit(); return 'formulario-directo'; } catch (e) { /* Intro */ }
            }

            intro(par.clave);
            return 'intro';
          }

          /**
           * Lo que dice Séneca cuando la cuenta no vale. Es la única señal fiable de que la
           * contraseña guardada está mal: cualquier otra cosa es un tropiezo del recorrido.
           */
          function hayError(doc) {
            var texto;
            try { texto = limpio(doc.body || doc.documentElement); } catch (e) { return false; }
            if (texto.indexOf('usuario') < 0 && texto.indexOf('contrasena') < 0) return false;
            return /incorrect|no son correct|erroneo|no es valid|no valid|no coincide/.test(texto);
          }

          var docs = documentos();
          var accion = '';
          var hayFormulario = false;
          var hayAviso = false;
          var hayFallo = false;
          var propio = false;
          var via = '';
          var d;

          // El formulario va primero: si ya está delante, cerrar avisos no hace falta.
          for (d = 0; d < docs.length; d++) {
            var par = campos(docs[d]);
            if (!par) continue;
            hayFormulario = true;
            hayFallo = hayError(docs[d]);
            propio = !!par.propio;
            if (actuar) {
              escribir(par.usuario, usuario);
              escribir(par.clave, clave);
              via = enviar(docs[d], par);
              accion = 'enviado';
            }
            break;
          }

          if (!hayFormulario) {
            for (d = 0; d < docs.length; d++) {
              var aspa = botonCerrar(docs[d]);
              if (!aspa) continue;
              hayAviso = true;
              if (actuar) { aspa.click(); accion = 'aviso'; }
              break;
            }
          }

          return JSON.stringify({
            accion: accion,
            formulario: hayFormulario,
            aviso: hayAviso,
            error: hayFallo,
            propio: propio,
            via: via
          });
        })();
    """.trimIndent()

    /**
     * Codifica el texto como literal de JavaScript. Se escapan también `<` y los saltos de
     * línea de Unicode, que rompen un guion aunque JSON los considere válidos.
     */
    internal fun comoLiteral(texto: String): String {
        val salida = StringBuilder("\"")
        for (caracter in texto) {
            when (caracter) {
                '"' -> salida.append("\\\"")
                '\\' -> salida.append("\\\\")
                '\n' -> salida.append("\\n")
                '\r' -> salida.append("\\r")
                '\t' -> salida.append("\\t")
                '<' -> salida.append("\\u003C")
                '>' -> salida.append("\\u003E")
                '&' -> salida.append("\\u0026")
                ' ' -> salida.append("\\u2028")
                ' ' -> salida.append("\\u2029")
                else ->
                    if (caracter < ' ') {
                        salida.append("\\u").append("%04x".format(caracter.code))
                    } else {
                        salida.append(caracter)
                    }
            }
        }
        return salida.append('"').toString()
    }
}
