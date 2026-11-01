package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StoreType
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.StoreScheduleFilter
import com.example.ui.theme.CyberGlowAmber
import com.example.ui.theme.CyberGlowCyan
import com.example.ui.theme.CyberGlowGold
import com.example.ui.theme.CyberGlowGreen
import com.example.ui.theme.CyberGlowPink
import com.example.ui.theme.CyberGlowPurple
import com.example.ui.theme.CyberGlowRose
import com.example.ui.theme.CyberGlowTeal
import com.example.ui.theme.CyberOrbGradient

data class HomeCategoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

@Composable
fun DailyHubScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentCity by viewModel.currentCity.collectAsState()
    val displayLocality = currentCity.ifBlank { "Malabrigo" }
    val stores by viewModel.stores.collectAsState()

    var showTrackingDetails by remember { mutableStateOf(false) }
    var showRedeemDialog by remember { mutableStateOf(false) }
    var selectedExperience by remember { mutableStateOf<String?>(null) }

    // Servicios principales, sin accesos duplicados con el dock inferior
    val categoryItems = listOf(
        HomeCategoryItem(
            id = "food",
            title = "Comida",
            subtitle = "Pedí tus comidas favoritas",
            icon = Icons.Default.LunchDining,
            accentColor = Color(0xFFFFA33A),
            onClick = {
                viewModel.filterStoresByType(StoreType.GASTRONOMIA)
                viewModel.navigateTo(Screen.StoresList)
            }
        ),
        HomeCategoryItem(
            id = "orders",
            title = "Pedidos",
            subtitle = "Seguí y gestioná compras",
            icon = Icons.Default.ShoppingCart,
            accentColor = Color(0xFF25D9FF),
            onClick = {
                viewModel.navigateTo(Screen.SmartList)
            }
        ),
        HomeCategoryItem(
            id = "cabs",
            title = "Remises",
            subtitle = "Pedí viajes y traslados",
            icon = Icons.Default.DirectionsCar,
            accentColor = Color(0xFF438BFF),
            onClick = {
                viewModel.navigateTo(Screen.TripPlanner)
            }
        ),
        HomeCategoryItem(
            id = "services",
            title = "Servicios",
            subtitle = "Técnicos, oficios y gas",
            icon = Icons.Default.Build,
            accentColor = Color(0xFFB879FF),
            onClick = {
                viewModel.navigateTo(Screen.Contacts)
            }
        ),
        HomeCategoryItem(
            id = "contacts",
            title = "Contactos",
            subtitle = "Agenda y servicios del barrio",
            icon = Icons.Default.People,
            accentColor = Color(0xFF54E5FF),
            onClick = { viewModel.navigateTo(Screen.Contacts) }
        ),
        HomeCategoryItem(
            id = "health",
            title = "Salud",
            subtitle = "Farmacias y emergencias",
            icon = Icons.Default.Favorite,
            accentColor = Color(0xFFFF5C9A),
            onClick = {
                viewModel.setScheduleFilter(StoreScheduleFilter.FARMACIAS_TURNO)
                viewModel.navigateTo(Screen.StoresList)
            }
        ),
        HomeCategoryItem(
            id = "more",
            title = "Comercios",
            subtitle = "Explorá locales de tu zona",
            icon = Icons.Default.GridView,
            accentColor = Color(0xFF54E5FF),
            onClick = {
                viewModel.setScheduleFilter(StoreScheduleFilter.TODOS)
                viewModel.navigateTo(Screen.StoresList)
            }
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A14))
            .testTag("daily_hub_screen"),
        // El padding inferior de 100.dp previene que la barra dock o la barra del sistema tape contenido
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // 2. SERVICIOS PRINCIPALES
        // ==========================================
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Fila 1 (Comida, Pedidos, Remises, Servicios)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryItems.take(4).forEach { item ->
                        CyberCategoryCard(
                            item = item,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Fila 2 (Salud y Comercios)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryItems.drop(4).take(4).forEach { item ->
                        CyberCategoryCard(
                            item = item,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 3. OMNI EXPERIENCE
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1629)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberGlowPurple.copy(alpha = 0.48f)),
                modifier = Modifier.fillMaxWidth().testTag("home_omni_experience_card")
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF292044)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberGlowPurple, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("OMNI EXPERIENCE", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("Experiencias, oportunidades y beneficios", fontSize = 10.sp, color = Color(0xFF9AAAC2))
                        }
                        Surface(shape = RoundedCornerShape(7.dp), color = Color(0xFF211B39)) {
                            Text("NUEVO", color = CyberGlowPurple, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        val experiences = listOf(
                            Triple("🎓", "OMNI Joven", "Estudiantes"),
                            Triple("🧳", "OMNI Turista", "Visitantes"),
                            Triple("⚡", "OMNI Flash", "Ofertas rápidas"),
                            Triple("⭐", "OMNI Puntos", "Recompensas")
                        )
                        experiences.forEach { experience ->
                            Surface(
                                modifier = Modifier.weight(1f).clickable { selectedExperience = experience.second }.testTag("home_experience_${experience.second.lowercase().replace(" ", "_")}"),
                                shape = RoundedCornerShape(11.dp), color = Color(0xFF111F35),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberGlowPurple.copy(alpha = 0.28f))
                            ) {
                                Column(Modifier.padding(vertical = 9.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(experience.first, fontSize = 17.sp)
                                    Text(experience.second.removePrefix("OMNI "), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. TARJETA DE SEGUIMIENTO EN VIVO (En español)
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1424)),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, CyberGlowCyan.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTrackingDetails = !showTrackingDetails }
                    .testTag("home_live_tracking_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header con pulso verde "Seguimiento en Vivo"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Seguimiento en Vivo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF132238),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3557))
                        ) {
                            Text(
                                text = "Móvil Activo",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberGlowCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mapa estilizado con trazo de ruta neón y vehículo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF080D1A))
                            .border(1.dp, Color(0xFF142038), RoundedCornerShape(12.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Líneas de cuadrícula de mapa sutil
                            drawLine(color = Color(0xFF152238), start = Offset(0f, h * 0.35f), end = Offset(w, h * 0.35f), strokeWidth = 1f)
                            drawLine(color = Color(0xFF152238), start = Offset(0f, h * 0.7f), end = Offset(w, h * 0.7f), strokeWidth = 1f)
                            drawLine(color = Color(0xFF152238), start = Offset(w * 0.25f, 0f), end = Offset(w * 0.25f, h), strokeWidth = 1f)
                            drawLine(color = Color(0xFF152238), start = Offset(w * 0.65f, 0f), end = Offset(w * 0.65f, h), strokeWidth = 1f)

                            val routePath = Path().apply {
                                moveTo(w * 0.16f, h * 0.75f)
                                lineTo(w * 0.28f, h * 0.75f)
                                lineTo(w * 0.38f, h * 0.42f)
                                lineTo(w * 0.58f, h * 0.42f)
                                lineTo(w * 0.70f, h * 0.30f)
                                lineTo(w * 0.82f, h * 0.30f)
                            }

                            drawPath(path = routePath, color = Color(0xFFFF9100).copy(alpha = 0.35f), style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            drawPath(path = routePath, color = Color(0xFFFFB300), style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                            drawCircle(color = Color(0xFF00E5FF), radius = 6f, center = Offset(w * 0.16f, h * 0.75f))
                            drawCircle(color = Color.White, radius = 2.5f, center = Offset(w * 0.16f, h * 0.75f))
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 26.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(CyberGlowAmber),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TwoWheeler,
                                contentDescription = "Delivery en camino",
                                tint = Color.Black,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pedido Activo: Llega en 5 min.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Rotisería La Nueva • Pedido #OMNI-9082",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Text(
                            text = "Ver ruta en vivo →",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGlowCyan
                        )
                    }

                    AnimatedVisibility(visible = showTrackingDetails) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .background(Color(0xFF080E1C), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(text = "📍 Destino: Tu domicilio en $displayLocality", fontSize = 11.sp, color = Color(0xFFE2E8F0))
                            Text(text = "💳 Abonado con OmniPay ($3.450)", fontSize = 11.sp, color = CyberGlowPink)
                            Text(text = "🛵 Repartidor asignado: Matías (Cel: 3482-654321)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. HERO BANNER OMNI AI
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1629)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(Screen.OmniAi) }
                    .testTag("home_omni_ai_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(CyberOrbGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ai",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Omni AI Copilot",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EN LÍNEA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyberGlowAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Preguntale a la IA sobre farmacias de turno, remises y precios en $displayLocality.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Text(
                        text = "Consultar →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberGlowAmber
                    )
                }
            }
        }

        // ==========================================
        // 6. OMNI PLACES: COMERCIOS DE TU ZONA
        // ==========================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMERCIOS EN $displayLocality".uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Ver todos",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberGlowCyan,
                    modifier = Modifier.clickable {
                        viewModel.navigateTo(Screen.StoresList)
                    }
                )
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(stores.take(8), key = { it.id }) { store ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1829)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4A)),
                        modifier = Modifier
                            .width(185.dp)
                            .clickable {
                                viewModel.selectStore(store)
                                viewModel.navigateTo(Screen.StoreDetail)
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (store.isEffectivelyOpen) "Abierto 🟢" else "Cerrado 🔴",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (store.isEffectivelyOpen) Color(0xFF00E676) else Color(0xFFEF4444)
                                )
                                Text(
                                    text = "⭐ ${store.rating}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberGlowGold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = store.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = store.address,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    selectedExperience?.let { experience ->
        AlertDialog(
            onDismissRequest = { selectedExperience = null },
            containerColor = Color(0xFF101B30),
            title = { Text(experience, color = Color.White, fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    when (experience) {
                        "OMNI Joven" -> "Beneficios para estudiantes: descuentos, promociones, cupones y recompensas en comercios adheridos."
                        "OMNI Turista" -> "Ofertas de bienvenida, gastronomía, alojamientos y actividades para quienes visitan una localidad."
                        "OMNI Flash" -> "Promociones de duración limitada y oportunidades cercanas publicadas por comercios."
                        else -> "Un espacio para consultar puntos, beneficios acumulados y futuras opciones de canje."
                    } + "\n\nMódulo en etapa de prototipo. Sus funciones se incorporarán progresivamente.",
                    color = Color(0xFFD2DCEC)
                )
            },
            confirmButton = { TextButton(onClick = { selectedExperience = null }) { Text("Entendido", color = CyberGlowCyan) } }
        )
    }
}

