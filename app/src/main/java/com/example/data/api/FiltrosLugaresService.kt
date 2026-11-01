package com.example.data.api

import android.location.Location

data class Lugar(
    val id: String,
    val nombre: String,
    val rubro: String,
    val estaAbierto: Boolean?,
    val latitud: Double,
    val longitud: Double,
    val horario: String
)

data class LugarConDistancia(
    val lugar: Lugar,
    val distanciaMetros: Float
)

object FiltrosLugaresService {
    fun calcularDistancia(
        latUsuario: Double,
        lonUsuario: Double,
        latLugar: Double,
        lonLugar: Double
    ): Float {
        val resultados = FloatArray(1)
        Location.distanceBetween(latUsuario, lonUsuario, latLugar, lonLugar, resultados)
        return resultados[0]
    }

    fun filtrarYOrdenar(
        todosLosLugares: List<Lugar>,
        latUsuario: Double,
        lonUsuario: Double,
        rubroFiltro: String? = null,
        soloAbiertos: Boolean = false,
        radioMaximoKm: Float
    ): List<LugarConDistancia> {
        val radioMetros = radioMaximoKm.coerceAtLeast(0f) * 1000f
        return todosLosLugares.map { lugar ->
            LugarConDistancia(
                lugar,
                calcularDistancia(latUsuario, lonUsuario, lugar.latitud, lugar.longitud)
            )
        }.filter { item ->
            item.distanciaMetros <= radioMetros &&
                (rubroFiltro.isNullOrBlank() || item.lugar.rubro.equals(rubroFiltro, true)) &&
                (!soloAbiertos || item.lugar.estaAbierto == true)
        }.sortedBy { it.distanciaMetros }
    }
}
