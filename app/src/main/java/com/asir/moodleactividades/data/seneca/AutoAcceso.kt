package com.asir.moodleactividades.data.seneca

/**
 * Séneca caduca la sesión por su cuenta —lo dice él mismo: «se ha agotado el tiempo de
 * sesión»—, así que ninguna cookie guardada evita volver a entrar. Este guion hace lo que
 * haría el alumno: cerrar el aviso, rellenar el formulario y enviarlo.
 *
 * Los datos no se pegan en el guion tal cual, sino codificados como JSON, para que una
 * comilla o una barra en la contraseña no rompan el código ni se escapen del literal.
 */
object AutoAcceso {

    fun guion(usuario: String, clave: String): String = """
        (function() {
          var usuario = ${comoLiteral(usuario)};
          var clave = ${comoLiteral(clave)};

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

          function visible(elemento) {
            try {
              var caja = elemento.getBoundingClientRect();
              return caja.width > 0 && caja.height > 0;
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

          /** El aviso de sesión caducada tapa el formulario hasta que se cierra. */
          function cerrarAviso(doc) {
            var nodos;
            try { nodos = doc.querySelectorAll('button, a, span, div, input'); } catch (e) { return false; }
            for (var i = 0; i < nodos.length; i++) {
              var texto = limpio(nodos[i]);
              var valor = (nodos[i].value || '').toLowerCase();
              if (texto !== 'cerrar' && valor !== 'cerrar') continue;
              if (!visible(nodos[i])) continue;
              nodos[i].click();
              return true;
            }
            return false;
          }

          function campos(doc) {
            var clave = null;
            var usuarioCampo = null;
            var entradas;
            try { entradas = doc.getElementsByTagName('input'); } catch (e) { return null; }

            for (var i = 0; i < entradas.length; i++) {
              var entrada = entradas[i];
              if (!visible(entrada)) continue;
              var tipo = (entrada.type || '').toLowerCase();
              if (tipo === 'password' && !clave) clave = entrada;
              // El de usuario es el campo de texto que va justo antes del de contraseña.
              if ((tipo === 'text' || tipo === 'email') && !clave && !usuarioCampo) {
                usuarioCampo = entrada;
              }
            }
            if (!clave || !usuarioCampo) return null;
            return { usuario: usuarioCampo, clave: clave };
          }

          function enviar(doc, par) {
            // Primero un botón de verdad: el formulario puede llevar validación propia.
            var botones;
            try {
              botones = doc.querySelectorAll('input[type=submit], button, input[type=button]');
            } catch (e) { botones = []; }
            for (var i = 0; i < botones.length; i++) {
              if (!visible(botones[i])) continue;
              var pista = (limpio(botones[i]) + ' ' + (botones[i].value || '')).toLowerCase();
              if (!/entrar|acceder|iniciar|enviar|login/.test(pista)) continue;
              botones[i].click();
              return true;
            }
            var formulario = par.clave.form;
            if (formulario) {
              formulario.submit();
              return true;
            }
            return false;
          }

          var docs = documentos();
          var d;

          for (d = 0; d < docs.length; d++) {
            if (cerrarAviso(docs[d])) {
              return JSON.stringify({ accion: 'aviso' });
            }
          }

          for (d = 0; d < docs.length; d++) {
            var par = campos(docs[d]);
            if (!par) continue;
            escribir(par.usuario, usuario);
            escribir(par.clave, clave);
            if (enviar(docs[d], par)) return JSON.stringify({ accion: 'enviado' });
            return JSON.stringify({ accion: 'sin-boton' });
          }

          return JSON.stringify({ accion: '' });
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
