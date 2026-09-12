package app.focusphone.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
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
 *
 * Failures are never swallowed silently: every one is logged and reported through a
 * typed result, because callers must be able to tell "nothing stored" apart from
 * "stored but unreadable" — treating the latter as empty is what used to overwrite
 * user data with defaults.
 */
object SecureStorageHelper {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "focusphone_master_key_v1"
    private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / 8
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val TAG = "SecureStorageHelper"

    /**
     * Smallest payload [encrypt] can produce: the IV, the authentication tag and at least
     * one byte of ciphertext.
     */
    private const val MIN_PAYLOAD_BYTES = GCM_IV_LENGTH_BYTES + GCM_TAG_LENGTH_BYTES + 1

    private val BASE64_SHAPE = Regex("^[A-Za-z0-9+/]+={0,2}$")

    /** Outcome of [decrypt]. */
    sealed interface DecryptResult {
        data class Success(val plainText: String) : DecryptResult

        /**
         * The Keystore key could not be loaded right now. The payload may be perfectly
         * fine, so this is transient and the stored value must be kept.
         */
        data object KeyUnavailable : DecryptResult

        /**
         * The payload can never be decrypted: it is damaged, or it was sealed with a key
         * that no longer exists (for example after the Keystore was reset).
         */
        data object Unreadable : DecryptResult
    }

    /** Encrypt [plainText]; returns the Base64 IV+ciphertext payload, or null on failure. */
    fun encrypt(plainText: String): String? {
        val key = getOrCreateKey() ?: return null
        return try {
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteBuffer.allocate(iv.size + cipherBytes.size)
                .put(iv)
                .put(cipherBytes)
                .array()
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            null
        }
    }

    /** Decrypt a Base64-encoded IV+ciphertext payload. */
    fun decrypt(cipherText: String): DecryptResult {
        val combined = try {
            Base64.decode(cipherText, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            Log.w(TAG, "Stored payload is not valid Base64")
            return DecryptResult.Unreadable
        }
        if (combined.size < MIN_PAYLOAD_BYTES) {
            Log.w(TAG, "Stored payload is too short to be an encrypted value")
            return DecryptResult.Unreadable
        }

        val key = getOrCreateKey() ?: return DecryptResult.KeyUnavailable
        return try {
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            DecryptResult.Success(String(cipher.doFinal(cipherBytes), Charsets.UTF_8))
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            DecryptResult.Unreadable
        }
    }

    /**
     * Whether [value] has the shape of a payload produced by [encrypt].
     *
     * Used to recognise legacy plain-text values by their *form* rather than by the fact
     * that decryption failed: a failed decryption of a real payload must never be mistaken
     * for plain text, or the ciphertext gets re-encrypted and is lost for good.
     */
    fun looksEncrypted(value: String): Boolean {
        if (!BASE64_SHAPE.matches(value)) return false
        val decoded = try {
            Base64.decode(value, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            return false
        }
        return decoded.size >= MIN_PAYLOAD_BYTES
    }

    private fun getOrCreateKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                // Never generate over an existing alias: that would replace the key and
                // make every stored value unreadable because of a transient read error.
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (entry == null) Log.e(TAG, "Keystore entry exists but could not be loaded")
                return entry?.secretKey
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
            keyGen.generateKey()
        } catch (e: Exception) {
            Log.e(TAG, "Keystore key creation/retrieval failed", e)
            null
        }
    }
}
