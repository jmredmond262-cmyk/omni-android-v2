const PRIMARY_PROJECT = "omni-57f69";
const ADMIN_AUTH_PROJECT = "omni-admin-52e3f";
const FIRESTORE_ROOT = `https://firestore.googleapis.com/v1/projects/${PRIMARY_PROJECT}/databases/(default)/documents`;
const TOKEN_ISSUER = `https://securetoken.google.com/${ADMIN_AUTH_PROJECT}`;
let serviceTokenCache = { token: "", expiresAt: 0 };
let firebaseKeysCache = { keys: [], expiresAt: 0 };

function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff" } });
}
function base64urlBytes(value) {
  const padded = value.replace(/-/g, "+").replace(/_/g, "/") + "=".repeat((4 - value.length % 4) % 4);
  const raw = atob(padded);
  return Uint8Array.from(raw, c => c.charCodeAt(0));
}
function decodeJwtPart(value) {
  return JSON.parse(new TextDecoder().decode(base64urlBytes(value)));
}
function pemBytes(pem) {
  const body = pem.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, "");
  const raw = atob(body);
  return Uint8Array.from(raw, c => c.charCodeAt(0));
}
function encodeBase64Url(bytes) {
  let binary = "";
  for (const byte of new Uint8Array(bytes)) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}
