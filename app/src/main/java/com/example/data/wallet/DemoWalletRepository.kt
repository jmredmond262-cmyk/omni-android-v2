package com.example.data.wallet

import com.example.model.TransactionType
import com.example.model.WalletTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.Timestamp
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

class DemoWalletRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
) {
    fun ensureWallet(done: (Result<DemoWalletAccount>) -> Unit) {
        functions.getHttpsCallable("ensureDemoWallet").call()
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                if (data == null) {
                    done(Result.failure(IllegalStateException("Firebase devolvió una cuenta DEMO inválida.")))
                    return@addOnSuccessListener
                }
                done(Result.success(DemoWalletAccount(
                    uid = data["uid"] as? String ?: "",
                    alias = data["alias"] as? String ?: "",
                    cvu = data["cvu"] as? String ?: "",
                    balance = (data["balance"] as? Number)?.toDouble() ?: 50000.0
                )))
            }
            .addOnFailureListener { done(Result.failure(it)) }
    }

    fun observeWallet(uid: String, onUpdate: (Result<DemoWalletSnapshot>) -> Unit): List<ListenerRegistration> {
        val accountRef = firestore.collection("walletAccounts").document(uid)
        var balance = 50000.0
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
            balance = (snapshot.get("balance") as? Number)?.toDouble() ?: balance
            cashback = (snapshot.get("cashbackTotal") as? Number)?.toDouble() ?: 0.0
            alias = snapshot.getString("alias").orEmpty()
            cvu = snapshot.getString("cvu").orEmpty()
            publish()
        }
        val transactionsListener = accountRef.collection("transactions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onUpdate(Result.failure(error))
                    return@addSnapshotListener
                }
                transactions = snapshot?.documents.orEmpty().map { document ->
                    val rawType = document.getString("type").orEmpty()
                    val type = runCatching { TransactionType.valueOf(rawType) }
                        .getOrDefault(TransactionType.CARGA_SALDO)
                    val timestamp = document.getTimestamp("createdAt")
                    val dateText = timestamp?.toDate()?.let {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                    } ?: "Procesando…"
                    WalletTransaction(
                        id = document.getString("operationId") ?: document.id,
                        title = document.getString("title").orEmpty(),
                        storeOrEntityName = document.getString("peerName").orEmpty(),
                        amount = (document.get("amount") as? Number)?.toDouble() ?: 0.0,
                        type = type,
                        dateText = dateText,
                        status = document.getString("status") ?: "APROBADA · DEMO"
                    )
                }
                publish()
            }
        return listOf(accountListener, transactionsListener)
    }

    fun transferDemoFunds(identifier: String, amount: Double, done: (Result<String>) -> Unit) {
        callOperation("transferDemoFunds", mapOf(
            "recipientIdentifier" to identifier.trim(),
            "amount" to amount,
            "operationId" to UUID.randomUUID().toString()
        ), done)
    }

    fun payDemoQr(payload: String, amount: Double, done: (Result<String>) -> Unit) {
        callOperation("payDemoQr", mapOf(
            "qrPayload" to payload,
            "amount" to amount,
            "operationId" to UUID.randomUUID().toString()
        ), done)
    }

    fun topUpDemo(amount: Double, done: (Result<String>) -> Unit) {
        callOperation("topUpDemoWallet", mapOf(
            "amount" to amount,
            "operationId" to UUID.randomUUID().toString()
        ), done)
    }

    private fun callOperation(
        name: String,
        data: Map<String, Any>,
        done: (Result<String>) -> Unit
    ) {
        functions.getHttpsCallable(name).call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<*, *>
                val status = response?.get("status") as? String ?: "APROBADA · DEMO"
                done(Result.success(status))
            }
            .addOnFailureListener { done(Result.failure(it)) }
    }

    companion object {
        fun qrPayload(uid: String, alias: String): String = "OMNI-DEMO|$uid|$alias"
    }
}
