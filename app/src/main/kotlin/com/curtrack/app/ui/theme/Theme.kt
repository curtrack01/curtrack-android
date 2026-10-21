package com.curtrack.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import com.curtrack.app.R

/** CurTrack design tokens — mirrors the CurTrack Design System (tokens.json). */
data class CurColors(
    val amber: Color = Color(0xFFF2A13C),
    val teal: Color = Color(0xFF57C9C1),
    val red: Color = Color(0xFFE2705B),
    val violet: Color = Color(0xFF8EA9F5),
    val onAccent: Color = Color(0xFF14181B),
    val offlineGrey: Color = Color(0xFF8B9499),
    val bg: Color,
    val bg2: Color,
    val card: Color,
    val card2: Color,
    val ink: Color,
    val dim: Color,
    val faint: Color,
    val line: Color,
    val isDark: Boolean,
)

val DarkColors = CurColors(
    bg = Color(0xFF0D1012), bg2 = Color(0xFF14181B), card = Color(0xFF171C1F), card2 = Color(0xFF1D2327),
    ink = Color(0xFFF2F5F4), dim = Color(0x99F2F5F4), faint = Color(0x1AF2F5F4), line = Color(0x17F2F5F4),
    isDark = true,
)

val LightColors = CurColors(
    bg = Color(0xFFF2F3F1), bg2 = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF), card2 = Color(0xFFF6F7F5),
    ink = Color(0xFF14181B), dim = Color(0x9E14181B), faint = Color(0x1414181B), line = Color(0x1A14181B),
    isDark = false,
)

object Radius {
    val chip = 8.dp; val field = 14.dp; val button = 16.dp; val card = 20.dp; val sheet = 26.dp
}

object Space {
    val s1 = 4.dp; val s2 = 6.dp; val s3 = 8.dp; val s4 = 10.dp; val s5 = 12.dp; val s6 = 14.dp
    val s7 = 16.dp; val s8 = 18.dp; val s9 = 26.dp; val navClear = 120.dp
}

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

/** Space Grotesk via the Google Fonts provider; falls back to the system sans if Play services are absent. */
val SansFamily = FontFamily(
    Font(GoogleFont("Space Grotesk"), googleFontProvider, FontWeight.Normal),
    Font(GoogleFont("Space Grotesk"), googleFontProvider, FontWeight.Medium),
    Font(GoogleFont("Space Grotesk"), googleFontProvider, FontWeight.SemiBold),
    Font(GoogleFont("Space Grotesk"), googleFontProvider, FontWeight.Bold),
)

val MonoFamily = FontFamily(
    Font(GoogleFont("JetBrains Mono"), googleFontProvider, FontWeight.Normal),
    Font(GoogleFont("JetBrains Mono"), googleFontProvider, FontWeight.Medium),
)

val LocalCurColors = staticCompositionLocalOf { DarkColors }

/** Shorthand used throughout the screens: `C.amber`, `C.card`, … */
val C: CurColors
    @Composable get() = LocalCurColors.current

@Composable
fun CurTrackTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    val scheme = if (dark) darkColorScheme(
        primary = colors.amber, onPrimary = colors.onAccent, background = colors.bg, surface = colors.card,
        onBackground = colors.ink, onSurface = colors.ink, secondary = colors.teal, error = colors.red,
    ) else lightColorScheme(
        primary = colors.amber, onPrimary = colors.onAccent, background = colors.bg, surface = colors.card,
        onBackground = colors.ink, onSurface = colors.ink, secondary = colors.teal, error = colors.red,
    )
    CompositionLocalProvider(LocalCurColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
