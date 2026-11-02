import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { randomInt, randomUUID } from "node:crypto";

initializeApp();
const db = getFirestore();
const REGION = "us-central1";
const INITIAL_BALANCE = 50000;
const MAX_TOPUP_PER_CALL = 50000;
const MAX_TOPUP_LIFETIME = 150000;

function requireUid(request: { auth?: { uid: string } | null }): string {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Iniciá sesión para usar OMNI Pay DEMO.");
  return uid;
}
function parseAmount(value: unknown): number {
  const amount = Number(value);
  if (!Number.isFinite(amount) || amount <= 0 || amount > 10000000) {
    throw new HttpsError("invalid-argument", "El importe debe ser mayor que cero y válido.");
  }
  return Math.round(amount * 100) / 100;
}
function accountRef(uid: string) { return db.collection("walletAccounts").doc(uid); }
function safeName(value: unknown): string {
  return typeof value === "string" && value.trim() ? value.trim().slice(0, 80) : "Usuario OMNI";
}

export const ensureDemoWallet = onCall({ region: REGION }, async (request) => {
  const uid = requireUid(request);
  const ref = accountRef(uid);
  const alias = "omni" + uid.replace(/[^a-zA-Z0-9]/g, "").slice(0, 10).toLowerCase();
  const aliasRef = db.collection("walletAliases").doc(alias);
  const displayName = safeName(request.auth?.token.name);
  const result = await db.runTransaction(async (tx) => {
    const existing = await tx.get(ref);
    if (existing.exists) return existing.data()!;
    const aliasOwner = await tx.get(aliasRef);
    if (aliasOwner.exists && aliasOwner.get("uid") !== uid) {
      throw new HttpsError("already-exists", "No se pudo reservar el alias automático. Contactá soporte DEMO.");
    }
    const cvu = "00000031" + Array.from({ length: 14 }, () => randomInt(0, 10)).join("");
    const account = {
      uid, displayName, alias, cvu, balance: INITIAL_BALANCE,
      cashbackTotal: 0, topUpTotal: 0, currency: "OM$", environment: "DEMO",
      createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp()
    };
    tx.create(ref, account);
    tx.set(aliasRef, { uid, createdAt: FieldValue.serverTimestamp() });
    return account;
  });
  return { uid, alias: result.alias, cvu: result.cvu, balance: result.balance, currency: "OM$", environment: "DEMO" };
});

async function resolveRecipient(identifier: string): Promise<string> {
  const clean = identifier.trim();
  if (!clean) throw new HttpsError("invalid-argument", "Ingresá un alias o CVU.");
  if (/^[A-Za-z0-9_-]{20,}$/.test(clean)) {
    const direct = await accountRef(clean).get();
    if (direct.exists) return clean;
  }
  const alias = await db.collection("walletAliases").doc(clean.toLowerCase()).get();
  if (alias.exists) return String(alias.get("uid"));
  const byCvu = await db.collection("walletAccounts").where("cvu", "==", clean).limit(1).get();
  if (!byCvu.empty) return byCvu.docs[0].id;
  throw new HttpsError("not-found", "No encontramos una cuenta OMNI Pay DEMO con ese alias o CVU.");
}

