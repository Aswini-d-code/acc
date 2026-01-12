package com.example.accident_detection

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    // For user registration after Google Sign-In
    @POST("/register_device")
    fun registerDevice(@Body payload: RegisterPayload): Call<GenericApiResponse>

    // For sending an accident alert
    // FIX: The method name is 'sendAlert'
    @POST("/alert")
    fun sendAlert(@Body payload: AccidentPayload): Call<Void>
}
