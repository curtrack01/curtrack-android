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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.curtrack.app.BuildConfig
import com.curtrack.app.LocalAppState
import com.curtrack.app.LocalNav
import com.curtrack.app.goTab
import com.curtrack.app.toast
import com.curtrack.app.data.CommandState
import com.curtrack.app.data.MockData
import com.curtrack.app.data.UnitStatus
import com.curtrack.app.ui.components.*
import com.curtrack.app.ui.theme.C
import com.curtrack.app.ui.theme.Radius
import com.curtrack.app.ui.theme.Space
import kotlinx.coroutines.delay
import org.osmdroid.util.GeoPoint

/* ───────────────────────── Driver detail (§2.10) ───────────────────────── */

@Composable
fun DriverScreen(id: String?) {
    val state = LocalAppState.current
    val nav = LocalNav.current
    val toast = toast()
    val u = LocalAppState.current.unit(id)
    Screen(kicker = "Driver · ${u.group}", title = u.driver, onBack = { nav.popBackStack() }, tab = Tab.LIVE, onTab = { nav.goTab(it) }, trailing = { IconButton(Icons.edit, "Edit driver") { toast("Edit driver") } }) {
        Card(gap = 14.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(statusColor(u.status), 56.dp) { Icon(Icons.person, null, Modifier.size(26.dp)) }
                Column(Modifier.weight(1f)) {
                    Title(u.driver); Sub("Driver ID 0412 · iButton assigned"); Gap(6.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (u.status == UnitStatus.MOVING) Pill("driving now", fill = C.teal, text = C.onAccent, border = androidx.compose.ui.graphics.Color.Transparent) else Pill("off duty")
                        Mono("${u.name} · ${state.speed(u.speed)}")
                    }
                }
            }
            StatGrid(Triple("${u.driverScore}", "Score", C.teal), Triple("37.4", "km today", null), Triple("3", "Trips", null), Triple("1", "Speeding", C.amber))
        }
        Card {
            Between { Kicker("Score · last 30 days"); Mono("▲ 4", color = C.teal) }
            Sparkline(MockData.driverScore30, 56.dp, highlight = { i, _ -> i == 8 }, base = C.teal, baseAlpha = 0.55f)
            Between { Sub("Amber days include a speeding event"); Mono("avg 88") }
        }
        Card {
            Kicker("Events this week")
            listOf(Triple("Hard braking", C.red, 2), Triple("Speeding", C.red, 1), Triple("Rapid acceleration", C.amber, 0), Triple("Excessive idling", C.amber, 0)).forEachIndexed { i, (l, c, n) ->
                ListRow(last = i == 3, trailing = { Mono("$n", 13, color = C.ink) }) { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { Dot(c); Body(l) } }
            }
        }
        Card {
            Kicker("Assigned units")
            ListRow(last = true, onClick = { nav.navigate("unit/${u.id}") }, avatar = { Avatar(statusColor(u.status)) { Icon(Icons.car, null, Modifier.size(20.dp)) } }) { Title(u.name); Sub("Primary · ${u.plate}") }
        }
        TwoUp { SecondaryButton("Trips") { nav.navigate("trips/${u.id}") }; SecondaryButton("Reassign unit") { toast("Unit picker") } }
    }
}

/* ───────────────────────── Find my car (§2.11) ───────────────────────── */

