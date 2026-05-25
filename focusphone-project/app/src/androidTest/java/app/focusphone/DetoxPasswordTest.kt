package app.focusphone

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.ui.home.HomeActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for Detox password verification and lockout behaviour.
 */
@RunWith(AndroidJUnit4::class)
class DetoxPasswordTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ThemePreferences.markOnboardingComplete(context)
        ProfilePreferences.setProfile(context, ProfilePreferences.PROFILE_NORMAL)
        ProfilePreferences.setDetoxEndTime(context, 0L)
        ProfilePreferences.setDetoxPassword(context, null)
        ProfilePreferences.clearFailedAttempts(context)
    }

    @Test
    fun detox_password_verification_succeeds_with_correct_password() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Activate detox with a known password
        ProfilePreferences.setDetoxPassword(context, "1234")
        ProfilePreferences.setProfile(context, ProfilePreferences.PROFILE_DETOX)
        ProfilePreferences.setDetoxEndTime(context, System.currentTimeMillis() + 3600_000L)

        ActivityScenario.launch(HomeActivity::class.java)

        // Open drawer
        onView(withId(app.focusphone.R.id.drawer_layout))
            .perform(DrawerActions.open())

        // Open profile dialog and select Normal (requires password verification)
        onView(withId(app.focusphone.R.id.tv_menu_profile))
            .perform(click())
        onView(withId(app.focusphone.R.id.btn_profile_normal))
            .perform(click())

        // Enter correct password in verification dialog
        onView(withId(app.focusphone.R.id.et_detox_password_verify))
            .perform(typeText("1234"))
        onView(withId(app.focusphone.R.id.btn_ok))
            .perform(click())

        // Verify detox is deactivated and profile is Normal
        assertEquals(ProfilePreferences.PROFILE_NORMAL, ProfilePreferences.getProfile(context))
        assertFalse(ProfilePreferences.isDetoxActive(context))
    }

    @Test
    fun detox_lockout_after_4_failed_attempts() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Activate detox with a known password
        ProfilePreferences.setDetoxPassword(context, "secret")
        ProfilePreferences.setProfile(context, ProfilePreferences.PROFILE_DETOX)
        ProfilePreferences.setDetoxEndTime(context, System.currentTimeMillis() + 3600_000L)
        ProfilePreferences.clearFailedAttempts(context)

        ActivityScenario.launch(HomeActivity::class.java)

        // Open drawer
        onView(withId(app.focusphone.R.id.drawer_layout))
            .perform(DrawerActions.open())

        // Open profile dialog and select Normal (requires password verification)
        onView(withId(app.focusphone.R.id.tv_menu_profile))
            .perform(click())
        onView(withId(app.focusphone.R.id.btn_profile_normal))
            .perform(click())

        // Enter wrong password 4 times
        repeat(4) {
            onView(withId(app.focusphone.R.id.et_detox_password_verify))
                .perform(typeText("wrong"))
            onView(withId(app.focusphone.R.id.btn_ok))
                .perform(click())
        }

        // After 4 failed attempts detox should be force-disabled
        assertEquals(ProfilePreferences.PROFILE_NORMAL, ProfilePreferences.getProfile(context))
        assertTrue(ProfilePreferences.isPasswordLockedOut(context))
    }
}
