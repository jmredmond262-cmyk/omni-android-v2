package com.example.ui.screens

import androidx.activity.compose.BackHandler
import android.content.ClipData
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Phone
import coil.compose.AsyncImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Store
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.DistancePill
import com.example.ui.components.OpenStatusBadge
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.RatingBadge
import com.example.ui.components.StoreTypeChip
import com.example.ui.components.getStoreGradient
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
fun StoreDetailScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = viewModel.selectedStore.collectAsState().value ?: return

    var showVideoTourModal by remember { mutableStateOf(false) }
    var addedMessage by remember { mutableStateOf<String?>(null) }

    BackHandler {
        viewModel.navigateTo(Screen.StoresList)
    }

    Box(modifier = modifier.fillMaxSize().testTag("store_detail_screen")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera Inmersiva (Estilo Booking con Fotos Reales)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(getStoreGradient(store.type))
                ) {
                    if (store.photoUrl != null) {
                        AsyncImage(
                            model = store.photoUrl,
                            contentDescription = store.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.5f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.75f)
                                        )
                                    )
                                )
                        )
                    }
                    // Botón volver
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.StoresList) },
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            .size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }

                    // Contador de fotos
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "📷 1 de ${store.photosCount} fotos",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Botón para reproducir video tour
                    if (store.hasVideoTour) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clickable { showVideoTourModal = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleFilled,
                                    contentDescription = "Reproducir tour",
                                    tint = RosePromo,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ver Video Tour (${store.videoTourDuration})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Badges inferiores sobre la cabecera
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StoreTypeChip(type = store.type)
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Comercio Verificado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                            }
                        }
                    }
                }
            }

            // Datos del Local
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = store.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateNavy
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RatingBadge(rating = store.rating, reviewsCount = store.reviewsCount)
                        DistancePill(distanceMeters = store.distanceMeters, walkingMinutes = store.walkingTimeMin)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horario y Guardia destacada con pelotita verde parpadeando
                    Surface(
                        color = if (store.isEffectivelyOpen) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OpenStatusBadge(
                                    isOpen = store.isOpen,
                                    is24Hours = store.is24Hours,
                                    isOnDuty = store.isOnDuty,
                                    openOnHolidays = store.openOnHolidays
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (store.isEffectivelyOpen) "Atendiendo en este momento" else "Cerrado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (store.isEffectivelyOpen) EmeraldDark else Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Lunes a Viernes: ${store.openTime} a ${store.closeTime} hs", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SlateNavy)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = SlateNavy.copy(alpha = 0.6f), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Fines de Semana: ${store.weekendHoursText}", fontSize = 11.sp, color = Color.Gray)
                            }

                            if (store.scheduleNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "🔔 ${store.scheduleNotes}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SlateNavy
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = store.address, fontSize = 12.sp, color = SlateNavy)
                        }
                        Button(
                            onClick = { GoogleMapsHelper.openSearchInGoogleMaps(context, "${store.name}, ${store.address}") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Maps 📍", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (store.phone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Tel: ${store.phone}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SlateNavy)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = store.description,
                        fontSize = 13.sp,
                        color = SlateNavy.copy(alpha = 0.85f),
                        lineHeight = 19.sp
                    )
                }
            }

            // Planificar viaje desde este local
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
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
                                .background(AmberDealContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = AmberDeal, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "¿Vas hacia otro lugar desde aquí?", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlateNavy)
                            Text(text = "Traza tu viaje hacia El Obelisco, Retiro o tu destino", fontSize = 11.sp, color = Color.Gray)
                        }
                        Button(
                            onClick = { viewModel.selectStoreAndPlanTripFrom(store) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Viaje 🚗", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Beneficios y Descuentos por Monto
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.LocalOffer, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Beneficio por Monto de Compra",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = EmeraldDark
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = store.specialOfferText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateNavy
                        )
                        Text(
                            text = "Se aplica automáticamente cuando el total supera los $${String.format("%,.0f", store.minSpendDiscountThreshold)}.",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Combos del Comercio
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Combos & Promociones del Local",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateNavy
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    store.comboDeals.forEach { combo ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = combo.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SlateNavy
                                    )
                                    Text(
                                        text = combo.description,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Surface(
                                    color = AmberDealContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = combo.discountBadge,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = AmberDeal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Inventario del Comercio
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Catálogo y Precios en Local",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateNavy
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(store.inventory, key = { it.id }) { product ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!product.photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = product.photoUri,
                                contentDescription = product.name,
                                modifier = Modifier.size(68.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = SlateNavy
                            )
                            if (product.description.isNotBlank()) {
                                Text(
                                    text = product.description,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 3
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${String.format("%,.0f", product.price)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = EmeraldPrimary
                                )
                                if (product.originalPrice != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${String.format("%,.0f", product.originalPrice)}",
                                        fontSize = 11.sp,
                                        textDecoration = TextDecoration.LineThrough,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    val message = buildString {
                                        appendLine("🛍️ ${product.name}")
                                        if (product.description.isNotBlank()) appendLine(product.description)
                                        appendLine("Precio: ${String.format("%,.0f", product.price)}")
                                        product.originalPrice?.let { appendLine("Precio anterior: ${String.format("%,.0f", it)}") }
                                        appendLine("Comercio: ${store.name}")
                                        appendLine("Dirección: ${store.address}")
                                        append("Compartido desde OMNI")
                                    }
                                    val imageUri = product.photoUri?.let { Uri.parse(it) }
                                    val canAttachImage = imageUri != null &&
                                        imageUri.scheme in listOf("content", "file", "android.resource")
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_SUBJECT, product.name)
                                        putExtra(Intent.EXTRA_TEXT, message)
                                        if (canAttachImage) {
                                            type = "image/*"
                                            putExtra(Intent.EXTRA_STREAM, imageUri)
                                            clipData = ClipData.newUri(context.contentResolver, product.name, imageUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        } else {
                                            type = "text/plain"
                                        }
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir producto"))
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir producto", tint = EmeraldPrimary)
                            }
                            Button(
                                onClick = {
                                    viewModel.addNeed(product.name, product.category, 1, product.unitLabel)
                                    addedMessage = "¡${product.name} agregado a tu lista!"
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldContainer),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "A mi lista", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                            }
                        }
                    }
                }
            }
        }

        // Barra inferior fija con CTA a la Ruta Interactiva
        Surface(
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "A ${store.distanceText} de ti",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SlateNavy
                    )
                    Text(
                        text = "${store.walkingTimeMin} min caminando",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { viewModel.selectStoreAndOpenRoute(store) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("btn_navigate_route")
                ) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Trazar Camino 📍", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }

    // Modal de Video Tour Simulado (Booking style)
    if (showVideoTourModal) {
        AlertDialog(
            onDismissRequest = { showVideoTourModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PlayCircleFilled, contentDescription = null, tint = RosePromo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Video Tour: ${store.name}")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(SlateNavy, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Reproduciendo recorrido de 360°",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Sector góndolas, carnicería y caja rápida",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    Text(
                        text = "Los comerciantes graban tours cortos para que conozcas la frescura, el orden y las promociones vigentes antes de ir.",
                        fontSize = 12.sp,
                        color = SlateNavy
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showVideoTourModal = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Diálogo feedback al agregar a la lista
    if (addedMessage != null) {
        AlertDialog(
            onDismissRequest = { addedMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Producto Agregado")
                }
            },
            text = { Text(addedMessage ?: "") },
            confirmButton = {
                Button(onClick = { addedMessage = null }) {
                    Text("Aceptar")
                }
            }
        )
    }
}
