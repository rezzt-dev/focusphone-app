package app.focusphone.security

import android.content.Context

/**
 * The single wrong-password policy shared by every FocusPhone lock (detox sessions and
 * app schedules).
 *
 * After [maxFailedAttempts] consecutive wrong passwords the lock enters a
 * [lockoutDurationMs] cooldown during which every attempt is refused — including the
 * correct password. Nothing is ever erased on failure: clearing the password or the
 * protected configuration would turn the lock into a bypass reachable in seconds. A detox
 * session can still never trap the user, because it always ends when its timer expires.
 *
 * The counter and the cooldown deadline are persisted through [AttemptStore], which in
 * production is encrypted at rest, so they survive process death and cannot be reset by
 * editing a plain-text preference file.
 */
class PasswordAttemptPolicy(
    private val store: AttemptStore,
    val maxFailedAttempts: Int = DEFAULT_MAX_FAILED_ATTEMPTS,
    val lockoutDurationMs: Long = DEFAULT_LOCKOUT_DURATION_MS,
    private val clock: () -> Long = System::currentTimeMillis
) {

    /** Persistence boundary; keeps the policy testable without Android Keystore. */
    interface AttemptStore {
        fun failedAttempts(): Int
        fun lockoutUntil(): Long
        fun write(failedAttempts: Int, lockoutUntil: Long)
        fun clear()
    }

    /** Milliseconds left before attempts are accepted again (0 when unlocked). */
    fun lockoutRemainingMs(): Long = (store.lockoutUntil() - clock()).coerceAtLeast(0L)

    /** Whether attempts are currently refused. */
    fun isLockedOut(): Boolean = lockoutRemainingMs() > 0L

    /** Attempts left before the cooldown starts; 0 while the cooldown is running. */
    fun remainingAttempts(): Int =
        if (isLockedOut()) 0
        else (maxFailedAttempts - store.failedAttempts()).coerceIn(0, maxFailedAttempts)

    /**
     * Records a wrong password.
     *
     * @return true when this failure started the cooldown.
     */
    fun recordFailure(): Boolean {
        val attempts = store.failedAttempts() + 1
        return if (attempts >= maxFailedAttempts) {
            // The counter restarts so the next budget begins once the cooldown is over.
            store.write(failedAttempts = 0, lockoutUntil = clock() + lockoutDurationMs)
            true
        } else {
            store.write(failedAttempts = attempts, lockoutUntil = store.lockoutUntil())
            false
        }
    }

    /** Forgets every failure; called after a successful attempt or a password change. */
    fun reset() = store.clear()

    companion object {
        const val DEFAULT_MAX_FAILED_ATTEMPTS = 5
        const val DEFAULT_LOCKOUT_DURATION_MS = 15 * 60 * 1000L

        /** A policy whose state lives in [prefs] under the two given keys. */
        fun encrypted(
            context: Context,
            prefs: EncryptedPrefs,
            attemptsKey: String,
            lockoutKey: String
        ): PasswordAttemptPolicy = PasswordAttemptPolicy(
            object : AttemptStore {
                override fun failedAttempts(): Int = prefs.getInt(context, attemptsKey, 0)
                override fun lockoutUntil(): Long = prefs.getLong(context, lockoutKey, 0L)
                override fun write(failedAttempts: Int, lockoutUntil: Long) {
                    prefs.putInt(context, attemptsKey, failedAttempts)
                    prefs.putLong(context, lockoutKey, lockoutUntil)
                }
                override fun clear() = prefs.remove(context, attemptsKey, lockoutKey)
            }
        )
    }
}
