package com.example.model

import com.example.ui.theme.AppThemeMode

data class UserAddress(
    val id: String,
    val title: String,
    val address: String,
    val isPrimary: Boolean = false,
    val iconType: String = "home" // home, work, favorite
)

enum class DietaryPreference(val label: String) {
    TODO("Sin restricciones"),
    SIN_TACC("Sin TACC / Celíaco"),
    VEGANO("Vegano"),
    VEGETARIANO("Vegetariano"),
    ORGANICO("100% Orgánico"),
    DIABETICO("Bajo en Azúcar / Diabético"),
    HIPERTENSO("Bajo en Sodio / Hipertenso")
}

data class SavedCard(
    val id: String,
    val brand: String, // Visa, Mastercard, Cabal
    val lastFourDigits: String,
    val bankName: String,
    val expiry: String,
    val isDefault: Boolean = false
)

data class UserProfile(
    val id: String = "",
    // --- RED SOCIAL & IDENTIDAD ---
    val fullName: String = "",
    val username: String = "",
    val bio: String = "",
    val statusMood: String = "",
    val email: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val birthDate: String = "",
    val pronouns: String = "",
    val gender: String = "",
    val isVerified: Boolean = false,
    val preferredLanguage: String = "Español (Latinoamérica)",
    val privacyLevel: String = "Público (Vecinos del Barrio)",
    val instagramHandle: String = "",
    val whatsappNumber: String = "",
    val twitterHandle: String = "",
    val tiktokHandle: String = "",

    // --- BILLETERA VIRTUAL (OMNIPAY) ---
    val walletAlias: String = "",
    val cvuCbu: String = "",
    val nationalId: String = "",
    val accountBalance: Double = 0.0,
    val dailySpendingLimit: Double = 0.0,
    val autoCashbackEnabled: Boolean = false,
    val savedCards: List<SavedCard> = emptyList(),

    // --- MAPAS, NAVEGACIÓN & UBICACIONES ---
    val currentGpsAddress: String = "",
    val currentLatitude: Double = 0.0,
    val currentLongitude: Double = 0.0,
    val searchRadiusMeters: Int = 5000,
    val preferredTransitMode: String = "", // A pie, Auto 🚗, Bici 🚲, Transporte público 🚌
    val mapStyle: String = "", // Satélite HD, Vectorial, Modo Noche
    val addresses: List<UserAddress> = emptyList(),

    // --- SALUD, SEGURIDAD & EMERGENCIAS ---
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val bloodType: String = "",
    val medicalNotes: String = "",
    val morningDigestEnabled: Boolean = false,
    val morningDigestTime: String = "08:00",
    val silentWakeupActive: Boolean = false,
    val severeWeatherAlertSound: Boolean = false,
    val seismicAlertEnabled: Boolean = false,
    val onRouteAlertsEnabled: Boolean = false,
    val maxDetourAlertMeters: Int = 80,

    // --- COMUNIDAD & REPUTACIÓN ---
    val neighborLevel: String = "",
    val monthlySavingsTotal: Double = 0.0,
    val dietaryPreference: DietaryPreference = DietaryPreference.TODO,

    // --- TEMA VISUAL ---
    val themeMode: AppThemeMode = AppThemeMode.NEON_EMERALD
)
