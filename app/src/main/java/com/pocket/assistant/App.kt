package com.pocket.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.pocket.assistant.location.LocationStore
import com.pocket.assistant.process.ProcessStore
import org.osmdroid.config.Configuration
import java.io.File

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        LocationStore.init(this)
        ProcessStore.init(this)
        initOsmdroid()
        createNotificationChannel()
    }

    /**
     * osmdroid 全局初始化。
     * 关键点：把瓦片缓存目录指到应用私有 cacheDir，避免申请存储权限。
     */
    private fun initOsmdroid() {
        val config = Configuration.getInstance()
        config.load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        config.userAgentValue = packageName
        config.osmdroidBasePath = File(cacheDir, "osmdroid")
        config.osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "pocket_assistant_location"
    }
}
