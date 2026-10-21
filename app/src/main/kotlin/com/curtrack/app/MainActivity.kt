package com.curtrack.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.curtrack.app.data.AppState
import com.curtrack.app.ui.components.Tab
import com.curtrack.app.ui.screens.*
import com.curtrack.app.ui.theme.CurTrackTheme

val LocalAppState = staticCompositionLocalOf<AppState> { error("AppState not provided") }
val LocalNav = staticCompositionLocalOf<NavHostController> { error("Nav not provided") }

/** FragmentActivity so BiometricPrompt works on the Starter-disable confirmation. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val state = (application as CurTrackApp).state
        setContent {
            val dark = when (state.theme) { "light" -> false; "dark" -> true; else -> isSystemInDarkTheme() }
            CurTrackTheme(dark) {
                val nav = rememberNavController()
                CompositionLocalProvider(LocalAppState provides state, LocalNav provides nav) { AppNav(nav, state) }
            }
        }
    }
}

@Composable
fun toast(): (String) -> kotlin.Unit {
    val ctx = LocalContext.current
    return { msg -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
}

@Composable
private fun AppNav(nav: NavHostController, state: AppState) {
    val start = when {
        !state.signedIn -> "login"
        !state.onboardingDone -> "permissions"
        else -> "home"
    }
    NavHost(nav, startDestination = start) {
        composable("login") { LoginScreen() }
        composable("forgot") { ForgotScreen() }
        composable("permissions") { PermissionsScreen() }
        composable("home") { HomeScreen() }
        composable("live") { LiveScreen() }
        composable("unit/{id}") { UnitScreen(it.arguments?.getString("id")) }
        composable("trips/{id}") { TripsScreen(it.arguments?.getString("id")) }
        composable("alerts") { AlertsScreen() }
        composable("reports") { ReportsScreen() }
        composable("report/{name}") { ReportScreen(it.arguments?.getString("name") ?: "Trip summary") }
        composable("geofence") { GeofenceScreen() }
        composable("settings") { SettingsScreen() }
        composable("driver/{id}") { DriverScreen(it.arguments?.getString("id")) }
        composable("findmycar/{id}") { FindMyCarScreen(it.arguments?.getString("id")) }
        composable("share") { ShareScreen() }
        composable("clips/{id}") { ClipsScreen(it.arguments?.getString("id")) }
        composable("watch/{id}") { WatchLiveScreen(it.arguments?.getString("id")) }
        composable("units") { UnitsScreen() }
        composable("landmarks") { LandmarksScreen() }
        composable("geofences") { GeofencesScreen() }
        composable("commandlog") { CommandLogScreen() }
        composable("help") { HelpScreen() }
    }
}

/** Bottom-nav switch: tabs replace each other and never stack. */
fun NavHostController.goTab(tab: Tab) = navigate(tab.route) {
    popUpTo("home") { inclusive = tab == Tab.HOME; saveState = false }
    launchSingleTop = true
}
