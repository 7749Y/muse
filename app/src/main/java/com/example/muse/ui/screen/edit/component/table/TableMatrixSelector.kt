package com.example.muse.ui.screen.edit.component.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_ROWS = 8
private const val MAX_COLS = 8
private val CELL_SIZE = 28.dp
private val CELL_SPACING = 2.dp

@Composable
fun TableMatrixSelector(
    onDismiss: () -> Unit,
    onConfirm: (rows: Int, cols: Int) -> Unit,
) {
    var hoveredRow by remember { mutableIntStateOf(0) }
    var hoveredCol by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val cellSizePx = with(density) { CELL_SIZE.toPx() }
    val cellSpacingPx = with(density) { CELL_SPACING.toPx() }
    val stepPx = cellSizePx + cellSpacingPx

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss)
        )

        Card(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 94.dp, bottom = 216.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val displayRows = max(2, hoveredRow + 1)
                val displayCols = hoveredCol + 1
                Text(
                    text = "${displayRows} × ${displayCols}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .pointerInput(stepPx) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                fun posToCell(px: Float): Int =
                                    (px / stepPx).roundToInt().coerceIn(0, MAX_ROWS - 1)
                                hoveredRow = posToCell(down.position.y)
                                hoveredCol = posToCell(down.position.x)

                                do {
                                    val event = awaitPointerEvent()
                                    val pos = event.changes.first().position
                                    hoveredRow = posToCell(pos.y)
                                    hoveredCol = posToCell(pos.x)
                                } while (event.changes.any { it.pressed })

                                onConfirm(max(2, hoveredRow + 1), hoveredCol + 1)
                            }
                        }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(CELL_SPACING)) {
                        for (r in 0 until MAX_ROWS) {
                            Row(horizontalArrangement = Arrangement.spacedBy(CELL_SPACING)) {
                                for (c in 0 until MAX_COLS) {
                                    val filled = r <= hoveredRow && c <= hoveredCol
                                    Box(
                                        modifier = Modifier
                                            .size(CELL_SIZE)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (filled) Color(0xFF4A90D9)
                                                else Color(0xFF3A3A3A)
                                            )
                                            .border(
                                                width = 0.5.dp,
                                                color = Color(0xFF555555),
                                                shape = RoundedCornerShape(4.dp),
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
