package com.example.purrytify.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.models.LocationResult
import com.example.purrytify.models.UserProfile
import com.example.purrytify.repository.UserRepository
import com.example.purrytify.util.LocationHelper
import com.example.purrytify.util.PhotoHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel untuk mengelola edit profile dengan enhanced location handling
 *
 * Fungsi utama:
 * 1. Mengelola state UI untuk edit profile
 * 2. Handle location selection dengan multiple fallback methods
 * 3. Handle photo selection dari camera dan gallery
 * 4. Kirim perubahan ke server dengan proper validation
 * 5. Enhanced error handling dan user feedback
 */
class EditProfileViewModel(
    application: Application,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    private val TAG = "EditProfileViewModel"
    private val context = getApplication<Application>().applicationContext

    // Helper classes
    val locationHelper = LocationHelper(context)
    val photoHelper = PhotoHelper(context)

    // Current profile data
    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    val currentProfile: StateFlow<UserProfile?> = _currentProfile.asStateFlow()

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Location state dengan enhanced handling
    private val _selectedLocation = MutableStateFlow<LocationResult?>(null)
    val selectedLocation: StateFlow<LocationResult?> = _selectedLocation.asStateFlow()

    private val _isLoadingLocation = MutableStateFlow(false)
    val isLoadingLocation: StateFlow<Boolean> = _isLoadingLocation.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    // Photo state
    private val _selectedPhotoUri = MutableStateFlow<Uri?>(null)
    val selectedPhotoUri: StateFlow<Uri?> = _selectedPhotoUri.asStateFlow()

    // Permission state
    private val _needLocationPermission = MutableStateFlow(false)
    val needLocationPermission: StateFlow<Boolean> = _needLocationPermission.asStateFlow()

    private val _needCameraPermission = MutableStateFlow(false)
    val needCameraPermission: StateFlow<Boolean> = _needCameraPermission.asStateFlow()

    /**
     * Fungsi untuk load current profile data
     *
     * Alur kerja:
     * 1. Set loading state
     * 2. Call repository untuk get profile
     * 3. Update current profile state
     * 4. Handle error jika ada
     */
    fun loadCurrentProfile() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val result = userRepository.getUserProfile()
                result.onSuccess { profile ->
                    _currentProfile.value = profile
                    Log.d(TAG, "Profile loaded: ${profile.username}, location: ${profile.location}")
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                    Log.e(TAG, "Failed to load profile: ${exception.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load profile: ${e.message}"
                Log.e(TAG, "Exception loading profile: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ENHANCED: Fungsi untuk mendapatkan lokasi current dengan better error handling
     *
     * Alur kerja:
     * 1. Check permissions dan location services
     * 2. Set loading state dan clear previous errors
     * 3. Request location dengan timeout dan retry
     * 4. Handle berbagai error scenarios
     * 5. Update UI state dengan hasil atau error
     */
    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                // Check permissions first
                if (!locationHelper.hasLocationPermission()) {
                    _needLocationPermission.value = true
                    return@launch
                }

                if (!locationHelper.isLocationEnabled()) {
                    _locationError.value = "Location services are disabled. Please enable GPS or Network location in your device settings."
                    return@launch
                }

                _isLoadingLocation.value = true
                _locationError.value = null
                _errorMessage.value = null

                Log.d(TAG, "Starting location detection...")

                // Request current location dengan enhanced error handling
                val locationResult = locationHelper.getCurrentLocation()

                if (locationResult != null) {
                    _selectedLocation.value = locationResult
                    _successMessage.value = "Location detected: ${locationResult.countryName} (${locationResult.countryCode})"
                    Log.d(TAG, "Location obtained successfully: ${locationResult.countryCode}")

                    // Log detailed location info untuk debugging
                    Log.d(TAG, "Location details - Country: ${locationResult.countryName}, Code: ${locationResult.countryCode}")
                    Log.d(TAG, "Coordinates: ${locationResult.latitude}, ${locationResult.longitude}")
                    Log.d(TAG, "Address: ${locationResult.address}")

                } else {
                    _locationError.value = "Unable to detect your current location. This might be due to:\n" +
                            "• Weak GPS signal (try moving to an open area)\n" +
                            "• Network connectivity issues\n" +
                            "• Location services restrictions\n\n" +
                            "Please try again or select your country manually."
                    Log.w(TAG, "Failed to get current location")
                }

            } catch (e: Exception) {
                val errorMsg = "Error detecting location: ${e.message}"
                _locationError.value = errorMsg
                _errorMessage.value = errorMsg
                Log.e(TAG, "Exception getting location: ${e.message}", e)
            } finally {
                _isLoadingLocation.value = false
            }
        }
    }

    /**
     * ENHANCED: Handle hasil dari manual location selection
     *
     * @param locationResult Hasil dari Google Maps atau country selector
     */
    fun setManualLocation(locationResult: LocationResult) {
        _selectedLocation.value = locationResult
        _locationError.value = null

        val message = if (locationResult.latitude != null && locationResult.longitude != null) {
            "Location selected: ${locationResult.countryName} (precise location)"
        } else {
            "Country selected: ${locationResult.countryName}"
        }

        _successMessage.value = message
        Log.d(TAG, "Manual location set: ${locationResult.countryCode} - ${locationResult.countryName}")
    }

    /**
     * Fungsi untuk clear selected location
     */
    fun clearSelectedLocation() {
        _selectedLocation.value = null
        _locationError.value = null
        Log.d(TAG, "Selected location cleared")
    }

    /**
     * ENHANCED: Handle photo dari camera dengan better debugging
     */
    fun setPhotoFromCamera() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== CAMERA PHOTO PROCESSING START ===")

                // Ambil URI dari PhotoHelper
                val photoUri = photoHelper.getCameraPhotoUri()

                Log.d(TAG, "Retrieved camera photo URI: $photoUri")

                if (photoUri == null) {
                    val errorMsg = "Failed to capture photo - no photo URI available"
                    Log.e(TAG, errorMsg)
                    _errorMessage.value = errorMsg
                    return@launch
                }

                Log.d(TAG, "Validating photo URI...")
                if (!photoHelper.isValidPhotoUri(photoUri)) {
                    val errorMsg = "Invalid photo file or file doesn't exist"
                    Log.e(TAG, errorMsg)
                    _errorMessage.value = errorMsg
                    return@launch
                }

                Log.d(TAG, "Getting file size...")
                val fileSize = photoHelper.getFileSize(photoUri)
                Log.d(TAG, "Photo file size: $fileSize bytes")

                if (fileSize <= 0) {
                    val errorMsg = "Photo file is empty or corrupted (size: $fileSize)"
                    Log.e(TAG, errorMsg)
                    _errorMessage.value = errorMsg
                    return@launch
                }

                if (fileSize > PhotoHelper.MAX_PHOTO_SIZE_BYTES) {
                    val errorMsg = "Photo file too large (${fileSize / 1024 / 1024}MB, max 5MB)"
                    Log.e(TAG, errorMsg)
                    _errorMessage.value = errorMsg
                    return@launch
                }

                Log.d(TAG, "Photo validation successful, setting selected photo URI")
                _selectedPhotoUri.value = photoUri
                _successMessage.value = "Photo captured successfully"

                Log.d(TAG, "=== CAMERA PHOTO PROCESSING SUCCESS ===")
                Log.d(TAG, "Final URI: $photoUri, Size: $fileSize bytes")

            } catch (e: Exception) {
                val errorMsg = "Error processing camera photo: ${e.message}"
                Log.e(TAG, "=== CAMERA PHOTO PROCESSING ERROR ===")
                Log.e(TAG, errorMsg, e)
                _errorMessage.value = errorMsg
            }
        }
    }

    /**
     * Handle photo dari gallery
     */
    fun setPhotoFromGallery(photoUri: Uri?) {
        viewModelScope.launch {
            try {
                if (photoUri == null) {
                    _errorMessage.value = "No photo selected"
                    return@launch
                }

                if (!photoHelper.isValidPhotoUri(photoUri)) {
                    _errorMessage.value = "Invalid photo file"
                    return@launch
                }

                val fileSize = photoHelper.getFileSize(photoUri)
                if (fileSize > PhotoHelper.MAX_PHOTO_SIZE_BYTES) {
                    _errorMessage.value = "Photo file too large (max 5MB)"
                    return@launch
                }

                _selectedPhotoUri.value = photoUri
                _successMessage.value = "Photo selected successfully"
                Log.d(TAG, "Photo from gallery set: $photoUri")

            } catch (e: Exception) {
                _errorMessage.value = "Error processing photo: ${e.message}"
                Log.e(TAG, "Exception processing gallery photo: ${e.message}")
            }
        }
    }

    /**
     * Clear selected photo
     */
    fun clearSelectedPhoto() {
        _selectedPhotoUri.value = null
        photoHelper.clearCameraPhotoUri()
        Log.d(TAG, "Selected photo cleared")
    }

    /**
     * ENHANCED: Save profile changes dengan better validation dan feedback
     */
    fun saveProfileChanges() {
        viewModelScope.launch {
            try {
                val locationCode = _selectedLocation.value?.countryCode
                val photoUri = _selectedPhotoUri.value

                // Validate ada perubahan
                if (locationCode == null && photoUri == null) {
                    _errorMessage.value = "No changes to save"
                    return@launch
                }

                _isLoading.value = true
                _errorMessage.value = null

                Log.d(TAG, "Saving profile changes:")
                Log.d(TAG, "- Location: $locationCode (${_selectedLocation.value?.countryName})")
                Log.d(TAG, "- Photo URI: $photoUri")

                // Call repository untuk edit profile
                val result = userRepository.editProfile(
                    context = context,
                    location = locationCode,
                    profilePhotoUri = photoUri?.toString()
                )

                result.onSuccess { response ->
                    // Update current profile dengan data terbaru
                    response.updatedProfile?.let { updatedProfile ->
                        _currentProfile.value = updatedProfile
                        Log.d(TAG, "Profile updated - new location: ${updatedProfile.location}")
                    }

                    // Clear selected changes
                    _selectedLocation.value = null
                    _selectedPhotoUri.value = null
                    _locationError.value = null
                    photoHelper.clearCameraPhotoUri()

                    _successMessage.value = "Profile updated successfully!"
                    Log.d(TAG, "Profile update completed successfully")

                }.onFailure { exception ->
                    _errorMessage.value = "Failed to update profile: ${exception.message}"
                    Log.e(TAG, "Failed to update profile: ${exception.message}")
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error saving profile: ${e.message}"
                Log.e(TAG, "Exception saving profile: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check apakah ada perubahan yang belum disave
     */
    fun hasUnsavedChanges(): Boolean {
        return _selectedLocation.value != null || _selectedPhotoUri.value != null
    }

    /**
     * Clear error messages
     */
    fun clearError() {
        _errorMessage.value = null
        _locationError.value = null
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _successMessage.value = null
    }

    /**
     * Handle location permission granted
     */
    fun onLocationPermissionGranted() {
        _needLocationPermission.value = false
        _locationError.value = null
        getCurrentLocation()
    }

    /**
     * Handle location permission denied
     */
    fun onLocationPermissionDenied() {
        _needLocationPermission.value = false
        _locationError.value = "Location permission is required to automatically detect your current location. You can still select your country manually."
        _errorMessage.value = "Location permission denied. Please select your country manually or grant location permission in app settings."
    }

    /**
     * Handle camera permission granted
     */
    fun onCameraPermissionGranted() {
        _needCameraPermission.value = false
    }

    /**
     * Handle camera permission denied
     */
    fun onCameraPermissionDenied() {
        _needCameraPermission.value = false
        _errorMessage.value = "Camera permission is required to take photos. You can still select photos from gallery."
    }

    /**
     * Request camera permission
     */
    fun requestCameraPermission() {
        if (!photoHelper.hasCameraPermission()) {
            _needCameraPermission.value = true
        }
    }

    /**
     * Debug function
     */
    fun debugLocationState() {
        Log.d(TAG, "=== LOCATION STATE DEBUG ===")
        Log.d(TAG, "Has location permission: ${locationHelper.hasLocationPermission()}")
        Log.d(TAG, "Location services enabled: ${locationHelper.isLocationEnabled()}")
        Log.d(TAG, "Selected location: ${_selectedLocation.value}")
        Log.d(TAG, "Location error: ${_locationError.value}")
        Log.d(TAG, "Loading location: ${_isLoadingLocation.value}")
        Log.d(TAG, "========================")
    }
}