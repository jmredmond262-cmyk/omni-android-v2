package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.Screen

private val PurchasesBackground = Color(0xFF070A14)
private val PurchasesCard = Color(0xFF101B30)
private val PurchasesCyan = Color(0xFF57DDF5)

@Composable
fun MyPurchasesScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val needs by viewModel.needs.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val city by viewModel.currentCity.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PurchasesBackground),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("CENTRO DE COMPRAS", color = PurchasesCyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                    Text("Mis compras", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = PurchasesCyan, modifier = Modifier.size(25.dp))
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PurchasesCard)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Todo tu recorrido de compra, en un solo lugar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Prepará listas, compará precios, elegí un comercio y consultá tus pedidos. Las compras de prueba se identificarán claramente como DEMO.", color = Color(0xFFB8C8D9), fontSize = 11.sp, lineHeight = 16.sp)
                    Text(needs.size.toString() + " artículos en tus listas · " + stores.size.toString() + " comercios disponibles en " + city.ifBlank { "tu zona" }, color = PurchasesCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            PurchaseActionCard(
                icon = { Icon(Icons.Default.ListAlt, contentDescription = null, tint = PurchasesCyan) },
                title = "Mis listas",
                description = "Organizá productos, cantidades y necesidades.",
                action = "ABRIR LISTAS",
                onClick = { viewModel.navigateTo(Screen.SmartList) }
            )
        }
        item {
            PurchaseActionCard(
                icon = { Icon(Icons.Default.CompareArrows, contentDescription = null, tint = PurchasesCyan) },
                title = "Comparar precios",
                description = "Compará productos y el costo final entre comercios con información disponible.",
                action = "COMPARAR",
                onClick = { viewModel.navigateTo(Screen.Comparison) }
            )
        }
        item {
            PurchaseActionCard(
                icon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = PurchasesCyan) },
                title = "Explorar comercios",
                description = "Buscá locales y consultá sus catálogos publicados.",
                action = "BUSCAR LOCALES",
                onClick = {
                    viewModel.clearStoreTypeFilter()
                    viewModel.setScheduleFilter(com.example.ui.StoreScheduleFilter.TODOS)
                    viewModel.navigateTo(Screen.StoresList)
                }
            )
        }
        item {
            PurchaseActionCard(
                icon = { Icon(Icons.Default.QrCode2, contentDescription = null, tint = PurchasesCyan) },
                title = "Pedidos y seguimiento",
                description = "Consultá pedidos QR, estados, retiro o envío a domicilio.",
                action = "VER PEDIDOS",
                onClick = { viewModel.navigateTo(Screen.OrderCenter) }
            )
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0C1829)
            ) {
                Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFFFFC978), modifier = Modifier.size(22.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Envío o retiro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("La modalidad se selecciona al confirmar cada pedido; los costos de envío se informan antes de finalizar.", color = Color(0xFFB8C8D9), fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseActionCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    action: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = PurchasesCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, PurchasesCyan.copy(alpha = 0.24f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = RoundedCornerShape(11.dp), color = Color(0xFF0B2A3A), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(description, color = Color(0xFFB8C8D9), fontSize = 10.sp, lineHeight = 14.sp)
            }
            Text(action, color = PurchasesCyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
