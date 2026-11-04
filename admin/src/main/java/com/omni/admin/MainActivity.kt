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
  AdminTab("wallets", "Billeteras", "walletAccounts"), AdminTab("operations", "Operaciones", "walletOperations"), AdminTab("finance", "Finanzas"),
  AdminTab("stores", "Comercios", "merchantProfiles"), AdminTab("orders", "Pedidos", "storeOrders"), AdminTab("events", "Eventos", "omniEvents"),
  AdminTab("feedback", "Comentarios", "appFeedback"), AdminTab("activity", "Actividad", "usageEvents"), AdminTab("modules", "Módulos", "moduleRegistry"),
  AdminTab("audit", "Auditoría", "adminAuditLogs"), AdminTab("invitations", "Invitaciones", "betaInvitations"), AdminTab("settings", "Configuración")
)
private data class AdminNavGroup(val id: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val tabIds: List<String>)
private val navGroups = listOf(
  AdminNavGroup("overview", "Inicio", Icons.Default.Dashboard, listOf("dashboard")),
  AdminNavGroup("people", "Personas", Icons.Default.PeopleAlt, listOf("users", "feedback")),
  AdminNavGroup("commerce", "Comercio", Icons.Default.Storefront, listOf("stores", "orders", "wallets", "operations", "finance")),
  AdminNavGroup("system", "Sistema", Icons.Default.Settings, listOf("activity", "events", "modules", "audit", "invitations", "settings"))
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
          "finance" -> AdminApi.call(token, "finance")
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
        if (nextPageToken.isNotBlank()) data.put("pageToken", nextPageToken)
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
  val activeGroup = navGroups.firstOrNull { selected in it.tabIds } ?: navGroups.first()
  val activeTab = tabs.firstOrNull { it.id == selected }
  Scaffold(
    containerColor = Navy,
    topBar = {
      Column(
        Modifier.fillMaxWidth()
          .background(Brush.verticalGradient(listOf(Color(0xFF10243A), Color(0xFF091321))))
          .border(width = 1.dp, color = Color(0xFF12364D))
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(13.dp))
              .background(Brush.linearGradient(listOf(Mint, Color(0xFF1268D8)))),
            contentAlignment = Alignment.Center
          ) { Text("O", color = Navy, fontSize = 25.sp, fontWeight = FontWeight.Black) }
          Spacer(Modifier.width(10.dp))
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("OMNI ADMIN", color = White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Text(activeTab?.label ?: "Centro de control", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          }
          IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar datos", tint = Mint) }
          IconButton(onClick = onSignOut) { Icon(Icons.Default.Logout, "Cerrar sesión", tint = Muted) }
        }
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(7.dp).background(Mint, RoundedCornerShape(50)))
          Spacer(Modifier.width(7.dp))
          Text(email, color = Muted, fontSize = 11.sp, maxLines = 1)
          Spacer(Modifier.weight(1f))
          Text("PANEL PRIVADO", color = Mint, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        val groupTabs = tabs.filter { it.id in activeGroup.tabIds }
        if (groupTabs.size > 1) {
          Spacer(Modifier.height(12.dp))
          Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            groupTabs.forEach { tab ->
              FilterChip(
                selected = selected == tab.id,
                onClick = { onSelect(tab.id) },
                label = { Text(tab.label, maxLines = 1, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Mint,
                  selectedLabelColor = Navy,
                  labelColor = Muted,
                  containerColor = PanelSoft
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true, selected = selected == tab.id,
                  borderColor = Color(0xFF21445D), selectedBorderColor = Mint
                )
              )
            }
          }
        }
      }
    },
    bottomBar = {
      NavigationBar(containerColor = Color(0xFF0A1423), contentColor = Mint, tonalElevation = 0.dp) {
        navGroups.forEach { group ->
          NavigationBarItem(
            selected = activeGroup.id == group.id,
            onClick = { onSelect(group.tabIds.first()) },
            icon = { Icon(group.icon, contentDescription = group.label) },
            label = { Text(group.label, fontSize = 10.sp, maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = Mint,
              selectedTextColor = Mint,
              indicatorColor = Color(0xFF12384C),
              unselectedIconColor = Muted,
              unselectedTextColor = Muted
            )
          )
        }
      }
    }
  ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding)) {
      if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Mint, trackColor = Panel)
      if (error.isNotBlank()) Text(
        error,
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = if (error.contains("correctamente") || error.contains("registrada") || error.contains("actualizado")) Mint else Danger,
        fontSize = 13.sp
      )
      when (selected) {
        "dashboard" -> DashboardContent(payload, onSelect)
        "finance" -> FinanceContent(payload)
        "modules" -> ModuleContent(payload.optJSONArray("items") ?: JSONArray(), onSaveModule)
        "settings" -> ConfigContent(payload.optJSONObject("config") ?: JSONObject(), onSaveConfig)
        "invitations" -> InvitationContent(payload.optJSONArray("items") ?: JSONArray(), onInvite)
        else -> RecordsContent(selected, payload.optJSONArray("items") ?: JSONArray(), hasMore, onLoadMore)
      }
    }
  }
}

