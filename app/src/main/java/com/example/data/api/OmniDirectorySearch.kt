package com.example.data.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class OmniDirectoryPlace(
    val id: String, val name: String, val category: String, val typeLabel: String,
    val latitude: Double, val longitude: Double, val distanceMeters: Int,
    val address: String, val phone: String, val openingHours: String, val osmUrl: String
)

/** Motor nativo de búsqueda integral sobre OpenStreetMap/Overpass. */
object OmniDirectorySearch {
    private const val TAG = "OmniDirectorySearch"
    private const val PREFS = "omni_directory_cache"
    private const val CACHE_TTL_MS = 6L * 60L * 60L * 1000L
    private data class TagFilter(val key: String, val values: String? = null)

    private val groups = linkedMapOf(
        "education" to listOf(TagFilter("amenity","school|kindergarten|college|university|language_school"),TagFilter("building","school")),
        "health" to listOf(TagFilter("amenity","hospital|clinic|doctors|dentist|pharmacy|veterinary"),TagFilter("healthcare"),TagFilter("shop","chemist|medical_supply"),TagFilter("building","hospital")),
        "commerce" to listOf(TagFilter("shop"),TagFilter("amenity","marketplace"),TagFilter("craft")),
        "services" to listOf(TagFilter("office"),TagFilter("amenity","community_centre|social_facility"),TagFilter("social_facility"),TagFilter("club")),
        "government" to listOf(TagFilter("office","government"),TagFilter("amenity","townhall|courthouse|police|fire_station|post_office"),TagFilter("government")),
        "food" to listOf(TagFilter("amenity","restaurant|cafe|fast_food|food_court|ice_cream"),TagFilter("shop","bakery|butcher|seafood|cheese|confectionery")),
        "transport" to listOf(TagFilter("amenity","bus_station|taxi|fuel"),TagFilter("public_transport"),TagFilter("railway","station"),TagFilter("aeroway","aerodrome")),
        "tourism" to listOf(TagFilter("tourism"),TagFilter("leisure"),TagFilter("historic"),TagFilter("amenity","arts_centre|theatre")),
        "finance" to listOf(TagFilter("amenity","bank|atm|bureau_de_change"),TagFilter("office","financial")),
        "auto" to listOf(TagFilter("shop","car|car_repair|tyres|motorcycle"),TagFilter("amenity","car_wash"))
    )
    private val labels = mapOf(
        "education" to "Educación","health" to "Salud","commerce" to "Comercios",
        "services" to "Servicios y mutuales","government" to "Organismos públicos",
        "food" to "Gastronomía","transport" to "Transporte","tourism" to "Turismo y recreación",
        "finance" to "Bancos y finanzas","auto" to "Automotores"
    )

