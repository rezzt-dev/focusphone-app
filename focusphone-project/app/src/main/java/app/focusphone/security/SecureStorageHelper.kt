package app.focusphone.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Symmetric encryption helper backed by Android Keystore.
 *
 * Uses AES-256/GCM/NoPadding with a hardware-backed (or TEE-backed) key
 * that never leaves the Keystore boundary.
 *
 * The encrypted payload format is:
 *   Base64( IV(12 bytes) || ciphertext || authTag(16 bytes) )
 *
 * This helper is intentionally synchronous so it can be used inside
 * SharedPreferences wrappers that are read on the main thread.
 */
object SecureStorageHelper {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "focusphone_master_key_v1"
    private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    /** Encrypt [plainText]; returns Base64-encoded IV+ciphertext or null on failure. */
    fun encrypt(context: Context, plainText: String): String? {
        return try {
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteBuffer.allocate(iv.size + cipherBytes.size)
                .put(iv)
                .put(cipherBytes)
                .array()
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /** Decrypt a Base64-encoded IV+ciphertext payload; returns plain text or null on failure. */
    fun decrypt(context: Context, cipherText: String): String? {
        return try {
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH_BYTES + 1) return null

            val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)

            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (entry != null) {
            return entry.secretKey
        }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }
}
