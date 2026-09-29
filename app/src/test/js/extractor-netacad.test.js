/*
 * Prueba el guion de ExtractorNetacad contra páginas de imitación, con jsdom.
 *
 * NetAcad no tiene API y su web no se puede consultar desde CI, así que se comprueba lo que
 * sí se puede: que el guion reconoce tablas, tarjetas, rejillas ARIA, marcos, el acceso de
 * Cisco y las páginas vacías. Se lee el guion del propio fichero Kotlin para probar
 * exactamente lo que viaja en la app.
 *
 * Uso: node extractor-netacad.test.js <ruta a ExtractorNetacad.kt>   (necesita jsdom)
 */
const fs = require('fs');
const { JSDOM } = require('jsdom');

// Se prueba exactamente el texto que viaja en la app: se saca del fichero Kotlin.
const fuente = fs.readFileSync(process.argv[2], 'utf8');
const ini = fuente.indexOf('val GUION: String = """') + 'val GUION: String = """'.length;
const fin = fuente.indexOf('""".trimIndent()', ini);
const GUION = fuente.slice(ini, fin);

let fallos = 0, total = 0;
function comprobar(nombre, condicion, detalle) {
  total++;
  if (condicion) console.log('  OK   ' + nombre);
  else { fallos++; console.log('  FALLA ' + nombre + '\n        ' + JSON.stringify(detalle)); }
}

async function ejecutar(html, url = 'https://www.netacad.com/dashboard', preparar) {
  const dom = new JSDOM(html, { url, runScripts: 'outside-only', pretendToBeVisual: true });
  await new Promise(r => dom.window.addEventListener('load', r));
  if (preparar) await preparar(dom.window);
  return JSON.parse(dom.window.eval(GUION));
}

