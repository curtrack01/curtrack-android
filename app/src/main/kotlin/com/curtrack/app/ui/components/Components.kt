package com.curtrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.curtrack.app.data.UnitStatus
import com.curtrack.app.ui.theme.C
import com.curtrack.app.ui.theme.MonoFamily
import com.curtrack.app.ui.theme.Radius
import com.curtrack.app.ui.theme.SansFamily
import com.curtrack.app.ui.theme.Space

/* ───────── Typography helpers (design system type scale) ───────── */

@Composable
fun Kicker(text: String, color: Color = C.dim, modifier: Modifier = Modifier) = Text(
    text.uppercase(), modifier, color = color, fontFamily = MonoFamily, fontSize = 10.sp,
    fontWeight = FontWeight.Medium, letterSpacing = 0.24.em, lineHeight = 10.sp,
)

@Composable
fun H1(text: String, modifier: Modifier = Modifier) = Text(
    text, modifier, color = C.ink, fontFamily = SansFamily, fontSize = 26.sp, fontWeight = FontWeight.Bold,
    letterSpacing = (-0.02).em, lineHeight = 30.sp,
)

@Composable
fun H2(text: String, modifier: Modifier = Modifier) = Text(
    text, modifier, color = C.ink, fontFamily = SansFamily, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
    letterSpacing = (-0.01).em, lineHeight = 23.sp,
)

@Composable
fun Title(text: String, size: Int = 15, modifier: Modifier = Modifier, maxLines: Int = 2) = Text(
    text, modifier, color = C.ink, fontFamily = SansFamily, fontSize = size.sp, fontWeight = FontWeight.Bold,
    lineHeight = (size + 4).sp, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
)

@Composable
fun Body(text: String, modifier: Modifier = Modifier, color: Color = C.ink) = Text(
    text, modifier, color = color, fontFamily = SansFamily, fontSize = 14.sp, lineHeight = 19.sp,
)

@Composable
fun Sub(text: String, modifier: Modifier = Modifier, color: Color = C.dim, maxLines: Int = Int.MAX_VALUE, align: TextAlign? = null) = Text(
    text, modifier, color = color, fontFamily = SansFamily, fontSize = 13.sp, lineHeight = 19.sp,
    maxLines = maxLines, overflow = TextOverflow.Ellipsis, textAlign = align,
)

@Composable
fun Mono(text: String, size: Int = 10, color: Color = C.dim, modifier: Modifier = Modifier, weight: FontWeight = FontWeight.Medium) = Text(
    text, modifier, color = color, fontFamily = MonoFamily, fontSize = size.sp, fontWeight = weight, lineHeight = (size + 2).sp,
)

/* ───────── Card (§3.4) ───────── */

@Composable
fun Card(
    modifier: Modifier = Modifier,
    fill: Color? = null,
    border: Color? = null,
    gap: Dp = Space.s5,
    padding: Dp = Space.s7,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Radius.card)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill ?: C.card)
            .border(1.dp, border ?: C.line, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

@Composable
fun Between(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) = Row(
    modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, content = content,
)

@Composable
fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))

/* ───────── Status pill (§3.2) ───────── */

@Composable
fun StatusPill(status: UnitStatus) {
    val (fill, text, border) = when (status) {
        UnitStatus.MOVING -> Triple(C.teal, C.onAccent, Color.Transparent)
        UnitStatus.IDLE -> Triple(C.amber, C.onAccent, Color.Transparent)
        UnitStatus.STOPPED -> Triple(C.faint, C.ink, Color.Transparent)
        UnitStatus.OFFLINE -> Triple(Color.Transparent, C.dim, C.line)
    }
    Pill(status.name.lowercase(), fill = fill, text = text, border = border)
}

@Composable
fun Pill(label: String, fill: Color = Color.Transparent, text: Color = C.dim, border: Color = C.line, upper: Boolean = true) {
    Text(
        if (upper) label.uppercase() else label,
        Modifier.clip(CircleShape).background(fill).border(1.dp, border, CircleShape).padding(horizontal = 9.dp, vertical = 6.dp),
        color = text, fontFamily = MonoFamily, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.em, lineHeight = 10.sp, maxLines = 1,
    )
}

