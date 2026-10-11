package com.pocket.assistant.process

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process

/**
 * 进程/内存工具。
 * 移植自逆向源码 cn.xx.mobilf.process.util.ProcessUtils。
 *
 * 说明：普通 App 拿不到完整进程列表与单进程内存，本方案用
 * `UsageStatsManager` 的「近 10 秒活动应用」近似「后台进程」。
 */
object ProcessUtils {

    private const val WINDOW_MS = 10_000L

    /** 是否已获得「使用情况访问」权限。 */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** 扫描「近 10 秒有活动」的应用，按最近使用时间降序返回。 */
    fun scanBackstage(context: Context): List<ProcessInfo> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val raw = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - WINDOW_MS, now)
            ?: return emptyList()
        return raw
            .sortedWith(Comparator { a, b -> ProcessSorter.comparatorDesc(a.lastTimeUsed, b.lastTimeUsed) })
            .mapNotNull { toProcessInfo(context, it) }
    }

    private fun toProcessInfo(context: Context, stat: UsageStats): ProcessInfo? = try {
        val pm = context.packageManager
        val ai = pm.getApplicationInfo(stat.packageName, 0)
        val label = pm.getApplicationLabel(ai).toString()
        ProcessInfo(
            icon = pm.getApplicationIcon(ai),
            packageName = stat.packageName,
            appName = label,
            firstLetter = LatterUtils.getSpells(label),
            lastTimeUsed = stat.lastTimeUsed,
            selected = true,
        )
    } catch (e: Exception) {
        // 已卸载残留 / 系统私有包，跳过
        null
    }

    /** 全部已安装应用（白名单选择用），按首字母 + 名称排序。 */
    fun allInstalledApps(context: Context): List<ProcessInfo> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { ai ->
                val label = pm.getApplicationLabel(ai).toString()
                ProcessInfo(
                    icon = pm.getApplicationIcon(ai),
                    packageName = ai.packageName,
                    appName = label,
                    firstLetter = LatterUtils.getSpells(label),
                )
            }
            .sortedWith(compareBy({ it.firstLetter }, { it.appName }))
    }

    fun killBackground(context: Context, packageName: String) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        runCatching { am.killBackgroundProcesses(packageName) }
    }

    fun usedMemoryText(context: Context): String {
        val info = memoryInfo(context)
        return byte2Size(info.totalMem - info.availMem, 2)
    }

    fun availMemoryText(context: Context): String = byte2Size(memoryInfo(context).availMem, 2)

    fun totalMemoryText(context: Context): String = byte2Size(memoryInfo(context).totalMem, 2)

    private fun memoryInfo(context: Context): ActivityManager.MemoryInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    }

    /** 复刻 StringExtKt.byte2Size -> byte2FitMemorySize。 */
    fun byte2Size(num: Long, precision: Int = 2): String {
        if (num < 0) return "0B"
        return when {
            num < 1024L -> "${num}B"
            num < 1024L * 1024 -> "%.${precision}fKB".format(num / 1024.0)
            num < 1024L * 1024 * 1024 -> "%.${precision}fMB".format(num / 1024.0 / 1024)
            else -> "%.${precision}fGB".format(num / 1024.0 / 1024 / 1024)
        }
    }
}
