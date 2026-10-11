package com.pocket.assistant.location

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Bundle
import android.os.SystemClock

/**
 * Mock Location 注入工具。
 * 移植自逆向源码 cn.xx.mobilf.virtualapp.utils.GlobalLocationUtils。
 *
 * 注：addTestProvider / setTestProviderStatus / Criteria 等 API 已标记弃用，
 * 但它们仍是设备端模拟定位唯一可用的官方途径，故此处显式抑制弃用告警。
 */
@Suppress("DEPRECATION")
object LocationUtils {

    private val PROVIDERS = arrayOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
    )

    private fun lm(ctx: Context): LocationManager =
        ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /** 清空测试 provider，防止残留假定位。 */
    fun removeAllTestProvider(ctx: Context) {
        val manager = lm(ctx)
        PROVIDERS.forEach { name ->
            runCatching {
                if (manager.getProvider(name) != null) manager.removeTestProvider(name)
            }
        }
    }

    /** 注册并启用 gps / network 两个测试 provider。 */
    fun addTestProviders(ctx: Context) {
        val manager = lm(ctx)
        runCatching {
            manager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false, true, true, false, true, true, true,
                Criteria.POWER_LOW, Criteria.ACCURACY_FINE,
            )
            manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
            manager.setTestProviderStatus(
                LocationManager.GPS_PROVIDER,
                LocationProvider.AVAILABLE,
                null,
                System.currentTimeMillis(),
            )
        }
        runCatching {
            manager.addTestProvider(
                LocationManager.NETWORK_PROVIDER,
                false, false, false, false, false, false, false,
                Criteria.POWER_LOW, Criteria.ACCURACY_COARSE,
            )
            manager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
            manager.setTestProviderStatus(
                LocationManager.NETWORK_PROVIDER,
                LocationProvider.AVAILABLE,
                null,
                System.currentTimeMillis(),
            )
        }
    }

    /** 向指定 provider 注入一次坐标。 */
    fun setTestLocation(ctx: Context, provider: String, lat: Double, lon: Double) {
        val manager = lm(ctx)
        manager.setTestProviderLocation(provider, buildLocation(provider, lat, lon))
    }

    private fun buildLocation(provider: String, lat: Double, lon: Double): Location =
        Location(provider).apply {
            accuracy = 2f
            altitude = 55.0
            bearing = 1f
            extras = Bundle().apply { putInt("satellites", 7) }
            latitude = lat
            longitude = lon
            // 必须刷新时间戳，否则系统会判定定位“过期”
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }

    /**
     * 探测本应用是否已被设为「模拟位置信息应用」。
     * 通过尝试注册测试 provider 来判定：SecurityException(MOCK_LOCATION) 即未授权。
     */
    fun hasMockLocationPermission(ctx: Context): Boolean {
        val manager = lm(ctx)
        return try {
            manager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false, true, true, false, true, true, true,
                Criteria.POWER_LOW, Criteria.ACCURACY_FINE,
            )
            manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
            manager.removeTestProvider(LocationManager.GPS_PROVIDER)
            true
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            // provider 已存在等其它异常，说明权限其实是有的
            true
        }
    }
}
