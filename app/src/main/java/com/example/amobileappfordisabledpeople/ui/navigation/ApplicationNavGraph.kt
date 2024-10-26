package com.example.amobileappfordisabledpeople.ui.navigation

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.amobileappfordisabledpeople.features.object_detection.YuvToRgbConverter
import com.example.amobileappfordisabledpeople.ui.views.DetectionScreen
import org.tensorflow.lite.Interpreter
import java.util.concurrent.ExecutorService

@Composable
fun ApplicationNavHost(
    navController: NavHostController,
    cameraExecutor: ExecutorService,
    yuvToRgbConverter: YuvToRgbConverter,
    interpreter: Interpreter,
    labels: List<String>,
    textToSpeech: TextToSpeech,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = DetectionDestination.route,
        modifier = modifier
    ) {
        composable(route = DetectionDestination.route) {
            DetectionScreen(cameraExecutor = cameraExecutor, yuvToRgbConverter = yuvToRgbConverter, interpreter = interpreter, labels = labels, textToSpeech = textToSpeech)
        }
        composable(route = DangerWarningDestination.route) {
//            DangerWarningScreen()
        }
        composable(route = ExploreDestination.route) {
//            ExploreScreen()

        }
    }
}