    suspend fun search(context: Context, latitude: Double, longitude: Double, radiusMeters: Int, category: String = "all"): List<OmniDirectoryPlace> =
        withContext(Dispatchers.IO) {
            val radius = radiusMeters.coerceIn(500, 100000)
            val selected = if (category == "all" || category !in groups) groups.keys.toList() else listOf(category)
            val filters = selected.flatMap { groups[it].orEmpty() }.distinct()
            val query = buildQuery(latitude, longitude, radius, filters)
            val key = "v3_${String.format(java.util.Locale.US,"%.4f",latitude)}_${String.format(java.util.Locale.US,"%.4f",longitude)}_${radius}_${category}"
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val cached = prefs.getString(key, null)
            if (cached != null && System.currentTimeMillis() - prefs.getLong(key+"_at",0L) < CACHE_TTL_MS) {
                try { return@withContext parseResults(JSONObject(cached), latitude, longitude, category) }
                catch (e: Exception) { Log.w(TAG, "Caché inválida: ${e.message}") }
            }
            val endpoints = listOf("https://overpass-api.de/api/interpreter","https://overpass.kumi.systems/api/interpreter","https://overpass.private.coffee/api/interpreter")
            var lastError = "No hubo respuesta de los servidores públicos."
            for (endpoint in endpoints) {
                try {
                    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod="POST"; connectTimeout=6000; readTimeout=14000; doOutput=true
                        setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8")
                        setRequestProperty("User-Agent","OMNI-Android/2.0 (OpenStreetMap directory)")
                    }
                    conn.outputStream.use { it.write(("data="+URLEncoder.encode(query,"UTF-8")).toByteArray(Charsets.UTF_8)) }
                    val status=conn.responseCode
                    if(status!=HttpURLConnection.HTTP_OK){lastError="Overpass respondió HTTP $status.";conn.disconnect();continue}
                    val body=conn.inputStream.bufferedReader().use{it.readText()}
                    val json=JSONObject(body)
                    val remark=json.optString("remark")
                    if(remark.isNotBlank()){lastError="Consulta incompleta del servidor: $remark";conn.disconnect();continue}
                    prefs.edit().putString(key,body).putLong(key+"_at",System.currentTimeMillis()).apply()
                    conn.disconnect()
                    return@withContext parseResults(json,latitude,longitude,category)
                } catch(e:Exception){lastError=e.message ?: "Error de conexión.";Log.w(TAG,"Falló $endpoint: $lastError")}
            }
            Log.w(TAG, "OmniDirectorySearch finalizó sin resultados: $lastError")
            return@withContext emptyList()
        }

    private fun buildQuery(lat:Double,lon:Double,radius:Int,filters:List<TagFilter>):String {
        val clauses=filters.map { f ->
            val selector = if (f.values.isNullOrBlank()) "[\"" + f.key + "\"]" else "[\"" + f.key + "\"~\"^(" + f.values + ")$\"]"
            "nwr(around:$radius,$lat,$lon)$selector;"
        }.distinct()
        return "[out:json][timeout:15];(${clauses.joinToString("")});out center tags;"
    }

    private fun parseResults(json:JSONObject,originLat:Double,originLon:Double,requestedCategory:String):List<OmniDirectoryPlace>{
        val elements=json.optJSONArray("elements")?:return emptyList()
        val seen=HashSet<String>();val results=ArrayList<OmniDirectoryPlace>(elements.length())
        for(i in 0 until elements.length()){
            val e=elements.optJSONObject(i)?:continue
            val kind=e.optString("type");val osmId=e.optLong("id",-1L)
            if(osmId<0L||!seen.add("$kind:$osmId"))continue
            val tags=e.optJSONObject("tags")?:JSONObject()
            val center=e.optJSONObject("center")
            val lat=if(e.has("lat"))e.optDouble("lat",Double.NaN) else center?.optDouble("lat",Double.NaN)?:Double.NaN
            val lon=if(e.has("lon"))e.optDouble("lon",Double.NaN) else center?.optDouble("lon",Double.NaN)?:Double.NaN
            if(!lat.isFinite()||!lon.isFinite())continue
            val name=tags.optString("name").ifBlank{tags.optString("name:es")}.ifBlank{tags.optString("brand")}.ifBlank{tags.optString("operator")}.ifBlank{"Lugar sin nombre"}
            val group=classify(tags)
            if(requestedCategory != "all" && group != requestedCategory) continue
            val type=listOf("amenity","shop","office","healthcare","tourism","leisure","craft","social_facility","government","railway","public_transport","historic")
                .firstNotNullOfOrNull{key->tags.optString(key).takeIf{it.isNotBlank()}}?.replace('_',' ')?: "lugar o servicio"
            val street=tags.optString("addr:street");val number=tags.optString("addr:housenumber");val place=tags.optString("addr:place");val city=tags.optString("addr:city")
            val address=when{street.isNotBlank()&&number.isNotBlank()->"$street $number";street.isNotBlank()->street;place.isNotBlank()->place;else->"Dirección no informada"}+(if(city.isNotBlank())", $city" else "")
            results.add(OmniDirectoryPlace(
                id="osm_${kind}_$osmId",name=name,category=labels[group]?: "Otros",typeLabel=type,
                latitude=lat,longitude=lon,distanceMeters=distanceMeters(originLat,originLon,lat,lon).toInt(),
                address=address,phone=tags.optString("phone").ifBlank{tags.optString("contact:phone")},
                openingHours=tags.optString("opening_hours"),osmUrl="https://www.openstreetmap.org/$kind/$osmId"
            ))
        }
        return results.sortedBy{it.distanceMeters}
    }
    private fun classify(tags:JSONObject):String{
        // Prioridad semántica: salud y gastronomía antes del genérico shop=commerce.
        val priority = listOf("health", "food", "education", "government", "transport", "finance", "auto", "tourism", "services", "commerce")
        for(group in priority){
            val filters = groups[group].orEmpty()if(filters.any{f->val v=tags.optString(f.key);v.isNotBlank()&&(f.values==null||f.values.split("|").contains(v))})return group
        return "services"
    }
    private fun distanceMeters(a:Double,b:Double,c:Double,d:Double):Double{
        val rad=Math.PI/180.0;val x=(c-a)*rad;val y=(d-b)*rad
        val h=sin(x/2)*sin(x/2)+cos(a*rad)*cos(c*rad)*sin(y/2)*sin(y/2)
        return 6371000.0*2.0*atan2(sqrt(h),sqrt(1.0-h))
    }
}