@Composable
private fun DashboardContent(payload: JSONObject, onSelect: (String) -> Unit) {
  val counts = payload.optJSONObject("counts") ?: JSONObject()
  val analytics = payload.optJSONObject("analytics") ?: JSONObject()
  val diagnostics = payload.optJSONObject("diagnostics") ?: JSONObject()
  val metrics = listOf(
    Triple("Usuarios", counts.optInt("users").toString(), Icons.Default.PeopleAlt),
    Triple("Comercios", counts.optInt("merchantProfiles").toString(), Icons.Default.Storefront),
    Triple("Pedidos", counts.optInt("storeOrders").toString(), Icons.Default.ShoppingCart),
    Triple("Billeteras DEMO", counts.optInt("walletAccounts").toString(), Icons.Default.AccountBalanceWallet),
    Triple("Capital virtual", "OM$ " + String.format("%.0f", payload.optDouble("walletBalanceSample")), Icons.Default.AccountBalance),
    Triple("Activos · 7 días", payload.optInt("activeUsers7d").toString(), Icons.Default.Person),
    Triple("Sesiones · 7 días", analytics.optInt("sessions7d").toString(), Icons.Default.QueryStats),
    Triple("Operaciones", counts.optInt("walletOperations").toString(), Icons.Default.SwapHoriz),
    Triple("Eventos", counts.optInt("omniEvents").toString(), Icons.Default.Event)
  )
  val trend = jsonArrayToList(payload.optJSONArray("usageTrend"))
  val recentUsers = jsonArrayToList(payload.optJSONArray("recentUsers"))
  val recentActivity = jsonArrayToList(payload.optJSONArray("recentUsageEvents"))
  val recentOperations = jsonArrayToList(payload.optJSONArray("recentOperations"))
  val eventCount = diagnostics.optInt("usageEventsVisibleLast7Days")
  val testSeen = diagnostics.optBoolean("diagnosticEventSeen")
  BoxWithConstraints(Modifier.fillMaxSize().background(Navy)) {
    val columns = if (maxWidth >= 900.dp) 4 else if (maxWidth >= 560.dp) 4 else 2
    val tabletLayout = maxWidth >= 560.dp
    val wideLayout = maxWidth >= 700.dp
    LazyColumn(
      Modifier.fillMaxSize().padding(horizontal = if (wideLayout) 22.dp else 14.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Row(
          Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF123A5D), Color(0xFF101B35), Color(0xFF0A1426))))
            .border(1.dp, Color(0xFF1679A6), RoundedCornerShape(22.dp)).padding(18.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            Modifier.size(58.dp).clip(RoundedCornerShape(50))
              .background(Brush.radialGradient(listOf(Color(0xFF27DFFF), Color(0xFF07538E)))),
            contentAlignment = Alignment.Center
          ) { Icon(Icons.Default.AdminPanelSettings, null, tint = White, modifier = Modifier.size(31.dp)) }
          Spacer(Modifier.width(14.dp))
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("OMNI · CONTROL CENTER", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.7.sp)
            Text("Bienvenido, Administrador", color = White, fontSize = if (tabletLayout) 23.sp else 19.sp, fontWeight = FontWeight.ExtraBold)
            Text("Panel ejecutivo del ecosistema OMNI", color = Muted, fontSize = 12.sp)
          }
          if (tabletLayout) {
            Column(horizontalAlignment = Alignment.End) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).background(if (testSeen) Color(0xFF39F59A) else Color(0xFFFFB84D), RoundedCornerShape(50)))
                Text(if (testSeen) "Conectividad verificada" else "Datos en observación", color = if (testSeen) Color(0xFF39F59A) else Color(0xFFFFB84D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
              Text("OMNI ADMIN", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
            }
          }
        }
      }
      items(metrics.chunked(columns)) { rowMetrics ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          rowMetrics.forEachIndexed { index, metric ->
            val accents = listOf(Color(0xFF16CFFF), Color(0xFF32E7A0), Color(0xFFBD70FF), Color(0xFFFFB52E))
            val accent = accents[(metrics.indexOf(metric) % accents.size)]
            val destination = when (metric.first) {
              "Usuarios", "Activos · 7 días" -> "users"
              "Comercios" -> "stores"
              "Pedidos" -> "orders"
              "Billeteras DEMO" -> "wallets"
              "Capital virtual" -> "finance"
              "Sesiones · 7 días" -> "activity"
              "Operaciones" -> "operations"
              "Eventos" -> "events"
              else -> "dashboard"
            }
            Card(
              Modifier.weight(1f).heightIn(min = 118.dp)
                .clickable { onSelect(destination) }
                .border(1.dp, accent.copy(alpha = 0.72f), RoundedCornerShape(18.dp)),
              colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1830)),
              shape = RoundedCornerShape(18.dp),
              elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
              Column(
                Modifier.fillMaxSize().background(Brush.linearGradient(listOf(accent.copy(alpha = 0.16f), Color(0xFF0B1830), Color(0xFF091426))))
                  .padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
              ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                  Icon(metric.third, null, tint = accent, modifier = Modifier.size(23.dp))
                  Spacer(Modifier.weight(1f))
                  Icon(Icons.Default.ArrowOutward, contentDescription = "Abrir ${metric.first}", tint = accent, modifier = Modifier.size(16.dp))
                }
                Text(metric.second, color = White, fontSize = if (tabletLayout) 27.sp else 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(metric.first, color = Muted, fontSize = 10.sp, maxLines = 2, lineHeight = 13.sp)
              }
            }
          }
          repeat((columns - rowMetrics.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
        }
      }
      item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Card(
            Modifier.weight(if (wideLayout) 1.7f else 1f),
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(20.dp)
          ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShowChart, null, tint = Mint)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                  Text("Evolución de OMNI", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                  Text("Actividad registrada · últimos 7 días", color = Muted, fontSize = 10.sp)
                }
                Text("7 DÍAS", color = Mint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
              }
              if (trend.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(142.dp), contentAlignment = Alignment.Center) {
                  Text("La evolución aparecerá cuando haya actividad registrada.", color = Muted, fontSize = 12.sp)
                }
              } else {
                val maxValue = (trend.maxOfOrNull { (it["count"] as? Number)?.toFloat() ?: 0f } ?: 0f).coerceAtLeast(1f)
                Row(Modifier.fillMaxWidth().height(142.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
                  trend.takeLast(7).forEach { point ->
                    val value = (point["count"] as? Number)?.toFloat() ?: 0f
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                      Text(value.toInt().toString(), color = White, fontSize = 9.sp)
                      Spacer(Modifier.height(4.dp))
                      Box(
                        Modifier.fillMaxWidth().fillMaxHeight((value / maxValue).coerceIn(0.06f, 1f))
                          .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                          .background(Brush.verticalGradient(listOf(Mint, Color(0xFF0876B5))))
                      )
                      Spacer(Modifier.height(5.dp))
                      Text(point["date"]?.toString()?.takeLast(5) ?: "—", color = Muted, fontSize = 8.sp, maxLines = 1)
                    }
                  }
                }
              }
            }
          }
          if (wideLayout) {
            Card(
              Modifier.weight(1f),
              colors = CardDefaults.cardColors(containerColor = Panel),
              shape = RoundedCornerShape(20.dp)
            ) {
              Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.MonitorHeart, null, tint = Mint)
                  Spacer(Modifier.width(7.dp))
                  Text("Estado del sistema", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.size(62.dp).border(2.dp, if (testSeen) Color(0xFF36F39A) else Color(0xFFFFB84D), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                  Icon(if (testSeen) Icons.Default.Check else Icons.Default.Info, null, tint = if (testSeen) Color(0xFF36F39A) else Color(0xFFFFB84D), modifier = Modifier.size(32.dp))
                }
                Text(if (testSeen) "Conexión verificada" else "En observación", color = if (testSeen) Color(0xFF36F39A) else Color(0xFFFFB84D), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Eventos visibles (7 días): $eventCount", color = Muted, fontSize = 10.sp)
                Text("Perfiles visibles: " + diagnostics.optInt("userProfilesVisible"), color = Muted, fontSize = 10.sp)
              }
            }
          }
        }
      }
      if (maxWidth < 700.dp) {
        item {
          Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.MonitorHeart, null, tint = if (testSeen) Color(0xFF36F39A) else Color(0xFFFFB84D), modifier = Modifier.size(26.dp))
              Spacer(Modifier.width(10.dp))
              Column(Modifier.weight(1f)) {
                Text("Estado del sistema", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(if (testSeen) "Conectividad verificada" else "Evento de diagnóstico aún no visible", color = Muted, fontSize = 10.sp)
              }
              Text("$eventCount", color = Mint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
      item {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.History, null, tint = Mint)
          Spacer(Modifier.width(8.dp))
          Text("Actividad reciente", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
          Spacer(Modifier.weight(1f))
          Text("REGISTROS REALES", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
      }
      item {
        val combined = (recentActivity + recentOperations).take(5)
        if (combined.isEmpty()) {
          EmptyState("Todavía no hay actividad reciente disponible.")
        } else {
          Card(Modifier.fillMaxWidth().border(1.dp, Color(0xFF174B69), RoundedCornerShape(18.dp)), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              combined.forEachIndexed { index, record ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(if (index % 2 == 0) Color(0xFF103B58) else Color(0xFF17314A)), contentAlignment = Alignment.Center) {
                    Icon(if (index % 2 == 0) Icons.Default.Bolt else Icons.Default.ReceiptLong, null, tint = if (index % 2 == 0) Mint else Color(0xFFB77CFF), modifier = Modifier.size(19.dp))
                  }
                  Column(Modifier.weight(1f)) {
                    Text(firstPresentValue(record, "event", "eventName", "kind", "type", "action").ifBlank { "Actividad registrada" }.let { friendlyEnum(it) }, color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(firstPresentValue(record, "userLabel", "storeName", "email", "module").ifBlank { "Ecosistema OMNI" }, color = Muted, fontSize = 10.sp, maxLines = 1)
                  }
                  Text(firstPresentValue(record, "createdAt", "timestamp").take(16), color = Muted, fontSize = 9.sp, maxLines = 1)
                }
                if (index < combined.lastIndex) HorizontalDivider(color = Color(0xFF1A2E49))
              }
            }
          }
        }
      }
      item {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.PeopleAlt, null, tint = Mint)
          Spacer(Modifier.width(8.dp))
          Text("Usuarios recientes", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
      }
      if (recentUsers.isEmpty()) item { EmptyState("Todavía no hay perfiles recientes disponibles.") }
      else items(recentUsers.take(3)) { RecordCard(it, "users") }
      item {
        Text("OMNI ADMIN  ·  CONTROL CENTRALIZADO", Modifier.fillMaxWidth().padding(top = 8.dp), color = Color(0xFF4F7998), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
      }
    }
  }
}

