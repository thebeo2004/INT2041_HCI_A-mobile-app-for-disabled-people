package com.example.amobileappfordisabledpeople.ui.views

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.amobileappfordisabledpeople.Data.CoordinatesModelRepoImpl
import com.example.amobileappfordisabledpeople.Data.RequestModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.io.File
import java.util.Objects

fun Context.createImageFile(): File {
    // Create an image file name
    val timeStamp = System.currentTimeMillis().toString()
    val imageFileName = "JPEG_" + timeStamp + "_"
    val image = File.createTempFile(
        imageFileName, /* prefix */
        ".jpg", /* suffix */
        externalCacheDir      /* directory */
    )
    return image
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ExploreScreen(navigateToDangerWarning: () -> Unit = {},
                  navigateToDetection: () -> Unit = {}) {
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

    var cameraImageFile: File?
    var cameraUri: Uri? = remember {
        null
    }

    var cameraImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                cameraImageUri = cameraUri
                viewModel.resetData()
            }
        }

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(key1 = cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            cameraImageFile = context.createImageFile()
            cameraUri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                cameraImageFile!!
            )
            Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            cameraPermissionState.launchPermissionRequest()
        }
    }

    val textPrompt = "captioning"

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
            if (cameraImageUri != null) {
                ImageWithBoundingBox(
                    uri = cameraImageUri!!,
                ) { h, w, leftDistance ->
                    imageHeight = h
                    imageWidth = w
                    viewModel.imageLeftDistance = leftDistance
                }
            }

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
                            cameraImageFile = context.createImageFile()
                            cameraUri = FileProvider.getUriForFile(
                                Objects.requireNonNull(context),
                                context.packageName + ".provider", cameraImageFile!!
                            )

                            val permissionCheckResult =
                                ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                )
                            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch(cameraUri!!)
                            } else {
                                // Request a permission
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(all = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            // Old color = #1A73E8
                            containerColor = Color(0xFF29B6F6),
                            contentColor = Color(0xFFFFFFFF)
                        )
                    ) {
                        Text("Open Camera")
                    }
                }

                Button(
                    onClick = {
                        viewModel.getCoordinatesModel(
                            requestModel = RequestModel(
                                text = textPrompt,
                                uri = cameraImageUri ?: Uri.EMPTY,
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

            }
        }
    }
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

