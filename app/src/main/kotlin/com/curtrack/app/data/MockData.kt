package com.curtrack.app.data

import org.osmdroid.util.GeoPoint

/**
 * Mock data layer — README §5.4. Seeded 1:1 from the design prototype so every screen is
 * demonstrable today. Swap for Remote* repositories once the platform API (prerequisites #1/#2) lands.
 */
object MockData {
    val personal = listOf(
        Unit("u1", "Escape", "CFRT 214", "Household", "Sarah", UnitStatus.MOVING, 48, 112, 43.6532, -79.3832, "Front St W at Bay St, Toronto", true, 96, 11, "12 s ago", 64211, 92, "Oil change in 900 km", true),
        Unit("u2", "F-150", "BJKL 908", "Household", "You", UnitStatus.STOPPED, 0, 0, 43.6629, -79.3957, "Bloor St W at Bedford Rd", false, 88, 9, "2 min ago", 118940, 87, "Brake check overdue", true),
    )
    val fleet = listOf(
        Unit("f1", "Van 04", "CV 4021", "North route", "M. Okafor", UnitStatus.MOVING, 62, 74, 43.7182, -79.4103, "Yonge St at Sheppard Ave", true, 94, 12, "8 s ago", 88213, 78, "Service due 14 Sep", true),
        Unit("f2", "Van 07", "CV 4207", "North route", "J. Reyes", UnitStatus.IDLE, 0, 190, 43.6890, -79.3300, "Danforth Ave at Pape Ave", true, 91, 10, "22 s ago", 74110, 64, null, false),
        Unit("f3", "Truck 12", "CT 1290", "West route", "A. Silva", UnitStatus.MOVING, 34, 255, 43.6395, -79.4535, "Lake Shore Blvd W at Windermere", true, 97, 11, "5 s ago", 204558, 81, "Tires 2 weeks", true),
        Unit("f4", "Van 09", "CV 4409", "West route", "K. Brar", UnitStatus.STOPPED, 0, 0, 43.6580, -79.4680, "Dundas St W at Runnymede Rd", false, 82, 8, "6 min ago", 59780, 90, null, false),
        Unit("f5", "Van 11", "CV 4511", "East route", "D. Novak", UnitStatus.MOVING, 71, 22, 43.7620, -79.2990, "Hwy 401 E at Victoria Park", true, 93, 12, "11 s ago", 132004, 58, null, true),
        Unit("f6", "Truck 03", "CT 1103", "Yard", "—", UnitStatus.OFFLINE, 0, 0, 43.6008, -79.5480, "Yard — Kipling Ave", false, 41, 0, "3 h 40 m ago", 311220, 74, "DTC P0420 stored", false),
        Unit("f7", "Van 02", "CV 4002", "East route", "L. Chen", UnitStatus.IDLE, 0, 300, 43.7010, -79.2560, "Kingston Rd at Brimley Rd", true, 90, 9, "40 s ago", 66190, 83, null, true),
        Unit("f8", "Van 15", "CV 4615", "North route", "S. Haddad", UnitStatus.STOPPED, 0, 0, 43.7790, -79.4160, "Landmark: North depot", false, 95, 10, "18 min ago", 41220, 95, null, false),
    )
    fun unit(id: String?): Unit = (personal + fleet).firstOrNull { it.id == id } ?: personal.first()

    val alerts = listOf(
        Alert("a1", "Geofence exit", "Van 11", "2 min ago", "Left “North depot” geofence", AlertKind.GEOFENCE, true),
        Alert("a2", "Speed limit", "Van 11", "6 min ago", "71 km/h in a 60 zone — Hwy 401 ramp", AlertKind.SPEED, true),
        Alert("a3", "Excessive idling", "Van 07", "14 min ago", "Idling 11 min at Danforth Ave", AlertKind.IDLE, false),
        Alert("a4", "Impact detected", "Escape", "1 h ago", "Dashcam clip saved — 3.1 g event", AlertKind.IMPACT, true),
        Alert("a5", "Ignition on", "F-150", "2 h ago", "Bloor St W at Bedford Rd", AlertKind.IGNITION, false),
        Alert("a6", "Low external battery", "Truck 03", "3 h ago", "11.4 V — unit may power down", AlertKind.BATTERY, false),
        Alert("a7", "Maintenance due", "Truck 12", "Yesterday", "Tire rotation at 205,000 km", AlertKind.MAINTENANCE, false),
    )

    val reportTypes = listOf(
        ReportType("Trip summary", "Trips, distance, drive time per unit"),
        ReportType("Mileage", "Distance by day, week or month"),
        ReportType("Idle", "Idle events over a threshold"),
        ReportType("Speed limit", "Every posted-limit exceedance"),
        ReportType("Start / stop", "Ignition on-off with locations"),
        ReportType("Time at landmark", "Dwell time inside landmarks"),
        ReportType("Maintenance due", "Upcoming and overdue service"),
        ReportType("DTC", "Engine fault codes from OBD units"),
    )