/**
 * Tarjeta compacta Cyber Neon: doble contorno, halo pulsante y acento luminoso.
 * Conserva las dimensiones originales para no alterar la grilla.
 */
@Composable
fun CyberCategoryCard(item: HomeCategoryItem, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val transition = rememberInfiniteTransition(label = "category_neon")
    val pulse by transition.animateFloat(
        initialValue = 0.48f, targetValue = 0.82f,
        animationSpec = infiniteRepeatable(tween(2200), repeatMode = RepeatMode.Reverse),
        label = "category_glow"
    )
    val glow = if (pressed) 1f else pulse
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier.height(106.dp).clip(shape)
            .background(Brush.radialGradient(listOf(item.accentColor.copy(alpha = 0.16f), Color(0xFF101B30), Color(0xFF080F1D))))
            .drawBehind {
                val radius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx())
                drawRoundRect(item.accentColor.copy(alpha = 0.10f + glow * 0.10f), style = Stroke(9.dp.toPx()), cornerRadius = radius)
                drawRoundRect(item.accentColor.copy(alpha = 0.42f + glow * 0.38f), style = Stroke(1.35.dp.toPx()), cornerRadius = radius)
                drawRoundRect(Color.White.copy(alpha = 0.10f + glow * 0.08f), topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                    style = Stroke(0.65.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()))
                drawRoundRect(brush = Brush.horizontalGradient(listOf(Color.Transparent, item.accentColor.copy(alpha = 0.62f * glow), Color.Transparent)),
                    topLeft = Offset(size.width * 0.24f, 1.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width * 0.52f, 1.2.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            }
            .clickable(interactionSource = interaction, indication = null) { item.onClick() }
            .testTag("home_category_${item.id}"),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(29.dp).drawBehind {
                drawCircle(item.accentColor.copy(alpha = 0.18f + glow * 0.20f), radius = size.minDimension * 0.48f)
                drawCircle(item.accentColor.copy(alpha = 0.10f + glow * 0.12f), radius = size.minDimension * 0.62f)
            }, contentAlignment = Alignment.Center) {
                Icon(item.icon, contentDescription = item.title, tint = item.accentColor, modifier = Modifier.size(23.dp))
            }
            Text(item.title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            Text(item.subtitle, fontSize = 8.sp, lineHeight = 10.sp, color = Color(0xFF9AAAC2), maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 2.dp))
            Box(Modifier.width(18.dp).height(2.dp).clip(RoundedCornerShape(2.dp)).background(Brush.horizontalGradient(listOf(item.accentColor.copy(alpha = 0.55f), item.accentColor))))
        }
    }
}
