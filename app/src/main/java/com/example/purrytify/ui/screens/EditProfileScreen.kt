package com.example.purrytify.ui.screens

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.R
import com.example.purrytify.models.LocationResult
import com.example.purrytify.repository.UserRepository
import com.example.purrytify.ui.components.CountrySelectorDialog
import com.example.purrytify.ui.components.LocationSelectionDialog
import com.example.purrytify.ui.theme.BACKGROUND_COLOR
import com.example.purrytify.ui.theme.GREEN_COLOR
import com.example.purrytify.util.TokenManager
import com.example.purrytify.viewmodels.EditProfileViewModel
import kotlinx.coroutines.launch

/**
 * Screen untuk edit profile pengguna dengan enhanced location selection
 *
 * Fungsi utama:
 * 1. Display current profile information
 * 2. Allow photo selection dari camera atau gallery
 * 3. Allow location selection dengan auto detect atau manual
 * 4. Save changes ke server dengan proper validation
 * 5. Handle permissions dan error states
 * 6. Provide multiple fallback options untuk location selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokenManager = TokenManager(context)
    val userRepository = UserRepository(tokenManager)

    // ViewModel dengan dependency injection manual
    val viewModel: EditProfileViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return EditProfileViewModel(
                    context.applicationContext as android.app.Application,
                    userRepository
                ) as T
            }
        }
    )

    // State dari ViewModel
    val currentProfile by viewModel.currentProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val selectedPhotoUri by viewModel.selectedPhotoUri.collectAsState()
    val isLoadingLocation by viewModel.isLoadingLocation.collectAsState()
    val needLocationPermission by viewModel.needLocationPermission.collectAsState()
    val needCameraPermission by viewModel.needCameraPermission.collectAsState()

    // Dialog states
    var showLocationSelectionDialog by remember { mutableStateOf(false) }
    var showCountrySelectorDialog by remember { mutableStateOf(false) }

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Activity result launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("EditProfileScreen", "Camera result received, processing photo")
            viewModel.setPhotoFromCamera()
        } else {
            Log.w("EditProfileScreen", "Camera result cancelled or failed")
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photoUri = result.data?.data
            Log.d("EditProfileScreen", "Gallery result received: $photoUri")
            viewModel.setPhotoFromGallery(photoUri)
        } else {
            Log.w("EditProfileScreen", "Gallery result cancelled")
        }
    }

    val googleMapsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("EditProfileScreen", "Google Maps result: ${result.resultCode}")
        Log.d("EditProfileScreen", "Result data: ${result.data}")
        Log.d("EditProfileScreen", "Result data URI: ${result.data?.data}")

        if (result.resultCode == Activity.RESULT_OK) {
            val locationResult = viewModel.locationHelper.parseGoogleMapsResult(result.data)
            if (locationResult != null) {
                Log.d("EditProfileScreen", "Successfully parsed location: ${locationResult.countryName}")
                viewModel.setManualLocation(locationResult)
            } else {
                Log.w("EditProfileScreen", "Failed to parse Google Maps result, showing fallback")
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Could not get location from Maps. Please try selecting your country from the list.",
                        duration = SnackbarDuration.Long
                    )
                }
                showCountrySelectorDialog = true
            }
        } else {
            Log.w("EditProfileScreen", "Google Maps cancelled or failed")
        }
    }


    // Permission launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.onLocationPermissionGranted()
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onCameraPermissionGranted()
        } else {
            viewModel.onCameraPermissionDenied()
        }
    }

    // Handle permission requests
    LaunchedEffect(needLocationPermission) {
        if (needLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(needCameraPermission) {
        if (needCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Show messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccess()
        }
    }

    // Load profile on first load
    LaunchedEffect(Unit) {
        viewModel.loadCurrentProfile()
    }

    // Show dialogs
    if (showLocationSelectionDialog) {
        LocationSelectionDialog(
            onGoogleMapsClick = {
                val mapsIntent = viewModel.locationHelper.createGoogleMapsIntent()
                if (mapsIntent.resolveActivity(context.packageManager) != null) {
                    googleMapsLauncher.launch(mapsIntent)
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Google Maps is not installed",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            onCountryListClick = {
                showCountrySelectorDialog = true
            },
            onDismiss = { showLocationSelectionDialog = false }
        )
    }

    if (showCountrySelectorDialog) {
        CountrySelectorDialog(
            onCountrySelected = { countryCode, countryName ->
                val locationResult = LocationResult(
                    countryCode = countryCode,
                    countryName = countryName,
                    address = countryName,
                    latitude = null,
                    longitude = null
                )
                viewModel.setManualLocation(locationResult)
                showCountrySelectorDialog = false
            },
            onDismiss = { showCountrySelectorDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Profile",
                        fontFamily = FontFamily(Font(R.font.poppins_semi_bold))
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.hasUnsavedChanges()) {
                            // TODO: Show confirmation dialog
                        }
                        navController.navigateUp()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BACKGROUND_COLOR,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BACKGROUND_COLOR
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading && currentProfile == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GREEN_COLOR)
                }
            } else {
                // Profile Photo Section
                ProfilePhotoSection(
                    currentProfile = currentProfile,
                    selectedPhotoUri = selectedPhotoUri,
                    onCameraClick = {
                        if (viewModel.photoHelper.hasCameraPermission()) {
                            val cameraIntent = viewModel.photoHelper.createCameraIntent()
                            cameraIntent?.let { (intent, _) ->
                                cameraLauncher.launch(intent)
                            }
                        } else {
                            viewModel.requestCameraPermission()
                        }
                    },
                    onGalleryClick = {
                        val galleryIntent = viewModel.photoHelper.createGalleryIntent()
                        galleryLauncher.launch(galleryIntent)
                    },
                    onClearPhoto = { viewModel.clearSelectedPhoto() }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Location Section
                LocationSection(
                    currentProfile = currentProfile,
                    selectedLocation = selectedLocation,
                    isLoadingLocation = isLoadingLocation,
                    onAutoDetectClick = { viewModel.getCurrentLocation() },
                    onManualSelectClick = {
                        // Show location selection dialog instead of direct Google Maps
                        showLocationSelectionDialog = true
                    },
                    onClearLocation = { viewModel.clearSelectedLocation() }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Save Button
                Button(
                    onClick = { viewModel.saveProfileChanges() },
                    enabled = !isLoading && viewModel.hasUnsavedChanges(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GREEN_COLOR,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Save Changes",
                            fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
                            fontSize = 16.sp
                        )
                    }
                }

                if (!viewModel.hasUnsavedChanges()) {
                    Text(
                        text = "No changes to save",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Komponen untuk section profile photo
 *
 * Fungsi:
 * 1. Display current atau selected profile photo
 * 2. Provide buttons untuk camera, gallery, dan clear
 * 3. Handle loading states dan errors
 */