@Composable
fun FindMyCarScreen(id: String?) {
    val nav = LocalNav.current
    val toast = toast()
    val ctx = LocalContext.current
    val u = LocalAppState.current.unit(id)
    val me = GeoPoint(u.lat - 0.0011, u.lng - 0.0006)
    Box(Modifier.fillMaxSize().background(C.bg)) {
        CurMap(units = listOf(u), center = GeoPoint(u.lat - 0.0005, u.lng - 0.0003), zoom = 17.0, fitUnits = false, trail = listOf(me, GeoPoint(me.latitude, u.lng), GeoPoint(u.lat, u.lng)))
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.shadow(12.dp, RoundedCornerShape(Radius.field))) { IconButton(Icons.back, "Back", fill = C.bg2) { nav.popBackStack() } }
            Box(Modifier.shadow(12.dp, RoundedCornerShape(Radius.field))) { IconButton(Icons.compass, "Compass", fill = C.bg2) { toast("Compass mode") } }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().shadow(24.dp, RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet))
                .clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)).background(C.bg2).padding(Space.s8).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(44.dp, 4.dp).clip(CircleShape).background(C.faint))
            Between { Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Kicker("Find my car"); H2("${u.name} is 140 m away") }; StatusPill(UnitStatus.STOPPED) }
            StatGrid(Triple("140", "metres", null), Triple("2", "min walk", null), Triple("NE", "bearing", null), Triple("2h", "parked", null))
            Card(fill = C.card2, gap = 6.dp) { Title(u.address, 14); Sub("Head north on Bay St, then right on Front St W. The car is on the south side, near the hotel entrance.") }
            TwoUp {
                PrimaryButton("Start walking", Icons.arrowRight) { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${u.lat},${u.lng}(${u.name})"))) }
                SecondaryButton("Sound buzzer", Icons.bell) { toast("Unit will beep once") }
            }
            Sub("Last fix 2 h 04 m ago · ${u.satellites} satellites", Modifier.fillMaxWidth(), align = TextAlign.Center)
        }
    }
}

/* ───────────────────────── Share location (§2.12) ───────────────────────── */

@Composable
fun ShareScreen() {
    val state = LocalAppState.current
    val nav = LocalNav.current
    val toast = toast()
    val ctx = LocalContext.current
    var unit by remember { mutableStateOf(state.units.first().id) }
    var ttl by remember { mutableStateOf("1 h") }
    var speed by remember { mutableStateOf(false) }; var trail by remember { mutableStateOf(false) }
    var links by remember { mutableStateOf(MockData.shareLinks) }
    val url = "curtrack.app/l/8f2k-qv7d"
    Screen(kicker = "Live link", title = "Share location", onBack = { nav.popBackStack() }) {
        Card { Kicker("Vehicle"); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { state.units.forEach { u -> Chip(u.name, unit == u.id) { unit = u.id } } } }
        Card {
            Between { Kicker("Link expires in"); Mono(ttl, 11, color = C.ink) }
            ChipRow(listOf("15 min", "1 h", "4 h", "24 h", "Until I stop"), ttl) { ttl = it }
            Sub("Anyone with the link sees the vehicle's live position and last address, nothing else. You can revoke it at any time.")
        }
        Card {
            Kicker("What they can see")
            SwitchRow("Live position", true) { }; SwitchRow("Current speed", speed) { speed = it }; SwitchRow("Trail for this trip", trail) { trail = it }
        }
        Card(fill = C.card2, gap = 10.dp) {
            Between { Kicker("Your link"); Pill("Active", text = C.teal, border = C.teal, upper = false) }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.field)).background(C.card).padding(start = 13.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Mono(url, 12, color = C.ink, modifier = Modifier.weight(1f))
                IconButton(Icons.copy, "Copy link", ghost = true, size = 32.dp) { (ctx.getSystemService(android.content.ClipboardManager::class.java)).setPrimaryClip(android.content.ClipData.newPlainText("CurTrack link", "https://$url")); toast("Link copied") }
            }
            Sub("Expires today at 10:41 · viewed 0 times")
        }
        TwoUp {
            PrimaryButton("Share link", Icons.share) { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Track my ${state.unit(unit).name} live for $ttl: https://$url") }, "Share live location")) }
            SecondaryButton("Revoke", text = C.red) { toast("Link revoked") }
        }
        Card(gap = 0.dp) {
            Kicker("Active links", modifier = Modifier.padding(bottom = 10.dp))
            if (links.isEmpty()) Sub("No active links.")
            links.forEachIndexed { i, l ->
                ListRow(last = i == links.lastIndex, trailing = { SecondaryButton("Revoke", Modifier.width(90.dp), compact = true) { links = links - l; toast("Link to ${l.label} revoked") } }) { Title("${l.unit} → ${l.label}", 14); Sub("Expires in ${l.expiresIn} · ${l.channel}") }
            }
        }
    }
}

/* ───────────────────────── Saved clips (§2.13) ───────────────────────── */

