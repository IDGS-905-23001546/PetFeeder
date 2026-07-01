package com.petfeeder.app

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit hacia la API. La URL base es CONFIGURABLE (ApiConfig) para
 * poder usar emulador o teléfono físico. Se reconstruye si la URL cambia.
 *
 * Los llamadores siguen usando `RetrofitClient.api` sin pasar contexto; la URL
 * se carga al inicio en PetFeederApp.init() y al cambiarla desde la config.
 */
object RetrofitClient {

    @Volatile private var currentUrl: String = ApiConfig.DEFAULT_BASE_URL
    @Volatile private var retrofit: Retrofit? = null

    /** Se llama al arrancar la app (PetFeederApp) y al cambiar el servidor. */
    fun init(ctx: Context) {
        val url = ApiConfig.getBaseUrl(ctx)
        if (url != currentUrl) {
            currentUrl = url
            retrofit = null   // forzar reconstrucción
        }
    }

    val api: ApiService
        get() {
            val r = retrofit ?: build(currentUrl).also { retrofit = it }
            return r.create(ApiService::class.java)
        }

    private fun build(baseUrl: String): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
