package com.curtrack.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.curtrack.app.BuildConfig
import com.curtrack.app.LocalAppState
import com.curtrack.app.LocalNav
import com.curtrack.app.goTab
import com.curtrack.app.toast
import com.curtrack.app.data.Alert
import com.curtrack.app.data.AlertKind
import com.curtrack.app.data.MockData
import com.curtrack.app.ui.components.*
import com.curtrack.app.ui.theme.C

/* ───────────────────────── Alerts (§2.6) ───────────────────────── */

@Composable
fun alertColor(k: AlertKind) = when (k) {
    AlertKind.GEOFENCE -> C.violet; AlertKind.SPEED, AlertKind.IMPACT -> C.red; AlertKind.IDLE, AlertKind.BATTERY -> C.amber
    AlertKind.IGNITION -> C.teal; AlertKind.MAINTENANCE -> C.dim
}

@Composable
fun AlertsScreen() {
    val nav = LocalNav.current
    val state = LocalAppState.current
    val toast = toast()
    var filter by remember { mutableStateOf("All") }
    var handled by remember { mutableStateOf(setOf<String>()) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLive) { if (state.isLive) { loading = true; state.refreshAlerts(); loading = false } }
    val list = state.alerts.filter { a ->
        when (filter) {
            "All" -> true; "Unread" -> a.highPriority && a.id !in handled
            "Geofence" -> a.kind == AlertKind.GEOFENCE; "Speed" -> a.kind == AlertKind.SPEED
            "Dash cam" -> a.kind == AlertKind.IMPACT; "Maintenance" -> a.kind == AlertKind.MAINTENANCE; else -> true
        }
    }
    Screen(kicker = "Push notifications", title = "Alerts", tab = Tab.ALERTS, onTab = { nav.goTab(it) }, trailing = { IconButton(Icons.check, "Clear all") { handled = state.alerts.map { it.id }.toSet(); toast("All alerts cleared") } }) {
        ChipRow(listOf("All", "Unread", "Geofence", "Speed", "Dash cam", "Maintenance"), filter) { filter = it }
        if (loading) Sub("Loading events from the platform…", Modifier.fillMaxWidth().padding(vertical = 12.dp), align = TextAlign.Center)
        else if (list.isEmpty()) Sub("No alerts in the last 7 days", Modifier.fillMaxWidth().padding(vertical = 40.dp), align = TextAlign.Center)
        list.forEach { a -> AlertCard(a, a.id in handled, onHandled = { handled = handled + a.id }, onMap = { nav.goTab(Tab.LIVE) }) }
    }
}

@Composable
private fun AlertCard(a: Alert, handled: Boolean, onHandled: () -> kotlin.Unit, onMap: () -> kotlin.Unit) {
    val nav = LocalNav.current
    val hot = a.highPriority && !handled
    Card(border = if (hot) C.red.copy(alpha = 0.35f) else null, gap = 10.dp, onClick = { if (a.kind == AlertKind.IMPACT) nav.navigate("clips/u1") else onMap() }) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Dot(alertColor(a.kind))
            Column(Modifier.weight(1f)) { Between { Title(a.title); Mono(a.relative) }; Sub("${a.unit} · ${a.description}") }
        }
        if (a.kind == AlertKind.IMPACT) Hatch(88.dp, "DASHCAM CLIP · 12 s", radius = 12.dp)
        if (hot) TwoUp { SecondaryButton("Show on map", compact = true, onClick = onMap); SecondaryButton("Mark handled", compact = true, onClick = onHandled) }
    }
}

/* ───────────────────────── Reports (§2.7) ───────────────────────── */

@Composable
fun ReportsScreen() {
    val nav = LocalNav.current
    val toast = toast()
    Screen(kicker = "Run on any unit or group", title = "Reports", tab = Tab.REPORTS, onTab = { nav.goTab(it) }, trailing = { IconButton(Icons.clock, "Scheduled reports") { toast("Scheduled reports") } }) {
        Card(fill = C.card2, gap = 10.dp, onClick = { nav.navigate("report/Mileage") }) {
            Kicker("Saved")
            Between { Column { Title("Weekly fleet mileage"); Sub("Every Monday 06:00 · emailed as PDF") }; Chevron() }
        }
        Card {
            Kicker("Report types")
            MockData.reportTypes.forEachIndexed { i, r ->
                ListRow(last = i == MockData.reportTypes.lastIndex, onClick = { nav.navigate("report/${r.name}") }, avatar = { Avatar(C.amber) { Icon(Icons.chart, null, Modifier.size(21.dp)) } }) { Title(r.name); Sub(r.description) }
            }
        }
    }
}

@Composable
fun ReportScreen(name: String) {
    val state = LocalAppState.current
    val nav = LocalNav.current
    val toast = toast()
    val fleet = state.fleetScope
    Screen(kicker = "Today · ${state.scopeLabel}", title = name, onBack = { nav.popBackStack() }, tab = Tab.REPORTS, onTab = { nav.goTab(it) }, trailing = { IconButton(Icons.share, "Share or export") { toast("Exported as PDF") } }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatTile(if (fleet) "812" else "37.4", "km driven"); StatTile(if (fleet) "96" else "3", "trips") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatTile(if (fleet) "4 h" else "11 m", "idle time"); StatTile(if (fleet) "23" else "2", "speed events") }
        Card {
            Kicker("By day")
            Sparkline(MockData.byDay, 74.dp, highlight = { i, _ -> i == 4 })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { "MTWTFSS".forEach { Mono(it.toString()) } }
        }
        Card {
            Kicker("Per unit")
            state.units.forEachIndexed { i, u ->
                ListRow(last = i == state.units.lastIndex, trailing = null) {
                    Between {
                        Column { Title(u.name, 14); Sub("${u.group} · ${u.driver}") }
                        Column(horizontalAlignment = Alignment.End) { Mono(state.km(60.0 + u.odometer % 90), 15, color = C.ink); Mono("${2 + u.odometer % 5} trips") }
                    }
                }
            }
        }
        PrimaryButton("Email this report") { toast("Emailed to ${state.session.userId}@curtrack") }
    }
}

