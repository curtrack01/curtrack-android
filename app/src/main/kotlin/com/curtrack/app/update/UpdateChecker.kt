package com.curtrack.app.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.curtrack.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Self-update for direct-install distribution (README §0.1).
 *
 * Flow: fetch `version.json` → compare versionCode → user taps "Update" → DownloadManager pulls the APK
 * into app-private external storage → SHA-256 verified → PackageInstaller launched via FileProvider.
 *
 * version.json shape:
 * { "versionCode": 101, "versionName": "1.0.1", "url": "https://curtrack.com/app/curtrack-1.0.1.apk",
 *   "sha256": "<hex>", "notes": "What changed", "mandatory": false }
 */
data class AppUpdate(val versionCode: Int, val versionName: String, val url: String, val sha256: String, val notes: String, val mandatory: Boolean)

object UpdateChecker {
    suspend fun check(): AppUpdate? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(BuildConfig.UPDATE_URL).openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            AppUpdate(
                json.getInt("versionCode"), json.optString("versionName", "?"), json.getString("url"),
                json.optString("sha256", ""), json.optString("notes", ""), json.optBoolean("mandatory", false),
            ).takeIf { it.versionCode > BuildConfig.VERSION_CODE }
        }.getOrNull()
    }

    /** Starts the download; the receiver in [UpdateReceiver] installs it when done. */
    fun download(context: Context, update: AppUpdate): Long {
        val dir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val file = File(dir, "curtrack-${update.versionName}.apk")
        val req = DownloadManager.Request(Uri.parse(update.url))
            .setTitle("CurTrack ${update.versionName}")
            .setDescription("Downloading update")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, "updates/${file.name}")
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = dm.enqueue(req)
        context.getSharedPreferences("curtrack", Context.MODE_PRIVATE).edit()
            .putLong("updateDownloadId", id).putString("updateSha", update.sha256).putString("updateFile", file.absolutePath).apply()
        return id
    }

    fun install(context: Context, file: File, expectedSha256: String): Boolean {
        if (expectedSha256.isNotBlank() && !sha256(file).equals(expectedSha256, ignoreCase = true)) { file.delete(); return false }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
        return true
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { s -> val buf = ByteArray(65536); var n: Int; while (s.read(buf).also { n = it } > 0) md.update(buf, 0, n) }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
