package com.github.biltudas1.dialsomev2

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException

class GoogleAuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    companion object {
        private const val TAG = "Dialsome_GoogleAuth"
    }

    /**
     * Launches the native Google Sign-In bottom sheet.
     * Returns the Google ID Token (JWT) on success, or null on failure.
     */
    suspend fun signIn(): String? {
        // 1. Build the Google ID Option
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Let user choose any account on the device
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(true) // Automatically select if there's only one account
            .build()

        // 2. Build the Request
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            // 3. Launch the Credential Manager (this suspends until the user completes the flow)
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            // 4. Extract the token
            val credential = result.credential
            if (credential is GoogleIdTokenCredential) {
                // Return the raw JWT string
                credential.idToken
            } else {
                try {
                    // Sometimes the credential needs to be parsed explicitly
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    googleIdTokenCredential.idToken
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e(TAG, "Received an unrecognized credential type.")
                    null
                }
            }
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Sign-in failed with error: ${e.errorMessage}")
            null
        } catch (e: CancellationException) {
            Log.i(TAG, "User cancelled the sign-in flow.")
            throw e // Always re-throw CancellationException in Coroutines
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sign-in: ${e.localizedMessage}")
            null
        }
    }
}