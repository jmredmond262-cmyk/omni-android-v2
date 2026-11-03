import { initializeApp, getApps } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore, Timestamp, type QuerySnapshot } from "firebase-admin/firestore";
import { defineString } from "firebase-functions/params";
import { HttpsError, onRequest } from "firebase-functions/v2/https";

const REGION = "us-central1";
const ADMIN_AUTH_PROJECT_ID = defineString("OMNI_ADMIN_AUTH_PROJECT_ID");
const primaryApp = getApps().find((app) => app.name === "[DEFAULT]") ?? initializeApp();
const db = getFirestore(primaryApp);
function getAuthVerifier() {
  const app = getApps().find((item) => item.name === "omni-admin-auth-verifier")
    ?? initializeApp({ projectId: ADMIN_AUTH_PROJECT_ID.value() }, "omni-admin-auth-verifier");
  return getAuth(app);
}

type AdminContext = { uid: string; email: string; role: "owner" | "admin" };

function bearerToken(header: string | undefined): string {
  if (!header?.startsWith("Bearer ")) throw new HttpsError("unauthenticated", "Falta la sesión administrativa.");
  return header.slice(7).trim();
}

async function authenticate(header: string | undefined): Promise<AdminContext> {
  const decoded = await getAuthVerifier().verifyIdToken(bearerToken(header));
  const adminSnap = await db.collection("systemAdmins").doc(decoded.uid).get();
  const role = adminSnap.get("role");
  if (!adminSnap.exists || adminSnap.get("enabled") !== true || (role !== "owner" && role !== "admin")) {
    throw new HttpsError("permission-denied", "Esta cuenta no está habilitada como administradora de OMNI.");
  }
  return { uid: decoded.uid, email: String(decoded.email ?? ""), role };
}

function clean(value: unknown): unknown {
  if (value instanceof Timestamp) return value.toDate().toISOString();
  if (Array.isArray(value)) return value.map(clean);
  if (value && typeof value === "object") {
    const source = value as Record<string, unknown>;
    return Object.fromEntries(Object.entries(source).map(([key, item]) => [key, clean(item)]));
  }
  if (typeof value === "bigint") return Number(value);
  return value;
}

function docList(snapshot: QuerySnapshot, limit = 100) {
  return snapshot.docs.slice(0, limit).map((doc) => ({ id: doc.id, ...clean(doc.data()) as object }));
}

async function count(collection: string): Promise<number> {
  const result = await db.collection(collection).count().get();
  return result.data().count;
}

async function audit(actor: AdminContext, action: string, target: string, details: Record<string, unknown> = {}) {
  await db.collection("adminAuditLogs").add({
    actorUid: actor.uid, actorEmail: actor.email, action, target,
    details, createdAt: FieldValue.serverTimestamp()
  });
}

