package com.example.muse.ui.screen.edit

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muse.R
import com.example.muse.ui.screen.edit.component.GutterItem
import com.example.muse.ui.screen.edit.component.RadialMenu
import com.example.muse.ui.screen.edit.component.RadialMenuItem
import com.example.muse.ui.theme.MuseTheme

private data class EditorConfig(
    val title: String,
    val gutterProvider: (Int) -> GutterItem,
    val textStyle: TextStyle,
    val paragraphSpacingPx: Float = 50f,
)

private val listConfig = EditorConfig(
    title = "列表",
    gutterProvider = { GutterItem.Bullet },
    textStyle = TextStyle(
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    )
)

private val codeConfig = EditorConfig(
    title = "代码块",
    gutterProvider = { GutterItem.Number(it + 1) },
    textStyle = TextStyle(
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    paragraphSpacingPx = 0f,
)

private val quoteConfig = EditorConfig(
    title = "引用",
    gutterProvider = { GutterItem.QuoteLine },
    textStyle = TextStyle(
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold
    ),
    paragraphSpacingPx = 20f,
)

@Composable
fun EditScreen(
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var editingConfig by remember { mutableStateOf<EditorConfig?>(null) }

    if (editingConfig != null) {
        val config = editingConfig!!
        GeneralEditScreen(
            title = config.title,
            onBackClick = { editingConfig = null },
            onDoneClick = { editingConfig = null },
            gutterProvider = config.gutterProvider,
            textStyle = config.textStyle,
            paragraphSpacingPx = config.paragraphSpacingPx,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
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

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onSaveClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = "Save",
                        tint = Color.White
                    )
                }
            }

            // Title & Tag row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 37.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title input
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "标题",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }

                // Tag toggle
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(40.dp)
                        .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Add tag",
                            tint = Color(0xFFB3B3B3),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "tag",
                            fontSize = 16.sp,
                            color = Color(0xFFB3B3B3)
                        )
                    }
                }
            }
        }

        // 径向菜单 — 按下滑动选择模块类型
        RadialMenu(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 94.dp, bottom = 152.dp),
            centerButton = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .border(4.dp, Color.White, RoundedCornerShape(100.dp))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = "Add module",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            items = listOf(
                RadialMenuItem("列表", R.drawable.ic_list) { editingConfig = listConfig },
                RadialMenuItem("引用", R.drawable.ic_quote) { editingConfig = quoteConfig },
                RadialMenuItem("图片", R.drawable.ic_image) { /* TODO: 添加图片模块 */ },
                RadialMenuItem("表格", R.drawable.ic_table) { /* TODO: 添加表格模块 */ },
                RadialMenuItem("子标题", R.drawable.ic_subheading) { /* TODO: 添加子标题模块 */ },
                RadialMenuItem("代码块", R.drawable.ic_code) { editingConfig = codeConfig },
            )
        )
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1E1E1E
)
@Composable
private fun EditScreenPreview() {
    MuseTheme(darkTheme = true) {
        EditScreen()
    }
}
