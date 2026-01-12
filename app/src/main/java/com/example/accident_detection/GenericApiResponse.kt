package com.example.accident_detection

import com.google.gson.annotations.SerializedName

/**
 * A generic data class to handle simple API responses from the server,
 * typically containing a success status and a message.
 */
data class GenericApiResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("message")
    val message: String
)
