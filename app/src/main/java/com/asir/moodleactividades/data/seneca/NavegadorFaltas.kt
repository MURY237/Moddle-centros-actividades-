package com.asir.moodleactividades.data.seneca

/**
 * Tras identificarse, Séneca deja al alumno en su portada, no en las faltas. Este guion busca
 * la entrada del menú por su texto y la pulsa, para que no haya que navegar a mano cada vez.
 *
 * Se guía por el texto visible porque los identificadores internos de Séneca se generan por
 * sesión: cualquier referencia a ellos duraría lo que dura la sesión.
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

          function buscar(doc, objetivo, margen) {
            var nodos;
            try { nodos = doc.querySelectorAll('a, span, li, td, div'); } catch (e) { return false; }
            for (var i = 0; i < nodos.length; i++) {
              var texto = limpio(nodos[i]);
              if (texto.indexOf(objetivo) < 0) continue;
              // Sin este tope se pulsaría el contenedor del menú entero en lugar de la entrada.
              if (texto.length > objetivo.length + margen) continue;
              pulsable(nodos[i]).click();
              return true;
            }
            return false;
          }

          var docs = documentos();

          for (var d = 0; d < docs.length; d++) {
            if (buscar(docs[d], 'faltas de asistencia', 10)) {
              return JSON.stringify({ pulsado: true, destino: 'faltas' });
            }
          }
          // Si la entrada no se ve, el apartado que la contiene está plegado.
          for (var e2 = 0; e2 < docs.length; e2++) {
            if (buscar(docs[e2], 'seguimiento del curso', 10)) {
              return JSON.stringify({ pulsado: true, destino: 'menu' });
            }
          }
          return JSON.stringify({ pulsado: false, destino: '' });
        })();
    """.trimIndent()
}
