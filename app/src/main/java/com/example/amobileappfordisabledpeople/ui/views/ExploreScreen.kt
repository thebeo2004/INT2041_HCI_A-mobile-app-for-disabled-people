//package com.example.amobileappfordisabledpeople.ui.views
//
//import androidx.compose.foundation.gestures.detectHorizontalDragGestures
//import androidx.compose.material.Scaffold
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.res.stringResource
//import com.example.amobileappfordisabledpeople.AppBar
//import com.example.amobileappfordisabledpeople.ui.navigation.ExploreDestination
//
//@Composable
//fun ExploreScreen(
//    navigateToDetection: () -> Unit = {},
//    navigateToDangerWarning: () -> Unit = {}
//) {
//
//    Scaffold(
//        modifier = Modifier.pointerInput(Unit) {
//            detectHorizontalDragGestures { change, dragAmount ->
//                if (dragAmount < 0) {
//                    navigateToDetection()
//                } else {
//                    navigateToDangerWarning()
//                }
//            }
//        },
//        topBar = {
//            AppBar(destinationName = stringResource(ExploreDestination.titleRes))
//        }
//    )
//}


package com.example.amobileappfordisabledpeople.ui.views

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.paligemma.data.RequestModel
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.paligemma.presentation.CoordinatesModelViewModel
import com.example.paligemma.presentation.UiState
import com.example.paligemma.presentation.createImageFile
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun ExploreScreen(viewModel: CoordinatesModelViewModel = viewModel()) {
    val context = LocalContext.current
    var imageHeight by remember { mutableStateOf(0) }
    var imageWidth by remember { mutableStateOf(0) }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var galleryUri by remember { mutableStateOf<Uri?>(null) }
    var textPrompt by rememberSaveable { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        if (it) {
            galleryUri = null
            viewModel.resetData()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            context.createImageFile()?.let { file ->
                cameraUri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
                cameraLauncher.launch(cameraUri)
            }
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.resetData()
            cameraUri = null
            galleryUri = it
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Error) {
            scope.launch { snackbarHostState.showSnackbar((uiState as UiState.Error).message) }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) {
                Snackbar(snackbarData = it, containerColor = Color.Red, contentColor = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ImageSection(cameraUri ?: galleryUri, uiState, onSizeChange = { h, w -> imageHeight = h; imageWidth = w })
            ControlButtons(context, permissionLauncher, cameraLauncher, galleryLauncher)
            PromptTextField(textPrompt) { textPrompt = it }
            SubmitButton(viewModel, textPrompt, cameraUri ?: galleryUri, imageHeight, imageWidth)

            if (uiState is UiState.SegmentationResponse) DrawSegmentationText(uiState.result)
            if (uiState is UiState.CaptionResponse) DrawCaptionText(uiState.result)
        }
    }
}

@Composable
fun ImageSection(uri: Uri?, uiState: UiState, onSizeChange: (Int, Int) -> Unit) {
    if (uri != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(uri).build(),
            modifier = Modifier
                .heightIn(max = 450.dp)
                .fillMaxWidth()
                .onGloballyPositioned { coords -> onSizeChange(coords.size.height, coords.size.width) },
            contentDescription = null
        )

        if (uiState is UiState.ObjectDetectionResponse) DrawBoundingBoxes(uiState.result)
        if (uiState is UiState.SegmentationResponse) DrawSegmentationOverlay(uiState.result)
    }
}

@Composable
fun ControlButtons(
    context: Context,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    galleryLauncher: ManagedActivityResultLauncher<String, Uri?>
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraLauncher.launch(context.createImageFileUri() ?: return@Button)
                } else permissionLauncher.launch(android.Manifest.permission.CAMERA)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6))
        ) {
            Text("Open Camera")
        }

        Button(
            onClick = { galleryLauncher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6))
        ) {
            Text("Upload Image")
        }
    }
}

@Composable
fun PromptTextField(prompt: String, onPromptChange: (String) -> Unit) {
    OutlinedTextField(
        value = prompt,
        onValueChange = onPromptChange,
        label = { Text("Prompt") },
        placeholder = { Text("Enter text prompt") },
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF29B6F6), cursorColor = Color.White),
        trailingIcon = {
            if (prompt.isNotEmpty()) IconButton(onClick = { onPromptChange("") }) {
                Icon(imageVector = Icons.Rounded.Clear, contentDescription = null)
            }
        }
    )
}

@Composable
fun SubmitButton(viewModel: CoordinatesModelViewModel, prompt: String, uri: Uri?, height: Int, width: Int) {
    Button(
        onClick = {
            viewModel.getCoordinatesModel(
                requestModel = RequestModel(text = prompt, uri = uri ?: Uri.EMPTY, height = height.toString(), width = width.toString())
            )
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6))
    ) {
        Text("Submit")
    }
}

// Additional helper Composables (DrawSegmentationText, DrawCaptionText, DrawBoundingBoxes, DrawSegmentationOverlay) would go here
