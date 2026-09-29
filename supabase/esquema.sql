-- =============================================================================
-- Grupos de clase: código de invitación, chat y calendario de exámenes.
--
-- Se pega entero en el editor SQL de Supabase y se ejecuta una vez. Se puede volver
-- a ejecutar para actualizarlo: todo es «if not exists», «or replace» o se borra antes.
--
-- La clave que lleva la app es pública por diseño: la puede leer cualquiera que abra
-- el APK. Lo que protege los grupos son las reglas de este fichero, así que TODO el
-- acceso pasa por ellas:
--   * Nadie ve un grupo si no es miembro. Para ser miembro hay que tener su código.
--   * Las altas y bajas solo van por funciones (unirse, salir, expulsar): así se
--     comprueba el código y se decide quién hereda el grupo al irse el creador.
--   * Lo que el cliente puede escribir se limita también por columnas: nadie puede
--     poner un autor que no sea él, ni cambiar la hora de un mensaje, ni mover a
--     alguien de grupo.
-- =============================================================================

-- ---------------------------------------------------------------- tablas

create table if not exists public.grupos (
  id       uuid primary key default gen_random_uuid(),
  nombre   text not null check (char_length(nombre) between 1 and 60),
  codigo   text not null unique check (codigo ~ '^[0-9A-HJKMNP-TV-Z]{8}$'),
  creador  uuid not null references auth.users(id) on delete cascade,
  creado   timestamptz not null default now()
);

create table if not exists public.miembros (
  grupo    uuid not null references public.grupos(id) on delete cascade,
  usuario  uuid not null references auth.users(id) on delete cascade,
  apodo    text not null check (char_length(apodo) between 1 and 30),
  unido    timestamptz not null default now(),
  primary key (grupo, usuario)
);

create table if not exists public.mensajes (
  id       bigint generated always as identity primary key,
  grupo    uuid not null references public.grupos(id) on delete cascade,
  autor    uuid not null default auth.uid() references auth.users(id) on delete cascade,
  texto    text not null check (char_length(btrim(texto)) between 1 and 2000),
  enviado  timestamptz not null default now()
);

create table if not exists public.examenes (
  id          bigint generated always as identity primary key,
  grupo       uuid not null references public.grupos(id) on delete cascade,
  autor       uuid not null default auth.uid() references auth.users(id) on delete cascade,
  asignatura  text not null check (char_length(btrim(asignatura)) between 1 and 80),
  fecha       date not null,
  hora        time,
  notas       text not null default '' check (char_length(notas) <= 500),
  creado      timestamptz not null default now()
);

create index if not exists miembros_usuario on public.miembros (usuario);
create index if not exists mensajes_grupo_id on public.mensajes (grupo, id);
create index if not exists examenes_grupo_fecha on public.examenes (grupo, fecha);

-- ---------------------------------------------------------------- comprobaciones

-- «security definer» para que la regla de miembros pueda consultar miembros sin
-- volver a aplicarse a sí misma, que sería una recursión infinita.
create or replace function public.es_miembro(g uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.miembros m where m.grupo = g and m.usuario = auth.uid()
  );
$$;

create or replace function public.es_creador(g uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.grupos x where x.id = g and x.creador = auth.uid()
  );
$$;

-- Crockford base32: 32 símbolos, así que un byte módulo 32 no favorece a ninguno.
-- Sin I, L, O ni U, que se confunden al dictarlo; al escribir, O se lee 0 e I/L se leen 1.
create or replace function public.nuevo_codigo() returns text
language plpgsql volatile set search_path = '' as $$
declare
  alfabeto constant text := '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
  azar bytea := extensions.gen_random_bytes(8);
  salida text := '';
begin
  for i in 0..7 loop
    salida := salida || substr(alfabeto, (get_byte(azar, i) % 32) + 1, 1);
  end loop;
  return salida;
end;
$$;

create or replace function public.normalizar_codigo(p text) returns text
language sql immutable set search_path = '' as $$
  select translate(upper(regexp_replace(coalesce(p, ''), '[^0-9A-Za-z]', '', 'g')), 'OIL', '011');
$$;

-- ---------------------------------------------------------------- funciones de la app

create or replace function public.crear_grupo(p_nombre text, p_apodo text)
returns public.grupos
language plpgsql security definer set search_path = '' as $$
declare
  g public.grupos;
  yo uuid := auth.uid();
  intentos int := 0;
