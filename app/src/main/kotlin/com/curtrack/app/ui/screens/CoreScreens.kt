package com.curtrack.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.curtrack.app.LocalAppState
import com.curtrack.app.LocalNav
import com.curtrack.app.goTab
import com.curtrack.app.toast
import com.curtrack.app.data.MockData
import com.curtrack.app.data.Unit
import com.curtrack.app.data.UnitStatus
import com.curtrack.app.ui.components.*
import com.curtrack.app.ui.theme.C
import com.curtrack.app.ui.theme.Radius
import com.curtrack.app.ui.theme.Space
import com.curtrack.app.update.AppUpdate
import com.curtrack.app.update.UpdateChecker
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun statusColor(s: UnitStatus) = when (s) { UnitStatus.MOVING -> C.teal; UnitStatus.IDLE -> C.amber; UnitStatus.OFFLINE -> C.offlineGrey; UnitStatus.STOPPED -> C.dim }

/** Unit row — README §3.1 */
@Composable
fun UnitRow(u: Unit, last: Boolean = false, onClick: () -> kotlin.Unit) {
    val state = LocalAppState.current
    ListRow(last = last, onClick = onClick, avatar = { Avatar(statusColor(u.status)) { Icon(Icons.car, null, Modifier.size(20.dp)) } }) {
        Between { Title(u.name, maxLines = 1); Mono(if (u.speed > 0) state.speed(u.speed) else "—", 13) }
        Sub(u.address, maxLines = 1)
        Gap(6.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) { StatusPill(u.status); Mono(u.lastReport) }
    }
}

/* ───────────────────────── Home (§2.2) ───────────────────────── */

