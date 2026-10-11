package com.pocket.assistant.process.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocket.assistant.common.PermissionHelper
import com.pocket.assistant.process.ProcessInfo
import com.pocket.assistant.process.ProcessViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessScreen(
    onOpenWhitelist: () -> Unit,
    vm: ProcessViewModel = viewModel(),
) {
    val context = LocalContext.current
    val list by vm.list.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val hasAccess by vm.hasUsageAccess.collectAsStateWithLifecycle()
    val usedMem by vm.usedMemory.collectAsStateWithLifecycle()
    val totalMem by vm.totalMemory.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val whitelist by vm.whitelist.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshStatus() }

    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message!!)
            vm.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("手机瘦身") },
                actions = {
                    IconButton(onClick = onOpenWhitelist) {
                        Icon(Icons.Filled.Security, contentDescription = "白名单")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MemoryCard(usedMem = usedMem, totalMem = totalMem)

            if (!hasAccess) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("需要「使用情况访问」权限", fontWeight = FontWeight.Bold)
                        Text(
                            "扫描后台应用需要该权限，请前往系统设置开启。",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        FilledTonalButton(onClick = { PermissionHelper.openUsageAccessSettings(context) }) {
                            Text("去授权")
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { vm.scan() },
                        enabled = !scanning,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (scanning) "扫描中…" else "扫描后台")
                    }
                    Button(
                        onClick = { vm.oneKeySpeed() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("一键加速")
                    }
                }
            }

            if (list.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .weight(1f, fill = true),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (hasAccess) "点击「扫描后台」查看近 10 秒活跃的应用" else "尚未授权",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.packageName }) { item ->
                        ProcessRow(
                            item = item,
                            isWhitelisted = item.packageName in whitelist,
                            onToggle = { vm.toggleSelect(item.packageName) },
                            onToggleWhitelist = { vm.toggleWhitelist(item.packageName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCard(usedMem: String, totalMem: String) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("已用内存", style = MaterialTheme.typography.labelMedium)
            Text(
                "$usedMem / $totalMem",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ProcessRow(
    item: ProcessInfo,
    isWhitelisted: Boolean,
    onToggle: () -> Unit,
    onToggleWhitelist: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = item.selected, onCheckedChange = { onToggle() })
            AppIcon(item.icon, Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.appName, fontWeight = FontWeight.Medium)
                Text(
                    if (isWhitelisted) "已在白名单 · ${item.packageName}" else item.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = onToggleWhitelist) {
                Text(if (isWhitelisted) "移出" else "加白", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
