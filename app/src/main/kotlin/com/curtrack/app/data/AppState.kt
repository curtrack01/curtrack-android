package com.curtrack.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * App preferences + session (README §5.1/§5.2), persisted in SharedPreferences.
 * The session token is mock-only here; the real one goes in EncryptedSharedPreferences once the broker exists.
 */
class AppState(context: Context) {
    private val app = context.applicationContext
    private val prefs = context.getSharedPreferences("curtrack", Context.MODE_PRIVATE)

    var theme by mutableStateOf(prefs.getString("theme", "dark") ?: "dark")          // dark | light | system
    var distanceUnit by mutableStateOf(prefs.getString("distance", "km") ?: "km")   // km | mi
    var refreshRate by mutableStateOf(prefs.getInt("refresh", 30))
    var fleetScope by mutableStateOf(prefs.getBoolean("fleet", false))
    var keepSignedIn by mutableStateOf(prefs.getBoolean("keep", true))
    var onboardingDone by mutableStateOf(prefs.getBoolean("onboarded", false))
    var signedIn by mutableStateOf(prefs.getBoolean("signedIn", false))
    var dismissedUpdate by mutableStateOf(prefs.getInt("dismissedUpdate", 0))
    var session by mutableStateOf(Session(prefs.getString("userId", "sarah.k") ?: "sarah.k", prefs.getString("portalId", "189") ?: "189", prefs.getString("firstName", "Sarah") ?: "Sarah"))

    /* Live mode — signed in to the CurTrack broker (real vehicles). Token lives in prefs for now; move to EncryptedSharedPreferences before wide release. */
    var liveToken by mutableStateOf(prefs.getString("liveToken", null))
    var liveExpiresAt by mutableStateOf(prefs.getLong("liveExpiresAt", 0L))
    var liveUnits by mutableStateOf<List<Unit>>(emptyList())
    var liveUnitConfigs by mutableStateOf<List<UnitConfig>>(emptyList())
    var liveDriverIds by mutableStateOf<List<String>>(emptyList())
    var liveAlerts by mutableStateOf<List<Alert>>(emptyList())
    var liveUpdatedAt by mutableStateOf(0L)
    var liveError by mutableStateOf<String?>(null)
    val isLive: Boolean get() = liveToken != null && liveExpiresAt > System.currentTimeMillis()

    fun startLive(r: LoginResult) {
        liveToken = r.token; liveExpiresAt = r.expiresAt; liveUnitConfigs = r.units; liveDriverIds = r.driverIds
        liveUnits = r.units.map { c -> Unit(c.esn, c.name.ifBlank { c.esn }, c.plate, c.group, "", UnitStatus.OFFLINE, 0, 0, 0.0, 0.0, "Loading…", false, -1, 0, "—", 0, 0, null, c.hasDashcam) }
        session = Session(r.userId, r.portalId, r.firstName); signedIn = true; save()
    }
    suspend fun refreshLive(): Boolean {
        val t = liveToken ?: return false
        return try { liveUnits = Broker.units(t); liveUpdatedAt = System.currentTimeMillis(); liveError = null; evaluateGeofences(liveUnits); true }
        catch (e: BrokerException) { if (e.status == 401) { liveToken = null; save() }; liveError = e.message; false }
        catch (e: Exception) { liveError = "No connection — showing last known positions"; false }
    }
    /* ── App-side geofences ── */
    var geofences by mutableStateOf(loadGeofences())
        private set
    private fun loadGeofences(): List<LocalGeofence> = runCatching {
        val a = JSONArray(prefs.getString("geofences", "[]"))
        (0 until a.length()).map { i -> val o = a.getJSONObject(i)
            LocalGeofence(o.getString("id"), o.getString("name"), o.getDouble("lat"), o.getDouble("lng"), o.getInt("radiusM"), o.optBoolean("onEntry", true), o.optBoolean("onExit", true),
                o.optJSONArray("unitIds")?.let { u -> (0 until u.length()).map { u.getString(it) } } ?: emptyList(),
                o.optJSONObject("inside")?.let { m -> m.keys().asSequence().associateWith { k -> m.getBoolean(k) } } ?: emptyMap()) }
    }.getOrDefault(emptyList())
    private fun saveGeofences(list: List<LocalGeofence>) {
        geofences = list
        prefs.edit().putString("geofences", JSONArray(list.map { g -> JSONObject().put("id", g.id).put("name", g.name).put("lat", g.lat).put("lng", g.lng).put("radiusM", g.radiusM).put("onEntry", g.onEntry).put("onExit", g.onExit).put("unitIds", JSONArray(g.unitIds)).put("inside", JSONObject(g.inside)) }).toString()).apply()
    }
    fun addGeofence(g: LocalGeofence) = saveGeofences(geofences.filter { it.id != g.id } + g)
    fun removeGeofence(id: String) = saveGeofences(geofences.filter { it.id != id })

