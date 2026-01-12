


package com.example.accident_detection

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.accident_detection.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var oneTapClient: SignInClient
    private lateinit var auth: FirebaseAuth

    // This launcher handles the result from the Google Sign-In UI (the account picker)
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            try {
                // Attempt to get the credential from the returned intent
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken

                if (idToken != null) {
                    // We have the Google ID Token, now authenticate with Firebase
                    firebaseAuthWithGoogle(idToken)
                } else {
                    // This can happen if the user cancels the sign-in flow
                    Log.d("LoginActivity", "No ID token from Google Sign-In.")
                    Toast.makeText(this, "Google Sign-In was cancelled.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                // Handle various API errors that can occur
                Log.w("LoginActivity", "Google Sign-In failed with ApiException", e)
                Toast.makeText(this, "An error occurred during sign-in.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // --- CORRECTED LOGIC ---
        // 1. Inflate the layout and set the content view FIRST.
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Initialize Firebase and Google Sign-In clients.
        auth = Firebase.auth
        oneTapClient = Identity.getSignInClient(this)

        // 3. Set up the click listener for the sign-in button.
        binding.googleSignInButton.setOnClickListener {
            startSignInFlow()
        }
    }

    private fun startSignInFlow() {
        // This is your Web Client ID from the Google Cloud Console / google-services.json
        // IMPORTANT: Make sure you have added this to your strings.xml file.
        val webClientId = getString(R.string.your_web_client_id)



        // Build the sign-in request for Google's One Tap UI
        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(webClientId)
                    .setFilterByAuthorizedAccounts(false) // Show all Google accounts on the device
                    .build()
            )
            .setAutoSelectEnabled(true) // Try to sign in automatically if possible
            .build()

        // Begin the sign-in process
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    // The One Tap UI is ready to be shown. Launch it using our launcher.
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    googleSignInLauncher.launch(intentSenderRequest)
                } catch (e: IntentSender.SendIntentException) {
                    // This is a failsafe if the intent sender fails
                    Log.e("LoginActivity", "Could not launch sign-in intent", e)
                    Toast.makeText(this, "Could not display sign-in options.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener(this) { e ->
                // This can fail if the device doesn't have Google Play Services or it's outdated.
                Log.e("LoginActivity", "Google Sign-In failed to start.", e)
                Toast.makeText(this, "Sign-in is unavailable on this device.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign-in success!
                    Toast.makeText(this, "Sign-In Successful!", Toast.LENGTH_SHORT).show()
                    // *** THE FIX: Navigate to PermissionsActivity, NOT MainActivity ***
                    navigateToPermissions()
                } else {
                    // Handle Firebase authentication failure
                    Log.w("LoginActivity", "Firebase authentication failed", task.exception)
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToPermissions() {
        // This is the correct navigation function for our app flow
        val intent = Intent(this, PermissionsActivity::class.java)
        // These flags prevent the user from going back to the login screen
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Close the login activity
    }
}