async function dashboard() {
  const collections = ["users", "walletAccounts", "walletOperations", "merchantProfiles", "storeOrders", "omniEvents", "appFeedback", "usageEvents", "moduleRegistry"];
  const counts = await Promise.all(collections.map(count));
  const since = Timestamp.fromDate(new Date(Date.now() - 7 * 24 * 60 * 60 * 1000));
  const [wallets, users, operations, usage, profiles] = await Promise.all([
    db.collection("walletAccounts").limit(1000).get(),
    db.collection("users").orderBy("createdAt", "desc").limit(8).get(),
    db.collection("walletOperations").orderBy("createdAt", "desc").limit(8).get(),
    db.collection("usageEvents").where("createdAt", ">=", since).limit(5000).get(),
    db.collection("users").limit(5000).get()
  ]);
  const balance = wallets.docs.reduce((sum, doc) => sum + (Number(doc.get("balance")) || 0), 0);
  const events = usage.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
  const valueOf = (event: Record<string, unknown>, keys: string[]) =>
    keys.map((key) => event[key]).find((value) => value !== undefined && value !== null);
  const eventName = (event: Record<string, unknown>) => String(valueOf(event, ["eventType", "type", "event", "name", "action"]) ?? "UNKNOWN");
  const screenName = (event: Record<string, unknown>) => String(valueOf(event, ["screen", "screenName", "route"]) ?? "");
  const moduleName = (event: Record<string, unknown>) => String(valueOf(event, ["module", "moduleId", "feature"]) ?? "");
  const countBy = (values: string[]) => Object.entries(values.filter(Boolean).reduce<Record<string, number>>((acc, value) => {
    acc[value] = (acc[value] ?? 0) + 1; return acc;
  }, {})).map(([name, value]) => ({ name, count: value })).sort((a, b) => b.count - a.count);
  const sessions = new Set(events.map((event) => String(valueOf(event, ["sessionId", "session_id"]) ?? "")).filter(Boolean));
  const activeUsers = new Set(events.map((event) => String(valueOf(event, ["uid", "userId", "userUid"]) ?? "")).filter(Boolean));
  const screenEvents = events.filter((event) => /screen|view|navigation/i.test(eventName(event)));
  const actionEvents = events.filter((event) => !/screen|view|navigation/i.test(eventName(event)));
  const dayCounts: Record<string, number> = {};
  for (let offset = 6; offset >= 0; offset--) {
    const day = new Date(); day.setHours(0, 0, 0, 0); day.setDate(day.getDate() - offset);
    dayCounts[day.toISOString().slice(5, 10)] = 0;
  }
  for (const event of events) {
    const raw = valueOf(event, ["createdAt", "timestamp", "serverTimestamp"]);
    const date = raw instanceof Timestamp ? raw.toDate() : raw instanceof Date ? raw : null;
    if (date) {
      const key = date.toISOString().slice(5, 10);
      if (key in dayCounts) dayCounts[key]++;
    }
  }
  const diagnosticEventSeen = events.some((event) => /diagnostic|health.?check/i.test(eventName(event)));
  return {
    counts: Object.fromEntries(collections.map((name, index) => [name, counts[index]])),
    activeUsers7d: activeUsers.size,
    analytics: {
      sessions7d: sessions.size,
      screenViews7d: screenEvents.length,
      actions7d: actionEvents.length,
      averageSessionSeconds: 0,
      averageScreenSeconds: 0,
      featureMetrics: {
        aiCameraScans: events.filter((event) => /camera|scan|image.?analysis/i.test(eventName(event))).length,
        mapsInteractions: events.filter((event) => /map|route|place/i.test(eventName(event) + " " + moduleName(event))).length,
        jobInteractions: events.filter((event) => /job|work|employment/i.test(eventName(event) + " " + moduleName(event))).length,
        eventInteractions: events.filter((event) => /event/i.test(eventName(event) + " " + moduleName(event))).length,
        routineWeatherInteractions: events.filter((event) => /routine|weather|forecast/i.test(eventName(event) + " " + moduleName(event))).length,
        commerceShares: events.filter((event) => /share|whatsapp/i.test(eventName(event))).length
      },
      topScreens: countBy(screenEvents.map(screenName)).slice(0, 8),
      moduleUsage: countBy(events.map(moduleName)).slice(0, 10),
      eventBreakdown: countBy(events.map(eventName)).slice(0, 12)
    },
    usageTrend: Object.entries(dayCounts).map(([date, value]) => ({ date, count: value })),
    diagnostics: {
      usageEventsVisibleLast7Days: usage.size,
      diagnosticEventSeen,
      userProfilesVisible: profiles.size
    },
    walletBalanceSample: balance,
    walletSampleSize: wallets.size,
    recentUsers: docList(users, 8),
    recentOperations: docList(operations, 8),
    recentUsageEvents: docList(usage, 20)
  };
}

async function listCollection(name: string, limit = 100) {
  const allowed = new Set(["users", "walletAccounts", "walletOperations", "merchantProfiles", "storeOrders", "omniEvents", "appFeedback", "usageEvents", "moduleRegistry", "adminAuditLogs", "betaInvitations"]);
  if (!allowed.has(name)) throw new HttpsError("invalid-argument", "Colección no disponible.");
  const snapshot = await db.collection(name).limit(Math.min(Math.max(limit, 1), 200)).get();
  return docList(snapshot, limit);
}

