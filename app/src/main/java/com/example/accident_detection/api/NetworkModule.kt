

package com.example.accident_detection.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // The base URL of your deployed Render server
    private const val BASE_URL = "https://accident-api-r53d.onrender.com/"

    // Create a logger to see request details in Logcat (very useful for debugging)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // =====================================================================
    //  MODIFIED: OkHttpClient now includes explicit timeouts
    // =====================================================================
    private val httpClient = OkHttpClient.Builder()
        // Set the timeout to 30 seconds (up from the default of ~10s)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor) // Keep the logger
        .build()
    // =====================================================================

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient) // Use the new client with the longer timeout
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val notificationApi: NotificationApi = retrofit.create(NotificationApi::class.java)
}
