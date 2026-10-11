package com.pocket.assistant.process

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 白名单持久化。
 * 移植自逆向源码的 Hawk.put/get("process_ignore_list")。
 */
object ProcessStore {

    private const val PREF = "process_store"
    private const val KEY_WHITELIST = "process_ignore_list"

    private var prefs: SharedPreferences? = null

    private val _whitelist = MutableStateFlow<Set<String>>(emptySet())
    val whitelist: StateFlow<Set<String>> = _whitelist.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs = p
        _whitelist.value = p.getStringSet(KEY_WHITELIST, emptySet())?.toSet() ?: emptySet()
    }

    fun putWhitelist(set: Set<String>) {
        _whitelist.value = set
        prefs?.edit()?.putStringSet(KEY_WHITELIST, set)?.apply()
    }

    fun toggle(packageName: String) {
        val current = _whitelist.value.toMutableSet()
        if (!current.add(packageName)) current.remove(packageName)
        putWhitelist(current)
    }

    fun isIgnored(packageName: String): Boolean = packageName in _whitelist.value
}
