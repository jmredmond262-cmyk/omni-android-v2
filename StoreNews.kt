package com.example.model

data class StoreNews(
    val id: String,
    val storeId: String,
    val storeName: String,
    val title: String,
    val content: String,
    val timeAgoText: String,
    val badge: String = "Novedad Flash",
    val isHot: Boolean = true
)
