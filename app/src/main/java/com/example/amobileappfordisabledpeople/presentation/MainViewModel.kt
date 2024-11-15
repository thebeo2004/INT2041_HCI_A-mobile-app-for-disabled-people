package com.example.amobileappfordisabledpeople.presentation

import android.content.Context
import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amobileappfordisabledpeople.domain.repository.CustomCameraRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: CustomCameraRepo
): ViewModel() {

    var capturedImageUri = mutableStateOf<Uri?>(null)
        private set

    fun showCameraPreview(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
    ) {
        viewModelScope.launch {
            repo.showCameraPreview(
                previewView = previewView,
                lifecycleOwner = lifecycleOwner
            )
        }
    }

    fun captureAndSave(context: Context, onImageCaptured: () -> Unit = {}) {
        viewModelScope.launch {
            repo.captureAndSaveImage(context) { uri ->
                updateCapturedImageUri(uri)
                onImageCaptured()
            }

        }
    }

    fun updateCapturedImageUri(uri: Uri) {
        capturedImageUri.value = uri
    }
}