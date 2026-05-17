package com.example.muse.ui.screen.edit.component.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muse.R
import com.example.muse.data.local.PrimaryTagEntity
import com.example.muse.data.local.SecondaryTagEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagDialog(
    primaryTags: List<PrimaryTagEntity>,
    secondaryTags: List<SecondaryTagEntity>,
    selectedPrimaryTagId: Long?,
    selectedSecondaryTagIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (primaryTagId: Long?, secondaryTagIds: List<Long>) -> Unit,
    onCreateSecondaryTag: suspend (String) -> Long,
    modifier: Modifier = Modifier,
) {
    var selPrimaryId by remember { mutableStateOf(selectedPrimaryTagId) }
    val selSecondaryIds = remember { mutableStateOf(selectedSecondaryTagIds) }
    val localTags = remember { mutableStateListOf<SecondaryTagEntity>() }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(secondaryTags) {
        localTags.clear()
        localTags.addAll(secondaryTags)
    }

    val filteredTags = remember(searchQuery, localTags.size) {
        if (searchQuery.isBlank()) localTags.toList()
        else localTags.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = modifier.fillMaxSize(),//.imePadding(),
    ) {
        // 半透明遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        // 对话框
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                .padding(16.dp),
        ) {
            // ── 关闭按钮 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "关闭",
                        tint = Color(0xFFB3B3B3),
                    )
                }
            }

            // ── 一级标签（分类）──
            Text(
                text = "分类",
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                primaryTags.forEach { tag ->
                    val selected = selPrimaryId == tag.id
                    TagChip(
                        name = tag.name,
                        selected = selected,
                        onClick = {
                            selPrimaryId = if (selected) null else tag.id
                        },
                    )
                }
                if (primaryTags.isEmpty()) {
                    Text(
                        text = "暂无分类",
                        color = Color(0xFF666666),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 二级标签 ──
            Text(
                text = "标签",
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // 搜索 + 确认按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                        .background(Color.Transparent, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "搜索或新建标签",
                            color = Color(0xFF666666),
                            fontSize = 14.sp,
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                        ),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                IconButton(
                    onClick = {
                        val name = searchQuery.trim()
                        if (name.isNotBlank()) {
                            scope.launch {
                                val existing = localTags.find {
                                    it.name.equals(name, ignoreCase = true)
                                }
                                if (existing != null) {
                                    selSecondaryIds.value = selSecondaryIds.value + existing.id
                                } else {
                                    val newId = onCreateSecondaryTag(name)
                                    val newTag = SecondaryTagEntity(id = newId, name = name)
                                    localTags.add(newTag)
                                    selSecondaryIds.value = selSecondaryIds.value + newId
                                }
                                searchQuery = ""
                            }
                        }
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = "确认标签",
                        tint = Color(0xFFB3B3B3),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 二级标签列表
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (filteredTags.isEmpty() && searchQuery.isNotBlank()) {
                    Text(
                        text = "输入新名称后点击 ✓ 创建",
                        color = Color(0xFF666666),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    filteredTags.forEach { tag ->
                        val selected = tag.id in selSecondaryIds.value
                        TagChip(
                            name = tag.name,
                            selected = selected,
                            onClick = {
                                selSecondaryIds.value = if (selected) {
                                    selSecondaryIds.value - tag.id
                                } else {
                                    selSecondaryIds.value + tag.id
                                }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 确定按钮 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, Color(0xFF949494), RoundedCornerShape(8.dp))
                        .background(Color(0xFF303030), RoundedCornerShape(8.dp))
                        .clickable {
                            onConfirm(selPrimaryId, selSecondaryIds.value.toList())
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "确定",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
            .background(
                color = if (selected) Color(0xFF444444) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = name,
            color = if (selected) Color(0xFFF5F5F5) else Color(0xFFB3B3B3),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
