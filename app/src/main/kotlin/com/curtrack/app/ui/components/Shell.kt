package com.curtrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.curtrack.app.ui.theme.C
import com.curtrack.app.ui.theme.MonoFamily
import com.curtrack.app.ui.theme.Space

enum class Tab(val label: String, val icon: ImageVector, val route: String) {
    HOME("Home", Icons.home, "home"),
    LIVE("Live", Icons.map, "live"),
    ALERTS("Alerts", Icons.bell, "alerts"),
    REPORTS("Reports", Icons.chart, "reports"),
    MORE("More", Icons.more, "settings"),
}

/** Sticky app bar: kicker + title, optional leading back and trailing slot (README §2). */
@Composable
fun AppBar(kicker: String, title: String, onBack: (() -> Unit)? = null, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = if (onBack != null) 12.dp else Space.s8, end = Space.s8, top = 6.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) IconButton(Icons.back, "Back", ghost = true, onClick = onBack)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Kicker(kicker); H2(title) }
        trailing?.invoke(this)
    }
}

/** Standard screen: status inset, app bar, scrolling body with 18dp side padding and 120dp nav clearance. */
@Composable
fun Screen(
    kicker: String? = null,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    tab: Tab? = null,
    onTab: ((Tab) -> Unit)? = null,
    scroll: Boolean = true,
    bodyPadding: PaddingValues = PaddingValues(start = Space.s8, end = Space.s8, top = 4.dp, bottom = if (tab != null) Space.navClear else 40.dp),
    body: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            if (title != null || onBack != null) AppBar(kicker ?: "", title ?: "", onBack, trailing)
            Column(
                Modifier.weight(1f).then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(bodyPadding),
                verticalArrangement = Arrangement.spacedBy(Space.s6),
                content = body,
            )
        }
        if (tab != null && onTab != null) BottomNav(tab, onTab, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun BottomNav(current: Tab, onTab: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(C.bg.copy(alpha = 0f), C.bg, C.bg)))) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp).height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { t ->
                val on = t == current
                Column(
                    Modifier.clickable { onTab(t) }.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(t.icon, null, Modifier.size(21.dp), tint = if (on) C.amber else C.dim)
                    Text(t.label.uppercase(), color = if (on) C.ink else C.dim, fontFamily = MonoFamily, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.12.em, lineHeight = 9.sp)
                }
            }
        }
        Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}