    /** Compare every unit against every fence; on a transition, raise an in-app alert and a phone notification. */
    private fun evaluateGeofences(units: List<Unit>) {
        if (geofences.isEmpty()) return
        val now = System.currentTimeMillis()
        val fresh = mutableListOf<Alert>()
        val updated = geofences.map { g ->
            var inside = g.inside
            units.filter { u -> (g.unitIds.isEmpty() || u.id in g.unitIds) && (u.lat != 0.0 || u.lng != 0.0) }.forEach { u ->
                val d = org.osmdroid.util.GeoPoint(u.lat, u.lng).distanceToAsDouble(org.osmdroid.util.GeoPoint(g.lat, g.lng))
                val isIn = d <= g.radiusM
                val was = inside[u.id]
                if (was != null && was != isIn) {
                    val title = if (isIn) "Geofence entry" else "Geofence exit"
                    if ((isIn && g.onEntry) || (!isIn && g.onExit)) {
                        fresh += Alert("gf-${g.id}-${u.id}-$now", title, u.name, "just now", "${if (isIn) "Entered" else "Left"} “${g.name}” · ${u.address}", AlertKind.GEOFENCE, true)
                        notify(title, "${u.name} ${if (isIn) "entered" else "left"} ${g.name}")
                    }
                }
                inside = inside + (u.id to isIn)
            }
            g.copy(inside = inside)
        }
        if (updated != geofences) saveGeofences(updated)
        if (fresh.isNotEmpty()) liveAlerts = fresh + liveAlerts
    }

    private fun notify(title: String, text: String) {
        if (pushToggles["geofence"] == false) return
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(android.app.NotificationChannel("alerts", "CurTrack alerts", android.app.NotificationManager.IMPORTANCE_HIGH))
        val intent = android.app.PendingIntent.getActivity(app, 0, app.packageManager.getLaunchIntentForPackage(app.packageName), android.app.PendingIntent.FLAG_IMMUTABLE)
        val n = (if (android.os.Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(app, "alerts") else @Suppress("DEPRECATION") android.app.Notification.Builder(app))
            .setSmallIcon(com.curtrack.app.R.drawable.ic_launcher_foreground).setContentTitle(title).setContentText(text).setAutoCancel(true).setContentIntent(intent).build()
        runCatching { nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), n) }
    }

    suspend fun refreshAlerts() { val t = liveToken ?: return; runCatching { liveAlerts = Broker.alerts(t, System.currentTimeMillis() - 7L * 86400000) } }

    var pushToggles by mutableStateOf(
        mapOf("geofence" to true, "speed" to true, "idle" to false, "impact" to true, "tow" to true, "maint" to true)
            .mapValues { (k, d) -> prefs.getBoolean("push_$k", d) },
    )

    fun save() {
        prefs.edit()
            .putString("theme", theme).putString("distance", distanceUnit).putInt("refresh", refreshRate)
            .putBoolean("fleet", fleetScope).putBoolean("keep", keepSignedIn).putBoolean("onboarded", onboardingDone)
            .putBoolean("signedIn", signedIn && keepSignedIn).putInt("dismissedUpdate", dismissedUpdate)
            .putString("userId", session.userId).putString("portalId", session.portalId).putString("firstName", session.firstName)
            .putString("liveToken", liveToken).putLong("liveExpiresAt", liveExpiresAt)
            .apply { pushToggles.forEach { (k, v) -> putBoolean("push_$k", v) } }
            .apply()
    }

    fun togglePush(key: String, on: Boolean) { pushToggles = pushToggles + (key to on); save() }

    val units: List<Unit> get() = if (isLive) liveUnits else if (fleetScope) MockData.fleet else MockData.personal
    fun unit(id: String?): Unit = if (isLive) liveUnits.firstOrNull { it.id == id } ?: liveUnits.firstOrNull() ?: MockData.unit(id) else MockData.unit(id)
    val scopeLabel: String get() = if (isLive) "My vehicles" else if (fleetScope) "Fleet" else "My vehicles"
    val alerts: List<Alert> get() = if (isLive) liveAlerts else MockData.alerts

    fun km(value: Double): String = if (distanceUnit == "mi") String.format("%.1f mi", value * 0.621371) else String.format("%.1f km", value)
    fun speed(value: Int): String = if (distanceUnit == "mi") "${(value * 0.621371).toInt()} mph" else "$value km/h"

    fun logout() { signedIn = false; liveToken = null; liveUnits = emptyList(); liveAlerts = emptyList(); save() }
}
