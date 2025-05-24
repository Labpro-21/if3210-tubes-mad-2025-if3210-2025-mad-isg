package com.example.purrytify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.purrytify.R
import com.example.purrytify.ui.theme.GREEN_COLOR
import com.example.purrytify.util.CountryCodeHelper

/**
 * Dialog untuk memilih metode location selection
 *
 * Fungsi:
 * 1. Memberikan pilihan antara Google Maps atau country list
 * 2. Handle case ketika Google Maps tidak tersedia
 * 3. Provide clear UI untuk user selection
 *
 * Alur kerja:
 * 1. User tap "Select on Map" button
 * 2. Dialog muncul dengan 2 opsi
 * 3. User pilih Google Maps → Launch Maps intent
 * 4. User pilih Country List → Show country selector
 * 5. Dialog dismiss setelah selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionDialog(
    onGoogleMapsClick: () -> Unit,
    onCountryListClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Title
                Text(
                    text = "Select Location Method",
                    color = Color.White,
                    fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Choose how you want to select your location:",
                    color = Color.Gray,
                    fontFamily = FontFamily(Font(R.font.poppins_regular)),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Google Maps option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onGoogleMapsClick()
                            onDismiss()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2A2A2A)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Maps",
                            tint = GREEN_COLOR,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Use Google Maps",
                                color = Color.White,
                                fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Select precise location on map",
                                color = Color.Gray,
                                fontFamily = FontFamily(Font(R.font.poppins_regular)),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Country list option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onCountryListClick()
                            onDismiss()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2A2A2A)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "List",
                            tint = GREEN_COLOR,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Choose from List",
                                color = Color.White,
                                fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Select from country list",
                                color = Color.Gray,
                                fontFamily = FontFamily(Font(R.font.poppins_regular)),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cancel button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Cancel",
                            color = Color.Gray,
                            fontFamily = FontFamily(Font(R.font.poppins_regular))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog untuk memilih country dari list dengan search functionality
 *
 * Fungsi:
 * 1. Menampilkan daftar negara yang didukung dan tambahan
 * 2. Provide search functionality untuk mudah find country
 * 3. Allow selection dan return country code & name
 * 4. Handle both supported dan additional countries
 *
 * Alur kerja:
 * 1. Dialog show dengan search bar dan country list
 * 2. User dapat search country by name atau code
 * 3. List filtered berdasarkan search query
 * 4. User select country → return country info
 * 5. Dialog dismiss dengan selected country
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectorDialog(
    onCountrySelected: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Combine supported dan additional countries
    val allCountries = remember {
        val supported = CountryCodeHelper.getSupportedCountriesList()
        val additional = listOf(
            "SG" to "Singapore",
            "TH" to "Thailand",
            "VN" to "Vietnam",
            "PH" to "Philippines",
            "JP" to "Japan",
            "KR" to "South Korea",
            "CN" to "China",
            "IN" to "India",
            "AU" to "Australia",
            "NZ" to "New Zealand",
            "CA" to "Canada",
            "FR" to "France",
            "IT" to "Italy",
            "ES" to "Spain",
            "NL" to "Netherlands",
            "RU" to "Russia",
            "UA" to "Ukraine",
            "PL" to "Poland",
            "TR" to "Turkey",
            "EG" to "Egypt",
            "ZA" to "South Africa",
            "NG" to "Nigeria",
            "KE" to "Kenya",
            "AR" to "Argentina",
            "CL" to "Chile",
            "CO" to "Colombia",
            "MX" to "Mexico",
            "PE" to "Peru"
        )

        // Combine dan sort by country name
        (supported + additional).sortedBy { it.second }
    }

    // Filter countries berdasarkan search query
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            allCountries
        } else {
            allCountries.filter { (code, name) ->
                name.contains(searchQuery, ignoreCase = true) ||
                        code.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Title
                Text(
                    text = "Select Country",
                    color = Color.White,
                    fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = {
                        Text(
                            "Search country...",
                            color = Color.Gray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.Gray
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GREEN_COLOR,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = GREEN_COLOR
                    ),
                    singleLine = true
                )

                // Countries list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Show supported countries first if no search
                    if (searchQuery.isBlank()) {
                        item {
                            Text(
                                text = "Supported Countries",
                                color = GREEN_COLOR,
                                fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(CountryCodeHelper.getSupportedCountriesList()) { (code, name) ->
                            CountryItem(
                                countryCode = code,
                                countryName = name,
                                isSupported = true,
                                onClick = { onCountrySelected(code, name) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color.Gray)
                            Text(
                                text = "Other Countries",
                                color = Color.Gray,
                                fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(allCountries.filter { (code, _) ->
                            !CountryCodeHelper.getSupportedCountriesList().map { it.first }.contains(code)
                        }) { (code, name) ->
                            CountryItem(
                                countryCode = code,
                                countryName = name,
                                isSupported = false,
                                onClick = { onCountrySelected(code, name) }
                            )
                        }
                    } else {
                        // Show filtered results
                        if (filteredCountries.isEmpty()) {
                            item {
                                Text(
                                    text = "No countries found",
                                    color = Color.Gray,
                                    fontFamily = FontFamily(Font(R.font.poppins_regular)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            items(filteredCountries) { (code, name) ->
                                val isSupported = CountryCodeHelper.getSupportedCountriesList()
                                    .map { it.first }.contains(code)
                                CountryItem(
                                    countryCode = code,
                                    countryName = name,
                                    isSupported = isSupported,
                                    onClick = { onCountrySelected(code, name) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Cancel",
                            color = Color.Gray,
                            fontFamily = FontFamily(Font(R.font.poppins_regular))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual country item component
 *
 * Fungsi:
 * 1. Display country info dengan proper styling
 * 2. Show badge untuk supported countries
 * 3. Handle click events
 */
@Composable
private fun CountryItem(
    countryCode: String,
    countryName: String,
    isSupported: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSupported) Color(0xFF2A3A2A) else Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = countryName,
                        color = Color.White,
                        fontFamily = FontFamily(Font(R.font.poppins_regular)),
                        fontSize = 16.sp
                    )

                    if (isSupported) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = GREEN_COLOR.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Supported",
                                color = GREEN_COLOR,
                                fontFamily = FontFamily(Font(R.font.poppins_regular)),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = countryCode,
                    color = Color.Gray,
                    fontFamily = FontFamily(Font(R.font.poppins_regular)),
                    fontSize = 12.sp
                )
            }
        }
    }
}