package com.example.model

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double
)

data class DigitalReceipt(
    val id: String,
    val receiptNumber: String,
    val storeName: String,
    val dateText: String,
    val items: List<ReceiptItem>,
    val subtotal: Double,
    val discountApplied: Double,
    val total: Double,
    val paymentMethod: String,
    val qrVerificationCode: String
)

data class TouristAttraction(
    val id: String,
    val name: String,
    val category: String,
    val distanceKm: Double,
    val estimatedVisitTime: String,
    val description: String,
    val rating: Float,
    val coordinatesX: Float,
    val coordinatesY: Float
)
