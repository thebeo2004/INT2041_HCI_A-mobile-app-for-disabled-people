package com.example.amobileappfordisabledpeople

import android.speech.tts.TextToSpeech
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.amobileappfordisabledpeople.features.object_detection.YuvToRgbConverter
import com.example.amobileappfordisabledpeople.ui.navigation.ApplicationNavHost
import org.tensorflow.lite.Interpreter
import java.util.concurrent.ExecutorService

@Composable
fun AppBar(
    title: String,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
               text = title,
                fontSize = MaterialTheme.typography.h4.fontSize,
            )
        },
        modifier = modifier
    )
}

@Composable
fun App(navHostController: NavHostController = rememberNavController(), cameraExecutor: ExecutorService, yuvToRgbConverter: YuvToRgbConverter, interpreter: Interpreter, labels: List<String>, textToSpeech: TextToSpeech) {
    ApplicationNavHost(navController = navHostController, cameraExecutor = cameraExecutor, yuvToRgbConverter = yuvToRgbConverter, interpreter = interpreter, labels = labels, textToSpeech = textToSpeech)
}

@Preview
@Composable
fun AppBarPreview() {
    AppBar("Preview")
}