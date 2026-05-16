package com.example.muse.ui.screen.edit.component.table

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val GRID_COLOR = Color(0xFF444444)
private val CELL_H_PADDING = 12.dp
private val CELL_V_PADDING = 8.dp

data class TableLayoutResult(
    val columnWidths: List<Float>,
    val rowHeights: List<Float>,
    val totalWidth: Float,
    val totalHeight: Float,
)

@Composable
fun TableModule(
    tableData: TableData,
    modifier: Modifier = Modifier,
) {
    val data = remember(tableData) { tableData.initialized() }
    if (data.rows == 0 || data.cols == 0) return

    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = Color.White, fontSize = 14.sp)
    val density = LocalDensity.current
    val cellHPx = with(density) { CELL_H_PADDING.toPx() }
    val cellVPx = with(density) { CELL_V_PADDING.toPx() }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }

        val layout = remember(data, screenWidthPx) {
            calculateLayout(textMeasurer, data, screenWidthPx, textStyle, cellHPx, cellVPx)
        }

        val textLayouts = remember(data, layout, textStyle) {
            data.cells.mapIndexed { r, row ->
                row.mapIndexed { c, text ->
                    val maxW = (layout.columnWidths[c] - cellHPx * 2).roundToInt().coerceAtLeast(1)
                    textMeasurer.measure(
                        text = text,
                        style = textStyle.copy(
                            textAlign = data.columnAlignments.getOrNull(c) ?: TextAlign.Start,
                        ),
                        constraints = Constraints(maxWidth = maxW)
                    )
                }
            }
        }

        val scrollState = rememberScrollState()
        val totalHeightDp = with(density) { layout.totalHeight.toDp() }

        if (layout.totalWidth > screenWidthPx) {
            val totalWidthDp = with(density) { layout.totalWidth.toDp() }
            Box(modifier = Modifier.horizontalScroll(scrollState)) {
                Canvas(modifier = Modifier.width(totalWidthDp).height(totalHeightDp)) {
                    drawTableContent(layout, textLayouts, cellHPx, cellVPx)
                }
            }
        } else {
            Canvas(modifier = Modifier.fillMaxWidth().height(totalHeightDp)) {
                drawTableContent(layout, textLayouts, cellHPx, cellVPx)
            }
        }
    }
}

private fun DrawScope.drawTableContent(
    layout: TableLayoutResult,
    textLayouts: List<List<TextLayoutResult>>,
    cellHPx: Float,
    cellVPx: Float,
) {
    var y = 0f
    for (r in textLayouts.indices) {
        var x = 0f
        val rowH = layout.rowHeights[r]
        for (c in textLayouts[r].indices) {
            val colW = layout.columnWidths[c]
            drawText(
                textLayouts[r][c],
                topLeft = Offset(x + cellHPx, y + cellVPx),
            )
            drawRect(
                color = GRID_COLOR,
                topLeft = Offset(x, y),
                size = Size(colW, rowH),
                style = Stroke(width = 0.5f),
            )
            x += colW
        }
        y += rowH
    }
}

internal fun calculateLayout(
    textMeasurer: TextMeasurer,
    data: TableData,
    screenWidthPx: Float,
    textStyle: TextStyle,
    cellHPx: Float,
    cellVPx: Float,
): TableLayoutResult {
    if (data.rows == 0 || data.cols == 0) {
        return TableLayoutResult(emptyList(), emptyList(), 0f, 0f)
    }

    val minColWidth = screenWidthPx / 5f
    val maxColWidth = if (data.cols == 1) screenWidthPx else screenWidthPx * 2f / 3f

    val naturalWidths = FloatArray(data.cols) { 0f }

    for (r in 0 until data.rows) {
        for (c in 0 until data.cols) {
            val text = data.cells.getOrNull(r)?.getOrNull(c) ?: ""
            val result = textMeasurer.measure(
                text = text,
                style = textStyle,
                constraints = Constraints(maxWidth = Int.MAX_VALUE, maxHeight = Int.MAX_VALUE)
            )
            naturalWidths[c] = maxOf(naturalWidths[c], result.size.width.toFloat() + cellHPx * 2)
        }
    }

    val colWidths = FloatArray(data.cols) { c -> maxOf(naturalWidths[c], minColWidth) }
    var totalWidth = colWidths.sum()

    if (totalWidth < screenWidthPx) {
        val ratio = screenWidthPx / totalWidth
        for (c in 0 until data.cols) colWidths[c] *= ratio
        totalWidth = screenWidthPx
    } else {
        for (c in 0 until data.cols) {
            if (naturalWidths[c] < minColWidth && totalWidth > screenWidthPx) {
                val reduction = colWidths[c] - naturalWidths[c]
                colWidths[c] = naturalWidths[c]
                totalWidth -= reduction
            }
        }
    }

    for (c in 0 until data.cols) {
        if (colWidths[c] > maxColWidth) {
            totalWidth -= colWidths[c] - maxColWidth
            colWidths[c] = maxColWidth
        }
    }

    val rowHeights = FloatArray(data.rows) { 0f }
    for (r in 0 until data.rows) {
        var maxH = 0f
        for (c in 0 until data.cols) {
            val text = data.cells.getOrNull(r)?.getOrNull(c) ?: ""
            val constraintW = (colWidths[c] - cellHPx * 2).roundToInt().coerceAtLeast(1)
            val result = textMeasurer.measure(
                text = text,
                style = textStyle.copy(
                    textAlign = data.columnAlignments.getOrNull(c) ?: TextAlign.Start,
                ),
                constraints = Constraints(maxWidth = constraintW)
            )
            maxH = maxOf(maxH, result.size.height.toFloat() + cellVPx * 2)
        }
        rowHeights[r] = maxH
    }

    return TableLayoutResult(
        columnWidths = colWidths.toList(),
        rowHeights = rowHeights.toList(),
        totalWidth = totalWidth,
        totalHeight = rowHeights.sum(),
    )
}
