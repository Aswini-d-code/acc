


package com.example.accident_detection

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.android_splash) // Ensure this layout file exists

        // --- FOR TESTING: Force logout every time the app starts ---
        FirebaseAuth.getInstance().signOut()
        // --- End of testing code ---

        Handler(Looper.getMainLooper()).postDelayed({

            // Now, when we check for a user, it will always be null.
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                // This block will now be skipped during testing, but it's correct for production.
                // We will navigate to PermissionsActivity to check permissions first.
                startActivity(Intent(this, PermissionsActivity::class.java))
            } else {
                // Since the user is signed out, the app will always go to the LoginActivity.
                startActivity(Intent(this, LoginActivity::class.java))
            }

            // Finish the SplashActivity so the user cannot navigate back to it
            finish()
        }, 2000) // 2000 milliseconds = 2 seconds
    }
}
