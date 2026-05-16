package com.example.muse.ui.screen.edit.component.widget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class RadialMenuItem(
    val label: String,
    val iconRes: Int,
    val onClick: () -> Unit,
)

/**
 * 径向菜单组件。
 *
 * 按下中心按钮后立即滑动，滑动方向的按钮即时出现，松手触发该按钮。
 * 仅轻触（不滑动）时不做任何事，保留给调用方处理。
 * 0° = 水平向左，顺时针排列。
 */
@Composable
fun RadialMenu(
    modifier: Modifier = Modifier,
    radius: Dp = 72.dp,
    centerButton: @Composable () -> Unit,
    items: List<RadialMenuItem>,
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val radiusPx = with(density) { radius.toPx() }
    val stepAngle = 360f / items.size
    val startAngle = 0f
    val touchSlopPx = with(density) { 8.dp.toPx() }

    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var centerSize by remember { mutableStateOf(IntSize.Zero) }

    val animProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        label = "radialAnim"
    )

    Box(modifier = modifier) {
        // 环绕按钮
        if (expanded) {
            items.forEachIndexed { index, item ->
                val angleDeg = startAngle + index * stepAngle
                val rad = angleDeg * PI / 180.0
                val ox = (-radiusPx * cos(rad)).roundToInt()
                val oy = (-radiusPx * sin(rad)).roundToInt()

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(ox, oy) }
                        .graphicsLayer {
                            scaleX = animProgress
                            scaleY = animProgress
                            alpha = animProgress
                        }
                ) {
                    RadialMenuItemView(
                        item = item,
                        isHighlighted = index == selectedIndex
                    )
                }
            }
        }

        // 中心按钮 —— 手势：按下立刻滑动 → 菜单出现 → 松手触发
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .onSizeChanged { centerSize = it }
                .pointerInput(items) {
                    awaitPointerEventScope {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull() ?: continue
                            if (!ch.pressed) continue
                            ch.consume()

                            val startPos = ch.position
                            var isDragging = false

                            while (true) {
                                val mev = awaitPointerEvent()
                                val mch = mev.changes.firstOrNull() ?: break

                                if (!mch.pressed) {
                                    mch.consume()
                                    // 松手：如果处于拖拽模式则触发选中项
                                    if (isDragging && selectedIndex in items.indices) {
                                        items[selectedIndex].onClick()
                                    }
                                    expanded = false
                                    selectedIndex = -1
                                    break
                                }

                                mch.consume()
                                val dx = mch.position.x - startPos.x
                                val dy = mch.position.y - startPos.y
                                val dist = sqrt(dx * dx + dy * dy)

                                if (!isDragging) {
                                    if (dist > touchSlopPx) {
                                        isDragging = true
                                        expanded = true
                                        selectedIndex = -1
                                    }
                                }

                                if (isDragging) {
                                    val cx = centerSize.width / 2f
                                    val cy = centerSize.height / 2f
                                    val rx = mch.position.x - cx
                                    val ry = mch.position.y - cy
                                    val rDist = sqrt(rx * rx + ry * ry)

                                    val innerR = 28.dp.toPx()
                                    val outerR = radiusPx + 24.dp.toPx()
                                    selectedIndex = if (rDist in innerR..outerR) {
                                        val rawAngle = atan2(-ry, -rx) * 180f / PI.toFloat()
                                        val norm = ((rawAngle - startAngle) % 360f + 360f) % 360f
                                        (norm / stepAngle).roundToInt().mod(items.size)
                                    } else -1
                                }
                            }
                        }
                    }
                },
            content = { centerButton() }
        )
    }
}

@Composable
private fun RadialMenuItemView(
    item: RadialMenuItem,
    isHighlighted: Boolean,
) {
    val bgColor = if (isHighlighted) Color(0xFF444444) else Color(0xFF2D2D2D)
    val borderColor = if (isHighlighted) Color(0xFF00BCD4) else Color(0xFF949494)
    val iconTint = if (isHighlighted) Color(0xFF00BCD4) else Color.White

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(bgColor, CircleShape)
            .border(1.5.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = item.label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}