/* ───────────────────────── Geofence builder (§2.8) ───────────────────────── */

@Composable
fun GeofenceScreen() {
    val state = LocalAppState.current
    val nav = LocalNav.current
    val toast = toast()
    var name by remember { mutableStateOf("North depot") }
    var radius by remember { mutableFloatStateOf(520f) }
    var center by remember { mutableStateOf(MockData.northDepot) }
    var circle by remember { mutableStateOf(true) }
    var entry by remember { mutableStateOf(true) }; var exit by remember { mutableStateOf(true) }; var outside by remember { mutableStateOf(false) }
    var applies by remember { mutableStateOf(state.units.map { it.id }.toSet()) }
    val valid = name.isNotBlank() && (entry || exit || outside) && applies.isNotEmpty()

    Screen(kicker = "Tap the map to place", title = "New geofence", onBack = { nav.popBackStack() }, scroll = true,
        bodyPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp),
        trailing = { IconButton(Icons.check, "Save geofence", fill = if (valid) C.amber else C.card, tint = if (valid) C.onAccent else C.dim) { if (valid) { toast("Geofence “$name” saved"); nav.popBackStack() } } }) {
        Box(Modifier.fillMaxWidth().height(300.dp)) { CurMap(center = center, zoom = 13.0, geofence = center to radius.toDouble(), geofenceColor = C.amber, onMapClick = { center = it; toast("Centre moved") }) }
        Column(Modifier.padding(horizontal = 18.dp).padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card {
                Field(name, { name = it }, "Geofence name", leading = { Icon(Icons.target, null, Modifier.size(18.dp), tint = C.dim) })
                Column {
                    Between { Kicker("Radius"); Mono("${radius.toInt()} m", 11, color = C.ink) }
                    Slider(radius, { radius = it }, valueRange = 100f..2000f, colors = SliderDefaults.colors(thumbColor = C.amber, activeTrackColor = C.amber, inactiveTrackColor = C.faint))
                }
                TwoUp { SecondaryButton("Circle", selected = circle) { circle = true }; SecondaryButton("Polygon", selected = !circle) { circle = false; toast("Polygon: tap to add vertices (prototype)") } }
            }
            Card {
                Kicker("Notify me on")
                SwitchRow("Entry", entry) { entry = it }; SwitchRow("Exit", exit) { exit = it }; SwitchRow("Only outside work hours", outside) { outside = it }
            }
            Card {
                Kicker("Applies to")
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { state.units.forEach { u -> Chip(u.name, u.id in applies) { applies = if (u.id in applies) applies - u.id else applies + u.id } } }
            }
            if (!valid) Sub("Name, at least one trigger and one unit are required before saving.")
        }
    }
}

/* ───────────────────────── Settings (§2.9) ───────────────────────── */

@Composable
fun SettingsScreen() {
    val state = LocalAppState.current
    val nav = LocalNav.current
    val toast = toast()
    Screen(kicker = "Portal ID ${state.session.portalId} · ${state.session.userId}", title = "Settings", tab = Tab.MORE, onTab = { nav.goTab(it) }, trailing = { BrandMark(22.dp) }) {
        Card {
            Between { Kicker("Refresh rate"); Mono("${state.refreshRate} s", 11, color = C.ink) }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf(10, 30, 60, 120).forEach { r -> Chip("${r}s", state.refreshRate == r) { state.refreshRate = r; state.save() } } }
            Sub("Faster refresh uses more battery on OBD-powered units.")
        }
        Card {
            Kicker("Push alerts")
            listOf("geofence" to "Geofence entry / exit", "speed" to "Speed limit", "idle" to "Excessive idling", "impact" to "Impact & dash cam events", "tow" to "Tow / unauthorised move", "maint" to "Maintenance due")
                .forEach { (k, l) -> SwitchRow(l, state.pushToggles[k] == true) { state.togglePush(k, it) } }
            Sub("Alert rules themselves are configured once on the platform; this app controls delivery to this phone.")
        }
        Card {
            Kicker("Appearance & units")
            Between { Body("Theme"); Segmented(listOf("Dark", "Light"), if (state.theme == "light") 1 else 0, Modifier.width(150.dp)) { state.theme = if (it == 1) "light" else "dark"; state.save() } }
            Between { Body("Distance"); Segmented(listOf("km", "mi"), if (state.distanceUnit == "mi") 1 else 0, Modifier.width(120.dp)) { state.distanceUnit = if (it == 1) "mi" else "km"; state.save() } }
        }
        Card(gap = 0.dp) {
            val rows = listOf("Live platform (real vehicles)" to "platform", "Units & groups" to "units", "Landmarks" to "landmarks", "Geofences" to "geofences", "Proximity" to null, "Drivers ID" to "driver/u1", "Command log" to "commandlog", "Help & support" to "help", "Log out" to "logout")
            rows.forEachIndexed { i, (label, route) ->
                ListRow(last = i == rows.lastIndex, onClick = {
                    when (route) { null -> toast("$label — coming with the platform API"); "logout" -> { state.logout(); nav.navigate("login") { popUpTo(0) } }; else -> nav.navigate(route) }
                }) { Title(label) }
            }
        }
        Sub("CurTrack app · ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${if (state.isLive) "live" else "demo"}", Modifier.fillMaxWidth(), align = TextAlign.Center)
    }
}
