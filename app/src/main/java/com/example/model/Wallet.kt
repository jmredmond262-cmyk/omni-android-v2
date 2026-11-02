package com.example.model

enum class TransactionType {
    COMPRA_LOCAL,
    REINTEGRO_CASHBACK,
    CARGA_SALDO,
    TRANSFERENCIA_ENVIADA,
    TRANSFERENCIA_RECIBIDA,
    QR_PAGO,
    QR_COBRO
}

data class WalletTransaction(
    val id: String,
    val title: String,
    val storeOrEntityName: String,
    val amount: Double,
    val type: TransactionType,
    val dateText: String,
    val cashbackEarned: Double = 0.0,
    val status: String = "APROBADA · DEMO"
)

data class PaymentCard(
    val id: String,
    val bankName: String,
    val lastFour: String,
    val cardBrand: String,
    val isDefault: Boolean = false
)

data class UserWallet(
    val balance: Double = 0.0,
    val totalCashbackEarned: Double = 0.0,
    val cards: List<PaymentCard> = emptyList(),
    val transactions: List<WalletTransaction> = emptyList()
)
