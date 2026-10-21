# CurTrack Android — how to build the APK

This is the complete Android project (Kotlin + Jetpack Compose) for the CurTrack app: all 24 screens from the
Design canvas, the design-system tokens, dark/light theme, real OpenStreetMap maps, the self-updater for direct-install
distribution, and a release signing key already configured.

**It runs on mock data.** Login works with any User ID and the password `dashcam`. Live vehicles need the platform API
(handoff README §0, prerequisites #1 and #2) and the broker (§6).

## One-time setup (≈ 20 min, mostly downloads)

1. Install **Android Studio** (free): https://developer.android.com/studio — accept the defaults; it brings its own JDK and
   Android SDK.
2. Unzip `curtrack-android.zip` somewhere permanent (e.g. `Documents/curtrack-android`).
3. Android Studio → **Open** → choose the `curtrack-android` folder. Wait for "Gradle sync" to finish (first time
   downloads ~1 GB of libraries; a progress bar shows at the bottom).
   - If it asks to install missing SDK components, click **Install**.

## Build the APK (every release)

1. Menu **Build → Generate Signed App Bundle / APK…** → choose **APK** → Next.
2. Key store path: click **Choose existing…** → select `app/curtrack-release.jks` inside the project.
   Passwords and alias are in `keystore.properties` (same folder as this file).
   Tick **Remember passwords**. Next.
3. Build variant: **release** → Create.
4. When it finishes, a notification offers **locate**: the file is at
   `app/release/app-release.apk`. Rename it to `curtrack-<version>.apk` (e.g. `curtrack-1.0.0.apk`).

Faster alternative once set up: open the Terminal tab in Android Studio and run `./gradlew assembleRelease`
(`gradlew.bat assembleRelease` on Windows). Output lands in `app/build/outputs/apk/release/`.

## Publish it on your install link

1. Upload the APK to `https://curtrack.com/app/curtrack-1.0.0.apk` (any HTTPS host works — your own site, S3, etc.).
2. Compute its SHA-256 (`shasum -a 256 curtrack-1.0.0.apk` on macOS/Linux, `certutil -hashfile … SHA256` on Windows).
3. Edit `version.json` (next to this file): set `versionCode`, `versionName`, `url`, `sha256`, `notes`. Upload it to
   `https://curtrack.com/app/version.json`.
4. Send clients the link to the APK (or a landing page that links to it). On the phone: open in Chrome → Download →
   tap the file → allow Chrome to install unknown apps (once) → Install.

For every later release: bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`, rebuild, upload the new
APK and `version.json`. Phones that already have the app will show an "Update available" card on Home.

## Building on GitHub (no Android Studio needed)

The repository has a workflow (`.github/workflows/build-apk.yml`) that builds the signed APK on every push to `main`.
The signing key is NOT in the repository: it is stored in the repository's **Settings → Secrets and variables → Actions** as
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. The finished APK and its SHA-256 are in the run's
**Artifacts** (`curtrack-apk`). Bump `versionCode`/`versionName`, push, download.

## Things to keep safe

- `app/curtrack-release.jks` + `keystore.properties` — **the signing key**. Back it up offline. Never share it. If it is
  lost, every client has to uninstall and reinstall; it cannot be recreated.
- The password in `keystore.properties` is in plain text so Android Studio can build; keep the folder private.

## Before going live with real data

- `app/build.gradle.kts` → `UPDATE_URL` must point at your real `version.json`.
- Replace the mock repositories (`data/MockData.kt`) with the broker client once the platform API exists.
- Fonts: Space Grotesk / JetBrains Mono load through Google Play services' font provider. If they don't appear on a
  test phone, the fallback is the system font — bundle the `.ttf` files under `app/src/main/res/font/` to make it
  deterministic (handoff README §7 asks for bundled fonts).
- Add Firebase (Crashlytics + FCM) — the handoff README §0.1 explains why this matters without a Play listing.

## Troubleshooting

| Symptom | Fix |
|---|---|
| "SDK location not found" | Android Studio → Tools → SDK Manager → install **Android 15 (API 35)** and **Build-Tools 35**. |
| Gradle sync fails on first open | Check internet; File → Invalidate Caches → Restart; Sync again. |
| "Play Protect blocked this install" on a phone | Tap **More details → Install anyway**. Expected for sideloaded apps (README §0.1). |
| Maps are blank | The phone needs internet for OpenStreetMap tiles the first time; tiles are cached afterwards. |
