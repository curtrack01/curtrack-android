package com.curtrack.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.curtrack.app.data.Unit
import com.curtrack.app.data.UnitStatus
import com.curtrack.app.ui.theme.C
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import kotlin.math.max

/**
 * OpenStreetMap map via osmdroid (prototype parity, no API key). In the dark theme tiles get the
 * README §2.3 filter via a colour matrix. Production may swap this for Google Maps / MapLibre.
 */
@Composable
fun CurMap(
    modifier: Modifier = Modifier,
    units: List<Unit> = emptyList(),
    center: GeoPoint? = null,
    zoom: Double = 13.0,
    fitUnits: Boolean = units.size > 1,
    interactive: Boolean = true,
    geofence: Pair<GeoPoint, Double>? = null,
    geofenceColor: androidx.compose.ui.graphics.Color? = null,
    trail: List<GeoPoint> = emptyList(),
    trailOverLimitFrom: Int = -1,
    playhead: GeoPoint? = null,
    onUnitClick: ((Unit) -> kotlin.Unit)? = null,
    onMapClick: ((GeoPoint) -> kotlin.Unit)? = null,
) {
    val c = C
    val ctx = LocalContext.current
    val teal = c.teal.toArgb(); val amber = c.amber.toArgb(); val ink = c.ink.toArgb(); val bg2 = c.bg2.toArgb(); val grey = c.offlineGrey.toArgb(); val onAccent = c.onAccent.toArgb()
    val gfColor = (geofenceColor ?: c.teal).toArgb()
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(interactive)
                zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                isTilesScaledToDpi = true
                if (!interactive) { setOnTouchListener { _, _ -> true } }
            }
        },
        update = { map ->
            map.overlays.clear()
            // Dark-theme tile filter (invert + desaturate) — README §2.3
            if (c.isDark) {
                val cm = android.graphics.ColorMatrix(floatArrayOf(
                    -0.62f, -0.19f, -0.05f, 0f, 220f,
                    -0.19f, -0.62f, -0.05f, 0f, 220f,
                    -0.19f, -0.19f, -0.48f, 0f, 220f,
                    0f, 0f, 0f, 1f, 0f,
                ))
                map.overlayManager.tilesOverlay.setColorFilter(android.graphics.ColorMatrixColorFilter(cm))
            } else map.overlayManager.tilesOverlay.setColorFilter(null)

            geofence?.let { (gp, r) ->
                val poly = Polygon(map).apply {
                    points = Polygon.pointsAsCircle(gp, r)
                    outlinePaint.color = gfColor; outlinePaint.strokeWidth = if (geofenceColor != null) 4f else 3f
                    fillPaint.color = AColor.argb(if (geofenceColor != null) 30 else 26, AColor.red(gfColor), AColor.green(gfColor), AColor.blue(gfColor))
                }
                map.overlays.add(poly)
            }
            if (trail.size > 1) {
                map.overlays.add(Polyline(map).apply { setPoints(trail); outlinePaint.color = teal; outlinePaint.strokeWidth = 8f; outlinePaint.alpha = 230 })
                if (trailOverLimitFrom in trail.indices) map.overlays.add(Polyline(map).apply { setPoints(trail.subList(trailOverLimitFrom, trail.size)); outlinePaint.color = amber; outlinePaint.strokeWidth = 8f })
                map.overlays.add(Marker(map).apply { position = trail.first(); icon = BitmapDrawable(ctx.resources, dotBitmap(ctx, 12, teal, null)); setAnchor(0.5f, 0.5f) })
                playhead?.let { map.overlays.add(Marker(map).apply { position = it; icon = BitmapDrawable(ctx.resources, dotBitmap(ctx, 16, amber, onAccent)); setAnchor(0.5f, 0.5f) }) }
            }
            units.forEach { u ->
                val fill = when (u.status) { UnitStatus.MOVING -> teal; UnitStatus.IDLE -> amber; UnitStatus.OFFLINE -> grey; UnitStatus.STOPPED -> ink }
                val textColor = if (u.status == UnitStatus.STOPPED) bg2 else onAccent
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(u.lat, u.lng)
                    icon = BitmapDrawable(ctx.resources, markerBitmap(ctx, u, fill, textColor, bg2, ink, c.line.toArgb()))
                    setAnchor(0.5f, 0.42f)
                    title = u.name
                    setOnMarkerClickListener { _, _ -> onUnitClick?.invoke(u); true }
                })
            }
            if (onMapClick != null) map.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
                override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
                    onMapClick(mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint); return true
                }
            })
            map.invalidate()
            if (fitUnits && units.isNotEmpty()) {
                val bb = BoundingBox.fromGeoPointsSafe(units.map { GeoPoint(it.lat, it.lng) })
                map.post { map.zoomToBoundingBox(bb.increaseByScale(1.35f), false) }
            } else if (trail.size > 1 && center == null) {
                val bb = BoundingBox.fromGeoPointsSafe(trail)
                map.post { map.zoomToBoundingBox(bb.increaseByScale(1.2f), false) }
            } else {
                val ctr = center ?: units.firstOrNull()?.let { GeoPoint(it.lat, it.lng) } ?: GeoPoint(12.1224, -68.8824) // Willemstad
                map.controller.setZoom(zoom); map.controller.setCenter(ctr)
            }
        },
    )
}

