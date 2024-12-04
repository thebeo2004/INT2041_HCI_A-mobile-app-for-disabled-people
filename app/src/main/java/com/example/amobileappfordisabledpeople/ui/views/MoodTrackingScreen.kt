package com.example.amobileappfordisabledpeople.ui.views

import android.graphics.PointF
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.amobileappfordisabledpeople.DragThreshold
import com.example.amobileappfordisabledpeople.SocializingModeBar
import com.example.amobileappfordisabledpeople.features.FaceDetectionAnalyzer
import com.example.amobileappfordisabledpeople.presentation.MoodTrackingViewModel
import com.example.amobileappfordisabledpeople.utils.adjustPoint
import com.example.amobileappfordisabledpeople.utils.adjustSize
import com.example.amobileappfordisabledpeople.utils.drawBounds
import com.google.mlkit.vision.face.Face
import java.util.concurrent.ExecutorService
import kotlin.math.abs

@Composable
fun MoodTrackingScreen(
    cameraExecutor: ExecutorService,
    moodTrackingViewModel: MoodTrackingViewModel = hiltViewModel(),
    navigateToFaceRecognition: () -> Unit = {},
    navigateToExploreMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView: PreviewView

    val screenWidth = remember { mutableStateOf(context.resources.displayMetrics.widthPixels) }
    val screenHeight = remember { mutableStateOf(context.resources.displayMetrics.heightPixels) }


    val imageWidth = remember { mutableStateOf(0) }
    val imageHeight = remember { mutableStateOf(0) }

    val faces = remember { mutableStateListOf<Face>() }

    val faceDetectionAnalyzer = FaceDetectionAnalyzer { detectedFace, width, height ->
        faces.clear()
        faces.addAll(detectedFace)
        imageWidth.value = width
        imageHeight.value = height
    }

    val imageAnalysis = ImageAnalysis.Builder()
        .setTargetRotation(android.view.Surface.ROTATION_0)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also {
            it.setAnalyzer(cameraExecutor, faceDetectionAnalyzer)
        }

    moodTrackingViewModel.initRepo(imageAnalysis)

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    if (abs(dragAmount.x) > abs(dragAmount.y)) {
                        if (abs(dragAmount.x) > DragThreshold) {
                            if (dragAmount.x > 0) {
                                navigateToFaceRecognition()
                            } else {
                                navigateToFaceRecognition
                            }
                        }
                    } else {
                        if (abs(dragAmount.y) > DragThreshold) {
                                navigateToExploreMode()
                        }
                    }
                }
            )
        },
        topBar = {
            SocializingModeBar(destinationName = "Mood Tracking")
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AndroidView(
                factory = {
                    previewView = PreviewView(it)
                    moodTrackingViewModel.showCameraPreview(previewView = previewView, lifecycleOwner = lifecycleOwner)
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            DrawFaces(faces, imageHeight.value, imageWidth.value, screenWidth.value, screenHeight.value)
        }
    }
}

@Composable
fun DrawFaces(faces: List<Face>, imageWidth: Int, imageHeight: Int, screenWidth: Int, screenHeight: Int) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        faces.forEach { face ->
            val boundingBox = face.boundingBox.toComposeRect()
            val topLeft = adjustPoint(PointF(boundingBox.left, boundingBox.top), imageWidth, imageHeight, screenWidth, screenHeight)
            val size = adjustSize(
                boundingBox.size,
                imageWidth, imageHeight, screenWidth, screenHeight
            )
            drawBounds(topLeft, size, Color.Yellow, 5f)

            // Calculate the level of certainty
            val certainty = "Certainty: ${calculateCertainty(face.headEulerAngleX, face.headEulerAngleY, face.headEulerAngleZ)}%"

            val smileLevel = "Smile: ${face.smilingProbability}%"
            val upsetLevel = "Upset: ${calculateUpsetLevel(face)}%"

            drawContext.canvas.nativeCanvas.drawText(
                "$certainty\n$smileLevel",
                topLeft.x,
                topLeft.y - 10, // Position the text above the bounding box
                android.graphics.Paint().apply {
                    color = android.graphics.Color.YELLOW
                    textSize = 40f
                }
            )
        }
    }
}

fun calculateCertainty(angleX: Float, angleY: Float, angleZ: Float): Int {
    // Example calculation: combine the angles to form a certainty score
    val maxAngle = 30.0f // Define a maximum angle for normalization
    val normalizedX = (maxAngle - Math.abs(angleX)) / maxAngle
    val normalizedY = (maxAngle - Math.abs(angleY)) / maxAngle
    val normalizedZ = (maxAngle - Math.abs(angleZ)) / maxAngle
    return ((normalizedX + normalizedY + normalizedZ) / 3 * 100).toInt()
}

fun calculateUpsetLevel(face: Face): Int {
    // Define weights for each attribute
    val smileWeight = 0.5f
    val eyeOpenWeight = 0.3f
    val headPoseWeight = 0.2f

    // Calculate the smile score (higher probability means less upset)
    val smileScore = 1 - (face.smilingProbability ?: 0f)

    // Calculate the eye openness score (lower probability means more upset)
    val leftEyeOpenScore = 1 - (face.leftEyeOpenProbability ?: 0f)
    val rightEyeOpenScore = 1 - (face.rightEyeOpenProbability ?: 0f)
    val eyeOpenScore = (leftEyeOpenScore + rightEyeOpenScore) / 2

    // Calculate the head pose score (larger angles mean more upset)
    val maxAngle = 30.0f
    val normalizedX = Math.abs(face.headEulerAngleX) / maxAngle
    val normalizedY = Math.abs(face.headEulerAngleY) / maxAngle
    val normalizedZ = Math.abs(face.headEulerAngleZ) / maxAngle
    val headPoseScore = (normalizedX + normalizedY + normalizedZ) / 3

    // Combine the scores using the defined weights
    val upsetLevel = (smileScore * smileWeight + eyeOpenScore * eyeOpenWeight + headPoseScore * headPoseWeight) * 100

    return upsetLevel.toInt()
}

