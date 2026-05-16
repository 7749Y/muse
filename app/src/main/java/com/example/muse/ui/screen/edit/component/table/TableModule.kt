package com.example.muse.ui.screen.edit.component.table

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.TextFieldValue
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
    isEditing: Boolean = false,
    onDeleteModule: () -> Unit = {},
    onAlignColumn: (colIndex: Int, align: TextAlign) -> Unit = { _, _ -> },
    onCellChange: (row: Int, col: Int, text: String) -> Unit = { _, _, _ -> },
) {
    val data = remember(tableData) { tableData.initialized() }
    if (data.rows == 0 || data.cols == 0) return

    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = Color.White, fontSize = 14.sp)
    val density = LocalDensity.current
    val cellHPx = with(density) { CELL_H_PADDING.toPx() }
    val cellVPx = with(density) { CELL_V_PADDING.toPx() }

    var editingCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var editingText by remember { mutableStateOf("") }
    var isCellEditing by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
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

            fun hitTestCol(px: Float): Int? {
                var acc = 0f
                for (c in layout.columnWidths.indices) {
                    acc += layout.columnWidths[c]
                    if (px < acc) return c
                }
                return null
            }
            fun hitTestRow(px: Float): Int? {
                var acc = 0f
                for (r in layout.rowHeights.indices) {
                    acc += layout.rowHeights[r]
                    if (px < acc) return r
                }
                return null
            }

            val touchModifier = if (isCellEditing) {
                Modifier.pointerInput(layout) {
                    detectTapGestures(
                        onTap = { offset ->
                            val row = hitTestRow(offset.y)
                            val col = hitTestCol(offset.x)
                            if (row != null && col != null) {
                                editingCell?.let { (r, c) ->
                                    if (r != row || c != col) {
                                        onCellChange(r, c, editingText)
                                    }
                                }
                                editingCell = Pair(row, col)
                                editingText = data.cells[row][col]
                            }
                        },
                    )
                }
            } else Modifier

            val scrollContent: @Composable (Modifier) -> Unit = { canvasModifier ->
                Box(modifier = canvasModifier) {
                    Canvas(modifier = Modifier.fillMaxSize().then(touchModifier)) {
                        drawTableContent(layout, textLayouts, cellHPx, cellVPx)
                    }

                    editingCell?.let { (r, c) ->
                        val cellXDp = with(density) { layout.columnWidths.take(c).sum().toDp() }
                        val cellYDp = with(density) { layout.rowHeights.take(r).sum().toDp() }
                        val cellWDp = with(density) { layout.columnWidths[c].toDp() }
                        val cellHDp = with(density) { layout.rowHeights[r].toDp() }

                        Box(
                            modifier = Modifier
                                .offset(x = cellXDp, y = cellYDp)
                                .size(width = cellWDp, height = cellHDp)
                        ) {
                            CellEditor(
                                text = editingText,
                                onTextChange = { editingText = it },
                                onDone = {
                                    onCellChange(r, c, editingText)
                                    editingCell = null
                                    isCellEditing = false
                                },
                            )
                        }
                    }
                }
            }

            if (layout.totalWidth > screenWidthPx) {
                val totalWidthDp = with(density) { layout.totalWidth.toDp() }
                scrollContent(
                    Modifier.horizontalScroll(scrollState).width(totalWidthDp).height(totalHeightDp)
                )
            } else {
                scrollContent(
                    Modifier.fillMaxWidth().height(totalHeightDp)
                )
            }
        }

        if (isEditing) {
            TableToolbar(
                onDeleteModule = onDeleteModule,
                onAlignColumn = onAlignColumn,
                isCellEditing = isCellEditing,
                onToggleCellEditing = { isCellEditing = !isCellEditing },
            )
        }
    }
}

@Composable
private fun CellEditor(
    text: String,
    onTextChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    var tfValue by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    val focusRequester = remember { FocusRequester() }
    var hasBeenFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BasicTextField(
        value = tfValue,
        onValueChange = { newValue ->
            if (newValue.text.contains('\n')) {
                val clean = newValue.text.replace("\n", "")
                onTextChange(clean)
                onDone()
                return@BasicTextField
            }
            tfValue = newValue
            onTextChange(newValue.text)
        },
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF3A3A3A))
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    hasBeenFocused = true
                } else if (hasBeenFocused) {
                    // 只在已经获得过焦点之后再次失去焦点时才提交
                    onDone()
                }
            },
        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
        cursorBrush = SolidColor(Color.White),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                innerTextField()
            }
        },
    )
}

@Composable
private fun TableToolbar(
    onDeleteModule: () -> Unit,
    onAlignColumn: (colIndex: Int, align: TextAlign) -> Unit,
    isCellEditing: Boolean = false,
    onToggleCellEditing: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(Color(0xFF2A2A2A))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            "L" to TextAlign.Start,
            "C" to TextAlign.Center,
            "R" to TextAlign.End,
        ).forEach { (label, align) ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onAlignColumn(0, align) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = Color(0xFFB3B3B3),
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Edit button — toggles cell editing mode
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isCellEditing) Color(0xFF4A90D9) else Color.Transparent)
                .clickable(onClick = onToggleCellEditing),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Create,
                contentDescription = "编辑单元格",
                tint = if (isCellEditing) Color.White else Color(0xFFB3B3B3),
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
            onClick = onDeleteModule,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "删除表格",
                tint = Color(0xFFB3B3B3),
                modifier = Modifier.size(18.dp),
            )
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
