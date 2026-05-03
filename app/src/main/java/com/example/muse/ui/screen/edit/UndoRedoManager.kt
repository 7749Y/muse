package com.example.muse.ui.screen.edit

import android.util.Log

class UndoRedoManager<T>(
    private val maxCapacity: Int = 100,
    private val areEquivalent: (T, T) -> Boolean = { a, b -> a == b }
) {
    val undoStack = ArrayDeque<T>()
    val redoStack = ArrayDeque<T>()

    val undoStackSize: Int get() = undoStack.size
    val redoStackSize: Int get() = redoStack.size

    fun push(state: T) {
        if (undoStack.isNotEmpty() && areEquivalent(undoStack.last(), state)) {
            Log.d("UndoRedo", "push 忽略相同状态(文本+光标): $state")
            return
        }
        undoStack.addLast(state)
        if (undoStack.size > maxCapacity) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(currentState: T): T? {
        if (undoStack.isEmpty()) {
            Log.d("UndoRedo", "undo: undoStack 为空")
            return null
        }
        // 防护：如果栈顶就是当前状态，先丢弃（可能是调用顺序错误导致）
        if (undoStack.last() == currentState) {
            Log.d("UndoRedo", "undo: 栈顶与当前状态相同，丢弃")
            undoStack.removeLast()
            if (undoStack.isEmpty()) return null
        }
        val previousState = undoStack.removeLast()
        redoStack.addLast(currentState)
        Log.d("UndoRedo", "undo: undoStack弹出='$previousState', redoStack压入='$currentState'")
        Log.d("UndoRedo", "       undo栈大小=${undoStack.size}, redo栈大小=${redoStack.size}")
        return previousState
    }

    fun redo(currentState: T): T? {
        if (redoStack.isEmpty()) {
            Log.d("UndoRedo", "redo: redoStack 为空")
            return null
        }
        val nextState = redoStack.removeLast()
        undoStack.addLast(currentState)
        Log.d("UndoRedo", "redo: redoStack弹出='${nextState}', undoStack压入='${currentState}'")
        Log.d("UndoRedo", "       undo栈大小=${undoStack.size}, redo栈大小=${redoStack.size}")
        return nextState
    }
}