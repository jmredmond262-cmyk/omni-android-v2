package com.omni.admin

import android.os.Bundle
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.util.Locale

private val Navy = Color(0xFF101827)
private val Panel = Color(0xFF192438)
private val Mint = Color(0xFF45D6B0)
private val Muted = Color(0xFF9EABC0)
private val White = Color(0xFFF5F7FB)
private val Money = NumberFormat.getNumberInstance(Locale("es", "AR"))

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Firebase is initialized by FirebaseInitProvider before the Activity starts.
    // Check it here, outside Compose, to keep platform initialization out of composition.
    val firebaseConfigured = FirebaseApp.getApps(this).isNotEmpty()
    setContent {
      MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, background = Navy, surface = Panel, onPrimary = Navy,
        onBackground = White, onSurface = White, secondary = Muted
      )) { OmniAdminApp(firebaseConfigured, this) }
    }
  }
}

private data class AdminRecord(val id: String, val title: String, val detail: String)
private data class DashboardData(
  val users: Int = 0, val wallets: Int = 0, val balance: Double = 0.0,
  val operations: Int = 0, val stores: Int = 0, val orders: Int = 0,
  val events: Int = 0, val recentUsers: List<AdminRecord> = emptyList(),
  val recentOperations: List<AdminRecord> = emptyList(), val loading: Boolean = true,
  val note: String = ""
)

@Composable
private fun OmniAdminApp(firebaseConfigured: Boolean, activity: Activity) {
  var user by remember { mutableStateOf(if (firebaseConfigured) FirebaseAuth.getInstance().currentUser else null) }
  var authorized by remember { mutableStateOf(false) }
  var checking by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var refresh by remember { mutableIntStateOf(0) }
  val scope = rememberCoroutineScope()

  fun signOut() {
    FirebaseAuth.getInstance().signOut()
    user = null
    password = ""
    error = ""
  }

  fun verifyAdmin(uid: String) {
    checking = true
    FirebaseFirestore.getInstance().collection("systemAdmins").document(uid).get()
      .addOnSuccessListener { doc ->
        val role = doc.getString("role") ?: ""
        authorized = doc.exists() && doc.getBoolean("enabled") == true && role in listOf("owner", "admin")
        checking = false
        if (!authorized) error = "Esta cuenta no tiene permisos administrativos. El propietario debe habilitarla en systemAdmins."
      }
      .addOnFailureListener {
        authorized = false; checking = false
        error = "No se pudo verificar el permiso: ${it.localizedMessage ?: "error de conexión"}"
      }
  }

  LaunchedEffect(user?.uid) {
    authorized = false
    error = ""
    user?.uid?.let(::verifyAdmin)
  }

  Surface(Modifier.fillMaxSize(), color = Navy) {
    when {
      !firebaseConfigured -> SetupNotice()
      user == null -> LoginScreen(activity, email, { email = it }, password, { password = it }, error,
        { authenticated -> user = authenticated }, { error = it })
      user!!.providerData.none { it.providerId == "password" } ->
        PasswordLinkScreen(user!!, { linkedUser -> user = linkedUser }, { error = it })
      checking -> LoadingScreen("Verificando permisos administrativos…")
      !authorized -> AccessDenied(error, ::signOut)
      else -> AdminHome(user!!.email ?: "Administrador", { refresh++ }, ::signOut)
    }
  }
}

