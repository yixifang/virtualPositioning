package com.pocket.assistant.process

/**
 * 排序比较器。
 * 移植自逆向源码 cn.xx.mobilf.commonsdk.app.utils.QueueSortKt。
 *
 * comparator(isReverse=false, v1, v2)：v1>v2 返回 -1，即「大的排前面」= 降序。
 * 用于按 lastTimeUsed 排序时，让最近使用的应用排在列表最前。
 */
object ProcessSorter {

    fun comparator(isReverse: Boolean, v1: Long, v2: Long): Int {
        if (v1 > v2) {
            if (isReverse) return 1
        } else {
            if (v1 >= v2) return 0
            if (!isReverse) return 1
        }
        return -1
    }

    /** 最近使用优先（降序）。 */
    fun comparatorDesc(v1: Long, v2: Long): Int = comparator(false, v1, v2)
}
