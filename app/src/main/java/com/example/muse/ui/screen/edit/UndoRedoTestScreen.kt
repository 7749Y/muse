package com.example.muse.ui.screen.edit

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UndoRedoTestScreen() {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val undoManager = remember {
        UndoRedoManager<TextFieldValue>(maxCapacity = 100) { a, b ->
            a.text == b.text && a.selection == b.selection
        }
    }
    var isUndoRedoing by remember { mutableStateOf(false) }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Undo/Redo 测试",
            fontSize = 20.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 栈状态显示
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Undo 栈: ${undoManager.undoStackSize}",
                color = Color.Black
            )
            Text(
                text = "Redo 栈: ${undoManager.redoStackSize}",
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 文本输入区
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                // 只有文本或光标真正变化时才记录撤销
                val actualChange = newValue.text != textFieldValue.text ||
                        newValue.selection != textFieldValue.selection

                if (actualChange && !isUndoRedoing) {
                    Log.d("UndoTest", "记录旧值: '${textFieldValue.text}'")
                    undoManager.push(textFieldValue)
                } else {
                    Log.d("UndoTest", "跳过记录 (文本/光标未变或正在撤销/重做)")
                }

                textFieldValue = newValue
                Log.d("UndoTest", "更新后 | Undo栈=${undoManager.undoStackSize}, Redo栈=${undoManager.redoStackSize}")
            },
            textStyle = TextStyle(
                color = Color.Black,
                fontSize = 18.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 按钮行
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    val current = textFieldValue
                    val undoTop = undoManager.undoStack.lastOrNull()?.text ?: "null"
                    val redoTop = undoManager.redoStack.lastOrNull()?.text ?: "null"
                    Log.d("UndoTest", "点击 Undo —— 当前文本:'${current.text}'")
                    Log.d("UndoTest", "  undo 栈顶:'$undoTop'  redo 栈顶:'$redoTop'")

                    undoManager.undo(current)?.let { previous ->
                        Log.d("UndoTest", "  undo 返回文本:'${previous.text}'")
                        textFieldValue = previous
                    } ?: Log.d("UndoTest", "  undo 返回 null (栈空)")
                },
                enabled = undoManager.undoStackSize > 0
            ) {
                Text("Undo (撤销)")
            }

            Button(
                onClick = {
                    val current = textFieldValue
                    val undoTop = undoManager.undoStack.lastOrNull()?.text ?: "null"
                    val redoTop = undoManager.redoStack.lastOrNull()?.text ?: "null"
                    Log.d("UndoTest", "点击 Redo —— 当前文本:'${current.text}'")
                    Log.d("UndoTest", "  undo 栈顶:'$undoTop'  redo 栈顶:'$redoTop'")

                    undoManager.redo(current)?.let { next ->
                        Log.d("UndoTest", "  redo 返回文本:'${next.text}'")
                        textFieldValue = next
                    } ?: Log.d("UndoTest", "  redo 返回 null (栈空)")
                },
                enabled = undoManager.redoStackSize > 0
            ) {
                Text("Redo (重做)")
            }
        }
    }
}