@Composable
fun ClipsScreen(id: String?) {
    val nav = LocalNav.current
    val toast = toast()
    val u = LocalAppState.current.unit(id)
    var filter by remember { mutableStateOf("All") }
    val clips = MockData.clips.filter { filter == "All" || it.kind.equals(filter.replace("Harsh ", ""), true) }
    Screen(kicker = "${u.name} · Dash cam", title = "Saved clips", onBack = { nav.popBackStack() }, trailing = { IconButton(Icons.calendar, "Filter by date") { toast("Date filter") } }) {
        ChipRow(listOf("All", "Impact", "Manual", "Harsh braking", "Parked"), filter) { filter = it }
        clips.firstOrNull()?.let { c ->
            Card(border = if (c.kind == "impact") C.red.copy(alpha = 0.35f) else null, gap = 10.dp) {
                Hatch(180.dp, "${c.camera.uppercase()} CAMERA · ${c.durationS} s") {
                    Box(Modifier.align(Alignment.Center).size(48.dp).clip(CircleShape).background(C.amber).clickableText { toast("Playing clip (vendor stream pending)") }, contentAlignment = Alignment.Center) { Icon(Icons.play, "Play", Modifier.size(18.dp), tint = C.onAccent) }
                    if (c.gForce != null) Box(Modifier.align(Alignment.TopStart).padding(10.dp).clip(CircleShape).background(C.bg2)) { Pill("${c.kind} · ${c.gForce} g", text = C.red, border = C.red, upper = false) }
                }
                Between { Column { Title(c.whenLabel); Sub("${c.address} · ${c.speed} km/h") }; Mono(c.relative) }
                TwoUp { SecondaryButton("Download", compact = true) { toast("Downloading…") }; SecondaryButton("Share", compact = true) { toast("Share clip") }; SecondaryButton("Show trip", compact = true) { nav.navigate("trips/${u.id}") } }
            }
        }
        clips.drop(1).forEach { c ->
            Card(gap = 10.dp, onClick = { toast("Open clip") }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Hatch(64.dp, "${c.durationS} s", radius = 10.dp, modifier = Modifier.width(96.dp))
                    Column(Modifier.weight(1f)) {
                        Title(c.whenLabel, 14); Sub("${c.kind.replaceFirstChar(Char::uppercase)} · ${c.address}", maxLines = 1); Gap(6.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) { Pill(c.kind, text = if (c.kind == "manual") C.teal else C.amber, border = if (c.kind == "manual") C.teal else C.amber); Mono("${c.camera} cam") }
                    }
                    Chevron()
                }
            }
        }
        Card(fill = C.card2, gap = 6.dp) {
            Between { Kicker("Storage"); Mono("${MockData.clips.size} of 50 clips", 11, color = C.ink) }
            ProgressBar(MockData.clips.size / 50f, C.teal)
            Sub("Clips are kept on the camera's SD card for 30 days and uploaded on Wi-Fi. Impact clips are uploaded immediately over mobile data.")
        }
    }
}

/* ───────────────────────── Watch live (§2.14) ───────────────────────── */

