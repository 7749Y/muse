package com.example.muse.ui.screen.edit.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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

data class RadialMenuItem(
    val label: String,
    val iconRes: Int,
    val onClick: () -> Unit,
)

/**
 * 径向菜单组件。
 *
 * 长按中心按钮展开环绕菜单，拖拽到目标项上松手触发。
 * 0° = 正上方，顺时针排列。
 */
@Composable
fun RadialMenu(
    modifier: Modifier = Modifier,
    radius: Dp = 100.dp,
    centerButton: @Composable () -> Unit,
    items: List<RadialMenuItem>,
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val radiusPx = with(density) { radius.toPx() }
    val stepAngle = 360f / items.size
    val startAngle = 0f

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
                val ox = (radiusPx * sin(rad)).roundToInt()
                val oy = (-radiusPx * cos(rad)).roundToInt()

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

        // 中心按钮
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .onSizeChanged { centerSize = it }
                .pointerInput(items) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            expanded = true
                            selectedIndex = -1
                        },
                        onDrag = { change, _ ->
                            val cx = centerSize.width / 2f
                            val cy = centerSize.height / 2f
                            val dx = change.position.x - cx
                            val dy = change.position.y - cy
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                            // 有效环形区域：内圈避免误触中心，外圈避免误触远处
                            val innerR = 16.dp.toPx()
                            val outerR = radiusPx + 24.dp.toPx()
                            selectedIndex = if (dist in innerR..outerR) {
                                val rawAngle = atan2(dx, -dy) * 180f / PI.toFloat()
                                val norm = ((rawAngle - startAngle) % 360f + 360f) % 360f
                                (norm / stepAngle).roundToInt().coerceIn(0, items.size - 1)
                            } else -1
                        },
                        onDragEnd = {
                            if (selectedIndex in items.indices) {
                                items[selectedIndex].onClick()
                            }
                            expanded = false
                        },
                        onDragCancel = { expanded = false }
                    )
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