@Composable
private fun FinanceContent(payload: JSONObject) {
  val summary = payload.optJSONObject("summary") ?: JSONObject()
  val accounts = jsonArrayToList(payload.optJSONArray("accounts"))
  val transactions = jsonArrayToList(payload.optJSONArray("transactions"))
  val operations = jsonArrayToList(payload.optJSONArray("operations"))
  fun money(key: String): String = "OM$ " + String.format("%.2f", summary.optDouble(key))
  LazyColumn(
    Modifier.fillMaxSize().background(Navy).padding(horizontal = 15.dp),
    contentPadding = PaddingValues(vertical = 18.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
          .background(Brush.linearGradient(listOf(Color(0xFF123A5D), Color(0xFF101B35), Color(0xFF0A1426))))
          .border(1.dp, Color(0xFF1679A6), RoundedCornerShape(22.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text("OMNI PAY · CONTROL FINANCIERO DEMO", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
        Text(money("currentBalance"), color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text("Capital virtual actualmente distribuido entre las billeteras", color = Muted, fontSize = 12.sp)
        Text("${summary.optInt("accountCount")} cuentas · ${summary.optInt("transactionCount")} movimientos individuales", color = Mint, fontSize = 11.sp)
      }
    }
    item { Text("Capital y circulación", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        FinanceMetric("Capital inicial asignado", money("initialCapital"), Icons.Default.Savings, Modifier.weight(1f))
        FinanceMetric("Recargas", money("deposits"), Icons.Default.AddCard, Modifier.weight(1f))
      }
    }
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        FinanceMetric("Transferencias", money("transferVolume"), Icons.Default.SwapHoriz, Modifier.weight(1f))
        FinanceMetric("Pagos QR", money("qrVolume"), Icons.Default.QrCode2, Modifier.weight(1f))
      }
    }
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        FinanceMetric("Transferencias recibidas", money("transferIn"), Icons.Default.CallReceived, Modifier.weight(1f))
        FinanceMetric("Cobros QR", money("qrCollections"), Icons.Default.QrCode2, Modifier.weight(1f))
      }
    }
    item {
      Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF1A5068), RoundedCornerShape(16.dp))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
          Text("Lectura correcta del capital", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          Text("Créditos totales: ${money("totalCredits")}", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Text("Salidas registradas: ${money("outflows")}", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Text("Las transferencias internas y los pagos QR representan circulación del saldo, no capital nuevo. No se suman a las recargas.", color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
          Text("Datos de prueba. No representan dinero real ni fondos bancarios.", color = Color(0xFFFFC66D), fontSize = 10.sp)
        }
      }
    }
    item { Text("Cuentas y saldos", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    if (accounts.isEmpty()) item { EmptyState("Todavía no hay billeteras registradas.") }
    else items(accounts, key = { it["id"]?.toString() ?: it.hashCode().toString() }) { RecordCard(it, "walletAccounts") }
    item { Text("Historial de movimientos", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    if (transactions.isEmpty()) item { EmptyState("Todavía no hay movimientos individuales registrados.") }
    else items(transactions, key = { it["id"]?.toString() ?: it.hashCode().toString() }) { RecordCard(it, "walletTransactions") }
    item { Text("Operaciones globales", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    if (operations.isEmpty()) item { EmptyState("Las operaciones se reflejan en el historial individual de cada billetera. La colección global todavía no contiene registros.") }
    else items(operations.take(100), key = { it["id"]?.toString() ?: it.hashCode().toString() }) { RecordCard(it, "walletOperations") }
  }
}

@Composable
private fun FinanceMetric(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
  Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1830)), shape = RoundedCornerShape(15.dp)) {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
      Icon(icon, null, tint = Mint, modifier = Modifier.size(20.dp))
      Text(value, color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
      Text(label, color = Muted, fontSize = 10.sp, maxLines = 2)
    }
  }
}

@Composable
private fun RecordsContent(selected: String, array: JSONArray, hasMore: Boolean, onLoadMore: () -> Unit) {
  val records = jsonArrayToList(array)
  var search by remember(selected) { mutableStateOf("") }
  val filtered = remember(records, search) {
    if (search.isBlank()) records else records.filter { record ->
      record.entries.any { (key, value) ->
        key !in setOf("password", "token", "accessToken", "refreshToken", "secret", "apiKey", "rawData", "authorization") &&
          displayValue(value).contains(search.trim(), ignoreCase = true)
      }
    }
  }
  LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
          .background(Brush.linearGradient(listOf(Color(0xFF12334B), Panel, Color(0xFF0A1423))))
          .border(1.dp, Color(0xFF1A6078), RoundedCornerShape(22.dp)).padding(19.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
      ) {
        Text("GESTIÓN · OMNI ADMIN", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp)
        Text(tabs.first { it.id == selected }.label, color = White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text("${filtered.size} de ${records.size} registros cargados · historial paginado", color = Muted, fontSize = 12.sp)
      }
    }
    item {
      OutlinedTextField(
        value = search, onValueChange = { search = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Buscar en ${tabs.first { it.id == selected }.label.lowercase()}") },
        placeholder = { Text("Nombre, correo, estado o referencia") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, null, tint = Mint) },
        trailingIcon = { if (search.isNotBlank()) IconButton(onClick = { search = "" }) { Icon(Icons.Default.Close, "Limpiar", tint = Muted) } }
      )
    }
    if (filtered.isEmpty()) item { EmptyState(if (records.isEmpty()) "Todavía no hay registros para mostrar." else "No hay coincidencias con esa búsqueda.") }
    items(filtered) { RecordCard(it, selected) }
    if (hasMore && search.isBlank()) item {
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
  "amount" to "Importe", "peerName" to "Contraparte", "title" to "Concepto", "senderUid" to "Cuenta de origen", "recipientUid" to "Cuenta de destino", "kind" to "Tipo", "type" to "Tipo", "status" to "Estado",
  "storeName" to "Comercio", "orderNumber" to "Pedido", "total" to "Total",
  "paymentStatus" to "Estado del pago", "paymentMethod" to "Medio de pago",
  "fulfillmentType" to "Entrega", "action" to "Acción", "target" to "Destino",
  "message" to "Mensaje", "description" to "Descripción", "enabled" to "Habilitado",
  "event" to "Evento", "module" to "Módulo", "eventName" to "Evento", "screen" to "Pantalla", "appVersion" to "Versión",
  "category" to "Categoría", "startAt" to "Inicio", "endAt" to "Finalización",
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
  "DEPOSIT" -> "Recarga virtual DEMO"
  "CARGA_SALDO" -> "Crédito de saldo DEMO"
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
private fun friendlyModule(value: String): String = when (value.trim().lowercase()) {
  "wallet", "wallet_qr" -> "Billetera"
  "orders", "store_orders", "marketplace" -> "Pedidos y Mercado"
  "events" -> "Eventos"
  "jobs" -> "Bolsa de Trabajo"
  "merchant", "merchants", "commerce" -> "Comercios"
  "users", "profile" -> "Usuarios"
  "navigation", "maps", "maps_locations" -> "Navegación y mapas"
  "tourism" -> "Turismo"
  "routine_weather" -> "Rutinas y clima"
  "ai_camera" -> "Cámara e inteligencia artificial"
  "app", "diagnostics" -> "Sistema"
  else -> value.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
private fun looksLikeTechnicalId(value: String): Boolean =
  value.length >= 22 && value.all { it.isLetterOrDigit() || it == '-' || it == '_' }

private fun activityCategory(item: Map<String, Any?>): String {
  val data = item["data"] as? Map<*, *> ?: emptyMap<Any, Any>()
  val sources = listOf(
    item["category"], item["module"], item["screenName"], item["screen"],
    item["source"], item["event"], item["eventName"], item["action"], data["category"], data["screenName"], data["source"],
    data["surface"], data["flow"]
  ).mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }
    .joinToString(" ").lowercase()
  val event = firstPresentValue(item, "event", "eventName", "action", "type").uppercase()
  return when {
    listOf("wallet", "billetera", "saldo", "qr_pay", "qr_payment", "wallet_qr", "omni_pay", "transfer", "deposit").any(sources::contains) ||
      event.contains("QR_PAYMENT") || event.contains("TRANSFER") || event.contains("DEPOSIT") -> "Billetera"
    listOf("order", "pedido", "market", "carrito", "checkout").any(sources::contains) -> "Pedidos y Mercado"
    listOf("event", "evento").any(sources::contains) -> "Eventos"
    listOf("job", "trabajo", "empleo", "cv").any(sources::contains) -> "Bolsa de Trabajo"
    listOf("merchant", "comercio", "store", "negocio").any(sources::contains) -> "Comercios"
    listOf("user", "usuario", "profile", "perfil", "contact").any(sources::contains) -> "Usuarios"
    listOf("navigation", "map", "mapa", "route", "ruta", "location", "ubicacion").any(sources::contains) -> "Navegación"
    listOf("tourism", "turismo", "experience", "experiencia").any(sources::contains) -> "Turismo y experiencias"
    listOf("routine", "rutina", "weather", "clima", "reminder", "recordatorio").any(sources::contains) -> "Rutinas y clima"
    listOf("camera", "camara", "ai", "inteligencia").any(sources::contains) -> "Cámara e IA"
    event.startsWith("SESSION_") || event.startsWith("APP_") || event.startsWith("SCREEN_") ||
      sources.contains("app") || sources.contains("diagnostics") -> "Sistema"
    else -> "Otros"
  }
}

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
    "activity", "usageEvents" -> { val category = activityCategory(item); val action = friendlyEnum(firstPresentValue(item, "event", "eventName", "action", "type").ifBlank { "Actividad registrada" }); if (category == "Otros") action else "$category · $action" }
    "walletTransactions" -> friendlyEnum(firstPresentValue(item, "type", "kind", "title").ifBlank { "Movimiento de billetera" })
    "omniEvents" -> firstPresentValue(item, "title", "name", "event", "eventName").ifBlank { "Evento OMNI" }
    "adminAuditLogs" -> friendlyEnum(firstPresentValue(item, "action").ifBlank { "Actividad administrativa" })
    else -> firstPresentValue(item, "title", "name", "action").ifBlank { "Actividad sin categoría" }
  }
  val preferred = when (type) {
    "users" -> listOf("email", "phoneNumber", "phone", "createdAt", "lastSeenAt")
    "walletAccounts" -> listOf("balance", "cashbackTotal", "createdAt")
    "walletOperations" -> listOf("amount", "status", "createdAt")
    "walletTransactions" -> listOf("amount", "status", "peerName", "createdAt")
    "merchantProfiles" -> listOf("description", "category", "address", "status", "createdAt")
    "storeOrders" -> listOf("total", "paymentStatus", "fulfillmentType", "createdAt")
    "appFeedback" -> listOf("message", "createdAt")
    "activity", "usageEvents" -> listOf("userLabel", "appVersion", "createdAt")
    "omniEvents" -> listOf("description", "category", "status", "startAt", "createdAt")
    else -> listOf("status", "createdAt", "updatedAt")
  }
  val visible = preferred.mapNotNull { key ->
    val value = item[key] ?: return@mapNotNull null
    val rendered = displayValue(value)
    if (rendered.isBlank() || looksLikeTechnicalId(rendered)) null
    else (if (key == "module" && (type == "activity" || type == "usageEvents")) "Categoría" else fieldLabels[key] ?: key) + ": " + when {
      key == "module" && (type == "activity" || type == "usageEvents") -> friendlyModule(rendered)
      key == "kind" || key == "type" || key == "status" || key == "paymentStatus" -> friendlyEnum(rendered)
      else -> rendered
    }
  }
  val protectedKeys = setOf("password", "token", "accessToken", "refreshToken", "secret", "apiKey", "qrNonce", "rawData", "authorization")
  val details = item.entries.filter { (key, value) -> key !in protectedKeys && value != null && displayValue(value).isNotBlank() }.sortedBy { it.key }
  Card(
    Modifier.fillMaxWidth().border(1.dp, Color(0xFF1A5068), RoundedCornerShape(19.dp)).clickable { expanded = !expanded },
    colors = CardDefaults.cardColors(containerColor = Panel),
    shape = RoundedCornerShape(19.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
  ) {
    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF10243A), Panel))).padding(17.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title.take(100), modifier = Modifier.weight(1f), color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2)
        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (expanded) "Contraer detalle" else "Ver detalle", tint = Mint)
      }
      if (type == "activity" || type == "usageEvents") {
        Text("Categoría: ${activityCategory(item)}", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        val actionName = firstPresentValue(item, "event", "eventName", "action", "type").ifBlank { "Actividad registrada" }
        Text("Acción: ${friendlyEnum(actionName)}", color = Muted, fontSize = 12.sp, maxLines = 2)
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
private fun ModuleSummary(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
  Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF10243A)), shape = RoundedCornerShape(13.dp)) {
    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Icon(icon, null, tint = Mint, modifier = Modifier.size(17.dp))
      Text(value, color = White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
      Text(label, color = Muted, fontSize = 9.sp, maxLines = 1)
    }
  }
}

