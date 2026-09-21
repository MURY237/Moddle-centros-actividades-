package com.asir.moodleactividades.data.seneca

/**
 * Tras identificarse, Séneca deja al alumno en su portada, no en las faltas. Este guion busca
 * la entrada del menú por su texto y la pulsa, para que no haya que navegar a mano cada vez.
 *
 * En móvil el menú viene plegado tras el icono de las tres rayas, y sus entradas ni siquiera
 * existen en la página hasta que se abre: por eso hay que desplegarlo antes de buscarlas.
 *
 * Todo se guía por el texto visible y por los atributos, nunca por los identificadores
 * internos de Séneca: esos se generan por sesión y duran lo que dura ella.
 */
object NavegadorFaltas {

    val GUION: String = """
        (function() {
          function limpio(elemento) {
            var texto = (elemento.textContent || '').replace(/\s+/g, ' ').trim().toLowerCase();
            return texto.normalize('NFD').replace(/[̀-ͯ]/g, '');
          }

          // El menú de Séneca vive en un marco lateral, así que hay que recorrerlos todos.
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

          function pulsable(elemento) {
            // El texto puede colgar de un nodo interior; el enlace suele ser un ancestro.
            var actual = elemento;
            for (var salto = 0; salto < 3 && actual; salto++) {
              if (actual.tagName === 'A' || actual.onclick) return actual;
              actual = actual.parentElement;
            }
            return elemento;
          }

          function visible(elemento) {
            try {
              var caja = elemento.getBoundingClientRect();
              return caja.width > 0 && caja.height > 0;
            } catch (e) { return true; }
          }

          function buscar(doc, objetivo, margen) {
            var nodos;
            try { nodos = doc.querySelectorAll('a, span, li, td, div'); } catch (e) { return false; }
            for (var i = 0; i < nodos.length; i++) {
              var texto = limpio(nodos[i]);
              if (texto.indexOf(objetivo) < 0) continue;
              // Sin este tope se pulsaría el contenedor del menú entero en lugar de la entrada.
              if (texto.length > objetivo.length + margen) continue;
              if (!visible(nodos[i])) continue;
              pulsable(nodos[i]).click();
              return true;
            }
            return false;
          }

          function atributos(elemento) {
            var clases = elemento.className;
            // En un SVG className no es una cadena, sino un objeto con la cadena dentro.
            if (clases && typeof clases !== 'string') clases = clases.baseVal || '';
            return [
              elemento.id || '',
              clases || '',
              elemento.getAttribute('aria-label') || '',
              elemento.getAttribute('title') || '',
              elemento.getAttribute('alt') || '',
              elemento.getAttribute('src') || ''
            ].join(' ').toLowerCase();
          }

          /**
           * La hamburguesa no tiene texto, así que se reconoce por sus atributos. Se descartan
           * los elementos grandes: eso sería el contenedor de la barra, no el botón.
           */
          function abrirMenu(doc) {
            var nodos;
            try {
              nodos = doc.querySelectorAll('a, button, div, span, img, i, svg');
            } catch (e) { return false; }
            for (var i = 0; i < nodos.length; i++) {
              var pistas = atributos(nodos[i]);
              if (!/menu|hamburg|toggle|navbar|barras|desplegar/.test(pistas)) continue;
              if (!visible(nodos[i])) continue;
              try {
                var caja = nodos[i].getBoundingClientRect();
                if (caja.width > 120 || caja.height > 120) continue;
              } catch (e) { /* sin medidas: se intenta igual */ }
              pulsable(nodos[i]).click();
              return true;
            }
            return false;
          }

          var docs = documentos();
          var d;

          for (d = 0; d < docs.length; d++) {
            if (buscar(docs[d], 'faltas de asistencia', 10)) {
              return JSON.stringify({ pulsado: true, destino: 'faltas' });
            }
          }
          // Si la entrada no se ve, el apartado que la contiene está plegado.
          for (d = 0; d < docs.length; d++) {
            if (buscar(docs[d], 'seguimiento del curso', 10)) {
              return JSON.stringify({ pulsado: true, destino: 'menu' });
            }
          }
          // Y si no hay ni apartado, es que el menú entero sigue detrás de la hamburguesa.
          for (d = 0; d < docs.length; d++) {
            if (abrirMenu(docs[d])) {
              return JSON.stringify({ pulsado: true, destino: 'hamburguesa' });
            }
          }
          return JSON.stringify({ pulsado: false, destino: '' });
        })();
    """.trimIndent()
}
