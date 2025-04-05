package com.example.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.ui.components.BottomNavbar
import com.example.purrytify.ui.components.MiniPlayer
import com.example.purrytify.ui.navigation.AppNavigation
import com.example.purrytify.ui.theme.PurrytifyTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.ui.viewmodels.MainViewModel
import com.example.purrytify.ui.viewmodels.ViewModelFactoryProvider

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

@Composable
fun PurrytifyApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel(factory = ViewModelFactoryProvider.Factory)
    val currentSong = viewModel.currentSong.collectAsState().value
    val isPlaying = viewModel.isPlaying.collectAsState().value
    Scaffold(
        bottomBar = {
            Column {
                // Show mini player if a song is playing
                if (currentSong != null) {
                    MiniPlayer(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onPlayerClick = { /* Navigate to full player */ }
                    )
                } else {
                Text("Debug: currentSong is null", color = Color.Red)
                }
                BottomNavbar(navController)
            }
        }
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PurrytifyAppPreview() {
    PurrytifyTheme {
        PurrytifyApp()
    }
}
