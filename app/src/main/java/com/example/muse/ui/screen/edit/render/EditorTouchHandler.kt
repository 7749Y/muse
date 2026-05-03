package com.example.muse.ui.screen.edit.render

import android.view.ViewConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.muse.ui.screen.edit.core.EditorScrollState
import com.example.muse.ui.screen.edit.core.EditorViewModel
import com.example.muse.ui.screen.edit.core.wordBoundaries
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.max

@Composable
fun EditorTouchHandler(
    scrollState: EditorScrollState,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    isImeVisible: Boolean,
    touchSlopPx: Float,
    topPaddingPx: Float,
    bottomPaddingPx: Float,
    editorViewModel: EditorViewModel?,
    modifier: Modifier = Modifier,
) {
    val currentValue by rememberUpdatedState(value)
    val currentAdjustedLines by rememberUpdatedState(scrollState.adjustedLines)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var lastTapTimeMs = 0L
                var lastTapX = 0f
                var lastTapY = 0f
                var tapCount = 0

                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull() ?: continue
                        if (!ch.pressed) continue

                        ch.consume()
                        scrollState.cancelFling()

                        val startPos = ch.position
                        var lastPos = startPos
                        var totalY = 0f
                        var drag = false
                        val velSamples = mutableListOf<Pair<Long, Float>>()

                        while (true) {
                            val mev = awaitPointerEvent()
                            val mch = mev.changes.firstOrNull() ?: break

                            if (!mch.pressed) {
                                mch.consume()

                                if (!drag) {
                                    val now = System.currentTimeMillis()
                                    val tapPos = mch.position
                                    val isDoubleTap = now - lastTapTimeMs < ViewConfiguration.getDoubleTapTimeout() &&
                                            abs(tapPos.x - lastTapX) < touchSlopPx * 3 &&
                                            abs(tapPos.y - lastTapY) < touchSlopPx * 3
                                    tapCount = if (isDoubleTap) tapCount + 1 else 1
                                    lastTapTimeMs = now
                                    lastTapX = tapPos.x
                                    lastTapY = tapPos.y

                                    val tapOffset = scrollState.textLayoutResult?.let { r ->
                                        val tapX = tapPos.x.coerceIn(0f, r.size.width.toFloat())
                                        val tapAdjustedY = tapPos.y + scrollState.scrollOffsetPx
                                        val cal2 = currentAdjustedLines
                                        val adjLine = cal2?.firstOrNull { tapAdjustedY in it.top..it.bottom }
                                        val tapY = if (adjLine != null) {
                                            r.getLineTop(adjLine.originalLineIndex) + (tapAdjustedY - adjLine.top)
                                        } else if (!cal2.isNullOrEmpty()) {
                                            val nearest = cal2.minByOrNull { abs(it.top + (it.bottom - it.top) / 2f - tapAdjustedY) }
                                            if (nearest != null) {
                                                val centerY = nearest.top + (nearest.bottom - nearest.top) / 2f
                                                val mappedY = if (tapAdjustedY <= centerY) nearest.bottom - 1f else nearest.top
                                                r.getLineTop(nearest.originalLineIndex) + (mappedY - nearest.top)
                                            } else {
                                                tapAdjustedY.coerceIn(0f, r.size.height.toFloat())
                                            }
                                        } else {
                                            tapAdjustedY.coerceIn(0f, r.size.height.toFloat())
                                        }
                                        r.getOffsetForPosition(Offset(tapX, tapY)).takeIf { it != -1 }
                                    }

                                    if (tapCount == 3) {
                                        tapCount = 0
                                        onValueChange(
                                            currentValue.copy(selection = TextRange(0, currentValue.text.length))
                                        )
                                    } else if (tapCount == 2 && tapOffset != null) {
                                        val bounds = wordBoundaries(currentValue.text, tapOffset)
                                        if (bounds != null) {
                                            onValueChange(
                                                currentValue.copy(selection = TextRange(bounds.first, bounds.last + 1))
                                            )
                                        }
                                    } else if (tapCount == 1 && tapOffset != null) {
                                        onValueChange(
                                            currentValue.copy(selection = TextRange(tapOffset))
                                        )
                                    }
                                    focusRequester.requestFocus()
                                } else {
                                    val velocityPxPerSec = if (!isImeVisible && velSamples.size >= 2) {
                                        val recent = velSamples.takeLast(3)
                                        val totalDelta = recent.sumOf { it.second.toDouble() }.toFloat()
                                        val dtMs = ((recent.last().first - recent.first().first) / 1_000_000f).coerceAtLeast(1f)
                                        -(totalDelta / dtMs) * 1000f
                                    } else 0f
                                    scrollState.cancelFling()
                                    val textH = scrollState.adjustedLines?.last()?.bottom
                                        ?: scrollState.textLayoutResult?.size?.height?.toFloat()
                                    if (textH != null) {
                                        val totalH: Float = topPaddingPx + textH + bottomPaddingPx
                                        val minSc: Float = if (totalH < scrollState.containerHeightPx) -(scrollState.containerHeightPx - totalH) / 2f else -topPaddingPx
                                        val maxSc: Float = max(0f, textH + bottomPaddingPx - scrollState.containerHeightPx)
                                        scrollState.startFling(velocityPxPerSec, minSc, maxSc) { value ->
                                            editorViewModel?.updateScroll(value)
                                        }
                                    }
                                    scrollState.isDragging = false
                                }
                                break
                            }

                            mch.consume()

                            val delta = mch.position - lastPos
                            lastPos = mch.position
                            totalY += delta.y

                            if (!drag && abs(totalY) > touchSlopPx) {
                                drag = true
                                scrollState.isDragging = true
                            }

                            if (drag) {
                                velSamples.add(System.nanoTime() to delta.y)
                                if (velSamples.size > 6) velSamples.removeAt(0)

                                val textH = scrollState.adjustedLines?.last()?.bottom
                                    ?: scrollState.textLayoutResult?.size?.height?.toFloat()
                                    ?: continue
                                val totalH = topPaddingPx + textH + bottomPaddingPx
                                val minSc = if (totalH < scrollState.containerHeightPx) {
                                    -(scrollState.containerHeightPx - totalH) / 2f
                                } else {
                                    -topPaddingPx
                                }
                                val maxSc = max(0f, textH + bottomPaddingPx - scrollState.containerHeightPx)

                                scrollState.scrollOffsetPx =
                                    (scrollState.scrollOffsetPx - delta.y).coerceIn(minSc, maxSc)
                                editorViewModel?.updateScroll(scrollState.scrollOffsetPx)
                            }
                        }
                    }
                }
            }
    )
}
