package com.example.model

enum class TransactionType {
    COMPRA_LOCAL,
    REINTEGRO_CASHBACK,
    CARGA_SALDO
}

data class WalletTransaction(
    val id: String,
    val title: String,
    val storeOrEntityName: String,
    val amount: Double,
    val type: TransactionType,
    val dateText: String,
    val cashbackEarned: Double = 0.0
)

data class PaymentCard(
    val id: String,
    val bankName: String,
    val lastFour: String,
    val cardBrand: String,
    val isDefault: Boolean = false
)

data class UserWallet(
    val balance: Double = 18450.0,
    val totalCashbackEarned: Double = 2340.0,
    val cards: List<PaymentCard> = listOf(
        PaymentCard("c1", "Banco Galicia", "4821", "Visa Débito", true),
        PaymentCard("c2", "Mercado Pago", "9012", "Mastercard", false)
    ),
    val transactions: List<WalletTransaction> = listOf(
        WalletTransaction(
            id = "w1",
            title = "Compra en Supermercado San Martín",
            storeOrEntityName = "Supermercado San Martín Express",
            amount = -4850.0,
            type = TransactionType.COMPRA_LOCAL,
            dateText = "Hoy, 10:45 hs",
            cashbackEarned = 242.5
        ),
        WalletTransaction(
            id = "w2",
            title = "Reintegro 5% Promoción Barrial",
            storeOrEntityName = "OmniHub Cashback",
            amount = 242.5,
            type = TransactionType.REINTEGRO_CASHBACK,
            dateText = "Hoy, 10:46 hs"
        ),
        WalletTransaction(
            id = "w3",
            title = "Carga de Saldo por Transferencia",
            storeOrEntityName = "CVU Bancario",
            amount = 15000.0,
            type = TransactionType.CARGA_SALDO,
            dateText = "Ayer, 18:20 hs"
        )
    )
)
