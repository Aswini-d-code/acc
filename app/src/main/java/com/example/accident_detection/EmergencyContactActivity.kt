package com.example.accident_detection

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.accident_detection.databinding.ActivityEmergencyContactsBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class EmergencyContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyContactsBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val TAG = "EmergencyContact" // Tag for logging

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load existing contacts as soon as the screen opens
        loadContactsFromFirestore()

        binding.btnSaveContacts.setOnClickListener {
            saveContactsToFirestore()
        }
    }

    private fun loadContactsFromFirestore() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show()
            finish() // Close the activity if the user is not logged in
            return
        }

        // Get the document for the current user
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Document found, now get the contacts list
                    val contacts = document.get("emergencyContacts") as? List<Map<String, String>>

                    if (!contacts.isNullOrEmpty()) {
                        // We have contacts, populate the fields

                        // Populate Primary Contact
                        val primaryContact = contacts[0]
                        binding.etContactName1.setText(primaryContact["name"])
                        binding.etContactPhone1.setText(primaryContact["phone"])

                        // Populate Secondary Contact (if it exists)
                        if (contacts.size > 1) {
                            val secondaryContact = contacts[1]
                            binding.etContactName2.setText(secondaryContact["name"])
                            binding.etContactPhone2.setText(secondaryContact["phone"])
                        }
                    }
                }
                // If document doesn't exist or has no contacts, the fields will simply remain empty, which is correct.
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load contacts: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    // =========================================================================
    //  MODIFIED: This function now formats phone numbers before saving
    // =========================================================================
    private fun saveContactsToFirestore() {
        val name1 = binding.etContactName1.text.toString().trim()
        val phone1Raw = binding.etContactPhone1.text.toString().trim()
        val name2 = binding.etContactName2.text.toString().trim()
        val phone2Raw = binding.etContactPhone2.text.toString().trim()

        if (name1.isEmpty() || phone1Raw.isEmpty()) {
            Toast.makeText(this, "Primary contact name and phone are required.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- START OF THE FIX ---
        // Create a new list to hold the formatted contacts
        val contactsList = mutableListOf<Map<String, String>>()

        // Format and add the primary contact
        val phone1Formatted = formatPhoneNumber(phone1Raw)
        contactsList.add(mapOf("name" to name1, "phone" to phone1Formatted))
        Log.d(TAG, "Primary contact formatted: $phone1Raw -> $phone1Formatted")


        // Format and add the secondary contact only if both fields are filled
        if (name2.isNotEmpty() && phone2Raw.isNotEmpty()) {
            val phone2Formatted = formatPhoneNumber(phone2Raw)
            contactsList.add(mapOf("name" to name2, "phone" to phone2Formatted))
            Log.d(TAG, "Secondary contact formatted: $phone2Raw -> $phone2Formatted")
        }
        // --- END OF THE FIX ---


        val userDocumentUpdate = mapOf(
            "emergencyContacts" to contactsList
        )

        // Use .update() instead of .set() to avoid deleting other user fields like "name"
        db.collection("users").document(currentUser.uid)
            .update(userDocumentUpdate)
            .addOnSuccessListener {
                Toast.makeText(this, "Contacts saved successfully!", Toast.LENGTH_SHORT).show()
                finish() // Go back to the previous screen
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving contacts: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Helper function to format a phone number to E.164 format for India (+91).
     * It handles numbers that already have +91, are 10 digits, or are invalid.
     */
    private fun formatPhoneNumber(phone: String): String {
        // Remove spaces and dashes first
        val cleanedPhone = phone.replace(" ", "").replace("-", "")

        return when {
            // Already in correct format
            cleanedPhone.startsWith("+91") && cleanedPhone.length == 13 -> cleanedPhone
            // 10-digit number, needs country code
            cleanedPhone.length == 10 -> "+91$cleanedPhone"
            // Any other case (e.g., has '0' prefix, is too short/long), return as is
            // The backend's error handling will catch this if it's invalid for Twilio
            else -> cleanedPhone
        }
    }
}
