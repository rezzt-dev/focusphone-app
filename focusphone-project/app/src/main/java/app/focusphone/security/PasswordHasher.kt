package app.focusphone.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Shared password hashing for every FocusPhone lock (detox sessions and app schedules).
 *
 * Passwords are hashed with PBKDF2-HMAC-SHA256 using a 128-bit random salt and
 * 100 000 iterations. The stored payload is `saltBase64:hashBase64`.
 *
 * The legacy unsalted SHA-256 hex format used by early builds is still recognised so
 * existing detox passwords keep working; [verify] reports it via
 * [VerifyResult.MatchNeedsUpgrade] and the caller re-stores the modern hash.
 */
object PasswordHasher {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 100_000
    private const val PBKDF2_KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val LEGACY_HASH_LENGTH = 64

    /** Outcome of comparing a plain-text attempt against a stored payload. */
    enum class VerifyResult {
        /** The password matches and is stored in the current format. */
        Match,

        /** The password matches a legacy SHA-256 hash and should be re-hashed. */
        MatchNeedsUpgrade,

        /** The password does not match, or the stored payload is unreadable. */
        Mismatch
    }

    /** Hashes [password] with a freshly generated random salt. */
    fun hash(password: String): String = hash(password, newSalt())

    /** Hashes [password] with an explicit [salt]; used internally to verify attempts. */
    fun hash(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS)
        val digest = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(digest, Base64.NO_WRAP)
        return "$saltB64:$hashB64"
    }

    /** Compares [input] against a [stored] payload in either the modern or legacy format. */
    fun verify(stored: String, input: String): VerifyResult {
        if (stored.contains(":")) {
            val parts = stored.split(":", limit = 2)
            if (parts.size != 2) return VerifyResult.Mismatch
            val salt = try {
                Base64.decode(parts[0], Base64.NO_WRAP)
            } catch (e: IllegalArgumentException) {
                return VerifyResult.Mismatch
            }
            // A payload whose salt is missing or unreadable can never be matched.
            if (salt.isEmpty()) return VerifyResult.Mismatch
            return if (constantTimeEquals(hash(input, salt), stored)) {
                VerifyResult.Match
            } else {
                VerifyResult.Mismatch
            }
        }

        if (isLegacyHash(stored)) {
            return if (constantTimeEquals(sha256Legacy(input), stored)) {
                VerifyResult.MatchNeedsUpgrade
            } else {
                VerifyResult.Mismatch
            }
        }

        return VerifyResult.Mismatch
    }

    private fun isLegacyHash(stored: String): Boolean =
        stored.length == LEGACY_HASH_LENGTH && stored.all { it.isDigit() || it in 'a'..'f' }

    private fun newSalt(): ByteArray = ByteArray(SALT_LENGTH_BYTES).apply {
        SecureRandom().nextBytes(this)
    }

    private fun sha256Legacy(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Length-aware comparison that does not short-circuit on the first differing char. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }
}
