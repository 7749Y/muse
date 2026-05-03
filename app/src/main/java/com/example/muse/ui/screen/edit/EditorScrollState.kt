package com.example.muse.ui.screen.edit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextLayoutResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** 编辑器的滚动状态 + fling 动画封装 */
class EditorScrollState(
    private val scope: CoroutineScope,
) {
    var scrollOffsetPx by mutableFloatStateOf(0f)
    var containerHeightPx by mutableFloatStateOf(0f)
    var isDragging by mutableStateOf(false)
    var textLayoutResult by mutableStateOf<TextLayoutResult?>(null)
    var adjustedLines by mutableStateOf<List<AdjustedLine>?>(null)

    private var flingJob: Job? = null
    private val decaySpec = exponentialDecay<Float>()

    /** 启动 fling 减速动画，每帧回调 onFrame 用于同步外部状态 */
    fun startFling(
        velocityPxPerSec: Float,
        minScroll: Float,
        maxScroll: Float,
        onFrame: ((Float) -> Unit)? = null,
    ) {
        cancelFling()
        if (abs(velocityPxPerSec) < 50f) return
        flingJob = scope.launch {
            try {
                val animatable = Animatable(scrollOffsetPx)
                animatable.animateDecay(
                    animationSpec = decaySpec,
                    initialVelocity = velocityPxPerSec
                ) {
                    val clamped = when {
                        this.value < minScroll -> minScroll
                        this.value > maxScroll -> maxScroll
                        else -> this.value
                    }
                    scrollOffsetPx = clamped
                    onFrame?.invoke(clamped)
                }
                val finalClamped = scrollOffsetPx.coerceIn(minScroll, maxScroll)
                scrollOffsetPx = finalClamped
                onFrame?.invoke(finalClamped)
            } catch (_: CancellationException) { /* 被新手势取消 */ }
        }
    }

    /** 取消正在进行的 fling */
    fun cancelFling() {
        flingJob?.cancel()
        flingJob = null
    }
}
