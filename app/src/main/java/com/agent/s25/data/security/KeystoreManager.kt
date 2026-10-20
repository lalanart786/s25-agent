package com.agent.s25.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "s25_agent_knox_vault_key"
    private const val TAG = "KeystoreManager"

    // Fixed 12-byte IV used solely to derive a stable DB passphrase (not for general encryption)
    private val DERIVATION_IV = byteArrayOf(
        0x53, 0x32, 0x35, 0x5F, 0x41, 0x47, 0x45, 0x4E, 0x54, 0x5F, 0x44, 0x42
    )
    // Fixed plaintext we encrypt to derive a stable passphrase
    private val DERIVATION_PLAINTEXT = "s25-agent-knox-vault-passphrase-seed".toByteArray(Charsets.UTF_8)

    fun getOrCreateMasterPassphrase(): ByteArray {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val builder = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false)

                // Leverage hardware StrongBox / Knox on S25 Ultra if available
                try {
                    builder.setIsStrongBoxBacked(true)
                } catch (e: Exception) {
                    Log.d(TAG, "StrongBox not available, falling back to TEE keystore")
                }

                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
            }

            // AndroidKeyStore keys do NOT expose .encoded (returns null by design).
            // Instead, we encrypt a fixed plaintext with a fixed IV to derive
            // a deterministic passphrase that never changes for this key.
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, DERIVATION_IV))
            val ciphertext = cipher.doFinal(DERIVATION_PLAINTEXT)

            // SHA-256 hash of the ciphertext gives us a stable 32-byte passphrase
            MessageDigest.getInstance("SHA-256").digest(ciphertext)
        } catch (e: Exception) {
            Log.e(TAG, "Keystore passphrase derivation failed, using fallback: ${e.message}")
            // Deterministic fallback passphrase derived from device-specific salt
            val fallback = "s25-agent-fallback-${android.os.Build.FINGERPRINT}"
            MessageDigest.getInstance("SHA-256").digest(fallback.toByteArray(Charsets.UTF_8))
        }
    }
}