(async () => {
  console.log('1) Tabla del libro de calificaciones (español)');
  let r = await ejecutar(`<html><head><title>CCNA1 Introducción a Redes | Cisco Networking Academy</title></head><body>
    <nav><ul><li><a href="/x">Inicio</a> hoy</li></ul></nav>
    <h1>CCNA1: Introducción a las Redes</h1>
    <table><thead><tr><th>Actividad</th><th>Fecha de entrega</th><th>Estado</th><th>Puntuación</th></tr></thead>
    <tbody>
      <tr><td><a href="/a1">Examen del capítulo 1</a></td><td>05/12/2025 23:59</td><td>No iniciado</td><td>-</td></tr>
      <tr><td><a href="/a2">Packet Tracer 2.3.8</a></td><td>01/11/2025</td><td>Completado</td><td>92%</td></tr>
      <tr><td>Práctica de laboratorio 3.4.6</td><td>10 de diciembre de 2025</td><td>En curso</td><td></td></tr>
    </tbody></table></body></html>`);
  comprobar('reconoce la página de trabajos', r.pagina === 'trabajos', r);
  comprobar('saca las tres filas', r.candidatos && r.candidatos.length === 3, r.candidatos);
  const c0 = r.candidatos[0] || {};
  comprobar('título de la fila', c0.titulo === 'Examen del capítulo 1', c0);
  comprobar('curso sacado del h1', c0.curso === 'CCNA1: Introducción a las Redes', c0);
  comprobar('fecha con hora', /05\/12\/2025 23:59/.test(c0.fecha), c0);
  comprobar('estado en crudo', c0.estado === 'No iniciado', c0);
  comprobar('enlace absoluto', c0.url === 'https://www.netacad.com/a1', c0);
  comprobar('nota 92%', (r.candidatos[1]||{}).nota === '92%', r.candidatos[1]);
  comprobar('fecha en letra, sin tildes', /10 de diciembre de 2025/.test((r.candidatos[2]||{}).fecha), r.candidatos[2]);

  console.log('2) Tarjetas en inglés con fecha de apertura y de entrega');
  r = await ejecutar(`<html><head><title>Dashboard</title></head><body><h1>CCNA2 SRWE</h1>
    <ul class="activity-list">
      <li class="activity-card"><h3 class="card-title"><a href="/m1">Module 4 Exam</a></h3>
          <span>Available: Nov 1, 2025</span><span class="due">Due: Dec 5, 2025 11:59 PM</span>
          <span class="status">Not started</span></li>
      <li class="activity-card"><h3 class="card-title">Skills Assessment</h3>
          <span>Due Nov 10, 2025</span><span class="status">Passed</span><span>18/20</span></li>
      <li class="activity-card" style="display:none"><h3>Oculto</h3><span>Due Dec 1, 2025</span></li>
    </ul></body></html>`);
  comprobar('dos tarjetas visibles, la oculta fuera', r.candidatos && r.candidatos.length === 2, r.candidatos);
  const t0 = (r.candidatos||[])[0] || {};
  comprobar('título desde el encabezado, no la fecha', t0.titulo === 'Module 4 Exam', t0);
  comprobar('elige la fecha de entrega, no la de apertura', /^dec 5, 2025 11:59 pm/.test(t0.fecha), t0);
  comprobar('estado leído', /not started/.test(t0.estado), t0);
  comprobar('puntos 18/20 como nota', ((r.candidatos||[])[1]||{}).nota === '18/20', r.candidatos[1]);

  console.log('3) Rejilla con divs y roles ARIA');
  r = await ejecutar(`<html><body><h1>Cybersecurity Essentials</h1>
    <div role="table">
      <div role="row"><span role="columnheader">Name</span><span role="columnheader">Due date</span><span role="columnheader">Status</span></div>
      <div role="row"><span role="cell">Chapter 2 Quiz</span><span role="cell">2025-12-12</span><span role="cell">Incomplete</span></div>
    </div></body></html>`);
  comprobar('lee la rejilla ARIA', r.candidatos && r.candidatos.length === 1 && r.candidatos[0].titulo === 'Chapter 2 Quiz', r);
  comprobar('fecha ISO', r.candidatos && /2025-12-12/.test(r.candidatos[0].fecha), r.candidatos);

  console.log('4) Contenido dentro de un iframe');
  r = await ejecutar(`<html><body><h1>Portal</h1><p>Contenido del curso cargado dentro del marco inferior de la página.</p><iframe></iframe></body></html>`,
    undefined, async (w) => {
      const doc = w.document.querySelector('iframe').contentDocument;
      doc.body.innerHTML = `<h1>IT Essentials</h1><table><tr><th>Assessment</th><th>Due</th></tr>
        <tr><td>Final Exam</td><td>Jan 15, 2026</td></tr></table>`;
    });
  comprobar('entra en el marco', r.candidatos && r.candidatos.length === 1 && r.candidatos[0].titulo === 'Final Exam', r);
  comprobar('curso del marco', r.candidatos && r.candidatos[0].curso === 'IT Essentials', r.candidatos);

  console.log('5) Acceso de Cisco (paso 1: solo correo, sin contraseña)');
  r = await ejecutar(`<html><body><h1>Sign in</h1><input type="email" name="identifier"><button>Next</button></body></html>`,
    'https://id.cisco.com/signin?x=1');
  comprobar('detecta el acceso por el dominio', r.pagina === 'acceso', r);

  console.log('6) Contraseña visible en cualquier dominio');
  r = await ejecutar(`<html><body><form><input type="text"><input type="password"></form><p>Introduzca sus credenciales para continuar con la sesión.</p></body></html>`);
  comprobar('detecta el acceso por el campo', r.pagina === 'acceso', r);

  console.log('7) Página vacía de una SPA que aún no ha pintado');
  r = await ejecutar(`<html><body><div id="root"><div class="spinner"></div></div></body></html>`);
  comprobar('dice que está cargando', r.pagina === 'cargando', r);

  console.log('8) Página sin trabajos: diagnóstico sin datos personales');
  r = await ejecutar(`<html><body><h1>Mis cursos</h1><h2>Hola, jose@example.com</h2><h2>Recursos</h2>
    <table><tr><th>Recurso</th><th>Tipo</th></tr><tr><td>PDF</td><td>Documento</td></tr></table>
    <p>Aquí verás tus cursos cuando te matricules en alguno de ellos.</p></body></html>`);
  comprobar('página desconocida', r.pagina === 'desconocida', r);
  comprobar('devuelve cabeceras vistas', r.pistas && r.pistas.some(p => /recurso \| tipo/.test(p)), r.pistas);
  comprobar('no filtra correos', r.pistas && !r.pistas.some(p => p.indexOf('@') >= 0), r.pistas);

  console.log('9) Tabla sin columna de nombre reconocible');
  r = await ejecutar(`<html><body><table><tr><th>Módulo 1-3</th><th>Fecha límite</th></tr>
    <tr><td>Checkpoint Exam</td><td>20/12/2025</td></tr></table></body></html>`);
  comprobar('usa la primera columna libre como título', r.candidatos && r.candidatos[0] && r.candidatos[0].titulo === 'Checkpoint Exam', r);

  console.log('10) Menú lateral con fechas no cuenta como trabajo');
  r = await ejecutar(`<html><body><nav><ul><li><a href="/c">Calendario de hoy</a></li></ul></nav>
    <main><p>Bienvenido a la plataforma, selecciona un curso de la lista para empezar.</p></main></body></html>`);
  comprobar('el nav no produce candidatos', r.pagina !== 'trabajos', r);

  console.log('11) Panel con saludo y plazos de varios cursos');
  r = await ejecutar(`<html><body><h1>Welcome back, José</h1>
    <section class="upcoming"><ul>
      <li class="deadline-item"><a class="item-title" href="/a">Modules 1-3 Exam</a>
          <span class="course-name">CCNA: Introduction to Networks</span><span>Due Dec 5, 2025</span></li>
      <li class="deadline-item"><a class="item-title" href="/b">Chapter 2 Quiz</a>
          <span class="course-name">Linux Essentials</span><span>Due Dec 9, 2025</span></li>
      <li class="deadline-item"><a class="item-title" href="/c">Lab 4.2</a><span>Due Dec 12, 2025</span></li>
    </ul></section></body></html>`);
  comprobar('tres plazos del panel', r.candidatos && r.candidatos.length === 3, r);
  const cursos = (r.candidatos || []).map(c => c.curso);
  comprobar('cada tarjeta lleva su curso', cursos[0] === 'CCNA: Introduction to Networks' && cursos[1] === 'Linux Essentials', cursos);
  comprobar('el saludo nunca pasa por curso', !cursos.some(c => /welcome/i.test(c)), cursos);

  console.log(`\n${total - fallos}/${total} comprobaciones correctas`);
  process.exit(fallos ? 1 : 0);
})().catch(e => { console.error(e); process.exit(2); });
