package com.example.muse.ui.screen.edit.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * 通用文本展示组件。根据模块类型选择合适的装订线和字体样式。
 */
@Composable
fun GeneralText(
    text: String,
    type: ModuleType,
    modifier: Modifier = Modifier,
) {
    val annotatedString = remember(text, type) {
        buildStyledText(text, type)
    }

    if (type == ModuleType.Quote) {
        // 引用：左侧竖线 + 文本
        Row(modifier = modifier) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .padding(end = 8.dp)
                    .align(Alignment.Top)
                    .background(Color.Gray.copy(alpha = 0.5f))
            )
            Text(
                text = annotatedString,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Text(
            text = annotatedString,
            modifier = modifier,
        )
    }
}

private fun buildStyledText(text: String, type: ModuleType): AnnotatedString = buildAnnotatedString {
    // 列表类型加前缀圆点
    if (type == ModuleType.List) {
        withStyle(SpanStyle(
            color = Color.Gray,
            fontSize = type.fontSize,
            fontWeight = type.fontWeight,
        )) {
            append("•  ")
        }
    }

    withStyle(SpanStyle(
        color = type.textColor,
        fontSize = type.fontSize,
        fontWeight = type.fontWeight,
    )) {
        append(text)
    }
}
