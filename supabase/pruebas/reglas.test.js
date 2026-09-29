/*
 * Prueba las reglas de esquema.sql contra un PostgreSQL de verdad (PGlite), con tres
 * usuarios: Ana crea el grupo, Bruno entra con el código y Carla no lo tiene.
 *
 * Se imita lo que Supabase pone por su cuenta —los roles de la API, auth.uid() leyendo
 * el token y los permisos por defecto que da sobre todo lo nuevo— para comprobar que el
 * esquema los recorta de verdad y no solo en teoría.
 *
 * Uso: node reglas.test.js <ruta a esquema.sql>   (necesita @electric-sql/pglite)
 */
const fs = require('fs');
const { PGlite } = require('@electric-sql/pglite');
const { pgcrypto } = require('@electric-sql/pglite/contrib/pgcrypto');

const ANA = '00000000-0000-0000-0000-00000000000a';
const BRUNO = '00000000-0000-0000-0000-00000000000b';
const CARLA = '00000000-0000-0000-0000-00000000000c';

let total = 0, fallos = 0;
function comprobar(nombre, condicion, detalle) {
  total++;
  if (condicion) console.log('  OK    ' + nombre);
  else { fallos++; console.log('  FALLA ' + nombre + '\n        ' + JSON.stringify(detalle)); }
}

