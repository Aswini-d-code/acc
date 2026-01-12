package com.example.accident_detection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * This function is automatically called by the Android system when your app
     * receives a push notification from your backend.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "FCM PUSH NOTIFICATION RECEIVED!")
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Your backend will likely send the notification details in the "data" payload.
        remoteMessage.data.isNotEmpty().let {
            val title = remoteMessage.data["title"] ?: "Emergency Alert"
            val body = remoteMessage.data["body"] ?: "An event has been detected."

            Log.d(TAG, "Notification Title: '$title'")
            Log.d(TAG, "Notification Body: '$body'")

            // Call our function to display the notification on the device.
            sendNotification(title, body)
        }
    }

    /**
     * Creates the notification channel and builds/shows the actual notification.
     */
    private fun sendNotification(title: String, messageBody: String) {
        val channelId = "accident_alert_channel" // A unique ID for this channel
        val notificationId = 101 // A unique ID for this notification

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // On Android 8.0 (API 26) and higher, you must create a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Accident Alerts"
            val descriptionText = "Urgent notifications for detected accidents"
            val importance = NotificationManager.IMPORTANCE_HIGH // This makes the notification pop up
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build the visual notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            // === THIS IS THE CORRECTED LINE ===
            .setSmallIcon(R.drawable.ic_sos)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // The notification disappears when tapped

        // Show the notification on the device
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /**
     * This is called when a new device token is generated. You need to send this
     * token to your backend so it knows which device to send notifications to.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        // Your team should have a backend endpoint to save this token against the user ID.
        // sendTokenToServer(token)
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
