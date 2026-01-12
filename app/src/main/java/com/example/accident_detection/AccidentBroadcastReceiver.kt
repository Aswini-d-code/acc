package com.example.accident_detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AccidentBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This is a placeholder. You could add logic here if needed,
        // but for now, its main purpose is to be a component that can be
        // targeted by other parts of the app.
    }

    companion object {
        // This is the unique "name" of our broadcast message.
        const val ACCIDENT_DETECTED_ACTION = "com.example.accident_detection.ACCIDENT_DETECTED"
    }
}
