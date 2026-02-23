package com.mintanable.notethepad.core.security

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager

class CryptoManager(private val context: Context) {
    init {
        AeadConfig.register()
    }

    private val aead: Aead by lazy {
        try {
            createAead()
        } catch (e: Exception) {
            if (e.message?.contains("master_key") == true || e is java.security.GeneralSecurityException) {
                Log.e("kptest", "CryptoManager: Keystore corruption detected. Attempting reset.")
                handleKeyCorruption()
                createAead()
            } else {
                throw e
            }
        }
    }

    private fun createAead(): Aead {
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "tink_keyset", "tink_prefs")
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri("android-keystore://master_key")
            .build()
            .keysetHandle
        return keysetHandle.getPrimitive(Aead::class.java)
    }

    private fun handleKeyCorruption() {
        context.getSharedPreferences("tink_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.deleteEntry("master_key")
    }

    fun encrypt(data: String): String {
        return try {
            val ciphertext = aead.encrypt(data.toByteArray(), null)
            Base64.encodeToString(ciphertext, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("kptest", "Encryption failed: ${e.message}")
            ""
        }
    }

    fun decrypt(encryptedData: String): String {
        if (encryptedData.isBlank()) return ""
        return try {
            val ciphertext = Base64.decode(encryptedData, Base64.DEFAULT)
            String(aead.decrypt(ciphertext, null))
        } catch (e: Exception) {
            Log.e("kptest", "Decryption failed: ${e.message}")
            ""
        }
    }
}