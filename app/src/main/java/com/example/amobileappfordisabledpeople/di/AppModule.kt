package com.example.amobileappfordisabledpeople.di

import android.app.Application
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import com.example.amobileappfordisabledpeople.Data.repository.CustomCameraRepoImpl
import com.example.amobileappfordisabledpeople.domain.repository.CustomCameraRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

//DI: stands for dependency injection, which is responsible for providing the dependencies to other app's components.

//Mark this object as a dagger module where provides  for Dagger Hilt
@Module
//Determine lifecycle scope of this module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    //Mark this method is sole, only one instance of this object will be created
    @Singleton
    fun provideCameraSelector(): CameraSelector{
        return CameraSelector.Builder()
            //Can choose between front or back camera
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }

    @Provides
    @Singleton
    fun provideCameraProvider(application: Application): ProcessCameraProvider{
        //for providing camera instance
        return ProcessCameraProvider.getInstance(application).get()
    }

    @Provides
    @Singleton
    fun provideCameraPreview(): Preview {
        //for previewing the camera
        return Preview.Builder().build()
    }

    @Provides
    @Singleton
    fun provideImageCapture(): ImageCapture {
        //for capturing image from camera
        //ignoring the apect ration
        return ImageCapture.Builder()
            .setFlashMode(ImageCapture.FLASH_MODE_ON)
//            .setTargeAspectRatio(AspectRatio.RATIO_16_9))
            .build()
    }

    @Provides
    @Singleton
    fun provideImageAnalysis(): ImageAnalysis {
        //analyzing image before capturing
        //often using in ML to analyze image
        return ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    @Provides
    @Singleton
    fun provideCustomeCameraRepo(
        cameraProvider: ProcessCameraProvider,
        selector: CameraSelector,
        preview: Preview,
        imageCapture: ImageCapture,
        imageAnalysis: ImageAnalysis
    ): CustomCameraRepo {
        return CustomCameraRepoImpl(
            cameraProvider = cameraProvider,
            selector = selector,
            preview = preview,
            imageCapture = imageCapture,
            imageAnalysis = imageAnalysis
        )
    }
}