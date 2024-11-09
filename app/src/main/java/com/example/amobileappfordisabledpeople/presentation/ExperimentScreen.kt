package com.example.amobileappfordisabledpeople.presentation

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.amobileappfordisabledpeople.ui.views.CameraPermission
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.amobileappfordisabledpeople.R

@SuppressLint("PermissionLaunchedDuringComposition")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ExperimentScreen(
    viewModel: MainViewModel = hiltViewModel()
) {

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    if (!cameraPermissionState.status.isGranted) {
        //SideEffect: is it really needed to use SideEffect -> Experimenting remove SideEffect later and check the result
//        SideEffect {
//            cameraPermissionState.launchPermissionRequest()
//        }
        CameraPermission(cameraPermissionState)
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val screenWidth = configuration.screenWidthDp
    var previewView: PreviewView

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        //Show camera preview once permission is granted
        if (cameraPermissionState.status.isGranted) {
            Box(
                modifier = Modifier.height((screenHeight * 0.85).dp)
            ) {
                //Because Google doesn't have composable for Camera Preview, we're using AndroidView to show it
                AndroidView(
                    factory = {
                        previewView = PreviewView(it)
                        viewModel.showCameraPreview(previewView = previewView, lifecycleOwner = lifecycleOwner)
                        previewView
                    },
                    modifier = Modifier.height((screenHeight * 0.8).dp)
                )
            }

            Box(
                modifier = Modifier.height((screenHeight * 0.8).dp)
            ) {
                IconButton(
                    onClick = {
                        if (cameraPermissionState.status.isGranted) {
                            viewModel.captureAndSave(context)
                        } else {
                            Toast.makeText(context, "Camera permission not granted", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_baseline_camera_24),
                        contentDescription = "Capture Image",
                        tint = Color.Magenta)
                }
            }
        }
    }
}
