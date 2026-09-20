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

          function pulsable(elemento) {
            // El texto puede colgar de un nodo interior; el enlace suele ser un ancestro.
            var actual = elemento;
            for (var salto = 0; salto < 3 && actual; salto++) {
              if (actual.tagName === 'A' || actual.onclick) return actual;
              actual = actual.parentElement;
            }
            return elemento;
          }

          function buscar(objetivo, margen) {
            var nodos = document.querySelectorAll('a, span, li, td, div');
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

          if (buscar('faltas de asistencia', 10)) {
            return JSON.stringify({ pulsado: true, destino: 'faltas' });
          }
          // Si la entrada no se ve, el apartado que la contiene está plegado.
          if (buscar('seguimiento del curso', 10)) {
            return JSON.stringify({ pulsado: true, destino: 'menu' });
          }
          return JSON.stringify({ pulsado: false, destino: '' });
        })();
    """.trimIndent()
}
