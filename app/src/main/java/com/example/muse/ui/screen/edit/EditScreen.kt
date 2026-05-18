package com.example.muse.ui.screen.edit

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import com.example.muse.R
import com.example.muse.ui.screen.edit.component.image.ImageLightbox
import com.example.muse.ui.screen.edit.component.image.ImageModule
import com.example.muse.ui.screen.edit.component.table.TableData
import com.example.muse.ui.screen.edit.component.table.TableMatrixSelector
import com.example.muse.ui.screen.edit.component.table.TableModule
import com.example.muse.ui.screen.edit.component.text.GeneralText
import com.example.muse.ui.screen.edit.component.gutter.GutterItem
import com.example.muse.ui.screen.edit.component.heading.HashKey
import com.example.muse.ui.screen.edit.component.heading.HeadlineEditor
import com.example.muse.ui.screen.edit.component.text.ModuleType
import com.example.muse.ui.screen.edit.component.widget.RadialMenu
import com.example.muse.ui.screen.edit.component.widget.RadialMenuItem
import com.example.muse.ui.screen.edit.component.heading.headingFontSize
import com.example.muse.ui.theme.MuseTheme
import androidx.compose.runtime.remember
import com.example.muse.data.local.PrimaryTagEntity
import com.example.muse.data.local.SecondaryTagEntity
import com.example.muse.ui.screen.edit.component.tag.TagDialog

data class SavedModule(
    val text: String,
    val type: ModuleType,
    val paragraphSpacingPx: Float = 0f,
    val headingLevel: Int = 2,
    val imageUris: List<Uri> = emptyList(),
    val tableData: TableData? = null,
)

private data class EditorConfig(
    val title: String,
    val gutterProvider: (Int) -> GutterItem,
    val textStyle: TextStyle,
    val paragraphSpacingPx: Float = 50f,
    val type: ModuleType,
)

