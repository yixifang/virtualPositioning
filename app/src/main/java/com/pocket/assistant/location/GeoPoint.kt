package com.pocket.assistant.location

/** 经纬度。 */
data class GeoPoint(val lat: Double, val lon: Double) {
    val isZero: Boolean get() = lat == 0.0 && lon == 0.0
    override fun toString(): String = "%.6f, %.6f".format(lat, lon)
}

/** 用户输入的坐标系类型。 */
enum class CoordType(val label: String) {
    WGS84("WGS84（GPS 原始 / OSM 地图）"),
    GCJ02("GCJ-02（高德 / 腾讯底图）"),
}
