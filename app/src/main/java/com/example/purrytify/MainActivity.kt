package com.example.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

import com.example.purrytify.ui.screens.HomeScreen
import com.example.purrytify.ui.screens.LoginScreen
import com.example.purrytify.ui.theme.PurrytifyTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.ui.viewmodels.MainViewModel
import com.example.purrytify.ui.viewmodels.ViewModelFactoryProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PurrytifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isLoggedIn by remember { mutableStateOf(false) }

                    if (isLoggedIn) {
                        HomeScreen()
                    } else {
                        LoginScreen(
                            onLoginSuccess = {
                                isLoggedIn = true
                            }
                        )
                    }
                }
            }
        }
    }
}
