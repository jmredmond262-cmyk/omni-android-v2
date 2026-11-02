package com.omni.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

private val Navy = Color(0xFF0B1220)
private val Panel = Color(0xFF151F31)
private val PanelSoft = Color(0xFF1D2A40)
private val Mint = Color(0xFF55E0B8)
private val Muted = Color(0xFF9EADC3)
private val White = Color(0xFFF4F7FC)
private val Danger = Color(0xFFFF8585)
private const val ADMIN_API_URL = "https://us-central1-omni-57f69.cloudfunctions.net/omniAdminApi"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.statusBarColor = android.graphics.Color.rgb(11, 18, 32)
    window.navigationBarColor = android.graphics.Color.rgb(11, 18, 32)
    setContent {
      MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, onPrimary = Navy, background = Navy, surface = Panel,
        onBackground = White, onSurface = White, secondary = Muted, error = Danger
      )) { OmniAdminRoot() }
    }
  }
}

private data class AdminTab(val id: String, val label: String, val collection: String? = null)
private val tabs = listOf(
  AdminTab("dashboard", "Resumen"), AdminTab("users", "Usuarios", "users"),
  AdminTab("wallets", "Billeteras", "walletAccounts"), AdminTab("operations", "Operaciones", "walletOperations"),
  AdminTab("stores", "Comercios", "merchantProfiles"), AdminTab("orders", "Pedidos", "storeOrders"),
  AdminTab("feedback", "Comentarios", "appFeedback"), AdminTab("modules", "Módulos", "moduleRegistry"),
  AdminTab("audit", "Auditoría", "adminAuditLogs"), AdminTab("settings", "Configuración")
)

@Composable
private fun OmniAdminRoot() {
  val auth = remember { FirebaseAuth.getInstance() }
  var user by remember { mutableStateOf(auth.currentUser) }
  var selected by remember { mutableStateOf("dashboard") }
  var payload by remember { mutableStateOf(JSONObject()) }
  var loading by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf("") }
  val scope = rememberCoroutineScope()

  fun refresh() {
    val current = auth.currentUser ?: return
    scope.launch {
      loading = true
      error = ""
      try {
        val token = current.getIdToken(true).await().token ?: throw IllegalStateException("No se pudo renovar la sesión.")
        val tab = tabs.first { it.id == selected }
        payload = when (selected) {
          "dashboard" -> AdminApi.call(token, "dashboard")
          "settings" -> AdminApi.call(token, "getConfig")
          else -> AdminApi.call(token, "list", JSONObject().put("collection", tab.collection).put("limit", 100))
        }
      } catch (ex: Exception) { error = ex.message ?: "No se pudo conectar con OMNI Admin." }
      finally { loading = false }
    }
  }

  LaunchedEffect(user?.uid, selected) { if (user != null) refresh() }

  if (user == null) {
    AdminLogin(
      onLogin = { email, password ->
        scope.launch {
          loading = true; error = ""
          try { user = auth.signInWithEmailAndPassword(email.trim(), password).await().user }
          catch (ex: Exception) { error = authMessage(ex) }
          finally { loading = false }
        }
      },
      onReset = { email ->
        scope.launch {
          try {
            auth.sendPasswordResetEmail(email.trim()).await()
            error = "Si el correo está registrado, recibirás un enlace para restablecer la contraseña."
          } catch (ex: Exception) { error = authMessage(ex) }
        }
      }, loading = loading, error = error
    )
  } else {
    AdminHome(
      email = user?.email ?: "Administrador", selected = selected, payload = payload,
      loading = loading, error = error, onSelect = { selected = it }, onRefresh = ::refresh,
      onSignOut = {
        auth.signOut(); user = null; payload = JSONObject(); error = ""; selected = "dashboard"
      },
      onSaveConfig = { config ->
        scope.launch {
          loading = true; error = ""
          try {
            val token = auth.currentUser?.getIdToken(true)?.await()?.token ?: throw IllegalStateException("Sesión vencida.")
            AdminApi.call(token, "saveConfig", JSONObject().put("config", config))
            error = "Configuración guardada correctamente."; refresh()
          } catch (ex: Exception) { error = ex.message ?: "No se pudo guardar." }
          finally { loading = false }
        }
      },
      onSaveModule = { id, title, description, enabled ->
        scope.launch {
          loading = true; error = ""
          try {
            val token = auth.currentUser?.getIdToken(true)?.await()?.token ?: throw IllegalStateException("Sesión vencida.")
            val module = JSONObject().put("title", title).put("description", description).put("enabled", enabled)
            AdminApi.call(token, "saveModule", JSONObject().put("id", id).put("module", module))
            error = "Módulo actualizado."; refresh()
          } catch (ex: Exception) { error = ex.message ?: "No se pudo actualizar el módulo." }
          finally { loading = false }
        }
      },
      onInvite = { email ->
        scope.launch {
          loading = true; error = ""
          try {
            val token = auth.currentUser?.getIdToken(true)?.await()?.token ?: throw IllegalStateException("Sesión vencida.")
            AdminApi.call(token, "createInvitation", JSONObject().put("email", email))
            error = "Invitación registrada."; selected = "audit"
          } catch (ex: Exception) { error = ex.message ?: "No se pudo crear la invitación." }
          finally { loading = false }
        }
      }
    )
  }
}

