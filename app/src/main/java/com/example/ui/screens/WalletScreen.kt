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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PaymentCard
import com.example.model.TransactionType
import com.example.model.WalletTransaction
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberDeal
import com.example.ui.theme.AmberDealContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RosePromo
import com.example.ui.theme.CyberGlowCyan
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.SlateNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val wallet by viewModel.wallet.collectAsState()
    val stores by viewModel.stores.collectAsState()

    var showPayQrDialog by remember { mutableStateOf(false) }
    var showAddFundsDialog by remember { mutableStateOf(false) }
    var payAmountText by remember { mutableStateOf("3500") }
    var selectedStoreIndex by remember { mutableStateOf(0) }
    var storeDropdownExpanded by remember { mutableStateOf(false) }
    var paymentSuccessMessage by remember { mutableStateOf<String?>(null) }

    val receipts by viewModel.receipts.collectAsState()
    var selectedWalletTab by remember { mutableStateOf(0) } // 0: Movimientos, 1: Comprobantes & Tickets
    var selectedReceiptForModal by remember { mutableStateOf<com.example.model.DigitalReceipt?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("wallet_screen"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tarjeta Principal de Saldo Billetera
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF081A35)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OMNI Pay Virtual • DEMO",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            color = CyberGlowCyan,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "5% Cashback Activo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Saldo virtual de prueba",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "OM$ ${String.format("%,.2f", wallet.balance)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Crédito ficticio para probar flujos. No es dinero real ni permite retiros.",
                        fontSize = 10.sp,
                        color = Color(0xFF9CC7DF),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = null,
                                tint = AmberDeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reintegros virtuales acumulados: OM$ ${String.format("%,.2f", wallet.totalCashbackEarned)}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Acciones rápidas de la billetera
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showPayQrDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGlowCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_pay_qr")
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Pagar QR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { showAddFundsDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Cargar demo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Selector de Pestañas: Movimientos vs Comprobantes Fiscales
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedWalletTab == 0,
                    onClick = { selectedWalletTab = 0 },
                    label = { Text("Movimientos & Tarjetas") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedWalletTab == 1,
                    onClick = { selectedWalletTab = 1 },
                    label = { Text("Comprobantes (${receipts.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (selectedWalletTab == 0) {
            // Tarjetas Vinculadas
            item {
                Column {
                    Text(
                        text = "Tus Medios de Pago Vinculados",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateNavy
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(wallet.cards) { card ->
                            PaymentCardItem(card = card)
                        }
                    }
                }
            }

            // Historial de Movimientos y Pagos
            item {
                Text(
                    text = "Últimos Movimientos y Reintegros",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateNavy
                )
            }

            items(wallet.transactions, key = { it.id }) { tx ->
                WalletTransactionCard(transaction = tx)
            }
        } else {
            // Pestaña de Comprobantes Digitales y Facturas
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Historial de Comprobantes & Tickets",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateNavy
                        )
                        Text(
                            text = "Validados con QR fiscal para garantías y reintegros",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            items(receipts, key = { it.id }) { receipt ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedReceiptForModal = receipt }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = receipt.storeName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SlateNavy
                            )
                            Text(
                                text = "$${String.format("%,.2f", receipt.total)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color(0xFF9CEEFF)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${receipt.receiptNumber} • ${receipt.dateText}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            if (receipt.discountApplied > 0) {
                                Surface(
                                    color = EmeraldContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Ahorraste $${String.format("%,.0f", receipt.discountApplied)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                            Text(
                                text = "${receipt.items.size} artículos • ${receipt.paymentMethod}",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )

                            Button(
                                onClick = { selectedReceiptForModal = receipt },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06364A)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Ver Ticket QR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Modal de Pago con QR en Comercio Barrial
    if (showPayQrDialog) {
        val currentStore = stores.getOrNull(selectedStoreIndex) ?: stores.first()
        AlertDialog(
            onDismissRequest = { showPayQrDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pagar con QR en el Local")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Selecciona el comercio y el monto. ¡Recibes 5% de reintegro en el acto en tu saldo!",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    // Selector de Comercio
                    ExposedDropdownMenuBox(
                        expanded = storeDropdownExpanded,
                        onExpandedChange = { storeDropdownExpanded = !storeDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentStore.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Comercio a pagar") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = storeDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = storeDropdownExpanded,
                            onDismissRequest = { storeDropdownExpanded = false }
                        ) {
                            stores.forEachIndexed { index, store ->
                                DropdownMenuItem(
                                    text = { Text(store.name) },
                                    onClick = {
                                        selectedStoreIndex = index
                                        storeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = payAmountText,
                        onValueChange = { payAmountText = it },
                        label = { Text("Monto a pagar ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    val amountToPay = payAmountText.toDoubleOrNull() ?: 0.0
                    if (amountToPay > 0) {
                        Surface(
                            color = EmeraldContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.LocalOffer, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reintegro estimado: +$${String.format("%,.2f", amountToPay * 0.05)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = EmeraldDark
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payAmountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            val success = viewModel.payWithWallet(currentStore.name, amount)
                            showPayQrDialog = false
                            if (success) {
                                paymentSuccessMessage = "¡Pago de prueba exitoso por OM$ ${String.format("%,.2f", amount)} en ${currentStore.name}! Reintegro virtual: OM$ ${String.format("%,.2f", amount * 0.05)}."
                            } else {
                                paymentSuccessMessage = "Saldo insuficiente en tu billetera. Por favor carga saldo antes de pagar."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Confirmar Pago QR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayQrDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Cargar Saldo
    if (showAddFundsDialog) {
        var addAmount by remember { mutableStateOf("10000") }
        AlertDialog(
            onDismissRequest = { showAddFundsDialog = false },
            title = { Text("Agregar saldo virtual de prueba") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ingresa el monto a transferir desde tu cuenta bancaria o tarjeta:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = addAmount,
                        onValueChange = { addAmount = it },
                        label = { Text("Monto ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = addAmount.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.addFunds(amount)
                            showAddFundsDialog = false
                            paymentSuccessMessage = "¡Carga virtual de prueba por OM$ ${String.format("%,.2f", amount)}!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Cargar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFundsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de feedback de pago
    if (paymentSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = { paymentSuccessMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("OmniPay Notificación")
                }
            },
            text = { Text(paymentSuccessMessage ?: "") },
            confirmButton = {
                Button(onClick = { paymentSuccessMessage = null }) {
                    Text("Entendido")
                }
            }
        )
    }

    // Modal de Comprobante Fiscal & Ticket Digital
    selectedReceiptForModal?.let { receipt ->
        AlertDialog(
            onDismissRequest = { selectedReceiptForModal = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = EmeraldDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Comprobante Fiscal Digital", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberDarkBg, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cabecera del comercio
                    Text(
                        text = receipt.storeName.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = SlateNavy
                    )
                    Text(
                        text = "CUIT: 30-71239845-6 • Responsable Inscripto",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Ticket: ${receipt.receiptNumber} • ${receipt.dateText}",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    ) {}
                    Spacer(modifier = Modifier.height(4.dp))

                    // Items desglosados
                    receipt.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.name}",
                                fontSize = 11.sp,
                                color = SlateNavy,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$${String.format("%,.2f", item.subtotal)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateNavy
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    ) {}
                    Spacer(modifier = Modifier.height(4.dp))

                    // Totales y Descuentos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Subtotal:", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "$${String.format("%,.2f", receipt.subtotal)}", fontSize = 12.sp, color = SlateNavy)
                    }

                    if (receipt.discountApplied > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Descuento / Reintegro:", fontSize = 12.sp, color = EmeraldDark, fontWeight = FontWeight.Bold)
                            Text(text = "-$${String.format("%,.2f", receipt.discountApplied)}", fontSize = 12.sp, color = EmeraldDark, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "TOTAL FACTURADO:", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = SlateNavy)
                        Text(text = "$${String.format("%,.2f", receipt.total)}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldDark)
                    }

                    Text(
                        text = "Medio: ${receipt.paymentMethod}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // QR de Validación Fiscal
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Código QR",
                                tint = SlateNavy,
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "Código Fiscal: ${receipt.qrVerificationCode}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateNavy
                            )
                            Text(
                                text = "Válido ante el comercio y AFIP para garantías",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Comprobante ${receipt.receiptNumber} - ${receipt.storeName}")
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                "Comprobante Fiscal OmniHub:\nComercio: ${receipt.storeName}\nNro: ${receipt.receiptNumber}\nFecha: ${receipt.dateText}\nTotal: $${String.format("%,.2f", receipt.total)}\nValidación: ${receipt.qrVerificationCode}"
                            )
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir Comprobante por:"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compartir")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedReceiptForModal = null }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun PaymentCardItem(card: PaymentCard, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.width(200.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = EmeraldPrimary)
                if (card.isDefault) {
                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Principal",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "•••• •••• •••• ${card.lastFour}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SlateNavy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${card.bankName} • ${card.cardBrand}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun WalletTransactionCard(transaction: WalletTransaction, modifier: Modifier = Modifier) {
    val isIncome = transaction.type == TransactionType.CARGA_SALDO || transaction.type == TransactionType.REINTEGRO_CASHBACK

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
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
                    .background(
                        if (isIncome) EmeraldContainer else RosePromo.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isIncome) EmeraldDark else RosePromo,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = SlateNavy
                )
                Text(
                    text = "${transaction.storeOrEntityName} • ${transaction.dateText}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (transaction.amount > 0) "+" else ""}OM$ ${String.format("%,.2f", transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isIncome) EmeraldDark else SlateNavy
                )
                if (transaction.cashbackEarned > 0) {
                    Text(
                        text = "+OM$ ${String.format("%,.2f", transaction.cashbackEarned)} virtuales",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberDeal
                    )
                }
            }
        }
    }
}
