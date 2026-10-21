package com.curtrack.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App preferences + session (README §5.1/§5.2), persisted in SharedPreferences.
 * The session token is mock-only here; the real one goes in EncryptedSharedPreferences once the broker exists.
 */
class AppState(context: Context) {
    private val prefs = context.getSharedPreferences("curtrack", Context.MODE_PRIVATE)

    var theme by mutableStateOf(prefs.getString("theme", "dark") ?: "dark")          // dark | light | system
    var distanceUnit by mutableStateOf(prefs.getString("distance", "km") ?: "km")   // km | mi
    var refreshRate by mutableStateOf(prefs.getInt("refresh", 30))
    var fleetScope by mutableStateOf(prefs.getBoolean("fleet", false))
    var keepSignedIn by mutableStateOf(prefs.getBoolean("keep", true))
    var onboardingDone by mutableStateOf(prefs.getBoolean("onboarded", false))
    var signedIn by mutableStateOf(prefs.getBoolean("signedIn", false))
    var dismissedUpdate by mutableStateOf(prefs.getInt("dismissedUpdate", 0))
    var session by mutableStateOf(Session(prefs.getString("userId", "sarah.k") ?: "sarah.k", prefs.getString("portalId", "189") ?: "189", "Sarah"))

    var pushToggles by mutableStateOf(
        mapOf("geofence" to true, "speed" to true, "idle" to false, "impact" to true, "tow" to true, "maint" to true)
            .mapValues { (k, d) -> prefs.getBoolean("push_$k", d) },
    )

    fun save() {
        prefs.edit()
            .putString("theme", theme).putString("distance", distanceUnit).putInt("refresh", refreshRate)
            .putBoolean("fleet", fleetScope).putBoolean("keep", keepSignedIn).putBoolean("onboarded", onboardingDone)
            .putBoolean("signedIn", signedIn && keepSignedIn).putInt("dismissedUpdate", dismissedUpdate)
            .putString("userId", session.userId).putString("portalId", session.portalId)
            .apply { pushToggles.forEach { (k, v) -> putBoolean("push_$k", v) } }
            .apply()
    }

    fun togglePush(key: String, on: Boolean) { pushToggles = pushToggles + (key to on); save() }

    val units: List<Unit> get() = if (fleetScope) MockData.fleet else MockData.personal
    val scopeLabel: String get() = if (fleetScope) "Fleet" else "My vehicles"

    fun km(value: Double): String = if (distanceUnit == "mi") String.format("%.1f mi", value * 0.621371) else String.format("%.1f km", value)
    fun speed(value: Int): String = if (distanceUnit == "mi") "${(value * 0.621371).toInt()} mph" else "$value km/h"

    fun logout() { signedIn = false; save() }
}
