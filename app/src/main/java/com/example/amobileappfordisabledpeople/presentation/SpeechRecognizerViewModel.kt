package com.example.amobileappfordisabledpeople.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SpeechRecognizerViewModel: ViewModel() {
    var state by mutableStateOf(SpeechRecognizerState())
        private set

    fun changeTextValue(text: String) {
        viewModelScope.launch {
            state = state.copy (
                text = text
            )
        }
    }
}