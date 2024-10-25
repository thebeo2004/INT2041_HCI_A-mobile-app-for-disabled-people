package com.example.amobileappfordisabledpeople

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.amobileappfordisabledpeople.ui.navigation.ApplicationNavHost

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
fun App(navHostController: NavHostController = rememberNavController()) {
    ApplicationNavHost(navHostController)
}

@Preview
@Composable
fun AppBarPreview() {
    AppBar("Preview")
}