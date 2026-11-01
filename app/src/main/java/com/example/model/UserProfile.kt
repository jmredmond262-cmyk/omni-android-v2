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
    val birthDate: String = "14/08/1994",
    val pronouns: String = "Él / He",
    val gender: String = "Masculino",
    val isVerified: Boolean = false,
    val preferredLanguage: String = "Español (Latinoamérica)",
    val privacyLevel: String = "Público (Vecinos del Barrio)",
    val instagramHandle: String = "",
    val whatsappNumber: String = "",
    val twitterHandle: String = "",
    val tiktokHandle: String = "",

    // --- BILLETERA VIRTUAL (OMNIPAY) ---
    val walletAlias: String = "omni.santi.ar",
    val cvuCbu: String = "0000003100049281749281",
    val nationalId: String = "38.291.842",
    val accountBalance: Double = 34500.0,
    val dailySpendingLimit: Double = 50000.0,
    val autoCashbackEnabled: Boolean = true,
    val savedCards: List<SavedCard> = listOf(
        SavedCard("card_1", "Visa Débito", "4821", "Banco Galicia", "08/28", true),
        SavedCard("card_2", "Mastercard Black", "9102", "Banco Santander", "11/29", false)
    ),

    // --- MAPAS, NAVEGACIÓN & UBICACIONES ---
    val currentGpsAddress: String = "Simón de Iriondo y San Martín, Romang, Santa Fe (CP 3555)",
    val currentLatitude: Double = -29.5052,
    val currentLongitude: Double = -59.7562,
    val searchRadiusMeters: Int = 2000,
    val preferredTransitMode: String = "A pie 🚶", // A pie, Auto 🚗, Bici 🚲, Transporte público 🚌
    val mapStyle: String = "Satélite HD", // Satélite HD, Vectorial, Modo Noche
    val addresses: List<UserAddress> = listOf(
        UserAddress("addr_1", "Mi Local (Redcom)", "Simón de Iriondo y San Martín, Romang", true, "work"),
        UserAddress("addr_2", "Casa", "Calle Brigadier López 520, Romang", false, "home"),
        UserAddress("addr_3", "Club / Balneario", "Camping Comunal y Balneario Romang", false, "favorite")
    ),

    // --- SALUD, SEGURIDAD & EMERGENCIAS ---
    val emergencyContactName: String = "Valeria Rossi (Hermana)",
    val emergencyContactPhone: String = "+54 9 11 4433-2211",
    val bloodType: String = "0 Positivo (0+)",
    val medicalNotes: String = "Alérgico a la penicilina",
    val morningDigestEnabled: Boolean = true,
    val morningDigestTime: String = "06:45",
    val silentWakeupActive: Boolean = true,
    val severeWeatherAlertSound: Boolean = true,
    val seismicAlertEnabled: Boolean = true,
    val onRouteAlertsEnabled: Boolean = true,
    val maxDetourAlertMeters: Int = 80,

    // --- COMUNIDAD & REPUTACIÓN ---
    val neighborLevel: String = "Vecino Platino",
    val monthlySavingsTotal: Double = 18450.0,
    val dietaryPreference: DietaryPreference = DietaryPreference.SIN_TACC,

    // --- TEMA VISUAL ---
    val themeMode: AppThemeMode = AppThemeMode.NEON_EMERALD
)
