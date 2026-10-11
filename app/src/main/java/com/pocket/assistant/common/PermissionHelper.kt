package com.pocket.assistant.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** 各类「特殊权限」跳转工具。 */
object PermissionHelper {

    /** 开发者选项（用于设置「模拟位置信息应用」）。 */
    fun openDeveloperSettings(context: Context) {
        launch(context, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
    }

    /** 悬浮窗权限。 */
    fun openOverlaySettings(context: Context) {
        launch(
            context,
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ),
        )
    }

    /** 使用情况访问权限。 */
    fun openUsageAccessSettings(context: Context) {
        launch(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    /** 应用详情页（用于手动授予通知等权限）。 */
    fun openAppDetailSettings(context: Context) {
        launch(
            context,
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"),
            ),
        )
    }

    private fun launch(context: Context, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
