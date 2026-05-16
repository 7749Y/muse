package com.example.muse.ui.screen.edit.component.gutter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muse.ui.screen.edit.core.AdjustedLine
import kotlin.math.max

@Composable
fun Gutter(
    itemProvider: (lineIndex: Int) -> GutterItem,
    totalLines: Int,
    scrollOffsetPx: Float,
    lineHeightPx: Float,
    containerHeightPx: Float,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    lineTextSpacingPx: Float = 8f,
    fixedWidth: Dp? = null,
    centerContent: Boolean = true,
    textLayoutResult: TextLayoutResult? = null,
    useLogicalLines: Boolean = false,
    adjustedLines: List<AdjustedLine> = emptyList(),
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val gutterStyle = textStyle.copy(
        color = Color.Gray,
        fontSize = textStyle.fontSize * 0.8f
    )

    val fixedWidthPx = fixedWidth?.let { with(density) { it.toPx() } }

    val isLogical = useLogicalLines && textLayoutResult != null
    val logicalData = remember(isLogical, textLayoutResult?.layoutInput?.text) {
        if (!isLogical) null
        else {
            val text = textLayoutResult.layoutInput.text
            val offsets = mutableListOf(0)
            var idx = text.indexOf('\n')
            while (idx >= 0) {
                offsets.add(idx + 1)
                idx = text.indexOf('\n', idx + 1)
            }
            offsets.map { off ->
                val visualLine = textLayoutResult.getLineForOffset(off.coerceAtMost(text.length))
                visualLine
            }
        }
    }
    val effectiveLineCount = logicalData?.size ?: totalLines

    val maxNumberString = remember(effectiveLineCount) {
        effectiveLineCount.toString()
    }
    val maxNumWidth = textMeasurer.measure(maxNumberString, gutterStyle).size.width
    val bulletWidth = textMeasurer.measure("•", gutterStyle).size.width
    val quoteLineWidth = with(density) { 4.dp.toPx() }

    val columnWidth = fixedWidthPx
        ?: remember(effectiveLineCount) {
            val maxContentWidth = maxNumWidth.toFloat().coerceAtLeast(bulletWidth.toFloat()).coerceAtLeast(quoteLineWidth)
            maxContentWidth + with(density) { 0.dp.toPx() }
        }

    Canvas(
        modifier = modifier
            .width(with(density) { columnWidth.toDp() })
            .fillMaxHeight()
    ) {
        for (itemIdx in 0 until effectiveLineCount) {
            val line = if (isLogical) logicalData!![itemIdx] else itemIdx
            val useLayout = textLayoutResult != null && line < textLayoutResult.lineCount

            val (lineH, top) = if (adjustedLines.isNotEmpty() && line < adjustedLines.size) {
                if (isLogical) {
                    val lastVLine = minOf(
                        if (itemIdx + 1 < logicalData!!.size) {
                            logicalData[itemIdx + 1] - 1
                        } else {
                            textLayoutResult.lineCount - 1
                        }, adjustedLines.size - 1
                    )
                    if (lastVLine !in adjustedLines.indices) continue
                    val h = adjustedLines[lastVLine].bottom - adjustedLines[line].top
                    val t = adjustedLines[line].top - scrollOffsetPx
                    Pair(h, t)
                } else {
                    val adj = adjustedLines[line]
                    Pair(adj.bottom - adj.top, adj.top - scrollOffsetPx)
                }
            } else if (useLayout) {
                if (isLogical) {
                    val lastVLine = if (itemIdx + 1 < logicalData!!.size)
                        logicalData[itemIdx + 1] - 1
                    else
                        textLayoutResult.lineCount - 1
                    val h = textLayoutResult.getLineBottom(lastVLine) - textLayoutResult.getLineTop(line)
                    val t = textLayoutResult.getLineTop(line) - scrollOffsetPx
                    Pair(h, t)
                } else {
                    val h = textLayoutResult.getLineBottom(line) - textLayoutResult.getLineTop(line)
                    val t = textLayoutResult.getLineTop(line) - scrollOffsetPx
                    Pair(h, t)
                }
            } else {
                Pair(lineHeightPx, itemIdx * lineHeightPx - scrollOffsetPx)
            }
            if (top + lineH < 0 || top > containerHeightPx) continue

            val item = itemProvider(itemIdx)
            val (textToDraw, isQuote) = when (item) {
                GutterItem.None -> continue
                is GutterItem.Number -> item.number.toString() to false
                GutterItem.Bullet -> "•" to false
                is GutterItem.OrderedNumber -> "${item.number}." to false
                GutterItem.QuoteLine -> "│" to true
            }

            if (isQuote) {
                val lineX = columnWidth / 2f
                drawLine(
                    Color.Gray,
                    Offset(lineX, top),
                    Offset(lineX, top + lineH),
                    strokeWidth = 4.dp.toPx()
                )
            } else {
                val textWidth = if (fixedWidthPx != null) {
                    var style = gutterStyle
                    var layout = textMeasurer.measure(textToDraw, style)
                    val maxFitWidth = columnWidth - with(density) { 8.dp.toPx() } * 2
                    if (layout.size.width > maxFitWidth) {
                        val scale = maxFitWidth / layout.size.width
                        style = gutterStyle.copy(fontSize = gutterStyle.fontSize * scale)
                        layout = textMeasurer.measure(textToDraw, style)
                    }
                    layout
                } else {
                    textMeasurer.measure(textToDraw, gutterStyle)
                }
                val x = if (centerContent) {
                    (columnWidth - textWidth.size.width) / 2f
                } else {
                    columnWidth - textWidth.size.width - with(density) { 0.dp.toPx() }
                }
                val y = top + (textWidth.size.height/3f)
                drawText(textWidth, topLeft = Offset(x, y))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E1E1E)
@Composable
private fun GutterPreview_Numbers() {
    PreviewGutter(
        provider = { GutterItem.Number(it + 1) },
        title = "Line Numbers"
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1E1E1E)
@Composable
private fun GutterPreview_Bullets() {
    PreviewGutter(
        provider = { GutterItem.Bullet },
        title = "Bullet List"
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1E1E1E)
@Composable
private fun GutterPreview_OrderedList() {
    PreviewGutter(
        provider = { GutterItem.OrderedNumber(it + 1) },
        title = "Ordered List"
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1E1E1E)
@Composable
private fun GutterPreview_Quotes() {
    PreviewGutter(
        provider = { GutterItem.QuoteLine },
        title = "Quote Lines"
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1E1E1E)
@Composable
private fun GutterPreview_Mixed() {
    PreviewGutter(
        provider = { index ->
            when {
                index % 5 == 0 -> GutterItem.Number(index + 1)
                index % 5 == 2 -> GutterItem.Bullet
                index % 5 == 3 -> GutterItem.OrderedNumber(index + 1)
                else -> GutterItem.QuoteLine
            }
        },
        title = "Mixed Styles"
    )
}

@Composable
private fun PreviewGutter(
    provider: (Int) -> GutterItem,
    title: String
) {
    val textStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    )
    val totalLines = 12
    val lineHeightPx = 24.dp.value * 2f
    val containerHeightPx = 300f
    val scrollOffsetPx = 0f

    Gutter(
        itemProvider = provider,
        totalLines = totalLines,
        scrollOffsetPx = scrollOffsetPx,
        lineHeightPx = lineHeightPx,
        containerHeightPx = containerHeightPx,
        textStyle = textStyle,
        modifier = Modifier
    )
}