@Composable
fun HomeScreen() {
    val state = LocalAppState.current
    val nav = LocalNav.current
    val toast = toast()
    val ctx = LocalContext.current
    val units = state.units
    val hot = MockData.alerts.filter { it.highPriority && !it.acknowledged }
    var update by remember { mutableStateOf<AppUpdate?>(null) }
    LaunchedEffect(true) { update = UpdateChecker.check()?.takeIf { it.versionCode != state.dismissedUpdate || it.mandatory } }
    val day = remember { SimpleDateFormat("EEEE · H:mm", Locale.getDefault()).format(Date()) }

    Screen(kicker = day, title = "Good morning, ${state.session.firstName}", tab = Tab.HOME, onTab = { nav.goTab(it) },
        trailing = { IconButton(Icons.plus, "New geofence") { nav.navigate("geofence") } }) {

        update?.let { u ->
            Card(border = C.amber.copy(alpha = 0.6f), gap = 10.dp) {
                Between { Kicker("Update available", C.amber); Mono("v${u.versionName}") }
                Sub(u.notes.ifBlank { "A newer version of CurTrack is ready to install." })
                TwoUp {
                    PrimaryButton("Update now") { UpdateChecker.download(ctx, u); toast("Downloading ${u.versionName}…") }
                    if (!u.mandatory) SecondaryButton("Later") { state.dismissedUpdate = u.versionCode; state.save(); update = null }
                }
            }
        }

        Card(border = C.teal.copy(alpha = 0.45f), gap = 10.dp, onClick = { nav.navigate("platform") }) {
            Between { Kicker("Live vehicles", C.teal); Pill("Portal 189", upper = false) }
            Sub("The screens below show sample data. Open the CurTrack platform for your real units, trips and alerts.")
            PrimaryButton("Open live platform", icon = Icons.arrowRight) { nav.navigate("platform") }
        }

        Segmented(listOf("My vehicles · ${MockData.personal.size}", "Fleet · 24"), if (state.fleetScope) 1 else 0) { state.fleetScope = it == 1; state.save() }

        Card(gap = 14.dp) {
            Between { Kicker("Right now"); Mono("LIVE · ${state.refreshRate}s", color = C.teal) }
            StatGrid(
                Triple(units.count { it.status == UnitStatus.MOVING }.toString(), "Moving", C.teal),
                Triple(units.count { it.status == UnitStatus.IDLE }.toString(), "Idle", C.amber),
                Triple(units.count { it.status == UnitStatus.STOPPED }.toString(), "Stopped", null),
                Triple(units.count { it.status == UnitStatus.OFFLINE }.toString(), "Offline", C.dim),
            )
            SecondaryButton("Open live map") { nav.goTab(Tab.LIVE) }
        }

        if (hot.isNotEmpty()) Card(border = C.red.copy(alpha = 0.45f), gap = 10.dp, onClick = { nav.goTab(Tab.ALERTS) }) {
            Between { Kicker("Needs attention", C.red); Pill("${hot.size} new", text = C.red, border = C.red, upper = false) }
            hot.take(2).forEach { a ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Dot(C.red)
                    Column(Modifier.weight(1f)) { Title("${a.title} · ${a.unit}", 14); Sub(a.description) }
                    Mono(a.relative)
                }
            }
            SecondaryButton("Review all alerts") { nav.goTab(Tab.ALERTS) }
        }

        Card {
            Between { Kicker("Vehicles"); Mono("${units.size} of ${if (state.fleetScope) 24 else units.size}") }
            Column { units.forEachIndexed { i, u -> UnitRow(u, last = i == units.lastIndex) { nav.navigate("unit/${u.id}") } } }
        }

        TwoUp {
            Column(Modifier.weight(1f)) {
                Card(gap = 8.dp, onClick = { nav.navigate("trips/${units.first().id}") }) {
                    Kicker("Today")
                    Row(verticalAlignment = Alignment.Bottom) { Mono("37.4", 26, color = C.ink); Mono(" km", 13) }
                    Sparkline(MockData.todaySpark, highlight = { _, v -> v > 28f })
                    Sub("3 trips · 1 h 17 m driving")
                }
            }
            Column(Modifier.weight(1f)) {
                Card(gap = 10.dp, onClick = { nav.navigate("driver/${units.first().id}") }) {
                    Kicker("Driver score")
                    Row(verticalAlignment = Alignment.Bottom) { Mono("${units.first().driverScore}", 26, color = C.ink); Mono("/100", 13) }
                    ProgressBar(units.first().driverScore / 100f, C.teal)
                    Sub("2 hard brakes, 1 speeding event")
                }
            }
        }

        Card {
            Kicker("Maintenance")
            val due = units.filter { !it.maintenanceNote.isNullOrBlank() }
            if (due.isEmpty()) Sub("Nothing due.")
            due.forEach { u -> Between { Column(Modifier.weight(1f)) { Title(u.name, 14); Sub(u.maintenanceNote!!) }; IconButton(Icons.chevron, "Snooze 7 days", ghost = true, tint = C.ink.copy(alpha = .5f)) { toast("Reminder snoozed 7 days") } } }
        }

        Card(fill = C.card2) {
            Kicker("Quick actions")
            TwoUp { SecondaryButton("Share location", Icons.share) { nav.navigate("share") }; SecondaryButton("Run report", Icons.chart) { nav.goTab(Tab.REPORTS) } }
            TwoUp { SecondaryButton("Find my car", Icons.target) { nav.navigate("findmycar/${units.first().id}") }; SecondaryButton("New geofence", Icons.plus) { nav.navigate("geofence") } }
        }
    }
}

/* ───────────────────────── Live map + sheet (§2.3) ───────────────────────── */