(async () => {
  const db = new PGlite({ extensions: { pgcrypto } });

  // Lo que Supabase ya trae montado antes de que se ejecute el esquema.
  await db.exec(`
    create role anon nologin;
    create role authenticated nologin;
    create schema auth;
    create table auth.users (id uuid primary key);
    create function auth.uid() returns uuid language sql stable as
      $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
    grant usage on schema auth to anon, authenticated;
    create schema extensions;
    create extension pgcrypto schema extensions;
    grant usage on schema extensions to anon, authenticated;
    grant usage on schema public to anon, authenticated;
    alter default privileges in schema public grant all on tables to anon, authenticated;
    alter default privileges in schema public grant all on functions to anon, authenticated;
    alter default privileges in schema public grant all on sequences to anon, authenticated;
    insert into auth.users values ('${ANA}'), ('${BRUNO}'), ('${CARLA}');
  `);

  const esquema = fs.readFileSync(process.argv[2], 'utf8');
  await db.exec(esquema);
  // Dos veces: tiene que poder volver a ejecutarse para actualizarlo.
  await db.exec(esquema);
  comprobar('el esquema se puede ejecutar dos veces', true);

  async function como(uid, sql, params) {
    await db.exec(`reset role; select set_config('request.jwt.claim.sub', '${uid || ''}', false);`);
    await db.exec(uid ? 'set role authenticated' : 'set role anon');
    try { return await db.query(sql, params); } finally { await db.exec('reset role'); }
  }
  async function falla(uid, sql, params) {
    try { await como(uid, sql, params); return null; } catch (e) { return e.message; }
  }

  console.log('Crear y unirse');
  const creado = (await como(ANA, `select * from public.crear_grupo($1, $2)`, ['2º ASIR', 'Ana'])).rows[0];
  const codigo = creado.codigo;
  comprobar('el código tiene 8 símbolos de Crockford', /^[0-9A-HJKMNP-TV-Z]{8}$/.test(codigo), codigo);
  comprobar('la creadora es miembro',
    (await como(ANA, `select * from public.miembros where grupo = $1`, [creado.id])).rows.length === 1);

  comprobar('sin código no se ve el grupo',
    (await como(CARLA, `select * from public.grupos`)).rows.length === 0);
  comprobar('sin código no se ven sus miembros',
    (await como(CARLA, `select * from public.miembros`)).rows.length === 0);

  // Como se dictaría: minúsculas, guion y una O en vez de un 0 si lo hubiera.
  const dictado = (codigo.slice(0, 4) + '-' + codigo.slice(4)).toLowerCase().replace(/0/g, 'o').replace(/1/g, 'l');
  const unido = (await como(BRUNO, `select * from public.unirse($1, $2)`, [dictado, 'Bruno'])).rows[0];
  comprobar('se entra con el código escrito a mano', unido && unido.id === creado.id, { dictado, unido });
  comprobar('volver a unirse no duplica', (await como(BRUNO, `select * from public.unirse($1, $2)`, [codigo, 'Bruno'])).rows[0].id === creado.id
    && (await como(ANA, `select count(*)::int n from public.miembros where grupo = $1`, [creado.id])).rows[0].n === 2);
  comprobar('un código que no existe se rechaza',
    /codigo_no_valido/.test(await falla(CARLA, `select * from public.unirse('ZZZZZZZZ', 'Carla')`)));
  comprobar('un apodo vacío se rechaza',
    /apodo_no_valido/.test(await falla(CARLA, `select * from public.unirse($1, '   ')`, [codigo])));
  comprobar('sin sesión no se crea nada',
    /permission denied|sin_sesion/.test(await falla(null, `select * from public.crear_grupo('x', 'y')`)));
  comprobar('nuevo_codigo no se puede llamar desde la app',
    /permission denied/.test(await falla(BRUNO, `select public.nuevo_codigo()`)));

  console.log('Chat');
  await como(BRUNO, `insert into public.mensajes (grupo, texto) values ($1, 'Hola a todos')`, [creado.id]);
  const vistos = (await como(ANA, `select * from public.mensajes where grupo = $1`, [creado.id])).rows;
  comprobar('los miembros leen el chat', vistos.length === 1 && vistos[0].autor === BRUNO, vistos);
  comprobar('quien no es miembro no lo lee',
    (await como(CARLA, `select * from public.mensajes`)).rows.length === 0);
  comprobar('quien no es miembro no escribe',
    /row-level security/.test(await falla(CARLA, `insert into public.mensajes (grupo, texto) values ($1, 'intrusa')`, [creado.id])));
  comprobar('no se puede firmar como otro',
    /permission denied/.test(await falla(BRUNO, `insert into public.mensajes (grupo, texto, autor) values ($1, 'soy Ana', $2)`, [creado.id, ANA])));
  comprobar('no se puede falsear la hora',
    /permission denied/.test(await falla(BRUNO, `insert into public.mensajes (grupo, texto, enviado) values ($1, 'x', '2000-01-01')`, [creado.id])));
  comprobar('un mensaje en blanco no entra',
    /check constraint/.test(await falla(BRUNO, `insert into public.mensajes (grupo, texto) values ($1, '   ')`, [creado.id])));
  await como(ANA, `delete from public.mensajes where autor = $1`, [BRUNO]);
  comprobar('nadie borra mensajes ajenos',
    (await como(BRUNO, `select count(*)::int n from public.mensajes`)).rows[0].n === 1);

  console.log('Exámenes');
  const examenBruno = (await como(BRUNO, `insert into public.examenes (grupo, asignatura, fecha, hora)
    values ($1, 'Redes', '2025-12-05', '09:00') returning *`, [creado.id])).rows[0];
  const examenAna = (await como(ANA, `insert into public.examenes (grupo, asignatura, fecha)
    values ($1, 'SGBD', '2025-12-10') returning *`, [creado.id])).rows[0];
  comprobar('el autor queda puesto solo', examenBruno.autor === BRUNO && examenAna.autor === ANA);
  comprobar('quien no es miembro no ve el calendario',
    (await como(CARLA, `select * from public.examenes`)).rows.length === 0);
  comprobar('quien no es miembro no añade exámenes',
    /row-level security/.test(await falla(CARLA, `insert into public.examenes (grupo, asignatura, fecha) values ($1, 'x', '2025-12-01')`, [creado.id])));
  await como(BRUNO, `delete from public.examenes where id = $1`, [examenAna.id]);
  comprobar('un miembro no borra el examen de otro',
    (await como(ANA, `select count(*)::int n from public.examenes`)).rows[0].n === 2);
  await como(BRUNO, `update public.examenes set asignatura = 'hackeado' where id = $1`, [examenAna.id]);
  comprobar('ni lo cambia',
    (await como(ANA, `select asignatura from public.examenes where id = $1`, [examenAna.id])).rows[0].asignatura === 'SGBD');
  await como(ANA, `update public.examenes set fecha = '2025-12-06' where id = $1`, [examenBruno.id]);
  comprobar('la creadora corrige la fecha de otro',
    (await como(BRUNO, `select fecha::text f from public.examenes where id = $1`, [examenBruno.id])).rows[0].f === '2025-12-06');
  comprobar('no se puede mover un examen a otro grupo',
    /permission denied/.test(await falla(ANA, `update public.examenes set grupo = gen_random_uuid() where id = $1`, [examenBruno.id])));

  console.log('Grupo y miembros');
  await como(BRUNO, `update public.grupos set nombre = 'mío' where id = $1`, [creado.id]);
  comprobar('solo la creadora renombra',
    (await como(ANA, `select nombre from public.grupos where id = $1`, [creado.id])).rows[0].nombre === '2º ASIR');
  comprobar('nadie se cambia de creador',
    /permission denied/.test(await falla(BRUNO, `update public.grupos set creador = $2 where id = $1`, [creado.id, BRUNO])));
  comprobar('nadie se mueve de grupo',
    /permission denied/.test(await falla(BRUNO, `update public.miembros set grupo = gen_random_uuid() where usuario = $1`, [BRUNO])));
  await como(BRUNO, `update public.miembros set apodo = 'Bruno R.' where usuario = $1 and grupo = $2`, [BRUNO, creado.id]);
  await como(BRUNO, `update public.miembros set apodo = 'Tonta' where usuario = $1 and grupo = $2`, [ANA, creado.id]);
  const apodos = (await como(ANA, `select usuario, apodo from public.miembros where grupo = $1 order by unido`, [creado.id])).rows;
  comprobar('cada uno cambia solo su apodo', apodos[0].apodo === 'Ana' && apodos[1].apodo === 'Bruno R.', apodos);
  comprobar('nadie se da de alta sin código',
    /permission denied/.test(await falla(CARLA, `insert into public.miembros (grupo, usuario, apodo) values ($1, $2, 'Carla')`, [creado.id, CARLA])));

  console.log('Código nuevo');
  comprobar('solo la creadora renueva el código',
    /solo_creador/.test(await falla(BRUNO, `select public.renovar_codigo($1)`, [creado.id])));
  const nuevo = (await como(ANA, `select public.renovar_codigo($1) c`, [creado.id])).rows[0].c;
  comprobar('el código cambia', nuevo !== codigo && /^[0-9A-HJKMNP-TV-Z]{8}$/.test(nuevo), { codigo, nuevo });
  comprobar('el viejo deja de servir',
    /codigo_no_valido/.test(await falla(CARLA, `select * from public.unirse($1, 'Carla')`, [codigo])));
  await como(CARLA, `select * from public.unirse($1, 'Carla')`, [nuevo]);
  comprobar('el nuevo sí', (await como(CARLA, `select count(*)::int n from public.mensajes`)).rows[0].n === 1);

  console.log('Expulsar y salir');
  comprobar('un miembro no expulsa',
    /solo_creador/.test(await falla(BRUNO, `select public.expulsar($1, $2)`, [creado.id, CARLA])));
  await como(ANA, `select public.expulsar($1, $2)`, [creado.id, CARLA]);
  comprobar('expulsada, deja de leer el chat',
    (await como(CARLA, `select count(*)::int n from public.mensajes`)).rows[0].n === 0);

  await como(ANA, `select public.salir($1)`, [creado.id]);
  comprobar('si se va la creadora, el grupo pasa al más antiguo',
    (await como(BRUNO, `select creador from public.grupos where id = $1`, [creado.id])).rows[0].creador === BRUNO);
  comprobar('y ella deja de verlo', (await como(ANA, `select * from public.grupos`)).rows.length === 0);
  await como(BRUNO, `select public.salir($1)`, [creado.id]);
  comprobar('si no queda nadie, el grupo se borra con todo',
    (await db.query(`select (select count(*) from public.grupos)::int g, (select count(*) from public.mensajes)::int m, (select count(*) from public.examenes)::int e`)).rows[0].g === 0);

  console.log('Rol anónimo de la API (sin iniciar sesión)');
  comprobar('anon no lee tablas', /permission denied/.test(await falla(null, `select * from public.grupos`)));
  comprobar('anon no llama a unirse', /permission denied/.test(await falla(null, `select * from public.unirse('x', 'y')`)));

  console.log(`\n${total - fallos}/${total} comprobaciones correctas`);
  process.exit(fallos ? 1 : 0);
})().catch(e => { console.error(e); process.exit(2); });