@Composable
fun WatchLiveScreen(id: String?) {
    val nav = LocalNav.current
    val toast = toast()
    val u = LocalAppState.current.unit(id)
    var cam by remember { mutableIntStateOf(0) }
    var audio by remember { mutableStateOf(false) }; var quality by remember { mutableIntStateOf(1) }
    var left by remember { mutableIntStateOf(5 * 60) }
    LaunchedEffect(Unit) { while (left > 0) { delay(1000); left-- } }
    Screen(kicker = "${u.name} · Dash cam", title = "Watch live", onBack = { nav.popBackStack() }, trailing = { Pill("Online", text = C.teal, border = C.teal, upper = false) }) {
        Card(padding = 0.dp, gap = 0.dp) {
            Hatch(222.dp, "${listOf("FRONT", "CABIN", "REAR")[cam]} CAMERA · 720p", radius = 0.dp) {
                Row(Modifier.align(Alignment.TopStart).padding(12.dp).clip(CircleShape).background(C.bg2).padding(horizontal = 8.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { Dot(C.red, 6.dp); Mono("LIVE", color = C.red) }
                Box(Modifier.align(Alignment.TopEnd).padding(12.dp).clip(CircleShape).background(C.bg2).padding(horizontal = 8.dp, vertical = 5.dp)) { Mono(java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()), color = C.ink) }
                Box(Modifier.align(Alignment.BottomStart).padding(12.dp).clip(CircleShape).background(C.bg2).padding(horizontal = 8.dp, vertical = 5.dp)) { Mono("${u.speed} km/h · ${u.address.substringBefore(" at")}", color = C.ink) }
            }
            Column(Modifier.padding(16.dp, 14.dp, 16.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Segmented(listOf("Front", "Cabin", "Rear"), cam) { cam = it }
                TwoUp { PrimaryButton("Save 30 s clip", Icons.record) { toast("Clip saved") }; SecondaryButton("Snapshot", Icons.camera) { toast("Snapshot saved") } }
            }
        }
        Card(gap = 10.dp) {
            Between { Kicker("Stream"); Mono("LTE · 1.2 Mb/s") }
            SwitchRow("Audio", audio) { audio = it }
            Between { Body("Quality"); Segmented(listOf("Data saver", "Auto"), quality, Modifier.width(150.dp)) { quality = it } }
            Sub("Live viewing uses the vehicle's mobile data plan. Streams end automatically after 5 minutes to protect the plan.")
        }
        Card(fill = C.card2, gap = 8.dp) {
            Between { Kicker("Session ends in"); Mono("%d:%02d".format(left / 60, left % 60), 11, color = C.amber) }
            ProgressBar(1f - left / 300f)
        }
        SecondaryButton("Saved clips") { nav.navigate("clips/${u.id}") }
        Sub("Dash-cam streaming needs the camera vendor's API (handoff prerequisite #6). This screen is the UI shell.", Modifier.alpha(0.6f))
    }
}

/* ───────────────────────── Units & groups (§2.19) ───────────────────────── */

@Composable
fun UnitsScreen() {
    val nav = LocalNav.current
    val toast = toast()
    var q by remember { mutableStateOf("") }
    val units = MockData.fleet.filter { q.isBlank() || it.name.contains(q, true) || it.plate.contains(q, true) }
    Screen(kicker = "Fleet · 24 units", title = "Units & groups", onBack = { nav.popBackStack() }, tab = Tab.MORE, onTab = { nav.goTab(it) }, trailing = { IconButton(Icons.plus, "New group") { toast("New group") } }) {
        Field(q, { q = it }, "Search units or plates", leading = { Icon(Icons.search, null, Modifier.size(17.dp), tint = C.dim) })
        units.groupBy { it.group }.forEach { (group, list) ->
            Card(gap = 0.dp) {
                Between(Modifier.padding(bottom = 10.dp)) { Kicker(group); Mono("${list.size} unit${if (list.size == 1) "" else "s"}") }
                Divider()
                list.forEachIndexed { i, u ->
                    ListRow(last = i == list.lastIndex, onClick = { nav.navigate("unit/${u.id}") }, avatar = { Avatar(statusColor(u.status)) { Icon(Icons.car, null, Modifier.size(20.dp)) } }) {
                        Between { Title(u.name); StatusPill(u.status) }
                        Sub(if (u.status == UnitStatus.OFFLINE) "${u.plate} · no driver · last seen ${u.lastReport}" else "${u.plate} · ${u.driver} · ${if (u.hasDashcam) "hard-wired" else "OBD unit"}")
                    }
                }
            }
        }
        Sub("Units are added and renamed by your dealer. Groups and driver assignments can be edited here.", Modifier.fillMaxWidth(), align = TextAlign.Center)
    }
}

/* ───────────────────────── Landmarks (§2.20) ───────────────────────── */

@Composable
fun LandmarksScreen() {
    val nav = LocalNav.current
    val toast = toast()
    Screen(kicker = "Named places", title = "Landmarks", onBack = { nav.popBackStack() }, trailing = { IconButton(Icons.plus, "New landmark") { toast("New landmark") } }) {
        Card(padding = 0.dp, gap = 0.dp) { Box(Modifier.fillMaxWidth().height(200.dp)) { CurMap(units = MockData.fleet.filter { it.id in setOf("f8", "f4") } + MockData.personal.take(1), zoom = 11.0) } }
        Card(gap = 0.dp) {
            MockData.landmarks.forEachIndexed { i, l ->
                ListRow(last = i == MockData.landmarks.lastIndex, onClick = { toast(l.name) }, avatar = { Avatar { Icon(when (l.kind) { "home" -> Icons.home; "building" -> Icons.building; else -> Icons.circle }, null, Modifier.size(20.dp)) } }) {
                    Title(l.name); Sub("${l.street} · ${l.radius} m radius"); Gap(6.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (l.inside > 0) Pill("${l.inside} unit${if (l.inside == 1) "" else "s"} inside", text = C.teal, border = C.teal, upper = false) else Pill("Nobody inside", upper = false)
                        l.note?.let { Mono(it) }
                    }
                }
            }
        }
        Sub("Landmarks name places in trips and reports. To get alerts when a unit enters or leaves, make it a geofence.", Modifier.fillMaxWidth(), align = TextAlign.Center)
    }
}

/* ───────────────────────── Geofences list (§2.21) ───────────────────────── */

@Composable
fun GeofencesScreen() {
    val nav = LocalNav.current
    var filter by remember { mutableStateOf("All") }
    val list = MockData.geofences.filter { filter == "All" || (filter == "Active" && !it.paused) || (filter == "Paused" && it.paused) }
    Screen(kicker = "${MockData.geofences.count { !it.paused }} active · ${MockData.geofences.count { it.paused }} paused", title = "Geofences", onBack = { nav.popBackStack() }, trailing = { IconButton(Icons.plus, "New geofence") { nav.navigate("geofence") } }) {
        ChipRow(listOf("All", "Active", "Paused", "Household", "North route"), filter) { filter = it }
        list.forEach { g ->
            Card(gap = 10.dp, onClick = { nav.navigate("geofence") }, modifier = Modifier.alpha(if (g.paused) 0.7f else 1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(Radius.field)).background(C.card2).border(1.dp, C.line, RoundedCornerShape(Radius.field)), contentAlignment = Alignment.Center) {
                        val shape = if (g.circle) CircleShape else RoundedCornerShape(6.dp)
                        Box(Modifier.size(if (g.circle) 40.dp else 48.dp).clip(shape).background(if (g.paused) androidx.compose.ui.graphics.Color.Transparent else C.teal.copy(alpha = 0.1f)).border(1.5.dp, if (g.paused) C.dim else C.teal, shape))
                    }
                    Column(Modifier.weight(1f)) {
                        Between { Title(g.name); if (g.paused) Pill("Paused", upper = false) else Pill("Active", text = C.teal, border = C.teal, upper = false) }
                        Sub("${g.detail} · ${g.triggers}"); Gap(6.dp)
                        Mono("${g.units} unit${if (g.units == 1) "" else "s"} · ${g.lastEvent}")
                    }
                }
            }
        }
    }
}

