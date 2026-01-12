package com.example.accident_detection.api


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * This data class represents the JSON body for the /accident request.
 */
data class AccidentRequestBody(
    val userId: String,
    val name: String,
    val lat: Double,
    val lon: Double
)

interface NotificationApi {

    /**
     * MODIFIED: This function now sends the accident data in the request body
     * to match the updated Python server, fixing the 422 error.
     */
    @POST("accident")
    suspend fun reportAccident(
        @Body body: AccidentRequestBody
    ): Response<AccidentReportResponse>

    /**
     * This function remains unchanged.
     */
    @POST("trigger_alerts/{accident_id}")
    suspend fun triggerAlerts(
        @Path("accident_id") accidentId: String
    ): Response<Unit>
}
