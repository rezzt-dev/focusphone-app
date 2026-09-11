package app.focusphone.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordAttemptPolicyTest {

    private var now = 1_000_000L

    private class MemoryStore : PasswordAttemptPolicy.AttemptStore {
        var attempts = 0
        var until = 0L
        override fun failedAttempts() = attempts
        override fun lockoutUntil() = until
        override fun write(failedAttempts: Int, lockoutUntil: Long) {
            attempts = failedAttempts
            until = lockoutUntil
        }
        override fun clear() {
            attempts = 0
            until = 0L
        }
    }

    private val store = MemoryStore()
    private val policy = PasswordAttemptPolicy(
        store = store,
        maxFailedAttempts = 5,
        lockoutDurationMs = 15 * 60_000L,
        clock = { now }
    )

    @Test
    fun `failures below the threshold only consume attempts`() {
        repeat(4) { assertFalse(policy.recordFailure()) }
        assertFalse(policy.isLockedOut())
        assertEquals(1, policy.remainingAttempts())
    }

    @Test
    fun `reaching the threshold starts a cooldown instead of unlocking`() {
        repeat(4) { policy.recordFailure() }
        assertTrue(policy.recordFailure())
        assertTrue(policy.isLockedOut())
        assertEquals(15 * 60_000L, policy.lockoutRemainingMs())
    }

    @Test
    fun `remaining attempts read zero while the cooldown runs`() {
        repeat(5) { policy.recordFailure() }
        assertEquals(0, policy.remainingAttempts())
    }

    @Test
    fun `a sixth wrong password does not escape the lock`() {
        repeat(6) { policy.recordFailure() }
        assertTrue(policy.isLockedOut())
    }

    @Test
    fun `a full budget is available again once the cooldown expires`() {
        repeat(5) { policy.recordFailure() }
        now += 15 * 60_000L
        assertFalse(policy.isLockedOut())
        assertEquals(5, policy.remainingAttempts())
    }

    @Test
    fun `reset forgets failures and cooldown`() {
        repeat(5) { policy.recordFailure() }
        policy.reset()
        assertFalse(policy.isLockedOut())
        assertEquals(5, policy.remainingAttempts())
    }
}
