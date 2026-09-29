package com.asir.moodleactividades.data.netacad

/**
 * Cisco no publica ninguna API de NetAcad para alumnos, así que la única vía es la misma que
 * con Séneca: leer la página dentro de la sesión que el propio alumno ha abierto. Este guion
 * se inyecta en esa página y devuelve el texto de lo que parecen trabajos con plazo.
 *
 * Solo recoge texto: no interpreta fechas ni decide estados. Eso lo hace [LectorNetacad] en
 * Kotlin, donde se puede probar. Cuanto más tonto sea este guion, menos se puede romper
 * cuando Cisco retoque la maquetación.
 *
 * Busca por cabeceras, atributos y texto, nunca por identificadores internos: la web de
 * NetAcad es una aplicación de una sola página y esos nombres cambian con cada versión.
 * Tampoco toca credenciales ni pulsa nada: solo lee.
 */
object ExtractorNetacad {

    val GUION: String = """
        (function() {
          /**
           * Junta el texto de cada nodo con un espacio. textContent pega los elementos tal
           * cual: «<span>Nov 10</span><span>Passed</span><span>18/20</span>» salía como
           * «Nov 10Passed18/20», y ni la nota ni el estado se podían separar de la fecha.
           */
          function limpio(elemento) {
            if (!elemento) return '';
            try {
              var paseo = elemento.ownerDocument.createTreeWalker(elemento, 4, null, false);
              var partes = [];
              var nodo;
              while ((nodo = paseo.nextNode())) partes.push(nodo.nodeValue);
              return partes.join(' ').replace(/\s+/g, ' ').trim();
            } catch (e) {
              return (elemento.textContent || '').replace(/\s+/g, ' ').trim();
            }
          }

          /** Minúsculas y sin tildes, para comparar sin depender del idioma del curso. */
          function clave(texto) {
            return (texto || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
          }

          // El contenido de los cursos se abre a veces dentro de un marco: hay que mirarlos todos.
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

          /** Visible es «no escondido a propósito»: la caja a cero no basta para descartar. */
          function visible(elemento) {
            try {
              var caja = elemento.getBoundingClientRect();
              if (caja.width > 0 && caja.height > 0) return true;
              if (elemento.offsetParent) return true;
              var ventana = elemento.ownerDocument.defaultView;
              var estilo = ventana.getComputedStyle(elemento);
              if (estilo.display === 'none' || estilo.visibility === 'hidden') return false;
              return !elemento.hidden && elemento.getAttribute('aria-hidden') !== 'true';
            } catch (e) { return true; }
          }

          var MESES = 'enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|' +
            'octubre|noviembre|diciembre|january|february|march|april|june|july|august|' +
            'september|october|november|december|ene|jan|feb|mar|abr|apr|may|jun|jul|ago|aug|' +
            'sept|sep|set|oct|nov|dic|dec';
          var RE_FECHA = new RegExp(
            '(\\d{4}-\\d{1,2}-\\d{1,2})' +
            '|(\\d{1,2}[/.\\-]\\d{1,2}[/.\\-]\\d{2,4})' +
            '|(\\d{1,2}\\s*(?:de\\s+)?\\b(?:' + MESES + ')\\b)' +
            '|(\\b(?:' + MESES + ')\\b\\.?\\s+\\d{1,2})' +
            '|(\\b(?:hoy|today|manana|tomorrow|ayer|yesterday)\\b)'
          );
          // Con dos fechas en la misma tarjeta, la buena es la que va tras «vence» o «due»:
          // la otra suele ser la de apertura, y tomarla adelantaba el plazo semanas.
          var RE_LIMITE = /(fecha limite|fecha de entrega|vencimiento|vence|due|deadline|entrega|hasta|until|cierra|closes?)[^0-9a-z]{0,6}/;
          var RE_ESTADO = /no entregad|sin entregar|not submitted|sin completar|complet|finalizad|terminad|entregad|submitted|\bdone\b|passed|aprobad|superad|not started|sin empezar|sin comenzar|no iniciad|in progress|en curso|en progreso|pendiente|incomplet|overdue|vencid|fuera de plazo/;
          var RE_NOTA = /\d{1,3}(?:[.,]\d+)?\s*%|\b\d{1,3}(?:[.,]\d+)?\s*\/\s*\d{1,3}\b/;

          function fechaDe(k) {
            var limite = RE_LIMITE.exec(k);
            if (limite) {
              var resto = k.substr(limite.index + limite[0].length, 60);
              var tras = RE_FECHA.exec(resto);
              if (tras) return resto.substr(tras.index, 40);
            }
            var primera = RE_FECHA.exec(k);
            return primera ? k.substr(primera.index, 40) : '';
          }

          function estadoDe(k) {
            var e = RE_ESTADO.exec(k);
            return e ? k.substr(e.index, 30) : '';
          }

          function notaDe(texto) {
            var n = RE_NOTA.exec(texto);
            return n ? n[0].replace(/\s+/g, '') : '';
          }

          function enlaceDe(elemento) {
            try {
              var a = elemento.tagName === 'A' ? elemento : elemento.querySelector('a[href]');
              if (!a) return '';
              var destino = a.href || '';
              return /^https?:/i.test(destino) ? destino : '';
            } catch (e) { return ''; }
          }

          function pareceSoloFechaOEstado(texto) {
            var k = clave(texto);
            var sinFecha = k.replace(RE_FECHA, '').replace(RE_LIMITE, '');
            var sinEstado = sinFecha.replace(RE_ESTADO, '').replace(/[^a-z]/g, '');
            return sinEstado.length < 3;
          }

          /**
           * El título es un encabezado, un enlace o algo que se llame «title» o «name». No se
           * cae nunca al texto del propio nodo: el nodo más pequeño con fecha suele ser el
           * propio «Due: Dec 5», y así se colaba la fecha como si fuera el nombre del trabajo.
           */
          function tituloDe(elemento) {
            var selectores = [
              'h1, h2, h3, h4, h5, h6',
              '[class*=title], [class*=Title], [class*=name], [class*=Name]',
              '[role=heading]',
              'a[href]',
              'strong, b'
            ];
            for (var s = 0; s < selectores.length; s++) {
              var nodos;
              try { nodos = elemento.querySelectorAll(selectores[s]); } catch (e) { continue; }
              for (var i = 0; i < nodos.length; i++) {
                var texto = limpio(nodos[i]);
                if (texto.length < 3 || texto.length > 160) continue;
                if (pareceSoloFechaOEstado(texto)) continue;
                return texto;
              }
            }
            return '';
          }

          // En el panel, el primer título suele ser el saludo con el nombre del alumno: eso
          // no es un curso, y usarlo como tal lo mezclaba todo bajo «Hola, …».
          var RE_SALUDO = /^(hola|hi|hello|hey|welcome|bienvenid|buenos|buenas|good (morning|afternoon|evening))\b/i;

          function cursoDe(doc) {
            var pistas = [
              '[class*=breadcrumb] li:last-child', '[class*=breadcrumb] a:last-child',
              '[class*=course-title]', '[class*=courseTitle]', '[class*=course-name]',
              '[class*=courseName]', 'h1'
            ];
            for (var p = 0; p < pistas.length; p++) {
              var nodo;
              try { nodo = doc.querySelector(pistas[p]); } catch (e) { continue; }
              if (!nodo) continue;
              var texto = limpio(nodo);
              if (RE_SALUDO.test(texto)) continue;
              if (texto.length >= 3 && texto.length <= 120) return texto;
            }
            var titulo = (doc.title || '').split(/\s[|\-–]\s/)[0].trim();
            return /netacad|networking academy/i.test(titulo) ? '' : titulo;
          }

          var PISTAS_COLUMNA = {
            titulo: /actividad|activity|assignment|assessment|tarea|trabajo|examen|exam|quiz|cuestionario|evaluacion|nombre|name|title|titulo|item|elemento/,
            fecha: /due|vence|vencimiento|fecha|entrega|deadline|plazo|limite|date|cierre|close/,
            estado: /status|estado|progress|progreso|situacion|completion|completado/,
            nota: /score|grade|nota|calific|puntuacion|points|puntos|resultado|result|%/,
            curso: /course|curso|modulo|module|clase|class/
          };

          function columna(cabeceras, tipo, usadas) {
            for (var i = 0; i < cabeceras.length; i++) {
              if (usadas.indexOf(i) >= 0) continue;
              if (PISTAS_COLUMNA[tipo].test(cabeceras[i])) return i;
            }
            return -1;
          }

          /** Filas de una tabla corriente o de una rejilla hecha con divs y roles ARIA. */
          function rejillas(doc) {
            var halladas = [];
            var tablas;
            try { tablas = doc.getElementsByTagName('table'); } catch (e) { tablas = []; }
            for (var t = 0; t < tablas.length; t++) {
              var filas = [];
              for (var f = 0; f < tablas[t].rows.length; f++) {
                filas.push([].slice.call(tablas[t].rows[f].cells));
              }
              halladas.push(filas);
            }
            var aria;
            try { aria = doc.querySelectorAll('[role=table], [role=grid], [role=treegrid]'); } catch (e) { aria = []; }
            for (var a = 0; a < aria.length; a++) {
              if (aria[a].tagName === 'TABLE') continue;
              var filasAria = [];
              var nodos = aria[a].querySelectorAll('[role=row]');
              for (var r = 0; r < nodos.length; r++) {
                filasAria.push([].slice.call(
                  nodos[r].querySelectorAll('[role=columnheader], [role=cell], [role=gridcell], [role=rowheader]')
                ));
              }
              halladas.push(filasAria);
            }
            return halladas;
          }

          var pistasVistas = [];

          function celdaDe(celdas, i) {
            return i >= 0 && celdas.length > i ? limpio(celdas[i]) : '';
          }

          function deRejillas(doc, curso) {
            var salida = [];
            var todas = rejillas(doc);
            for (var g = 0; g < todas.length; g++) {
              var filas = todas[g];
              if (filas.length < 2 || filas[0].length < 2) continue;

              var cabeceras = filas[0].map(function (celda) { return clave(limpio(celda)); });
              var usadas = [];
              var iFecha = columna(cabeceras, 'fecha', usadas); if (iFecha >= 0) usadas.push(iFecha);
              var iEstado = columna(cabeceras, 'estado', usadas); if (iEstado >= 0) usadas.push(iEstado);
              var iNota = columna(cabeceras, 'nota', usadas); if (iNota >= 0) usadas.push(iNota);

              if (iFecha < 0 && iEstado < 0 && iNota < 0) {
                pistasVistas.push(cabeceras.join(' | ').substr(0, 200));
                continue;
              }

              // El título se reparte antes que el curso: una cabecera como «Módulo 1-3»
              // casaba con «curso», se quedaba la única columna libre y la tabla entera se
              // descartaba por no tener nombre.
              var iTitulo = columna(cabeceras, 'titulo', usadas);
              if (iTitulo < 0) {
                // Sin columna de nombre reconocible, la primera libre suele ser el título.
                for (var libre = 0; libre < cabeceras.length; libre++) {
                  if (usadas.indexOf(libre) < 0) { iTitulo = libre; break; }
                }
              }
              if (iTitulo < 0) continue;
              usadas.push(iTitulo);
              var iCurso = columna(cabeceras, 'curso', usadas);

              for (var f = 1; f < filas.length; f++) {
                var celdas = filas[f];
                if (celdas.length <= iTitulo) continue;
                var titulo = limpio(celdas[iTitulo]);
                if (!titulo) continue;
                var textoFecha = celdaDe(celdas, iFecha);
                var textoNota = celdaDe(celdas, iNota);
                salida.push({
                  titulo: titulo,
                  curso: celdaDe(celdas, iCurso) || curso,
                  fecha: textoFecha ? (fechaDe(clave(textoFecha)) || clave(textoFecha)) : '',
                  estado: celdaDe(celdas, iEstado),
                  nota: textoNota ? (notaDe(textoNota) || textoNota) : '',
                  url: enlaceDe(celdas[iTitulo])
                });
              }
            }
            return salida;
          }

          /**
           * El resumen de «próximos plazos» del panel mezcla trabajos de varios cursos, y cada
           * tarjeta dice el suyo. Con el curso de la página todos acababan en el mismo.
           */
          function cursoPropio(nodo, titulo) {
            var marcas;
            try { marcas = nodo.querySelectorAll('[class*=course], [class*=Course], [class*=curso]'); } catch (e) { return ''; }
            for (var i = 0; i < marcas.length; i++) {
              var texto = limpio(marcas[i]);
              if (texto.length < 3 || texto.length > 120 || texto === titulo) continue;
              if (pareceSoloFechaOEstado(texto)) continue;
              return texto;
            }
            return '';
          }

          function deTarjetas(doc, curso) {
            var nodos;
            try {
              nodos = doc.querySelectorAll(
                'li, article, [role=listitem], [role=row], [class*=card], [class*=Card], ' +
                '[class*=item], [class*=Item], [class*=assign], [class*=Assign], ' +
                '[class*=activit], [class*=Activit], [class*=task], [class*=Task], ' +
                '[class*=assessment], [class*=Assessment]'
              );
            } catch (e) { return []; }

            var hallados = [];
            for (var i = 0; i < nodos.length; i++) {
              var nodo = nodos[i];
              if (nodo.closest && nodo.closest('table, nav, header, footer')) continue;
              if (!visible(nodo)) continue;
              var texto = limpio(nodo);
              if (texto.length < 6 || texto.length > 500) continue;
              var k = clave(texto);
              var fecha = fechaDe(k);
              var estado = estadoDe(k);
              if (!fecha && !estado) continue;
              var titulo = tituloDe(nodo);
              if (!titulo) continue;
              hallados.push({
                nodo: nodo,
                datos: {
                  titulo: titulo, curso: cursoPropio(nodo, titulo) || curso, fecha: fecha, estado: estado,
                  nota: notaDe(texto), url: enlaceDe(nodo)
                }
              });
            }

            // Una tarjeta vive dentro de una lista que también «tiene» fecha y título: se
            // queda solo el nodo más interior de cada uno, que es la tarjeta de verdad.
            return hallados
              .filter(function (h) {
                return !hallados.some(function (otro) {
                  return otro !== h && h.nodo.contains(otro.nodo);
                });
              })
              .map(function (h) { return h.datos; });
          }

          /**
           * El acceso de Cisco va en dos pasos: primero solo el correo y luego la contraseña.
           * Por eso no basta con buscar un campo de contraseña; también cuenta estar en el
           * dominio de identidad de Cisco o en una ruta de entrada.
           */
          function pideAcceso(doc) {
            try {
              var sitio = (doc.location && (doc.location.hostname + doc.location.pathname) || '').toLowerCase();
              if (/id\.cisco\.com|sso\.|okta|\/login|\/signin|\/sign-in|\/oauth|\/authorize/.test(sitio)) return true;
            } catch (e) { /* sin ubicación legible */ }
            var entradas;
            try { entradas = doc.getElementsByTagName('input'); } catch (e) { return false; }
            for (var i = 0; i < entradas.length; i++) {
              if ((entradas[i].type || '').toLowerCase() !== 'password') continue;
              if (visible(entradas[i])) return true;
            }
            return false;
          }

          function cargando(doc) {
            try {
              if (doc.readyState && doc.readyState !== 'complete') return true;
              var cuerpo = limpio(doc.body);
              if (cuerpo.length < 40) return true;
              var ruedas = doc.querySelectorAll('[class*=spinner], [class*=loading], [class*=loader], [aria-busy=true]');
              for (var i = 0; i < ruedas.length; i++) if (visible(ruedas[i])) return true;
            } catch (e) { /* se da por cargado */ }
            return false;
          }

          function encabezados(doc) {
            var vistos = [];
            try {
              var nodos = doc.querySelectorAll('h1, h2, h3, [role=heading]');
              for (var i = 0; i < nodos.length && vistos.length < 12; i++) {
                var texto = limpio(nodos[i]);
                // Nada que parezca un correo: el diagnóstico no debe llevar datos personales.
                if (texto.length >= 3 && texto.length <= 80 && texto.indexOf('@') < 0) vistos.push(texto);
              }
            } catch (e) { /* sin encabezados */ }
            return vistos;
          }

          var docs = documentos();
          var d;

          for (d = 0; d < docs.length; d++) {
            if (pideAcceso(docs[d])) return JSON.stringify({ pagina: 'acceso' });
          }

          var candidatos = [];
          for (d = 0; d < docs.length; d++) {
            var curso = cursoDe(docs[d]);
            candidatos = candidatos.concat(deRejillas(docs[d], curso));
          }
          // Las tarjetas solo si no hubo tablas: la tabla es más fiable y repetirla sobra.
          if (candidatos.length === 0) {
            for (d = 0; d < docs.length; d++) {
              candidatos = candidatos.concat(deTarjetas(docs[d], cursoDe(docs[d])));
            }
          }

          if (candidatos.length > 0) {
            return JSON.stringify({ pagina: 'trabajos', candidatos: candidatos.slice(0, 400) });
          }

          for (d = 0; d < docs.length; d++) {
            if (cargando(docs[d])) return JSON.stringify({ pagina: 'cargando' });
          }

          var pistas = pistasVistas.slice(0, 8);
          for (d = 0; d < docs.length && pistas.length < 20; d++) {
            pistas = pistas.concat(encabezados(docs[d]));
          }
          return JSON.stringify({ pagina: 'desconocida', pistas: pistas.slice(0, 20) });
        })();
    """.trimIndent()
}
