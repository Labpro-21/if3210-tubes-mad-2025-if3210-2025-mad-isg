package com.example.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp // Import ini penting untuk .dp
import com.example.purrytify.ui.theme.PurrytifyTheme

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
    var selectedScreen by remember { mutableStateOf(Screen.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = {
                            Text(
                                text = screen.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Content based on selected screen
        when (selectedScreen) {
            Screen.Home -> HomeScreen(Modifier.padding(innerPadding))
            Screen.Profile -> ProfileScreen(Modifier.padding(innerPadding))
        }
    }
}

// Screen enum to define navigation
enum class Screen(val title: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Profile("Profile", Icons.Default.Person)
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Recently Played",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No recent songs",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "New Songs",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No new songs available",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No songs in library",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Username: Not Logged In",
            style = MaterialTheme.typography.bodyMedium
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
