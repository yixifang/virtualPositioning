package com.pocket.assistant.location.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocket.assistant.location.CoordType
import com.pocket.assistant.location.LocationViewModel
import org.osmdroid.api.IGeoPoint
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * 地图选点。
 *
 * 使用 osmdroid + OpenStreetMap 瓦片：**无需 API Key**，开箱即用。
 * OSM 瓦片是 WGS84 坐标，因此中心点可直接作为 WGS84 注入值。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION") // setBuiltInZoomControls 已弃用但仍是关闭内置缩放按钮的常用做法
@Composable
fun MapPickerScreen(onBack: () -> Unit) {
    val vm: LocationViewModel = viewModel()
    // 用显式 MutableState 而非 by remember 委托：局部委托属性无法在匿名对象(MapListener)里被引用
    val centerState = remember { mutableStateOf<IGeoPoint>(GeoPoint(39.9087, 116.3975)) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("地图选点") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val map = MapView(ctx)
                    map.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        controller.setZoom(16.0)
                        controller.setCenter(GeoPoint(39.9087, 116.3975))
                        // osmdroid 的中心点读取在 MapView.getMapCenter()，而非 MapController
                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                centerState.value = map.mapCenter
                                return true
                            }

                            override fun onZoom(event: ZoomEvent?): Boolean {
                                centerState.value = map.mapCenter
                                return true
                            }
                        })
                        onResume()
                    }
                    mapView = map
                    map
                },
            )

            // 屏幕正中准星（代表将要选中的点）
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
            )

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("中心点（WGS84）", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "%.6f, %.6f".format(centerState.value.latitude, centerState.value.longitude),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                vm.setTarget(
                                    centerState.value.latitude,
                                    centerState.value.longitude,
                                    CoordType.WGS84,
                                )
                                onBack()
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("确认选点") }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView?.onDetach() }
    }
}