@Composable
fun LiveScreen() {
    val state = LocalAppState.current
    val nav = LocalNav.current
    val toast = toast()
    val units = state.units
    var filter by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    var detent by remember { mutableIntStateOf(1) } // 0 peek · 1 half · 2 full
    val groups = units.map { it.group }.distinct()
    val list = units.filter { u ->
        (filter == "All" || u.status.name.equals(filter, true) || u.group == filter) &&
            (query.isBlank() || u.name.contains(query, true) || u.driver.contains(query, true) || u.address.contains(query, true))
    }

    Box(Modifier.fillMaxSize().background(C.bg)) {
        CurMap(units = units, geofence = MockData.northDepot to 520.0, onUnitClick = { nav.navigate("unit/${it.id}") })
        Column(Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.statusBars).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MapTool(Icons.layers, "Map or satellite") { toast("Satellite view") }
            MapTool(Icons.target, "Recentre") { toast("Centred on all units") }
            MapTool(Icons.plus, "Add geofence") { nav.navigate("geofence") }
        }
        val sheetH = when (detent) { 0 -> 118.dp; 1 -> 520.dp; else -> 700.dp }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(sheetH)
                .shadow(24.dp, RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet))
                .clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)).background(C.bg2),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp).clickableText { detent = (detent + 1) % 3 }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(44.dp, 4.dp).clip(CircleShape).background(C.faint))
            }
            Column(Modifier.padding(horizontal = Space.s8).padding(top = 4.dp, bottom = 92.dp)) {
                Between { Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Kicker(state.scopeLabel); H2("${list.size} unit${if (list.size == 1) "" else "s"}") }; IconButton(Icons.bolt, "Sort") { toast("Sorted by last report") } }
                Gap(12.dp)
                Field(query, { query = it }, "Search units, drivers, landmarks", leading = { Icon(Icons.search, null, Modifier.size(17.dp), tint = C.dim) })
                Gap(10.dp)
                ChipRow(listOf("All", "Moving", "Idle", "Stopped") + groups, filter) { filter = it }
                Gap(4.dp)
                LazyColumn { items(list.size) { i -> UnitRow(list[i], last = i == list.lastIndex) { nav.navigate("unit/${list[i].id}") } }; if (list.isEmpty()) item { Sub("No units match.", Modifier.padding(vertical = 18.dp)) } }
            }
        }
        BottomNav(Tab.LIVE, { nav.goTab(it) }, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MapTool(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> kotlin.Unit) =
    Box(Modifier.shadow(12.dp, RoundedCornerShape(Radius.field))) { IconButton(icon, desc, fill = C.bg2, size = 42.dp, onClick = onClick) }

/* ───────────────────────── Unit detail (§2.4) ───────────────────────── */

@Composable
fun UnitScreen(id: String?) {
    val state = LocalAppState.current
    val nav = LocalNav.current
    val toast = toast()
    val ctx = LocalContext.current
    val u = MockData.unit(id)
    var confirm by remember { mutableStateOf<String?>(null) }

    Box {
        Screen(kicker = "${u.group} · ${u.plate}", title = u.name, onBack = { nav.popBackStack() }, tab = Tab.LIVE, onTab = { nav.goTab(it) }, trailing = { StatusPill(u.status) }) {
            Card(padding = 0.dp, gap = 0.dp) {
                Box(Modifier.fillMaxWidth().height(172.dp)) { CurMap(units = listOf(u), zoom = 15.0, fitUnits = false, interactive = false) }
                Column(Modifier.padding(16.dp, 14.dp, 16.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column { Title(u.address); Sub("Last report ${u.lastReport} · ${u.satellites} satellites") }
                    StatGrid(Triple(u.speed.toString(), if (state.distanceUnit == "mi") "mph" else "km/h", null), Triple(if (u.ignition) "ON" else "OFF", "Ignition", null), Triple("${u.battery}%", "Battery", null), Triple("${u.odometer / 1000}k", "Odometer", null))
                    TwoUp {
                        PrimaryButton("Trail & trips", Icons.play) { nav.navigate("trips/${u.id}") }
                        SecondaryButton("Directions", Icons.target) { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${u.lat},${u.lng}")).apply { setPackage("com.google.android.apps.maps") }.let { i -> if (i.resolveActivity(ctx.packageManager) != null) i else Intent(Intent.ACTION_VIEW, Uri.parse("geo:${u.lat},${u.lng}?q=${u.lat},${u.lng}")) }) }
                    }
                }
            }
            if (u.hasDashcam) Card {
                Between { Kicker("Dash cam"); Pill("Online", text = C.teal, border = C.teal, upper = false) }
                Hatch(118.dp, "LIVE VIEW · tap to stream", modifier = Modifier.clickableText { nav.navigate("watch/${u.id}") })
                TwoUp { SecondaryButton("Watch live") { nav.navigate("watch/${u.id}") }; SecondaryButton("Saved clips") { nav.navigate("clips/${u.id}") } }
            }
            Card {
                Kicker("Commands")
                TwoUp { SecondaryButton("Locate now") { confirm = "Locate now" }; SecondaryButton("Starter disable") { if (u.speed > 0) toast("Blocked while the unit is moving") else confirm = "Starter disable" } }
                TwoUp { SecondaryButton("Boost to 10 s") { confirm = "Boost to 10 s" }; SecondaryButton("Sound buzzer") { confirm = "Sound buzzer" } }
                Sub("Commands are logged against your user and require confirmation.")
            }
            Card {
                Kicker("Driver")
                ListRow(last = true, onClick = { nav.navigate("driver/${u.id}") }, avatar = { Avatar { Icon(Icons.person, null, Modifier.size(20.dp)) } }) {
                    Title(u.driver); Sub("Score ${u.driverScore}/100 · ${u.maintenanceNote ?: "No maintenance due"}")
                }
            }
        }
        confirm?.let { cmd -> CommandConfirmSheet(cmd, u, onDismiss = { confirm = null }, onSent = { toast("$cmd queued → sent"); confirm = null }) }
    }
}

/* ───────────────────────── Trips & trail (§2.5) ───────────────────────── */

@Composable
fun TripsScreen(id: String?) {
    val state = LocalAppState.current
    val nav = LocalNav.current
    val toast = toast()
    val u = MockData.unit(id)
    var playing by remember { mutableStateOf(false) }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) { while (playing) { delay(60); t = (t + 0.012f) % 1f } }
    val trail = MockData.trail
    val idx = (t * (trail.size - 1)).toInt().coerceIn(0, trail.lastIndex)
    val minutes = 12 + (t * 29).toInt()
    val speed = (30 + 40 * kotlin.math.abs(kotlin.math.sin(t * 6))).toInt()

    Screen(kicker = u.name, title = "Trips & trail", onBack = { nav.popBackStack() }, tab = Tab.LIVE, onTab = { nav.goTab(it) }, trailing = { IconButton(Icons.calendar, "Date range") { toast("Date range: today") } }) {
        Card(padding = 0.dp, gap = 0.dp) {
            Box(Modifier.fillMaxWidth().height(230.dp)) { CurMap(trail = trail, trailOverLimitFrom = 8, playhead = trail[idx], fitUnits = false) }
            Column(Modifier.padding(16.dp, 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(if (playing) Icons.pause else Icons.play, if (playing) "Pause" else "Play", fill = C.amber, tint = C.onAccent, size = 42.dp) { playing = !playing }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        ProgressBar(t)
                        Between { Mono("07:12"); Mono("07:%02d · ${state.speed(speed)}".format(minutes)); Mono("07:41") }
                    }
                }
                Sub("Trail shows heading and speed; amber segments are above the posted limit.")
            }
        }
        Card {
            Between { Kicker("Today · ${MockData.trips.size} trips"); Mono(state.km(37.4)) }
            MockData.trips.forEachIndexed { i, tr ->
                ListRow(last = i == MockData.trips.lastIndex, onClick = { toast("Trip ${i + 1} loaded on the map"); t = 0f }, avatar = { Avatar(C.amber) { Mono("${i + 1}", 13, color = C.amber) } }) {
                    Title(tr.span, 14); Sub("${tr.from} → ${tr.to}", maxLines = 1); Gap(5.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Mono(state.km(tr.km)); Mono("max ${tr.maxSpeed}"); Mono("idle ${tr.idle}") }
                }
            }
        }
        Card {
            Kicker("Speed through the trip")
            Sparkline(MockData.speedProfile, 56.dp, highlight = { _, v -> v > 58f }, base = C.teal, baseAlpha = 0.55f)
            Between { Sub("Posted limit exceeded twice"); Mono("max ${state.speed(78)}", color = C.amber) }
        }
    }
}

/* ───────────────────────── Command confirmation sheet (§2.18) ───────────────────────── */

@Composable
fun CommandConfirmSheet(cmd: String, u: Unit, onDismiss: () -> kotlin.Unit, onSent: () -> kotlin.Unit) {
    val state = LocalAppState.current
    val ctx = LocalContext.current
    val dangerous = cmd == "Starter disable"
    var pin by remember { mutableStateOf("") }
    val toast = toast()
    Box(Modifier.fillMaxSize().background(C.bg.copy(alpha = 0.55f)).clickableText(onDismiss)) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().shadow(24.dp, RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet))
                .clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)).background(C.bg2).clickableText { }
                .padding(Space.s8).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(44.dp, 4.dp).clip(CircleShape).background(C.faint))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(if (dangerous) C.red else C.amber) { Icon(if (dangerous) Icons.warning else Icons.bolt, null, Modifier.size(22.dp)) }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Kicker("Command · ${u.name}", if (dangerous) C.red else C.dim); H2(if (dangerous) "Disable the starter?" else "Send “$cmd”?") }
            }
            Sub(if (dangerous) "The engine will not start until you send Starter enable. The vehicle is ${u.status.name.lowercase()} with ignition ${if (u.ignition) "on" else "off"}. The command is logged against ${state.session.userId}."
                else "The unit will receive the command on its next check-in. It is logged against ${state.session.userId}.")
            if (dangerous) Card(fill = C.card2, gap = 10.dp) {
                Between { Kicker("Confirm with PIN"); Mono("or fingerprint") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) { i ->
                        val active = i == pin.length
                        Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp)).background(C.card).border(1.dp, if (active) C.amber else C.line, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Mono(if (i < pin.length) "•" else if (active) "|" else "", 22, color = if (active) C.amber else C.ink)
                        }
                    }
                }
                Field(pin, { if (it.length <= 4 && it.all(Char::isDigit)) pin = it }, "Type your PIN", password = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.fingerprint, null, Modifier.size(16.dp), tint = C.teal)
                    Sub("Touch the sensor to confirm", Modifier.clickableText { biometric(ctx, onSent) { toast(it) } }, color = C.teal)
                }
            }
            TwoUp {
                SecondaryButton("Cancel", onClick = onDismiss)
                PrimaryButton("Send command", fill = if (dangerous) C.red else null, text = if (dangerous) C.ink else null, enabled = !dangerous || pin.length == 4, onClick = onSent)
            }
            if (dangerous) Sub("Starter disable is blocked while the unit reports speed above 0.", Modifier.fillMaxWidth(), align = TextAlign.Center)
        }
    }
}

private fun biometric(ctx: android.content.Context, onOk: () -> kotlin.Unit, onFail: (String) -> kotlin.Unit) {
    val act = ctx as? androidx.fragment.app.FragmentActivity ?: return onFail("Biometrics unavailable")
    val prompt = androidx.biometric.BiometricPrompt(act, androidx.core.content.ContextCompat.getMainExecutor(ctx), object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) = onOk()
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFail(errString.toString())
    })
    prompt.authenticate(androidx.biometric.BiometricPrompt.PromptInfo.Builder().setTitle("Confirm command").setSubtitle("CurTrack · Starter disable")
        .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL).build())
}
