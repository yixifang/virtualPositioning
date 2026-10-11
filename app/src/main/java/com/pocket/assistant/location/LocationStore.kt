package com.pocket.assistant.location

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 目标坐标的全局存储 + 持久化。
 *
 * 设计要点：**内部统一以 WGS84 保存**（这是 Android 系统 Location 的坐标系，也是注入值）。
 * 用户从 GCJ-02 地图上选的点会先转成 WGS84 再存储，展示时再反算回 GCJ-02。
 */
object LocationStore {

    private const val PREF = "location_store"
    private const val KEY_LAT = "target_lat"
    private const val KEY_LON = "target_lon"
    private const val KEY_HAS = "target_has"
    private const val KEY_TYPE = "source_type"

    private var prefs: SharedPreferences? = null

    private val _targetWgs = MutableStateFlow<GeoPoint?>(null)
    val targetWgs: StateFlow<GeoPoint?> = _targetWgs.asStateFlow()

    private val _sourceType = MutableStateFlow(CoordType.WGS84)
    val sourceType: StateFlow<CoordType> = _sourceType.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs = p
        if (p.getBoolean(KEY_HAS, false)) {
            val lat = p.getString(KEY_LAT, null)?.toDoubleOrNull()
            val lon = p.getString(KEY_LON, null)?.toDoubleOrNull()
            if (lat != null && lon != null) _targetWgs.value = GeoPoint(lat, lon)
        }
        _sourceType.value = runCatching {
            CoordType.valueOf(p.getString(KEY_TYPE, CoordType.WGS84.name)!!)
        }.getOrDefault(CoordType.WGS84)
    }

    /**
     * 设置目标点。type 是「用户输入所用的坐标系」，内部统一折算成 WGS84 保存。
     */
    fun setTarget(lat: Double, lon: Double, type: CoordType) {
        val wgs = when (type) {
            CoordType.WGS84 -> GeoPoint(lat, lon)
            CoordType.GCJ02 -> GpsUtilNew.toWGS84Point(lat, lon)
        }
        _targetWgs.value = wgs
        _sourceType.value = type
        prefs?.edit()
            ?.putBoolean(KEY_HAS, true)
            ?.putString(KEY_LAT, wgs.lat.toString())
            ?.putString(KEY_LON, wgs.lon.toString())
            ?.putString(KEY_TYPE, type.name)
            ?.apply()
    }

    fun clear() {
        _targetWgs.value = null
        prefs?.edit()?.putBoolean(KEY_HAS, false)?.apply()
    }

    /** 当前目标的 GCJ-02 表示（仅用于展示）。 */
    fun gcjOf(point: GeoPoint): GeoPoint = GpsUtilNew.toGCJ02Point(point.lat, point.lon)

    /** 当前目标的 WGS84 表示。 */
    fun wgsOf(point: GeoPoint): GeoPoint = GpsUtilNew.toWGS84Point(point.lat, point.lon)
}
