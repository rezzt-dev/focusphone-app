package app.focusphone.data.preferences

import android.content.Context
import androidx.annotation.VisibleForTesting
import app.focusphone.security.EncryptedPrefs
import app.focusphone.security.PasswordHasher

/**
 * Synchronous preference manager for Contextual Profiles and Detox app blocking.
 *
 * Security guarantees:
 *  - Sensitive values (profile, detox timers, password hash, blocked apps, work keywords) are
 *    encrypted at rest via [EncryptedPrefs] (AES-256/GCM + Android Keystore).
 *  - Detox passwords are hashed by [PasswordHasher] with PBKDF2-HMAC-SHA256 using a
 *    128-bit random salt and 100 000 iterations. The legacy SHA-256 (no salt) format is
 *    detected and transparently upgraded on successful verification.
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

    private const val ATTEMPT_WINDOW_MS = 30 * 60 * 1000L // 30 minutes

    private val DEFAULT_WORK_KEYWORDS = setOf(
        "instagram", "facebook", "twitter", "tiktok", "youtube", "snapchat",
        "netflix", "pinterest", "twitch", "discord", "reddit", "linkedin",
        "clash", "candycrush", "pubg", "fortnite"
    )

    private val store = EncryptedPrefs(PREF_FILE)

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
        putEncryptedString(context, KEY_DETOX_PASSWORD, password?.let { PasswordHasher.hash(it) })
    }

    /** Hashes input and compares with stored hash. Supports transparent migration from legacy SHA-256. */
    fun verifyDetoxPassword(context: Context, input: String): Boolean {
        val stored = getDetoxPassword(context) ?: return false

        return when (PasswordHasher.verify(stored, input)) {
            PasswordHasher.VerifyResult.Match -> true

            PasswordHasher.VerifyResult.MatchNeedsUpgrade -> {
                // Transparent upgrade of the legacy unsalted SHA-256 hash.
                setDetoxPassword(context, input)
                true
            }

            PasswordHasher.VerifyResult.Mismatch -> {
                recordFailedAttempt(context)
                false
            }
        }
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

    // ── Secure storage primitives ──────────────────────────────────

    private fun getEncryptedString(context: Context, key: String, default: String?): String? =
        store.getString(context, key, default)

    private fun putEncryptedString(context: Context, key: String, value: String?) =
        store.putString(context, key, value)

    private fun getEncryptedLong(context: Context, key: String, default: Long): Long =
        store.getLong(context, key, default)

    private fun putEncryptedLong(context: Context, key: String, value: Long) =
        store.putLong(context, key, value)
}
