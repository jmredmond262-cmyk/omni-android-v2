package com.omni.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
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

private val Navy = Color(0xFF060B16)
private val Panel = Color(0xFF0D1729)
private val PanelSoft = Color(0xFF14243B)
private val Mint = Color(0xFF00E5FF)
private val Muted = Color(0xFF9EADC3)
private val White = Color(0xFFF4F7FC)
private val Danger = Color(0xFFFF8585)
private const val ADMIN_API_URL = "https://omni-admin-api.jmredmond262.workers.dev"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.statusBarColor = android.graphics.Color.rgb(6, 11, 22)
    window.navigationBarColor = android.graphics.Color.rgb(6, 11, 22)
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
  AdminTab("feedback", "Comentarios", "appFeedback"), AdminTab("activity", "Actividad", "usageEvents"), AdminTab("modules", "Módulos", "moduleRegistry"),
  AdminTab("audit", "Auditoría", "adminAuditLogs")
)

@Composable
private fun OmniAdminRoot() {
  val auth = remember { FirebaseAuth.getInstance() }
  var user by remember { mutableStateOf(auth.currentUser) }
  var selected by remember { mutableStateOf("dashboard") }
  var payload by remember { mutableStateOf(JSONObject()) }
  var loading by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf("") }
  var nextPageToken by remember { mutableStateOf("") }
  var nextOffset by remember { mutableStateOf(0) }
  var hasMore by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  fun refresh() {
    val current = auth.currentUser ?: return
    scope.launch {
      loading = true
      error = ""
      try {
        val token = current.getIdToken(true).await().token ?: throw IllegalStateException("No se pudo renovar la sesión.")
        val tab = tabs.first { it.id == selected }
        nextPageToken = ""; nextOffset = 0; hasMore = false
        payload = when (selected) {
          "dashboard" -> AdminApi.call(token, "dashboard")
          "settings" -> AdminApi.call(token, "getConfig")
          else -> AdminApi.call(token, "list", JSONObject().put("collection", tab.collection).put("limit", 100))
        }
        nextPageToken = payload.optString("nextPageToken", "")
        nextOffset = payload.optInt("nextOffset", 0)
        hasMore = payload.optBoolean("hasMore", false)
      } catch (ex: Exception) { error = ex.message ?: "No se pudo conectar con OMNI Admin." }
      finally { loading = false }
    }
  }

  fun loadMore() {
    val current = auth.currentUser ?: return
    if (!hasMore || loading) return
    scope.launch {
      loading = true; error = ""
      try {
        val token = current.getIdToken(true).await().token ?: throw IllegalStateException("No se pudo renovar la sesión.")
        val tab = tabs.first { it.id == selected }
        val data = JSONObject().put("collection", tab.collection).put("limit", 100)
        if (selected == "activity") data.put("offset", nextOffset) else if (nextPageToken.isNotBlank()) data.put("pageToken", nextPageToken)
        val page = AdminApi.call(token, "list", data)
        val merged = JSONArray()
        val previous = payload.optJSONArray("items") ?: JSONArray()
        val incoming = page.optJSONArray("items") ?: JSONArray()
        for (i in 0 until previous.length()) merged.put(previous.get(i))
        for (i in 0 until incoming.length()) merged.put(incoming.get(i))
        payload.put("items", merged)
        nextPageToken = page.optString("nextPageToken", "")
        nextOffset = page.optInt("nextOffset", nextOffset + incoming.length())
        hasMore = page.optBoolean("hasMore", false)
      } catch (ex: Exception) { error = ex.message ?: "No se pudieron cargar más registros." }
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
      loading = loading, error = error, onSelect = { selected = it }, onRefresh = ::refresh, onLoadMore = ::loadMore, hasMore = hasMore,
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
  Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF071426), Navy, Color(0xFF030711)))).padding(24.dp), contentAlignment = Alignment.Center) {
    Column(Modifier.fillMaxWidth().widthIn(max = 440.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF1268D8)))), contentAlignment = Alignment.Center) { Text("O", color = Navy, fontSize = 30.sp, fontWeight = FontWeight.Black) }; Column { Text("OMNI", color = White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp); Text("ADMIN CONTROL", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp) } }
      Text("Tu ecosistema, bajo control", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
      Text("Centro de control y administración", color = Muted, fontSize = 13.sp)
      Spacer(Modifier.height(30.dp))
      Card(modifier = Modifier.border(1.dp, Color(0xFF164B68), RoundedCornerShape(24.dp)), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)) {
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
  onSelect: (String) -> Unit, onRefresh: () -> Unit, onLoadMore: () -> Unit, hasMore: Boolean, onSignOut: () -> Unit,
  onSaveConfig: (JSONObject) -> Unit, onSaveModule: (String, String, String, Boolean) -> Unit,
  onInvite: (String) -> Unit
) {
  Scaffold(containerColor = Navy, topBar = {
    Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF101F35), Color(0xFF091321), Color(0xFF101F35)))).border(width = 1.dp, color = Color(0xFF12364D)).padding(horizontal = 16.dp, vertical = 13.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(8.dp).background(Mint, RoundedCornerShape(50))) ; Text("OMNI ADMIN", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp) }
          Text(email, color = White, fontSize = 12.sp, maxLines = 1)
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
        else -> RecordsContent(selected, payload.optJSONArray("items") ?: JSONArray(), hasMore, onLoadMore)
      }
    }
  }
}