@Composable
private fun ModuleContent(array: JSONArray, onSave: (String, String, String, Boolean) -> Unit) {
  val records = jsonArrayToList(array)
  val enabledCount = records.count { it["enabled"] == true }
  LazyColumn(Modifier.fillMaxSize().padding(16.dp), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
    item {
      Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xFF123A5D), Panel, Color(0xFF0A1426)))).border(1.dp, Color(0xFF1679A6), RoundedCornerShape(22.dp)).padding(19.dp)) {
        Text("CENTRO DE FUNCIONES", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp)
        Text("Módulos de OMNI", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Supervisión del catálogo registrado. Los cambios quedan auditados.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          ModuleSummary("Registrados", records.size.toString(), Icons.Default.Apps, Modifier.weight(1f))
          ModuleSummary("Habilitados", enabledCount.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
          ModuleSummary("Pausados", (records.size - enabledCount).toString(), Icons.Default.PauseCircle, Modifier.weight(1f))
        }
      }
    }
    if (records.isEmpty()) item { EmptyState("No hay módulos registrados en moduleRegistry. No se muestran módulos inventados.") }
    items(records) { item ->
      val id = item["id"]?.toString() ?: return@items
      var enabled by remember(id, item["enabled"]) { mutableStateOf(item["enabled"] == true) }
      val title = item["title"]?.toString() ?: id
      val description = item["description"]?.toString() ?: "Sin descripción registrada."
      Card(Modifier.fillMaxWidth().border(1.dp, if (enabled) Color(0xFF17647A) else Color(0xFF34445B), RoundedCornerShape(18.dp)), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(if (enabled) Color(0xFF103B4B) else PanelSoft), contentAlignment = Alignment.Center) { Icon(Icons.Default.Extension, null, tint = if (enabled) Mint else Muted) }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) { Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(id, color = Muted, fontSize = 10.sp) }
            Column(horizontalAlignment = Alignment.End) {
              Text(if (enabled) "HABILITADO" else "PAUSADO", color = if (enabled) Mint else Muted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
              Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
          }
          Text(description, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
          val updated = item["updatedAt"]?.toString().orEmpty()
          if (updated.isNotBlank()) Text("Última modificación: ${updated.take(16).replace("T", " ")}", color = Color(0xFF7890AA), fontSize = 10.sp)
          Button(onClick = { onSave(id, title, description, enabled) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text("Guardar cambio") }
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
  Card(
    Modifier.fillMaxWidth().border(1.dp, Color(0xFF1A4058), RoundedCornerShape(18.dp)),
    colors = CardDefaults.cardColors(containerColor = Panel),
    shape = RoundedCornerShape(18.dp)
  ) {
    Row(Modifier.fillMaxWidth().padding(19.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Icon(Icons.Default.Info, contentDescription = null, tint = Mint)
      Text(message, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
    }
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
