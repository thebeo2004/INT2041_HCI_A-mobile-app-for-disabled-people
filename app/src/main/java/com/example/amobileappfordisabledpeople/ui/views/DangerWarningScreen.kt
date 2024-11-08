package com.example.amobileappfordisabledpeople.ui.views

import android.content.Context
import android.graphics.Paint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.*
import com.example.amobileappfordisabledpeople.features.object_detection.ObjectDetector
import com.example.amobileappfordisabledpeople.R
import com.example.amobileappfordisabledpeople.features.object_detection.YuvToRgbConverter
import com.example.amobileappfordisabledpeople.ui.theme.ObjectDetectionTheme
import org.tensorflow.lite.Interpreter
import java.util.concurrent.ExecutorService
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import com.example.amobileappfordisabledpeople.AppBar
import com.example.amobileappfordisabledpeople.ui.navigation.DangerWarningDestination
import com.example.amobileappfordisabledpeople.ui.navigation.DetectionDestination

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DangerWarningScreen(
    cameraExecutor: ExecutorService,
    yuvToRgbConverter: YuvToRgbConverter,
    interpreter: Interpreter,
    labels: List<String>,
    textToSpeech: TextToSpeech,
    navigateToExplore: () -> Unit = {},
    navigateToDetection: () -> Unit = {}
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures { change, dragAmount ->
                if (dragAmount < 0) {
                    navigateToExplore()
                } else {
                    navigateToDetection()
                }
            }
        },
        topBar = {
            AppBar(destinationName = stringResource(DangerWarningDestination.titleRes))
        }
    ) {
            innerPadding ->
        if (cameraPermissionState.status.isGranted) {
            OpenWarningCamera(
                cameraExecutor = cameraExecutor,
                yuvToRgbConverter = yuvToRgbConverter,
                interpreter = interpreter,
                labels = labels,
                textToSpeech = textToSpeech,
                contentPadding = innerPadding
            )
        } else {
            Permission(cameraPermissionState)
        }
    }
}

val dangerousObjects = listOf(
    "bicycle",
    "car",
    "motorcycle",
    "bus",
    "train",
    "truck",
    "boat",
    "traffic light",
    "fire hydrant",
    "stop sign",
    "parking meter",
    "bench",
    "skateboard",
    "bottle",
    "chair",
    "couch",
    "dining table",
    "desk"
)


@Composable
fun OpenWarningCamera(
    cameraExecutor: ExecutorService,
    yuvToRgbConverter: YuvToRgbConverter,
    interpreter: Interpreter,
    labels: List<String>,
    textToSpeech: TextToSpeech, // Nhận TextToSpeech,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CameraWarningPreview(
            context = context,
            lifecycleOwner = lifecycleOwner,
            cameraExecutor = cameraExecutor,
            yuvToRgbConverter = yuvToRgbConverter,
            interpreter = interpreter,
            labels = labels,
            textToSpeech = textToSpeech // Truyền TextToSpeech vào CameraWarningPreview
        )
    }
}

