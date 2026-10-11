package com.pocket.assistant.location

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 运行所需的权限/开关状态。 */
data class LocationPermState(
    val mockLocation: Boolean = false,
    val overlay: Boolean = false,
)

class LocationViewModel(app: Application) : AndroidViewModel(app) {

    val target: StateFlow<GeoPoint?> = LocationStore.targetWgs
    val sourceType: StateFlow<CoordType> = LocationStore.sourceType
    val running: StateFlow<Boolean> = MockLocationService.running
    val error: StateFlow<String?> = MockLocationService.error

    private val _perms = MutableStateFlow(LocationPermState())
    val perms: StateFlow<LocationPermState> = _perms.asStateFlow()

    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        _perms.value = LocationPermState(
            mockLocation = LocationUtils.hasMockLocationPermission(ctx),
            overlay = Settings.canDrawOverlays(ctx),
        )
    }

    fun setTarget(lat: Double, lon: Double, type: CoordType) =
        LocationStore.setTarget(lat, lon, type)

    fun clearTarget() = LocationStore.clear()

    fun start() {
        val ctx = getApplication<Application>()
        if (LocationStore.targetWgs.value == null) {
            _localError.value = "请先在地图上选一个目标位置"
            return
        }
        _localError.value = null
        MockLocationService.clearError()
        MockLocationService.start(ctx)
    }

    fun stop() = MockLocationService.stop(getApplication())

    private val _localError = MutableStateFlow<String?>(null)
    val localError: StateFlow<String?> = _localError.asStateFlow()

    fun dismissError() {
        _localError.value = null
        MockLocationService.clearError()
    }
}
