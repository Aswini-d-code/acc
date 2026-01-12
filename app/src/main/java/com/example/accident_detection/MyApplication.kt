package com.example.accident_detection

import android.app.Application
import com.google.firebase.FirebaseApp

// This class is the first thing that runs when your app starts.
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase safely before any activity starts.
        // This prevents crashes related to using Firebase services too early.
        FirebaseApp.initializeApp(this)
    }
}
