package com.example.muse.ui.screen.edit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** 左右移动光标（-1 左移，1 右移）*/
internal fun moveCursor(direction: Int, current: TextFieldValue, onResult: (TextFieldValue) -> Unit) {
    val text = current.text
    val sel = current.selection
    if (direction < 0) {
        val pos = if (!sel.collapsed) sel.min else (sel.start - 1).coerceAtLeast(0)
        onResult(current.copy(selection = TextRange(pos)))
    } else {
        val pos = if (!sel.collapsed) sel.max else (sel.start + 1).coerceAtMost(text.length)
        onResult(current.copy(selection = TextRange(pos)))
    }
}

/** 上下移动光标（逐行）*/
internal fun moveCursorLine(direction: Int, current: TextFieldValue, vm: EditorViewModel, onResult: (TextFieldValue) -> Unit) {
    val layout = vm.textLayoutResult ?: return
    val text = current.text
    val sel = current.selection
    val cursorEnd = sel.end.coerceIn(0, text.length)
    val cursorLine = layout.getLineForOffset(cursorEnd)
    if (direction < 0 && cursorLine == 0) {
        onResult(current.copy(selection = TextRange(0)))
        return
    }
    if (direction > 0 && cursorLine == layout.lineCount - 1) {
        onResult(current.copy(selection = TextRange(text.length)))
        return
    }
    val targetLine = cursorLine + direction
    val cursorRect = layout.getCursorRect(cursorEnd)
    val targetOffset = layout.getOffsetForPosition(Offset(cursorRect.left, layout.getLineTop(targetLine)))
    if (targetOffset != -1) onResult(current.copy(selection = TextRange(targetOffset)))
}

/** 查找 offset 所在单词的边界（字母/数字/下划线视为单词字符，CJK 字符也计入）*/
internal fun wordBoundaries(text: String, offset: Int): IntRange? {
    if (text.isEmpty() || offset !in text.indices) return null
    val isWord = { c: Char -> c.isLetterOrDigit() || c == '_' }
    if (!isWord(text[offset])) return null
    var start = offset
    while (start > 0 && isWord(text[start - 1])) start--
    var end = offset
    while (end < text.length - 1 && isWord(text[end + 1])) end++
    return start..end
}
