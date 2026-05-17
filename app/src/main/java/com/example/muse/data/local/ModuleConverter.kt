package com.example.muse.data.local

import android.net.Uri
import com.example.muse.ui.screen.edit.SavedModule
import com.example.muse.ui.screen.edit.component.table.TableData
import com.example.muse.ui.screen.edit.component.text.ModuleType
import org.json.JSONArray
import org.json.JSONObject

object ModuleConverter {

    private const val KEY_PS = "ps"          // paragraphSpacingPx
    private const val KEY_URIS = "uris"      // image URIs
    private const val KEY_TABLE = "table"    // TableData JSON

    fun toBlocks(modules: List<SavedModule>, documentId: Long): List<BlockEntity> {
        return modules.mapIndexed { index, module ->
            val meta = JSONObject()

            // paragraphSpacingPx — 仅当非默认值时存储
                if (module.paragraphSpacingPx != 0f && module.paragraphSpacingPx.isFinite()) {
                meta.put(KEY_PS, module.paragraphSpacingPx)
            }

            // Image — 存储 URI 列表
            if (module.type == ModuleType.Image && module.imageUris.isNotEmpty()) {
                val arr = JSONArray()
                module.imageUris.forEach { arr.put(it.toString()) }
                meta.put(KEY_URIS, arr)
            }

            // Table — metadata 存完整 JSON，content 存 markdown
            if (module.type == ModuleType.Table && module.tableData != null) {
                meta.put(KEY_TABLE, serializeTable(module.tableData))
            }

            BlockEntity(
                documentId = documentId,
                type = module.type.name,
                content = module.text,
                sequence = index,
                headingLevel = if (module.type == ModuleType.SubHeading) module.headingLevel else null,
                metadataJson = if (meta.length() > 0) meta.toString() else null,
            )
        }
    }

    fun toModules(blocks: List<BlockEntity>): List<SavedModule> {
        return blocks.map { block ->
            val type = try {
                ModuleType.valueOf(block.type)
            } catch (_: IllegalArgumentException) {
                ModuleType.List // fallback
            }

            val meta = block.metadataJson?.let { JSONObject(it) }

            SavedModule(
                text = block.content,
                type = type,
                paragraphSpacingPx = meta?.optDouble(KEY_PS)?.toFloat()
                    ?.takeIf { !it.isNaN() } ?: 0f,
                headingLevel = block.headingLevel ?: 2,
                imageUris = parseUris(meta?.optJSONArray(KEY_URIS)),
                tableData = parseTable(type, block.content, meta),
            )
        }
    }

    private fun serializeTable(td: TableData): JSONObject {
        val cells = JSONArray()
        for (row in td.cells) {
            val rowArr = JSONArray()
            row.forEach { rowArr.put(it) }
            cells.put(rowArr)
        }
        val aligns = JSONArray()
        td.columnAlignments.forEach { aligns.put(textAlignToInt(it)) }
        return JSONObject().apply {
            put("rows", td.rows)
            put("cols", td.cols)
            put("cells", cells)
            put("aligns", aligns)
        }
    }

    private fun parseTable(type: ModuleType, content: String, meta: JSONObject?): TableData? {
        if (type != ModuleType.Table) return null
        val tableObj = meta?.optJSONObject(KEY_TABLE)
        if (tableObj != null) {
            val rows = tableObj.optInt("rows", 3)
            val cols = tableObj.optInt("cols", 3)
            val cells = mutableListOf<List<String>>()
            val cellsArr = tableObj.optJSONArray("cells")
            if (cellsArr != null) {
                for (i in 0 until cellsArr.length()) {
                    val rowArr = cellsArr.optJSONArray(i)
                    val row = mutableListOf<String>()
                    if (rowArr != null) {
                        for (j in 0 until rowArr.length()) {
                            row.add(rowArr.optString(j, ""))
                        }
                    }
                    cells.add(row)
                }
            }
            val aligns = mutableListOf<androidx.compose.ui.text.style.TextAlign>()
            val alignsArr = tableObj.optJSONArray("aligns")
            if (alignsArr != null) {
                for (i in 0 until alignsArr.length()) {
                    aligns.add(intToTextAlign(alignsArr.optInt(i, 0)))
                }
            }
            return TableData(rows = rows, cols = cols, cells = cells, columnAlignments = aligns)
        }
        return TableData.fromMarkdown(content)
    }

    private fun textAlignToInt(align: androidx.compose.ui.text.style.TextAlign): Int = when (align) {
        androidx.compose.ui.text.style.TextAlign.Center -> 1
        androidx.compose.ui.text.style.TextAlign.End -> 2
        else -> 0
    }

    private fun intToTextAlign(value: Int): androidx.compose.ui.text.style.TextAlign = when (value) {
        1 -> androidx.compose.ui.text.style.TextAlign.Center
        2 -> androidx.compose.ui.text.style.TextAlign.End
        else -> androidx.compose.ui.text.style.TextAlign.Start
    }

    private fun parseUris(arr: JSONArray?): List<Uri> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val s = arr.optString(i, null) ?: return@mapNotNull null
            Uri.parse(s)
        }
    }
}
