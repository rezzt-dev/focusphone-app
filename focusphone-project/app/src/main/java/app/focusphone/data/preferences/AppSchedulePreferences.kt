package app.focusphone.data.preferences

import android.content.Context
import androidx.annotation.VisibleForTesting
import app.focusphone.domain.model.AppScheduleRule
import app.focusphone.domain.model.ScheduleBlockReason
import app.focusphone.domain.util.AppScheduleEvaluator
import app.focusphone.security.EncryptedPrefs
import app.focusphone.security.PasswordAttemptPolicy
import app.focusphone.security.PasswordHasher
import java.util.Calendar

/**
 * Synchronous preference manager for schedule-based app blocking.
 *
 * Stores three kinds of data, all encrypted at rest with AES-256/GCM through
 * [EncryptedPrefs]:
 *  - the per-app [AppScheduleRule] set,
 *  - the password that guards those rules (PBKDF2 hash, never plain text),
 *  - the per-app usage counters of the current calendar day.
 *
 * **Lockout policy.** Wrong passwords follow the shared [PasswordAttemptPolicy], exactly
 * like detox: after [MAX_FAILED_ATTEMPTS] failures every attempt is refused for
 * [LOCKOUT_DURATION_MS] and nothing is erased, because clearing the rules on failed
 * attempts would turn the lock into a one-minute bypass.
 *
 * **Unreadable storage.** While the Keystore key is temporarily unavailable, writers refuse
 * to run instead of treating the rules as empty — otherwise the next save would replace
 * every rule the user configured with a single one. Rules that can never be decrypted
 * again are treated as gone, so the user can configure them anew.
 */
object AppSchedulePreferences {

    private const val PREF_FILE = "focusphone_schedule"

    private const val KEY_RULES = "schedule_rules"
    private const val KEY_PASSWORD = "schedule_password"
    private const val KEY_FAILED_ATTEMPTS = "schedule_failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "schedule_lockout_until"
    private const val KEY_USAGE_DAY = "schedule_usage_day"
    private const val KEY_USAGE_COUNTERS = "schedule_usage_counters"

    /** Wrong attempts tolerated before the settings screen enters its cooldown. */
    const val MAX_FAILED_ATTEMPTS = PasswordAttemptPolicy.DEFAULT_MAX_FAILED_ATTEMPTS

    /** How long the schedule settings stay locked after [MAX_FAILED_ATTEMPTS]. */
    const val LOCKOUT_DURATION_MS = PasswordAttemptPolicy.DEFAULT_LOCKOUT_DURATION_MS

    /** Minimum length accepted when the user creates the schedule password. */
    const val MIN_PASSWORD_LENGTH = 4

    private val store = EncryptedPrefs(PREF_FILE)

    private fun attemptPolicy(context: Context) = PasswordAttemptPolicy.encrypted(
        context = context.applicationContext,
        prefs = store,
        attemptsKey = KEY_FAILED_ATTEMPTS,
        lockoutKey = KEY_LOCKOUT_UNTIL
    )

    // ── Rules ──────────────────────────────────────────────────────

    /** Returns every stored rule, skipping entries that fail to parse. */
    fun getRules(context: Context): List<AppScheduleRule> = readRules(context) ?: emptyList()

    /** The stored rules, or null while they exist but cannot be decrypted yet. */
    private fun readRules(context: Context): List<AppScheduleRule>? =
        when (val stored = store.read(context, KEY_RULES)) {
            EncryptedPrefs.StoredValue.Absent, EncryptedPrefs.StoredValue.Lost -> emptyList()
            EncryptedPrefs.StoredValue.Unavailable -> null
            is EncryptedPrefs.StoredValue.Present -> stored.value.split("\n")
                .filter { it.isNotBlank() }
                .mapNotNull { AppScheduleEvaluator.parse(it) }
        }

    /** Returns the rule for [packageName], or null when the app is unrestricted. */
    fun getRule(context: Context, packageName: String): AppScheduleRule? =
        getRules(context).firstOrNull { it.packageName == packageName }