@Composable
private fun DashboardContent(payload: JSONObject) {
  val counts = payload.optJSONObject("counts") ?: JSONObject()
  val analytics = payload.optJSONObject("analytics") ?: JSONObject()
  val cards = listOf(
    "Usuarios" to counts.optInt("users").toString(),
    "Usuarios activos · 7 días" to payload.optInt("activeUsers7d").toString(),
    "Sesiones · 7 días" to analytics.optInt("sessions7d").toString(),
    "Pantallas vistas · 7 días" to analytics.optInt("screenViews7d").toString(),
    "Acciones · 7 días" to analytics.optInt("actions7d").toString(),
    "Promedio por sesión" to analytics.optInt("averageSessionSeconds").let { if (it > 0) "${it}s" else "—" },
    "Cámara IA · análisis" to analytics.optJSONObject("featureMetrics")?.optInt("aiCameraScans", 0).toString(),
    "Interacciones en mapas" to analytics.optJSONObject("featureMetrics")?.optInt("mapsInteractions", 0).toString(),
    "Bolsa de trabajo" to analytics.optJSONObject("featureMetrics")?.optInt("jobInteractions", 0).toString(),
    "Eventos e interacciones" to analytics.optJSONObject("featureMetrics")?.optInt("eventInteractions", 0).toString(),
    "Rutinas y clima" to analytics.optJSONObject("featureMetrics")?.optInt("routineWeatherInteractions", 0).toString(),
    "Compartidos" to analytics.optJSONObject("featureMetrics")?.optInt("commerceShares", 0).toString(),
    "Billeteras" to counts.optInt("walletAccounts").toString(),
    "Operaciones" to counts.optInt("walletOperations").toString(),
    "Comercios" to counts.optInt("merchantProfiles").toString(),
    "Pedidos" to counts.optInt("storeOrders").toString(),
    "Eventos" to counts.optInt("omniEvents").toString(),
    "Comentarios" to counts.optInt("appFeedback").toString(),
    "Uso registrado" to counts.optInt("usageEvents").toString(),
    "Módulos" to counts.optInt("moduleRegistry").toString()
  )
  val activity = listOf(
    "Operaciones" to counts.optInt("walletOperations"),
    "Pedidos" to counts.optInt("storeOrders"),
    "Eventos comunitarios" to counts.optInt("omniEvents"),
    "Actividad de usuarios" to counts.optInt("usageEvents"),
    "Comentarios" to counts.optInt("appFeedback")
  )
  val activityMax = (activity.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
  BoxWithConstraints(Modifier.fillMaxSize()) {
    val columns = if (maxWidth >= 600.dp) 2 else 1
    LazyColumn(
      Modifier.fillMaxSize().padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
      item {
        Text("Centro de control", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Una visión general de lo que sucede en OMNI", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(8.dp).background(Mint, RoundedCornerShape(50)))
          Text("Datos consultados en tiempo real", color = Muted, fontSize = 11.sp)
        }
      }
      items(cards.chunked(columns)) { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          pair.forEach { entry ->
            Card(
              Modifier.weight(1f).border(1.dp, Color(0xFF17415A), RoundedCornerShape(18.dp)),
              colors = CardDefaults.cardColors(containerColor = Panel),
              shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
              Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(entry.first, color = Muted, fontSize = 12.sp)
                Text(entry.second.toString(), color = Mint, fontSize = 27.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
      item {
        Card(Modifier.border(1.dp, Color(0xFF17415A), RoundedCornerShape(20.dp)), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
          Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Actividad de la plataforma", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Volumen acumulado por tipo de registro", color = Muted, fontSize = 11.sp)
            activity.forEach { (label, value) ->
              Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                  Text(label, Modifier.weight(1f), color = White, fontSize = 12.sp)
                  Text(value.toString(), color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.fillMaxWidth().height(8.dp).background(PanelSoft, RoundedCornerShape(8.dp))) {
                  Box(
                    Modifier.fillMaxWidth((value.toFloat() / activityMax.toFloat()).coerceIn(0f, 1f))
                      .fillMaxHeight().background(Mint, RoundedCornerShape(8.dp))
                  )
                }
              }
            }
            Text("El gráfico refleja registros existentes; no inventa actividad histórica que OMNI todavía no haya guardado.", color = Muted, fontSize = 10.sp)
          }
        }
      }
      item {
        val trend = jsonArrayToList(payload.optJSONArray("usageTrend"))
        val trendMax = (trend.maxOfOrNull { (it["count"] as? Number)?.toInt() ?: 0 } ?: 0).coerceAtLeast(1)
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
          Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Uso de OMNI · últimos 7 días", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Sesiones y pantallas registradas", color = Muted, fontSize = 11.sp)
            if (trend.isEmpty()) {
              Text("Todavía no hay historial suficiente para mostrar la evolución.", color = Muted, fontSize = 12.sp)
            } else {
              trend.forEach { point ->
                val value = (point["count"] as? Number)?.toInt() ?: 0
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                  Text(point["date"]?.toString().orEmpty(), color = Muted, fontSize = 10.sp, modifier = Modifier.width(38.dp))
                  Box(Modifier.weight(1f).height(9.dp).background(PanelSoft, RoundedCornerShape(8.dp))) {
                    Box(Modifier.fillMaxWidth((value.toFloat() / trendMax.toFloat()).coerceIn(0f, 1f)).fillMaxHeight().background(Mint, RoundedCornerShape(8.dp)))
                  }
                  Text(value.toString(), color = White, fontSize = 11.sp, modifier = Modifier.width(24.dp))
                }
              }
            }
          }
        }
      }
      item {
        val topScreens = jsonArrayToList(analytics.optJSONArray("topScreens"))
        val moduleUsage = jsonArrayToList(analytics.optJSONArray("moduleUsage"))
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
          Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Comportamiento de usuarios", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Permanencia media por pantalla: " + analytics.optInt("averageScreenSeconds") + " s", color = Muted, fontSize = 11.sp)
            Text("Pantallas más visitadas", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (topScreens.isEmpty()) Text("Se mostrará cuando haya navegación registrada.", color = Muted, fontSize = 12.sp)
            topScreens.forEach { row ->
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row["name"]?.toString().orEmpty(), color = White, fontSize = 12.sp)
                Text(row["count"]?.toString().orEmpty(), color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
            Text("Módulos con más interacciones", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (moduleUsage.isEmpty()) Text("Las acciones aparecerán a medida que se utilicen las funciones.", color = Muted, fontSize = 12.sp)
            moduleUsage.forEach { row ->
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row["name"]?.toString().orEmpty(), color = White, fontSize = 12.sp)
                Text(row["count"]?.toString().orEmpty(), color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
            val eventBreakdown = jsonArrayToList(analytics.optJSONArray("eventBreakdown"))
            Text("Acciones más frecuentes", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (eventBreakdown.isEmpty()) Text("Las acciones aparecerán cuando se utilicen los módulos.", color = Muted, fontSize = 12.sp)
            eventBreakdown.take(8).forEach { row ->
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(friendlyEnum(row["name"]?.toString().orEmpty()), color = White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text(row["count"]?.toString().orEmpty(), color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
      item {
        val diagnostics = payload.optJSONObject("diagnostics") ?: JSONObject()
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
          Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Diagnóstico de conexión", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            val visible = diagnostics.optInt("usageEventsVisibleLast7Days")
            val testSeen = diagnostics.optBoolean("diagnosticEventSeen")
            Text(
              if (testSeen) "Escritura y lectura de analíticas: verificadas"
              else "Evento de prueba: todavía no visible en el servidor",
              color = if (testSeen) Mint else Danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
            )
            Text("Eventos visibles (7 días): $visible", color = Muted, fontSize = 12.sp)
            Text("Perfiles de usuario visibles: " + diagnostics.optInt("userProfilesVisible"), color = Muted, fontSize = 12.sp)
            Text(
              "Este control ayuda a distinguir un fallo de escritura desde OMNI de un problema de lectura del panel. Actualizá OMNI y luego este resumen.",
              color = Muted, fontSize = 10.sp
            )
          }
        }
      }
      item {
        val balance = payload.optDouble("walletBalanceSample", 0.0)
        val sample = payload.optInt("walletSampleSize")
        Card(colors = CardDefaults.cardColors(containerColor = PanelSoft), shape = RoundedCornerShape(18.dp)) {
          Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Saldo DEMO observado", color = Muted, fontSize = 12.sp)
            Text("OM$ " + "%,.2f".format(java.util.Locale("es", "AR"), balance), color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Muestra de hasta 1.000 billeteras ($sample). No representa fondos reales.", color = Muted, fontSize = 11.sp)
          }
        }
      }
      item { Text("Usuarios recientes", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
      if (jsonArrayToList(payload.optJSONArray("recentUsers")).isEmpty()) {
        item { EmptyState("Todavía no hay perfiles recientes disponibles en la colección de usuarios.") }
      } else {
        items(jsonArrayToList(payload.optJSONArray("recentUsers"))) { RecordCard(it, "users") }
      }
      item { Text("Actividad reciente en OMNI", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
      if (jsonArrayToList(payload.optJSONArray("recentUsageEvents")).isEmpty()) {
        item { EmptyState("La medición de actividad comienza con las próximas sesiones de la aplicación OMNI.") }
      } else {
        items(jsonArrayToList(payload.optJSONArray("recentUsageEvents"))) { RecordCard(it, "usageEvents") }
      }
      item { Text("Últimas operaciones", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
      if (jsonArrayToList(payload.optJSONArray("recentOperations")).isEmpty()) {
        item { EmptyState("Todavía no hay operaciones para mostrar.") }
      } else {
        items(jsonArrayToList(payload.optJSONArray("recentOperations"))) { RecordCard(it, "walletOperations") }
      }
    }
  }
}

@Composable
private fun RecordsContent(selected: String, array: JSONArray, hasMore: Boolean, onLoadMore: () -> Unit) {
  val records = jsonArrayToList(array)
  LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item {
      Text(tabs.first { it.id == selected }.label, color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
      Text(records.size.toString() + (if (records.size == 1) " registro cargado" else " registros cargados") + if (selected == "activity") " · orden cronológico" else " · historial paginado", color = Muted, fontSize = 12.sp)
    }
    if (records.isEmpty()) item { EmptyState("Todavía no hay registros para mostrar.") }
    items(records) { RecordCard(it, selected) }
    if (hasMore) item {
      Button(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) { Text("Cargar más registros") }
    }
  }
}

private val technicalFields = setOf(
  "id", "uid", "ownerUid", "senderUid", "recipientUid", "customerUid", "merchantUid",
  "operationId", "storeId", "targetUid", "createdBy", "updatedBy", "photoUrl", "providerIds",
  "qrNonce", "lastOperationId", "cvu", "reference", "rawData"
)
private val fieldLabels = mapOf(
  "displayName" to "Nombre", "fullName" to "Nombre", "name" to "Nombre",
  "email" to "Correo", "phoneNumber" to "Teléfono", "phone" to "Teléfono",
  "alias" to "Alias", "balance" to "Saldo", "cashbackTotal" to "Cashback acumulado",
  "amount" to "Importe", "kind" to "Tipo", "type" to "Tipo", "status" to "Estado",
  "storeName" to "Comercio", "orderNumber" to "Pedido", "total" to "Total",
  "paymentStatus" to "Estado del pago", "paymentMethod" to "Medio de pago",
  "fulfillmentType" to "Entrega", "action" to "Acción", "target" to "Destino",
  "message" to "Mensaje", "description" to "Descripción", "enabled" to "Habilitado",
  "event" to "Evento", "module" to "Módulo", "eventName" to "Evento", "screen" to "Pantalla", "appVersion" to "Versión",
  "createdAt" to "Fecha de registro", "updatedAt" to "Última actualización", "lastSeenAt" to "Última actividad", "userLabel" to "Usuario",
  "data" to "Detalles de acción", "screenName" to "Pantalla", "eventId" to "Identificador de evento",
  "cvu" to "CVU"
)
private fun friendlyEnum(value: String): String = when (value.uppercase()) {
  "SESSION_START" -> "Inicio de sesión"
  "SESSION_END" -> "Fin de sesión"
  "APP_FOREGROUND" -> "Aplicación en primer plano"
  "APP_BACKGROUND" -> "Aplicación en segundo plano"
  "SCREEN_VIEW" -> "Visita de pantalla"
  "SCREEN_EXIT" -> "Salida de pantalla"
  "DEPOSIT" -> "Carga de saldo DEMO"
  "TRANSFER" -> "Transferencia"
  "QR_PAYMENT" -> "Pago con QR"
  "COMPRA_LOCAL" -> "Compra en comercio"
  "REINTEGRO_CASHBACK" -> "Reintegro de cashback"
  "CARGA_SALDO" -> "Carga de saldo"
  "TRANSFERENCIA_ENVIADA" -> "Transferencia enviada"
  "TRANSFERENCIA_RECIBIDA" -> "Transferencia recibida"
  "QR_PAGO" -> "Pago con QR"
  "QR_COBRO" -> "Cobro con QR"
  "RECIBIDO" -> "Recibido"
  "ACEPTADO" -> "Aceptado"
  "EN_PREPARACION" -> "En preparación"
  "LISTO" -> "Listo"
  "EN_CAMINO" -> "En camino"
  "ENTREGADO" -> "Entregado"
  "PENDIENTE" -> "Pendiente"
  "APROBADA" -> "Aprobada"
  "RECHAZADO" -> "Rechazado"
  else -> value.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}
private fun looksLikeTechnicalId(value: String): Boolean =
  value.length >= 22 && value.all { it.isLetterOrDigit() || it == '-' || it == '_' }

@Composable
private fun RecordCard(item: Map<String, Any?>, type: String) {
  var expanded by remember(item["id"]) { mutableStateOf(false) }
  val title = when (type) {
    "users" -> firstPresentValue(item, "displayName", "fullName", "name", "email").ifBlank { "Usuario OMNI" }
    "walletAccounts" -> firstPresentValue(item, "alias").ifBlank { "Billetera OMNI Pay" }
    "walletOperations" -> friendlyEnum(firstPresentValue(item, "kind", "type", "title").ifBlank { "Operación" })
    "merchantProfiles" -> firstPresentValue(item, "storeName", "name").ifBlank { "Comercio OMNI" }
    "storeOrders" -> "Pedido " + firstPresentValue(item, "orderNumber", "status").ifBlank { "OMNI" }
    "appFeedback" -> "Comentario de usuario"
    "usageEvents" -> friendlyEnum(firstPresentValue(item, "event", "eventName").ifBlank { "Actividad OMNI" })
    "adminAuditLogs" -> friendlyEnum(firstPresentValue(item, "action").ifBlank { "Actividad administrativa" })
    else -> firstPresentValue(item, "title", "name", "action").ifBlank { "Registro OMNI" }
  }
  val preferred = when (type) {
    "users" -> listOf("email", "phoneNumber", "phone", "createdAt", "lastSeenAt")
    "walletAccounts" -> listOf("balance", "cashbackTotal", "createdAt")
    "walletOperations" -> listOf("amount", "status", "createdAt")
    "merchantProfiles" -> listOf("description", "category", "address", "status", "createdAt")
    "storeOrders" -> listOf("total", "paymentStatus", "fulfillmentType", "createdAt")
    "appFeedback" -> listOf("message", "createdAt")
    "usageEvents" -> listOf("userLabel", "module", "appVersion", "createdAt")
    else -> listOf("status", "createdAt", "updatedAt")
  }
  val visible = preferred.mapNotNull { key ->
    val value = item[key] ?: return@mapNotNull null
    val rendered = displayValue(value)
    if (rendered.isBlank() || looksLikeTechnicalId(rendered)) null
    else (fieldLabels[key] ?: key) + ": " + if (key == "kind" || key == "type" || key == "status" || key == "paymentStatus") friendlyEnum(rendered) else rendered
  }
  val protectedKeys = setOf("password", "token", "accessToken", "refreshToken", "secret", "apiKey", "qrNonce", "rawData", "authorization")
  val details = item.entries.filter { (key, value) -> key !in protectedKeys && value != null && displayValue(value).isNotBlank() }.sortedBy { it.key }
  Card(Modifier.fillMaxWidth().border(1.dp, Color(0xFF163B54), RoundedCornerShape(16.dp)).clickable { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
    Column(Modifier.fillMaxWidth().padding(15.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title.take(100), modifier = Modifier.weight(1f), color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2)
        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (expanded) "Contraer detalle" else "Ver detalle", tint = Mint)
      }
      visible.forEach { line -> Spacer(Modifier.height(5.dp)); Text(line.take(180), color = Muted, fontSize = 12.sp, maxLines = 2) }
      if (expanded) {
        Spacer(Modifier.height(10.dp)); HorizontalDivider(color = PanelSoft)
        Text("Detalle del registro", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 9.dp, bottom = 4.dp))
        details.forEach { (key, value) ->
          val rendered = displayValue(value)
          val label = fieldLabels[key] ?: key.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() }
          Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Text(label, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(rendered.take(1200), color = White, fontSize = 12.sp)
          }
        }
      } else Text("Tocá para ver todos los datos disponibles", color = Mint, fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp))
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
private fun firstPresentValue(item: Map<String, Any?>, vararg keys: String): String =
  keys.firstNotNullOfOrNull { key -> item[key]?.toString()?.takeIf { it.isNotBlank() && it != "null" && !looksLikeTechnicalId(it) } } ?: ""
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
