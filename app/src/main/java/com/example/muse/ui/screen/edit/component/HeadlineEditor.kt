package com.example.muse.ui.screen.edit.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muse.ui.screen.edit.FixedCursorTextField

/** 根据标题等级返回对应字体大小 */
fun headingFontSize(level: Int): TextUnit = when (level) {
    1 -> 28.sp
    2 -> 22.sp
    3 -> 20.sp
    4 -> 18.sp
    5 -> 16.sp
    6 -> 14.sp
    else -> 22.sp
}

/**
 * 标题行内编辑器。
 *
 * @param text 当前文本内容（不包含 "# " 前缀）
 * @param onTextChange 文本变更回调
 * @param level 标题等级（1-6）
 * @param onLevelChange H2_6Button / "# " 前缀等级变更回调
 * @param showDrum 是否显示 H2_6Button（子标题显示，主标题不显示）
 * @param onFocusLost 失去焦点时的回调——参数为清理 "# " 前缀后的最终文本
 * @param placeholder 占位提示文本
 */
@Composable
fun HeadlineEditor(
    text: String,
    onTextChange: (String) -> Unit,
    level: Int,
    onLevelChange: (Int) -> Unit = {},
    showDrum: Boolean = true,
    modifier: Modifier = Modifier,
    onFocusLost: (String) -> Unit = {},
    placeholder: String = "",
) {
    val focusManager = LocalFocusManager.current
    // 不把 text 加入 keys —— 编辑期间外部 text 不会变化，避免重建 TextFieldValue 导致光标跳回开头
    var tfValue by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    var everFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDrum) {
            H2_6Button(
                level = level,
                onLevelChange = onLevelChange,
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        FixedCursorTextField(
            value = tfValue,
            onValueChange = { newValue ->
                // 检测到换行符（物理键盘回车）→ 结束编辑
                if (newValue.text.contains('\n')) {
                    val clean = newValue.text.replace("\n", "")
                    tfValue = tfValue.copy(text = clean)
                    onTextChange(clean)
                    val hashResult = HashKey.detectLevel(clean)
                    if (hashResult != null) onLevelChange(hashResult.first)
                    focusManager.clearFocus()
                    return@FixedCursorTextField
                }
                tfValue = newValue
                val hashResult = HashKey.detectLevel(newValue.text)
                if (hashResult != null) {
                    val (newLevel, _) = hashResult
                    onLevelChange(newLevel)
                }
                onTextChange(newValue.text)
            },
            modifier = Modifier.weight(1f),
            focusRequester = focusRequester,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = headingFontSize(level),
                fontWeight = FontWeight.Bold,
            ),
            placeholderText = placeholder,
            enableScroll = false,
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            onFocusChanged = { focused ->
                if (focused) {
                    everFocused = true
                } else if (everFocused) {
                    val hashResult = HashKey.detectLevel(tfValue.text)
                    val finalText = if (hashResult != null) {
                        val (_, clean) = hashResult
                        tfValue = tfValue.copy(text = clean)
                        onTextChange(clean)
                        clean
                    } else {
                        tfValue.text
                    }
                    onFocusLost(finalText)
                }
            },
        )
    }
}
