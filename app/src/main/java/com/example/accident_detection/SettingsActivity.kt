package com.example.accident_detection

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.accident_detection.databinding.ActivitySettingsBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SettingsActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This line links the Kotlin file to your XML layout. It will work perfectly now.
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the click listeners for all three clickable elements
        binding.btnEditContacts.setOnClickListener(this)
        binding.btnLogout.setOnClickListener(this)
        // *** FIX: Correctly listen to the TextView ***
        binding.btnBack.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnEditContacts -> {
                // Navigate to the activity for managing contacts
                startActivity(Intent(this, EmergencyContactActivity::class.java))
            }
            R.id.btnLogout -> {
                // Sign out the user from Firebase
                Firebase.auth.signOut()
                // Go back to the Login screen and clear all previous activities
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            // *** FIX: Added the case to handle the back button click ***
            R.id.btnBack -> {
                // Simply finish this activity and go back to the previous one (MainActivity)
                finish()
            }
        }
    }
}
