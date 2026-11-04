package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.wallet.DemoWalletRepository
import com.example.data.analytics.OmniTracker
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestore
import com.example.model.PaymentCard
import com.example.model.TransactionType
import com.example.model.WalletTransaction
import com.example.ui.MainViewModel
import com.example.ui.screens.OrderQRScanner
import com.example.ui.screens.OrderQRPayload
import com.example.ui.screens.OrderQRItem
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

private data class PendingOrderQRPayment(
    val payload: OrderQRPayload,
    val merchantUid: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val wallet by viewModel.wallet.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val walletRepository = remember { DemoWalletRepository() }
    val orderDb = remember { FirebaseFirestore.getInstance() }
    var pendingOrderQr by remember { mutableStateOf<PendingOrderQRPayment?>(null) }
    var orderPaymentInProgress by remember { mutableStateOf(false) }
    var showMyQrDialog by remember { mutableStateOf(false) }
    var showQrPaymentDialog by remember { mutableStateOf(false) }
    var showAddFundsDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var transferRecipient by remember { mutableStateOf("") }
    var transferAmount by remember { mutableStateOf("1500") }
    var qrPaymentPayload by remember { mutableStateOf("") }
    var qrPaymentAmount by remember { mutableStateOf("1500") }
    var paymentSuccessMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(currentUid) {
        var disposed = false
        var registrations: List<ListenerRegistration> = emptyList()
        if (currentUid.isNotBlank()) {
            walletRepository.ensureWallet { accountResult ->
                if (!disposed) {
                    accountResult.fold(
                        onSuccess = { account ->
                            viewModel.applyRemoteDemoWallet(
                                account.balance, 0.0, emptyList(), account.alias, account.cvu
                            )
                            registrations = walletRepository.observeWallet(currentUid) { remoteResult ->
                                if (!disposed) remoteResult.fold(
                                    onSuccess = { snapshot ->
                                        viewModel.applyRemoteDemoWallet(
                                            snapshot.balance, snapshot.cashbackTotal,
                                            snapshot.transactions, snapshot.alias, snapshot.cvu
                                        )
                                    },
                                    onFailure = { error ->
                                        paymentSuccessMessage = "No se pudo sincronizar OMNI Pay: ${error.localizedMessage ?: "error de conexión"}"
                                    }
                                )
                            }
                            if (disposed) registrations.forEach { it.remove() }
                        },
                        onFailure = { error ->
                            paymentSuccessMessage = "No se pudo inicializar tu cuenta DEMO: ${error.localizedMessage ?: "revisá la conexión"}"
                        }
                    )
                }
            }
        }
        onDispose {
            disposed = true
            registrations.forEach { it.remove() }
        }
    }

    val myQrPayload = remember(currentUid, profile.walletAlias) {
        if (currentUid.isNotBlank() && profile.walletAlias.isNotBlank())
            DemoWalletRepository.qrPayload(currentUid, profile.walletAlias) else ""
    }
    val myQrBitmap = remember(myQrPayload) {
        if (myQrPayload.isBlank()) null else runCatching {
            val matrix = QRCodeWriter().encode(myQrPayload, BarcodeFormat.QR_CODE, 560, 560)
            android.graphics.Bitmap.createBitmap(560, 560, android.graphics.Bitmap.Config.RGB_565).also { bitmap ->
                for (x in 0 until 560) for (y in 0 until 560) {
                    bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
        }.getOrNull()
    }

    val receipts by viewModel.receipts.collectAsState()
    var selectedWalletTab by remember { mutableStateOf(0) } // 0: Movimientos, 1: Comprobantes & Tickets
    var selectedReceiptForModal by remember { mutableStateOf<com.example.model.DigitalReceipt?>(null) }

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
                                text = "MODO PRUEBA",
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
                        OrderQRScanner(
                            onOrderScanned = { payload ->
                                OmniTracker.track("orders", "ORDER_QR_PAYMENT_SCAN")
                                orderDb.collection("storeOrders").document(payload.orderId).get()
                                    .addOnSuccessListener { doc ->
                                        val storedItems = doc.get("items") as? List<*>
                                        val itemsMatch = storedItems != null &&
                                            storedItems.size == payload.items.size &&
                                            payload.items.all { qrItem ->
                                                storedItems.any { raw ->
                                                    val item = raw as? Map<*, *> ?: return@any false
                                                    (item["productId"] as? String).orEmpty() == qrItem.productId &&
                                                        (item["name"] as? String).orEmpty() == qrItem.name &&
                                                        kotlin.math.abs(((item["price"] as? Number)?.toDouble() ?: -1.0) - qrItem.price) < 0.005 &&
                                                        ((item["quantity"] as? Number)?.toInt() ?: 0) == qrItem.quantity
                                                }
                                            }
                                        val total = (doc.get("total") as? Number)?.toDouble() ?: 0.0
                                        val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                                        val expired = createdAt == 0L || System.currentTimeMillis() - createdAt > 24L * 60L * 60L * 1000L
                                        val merchantUid = doc.getString("merchantUid").orEmpty()
                                        val valid = doc.exists() &&
                                            doc.getString("customerUid") == currentUid &&
                                            doc.getString("storeName") == payload.merchantName &&
                                            doc.getString("qrNonce") == payload.qrNonce &&
                                            kotlin.math.abs(total - payload.totalAmount) < 0.005 &&
                                            itemsMatch && merchantUid.isNotBlank() &&
                                            doc.getString("paymentStatus") == "PENDIENTE" &&
                                            doc.getString("status") !in listOf("ENTREGADO", "RECHAZADO", "CANCELADO") &&
                                            !expired
                                        if (valid) {
                                            pendingOrderQr = PendingOrderQRPayment(payload.copy(totalAmount = total), merchantUid)
                                            paymentSuccessMessage = null
                                            OmniTracker.track("orders", "ORDER_QR_VALIDATED_FOR_PAYMENT", mapOf("operationId" to payload.orderId, "success" to true))
                                        } else {
                                            paymentSuccessMessage = "El pedido no está pendiente, venció o los datos del QR no coinciden con Firestore."
                                        }
                                    }
                                    .addOnFailureListener {
                                        paymentSuccessMessage = "No se pudo verificar el pedido en Firestore."
                                    }
                            },
                            onLegacyWalletQr = { scanned ->
                                qrPaymentPayload = scanned
                                qrPaymentAmount = "1500"
                                showQrPaymentDialog = true
                            },
                            onError = { message -> paymentSuccessMessage = message },
                            modifier = Modifier.weight(1f).testTag("btn_pay_qr"),
                            buttonLabel = "Pagar QR",
                            buttonColor = CyberGlowCyan
                        )

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

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showTransferDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263B68)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Transferir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { showMyQrDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF245A52)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Cobrar QR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Identidad virtual de prueba: alias y CVU ficticios.
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101D34)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tu cuenta OMNI Pay DEMO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Alias: ${profile.walletAlias}", color = Color(0xFFBCEEFF), fontSize = 13.sp)
                    Text("CVU virtual: ${profile.cvuCbu}", color = Color.White, fontSize = 12.sp)
                    Text("Identificadores ficticios: no reciben transferencias bancarias reales.", color = Color(0xFF9AAAC2), fontSize = 10.sp)
                    Button(
                        onClick = { showMyQrDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF174A66))
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mostrar mi QR demo")
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

    if (showMyQrDialog) {
        AlertDialog(
            onDismissRequest = { showMyQrDialog = false },
            title = { Text("Mi QR OMNI Pay · DEMO") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (myQrBitmap != null) {
                        Image(
                            bitmap = myQrBitmap.asImageBitmap(),
                            contentDescription = "QR de cobro OMNI Pay DEMO",
                            modifier = Modifier.size(220.dp)
                        )
                    } else {
                        Text("Preparando tu QR…", color = Color.Gray)
                    }
                    Text("Alias: ${profile.walletAlias}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("CVU virtual: ${profile.cvuCbu}", fontSize = 12.sp)
                    Text("Compartí este QR para recibir OM$ ficticios de otra cuenta OMNI.", fontSize = 11.sp, color = Color.Gray)
                    Text("Solo funciona dentro del entorno OMNI Pay DEMO. No recibe dinero real.", fontSize = 10.sp, color = Color(0xFF9A5B12))
                }
            },
            confirmButton = { TextButton(onClick = { showMyQrDialog = false }) { Text("Cerrar") } }
        )
    }

    pendingOrderQr?.let { pending ->
        AlertDialog(
            onDismissRequest = { if (!orderPaymentInProgress) pendingOrderQr = null },
            title = { Text("Pagar pedido · OMNI Pay DEMO") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Comercio: ${pending.payload.merchantName}", fontWeight = FontWeight.Bold)
                    pending.payload.items.forEach { item ->
                        Text("${item.quantity} × ${item.name}", fontSize = 12.sp)
                    }
                    Text("Total confirmado: OM$ ${String.format("%,.2f", pending.payload.totalAmount)}", fontWeight = FontWeight.ExtraBold)
                    Text("El importe se valida nuevamente en Firestore. No se mueve dinero real.", fontSize = 10.sp, color = Color.Gray)
                    Text("Saldo disponible: OM$ ${String.format("%,.2f", wallet.balance)}", fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    enabled = !orderPaymentInProgress,
                    onClick = {
                        if (!orderPaymentInProgress) {
                            orderPaymentInProgress = true
                            walletRepository.ensureWallet { walletResult ->
                                walletResult.fold(
                                    onSuccess = {
                                        walletRepository.payStoreOrderDemo(
                                            pending.payload.orderId,
                                            pending.merchantUid,
                                            pending.payload.totalAmount
                                        ) { paymentResult ->
                                            orderPaymentInProgress = false
                                            paymentResult.fold(
                                                onSuccess = { message ->
                                                    OmniTracker.track("orders", "ORDER_QR_PAYMENT_CONFIRMED", mapOf("operationId" to pending.payload.orderId, "success" to true))
                                                    pendingOrderQr = null
                                                    paymentSuccessMessage = "$message. Operación ficticia; no se movió dinero real."
                                                },
                                                onFailure = { error ->
                                                    paymentSuccessMessage = error.localizedMessage ?: "No se pudo completar el pago del pedido."
                                                }
                                            )
                                        }
                                    },
                                    onFailure = { error ->
                                        orderPaymentInProgress = false
                                        paymentSuccessMessage = error.localizedMessage ?: "No se pudo preparar OMNI Pay DEMO."
                                    }
                                )
                            }
                        }
                    }
                ) { Text(if (orderPaymentInProgress) "Procesando…" else "Confirmar pago DEMO") }
            },
            dismissButton = {
                TextButton(onClick = { if (!orderPaymentInProgress) pendingOrderQr = null }, enabled = !orderPaymentInProgress) { Text("Cancelar") }
            }
        )
    }

    if (showQrPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showQrPaymentDialog = false },
            title = { Text("Confirmar pago QR · DEMO") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("El pago se acreditará en la cuenta OMNI que emitió el QR.", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = qrPaymentAmount,
                        onValueChange = { qrPaymentAmount = it },
                        label = { Text("Importe OM$") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("Disponible: OM$ ${String.format("%,.2f", wallet.balance)}", fontSize = 11.sp, color = EmeraldDark)
                    Text("Operación ficticia entre cuentas OMNI autenticadas.", fontSize = 10.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = qrPaymentAmount.toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            paymentSuccessMessage = "Ingresá un importe mayor que cero."
                        } else {
                            walletRepository.payDemoQr(qrPaymentPayload, amount) { result ->
                                result.fold(
                                    onSuccess = {
                                        paymentSuccessMessage = "Pago QR aprobado en el entorno DEMO por OM$ ${String.format("%,.2f", amount)}. El saldo del destinatario se actualiza automáticamente."
                                        showQrPaymentDialog = false
                                    },
                                    onFailure = { error ->
                                        paymentSuccessMessage = error.localizedMessage ?: "No se pudo completar el pago DEMO."
                                    }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) { Text("Confirmar pago DEMO") }
            },
            dismissButton = { TextButton(onClick = { showQrPaymentDialog = false }) { Text("Cancelar") } }
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
                    Text("Agregá saldo ficticio al entorno de pruebas. Límite: OM$ 50.000 por carga y OM$ 150.000 acumulados por cuenta.", fontSize = 13.sp)
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
                            walletRepository.topUpDemo(amount) { result ->
                                result.fold(
                                    onSuccess = {
                                        showAddFundsDialog = false
                                        paymentSuccessMessage = "Carga DEMO aprobada por OM$ ${String.format("%,.2f", amount)}."
                                    },
                                    onFailure = { error ->
                                        paymentSuccessMessage = error.localizedMessage ?: "No se pudo cargar saldo DEMO."
                                    }
                                )
                            }
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

    // Transferencia saliente entre identidades ficticias de prueba
    if (showTransferDialog) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text("Transferir con OMNI Pay") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Simulación entre usuarios OMNI. No se envía dinero real.", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = transferRecipient,
                        onValueChange = { transferRecipient = it },
                        label = { Text("Alias o CVU de destino") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { transferAmount = it },
                        label = { Text("Importe OM$") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("Disponible: OM$ ${String.format("%,.2f", wallet.balance)}", fontSize = 11.sp, color = EmeraldDark)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = transferAmount.toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0 || !amount.isFinite() || transferRecipient.isBlank()) {
                            paymentSuccessMessage = "Revisá el alias/CVU y el importe."
                        } else {
                            walletRepository.transferDemoFunds(transferRecipient, amount) { result ->
                                result.fold(
                                    onSuccess = {
                                        paymentSuccessMessage = "Transferencia DEMO aprobada por OM$ ${String.format("%,.2f", amount)}. La cuenta destinataria recibe el importe automáticamente."
                                        transferRecipient = ""
                                        showTransferDialog = false
                                    },
                                    onFailure = { error ->
                                        paymentSuccessMessage = error.localizedMessage ?: "No se pudo realizar la transferencia DEMO."
                                    }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) { Text("Confirmar transferencia demo") }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) { Text("Cancelar") }
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
    val isIncome = transaction.type == TransactionType.CARGA_SALDO || transaction.type == TransactionType.REINTEGRO_CASHBACK || transaction.type == TransactionType.TRANSFERENCIA_RECIBIDA || transaction.type == TransactionType.QR_COBRO

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
                Text(transaction.status, fontSize = 9.sp, color = EmeraldDark, fontWeight = FontWeight.SemiBold)
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
