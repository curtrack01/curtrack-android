package com.curtrack.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.curtrack.app.BuildConfig
import com.curtrack.app.LocalAppState
import com.curtrack.app.LocalNav
import com.curtrack.app.toast
import com.curtrack.app.data.Broker
import com.curtrack.app.data.BrokerException
import com.curtrack.app.data.Session
import com.curtrack.app.ui.components.*
import com.curtrack.app.ui.theme.C
import com.curtrack.app.ui.theme.SansFamily
import com.curtrack.app.ui.theme.Space
import kotlinx.coroutines.delay


@Composable
fun LoginScreen() {
    val state = LocalAppState.current
    val nav = LocalNav.current
    var user by remember { mutableStateOf(if (state.session.userId == "sarah.k") "" else state.session.userId) }
    var pass by remember { mutableStateOf("") }
    var portal by remember { mutableStateOf(state.session.portalId) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var demoOpen by remember { mutableStateOf(false) }

    LaunchedEffect(attempt) {
        if (attempt == 0) return@LaunchedEffect
        busy = true; error = null
        try {
            val r = Broker.login(user.trim(), pass, portal.trim().ifBlank { "189" })
            state.startLive(r)
            nav.navigate(if (state.onboardingDone) "home" else "permissions") { popUpTo("login") { inclusive = true } }
        } catch (e: BrokerException) { error = e.message } catch (e: Exception) { error = "Can't reach CurTrack — check your connection." }
        busy = false
    }

    Screen(bodyPadding = PaddingValues(horizontal = Space.s9, vertical = 40.dp)) {
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            BrandMark(46.dp)
            Kicker("CurTrack", color = C.amber)
            Text("See every vehicle,\nright now.", color = C.ink, fontFamily = SansFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em, lineHeight = 35.sp)
        }
        Gap(8.dp)
        Card(gap = 14.dp) {
            Field(user, { user = it }, "User ID", leading = { Icon(Icons.car, null, Modifier.size(20.dp), tint = C.dim) })
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Field(pass, { pass = it }, "Password", leading = { Mono("••", 11) }, password = true, error = error != null)
                if (error != null) Sub(error!!, color = C.red)
            }
            Field(portal, { portal = it }, "Portal ID", leading = { Mono("PID", 11) })
            SwitchRow("Keep me signed in", state.keepSignedIn) { state.keepSignedIn = it; state.save() }
            PrimaryButton(if (busy) "Signing in…" else "Log in", enabled = !busy && user.isNotBlank() && pass.isNotBlank()) { attempt++ }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                Sub("Forgot ID", Modifier.clickableText { nav.navigate("forgot") })
                Sub("Forgot password", Modifier.clickableText { nav.navigate("forgot") })
            }
        }
        Card(fill = C.card2, gap = 10.dp) {
            Between { Kicker("Platform web page"); Pill("Portal 189", upper = false) }
            Sub("Commands, geofences and reports are on the CurTrack platform until the app covers them.")
            SecondaryButton("Open platform page", icon = Icons.arrowRight) { nav.navigate("platform") }
        }
        Sub("Portal ID ${portal.ifBlank { "189" }} · CurTrack dash cams & GPS", Modifier.fillMaxWidth(), align = TextAlign.Center)
        Sub(if (demoOpen) "Demo: user demo · password dashcam" else "Try the demo with sample data", Modifier.fillMaxWidth().alpha(0.6f).clickableText {
            if (demoOpen) { state.liveToken = null; state.session = Session("demo", "189", "Sarah"); state.signedIn = true; state.save(); nav.navigate(if (state.onboardingDone) "home" else "permissions") { popUpTo("login") { inclusive = true } } } else demoOpen = true
        }, align = TextAlign.Center)
        Spacer(Modifier.weight(1f))
    }
}

fun Modifier.clickableText(onClick: () -> kotlin.Unit): Modifier = this.clickable(onClick = onClick)

