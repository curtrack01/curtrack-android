package com.curtrack.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Line icons on a 24px grid, 1.9px stroke, round caps — the prototype's set. Tinted via LocalContentColor. */
object Icons {
    private fun stroke(name: String, width: Float = 1.9f, fill: Boolean = false, build: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = if (fill) SolidColor(Color.Black) else null,
                stroke = if (fill) null else SolidColor(Color.Black),
                strokeLineWidth = width, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero, pathBuilder = build,
            )
        }.build()

    val home = stroke("home") { moveTo(4f, 10.5f); lineTo(12f, 4f); lineTo(20f, 10.5f); verticalLineTo(20f); horizontalLineTo(4f); close() }
    val map = stroke("map") {
        moveTo(12f, 21f); curveTo(16.4f, 15.8f, 18.6f, 12.4f, 18.6f, 10f); arcTo(6.6f, 6.6f, 0f, true, false, 5.4f, 10f); curveTo(5.4f, 12.4f, 7.6f, 15.8f, 12f, 21f); close()
        moveTo(15.2f, 10f); arcTo(3.2f, 3.2f, 0f, true, true, 8.8f, 10f); arcTo(3.2f, 3.2f, 0f, true, true, 15.2f, 10f)
    }
    val bell = stroke("bell") { moveTo(6.5f, 17f); verticalLineTo(11f); arcTo(5.5f, 5.5f, 0f, false, true, 17.5f, 11f); verticalLineTo(17f); moveTo(4.5f, 17f); horizontalLineTo(19.5f); moveTo(10.2f, 20.2f); horizontalLineTo(13.8f) }
    val chart = stroke("chart") { moveTo(5f, 19f); verticalLineTo(11f); moveTo(12f, 19f); verticalLineTo(5f); moveTo(19f, 19f); verticalLineTo(14f) }
    val more = stroke("more", fill = true) {
        for (cx in listOf(5.5f, 12f, 18.5f)) { moveTo(cx + 1.8f, 12f); arcTo(1.8f, 1.8f, 0f, true, true, cx - 1.8f, 12f); arcTo(1.8f, 1.8f, 0f, true, true, cx + 1.8f, 12f) }
    }
    val back = stroke("back", 2f) { moveTo(14f, 5f); lineTo(7f, 12f); lineTo(14f, 19f) }
    val chevron = stroke("chevron", 2f) { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
    val car = stroke("car", 1.8f) { moveTo(6f, 9f); horizontalLineTo(18f); arcTo(2.5f, 2.5f, 0f, false, true, 20.5f, 11.5f); verticalLineTo(13.5f); arcTo(2.5f, 2.5f, 0f, false, true, 18f, 16f); horizontalLineTo(6f); arcTo(2.5f, 2.5f, 0f, false, true, 3.5f, 13.5f); verticalLineTo(11.5f); arcTo(2.5f, 2.5f, 0f, false, true, 6f, 9f); close(); moveTo(7f, 16f); verticalLineTo(18f); moveTo(17f, 16f); verticalLineTo(18f) }
    val search = stroke("search", 2f) { moveTo(17.5f, 11f); arcTo(6.5f, 6.5f, 0f, true, true, 4.5f, 11f); arcTo(6.5f, 6.5f, 0f, true, true, 17.5f, 11f); moveTo(16f, 16f); lineTo(20f, 20f) }
    val plus = stroke("plus", 2f) { moveTo(12f, 6f); verticalLineTo(18f); moveTo(6f, 12f); horizontalLineTo(18f) }
    val target = stroke("target") { moveTo(15f, 12f); arcTo(3f, 3f, 0f, true, true, 9f, 12f); arcTo(3f, 3f, 0f, true, true, 15f, 12f); moveTo(20f, 12f); arcTo(8f, 8f, 0f, true, true, 4f, 12f); arcTo(8f, 8f, 0f, true, true, 20f, 12f); moveTo(12f, 1f); verticalLineTo(4f); moveTo(12f, 20f); verticalLineTo(23f); moveTo(1f, 12f); horizontalLineTo(4f); moveTo(20f, 12f); horizontalLineTo(23f) }
    val layers = stroke("layers", 1.8f) { moveTo(12f, 4f); lineTo(20f, 8.5f); lineTo(12f, 13f); lineTo(4f, 8.5f); close(); moveTo(4f, 14f); lineTo(12f, 18.5f); lineTo(20f, 14f) }
    val share = stroke("share") {
        for ((cx, cy) in listOf(17f to 6f, 6.5f to 12f, 17f to 18f)) { moveTo(cx + 2.6f, cy); arcTo(2.6f, 2.6f, 0f, true, true, cx - 2.6f, cy); arcTo(2.6f, 2.6f, 0f, true, true, cx + 2.6f, cy) }
        moveTo(14.7f, 7.4f); lineTo(8.9f, 10.7f); moveTo(8.9f, 13.4f); lineTo(14.7f, 16.7f)
    }
    val play = stroke("play", fill = true) { moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close() }
    val pause = stroke("pause", fill = true) { moveTo(7f, 5f); horizontalLineTo(11f); verticalLineTo(19f); horizontalLineTo(7f); close(); moveTo(13f, 5f); horizontalLineTo(17f); verticalLineTo(19f); horizontalLineTo(13f); close() }
    val bolt = stroke("bolt", 1.8f) { moveTo(13f, 3f); lineTo(6f, 13f); horizontalLineTo(11f); lineTo(10f, 21f); lineTo(17f, 11f); horizontalLineTo(12f); close() }
    val check = stroke("check", 2.2f) { moveTo(5f, 12f); lineTo(9.5f, 16.5f); lineTo(19f, 7f) }
    val calendar = stroke("calendar", 1.8f) { moveTo(6.5f, 5f); horizontalLineTo(17.5f); arcTo(3f, 3f, 0f, false, true, 20.5f, 8f); verticalLineTo(17f); arcTo(3f, 3f, 0f, false, true, 17.5f, 20f); horizontalLineTo(6.5f); arcTo(3f, 3f, 0f, false, true, 3.5f, 17f); verticalLineTo(8f); arcTo(3f, 3f, 0f, false, true, 6.5f, 5f); close(); moveTo(3.5f, 10f); horizontalLineTo(20.5f); moveTo(8f, 3f); verticalLineTo(7f); moveTo(16f, 3f); verticalLineTo(7f) }
    val person = stroke("person", 1.8f) { moveTo(16f, 8f); arcTo(4f, 4f, 0f, true, true, 8f, 8f); arcTo(4f, 4f, 0f, true, true, 16f, 8f); moveTo(4f, 21f); curveTo(4f, 17f, 7.6f, 14f, 12f, 14f); reflectiveCurveTo(20f, 17f, 20f, 21f) }
    val edit = stroke("edit") { moveTo(4f, 20f); horizontalLineTo(8f); lineTo(18f, 10f); lineTo(14f, 6f); lineTo(4f, 16f); close(); moveTo(12.5f, 7.5f); lineTo(16.5f, 11.5f) }
    val arrowRight = stroke("arrowRight") { moveTo(3f, 12f); horizontalLineTo(21f); moveTo(13f, 6f); lineTo(19f, 12f); lineTo(13f, 18f) }
    val warning = stroke("warning") { moveTo(12f, 3f); lineTo(21f, 19f); horizontalLineTo(3f); close(); moveTo(12f, 10f); verticalLineTo(14f); moveTo(12f, 17f); verticalLineTo(17.01f) }
    val phone = stroke("phone") { moveTo(4f, 5f); horizontalLineTo(8f); lineTo(10f, 10f); lineTo(7.5f, 11.5f); arcTo(11f, 11f, 0f, false, false, 12.5f, 16.5f); lineTo(14f, 14f); lineTo(19f, 16f); verticalLineTo(20f); arcTo(2f, 2f, 0f, false, true, 17f, 22f); arcTo(16f, 16f, 0f, false, true, 2f, 7f); arcTo(2f, 2f, 0f, false, true, 4f, 5f); close() }
    val chat = stroke("chat") { moveTo(4f, 5f); horizontalLineTo(20f); verticalLineTo(16f); horizontalLineTo(8f); lineTo(4f, 20f); close() }
    val copy = stroke("copy") { moveTo(10f, 8f); horizontalLineTo(18f); arcTo(2f, 2f, 0f, false, true, 20f, 10f); verticalLineTo(18f); arcTo(2f, 2f, 0f, false, true, 18f, 20f); horizontalLineTo(10f); arcTo(2f, 2f, 0f, false, true, 8f, 18f); verticalLineTo(10f); arcTo(2f, 2f, 0f, false, true, 10f, 8f); close(); moveTo(16f, 8f); verticalLineTo(6f); arcTo(2f, 2f, 0f, false, false, 14f, 4f); horizontalLineTo(6f); arcTo(2f, 2f, 0f, false, false, 4f, 6f); verticalLineTo(14f); arcTo(2f, 2f, 0f, false, false, 6f, 16f); horizontalLineTo(8f) }
    val download = stroke("download") { moveTo(12f, 4f); verticalLineTo(15f); moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f); moveTo(5f, 19f); horizontalLineTo(19f) }
    val camera = stroke("camera") { moveTo(6f, 5f); horizontalLineTo(18f); arcTo(2f, 2f, 0f, false, true, 20f, 7f); verticalLineTo(17f); arcTo(2f, 2f, 0f, false, true, 18f, 19f); horizontalLineTo(6f); arcTo(2f, 2f, 0f, false, true, 4f, 17f); verticalLineTo(7f); arcTo(2f, 2f, 0f, false, true, 6f, 5f); close(); moveTo(4f, 15f); lineTo(9f, 11f); lineTo(13f, 14f); lineTo(20f, 8f) }
    val record = stroke("record") { moveTo(20f, 12f); arcTo(8f, 8f, 0f, true, true, 4f, 12f); arcTo(8f, 8f, 0f, true, true, 20f, 12f); moveTo(15f, 12f); arcTo(3f, 3f, 0f, true, true, 9f, 12f); arcTo(3f, 3f, 0f, true, true, 15f, 12f) }
    val wifiOff = stroke("wifiOff", 2f) { moveTo(2f, 9f); arcTo(14f, 14f, 0f, false, true, 22f, 9f); moveTo(5.5f, 12.5f); arcTo(9f, 9f, 0f, false, true, 18.5f, 12.5f); moveTo(9f, 16f); arcTo(4f, 4f, 0f, false, true, 15f, 16f); moveTo(12f, 19f); verticalLineTo(19.01f); moveTo(3f, 3f); lineTo(21f, 21f) }
    val lock = stroke("lock") { moveTo(7f, 11f); horizontalLineTo(17f); arcTo(2f, 2f, 0f, false, true, 19f, 13f); verticalLineTo(19f); arcTo(2f, 2f, 0f, false, true, 17f, 21f); horizontalLineTo(7f); arcTo(2f, 2f, 0f, false, true, 5f, 19f); verticalLineTo(13f); arcTo(2f, 2f, 0f, false, true, 7f, 11f); close(); moveTo(8f, 11f); verticalLineTo(8f); arcTo(4f, 4f, 0f, false, true, 16f, 8f); verticalLineTo(11f) }
    val fingerprint = stroke("fingerprint", 1.8f) { moveTo(7f, 12f); arcTo(5f, 5f, 0f, false, true, 17f, 12f); verticalLineTo(16f); moveTo(9.5f, 12f); verticalLineTo(17f); moveTo(14.5f, 12f); verticalLineTo(18f); moveTo(12f, 9f); verticalLineTo(19f) }
    val clock = stroke("clock", 1.8f) { moveTo(20.5f, 12f); arcTo(8.5f, 8.5f, 0f, true, true, 3.5f, 12f); arcTo(8.5f, 8.5f, 0f, true, true, 20.5f, 12f); moveTo(12f, 7.5f); verticalLineTo(12f); lineTo(15f, 14f) }
    val building = stroke("building", 1.8f) { moveTo(6f, 6f); horizontalLineTo(18f); arcTo(2f, 2f, 0f, false, true, 20f, 8f); verticalLineTo(16f); arcTo(2f, 2f, 0f, false, true, 18f, 18f); horizontalLineTo(6f); arcTo(2f, 2f, 0f, false, true, 4f, 16f); verticalLineTo(8f); arcTo(2f, 2f, 0f, false, true, 6f, 6f); close(); moveTo(8f, 10f); horizontalLineTo(16f); moveTo(8f, 14f); horizontalLineTo(13f) }
    val compass = stroke("compass") { moveTo(21f, 12f); arcTo(9f, 9f, 0f, true, true, 3f, 12f); arcTo(9f, 9f, 0f, true, true, 21f, 12f); moveTo(15.5f, 8.5f); lineTo(13.5f, 13.5f); lineTo(8.5f, 15.5f); lineTo(10.5f, 10.5f); close() }
    val circle = stroke("circle", 1.8f) { moveTo(19f, 12f); arcTo(7f, 7f, 0f, true, true, 5f, 12f); arcTo(7f, 7f, 0f, true, true, 19f, 12f) }
    val refresh = stroke("refresh") { moveTo(20f, 12f); arcTo(8f, 8f, 0f, true, true, 17.7f, 6.3f); moveTo(20f, 3f); verticalLineTo(7f); horizontalLineTo(16f) }
}
