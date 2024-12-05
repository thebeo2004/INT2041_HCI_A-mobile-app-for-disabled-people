package com.example.amobileappfordisabledpeople.ui.views

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.amobileappfordisabledpeople.SocializingModeBar
import com.example.amobileappfordisabledpeople.features.face_detection.FaceDetectionAnalyzer
import com.example.amobileappfordisabledpeople.features.face_recognition.FaceRecognitionAnalyzer
import com.example.amobileappfordisabledpeople.presentation.MainViewModel
import com.example.amobileappfordisabledpeople.utils.adjustPoint
import com.example.amobileappfordisabledpeople.utils.adjustSize
import com.example.amobileappfordisabledpeople.utils.drawBounds
import com.google.mlkit.vision.face.Face
import java.util.concurrent.ExecutorService
import kotlin.collections.forEach

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FaceRecognitionScreen(
    cameraExecutor: ExecutorService,
    moodTrackingViewModel: MainViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView: PreviewView

    val screenWidth = remember { mutableStateOf(context.resources.displayMetrics.widthPixels) }
    val screenHeight = remember { mutableStateOf(context.resources.displayMetrics.heightPixels) }

    val imageWidth = remember { mutableStateOf(0) }
    val imageHeight = remember { mutableStateOf(0) }

    val faces = remember { mutableStateListOf<Face>() }

    var showDialog by remember { mutableStateOf(false) }

    val mood = remember { mutableStateOf<MoodState>(MoodState.Normal) }

    LaunchedEffect(faces) {
        mood.value = MoodState.Normal
    }

    val faceRecognitionAnalyzer = FaceRecognitionAnalyzer(context) { detectedFace, width, height ->
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
            it.setAnalyzer(cameraExecutor, faceRecognitionAnalyzer)
        }

    moodTrackingViewModel.initRepo(imageAnalysis)

    Scaffold(
        topBar = {
            SocializingModeBar(destinationName = "Face Recognition")
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

            drawContext.canvas.nativeCanvas.drawText(
                certainty,
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

fun cropFace(image: Bitmap, boundingBox: Rect): Bitmap? {
    val shift = 0
    if (boundingBox.top >= 0 && boundingBox.bottom <= image.getWidth()
        && boundingBox.top + boundingBox.height() <= image.getHeight()
        && boundingBox.left >= 0
        && boundingBox.left + boundingBox.width() <= image.getWidth()
    ) {
        return Bitmap.createBitmap(
            image,
            boundingBox.left,
            boundingBox.top + shift,
            boundingBox.width(),
            boundingBox.height()
        )
    }
    return null
}
