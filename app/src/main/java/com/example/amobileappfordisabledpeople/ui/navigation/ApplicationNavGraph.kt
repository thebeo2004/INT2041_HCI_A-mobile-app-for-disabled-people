package com.example.amobileappfordisabledpeople.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun ApplicationNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = DetectionDestination.route,
        modifier = modifier
    ) {
        composable(route = DetectionDestination.route) {
//            DetectionScreen()
        }
        composable(route = DangerWarningDestination.route) {
//            DangerWarningScreen()
        }
        composable(route = ExploreDestination.route) {
//            ExploreScreen()

        }
    }
}