    /** Lake Shore / Gardiner corridor into downtown Toronto — the prototype's trail. */
    val trail = listOf(
        43.6289 to -79.4695, 43.6300 to -79.4600, 43.6312 to -79.4498, 43.6320 to -79.4390, 43.6330 to -79.4275, 43.6350 to -79.4160, 43.6370 to -79.4055,
        43.6386 to -79.3960, 43.6404 to -79.3868, 43.6425 to -79.3800, 43.6455 to -79.3790, 43.6490 to -79.3800, 43.6520 to -79.3820, 43.6532 to -79.3832,
    ).map { GeoPoint(it.first, it.second) }

    val trips = listOf(
        Trip("t1", "07:12 → 07:41", "Home — Windermere Ave", "Front St W at Bay St", 14.8, 78, "3 m", 2),
        Trip("t2", "12:04 → 12:19", "Front St W at Bay St", "Queen St E at Broadview", 6.2, 56, "1 m", 1),
        Trip("t3", "17:38 → 18:11", "Queen St E at Broadview", "Home — Windermere Ave", 16.4, 74, "7 m", 3),
    )
    val todaySpark = listOf(9f, 14f, 20f, 12f, 26f, 31f, 18f, 24f, 34f, 22f, 15f, 28f)
    val speedProfile = listOf(12f, 24f, 38f, 46f, 52f, 61f, 58f, 44f, 30f, 41f, 55f, 63f, 70f, 52f, 36f, 22f, 14f, 26f, 40f, 33f)
    val byDay = listOf(38f, 52f, 44f, 61f, 73f, 29f, 18f)
    val driverScore30 = listOf(60f, 62f, 58f, 64f, 66f, 63f, 68f, 70f, 54f, 66f, 71f, 74f, 76f, 75f, 78f, 80f)

    val geofences = listOf(
        Geofence("g1", "North depot", true, "Circle · 520 m", "Entry & exit", 8, "last event 2 min ago"),
        Geofence("g2", "Home street", false, "Polygon · 5 points", "Exit only, outside work hours", 2, "no events this week"),
        Geofence("g3", "Airport", true, "Circle · 1 500 m", "Entry", 24, "last event Yesterday"),
        Geofence("g4", "Cottage", true, "Circle · 300 m", "Entry & exit", 1, "paused until 1 Nov", paused = true),
    )
    val northDepot = GeoPoint(43.7790, -79.4160)

    val landmarks = listOf(
        Landmark("l1", "Home", "Windermere Ave", 150, 2, "used in 1 report", "home"),
        Landmark("l2", "North depot", "Steeles Ave W", 520, 1, "also a geofence", "building"),
        Landmark("l3", "Office", "Front St W", 120, 0, null, "building"),
        Landmark("l4", "School", "Bloor St W", 200, 0, null, "circle"),
    )

    val commandLog = listOf(
        CommandLogEntry("c1", "Today", "Boost to 10 s", "Escape", "09:38", "sarah.k", "from this phone", null, CommandState.ACKNOWLEDGED, "acknowledged 09:38:41"),
        CommandLogEntry("c2", "Today", "Locate now", "F-150", "07:02", "sarah.k", "from this phone", null, CommandState.ACKNOWLEDGED, "acknowledged 07:02:09"),
        CommandLogEntry("c3", "Yesterday", "Starter disable", "F-150", "22:14", "sarah.k", "from this phone", "fingerprint", CommandState.ACKNOWLEDGED, "acknowledged 22:14:52"),
        CommandLogEntry("c4", "Yesterday", "Starter enable", "F-150", "22:31", "sarah.k", "from this phone", "PIN", CommandState.ACKNOWLEDGED, "acknowledged 22:31:07"),
        CommandLogEntry("c5", "Yesterday", "Sound buzzer", "Truck 03", "14:50", "m.okafor", "dispatcher web", null, CommandState.FAILED, "failed · unit offline"),
    )

    val clips = listOf(
        Clip("k1", "Today 08:52", "impact", "front", 12, 3.1, "Front St W at Bay St", 34, "1 h ago"),
        Clip("k2", "Yesterday 17:40", "braking", "rear", 10, null, "Queen St E at Broadview", 41, "Yesterday"),
        Clip("k3", "Mon 12:06", "manual", "front", 30, null, "Front St W at Bay St", 0, "Mon"),
    )

    val shareLinks = listOf(ShareLink("s1", "Escape", "Mum", "52 min", "WhatsApp"))
}
