package com.example.accident_detection

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// REMOVED: import androidx.compose.ui.semantics.text
import com.example.accident_detection.databinding.ActivityRegisterBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide unnecessary UI elements from the old design
        binding.etEmail.visibility = android.view.View.GONE
        binding.etPassword.visibility = android.view.View.GONE
        binding.tvBackToLogin.visibility = android.view.View.GONE
        binding.textViewLabel.text = "Welcome! Please confirm your name." // This will now work

        // Get the newly signed-in user from Firebase Auth
        val user = auth.currentUser
        if (user == null) {
            // This should not happen if the flow is correct, but as a safeguard:
            Toast.makeText(this, "Error: No signed-in user found.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Pre-fill the name field if Google provides a display name
        if (!user.displayName.isNullOrEmpty()) {
            binding.etName.setText(user.displayName)
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val userId = user.uid

            if (name.isEmpty()) {
                binding.etName.error = "Name cannot be empty"
                return@setOnClickListener
            }

            // UI feedback
            binding.btnRegister.isEnabled = false
            Toast.makeText(this, "Saving your profile...", Toast.LENGTH_SHORT).show()

            // Create the request payload for the backend API
            val payload = RegisterPayload(userId = userId, name = name)

            // Call the /register_device endpoint on your backend
            ApiClient.apiService.registerDevice(payload).enqueue(object : Callback<GenericApiResponse> {
                override fun onResponse(call: Call<GenericApiResponse>, response: Response<GenericApiResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "Registration Successful!", Toast.LENGTH_SHORT).show()
                        // After successful backend registration, go to MainActivity
                        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                        startActivity(intent)
                        finishAffinity() // Close all previous activities (Login, Splash)
                    } else {
                        binding.btnRegister.isEnabled = true
                        val errorMsg = "Registration failed on server. Code: ${response.code()}"
                        Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_LONG).show()
                        Log.e("RegisterActivity", errorMsg)
                    }
                }

                override fun onFailure(call: Call<GenericApiResponse>, t: Throwable) {
                    binding.btnRegister.isEnabled = true
                    val errorMsg = "Network Error: ${t.message}"
                    Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_LONG).show()
                    Log.e("RegisterActivity", errorMsg, t)
                }
            })
        }
    }
}
