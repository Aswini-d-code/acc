

package com.example.accident_detection

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.accident_detection.databinding.ActivityPermissionBinding

class PermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding

    // Launcher for standard permissions (Location, SMS, Notifications)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLoc = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val sms = permissions[Manifest.permission.SEND_SMS] ?: false
        val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true

        if (fineLoc && sms && notifications) {
            // All standard permissions granted, now check for background location
            checkBackgroundLocation()
        } else {
            Toast.makeText(this, "Location, SMS, and Notifications are needed for safety!", Toast.LENGTH_LONG).show()
            // You could optionally finish() here or let the user try again via the button
        }
    }

    // Separate launcher specifically for Background Location
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Background location helps protect you even when the app is closed.", Toast.LENGTH_LONG).show()
        }
        // Whether granted or not, proceed to the main app screen
        handleFinalNavigation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the button to allow the user to try again if they initially deny permissions
        binding.btnGrantPermissions.setOnClickListener {
            checkAndRequestPermissions()
        }

        // *** THE FIX: Automatically start the permission check when the activity is created ***
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Check for standard permissions that are NOT yet granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            // If any standard permissions are missing, request them
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // All standard permissions are already granted, proceed to check background location
            checkBackgroundLocation()
        }
    }

    private fun checkBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Background location is not granted, request it
                Toast.makeText(this, "Please select 'Allow all the time' on the next screen for full protection.", Toast.LENGTH_LONG).show()
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                // All permissions (including background) are granted, navigate to main
                handleFinalNavigation()
            }
        } else {
            // For older Android versions, background location is granted implicitly with fine location
            handleFinalNavigation()
        }
    }

    private fun handleFinalNavigation() {
        Toast.makeText(this, "Setup complete! You are now protected.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        // These flags clear the entire task stack and start MainActivity fresh.
        // The user cannot press "back" to go to the permissions or login screens.
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Close this PermissionsActivity
    }
}