/* ───────────────────────── Command log (§2.22) ───────────────────────── */

@Composable
fun CommandLogScreen() {
    val nav = LocalNav.current
    val toast = toast()
    var filter by remember { mutableStateOf("All") }
    val list = MockData.commandLog.filter { filter == "All" || (filter == "Mine" && it.issuedBy == "sarah.k") || (filter == "Failed" && it.state == CommandState.FAILED) || it.command == filter || it.unit == filter }
    Screen(kicker = "Audit · last 30 days", title = "Command log", onBack = { nav.popBackStack() }, trailing = { IconButton(Icons.download, "Export CSV") { toast("CSV exported") } }) {
        ChipRow(listOf("All", "Mine", "Starter disable", "Failed", "Escape", "F-150"), filter) { filter = it }
        list.groupBy { it.day }.forEach { (day, rows) ->
            Card(gap = 0.dp) {
                Kicker(day, modifier = Modifier.padding(bottom = 10.dp)); Divider()
                rows.forEachIndexed { i, e ->
                    val (icon, tint) = when {
                        e.state == CommandState.FAILED -> Icons.bell to C.dim
                        e.command.startsWith("Starter disable") -> Icons.warning to C.red
                        e.command.startsWith("Starter enable") -> Icons.check to C.teal
                        e.command.startsWith("Boost") -> Icons.bolt to C.amber
                        else -> Icons.target to C.teal
                    }
                    ListRow(last = i == rows.lastIndex, trailing = null, avatar = { Avatar(tint) { Icon(icon, null, Modifier.size(20.dp)) } }) {
                        Between { Title("${e.command} · ${e.unit}"); Mono(e.time) }
                        Sub("${e.issuedBy} · ${e.source}${e.confirmedWith?.let { " · confirmed with $it" } ?: ""}"); Gap(8.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Mono("QUEUED", color = C.teal); Mono("→"); Mono("SENT", color = C.teal); Mono("→")
                            Mono(e.detail.uppercase(), color = if (e.state == CommandState.FAILED) C.red else C.teal)
                        }
                    }
                }
            }
        }
        Sub("Commands are kept for 12 months and cannot be deleted from the app.", Modifier.fillMaxWidth(), align = TextAlign.Center)
    }
}

