package com.example.accident_detection

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.sqrt

class AccidentDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Location components
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // --- Detection Thresholds ---
    // G-force threshold for a significant physical shock.
    private val G_FORCE_THRESHOLD = 30.0f
    // Minimum speed (in meters/second) before we start considering a sudden stop. 10 m/s is ~36 km/h or 22 mph.
    private val MIN_SPEED_THRESHOLD = 10f
    // Speed drops below this threshold (in m/s) after being above MIN_SPEED_THRESHOLD. 1 m/s is ~3.6 km/h.
    private val SUDDEN_STOP_THRESHOLD = 1f
    // --- End of Thresholds ---

    private var previousSpeed: Float = 0f
    private var isAlertSent = false

    private val CHANNEL_ID = "AccidentDetectionChannel"

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupLocationUpdates()

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        createNotificationChannel()
        startForegroundService()
        Log.d("AccidentService", "Service created and location/sensor listeners registered.")
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Accident Detection Active")
            .setContentText("Monitoring your location and movement for safety.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure you have this drawable
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // --- THE FIX ---
        // Check the Android version before calling startForeground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 (API 29) and above, use the version that requires a service type.
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            // For older versions, use the simpler version of the method.
            startForeground(1, notification)
        }
    }


    private fun setupLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val currentSpeed = location.speed // Speed in meters/second
                    Log.d("AccidentService", "Speed update: ${String.format("%.2f", currentSpeed)} m/s")

                    // Check for sudden deceleration
                    if (previousSpeed >= MIN_SPEED_THRESHOLD && currentSpeed < SUDDEN_STOP_THRESHOLD) {
                        Log.d("AccidentService", "SUDDEN STOP DETECTED! Speed dropped from $previousSpeed to $currentSpeed m/s.")
                        // This indicates a potential accident. We still wait for the G-force check to confirm.
                        // The onSensorChanged will handle the final trigger.
                    }
                    previousSpeed = currentSpeed
                }
            }
        }

        // Check for permission before requesting updates
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            Log.e("AccidentService", "Location permission not granted. Speed detection will not work.")
        }
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (isAlertSent) return

        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // *** COMBINED DETECTION LOGIC ***
            // An accident is likely if there is a high G-force impact AND we recently detected a sudden stop.
            if (gForce > G_FORCE_THRESHOLD && previousSpeed < SUDDEN_STOP_THRESHOLD) {
                Log.d("AccidentService", "CRITICAL EVENT: High G-Force ($gForce) combined with recent sudden stop. Triggering alert.")
                isAlertSent = true // Prevent multiple alerts
                sendEmergencyAlert()
                stopMonitoring() // Stop listeners to save battery and prevent re-triggering
            }
        }
    }

    private fun sendEmergencyAlert() {
        val user = Firebase.auth.currentUser
        if (user == null) {
            Log.e("AccidentService", "Cannot send alert, user is not logged in.")
            stopSelf()
            return
        }

        Log.d("AccidentService", "Fetching last known location to send alert...")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("AccidentService", "Location permission not granted. Cannot get location for alert.")
            stopSelf()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                Log.d("AccidentService", "Final location for alert: Lat: $lat, Lon: $lon. Sending to backend.")

                val payload = AccidentPayload(
                    userId = user.uid,
                    name = user.displayName ?: "N/A",
                    latitude = lat,
                    longitude = lon
                )

                ApiClient.apiService.sendAlert(payload).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Log.d("AccidentService", "Backend successfully notified of the accident.")
                        } else {
                            Log.e("AccidentService", "Backend notification failed. Response Code: ${response.code()}, Message: ${response.message()}")
                        }
                        stopSelf() // Stop the service after the attempt
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("AccidentService", "Network request to backend failed.", t)
                        stopSelf() // Stop the service after the attempt
                    }
                })

            } else {
                Log.e("AccidentService", "Could not get last known location. Cannot send alert.")
                stopSelf()
            }
        }.addOnFailureListener {
            Log.e("AccidentService", "Failed to get location for alert.", it)
            stopSelf()
        }
    }


    private fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("AccidentService", "Monitoring stopped to prevent re-triggering.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Accident Detection Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring() // Ensure all listeners are stopped when the service is destroyed
        Log.d("AccidentService", "Service destroyed. All listeners unregistered.")
    }
}