@Composable
private fun SetupNotice() {
  Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
    Text("OMNI ADMIN", color = Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Text("Falta configurar Firebase", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text("Esta compilación necesita el archivo de configuración web/Android de Firebase para conectarse al proyecto OMNI. No se puede iniciar sesión hasta incorporar google-services.json.", color = Muted)
  }
}

@Composable
private fun LoginScreen(
  activity: Activity,
  email: String,
  onEmail: (String) -> Unit,
  password: String,
  onPassword: (String) -> Unit,
  error: String,
  onAuthenticated: (com.google.firebase.auth.FirebaseUser?) -> Unit,
  onError: (String) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val auth = remember { FirebaseAuth.getInstance() }
  var method by remember { mutableStateOf("email") }
  var phone by remember { mutableStateOf("+54") }
  var smsCode by remember { mutableStateOf("") }
  var verificationId by remember { mutableStateOf<String?>(null) }
  var loading by remember { mutableStateOf(false) }

  fun complete(task: com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>) {
    task.addOnSuccessListener { result -> loading = false; onAuthenticated(result.user) }
      .addOnFailureListener { ex -> loading = false; onError(ex.localizedMessage ?: "No se pudo iniciar sesión.") }
  }

  val phoneCallbacks = remember {
    object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
      override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
        loading = true
        complete(auth.signInWithCredential(credential))
      }
      override fun onVerificationFailed(ex: com.google.firebase.FirebaseException) {
        loading = false
        onError(ex.localizedMessage ?: "No se pudo verificar el teléfono.")
      }
      override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
        verificationId = id
        loading = false
        onError("Código enviado por SMS. Ingresalo para continuar.")
      }
    }
  }

  Column(Modifier.fillMaxSize().padding(26.dp), verticalArrangement = Arrangement.Center) {
    Text("OMNI", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Text("ADMIN", color = Mint, fontSize = 36.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(8.dp))
    Text("Ingresá con la misma identidad que usás en OMNI.", color = Muted)
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(selected = method == "email", onClick = { method = "email"; onError("") }, label = { Text("Correo") })
      FilterChip(selected = method == "phone", onClick = { method = "phone"; onError("") }, label = { Text("Teléfono") })
    }
    if (method == "email") {
      OutlinedTextField(email, onEmail, Modifier.fillMaxWidth(), label = { Text("Correo electrónico") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
      Spacer(Modifier.height(10.dp))
      OutlinedTextField(password, onPassword, Modifier.fillMaxWidth(), label = { Text("Contraseña") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
      Spacer(Modifier.height(14.dp))
      Button(onClick = { loading = true; onError(""); complete(auth.signInWithEmailAndPassword(email.trim(), password)) },
        Modifier.fillMaxWidth().height(50.dp), enabled = !loading && email.isNotBlank() && password.isNotBlank()) {
        Text("Ingresar con mi cuenta OMNI", fontWeight = FontWeight.Bold)
      }
    } else {
      OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Teléfono con código de país") }, placeholder = { Text("+549...") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
      if (verificationId == null) {
        Button(onClick = {
          if (!phone.startsWith("+") || phone.length < 8) onError("Ingresá el número con código de país, por ejemplo +549...")
          else {
            loading = true; onError("")
            val options = PhoneAuthOptions.newBuilder(auth).setPhoneNumber(phone.trim())
              .setTimeout(60L, TimeUnit.SECONDS).setActivity(activity).setCallbacks(phoneCallbacks).build()
            PhoneAuthProvider.verifyPhoneNumber(options)
          }
        }, Modifier.fillMaxWidth(), enabled = !loading) { Text("Enviar código SMS") }
      } else {
        OutlinedTextField(smsCode, { smsCode = it }, Modifier.fillMaxWidth(), label = { Text("Código SMS") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Button(onClick = {
          val id = verificationId
          if (id == null || smsCode.length < 4) onError("Ingresá el código recibido.")
          else { loading = true; onError(""); complete(auth.signInWithCredential(PhoneAuthProvider.getCredential(id, smsCode.trim()))) }
        }, Modifier.fillMaxWidth(), enabled = !loading) { Text("Verificar teléfono") }
      }
    }
    Spacer(Modifier.height(10.dp))
    OutlinedButton(onClick = {
      loading = true; onError("")
      scope.launch {
        try {
          val clientIdResource = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
          if (clientIdResource == 0) throw IllegalStateException("Falta configurar el cliente OAuth web de Google en Firebase.")
          val serverClientId = context.getString(clientIdResource)
          // Keep the same Credential Manager flow used by the working OMNI app.
          // Do not clear provider state or switch to a second Google sign-in option here.
          val googleOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()
          val credentialManager = CredentialManager.create(context)
          val credential = try {
            val request = GetCredentialRequest.Builder()
              .addCredentialOption(googleOption)
              .build()
            credentialManager.getCredential(context, request).credential
          } catch (noCredential: NoCredentialException) {
            // No saved/eligible Google credential: launch the explicit Google sign-in flow.
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
            val signInRequest = GetCredentialRequest.Builder()
              .addCredentialOption(signInOption)
              .build()
            credentialManager.getCredential(context, signInRequest).credential
          }
          if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
            complete(auth.signInWithCredential(com.google.firebase.auth.GoogleAuthProvider.getCredential(token, null)))
          } else throw IllegalStateException("Google no devolvió una credencial compatible.")
        } catch (ex: Exception) { loading = false; onError(ex.localizedMessage ?: "No se pudo iniciar sesión con Google.") }
      }
    }, Modifier.fillMaxWidth(), enabled = !loading) { Text("Continuar con Google") }
    if (loading) { Spacer(Modifier.height(10.dp)); CircularProgressIndicator(color = Mint, modifier = Modifier.align(Alignment.CenterHorizontally)) }
    if (error.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(error, color = Color(0xFFFF8B8B), fontSize = 13.sp) }
    Spacer(Modifier.height(16.dp))
    Text("El acceso administrativo se valida por UID. Tener una cuenta OMNI no concede permisos de administrador automáticamente.", color = Muted, fontSize = 12.sp)
  }
}

@Composable
private fun PasswordLinkScreen(
  user: com.google.firebase.auth.FirebaseUser,
  onLinked: (com.google.firebase.auth.FirebaseUser) -> Unit,
  onError: (String) -> Unit
) {
  var newPassword by remember { mutableStateOf("") }
  var confirmation by remember { mutableStateOf("") }
  var loading by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf("") }
  val email = user.email.orEmpty()

  Column(Modifier.fillMaxSize().padding(26.dp), verticalArrangement = Arrangement.Center) {
    Text("OMNI", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Text("VINCULAR CONTRASEÑA", color = Mint, fontSize = 25.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(12.dp))
    Text("Google ya verificó tu identidad. Para habilitar el acceso por correo y contraseña en OMNI Admin, creá una contraseña para esta misma cuenta.", color = Muted)
    Spacer(Modifier.height(8.dp))
    Text(email, color = White, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(18.dp))
    OutlinedTextField(newPassword, { newPassword = it }, Modifier.fillMaxWidth(),
      label = { Text("Nueva contraseña de OMNI Admin") }, singleLine = true,
      visualTransformation = PasswordVisualTransformation())
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(confirmation, { confirmation = it }, Modifier.fillMaxWidth(),
      label = { Text("Repetir contraseña") }, singleLine = true,
      visualTransformation = PasswordVisualTransformation())
    Spacer(Modifier.height(14.dp))
    Button(onClick = {
      when {
        newPassword.length < 6 -> message = "La contraseña debe tener al menos 6 caracteres."
        newPassword != confirmation -> message = "Las contraseñas no coinciden."
        email.isBlank() -> message = "La cuenta de Google no tiene un correo disponible."
        else -> {
          loading = true
          message = ""
          val credential = EmailAuthProvider.getCredential(email, newPassword)
          user.linkWithCredential(credential)
            .addOnSuccessListener { result ->
              loading = false
              val linked = result.user ?: FirebaseAuth.getInstance().currentUser
              if (linked != null) onLinked(linked)
              else message = "La vinculación se completó, pero no se pudo recuperar la sesión. Volvé a ingresar."
            }
            .addOnFailureListener { ex ->
              loading = false
              message = ex.localizedMessage ?: "No se pudo vincular la contraseña."
            }
        }
      }
    }, Modifier.fillMaxWidth().height(50.dp), enabled = !loading) {
      Text(if (loading) "Vinculando…" else "Vincular a mi cuenta OMNI", fontWeight = FontWeight.Bold)
    }
    if (loading) {
      Spacer(Modifier.height(10.dp))
      CircularProgressIndicator(color = Mint, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
    if (message.isNotBlank()) {
      Spacer(Modifier.height(10.dp))
      Text(message, color = Color(0xFFFF8B8B), fontSize = 13.sp)
    }
    Spacer(Modifier.height(14.dp))
    Text("Esto agrega el proveedor correo/contraseña al mismo usuario de Firebase; no crea otra cuenta ni cambia su UID. No uses la contraseña de Google: Google no la comparte con OMNI.", color = Muted, fontSize = 12.sp)
  }
}

@Composable
private fun LoadingScreen(message: String) {
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
    CircularProgressIndicator(color = Mint); Spacer(Modifier.height(16.dp)); Text(message, color = Muted)
  }
}

@Composable
private fun AccessDenied(message: String, onExit: () -> Unit) {
  Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
    Icon(Icons.Default.Lock, null, tint = Color(0xFFFFB86B), modifier = Modifier.size(48.dp))
    Spacer(Modifier.height(16.dp)); Text("Acceso restringido", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp)); Text(message, color = Muted)
    Spacer(Modifier.height(22.dp)); OutlinedButton(onClick = onExit) { Text("Cerrar sesión") }
  }
}

private enum class AdminTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
  OVERVIEW("Resumen", Icons.Default.Dashboard), USERS("Usuarios", Icons.Default.People),
  PAY("OMNI Pay", Icons.Default.AccountBalanceWallet), MODULES("Módulos", Icons.Default.Widgets),
  SETTINGS("Sistema", Icons.Default.Tune)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminHome(email: String, onRefresh: () -> Unit, onLogout: () -> Unit) {
  var tab by remember { mutableStateOf(AdminTab.OVERVIEW) }
  var data by remember { mutableStateOf(DashboardData()) }
  var busy by remember { mutableStateOf(false) }
  var status by remember { mutableStateOf("") }
  val db = remember { FirebaseFirestore.getInstance() }

  fun load() {
    busy = true
    val collections = listOf("users", "walletAccounts", "walletOperations", "merchantProfiles", "storeOrders", "usageEvents")
    val results = mutableMapOf<String, List<com.google.firebase.firestore.DocumentSnapshot>>()
    var finished = 0
    var failed = false
    collections.forEach { name ->
      db.collection(name).limit(500).get()
        .addOnSuccessListener { snap ->
          results[name] = snap.documents
          finished++
          if (finished == collections.size) {
            val wallets = results["walletAccounts"].orEmpty()
            val total = wallets.sumOf { it.getDouble("balance") ?: (it.getLong("balance")?.toDouble() ?: 0.0) }
            data = DashboardData(
              users = results["users"].orEmpty().size,
              wallets = wallets.size,
              balance = total,
              operations = results["walletOperations"].orEmpty().size,
              stores = results["merchantProfiles"].orEmpty().size,
              orders = results["storeOrders"].orEmpty().size,
              events = results["usageEvents"].orEmpty().size,
              recentUsers = results["users"].orEmpty().takeLast(12).reversed().map {
                AdminRecord(it.id, it.getString("displayName") ?: it.getString("name") ?: "Usuario OMNI",
                  it.getString("email") ?: it.id)
              },
              recentOperations = results["walletOperations"].orEmpty().takeLast(12).reversed().map {
                AdminRecord(it.id, it.getString("kind") ?: "Operación DEMO",
                  "${it.getString("senderUid") ?: "—"} → ${it.getString("recipientUid") ?: "—"} · OM$ ${it.get("amount") ?: "0"}")
              },
              loading = false,
              note = "Lectura limitada a los últimos 500 documentos por colección. Los totales son una primera vista operativa."
            )
            busy = false
          }
        }
        .addOnFailureListener { ex ->
          failed = true; finished++
          if (finished == collections.size) {
            data = data.copy(loading = false, note = "Algunas colecciones no pudieron leerse: ${ex.localizedMessage ?: "revisar permisos"}")
            busy = false
          }
        }
    }
  }

  LaunchedEffect(Unit) { load() }
  LaunchedEffect(tab) { if (tab == AdminTab.USERS || tab == AdminTab.PAY) load() }

  Scaffold(
    containerColor = Navy,
    topBar = {
      TopAppBar(
        title = { Column { Text("OMNI ADMIN", color = White, fontWeight = FontWeight.Black, fontSize = 17.sp); Text("Panel de control", color = Muted, fontSize = 11.sp) } },
        actions = {
          IconButton(onClick = { load(); onRefresh() }) { Icon(Icons.Default.Refresh, "Actualizar", tint = Mint) }
          IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Cerrar sesión", tint = Muted) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy)
      )
    },
    bottomBar = {
      NavigationBar(containerColor = Panel) {
        AdminTab.values().forEach { item ->
          NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(item.icon, item.label) }, label = { Text(item.label, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Mint, selectedTextColor = Mint, indicatorColor = Color(0xFF263A4B), unselectedIconColor = Muted, unselectedTextColor = Muted))
        }
      }
    }
  ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
      Text(email, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
      if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Mint)
      when (tab) {
        AdminTab.OVERVIEW -> OverviewScreen(data, busy)
        AdminTab.USERS -> UsersScreen(data)
        AdminTab.PAY -> PayScreen(data)
        AdminTab.MODULES -> ModulesScreen(data)
        AdminTab.SETTINGS -> SettingsScreen(status) { message -> status = message }
      }
    }
  }
}

@Composable
private fun OverviewScreen(data: DashboardData, busy: Boolean) {
  LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
    item {
      Text("Vista general", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
      Text("Actividad real registrada en Firebase", color = Muted, fontSize = 13.sp)
    }
    item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      MetricCard("Usuarios", data.users.toString(), Icons.Default.People, Modifier.weight(1f))
      MetricCard("Billeteras", data.wallets.toString(), Icons.Default.AccountBalanceWallet, Modifier.weight(1f))
    } }
    item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      MetricCard("Saldo circulante", "OM$ ${Money.format(data.balance)}", Icons.Default.Savings, Modifier.weight(1f))
      MetricCard("Operaciones", data.operations.toString(), Icons.Default.SwapHoriz, Modifier.weight(1f))
    } }
    item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      MetricCard("Comercios", data.stores.toString(), Icons.Default.Storefront, Modifier.weight(1f))
      MetricCard("Pedidos", data.orders.toString(), Icons.Default.ReceiptLong, Modifier.weight(1f))
    } }
    item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      MetricCard("Eventos de uso", data.events.toString(), Icons.Default.Insights, Modifier.weight(1f))
      MetricCard("Estado", if (busy) "Cargando" else "Conectado", Icons.Default.CloudDone, Modifier.weight(1f))
    } }
    item { Text("Últimas cuentas", color = White, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
    items(data.recentUsers) { RecordRow(it) }
    item { if (data.note.isNotBlank()) Text(data.note, color = Muted, fontSize = 11.sp) }
  }
}

