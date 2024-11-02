package com.example.amobileappfordisabledpeople.ui.views

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data class ObjectDetectionResponse(val result: List<ObjectDetectionUiData>) : UiState
    data class CaptionResponse(val result: String) : UiState
    data class Error(val e: String) : UiState
}

data class ObjectDetectionUiData(
    val topLeft: Offset,
    val color: Color,
    val size: Size,
    val textTopLeft: Offset,
    val text: String,
)