@Composable
private fun AdminLogin(onLogin: (String, String) -> Unit, onReset: (String) -> Unit, loading: Boolean, error: String) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var resetMode by remember { mutableStateOf(false) }
  Box(Modifier.fillMaxSize().background(Navy).padding(24.dp), contentAlignment = Alignment.Center) {
    Column(Modifier.fillMaxWidth().widthIn(max = 440.dp)) {
      Text("OMNI", color = White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
      Text("ADMIN", color = Mint, fontSize = 42.sp, fontWeight = FontWeight.Black)
      Text("Centro de control y administración", color = Muted, fontSize = 15.sp)
      Spacer(Modifier.height(30.dp))
      Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
          Text(if (resetMode) "Recuperar acceso" else "Acceso administrativo", color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
          Spacer(Modifier.height(18.dp))
          OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Correo electrónico") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
          if (!resetMode) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Contraseña") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
          }
          Spacer(Modifier.height(18.dp))
          Button(onClick = { if (resetMode) onReset(email) else onLogin(email, password) }, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !loading && email.contains("@") && (resetMode || password.isNotBlank())) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(if (resetMode) "Enviar enlace" else "Ingresar", fontWeight = FontWeight.Bold)
          }
          TextButton(onClick = { resetMode = !resetMode }) { Text(if (resetMode) "Volver al inicio de sesión" else "Olvidé mi contraseña", color = Mint) }
          if (error.isNotBlank()) Text(error, color = Danger, fontSize = 13.sp)
        }
      }
      Spacer(Modifier.height(18.dp))
      Text("Acceso privado. Solo las cuentas habilitadas por el propietario pueden ingresar.", color = Muted, fontSize = 12.sp)
    }
  }
}

@Composable
private fun AdminHome(
  email: String, selected: String, payload: JSONObject, loading: Boolean, error: String,
  onSelect: (String) -> Unit, onRefresh: () -> Unit, onSignOut: () -> Unit,
  onSaveConfig: (JSONObject) -> Unit, onSaveModule: (String, String, String, Boolean) -> Unit,
  onInvite: (String) -> Unit
) {
  Scaffold(containerColor = Navy, topBar = {
    Column(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 16.dp, vertical = 12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text("OMNI ADMIN", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
          Text(email, color = White, fontSize = 13.sp, maxLines = 1)
        }
        IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar", tint = White) }
        IconButton(onClick = onSignOut) { Icon(Icons.Default.Logout, "Cerrar sesión", tint = White) }
      }
      Spacer(Modifier.height(10.dp))
      Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEach { tab ->
          FilterChip(selected == tab.id, { onSelect(tab.id) }, label = { Text(tab.label, maxLines = 1) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Mint, selectedLabelColor = Navy, labelColor = Muted))
        }
      }
    }
  }) { padding ->
    Column(Modifier.fillMaxSize().padding(padding)) {
      if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Mint, trackColor = Panel)
      if (error.isNotBlank()) Text(error, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), color = if (error.contains("correctamente") || error.contains("registrada") || error.contains("actualizado")) Mint else Danger, fontSize = 13.sp)
      when (selected) {
        "dashboard" -> DashboardContent(payload)
        "settings" -> ConfigContent(payload.optJSONObject("config") ?: JSONObject(), onSaveConfig)
        "modules" -> ModuleContent(payload.optJSONArray("items") ?: JSONArray(), onSaveModule)
        "audit" -> InvitationContent(payload.optJSONArray("items") ?: JSONArray(), onInvite)
        else -> RecordsContent(selected, payload.optJSONArray("items") ?: JSONArray())
      }
    }
  }
}

