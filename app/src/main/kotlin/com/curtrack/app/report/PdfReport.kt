package com.curtrack.app.report

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.curtrack.app.data.Analytics
import com.curtrack.app.data.Unit
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a report PDF with Android's own PdfDocument (no library) and shares it — WhatsApp first, any app as fallback.
 * A4 portrait, CurTrack look: amber accents, mono figures.
 */
object PdfReport {
    private const val W = 595; private const val H = 842; private const val M = 40f
    private val AMBER = Color.parseColor("#F2A13C"); private val TEAL = Color.parseColor("#2A8F88"); private val INK = Color.parseColor("#14181B")
    private val DIM = Color.parseColor("#6B7075"); private val LINE = Color.parseColor("#E1E3E1"); private val BG2 = Color.parseColor("#F6F7F5")

    data class Section(val title: String, val headers: List<String>, val rows: List<List<String>>, val widths: List<Float>)

    fun build(ctx: Context, type: String, unit: Unit, rangeLabel: String, s: Analytics.Summary, userId: String): File {
        val doc = PdfDocument()
        var page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, doc.pages.size + 1).create()); var c = page.canvas; var y = M
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        fun text(str: String, x: Float, size: Float, color: Int = INK, bold: Boolean = false, mono: Boolean = false, align: Paint.Align = Paint.Align.LEFT) {
            p.color = color; p.textSize = size; p.textAlign = align
            p.typeface = Typeface.create(if (mono) Typeface.MONOSPACE else Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
            c.drawText(str, x, y, p)
        }
        fun newPage() { doc.finishPage(page); page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, doc.pages.size + 1).create()); c = page.canvas; y = M }
        fun ensure(h: Float) { if (y + h > H - M) newPage() }
        fun footer() { val sy = y; y = H - 20f; text("CurTrack · portal 189 · generated ${SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault()).format(Date())} · for $userId", M, 8f, DIM); y = sy }

        // Header: brand ring + title
        p.color = AMBER; p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.strokeCap = Paint.Cap.ROUND
        c.drawArc(M, y, M + 24, y + 24, -100f, 251f, false, p)
        p.style = Paint.Style.FILL; p.color = TEAL; c.drawCircle(M + 12, y + 12, 4f, p)
        y += 18; text("CURTRACK", M + 34, 9f, AMBER, bold = true, mono = true)
        y += 22; text(type, M, 22f, INK, bold = true)
        y += 18; text("${unit.name}${if (unit.plate.isNotBlank()) " · ${unit.plate}" else ""} · $rangeLabel", M, 11f, DIM)
        y += 20

        // Summary tiles
        val tiles = listOf("km driven" to "%.1f".format(s.km), "trips" to "${s.trips}", "idle" to "${s.idleMin} min", "driving" to "${s.drivingMin} min", "max speed" to "${s.maxSpeed} km/h", "speed events" to "${s.speedEvents}")
        val tw = (W - 2 * M - 5 * 8) / 6f
        tiles.forEachIndexed { i, (l, v) ->
            val x = M + i * (tw + 8)
            p.color = BG2; p.style = Paint.Style.FILL; c.drawRoundRect(x, y, x + tw, y + 52, 8f, 8f, p)
            p.color = LINE; p.style = Paint.Style.STROKE; p.strokeWidth = 1f; c.drawRoundRect(x, y, x + tw, y + 52, 8f, 8f, p); p.style = Paint.Style.FILL
            val sy = y; y += 24; text(v, x + 10, 15f, INK, bold = true, mono = true); y += 16; text(l.uppercase(), x + 10, 7f, DIM, mono = true); y = sy
        }
        y += 72

        val sections = when (type) {
            "Mileage" -> listOf(Section("By day", listOf("Day", "km", "Trips", "Max km/h", "Idle min"), s.days.map { listOf(it.label, "%.1f".format(it.km), "${it.trips}", "${it.maxSpeed}", "${it.idleMin}") }, listOf(0.3f, 0.15f, 0.15f, 0.2f, 0.2f)))
            "Idle" -> listOf(Section("Idle periods (≥ 5 min, ignition on)", listOf("From", "To", "Minutes", "Where"), s.idle.map { listOf(it.from, it.to, "${it.minutes}", it.address) }, listOf(0.12f, 0.12f, 0.14f, 0.62f)))
            "Speed limit" -> listOf(Section("Samples above 60 km/h", listOf("Time", "km/h", "Where"), s.speeding.map { listOf(it.time, "${it.speed}", it.address) }, listOf(0.15f, 0.12f, 0.73f)))
            "Start / stop" -> listOf(Section("Ignition events", listOf("Time", "Event"), s.ignition.map { listOf(it.first, it.second) }, listOf(0.2f, 0.8f)))
            else -> listOf(
                Section("Trips", listOf("Time", "From → To", "km", "Max", "Duration"), s.tripList.map { listOf(it.span, "${it.from} → ${it.to}", "%.1f".format(it.km), "${it.maxSpeed}", it.idle) }, listOf(0.17f, 0.5f, 0.1f, 0.1f, 0.13f)),
                Section("By day", listOf("Day", "km", "Trips", "Max km/h", "Idle min"), s.days.map { listOf(it.label, "%.1f".format(it.km), "${it.trips}", "${it.maxSpeed}", "${it.idleMin}") }, listOf(0.3f, 0.15f, 0.15f, 0.2f, 0.2f)),
            )
        }
        sections.forEach { sec ->
            ensure(60f); text(sec.title.uppercase(), M, 8f, DIM, mono = true); y += 14
            val cw = sec.widths.map { it * (W - 2 * M) }
            fun row(cells: List<String>, header: Boolean) {
                ensure(18f)
                var x = M
                cells.forEachIndexed { i, cell ->
                    p.textSize = if (header) 8f else 9f
                    var t = cell; while (p.measureText(t) > cw[i] - 6 && t.length > 3) t = t.dropLast(2) + "…"
                    val sy = y; y += 12; text(t, x, if (header) 8f else 9f, if (header) DIM else INK, bold = header, mono = i > 0 && !header && cell.firstOrNull()?.isDigit() == true); y = sy
                    x += cw[i]
                }
                y += 16; p.color = LINE; p.strokeWidth = 0.5f; c.drawLine(M, y - 3, W - M, y - 3, p)
            }
            row(sec.headers, true)
            if (sec.rows.isEmpty()) { ensure(16f); y += 12; text("Nothing in this period.", M, 9f, DIM); y += 10 }
            sec.rows.forEach { row(it, false) }
            y += 16
        }
        footer(); doc.finishPage(page)
        val dir = File(ctx.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val file = File(dir, "CurTrack ${type} ${unit.name} ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.pdf".replace(Regex("[^A-Za-z0-9 ._-]"), "_"))
        file.outputStream().use { doc.writeTo(it) }; doc.close()
        return file
    }

    /** Share the PDF: WhatsApp directly when installed, otherwise the system chooser. */
    fun share(ctx: Context, file: File, whatsappOnly: Boolean) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        if (whatsappOnly) {
            val wa = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { runCatching { ctx.packageManager.getPackageInfo(it, 0) }.isSuccess }
            if (wa != null) { ctx.startActivity(send.apply { setPackage(wa) }); return }
        }
        ctx.startActivity(Intent.createChooser(send, "Send report"))
    }
}
