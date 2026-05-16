package com.example.muse.ui.screen.edit.component.widget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * RadialMenu 测试窗口 —— 记录长按、滑动选中、松手触发等事件。
 *
 * 使用 android.R.drawable 内的系统图标作为菜单项占位图标，
 * 如果你的模块无法直接引用 android.R.drawable，可替换为自己项目中的资源 ID。
 */
@Composable
fun RadialMenuTestScreen() {
    // 日志列表
    val logEntries = remember { mutableStateListOf<String>() }
    // 当前高亮的菜单项索引（用于展示，不从组件内部读取，这里通过回调间接获得）
    var selectedLabel by remember { mutableStateOf("无") }

    fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        logEntries.add("[$time] $message")
    }

    // 构造菜单项，使用系统图标作为占位
    val menuItems = listOf(
        RadialMenuItem(
            label = "编辑",
            iconRes = android.R.drawable.ic_menu_edit,
            onClick = {
                addLog("点击了「编辑」")
                selectedLabel = "编辑"
            }
        ),
        RadialMenuItem(
            label = "分享",
            iconRes = android.R.drawable.ic_menu_share,
            onClick = {
                addLog("点击了「分享」")
                selectedLabel = "分享"
            }
        ),
        RadialMenuItem(
            label = "删除",
            iconRes = android.R.drawable.ic_menu_delete,
            onClick = {
                addLog("点击了「删除」")
                selectedLabel = "删除"
            }
        ),
        RadialMenuItem(
            label = "置顶",
            iconRes = android.R.drawable.ic_menu_upload, // 仅做示意
            onClick = {
                addLog("点击了「置顶」")
                selectedLabel = "置顶"
            }
        ),
        RadialMenuItem(
            label = "复制",
            iconRes = android.R.drawable.ic_menu_save,
            onClick = {
                addLog("点击了「复制」")
                selectedLabel = "复制"
            }
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "RadialMenu 测试",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 测试控制区（居中显示菜单 + 提示）
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // 实际菜单
                RadialMenu(
                    radius = 120.dp,
                    centerButton = {
                        // 中心按钮样式，同时可触发日志
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .then(
                                    Modifier.pointerInput(Unit) {
                                        // 此处仅作日志记录，手势由 RadialMenu 内部处理
                                        awaitPointerEventScope {
                                            awaitPointerEvent()
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_add),
                                contentDescription = "菜单",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    },
                    items = menuItems
                )

                // 提示文字
                if (logEntries.isEmpty()) {
                    Text(
                        "长按中间按钮，拖拽选择功能",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 当前选中项显示
        Text(
            text = "当前触发项：$selectedLabel",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 日志区域标题与清空按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("操作日志", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { logEntries.clear() }) {
                Text("清空")
            }
        }

        // 日志列表（自动滚动到最新）
        val listState = rememberLazyListState()
        LaunchedEffect(logEntries.size) {
            if (logEntries.isNotEmpty()) {
                listState.animateScrollToItem(logEntries.size - 1)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logEntries) { log ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = log,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}