package com.curtrack.app.data

import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Derived figures from raw position history — the API has no trip/idle/mileage reports, so the app computes them. */
object Analytics {
    private val hm = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val day = SimpleDateFormat("EEE d MMM", Locale.getDefault())

    fun haversineKm(a: GeoPoint, b: GeoPoint) = a.distanceToAsDouble(b) / 1000.0

    /** No driver on the account → derive trips from the trail: runs of movement separated by ≥10 min stopped. */
    fun deriveTrips(points: List<TrailPoint>): List<Trip> {
        if (points.size < 2) return emptyList()
        val out = mutableListOf<Trip>()
        var start = -1; var lastMove = 0L
        fun close(end: Int) {
            if (start < 0 || end <= start) return
            val seg = points.subList(start, end + 1)
            val km = seg.zipWithNext { p, q -> haversineKm(p.point, q.point) }.sum()
            if (km >= 0.3) out += Trip("d${out.size}", "${hm.format(Date(seg.first().at))} → ${hm.format(Date(seg.last().at))}", seg.first().address.ifBlank { "Start" }, seg.last().address.ifBlank { "End" }, Math.round(km * 10) / 10.0, seg.maxOf { it.speed }, "${((seg.last().at - seg.first().at) / 60000)} min", 0)
            start = -1
        }
        points.forEachIndexed { i, p ->
            if (p.speed > 2) { if (start < 0) start = i; lastMove = p.at }
            else if (start >= 0 && p.at - lastMove > 10 * 60000L) close(i)
        }
        close(points.lastIndex)
        return out
    }

    data class DayRow(val label: String, val km: Double, val trips: Int, val maxSpeed: Int, val idleMin: Int, val drivingMin: Int)
    data class IdlePeriod(val from: String, val to: String, val minutes: Int, val address: String)
    data class SpeedEvent(val time: String, val speed: Int, val address: String)
    data class Summary(val km: Double, val trips: Int, val idleMin: Int, val drivingMin: Int, val maxSpeed: Int, val speedEvents: Int, val days: List<DayRow>, val tripList: List<Trip>, val idle: List<IdlePeriod>, val speeding: List<SpeedEvent>, val ignition: List<Pair<String, String>>)

    fun summarise(points: List<TrailPoint>, speedLimit: Int = 60): Summary {
        val byDay = points.groupBy { Calendar.getInstance().apply { timeInMillis = it.at; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis }.toSortedMap()
        val days = byDay.map { (d, pts) ->
            val trips = deriveTrips(pts)
            val idle = idlePeriods(pts).sumOf { it.minutes }
            DayRow(day.format(Date(d)), Math.round(trips.sumOf { it.km } * 10) / 10.0, trips.size, pts.maxOfOrNull { it.speed } ?: 0, idle, trips.sumOf { it.idle.filter(Char::isDigit).toIntOrNull() ?: 0 })
        }
        val trips = deriveTrips(points)
        val idle = idlePeriods(points)
        val speeding = points.filter { it.speed > speedLimit }.map { SpeedEvent(hm.format(Date(it.at)), it.speed, it.address) }
        val ignition = points.filter { it.event.contains("ignition", true) }.map { hm.format(Date(it.at)) to it.event }
        return Summary(Math.round(trips.sumOf { it.km } * 10) / 10.0, trips.size, idle.sumOf { it.minutes }, days.sumOf { it.drivingMin }, points.maxOfOrNull { it.speed } ?: 0, speeding.size, days, trips, idle, speeding, ignition)
    }

    /** Ignition on but not moving for ≥ 5 min. Events named "Idle"/"Ignition On" between fixes count too. */
    fun idlePeriods(points: List<TrailPoint>): List<IdlePeriod> {
        val out = mutableListOf<IdlePeriod>()
        var start: TrailPoint? = null
        points.forEach { p ->
            val stopped = p.speed <= 2 && !p.event.contains("ignition off", true) && !p.event.contains("heartbeat", true)
            if (stopped) { if (start == null) start = p }
            else { start?.let { s -> val m = ((p.at - s.at) / 60000).toInt(); if (m >= 5) out += IdlePeriod(hm.format(Date(s.at)), hm.format(Date(p.at)), m, s.address) }; start = null }
        }
        return out
    }
}
