package app.focusphone.data.preferences

import android.content.Context
import androidx.annotation.VisibleForTesting
import app.focusphone.domain.model.AppScheduleRule
import app.focusphone.domain.model.ScheduleBlockReason
import app.focusphone.domain.util.AppScheduleEvaluator
import app.focusphone.security.EncryptedPrefs
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
 * **Lockout policy.** Detox force-disables itself after repeated wrong passwords so a
 * user can never be trapped by a session they no longer remember the password for.
 * Schedules deliberately behave the *opposite* way: wrong attempts trigger a
 * [LOCKOUT_DURATION_MS] cooldown and nothing is erased, because clearing the rules on
 * failed attempts would turn the lock into a one-minute bypass. Rules are a standing
 * configuration, not a timed session, so waiting out the cooldown is always enough.
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
    const val MAX_FAILED_ATTEMPTS = 5

    /** How long the schedule settings stay locked after [MAX_FAILED_ATTEMPTS]. */
    const val LOCKOUT_DURATION_MS = 15 * 60 * 1000L

    /** Minimum length accepted when the user creates the schedule password. */
    const val MIN_PASSWORD_LENGTH = 4

    private val store = EncryptedPrefs(PREF_FILE)

    // ── Rules ──────────────────────────────────────────────────────

    /** Returns every stored rule, skipping entries that fail to parse. */
    fun getRules(context: Context): List<AppScheduleRule> {
        val raw = store.getString(context, KEY_RULES, null) ?: return emptyList()
        return raw.split("\n")
            .filter { it.isNotBlank() }
            .mapNotNull { AppScheduleEvaluator.parse(it) }
    }

    /** Returns the rule for [packageName], or null when the app is unrestricted. */
    fun getRule(context: Context, packageName: String): AppScheduleRule? =
        getRules(context).firstOrNull { it.packageName == packageName }

    /**
     * Persists [rule], replacing any existing rule for the same package.
     * A rule that restricts nothing is removed instead of stored.
     */
    fun saveRule(context: Context, rule: AppScheduleRule) {
        val updated = getRules(context).filterNot { it.packageName == rule.packageName }.toMutableList()
        if (rule.isActive) updated.add(rule)
        writeRules(context, updated)
    }

    /** Removes the rule for [packageName] and its accumulated usage for today. */
    fun removeRule(context: Context, packageName: String) {
        writeRules(context, getRules(context).filterNot { it.packageName == packageName })
        val counters = readUsageCounters(context).toMutableMap()
        if (counters.remove(packageName) != null) {
            writeUsageCounters(context, counters)
        }
    }

    private fun writeRules(context: Context, rules: List<AppScheduleRule>) {
        val raw = rules
            .sortedBy { it.packageName }
            .joinToString("\n") { AppScheduleEvaluator.serialize(it) }
        store.putString(context, KEY_RULES, raw.ifBlank { null })
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
        readUsageCounters(context)[packageName] ?: 0

    /**
     * Adds [seconds] of foreground time to [packageName]'s counter for today.
     * Counters from a previous day are discarded first, which is what resets every
     * daily allowance at local midnight.
     */
    fun addUsageSeconds(context: Context, packageName: String, seconds: Int) {
        if (seconds <= 0) return
        val counters = readUsageCounters(context).toMutableMap()
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

    private fun readUsageCounters(context: Context): Map<String, Int> {
        val storedDay = store.getInt(context, KEY_USAGE_DAY, 0)
        if (storedDay != currentDayStamp()) return emptyMap()

        val raw = store.getString(context, KEY_USAGE_COUNTERS, null) ?: return emptyMap()
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

    /** Whether a password has been set for editing schedules. */
    fun hasPassword(context: Context): Boolean =
        !store.getString(context, KEY_PASSWORD, null).isNullOrBlank()

    /** Stores [password] as a PBKDF2 hash. Passing null clears the protection. */
    fun setPassword(context: Context, password: String?) {
        store.putString(context, KEY_PASSWORD, password?.let { PasswordHasher.hash(it) })
        clearFailedAttempts(context)
    }

    /**
     * Verifies [input] against the stored hash.
     *
     * Returns false without consuming an attempt while the cooldown is active.
     * A successful attempt clears the failure counter; a failed one increments it and
     * starts the cooldown once [MAX_FAILED_ATTEMPTS] is reached. Neither the password
     * nor the rules are ever cleared here — see the class documentation.
     */
    fun verifyPassword(context: Context, input: String): Boolean {
        // Enforced here rather than only in the UI so no caller can spend attempts
        // while the cooldown is running.
        if (isLockedOut(context)) return false

        val stored = store.getString(context, KEY_PASSWORD, null) ?: return false

        return when (PasswordHasher.verify(stored, input)) {
            PasswordHasher.VerifyResult.Match -> {
                clearFailedAttempts(context)
                true
            }

            PasswordHasher.VerifyResult.MatchNeedsUpgrade -> {
                store.putString(context, KEY_PASSWORD, PasswordHasher.hash(input))
                clearFailedAttempts(context)
                true
            }

            PasswordHasher.VerifyResult.Mismatch -> {
                recordFailedAttempt(context)
                false
            }
        }
    }

    /** Milliseconds left before password attempts are accepted again (0 when unlocked). */
    fun getLockoutRemainingMs(context: Context): Long {
        val until = store.getLong(context, KEY_LOCKOUT_UNTIL, 0L)
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    /** Whether password attempts are currently rejected because of the cooldown. */
    fun isLockedOut(context: Context): Boolean = getLockoutRemainingMs(context) > 0L

    /** Attempts remaining before the cooldown starts. */
    fun getRemainingAttempts(context: Context): Int =
        (MAX_FAILED_ATTEMPTS - store.getInt(context, KEY_FAILED_ATTEMPTS, 0)).coerceAtLeast(0)

    private fun recordFailedAttempt(context: Context) {
        val attempts = store.getInt(context, KEY_FAILED_ATTEMPTS, 0) + 1
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            store.putInt(context, KEY_FAILED_ATTEMPTS, 0)
            store.putLong(context, KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
        } else {
            store.putInt(context, KEY_FAILED_ATTEMPTS, attempts)
        }
    }

    private fun clearFailedAttempts(context: Context) {
        store.remove(context, KEY_FAILED_ATTEMPTS, KEY_LOCKOUT_UNTIL)
    }
}
