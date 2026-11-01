package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Store
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.RatingBadge
import com.example.ui.components.StoreTypeChip
import com.example.ui.theme.AmberDeal
import com.example.ui.theme.AmberDealContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateNavy
import androidx.compose.ui.platform.LocalContext
import com.example.ui.util.GoogleMapsHelper

@Composable
fun InteractiveRouteScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = viewModel.selectedStore.collectAsState().value ?: return
    val allStores by viewModel.stores.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()

    var isWalkingMode by remember { mutableStateOf(true) }
    var navigationStep by remember { mutableIntStateOf(1) }
    var isVoiceGuidanceActive by remember { mutableStateOf(false) }
    var selectedWaypointStore by remember { mutableStateOf<Store?>(null) }
    var isDetourEnabled by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    BackHandler {
        viewModel.navigateTo(Screen.StoreDetail)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("interactive_route_screen")
    ) {
        // MAPA INTERACTIVO DIBUJADO EN CANVAS CON TAP GESTURES
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE2E8F0))
                .pointerInput(allStores) {
                    detectTapGestures { tapOffset ->
                        val w = size.width
                        val h = size.height

                        // Detectar si el usuario tocó sobre algún comercio cercano en el mapa
                        val tappedStore = allStores.firstOrNull { other ->
                            val otherPos = Offset(w * other.mapOffsetX, h * other.mapOffsetY)
                            val dist = kotlin.math.hypot((tapOffset.x - otherPos.x).toDouble(), (tapOffset.y - otherPos.y).toDouble())
                            dist < 45.0
                        }

                        if (tappedStore != null) {
                            selectedWaypointStore = tappedStore
                        } else {
                            selectedWaypointStore = null
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. Manzanas / Bloques urbanos
            val blockColor = Color(0xFFF1F5F9)
            val streetColor = Color(0xFFCBD5E1)
            val gridStepX = w / 4f
            val gridStepY = h / 6f

            for (i in 0..4) {
                drawLine(
                    color = streetColor,
                    start = Offset(i * gridStepX, 0f),
                    end = Offset(i * gridStepX, h),
                    strokeWidth = 24f
                )
            }
            for (j in 0..6) {
                drawLine(
                    color = streetColor,
                    start = Offset(0f, j * gridStepY),
                    end = Offset(w, j * gridStepY),
                    strokeWidth = 24f
                )
            }

            // Manzanas
            for (i in 0..3) {
                for (j in 0..5) {
                    drawRoundRect(
                        color = blockColor,
                        topLeft = Offset(i * gridStepX + 14f, j * gridStepY + 14f),
                        size = androidx.compose.ui.geometry.Size(gridStepX - 28f, gridStepY - 28f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                }
            }

            // 2. Coordenadas de inicio (Usuario), posible desvío y fin
            val userPos = if (navigationStep == 1) Offset(w * 0.25f, h * 0.72f) else Offset(w * 0.45f, h * 0.55f)
            val targetPos = Offset(w * store.mapOffsetX, h * store.mapOffsetY)

            // Ruta interactiva calculada (se desvía dinámicamente si el usuario activó la parada)
            val routePath = Path().apply {
                moveTo(userPos.x, userPos.y)
                if (isDetourEnabled && selectedWaypointStore != null) {
                    val waypointPos = Offset(w * selectedWaypointStore!!.mapOffsetX, h * selectedWaypointStore!!.mapOffsetY)
                    lineTo(waypointPos.x, userPos.y)
                    lineTo(waypointPos.x, waypointPos.y)
                    lineTo(targetPos.x, waypointPos.y)
                    lineTo(targetPos.x, targetPos.y)
                } else {
                    val midY = (userPos.y + targetPos.y) / 2f
                    lineTo(userPos.x, midY)
                    lineTo(targetPos.x, midY)
                    lineTo(targetPos.x, targetPos.y)
                }
            }

            // Línea de ruta con resplandor
            drawPath(
                path = routePath,
                color = Color(0xFF0F766E).copy(alpha = 0.25f),
                style = Stroke(width = 18f, cap = StrokeCap.Round)
            )

            // Línea de ruta principal
            drawPath(
                path = routePath,
                color = Color(0xFF0F766E),
                style = Stroke(
                    width = 8f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 12f), 0f)
                )
            )

            // 3. Otros comercios cercanos como puntos de interés interactivos
            allStores.filter { it.id != store.id }.forEach { other ->
                val otherPos = Offset(w * other.mapOffsetX, h * other.mapOffsetY)
                val isSelected = selectedWaypointStore?.id == other.id

                if (isSelected) {
                    drawCircle(
                        color = AmberDeal.copy(alpha = 0.4f),
                        radius = 28f,
                        center = otherPos
                    )
                }

                drawCircle(
                    color = Color.White,
                    radius = 16f,
                    center = otherPos
                )
                drawCircle(
                    color = if (isSelected) AmberDeal else when (other.type) {
                        com.example.model.StoreType.KIOSCO -> Color(0xFFEF4444)
                        com.example.model.StoreType.PANADERIA -> Color(0xFFD97706)
                        else -> Color(0xFF64748B)
                    },
                    radius = 11f,
                    center = otherPos
                )
            }

            // 4. Pin del Comercio de Destino
            drawCircle(
                color = Color(0xFF0F766E).copy(alpha = 0.3f),
                radius = 32f,
                center = targetPos
            )
            drawCircle(
                color = Color.White,
                radius = 20f,
                center = targetPos
            )
            drawCircle(
                color = Color(0xFF0F766E),
                radius = 14f,
                center = targetPos
            )

            // 5. Pin del Usuario con pulso radar animado
            drawCircle(
                color = Color(0xFF2563EB).copy(alpha = 0.35f),
                radius = pulseRadius,
                center = userPos
            )
            drawCircle(
                color = Color.White,
                radius = 16f,
                center = userPos
            )
            drawCircle(
                color = Color(0xFF2563EB),
                radius = 10f,
                center = userPos
            )
        }

        // CABECERA SUPERIOR: Controles, Modo de Viaje y Asistente de Voz
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.StoreDetail) },
                    modifier = Modifier
                        .background(Color.White, CircleShape)
                        .size(42.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = SlateNavy)
                }

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        FilterChip(
                            selected = isWalkingMode,
                            onClick = { isWalkingMode = true },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("${store.walkingTimeMin + if (isDetourEnabled) 3 else 0} min") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(
                            selected = !isWalkingMode,
                            onClick = { isWalkingMode = false },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("${maxOf(1, store.walkingTimeMin / 3)} min") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                }

                // Botón de Guía por Voz
                IconButton(
                    onClick = { isVoiceGuidanceActive = !isVoiceGuidanceActive },
                    modifier = Modifier
                        .background(if (isVoiceGuidanceActive) EmeraldPrimary else Color.White, CircleShape)
                        .size(42.dp)
                ) {
                    Icon(
                        imageVector = if (isVoiceGuidanceActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Guía por voz",
                        tint = if (isVoiceGuidanceActive) Color.White else SlateNavy
                    )
                }
            }

            // Banner de Asistente de Audio
            if (isVoiceGuidanceActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = EmeraldDark,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (navigationStep == 1) "🔊 'En 120m gira a la derecha por Av. Mitre'" else "🔊 'A 50m llegas a tu destino: ${store.name}'",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Indicación paso a paso interactiva
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(EmeraldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (navigationStep == 1) "A 120m: Gira a la derecha en Av. Mitre" else "A 50m: Llegas a ${store.name} a tu izquierda",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isDetourEnabled && selectedWaypointStore != null) "Desvío activo: Parada en ${selectedWaypointStore!!.name}" else "Paso $navigationStep de 2 • Toca los comercios en el mapa para interactuar",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = { navigationStep = if (navigationStep == 1) 2 else 1 },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Siguiente paso",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Carrusel horizontal de Puntos del Camino para interacción táctil directa
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allStores.filter { it.id != store.id }) { other ->
                    val isSelected = selectedWaypointStore?.id == other.id
                    Surface(
                        color = if (isSelected) AmberDeal else Color.White,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 3.dp,
                        modifier = Modifier.clickable {
                            selectedWaypointStore = if (isSelected) null else other
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLocation,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else AmberDeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = other.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else SlateNavy
                            )
                        }
                    }
                }
            }
        }

        // TARJETA INFERIOR: Resumen o Detalle del Punto Tocado en Ruta
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (selectedWaypointStore != null) {
                    // Modal de punto interactivo en el camino
                    val wp = selectedWaypointStore!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(color = AmberDealContainer, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "📍 Punto en tu trayecto (Tocado en el mapa)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberDeal,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = wp.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SlateNavy)
                            Text(text = "${wp.address} • ${wp.specialOfferText}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isDetourEnabled = !isDetourEnabled },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDetourEnabled) Color.Red.copy(alpha = 0.8f) else AmberDeal
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isDetourEnabled) "Quitar parada" else "+ Parada intermedia",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.selectStoreAndOpenDetail(wp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Ver Ofertas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Comercio Destino Principal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StoreTypeChip(type = store.type)
                                Spacer(modifier = Modifier.width(6.dp))
                                RatingBadge(rating = store.rating, reviewsCount = store.reviewsCount)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = store.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateNavy
                            )
                        }

                        Text(
                            text = store.distanceText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = AmberDeal.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.LocalOffer, contentDescription = null, tint = AmberDeal, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = store.specialOfferText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberDeal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.navigateTo(Screen.StoreDetail) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Ver catálogo completo de ${store.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { GoogleMapsHelper.openSearchInGoogleMaps(context, "${store.name}, ${store.address}") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Navegar hacia este local en Google Maps 📍", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