@Composable
private fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
  Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
    Column(Modifier.padding(14.dp)) {
      Icon(icon, null, tint = Mint, modifier = Modifier.size(20.dp))
      Spacer(Modifier.height(10.dp))
      Text(value, color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
      Text(label, color = Muted, fontSize = 11.sp)
    }
  }
}

@Composable
private fun UsersScreen(data: DashboardData) {
  Column(Modifier.fillMaxSize()) {
    Text("Usuarios y cuentas", color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
    Text("${data.users} perfiles consultados · ${data.wallets} billeteras", color = Muted, fontSize = 12.sp)
    Spacer(Modifier.height(12.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(data.recentUsers) { RecordRow(it) }
      if (data.recentUsers.isEmpty()) item { EmptyState("Todavía no hay perfiles para mostrar.") }
    }
  }
}

@Composable
private fun PayScreen(data: DashboardData) {
  LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
    item { Text("OMNI Pay · DEMO", color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Vista de control. No mueve dinero ni modifica saldos.", color = Muted, fontSize = 12.sp) }
    item { MetricCard("Saldo total consultado", "OM$ ${Money.format(data.balance)}", Icons.Default.Savings, Modifier.fillMaxWidth()) }
    item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      MetricCard("Billeteras", data.wallets.toString(), Icons.Default.Wallet, Modifier.weight(1f))
      MetricCard("Operaciones", data.operations.toString(), Icons.Default.SwapHoriz, Modifier.weight(1f))
    } }
    item { Text("Operaciones recientes", color = White, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
    items(data.recentOperations) { RecordRow(it) }
    item { EmptyState("La conciliación avanzada y los ajustes auditados se incorporarán en la siguiente iteración.") }
  }
}

@Composable
private fun ModulesScreen(data: DashboardData) {
  val modules = listOf(
    "OMNI Pay" to "Billeteras, transferencias, depósitos y pagos DEMO",
    "OMNI Store" to "Comercios, pedidos y códigos QR",
    "OMNI Eventos" to "Agenda y actividad comunitaria",
    "OMNI Oportunidades" to "Empleos y postulaciones",
    "OMNI Insights" to "Eventos de uso y métricas de adopción"
  )
  LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
    item { Text("Módulos del ecosistema", color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Catálogo inicial. Los módulos nuevos se incorporarán al registro de eventos.", color = Muted, fontSize = 12.sp) }
    items(modules) { (name, description) ->
      Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Extension, null, tint = Mint, modifier = Modifier.size(25.dp))
          Spacer(Modifier.width(12.dp))
          Column(Modifier.weight(1f)) { Text(name, color = White, fontWeight = FontWeight.Bold); Text(description, color = Muted, fontSize = 11.sp) }
          Text("Base", color = Mint, fontSize = 11.sp)
        }
      }
    }
    item { Text("Eventos registrados: ${data.events}", color = Muted, fontSize = 12.sp) }
  }
}

@Composable
private fun SettingsScreen(status: String, onStatus: (String) -> Unit) {
  val db = remember { FirebaseFirestore.getInstance() }
  var initial by remember { mutableStateOf("50000") }
  var transfer by remember { mutableStateOf("50000") }
  var topup by remember { mutableStateOf("50000") }
  var lifetime by remember { mutableStateOf("150000") }
  var betaMode by remember { mutableStateOf("cerrada") }
  var maintenance by remember { mutableStateOf(false) }
  var loading by remember { mutableStateOf(true) }
  LaunchedEffect(Unit) {
    db.collection("adminConfig").document("global").get().addOnSuccessListener { doc ->
      initial = (doc.getLong("demoInitialBalance") ?: 50000L).toString()
      transfer = (doc.getLong("maxTransferPerOperation") ?: 50000L).toString()
      topup = (doc.getLong("maxTopUpPerOperation") ?: 50000L).toString()
      lifetime = (doc.getLong("maxTopUpLifetime") ?: 150000L).toString()
      betaMode = doc.getString("betaMode") ?: "cerrada"
      maintenance = doc.getBoolean("maintenanceMode") ?: false
      loading = false
    }.addOnFailureListener { loading = false; onStatus("No se pudo leer configuración: ${it.localizedMessage}") }
  }
  fun save() {
    val values = listOf(initial, transfer, topup, lifetime).mapNotNull { it.toLongOrNull() }
    if (values.size != 4 || values.any { it < 0 }) { onStatus("Revisá los importes: deben ser números enteros no negativos."); return }
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val config = mapOf("demoInitialBalance" to values[0], "maxTransferPerOperation" to values[1],
      "maxTopUpPerOperation" to values[2], "maxTopUpLifetime" to values[3],
      "betaMode" to betaMode, "maintenanceMode" to maintenance,
      "updatedAt" to Timestamp.now(), "updatedBy" to uid)
    db.collection("adminConfig").document("global").set(config)
      .addOnSuccessListener {
        db.collection("adminAuditLogs").add(mapOf("actorUid" to uid, "action" to "CONFIG_UPDATED", "target" to "adminConfig/global", "createdAt" to Timestamp.now()))
        onStatus("Configuración guardada en Firebase. Los parámetros quedarán almacenados; todavía deben conectarse al comportamiento de OMNI Android para aplicarse allí.")
      }.addOnFailureListener { onStatus("No se pudo guardar: ${it.localizedMessage}") }
  }
  LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
    item { Text("Configuración del sistema", color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Parámetros DEMO y de pruebas", color = Muted, fontSize = 12.sp) }
    item { NumberField("Saldo inicial DEMO (OM$)", initial) { initial = it } }
    item { NumberField("Máximo por transferencia (OM$)", transfer) { transfer = it } }
    item { NumberField("Máximo por recarga (OM$)", topup) { topup = it } }
    item { NumberField("Máximo acumulado de recargas (OM$)", lifetime) { lifetime = it } }
    item {
      Text("Modalidad de beta", color = Muted, fontSize = 12.sp)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = betaMode == "cerrada", onClick = { betaMode = "cerrada" }, label = { Text("Cerrada") })
        FilterChip(selected = betaMode == "ampliable", onClick = { betaMode = "ampliable" }, label = { Text("Ampliable") })
      }
    }
    item { Row(verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) { Text("Modo mantenimiento", color = White, fontWeight = FontWeight.Bold); Text("Indicador administrativo; requiere integración en OMNI para bloquear accesos.", color = Muted, fontSize = 11.sp) }
      Switch(maintenance, { maintenance = it })
    } }
    item { Button(onClick = ::save, Modifier.fillMaxWidth(), enabled = !loading) { Text("Guardar parámetros") } }
    item { if (status.isNotBlank()) Text(status, color = Mint, fontSize = 12.sp) }
    item { Text("Seguridad: estos valores no cambian automáticamente las reglas de Firestore ni los límites que hoy valida OMNI Android. La integración de consumo es un paso obligatorio antes de usarlos en producción.", color = Muted, fontSize = 11.sp) }
  }
}

@Composable
private fun NumberField(label: String, value: String, onValue: (String) -> Unit) {
  OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
}

@Composable
private fun RecordRow(record: AdminRecord) {
  Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.AccountCircle, null, tint = Mint)
      Spacer(Modifier.width(10.dp))
      Column(Modifier.weight(1f)) {
        Text(record.title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(record.detail, color = Muted, fontSize = 10.sp, maxLines = 2)
      }
    }
  }
}

@Composable
private fun EmptyState(message: String) {
  Text(message, color = Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
}
