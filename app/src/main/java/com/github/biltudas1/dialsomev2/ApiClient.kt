package com.github.biltudas1.dialsomev2

import android.content.Context
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Replace with your actual backend URL
    private const val BASE_URL = "https://dialsome.onrender.com/"

    @Volatile
    private var retrofit: Retrofit? = null

    fun getApiService(context: Context): DialsomeApiService {
        return retrofit?.create(DialsomeApiService::class.java) ?: synchronized(this) {
            val secureStorage = SecureStorageManager(context)

            // 1. Logger to see API requests/responses in Android Studio Logcat
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // 2. Auth Interceptor to automatically add the JWT to headers
            val authInterceptor = Interceptor { chain ->
                val requestBuilder = chain.request().newBuilder()

                // Read token synchronously for the OkHttp thread
                val token = runBlocking { secureStorage.getData("access_token") }

                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }

                chain.proceed(requestBuilder.build())
            }

            // 3. Build OkHttpClient
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            // 4. Build Retrofit
            val newRetrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit = newRetrofit
            newRetrofit.create(DialsomeApiService::class.java)
        }
    }
}