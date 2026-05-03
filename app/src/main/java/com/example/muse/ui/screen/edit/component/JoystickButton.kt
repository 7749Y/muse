package com.example.muse.ui.screen.edit.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 摇杆组件：圆形底板 + 可拖动的滑块，通过回调输出归一化方向向量（-1..1）。
 */
@Composable
fun JoystickButton(
    onJoystickMove: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier,
    baseRadius: Dp = 18.dp,
    thumbRadius: Dp = 7.dp,
    baseColor: Color = Color(0x44FFFFFF),
    thumbColor: Color = Color(0xCCFFFFFF),
) {
    val density = LocalDensity.current
    val baseRadiusPx = with(density) { baseRadius.toPx() }
    val thumbRadiusPx = with(density) { thumbRadius.toPx() }
    val maxDragPx = baseRadiusPx - thumbRadiusPx

    var offset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .size(baseRadius * 2)
            .pointerInput(maxDragPx) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull() ?: continue
                        if (!ch.pressed) continue
                        ch.consume()

                        var lastPos = ch.position
                        var totalDelta = Offset.Zero

                        while (true) {
                            val mev = awaitPointerEvent()
                            val mch = mev.changes.firstOrNull() ?: break

                            if (!mch.pressed) {
                                mch.consume()
                                offset = Offset.Zero
                                onJoystickMove(0f, 0f)
                                break
                            }
                            mch.consume()

                            val frameDelta = mch.position - lastPos
                            lastPos = mch.position

                            totalDelta = Offset(
                                (totalDelta.x + frameDelta.x).coerceIn(-maxDragPx, maxDragPx),
                                (totalDelta.y + frameDelta.y).coerceIn(-maxDragPx, maxDragPx)
                            )
                            offset = totalDelta

                            onJoystickMove(
                                (totalDelta.x / maxDragPx).coerceIn(-1f, 1f),
                                (totalDelta.y / maxDragPx).coerceIn(-1f, 1f)
                            )
                        }
                    }
                }
            }
    ) {
        drawCircle(color = baseColor, radius = baseRadiusPx)
        drawCircle(
            color = thumbColor,
            radius = thumbRadiusPx,
            center = center + offset
        )
    }
}
