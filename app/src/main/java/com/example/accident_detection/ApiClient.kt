// ApiClient.kt
package com.example.accident_detection

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // ##################################################################
    // PASTE THE URL FROM YOUR TEAMMATE HERE
    // It should look something like: "https://us-central1-your-project.cloudfunctions.net/"
    // Make sure it ends with a slash "/"
    // ##################################################################
    internal const val BASE_URL = "https://accident-api-r53d.onrender.com"

    // Creates a single, reusable instance of Retrofit
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Creates a single, reusable instance of our ApiService
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}