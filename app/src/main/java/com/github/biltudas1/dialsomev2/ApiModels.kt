package com.github.biltudas1.dialsomev2

import com.google.gson.annotations.SerializedName

// Request body for sending the Google Token to your backend
data class GoogleAuthRequest(
    @SerializedName("id_token") val idToken: String
)

// Response from your backend containing your app's JWTs
data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("user_id") val userId: String
)

// Example FCM Token update request
data class FcmUpdateRequest(
    @SerializedName("fcm_token") val fcmToken: String
)

// Example Contact model (Adjust fields based on your backend models)
data class Contact(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("avatar_url") val avatarUrl: String?
)