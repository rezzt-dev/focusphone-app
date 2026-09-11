package app.focusphone.data.preferences

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import app.focusphone.security.EncryptedPrefs
import app.focusphone.security.PasswordAttemptPolicy
import app.focusphone.security.PasswordHasher

/**
 * Synchronous preference manager for Contextual Profiles and Detox app blocking.
 *
 * Security guarantees:
 *  - Every value in `focusphone_profile` — profile, detox timer, password hash, blocked
 *    apps, work keywords and the wrong-password counter — is encrypted at rest via
 *    [EncryptedPrefs] (AES-256/GCM + Android Keystore).
 *  - Detox passwords are hashed by [PasswordHasher] with PBKDF2-HMAC-SHA256 using a
 *    128-bit random salt and 100 000 iterations. The legacy SHA-256 (no salt) format is
 *    detected and transparently upgraded on successful verification.
 *  - Wrong passwords follow the shared [PasswordAttemptPolicy]: after
 *    [PasswordAttemptPolicy.DEFAULT_MAX_FAILED_ATTEMPTS] failures every attempt is refused
 *    for a cooldown. The session is never disabled on failure — a detox session always
 *    ends on its own when the timer expires, so nobody can be trapped, and wrong guesses
 *    can never be used to end it early.
 */
object ProfilePreferences {

    private const val PREF_FILE = "focusphone_profile"
    private const val KEY_PROFILE = "current_profile"
    private const val KEY_DETOX_END = "detox_end_time"
    private const val KEY_DETOX_PASSWORD = "detox_password"
    private const val KEY_BLOCKED_APPS = "blocked_apps"
    private const val KEY_WORK_KEYWORDS = "work_keywords"
    private const val EMPTY_WORK_KEYWORDS = "__focusphone_empty_work_keywords__"
    private const val KEY_ATTEMPTS = "detox_attempts"
    private const val KEY_LOCKOUT_UNTIL = "detox_lockout_until"

    /** Plain-text counters written by builds before the shared attempt policy. */
    private val LEGACY_PLAIN_KEYS = arrayOf("detox_failed_attempts", "detox_last_failed_time")

    const val PROFILE_NORMAL = "normal"
    const val PROFILE_WORK = "work"
    const val PROFILE_DETOX = "detox"

    /** Minimum length accepted when the user creates a detox password. */
    const val MIN_PASSWORD_LENGTH = 4

    private val DEFAULT_WORK_KEYWORDS = setOf(
        "instagram", "facebook", "twitter", "tiktok", "youtube", "snapchat",
        "netflix", "pinterest", "twitch", "discord", "reddit", "linkedin",
        "clash", "candycrush", "pubg", "fortnite"
    )

    private val store = EncryptedPrefs(PREF_FILE)

    private fun attemptPolicy(context: Context) = PasswordAttemptPolicy.encrypted(
        context = context.applicationContext,
        prefs = store,
        attemptsKey = KEY_ATTEMPTS,
        lockoutKey = KEY_LOCKOUT_UNTIL
    )

    /**
     * Removes the plain-text attempt counters left by older builds. Idempotent and cheap;
     * run once per process start.
     */
    fun migrateLegacyKeys(context: Context) {
        val prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        if (LEGACY_PLAIN_KEYS.any { prefs.contains(it) }) {
            prefs.edit { LEGACY_PLAIN_KEYS.forEach { remove(it) } }
        }
    }

    // ── Profile ────────────────────────────────────────────────────

    fun getProfile(context: Context): String =
        store.getString(context, KEY_PROFILE, PROFILE_NORMAL) ?: PROFILE_NORMAL

    fun setProfile(context: Context, profile: String) {
        store.putString(context, KEY_PROFILE, profile)
    }

    // ── Detox timer ────────────────────────────────────────────────

    fun getDetoxEndTime(context: Context): Long =
        store.getLong(context, KEY_DETOX_END, 0L)

    fun setDetoxEndTime(context: Context, endTimeMs: Long) {
        store.putLong(context, KEY_DETOX_END, endTimeMs)
    }

    /**
     * Starts a detox session lasting [durationMs], protected by [password].
     *
     * @return false when the session could not be stored securely, in which case nothing
     * was activated.
     */
    fun startDetoxSession(context: Context, password: String, durationMs: Long): Boolean {
        if (!store.putString(context, KEY_DETOX_PASSWORD, PasswordHasher.hash(password))) return false
        attemptPolicy(context).reset()
        store.putLong(context, KEY_DETOX_END, System.currentTimeMillis() + durationMs)
        store.putString(context, KEY_PROFILE, PROFILE_DETOX)
        return true
    }

    /** Ends the current detox session, switching to [nextProfile]. */
    fun endDetoxSession(context: Context, nextProfile: String = PROFILE_NORMAL) {
        store.putString(context, KEY_DETOX_PASSWORD, null)
        store.putLong(context, KEY_DETOX_END, 0L)
        attemptPolicy(context).reset()
        store.putString(context, KEY_PROFILE, nextProfile)
    }

    // ── Detox password (PBKDF2 + salt) ─────────────────────────────

    /** Returns the stored PBKDF2 hash payload, or null if none is set. */
    fun getDetoxPassword(context: Context): String? =
        store.getString(context, KEY_DETOX_PASSWORD, null)

    /** Stores the password as a PBKDF2 hash with random salt. Pass null to clear. */
    fun setDetoxPassword(context: Context, password: String?) {
        store.putString(context, KEY_DETOX_PASSWORD, password?.let { PasswordHasher.hash(it) })
        attemptPolicy(context).reset()
    }

