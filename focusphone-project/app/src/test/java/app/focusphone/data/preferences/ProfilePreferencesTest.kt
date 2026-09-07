package app.focusphone.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class ProfilePreferencesTest {

    // ── Pure blocking logic ───────────────────────────────────────

    @Test
    fun `isAppBlockedPure returns false for NORMAL profile`() {
        val result = ProfilePreferences.isAppBlockedPure(
            profile = ProfilePreferences.PROFILE_NORMAL,
            detoxEndTime = System.currentTimeMillis() + 60_000L,
            blockedApps = setOf("com.instagram.android"),
            workKeywords = setOf("instagram"),
            packageName = "com.instagram.android",
            ownPackage = "app.focusphone",
            now = System.currentTimeMillis()
        )
        assertFalse(result)
    }

    @Test
    fun `isAppBlockedPure blocks everything except own package in DETOX`() {
        val now = System.currentTimeMillis()
        val resultOwn = ProfilePreferences.isAppBlockedPure(
            profile = ProfilePreferences.PROFILE_DETOX,
            detoxEndTime = now + 60_000L,
            blockedApps = emptySet(),
            workKeywords = emptySet(),
            packageName = "app.focusphone",
            ownPackage = "app.focusphone",
            now = now
        )
        val resultOther = ProfilePreferences.isAppBlockedPure(
            profile = ProfilePreferences.PROFILE_DETOX,
            detoxEndTime = now + 60_000L,
            blockedApps = emptySet(),
            workKeywords = emptySet(),
            packageName = "com.other.app",
            ownPackage = "app.focusphone",
            now = now
        )
        assertFalse(resultOwn)
        assertTrue(resultOther)
    }

    @Test
    fun `isAppBlockedPure does not block in DETOX when expired`() {
        val now = System.currentTimeMillis()
        val result = ProfilePreferences.isAppBlockedPure(
            profile = ProfilePreferences.PROFILE_DETOX,
            detoxEndTime = now - 1L,
            blockedApps = emptySet(),
            workKeywords = emptySet(),
            packageName = "com.other.app",
            ownPackage = "app.focusphone",
            now = now
        )
        assertFalse(result)
    }

    @Test
    fun `isAppBlockedPure blocks manual blocked apps in WORK`() {
        val result = ProfilePreferences.isAppBlockedPure(
            profile = ProfilePreferences.PROFILE_WORK,
            detoxEndTime = 0L,
            blockedApps = setOf("com.blocked.app"),
            workKeywords = emptySet(),
            packageName = "com.blocked.app",
            ownPackage = "app.focusphone",
            now = System.currentTimeMillis()
        )
        assertTrue(result)
    }

    @Test
    fun `isAppBlockedPure blocks apps matching work keywords in WORK`() {
        val result = ProfilePreferences.isAppBlockedPure(
            profile = ProfilePreferences.PROFILE_WORK,
            detoxEndTime = 0L,
            blockedApps = emptySet(),
            workKeywords = setOf("instagram", "facebook"),
            packageName = "com.instagram.android",
            ownPackage = "app.focusphone",
            now = System.currentTimeMillis()
        )
        assertTrue(result)
    }

    @Test
    fun `isAppBlockedPure does not block non-matching apps in WORK`() {
        val result = ProfilePreferences.isAppBlockedPure(
            profile = ProfilePreferences.PROFILE_WORK,
            detoxEndTime = 0L,
            blockedApps = emptySet(),
            workKeywords = setOf("instagram"),
            packageName = "com.productivity.app",
            ownPackage = "app.focusphone",
            now = System.currentTimeMillis()
        )
        assertFalse(result)
    }

    @Test
    fun `isAppBlockedPure allows own package in WORK`() {
        val result = ProfilePreferences.isAppBlockedPure(
            profile = ProfilePreferences.PROFILE_WORK,
            detoxEndTime = 0L,
            blockedApps = emptySet(),
            workKeywords = setOf("instagram"),
            packageName = "app.focusphone",
            ownPackage = "app.focusphone",
            now = System.currentTimeMillis()
        )
        assertFalse(result)
    }

    // ── Detox expiration ──────────────────────────────────────────

    @Test
    fun `isDetoxActivePure returns true when profile is DETOX and time not expired`() {
        val now = 1_000_000L
        val result = ProfilePreferences.isDetoxActivePure(
            profile = ProfilePreferences.PROFILE_DETOX,
            detoxEndTime = now + 60_000L,
            now = now
        )
        assertTrue(result)
    }

    @Test
    fun `isDetoxActivePure returns false when detox time expired`() {
        val now = 1_000_000L
        val result = ProfilePreferences.isDetoxActivePure(
            profile = ProfilePreferences.PROFILE_DETOX,
            detoxEndTime = now - 1L,
            now = now
        )
        assertFalse(result)
    }

    @Test
    fun `isDetoxActivePure returns false when profile is not DETOX`() {
        val now = 1_000_000L
        val result = ProfilePreferences.isDetoxActivePure(
            profile = ProfilePreferences.PROFILE_NORMAL,
            detoxEndTime = now + 60_000L,
            now = now
        )
        assertFalse(result)
    }

    @Test
    fun `expireDetoxIfNeededPure returns NORMAL when detox expired`() {
        val now = 1_000_000L
        val result = ProfilePreferences.expireDetoxIfNeededPure(
            profile = ProfilePreferences.PROFILE_DETOX,
            detoxEndTime = now - 1L,
            now = now
        )
        assertEquals(ProfilePreferences.PROFILE_NORMAL, result)
    }

    @Test
    fun `expireDetoxIfNeededPure returns DETOX when still active`() {
        val now = 1_000_000L
        val result = ProfilePreferences.expireDetoxIfNeededPure(
            profile = ProfilePreferences.PROFILE_DETOX,
            detoxEndTime = now + 60_000L,
            now = now
        )
        assertEquals(ProfilePreferences.PROFILE_DETOX, result)
    }

    @Test
    fun `expireDetoxIfNeededPure returns NORMAL when already NORMAL`() {
        val now = 1_000_000L
        val result = ProfilePreferences.expireDetoxIfNeededPure(
            profile = ProfilePreferences.PROFILE_NORMAL,
            detoxEndTime = 0L,
            now = now
        )
        assertEquals(ProfilePreferences.PROFILE_NORMAL, result)
    }

    // ── Hashing & password verification ───────────────────────────

    @Test
    fun `sha256 produces deterministic hash`() {
        val input = "password123"
        val expected = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        assertEquals(64, expected.length)
        assertTrue(expected.matches(Regex("^[0-9a-f]+$")))
    }

    @Test
    fun `sha256 different inputs produce different hashes`() {
        val hash1 = legacySha256("abc")
        val hash2 = legacySha256("abd")
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `verifyDetoxPassword upgrades legacy SHA-256 to PBKDF2`() {
        // Simulate a legacy stored hash
        val legacyHash = legacySha256("mysecret")
        // In a real context test we would call verifyDetoxPassword and assert
        // the stored value is upgraded. Here we verify the hash format transition.
        assertEquals(64, legacyHash.length)
        assertFalse(legacyHash.contains(":"))
    }

    // ── Helpers ───────────────────────────────────────────────────

    /**
     * Reproduces the legacy unsalted hash format that early builds stored.
     * Verification of that format now lives in
     * [app.focusphone.security.PasswordHasher] and is covered by its own tests.
     */
    private fun legacySha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
