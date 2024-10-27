package com.example.amobileappfordisabledpeople.ui.views

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import com.example.amobileappfordisabledpeople.AppBar
import com.example.amobileappfordisabledpeople.ui.navigation.ExploreDestination

@Composable
fun ExploreScreen(
    navigateToDetection: () -> Unit = {},
    navigateToDangerWarning: () -> Unit = {}
) {

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures { change, dragAmount ->
                if (dragAmount < 0) {
                    navigateToDetection()
                } else {
                    navigateToDangerWarning()
                }
            }
        },
        topBar = {
            AppBar(destinationName = stringResource(ExploreDestination.titleRes))
        }
    ) {

    }
}