@Composable
fun ForgotScreen() {
    val nav = LocalNav.current
    var user by remember { mutableStateOf("sarah.k") }
    var portal by remember { mutableStateOf("189") }
    var method by remember { mutableIntStateOf(0) }
    var sent by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    val toast = toast()
    Screen(onBack = { nav.popBackStack() }, bodyPadding = PaddingValues(horizontal = Space.s9, vertical = 10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Kicker("CurTrack", color = C.amber)
            Text("Reset your password", color = C.ink, fontFamily = SansFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em, lineHeight = 35.sp)
            Sub("Enter your User ID and Portal ID. We'll send a reset code to the phone number or email your dealer registered.")
        }
        Card(gap = 14.dp) {
            Field(user, { user = it }, "User ID", leading = { Icon(Icons.car, null, Modifier.size(20.dp), tint = C.dim) })
            Field(portal, { portal = it }, "Portal ID", leading = { Mono("PID", 11) })
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Kicker("Send code by")
                Segmented(listOf("SMS · ···· 4471", "Email · s···@gmail.com"), method) { method = it }
            }
            PrimaryButton("Send reset code") { sent = true; toast("Code sent (mock: 481920)") }
        }
        if (sent) Card(gap = 14.dp) {
            Between { Kicker("Enter the 6-digit code"); Mono("expires in 9:41") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { i ->
                    val ch = code.getOrNull(i)?.toString() ?: ""
                    val active = i == code.length
                    Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp)).background(C.card2).border(1.dp, if (active) C.amber else C.line, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Mono(if (active) "|" else ch, 22, color = if (active) C.amber else C.ink)
                    }
                }
            }
            Field(code, { if (it.length <= 6 && it.all(Char::isDigit)) code = it; if (it == "481920") { toast("Password reset — log in with your new password"); nav.popBackStack() } }, "Type the code")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                Sub("Resend code", Modifier.clickableText { toast("Code re-sent") })
                Sub("Use a different method", Modifier.clickableText { sent = false })
            }
        }
        Sub("Can't get a code? Call your dealer or CurTrack support\n${BuildConfig.SUPPORT_PHONE} · ${BuildConfig.SUPPORT_EMAIL}", Modifier.fillMaxWidth(), align = TextAlign.Center)
    }
}

@Composable
fun PermissionsScreen() {
    val state = LocalAppState.current
    val nav = LocalNav.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) {
        state.onboardingDone = true; state.save()
        nav.navigate("home") { popUpTo("permissions") { inclusive = true } }
    }
    Screen(bodyPadding = PaddingValues(horizontal = Space.s9, vertical = 40.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Kicker("Step 1 of 2 · Welcome, ${state.session.firstName}", color = C.amber)
            Text("Three things the app needs", color = C.ink, fontFamily = SansFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em, lineHeight = 35.sp)
            Sub("You can change any of these later in Android Settings. Nothing is turned on without your say-so.")
        }
        Card(gap = 14.dp) {
            PermRow(Icons.bell, C.teal, "Notifications", "For geofence, speed, impact and tow alerts. Only the categories you leave on in Settings will wake your phone.", "Asking next", C.amber)
            Divider()
            PermRow(Icons.target, C.amber, "Your location · while using the app", "Only for \"Find my car\" walking guidance and to centre the map on you. We never track your phone in the background.", "Asking next", C.amber)
            Divider()
            PermRow(Icons.lock, C.dim, "Screen lock for commands", "Starter disable and other commands ask for your PIN or fingerprint before they are sent.", "Optional", C.dim)
        }
        Card(fill = C.card2, gap = 10.dp) {
            Kicker("Not asked for")
            Sub("Background location · Contacts · Camera roll. The app doesn't need them and won't ask.")
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("Continue") {
            val perms = buildList {
                if (android.os.Build.VERSION.SDK_INT >= 33) add(android.Manifest.permission.POST_NOTIFICATIONS)
                add(android.Manifest.permission.ACCESS_FINE_LOCATION); add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            launcher.launch(perms.toTypedArray())
        }
        Sub("Skip for now", Modifier.fillMaxWidth().clickableText { state.onboardingDone = true; state.save(); nav.navigate("home") { popUpTo("permissions") { inclusive = true } } }, align = TextAlign.Center)
    }
}

@Composable
private fun PermRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, title: String, body: String, pill: String, pillColor: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Avatar(tint) { Icon(icon, null, Modifier.size(20.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Title(title)
            Sub(body)
            Gap(4.dp)
            Pill(pill, text = pillColor, border = pillColor, upper = false)
        }
    }
}
