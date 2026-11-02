package com.example.data.wallet

import com.example.model.TransactionType
import com.example.model.WalletTransaction
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.example.data.analytics.UsageAnalyticsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DemoWalletAccount(
    val uid: String,
    val alias: String,
    val cvu: String,
    val balance: Double
)

data class DemoWalletSnapshot(
    val balance: Double,
    val cashbackTotal: Double,
    val alias: String,
    val cvu: String,
    val transactions: List<WalletTransaction>
)

/**
 * OMNI Pay sandbox on Firebase Spark.
 * All credits are fictitious. Firestore transactions keep both sides in sync.
 * This is a test wallet, never use it to store or move real money.
 */
class DemoWalletRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun ensureWallet(done: (Result<DemoWalletAccount>) -> Unit) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            done(Result.failure(IllegalStateException("Iniciá sesión para crear tu billetera DEMO.")))
            return
        }
        val accountRef = firestore.collection("walletAccounts").document(uid)
        val alias = "omni-" + uid.takeLast(8).lowercase(Locale.ROOT)
        val cvu = UUID.nameUUIDFromBytes(uid.toByteArray())
            .toString().filter { it.isDigit() }.padEnd(22, '0').take(22)
        firestore.runTransaction { transaction ->
            val existing = transaction.get(accountRef)
            if (existing.exists()) {
                DemoWalletAccount(
                    uid, existing.getString("alias").orEmpty(),
                    existing.getString("cvu").orEmpty(),
                    (existing.get("balance") as? Number)?.toDouble() ?: 0.0
                )
            } else {
                val directoryRef = firestore.collection("walletDirectory").document(alias)
                transaction.set(accountRef, mapOf(
                    "uid" to uid, "alias" to alias, "cvu" to cvu,
                    "balance" to 50000.0, "cashbackTotal" to 0.0,
                    "depositedTotal" to 0.0, "lastOperationId" to "welcome",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                transaction.set(directoryRef, mapOf("uid" to uid, "alias" to alias, "cvu" to cvu))
                transaction.set(accountRef.collection("transactions").document("welcome"), mapOf(
                    "operationId" to "welcome", "type" to TransactionType.CARGA_SALDO.name,
                    "title" to "Crédito inicial de prueba", "peerName" to "OMNI Pay DEMO",
                    "amount" to 50000.0, "status" to "APROBADA · DEMO",
                    "createdAt" to FieldValue.serverTimestamp()
                ))
                DemoWalletAccount(uid, alias, cvu, 50000.0)
            }
        }.addOnSuccessListener { done(Result.success(it)) }
            .addOnFailureListener { done(Result.failure(it)) }
    }

    fun observeWallet(uid: String, onUpdate: (Result<DemoWalletSnapshot>) -> Unit): List<ListenerRegistration> {
        val accountRef = firestore.collection("walletAccounts").document(uid)
        var balance = 0.0
        var cashback = 0.0
        var alias = ""
        var cvu = ""
        var transactions = emptyList<WalletTransaction>()
        fun publish() = onUpdate(Result.success(DemoWalletSnapshot(balance, cashback, alias, cvu, transactions)))

        val accountListener = accountRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onUpdate(Result.failure(error))
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
            balance = (snapshot.get("balance") as? Number)?.toDouble() ?: 0.0
            cashback = (snapshot.get("cashbackTotal") as? Number)?.toDouble() ?: 0.0
            alias = snapshot.getString("alias").orEmpty()
            cvu = snapshot.getString("cvu").orEmpty()
            publish()
        }
        val transactionsListener = accountRef.collection("transactions")
            .orderBy("createdAt", Query.Direction.DESCENDING).limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onUpdate(Result.failure(error))
                    return@addSnapshotListener
                }
                transactions = snapshot?.documents.orEmpty().map { document ->
                    val type = runCatching {
                        TransactionType.valueOf(document.getString("type").orEmpty())
                    }.getOrDefault(TransactionType.CARGA_SALDO)
                    val dateText = document.getTimestamp("createdAt")?.toDate()?.let {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                    } ?: "Procesando…"
                    WalletTransaction(
                        id = document.getString("operationId") ?: document.id,
                        title = document.getString("title").orEmpty(),
                        storeOrEntityName = document.getString("peerName").orEmpty(),
                        amount = (document.get("amount") as? Number)?.toDouble() ?: 0.0,
                        type = type, dateText = dateText,
                        status = document.getString("status") ?: "APROBADA · DEMO"
                    )
                }
                publish()
            }
        return listOf(accountListener, transactionsListener)
    }

    fun transferDemoFunds(identifier: String, amount: Double, done: (Result<String>) -> Unit) {
        if (!validAmount(amount)) {
            done(Result.failure(IllegalArgumentException("El importe debe ser mayor que cero y no superar OM$ 50.000.")))
            return
        }
        val input = identifier.trim().lowercase(Locale.ROOT)
        if (input.isBlank()) {
            done(Result.failure(IllegalArgumentException("Ingresá un alias o CVU.")))
            return
        }
        val directory = firestore.collection("walletDirectory")
        val onResolved: (String?) -> Unit = { recipientUid ->
            if (recipientUid.isNullOrBlank()) {
                done(Result.failure(IllegalArgumentException("No encontramos una cuenta OMNI Pay con ese alias o CVU.")))
            } else {
                executeTransfer(recipientUid, amount, false, done)
            }
        }
        if (input.startsWith("omni-")) {
            directory.document(input).get()
                .addOnSuccessListener { onResolved(it.getString("uid")) }
                .addOnFailureListener { done(Result.failure(it)) }
        } else {
            directory.whereEqualTo("cvu", input).limit(1).get()
                .addOnSuccessListener { onResolved(it.documents.firstOrNull()?.getString("uid")) }
                .addOnFailureListener { done(Result.failure(it)) }
        }
    }

    fun payDemoQr(payload: String, amount: Double, done: (Result<String>) -> Unit) {
        val parts = payload.split("|")
        if (parts.size < 3 || parts[0] != "OMNI-DEMO") {
            done(Result.failure(IllegalArgumentException("El QR no pertenece a OMNI Pay DEMO.")))
            return
        }
        if (!validAmount(amount)) {
            done(Result.failure(IllegalArgumentException("El importe no es válido.")))
            return
        }
        executeTransfer(parts[1], amount, true, done)
    }

    fun topUpDemo(amount: Double, done: (Result<String>) -> Unit) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank() || !validAmount(amount)) {
            done(Result.failure(IllegalArgumentException("Iniciá sesión e ingresá un importe válido (máximo OM$ 50.000).")))
            return
        }
        val accountRef = firestore.collection("walletAccounts").document(uid)
        val operationId = UUID.randomUUID().toString()
        val operationRef = firestore.collection("walletOperations").document(operationId)
        firestore.runTransaction { tx ->
            val account = tx.get(accountRef)
            check(account.exists()) { "No existe tu billetera DEMO." }
            val deposited = (account.get("depositedTotal") as? Number)?.toDouble() ?: 0.0
            check(deposited + amount <= 150000.0) { "Alcanzaste el límite de cargas DEMO de OM$ 150.000." }
            val before = (account.get("balance") as? Number)?.toDouble() ?: 0.0
            tx.update(accountRef, mapOf(
                "balance" to before + amount, "depositedTotal" to deposited + amount,
                "lastOperationId" to operationId, "updatedAt" to FieldValue.serverTimestamp()
            ))
            tx.set(operationRef, mapOf(
                "operationId" to operationId, "kind" to "DEPOSIT",
                "senderUid" to uid, "recipientUid" to uid, "amount" to amount,
                "createdAt" to FieldValue.serverTimestamp()
            ))
            tx.set(accountRef.collection("transactions").document(operationId), transactionData(
                operationId, TransactionType.CARGA_SALDO, "Carga virtual de prueba",
                "OMNI Pay DEMO", amount
            ))
            "CARGA ACREDITADA · DEMO"
        }.addOnSuccessListener {
            UsageAnalyticsRepository.trackWalletOperation(uid, "DEMO_DEPOSIT")
            done(Result.success(it))
        }.addOnFailureListener { done(Result.failure(it)) }
    }

    private fun executeTransfer(
        recipientUid: String,
        amount: Double,
        isQr: Boolean,
        done: (Result<String>) -> Unit
    ) {
        val senderUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (senderUid.isNullOrBlank()) {
            done(Result.failure(IllegalStateException("Iniciá sesión para operar.")))
            return
        }
        if (senderUid == recipientUid) {
            done(Result.failure(IllegalArgumentException("No podés transferirte a tu propia cuenta.")))
            return
        }
        val senderRef = firestore.collection("walletAccounts").document(senderUid)
        val recipientRef = firestore.collection("walletAccounts").document(recipientUid)
        val operationId = UUID.randomUUID().toString()
        val operationRef = firestore.collection("walletOperations").document(operationId)
        firestore.runTransaction { tx ->
            val sender = tx.get(senderRef)
            check(sender.exists()) { "Tu billetera DEMO no está disponible." }
            val senderBalance = (sender.get("balance") as? Number)?.toDouble() ?: 0.0
            check(senderBalance >= amount) { "Saldo insuficiente para completar la operación." }
            val recipientAlias = "Cuenta OMNI Pay"
            val senderAlias = sender.getString("alias").orEmpty()
            val senderType = if (isQr) TransactionType.QR_PAGO else TransactionType.TRANSFERENCIA_ENVIADA
            val recipientType = if (isQr) TransactionType.QR_COBRO else TransactionType.TRANSFERENCIA_RECIBIDA
            tx.update(senderRef, mapOf(
                "balance" to senderBalance - amount, "lastOperationId" to operationId,
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            tx.update(recipientRef, mapOf(
                "balance" to FieldValue.increment(amount), "lastOperationId" to operationId,
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            tx.set(operationRef, mapOf(
                "operationId" to operationId, "kind" to if (isQr) "QR_PAYMENT" else "TRANSFER",
                "senderUid" to senderUid, "recipientUid" to recipientUid,
                "amount" to amount, "createdAt" to FieldValue.serverTimestamp()
            ))
            tx.set(senderRef.collection("transactions").document(operationId), transactionData(
                operationId, senderType, if (isQr) "Pago con QR" else "Transferencia enviada",
                recipientAlias, amount
            ))
            tx.set(recipientRef.collection("transactions").document(operationId), transactionData(
                operationId, recipientType, if (isQr) "Cobro con QR" else "Transferencia recibida",
                senderAlias, amount
            ))
            if (isQr) "PAGO QR APROBADO · DEMO" else "TRANSFERENCIA APROBADA · DEMO"
        }.addOnSuccessListener {
            UsageAnalyticsRepository.trackWalletOperation(senderUid, if (isQr) "DEMO_QR_PAYMENT" else "DEMO_TRANSFER")
            done(Result.success(it))
        }.addOnFailureListener { done(Result.failure(it)) }
    }

    private fun transactionData(
        operationId: String, type: TransactionType, title: String, peerName: String, amount: Double
    ) = mapOf(
        "operationId" to operationId, "type" to type.name, "title" to title,
        "peerName" to peerName, "amount" to amount,
        "status" to "APROBADA · DEMO", "createdAt" to FieldValue.serverTimestamp()
    )

    private fun validAmount(amount: Double) = amount.isFinite() && amount > 0.0 && amount <= 50000.0

    companion object {
        fun qrPayload(uid: String, alias: String): String = "OMNI-DEMO|$uid|$alias"
    }
}
