/**
 * Función «avisar» de Supabase: manda al móvil de los demás miembros del grupo un aviso
 * (Firebase Cloud Messaging) cuando alguien escribe en el chat o apunta un examen.
 *
 * La llama la app justo después de enviar, con la sesión de quien envía. Aquí se comprueba
 * todo otra vez con la clave del servidor, que nunca sale de Supabase:
 *   - quien llama es el autor de ese mensaje o examen,
 *   - es reciente (no se puede volver a avisar de algo viejo),
 *   - no se ha avisado ya (tabla «avisados»),
 *   - y solo se avisa a los miembros del grupo, nunca a nadie más.
 *
 * No usa librerías: un solo fichero que se puede pegar tal cual en el editor de funciones
 * del panel de Supabase. Necesita el secreto FIREBASE_CUENTA (el JSON de la cuenta de
 * servicio de Firebase); SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY los pone Supabase.
 */

export interface Cuenta {
  client_email: string;
  private_key: string;
  project_id: string;
}

export interface Entorno {
  url: string;
  claveServidor: string;
  cuenta: Cuenta | null;
}

type Pedir = (url: string, init?: RequestInit) => Promise<Response>;

const MAX_TEXTO = 500;
const MAX_EDAD_MS = 10 * 60 * 1000;
const ALCANCE = "https://www.googleapis.com/auth/firebase.messaging";
const TOKEN_GOOGLE = "https://oauth2.googleapis.com/token";

// ------------------------------------------------------------------ firma para Google

export function base64url(datos: Uint8Array | string): string {
  const bytes = typeof datos === "string" ? new TextEncoder().encode(datos) : datos;
  let binario = "";
  for (const b of bytes) binario += String.fromCharCode(b);
  return btoa(binario).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemADer(pem: string): Uint8Array {
  const cuerpo = pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  const binario = atob(cuerpo);
  const salida = new Uint8Array(binario.length);
  for (let i = 0; i < binario.length; i++) salida[i] = binario.charCodeAt(i);
  return salida;
}

/** El JWT con el que la cuenta de servicio pide a Google un token para FCM. */
export async function firmarJwt(cuenta: Cuenta, ahoraS: number): Promise<string> {
  const cabecera = base64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const datos = base64url(JSON.stringify({
    iss: cuenta.client_email,
    scope: ALCANCE,
    aud: TOKEN_GOOGLE,
    iat: ahoraS,
    exp: ahoraS + 3600,
  }));
  const clave = await crypto.subtle.importKey(
    "pkcs8",
    pemADer(cuenta.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const firma = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    clave,
    new TextEncoder().encode(`${cabecera}.${datos}`),
  );
  return `${cabecera}.${datos}.${base64url(new Uint8Array(firma))}`;
}

let tokenGuardado: { valor: string; caduca: number } | null = null;

async function tokenDeGoogle(cuenta: Cuenta, pedir: Pedir): Promise<string> {
  const ahora = Date.now();
  if (tokenGuardado && tokenGuardado.caduca - 60_000 > ahora) return tokenGuardado.valor;
  const respuesta = await pedir(TOKEN_GOOGLE, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: await firmarJwt(cuenta, Math.floor(ahora / 1000)),
    }).toString(),
  });
  if (!respuesta.ok) throw new Error(`google ${respuesta.status}`);
  const cuerpo = await respuesta.json();
  tokenGuardado = { valor: cuerpo.access_token, caduca: ahora + (cuerpo.expires_in ?? 3600) * 1000 };
  return tokenGuardado.valor;
}

// ------------------------------------------------------------------ el aviso

/**
 * Solo datos, sin «notification»: así lo pinta siempre la app, igual que sus demás avisos,
 * y puede callarse si el chat de ese grupo está abierto en pantalla.
 */
export function mensajeFcm(token: string, datos: Record<string, string>) {
  return {
    message: {
      token,
      data: datos,
      android: { priority: "HIGH", ttl: "86400s" },
    },
  };
}

export function datosDelAviso(
  tipo: "mensaje" | "examen",
  fila: Record<string, unknown>,
  grupoNombre: string,
  apodo: string,
): Record<string, string> {
  const comunes = {
    tipo,
    id: String(fila.id),
    grupo: String(fila.grupo),
    grupoNombre,
    apodo,
  };
  if (tipo === "mensaje") {
    return { ...comunes, texto: String(fila.texto ?? "").slice(0, MAX_TEXTO) };
  }
  return {
    ...comunes,
    asignatura: String(fila.asignatura ?? ""),
    fecha: String(fila.fecha ?? ""),
    hora: fila.hora ? String(fila.hora) : "",
  };
}

// ------------------------------------------------------------------ la petición

function json(estado: number, cuerpo: unknown): Response {
  return new Response(JSON.stringify(cuerpo), {
    status: estado,
    headers: { "Content-Type": "application/json" },
  });
}

