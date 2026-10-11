package com.pocket.assistant.process

/**
 * 汉字 -> 拼音首字母索引。
 * 移植自逆向源码 cn.xx.mobilf.commonsdk.app.utils.LatterUtils。
 *
 * 原理：把汉字按 GBK 编码得到双字节，换算成「区位码」后查表得到 A-Z 首字母。
 * 纯查表 O(1)，无需拼音库。
 */
object LatterUtils {

    private const val GB_SP_DIFF = 160

    private val secPosValueList = intArrayOf(
        1601, 1637, 1833, 2078, 2274, 2302, 2433, 2594, 2787, 3106,
        3212, 3472, 3635, 3722, 3730, 3858, 4027, 4086, 4390, 4558,
        4684, 4925, 5249, 5600,
    )

    // 注意：拼音中没有 I / U / V，故表里是 23 个字母
    private val firstLetter = arrayOf(
        "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M",
        "N", "O", "P", "Q", "R", "S", "T", "W", "X", "Y", "Z",
    )

    fun getSpells(characters: String): String {
        if (characters.isEmpty()) return "#"
        if (isLetter(characters)) return characters.uppercase()
        val sb = StringBuilder()
        for (c in characters) {
            if ((c.code shr 7) == 0) return "#"
            sb.append(getFirstLetter(c.toString()))
        }
        return if (sb.toString() == "-") "#" else sb.toString()
    }

    fun isLetter(characters: String): Boolean =
        Regex("[a-zA-Z]").matches(characters)

    fun getFirstLetter(ch: String): String = try {
        val bytes = ch.toByteArray(charset("GBK"))
        if (bytes[0].toInt() > 0) "#" else convert(bytes)
    } catch (e: Exception) {
        "#"
    }

    private fun convert(src: ByteArray): String {
        // 复刻 Java 的有符号 byte 运算：(byte)(b - 160)
        val bytes = ByteArray(src.size) { (src[it] - GB_SP_DIFF).toByte() }
        val pos = bytes[0] * 100 + bytes[1]
        for (i in 0 until 23) {
            if (pos >= secPosValueList[i] && pos < secPosValueList[i + 1]) {
                return firstLetter[i]
            }
        }
        return "-"
    }
}
