package com.example.muse.ui.screen.edit

import android.view.ViewConfiguration
import androidx.compose.foundation.layout.Box
import com.example.muse.ui.screen.edit.core.EditorScrollState
import com.example.muse.ui.screen.edit.core.EditorViewModel
import com.example.muse.ui.screen.edit.core.buildAdjustedLines
import com.example.muse.ui.screen.edit.render.EditorCanvas
import com.example.muse.ui.screen.edit.render.EditorTouchHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.max

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
    val scope = rememberCoroutineScope()
    val scrollState = remember { EditorScrollState(scope) }
    var textLayoutResult by scrollState::textLayoutResult
    var containerHeightPx by scrollState::containerHeightPx
    var scrollOffsetPx by scrollState::scrollOffsetPx
    var isDragging by scrollState::isDragging
    var adjustedLines by scrollState::adjustedLines

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

    // 同步调整后的行位置到 ViewModel
    LaunchedEffect(adjustedLines) {
        editorViewModel?.updateAdjustedLines(
            adjustedLines ?: emptyList(),
            if (adjustedLines != null) paragraphSpacingPx else 0f
        )
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
//    LaunchedEffect(isFocused) {
//        if (!isFocused) { cursorVisible = false; return@LaunchedEffect }
//        cursorVisible = true
//        while (true) { delay(530); cursorVisible = !cursorVisible }
//    }

// 光标始终显示，不闪烁
    LaunchedEffect(isFocused) {
        cursorVisible = isFocused
    }
    // 将 topPaddingPx 加入依赖，自动居中逻辑也会适配新的上下留白
    LaunchedEffect(
        textLayoutResult,
        value.selection,
        isDragging,
        isImeVisible,
        containerHeightPx,
        topPaddingPx,
        bottomPaddingPx,
        adjustedLines,
    ) {
        if (!isImeVisible || isDragging || textLayoutResult == null || containerHeightPx <= 0f) return@LaunchedEffect
        val r = textLayoutResult!!
        val cursorRect = r.getCursorRect(value.selection.start)

        val al1 = adjustedLines
        val (cursorTop, textHeight) = if (al1 != null) {
            val cursorLine = r.getLineForOffset(value.selection.start)
            val adj = al1.getOrNull(cursorLine)
            if (adj != null) {
                val adjCursorTop = adj.top + (cursorRect.top - r.getLineTop(cursorLine))
                adjCursorTop to al1.last().bottom
            } else {
                cursorRect.top to al1.last().bottom
            }
        } else {
            cursorRect.top to r.size.height.toFloat()
        }

        val targetScroll = cursorTop - containerHeightPx / 2f + cursorRect.height / 2f

        // 允许滚动范围涵盖从「只显示顶部留白」到「只显示底部留白」的所有位置
        val minScroll = minOf(-topPaddingPx, textHeight + bottomPaddingPx - containerHeightPx)
        val maxScroll = maxOf(-topPaddingPx, textHeight + bottomPaddingPx - containerHeightPx)

        scrollOffsetPx = targetScroll.coerceIn(minScroll, maxScroll)
        editorViewModel?.updateScroll(scrollOffsetPx)
    }

    LaunchedEffect(isImeVisible, adjustedLines) {
        if (isImeVisible && !isDragging) {
            // 手动触发一次居中（复用原有居中逻辑）
            val r = textLayoutResult ?: return@LaunchedEffect
            val cursorRect = r.getCursorRect(value.selection.start)

            val al2 = adjustedLines
            val (cursorTop, textHeight) = if (al2 != null) {
                val cursorLine = r.getLineForOffset(value.selection.start)
                val adj = al2.getOrNull(cursorLine)
                if (adj != null) {
                    val adjCursorTop = adj.top + (cursorRect.top - r.getLineTop(cursorLine))
                    adjCursorTop to al2.last().bottom
                } else {
                    cursorRect.top to al2.last().bottom
                }
            } else {
                cursorRect.top to r.size.height.toFloat()
            }

            val targetScroll = cursorTop - containerHeightPx / 2f + cursorRect.height / 2f
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
                onTextLayout = { layoutResult ->
                    textLayoutResult = layoutResult
                    // 立即计算调整后的行位置，消除闪烁
                    adjustedLines = if (paragraphSpacingPx > 0f) {
                        layoutResult.buildAdjustedLines(paragraphSpacingPx)
                    } else null

                    // 同步给 ViewModel
                    editorViewModel?.updateAdjustedLines(
                        adjustedLines ?: emptyList(),
                        if (adjustedLines != null) paragraphSpacingPx else 0f
                    )
                },
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
        EditorCanvas(
            textLayoutResult = textLayoutResult,
            scrollOffsetPx = scrollOffsetPx,
            adjustedLines = adjustedLines,
            value = value,
            isFocused = isFocused,
            cursorVisible = cursorVisible,
            textMeasurer = textMeasurer,
            effectiveTextStyle = effectiveTextStyle,
            placeholderText = placeholderText,
            modifier = Modifier.fillMaxSize(),
        )

        // Layer 3: 触摸处理（点击选光标、拖拽滚动）
        EditorTouchHandler(
            scrollState = scrollState,
            value = value,
            onValueChange = onValueChange,
            focusRequester = focusRequester,
            isImeVisible = isImeVisible,
            touchSlopPx = touchSlopPx,
            topPaddingPx = topPaddingPx,
            bottomPaddingPx = bottomPaddingPx,
            editorViewModel = editorViewModel,
        )
    }
}

