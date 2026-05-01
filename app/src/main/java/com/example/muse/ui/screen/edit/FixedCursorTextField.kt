package com.example.muse.ui.screen.edit

import android.view.ViewConfiguration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FixedCursorTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    textStyle: TextStyle = TextStyle.Default,
    placeholderText: String? = null,
    editorViewModel: EditorViewModel? = null,
    paragraphSpacingPx: Float = 0f,
) {
    val isImeVisible = WindowInsets.isImeVisible
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var containerHeightPx by remember { mutableStateOf(0f) }
    var scrollOffsetPx by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var cursorVisible by remember { mutableStateOf(true) }
    var lineHeightPx by remember { mutableStateOf(0f) } // 新增：记录行高

    val density = LocalDensity.current
    val context = LocalContext.current
    // 修复：使用系统的触摸斜率
    val touchSlopPx = with(density) { ViewConfiguration.get(context).scaledTouchSlop.toFloat() }
    val textMeasurer = rememberTextMeasurer()

    // 统一行高：确保中文/英文行高一致
    val effectiveTextStyle = remember(textStyle) {
        textStyle.copy(lineHeight = textStyle.fontSize * 1.4f)
    }

    // 计算底部留白（用于”滚动超出最后一行”效果）
    // 当有有效文本布局时根据行高计算，否则使用默认值
    val bottomPaddingPx = remember(lineHeightPx, containerHeightPx) {
        if (lineHeightPx > 0f && containerHeightPx > 0f) {
            (containerHeightPx - lineHeightPx).coerceAtLeast(0f) // 至少一行高，留出整个视口
        } else {
            containerHeightPx // 未就绪时取视口高度（保守值）
        }
    }

    // 顶部留白（与底部留白对称）
    val topPaddingPx = remember(lineHeightPx, containerHeightPx) {
        if (lineHeightPx > 0f && containerHeightPx > 0f) {
            (containerHeightPx - lineHeightPx).coerceAtLeast(0f)
        } else {
            containerHeightPx
        }
    }

    // 计算段落间距调整后的行位置
    val adjustedLines = remember(textLayoutResult, paragraphSpacingPx) {
        if (textLayoutResult != null && paragraphSpacingPx > 0f) {
            textLayoutResult!!.buildAdjustedLines(paragraphSpacingPx)
        } else {
            null
        }
    }

    // 同步调整后的行位置到 ViewModel
    LaunchedEffect(adjustedLines) {
        if (editorViewModel != null) {
            editorViewModel.updateAdjustedLines(
                adjustedLines ?: emptyList(),
                if (adjustedLines != null) paragraphSpacingPx else 0f
            )
        }
    }



    // 更新行高（基于第一行）并同步到 ViewModel
    LaunchedEffect(textLayoutResult) {
        textLayoutResult?.let { r ->
            if (r.lineCount > 0) {
                lineHeightPx = (r.getLineBottom(0) - r.getLineTop(0))
                editorViewModel?.updateLayout(r, lineHeightPx)
            }
        }
    }

    // Cursor blink
    LaunchedEffect(isFocused) {
        if (!isFocused) { cursorVisible = false; return@LaunchedEffect }
        cursorVisible = true
        while (true) { delay(530); cursorVisible = !cursorVisible }
    }

    // 将 topPaddingPx 加入依赖，自动居中逻辑也会适配新的上下留白
    LaunchedEffect(
        textLayoutResult,
        value.selection,
        isDragging,
        isImeVisible,
        containerHeightPx,
        topPaddingPx,
        bottomPaddingPx
    ) {
        if (!isImeVisible || isDragging || textLayoutResult == null || containerHeightPx <= 0f) return@LaunchedEffect
        val r = textLayoutResult!!
        val cursorRect = r.getCursorRect(value.selection.start)
        val targetScroll = cursorRect.top - containerHeightPx / 2f + cursorRect.height / 2f
        val textHeight = r.size.height.toFloat()

        // 允许滚动范围涵盖从「只显示顶部留白」到「只显示底部留白」的所有位置
        val minScroll = minOf(-topPaddingPx, textHeight + bottomPaddingPx - containerHeightPx)
        val maxScroll = maxOf(-topPaddingPx, textHeight + bottomPaddingPx - containerHeightPx)

        scrollOffsetPx = targetScroll.coerceIn(minScroll, maxScroll)
        editorViewModel?.updateScroll(scrollOffsetPx)
    }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible && !isDragging) {
            // 手动触发一次居中（复用原有居中逻辑）
            val r = textLayoutResult ?: return@LaunchedEffect
            val cursorRect = r.getCursorRect(value.selection.start)
            val targetScroll = cursorRect.top - containerHeightPx / 2f + cursorRect.height / 2f
            val textHeight = r.size.height.toFloat()
            val totalH = topPaddingPx + textHeight + bottomPaddingPx
            val minScroll = if (totalH < containerHeightPx) -(containerHeightPx - totalH) / 2f else -topPaddingPx
            val maxScroll = max(0f, textHeight + bottomPaddingPx - containerHeightPx)
            scrollOffsetPx = targetScroll.coerceIn(minScroll, maxScroll)
            editorViewModel?.updateScroll(scrollOffsetPx)
        }
    }

    val currentValue by rememberUpdatedState(value)
    val currentAdjustedLines by rememberUpdatedState(adjustedLines)

    Box(modifier = modifier.clip(RoundedCornerShape(0.dp))) {
        // Layer 1: 隐藏的 BasicTextField —— 仅负责IME输入
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    containerHeightPx = it.height.toFloat()
                    editorViewModel?.updateContainerHeight(it.height.toFloat())
                }
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .drawWithContent { /* 隐藏绘制 */ },
                textStyle = effectiveTextStyle,
                cursorBrush = SolidColor(Color.Transparent),
                decorationBox = { inner -> inner() }
            )
        }

        // Layer 2: 自定义 Canvas 绘制（文本、行高亮、光标）
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = textLayoutResult ?: return@Canvas
            val cursorPos = value.selection.start.coerceIn(0, value.text.length)

            // ✅ 获取光标所在段落的所有视觉行
            val paragraphLines = if (value.text.isNotEmpty()) {
                r.getParagraphLines(cursorPos)
            } else {
                IntRange.EMPTY
            }
            // 绘制段落高亮（使用调整后的行位置）
            for (line in paragraphLines) {
                val (lineTop, lineBottom) = if (adjustedLines != null && line < adjustedLines.size) {
                    val adj = adjustedLines[line]
                    adj.top to adj.bottom
                } else {
                    r.getLineTop(line) to r.getLineBottom(line)
                }
                drawRect(
                    color = Color(0xFF444444),
                    topLeft = Offset(0f, lineTop - scrollOffsetPx),
                    size = Size(size.width, lineBottom - lineTop)
                )
            }

            // 修复：即便 textLayoutResult 为 null，也尝试绘制占位符
            if (r == null) {
                if (placeholderText != null && value.text.isEmpty()) {
                    val phLayout = textMeasurer.measure(
                        text = placeholderText,
                        style = effectiveTextStyle.copy(color = effectiveTextStyle.color.copy()),
                        constraints = Constraints(maxWidth = size.width.roundToInt())
                    )
                    drawText(phLayout, topLeft = Offset(0f, -scrollOffsetPx))
                }
                return@Canvas
            }

            // 以下正常绘制
            if (value.text.isNotEmpty()) {
                if (adjustedLines != null) {
                    // 逐行测量绘制，每行出现在调整后的 Y 位置
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
                // 空文本时绘制占位符
                val phLayout = textMeasurer.measure(
                    text = placeholderText,
                    style = effectiveTextStyle.copy(color = effectiveTextStyle.color.copy(alpha = 0.5F)),
                    constraints = Constraints(maxWidth = size.width.roundToInt())
                )
                drawText(phLayout, topLeft = Offset(0f, -scrollOffsetPx))
            }

            // 光标绘制
            if (isFocused && cursorVisible && cursorPos <= r.layoutInput.text.length) {
                val cursorRect = r.getCursorRect(cursorPos)
                val cursorY = adjustedLines?.let { lines ->
                    val cursorLine = r.getLineForOffset(cursorPos)
                    lines.getOrNull(cursorLine)?.let { adj ->
                        adj.top + (cursorRect.top - r.getLineTop(cursorLine))
                    }
                } ?: cursorRect.top   // 如果 adjustedLines 为空或取不到，退回原始 top

                drawRect(
                    color = Color.White,
                    topLeft = Offset(cursorRect.left, cursorY - scrollOffsetPx),
                    size = Size(max(2f, cursorRect.width), cursorRect.height)
                )
            }
        }

        // Layer 3: 触摸处理（点击选光标、拖拽滚动）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull() ?: continue
                            if (!ch.pressed) continue

                            // 修复：消费按下事件，防止穿透到底层 BasicTextField
                            ch.consume()

                            val startPos = ch.position
                            var lastPos = startPos
                            var totalY = 0f
                            var drag = false

                            while (true) {
                                val mev = awaitPointerEvent()
                                val mch = mev.changes.firstOrNull() ?: break

                                if (!mch.pressed) {
                                    // 修复：消费抬起事件
                                    mch.consume()

                                    if (!drag) {
                                        // 点击：设置光标到点击位置
                                        textLayoutResult?.let { r ->
                                            val tapX =
                                                mch.position.x.coerceIn(0f, r.size.width.toFloat())
                                            val tapAdjustedY = mch.position.y + scrollOffsetPx
                                            val cal2 = currentAdjustedLines
                                            val adjLine = cal2?.firstOrNull { tapAdjustedY in it.top..it.bottom }
                                            val tapY = if (adjLine != null) {
                                                // 精确命中某行
                                                r.getLineTop(adjLine.originalLineIndex) + (tapAdjustedY - adjLine.top)
                                            } else if (!cal2.isNullOrEmpty()) {
                                                // 点击在段落间隙：吸附到最近的可行行
                                                val nearest = cal2.minByOrNull { abs(it.top + (it.bottom - it.top) / 2f - tapAdjustedY) }
                                                if (nearest != null) {
                                                    val centerY = nearest.top + (nearest.bottom - nearest.top) / 2f
                                                    val mappedY = if (tapAdjustedY <= centerY) nearest.bottom - 1f else nearest.top
                                                    r.getLineTop(nearest.originalLineIndex) + (mappedY - nearest.top)
                                                } else {
                                                    tapAdjustedY.coerceIn(0f, r.size.height.toFloat())
                                                }
                                            } else {
                                                tapAdjustedY.coerceIn(0f, r.size.height.toFloat())
                                            }
                                            r.getOffsetForPosition(Offset(tapX, tapY))
                                                .takeIf { it != -1 }
                                                ?.let { off ->
                                                    onValueChange(
                                                        currentValue.copy(selection = TextRange(off))
                                                    )
                                                }
                                        }
                                        focusRequester.requestFocus()
                                    } else {
                                        // 拖拽结束：只结束拖拽状态，不移动光标（修复原 bug）
                                        isDragging = false
                                        // 如果希望拖拽后保持滚动位置（即不自动居中），则不需要额外操作
                                    }
                                    break
                                }

                                // 修复：消费移动事件
                                mch.consume()

                                val delta = mch.position - lastPos
                                lastPos = mch.position
                                totalY += delta.y

                                if (!drag && abs(totalY) > touchSlopPx) {
                                    drag = true
                                    isDragging = true
                                }

                                if (drag) {
                                    val textH =
                                        textLayoutResult?.size?.height?.toFloat() ?: continue
                                    // 应用顶部/底部留白限制
                                    val totalH = topPaddingPx + textH + bottomPaddingPx
                                    val minSc = if (totalH < containerHeightPx) {
                                        -(containerHeightPx - totalH) / 2f
                                    } else {
                                        -topPaddingPx
                                    }
                                    val maxSc = max(0f, textH + bottomPaddingPx - containerHeightPx)

                                    // 修复：滚动方向
                                    scrollOffsetPx =
                                        (scrollOffsetPx - delta.y).coerceIn(minSc, maxSc)
                                    editorViewModel?.updateScroll(scrollOffsetPx)
                                }
                            }
                        }
                    }
                }
        )
    }
}


