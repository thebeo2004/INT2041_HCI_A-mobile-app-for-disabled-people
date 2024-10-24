package com.example.amobileappfordisabledpeople

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DefaultHomeScreen(currentMode: String, onModeChange: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Navigation Bar
        NavigationBar(currentMode = currentMode, onModeChange = onModeChange)

        // Camera or mode-specific content
        when (currentMode) {
            "Danger" -> CameraPreview()
            "Find" -> ModeContent("Object Description Mode")
            "Description" -> ModeContent("Environment Description Mode")
        }
    }
}

@Composable
fun NavigationBar(currentMode: String, onModeChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ModeButton("Danger", currentMode == "Danger") { onModeChange("Danger") }
        ModeButton("Find", currentMode == "Find") { onModeChange("Find") }
        ModeButton("Description", currentMode == "Description") { onModeChange("Description") }
    }
}

@Composable
fun ModeButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text = text, fontSize = 18.sp)
    }
}

@Composable
fun CameraPreview() {
    // Placeholder for camera preview
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Camera Preview", fontSize = 24.sp)
    }
}

@Composable
fun ModeContent(contentDescription: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = contentDescription, fontSize = 24.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DefaultHomeScreen(currentMode = "Danger", onModeChange = {})
}


