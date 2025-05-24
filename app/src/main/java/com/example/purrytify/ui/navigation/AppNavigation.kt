package com.example.purrytify.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.ui.screens.HomeScreen
import com.example.purrytify.ui.screens.LibraryScreen
import com.example.purrytify.ui.screens.OnlineSongsScreen
import com.example.purrytify.ui.screens.ProfileScreen
import com.example.purrytify.ui.screens.QRScannerScreen
import com.example.purrytify.ui.screens.QueueScreen
import com.example.purrytify.util.NetworkConnectionObserver
import com.example.purrytify.viewmodels.MainViewModel

object Destinations {
    const val HOME_ROUTE = "home"
    const val LIBRARY_ROUTE = "library"
    const val PROFILE_ROUTE = "profile"
    const val QUEUE_ROUTE = "queue"
    const val ONLINE_SONGS_ROUTE = "online_songs"
    const val QR_SCANNER_ROUTE = "qr_scanner"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel,
    networkConnectionObserver: NetworkConnectionObserver
) {

    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Destinations.HOME_ROUTE,
        modifier = modifier
    ) {
        composable(Destinations.HOME_ROUTE) {
            HomeScreen(navController = navController)
        }
        composable(Destinations.LIBRARY_ROUTE) {
            LibraryScreen()
        }
        composable(Destinations.PROFILE_ROUTE) {
            ProfileScreen()
        }
        composable(Destinations.QUEUE_ROUTE) {
            QueueScreen(
                onNavigateBack = { navController.popBackStack() },
                mainViewModel = mainViewModel
            )
        }
        composable(Destinations.ONLINE_SONGS_ROUTE) {
            OnlineSongsScreen(
                onSongSelected = { song ->
                    // Play the online song
                    mainViewModel.playOnlineSong(song)
                }
            )
        }
        composable(Destinations.QR_SCANNER_ROUTE) {
            QRScannerScreen(
                onQRCodeDetected = { qrCode ->
                    // Handle QR code detection
                    val songId = com.example.purrytify.util.ShareUtils.extractSongIdFromDeepLink(qrCode)
                    if (songId != null) {
                        // Navigate back and handle deep link
                        navController.popBackStack()


                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qrCode))
                        context.startActivity(intent)
                    } else {
                        // Show error - invalid QR code
                        // Navigate back and show error message
                        navController.popBackStack()
                    }
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
    }
}