/* ───────── Stat tile (§3.3) ───────── */

@Composable
fun RowScope.StatTile(value: String, label: String, valueColor: Color? = null) {
    val shape = RoundedCornerShape(Radius.button)
    Column(
        Modifier.weight(1f).clip(shape).background(C.card2).border(1.dp, C.line, shape).padding(12.dp, 12.dp, 12.dp, 13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(value, color = valueColor ?: C.ink, fontFamily = MonoFamily, fontSize = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.02).em, lineHeight = 22.sp, maxLines = 1)
        Text(label.uppercase(), color = C.dim, fontFamily = MonoFamily, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.16.em, lineHeight = 11.sp, maxLines = 1)
    }
}

@Composable
fun StatGrid(vararg tiles: Triple<String, String, Color?>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.forEach { (v, l, c) -> StatTile(v, l, c) }
    }
}

/* ───────── Buttons (§3.5) ───────── */

@Composable
fun RowScope.PrimaryButton(label: String, icon: ImageVector? = null, fill: Color? = null, text: Color? = null, enabled: Boolean = true, onClick: () -> Unit) =
    PrimaryButton(label, Modifier.weight(1f), icon, fill, text, enabled, onClick)

@Composable
fun PrimaryButton(label: String, modifier: Modifier = Modifier, icon: ImageVector? = null, fill: Color? = null, text: Color? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.button)
    Row(
        modifier.fillMaxWidth().clip(shape).background((fill ?: C.amber).copy(alpha = if (enabled) 1f else 0.4f)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = text ?: C.onAccent)
        Text(label, color = text ?: C.onAccent, fontFamily = SansFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp, maxLines = 1)
    }
}

@Composable
fun RowScope.SecondaryButton(label: String, icon: ImageVector? = null, text: Color? = null, compact: Boolean = false, selected: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) =
    SecondaryButton(label, Modifier.weight(1f), icon, text, compact, selected, enabled, onClick)

@Composable
fun SecondaryButton(label: String, modifier: Modifier = Modifier, icon: ImageVector? = null, text: Color? = null, compact: Boolean = false, selected: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.button)
    Row(
        modifier.fillMaxWidth().clip(shape).background(if (selected) C.ink else C.card2).border(1.dp, if (selected) Color.Transparent else C.line, shape)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = if (compact) 11.dp else 15.dp).then(if (enabled) Modifier else Modifier.alpha(0.5f)),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (selected) C.bg else text ?: C.ink
        if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = tint)
        Text(label, color = tint, fontFamily = SansFamily, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp, maxLines = 1)
    }
}

@Composable
fun IconButton(icon: ImageVector, contentDescription: String, ghost: Boolean = false, fill: Color? = null, tint: Color? = null, size: Dp = 40.dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.field)
    Box(
        Modifier.size(size).clip(shape).background(if (ghost) Color.Transparent else fill ?: C.card)
            .border(1.dp, if (ghost || fill != null) Color.Transparent else C.line, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, Modifier.size(20.dp), tint = tint ?: C.ink) }
}

/* ───────── Chips (§3.6) ───────── */

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        Modifier.clip(CircleShape).background(if (selected) C.ink else C.card).border(1.dp, if (selected) Color.Transparent else C.line, CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
        color = if (selected) C.bg else C.dim, fontFamily = SansFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 11.sp, maxLines = 1,
    )
}

@Composable
fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(options.size) { i -> Chip(options[i], options[i] == selected) { onSelect(options[i]) } }
    }
}

/* ───────── Segmented control ───────── */

@Composable
fun Segmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(Radius.field)
    Row(modifier.clip(shape).background(C.card).border(1.dp, C.line, shape).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Text(
                label,
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(if (on) C.ink else Color.Transparent).clickable { onSelect(i) }.padding(horizontal = 10.dp, vertical = 9.dp),
                color = if (on) C.bg else C.dim, fontFamily = SansFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 12.sp, textAlign = TextAlign.Center, maxLines = 1,
            )
        }
    }
}

/* ───────── Switch (§3.7) ───────── */

