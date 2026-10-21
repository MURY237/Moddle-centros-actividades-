/*
 * Prueba la función «avisar» sin Supabase ni Firebase: se le da un fetch de mentira que
 * hace de base de datos, de Google y de FCM, y se mira qué pide y a quién avisa.
 *
 * Uso: node avisar.test.mjs <ruta a supabase/functions/avisar/index.ts>   (Node 22.6+)
 */
import crypto from 'node:crypto';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const modulo = await import(pathToFileURL(path.resolve(process.argv[2])).href);
const { atender, firmarJwt, datosDelAviso } = modulo;

let total = 0, fallos = 0;
function comprobar(nombre, condicion, detalle) {
  total++;
  if (condicion) console.log('  OK    ' + nombre);
  else { fallos++; console.log('  FALLA ' + nombre + '\n        ' + JSON.stringify(detalle)); }
}

const { privateKey, publicKey } = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
const cuenta = {
  client_email: 'avisos@proyecto.iam.gserviceaccount.com',
  private_key: privateKey.export({ type: 'pkcs8', format: 'pem' }),
  project_id: 'proyecto-asir',
};
const entorno = { url: 'https://abc.supabase.co', claveServidor: 'clave-servidor', cuenta };

console.log('Firma para Google');
const jwt = await firmarJwt(cuenta, 1_800_000_000);
const [cab, datos, firma] = jwt.split('.');
const valida = crypto.verify('sha256', Buffer.from(`${cab}.${datos}`), publicKey, Buffer.from(firma, 'base64url'));
comprobar('la firma RS256 se verifica con la clave pública', valida);
const reclamos = JSON.parse(Buffer.from(datos, 'base64url').toString());
comprobar('pide permiso solo para FCM y caduca en una hora',
  reclamos.scope === 'https://www.googleapis.com/auth/firebase.messaging' &&
  reclamos.iss === cuenta.client_email && reclamos.exp - reclamos.iat === 3600, reclamos);

console.log('Contenido del aviso');
const largo = datosDelAviso('mensaje', { id: 7, grupo: 'g', texto: 'x'.repeat(900) }, '2º ASIR', 'Ana');
comprobar('el texto se recorta y todo va como texto',
  largo.texto.length === 500 && Object.values(largo).every((v) => typeof v === 'string'), largo);

// ------------------------------------------------------------ servidor de mentira
const ANA = 'uuid-ana', BRUNO = 'uuid-bruno', CARLA = 'uuid-carla';
function escenario({ autor = ANA, enviado = new Date().toISOString(), yaAvisado = false, quien = ANA } = {}) {
  const pedidas = [];
  const enviados = [];
  const borrados = [];
  const pedir = async (url, init = {}) => {
    pedidas.push(url);
    const u = new URL(url);
    const r = (cuerpo, estado = 200) => new Response(typeof cuerpo === 'string' ? cuerpo : JSON.stringify(cuerpo), { status: estado });
    if (u.pathname === '/auth/v1/user') {
      return init.headers.Authorization === 'Bearer sesion-valida' ? r({ id: quien }) : r({}, 401);
    }
    if (u.pathname === '/rest/v1/mensajes') return r([{ id: 7, grupo: 'g1', autor, texto: 'Mañana examen', enviado }]);
    if (u.pathname === '/rest/v1/avisados') return r(yaAvisado ? [] : [{ tipo: 'mensaje', id: 7 }], 201);
    if (u.pathname === '/rest/v1/grupos') return r([{ nombre: '2º ASIR' }]);
    if (u.pathname === '/rest/v1/miembros') return r([{ usuario: ANA, apodo: 'Ana' }, { usuario: BRUNO, apodo: 'Bruno' }, { usuario: CARLA, apodo: 'Carla' }]);
    if (u.pathname === '/rest/v1/dispositivos' && (init.method ?? 'GET') === 'GET') {
      const usuarios = decodeURIComponent(u.search);
      return r([
        ...(usuarios.includes(BRUNO) ? [{ token: 'movil-bruno' }] : []),
        ...(usuarios.includes(CARLA) ? [{ token: 'movil-carla-viejo' }] : []),
        ...(usuarios.includes(ANA) ? [{ token: 'movil-ana' }] : []),
      ]);
    }
    if (u.pathname === '/rest/v1/dispositivos' && init.method === 'DELETE') { borrados.push(u.search); return new Response(null, { status: 204 }); }
    if (url === 'https://oauth2.googleapis.com/token') return r({ access_token: 'acceso-google', expires_in: 3600 });
    if (u.hostname === 'fcm.googleapis.com') {
      const cuerpo = JSON.parse(init.body);
      if (cuerpo.message.token === 'movil-carla-viejo') return r({ error: { status: 'NOT_FOUND', details: [{ errorCode: 'UNREGISTERED' }] } }, 404);
      enviados.push(cuerpo.message);
      return r({ name: 'ok' });
    }
    return r({ error: 'inesperado ' + url }, 500);
  };
  return { pedir, pedidas, enviados, borrados };
}
const peticion = (cuerpo, sesion = 'Bearer sesion-valida') => new Request('https://abc.supabase.co/functions/v1/avisar', {
  method: 'POST', headers: { Authorization: sesion, 'Content-Type': 'application/json' }, body: JSON.stringify(cuerpo),
});

