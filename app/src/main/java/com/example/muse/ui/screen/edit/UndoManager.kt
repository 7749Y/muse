package com.example.muse.ui.screen.edit

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UndoManager(
    private val maxUndoDepth: Int = 200,      // 最多撤销步数
    private val mergeDelayMs: Long = 300       // 合并连续输入的间隔（毫秒）
) {
    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()

    private var pendingJob: Job? = null
    private var pendingBaseline: TextFieldValue? = null

    // 记录一次“用户编辑”（在文本变化前调用，oldValue 是编辑前的值）
    fun recordEdit(oldValue: TextFieldValue, scope: CoroutineScope) {
        // 只有文本真的变了才需要记录
        pendingJob?.cancel()

        // 第一次变化，暂存基线
        if (pendingBaseline == null) {
            pendingBaseline = oldValue
        }

        pendingJob = scope.launch {
            delay(mergeDelayMs)
            // 提交基线到撤销栈
            val baseline = pendingBaseline
            if (baseline != null && (undoStack.lastOrNull()?.text != baseline.text)) {
                undoStack.add(baseline)
                if (undoStack.size > maxUndoDepth) {
                    undoStack.removeAt(0)
                }
            }
            redoStack.clear()
            pendingBaseline = null
            pendingJob = null
        }
    }

    // 撤销操作：传入当前文本，返回恢复到的文本
    fun undo(currentValue: TextFieldValue): TextFieldValue? {
        cancelPending()   // 终止任何未完成的合并
        if (undoStack.isEmpty()) return null

        val previous = undoStack.removeAt(0)
        // 当前值进入重做栈（仅当文本非空时才放入，避免空状态残留）
        if (currentValue.text.isNotEmpty()) {
            redoStack.add(currentValue)
        }
        return previous
    }

    // 重做操作
    fun redo(currentValue: TextFieldValue): TextFieldValue? {
        cancelPending()
        if (redoStack.isEmpty()) return null

        val next = redoStack.removeAt(0)
        if (currentValue.text.isNotEmpty()) {
            undoStack.add(currentValue)
        }
        return next
    }

    // 强制提交当前合并（例如失焦时调用）
    fun forceCommit(scope: CoroutineScope) {
        cancelPending()
        val baseline = pendingBaseline
        if (baseline != null && (undoStack.lastOrNull()?.text != baseline.text)) {
            undoStack.add(baseline)
            redoStack.clear()
        }
        pendingBaseline = null
    }

    // 清空所有历史
    fun clear() {
        cancelPending()
        undoStack.clear()
        redoStack.clear()
        pendingBaseline = null
    }

    private fun cancelPending() {
        pendingJob?.cancel()
        pendingJob = null
        pendingBaseline = null
    }
}