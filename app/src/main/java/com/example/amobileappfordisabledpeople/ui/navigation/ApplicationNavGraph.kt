package com.example.amobileappfordisabledpeople.ui.navigation

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.amobileappfordisabledpeople.features.object_detection.YuvToRgbConverter
import com.example.amobileappfordisabledpeople.ui.views.DangerWarningScreen
import com.example.amobileappfordisabledpeople.ui.views.DetectionScreen
import com.example.amobileappfordisabledpeople.ui.views.ExploreScreen
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
            DetectionScreen(cameraExecutor = cameraExecutor,
                yuvToRgbConverter = yuvToRgbConverter,
                interpreter = interpreter,
                labels = labels,
                textToSpeech = textToSpeech,
                navigateToDangerWarning = {navController.navigate(DangerWarningDestination.route)},
                navigateToExplore = {navController.navigate(ExploreDestination.route)}
            )
        }
        composable(route = DangerWarningDestination.route) {
            DangerWarningScreen(
                navigateToExplore = {navController.navigate(ExploreDestination.route)},
                navigateToDetection = {navController.navigate(DetectionDestination.route)}
            )
        }
        composable(route = ExploreDestination.route) {
            ExploreScreen(
                navigateToDetection = {navController.navigate(DetectionDestination.route)},
                navigateToDangerWarning = {navController.navigate(DangerWarningDestination.route)}
            )

        }
    }
}