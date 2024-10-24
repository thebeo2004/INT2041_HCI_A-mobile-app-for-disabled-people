package com.example.amobileappfordisabledpeople

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.amobileappfordisabledpeople.ui.theme.AMobileAppForDisabledPeopleTheme
import com.example.amobileappfordisabledpeople.DefaultHomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

@Composable
fun MyApp() {
    // State to manage the current mode ('Danger', 'Find', 'Description')
    var currentMode by remember { mutableStateOf("Danger") }

    // Material Theme wrapper
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DefaultHomeScreen(currentMode = currentMode, onModeChange = { currentMode = it })
        }
    }
}
