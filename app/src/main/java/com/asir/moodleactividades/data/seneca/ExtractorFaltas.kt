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

          function indiceDe(cabeceras, pistas) {
            for (var i = 0; i < cabeceras.length; i++) {
              for (var p = 0; p < pistas.length; p++) {
                if (cabeceras[i].indexOf(pistas[p]) >= 0) return i;
              }
            }
            return -1;
          }

          var tablas = document.getElementsByTagName('table');
          var vistas = [];

          for (var t = 0; t < tablas.length; t++) {
            var filas = tablas[t].rows;
            if (!filas || filas.length < 2) continue;

            var cabeceras = [];
            for (var c = 0; c < filas[0].cells.length; c++) {
              cabeceras.push(limpio(filas[0].cells[c]).toLowerCase());
            }
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

            return JSON.stringify({ encontrada: true, faltas: faltas });
          }

          // Sin tabla válida solo se devuelven los encabezados vistos: sirven para ajustar el
          // guion si Séneca cambia la página, y no llevan ningún dato del alumno.
          return JSON.stringify({ encontrada: false, cabeceras: vistas });
        })();
    """.trimIndent()
}