/** 38dp status circle with mono speed, heading arrow when moving, name chip below (README §2.3). */
private fun markerBitmap(ctx: android.content.Context, u: Unit, fill: Int, text: Int, chipBg: Int, chipText: Int, line: Int): Bitmap {
    val d = ctx.resources.displayMetrics.density
    val size = (38 * d).toInt(); val labelH = (22 * d).toInt()
    val label = u.name
    val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10 * d; typeface = Typeface.MONOSPACE; color = chipText }
    val lw = lp.measureText(label) + 14 * d
    val w = max(size.toFloat(), lw).toInt() + (4 * d).toInt()
    val h = size + (10 * d).toInt() + labelH
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)
    val cx = w / 2f; val top = 8 * d; val cy = top + size / 2f
    if (u.status == UnitStatus.MOVING) {
        cv.save(); cv.rotate(u.heading.toFloat(), cx, cy)
        val tri = Path().apply { moveTo(cx, top - 7 * d); lineTo(cx - 6 * d, top + 1 * d); lineTo(cx + 6 * d, top + 1 * d); close() }
        cv.drawPath(tri, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill }); cv.restore()
    }
    cv.drawCircle(cx, cy + 2 * d, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.argb(90, 0, 0, 0) })
    cv.drawCircle(cx, cy, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill })
    val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11 * d; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); color = text; textAlign = Paint.Align.CENTER }
    cv.drawText(if (u.speed > 0) u.speed.toString() else "0", cx, cy + 4 * d, sp)
    val chip = RectF(cx - lw / 2, top + size + 4 * d, cx + lw / 2, top + size + 4 * d + 18 * d)
    cv.drawRoundRect(chip, 8 * d, 8 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = chipBg })
    cv.drawRoundRect(chip, 8 * d, 8 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = line; style = Paint.Style.STROKE; strokeWidth = 1 * d })
    lp.textAlign = Paint.Align.CENTER
    cv.drawText(label, cx, chip.centerY() + 3.5f * d, lp)
    return bmp
}

private fun dotBitmap(ctx: android.content.Context, dp: Int, fill: Int, ring: Int?): Bitmap {
    val d = ctx.resources.displayMetrics.density
    val s = (dp * d).toInt()
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)
    cv.drawCircle(s / 2f, s / 2f, s / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill })
    if (ring != null) cv.drawCircle(s / 2f, s / 2f, s / 2f - 1.5f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ring; style = Paint.Style.STROKE; strokeWidth = 2 * d })
    return bmp
}
