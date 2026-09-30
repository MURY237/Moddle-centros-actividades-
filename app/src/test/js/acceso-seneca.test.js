/*
 * Prueba el guion de AutoAcceso (Séneca) contra páginas que imitan su acceso.
 *
 * Lo que importa: que rellene y envíe el formulario de la Junta esté donde esté —en la
 * página o dentro de un marco—, y que en una página de otro dominio no escriba nada.
 *
 * Uso: node acceso-seneca.test.js <ruta a AutoAcceso.kt>   (necesita jsdom)
 */
const fs = require('fs');
const { JSDOM } = require('jsdom');

const fuente = fs.readFileSync(process.argv[2], 'utf8');
const ini = fuente.indexOf('private fun cuerpo(usuario: String, clave: String, actuar: Boolean): String = """');
const desde = fuente.indexOf('"""', ini) + 3;
const hasta = fuente.indexOf('""".trimIndent()', desde);
const USUARIO = 'alumno.asir';
const CLAVE = 'Clave"con\\raras</script>';
// Lo mismo que hace Kotlin: plantilla con los literales y el «$» escapado.
function guion(actuar) {
  return fuente.slice(desde, hasta)
    .split("${'$'}").join('$')
    .replace('$usuario', actuar ? JSON.stringify(USUARIO) : '""')
    .replace('$clave', actuar ? JSON.stringify(CLAVE) : '""')
    .replace('$actuar', String(actuar));
}

let total = 0, fallos = 0;
function comprobar(nombre, condicion, detalle) {
  total++;
  if (condicion) console.log('  OK    ' + nombre);
  else { fallos++; console.log('  FALLA ' + nombre + '\n        ' + JSON.stringify(detalle)); }
}

const FORMULARIO = '<form name="acceso" action="#">' +
  '<input type="text" name="USUARIO" id="USUARIO">' +
  '<input type="password" name="CLAVE" id="CLAVE">' +
  '<input type="submit" value="Entrar"></form>';

/** Ejecuta el guion en una página; [enMarco] mete el formulario en un marco about:blank. */
function ejecutar(url, enMarco, actuar = true) {
  const dom = new JSDOM(enMarco ? '<body><iframe id="marco"></iframe></body>' : '<body>' + FORMULARIO + '</body>', {
    url, runScripts: 'outside-only', pretendToBeVisual: true
  });
  let doc = dom.window.document;
  if (enMarco) {
    doc = doc.getElementById('marco').contentDocument;
    doc.open(); doc.write('<body>' + FORMULARIO + '</body>'); doc.close();
  }
  // jsdom no navega: basta con saber que se intentó enviar.
  let enviado = false;
  const marcar = () => { enviado = true; };
  for (const ventana of [dom.window, doc.defaultView]) {
    ventana.HTMLFormElement.prototype.submit = marcar;
    ventana.HTMLFormElement.prototype.requestSubmit = marcar;
  }
  doc.addEventListener('submit', (e) => { e.preventDefault(); marcar(); }, true);
  const respuesta = JSON.parse(dom.window.eval(guion(actuar)));
  return {
    respuesta,
    enviado,
    usuario: doc.getElementById('USUARIO').value,
    clave: doc.getElementById('CLAVE').value
  };
}

const SENECA = 'https://seneca.juntadeandalucia.es/seneca/jsp/ComprobarUsuarioExt.jsp';
const AJENA = 'https://seneca.juntadeandalucia.es.ejemplo.com/acceso';

console.log('Séneca: formulario en la propia página');
let r = ejecutar(SENECA, false);
comprobar('rellena usuario y contraseña', r.usuario === USUARIO && r.clave === CLAVE, r);
comprobar('envía el formulario', r.enviado && r.respuesta.accion === 'enviado', r);

console.log('Séneca: formulario dentro de un marco sin dirección propia');
r = ejecutar(SENECA, true);
comprobar('rellena usuario y contraseña', r.usuario === USUARIO && r.clave === CLAVE, r);
comprobar('envía el formulario', r.enviado && r.respuesta.accion === 'enviado', r);

console.log('Página de otro dominio que imita a Séneca');
r = ejecutar(AJENA, false);
comprobar('no escribe la contraseña', r.clave === '' && r.usuario === '', r);
comprobar('no envía nada', !r.enviado && r.respuesta.accion !== 'enviado', r);
r = ejecutar(AJENA, true);
comprobar('tampoco dentro de un marco', r.clave === '' && !r.enviado, r);

console.log('Sondeo (sin contraseña)');
r = ejecutar(SENECA, false, false);
comprobar('ve el formulario', r.respuesta.formulario === true, r);
comprobar('no toca los campos', r.usuario === '' && r.clave === '' && !r.enviado, r);

console.log(`\n${total - fallos}/${total} comprobaciones`);
process.exit(fallos ? 1 : 0);
