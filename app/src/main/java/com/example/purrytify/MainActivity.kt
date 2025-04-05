package com.example.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.ui.components.BottomNavbar
import com.example.purrytify.ui.navigation.AppNavigation
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.ui.screens.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PurrytifyTheme {
                PurrytifyApp()
            }
        }
    }
}
//
//@Composable
//fun PurrytifyApp() {
//    val navController = rememberNavController()
//    Scaffold(
//        bottomBar = { BottomNavbar(navController) }
//    ) { innerPadding ->
//        AppNavigation(
//            navController = navController,
//            modifier = Modifier.padding(innerPadding)
//        )
//    }
//}
//

// coba coba
@Composable
fun PurrytifyApp() {
    LoginScreen()
}

@Preview(showBackground = true)
@Composable
fun PurrytifyAppPreview() {
    PurrytifyTheme {
        PurrytifyApp()
    }
}
