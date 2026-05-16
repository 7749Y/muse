package com.example.muse.ui.screen.edit.component.table

import androidx.compose.ui.text.style.TextAlign

data class TableData(
    val rows: Int = 3,
    val cols: Int = 3,
    val cells: List<List<String>> = emptyList(),
    val columnAlignments: List<TextAlign> = emptyList(),
) {
    fun toMarkdown(): String {
        if (rows == 0 || cols == 0) return ""
        val sb = StringBuilder()
        // Header row
        sb.append("|")
        for (c in 0 until cols) {
            val cell = cells.getOrNull(0)?.getOrNull(c) ?: ""
            sb.append(" $cell |")
        }
        sb.appendLine()
        // Separator
        sb.append("|")
        for (c in 0 until cols) {
            val align = columnAlignments.getOrNull(c) ?: TextAlign.Start
            val sep = when (align) {
                TextAlign.Center -> ":---:"
                TextAlign.End -> "---:"
                else -> "---"
            }
            sb.append(" $sep |")
        }
        sb.appendLine()
        // Data rows
        for (r in 1 until rows) {
            sb.append("|")
            for (c in 0 until cols) {
                val cell = cells.getOrNull(r)?.getOrNull(c) ?: ""
                sb.append(" $cell |")
            }
            sb.appendLine()
        }
        return sb.toString().trimEnd()
    }

    fun initialized(): TableData {
        if (cells.size == rows && cells.all { it.size == cols }) return this
        val newCells = (0 until rows).map { r ->
            (0 until cols).map { c ->
                cells.getOrNull(r)?.getOrNull(c) ?: ""
            }
        }
        val newAlign = (0 until cols).map { c ->
            columnAlignments.getOrNull(c) ?: TextAlign.Start
        }
        return copy(cells = newCells, columnAlignments = newAlign)
    }

    companion object {
        fun fromMarkdown(md: String): TableData? {
            val lines = md.lines().filter { it.startsWith("|") }
            if (lines.size < 2) return null
            val cols = lines[0].trim('|').split("|").size
            if (cols == 0) return null
            val cells = lines.filterIndexed { i, _ -> i != 1 }.map { line ->
                line.trim('|').split("|").map { it.trim() }
            }
            val alignRow = lines[1].trim('|').split("|").map { it.trim() }
            val aligns = alignRow.map { sep ->
                when {
                    sep.startsWith(":") && sep.endsWith(":") -> TextAlign.Center
                    sep.endsWith(":") -> TextAlign.End
                    else -> TextAlign.Start
                }
            }
            return TableData(
                rows = cells.size,
                cols = cols,
                cells = cells,
                columnAlignments = aligns,
            )
        }
    }
}
