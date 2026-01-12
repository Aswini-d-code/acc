package com.example.accident_detection.api

import com.google.gson.annotations.SerializedName

data class NotificationPayload(
    @SerializedName("userId") val userId: String
)