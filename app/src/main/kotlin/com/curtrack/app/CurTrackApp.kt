package com.curtrack.app

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.IntentFilter
import com.curtrack.app.data.AppState
import com.curtrack.app.update.UpdateReceiver

class CurTrackApp : Application() {
    lateinit var state: AppState
        private set

    override fun onCreate() {
        super.onCreate()
        state = AppState(this)
        org.osmdroid.config.Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = getExternalFilesDir(null)
            osmdroidTileCache = java.io.File(getExternalFilesDir(null), "tiles")
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(UpdateReceiver(), filter, Context.RECEIVER_EXPORTED)
        else registerReceiver(UpdateReceiver(), filter)
    }
}
