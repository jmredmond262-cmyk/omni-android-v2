package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.model.StoreComparisonResult
import com.example.ui.MainViewModel
import com.example.ui.components.DistancePill
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.RatingBadge
import com.example.ui.components.StoreTypeChip
import com.example.ui.theme.AmberDeal
import com.example.ui.theme.AmberDealContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RosePromo
import com.example.ui.theme.SlateNavy

enum class ComparisonSort(val label: String) {
    BEST_VALUE("Mejor Ahorro"),
    NEAREST("Más Cercano"),
    MOST_PRODUCTS("Mayor Cobertura")
}

@Composable
fun ComparisonScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val needs by viewModel.needs.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val priceSeed by viewModel.priceSimulationSeed.collectAsState()

    var activeSort by remember { mutableStateOf(ComparisonSort.BEST_VALUE) }

    val rawComparisons = remember(needs, stores, priceSeed) {
        SampleData.calculateComparison(stores, needs, priceSeed)
    }

    val sortedComparisons = remember(rawComparisons, activeSort) {
        when (activeSort) {
            ComparisonSort.BEST_VALUE -> rawComparisons.sortedByDescending { it.estimatedSavings }
            ComparisonSort.NEAREST -> rawComparisons.sortedBy { it.store.distanceMeters }
            ComparisonSort.MOST_PRODUCTS -> rawComparisons.sortedByDescending { it.availableCount }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("comparison_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera Resumen de Canasta
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Comparador Inteligente de Canasta",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Analizamos tu lista de ${needs.size} productos en ${stores.size} locales de tu radio habitual.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón para simular variaciones de ofertas y precios
                    Button(
                        onClick = { viewModel.randomizeComparisonOffers() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🎲 Simular Nuevas Ofertas / Descuentos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chips para cargar canastas de prueba variadas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.loadSampleBasket(1) }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                Text("🧺 Canasta Familiar (8)", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.loadSampleBasket(2) }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                Text("🍎 Canasta Saludable (5)", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val topSaver = rawComparisons.maxByOrNull { it.estimatedSavings }
                    if (topSaver != null && topSaver.estimatedSavings > 0) {
                        Surface(
                            color = EmeraldPrimary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Ahorro Máximo Detectado: $${String.format("%,.0f", topSaver.estimatedSavings)}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Comprando en ${topSaver.store.name}",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Barra de ordenamiento
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ordenar por:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateNavy.copy(alpha = 0.7f)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ComparisonSort.values()) { sort ->
                        FilterChip(
                            selected = activeSort == sort,
                            onClick = { activeSort = sort },
                            label = { Text(sort.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Resultados por tienda
        items(sortedComparisons, key = { it.store.id }) { result ->
            StoreComparisonCard(
                result = result,
                onViewDetails = { viewModel.selectStoreAndOpenDetail(result.store) },
                onViewRoute = { viewModel.selectStoreAndOpenRoute(result.store) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun StoreComparisonCard(
    result: StoreComparisonResult,
    onViewDetails: () -> Unit,
    onViewRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("comparison_card_${result.store.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Badge superior opcional
            if (result.highlightBadge != null) {
                Surface(
                    color = AmberDealContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Text(
                        text = result.highlightBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberDeal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Encabezado del comercio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StoreTypeChip(type = result.store.type)
                        Spacer(modifier = Modifier.width(6.dp))
                        RatingBadge(rating = result.store.rating, reviewsCount = result.store.reviewsCount)
                        if (result.store.isEffectivelyOpen) {
                            Spacer(modifier = Modifier.width(6.dp))
                            PulsingStatusDot(color = Color(0xFF10B981), sizeDp = 7.dp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.store.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateNavy
                    )
                    Text(
                        text = result.store.hoursText,
                        fontSize = 11.sp,
                        color = if (result.store.isEffectivelyOpen) EmeraldDark else Color.Gray,
                        fontWeight = if (result.store.isEffectivelyOpen) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                DistancePill(
                    distanceMeters = result.store.distanceMeters,
                    walkingMinutes = result.store.walkingTimeMin
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cobertura de productos y Total
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Disponibilidad:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${result.availableCount} de ${result.totalRequestedCount} productos (${result.coveragePercent}%)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (result.coveragePercent == 100) EmeraldDark else SlateNavy
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Estimado",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "$${String.format("%,.0f", result.finalTotal)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = EmeraldPrimary
                    )
                    if (result.estimatedSavings > 0) {
                        Text(
                            text = "Ahorras $${String.format("%,.0f", result.estimatedSavings)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RosePromo
                        )
                    }
                }
            }

            // Descuento por monto alcanzado
            if (result.thresholdDiscountApplied > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = EmeraldContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = EmeraldDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "¡Alcanzaste el descuento de ${result.store.minSpendDiscountPercent}% por superar $${String.format("%,.0f", result.store.minSpendDiscountThreshold)}!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldDark
                        )
                    }
                }
            }

            // Toggle para ver desglose de productos
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Ocultar desglose de precios" else "Ver desglose producto por producto",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldPrimary
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Desglose expandible
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Divider(color = Color(0xFFE2E8F0))
                    result.matchedItems.forEach { matched ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (matched.isAvailable) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (matched.isAvailable) EmeraldPrimary else Color.Red,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = matched.need.name,
                                    fontSize = 12.sp,
                                    color = if (matched.isAvailable) SlateNavy else Color.Gray
                                )
                            }

                            if (matched.isAvailable && matched.matchedProduct != null) {
                                Text(
                                    text = "$${String.format("%,.0f", matched.matchedProduct.price * matched.need.quantity)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateNavy
                                )
                            } else {
                                Text(
                                    text = "Sin stock",
                                    fontSize = 11.sp,
                                    color = Color.Red
                                )
                            }
                        }
                    }
                    Divider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(top = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val cardContext = androidx.compose.ui.platform.LocalContext.current

            // Botones de acción: Ver Local y Navegar con Google Maps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Ver Local", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        com.example.ui.util.GoogleMapsHelper.openNavigationInGoogleMaps(cardContext, "${result.store.name} ${result.store.address}")
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(imageVector = Icons.Default.Directions, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Google Maps 🗺️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
