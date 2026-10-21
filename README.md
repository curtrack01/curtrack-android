# CurTrack Android

Kotlin / Jetpack Compose app for CurTrack (GPS tracking & dash cams), portal 189.
See **BUILD.md** for how to produce the APK and publish it on the direct-install link.
Design source: the *CurTrack Android Screens* canvas and *CurTrack Design System* (Claude Design); spec: the Android handoff README v2.

Structure
- `app/src/main/kotlin/com/curtrack/app/ui/theme` — design tokens (colours both themes, fonts, radii, spacing)
- `…/ui/components` — Card, StatTile, StatusPill, UnitRow, Chip, Switch, Button, AppBar, BottomNav, map, icons
- `…/ui/screens` — Auth (Login, Forgot, Permissions), Core (Home, Live, Unit, Trips, command confirm), Tabs (Alerts, Reports, Report, Geofence, Settings), Features (Driver, Find my car, Share, Clips, Watch live, Units, Landmarks, Geofences, Command log, Help)
- `…/data` — domain models, mock data seeded from the prototype, persisted app state
- `…/update` — self-updater for direct-install distribution (version.json → download → verify → install)
