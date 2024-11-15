package com.example.amobileappfordisabledpeople.ui.views

import android.net.Uri
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.amobileappfordisabledpeople.Data.CoordinatesModelRepoImpl
import com.example.amobileappfordisabledpeople.Data.RequestModel
import com.example.amobileappfordisabledpeople.presentation.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ExploreScreen(navigateToDangerWarning: () -> Unit = {},
                  navigateToDetection: () -> Unit = {},
                  mainViewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current

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
    var showCameraPreview by rememberSaveable { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val screenWidth = configuration.screenWidthDp
    var previewView: PreviewView

    val textPrompt = "brief description of the image"

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
        }
    ) { it ->
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            mainViewModel.capturedImageUri.value?.let {
                ImageWithBoundingBox(
                    uri = it
                ) { height, width, _ ->
                    imageHeight = height
                    imageWidth = width
                }
            } ?: Text(
                text = "No image captured",
                fontSize = 16.sp,
                color = Color.White
            )

            if (uiState is UiState.Loading) {
                CircularProgressIndicator(color = Color(0xFF29B6F6))
            } else {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            showCameraPreview = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(all = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF29B6F6),
                            contentColor = Color(0xFFFFFFFF)
                        )
                    ) {
                        Text("Open Camera")
                    }
                }

                if (showCameraPreview) {
                    Box(
                        modifier = Modifier
                            .height((screenHeight * 0.8).dp)
                            .width((screenWidth).dp)
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

                    Box(
                        modifier = Modifier
                            .height((screenHeight * 0.15).dp)
                            .padding(all = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                if (cameraPermissionState.status.isGranted) {
                                    mainViewModel.captureAndSave(context) {
                                        showCameraPreview = false
                                    }
                                } else {
                                    Toast.makeText(context, "Camera permission not granted", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF29B6F6),
                                contentColor = Color(0xFFFFFFFF)
                            )
                        ) {
                            Text("Capture Image")
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.getCoordinatesModel(
                            requestModel = RequestModel(
                                text = textPrompt,
                                uri = mainViewModel.capturedImageUri.value ?: Uri.EMPTY,
                                height = imageHeight.toString(),
                                width = imageWidth.toString()
                            )
                        )
                    },
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF29B6F6),
                        contentColor = Color(0xFFFAFAFA)
                    )
                ) {
                    Text("Submit")
                }

                if (uiState is UiState.CaptionResponse) {
                    DrawCaptionResponse(uiState.result)
                }

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
    onSizeChange: (Int, Int, Float) -> Unit
) {
    Box {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .build(),
                modifier = Modifier
                    .heightIn(max = 450.dp)
                    .onGloballyPositioned {
                        onSizeChange(it.size.height, it.size.width, it.positionInRoot().x)
                    },
                contentDescription = null
            )
        }
    }
}

