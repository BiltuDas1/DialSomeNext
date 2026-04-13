package com.github.biltudas1.dialsomev2

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.nio.charset.StandardCharsets

// Create a DataStore instance using the Kotlin property delegate
private val Context.dataStore by preferencesDataStore(name = "dialsome_secure_prefs")

class SecureStorageManager(private val context: Context) {

    private var aead: Aead? = null

    companion object {
        private const val TAG = "Dialsome_Auth"
        private const val KEYSET_NAME = "dialsome_master_keyset"
        private const val PREF_FILE_NAME = "dialsome_tink_prefs"
        private const val MASTER_KEY_URI = "android-keystore://dialsome_master_key"
    }

    init {
        try {
            AeadConfig.register()
            // This builds the hardware-backed bridge using Android Keystore
            aead = AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
                .getPrimitive(Aead::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Tink: ${e.message}")
        }
    }

    /**
     * Encrypts and saves data asynchronously.
     * Use this inside a Coroutine scope.
     */
    suspend fun saveData(keyName: String, plainText: String) {
        val cryptoAead = aead
        if (cryptoAead == null) {
            Log.e(TAG, "Tink Aead not initialized")
            return
        }

        try {
            val encrypted = cryptoAead.encrypt(plainText.toByteArray(StandardCharsets.UTF_8), null)
            val base64Encrypted = Base64.encodeToString(encrypted, Base64.DEFAULT)

            val preferencesKey = stringPreferencesKey(keyName)
            context.dataStore.edit { preferences ->
                preferences[preferencesKey] = base64Encrypted
            }
        } catch (e: Exception) {
            Log.e(TAG, "Save Error: ${e.message}")
        }
    }

    /**
     * Retrieves and decrypts data asynchronously.
     * Returns null if the key doesn't exist or decryption fails.
     */
    suspend fun getData(keyName: String): String? {
        val cryptoAead = aead ?: return null

        return try {
            val preferencesKey = stringPreferencesKey(keyName)
            val preferences = context.dataStore.data.first()
            val base64Encrypted = preferences[preferencesKey]

            if (base64Encrypted.isNullOrEmpty()) return null

            val encrypted = Base64.decode(base64Encrypted, Base64.DEFAULT)
            val decrypted = cryptoAead.decrypt(encrypted, null)
            String(decrypted, StandardCharsets.UTF_8)

        } catch (e: Exception) {
            Log.e(TAG, "Retrieve Error: ${e.message}")
            null
        }
    }

    /**
     * Wipes all encrypted data. Use this for Logging out.
     */
    suspend fun clearData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Checks if a specific key exists in the DataStore.
     */
    suspend fun hasKey(keyName: String): Boolean {
        val preferencesKey = stringPreferencesKey(keyName)
        val preferences = context.dataStore.data.first()
        return preferences.contains(preferencesKey)
    }
}