// 判断某视觉行结束后是否是段落结束（即遇到 '\n' 或文本结尾）
internal fun TextLayoutResult.isParagraphEnd(line: Int): Boolean {
    if (line < 0 || line >= lineCount) return true
    val end = getLineEnd(line)          // 该行结束后的索引
    val text = layoutInput.text
    // 如果 end 在有效范围内，且前一个字符是换行符，则该行是段落结尾
    if (end > 0 && end <= text.length && text[end - 1] == '\n') return true
    // 如果 end 等于文本长度，说明是最后一行，也视为段落结尾
    return end >= text.length
}

// 构建调整后的行位置列表，在段落间插入 paragraphSpacingPx 间距
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

//获取光标所在段落的所有视觉行索引（闭区间）
private fun TextLayoutResult.getParagraphLines(offset: Int): IntRange {
    // 1. 定位光标所在的视觉行
    val cursorLine = getLineForOffset(offset.coerceIn(0, layoutInput.text.length))
    // 2. 向前查找段落起点
    var startLine = cursorLine
    while (startLine > 0 && !isParagraphEnd(startLine - 1)) {
        startLine--
    }
    // 3. 向后查找段落终点
    var endLine = cursorLine
    while (endLine < lineCount - 1 && !isParagraphEnd(endLine)) {
        endLine++
    }
    return startLine..endLine
}