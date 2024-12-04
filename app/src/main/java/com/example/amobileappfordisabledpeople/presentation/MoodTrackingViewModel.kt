package com.example.amobileappfordisabledpeople.presentation

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.amobileappfordisabledpeople.Data.repository.CustomCameraRepoImpl
import com.example.amobileappfordisabledpeople.domain.repository.CustomCameraRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MoodTrackingViewModel @Inject constructor(
    repo: CustomCameraRepo,
    cameraProvider: ProcessCameraProvider,
    selector: CameraSelector,
    preview: Preview,
    imageCapture: ImageCapture,
    imageAnalysis: ImageAnalysis
) : MainViewModel(
    repo = repo,
    cameraProvider = cameraProvider,
    selector = selector,
    preview = preview,
    imageCapture = imageCapture,
    imageAnalysis = imageAnalysis
) {

    private val _moodState = MutableLiveData<MoodState>()
    val moodState: LiveData<MoodState> get() = _moodState


}

sealed class MoodState {
    object Normal: MoodState()
    object Happy: MoodState()
    object Sad: MoodState()
}