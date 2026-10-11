package com.pocket.assistant.process

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProcessViewModel(app: Application) : AndroidViewModel(app) {

    private val _list = MutableStateFlow<List<ProcessInfo>>(emptyList())
    val list: StateFlow<List<ProcessInfo>> = _list.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _hasUsageAccess = MutableStateFlow(false)
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    private val _usedMemory = MutableStateFlow("")
    val usedMemory: StateFlow<String> = _usedMemory.asStateFlow()

    private val _totalMemory = MutableStateFlow("")
    val totalMemory: StateFlow<String> = _totalMemory.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val whitelist: StateFlow<Set<String>> = ProcessStore.whitelist

    fun refreshStatus() {
        val ctx = getApplication<Application>()
        _hasUsageAccess.value = ProcessUtils.hasUsageAccess(ctx)
        _usedMemory.value = ProcessUtils.usedMemoryText(ctx)
        _totalMemory.value = ProcessUtils.totalMemoryText(ctx)
    }

    fun scan() {
        val ctx = getApplication<Application>()
        if (!ProcessUtils.hasUsageAccess(ctx)) {
            _hasUsageAccess.value = false
            _message.value = "请先授予「使用情况访问」权限"
            return
        }
        _scanning.value = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { ProcessUtils.scanBackstage(ctx) }
            _list.value = result
            _scanning.value = false
            refreshStatus()
            if (result.isEmpty()) _message.value = "近 10 秒没有活跃的后台应用"
        }
    }

    fun toggleSelect(packageName: String) {
        _list.value = _list.value.map {
            if (it.packageName == packageName) it.copy(selected = !it.selected) else it
        }
    }

    /** 一键加速：结束所有「已勾选且不在白名单」的应用。 */
    fun oneKeySpeed() {
        val ctx = getApplication<Application>()
        val wl = ProcessStore.whitelist.value
        val targets = _list.value.filter { it.selected && it.packageName !in wl }
        if (targets.isEmpty()) {
            _message.value = "没有可清理的应用（可能都在白名单里）"
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                targets.forEach { ProcessUtils.killBackground(ctx, it.packageName) }
            }
            _message.value = "已结束 ${targets.size} 个后台应用"
            refreshStatus()
        }
    }

    fun toggleWhitelist(packageName: String) = ProcessStore.toggle(packageName)

    fun consumeMessage() {
        _message.value = null
    }
}
