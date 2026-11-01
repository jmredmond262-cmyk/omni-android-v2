package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.data.auth.AuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(activity: Activity, repository: AuthRepository = remember { AuthRepository() }) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var method by remember { mutableStateOf("email") }
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+54") }
    var smsCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    fun finish(result: Result<Unit>) {
        loading = false
        message = result.exceptionOrNull()?.localizedMessage
            ?: "Cuenta verificada. Sincronizando tu perfil…"
    }

    val phoneCallbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                loading = true
                repository.completePhoneCredential(credential, ::finish)
            }
            override fun onVerificationFailed(error: com.google.firebase.FirebaseException) {
                loading = false
                message = error.localizedMessage ?: "No se pudo enviar el SMS."
            }
            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
                loading = false
                message = "Código enviado por SMS. Ingresalo para continuar."
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 5.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("OMNI", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Text("Tu mundo, conectado.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Ingresá o creá tu cuenta para continuar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { method = "email"; message = "" }, modifier = Modifier.weight(1f)) { Text("Correo") }
                    OutlinedButton(onClick = { method = "phone"; message = "" }, modifier = Modifier.weight(1f)) { Text("Teléfono") }
                }

                if (method == "email") {
                    if (isRegister) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo electrónico") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            if (email.isBlank() || password.length < 6 || (isRegister && name.isBlank())) {
                                message = "Completá los datos. La contraseña debe tener al menos 6 caracteres."
                            } else {
                                loading = true
                                message = ""
                                if (isRegister) repository.registerEmail(name, email, password, ::finish)
                                else repository.signInEmail(email, password, ::finish)
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isRegister) "Crear cuenta" else "Ingresar") }
                    TextButton(onClick = { isRegister = !isRegister; message = "" }) {
                        Text(if (isRegister) "Ya tengo una cuenta · Iniciar sesión" else "No tengo cuenta · Registrarme")
                    }
                } else {
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono con código de país") }, placeholder = { Text("+549XXXXXXXXXX") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (verificationId == null) {
                        Button(onClick = {
                            if (!phone.startsWith("+") || phone.length < 8) message = "Ingresá el teléfono con código de país, por ejemplo +549..."
                            else {
                                loading = true
                                message = "Solicitando código…"
                                repository.requestPhoneCode(activity, phone, phoneCallbacks)
                            }
                        }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Enviar código SMS") }
                    } else {
                        OutlinedTextField(value = smsCode, onValueChange = { smsCode = it }, label = { Text("Código recibido") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            val id = verificationId
                            if (id == null || smsCode.length < 4) message = "Ingresá el código recibido."
                            else {
                                loading = true
                                repository.verifyPhoneCode(id, smsCode, ::finish)
                            }
                        }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Verificar teléfono") }
                        TextButton(onClick = { verificationId = null; smsCode = ""; message = "" }) { Text("Cambiar número") }
                    }
                }

                OutlinedButton(
                    onClick = {
                        loading = true
                        message = ""
                        scope.launch {
                            try {
                                val clientIdResource = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                                if (clientIdResource == 0) throw IllegalStateException("Falta el ID de cliente web de Google en google-services.json. Revisá el proveedor Google en Firebase.")
                                val googleOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(context.getString(clientIdResource))
                                    .setAutoSelectEnabled(false)
                                    .build()
                                val request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build()
                                val result = CredentialManager.create(context).getCredential(context, request)
                                val credential = result.credential
                                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                    repository.signInGoogle(token, ::finish)
                                } else {
                                    loading = false
                                    message = "Google no devolvió una credencial compatible."
                                }
                            } catch (error: Exception) {
                                loading = false
                                message = error.localizedMessage ?: "No se pudo iniciar sesión con Google."
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Continuar con Google") }

                if (loading) CircularProgressIndicator()
                if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(2.dp))
                Text("Tus datos de acceso se guardan de forma segura en Firebase.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
