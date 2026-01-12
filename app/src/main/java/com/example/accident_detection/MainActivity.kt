package com.example.accident_detection

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.accident_detection.api.AccidentRequestBody
import com.example.accident_detection.api.NetworkModule
import com.example.accident_detection.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// --- Data class to hold the UI state ---
data class UiState(
    val isLoading: Boolean = false,
    val userName: String? = null,
    val hasContacts: Boolean = false
)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth by lazy { Firebase.auth }
    private val db by lazy { Firebase.firestore }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null // Still useful for background updates

    private val _uiState = MutableStateFlow(UiState())
    private val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val contactActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d(TAG, "Returned from EmergencyContactActivity. Refreshing UI state.")
            checkContactStatus()
        }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                Log.d(TAG, "Fine location permission granted")
                startLocationUpdates() // Start background updates
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Log.d(TAG, "Coarse location permission granted")
                startLocationUpdates() // Start background updates
            }
            else -> {
                Toast.makeText(this, "Location permission is required for SOS.", Toast.LENGTH_LONG).show()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupClickListeners()
        observeUiState()
        checkLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        checkContactStatus() // Check contacts and user name every time the screen is shown
    }

    private fun setupClickListeners() {
        binding.ivSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnAddContact.setOnClickListener {
            val intent = Intent(this, EmergencyContactActivity::class.java)
            contactActivityResultLauncher.launch(intent)
        }
        binding.btnSos.setOnClickListener {
            lifecycleScope.launch {
                handleSosClick()
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                uiState.collect { state ->
                    // Update UI based on the state
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.btnSos.isEnabled = !state.isLoading

                    binding.tvUserName.text = if(state.userName != null) "Welcome, ${state.userName}" else "Welcome"

                    binding.layoutNoContacts.visibility = if (state.hasContacts) View.GONE else View.VISIBLE
                    binding.btnSos.visibility = if (state.hasContacts) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun checkContactStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userName = document.getString("name")
                    val contacts = document.get("emergencyContacts") as? List<*>
                    _uiState.update { it.copy(userName = userName, hasContacts = !contacts.isNullOrEmpty()) }
                } else {
                    _uiState.update { it.copy(hasContacts = false) }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Error fetching user data", it)
                _uiState.update { state -> state.copy(hasContacts = false) }
            }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            startLocationUpdates()
        }
    }


    @SuppressLint("MissingPermission")
    private suspend fun handleSosClick() {
        _uiState.update { it.copy(isLoading = true) } // Show loading indicator
        Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show()

        val userId = auth.currentUser?.uid
        val userName = uiState.value.userName ?: "A friend"

        if (!uiState.value.hasContacts) {
            Toast.makeText(this, "Please add emergency contacts first.", Toast.LENGTH_SHORT).show()
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        if (userId.isNullOrBlank()) {
            Toast.makeText(this, "Could not verify user. Please log in again.", Toast.LENGTH_SHORT).show()
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        var freshLocation: Location? = null
        try {
            freshLocation = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            Log.d(TAG, "Successfully fetched on-demand location.")
        } catch (e: Exception) {
            Log.e(TAG, "Could not get on-demand location", e)
        }

        // Fallback to last known location if high-accuracy one fails
        if (freshLocation == null) {
            freshLocation = currentLocation
        }

        if (freshLocation == null) {
            Toast.makeText(this, "Could not get current location. Please ensure location is enabled and try again.", Toast.LENGTH_LONG).show()
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        Log.d(TAG, "All checks passed. Proceeding with SOS.")
        sendSosNotification(userId, userName, freshLocation)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60000)
            .setMinUpdateIntervalMillis(30000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                currentLocation = locationResult.lastLocation
                Log.d(TAG, "Background location updated: ${currentLocation?.latitude}, ${currentLocation?.longitude}")
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun sendSosNotification(userId: String, userName: String, location: Location) {
        Toast.makeText(this, "Reporting accident...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // --- First Network Call: Report the accident ---

                // 1. Create the request body object
                val requestBody = AccidentRequestBody(
                    userId = userId,
                    name = userName,
                    lat = location.latitude,
                    lon = location.longitude
                )

                // 2. Pass the single body object to the function
                val reportResponse = NetworkModule.notificationApi.reportAccident(requestBody)

                if (reportResponse.isSuccessful && reportResponse.body() != null) {
                    val accidentId = reportResponse.body()!!.accidentId
                    Log.d(TAG, "SUCCESS: /accident call returned ID: $accidentId")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Accident reported. Triggering alerts...", Toast.LENGTH_SHORT).show()
                    }

                    // --- CRUCIAL: Add a 2-second delay before the next API call ---
                    Log.d(TAG, "Waiting for 2 seconds before triggering alerts...")
                    delay(2000) // Wait for 2000 milliseconds

                    // --- Second Network Call: Trigger the alerts ---
                    Log.d(TAG, "Calling /trigger_alerts for ID: $accidentId")
                    val alertResponse = NetworkModule.notificationApi.triggerAlerts(accidentId)

                    withContext(Dispatchers.Main) {
                        if (alertResponse.isSuccessful) {
                            Log.d(TAG, "SUCCESS: /trigger_alerts call was successful.")
                            Toast.makeText(this@MainActivity, "Emergency alerts sent successfully!", Toast.LENGTH_LONG).show()
                        } else {
                            Log.e(TAG, "FAILED: /trigger_alerts call failed with code ${alertResponse.code()}")
                            Toast.makeText(this@MainActivity, "Failed to trigger alerts. Server Error: ${alertResponse.code()}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    val errorBody = reportResponse.errorBody()?.string() // Read error body for more details
                    Log.e(TAG, "FAILED: /accident call failed with code ${reportResponse.code()}. Body: $errorBody")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Failed to report accident. Server error: ${reportResponse.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "SOS Network Exception", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "SOS failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                // This block ALWAYS runs, ensuring the UI is reset
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
