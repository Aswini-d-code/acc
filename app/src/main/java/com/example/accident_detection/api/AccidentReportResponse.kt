package com.example.accident_detection.api

import com.google.gson.annotations.SerializedName

/**
 * This data class models the JSON response from the /accident endpoint.
 * e.g., {"accidentId": "some-unique-id", "status": "..."}
 */
data class AccidentReportResponse(
    @SerializedName("accidentId")
    val accidentId: String,

    @SerializedName("status")
    val status: String
)
