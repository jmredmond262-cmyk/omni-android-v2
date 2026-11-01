package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.NeedItem
import com.example.model.ProductCategory
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartListScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val needs by viewModel.needs.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showAirShareDialog by remember { mutableStateOf(false) }
    var showVoiceInputDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf(ProductCategory.ALMACEN) }
    var newQty by remember { mutableIntStateOf(1) }
    var newUnit by remember { mutableStateOf("unid.") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val filteredNeeds = if (selectedCategory == null) {
        needs
    } else {
        needs.filter { it.category == selectedCategory }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 88.dp)
                .testTag("smart_list_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cabecera explicativa con acciones AirShare y Dictado por voz
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(EmeraldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lista Inteligente de Compras",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Agrega lo que necesitas y busca el mejor ahorro local",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showAirShareDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AirShare QR", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showVoiceInputDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Dictar por Voz", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Barra de filtros por categorías
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.setCategoryFilter(null) },
                            label = { Text("Todas (${needs.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(ProductCategory.values()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                viewModel.setCategoryFilter(if (selectedCategory == cat) null else cat)
                            },
                            label = { Text(cat.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Botones de Acción Rápida: Agregar manual, Canasta Básica y Comparador
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldContainer),
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("btn_open_add_product")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = EmeraldDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Nuevo Artículo",
                                color = EmeraldDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.loadFamilyBasket() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavy),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "🧺 Canasta Típica",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Botón directo a Comparar Precios Reales
                    Button(
                        onClick = { viewModel.navigateTo(Screen.Comparison) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Comparar Precios en Comercios (${needs.size} productos) 💰",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Catálogo Rápido con 24 productos esenciales para agregar con 1 tap
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛒 Catálogo Rápido (+1 Tap)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateNavy
                        )
                        Text(
                            text = "Toca (+) para sumar",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(com.example.data.SampleData.predefinedCatalog) { catProduct ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable { viewModel.addCatalogProductToNeeds(catProduct) }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = catProduct.iconEmoji, fontSize = 20.sp)
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(EmeraldContainer, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Agregar",
                                                tint = EmeraldDark,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = catProduct.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateNavy,
                                        maxLines = 2,
                                        minLines = 2,
                                        lineHeight = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$${String.format("%,.0f", catProduct.basePrice)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sugerencias Inteligentes en base a la lista
            if (suggestions.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = com.example.ui.theme.AmberDeal, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Sugerencias Inteligentes para tu Canasta",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateNavy
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(suggestions, key = { it.id }) { sug ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.width(220.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Surface(
                                            color = com.example.ui.theme.AmberDealContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = sug.discountBadge,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = com.example.ui.theme.AmberDeal,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = sug.productName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SlateNavy,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = sug.reasonText,
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 2,
                                            lineHeight = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$${String.format("%,.0f", sug.price)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = EmeraldPrimary
                                            )
                                            Button(
                                                onClick = { viewModel.addSuggestionToNeeds(sug) },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldContainer),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("+ Agregar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Lista de productos
            if (filteredNeeds.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No hay productos en esta categoría",
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredNeeds, key = { it.id }) { item ->
                    NeedItemCard(
                        item = item,
                        onToggle = { viewModel.toggleNeed(item.id) },
                        onDelete = { viewModel.deleteNeed(item.id) }
                    )
                }
            }
        }

        // Barra inferior fija con CTA hacia el comparador
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
                        text = "${needs.count { !it.isChecked }} pendientes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SlateNavy
                    )
                    Text(
                        text = "5 comercios listos",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { viewModel.navigateTo(Screen.Comparison) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("btn_go_to_comparison")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Comparar en mi zona",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Modal para agregar producto
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = "Nuevo Producto para la Lista") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre del producto") },
                        placeholder = { Text("Ej: Queso cremoso 500g") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_product_name")
                    )

                    // Selector de categoría
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = newCategory.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            ProductCategory.values().forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName) },
                                    onClick = {
                                        newCategory = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Selector de cantidad y unidad
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = { if (newQty > 1) newQty-- }) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Menos")
                            }
                            Text(
                                text = "$newQty",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = { newQty++ }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Más")
                            }
                        }

                        OutlinedTextField(
                            value = newUnit,
                            onValueChange = { newUnit = it },
                            label = { Text("Unidad") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addNeed(newName, newCategory, newQty, newUnit)
                            newName = ""
                            newQty = 1
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de AirShare Offline QR & Compartir Multicanal
    if (showAirShareDialog) {
        AlertDialog(
            onDismissRequest = { showAirShareDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AirShare • Compartir Offline", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Si no tienes datos móviles, otro usuario de OmniHub puede escanear este QR desde su app para clonar tus ${needs.size} productos al instante sin internet.",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )

                    // Matriz QR de Alta Visibilidad
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.size(170.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Código QR AirShare",
                                tint = SlateNavy,
                                modifier = Modifier.size(90.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "OMNI-LIST-AIRSHARE-SYNC",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateNavy
                            )
                            Text(
                                text = "${needs.size} artículos codificados",
                                fontSize = 9.sp,
                                color = EmeraldDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(text = "O comparte por tus redes habituales:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateNavy)

                    Button(
                        onClick = {
                            val listText = "Lista de Compras OmniHub:\n" + needs.joinToString("\n") { "• ${it.quantity} ${it.unit} ${it.name}" }
                            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Mi Lista de Compras OmniHub")
                                putExtra(android.content.Intent.EXTRA_TEXT, listText)
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, "Enviar lista por:"))
                            showAirShareDialog = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enviar por WhatsApp / Redes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAirShareDialog = false }) { Text("Cerrar") }
            }
        )
    }

    // Modal de Dictado por Voz
    if (showVoiceInputDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceInputDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dictado de Lista por Voz")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "🎙️ Escuchando... Di en voz alta lo que necesitas sumar o toca un dictado rápido de ejemplo:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Micrófono activo • Reconociendo audio en español...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldDark
                            )
                        }
                    }

                    Text(text = "Comandos de voz sugeridos:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateNavy)

                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.addNeed("Yerba Mate Especial 1kg", ProductCategory.ALMACEN, 1, "kg")
                                viewModel.addNeed("Café Tostado Molido", ProductCategory.ALMACEN, 1, "paq.")
                                showVoiceInputDialog = false
                            }
                    ) {
                        Text(
                            text = "🗣️ \"Sumar yerba mate y café molido\"",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp),
                            color = SlateNavy,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.addNeed("Queso Cremoso Primera Marca", ProductCategory.LACTEOS, 1, "kg")
                                viewModel.addNeed("Pan de Campo Rústico", ProductCategory.ALMACEN, 1, "unid.")
                                showVoiceInputDialog = false
                            }
                    ) {
                        Text(
                            text = "🗣️ \"Sumar queso cremoso y pan de campo\"",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp),
                            color = SlateNavy,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showVoiceInputDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun NeedItemCard(
    item: NeedItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("need_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (item.isChecked) "Comprado" else "Pendiente",
                    tint = if (item.isChecked) EmeraldPrimary else Color.LightGray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isChecked) Color.Gray else SlateNavy
                )
                Text(
                    text = "${item.quantity} ${item.unit} • ${item.category.displayName}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
