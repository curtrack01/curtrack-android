package com.curtrack.app.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.io.File

/** Fires when DownloadManager finishes the update APK; verifies and launches the installer. */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val prefs = context.getSharedPreferences("curtrack", Context.MODE_PRIVATE)
        val expected = prefs.getLong("updateDownloadId", -1L)
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2L)
        if (id != expected) return
        val file = File(prefs.getString("updateFile", "") ?: return)
        if (!file.exists()) return
        val ok = UpdateChecker.install(context, file, prefs.getString("updateSha", "") ?: "")
        if (!ok) Toast.makeText(context, "Update file failed verification — not installed", Toast.LENGTH_LONG).show()
    }
}