export async function atender(peticion: Request, entorno: Entorno, pedir: Pedir = fetch): Promise<Response> {
  if (peticion.method !== "POST") return json(405, { error: "solo_post" });
  if (!entorno.cuenta) return json(503, { error: "sin_firebase" });

  let tipo: unknown, id: unknown;
  try {
    ({ tipo, id } = await peticion.json());
  } catch {
    return json(400, { error: "json_no_valido" });
  }
  if ((tipo !== "mensaje" && tipo !== "examen") || !Number.isSafeInteger(id) || (id as number) <= 0) {
    return json(400, { error: "datos_no_validos" });
  }

  const servidor = {
    apikey: entorno.claveServidor,
    Authorization: `Bearer ${entorno.claveServidor}`,
  };
  const rest = async (ruta: string, init: RequestInit = {}) => {
    const r = await pedir(`${entorno.url}/rest/v1/${ruta}`, {
      ...init,
      headers: { ...servidor, "Content-Type": "application/json", ...(init.headers ?? {}) },
    });
    if (!r.ok) throw new Error(`rest ${r.status} ${ruta.split("?")[0]}`);
    const texto = await r.text();
    return texto ? JSON.parse(texto) : null;
  };

  // Quién llama: lo dice Supabase a partir de su sesión, no lo que mande la app.
  const sesion = peticion.headers.get("Authorization") ?? "";
  const quien = await pedir(`${entorno.url}/auth/v1/user`, {
    headers: { apikey: entorno.claveServidor, Authorization: sesion },
  });
  if (!quien.ok) return json(401, { error: "sin_sesion" });
  const usuario: string | undefined = (await quien.json())?.id;
  if (!usuario) return json(401, { error: "sin_sesion" });

  try {
    const tabla = tipo === "mensaje" ? "mensajes" : "examenes";
    const fila = (await rest(`${tabla}?id=eq.${id}&select=*`))?.[0];
    if (!fila || fila.autor !== usuario) return json(403, { error: "no_es_tuyo" });

    const momento = Date.parse(String(tipo === "mensaje" ? fila.enviado : fila.creado));
    if (!Number.isFinite(momento) || Date.now() - momento > MAX_EDAD_MS) {
      return json(200, { avisados: 0, motivo: "antiguo" });
    }

    // Si ya estaba, no se inserta nada y no se vuelve a avisar.
    const nuevo = await rest("avisados", {
      method: "POST",
      headers: { Prefer: "resolution=ignore-duplicates,return=representation" },
      body: JSON.stringify({ tipo, id }),
    });
    if (!Array.isArray(nuevo) || nuevo.length === 0) return json(200, { avisados: 0, motivo: "repetido" });

    const grupo = (await rest(`grupos?id=eq.${fila.grupo}&select=nombre`))?.[0];
    const miembros: { usuario: string; apodo: string }[] =
      await rest(`miembros?grupo=eq.${fila.grupo}&select=usuario,apodo`) ?? [];
    const apodo = miembros.find((m) => m.usuario === usuario)?.apodo ?? "Alguien";
    const destinatarios = miembros.map((m) => m.usuario).filter((u) => u !== usuario);
    if (!grupo || destinatarios.length === 0) return json(200, { avisados: 0 });

    const moviles: { token: string }[] =
      await rest(`dispositivos?usuario=in.(${destinatarios.join(",")})&select=token`) ?? [];
    if (moviles.length === 0) return json(200, { avisados: 0 });

    const datos = datosDelAviso(tipo as "mensaje" | "examen", fila, grupo.nombre, apodo);
    const acceso = await tokenDeGoogle(entorno.cuenta, pedir);
    const envio = `https://fcm.googleapis.com/v1/projects/${entorno.cuenta.project_id}/messages:send`;

    let avisados = 0;
    await Promise.all(moviles.map(async ({ token }) => {
      const r = await pedir(envio, {
        method: "POST",
        headers: { Authorization: `Bearer ${acceso}`, "Content-Type": "application/json" },
        body: JSON.stringify(mensajeFcm(token, datos)),
      });
      if (r.ok) {
        avisados++;
        return;
      }
      // Una app desinstalada o un token caducado: se borra para no volver a intentarlo.
      const error = await r.text();
      if (r.status === 404 || /UNREGISTERED|INVALID_ARGUMENT/.test(error)) {
        await rest(`dispositivos?token=eq.${encodeURIComponent(token)}`, { method: "DELETE" });
      }
    }));
    return json(200, { avisados });
  } catch (e) {
    return json(500, { error: String((e as Error)?.message ?? e) });
  }
}

export function leerEntorno(leer: (n: string) => string | undefined): Entorno {
  let cuenta: Cuenta | null = null;
  try {
    const crudo = leer("FIREBASE_CUENTA");
    if (crudo) cuenta = JSON.parse(crudo);
  } catch {
    cuenta = null;
  }
  return {
    url: (leer("SUPABASE_URL") ?? "").replace(/\/+$/, ""),
    claveServidor: leer("SUPABASE_SERVICE_ROLE_KEY") ?? "",
    cuenta,
  };
}

// En Supabase (Deno) arranca el servidor; en las pruebas (Node) solo se importa.
const deno = (globalThis as any).Deno;
if (deno) {
  const entorno = leerEntorno((n) => deno.env.get(n));
  deno.serve((peticion: Request) => atender(peticion, entorno));
}
