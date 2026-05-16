package com.example.muse.ui.screen.edit.component.heading

/**
 * 解析文本开头的 "# " 前缀，检测标题等级。
 * 规则：
 * - "# text" → level=2（一个 # 也转为 2 级标题）
 * - "## text" → level=2
 * - "### text" → level=3
 * - "#### text" → level=4
 * - 以此类推，最高 6 级
 * - 未匹配时返回 null
 */
object HashKey {

    fun detectLevel(text: String): Pair<Int, String>? {
        if (text.isEmpty()) return null
        if (text.first() != '#') return null

        val prefix = text.takeWhile { it == '#' }
        if (prefix.isEmpty()) return null

        // 必须在 # 后紧跟空格
        val afterHash = text.drop(prefix.length)
        if (afterHash.firstOrNull() != ' ') return null

        val level = maxOf(prefix.length, 2).coerceIn(2, 6)
        val cleanText = afterHash.trimStart()
        return (level to cleanText).takeIf { cleanText.isNotEmpty() }
    }
}