    /**
     * Verifies [input] against the stored hash under the shared attempt policy.
     *
     * Returns false without consuming an attempt while the cooldown is active. A
     * successful attempt clears the failure counter; a failed one consumes an attempt.
     * Nothing else changes here: ending the session is the caller's decision.
     *
     * PBKDF2 is deliberately slow; call this off the main thread.
     */
    fun verifyDetoxPassword(context: Context, input: String): Boolean {
        val policy = attemptPolicy(context)
        // Enforced here rather than only in the UI so no caller can spend attempts
        // while the cooldown is running.
        if (policy.isLockedOut()) return false

        val stored = getDetoxPassword(context) ?: return false

        return when (PasswordHasher.verify(stored, input)) {
            PasswordHasher.VerifyResult.Match -> {
                policy.reset()
                true
            }

            PasswordHasher.VerifyResult.MatchNeedsUpgrade -> {
                // Transparent upgrade of the legacy unsalted SHA-256 hash.
                store.putString(context, KEY_DETOX_PASSWORD, PasswordHasher.hash(input))
                policy.reset()
                true
            }

            PasswordHasher.VerifyResult.Mismatch -> {
                policy.recordFailure()
                false
            }
        }
    }

    // ── Failed-attempt counter ─────────────────────────────────────

    /** Whether password attempts are currently refused because of the cooldown. */
    fun isPasswordLockedOut(context: Context): Boolean = attemptPolicy(context).isLockedOut()

    /** Milliseconds left before password attempts are accepted again (0 when unlocked). */
    fun getLockoutRemainingMs(context: Context): Long = attemptPolicy(context).lockoutRemainingMs()

    /** Attempts remaining before the cooldown starts. */
    fun getRemainingAttempts(context: Context): Int = attemptPolicy(context).remainingAttempts()

    fun clearFailedAttempts(context: Context) = attemptPolicy(context).reset()

    // ── Blocked apps ───────────────────────────────────────────────

    fun getBlockedApps(context: Context): Set<String> {
        val raw = store.getString(context, KEY_BLOCKED_APPS, null) ?: return emptySet()
        return raw.split("\n").filter { it.isNotBlank() }.toSet()
    }

    /**
     * Adds or removes [packageName] from the manual block list.
     *
     * @return false when the current list is temporarily unreadable or the new one could
     * not be written. A list that cannot be read right now is never replaced, since that
     * would silently unblock every other app.
     */
    fun setAppBlockedState(context: Context, packageName: String, blocked: Boolean): Boolean {
        val current = when (val stored = store.read(context, KEY_BLOCKED_APPS)) {
            is EncryptedPrefs.StoredValue.Present ->
                stored.value.split("\n").filter { it.isNotBlank() }.toMutableSet()
            EncryptedPrefs.StoredValue.Absent, EncryptedPrefs.StoredValue.Lost -> mutableSetOf()
            EncryptedPrefs.StoredValue.Unavailable -> return false
        }
        if (blocked) current.add(packageName) else current.remove(packageName)
        val raw = if (current.isEmpty()) null else current.joinToString("\n")
        return store.putString(context, KEY_BLOCKED_APPS, raw)
    }

    // ── Work keywords ──────────────────────────────────────────────

    /**
     * Returns the set of user-configured keywords that trigger app blocking in Work profile.
     * If the user has never configured keywords, the default hardcoded list is returned.
     */
    fun getWorkKeywords(context: Context): Set<String> {
        val raw = store.getString(context, KEY_WORK_KEYWORDS, null)
        return workKeywordsFromStored(raw)
    }

    /**
     * Persists the set of keywords that should block apps in Work profile.
     * Each keyword is stored newline-delimited and encrypted at rest.
     */
    fun setWorkKeywords(context: Context, keywords: Set<String>) {
        // A non-empty encrypted marker means "the user configured no keywords". An
        // actual empty plaintext cannot be used because the encrypted payload validator
        // intentionally rejects zero-byte ciphertexts as malformed.
        val raw = if (keywords.isEmpty()) EMPTY_WORK_KEYWORDS else keywords.joinToString("\n")
        store.putString(context, KEY_WORK_KEYWORDS, raw)
    }

    /** Converts the stored value while preserving the absent-vs-empty distinction. */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun workKeywordsFromStored(raw: String?): Set<String> = when (raw) {
        null -> DEFAULT_WORK_KEYWORDS
        EMPTY_WORK_KEYWORDS -> emptySet()
        else -> raw.split("\n").filter { it.isNotBlank() }.toSet()
    }

    // ── App blocking logic (public API delegates to pure functions) ─

    fun isDetoxActive(context: Context): Boolean {
        return isDetoxActivePure(
            profile = getProfile(context),
            detoxEndTime = getDetoxEndTime(context),
            now = System.currentTimeMillis()
        )
    }

    /** Ends a detox session whose timer has run out. */
    fun expireDetoxIfNeeded(context: Context) {
        val profile = getProfile(context)
        val newProfile = expireDetoxIfNeededPure(
            profile = profile,
            detoxEndTime = getDetoxEndTime(context),
            now = System.currentTimeMillis()
        )
        if (newProfile != profile) {
            endDetoxSession(context, newProfile)
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

    /** Whether a detox session is running at [now]. Public so hot paths can cache inputs. */
    fun isDetoxActivePure(profile: String, detoxEndTime: Long, now: Long): Boolean {
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
}
