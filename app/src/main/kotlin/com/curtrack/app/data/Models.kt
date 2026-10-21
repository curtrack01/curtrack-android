package com.curtrack.app.data

/** Domain models — README §5.3. Platform vocabulary: Unit, Group, Driver, Landmark, Geofence, Command, Trail, Trip. */

enum class UnitStatus { MOVING, IDLE, STOPPED, OFFLINE }

data class Unit(
    val id: String, val name: String, val plate: String, val group: String, val driver: String,
    val status: UnitStatus, val speed: Int, val heading: Int, val lat: Double, val lng: Double,
    val address: String, val ignition: Boolean, val battery: Int, val satellites: Int,
    val lastReport: String, val odometer: Int, val driverScore: Int, val maintenanceNote: String?, val hasDashcam: Boolean,
)

enum class AlertKind { GEOFENCE, SPEED, IDLE, IMPACT, IGNITION, BATTERY, MAINTENANCE }

data class Alert(val id: String, val title: String, val unit: String, val relative: String, val description: String, val kind: AlertKind, val highPriority: Boolean, val acknowledged: Boolean = false)

data class Trip(val id: String, val span: String, val from: String, val to: String, val km: Double, val maxSpeed: Int, val idle: String, val stops: Int)

data class ReportType(val name: String, val description: String)

data class Geofence(val id: String, val name: String, val circle: Boolean, val detail: String, val triggers: String, val units: Int, val lastEvent: String, val paused: Boolean = false)

data class Landmark(val id: String, val name: String, val street: String, val radius: Int, val inside: Int, val note: String?, val kind: String)

enum class CommandState { QUEUED, SENT, ACKNOWLEDGED, FAILED }

data class CommandLogEntry(val id: String, val day: String, val command: String, val unit: String, val time: String, val issuedBy: String, val source: String, val confirmedWith: String?, val state: CommandState, val detail: String)

data class Clip(val id: String, val whenLabel: String, val kind: String, val camera: String, val durationS: Int, val gForce: Double?, val address: String, val speed: Int, val relative: String)

data class ShareLink(val id: String, val unit: String, val label: String, val expiresIn: String, val channel: String)

data class Session(val userId: String, val portalId: String, val firstName: String)
