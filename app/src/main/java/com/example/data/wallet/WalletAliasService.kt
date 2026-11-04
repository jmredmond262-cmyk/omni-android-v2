package com.example.data.wallet

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

data class WalletAliasResult(val alias: String, val cvu: String)

/**
 * Shared alias index. The account and walletDirectory index are updated atomically,
 * so a rename cannot leave the previous alias pointing at the account.
 */
class WalletAliasService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun resolveRecipientUid(query: String, done: (Result<String>) -> Unit) {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        if (cleanQuery.isBlank()) {
            done(Result.failure(IllegalArgumentException("Ingresá un alias o CVU.")))
            return
        }
        val directory = firestore.collection("walletDirectory")
        directory.document(cleanQuery).get()
            .addOnSuccessListener { aliasSnapshot ->
                val aliasUid = aliasSnapshot.getString("uid")
                if (!aliasUid.isNullOrBlank()) {
                    done(Result.success(aliasUid))
                } else {
                    directory.whereEqualTo("cvu", cleanQuery).limit(1).get()
                        .addOnSuccessListener { result ->
                            val uid = result.documents.firstOrNull()?.getString("uid")
                            if (uid.isNullOrBlank()) done(Result.failure(
                                IllegalArgumentException("No encontramos una cuenta OMNI Pay con ese alias o CVU.")
                            )) else done(Result.success(uid))
                        }
                        .addOnFailureListener { done(Result.failure(it)) }
                }
            }
            .addOnFailureListener { done(Result.failure(it)) }
    }

    fun updateAlias(rawAlias: String, done: (Result<WalletAliasResult>) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            done(Result.failure(IllegalStateException("Iniciá sesión para actualizar tu alias.")))
            return
        }
        val cleanQuery = rawAlias.trim().lowercase(Locale.ROOT)
        if (!cleanQuery.matches(Regex("^[a-z0-9][a-z0-9._-]{2,29}$"))) {
            done(Result.failure(IllegalArgumentException(
                "El alias debe tener entre 3 y 30 caracteres: letras, números, punto, guion o guion bajo."
            )))
            return
        }

        val accountRef = firestore.collection("walletAccounts").document(uid)
        val directory = firestore.collection("walletDirectory")
        val newAliasRef = directory.document(cleanQuery)

        firestore.runTransaction { transaction ->
            val account = transaction.get(accountRef)
            check(account.exists()) { "No encontramos tu billetera OMNI Pay DEMO." }
            val oldAlias = account.getString("alias").orEmpty().trim().lowercase(Locale.ROOT)
            val cvu = account.getString("cvu").orEmpty()
            check(cvu.isNotBlank()) { "La cuenta no tiene un CVU virtual válido." }

            val newAlias = transaction.get(newAliasRef)
            check(!newAlias.exists() || newAlias.getString("uid") == uid) {
                "Ese alias ya está asociado a otra cuenta OMNI."
            }

            val oldAliasRef = if (oldAlias.isNotBlank() && oldAlias != cleanQuery) directory.document(oldAlias) else null
            val oldAliasSnapshot = oldAliasRef?.let { transaction.get(it) }

            if (oldAlias != cleanQuery) {
                if (oldAliasSnapshot?.exists() == true && oldAliasSnapshot.getString("uid") == uid) {
                    transaction.delete(oldAliasRef!!)
                }
                transaction.set(newAliasRef, mapOf("uid" to uid, "alias" to cleanQuery, "cvu" to cvu))
                transaction.update(accountRef, mapOf(
                    "alias" to cleanQuery,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
            } else if (!newAlias.exists()) {
                transaction.set(newAliasRef, mapOf("uid" to uid, "alias" to cleanQuery, "cvu" to cvu))
            }
            WalletAliasResult(cleanQuery, cvu)
        }.addOnSuccessListener { done(Result.success(it)) }
            .addOnFailureListener { done(Result.failure(it)) }
    }
}