    /**
     * Persists [rule], replacing any existing rule for the same package.
     * A rule that restricts nothing is removed instead of stored.
     *
     * @return false when nothing was saved because the stored rules are unreadable or
     * the write failed.
     */
    fun saveRule(context: Context, rule: AppScheduleRule): Boolean {
        val current = readRules(context) ?: return false
        val updated = current.filterNot { it.packageName == rule.packageName }.toMutableList()
        if (rule.isActive) updated.add(rule)
        return writeRules(context, updated)
    }

    /**
     * Removes the rule for [packageName] and its accumulated usage for today.
     *
     * @return false when nothing was removed because the stored rules are unreadable or
     * the write failed.
     */
    fun removeRule(context: Context, packageName: String): Boolean {
        val current = readRules(context) ?: return false
        if (!writeRules(context, current.filterNot { it.packageName == packageName })) return false
        readUsageCounters(context)?.let { counters ->
            if (packageName in counters) writeUsageCounters(context, counters - packageName)
        }
        return true
    }

    private fun writeRules(context: Context, rules: List<AppScheduleRule>): Boolean {
        val raw = rules
            .sortedBy { it.packageName }
            .joinToString("\n") { AppScheduleEvaluator.serialize(it) }
        return store.putString(context, KEY_RULES, raw.ifBlank { null })
    }

    // ── Blocking decision ──────────────────────────────────────────

    /**
     * Returns why [packageName] is blocked right now, or null when it may be opened.
     * Apps without a rule are never blocked by this feature.
     */
    fun getBlockReason(context: Context, packageName: String): ScheduleBlockReason? {
        val rule = getRule(context, packageName) ?: return null
        return evaluateRule(context, rule)
    }

