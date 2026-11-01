package com.example.model

enum class AlertSeverity(val label: String, val hexColor: Long) {
    INFO("Aviso Informativo", 0xFF0284C7),
    AMARILLO("Alerta Amarilla", 0xFFF59E0B),
    NARANJA("Alerta Naranja", 0xFFEA580C),
    ROJO("Alerta Roja Extrema", 0xFFDC2626)
}

enum class AlertType(val label: String) {
    CLIMA_CALOR("Ola de Calor Extremo"),
    CLIMA_FRIO("Ola de Frío Polar"),
    TORMENTA("Tormentas Severas & Granizo"),
    HURACAN_TORNADO("Huracán / Vientos Severos"),
    SISMO_TERREMOTO("Alerta de Sismo"),
    CRECIDA_RIO("Crecida de Río & Inundación")
}

data class WeatherAlert(
    val id: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val title: String,
    val description: String,
    val instructions: String,
    val validUntil: String
)

data class MorningBriefing(
    val greeting: String,
    val dateText: String,
    val weatherSummary: String,
    val temperatureRange: String,
    val motivationalQuote: String,
    val activeDealsCount: Int,
    val pendingTasksCount: Int
)
