package com.example.data.auth

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun currentUser() = auth.currentUser

    fun signInEmail(email: String, password: String, done: (Result<Unit>) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) persistProfile(done)
                else done(Result.failure(task.exception ?: Exception("No se pudo iniciar sesión.")))
            }
    }

    fun registerEmail(name: String, email: String, password: String, done: (Result<Unit>) -> Unit) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    done(Result.failure(task.exception ?: Exception("No se pudo crear la cuenta.")))
                    return@addOnCompleteListener
                }
                val user = auth.currentUser
                val cleanName = name.trim()
                if (user != null && cleanName.isNotBlank()) {
                    val request = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(cleanName)
                        .build()
                    user.updateProfile(request).addOnCompleteListener {
                        persistProfile(done)
                    }
                } else persistProfile(done)
            }
    }

    fun requestPhoneCode(
        activity: Activity,
        phone: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone.trim())
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyPhoneCode(verificationId: String, code: String, done: (Result<Unit>) -> Unit) {
        val credential: PhoneAuthCredential =
            PhoneAuthProvider.getCredential(verificationId, code.trim())
        completePhoneCredential(credential, done)
    }

    fun completePhoneCredential(credential: PhoneAuthCredential, done: (Result<Unit>) -> Unit) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) persistProfile(done)
            else done(Result.failure(task.exception ?: Exception("El código no es válido.")))
        }
    }

    fun signInGoogle(idToken: String, done: (Result<Unit>) -> Unit) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) persistProfile(done)
            else done(Result.failure(task.exception ?: Exception("No se pudo ingresar con Google.")))
        }
    }

    fun signOut() = auth.signOut()

    /** Re-synchronizes an already authenticated session on app startup. */
    fun syncCurrentProfile(done: (Result<Unit>) -> Unit = {}) {
        persistProfile(done)
    }

    private fun persistProfile(done: (Result<Unit>) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            done(Result.failure(Exception("Firebase no devolvió un usuario autenticado.")))
            return
        }
        val emailName = user.email.orEmpty().substringBefore("@")
            .replace(Regex("[._-]+"), " ")
            .split(" ").filter { it.isNotBlank() }
            .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
        val resolvedName = user.displayName?.takeIf { it.isNotBlank() }
            ?: emailName.takeIf { it.isNotBlank() }
            ?: "Usuario OMNI"
        if (user.displayName.isNullOrBlank() && resolvedName != "Usuario OMNI") {
            user.updateProfile(
                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(resolvedName).build()
            )
        }
        val profile = hashMapOf<String, Any?>(
            "uid" to user.uid,
            "displayName" to resolvedName,
            "email" to (user.email ?: ""),
            "phoneNumber" to (user.phoneNumber ?: ""),
            "photoUrl" to user.photoUrl?.toString(),
            "providerIds" to user.providerData.map { it.providerId }.filter { it != "firebase" },
            "updatedAt" to FieldValue.serverTimestamp(),
            "lastSeenAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) profile["createdAt"] = FieldValue.serverTimestamp()
                firestore.collection("users").document(user.uid).set(profile, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener { done(Result.success(Unit)) }
                    .addOnFailureListener { error ->
                        // Authentication is valid even if profile synchronization is temporarily unavailable.
                        done(Result.failure(error))
                    }
            }
            .addOnFailureListener { done(Result.failure(it)) }
    }
}
