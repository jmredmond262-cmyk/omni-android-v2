package com.example.model

enum class ContactCategory(val label: String) {
    REMIS("Remisería"),
    TAXI("Radio Taxi"),
    FARMACIA_TURNO("Farmacia de Turno"),
    CERRAJERIA("Cerrajería 24hs"),
    FLETE("Flete & Mudanza"),
    EMERGENCIA("Emergencias")
}

data class NeighborhoodContact(
    val id: String,
    val name: String,
    val category: ContactCategory,
    val phoneNumber: String,
    val statusText: String,
    val rating: Float,
    val address: String? = null,
    val isFavorite: Boolean = false,
    val averageWaitMinutes: Int? = null
)