async function verifyFirebaseToken(token) {
  const parts = token.split(".");
  if (parts.length !== 3) throw new ApiError(401, "unauthenticated", "La sesión administrativa no es válida o venció. Ingresá nuevamente.");
  const header = decodeJwtPart(parts[0]);
  const claims = decodeJwtPart(parts[1]);
  const now = Math.floor(Date.now() / 1000);
  if (header.alg !== "RS256" || !header.kid || claims.aud !== ADMIN_AUTH_PROJECT ||
      claims.iss !== TOKEN_ISSUER || !claims.sub || claims.sub.length > 128 ||
      !Number.isFinite(claims.exp) || claims.exp <= now ||
      !Number.isFinite(claims.iat) || claims.iat > now + 300) {
    throw new ApiError(401, "unauthenticated", "La sesión administrativa no es válida o venció. Ingresá nuevamente.");
  }
  if (firebaseKeysCache.expiresAt < now || !firebaseKeysCache.keys.length) {
    const response = await fetch("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com");
    if (!response.ok) throw new Error("No se pudieron obtener las claves de validación de Firebase.");
    const data = await response.json();
    firebaseKeysCache = { keys: data.keys || [], expiresAt: now + 3300 };
  }
  const jwk = firebaseKeysCache.keys.find(key => key.kid === header.kid);
  if (!jwk) throw new ApiError(401, "unauthenticated", "La firma de la sesión no es válida.");
  const publicKey = await crypto.subtle.importKey("jwk", jwk, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
  const valid = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", publicKey, base64urlBytes(parts[2]), new TextEncoder().encode(parts[0] + "." + parts[1]));
  if (!valid) throw new ApiError(401, "unauthenticated", "La firma de la sesión no es válida.");
  return { uid: claims.sub, email: String(claims.email || "") };
}
class ApiError extends Error {
  constructor(status, code, message) { super(message); this.status = status; this.code = code; }
}
async function googleAccessToken(env) {
  const now = Math.floor(Date.now() / 1000);
  if (serviceTokenCache.token && serviceTokenCache.expiresAt > now + 60) return serviceTokenCache.token;
  const account = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT_JSON);
  if (!account.client_email || !account.private_key || account.project_id !== PRIMARY_PROJECT) throw new Error("La credencial de Firebase no corresponde al proyecto principal esperado.");
  const header = encodeBase64Url(new TextEncoder().encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
  const claims = encodeBase64Url(new TextEncoder().encode(JSON.stringify({
    iss: account.client_email, scope: "https://www.googleapis.com/auth/datastore",
    aud: "https://oauth2.googleapis.com/token", iat: now, exp: now + 3600
  })));
  const unsigned = header + "." + claims;
  const key = await crypto.subtle.importKey("pkcs8", pemBytes(account.private_key), { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned));
  const assertion = unsigned + "." + encodeBase64Url(signature);
  const body = new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion });
  const response = await fetch("https://oauth2.googleapis.com/token", { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" }, body });
  const data = await response.json();
  if (!response.ok || !data.access_token) throw new Error("No se pudo autenticar el servicio de OMNI con Google.");
  serviceTokenCache = { token: data.access_token, expiresAt: now + Number(data.expires_in || 3600) };
  return data.access_token;
}
function decodeValue(value) {
  if (!value || typeof value !== "object") return value;
  if ("nullValue" in value) return null;
  if ("stringValue" in value) return value.stringValue;
  if ("booleanValue" in value) return value.booleanValue;
  if ("integerValue" in value) return Number(value.integerValue);
  if ("doubleValue" in value) return Number(value.doubleValue);
  if ("timestampValue" in value) return value.timestampValue;
  if ("referenceValue" in value) return value.referenceValue;
  if ("geoPointValue" in value) return value.geoPointValue;
  if ("arrayValue" in value) return (value.arrayValue.values || []).map(decodeValue);
  if ("mapValue" in value) return decodeFields(value.mapValue.fields || {});
  return value;
}
function decodeFields(fields) {
  return Object.fromEntries(Object.entries(fields || {}).map(([key, value]) => [key, decodeValue(value)]));
}
function encodeValue(value) {
  if (value === null || value === undefined) return { nullValue: "NULL_VALUE" };
  if (typeof value === "string") return { stringValue: value };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(encodeValue) } };
  if (typeof value === "object") return { mapValue: { fields: encodeFields(value) } };
  return { stringValue: String(value) };
}
function encodeFields(data) {
  return Object.fromEntries(Object.entries(data).map(([key, value]) => {
    if (typeof value === "string" && /(?:At|_at)$/.test(key) && Number.isFinite(Date.parse(value))) {
      return [key, { timestampValue: new Date(value).toISOString() }];
    }
    return [key, encodeValue(value)];
  }));
}
function documentData(doc) {
  return { id: doc.name.split("/").pop(), ...decodeFields(doc.fields || {}) };
}
async function firestore(env, path, options = {}) {
  const token = await googleAccessToken(env);
  const response = await fetch(FIRESTORE_ROOT + path, {
    ...options,
    headers: { Authorization: "Bearer " + token, "Content-Type": "application/json", ...(options.headers || {}) }
  });
  const raw = await response.text();
  let data = {};
  try { data = raw ? JSON.parse(raw) : {}; } catch {}
  if (!response.ok) {
    const message = data.error?.message || "Error al consultar la base de datos OMNI.";
    if (response.status === 404) return { missing: true };
    throw new Error(message);
  }
  return data;
}
async function getDocument(env, collection, id) {
  return firestore(env, "/" + encodeURIComponent(collection) + "/" + encodeURIComponent(id));
}
async function listDocuments(env, collection, limit = 100) {
  const pageSize = Math.min(Math.max(Number(limit) || 100, 1), 200);
  const data = await firestore(env, "/" + encodeURIComponent(collection) + "?pageSize=" + pageSize);
  return (data.documents || []).map(documentData);
}
async function countDocuments(env, collection) {
  const query = {
    structuredAggregationQuery: {
      structuredQuery: { from: [{ collectionId: collection }] },
      aggregations: [{ count: { } , alias: "total" }]
    }
  };
  const result = await firestore(env, ":runAggregationQuery", { method: "POST", body: JSON.stringify(query) });
  return Number(result[0]?.result?.aggregateFields?.total?.integerValue || 0);
}
async function recentDocuments(env, collection) {
  const query = {
    structuredQuery: {
      from: [{ collectionId: collection }],
      orderBy: [{ field: { fieldPath: "createdAt" }, direction: "DESCENDING" }],
      limit: 8
    }
  };
  const rows = await firestore(env, ":runQuery", { method: "POST", body: JSON.stringify({ structuredQuery: query.structuredQuery }) });
  return (rows || []).filter(row => row.document).map(row => documentData(row.document));
}
async function writeDocument(env, collection, id, data, merge = true) {
  const mask = Object.keys(data).map(key => "updateMask.fieldPaths=" + encodeURIComponent(key)).join("&");
  return firestore(env, "/" + encodeURIComponent(collection) + "/" + encodeURIComponent(id) + (merge && mask ? "?" + mask : ""), {
    method: "PATCH", body: JSON.stringify({ fields: encodeFields(data) })
  });
}
async function addDocument(env, collection, data) {
  const id = crypto.randomUUID();
  await firestore(env, "/" + encodeURIComponent(collection) + "?documentId=" + id, {
    method: "POST", body: JSON.stringify({ fields: encodeFields(data) })
  });
  return id;
}
async function audit(env, actor, action, target, details = {}) {
  await addDocument(env, "adminAuditLogs", {
    actorUid: actor.uid, actorEmail: actor.email, action, target, details,
    createdAt: new Date().toISOString()
  });
}
async function dashboard(env) {
  const collections = ["users", "walletAccounts", "walletOperations", "merchantProfiles", "storeOrders", "omniEvents", "appFeedback", "usageEvents"];
  const counts = await Promise.all(collections.map(name => countDocuments(env, name)));
  const [wallets, users, operations] = await Promise.all([
    listDocuments(env, "walletAccounts", 1000),
    recentDocuments(env, "users"),
    recentDocuments(env, "walletOperations")
  ]);
  return {
    counts: Object.fromEntries(collections.map((name, index) => [name, counts[index]])),
    walletBalanceSample: wallets.reduce((sum, item) => sum + (Number(item.balance) || 0), 0),
    walletSampleSize: wallets.length,
    recentUsers: users, recentOperations: operations
  };
}
async function handleAction(env, actor, action, data) {
  switch (action) {
    case "dashboard": return dashboard(env);
    case "list": {
      const collection = String(data.collection || "");
      const allowed = new Set(["users", "walletAccounts", "walletOperations", "merchantProfiles", "storeOrders", "omniEvents", "appFeedback", "usageEvents", "moduleRegistry", "adminAuditLogs", "betaInvitations"]);
      if (!allowed.has(collection)) throw new ApiError(400, "invalid-argument", "Colección no disponible.");
      return { items: await listDocuments(env, collection, data.limit) };
    }
    case "getConfig": {
      const snap = await getDocument(env, "adminConfig", "global");
      return { config: snap.missing ? {} : decodeFields(snap.fields || {}) };
    }
    case "saveConfig": {
      if (actor.role !== "owner") throw new ApiError(403, "permission-denied", "Solo el propietario puede modificar la configuración global.");
      const input = data.config && typeof data.config === "object" ? data.config : {};
      const allowed = ["maintenanceMode", "maintenanceMessage", "minimumVersion", "recommendedVersion", "globalNotice", "featureFlags"];
      const patch = Object.fromEntries(Object.entries(input).filter(([key]) => allowed.includes(key)));
      if (!Object.keys(patch).length) throw new ApiError(400, "invalid-argument", "No hay parámetros válidos para guardar.");
      await writeDocument(env, "adminConfig", "global", { ...patch, updatedAt: new Date().toISOString(), updatedBy: actor.uid });
      await audit(env, actor, "CONFIG_UPDATED", "adminConfig/global", { fields: Object.keys(patch) });
      return { saved: true };
    }
    case "saveModule": {
      if (actor.role !== "owner") throw new ApiError(403, "permission-denied", "Solo el propietario puede modificar módulos.");
      const id = String(data.id || "").trim();
      if (!/^[A-Za-z0-9_-]{2,64}$/.test(id)) throw new ApiError(400, "invalid-argument", "Identificador de módulo inválido.");
      const input = data.module && typeof data.module === "object" ? data.module : {};
      const patch = { title: String(input.title || id).slice(0, 80), description: String(input.description || "").slice(0, 500), enabled: input.enabled === true, updatedAt: new Date().toISOString(), updatedBy: actor.uid };
      await writeDocument(env, "moduleRegistry", id, patch);
      await audit(env, actor, "MODULE_UPDATED", "moduleRegistry/" + id, { enabled: patch.enabled });
      return { saved: true };
    }
    case "createInvitation": {
      const email = String(data.email || "").trim().toLowerCase();
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) throw new ApiError(400, "invalid-argument", "Ingresá un correo válido.");
      const id = await addDocument(env, "betaInvitations", { email, role: "beta", status: "PENDING", createdAt: new Date().toISOString(), createdBy: actor.uid });
      await audit(env, actor, "BETA_INVITATION_CREATED", "betaInvitations/" + id, { email });
      return { id, created: true };
    }
    default: throw new ApiError(400, "invalid-argument", "Acción administrativa desconocida.");
  }
}
export default {
  async fetch(request, env) {
    if (request.method !== "POST") return json({ ok: false, error: "method-not-allowed", message: "Usá POST." }, 405);
    try {
      const authHeader = request.headers.get("Authorization") || "";
      if (!authHeader.startsWith("Bearer ")) throw new ApiError(401, "unauthenticated", "Falta la sesión administrativa.");
      const identity = await verifyFirebaseToken(authHeader.slice(7).trim());
      const admin = await getDocument(env, "systemAdmins", identity.uid);
      if (admin.missing) throw new ApiError(403, "permission-denied", "Esta cuenta no está habilitada como administradora de OMNI.");
      const fields = decodeFields(admin.fields || {});
      const role = fields.role;
      if (fields.enabled !== true || !["owner", "admin"].includes(role)) throw new ApiError(403, "permission-denied", "Esta cuenta no está habilitada como administradora de OMNI.");
      const body = await request.json();
      const result = await handleAction(env, { ...identity, role }, String(body.action || ""), body.data && typeof body.data === "object" ? body.data : {});
      return json({ ok: true, result });
    } catch (error) {
      if (error instanceof ApiError) return json({ ok: false, error: error.code, message: error.message }, error.status);
      console.error("OMNI Admin API error", error);
      return json({ ok: false, error: "internal", message: "No se pudo completar la operación." }, 500);
    }
  }
};
