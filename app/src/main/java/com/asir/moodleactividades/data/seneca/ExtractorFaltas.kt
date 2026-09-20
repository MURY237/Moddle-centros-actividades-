package com.asir.moodleactividades.data.seneca

/**
 * Séneca no publica ninguna API, pero la página de faltas es una tabla HTML corriente dentro
 * de la sesión que el propio alumno ha abierto. Este guion se inyecta en esa página y
 * devuelve sus filas, sin tocar credenciales ni enviar nada fuera del móvil.
 *
 * Busca la tabla por sus encabezados y no por su posición ni por un identificador: Séneca
 * genera los nombres internos por sesión, así que cualquier otra referencia se rompería sola.
 */
object ExtractorFaltas {

    val GUION: String = """
        (function() {
          function limpio(elemento) {
            return (elemento.textContent || '').replace(/\s+/g, ' ').trim();
          }

          // Séneca monta su interfaz con marcos, y las tablas viven dentro de ellos: mirando
          // solo el documento de arriba no se encontraría nunca nada.
          function documentos() {
            var lista = [document];
            for (var nivel = 0; nivel < lista.length && nivel < 12; nivel++) {
              var actual = lista[nivel];
              var marcos = [];
              try {
                marcos = [].concat(
                  [].slice.call(actual.getElementsByTagName('iframe')),
                  [].slice.call(actual.getElementsByTagName('frame'))
                );
              } catch (e) { marcos = []; }
              for (var m = 0; m < marcos.length; m++) {
                try {
                  var dentro = marcos[m].contentDocument;
                  if (dentro && lista.indexOf(dentro) < 0) lista.push(dentro);
                } catch (e) { /* de otro origen: no es de Séneca */ }
              }
            }
            return lista;
          }

          function indiceDe(cabeceras, pistas) {
            for (var i = 0; i < cabeceras.length; i++) {
              for (var p = 0; p < pistas.length; p++) {
                if (cabeceras[i].indexOf(pistas[p]) >= 0) return i;
              }
            }
            return -1;
          }

          // El desplegable «Mostrar» puede venir acotado; sin ponerlo en «Todas» se leerían
          // solo algunas faltas y el recuento saldría corto.
          function ponerTodas(doc) {
            var listas;
            try { listas = doc.getElementsByTagName('select'); } catch (e) { return false; }
            for (var i = 0; i < listas.length; i++) {
              var lista = listas[i];
              for (var o = 0; o < lista.options.length; o++) {
                if (limpio(lista.options[o]).toLowerCase() !== 'todas') continue;
                if (lista.selectedIndex === o) return false;
                lista.selectedIndex = o;
                try {
                  var evento = doc.createEvent('HTMLEvents');
                  evento.initEvent('change', true, true);
                  lista.dispatchEvent(evento);
                } catch (e) { if (lista.onchange) lista.onchange(); }
                return true;
              }
            }
            return false;
          }

          function esPaginaDeFaltas(doc) {
            try {
              var cuerpo = (doc.body && doc.body.textContent || '').toLowerCase();
              return cuerpo.indexOf('faltas de asistencia') >= 0;
            } catch (e) { return false; }
          }

          var docs = documentos();
          var vistas = [];
          var enFaltas = false;

          for (var d = 0; d < docs.length; d++) {
            if (esPaginaDeFaltas(docs[d])) enFaltas = true;
            if (ponerTodas(docs[d])) {
              // Cambiar el filtro recarga la tabla: hay que volver a mirar dentro de un momento.
              return JSON.stringify({ encontrada: false, ajustado: true });
            }

            var tablas;
            try { tablas = docs[d].getElementsByTagName('table'); } catch (e) { continue; }

            for (var t = 0; t < tablas.length; t++) {
              var filas = tablas[t].rows;
              if (!filas || filas.length < 1) continue;

              var cabeceras = [];
              for (var c = 0; c < filas[0].cells.length; c++) {
                cabeceras.push(limpio(filas[0].cells[c]).toLowerCase());
              }
              if (cabeceras.length === 0) continue;
              vistas.push(cabeceras.join(' | '));

              var iFecha = indiceDe(cabeceras, ['fecha']);
              var iAsignatura = indiceDe(cabeceras, ['ausencia', 'materia', 'asignatura']);
              if (iFecha < 0 || iAsignatura < 0) continue;

              var iTramo = indiceDe(cabeceras, ['tramo', 'hora']);
              var iEstado = indiceDe(cabeceras, ['estado', 'justific']);

              var faltas = [];
              for (var f = 1; f < filas.length; f++) {
                var celdas = filas[f].cells;
                if (!celdas || celdas.length <= iAsignatura) continue;
                var asignatura = limpio(celdas[iAsignatura]);
                if (!asignatura) continue;
                faltas.push({
                  fecha: limpio(celdas[iFecha]),
                  tramo: iTramo >= 0 && celdas.length > iTramo ? limpio(celdas[iTramo]) : '',
                  asignatura: asignatura,
                  estado: iEstado >= 0 && celdas.length > iEstado ? limpio(celdas[iEstado]) : ''
                });
              }

              // Una tabla vacía en la página de faltas significa que no hay ninguna, que es
              // un resultado tan válido como cualquier otro y hay que poder guardarlo.
              if (faltas.length > 0 || enFaltas) {
                return JSON.stringify({ encontrada: true, faltas: faltas });
              }
            }
          }

          // Sin tabla válida solo se devuelven los encabezados vistos: sirven para ajustar el
          // guion si Séneca cambia la página, y no llevan ningún dato del alumno.
          return JSON.stringify({ encontrada: false, cabeceras: vistas });
        })();
    """.trimIndent()
}
