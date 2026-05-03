package com.example.muse.ui.screen.edit.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun ToolbarButton(
    label: String,
    repeatOnHold: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentAction by rememberUpdatedState(onAction)
    Box(
        modifier = modifier
            .border(1.dp, Color(0xFF949494), RoundedCornerShape(8.dp))
            .size(36.dp)
            .then(
                if (repeatOnHold) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull() ?: continue
                                if (!ch.pressed) continue
                                ch.consume()

                                currentAction()

                                var first = true
                                while (true) {
                                    val timeoutMs = if (first) 200L else 80L; first = false
                                    val mev = withTimeoutOrNull(timeoutMs) { awaitPointerEvent() }
                                    if (mev != null) {
                                        val mch = mev.changes.firstOrNull()
                                        if (mch == null || !mch.pressed) break
                                        mch.consume()
                                    } else {
                                        currentAction()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Modifier.clickable { currentAction() }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}
