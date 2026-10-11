package com.pocket.assistant.location

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pocket.assistant.App
import com.pocket.assistant.MainActivity
import com.pocket.assistant.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 虚拟定位前台服务。
 * 移植自逆向源码 cn.xx.mobilf.virtualapp.service.GlobalLocationService。
 *
 * 关键点：Mock Location 必须**持续**注入（源码为 100ms，即 10Hz），
 * 因为系统会判断定位是否“新鲜”，很多 App 也会校验时间戳。
 */
class MockLocationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var injectJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        _running.value = true
        startForegroundCompat()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LocationStore.init(this)
        clearError()
        startInjection()
        LocationFloatingController.show(this)
        return START_STICKY
    }

    private fun startInjection() {
        injectJob?.cancel()
        LocationUtils.removeAllTestProvider(this)
        LocationUtils.addTestProviders(this)

        injectJob = scope.launch {
            while (isActive) {
                val target = LocationStore.targetWgs.value
                if (target != null) {
                    val result = runCatching {
                        LocationUtils.setTestLocation(
                            this@MockLocationService,
                            LocationManager.GPS_PROVIDER, target.lat, target.lon,
                        )
                        LocationUtils.setTestLocation(
                            this@MockLocationService,
                            LocationManager.NETWORK_PROVIDER, target.lat, target.lon,
                        )
                    }
                    result.onFailure { e ->
                        if (e is SecurityException) {
                            _error.value = "未授权模拟位置：请在开发者选项中将本应用设为「模拟位置信息应用」"
                            stopSelf()
                        }
                    }
                }
                delay(100L) // 10Hz
            }
        }
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, App.CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_running_title))
            .setContentText(getString(R.string.notif_running_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pending)
            .build()
    }

    override fun onDestroy() {
        injectJob?.cancel()
        LocationUtils.removeAllTestProvider(this)
        LocationFloatingController.hide(this)
        _running.value = false
        super.onDestroy()
    }

    companion object {
        const val NOTIF_ID = 10010

        private val _error = MutableStateFlow<String?>(null)
        val error: StateFlow<String?> = _error.asStateFlow()

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        fun clearError() {
            _error.value = null
        }

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MockLocationService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MockLocationService::class.java))
        }
    }
}
