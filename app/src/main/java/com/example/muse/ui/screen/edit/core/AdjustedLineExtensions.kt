package com.example.muse.ui.screen.edit.core

import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.max

/** 段落间距调整后的逻辑行信息 */
data class AdjustedLine(
    val originalLineIndex: Int,
    val top: Float,
    val bottom: Float,
)

/** [resolveAdjustedLine] 的返回值 */
data class AdjustedLineResolution(
    val top: Float,
    val bottom: Float,
)

/** 查询某视觉行在 adjustedLines 中的调整后 Y 坐标，若无则退回原始布局值 */
fun TextLayoutResult.resolveAdjustedLine(line: Int, adjustedLines: List<AdjustedLine>?): AdjustedLineResolution {
    val adj = adjustedLines?.getOrNull(line)
    return AdjustedLineResolution(
        top = adj?.top ?: getLineTop(line),
        bottom = adj?.bottom ?: getLineBottom(line),
    )
}

/** 滚动边界计算结果 */
data class ScrollBounds(val minScroll: Float, val maxScroll: Float)

/**
 * 计算滚动允许的 min/max 边界。
 * @param centerWhenSmaller 内容总高小于容器时是否居中（拖拽/fling 用 true，光标居中用 false）
 */
fun computeScrollBounds(
    textHeight: Float,
    containerHeightPx: Float,
    topPaddingPx: Float,
    bottomPaddingPx: Float,
    centerWhenSmaller: Boolean = false,
): ScrollBounds {
    val totalH = topPaddingPx + textHeight + bottomPaddingPx
    return if (centerWhenSmaller && totalH < containerHeightPx) {
        ScrollBounds(
            -(containerHeightPx - totalH) / 2f,
            max(0f, textHeight + bottomPaddingPx - containerHeightPx)
        )
    } else {
        ScrollBounds(
            -topPaddingPx,
            max(0f, textHeight + bottomPaddingPx - containerHeightPx)
        )
    }
}

/** 判断某视觉行结束后是否是段落结束（即遇到 '\n' 或文本结尾）*/
internal fun TextLayoutResult.isParagraphEnd(line: Int): Boolean {
    if (line !in 0..<lineCount) return true
    val end = getLineEnd(line)
    val text = layoutInput.text
    if (end > 0 && end <= text.length && text[end - 1] == '\n') return true
    return end >= text.length
}

/** 构建调整后的行位置列表，在段落间插入 paragraphSpacingPx 间距 */
internal fun TextLayoutResult.buildAdjustedLines(paragraphSpacingPx: Float): List<AdjustedLine> {
    val result = mutableListOf<AdjustedLine>()
    var accumulatedOffset = 0f
    for (line in 0 until lineCount) {
        if (line > 0 && isParagraphEnd(line - 1)) {
            accumulatedOffset += paragraphSpacingPx
        }
        result.add(AdjustedLine(
            originalLineIndex = line,
            top = getLineTop(line) + accumulatedOffset,
            bottom = getLineBottom(line) + accumulatedOffset,
        ))
    }
    return result
}

/** 获取光标所在段落的所有视觉行索引（闭区间）*/
internal fun TextLayoutResult.getParagraphLines(offset: Int): IntRange {
    val cursorLine = getLineForOffset(offset.coerceIn(0, layoutInput.text.length))
    var startLine = cursorLine
    while (startLine > 0 && !isParagraphEnd(startLine - 1)) {
        startLine--
    }
    var endLine = cursorLine
    while (endLine < lineCount - 1 && !isParagraphEnd(endLine)) {
        endLine++
    }
    return startLine..endLine
}
