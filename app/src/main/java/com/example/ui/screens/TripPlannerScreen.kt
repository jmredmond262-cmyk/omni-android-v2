package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TravelMode
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.AmberDeal
import com.example.ui.theme.AmberDealContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RosePromo
import com.example.ui.theme.SlateNavy
import androidx.compose.ui.platform.LocalContext
import com.example.ui.util.GoogleMapsHelper

@Composable
fun TripPlannerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val trip by viewModel.currentTrip.collectAsState()
    val tripOrigin by viewModel.tripOrigin.collectAsState()
    val tripDestination by viewModel.tripDestination.collectAsState()
    val attractions by viewModel.touristAttractions.collectAsState()
    val liveResults by viewModel.liveSearchResults.collectAsState()
    val isSearching by viewModel.isSearchingPlaces.collectAsState()

    var originInput by remember(tripOrigin) { mutableStateOf(tripOrigin) }
    var destinationInput by remember(tripDestination) { mutableStateOf(tripDestination) }
    var selectedTravelMode by remember { mutableStateOf(TravelMode.DRIVING) }
    var isLiveNavigationActive by remember { mutableStateOf(false) }
    var waypointConfirmationMessage by remember { mutableStateOf<String?>(null) }

    val quickDestinations = listOf(
        "Reconquista, Santa Fe",
        "Santa Fe Capital",
        "San Javier, Santa Fe",
        "Malabrigo, Santa Fe",
        "Camping Comunal Romang",
        "Rosario, Santa Fe",
        "El Obelisco (Buenos Aires)"
    )

    BackHandler {
        viewModel.navigateTo(Screen.DailyHub)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("trip_planner_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera con Buscador de Rutas (Origen -> Destino)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateTo(Screen.DailyHub) },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Planificador de Recorridos",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Busca tu viaje sin salir de tu comercio o ubicación",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Input de Origen
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = originInput,
                                onValueChange = { originInput = it },
                                placeholder = { Text("Punto de partida o local...", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input de Destino
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = RosePromo, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = destinationInput,
                                onValueChange = { destinationInput = it },
                                placeholder = { Text("¿Hacia dónde vas? Ej: El Obelisco", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.searchTrip(originInput, destinationInput) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Trazar Ruta", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.searchRealDestinations(destinationInput) },
                            enabled = !isSearching,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = if (isSearching) "Buscando..." else "📡 Buscar en Vivo", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        }
                    }

                    // Resultados en vivo de OpenStreetMap
                    if (liveResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Lugares Reales Detectados (${liveResults.size}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            liveResults.forEach { result ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            destinationInput = result.name
                                            viewModel.selectRealLocationAsDestination(result)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = result.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(text = result.displayName, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Destinos Rápidos Sugeridos
        item {
            Column {
                Text(
                    text = "Destinos Populares:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateNavy.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickDestinations) { dest ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 1.dp,
                            modifier = Modifier.clickable {
                                destinationInput = dest
                                viewModel.searchTrip(originInput, dest)
                            }
                        ) {
                            Text(
                                text = "📍 $dest",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateNavy,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Alternador de Medios de Transporte & Estimaciones
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (trip.distanceKm > 0.0) "Distancia: ${String.format("%.1f", trip.distanceKm)} km" else "Distancia no disponible",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SlateNavy
                        )

                        Text(
                            text = "Hacia ${trip.destinationName}",
                            fontSize = 12.sp,
                            color = EmeraldDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = trip.distanceSourceLabel,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Opción Auto / Remis
                        FilterChip(
                            selected = selectedTravelMode == TravelMode.DRIVING,
                            onClick = { selectedTravelMode = TravelMode.DRIVING },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(if (trip.drivingMinutes > 0) "Auto • ${trip.drivingMinutes} min" else "Auto • —") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Opción Transporte
                        FilterChip(
                            selected = selectedTravelMode == TravelMode.TRANSIT,
                            onClick = { selectedTravelMode = TravelMode.TRANSIT },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.DirectionsBus, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(if (trip.transitMinutes > 0) "Transp. aprox. • ${trip.transitMinutes} min" else "Transp. • —") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Opción A pie
                        FilterChip(
                            selected = selectedTravelMode == TravelMode.WALKING,
                            onClick = { selectedTravelMode = TravelMode.WALKING },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(if (trip.walkingMinutes > 0) "A pie aprox. • ${trip.walkingMinutes} min" else "Pie • —") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // TARJETA DE RECORRIDO Y NAVEGACIÓN EN TIEMPO REAL
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Cabecera interactiva del trayecto
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateNavy)
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(EmeraldPrimary, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Origen: ${trip.originName}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.padding(start = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(18.dp)
                                        .background(Color.White.copy(alpha = 0.4f))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Distancia calculada: ${String.format("%.1f", trip.distanceKm)} km • Enlace satelital Google Maps",
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(RosePromo, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Destino: ${trip.destinationName}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Tiempos estimados dinámicos
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🚗 Auto / Remis", fontSize = 11.sp, color = SlateNavy, fontWeight = FontWeight.Bold)
                            Text(text = "${trip.drivingMinutes} min", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldDark)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🚌 Colectivo/Subte", fontSize = 11.sp, color = SlateNavy, fontWeight = FontWeight.Bold)
                            Text(text = "${trip.transitMinutes} min", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = SlateNavy)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🚶 A pie", fontSize = 11.sp, color = SlateNavy, fontWeight = FontWeight.Bold)
                            Text(text = "${trip.walkingMinutes} min", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = SlateNavy)
                        }
                    }

                    // ACCIÓN DIRECTA: Abrir la ruta completa en Google Maps
                    Button(
                        onClick = {
                            GoogleMapsHelper.openRouteInGoogleMaps(context, originInput, destinationInput)
                        },
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Directions, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Abrir Ruta y Tráfico en Google Maps 🗺️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    // SECCIÓN DE PEDIR REMIS BARRIAL
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AmberDealContainer)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tarifa Estimada de Remis Barrial:",
                                fontSize = 11.sp,
                                color = AmberDeal,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$${String.format("%,.0f", trip.estimatedRemisCost)}",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SlateNavy
                            )
                            Text(
                                text = "Calculado según ${String.format("%.1f", trip.distanceKm)} km de recorrido",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }

                        Button(
                            onClick = {
                                GoogleMapsHelper.callOrMessageRemis(context, "+54 11 4782-9900", originInput, destinationInput)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberDeal)
                        ) {
                            Icon(imageVector = Icons.Default.LocalTaxi, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pedir Remis", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Botón de Inicio de Navegación Guiada Turn-by-Turn
        item {
            Button(
                onClick = {
                    isLiveNavigationActive = !isLiveNavigationActive
                    if (isLiveNavigationActive) {
                        GoogleMapsHelper.openNavigationInGoogleMaps(context, destinationInput)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLiveNavigationActive) SlateNavy else EmeraldPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLiveNavigationActive) "Detener Navegación en Vivo" else "Iniciar Navegación Turn-by-Turn con Google Maps 🗺️",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Botón directo para Abrir y Navegar con Google Maps Oficial
        item {
            Button(
                onClick = {
                    GoogleMapsHelper.openRouteInGoogleMaps(context, originInput, destinationInput)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A73E8)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Abrir Ruta Completa en Google Maps 🛰️",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }

        if (isLiveNavigationActive) {
            item {
                val hours = trip.drivingMinutes / 60
                val mins = trip.drivingMinutes % 60
                val etaFormatted = if (hours > 0) "${hours} h ${mins} min" else "${mins} min"
                val isHighway = trip.distanceKm > 25.0

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateNavy),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(EmeraldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.NearMe, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🔊 NAVEGACIÓN ACTIVA • GPS SATELITAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberDeal
                                )
                                Text(
                                    text = "Rumbo hacia ${trip.destinationName} (${String.format("%.1f", trip.distanceKm)} km)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Tiempo Estimado (ETA)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                Text(text = etaFormatted, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Velocidad Promedio", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                Text(text = if (isHighway) "90 km/h (Ruta)" else "30 km/h (Urbano)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                GoogleMapsHelper.openNavigationInGoogleMaps(context, destinationInput)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reanudar Guía de Voz en Google Maps 🚗", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Puntos de Interés Interactivos en el Camino
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Puntos de Interés & Comercios en tu Trayecto",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SlateNavy
                    )
                    Text(
                        text = "Toca para sumar una parada intermedia o desvío a tu viaje:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    trip.landmarksOnRoute.forEach { landmark ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.NearMe, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = landmark, fontSize = 12.sp, color = SlateNavy, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    viewModel.addStopToTrip(landmark)
                                    waypointConfirmationMessage = "¡Parada '$landmark' sumada al viaje! Recalculamos distancias y tarifa de remis."
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberDealContainer),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("+ Parada", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberDeal)
                            }
                        }
                    }
                }
            }
        }

        // Atracciones Turísticas Cercanas al Recorrido (Modo Viajero)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Atracciones Turísticas Cercanas (Modo Viajero)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SlateNavy
                            )
                            Text(
                                text = "Lugares imperdibles a pocos kilómetros de tu ruta:",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    attractions.forEach { attraction ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .padding(bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = EmeraldContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${attraction.category} • ⭐ ${attraction.rating}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "A ${attraction.distanceKm} km",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = attraction.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlateNavy)
                            Text(text = attraction.description, fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp)

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Tiempo estimado: ${attraction.estimatedVisitTime}", fontSize = 10.sp, color = Color.Gray)

                                Button(
                                    onClick = {
                                        viewModel.addStopToTrip(attraction.name, attraction.distanceKm / 2.0)
                                        waypointConfirmationMessage = "¡Atracción '${attraction.name}' sumada al recorrido de tu viaje!"
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("+ Añadir al Recorrido", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Pasos detallados del viaje
        item {
            Text(
                text = "Indicaciones Paso a Paso del Recorrido",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SlateNavy
            )
        }

        items(trip.steps) { step ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(EmeraldContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.NearMe, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(14.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = step.instruction, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SlateNavy)
                        if (step.subtext != null) {
                            Text(text = step.subtext, fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Text(text = step.distanceText, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = EmeraldPrimary)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Modal de confirmación al añadir paradas
    if (waypointConfirmationMessage != null) {
        AlertDialog(
            onDismissRequest = { waypointConfirmationMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.NearMe, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recorrido Actualizado")
                }
            },
            text = { Text(waypointConfirmationMessage ?: "") },
            confirmButton = {
                Button(
                    onClick = { waypointConfirmationMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Genial")
                }
            }
        )
    }
}
