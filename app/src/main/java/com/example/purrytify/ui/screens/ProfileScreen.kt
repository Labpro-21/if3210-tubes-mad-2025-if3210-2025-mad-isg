package com.example.purrytify.ui.screens

import android.provider.ContactsContract.Profile
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.MainActivity
import com.example.purrytify.R
import com.example.purrytify.ui.theme.BACKGROUND_COLOR
import com.example.purrytify.viewmodels.LoginViewModel
import com.example.purrytify.viewmodels.ProfileViewModel
import com.example.purrytify.viewmodels.ViewModelFactory

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current)
    )
)
{
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND_COLOR),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profile Screen",
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
            fontSize = 24.sp,
            )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                profileViewModel.logout()
                (context as? MainActivity)?.recreate()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            ),

        ) {
            Text("Logout")
        }
    }
}