@Composable
private fun DashboardContent(payload: JSONObject) {
  val counts = payload.optJSONObject("counts") ?: JSONObject()
  val cards = listOf(
    "Usuarios" to counts.optInt("users"), "Billeteras" to counts.optInt("walletAccounts"),
    "Operaciones" to counts.optInt("walletOperations"), "Comercios" to counts.optInt("merchantProfiles"),
    "Pedidos" to counts.optInt("storeOrders"), "Eventos" to counts.optInt("omniEvents"),
    "Comentarios" to counts.optInt("appFeedback"), "Eventos de uso" to counts.optInt("usageEvents")
  )
  LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item { Text("Vista general", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold); Text("Estado de la plataforma OMNI", color = Muted, fontSize = 13.sp) }
    items(cards.chunked(2)) { pair ->
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        pair.forEach { entry ->
          Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
              Text(entry.first, color = Muted, fontSize = 12.sp)
              Text(entry.second.toString(), color = Mint, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
        if (pair.size == 1) Spacer(Modifier.weight(1f))
      }
    }
    item {
      val balance = payload.optDouble("walletBalanceSample", 0.0)
      val sample = payload.optInt("walletSampleSize")
      Card(colors = CardDefaults.cardColors(containerColor = PanelSoft), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp)) {
          Text("Saldo DEMO observado", color = Muted, fontSize = 12.sp)
          Text("OM$ " + "%,.2f".format(java.util.Locale("es", "AR"), balance), color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
          Text("Muestra de hasta 1.000 billeteras (" + sample + "). No representa fondos reales.", color = Muted, fontSize = 11.sp)
        }
      }
    }
    item { Text("Últimos usuarios", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    items(jsonArrayToList(payload.optJSONArray("recentUsers"))) { RecordCard(it, "users") }
    item { Text("Últimas operaciones", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    items(jsonArrayToList(payload.optJSONArray("recentOperations"))) { RecordCard(it, "walletOperations") }
  }
}

@Composable
private fun RecordsContent(selected: String, array: JSONArray) {
  val records = jsonArrayToList(array)
  LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item {
      Text(tabs.first { it.id == selected }.label, color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
      Text(records.size.toString() + " registros cargados · máximo 100", color = Muted, fontSize = 12.sp)
    }
    if (records.isEmpty()) item { EmptyState("Todavía no hay registros para mostrar.") }
    items(records) { RecordCard(it, selected) }
  }
}

@Composable
private fun RecordCard(item: Map<String, Any?>, type: String) {
  val title = when (type) {
    "users" -> firstValue(item, "displayName", "name", "email", "username", "id")
    "walletAccounts" -> firstValue(item, "displayName", "alias", "uid", "id")
    "walletOperations" -> firstValue(item, "title", "kind", "type", "status", "id")
    "merchantProfiles" -> firstValue(item, "storeName", "name", "id")
    "storeOrders" -> firstValue(item, "orderNumber", "status", "storeId", "id")
    "appFeedback" -> firstValue(item, "message", "title", "id")
    "adminAuditLogs" -> firstValue(item, "action", "target", "id")
    else -> firstValue(item, "title", "name", "action", "id")
  }
  val details = item.entries.filter { it.key !in listOf("id", "displayName", "name", "email", "message", "title", "createdAt", "updatedAt") }
    .take(4).joinToString("  ·  ") { it.key + ": " + displayValue(it.value).take(80) }
  Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
    Column(Modifier.fillMaxWidth().padding(15.dp)) {
      Text(title.take(110), color = White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      if (details.isNotBlank()) { Spacer(Modifier.height(5.dp)); Text(details, color = Muted, fontSize = 11.sp, maxLines = 3) }
      val date = displayValue(item["createdAt"] ?: item["updatedAt"])
      if (date.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text(date, color = Muted, fontSize = 10.sp) }
      val message = item["message"]?.toString()
      if (!message.isNullOrBlank() && title != message) { Spacer(Modifier.height(4.dp)); Text(message.take(300), color = Muted, fontSize = 12.sp) }
    }
  }
}

@Composable
private fun ConfigContent(config: JSONObject, onSave: (JSONObject) -> Unit) {
  var maintenance by remember(config.toString()) { mutableStateOf(config.optBoolean("maintenanceMode", false)) }
  var message by remember(config.toString()) { mutableStateOf(config.optString("maintenanceMessage", "")) }
  var notice by remember(config.toString()) { mutableStateOf(config.optString("globalNotice", "")) }
  var minVersion by remember(config.toString()) { mutableStateOf(config.optString("minimumVersion", "")) }
  var recommended by remember(config.toString()) { mutableStateOf(config.optString("recommendedVersion", "")) }
  LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item { Text("Configuración global", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold); Text("Los cambios se guardan en adminConfig/global y quedan auditados.", color = Muted, fontSize = 12.sp) }
    item {
      Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Modo mantenimiento", color = White, fontWeight = FontWeight.SemiBold); Text("Indicador global para OMNI.", color = Muted, fontSize = 11.sp) }
            Switch(maintenance, { maintenance = it })
          }
          OutlinedTextField(message, { message = it }, Modifier.fillMaxWidth(), label = { Text("Mensaje de mantenimiento") })
          Spacer(Modifier.height(10.dp))
          OutlinedTextField(notice, { notice = it }, Modifier.fillMaxWidth(), label = { Text("Aviso global") })
          Spacer(Modifier.height(10.dp))
          OutlinedTextField(minVersion, { minVersion = it }, Modifier.fillMaxWidth(), label = { Text("Versión mínima") }, singleLine = true)
          Spacer(Modifier.height(10.dp))
          OutlinedTextField(recommended, { recommended = it }, Modifier.fillMaxWidth(), label = { Text("Versión recomendada") }, singleLine = true)
          Spacer(Modifier.height(16.dp))
          Button(onClick = { onSave(JSONObject().put("maintenanceMode", maintenance).put("maintenanceMessage", message).put("globalNotice", notice).put("minimumVersion", minVersion).put("recommendedVersion", recommended)) }, modifier = Modifier.fillMaxWidth()) { Text("Guardar configuración") }
        }
      }
    }
  }
}

