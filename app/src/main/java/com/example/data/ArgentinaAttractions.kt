package com.example.data

import com.example.model.TouristAttraction
import kotlin.math.*

/**
 * Curated regional points of interest. Results are always distance-filtered:
 * OMNI must not suggest national destinations as if they were nearby.
 */
object ArgentinaAttractions {
    private data class Point(
        val name: String,
        val city: String,
        val latitude: Double,
        val longitude: Double,
        val category: String,
        val duration: String,
        val description: String
    )

    private val places = listOf(
        Point("Balneario Municipal de Romang", "Romang", -29.5052, -59.7562, "Naturaleza", "2 h", "Área recreativa junto al río San Javier, ideal para disfrutar del entorno natural."),
        Point("Jaaukanigás – Sitio Ramsar", "Reconquista", -29.1450, -59.6430, "Naturaleza", "Medio día", "Humedales del Paraná con gran biodiversidad y oportunidades de observación de aves."),
        Point("Puerto de Reconquista", "Reconquista", -29.2500, -59.6000, "Paseo", "1 h 30 min", "Paseo ribereño y acceso al paisaje de islas del Paraná."),
        Point("Camping y balneario La Lola", "Reconquista", -29.2050, -59.6100, "Naturaleza", "2 h", "Entorno de río e islas para actividades recreativas al aire libre."),
        Point("Laguna El Cristal", "Calchaquí", -29.9000, -60.3000, "Naturaleza", "Medio día", "Complejo natural de laguna y espacios recreativos de la región."),
        Point("Costanera de Santa Fe", "Santa Fe", -31.6400, -60.7000, "Paseo", "1 h 30 min", "Recorrido urbano junto al río, con espacios verdes y vistas panorámicas."),
        Point("Puente Colgante", "Santa Fe", -31.6405, -60.6905, "Monumento", "45 min", "Emblema arquitectónico de la ciudad y uno de sus principales puntos fotográficos."),
        Point("Parque Nacional a la Bandera", "Rosario", -32.9475, -60.6300, "Historia", "1 h", "Conjunto monumental junto al Paraná y espacio público de valor histórico.")
    )

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return 6371.0 * 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    }

    fun nearby(
        latitude: Double,
        longitude: Double,
        limit: Int = 6,
        radiusKm: Double = 80.0
    ): List<TouristAttraction> = places
        .map { point -> point to distanceKm(latitude, longitude, point.latitude, point.longitude) }
        .filter { (_, distance) -> distance <= radiusKm }
        .sortedBy { it.second }
        .take(limit)
        .mapIndexed { index, (point, distance) ->
            TouristAttraction(
                "regional_att_${index}_${point.name.hashCode()}",
                point.name,
                point.category,
                distance,
                point.duration,
                "${point.description} · ${point.city}",
                4.7f,
                0.5f,
                0.5f
            )
        }
}
