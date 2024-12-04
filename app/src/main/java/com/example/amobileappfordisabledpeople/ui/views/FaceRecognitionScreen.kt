package com.example.amobileappfordisabledpeople.ui.views

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.amobileappfordisabledpeople.common.components.CameraView
import com.example.amobileappfordisabledpeople.common.utils.adjustPoint
import com.example.amobileappfordisabledpeople.common.utils.adjustSize
import com.example.amobileappfordisabledpeople.common.utils.drawBounds
import com.example.amobileappfordisabledpeople.features.FaceDetectionAnalyzer
import com.google.mlkit.vision.face.Face
import kotlin.text.toInt
import kotlin.times

@Composable
fun FaceRecognitionScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val faces = remember { mutableStateListOf<Face>() }

    val screenWidth = remember { mutableStateOf(context.resources.displayMetrics.widthPixels) }
    val screenHeight = remember { mutableStateOf(context.resources.displayMetrics.heightPixels) }

    val imageWidth = remember { mutableStateOf(0) }
    val imageHeight = remember { mutableStateOf(0) }


    Box(modifier = Modifier.fillMaxSize()) {
        CameraView(
            context = context,
            lifecycleOwner = lifecycleOwner,
            analyzer = FaceDetectionAnalyzer(context) { detectedFaces, width, height ->
                faces.clear()
                faces.addAll(detectedFaces)
                imageWidth.value = width
                imageHeight.value = height
            }
        )

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(10.dp)
                .fillMaxHeight()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Face Recognition",
                    color = Color.White
                )
            }
        }
        DrawFaces(faces = faces, imageHeight.value, imageWidth.value, screenWidth.value, screenHeight.value)
    }
}

@Composable
fun DrawFaces(faces: List<Face>, imageWidth: Int, imageHeight: Int, screenWidth: Int, screenHeight: Int) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        faces.forEach { face ->
            val boundingBox = face.boundingBox.toComposeRect()
            val topLeft = adjustPoint(PointF(boundingBox.topLeft.x, boundingBox.topLeft.y), imageWidth, imageHeight, screenWidth, screenHeight)
            val size = adjustSize(boundingBox.size, imageWidth, imageHeight, screenWidth, screenHeight)
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

fun calculateCertainty(angleX: Float, angleY: Float, angleZ: Float): Int {
    // Example calculation: combine the angles to form a certainty score
    val maxAngle = 30.0f // Define a maximum angle for normalization
    val normalizedX = (maxAngle - Math.abs(angleX)) / maxAngle
    val normalizedY = (maxAngle - Math.abs(angleY)) / maxAngle
    val normalizedZ = (maxAngle - Math.abs(angleZ)) / maxAngle
    return ((normalizedX + normalizedY + normalizedZ) / 3 * 100).toInt()
}


