package com.example.purrytify.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.purrytify.models.LocationResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.Locale
import kotlin.coroutines.resume
import androidx.core.net.toUri

/**
 * Helper class untuk mengelola lokasi pengguna
 *
 * Fungsi utama:
 * 1. Mendapatkan lokasi current user menggunakan GPS atau Network
 * 2. Convert koordinat ke country code menggunakan Geocoder
 * 3. Handle Google Maps intent untuk manual location selection
 * 4. Validasi permission dan availability location services
 */
class LocationHelper(private val context: Context) {
    private val TAG = "LocationHelper"
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // FIXED: Gunakan Geocoder dengan Locale Indonesia sebagai fallback
    private val geocoder = if (Geocoder.isPresent()) {
        Geocoder(context, Locale("id", "ID")) // Indonesia locale
    } else {
        null
    }

    /**
     * Fungsi untuk mendapatkan lokasi pengguna saat ini dengan timeout dan retry
     *
     * Alur kerja:
     * 1. Check permission dan location services
     * 2. Coba ambil last known location dulu (lebih cepat)
     * 3. Jika tidak ada, request location update dengan timeout
     * 4. Convert koordinat ke country code menggunakan Geocoder
     * 5. Return LocationResult atau null jika gagal
     *
     * @return LocationResult dengan country code atau null jika gagal
     */
    suspend fun getCurrentLocation(): LocationResult? {
        return try {
            withTimeout(15000L) { // 15 second timeout
                getCurrentLocationInternal()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Timeout or error getting location: ${e.message}")
            null
        }
    }

    private suspend fun getCurrentLocationInternal(): LocationResult? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        if (!isLocationEnabled()) {
            Log.e(TAG, "Location services disabled")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            // FIXED: Try multiple approaches untuk mendapatkan location

            // 1. Coba last known location dulu dari multiple providers
            val lastKnownLocation = getBestLastKnownLocation()
            if (lastKnownLocation != null && isLocationFresh(lastKnownLocation)) {
                Log.d(TAG, "Using fresh last known location: ${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}")
                val result = locationToCountryCode(lastKnownLocation.latitude, lastKnownLocation.longitude)
                continuation.resume(result)
                return@suspendCancellableCoroutine
            }

            // 2. Request fresh location update
            var locationReceived = false
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!locationReceived) {
                        locationReceived = true
                        Log.d(TAG, "Got fresh location: ${location.latitude}, ${location.longitude}")
                        locationManager.removeUpdates(this)

                        val result = locationToCountryCode(location.latitude, location.longitude)
                        continuation.resume(result)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    Log.w(TAG, "Location provider disabled: $provider")
                }
            }

