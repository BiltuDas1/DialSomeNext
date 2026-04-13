package com.github.biltudas1.dialsomev2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var progressBar: ProgressBar

    private lateinit var googleAuthManager: GoogleAuthManager
    private lateinit var secureStorage: SecureStorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        progressBar = findViewById(R.id.progressBar)

        googleAuthManager = GoogleAuthManager(this)
        secureStorage = SecureStorageManager(this)

        // Check if user is already logged in
        checkExistingSession()

        btnGoogleSignIn.setOnClickListener {
            performLogin()
        }
    }

    private fun checkExistingSession() {
        lifecycleScope.launch {
            val token = secureStorage.getData("access_token")
            if (!token.isNullOrEmpty()) {
                // Already logged in, go straight to MainActivity
                navigateToMain()
            }
        }
    }

    private fun performLogin() {
        lifecycleScope.launch {
            setLoading(true)
            try {
                // 1. Get Google ID Token from Native Android Dialog
                val googleIdToken = googleAuthManager.signIn()

                if (googleIdToken != null) {
                    // 2. Send token to your Python/FastAPI backend
                    val apiService = ApiClient.getApiService(this@LoginActivity)
                    val request = GoogleAuthRequest(idToken = googleIdToken)

                    // NOTE: Make sure your DialsomeApiService has the login method defined
                    val response = apiService.loginWithGoogle(request)

                    if (response.isSuccessful && response.body() != null) {
                        val authData = response.body()!!

                        // 3. Save the custom backend tokens securely
                        secureStorage.saveData("access_token", authData.accessToken)
                        if (authData.refreshToken != null) {
                            secureStorage.saveData("refresh_token", authData.refreshToken)
                        }

                        Log.d("DialSomeLogin", "Login successful! User ID: ${authData.userId}")
                        navigateToMain()
                    } else {
                        showError("Backend Authentication Failed: ${response.code()}")
                    }
                } else {
                    showError("Google Sign-In Failed or Cancelled.")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close LoginActivity so the user can't press "Back" to get to it
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            btnGoogleSignIn.text = ""
            btnGoogleSignIn.isEnabled = false
            progressBar.visibility = View.VISIBLE
        } else {
            btnGoogleSignIn.text = "Sign in with Google"
            btnGoogleSignIn.isEnabled = true
            progressBar.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        Log.e("DialSomeLogin", message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}