package com.curtrack.app.data

import com.curtrack.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Client for the CurTrack broker (broker/README.md). All calls are suspend + IO. */
class BrokerException(val status: Int, message: String) : Exception(message)

data class LoginResult(val token: String, val expiresAt: Long, val userId: String, val firstName: String, val portalId: String, val units: List<UnitConfig>, val driverIds: List<String>, val platformUrl: String?)
data class UnitConfig(val esn: String, val name: String, val plate: String, val group: String, val driverId: String?, val hasDashcam: Boolean)
data class RouteStep(val instruction: String, val distanceM: Int, val durationS: Int, val point: GeoPoint?)
data class RouteResult(val distanceM: Int, val durationS: Int, val points: List<GeoPoint>, val steps: List<RouteStep>, val attribution: String)
data class TrailPoint(val point: GeoPoint, val speed: Int, val heading: Int, val at: Long, val event: String, val address: String)

object Broker {
    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    fun stamp(t: Long): String = iso.format(Date(t))

    private suspend fun call(path: String, token: String? = null, body: JSONObject? = null): String = withContext(Dispatchers.IO) {
        val conn = (URL(BuildConfig.BROKER_URL + path).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000; readTimeout = 45000
            setRequestProperty("accept", "application/json")
            if (token != null) setRequestProperty("authorization", "Bearer $token")
            if (body != null) { requestMethod = "POST"; doOutput = true; setRequestProperty("content-type", "application/json"); outputStream.use { it.write(body.toString().toByteArray()) } }
        }
        val status = conn.responseCode
        val text = (if (status < 400) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText() ?: ""
        if (status >= 400) {
            val msg = runCatching { JSONObject(text).optString("error") }.getOrNull().takeUnless { it.isNullOrBlank() } ?: "Server error $status"
            throw BrokerException(status, msg)
        }
        text
    }

    suspend fun login(userId: String, password: String, portalId: String): LoginResult {
        val j = JSONObject(call("/auth/login", body = JSONObject().put("userId", userId).put("password", password).put("portalId", portalId)))
        val u = j.getJSONObject("user")
        val units = j.getJSONArray("units").map { UnitConfig(it.getString("esn"), it.optString("name"), it.optString("plate"), it.optString("group", "My vehicles"), it.optString("driverId").takeIf { s -> s.isNotBlank() && s != "null" }, it.optBoolean("hasDashcam")) }
        val drivers = j.optJSONArray("driverIds")?.let { a -> (0 until a.length()).map { a.get(it).toString() } } ?: emptyList()
        return LoginResult(j.getString("token"), j.getLong("expiresAt"), u.getString("userId"), u.optString("firstName", u.getString("userId")), u.optString("portalId", "189"), units, drivers, j.optJSONObject("capabilities")?.optString("platformUrl"))
    }

    suspend fun units(token: String): List<Unit> = JSONObject(call("/units", token)).getJSONArray("units").map { it.toUnit() }

    suspend fun history(token: String, esn: String, from: Long, to: Long): List<TrailPoint> =
        JSONObject(call("/units/${enc(esn)}/history?from=${enc(stamp(from))}&to=${enc(stamp(to))}", token)).getJSONArray("points").map {
            TrailPoint(GeoPoint(it.getDouble("lat"), it.getDouble("lng")), it.optInt("speed"), it.optInt("heading"), it.optLong("at"), it.optString("event"), it.optString("address"))
        }

    suspend fun alerts(token: String, fromMs: Long): List<Alert> = JSONObject(call("/alerts?from=${enc(stamp(fromMs))}", token)).getJSONArray("alerts").map {
        Alert(it.getString("id"), it.getString("title"), it.optString("unit"), it.optString("relative"), it.optString("description"),
            runCatching { AlertKind.valueOf(it.optString("kind", "IGNITION")) }.getOrDefault(AlertKind.IGNITION), it.optBoolean("highPriority"))
    }

    suspend fun trips(token: String, driverId: String?, from: Long, to: Long): List<Trip> {
        val q = buildString { append("/trips?from=${enc(stamp(from))}&to=${enc(stamp(to))}"); if (driverId != null) append("&driverId=${enc(driverId)}") }
        fun clock(t: String) = if (t.isBlank() || t == "null") "…" else t.substringAfter(" ", t).take(5)
        return JSONObject(call(q, token)).optJSONArray("trips")?.map {
            Trip(it.getString("id"), "${clock(it.optString("startAt"))} → ${clock(it.optString("endAt"))}", it.optString("fromAddress"), it.optString("toAddress"), it.optDouble("km", 0.0), 0, "${it.optInt("durationMinutes")} m", 0)
        } ?: emptyList()
    }

    suspend fun route(token: String, from: GeoPoint, to: GeoPoint, walking: Boolean): RouteResult {
        val j = JSONObject(call("/route?from=${from.latitude},${from.longitude}&to=${to.latitude},${to.longitude}&mode=${if (walking) "walking" else "driving"}", token))
        return RouteResult(j.getInt("distanceM"), j.getInt("durationS"), j.getJSONArray("points").map { GeoPoint(it.getDouble("lat"), it.getDouble("lng")) },
            j.getJSONArray("steps").map { RouteStep(it.optString("instruction"), it.optInt("distanceM"), it.optInt("durationS"), if (it.isNull("lat")) null else GeoPoint(it.getDouble("lat"), it.getDouble("lng"))) }, j.optString("attribution"))
    }

    private fun JSONObject.toUnit(): Unit = Unit(
        id = getString("esn"), name = optString("name").ifBlank { getString("esn") }, plate = optString("plate"), group = optString("group", "My vehicles"), driver = optString("driver"),
        status = runCatching { UnitStatus.valueOf(optString("status", "OFFLINE")) }.getOrDefault(UnitStatus.OFFLINE),
        speed = optInt("speed"), heading = optInt("heading"), lat = optDouble("lat", 0.0), lng = optDouble("lng", 0.0), address = optString("address").trim().ifBlank { "No address yet" },
        ignition = optBoolean("ignition"), battery = if (isNull("battery")) -1 else optDouble("battery").toInt(), satellites = if (isNull("satellites")) 0 else optInt("satellites"),
        lastReport = optString("lastReport", "—"), odometer = if (isNull("odometer")) 0 else optInt("odometer"), driverScore = 0, maintenanceNote = null, hasDashcam = optBoolean("hasDashcam"),
    )

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private inline fun <T> JSONArray.map(f: (JSONObject) -> T): List<T> = (0 until length()).map { f(getJSONObject(it)) }
}