@Composable
private fun ModuleContent(array: JSONArray, onSave: (String, String, String, Boolean) -> Unit) {
  val records = jsonArrayToList(array)
  LazyColumn(Modifier.fillMaxSize().padding(16.dp), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item { Text("Módulos de OMNI", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold); Text("Activá o desactivá módulos registrados.", color = Muted, fontSize = 12.sp) }
    if (records.isEmpty()) item { EmptyState("No hay módulos registrados todavía.") }
    items(records) { item ->
      val id = item["id"]?.toString() ?: return@items
      var enabled by remember(id, item["enabled"]) { mutableStateOf(item["enabled"] == true) }
      val title = item["title"]?.toString() ?: id
      val description = item["description"]?.toString() ?: ""
      Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(15.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, color = White, fontWeight = FontWeight.SemiBold); Text(id, color = Muted, fontSize = 10.sp) }
            Switch(enabled, { enabled = it })
          }
          if (description.isNotBlank()) Text(description, color = Muted, fontSize = 12.sp)
          TextButton(onClick = { onSave(id, title, description, enabled) }) { Text("Guardar estado", color = Mint) }
        }
      }
    }
  }
}

@Composable
private fun InvitationContent(array: JSONArray, onInvite: (String) -> Unit) {
  var email by remember { mutableStateOf("") }
  LazyColumn(Modifier.fillMaxSize().padding(16.dp), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item {
      Text("Auditoría y accesos", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
      Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(15.dp)) {
          Text("Invitar a la beta", color = White, fontWeight = FontWeight.SemiBold)
          Spacer(Modifier.height(8.dp))
          OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Correo electrónico") }, singleLine = true)
          Spacer(Modifier.height(8.dp))
          Button(onClick = { onInvite(email); email = "" }, enabled = email.contains("@"), modifier = Modifier.fillMaxWidth()) { Text("Registrar invitación") }
        }
      }
      Spacer(Modifier.height(8.dp)); Text("Registro de auditoría", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
    items(jsonArrayToList(array)) { RecordCard(it, "adminAuditLogs") }
  }
}

@Composable
private fun EmptyState(message: String) {
  Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
    Text(message, Modifier.fillMaxWidth().padding(20.dp), color = Muted, fontSize = 13.sp)
  }
}

