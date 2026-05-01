package com.example.muse.ui.screen.edit

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muse.ui.theme.MuseTheme

@Composable
fun GeneralEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    gutterProvider: (lineIndex: Int) -> GutterItem = { GutterItem.Number(it + 1) },
    focusRequester: FocusRequester = remember { FocusRequester() },
    textStyle: TextStyle = TextStyle.Default,
    placeholderText: String? = null,
    editorViewModel: EditorViewModel = remember { EditorViewModel() },
    paragraphSpacingPx: Float = 0f,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { editorViewModel.updateContainerHeight(it.height.toFloat()) }
    ) {
        // Gutter — 共享 ViewModel 中的滚动/布局状态
        val vm = editorViewModel
        if (vm.textLayoutResult != null && vm.lineHeightPx > 0f) {
            Gutter(
                itemProvider = gutterProvider,
                totalLines = vm.textLayoutResult!!.lineCount,
                scrollOffsetPx = vm.scrollOffsetPx,
                lineHeightPx = vm.lineHeightPx,
                containerHeightPx = vm.containerHeightPx,
                textStyle = textStyle,
                modifier = Modifier.fillMaxHeight(),
                fixedWidth = 30.dp,
                centerContent = true,
                textLayoutResult = vm.textLayoutResult,
                useLogicalLines = true,
                adjustedLines = vm.adjustedLines,
            )
        }

        // Text editor — 同步状态到 ViewModel
        FixedCursorTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            focusRequester = focusRequester,
            textStyle = textStyle,
            placeholderText = placeholderText,
            editorViewModel = vm,
            paragraphSpacingPx = paragraphSpacingPx,
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1E1E1E
)
@Composable
private fun GeneralEditorPreview() {
    MuseTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
                .padding(16.dp)
        ) {
            GeneralEditor(
                value = TextFieldValue("你好\n第二行\n"),
                onValueChange = { },
                modifier = Modifier
                    .fillMaxSize()
                    .height(300.dp),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                ),
                placeholderText = "输入内容..."
            )
        }
    }
}
