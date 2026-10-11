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

class WhitelistViewModel(app: Application) : AndroidViewModel(app) {

    private val _apps = MutableStateFlow<List<ProcessInfo>>(emptyList())
    val apps: StateFlow<List<ProcessInfo>> = _apps.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val whitelist: StateFlow<Set<String>> = ProcessStore.whitelist

    fun load() {
        viewModelScope.launch {
            _apps.value = withContext(Dispatchers.IO) {
                ProcessUtils.allInstalledApps(getApplication())
            }
        }
    }

    fun search(text: String) {
        _query.value = text
    }

    fun toggle(packageName: String) = ProcessStore.toggle(packageName)
}
