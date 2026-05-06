package ngo.xnet.zeeksworld

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeoapifyEnricher {
    private const val API_KEY = "dff28ab1ea75480a8aadef9fb13c773f"
    private const val BASE_URL = "https://api.geoapify.com/v2"

    data class PlaceInfo(
        val name: String,
        val categories: List<String>,
        val lat: Double,
        val lon: Double
    )

    data class BuildingInfo(
        val type: String,
        val levels: Int,
        val height: Double,
        val color: String
    )

    /** Fetch nearby places (shops, parks, restaurants) to add as landmarks */
    fun fetchPlacesNear(lat: Double, lon: Double, radiusM: Int = 200): List<PlaceInfo> {
        val categories = "commercial,catering,leisure,education,entertainment"
        val url = "$BASE_URL/places?categories=$categories&filter=circle:$lon,$lat,$radiusM&limit=50&apiKey=$API_KEY"
        val json = httpGet(url) ?: return emptyList()
        val features = json.optJSONArray("features") ?: return emptyList()
        val places = mutableListOf<PlaceInfo>()
        for (i in 0 until features.length()) {
            val props = features.getJSONObject(i).getJSONObject("properties")
            places.add(PlaceInfo(
                name = props.optString("name", ""),
                categories = props.optJSONArray("categories")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList(),
                lat = props.optDouble("lat", 0.0),
                lon = props.optDouble("lon", 0.0)
            ))
        }
        return places
    }

    /** Fetch building details at coordinates */
    fun fetchBuildingAt(lat: Double, lon: Double): BuildingInfo? {
        val url = "$BASE_URL/place-details?lat=$lat&lon=$lon&features=building&apiKey=$API_KEY"
        val json = httpGet(url) ?: return null
        val features = json.optJSONArray("features") ?: return null
        for (i in 0 until features.length()) {
            val props = features.getJSONObject(i).getJSONObject("properties")
            val building = props.optJSONObject("building") ?: continue
            val levels = building.optString("levels", "1").toIntOrNull() ?: 1
            return BuildingInfo(
                type = building.optString("type", ""),
                levels = levels,
                height = levels * 3.5,
                color = building.optString("color", "")
            )
        }
        return null
    }

    /** Enrich the world with place markers (colored blocks at POI locations) */
    fun enrichWorld(world: World, centerLat: Double, centerLon: Double) {
        val places = fetchPlacesNear(centerLat, centerLon)
        println("Geoapify: enriching with ${places.size} places")

        val metersPerDegLat = 111320.0
        val metersPerDegLon = metersPerDegLat * Math.cos(Math.toRadians(centerLat))

        for (place in places) {
            if (place.name.isEmpty()) continue
            val bx = ((place.lon - centerLon) * metersPerDegLon / 2.0).toInt()
            val bz = ((place.lat - centerLat) * metersPerDegLat / 2.0).toInt()

            // Place a colored marker based on category
            val block = when {
                place.categories.any { "catering" in it } -> Block.RUBY      // restaurants = red
                place.categories.any { "commercial" in it } -> Block.AMETHYST // shops = purple
                place.categories.any { "leisure" in it } -> Block.GLOW       // parks/fun = gold
                place.categories.any { "education" in it } -> Block.CLOUD    // schools = white
                else -> Block.GLOW
            }

            // Place marker pole (3 blocks tall so it's visible)
            for (y in 1..3) {
                world.setBlock(bx, y, bz, block)
            }
            println("  📍 ${place.name} at ($bx, $bz) [${place.categories.firstOrNull() ?: ""}]")
        }
    }

    private fun httpGet(urlStr: String): JSONObject? {
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "zeeks-world/1.0")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().readText()
            JSONObject(body)
        } catch (e: Exception) {
            println("Geoapify error: ${e.message}")
            null
        }
    }
}