async function executeTransfer(senderUid: string, recipientUid: string, amountValue: unknown, kind: "TRANSFERENCIA" | "QR", operationIdValue: unknown) {
  if (senderUid === recipientUid) throw new HttpsError("failed-precondition", "No podés transferirte a tu propia cuenta.");
  const amount = parseAmount(amountValue);
  const operationId = typeof operationIdValue === "string" && /^[A-Za-z0-9_-]{12,80}$/.test(operationIdValue)
    ? operationIdValue : randomUUID();
  const senderRef = accountRef(senderUid);
  const recipientRef = accountRef(recipientUid);
  const senderTx = senderRef.collection("transactions").doc(operationId);
  const recipientTx = recipientRef.collection("transactions").doc(operationId);
  return db.runTransaction(async (tx) => {
    const [senderSnap, recipientSnap, senderTxSnap, recipientTxSnap] = await Promise.all([
      tx.get(senderRef), tx.get(recipientRef), tx.get(senderTx), tx.get(recipientTx)
    ]);
    if (senderTxSnap.exists || recipientTxSnap.exists) return { operationId, status: "APROBADA · DEMO", duplicate: true };
    if (!senderSnap.exists || !recipientSnap.exists) throw new HttpsError("not-found", "Una de las cuentas DEMO no está inicializada.");
    const senderBalance = Number(senderSnap.get("balance") || 0);
    const recipientBalance = Number(recipientSnap.get("balance") || 0);
    if (senderBalance < amount) throw new HttpsError("failed-precondition", "Saldo virtual insuficiente.");
    const senderName = safeName(requestName(senderSnap.data()));
    const recipientName = safeName(requestName(recipientSnap.data()));
    const now = FieldValue.serverTimestamp();
    tx.update(senderRef, { balance: Math.round((senderBalance - amount) * 100) / 100, updatedAt: now });
    tx.update(recipientRef, { balance: Math.round((recipientBalance + amount) * 100) / 100, updatedAt: now });
    tx.create(senderTx, {
      operationId, type: kind === "QR" ? "QR_PAGO" : "TRANSFERENCIA_ENVIADA",
      title: kind === "QR" ? "Pago QR enviado" : "Transferencia enviada",
      peerUid: recipientUid, peerName: recipientName, amount: -amount,
      status: "APROBADA · DEMO", environment: "DEMO", createdAt: now
    });
    tx.create(recipientTx, {
      operationId, type: kind === "QR" ? "QR_COBRO" : "TRANSFERENCIA_RECIBIDA",
      title: kind === "QR" ? "Cobro QR recibido" : "Transferencia recibida",
      peerUid: senderUid, peerName: senderName, amount,
      status: "APROBADA · DEMO", environment: "DEMO", createdAt: now
    });
    return { operationId, status: "APROBADA · DEMO", duplicate: false };
  });
}
function requestName(data: Record<string, unknown> | undefined): unknown { return data?.displayName; }

export const transferDemoFunds = onCall({ region: REGION }, async (request) => {
  const uid = requireUid(request);
  const identifier = String(request.data?.recipientIdentifier || "");
  const recipientUid = await resolveRecipient(identifier);
  return executeTransfer(uid, recipientUid, request.data?.amount, "TRANSFERENCIA", request.data?.operationId);
});

export const payDemoQr = onCall({ region: REGION }, async (request) => {
  const uid = requireUid(request);
  const payload = String(request.data?.qrPayload || "");
  const parts = payload.split("|");
  if (parts.length !== 3 || parts[0] !== "OMNI-DEMO" || !parts[1] || !parts[2]) {
    throw new HttpsError("invalid-argument", "El QR no pertenece a una cuenta OMNI Pay DEMO.");
  }
  const recipientUid = parts[1];
  const recipient = await accountRef(recipientUid).get();
  if (!recipient.exists || recipient.get("alias") !== parts[2]) {
    throw new HttpsError("not-found", "El QR está vencido o no corresponde a una cuenta válida.");
  }
  return executeTransfer(uid, recipientUid, request.data?.amount, "QR", request.data?.operationId);
});

export const topUpDemoWallet = onCall({ region: REGION }, async (request) => {
  const uid = requireUid(request);
  const amount = parseAmount(request.data?.amount);
  if (amount > MAX_TOPUP_PER_CALL) throw new HttpsError("invalid-argument", "La carga máxima por operación es OM$ 50.000.");
  const ref = accountRef(uid);
  const operationId = typeof request.data?.operationId === "string" ? request.data.operationId : randomUUID();
  const txRef = ref.collection("transactions").doc(operationId);
  return db.runTransaction(async (tx) => {
    const [account, existingTx] = await Promise.all([tx.get(ref), tx.get(txRef)]);
    if (existingTx.exists) return { operationId, status: "APROBADA · DEMO", duplicate: true };
    if (!account.exists) throw new HttpsError("failed-precondition", "Primero inicializá tu cuenta OMNI Pay DEMO.");
    const topUpTotal = Number(account.get("topUpTotal") || 0);
    if (topUpTotal + amount > MAX_TOPUP_LIFETIME) throw new HttpsError("resource-exhausted", "Alcanzaste el límite acumulado de cargas DEMO de OM$ 150.000.");
    tx.update(ref, {
      balance: Math.round((Number(account.get("balance") || 0) + amount) * 100) / 100,
      topUpTotal: Math.round((topUpTotal + amount) * 100) / 100,
      updatedAt: FieldValue.serverTimestamp()
    });
    tx.create(txRef, {
      operationId, type: "CARGA_SALDO", title: "Carga de saldo DEMO",
      peerUid: uid, peerName: "Simulador OMNI", amount,
      status: "APROBADA · DEMO", environment: "DEMO", createdAt: FieldValue.serverTimestamp()
    });
    return { operationId, status: "APROBADA · DEMO", duplicate: false };
  });
});
