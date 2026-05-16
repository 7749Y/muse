package com.example.muse.ui.screen.edit.component.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
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
    onTableResize: (rows: Int, cols: Int) -> Unit = { _, _ -> },
) {
    val data = remember(tableData) { tableData.initialized() }
    if (data.rows == 0 || data.cols == 0) return

    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = Color.White, fontSize = 14.sp)
    val density = LocalDensity.current

    var editingCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var editingText by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val cellHPx = with(density) { CELL_H_PADDING.toPx() }
            val cellVPx = with(density) { CELL_V_PADDING.toPx() }

            val layout = remember(data, screenWidthPx) {
                calculateLayout(textMeasurer, data, screenWidthPx, textStyle, cellHPx, cellVPx)
            }

            val scrollState = rememberScrollState()
            val needsScroll = layout.totalWidth > screenWidthPx

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (needsScroll) Modifier.horizontalScroll(scrollState) else Modifier)
            ) {
                for (r in 0 until data.rows) {
                    Row(
                        modifier = Modifier.height(
                            with(density) { layout.rowHeights[r].toDp() }
                        )
                    ) {
                        for (c in 0 until data.cols) {
                            TableCell(
                                text = data.cells[r][c],
                                width = with(density) { layout.columnWidths[c].toDp() },
                                height = with(density) { layout.rowHeights[r].toDp() },
                                textAlign = data.columnAlignments.getOrNull(c)
                                    ?: TextAlign.Start,
                                textStyle = textStyle,
                                isEditingThis = editingCell == Pair(r, c),
                                onClick = {
                                    val clicked = Pair(r, c)
                                    if (editingCell != clicked) {
                                        editingCell?.let { (er, ec) ->
                                            onCellChange(er, ec, editingText)
                                        }
                                        editingCell = clicked
                                        editingText = data.cells[r][c]
                                    }
                                },
                                onTextChange = { editingText = it },
                                onDone = {
                                    // 防护：仅当仍指向当前单元格时才清除
                                    if (editingCell == Pair(r, c)) {
                                        onCellChange(r, c, editingText)
                                        editingCell = null
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        if (isEditing) {
            TableToolbar(
                rows = data.rows,
                cols = data.cols,
                onDeleteModule = onDeleteModule,
                onAlignColumn = onAlignColumn,
                onTableResize = onTableResize,
            )
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: Dp,
    height: Dp,
    textAlign: TextAlign,
    textStyle: TextStyle,
    isEditingThis: Boolean,
    onClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(0.5.dp, GRID_COLOR)
            .then(if (!isEditingThis) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        if (isEditingThis) {
            CellEditor(
                text = text,
                onTextChange = onTextChange,
                onDone = onDone,
            )
        } else {
            Text(
                text = text,
                style = textStyle.copy(textAlign = textAlign),
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = CELL_H_PADDING,
                        end = CELL_H_PADDING,
                        top = CELL_V_PADDING,
                        bottom = CELL_V_PADDING,
                    ),
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
    var hasBeenFocused by remember { mutableStateOf(false) }   // ← 新增标记

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
                    hasBeenFocused = true          // 获得焦点，标记
                } else if (hasBeenFocused) {
                    // 只有曾经获得过焦点，之后的失焦才代表真正退出
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
    rows: Int = 3,
    cols: Int = 3,
    onDeleteModule: () -> Unit = {},
    onAlignColumn: (colIndex: Int, align: TextAlign) -> Unit = { _, _ -> },
    onTableResize: (rows: Int, cols: Int) -> Unit = { _, _ -> },
) {
    var rowsText by remember(rows) { mutableStateOf(rows.toString()) }
    var colsText by remember(cols) { mutableStateOf(cols.toString()) }

    fun commitSize() {
        val newRows = (rowsText.toIntOrNull() ?: rows).coerceAtLeast(2)
        val newCols = (colsText.toIntOrNull() ?: cols).coerceAtLeast(1)
        rowsText = newRows.toString()
        colsText = newCols.toString()
        if (newRows != rows || newCols != cols) {
            onTableResize(newRows, newCols)
        }
    }

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

        val inputTextStyle = TextStyle(color = Color.White, fontSize = 12.sp)
        BasicTextField(
            value = rowsText,
            onValueChange = {
                if (it.all { c -> c.isDigit() }) rowsText = it
            },
            modifier = Modifier
                .width(30.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .onFocusChanged { if (!it.isFocused) commitSize() },
            textStyle = inputTextStyle.copy(textAlign = TextAlign.Center),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = { commitSize() }),
            cursorBrush = SolidColor(Color.White),
            decorationBox = { inner -> inner() },
        )
        Text(" × ", color = Color(0xFFB3B3B3), fontSize = 12.sp)
        BasicTextField(
            value = colsText,
            onValueChange = {
                if (it.all { c -> c.isDigit() }) colsText = it
            },
            modifier = Modifier
                .width(30.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .onFocusChanged { if (!it.isFocused) commitSize() },
            textStyle = inputTextStyle.copy(textAlign = TextAlign.Center),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = { commitSize() }),
            cursorBrush = SolidColor(Color.White),
            decorationBox = { inner -> inner() },
        )

        Spacer(modifier = Modifier.width(8.dp))

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

internal fun calculateLayout(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
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

    // 每列取 max(自然宽度, screen/5)，绝不压缩低于 minColWidth
    val colWidths = FloatArray(data.cols) { c -> maxOf(naturalWidths[c], minColWidth) }
    var totalWidth = colWidths.sum()

    if (totalWidth < screenWidthPx) {
        // 总宽度小于屏幕 → 等比拉伸填满
        val ratio = screenWidthPx / totalWidth
        for (c in 0 until data.cols) colWidths[c] *= ratio
        totalWidth = screenWidthPx
    }

    // 施加最大约束
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
