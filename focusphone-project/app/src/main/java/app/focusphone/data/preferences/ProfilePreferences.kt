package app.focusphone.data.preferences

import android.content.Context
import android.util.Base64
import androidx.annotation.VisibleForTesting
import app.focusphone.security.SecureStorageHelper
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Synchronous preference manager for Contextual Profiles and Detox app blocking.
 *
 * Security guarantees:
 *  - Sensitive values (profile, detox timers, password hash, blocked apps, work keywords) are
 *    encrypted at rest via [SecureStorageHelper] (AES-256/GCM + Android Keystore).
 *  - Detox passwords are hashed with PBKDF2-HMAC-SHA256 using a 128-bit random salt
 *    and 100 000 iterations. The legacy SHA-256 (no salt) format is detected and
 *    transparently upgraded on successful verification.
 *  - Failed password attempts are persisted with a 30-minute sliding window.
 *    After 4 failures the detox session is force-disabled.
 */
object ProfilePreferences {

    private const val PREF_FILE = "focusphone_profile"
    private const val KEY_PROFILE = "current_profile"
    private const val KEY_DETOX_END = "detox_end_time"
    private const val KEY_DETOX_PASSWORD = "detox_password"
    private const val KEY_BLOCKED_APPS = "blocked_apps"
    private const val KEY_WORK_KEYWORDS = "work_keywords"
    private const val KEY_FAILED_ATTEMPTS = "detox_failed_attempts"
    private const val KEY_LAST_FAILED_TIME = "detox_last_failed_time"

    const val PROFILE_NORMAL = "normal"
    const val PROFILE_WORK = "work"
    const val PROFILE_DETOX = "detox"

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 100_000
    private const val PBKDF2_KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val ATTEMPT_WINDOW_MS = 30 * 60 * 1000L // 30 minutes

