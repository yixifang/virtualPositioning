package com.pocket.assistant.location

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * GCJ-02（火星坐标）与 WGS84 互转。
 *
 * 移植自逆向源码 cn.xx.mobilf.virtualapp.utils.GpsUtilNew。
 * 中国境内地图（高德/腾讯/百度底图）使用 GCJ-02，而设备 GPS 与 Android 系统的
 * Location 使用 WGS84。二者在中国境内存在数百米偏移，注入 Mock Location 前必须换算。
 */
object GpsUtilNew {

    private const val A = 6378245.0                 // 克拉索夫斯基椭球长半轴(m)
    private const val EE = 0.006693421622965943     // 椭球偏心率平方
    private const val PI = 3.141592653589793

    /** 中国境外不偏移；边界值取自源码。 */
    fun isOutOfChina(lat: Double, lon: Double): Boolean {
        return lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271
    }

    /** 纬度偏移多项式，x = lon-105, y = lat-35。 */
    private fun calLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    /** 经度偏移多项式，x = lon-105, y = lat-35。 */
    private fun calLon(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }

    /** 返回 [dlat, dlon]（单位：度）。境外返回 [0,0]。 */
    private fun calDev(wgLat: Double, wgLon: Double): DoubleArray {
        if (isOutOfChina(wgLat, wgLon)) return doubleArrayOf(0.0, 0.0)
        val d = wgLon - 105.0
        val d2 = wgLat - 35.0
        val cLat = calLat(d, d2)
        val cLon = calLon(d, d2)
        val radLat = wgLat / 180.0 * PI
        val sinLat = sin(radLat)
        val d4 = 1 - EE * sinLat * sinLat
        val sqrtD4 = sqrt(d4)
        val dLat = cLat * 180.0 / ((6335552.717000426 / (d4 * sqrtD4)) * PI)
        val dLon = cLon * 180.0 / ((A / sqrtD4) * cos(radLat) * PI)
        return doubleArrayOf(dLat, dLon)
    }

    /** WGS84 -> GCJ-02 */
    fun toGCJ02Point(lat: Double, lon: Double): GeoPoint {
        val dev = calDev(lat, lon)
        return GeoPoint(lat + dev[0], lon + dev[1])
    }

    /** GCJ-02 -> WGS84（一次反向逼近，亚米级误差） */
    fun toWGS84Point(lat: Double, lon: Double): GeoPoint {
        val d1 = calDev(lat, lon)
        val d2 = calDev(lat - d1[0], lon - d1[1])
        return GeoPoint(lat - d2[0], lon - d2[1])
    }

    /** 球面距离（米），地球半径取 6378137。 */
    fun distance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val radLat1 = lat1 * PI / 180.0
        val radLat2 = lat2 * PI / 180.0
        val dLon = (lng1 - lng2) * PI / 180.0
        val a = sin((radLat1 - radLat2) / 2.0)
        val b = sin(dLon / 2.0)
        return 2 * 6378137.0 * asin(sqrt(a * a + cos(radLat1) * cos(radLat2) * b * b))
    }
}
