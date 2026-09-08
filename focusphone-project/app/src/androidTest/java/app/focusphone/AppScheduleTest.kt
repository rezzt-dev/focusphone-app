package app.focusphone

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.focusphone.data.preferences.AppSchedulePreferences
import app.focusphone.domain.model.AppScheduleRule
import app.focusphone.domain.model.ScheduleBlockReason
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

/**
 * Instrumented tests for schedule persistence, usage accounting and the password lock.
 *
 * These run on a device because every value is encrypted with AES-256/GCM through the
 * Android Keystore, which the JVM unit tests cannot exercise.
 */
@RunWith(AndroidJUnit4::class)
class AppScheduleTest {

    private val testPackage = "com.example.scheduled"

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = clearScheduleState()

    @After
    fun tearDown() = clearScheduleState()

    private fun clearScheduleState() {
        AppSchedulePreferences.getRules(context).forEach {
            AppSchedulePreferences.removeRule(context, it.packageName)
        }
        AppSchedulePreferences.setPassword(context, null)
    }

    // ── Rules ──────────────────────────────────────────────────────

    @Test
    fun rule_survives_a_save_and_load_round_trip() {
        val rule = AppScheduleRule(
            packageName = testPackage,
            windowEnabled = true,
            startMinute = 8 * 60 + 15,
            endMinute = 20 * 60,
            days = setOf(Calendar.MONDAY, Calendar.FRIDAY),
            dailyLimitMinutes = 45
        )

        AppSchedulePreferences.saveRule(context, rule)

        assertEquals(rule, AppSchedulePreferences.getRule(context, testPackage))
    }

    @Test
    fun saving_a_rule_twice_replaces_it_instead_of_duplicating() {
        AppSchedulePreferences.saveRule(context, AppScheduleRule(testPackage, dailyLimitMinutes = 30))
        AppSchedulePreferences.saveRule(context, AppScheduleRule(testPackage, dailyLimitMinutes = 60))

        val rules = AppSchedulePreferences.getRules(context).filter { it.packageName == testPackage }
        assertEquals(1, rules.size)
        assertEquals(60, rules.first().dailyLimitMinutes)
    }

    @Test
    fun a_rule_that_restricts_nothing_is_not_stored() {
        AppSchedulePreferences.saveRule(context, AppScheduleRule(testPackage))

        assertNull(AppSchedulePreferences.getRule(context, testPackage))
    }

    @Test
    fun apps_without_a_rule_are_never_blocked() {
        assertNull(AppSchedulePreferences.getBlockReason(context, "com.example.unrestricted"))
    }

    // ── Daily usage ────────────────────────────────────────────────

    @Test
    fun usage_accumulates_and_blocks_once_the_limit_is_reached() {
        AppSchedulePreferences.saveRule(context, AppScheduleRule(testPackage, dailyLimitMinutes = 2))

        AppSchedulePreferences.addUsageSeconds(context, testPackage, 60)
        assertEquals(1, AppSchedulePreferences.getUsedMinutes(context, testPackage))
        assertNull(AppSchedulePreferences.getBlockReason(context, testPackage))

        AppSchedulePreferences.addUsageSeconds(context, testPackage, 60)
        val reason = AppSchedulePreferences.getBlockReason(context, testPackage)
        assertTrue(reason is ScheduleBlockReason.DailyLimitReached)
    }

    @Test
    fun remaining_allowance_shrinks_with_usage_and_never_goes_negative() {
        val rule = AppScheduleRule(testPackage, dailyLimitMinutes = 1)
        AppSchedulePreferences.saveRule(context, rule)

        assertEquals(60, AppSchedulePreferences.getRemainingLimitSeconds(context, rule))

        AppSchedulePreferences.addUsageSeconds(context, testPackage, 90)
        assertEquals(0, AppSchedulePreferences.getRemainingLimitSeconds(context, rule))
    }

    @Test
    fun removing_a_rule_also_clears_its_usage() {
        AppSchedulePreferences.saveRule(context, AppScheduleRule(testPackage, dailyLimitMinutes = 10))
        AppSchedulePreferences.addUsageSeconds(context, testPackage, 120)

        AppSchedulePreferences.removeRule(context, testPackage)

        assertEquals(0, AppSchedulePreferences.getUsedSeconds(context, testPackage))
    }

    // ── Password ───────────────────────────────────────────────────

    @Test
    fun password_is_stored_hashed_and_verifies_correctly() {
        AppSchedulePreferences.setPassword(context, "focus1234")

        assertTrue(AppSchedulePreferences.hasPassword(context))
        assertTrue(AppSchedulePreferences.verifyPassword(context, "focus1234"))
        assertFalse(AppSchedulePreferences.verifyPassword(context, "focus1235"))
    }

    @Test
    fun repeated_wrong_attempts_start_a_cooldown_without_erasing_the_rules() {
        AppSchedulePreferences.setPassword(context, "focus1234")
        AppSchedulePreferences.saveRule(context, AppScheduleRule(testPackage, dailyLimitMinutes = 30))

        repeat(AppSchedulePreferences.MAX_FAILED_ATTEMPTS) {
            AppSchedulePreferences.verifyPassword(context, "wrong")
        }

        assertTrue(AppSchedulePreferences.isLockedOut(context))
        // The lock must not become a bypass: password and rules both survive.
        assertTrue(AppSchedulePreferences.hasPassword(context))
        assertNotNull(AppSchedulePreferences.getRule(context, testPackage))
        // Even the correct password is refused while the cooldown runs.
        assertFalse(AppSchedulePreferences.verifyPassword(context, "focus1234"))
    }

    @Test
    fun a_successful_attempt_clears_previous_failures() {
        AppSchedulePreferences.setPassword(context, "focus1234")

        AppSchedulePreferences.verifyPassword(context, "wrong")
        assertEquals(
            AppSchedulePreferences.MAX_FAILED_ATTEMPTS - 1,
            AppSchedulePreferences.getRemainingAttempts(context)
        )

        assertTrue(AppSchedulePreferences.verifyPassword(context, "focus1234"))
        assertEquals(
            AppSchedulePreferences.MAX_FAILED_ATTEMPTS,
            AppSchedulePreferences.getRemainingAttempts(context)
        )
    }
}
