/*
 * Prueba el guion de AutoAccesoNetacad contra páginas que imitan el acceso de Cisco.
 *
 * Lo importante no es solo que rellene: es a quién le da la contraseña (solo a cisco.com y
 * netacad.com), qué no pulsa nunca («Go back», «Reset password», «Google») y cuándo se para
 * (contraseña rechazada o verificación en dos pasos).
 *
 * Uso: node acceso-netacad.test.js <ruta a AutoAccesoNetacad.kt>   (necesita jsdom)
 */
const fs = require('fs');
const { JSDOM } = require('jsdom');

const fuente = fs.readFileSync(process.argv[2], 'utf8');
const ini = fuente.indexOf('private fun cuerpo(correo: String, clave: String): String = """') ;
const desde = fuente.indexOf('"""', ini) + 3;
const hasta = fuente.indexOf('""".trimIndent()', desde);
const CORREO = 'alumno@correo.es';
const CLAVE = 'Clave"con\\raras</script>';
// Lo mismo que hace Kotlin: plantilla con los literales y el «$» escapado.
const GUION = fuente.slice(desde, hasta)
  .split("${'$'}").join('$')
  .replace('$correo', JSON.stringify(CORREO))
  .replace('$clave', JSON.stringify(CLAVE));

let total = 0, fallos = 0;
function comprobar(nombre, condicion, detalle) {
  total++;
  if (condicion) console.log('  OK    ' + nombre);
  else { fallos++; console.log('  FALLA ' + nombre + '\n        ' + JSON.stringify(detalle)); }
}

async function pagina(html, url) {
  const dom = new JSDOM(html, { url, runScripts: 'outside-only', pretendToBeVisual: true });
  await new Promise(r => dom.window.addEventListener('load', r));
  const pulsados = [];
  dom.window.document.addEventListener('click', e => pulsados.push((e.target.textContent || e.target.value || '').trim()), true);
  return { w: dom.window, d: dom.window.document, pulsados, correr: () => JSON.parse(dom.window.eval(GUION)) };
}

(async () => {
  console.log('1) Paso del correo, como en la captura');
  let p = await pagina(`<html><body><h1>Cisco Networking Academy</h1>
    <button type="button">Go back</button><h2>Welcome!</h2><p>Please login to your account.</p>
    <form><label>Email *</label><input type="text" name="username" id="email">
      <a href="/reset">Setup or Reset Password</a>
      <button type="button">Sign in with Google</button>
      <button type="submit">Next</button></form></body></html>`, 'https://id.cisco.com/signin');
  // Un campo «controlado» como los de React: una asignación a .value se pierde.
  const campo = p.d.getElementById('email');
  const nativo = Object.getOwnPropertyDescriptor(p.w.HTMLInputElement.prototype, 'value');
  let visto = '';
  Object.defineProperty(campo, 'value', { configurable: true, get() { return nativo.get.call(this); }, set(v) { /* se lo traga */ } });
  campo.addEventListener('input', e => { visto = e.target.value; });
  let r = p.correr();
  comprobar('rellena el correo', r.accion === 'correo', r);
  comprobar('el framework se entera del valor', visto === CORREO, { visto });
  comprobar('pulsa Next', p.pulsados.includes('Next'), p.pulsados);
  comprobar('no pulsa Go back, Reset ni Google',
    !p.pulsados.some(t => /Go back|Reset|Google/.test(t)), p.pulsados);

  console.log('2) Paso de la contraseña');
  p = await pagina(`<html><body><form>
      <a href="/forgot">Forgot password?</a>
      <input type="password" name="credentials.passcode" id="clave">
      <input type="submit" value="Verify"></form></body></html>`, 'https://id.cisco.com/signin/verify');
  r = p.correr();
  comprobar('rellena la contraseña', r.accion === 'clave' && p.d.getElementById('clave').value === CLAVE, r);
  comprobar('los caracteres raros llegan tal cual', p.d.getElementById('clave').value === CLAVE);
  comprobar('pulsa Verify y no Forgot', p.pulsados.includes('Verify') && !p.pulsados.some(t => /Forgot/.test(t)), p.pulsados);

  console.log('3) Correo y contraseña en el mismo formulario');
  p = await pagina(`<html><body><form><input type="email" id="c"><input type="password" id="k">
      <button type="submit">Log in</button></form></body></html>`, 'https://www.netacad.com/login');
  r = p.correr();
  comprobar('rellena los dos y entra', r.accion === 'clave' && p.d.getElementById('c').value === CORREO && p.d.getElementById('k').value === CLAVE, r);

  console.log('4) Una página que no es de Cisco no recibe nada');
  for (const url of ['https://evil.example.com/login', 'https://cisco.com.evil.net/login', 'https://notcisco.com/login']) {
    p = await pagina(`<html><body><form><input type="email" id="c"><input type="password" id="k">
      <button type="submit">Log in</button></form></body></html>`, url);
    r = p.correr();
    comprobar('rechaza ' + new URL(url).hostname,
      r.ajena === true && p.d.getElementById('c').value === '' && p.d.getElementById('k').value === '' && p.pulsados.length === 0, r);
  }

  console.log('5) Contraseña rechazada: se para');
  p = await pagina(`<html><body><div role="alert">Incorrect username or password.</div>
    <form><input type="password" id="k"><button type="submit">Verify</button></form></body></html>`, 'https://id.cisco.com/signin');
  r = p.correr();
  comprobar('dice que hay error y no vuelve a escribir', r.error === true && p.d.getElementById('k').value === '' && p.pulsados.length === 0, r);

  console.log('6) Verificación en dos pasos: se la deja al alumno');
  p = await pagina(`<html><body><h2>Enter the verification code</h2>
    <form><input type="text" autocomplete="one-time-code" id="otp"><button type="submit">Verify</button></form></body></html>`, 'https://id.cisco.com/mfa');
  r = p.correr();
  comprobar('detecta el segundo paso sin tocar nada', r.mfa === true && p.d.getElementById('otp').value === '' && p.pulsados.length === 0, r);

  console.log('7) El formulario aún no ha aparecido');
  p = await pagina(`<html><body><div class="spinner"></div></body></html>`, 'https://id.cisco.com/signin');
  r = p.correr();
  comprobar('no hace nada y lo dice', r.accion === '' && !r.error && !r.mfa, r);

  console.log('8) Buscador en la barra de arriba');
  p = await pagina(`<html><body><header><input type="text" placeholder="Search" id="b"></header>
    <form><input type="text" name="identifier" id="c"><button type="submit">Next</button></form></body></html>`, 'https://id.cisco.com/signin');
  r = p.correr();
  comprobar('escribe en el de acceso, no en el buscador', p.d.getElementById('c').value === CORREO && p.d.getElementById('b').value === '', r);

  console.log(`\n${total - fallos}/${total} comprobaciones correctas`);
  process.exit(fallos ? 1 : 0);
})().catch(e => { console.error(e); process.exit(2); });
