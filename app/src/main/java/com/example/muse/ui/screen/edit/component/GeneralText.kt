package com.example.muse.ui.screen.edit.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.muse.ui.screen.edit.core.buildAdjustedLines
import kotlin.math.roundToInt

/**
 * 通用文本展示组件。使用 Gutter 装订线 + 逻辑行处理，与 GeneralEditor 一致的排版。
 * 现在直接通过 Canvas 绘制文本，并在段落间插入 [paragraphSpacingPx] 间距。
 */
@Composable
fun GeneralText(
    text: String,
    type: ModuleType,
    modifier: Modifier = Modifier,
    paragraphSpacingPx: Float = 0f,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        color = type.textColor,
        fontSize = type.fontSize,
        fontWeight = type.fontWeight,
        lineHeight = type.fontSize * 1.4f
    )

    BoxWithConstraints(modifier = modifier) {
        val gutterWidthDp = 24.dp
        val textAreaWidthPx = with(density) {
            (maxWidth - gutterWidthDp).coerceAtLeast(0.dp).toPx().roundToInt()
        }

        val layoutResult = remember(text, textAreaWidthPx, textStyle) {
            textMeasurer.measure(
                text = text,
                style = textStyle,
                constraints = Constraints(maxWidth = textAreaWidthPx.coerceAtLeast(1))
            )
        }

        val adjustedLines = remember(layoutResult, paragraphSpacingPx) {
            if (paragraphSpacingPx > 0f) layoutResult.buildAdjustedLines(paragraphSpacingPx)
            else null
        }

        val totalTextHeight = adjustedLines?.lastOrNull()?.bottom
            ?: layoutResult.size.height.toFloat()

        val lineHeightPx = if (layoutResult.lineCount > 0)
            (layoutResult.getLineBottom(0) - layoutResult.getLineTop(0))
        else
            with(density) { type.fontSize.toPx() * 1.4f }

        Row {
            Gutter(
                itemProvider = type.gutterProvider,
                totalLines = layoutResult.lineCount,
                scrollOffsetPx = 0f,
                lineHeightPx = lineHeightPx,
                containerHeightPx = totalTextHeight,
                textStyle = textStyle,
                modifier = Modifier.fillMaxHeight(),
                fixedWidth = gutterWidthDp,
                centerContent = true,
                textLayoutResult = layoutResult,
                useLogicalLines = true,
                adjustedLines = adjustedLines ?: emptyList(),
            )

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (adjustedLines != null) {
                    for (adj in adjustedLines) {
                        val lineIndex = adj.originalLineIndex
                        if (lineIndex !in 0 until layoutResult.lineCount) continue

                        val start = layoutResult.getLineStart(lineIndex)
                        val end = layoutResult.getLineEnd(lineIndex)
                        val lineText = layoutResult.layoutInput.text.substring(start, end).trimEnd('\n')

                        val lineLayout = textMeasurer.measure(
                            text = lineText,
                            style = textStyle,
                            constraints = Constraints(maxWidth = size.width.roundToInt())
                        )

                        drawText(
                            lineLayout,
                            topLeft = Offset(
                                layoutResult.getLineLeft(lineIndex),
                                adj.top
                            )
                        )
                    }
                } else {
                    drawText(layoutResult, topLeft = Offset.Zero)
                }
            }
        }
    }
}