@Composable
fun ProfilePhotoSection(
    currentProfile: com.example.purrytify.models.UserProfile?,
    selectedPhotoUri: Uri?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onClearPhoto: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profile Photo",
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Current/Selected Photo
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.Gray)
                .border(2.dp, GREEN_COLOR, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selectedPhotoUri != null) {
                // Show selected photo
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(selectedPhotoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Selected Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (currentProfile != null) {
                // Show current profile photo
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("http://34.101.226.132:3000/uploads/profile-picture/${currentProfile.profilePhoto}")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Current Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.ic_person_placeholder),
                    fallback = painterResource(id = R.drawable.ic_person_placeholder)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default Profile",
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Photo action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Camera button
            OutlinedButton(
                onClick = onCameraClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Camera")
            }

            // Gallery button
            OutlinedButton(
                onClick = onGalleryClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gallery")
            }

            // Clear button (only show if photo selected)
            if (selectedPhotoUri != null) {
                TextButton(
                    onClick = onClearPhoto,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("Clear")
                }
            }
        }
    }
}

/**
 * Komponen untuk section location dengan enhanced UI
 *
 * Fungsi:
 * 1. Display current dan selected location
 * 2. Provide buttons untuk auto detect dan manual select
 * 3. Handle loading states dan show clear option
 */
@Composable
fun LocationSection(
    currentProfile: com.example.purrytify.models.UserProfile?,
    selectedLocation: com.example.purrytify.models.LocationResult?,
    isLoadingLocation: Boolean,
    onAutoDetectClick: () -> Unit,
    onManualSelectClick: () -> Unit,
    onClearLocation: () -> Unit
) {
    Column {
        Text(
            text = "Location",
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Current location display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Current Location",
                    color = Color.Gray,
                    fontFamily = FontFamily(Font(R.font.poppins_regular)),
                    fontSize = 14.sp
                )
                Text(
                    text = if (currentProfile?.location != null) {
                        "${com.example.purrytify.util.CountryCodeHelper.getCountryName(currentProfile.location)} (${currentProfile.location})"
                    } else {
                        "Not set"
                    },
                    color = Color.White,
                    fontFamily = FontFamily(Font(R.font.poppins_regular)),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                if (selectedLocation != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "New Location",
                        color = GREEN_COLOR,
                        fontFamily = FontFamily(Font(R.font.poppins_regular)),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${selectedLocation.countryName} (${selectedLocation.countryCode})",
                        color = Color.White,
                        fontFamily = FontFamily(Font(R.font.poppins_regular)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    selectedLocation.address?.let { address ->
                        Text(
                            text = address,
                            color = Color.Gray,
                            fontFamily = FontFamily(Font(R.font.poppins_regular)),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Location action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Auto detect button
            OutlinedButton(
                onClick = onAutoDetectClick,
                enabled = !isLoadingLocation,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                ),
                modifier = Modifier.weight(1f)
            ) {
                if (isLoadingLocation) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Auto Detect",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Auto Detect")
            }

            // Manual select button
            OutlinedButton(
                onClick = onManualSelectClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Select Manually",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Select Manually")
            }
        }

        // Clear location button (only show if location selected)
        if (selectedLocation != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onClearLocation,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Red
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Selected Location")
            }
        }
    }
}