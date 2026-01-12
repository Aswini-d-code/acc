// In app/src/main/java/com/example/accident_detection/LocationService.kt

package com.example.accident_detection // Make sure this matches your package name

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_CHANNEL_ID = "location_service_channel"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d("LocationService", "Location Update: ${location.latitude}, ${location.longitude}")
                    // Here you could broadcast the location to MainActivity if needed,
                    // but for the SOS feature, getting a single location is often enough.
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startLocationUpdates()
            ACTION_STOP -> stopLocationService()
        }
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Location Service",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText("Tracking location for safety...")
            .setSmallIcon(R.mipmap.ic_launcher) // Use your app's icon
            .build()

        // A service must start in the foreground within a few seconds of being created.
        startForeground(1, notification)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LocationService", "Location permission not granted.", e)
            stopSelf()
        }
    }

    private fun stopLocationService() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't provide binding, so return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure location updates are stopped when the service is destroyed.
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}