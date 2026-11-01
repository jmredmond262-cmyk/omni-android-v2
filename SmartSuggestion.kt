package com.example.model

data class SmartSuggestion(
    val id: String,
    val productName: String,
    val category: ProductCategory,
    val reasonText: String,
    val discountBadge: String,
    val storeName: String,
    val price: Double,
    val originalPrice: Double? = null,
    val isOnRoute: Boolean = false,
    val detourMeters: Int = 0
)
