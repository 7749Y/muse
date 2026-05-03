package com.example.muse.ui.screen.edit

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.drawText
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 编辑器 Canvas 绘制层：段落高亮、选中高亮、文本、composition 下划线、光标。
 */
@Composable
fun EditorCanvas(
    textLayoutResult: TextLayoutResult?,
    scrollOffsetPx: Float,
    adjustedLines: List<AdjustedLine>?,
    value: TextFieldValue,
    isFocused: Boolean,
    cursorVisible: Boolean,
    textMeasurer: TextMeasurer,
    effectiveTextStyle: TextStyle,
    placeholderText: String?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val r = textLayoutResult ?: return@Canvas
        val cursorPos = value.selection.start.coerceIn(0, value.text.length)

        // 段落高亮
        val paragraphLines = if (value.text.isNotEmpty()) {
            r.getParagraphLines(cursorPos)
        } else {
            IntRange.EMPTY
        }
        for (line in paragraphLines) {
            val (lineTop, lineBottom) = r.resolveAdjustedLine(line, adjustedLines)
            drawRect(
                color = Color(0xFF444444),
                topLeft = Offset(0f, lineTop - scrollOffsetPx),
                size = Size(size.width, lineBottom - lineTop)
            )
        }

        // 选中高亮
        val selStart = value.selection.start
        val selEnd = value.selection.end
        if (selStart != selEnd) {
            val minSel = minOf(selStart, selEnd)
            val maxSel = maxOf(selStart, selEnd)
            var off = minSel
            while (off < maxSel && off < r.layoutInput.text.length) {
                val line = r.getLineForOffset(off)
                val lineEnd = minOf(r.getLineEnd(line), maxSel)
                if (off < lineEnd) {
                    val startRect = r.getBoundingBox(off)
                    val endRect = r.getBoundingBox(lineEnd - 1)
                    val left = startRect.left
                    val right = endRect.right
                    val (adjTop, adjBottom) = r.resolveAdjustedLine(line, adjustedLines)
                    drawRect(
                        color = Color(0xFF00BCD4),
                        topLeft = Offset(left, adjTop - scrollOffsetPx),
                        size = Size(right - left, adjBottom - adjTop)
                    )
                }
                off = lineEnd
            }
        }

        // 文本
        if (value.text.isNotEmpty()) {
            if (adjustedLines != null) {
                for (adj in adjustedLines) {
                    if (adj.originalLineIndex !in 0 until r.lineCount) continue
                    val start = r.getLineStart(adj.originalLineIndex)
                    val end = r.getLineEnd(adj.originalLineIndex)
                    val lineText = r.layoutInput.text.substring(start, end).trimEnd('\n')
                    val lineLayout = textMeasurer.measure(
                        text = lineText,
                        style = effectiveTextStyle,
                        constraints = Constraints(maxWidth = size.width.roundToInt())
                    )
                    val adjVisTop = adj.top - scrollOffsetPx
                    val adjVisBottom = adj.bottom - scrollOffsetPx
                    if (adjVisBottom < 0 || adjVisTop > size.height) continue
                    drawText(
                        lineLayout,
                        topLeft = Offset(r.getLineLeft(adj.originalLineIndex), adjVisTop)
                    )
                }
            } else {
                drawText(r, topLeft = Offset(0f, -scrollOffsetPx))
            }
        } else if (placeholderText != null) {
            val phLayout = textMeasurer.measure(
                text = placeholderText,
                style = effectiveTextStyle.copy(color = effectiveTextStyle.color.copy(alpha = 0.5F)),
                constraints = Constraints(maxWidth = size.width.roundToInt())
            )
            drawText(phLayout, topLeft = Offset(0f, -scrollOffsetPx))
        }

        // IME composition 下划线（使用 layout 的文本长度确保不会越界）
        val composition = value.composition
        val layoutTextLen = r.layoutInput.text.length
        if (composition != null && !composition.collapsed &&
            composition.start >= 0 && composition.start < layoutTextLen &&
            composition.end > composition.start) {
            val compMin = composition.min.coerceAtMost(layoutTextLen)
            val compMax = composition.max.coerceAtMost(layoutTextLen)
            var off = compMin
            while (off < compMax) {
                if (off >= layoutTextLen) break
                val line = r.getLineForOffset(off)
                val lineEnd = minOf(r.getLineEnd(line), compMax)
                if (lineEnd <= off) break  // 防止死循环
                if (off < lineEnd) {
                    val startRect = r.getBoundingBox(off)
                    val endRect = r.getBoundingBox(lineEnd - 1)
                    val left = startRect.left
                    val right = endRect.right
                    val adjBottom = r.resolveAdjustedLine(line, adjustedLines).bottom
                    drawLine(
                        color = Color(0xFFFF7F7F),
                        start = Offset(left, adjBottom - scrollOffsetPx),
                        end = Offset(right, adjBottom - scrollOffsetPx),
                        strokeWidth = 1.5f
                    )
                }
                off = lineEnd
            }
        }

        // 光标
        if (isFocused && cursorVisible && cursorPos <= r.layoutInput.text.length) {
            val cursorRect = r.getCursorRect(cursorPos)
            val cursorY = adjustedLines?.let { lines ->
                val cursorLine = r.getLineForOffset(cursorPos)
                lines.getOrNull(cursorLine)?.let { adj ->
                    adj.top + (cursorRect.top - r.getLineTop(cursorLine))
                }
            } ?: cursorRect.top

            drawRect(
                color = Color(0xFFFF7F7F),
                topLeft = Offset(cursorRect.left, cursorY - scrollOffsetPx),
                size = Size(max(8f, cursorRect.width), cursorRect.height)
            )
        }
    }
}