    /** Evaluates an already-loaded [rule] against the current time and usage. */
    fun evaluateRule(context: Context, rule: AppScheduleRule): ScheduleBlockReason? {
        val calendar = Calendar.getInstance()
        return AppScheduleEvaluator.evaluate(
            rule = rule,
            minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE),
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
            usedMinutesToday = getUsedMinutes(context, rule.packageName)
        )
    }

    // ── Daily usage counters ───────────────────────────────────────

    /** Minutes [packageName] has been used during the current calendar day. */
    fun getUsedMinutes(context: Context, packageName: String): Int =
        getUsedSeconds(context, packageName) / 60

    /** Seconds [packageName] has been used during the current calendar day. */
    fun getUsedSeconds(context: Context, packageName: String): Int =
        readUsageCounters(context)?.get(packageName) ?: 0

    /**
     * Adds [seconds] of foreground time to [packageName]'s counter for today.
     * Counters from a previous day are discarded first, which is what resets every
     * daily allowance at local midnight. Unreadable counters are left untouched rather
     * than reset, so a storage hiccup can never hand out a fresh allowance.
     */
    fun addUsageSeconds(context: Context, packageName: String, seconds: Int) {
        if (seconds <= 0) return
        val counters = readUsageCounters(context)?.toMutableMap() ?: return
        counters[packageName] = (counters[packageName] ?: 0) + seconds
        writeUsageCounters(context, counters)
    }

    /**
     * Seconds of allowance left for [rule] today, or null when it has no daily limit.
     * A fully consumed allowance returns 0.
     */
    fun getRemainingLimitSeconds(context: Context, rule: AppScheduleRule): Int? {
        if (!rule.hasDailyLimit) return null
        val remaining = rule.dailyLimitMinutes * 60 - getUsedSeconds(context, rule.packageName)
        return remaining.coerceAtLeast(0)
    }

    /** Today's counters, an empty map on a new day, or null while they cannot be read yet. */
    private fun readUsageCounters(context: Context): Map<String, Int>? {
        val storedDay = store.getInt(context, KEY_USAGE_DAY, 0)
        if (storedDay != currentDayStamp()) return emptyMap()

        val raw = when (val stored = store.read(context, KEY_USAGE_COUNTERS)) {
            EncryptedPrefs.StoredValue.Absent, EncryptedPrefs.StoredValue.Lost -> return emptyMap()
            EncryptedPrefs.StoredValue.Unavailable -> return null
            is EncryptedPrefs.StoredValue.Present -> stored.value
        }
        return raw.split("\n")
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val separator = line.lastIndexOf('=')
                if (separator <= 0) return@mapNotNull null
                val seconds = line.substring(separator + 1).toIntOrNull() ?: return@mapNotNull null
                line.substring(0, separator) to seconds
            }
            .toMap()
    }

    private fun writeUsageCounters(context: Context, counters: Map<String, Int>) {
        val raw = counters.entries.joinToString("\n") { "${it.key}=${it.value}" }
        store.putInt(context, KEY_USAGE_DAY, currentDayStamp())
        store.putString(context, KEY_USAGE_COUNTERS, raw.ifBlank { null })
    }

    /** Local calendar day encoded as `yyyyMMdd`, used to expire usage counters. */
    private fun currentDayStamp(): Int = dayStampOf(Calendar.getInstance())

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun dayStampOf(calendar: Calendar): Int =
        calendar.get(Calendar.YEAR) * 10_000 +
            (calendar.get(Calendar.MONTH) + 1) * 100 +
            calendar.get(Calendar.DAY_OF_MONTH)

    // ── Password protection ────────────────────────────────────────

    /**
     * Whether a password has been set for editing schedules.
     *
     * A password that is only temporarily unreadable still counts, so the screen stays
     * locked instead of offering to create a new one on top of it.
     */
    fun hasPassword(context: Context): Boolean = when (store.read(context, KEY_PASSWORD)) {
        EncryptedPrefs.StoredValue.Absent, EncryptedPrefs.StoredValue.Lost -> false
        EncryptedPrefs.StoredValue.Unavailable, is EncryptedPrefs.StoredValue.Present -> true
    }

    /**
     * Stores [password] as a PBKDF2 hash. Passing null clears the protection.
     *
     * @return false when the new password could not be stored.
     */
    fun setPassword(context: Context, password: String?): Boolean {
        val saved = store.putString(context, KEY_PASSWORD, password?.let { PasswordHasher.hash(it) })
        if (saved) attemptPolicy(context).reset()
        return saved
    }

    /**
     * Verifies [input] against the stored hash.
     *
     * Returns false without consuming an attempt while the cooldown is active.
     * A successful attempt clears the failure counter; a failed one consumes an attempt.
     * Neither the password nor the rules are ever cleared here — see the class
     * documentation. PBKDF2 is deliberately slow; call this off the main thread.
     */
    fun verifyPassword(context: Context, input: String): Boolean {
        val policy = attemptPolicy(context)
        // Enforced here rather than only in the UI so no caller can spend attempts
        // while the cooldown is running.
        if (policy.isLockedOut()) return false

        val stored = store.getString(context, KEY_PASSWORD, null) ?: return false

        return when (PasswordHasher.verify(stored, input)) {
            PasswordHasher.VerifyResult.Match -> {
                policy.reset()
                true
            }

            PasswordHasher.VerifyResult.MatchNeedsUpgrade -> {
                store.putString(context, KEY_PASSWORD, PasswordHasher.hash(input))
                policy.reset()
                true
            }

            PasswordHasher.VerifyResult.Mismatch -> {
                policy.recordFailure()
                false
            }
        }
    }

    /** Milliseconds left before password attempts are accepted again (0 when unlocked). */
    fun getLockoutRemainingMs(context: Context): Long = attemptPolicy(context).lockoutRemainingMs()

    /** Whether password attempts are currently rejected because of the cooldown. */
    fun isLockedOut(context: Context): Boolean = attemptPolicy(context).isLockedOut()

    /** Attempts remaining before the cooldown starts; 0 while it is running. */
    fun getRemainingAttempts(context: Context): Int = attemptPolicy(context).remainingAttempts()
}