private val listConfig = EditorConfig(
    title = "列表",
    gutterProvider = { GutterItem.Bullet },
    textStyle = TextStyle(
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    type = ModuleType.List,
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
    type = ModuleType.Code,
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
    type = ModuleType.Quote,
)

@Composable
fun EditScreen(
    onBackClick: () -> Unit = {},
    onSaveClick: (modules: List<SavedModule>, title: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    moduleSpacing: Dp = 0.dp,
    initialModules: List<SavedModule> = emptyList(),
    initialTitle: String = "",
    primaryTags: List<PrimaryTagEntity> = emptyList(),
    secondaryTags: List<SecondaryTagEntity> = emptyList(),
    selectedPrimaryTagId: Long? = null,
    selectedSecondaryTagIds: Set<Long> = emptySet(),
    onTagsChanged: (primaryTagId: Long?, secondaryTagIds: List<Long>) -> Unit = { _, _ -> },
    onCreateSecondaryTag: suspend (String) -> Long = { 0L },
) {
    var focusManager = LocalFocusManager.current
    var editingConfig by remember { mutableStateOf<EditorConfig?>(null) }
    var modules by remember(initialModules) { mutableStateOf(initialModules) }
    var titleText by remember(initialTitle) { mutableStateOf(initialTitle) }
    var editingSubHeadingIndex by remember { mutableStateOf<Int?>(null) }
    var isEditingTitle by remember { mutableStateOf(false) }
    var lightboxUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var lightboxIndex by remember { mutableIntStateOf(0) }
    var deletingModuleIndex by remember { mutableIntStateOf(-1) }
    var showTableMatrix by remember { mutableStateOf(false) }
    var editingModuleIndex by remember { mutableIntStateOf(-1) }
    var showTagDialog by remember { mutableStateOf(false) }

    val isDirty by remember {
        derivedStateOf {
            modules != initialModules || titleText != initialTitle
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val lastIsImage = modules.isNotEmpty() && modules.last().type == ModuleType.Image
            if (lastIsImage) {
                modules = modules.toMutableList().apply {
                    val i = size - 1
                    set(i, get(i).copy(imageUris = get(i).imageUris + uris))
                }
            } else {
                modules = modules + SavedModule("", ModuleType.Image, imageUris = uris)
            }
        }
    }

    val scrollFocusConnection = remember(focusManager) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    focusManager.clearFocus()
                }
                return Offset.Zero
            }
        }
    }

    fun commitSubHeading(index: Int) {
        modules = modules.toMutableList().apply {
            val mod = modules[index]
            val hashResult = HashKey.detectLevel(mod.text)
            val (newLevel, cleanText) = if (hashResult != null) {
                hashResult.first to hashResult.second
            } else {
                mod.headingLevel to mod.text   // 无前缀则保持原等级和文本
            }

            if (cleanText.isBlank()) {
                // 空内容 → 删除该子标题
                removeAt(index)
            } else {
                set(index, mod.copy(text = cleanText, headingLevel = newLevel))
            }
        }
    }

    fun moduleTypeToConfig(type: ModuleType): EditorConfig? = when (type) {
        ModuleType.List -> listConfig
        ModuleType.Code -> codeConfig
        ModuleType.Quote -> quoteConfig
        else -> null
    }

    if (editingConfig != null) {
        val config = editingConfig!!
        val initial = if (editingModuleIndex >= 0) modules[editingModuleIndex].text else ""
        GeneralEditScreen(
            title = config.title,
            initialText = initial,
            onBackClick = {
                editingConfig = null
                editingModuleIndex = -1
            },
            onDoneClick = { text ->
                if (editingModuleIndex >= 0) {
                    if (text.isBlank()) {
                        modules = modules.toMutableList().apply { removeAt(editingModuleIndex) }
                    } else {
                        modules = modules.toMutableList().apply {
                            set(editingModuleIndex, get(editingModuleIndex).copy(text = text))
                        }
                    }
                } else if (text.isNotBlank()) {
                    modules = modules + SavedModule(text, config.type, config.paragraphSpacingPx)
                }
                editingConfig = null
                editingModuleIndex = -1
            },
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                    deletingModuleIndex = -1
                }
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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

                if (isDirty) {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            onSaveClick(modules, titleText)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = "Save",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // Title & Tag row
            if (isEditingTitle) {
                // 编辑模式：标题编辑器独占整行
                HeadlineEditor(
                    text = titleText,
                    onTextChange = { titleText = it },
                    level = 1,
                    showDrum = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 37.dp),
                    placeholder = "标题",
                    onFocusLost = { isEditingTitle = false },
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = titleText.ifEmpty { "标题" },
                        fontSize = headingFontSize(1),
                        fontWeight = FontWeight.Bold,
                        color = if (titleText.isEmpty()) Color.White.copy(alpha = 0.4f) else Color.White,
                        lineHeight = headingFontSize(1) * 1.4f,
                        modifier = Modifier
                            .weight(1f)
                            .combinedClickable(
                                onClick = {},
                                onDoubleClick = { isEditingTitle = true },
                            ),
                    )

                    // Tag toggle
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .height(40.dp)
                            .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .clickable { showTagDialog = true },
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

            // 可滚动的已保存模块列表
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val bottomSpace = maxHeight / 2f
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .nestedScroll(scrollFocusConnection)
                ) {
                    modules.forEachIndexed { index, module ->
                        if (module.type == ModuleType.SubHeading) {
                            if (editingSubHeadingIndex == index) {
                                HeadlineEditor(
                                    text = module.text,
                                    onTextChange = { newText ->
                                        modules = modules.toMutableList().apply {
                                            val current = get(index)
                                            val hashResult = HashKey.detectLevel(newText)
                                            val newLevel = hashResult?.first ?: current.headingLevel
                                            set(index, current.copy(
                                                text = newText,
                                                headingLevel = newLevel,
                                            ))
                                        }
                                    },
                                    level = module.headingLevel,
                                    onLevelChange = { newLevel ->
                                        modules = modules.toMutableList().apply {
                                            set(index, get(index).copy(headingLevel = newLevel))
                                        }
                                    },
                                    showDrum = true,
                                    onFocusLost = { finalText ->
                                        if (editingSubHeadingIndex != index) return@HeadlineEditor
                                        editingSubHeadingIndex = null
                                        if (finalText.isBlank()) {
                                            modules = modules.toMutableList().apply { removeAt(index) }
                                        }
                                    },
                                    placeholder = "输入子标题...",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 5.dp),
                                )
                            } else {
                                GeneralText(
                                    text = module.text,
                                    type = ModuleType.SubHeading,
                                    fontSize = headingFontSize(module.headingLevel),
                                    showGutter = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 5.dp)
                                        .combinedClickable(
                                            onClick = {},
                                            onDoubleClick = { editingSubHeadingIndex = index },
                                        ),
                                )
                            }
                        } else if (module.type == ModuleType.Table) {
                            val isEditingTable = deletingModuleIndex == index
                            module.tableData?.let { td ->
                                TableModule(
                                    tableData = td,
                                    isEditing = true,
                                    onDeleteModule = {
                                        modules = modules.toMutableList().apply {
                                            removeAt(index)
                                        }
                                    },
                                    onAlignColumn = { _, align ->
                                        val newAligns = td.columnAlignments.map { align }
                                        modules = modules.toMutableList().apply {
                                            set(index, module.copy(
                                                tableData = td.copy(columnAlignments = newAligns)
                                            ))
                                        }
                                    },
                                    onCellChange = { row, col, text ->
                                        val newCells = td.cells.toMutableList().apply {
                                            val rowList = this[row].toMutableList()
                                            rowList[col] = text
                                            this[row] = rowList
                                        }
                                        modules = modules.toMutableList().apply {
                                            set(index, module.copy(
                                                tableData = td.copy(cells = newCells)
                                            ))
                                        }
                                    },
                                    onTableResize = { newRows, newCols ->
                                        val newCells = td.cells.take(newRows).map { row ->
                                            row.take(newCols) + List(maxOf(0, newCols - row.size)) { "" }
                                        }
                                        val newAligns = td.columnAlignments.take(newCols) +
                                            List(maxOf(0, newCols - td.columnAlignments.size)) {
                                                TextAlign.Start
                                            }
                                        modules = modules.toMutableList().apply {
                                            set(index, module.copy(
                                                tableData = td.copy(
                                                    cells = newCells,
                                                    rows = newRows,
                                                    cols = newCols,
                                                    columnAlignments = newAligns,
                                                )
                                            ))
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 5.dp),
                                )
                            }
                        } else if (module.type == ModuleType.Image) {
                            ImageModule(
                                imageUris = module.imageUris,
                                onImageClick = { i ->
                                    lightboxUris = module.imageUris
                                    lightboxIndex = i
                                },
                                isDeleting = deletingModuleIndex == index,
                                onDeleteImage = { imageIdx ->
                                    val newUris = module.imageUris.toMutableList().apply {
                                        removeAt(imageIdx)
                                    }
                                    if (newUris.isEmpty()) {
                                        modules = modules.toMutableList().apply { removeAt(index) }
                                        deletingModuleIndex = -1
                                    } else {
                                        modules = modules.toMutableList().apply {
                                            set(index, module.copy(imageUris = newUris))
                                        }
                                    }
                                },
                                onDeleteModule = { deletingModuleIndex = index },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 5.dp),
                            )
                        } else {
                            GeneralText(
                                text = module.text,
                                type = module.type,
                                paragraphSpacingPx = module.paragraphSpacingPx,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 5.dp)
                                    .combinedClickable(
                                        onClick = {
                                            focusManager.clearFocus()
                                            deletingModuleIndex = -1
                                        },
                                        onDoubleClick = {
                                            val cfg = moduleTypeToConfig(module.type)
                                            if (cfg != null) {
                                                editingModuleIndex = index
                                                editingConfig = cfg
                                            }
                                        },
                                    ),
                            )
                        }
                        if (index < modules.size - 1) {
                            Spacer(modifier = Modifier.height(moduleSpacing))
                        }
                    }
                    // 底部添加按钮 — 作为模块列表最后一个元素
                    RadialMenu(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 88.dp),
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
                            RadialMenuItem("列表", R.drawable.ic_list) {
                                editingModuleIndex = -1
                                editingConfig = listConfig
                            },
                            RadialMenuItem("引用", R.drawable.ic_quote) {
                                editingModuleIndex = -1
                                editingConfig = quoteConfig
                            },
                            RadialMenuItem("图片", R.drawable.ic_image) {
                                imagePicker.launch("image/*")
                            },
                            RadialMenuItem("表格", R.drawable.ic_table) { showTableMatrix = true },
                            RadialMenuItem("子标题", R.drawable.ic_subheading) {
                                editingSubHeadingIndex?.let { commitSubHeading(it) }
                                editingSubHeadingIndex = null
                                val idx = modules.size
                                modules = modules + SavedModule("", ModuleType.SubHeading, headingLevel = 2)
                                editingSubHeadingIndex = idx
                            },
                            RadialMenuItem("代码块", R.drawable.ic_code) {
                                editingModuleIndex = -1
                                editingConfig = codeConfig
                            },
                        )
                    )
                    Spacer(modifier = Modifier.height(bottomSpace))
                }
            }
        }

            if (showTableMatrix) {
                TableMatrixSelector(
                    onDismiss = { showTableMatrix = false },
                    onConfirm = { rows, cols ->
                        showTableMatrix = false
                        modules = modules + SavedModule(
                            "",
                            ModuleType.Table,
                            tableData = TableData(
                                rows = rows,
                                cols = cols,
                            ).initialized(),
                        )
                    },
                )
            }

            // 图片放大浮动窗口
            if (lightboxUris.isNotEmpty()) {
                ImageLightbox(
                    imageUris = lightboxUris,
                    initialIndex = lightboxIndex,
                    onDismiss = {
                        lightboxUris = emptyList()
                        lightboxIndex = 0
                    },
                )
            }

            // 标签对话框
            if (showTagDialog) {
                TagDialog(
                    primaryTags = primaryTags,
                    secondaryTags = secondaryTags,
                    selectedPrimaryTagId = selectedPrimaryTagId,
                    selectedSecondaryTagIds = selectedSecondaryTagIds,
                    onDismiss = { showTagDialog = false },
                    onConfirm = { pId, sIds ->
                        showTagDialog = false
                        onTagsChanged(pId, sIds)
                    },
                    onCreateSecondaryTag = onCreateSecondaryTag,
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
private fun EditScreenPreview() {
    MuseTheme(darkTheme = true) {
        EditScreen(
            moduleSpacing = 8.dp,
            initialModules = listOf(
                SavedModule("购物清单\n牛奶\n鸡蛋\n面包", ModuleType.List, 50f),
                SavedModule("子标题示例", ModuleType.SubHeading, 0f, headingLevel = 2),
                SavedModule("子曰：学而时习之，不亦说乎。有朋自远方来，不亦乐乎。", ModuleType.Quote, 20f),
                SavedModule("""fun main() {
    println("Hello, World!")
    val x = 42
    return x
}""", ModuleType.Code, 0f),
            )
        )
    }
}
