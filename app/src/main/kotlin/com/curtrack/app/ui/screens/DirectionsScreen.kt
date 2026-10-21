package com.curtrack.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.curtrack.app.LocalAppState
import com.curtrack.app.LocalNav
import com.curtrack.app.data.Broker
import com.curtrack.app.data.RouteResult
import com.curtrack.app.toast
import com.curtrack.app.ui.components.*
import com.curtrack.app.ui.theme.C
import com.curtrack.app.ui.theme.Radius
import com.curtrack.app.ui.theme.Space
import kotlinx.coroutines.delay
import org.osmdroid.util.GeoPoint

/** In-app directions from the phone to the vehicle: route drawn on our own map, step list, ETA. Routing via the broker (OSRM). */
@Composable
fun DirectionsScreen(id: String?) {
    val state = LocalAppState.current
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val toast = toast()
    val u = state.unit(id)
    val target = GeoPoint(u.lat, u.lng)
    var walking by remember { mutableStateOf(false) }
    var me by remember { mutableStateOf<GeoPoint?>(null) }
    var route by remember { mutableStateOf<RouteResult?>(null) }
    var status by remember { mutableStateOf("Finding your location…") }
    var permission by remember { mutableIntStateOf(if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) 1 else 0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission = if (it.values.any { v -> v }) 1 else 2 }
    LaunchedEffect(Unit) { if (permission == 0) launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }

    // Poll the phone's last known location every few seconds while the screen is open (no background tracking).
    LaunchedEffect(permission) {
        if (permission != 1) { if (permission == 2) status = "Location permission denied — allow it in Android settings to get directions."; return@LaunchedEffect }
        val lm = ctx.getSystemService(LocationManager::class.java)
        while (true) {
            val loc = runCatching { listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER).mapNotNull { @Suppress("MissingPermission") lm.getLastKnownLocation(it) }.maxByOrNull { it.time } }.getOrNull()
            if (loc != null) { val p = GeoPoint(loc.latitude, loc.longitude); if (me == null || me!!.distanceToAsDouble(p) > 25) me = p }
            else status = "Waiting for a GPS fix — open Google Maps once if this takes long."
            delay(5000)
        }
    }
    LaunchedEffect(me, walking) {
        val from = me ?: return@LaunchedEffect
        val token = state.liveToken
        status = "Calculating route…"
        route = if (token == null) null else runCatching { Broker.route(token, from, target, walking) }.getOrElse { status = it.message ?: "Routing failed"; null }
        if (route != null) status = ""
        else if (token == null) status = "Directions need a live login."
    }
    val km = route?.let { "%.1f km".format(it.distanceM / 1000.0) } ?: "—"
    val mins = route?.let { "${(it.durationS + 30) / 60} min" } ?: "—"
    val sheetH = 400.dp

    Box(Modifier.fillMaxSize().background(C.bg)) {
        CurMap(units = listOf(u).filter { it.lat != 0.0 || it.lng != 0.0 }, route = route?.points, myLocation = me, fitAll = true)
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.shadow(12.dp, RoundedCornerShape(Radius.field))) { IconButton(Icons.back, "Back", fill = C.bg2) { nav.popBackStack() } }
            Box(Modifier.shadow(12.dp, RoundedCornerShape(Radius.field))) { IconButton(Icons.refresh, "Recalculate", fill = C.bg2) { me = me?.let { GeoPoint(it.latitude + 1e-7, it.longitude) } } }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(sheetH).shadow(24.dp, RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet))
                .clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)).background(C.bg2).padding(horizontal = Space.s8).padding(top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(44.dp, 4.dp).clip(CircleShape).background(C.faint))
            Between { Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Kicker("Directions to"); H2(u.name) }; Segmented(listOf("Drive", "Walk"), if (walking) 1 else 0, Modifier.size(150.dp, 40.dp)) { walking = it == 1 } }
            StatGrid(Triple(km, "distance", null), Triple(mins, "ETA", C.teal), Triple(route?.steps?.size?.toString() ?: "—", "turns", null))
            if (status.isNotBlank()) Sub(status)
            Sub(u.address, maxLines = 1)
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                val steps = route?.steps ?: emptyList()
                items(steps.size) { i ->
                    val s = steps[i]
                    ListRow(last = i == steps.lastIndex, trailing = null, avatar = { Avatar(if (i == 0) C.violet else C.ink) { Mono("${i + 1}", 12, color = if (i == 0) C.violet else C.ink) } }) {
                        Body(s.instruction); if (s.distanceM > 0) Mono(if (s.distanceM >= 1000) "%.1f km".format(s.distanceM / 1000.0) else "${s.distanceM} m")
                    }
                }
            }
            TwoUp {
                SecondaryButton("Open in Google Maps", compact = true) {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${u.lat},${u.lng}&mode=${if (walking) "w" else "d"}"))) }
                        .onFailure { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${u.lat},${u.lng}?q=${u.lat},${u.lng}(${Uri.encode(u.name)})"))) }
                }
                SecondaryButton("Sound buzzer", compact = true) { toast("Buzzer is sent from the platform page") }
            }
            route?.let { Mono(it.attribution, 9) }
        }
    }
}