begin
  if yo is null then raise exception 'sin_sesion'; end if;
  p_nombre := btrim(coalesce(p_nombre, ''));
  p_apodo := btrim(coalesce(p_apodo, ''));
  if char_length(p_nombre) not between 1 and 60 then raise exception 'nombre_no_valido'; end if;
  if char_length(p_apodo) not between 1 and 30 then raise exception 'apodo_no_valido'; end if;
  -- Tope contra el abuso: una cuenta anónima se crea gratis, un grupo no debería serlo.
  if (select count(*) from public.grupos where creador = yo) >= 20 then
    raise exception 'demasiados_grupos';
  end if;

  loop
    begin
      insert into public.grupos (nombre, codigo, creador)
      values (p_nombre, public.nuevo_codigo(), yo)
      returning * into g;
      exit;
    exception when unique_violation then
      intentos := intentos + 1;
      if intentos >= 5 then raise; end if;
    end;
  end loop;

  insert into public.miembros (grupo, usuario, apodo) values (g.id, yo, p_apodo);
  return g;
end;
$$;

create or replace function public.unirse(p_codigo text, p_apodo text)
returns public.grupos
language plpgsql security definer set search_path = '' as $$
declare
  g public.grupos;
  yo uuid := auth.uid();
begin
  if yo is null then raise exception 'sin_sesion'; end if;
  p_apodo := btrim(coalesce(p_apodo, ''));
  if char_length(p_apodo) not between 1 and 30 then raise exception 'apodo_no_valido'; end if;

  select * into g from public.grupos where codigo = public.normalizar_codigo(p_codigo);
  if not found then raise exception 'codigo_no_valido'; end if;

  -- Volver a unirse a un grupo en el que ya se está no es un error: se devuelve y ya.
  if exists (select 1 from public.miembros where grupo = g.id and usuario = yo) then
    return g;
  end if;
  if (select count(*) from public.miembros where grupo = g.id) >= 100 then
    raise exception 'grupo_lleno';
  end if;

  insert into public.miembros (grupo, usuario, apodo) values (g.id, yo, p_apodo);
  return g;
end;
$$;

-- Cambia el código: el anterior deja de servir. Para cuando se ha compartido donde no debía.
create or replace function public.renovar_codigo(p_grupo uuid)
returns text
language plpgsql security definer set search_path = '' as $$
declare
  nuevo text;
  intentos int := 0;
begin
  if not public.es_creador(p_grupo) then raise exception 'solo_creador'; end if;
  loop
    begin
      update public.grupos set codigo = public.nuevo_codigo() where id = p_grupo
      returning codigo into nuevo;
      return nuevo;
    exception when unique_violation then
      intentos := intentos + 1;
      if intentos >= 5 then raise; end if;
    end;
  end loop;
end;
$$;

-- Si se va el creador, el grupo pasa al miembro más antiguo; si no queda nadie, se borra.
create or replace function public.salir(p_grupo uuid)
returns void
language plpgsql security definer set search_path = '' as $$
declare
  yo uuid := auth.uid();
  era_creador boolean;
  siguiente uuid;
begin
  era_creador := public.es_creador(p_grupo);
  delete from public.miembros where grupo = p_grupo and usuario = yo;
  if not found then return; end if;
  if not era_creador then return; end if;

  select usuario into siguiente from public.miembros
  where grupo = p_grupo order by unido, usuario limit 1;

  if siguiente is null then
    delete from public.grupos where id = p_grupo;
  else
    update public.grupos set creador = siguiente where id = p_grupo;
  end if;
end;
$$;

create or replace function public.expulsar(p_grupo uuid, p_usuario uuid)
returns void
language plpgsql security definer set search_path = '' as $$
begin
  if not public.es_creador(p_grupo) then raise exception 'solo_creador'; end if;
  if p_usuario = auth.uid() then raise exception 'no_a_ti_mismo'; end if;
  delete from public.miembros where grupo = p_grupo and usuario = p_usuario;
end;
$$;

-- ---------------------------------------------------------------- permisos

-- Supabase da por defecto todos los permisos sobre tablas y funciones nuevas a los
-- roles de la API. Se quitan todos y se da solo lo justo, columna a columna.
revoke all on public.grupos, public.miembros, public.mensajes, public.examenes
  from anon, authenticated;

