package com.example.muse.ui.screen.edit

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextLayoutResult

data class AdjustedLine(
    val originalLineIndex: Int,
    val top: Float,
    val bottom: Float,
)

class EditorViewModel {
    var scrollOffsetPx by mutableFloatStateOf(0f)
        private set

    var containerHeightPx by mutableFloatStateOf(0f)

    var lineHeightPx by mutableFloatStateOf(0f)

    var textLayoutResult: TextLayoutResult? by mutableStateOf(null)

    var adjustedLines: List<AdjustedLine> by mutableStateOf(emptyList())

    var paragraphSpacingPx by mutableFloatStateOf(0f)

    fun updateScroll(offset: Float) {
        scrollOffsetPx = offset
    }

    fun updateLayout(result: TextLayoutResult, lineHeight: Float) {
        textLayoutResult = result
        lineHeightPx = lineHeight
    }

    fun updateContainerHeight(height: Float) {
        containerHeightPx = height
    }

    fun updateAdjustedLines(lines: List<AdjustedLine>, spacingPx: Float) {
        adjustedLines = lines
        paragraphSpacingPx = spacingPx
    }
}