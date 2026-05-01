package com.example.muse.ui.screen.edit

import android.content.res.Configuration
import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

@Composable
fun MaximizedEditScreen(
    title: String = "标题",
    onBackClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var tfValue by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }

    val density = LocalDensity.current
    val view = LocalView.current
    var keyboardHeightDp by remember { mutableStateOf(0.dp) }
    var isKeyboardOpen by remember { mutableStateOf(false) }

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
                    onValueChange = { tfValue = it },
                    modifier = Modifier
                        .fillMaxSize(),
                    gutterProvider = { GutterItem.OrderedNumber(it +1) },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    placeholderText = "输入内容...",
                    extraNewlines = 1
                )
            }
        }

        // "完成" button — only when keyboard is open
        AnimatedVisibility(
            visible = isKeyboardOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 37.dp, bottom = effectiveKeyboardHeightDp + 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .border(1.dp, Color(0xFF949494), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "完成",
                    color = Color.White,
                    fontSize = 14.sp
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
                .padding(end = 37.dp, bottom = 22.dp)
        ) {
            val keyboardController = LocalSoftwareKeyboardController.current
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

    // Autofocus on first composition to open keyboard
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1E1E1E
)
@Composable
private fun MaximizedEditScreenPreview() {
    MuseTheme(darkTheme = true) {
        MaximizedEditScreen()
    }
}
