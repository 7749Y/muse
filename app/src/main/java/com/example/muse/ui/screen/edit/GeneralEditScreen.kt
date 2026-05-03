package com.example.muse.ui.screen.edit

import android.content.res.Configuration
import android.graphics.Rect
import android.view.ViewTreeObserver
import com.example.muse.ui.screen.edit.component.GutterItem
import com.example.muse.ui.screen.edit.component.JoystickButton
import com.example.muse.ui.screen.edit.component.ToolbarButton
import com.example.muse.ui.screen.edit.core.EditorViewModel
import com.example.muse.ui.screen.edit.core.UndoRedoManager
import com.example.muse.ui.screen.edit.core.moveCursor
import com.example.muse.ui.screen.edit.core.moveCursorLine
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muse.R
import com.example.muse.ui.theme.MuseTheme
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun GeneralEditScreen(
    title: String = "标题",
    onBackClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    gutterProvider: (lineIndex: Int) -> GutterItem = { GutterItem.Number(it + 1) },
    textStyle: TextStyle = TextStyle(
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    ),
    paragraphSpacingPx: Float = 50f,
) {
    var tfValue by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    val editorViewModel = remember { EditorViewModel() }
    val undoManager = remember {
        UndoRedoManager<TextFieldValue>(maxCapacity = 500) { a, b ->
            a.text == b.text && a.selection == b.selection
        }
    }
    var isUndoRedoing by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val view = LocalView.current
    var keyboardHeightDp by remember { mutableStateOf(0.dp) }
    var isKeyboardOpen by remember { mutableStateOf(false) }

    // 摇杆状态：归一化方向向量（-1..1），松手时为 (0,0)
    var joystickDx by remember { mutableStateOf(0f) }
    var joystickDy by remember { mutableStateOf(0f) }

    val currentTf by rememberUpdatedState(tfValue)
    val currentVm by rememberUpdatedState(editorViewModel)

    // 摇杆方向 → 光标自动重复移动
    LaunchedEffect(joystickDx, joystickDy) {
        val threshold = 0.3f
        if (abs(joystickDx) < threshold && abs(joystickDy) < threshold) return@LaunchedEffect

        // 立即触发第一次移动
        val handler: (TextFieldValue) -> Unit = { tfValue = it }
        val isHorizontal = abs(joystickDx) > abs(joystickDy)
        if (isHorizontal) {
            if (joystickDx < 0) moveCursor(-1, currentTf, handler) else moveCursor(1, currentTf, handler)
        } else {
            if (joystickDy < 0) moveCursorLine(-1, currentTf, currentVm, handler) else moveCursorLine(1, currentTf, currentVm, handler)
        }

        delay(200L) // 初始延迟

        while (true) {
            if (abs(joystickDx) < threshold && abs(joystickDy) < threshold) break
            if (isHorizontal) {
                if (joystickDx < 0) moveCursor(-1, currentTf, handler) else moveCursor(1, currentTf, handler)
            } else {
                if (joystickDy < 0) moveCursorLine(-1, currentTf, currentVm, handler) else moveCursorLine(1, currentTf, currentVm, handler)
            }
            delay(80L)
        }
    }

    // Detect keyboard open/close via ViewTreeObserver
    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            val visible = keypadHeight > screenHeight * 0.15
            isKeyboardOpen = visible
            keyboardHeightDp = with(density) { keypadHeight.toDp() }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    // 导航栏底部高度，从 keypadHeight 中扣除得到纯键盘高度
    val bottomNavBarInsetDp = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val effectiveKeyboardHeightDp = (keyboardHeightDp - bottomNavBarInsetDp).coerceAtLeast(0.dp)

    val navBarHeight = if (isKeyboardOpen) 22.dp else 44.dp
    val titleFontSize = if (isKeyboardOpen) 12.sp else 24.sp

    val keyboardController = LocalSoftwareKeyboardController.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Navigation bar — shrinks when keyboard opens
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navBarHeight)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = !isKeyboardOpen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }

                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    letterSpacing = (-0.48).sp
                )

                AnimatedVisibility(
                    visible = !isKeyboardOpen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = onDoneClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = "Done",
                            tint = Color.White
                        )
                    }
                }
            }

            // Edit area — padding bottom = keyboard height when open
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(0.dp))
                    .padding(end = 5.dp)
                    .padding(bottom = effectiveKeyboardHeightDp)
            ) {
                GeneralEditor(
                    value = tfValue,
                    onValueChange = { newValue ->
                        val actualChange = newValue.text != tfValue.text ||
                                newValue.selection != tfValue.selection
                        if (actualChange && !isUndoRedoing) {
                            undoManager.push(tfValue)
                        }
                        tfValue = newValue
                    },
                    modifier = Modifier
                        .fillMaxSize(),
                    gutterProvider = gutterProvider,
                    textStyle = textStyle,
                    placeholderText = "输入内容...",
                    paragraphSpacingPx = paragraphSpacingPx,
                    editorViewModel = editorViewModel,
                )
            }
        }

        // 工具栏 — 键盘打开时显示
        AnimatedVisibility(
            visible = isKeyboardOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = effectiveKeyboardHeightDp + 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarButton(
                    label = "重做",
                    repeatOnHold = false,
                    onAction = {
                        isUndoRedoing = true
                        undoManager.redo(tfValue)?.let { next -> tfValue = next }
                        isUndoRedoing = false
                    }
                )
                ToolbarButton(
                    label = "←",
                    repeatOnHold = true,
                    onAction = { moveCursor(-1, tfValue) { tfValue = it } }
                )
                ToolbarButton(
                    label = "↑",
                    repeatOnHold = true,
                    onAction = { moveCursorLine(-1, tfValue, editorViewModel) { tfValue = it } }
                )
                JoystickButton(
                    onJoystickMove = { dx, dy ->
                        joystickDx = dx
                        joystickDy = dy
                    }
                )
                ToolbarButton(
                    label = "↓",
                    repeatOnHold = true,
                    onAction = { moveCursorLine(1, tfValue, editorViewModel) { tfValue = it } }
                )
                ToolbarButton(
                    label = "→",
                    repeatOnHold = true,
                    onAction = { moveCursor(1, tfValue) { tfValue = it } }
                )
                ToolbarButton(
                    label = "撤销",
                    repeatOnHold = false,
                    onAction = {
                        isUndoRedoing = true
                        undoManager.undo(tfValue)?.let { previous -> tfValue = previous }
                        isUndoRedoing = false
                    }
                )
            }
        }
        // Keyboard open button — only when keyboard is closed
        AnimatedVisibility(
            visible = !isKeyboardOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 22.dp)
        ) {
            IconButton(
                onClick = {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                },
                modifier = Modifier
                    .border(
                        1.dp, Color(0xFF949494), RoundedCornerShape(8.dp)
                    )
                    .size(40.dp)
            ) {
                Icon(
                    painter = painterResource(
                        R.drawable.ic_add
                    ),
                    contentDescription = "打开键盘",
                    tint = Color.White
                )
            }
        }

    }

}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1E1E1E
)
@Composable
private fun GeneralEditScreenPreview() {
    MuseTheme(darkTheme = true) {
        GeneralEditScreen()
    }
}