    private val DEFAULT_WORK_KEYWORDS = setOf(
        "instagram", "facebook", "twitter", "tiktok", "youtube", "snapchat",
        "netflix", "pinterest", "twitch", "discord", "reddit", "linkedin",
        "clash", "candycrush", "pubg", "fortnite"
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    // ── Profile ────────────────────────────────────────────────────

    fun getProfile(context: Context): String =
        getEncryptedString(context, KEY_PROFILE, PROFILE_NORMAL) ?: PROFILE_NORMAL

    fun setProfile(context: Context, profile: String) {
        putEncryptedString(context, KEY_PROFILE, profile)
    }

    // ── Detox timer ────────────────────────────────────────────────

    fun getDetoxEndTime(context: Context): Long =
        getEncryptedLong(context, KEY_DETOX_END, 0L)

    fun setDetoxEndTime(context: Context, endTimeMs: Long) {
        putEncryptedLong(context, KEY_DETOX_END, endTimeMs)
    }

    // ── Detox password (PBKDF2 + salt) ─────────────────────────────

    /** Returns the stored PBKDF2 hash payload, or null if none is set. */
    fun getDetoxPassword(context: Context): String? =
        getEncryptedString(context, KEY_DETOX_PASSWORD, null)

    /** Stores the password as a PBKDF2 hash with random salt. Pass null to clear. */
    fun setDetoxPassword(context: Context, password: String?) {
        val payload = password?.let { pbkdf2Hash(it) }
        putEncryptedString(context, KEY_DETOX_PASSWORD, payload)
    }

    /** Hashes input and compares with stored hash. Supports transparent migration from legacy SHA-256. */
    fun verifyDetoxPassword(context: Context, input: String): Boolean {
        val stored = getDetoxPassword(context) ?: return false

        // Modern format: "saltBase64:hashBase64"
        if (stored.contains(":")) {
            val parts = stored.split(":", limit = 2)
            if (parts.size == 2) {
                val salt = Base64.decode(parts[0], Base64.NO_WRAP)
                val inputPayload = pbkdf2Hash(input, salt)
                return if (inputPayload == stored) {
                    true
                } else {
                    recordFailedAttempt(context)
                    false
                }
            }
        }

        // Legacy format: raw SHA-256 hex string (no salt)
        if (stored.length == 64 && stored.all { it.isDigit() || it in 'a'..'f' }) {
            if (stored == sha256Legacy(input)) {
                // Transparent upgrade to modern hash
                setDetoxPassword(context, input)
                return true
            } else {
                recordFailedAttempt(context)
                return false
            }
        }

        recordFailedAttempt(context)
        return false
    }

    // ── Failed-attempt counter ─────────────────────────────────────

    /** Returns whether the user is currently locked out from password attempts. */
    fun isPasswordLockedOut(context: Context): Boolean =
        getFailedAttempts(context) >= 4

    /** Records a failed attempt and returns the new total within the sliding window. */
    fun recordFailedAttempt(context: Context): Int {
        val now = System.currentTimeMillis()
        val last = prefs(context).getLong(KEY_LAST_FAILED_TIME, 0L)
        val current = if (now - last > ATTEMPT_WINDOW_MS) 1 else {
            prefs(context).getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        }
        prefs(context).edit()
            .putInt(KEY_FAILED_ATTEMPTS, current)
            .putLong(KEY_LAST_FAILED_TIME, now)
            .apply()
        return current
    }

    fun getFailedAttempts(context: Context): Int {
        val now = System.currentTimeMillis()
        val last = prefs(context).getLong(KEY_LAST_FAILED_TIME, 0L)
        return if (now - last > ATTEMPT_WINDOW_MS) 0
        else prefs(context).getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    fun clearFailedAttempts(context: Context) {
        prefs(context).edit()
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LAST_FAILED_TIME)
            .apply()
    }

    // ── Blocked apps ───────────────────────────────────────────────

    fun getBlockedApps(context: Context): Set<String> {
        val raw = getEncryptedString(context, KEY_BLOCKED_APPS, null) ?: return emptySet()
        return raw.split("\n").filter { it.isNotBlank() }.toSet()
    }

    fun setAppBlockedState(context: Context, packageName: String, blocked: Boolean) {
        val current = getBlockedApps(context).toMutableSet()
        if (blocked) current.add(packageName) else current.remove(packageName)
        val raw = if (current.isEmpty()) null else current.joinToString("\n")
        putEncryptedString(context, KEY_BLOCKED_APPS, raw)
    }

    // ── Work keywords ──────────────────────────────────────────────

    /**
     * Returns the set of user-configured keywords that trigger app blocking in Work profile.
     * If the user has never configured keywords, the default hardcoded list is returned.
     */
    fun getWorkKeywords(context: Context): Set<String> {
        val raw = getEncryptedString(context, KEY_WORK_KEYWORDS, null)
        return if (raw.isNullOrBlank()) DEFAULT_WORK_KEYWORDS else raw.split("\n").filter { it.isNotBlank() }.toSet()
    }

    /**
     * Persists the set of keywords that should block apps in Work profile.
     * Each keyword is stored newline-delimited and encrypted at rest.
     */
    fun setWorkKeywords(context: Context, keywords: Set<String>) {
        val raw = if (keywords.isEmpty()) null else keywords.joinToString("\n")
        putEncryptedString(context, KEY_WORK_KEYWORDS, raw)
    }

    // ── App blocking logic (public API delegates to pure functions) ─

    fun isDetoxActive(context: Context): Boolean {
        return isDetoxActivePure(
            profile = getProfile(context),
            detoxEndTime = getDetoxEndTime(context),
            now = System.currentTimeMillis()
        )
    }

    fun expireDetoxIfNeeded(context: Context) {
        val newProfile = expireDetoxIfNeededPure(
            profile = getProfile(context),
            detoxEndTime = getDetoxEndTime(context),
            now = System.currentTimeMillis()
        )
        if (newProfile != getProfile(context)) {
            setProfile(context, newProfile)
        }
    }

    fun isAppBlocked(context: Context, packageName: String): Boolean {
        return isAppBlockedPure(
            profile = getProfile(context),
            detoxEndTime = getDetoxEndTime(context),
            blockedApps = getBlockedApps(context),
            workKeywords = getWorkKeywords(context),
            packageName = packageName,
            ownPackage = context.packageName,
            now = System.currentTimeMillis()
        )
    }

    // ── Pure logic (testable without Context or Android framework) ─

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun isAppBlockedPure(
        profile: String,
        detoxEndTime: Long,
        blockedApps: Set<String>,
        workKeywords: Set<String>,
        packageName: String,
        ownPackage: String,
        now: Long
    ): Boolean {
        if (profile == PROFILE_NORMAL) return false

        if (isDetoxActivePure(profile, detoxEndTime, now)) {
            return packageName != ownPackage
        }

        if (profile == PROFILE_WORK) {
            if (blockedApps.contains(packageName)) return true
            val pkgLower = packageName.lowercase()
            return workKeywords.any { pkgLower.contains(it) }
        }

        return false
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun isDetoxActivePure(profile: String, detoxEndTime: Long, now: Long): Boolean {
        if (profile != PROFILE_DETOX) return false
        val remaining = detoxEndTime - now
        return remaining > 0
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun expireDetoxIfNeededPure(profile: String, detoxEndTime: Long, now: Long): String {
        return if (profile == PROFILE_DETOX && detoxEndTime - now <= 0) {
            PROFILE_NORMAL
        } else {
            profile
        }
    }

    // ── Cryptographic helpers ──────────────────────────────────────

    private fun pbkdf2Hash(password: String, existingSalt: ByteArray? = null): String {
        val salt = existingSalt ?: ByteArray(SALT_LENGTH_BYTES).apply {
            SecureRandom().nextBytes(this)
        }
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        return "$saltB64:$hashB64"
    }

    private fun sha256Legacy(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ── Secure storage primitives ──────────────────────────────────

    private fun getEncryptedString(context: Context, key: String, default: String?): String? {
        val encrypted = try {
            prefs(context).getString(key, null)
        } catch (e: ClassCastException) {
            // Legacy value stored as a different primitive type (e.g. Long or Set).
            // Remove the stale key so the next write persists in the correct format.
            prefs(context).edit().remove(key).apply()
            null
        } ?: return default
        return SecureStorageHelper.decrypt(context, encrypted) ?: default
    }

    private fun putEncryptedString(context: Context, key: String, value: String?) {
        if (value == null) {
            prefs(context).edit().remove(key).apply()
        } else {
            val encrypted = SecureStorageHelper.encrypt(context, value)
            if (encrypted != null) {
                prefs(context).edit().putString(key, encrypted).apply()
            }
        }
    }

    private fun getEncryptedLong(context: Context, key: String, default: Long): Long {
        return getEncryptedString(context, key, null)?.toLongOrNull() ?: default
    }

    private fun putEncryptedLong(context: Context, key: String, value: Long) {
        putEncryptedString(context, key, value.toString())
    }
}
