package com.pocket.assistant.process

import android.graphics.drawable.Drawable

/** 一条「后台应用」记录。 */
data class ProcessInfo(
    val icon: Drawable? = null,
    val packageName: String = "",
    val appName: String = "",
    val firstLetter: String = "#",
    val lastTimeUsed: Long = 0L,
    val selected: Boolean = false,
)