            // Try GPS first, then Network
            val providers = mutableListOf<String>()
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                providers.add(LocationManager.GPS_PROVIDER)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                providers.add(LocationManager.NETWORK_PROVIDER)
            }

            if (providers.isEmpty()) {
                Log.e(TAG, "No location providers available")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            // Request updates from available providers
            providers.forEach { provider ->
                try {
                    locationManager.requestLocationUpdates(
                        provider,
                        1000L, // 1 second
                        0f, // 0 meters
                        locationListener
                    )
                    Log.d(TAG, "Requested location updates from: $provider")
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception for provider $provider: ${e.message}")
                }
            }

            // Set timeout fallback - jika tidak dapat location dalam 10 detik, coba pakai last known
            continuation.invokeOnCancellation {
                try {
                    locationManager.removeUpdates(locationListener)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing location updates: ${e.message}")
                }
            }

            // Fallback timeout - jika masih belum dapat location, pakai last known meskipun lama
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!locationReceived) {
                    locationReceived = true
                    Log.w(TAG, "Location request timeout, trying stale last known location")
                    locationManager.removeUpdates(locationListener)

                    val staleLocation = getBestLastKnownLocation()
                    if (staleLocation != null) {
                        val result = locationToCountryCode(staleLocation.latitude, staleLocation.longitude)
                        continuation.resume(result)
                    } else {
                        continuation.resume(null)
                    }
                }
            }, 10000L) // 10 second fallback

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting location: ${e.message}")
            continuation.resume(null)
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting location: ${e.message}")
            continuation.resume(null)
        }
    }

    /**
     * Fungsi untuk mendapatkan last known location terbaik dari semua provider
     *
     * Alur kerja:
     * 1. Ambil last known location dari GPS dan Network provider
     * 2. Pilih yang paling fresh (timestamp terbaru)
     * 3. Return location terbaik atau null
     */
    private fun getBestLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null

        return try {
            val locations = mutableListOf<Location>()

            // Get from GPS provider
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                    locations.add(it)
                    Log.d(TAG, "GPS last known: ${it.latitude}, ${it.longitude}, age: ${System.currentTimeMillis() - it.time}ms")
                }
            }

            // Get from Network provider
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                    locations.add(it)
                    Log.d(TAG, "Network last known: ${it.latitude}, ${it.longitude}, age: ${System.currentTimeMillis() - it.time}ms")
                }
            }

            // Get from Passive provider
            if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)?.let {
                    locations.add(it)
                    Log.d(TAG, "Passive last known: ${it.latitude}, ${it.longitude}, age: ${System.currentTimeMillis() - it.time}ms")
                }
            }

            // Return most recent location
            locations.maxByOrNull { it.time }?.also {
                Log.d(TAG, "Best last known location: ${it.latitude}, ${it.longitude} from ${it.provider}")
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting last known location: ${e.message}")
            null
        }
    }

    /**
     * Check apakah location masih fresh (tidak lebih dari 5 menit)
     */
    private fun isLocationFresh(location: Location): Boolean {
        val age = System.currentTimeMillis() - location.time
        val isFresh = age < 5 * 60 * 1000L // 5 minutes
        Log.d(TAG, "Location age: ${age / 1000}s, fresh: $isFresh")
        return isFresh
    }

    /**
     * Fungsi untuk konversi koordinat ke country code menggunakan Geocoder
     *
     * FIXED: Multiple fallback approaches dan better error handling
     *
     * Alur kerja:
     * 1. Gunakan Geocoder untuk reverse geocoding
     * 2. Jika gagal, coba dengan Locale default
     * 3. Jika masih gagal, coba dengan multiple Locale
     * 4. Return LocationResult dengan country code atau null
     */
    private fun locationToCountryCode(latitude: Double, longitude: Double): LocationResult? {
        if (geocoder == null) {
            Log.e(TAG, "Geocoder not available on this device")
            return null
        }

        return try {
            Log.d(TAG, "Converting coordinates to country: $latitude, $longitude")

            // Try multiple geocoding approaches
            var addresses: List<Address>? = null
            var lastException: Exception? = null

            // 1. Try with Indonesia locale first
            try {
                addresses = geocoder.getFromLocation(latitude, longitude, 1)
                Log.d(TAG, "Geocoder with ID locale returned ${addresses?.size ?: 0} results")
            } catch (e: Exception) {
                Log.w(TAG, "Geocoder with ID locale failed: ${e.message}")
                lastException = e
            }

            // 2. If failed, try with default locale
            if (addresses.isNullOrEmpty()) {
                try {
                    val defaultGeocoder = Geocoder(context, Locale.getDefault())
                    addresses = defaultGeocoder.getFromLocation(latitude, longitude, 1)
                    Log.d(TAG, "Geocoder with default locale returned ${addresses?.size ?: 0} results")
                } catch (e: Exception) {
                    Log.w(TAG, "Geocoder with default locale failed: ${e.message}")
                    lastException = e
                }
            }

            // 3. If still failed, try with English locale
            if (addresses.isNullOrEmpty()) {
                try {
                    val englishGeocoder = Geocoder(context, Locale.ENGLISH)
                    addresses = englishGeocoder.getFromLocation(latitude, longitude, 1)
                    Log.d(TAG, "Geocoder with English locale returned ${addresses?.size ?: 0} results")
                } catch (e: Exception) {
                    Log.w(TAG, "Geocoder with English locale failed: ${e.message}")
                    lastException = e
                }
            }

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val countryCode = address.countryCode ?: "ID" // Default to Indonesia if null
                val countryName = address.countryName ?: CountryCodeHelper.getCountryName(countryCode)
                val fullAddress = address.getAddressLine(0)

                Log.d(TAG, "Location resolved to country: $countryName ($countryCode)")
                Log.d(TAG, "Full address: $fullAddress")

                LocationResult(
                    countryCode = countryCode,
                    countryName = countryName,
                    address = fullAddress,
                    latitude = latitude,
                    longitude = longitude
                )
            } else {
                Log.w(TAG, "No address found for coordinates after all attempts")
                // FIXED: Return Indonesia as default if we can't geocode but coordinates suggest Indonesia
                if (isCoordinatesInIndonesia(latitude, longitude)) {
                    Log.d(TAG, "Coordinates appear to be in Indonesia, using as fallback")
                    LocationResult(
                        countryCode = "ID",
                        countryName = "Indonesia",
                        address = "Location in Indonesia",
                        latitude = latitude,
                        longitude = longitude
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting location to country code: ${e.message}")
            // FIXED: Fallback untuk koordinat Indonesia
            if (isCoordinatesInIndonesia(latitude, longitude)) {
                Log.d(TAG, "Using Indonesia fallback for coordinates")
                LocationResult(
                    countryCode = "ID",
                    countryName = "Indonesia",
                    address = "Location in Indonesia",
                    latitude = latitude,
                    longitude = longitude
                )
            } else {
                null
            }
        }
    }

    /**
     * Check apakah koordinat berada di wilayah Indonesia
     * Rough bounds untuk Indonesia: lat -11 to 6, lng 95 to 141
     */
    private fun isCoordinatesInIndonesia(latitude: Double, longitude: Double): Boolean {
        return latitude >= -11.0 && latitude <= 6.0 && longitude >= 95.0 && longitude <= 141.0
    }

    /**
     * FIXED: Membuat intent Google Maps untuk location picking yang benar
     *
     * Alur kerja:
     * 1. Buat URI untuk place picker atau search
     * 2. Set package ke Google Maps
     * 3. Return intent yang bisa di-handle untuk result
     */
    fun createGoogleMapsIntent(currentLatitude: Double? = null, currentLongitude: Double? = null): Intent {
        // FIXED: Use proper place picker atau geo URI untuk location selection
        val uri = if (currentLatitude != null && currentLongitude != null) {
            // Open maps centered on current location for place selection
            "geo:$currentLatitude,$currentLongitude?q=$currentLatitude,$currentLongitude(Current Location)".toUri()
        } else {
            // Open maps for general place search
            "geo:0,0?q=".toUri()
        }

        val intent = Intent(Intent.ACTION_VIEW, uri)

        // Try Google Maps first
        if (isGoogleMapsAvailable()) {
            intent.setPackage("com.google.android.apps.maps")
        }

        Log.d(TAG, "Created Google Maps intent with URI: $uri")
        return intent
    }

    /**
     * Alternative: Create intent untuk place picker dengan koordinat
     */
    fun createPlacePickerIntent(): Intent {
        // FIXED: Create simple place search intent
        val uri = "geo:0,0?q=pick location".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)

        if (isGoogleMapsAvailable()) {
            intent.setPackage("com.google.android.apps.maps")
        }

        return intent
    }

    /**
     * FIXED: Parse hasil dari Google Maps dengan multiple format support
     *
     * Alur kerja:
     * 1. Parse URI dari intent result
     * 2. Extract koordinat dari berbagai format URI
     * 3. Convert koordinat ke country code
     * 4. Return LocationResult
     */
    fun parseGoogleMapsResult(data: Intent?): LocationResult? {
        return try {
            val uri = data?.data
            if (uri == null) {
                Log.w(TAG, "No URI data from Google Maps result")
                return null
            }

            Log.d(TAG, "Parsing Google Maps result URI: $uri")

            val coordinates = uri.toString()
            var latitude: Double? = null
            var longitude: Double? = null

            // Try different URI patterns

            // Pattern 1: geo:lat,lng
            val geoPattern = Regex("geo:(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
            val geoMatch = geoPattern.find(coordinates)
            if (geoMatch != null) {
                latitude = geoMatch.groupValues[1].toDoubleOrNull()
                longitude = geoMatch.groupValues[2].toDoubleOrNull()
                Log.d(TAG, "Matched geo pattern: $latitude, $longitude")
            }

            // Pattern 2: q=lat,lng
            if (latitude == null || longitude == null) {
                val qPattern = Regex("q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
                val qMatch = qPattern.find(coordinates)
                if (qMatch != null) {
                    latitude = qMatch.groupValues[1].toDoubleOrNull()
                    longitude = qMatch.groupValues[2].toDoubleOrNull()
                    Log.d(TAG, "Matched q pattern: $latitude, $longitude")
                }
            }

            // Pattern 3: ll=lat,lng
            if (latitude == null || longitude == null) {
                val llPattern = Regex("ll=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
                val llMatch = llPattern.find(coordinates)
                if (llMatch != null) {
                    latitude = llMatch.groupValues[1].toDoubleOrNull()
                    longitude = llMatch.groupValues[2].toDoubleOrNull()
                    Log.d(TAG, "Matched ll pattern: $latitude, $longitude")
                }
            }

            if (latitude != null && longitude != null) {
                Log.d(TAG, "Successfully parsed coordinates: $latitude, $longitude")
                locationToCountryCode(latitude, longitude)
            } else {
                Log.w(TAG, "Could not parse coordinates from Maps result: $coordinates")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Google Maps result: ${e.message}")
            null
        }
    }

    /**
     * Check apakah Google Maps tersedia
     */
    private fun isGoogleMapsAvailable(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, "geo:0,0".toUri())
            intent.setPackage("com.google.android.apps.maps")
            intent.resolveActivity(context.packageManager) != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check apakah app memiliki permission lokasi
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check apakah location services enabled
     */
    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val GOOGLE_MAPS_REQUEST_CODE = 1002
    }
}