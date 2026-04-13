package com.github.biltudas1.dialsomev2

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface DialsomeApiService {

    // 1. Authentication
    @POST("api/v1/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleAuthRequest): Response<AuthResponse>

    // 2. Users / Contacts
    @GET("users/contacts")
    suspend fun getContacts(): Response<List<Contact>>

    // 3. FCM Token Update
    @POST("fcm/update-token")
    suspend fun updateFcmToken(@Body request: FcmUpdateRequest): Response<Unit>

    // Add your WebRTC signaling endpoints here later...
}