@Composable
fun CameraWarningPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    yuvToRgbConverter: YuvToRgbConverter,
    interpreter: Interpreter,
    labels: List<String>,
    viewModel: DetectionViewModel = hiltViewModel(),
    textToSpeech: TextToSpeech // Nhận TextToSpeech từ OpenWarningCamera
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var preview by remember { mutableStateOf<androidx.camera.core.Preview?>(null) }
    val executor = ContextCompat.getMainExecutor(context)
    val cameraProvider = cameraProviderFuture.get()

    val drawCanvas by remember { viewModel.isLoading }
    val detectionListObject by remember { viewModel.detectionList }

    val paint = Paint()
    val pathColorList = listOf(Color.Red, Color.Green, Color.Cyan, Color.Blue)
    val pathColorListInt = listOf(
        android.graphics.Color.RED,
        android.graphics.Color.GREEN,
        android.graphics.Color.CYAN,
        android.graphics.Color.BLUE
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ){
        val boxConstraint = this
        val sizeWith = with(LocalDensity.current) { boxConstraint.maxWidth.toPx() }
        val sizeHeight = with(LocalDensity.current) { boxConstraint.maxHeight.toPx() }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    var previousDetectedObjects: List<String> = emptyList()
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setTargetRotation(android.view.Surface.ROTATION_0)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(
                                cameraExecutor,
                                ObjectDetector(
                                    yuvToRgbConverter = yuvToRgbConverter,
                                    interpreter = interpreter,
                                    labels = labels,
                                    resultViewSize = Size(sizeWith.toInt(), sizeHeight.toInt()
                                    )
                                ) { detectedObjectList ->
                                    // So sánh danh sách đối tượng hiện tại với danh sách trước đó
                                    var check: Boolean = detectedObjectList.map { it.label } == previousDetectedObjects
                                    Log.d ("Check", "Check: ${check}")
                                    if (detectedObjectList.isNotEmpty() && !check) {

                                        // Cập nhật danh sách đối tượng đã phát hiện
                                        Log.d("ObjectDetection", "Previous Detected Objects: ${previousDetectedObjects}")
                                        previousDetectedObjects = detectedObjectList.map { it.label }
                                        Log.d("ObjectDetection", "Detected Objects: ${previousDetectedObjects}")

                                        val intersection = dangerousObjects.intersect(previousDetectedObjects).toList()
                                        // Đọc nhãn của đối tượng đầu tiên (hoặc tất cả các đối tượng nếu muốn)
                                        intersection.forEach { detectedObject ->
                                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                val vibrationEffect = VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE) // Rung 300ms
                                                vibrator.vibrate(vibrationEffect)
                                            } else {
                                                vibrator.vibrate(300) // Rung 300ms cho các phiên bản cũ hơn
                                            }
                                            textToSpeech.speak(
                                                detectedObject,
                                                TextToSpeech.QUEUE_FLUSH,
                                                null,
                                                null
                                            )
                                        }
                                    }
                                    viewModel.setList(detectedObjectList)
                                }
                            )
                        }

                    imageCapture = ImageCapture.Builder()
                        .setTargetRotation(previewView.display.rotation)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageCapture,
                        preview,
                        imageAnalyzer
                    )
                }, executor)
                preview = androidx.camera.core.Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                previewView
            }
        )

        if (drawCanvas){
            Canvas(
                modifier = Modifier.fillMaxSize(),
                onDraw = {

                    detectionListObject.mapIndexed { i, detectionObject ->
                        Log.d("Object", detectionObject.label + " --- "  +detectionObject.score)
                        paint.apply {
                            color = pathColorListInt[i]
                            style = Paint.Style.FILL
                            isAntiAlias = true
                            textSize = 50f
                        }

                        drawRect(
                            color = pathColorList[i],
                            topLeft =  Offset(
                                x = detectionObject.boundingBox.left,
                                y = detectionObject.boundingBox.top
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                width = detectionObject.boundingBox.width(),
                                height = detectionObject.boundingBox.height()
                            ),
                            style = Stroke(width = 3.dp.toPx())
                        )

                        drawIntoCanvas {
                            it.nativeCanvas.drawText(
                                detectionObject.label + " " + "%,.2f".format(detectionObject.score * 100) + "%",
                                detectionObject.boundingBox.left,            // x-coordinates of the origin (top left)
                                detectionObject.boundingBox.top - 5f, // y-coordinates of the origin (top left)
                                paint
                            )
                        }
                    }
                }
            )
        }
    }
}


//----------------------------- PERMISSION --------------------------------------
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun Permission(
    cameraPermissionState: PermissionState
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!cameraPermissionState.status.isGranted) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally) {

                val textToShow = if (cameraPermissionState.status.shouldShowRationale) {
                    "The camera is important for this app.\n Please grant the permission."
                } else {
                    "Camera not available"
                }
                Text(textToShow, textAlign = TextAlign.Center, color = MaterialTheme.colors.onSurface)

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    shape = CircleShape,
                    onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Request permission")
                    Icon(
                        painterResource(id = R.drawable.ic_baseline_camera_24),
                        contentDescription = "Icon camera", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
