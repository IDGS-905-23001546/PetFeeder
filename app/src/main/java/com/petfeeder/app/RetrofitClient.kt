package com.petfeeder.app

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // La dirección se configura en ApiConfig.kt (emulador vs teléfono físico)
    private const val BASE_URL = ApiConfig.BASE_URL

    val api: ApiService by lazy {
        // Muestra en el Logcat todo lo que se envía/recibe (útil para depurar)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
