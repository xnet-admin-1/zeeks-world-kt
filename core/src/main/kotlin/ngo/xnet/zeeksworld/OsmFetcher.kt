package ngo.xnet.zeeksworld

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

data class LatLon(val lat: Double, val lon: Double)
data class Building(val outline: List<LatLon>, val height: Double, val name: String)
data class Road(val points: List<LatLon>, val width: Double, val type: String)
data class Park(val outline: List<LatLon>, val name: String)
data class Water(val outline: List<LatLon>)
data class OsmData(
    val buildings: List<Building>,
    val roads: List<Road>,
    val parks: List<Park>,
    val water: List<Water>
)

object OsmFetcher {
    private const val OVERPASS_URL = "https://overpass-api.de/api/interpreter"

    fun fetchArea(lat: Double, lon: Double, radiusMeters: Double): OsmData {
        val query = "[out:json][timeout:90];(" +
            "way[\"building\"](around:%.0f,%.6f,%.6f);".format(radiusMeters, lat, lon) +
            "way[\"highway\"](around:%.0f,%.6f,%.6f);".format(radiusMeters, lat, lon) +
            "way[\"leisure\"=\"park\"](around:%.0f,%.6f,%.6f);".format(radiusMeters, lat, lon) +
            "way[\"natural\"=\"water\"](around:%.0f,%.6f,%.6f);".format(radiusMeters, lat, lon) +
            ");out geom;"

        val body = "data=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val conn = URI(OVERPASS_URL).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.setRequestProperty("User-Agent", "zeeks-world/1.0")
        conn.doOutput = true
        conn.outputStream.use { it.write(body.toByteArray()) }

        check(conn.responseCode == 200) {
            "Overpass returned ${conn.responseCode}: ${conn.errorStream?.bufferedReader()?.readText()?.take(200)}"
        }

        val json = conn.inputStream.bufferedReader().readText()
        return parseResponse(JSONObject(json))
    }

    private fun parseResponse(json: JSONObject): OsmData {
        val buildings = mutableListOf<Building>()
        val roads = mutableListOf<Road>()
        val parks = mutableListOf<Park>()
        val water = mutableListOf<Water>()

        val elements = json.getJSONArray("elements")
        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val tags = el.optJSONObject("tags") ?: continue
            val geom = el.optJSONArray("geometry") ?: continue
            val coords = (0 until geom.length()).map { j ->
                val n = geom.getJSONObject(j)
                LatLon(n.getDouble("lat"), n.getDouble("lon"))
            }

            when {
                tags.has("building") -> {
                    var h = 4.0
                    tags.optString("height", "").toDoubleOrNull()?.let { v ->
                        h = (v / 2.0).coerceIn(3.0, 5.0)
                    }
                    buildings += Building(coords, h, tags.optString("name", ""))
                }
                tags.has("highway") -> {
                    val type = tags.getString("highway")
                    roads += Road(coords, roadWidth(type), type)
                }
                tags.optString("leisure") == "park" ->
                    parks += Park(coords, tags.optString("name", ""))
                tags.optString("natural") == "water" ->
                    water += Water(coords)
            }
        }
        return OsmData(buildings, roads, parks, water)
    }

    private fun roadWidth(type: String): Double = when (type) {
        "motorway", "trunk" -> 4.0
        "primary", "secondary" -> 3.0
        "tertiary", "residential" -> 2.0
        else -> 1.0
    }
}