@Composable
fun CurSwitch(on: Boolean, onToggle: (Boolean) -> Unit) {
    Box(
        Modifier.size(44.dp, 26.dp).clip(CircleShape).background(if (on) C.teal else C.faint).clickable { onToggle(!on) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.offset(x = if (on) 21.dp else 3.dp).size(20.dp).clip(CircleShape).background(if (on) C.onAccent else C.ink))
    }
}

@Composable
fun SwitchRow(label: String, on: Boolean, onToggle: (Boolean) -> Unit) = Between {
    Body(label, Modifier.weight(1f))
    CurSwitch(on, onToggle)
}

/* ───────── Avatar + list row (§3.1) ───────── */

@Composable
fun Avatar(tint: Color = C.ink, size: Dp = 44.dp, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(if (size > 50.dp) 18.dp else Radius.field)
    Box(Modifier.size(size).clip(shape).background(C.card2).border(1.dp, C.line, shape), contentAlignment = Alignment.Center) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides tint) { content() }
    }
}

@Composable
fun ListRow(last: Boolean = false, onClick: (() -> Unit)? = null, avatar: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = { Chevron() }, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            if (avatar != null) avatar()
            Column(Modifier.weight(1f), content = content)
            if (trailing != null) trailing()
        }
        if (!last) Divider()
    }
}

@Composable
fun Chevron() = Icon(Icons.chevron, null, Modifier.size(17.dp), tint = C.ink.copy(alpha = 0.5f))

@Composable
fun Dot(color: Color, size: Dp = 8.dp) = Box(Modifier.size(size).clip(CircleShape).background(color))

/* ───────── Bars / sparklines (§3.8) ───────── */

@Composable
fun Sparkline(values: List<Float>, height: Dp = 34.dp, highlight: (Int, Float) -> Boolean = { _, _ -> false }, base: Color? = null, baseAlpha: Float = 1f) {
    val max = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            val hi = highlight(i, v)
            Box(Modifier.weight(1f).height(height * (v / max)).clip(RoundedCornerShape(2.dp)).background(if (hi) C.amber else (base ?: C.faint).copy(alpha = if (hi) 1f else baseAlpha)))
        }
    }
}

@Composable
fun ProgressBar(fraction: Float, color: Color? = null) {
    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(C.faint)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).background(color ?: C.amber))
    }
}

/* ───────── Hatched placeholder (dash-cam) ───────── */

@Composable
fun Hatch(height: Dp, label: String, radius: Dp = Radius.field, modifier: Modifier = Modifier, overlay: (@Composable androidx.compose.foundation.layout.BoxScope.() -> Unit)? = null) {
    val shape = RoundedCornerShape(radius)
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(C.card2).border(1.dp, C.line, shape), contentAlignment = Alignment.Center) {
        Mono(label, 11)
        overlay?.invoke(this)
    }
}

/* ───────── Text field ───────── */

@Composable
fun Field(value: String, onChange: (String) -> Unit, placeholder: String, leading: (@Composable () -> Unit)? = null, password: Boolean = false, error: Boolean = false, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Radius.field)
    Row(
        modifier.fillMaxWidth().clip(shape).background(C.card2).border(1.dp, if (error) C.red else C.line, shape).padding(horizontal = 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) Box(Modifier.width(20.dp), contentAlignment = Alignment.Center) { leading() }
        androidx.compose.foundation.text.BasicTextField(
            value, onChange, Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(color = C.ink, fontFamily = SansFamily, fontSize = 14.sp),
            singleLine = true,
            visualTransformation = if (password) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(C.amber),
            decorationBox = { inner -> Box { if (value.isEmpty()) Sub(placeholder); inner() } },
        )
    }
}

@Composable
fun Gap(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun BrandMark(size: Dp = 22.dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val stroke = this.size.minDimension * 0.10f
        val r = this.size.minDimension * 0.40f
        drawArc(
            color = Color(0xFFF2A13C), startAngle = -100f, sweepAngle = 251f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(center.x - r, center.y - r),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
        drawCircle(Color(0xFF57C9C1), radius = this.size.minDimension * 0.14f)
    }
}

@Composable
fun ColumnScope.TwoUp(content: @Composable RowScope.() -> Unit) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
