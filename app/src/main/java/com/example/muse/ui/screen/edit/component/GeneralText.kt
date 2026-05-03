package com.example.muse.ui.screen.edit.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 通用文本展示组件。使用 Gutter 装订线 + 逻辑行处理，与 GeneralEditor 一致的排版。
 */
@Composable
fun GeneralText(
    text: String,
    type: ModuleType,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        color = type.textColor,
        fontSize = type.fontSize,
        fontWeight = type.fontWeight,
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
                containerHeightPx = layoutResult.size.height.toFloat(),
                textStyle = textStyle,
                modifier = Modifier.fillMaxHeight(),
                fixedWidth = gutterWidthDp,
                centerContent = true,
                textLayoutResult = layoutResult,
                useLogicalLines = true,
            )

            Text(
                text = text,
                style = textStyle,
            )
        }
    }
}
