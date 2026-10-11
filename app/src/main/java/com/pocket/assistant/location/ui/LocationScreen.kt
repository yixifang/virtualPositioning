package com.pocket.assistant.location.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocket.assistant.common.PermissionHelper
import com.pocket.assistant.location.CoordType
import com.pocket.assistant.location.GeoPoint
import com.pocket.assistant.location.GpsUtilNew
import com.pocket.assistant.location.LocationPermState
import com.pocket.assistant.location.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(onOpenMap: () -> Unit) {
    val context = LocalContext.current
    val vm: LocationViewModel = viewModel()
    val target by vm.target.collectAsStateWithLifecycle()
    val sourceType by vm.sourceType.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    val perms by vm.perms.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val localError by vm.localError.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    var showInput by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshPermissions() }

    LaunchedEffect(error, localError) {
        val msg = error ?: localError
        if (msg != null) {
            snackbar.showSnackbar(msg)
            vm.dismissError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("虚拟定位") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TargetCard(target = target, sourceType = sourceType, running = running)
            PermissionCard(perms, onOpenMock = { PermissionHelper.openDeveloperSettings(context) },
                onOpenOverlay = { PermissionHelper.openOverlaySettings(context) })

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onOpenMap, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("地图选点")
                }
                OutlinedButton(onClick = { showInput = true }, modifier = Modifier.weight(1f)) {
                    Text("手动输入")
                }
            }

            Button(
                onClick = { if (running) vm.stop() else vm.start() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(
                    if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.padding(4.dp))
                Text(if (running) "停止虚拟定位" else "开始虚拟定位", fontWeight = FontWeight.SemiBold)
            }

            if (target != null) {
                OutlinedButton(onClick = { vm.clearTarget() }, modifier = Modifier.fillMaxWidth()) {
                    Text("清除目标位置")
                }
            }

            TipCard()
        }
    }

    if (showInput) {
        ManualInputDialog(
            initialType = sourceType,
            onDismiss = { showInput = false },
            onConfirm = { lat, lon, type ->
                vm.setTarget(lat, lon, type)
                showInput = false
            },
        )
    }
}

@Composable
private fun TargetCard(target: GeoPoint?, sourceType: CoordType, running: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (running) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (running) "● 虚拟定位运行中" else "○ 未运行",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (target == null) {
                Text("尚未设置目标位置，请先选点", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val gcj = GpsUtilNew.toGCJ02Point(target.lat, target.lon)
                Text("注入坐标 (WGS84)：$target")
                Text("对应火星坐标 (GCJ-02)：$gcj")
                Text(
                    "输入坐标系：${sourceType.label}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    perms: LocationPermState,
    onOpenMock: () -> Unit,
    onOpenOverlay: () -> Unit,
) {
    if (perms.mockLocation && perms.overlay) {
        Card(Modifier.fillMaxWidth()) {
            Text(
                "✓ 权限已就绪",
                Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        return
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("需要先完成以下设置", fontWeight = FontWeight.Bold)
            if (!perms.mockLocation) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("· 设为「模拟位置信息应用」", Modifier.weight(1f))
                    FilledTonalButton(onClick = onOpenMock) { Text("去设置") }
                }
            }
            if (!perms.overlay) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("· 授予悬浮窗权限", Modifier.weight(1f))
                    FilledTonalButton(onClick = onOpenOverlay) { Text("去授权") }
                }
            }
        }
    }
}

@Composable
private fun TipCard() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("使用说明", fontWeight = FontWeight.Bold)
            Text("1. 在「开发者选项 → 选择模拟位置信息应用」中选中「随身助手」。", style = MaterialTheme.typography.bodySmall)
            Text("2. 地图采用 OSM（WGS84），选点后可直接注入；若你手里是高德/腾讯的 GCJ-02 坐标，请在手动输入里切换坐标系。", style = MaterialTheme.typography.bodySmall)
            Text("3. 开始后系统会以 10Hz 持续注入 gps/network 定位，悬浮球单击可停止。", style = MaterialTheme.typography.bodySmall)
        }
    }
}
