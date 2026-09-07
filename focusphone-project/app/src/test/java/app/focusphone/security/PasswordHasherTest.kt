package app.focusphone.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

/**
 * Unit tests for the shared password hashing used by detox locks and app schedules.
 *
 * Robolectric is required because the payload encoding relies on `android.util.Base64`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class PasswordHasherTest {

    @Test
    fun `hash produces a salted two-part payload`() {
        val payload = PasswordHasher.hash("focus1234")
        val parts = payload.split(":")
        assertEquals(2, parts.size)
        assertTrue(parts.all { it.isNotBlank() })
    }

    @Test
    fun `the same password hashes differently every time`() {
        assertNotEquals(PasswordHasher.hash("focus1234"), PasswordHasher.hash("focus1234"))
    }

    @Test
    fun `verify accepts the correct password`() {
        val stored = PasswordHasher.hash("focus1234")
        assertEquals(PasswordHasher.VerifyResult.Match, PasswordHasher.verify(stored, "focus1234"))
    }

    @Test
    fun `verify rejects a wrong password`() {
        val stored = PasswordHasher.hash("focus1234")
        assertEquals(PasswordHasher.VerifyResult.Mismatch, PasswordHasher.verify(stored, "focus1235"))
    }

    @Test
    fun `verify reports legacy SHA-256 hashes as needing an upgrade`() {
        val legacy = sha256("focus1234")
        assertEquals(
            PasswordHasher.VerifyResult.MatchNeedsUpgrade,
            PasswordHasher.verify(legacy, "focus1234")
        )
        assertEquals(
            PasswordHasher.VerifyResult.Mismatch,
            PasswordHasher.verify(legacy, "wrong")
        )
    }

    @Test
    fun `verify treats an unreadable payload as a mismatch instead of crashing`() {
        assertEquals(PasswordHasher.VerifyResult.Mismatch, PasswordHasher.verify("garbage", "focus1234"))
        assertEquals(PasswordHasher.VerifyResult.Mismatch, PasswordHasher.verify("!!!:???", "focus1234"))
    }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