/* ───────────────────────── Help & support (§2.23) ───────────────────────── */

@Composable
fun HelpScreen() {
    val state = LocalAppState.current
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val toast = toast()
    val phone = BuildConfig.SUPPORT_PHONE
    fun dial() = ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.replace(" ", "")}")))
    fun whatsapp() = ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${phone.filter(Char::isDigit)}")))
    Screen(kicker = "CurTrack support", title = "Help & support", onBack = { nav.popBackStack() }) {
        Card(fill = C.card2) {
            Between { Kicker("Talk to a person"); Pill("Open now", text = C.teal, border = C.teal, upper = false) }
            Sub("Mon–Fri 9:00–18:00, Sat 10:00–14:00 · Willemstad")
            TwoUp { PrimaryButton("Call $phone", Icons.phone) { dial() }; SecondaryButton("WhatsApp", Icons.chat) { whatsapp() } }
            SecondaryButton("Book a callback") { androidx.browser.customtabs.CustomTabsIntent.Builder().build().launchUrl(ctx, Uri.parse("https://curtrack.com/support/callback")) }
        }
        Card(gap = 0.dp) {
            Kicker("Quick fixes", modifier = Modifier.padding(bottom = 10.dp)); Divider()
            val fixes = listOf(
                "How to install and update the app" to "Open the link from CurTrack in Chrome, allow installs from Chrome once, tap Install. Updates appear as a card on Home.",
                "My unit shows offline" to "Check power, then send Locate now. Most units reconnect within 10 min.",
                "I'm not getting alerts" to "Notifications permission and the six push toggles in Settings.",
                "The dash cam won't stream" to "Needs LTE signal and the vehicle's data plan to be active.",
                "Starter disable is greyed out" to "It is blocked while the unit is moving. Wait for speed 0.",
            )
            fixes.forEachIndexed { i, (t, b) -> ListRow(last = i == fixes.lastIndex, onClick = { toast(t) }) { Title(t); Sub(b) } }
        }
        Card(gap = 0.dp) {
            Kicker("Your account", modifier = Modifier.padding(bottom = 10.dp))
            listOf("User ID" to state.session.userId, "Portal ID" to state.session.portalId, "Dealer" to "CurTrack · Willemstad", "App version" to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})").forEach { (k, v) ->
                Divider(); Between(Modifier.padding(vertical = 11.dp)) { Sub(k); Mono(v, 12, color = C.ink) }
            }
        }
        Card(gap = 0.dp) {
            ListRow(onClick = { toast("Diagnostics shared") }) { Title("Send diagnostics to support") }
            ListRow(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://curtrack.com/privacy"))) }) { Title("Privacy policy") }
            ListRow(last = true, onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://curtrack.com/terms"))) }) { Title("Terms of service") }
        }
        Sub("$phone · ${BuildConfig.SUPPORT_EMAIL}", Modifier.fillMaxWidth(), align = TextAlign.Center)
    }
}
