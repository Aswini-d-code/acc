package com.example.accident_detection

import com.google.gson.annotations.SerializedName

/**
 * This data class represents the JSON payload sent to the backend
 * when a user's device is registered for the first time.
 */
data class RegisterPayload(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("name")
    val name: String
)