grant select, delete on public.grupos to authenticated;
grant update (nombre) on public.grupos to authenticated;

grant select on public.miembros to authenticated;
grant update (apodo) on public.miembros to authenticated;

grant select, delete on public.mensajes to authenticated;
grant insert (grupo, texto) on public.mensajes to authenticated;

grant select, delete on public.examenes to authenticated;
grant insert (grupo, asignatura, fecha, hora, notas) on public.examenes to authenticated;
grant update (asignatura, fecha, hora, notas) on public.examenes to authenticated;

revoke all on function
  public.es_miembro(uuid), public.es_creador(uuid), public.nuevo_codigo(),
  public.normalizar_codigo(text), public.crear_grupo(text, text), public.unirse(text, text),
  public.renovar_codigo(uuid), public.salir(uuid), public.expulsar(uuid, uuid)
  from public, anon, authenticated;

-- Las reglas llaman a estas dos con el rol de quien consulta, así que necesita poder.
grant execute on function public.es_miembro(uuid), public.es_creador(uuid) to authenticated;
grant execute on function
  public.crear_grupo(text, text), public.unirse(text, text), public.renovar_codigo(uuid),
  public.salir(uuid), public.expulsar(uuid, uuid)
  to authenticated;

-- ---------------------------------------------------------------- reglas (RLS)

alter table public.grupos   enable row level security;
alter table public.miembros enable row level security;
alter table public.mensajes enable row level security;
alter table public.examenes enable row level security;

drop policy if exists grupos_ver on public.grupos;
drop policy if exists grupos_renombrar on public.grupos;
drop policy if exists grupos_borrar on public.grupos;
create policy grupos_ver on public.grupos for select to authenticated
  using (public.es_miembro(id));
create policy grupos_renombrar on public.grupos for update to authenticated
  using (creador = (select auth.uid())) with check (creador = (select auth.uid()));
create policy grupos_borrar on public.grupos for delete to authenticated
  using (creador = (select auth.uid()));

drop policy if exists miembros_ver on public.miembros;
drop policy if exists miembros_apodo on public.miembros;
create policy miembros_ver on public.miembros for select to authenticated
  using (public.es_miembro(grupo));
create policy miembros_apodo on public.miembros for update to authenticated
  using (usuario = (select auth.uid())) with check (usuario = (select auth.uid()));

drop policy if exists mensajes_ver on public.mensajes;
drop policy if exists mensajes_escribir on public.mensajes;
drop policy if exists mensajes_borrar on public.mensajes;
create policy mensajes_ver on public.mensajes for select to authenticated
  using (public.es_miembro(grupo));
create policy mensajes_escribir on public.mensajes for insert to authenticated
  with check (autor = (select auth.uid()) and public.es_miembro(grupo));
create policy mensajes_borrar on public.mensajes for delete to authenticated
  using (autor = (select auth.uid()));

drop policy if exists examenes_ver on public.examenes;
drop policy if exists examenes_anadir on public.examenes;
drop policy if exists examenes_cambiar on public.examenes;
drop policy if exists examenes_borrar on public.examenes;
create policy examenes_ver on public.examenes for select to authenticated
  using (public.es_miembro(grupo));
create policy examenes_anadir on public.examenes for insert to authenticated
  with check (autor = (select auth.uid()) and public.es_miembro(grupo));
-- Lo puede corregir quien lo puso o quien lleva el grupo: si alguien pone una fecha
-- mal y no lo ve, que no se quede así para todos.
create policy examenes_cambiar on public.examenes for update to authenticated
  using (autor = (select auth.uid()) or public.es_creador(grupo))
  with check (public.es_miembro(grupo));
create policy examenes_borrar on public.examenes for delete to authenticated
  using (autor = (select auth.uid()) or public.es_creador(grupo));

-- ---------------------------------------------------------------- latido

-- La llama cada tres días una tarea de GitHub Actions (.github/workflows/latido.yml)
-- para que el plan gratuito no pause el proyecto por una semana sin actividad, como en
-- vacaciones. Solo devuelve la hora del servidor: no lee ni enseña nada de nadie, así
-- que puede llamarla cualquiera con la clave pública, sin sesión.
create or replace function public.latido() returns timestamptz
language sql stable set search_path = '' as $$
  select now();
$$;

revoke all on function public.latido() from public;
grant execute on function public.latido() to anon, authenticated;
