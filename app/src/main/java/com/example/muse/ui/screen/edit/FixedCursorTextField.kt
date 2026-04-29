package com.example.muse.ui.screen.edit

import android.view.ViewConfiguration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun FixedCursorTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    textStyle: TextStyle = TextStyle.Default,
    placeholderText: String? = null,
) {
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

    // 计算底部留白（用于“滚动超出最后一行”效果）
    // 当有有效文本布局时根据行高计算，否则使用默认值
    val bottomPaddingPx = remember(lineHeightPx, containerHeightPx) {
        if (lineHeightPx > 0f && containerHeightPx > 0f) {
            (containerHeightPx - lineHeightPx).coerceAtLeast(0f) // 至少一行高，留出整个视口
        } else {
            containerHeightPx // 未就绪时取视口高度（保守值）
        }
    }

    // 更新行高（基于第一行）
    LaunchedEffect(textLayoutResult) {
        textLayoutResult?.let { r ->
            if (r.lineCount > 0) {
                lineHeightPx = r.getLineBottom(0) - r.getLineTop(0)
            }
        }
    }

    // Cursor blink
    LaunchedEffect(isFocused) {
        if (!isFocused) { cursorVisible = false; return@LaunchedEffect }
        cursorVisible = true
        while (true) { delay(530); cursorVisible = !cursorVisible }
    }

    // 自动居中光标（当选区变化时，但排除手动拖拽期间）
    LaunchedEffect(textLayoutResult, value.selection, isDragging, containerHeightPx, bottomPaddingPx) {
        if (isDragging || textLayoutResult == null || containerHeightPx <= 0f) return@LaunchedEffect
        val r = textLayoutResult!!
        val cursorRect = r.getCursorRect(value.selection.start)
        val targetScroll = cursorRect.top - containerHeightPx / 2f + cursorRect.height / 2f
        val textHeight = r.size.height.toFloat()
        val totalHeight = textHeight + bottomPaddingPx
        // 修正 minScroll/maxScroll 包含底部留白
        val minScroll = if (totalHeight < containerHeightPx) -(containerHeightPx - totalHeight) / 2f else 0f
        val maxScroll = max(0f, totalHeight - containerHeightPx)
        scrollOffsetPx = targetScroll.coerceIn(minScroll, maxScroll)
    }

    Box(modifier = modifier.clip(RoundedCornerShape(0.dp))) {
        // Layer 1: 隐藏的 BasicTextField —— 仅负责IME输入
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerHeightPx = it.height.toFloat() }
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
                textStyle = TextStyle(color = Color.White),
                cursorBrush = SolidColor(Color.Transparent),
                decorationBox = { inner -> inner() }
            )
        }

        // Layer 2: 自定义 Canvas 绘制（文本、行高亮、光标）
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = textLayoutResult
            val cursorPos = value.selection.start
            val hasText = value.text.isNotEmpty()

            // 修复：即便 textLayoutResult 为 null，也尝试绘制占位符
            if (r == null) {
                if (placeholderText != null && !isFocused) {
                    val phLayout = textMeasurer.measure(
                        text = placeholderText,
                        style = textStyle.copy(color = Color.Gray),
                        constraints = Constraints(maxWidth = size.width.roundToInt())
                    )
                    drawText(phLayout, topLeft = Offset(0f, -scrollOffsetPx))
                }
                return@Canvas
            }

            // 以下正常绘制
            if (hasText) {
                // 当前行高亮
                val line = r.getLineForOffset(cursorPos)
                val lineTop = r.getLineTop(line)
                val lineBottom = r.getLineBottom(line)
                drawRect(
                    color = Color(0xFF444444),
                    topLeft = Offset(0f, lineTop - scrollOffsetPx),
                    size = Size(size.width, lineBottom - lineTop)
                )
                // 文本内容
                drawText(r, topLeft = Offset(0f, -scrollOffsetPx))
            } else if (placeholderText != null && !isFocused) {
                // 空文本时绘制占位符
                val phLayout = textMeasurer.measure(
                    text = placeholderText,
                    style = textStyle.copy(color = Color.Gray),
                    constraints = Constraints(maxWidth = size.width.roundToInt())
                )
                drawText(phLayout, topLeft = Offset(0f, -scrollOffsetPx))
            }

            // 光标绘制
            if (isFocused && cursorVisible && cursorPos <= r.layoutInput.text.length) {
                val cursorRect = r.getCursorRect(cursorPos)
                drawRect(
                    color = Color.White,
                    topLeft = Offset(cursorRect.left, cursorRect.top - scrollOffsetPx),
                    size = Size(max(2f, cursorRect.width), cursorRect.height)
                )
            }
        }

        // Layer 3: 触摸处理（点击选光标、拖拽滚动）
        val currentValue by rememberUpdatedState(value)
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
                                            val tapX = mch.position.x.coerceIn(0f, r.size.width.toFloat())
                                            val tapY = (mch.position.y + scrollOffsetPx)
                                                .coerceIn(0f, r.size.height.toFloat())
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
                                    val textH = textLayoutResult?.size?.height?.toFloat() ?: continue
                                    val totalH = textH + bottomPaddingPx
                                    val minSc = if (totalH < containerHeightPx)
                                        -(containerHeightPx - totalH) / 2f
                                    else
                                        0f
                                    val maxSc = max(0f, totalH - containerHeightPx)

                                    // 修复：滚动方向（手指向下移动时 scrollOffset 应增大，看到下面内容）
                                    scrollOffsetPx = (scrollOffsetPx + delta.y).coerceIn(minSc, maxSc)
                                }
                            }
                        }
                    }
                }
        )
    }
}