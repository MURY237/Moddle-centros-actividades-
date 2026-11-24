/*
 * Prueba el guion de ExtractorFaltas contra una copia de la tabla de faltas de Pasen, con jsdom.
 *
 * Séneca no se puede consultar desde CI, así que se comprueba lo que sí se puede: que el
 * guion encuentra la tabla por sus encabezados y que no tira las faltas de «Día completo»,
 * que llegan sin asignatura. Se lee el guion del propio fichero Kotlin para probar
 * exactamente lo que viaja en la app.
 *
 * Uso: node extractor-faltas.test.js <ruta a ExtractorFaltas.kt>   (necesita jsdom)
 */
const fs = require('fs');
const { JSDOM } = require('jsdom');

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

async function ejecutar(html) {
  const dom = new JSDOM(html, { url: 'https://seneca.juntadeandalucia.es/seneca/jsp/pasen/', runScripts: 'outside-only' });
  await new Promise(r => dom.window.addEventListener('load', r));
  return JSON.parse(dom.window.eval(GUION));
}

(async () => {
  console.log('1) Tabla de Pasen con una falta de día completo');
  let r = await ejecutar(`<html><body><h2>FALTAS DE ASISTENCIA DEL ALUMNO</h2>
    <table>
      <tr><th>Fecha</th><th>Tramo horario</th><th>Ausencia de</th><th>Estado</th></tr>
      <tr><td>16/11/2026</td><td>13:45 - 14:45</td><td>Administración de sistemas operativos</td><td>Injustificada</td></tr>
      <tr class="dia"><td>11/11/2026</td><td>Día completo</td><td></td><td>Justificada</td></tr>
      <tr><td>10/11/2026</td><td>10:15 - 11:15</td><td>Itinerario Personal para la Empleabilidad II</td><td>Injustificada</td></tr>
      <tr><td></td><td></td><td></td><td></td></tr>
    </table></body></html>`);
  comprobar('encuentra la tabla', r.encontrada === true, r);
  comprobar('tres faltas: la fila vacía no cuenta', r.faltas && r.faltas.length === 3, r.faltas);
  const dia = (r.faltas || [])[1] || {};
  comprobar('el día completo llega sin asignatura', dia.asignatura === '', dia);
  comprobar('con su fecha, su tramo y su estado', dia.fecha === '11/11/2026' && dia.tramo === 'Día completo' && dia.estado === 'Justificada', dia);
  comprobar('las demás siguen igual', ((r.faltas || [])[0] || {}).asignatura === 'Administración de sistemas operativos', r.faltas);

  console.log('2) Página de faltas sin ninguna');
  r = await ejecutar(`<html><body><h2>Faltas de asistencia</h2>
    <table><tr><th>Fecha</th><th>Tramo horario</th><th>Ausencia de</th><th>Estado</th></tr></table></body></html>`);
  comprobar('vacía también es un resultado', r.encontrada === true && r.faltas.length === 0, r);

  console.log(`\n${total - fallos}/${total} comprobaciones correctas`);
  process.exit(fallos ? 1 : 0);
})();
