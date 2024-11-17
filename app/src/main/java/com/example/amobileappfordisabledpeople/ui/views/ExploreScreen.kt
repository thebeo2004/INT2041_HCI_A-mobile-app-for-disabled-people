package com.example.amobileappfordisabledpeople.ui.views

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.amobileappfordisabledpeople.AppBar
import com.example.amobileappfordisabledpeople.Data.CoordinatesModelRepoImpl
import com.example.amobileappfordisabledpeople.Data.RequestModel
import com.example.amobileappfordisabledpeople.SpeechRecognizerContract
import com.example.amobileappfordisabledpeople.presentation.MainViewModel
import com.example.amobileappfordisabledpeople.presentation.SpeechRecognizerViewModel
import com.example.amobileappfordisabledpeople.ui.navigation.ExploreDestination
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ExploreScreen(navigateToDangerWarning: () -> Unit = {},
                  navigateToDetection: () -> Unit = {},
                  mainViewModel: MainViewModel = hiltViewModel(),
                  speechRecognizerViewModel: SpeechRecognizerViewModel = viewModel()
) {
    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
            }
        }
    }

    // Dispose of TextToSpeech when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            textToSpeech?.shutdown()
        }
    }

    val audioPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    SideEffect {
        if (!audioPermissionState.status.isGranted) {
            audioPermissionState.launchPermissionRequest()
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = SpeechRecognizerContract(),
        onResult = {
            //Here we get the text
            speechRecognizerViewModel.changeTextValue(it.toString())
        }
    )

    val speechRecognizerState by speechRecognizerViewModel.state.collectAsState()

    var imageHeight by remember { mutableIntStateOf(0) }
    var imageWidth by remember { mutableIntStateOf(0) }

    val viewModel = viewModel<CoordinatesModelViewModel>(
        factory = CoordinatesModelViewModelFactory(
            coordinatesModelRepo = CoordinatesModelRepoImpl(
                applicationContext = context.applicationContext
            )
        )
    )
    val uiState = viewModel.uiState
    var showCameraPreview by rememberSaveable { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val screenWidth = configuration.screenWidthDp
    var previewView: PreviewView

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(key1 = uiState) {
        if (uiState is UiState.Error) {
            scope.launch {
                snackbarHostState.showSnackbar(uiState.e)
            }
        }
    }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) {
                Snackbar(
                    snackbarData = it,
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            }
        },

        modifier = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures { change, dragAmount ->
                if (dragAmount > 0) {
                    navigateToDangerWarning()
                } else {
                    navigateToDetection()
                }
            }
        },

        topBar = {
            AppBar(destinationName = stringResource(ExploreDestination.titleRes))
        }
    ) { it ->
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (!showCameraPreview) {
                                speechRecognizerLauncher.launch(Unit)
                            }
                        }
                    )
                },

            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display the captured image with bounding box
            if (!showCameraPreview) {
                mainViewModel.capturedImageUri.value?.let {
                    ImageWithBoundingBox(
                        uri = it,
                    ) { height, width, _ ->
                        imageHeight = height
                        imageWidth = width
                    }
                }
            }

            if (uiState is UiState.Loading) {
                // Display a progress indicator while loading
                CircularProgressIndicator(color = Color(0xFF29B6F6))
            } else {

                if (showCameraPreview) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        mainViewModel.captureAndSave(context) {
                                            showCameraPreview = false
                                        }
                                    }
                                )
                            }
                    ) {
                        AndroidView(
                            factory = {
                                previewView = PreviewView(it)
                                mainViewModel.showCameraPreview(previewView = previewView, lifecycleOwner = lifecycleOwner)
                                previewView
                            },
                            modifier = Modifier
                                .height((screenHeight * 0.8).dp)
                                .width((screenWidth).dp)
                        )
                    }
                }

                if (!showCameraPreview) {
                    speechRecognizerState.text?.let { text ->
//                        Text(
//                            text = text,
//                            fontSize = 24.sp
//                        )
//
//                        Log.d("ExploreScreen", "text: $text")
                        Toast(context).apply {
                            duration = Toast.LENGTH_SHORT
                            setText(text)
                            show()
                        }

                        viewModel.getCoordinatesModel(
                            requestModel = RequestModel(
                                text = text,
                                uri = mainViewModel.capturedImageUri.value ?: Uri.EMPTY,
                                height = imageHeight.toString(),
                                width = imageWidth.toString()
                            )
                        )
                    }
                }

                if (uiState is UiState.CaptionResponse) {
                    DrawCaptionResponse(uiState.result)
                    speechRecognizerViewModel.changeTextValue(null)
                }

            }

            if (uiState is UiState.CaptionResponse) {
                DrawCaptionResponse(uiState.result)
                speechRecognizerViewModel.changeTextValue(null)

                // Convert text to speech
                textToSpeech?.speak(uiState.result, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }
}

@Composable
private fun DrawCaptionResponse(result: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TitleText(
            text = "PaliGemma response:",
        )
        Text(
            text = result,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White
        )
    }
}

@Composable
private fun TitleText(text: String) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White
    )
}

@Composable
private fun ImageWithBoundingBox(
    uri: Uri,
    onSizeChange: (Int, Int, Float) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    Box {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .build(),
                modifier = Modifier
                    .height(screenHeight * 0.8f)
                    .width(screenWidth)
                    .onGloballyPositioned {
                        onSizeChange(it.size.height, it.size.width, it.positionInRoot().x)
                    },
                contentDescription = null
            )
        }
    }
}