private object AdminApi {
  suspend fun call(token: String, action: String, data: JSONObject = JSONObject()): JSONObject = withContext(Dispatchers.IO) {
    val connection = (URL(ADMIN_API_URL).openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"; connectTimeout = 20000; readTimeout = 30000; doOutput = true
      setRequestProperty("Authorization", "Bearer " + token)
      setRequestProperty("Content-Type", "application/json; charset=utf-8")
      setRequestProperty("Accept", "application/json")
    }
    try {
      val body = JSONObject().put("action", action).put("data", data).toString()
      connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
      val status = connection.responseCode
      val stream = if (status in 200..299) connection.inputStream else connection.errorStream
      val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
      val response = JSONObject(text.ifBlank { "{}" })
      if (status !in 200..299 || !response.optBoolean("ok", false)) throw IllegalStateException(response.optString("message", "Error HTTP " + status))
      response.optJSONObject("result") ?: JSONObject()
    } finally { connection.disconnect() }
  }
}

private fun jsonArrayToList(array: JSONArray?): List<Map<String, Any?>> {
  if (array == null) return emptyList()
  return (0 until array.length()).mapNotNull { index ->
    val obj = array.optJSONObject(index) ?: return@mapNotNull null
    jsonObjectToMap(obj)
  }
}
private fun jsonObjectToMap(obj: JSONObject): Map<String, Any?> =
  obj.keys().asSequence().associateWith { key -> jsonValue(obj.opt(key)) }
private fun jsonValue(value: Any?): Any? = when (value) {
  JSONObject.NULL -> null
  is JSONObject -> jsonObjectToMap(value)
  is JSONArray -> (0 until value.length()).map { jsonValue(value.opt(it)) }
  else -> value
}
private fun firstValue(item: Map<String, Any?>, vararg keys: String): String =
  keys.firstNotNullOfOrNull { key -> item[key]?.toString()?.takeIf { it.isNotBlank() && it != "null" } } ?: "Registro"
private fun displayValue(value: Any?): String = when (value) {
  null -> ""
  is Map<*, *> -> value.entries.joinToString(" ") { it.key.toString() + ": " + it.value }
  is List<*> -> value.joinToString(", ")
  else -> value.toString()
}
private fun authMessage(ex: Exception): String {
  val raw = ex.message ?: "No se pudo iniciar sesión."
  return when {
    raw.contains("INVALID_LOGIN_CREDENTIALS") || raw.contains("wrong-password") -> "Correo o contraseña incorrectos."
    raw.contains("too-many-requests") -> "Demasiados intentos. Esperá unos minutos y volvé a probar."
    raw.contains("network") -> "No hay conexión. Revisá internet e intentá nuevamente."
    raw.contains("user-not-found") -> "No existe una cuenta administrativa con ese correo."
    else -> raw
  }
}
