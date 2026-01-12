package com.example.accident_detection

import com.google.gson.annotations.SerializedName

/**
 * This data class represents the JSON payload sent to the backend
 * when an SOS or accident alert is triggered.
 * It MUST match the fields expected by the service.
 */
data class AccidentPayload(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("name")
    val name: String,

    // FIX: The field is named 'latitude'
    @SerializedName("latitude")
    val latitude: Double,

    // FIX: The field is named 'longitude'
    @SerializedName("longitude")
    val longitude: Double
)
