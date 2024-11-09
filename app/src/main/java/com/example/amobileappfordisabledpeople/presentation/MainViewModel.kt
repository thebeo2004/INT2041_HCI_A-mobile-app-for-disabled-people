package com.example.amobileappfordisabledpeople.presentation

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amobileappfordisabledpeople.Data.repository.CustomCameraRepoImpl
import com.example.amobileappfordisabledpeople.domain.repository.CustomCameraRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val selector: CameraSelector,
    private val cameraProvider: ProcessCameraProvider,
    private val preview: Preview,
    private val imageCapture: ImageCapture,
): ViewModel() {

    private lateinit var repo: CustomCameraRepo

    fun initRepo(imageAnalysis: ImageAnalysis) {
        repo =  CustomCameraRepoImpl(
            cameraProvider = cameraProvider,
            selector = selector,
            preview = preview,
            imageCapture = imageCapture,
            imageAnalysis = imageAnalysis
        )
    }

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

    fun captureAndSave(context: Context) {
        viewModelScope.launch {
            repo.captureAndSaveImage(context)
        }
    }
}