async function handleAction(actor: AdminContext, action: string, data: Record<string, unknown>) {
  switch (action) {
    case "dashboard":
      return dashboard();
    case "list": {
      const collection = String(data.collection ?? "");
      return { items: await listCollection(collection, Number(data.limit) || 100) };
    }
    case "getConfig": {
      const snap = await db.collection("adminConfig").doc("global").get();
      return { config: snap.exists ? clean(snap.data()) : {} };
    }
    case "saveConfig": {
      if (actor.role !== "owner") throw new HttpsError("permission-denied", "Solo el propietario puede modificar la configuración global.");
      const allowed = ["maintenanceMode", "maintenanceMessage", "minimumVersion", "recommendedVersion", "globalNotice", "featureFlags", "updatedAt"];
      const patch = Object.fromEntries(Object.entries((data.config ?? {}) as Record<string, unknown>).filter(([key]) => allowed.includes(key) && key !== "updatedAt"));
      if (!Object.keys(patch).length) throw new HttpsError("invalid-argument", "No hay parámetros válidos para guardar.");
      await db.collection("adminConfig").doc("global").set({ ...patch, updatedAt: FieldValue.serverTimestamp(), updatedBy: actor.uid }, { merge: true });
      await audit(actor, "CONFIG_UPDATED", "adminConfig/global", { fields: Object.keys(patch) });
      return { saved: true };
    }
    case "saveModule": {
      if (actor.role !== "owner") throw new HttpsError("permission-denied", "Solo el propietario puede modificar módulos.");
      const id = String(data.id ?? "").trim();
      if (!/^[A-Za-z0-9_-]{2,64}$/.test(id)) throw new HttpsError("invalid-argument", "Identificador de módulo inválido.");
      const input = (data.module ?? {}) as Record<string, unknown>;
      const patch = {
        title: String(input.title ?? id).slice(0, 80),
        description: String(input.description ?? "").slice(0, 500),
        enabled: input.enabled === true,
        updatedAt: FieldValue.serverTimestamp(),
        updatedBy: actor.uid
      };
      await db.collection("moduleRegistry").doc(id).set(patch, { merge: true });
      await audit(actor, "MODULE_UPDATED", `moduleRegistry/${id}`, { enabled: patch.enabled });
      return { saved: true };
    }
    case "createInvitation": {
      const email = String(data.email ?? "").trim().toLowerCase();
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) throw new HttpsError("invalid-argument", "Ingresá un correo válido.");
      const ref = await db.collection("betaInvitations").add({
        email, role: "beta", status: "PENDING", createdAt: FieldValue.serverTimestamp(), createdBy: actor.uid
      });
      await audit(actor, "BETA_INVITATION_CREATED", ref.path, { email });
      return { id: ref.id, created: true };
    }
    default:
      throw new HttpsError("invalid-argument", "Acción administrativa desconocida.");
  }
}

export const omniAdminApi = onRequest({ region: REGION, timeoutSeconds: 60, memory: "512MiB" }, async (request, response) => {
  response.set("Cache-Control", "no-store");
  if (request.method !== "POST") {
    response.status(405).json({ error: "method-not-allowed", message: "Usá POST." });
    return;
  }
  try {
    const actor = await authenticate(request.header("authorization"));
    const action = String(request.body?.action ?? "");
    const data = (request.body?.data ?? {}) as Record<string, unknown>;
    const result = await handleAction(actor, action, data);
    response.status(200).json({ ok: true, result });
  } catch (error) {
    const authCode = (error as { code?: string })?.code ?? "";
    const code = error instanceof HttpsError ? error.code : authCode.startsWith("auth/") ? "unauthenticated" : "internal";
    const message = error instanceof Error ? error.message : "Error interno.";
    const status = code === "unauthenticated" ? 401 : code === "permission-denied" ? 403
      : code === "invalid-argument" ? 400 : 500;
    if (status === 500) console.error("omniAdminApi failed", error);
    response.status(status).json({ ok: false, error: code, message: status === 500 ? "No se pudo completar la operación." : code === "unauthenticated" ? "La sesión administrativa no es válida o venció. Ingresá nuevamente." : message });
  }
});