console.log('Aviso de un mensaje');
let e = escenario();
let resp = await atender(peticion({ tipo: 'mensaje', id: 7 }), entorno, e.pedir);
let cuerpo = await resp.json();
comprobar('avisa a los demás miembros y no a quien escribe',
  resp.status === 200 && cuerpo.avisados === 1 && e.enviados.length === 1 && e.enviados[0].token === 'movil-bruno', { cuerpo, enviados: e.enviados });
comprobar('ni pregunta por el móvil del autor',
  !e.pedidas.some((u) => u.includes('dispositivos') && decodeURIComponent(u).includes(ANA)));
comprobar('el aviso lleva grupo, apodo y texto, con prioridad alta',
  e.enviados[0].data.grupoNombre === '2º ASIR' && e.enviados[0].data.apodo === 'Ana' &&
  e.enviados[0].data.texto === 'Mañana examen' && e.enviados[0].android.priority === 'HIGH', e.enviados[0]);
comprobar('el móvil desinstalado se borra', e.borrados.length === 1 && e.borrados[0].includes('movil-carla-viejo'), e.borrados);

console.log('Lo que no debe avisar');
e = escenario();
resp = await atender(peticion({ tipo: 'mensaje', id: 7 }, 'Bearer inventada'), entorno, e.pedir);
comprobar('sin sesión válida no hace nada', resp.status === 401 && e.enviados.length === 0);
e = escenario({ quien: BRUNO });
resp = await atender(peticion({ tipo: 'mensaje', id: 7 }), entorno, e.pedir);
comprobar('nadie avisa en nombre de otro', resp.status === 403 && e.enviados.length === 0);
e = escenario({ enviado: new Date(Date.now() - 3600_000).toISOString() });
resp = await atender(peticion({ tipo: 'mensaje', id: 7 }), entorno, e.pedir);
comprobar('un mensaje viejo no se vuelve a avisar', (await resp.json()).motivo === 'antiguo' && e.enviados.length === 0);
e = escenario({ yaAvisado: true });
resp = await atender(peticion({ tipo: 'mensaje', id: 7 }), entorno, e.pedir);
comprobar('llamar dos veces no avisa dos veces', (await resp.json()).motivo === 'repetido' && e.enviados.length === 0);
e = escenario();
resp = await atender(peticion({ tipo: 'mensajes', id: '7; drop' }), entorno, e.pedir);
comprobar('datos raros se rechazan antes de tocar nada', resp.status === 400 && e.pedidas.length === 0);
resp = await atender(peticion({ tipo: 'mensaje', id: 7 }), { ...entorno, cuenta: null }, e.pedir);
comprobar('sin la cuenta de Firebase lo dice', resp.status === 503);

console.log(`\n${total - fallos}/${total} comprobaciones`);
process.exit(fallos ? 1 : 0);
