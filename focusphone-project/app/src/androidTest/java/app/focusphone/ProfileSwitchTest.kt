package app.focusphone

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.ui.home.HomeActivity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for profile switching (Normal → Work → Detox).
 */
@RunWith(AndroidJUnit4::class)
class ProfileSwitchTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ThemePreferences.markOnboardingComplete(context)
        ProfilePreferences.setProfile(context, ProfilePreferences.PROFILE_NORMAL)
        ProfilePreferences.setDetoxEndTime(context, 0L)
        ProfilePreferences.clearFailedAttempts(context)
    }

    @Test
    fun switch_from_normal_to_work_and_back() {
        ActivityScenario.launch(HomeActivity::class.java)

        // Open drawer
        onView(withId(app.focusphone.R.id.drawer_layout))
            .perform(DrawerActions.open())

        // Open profile dialog
        onView(withId(app.focusphone.R.id.tv_menu_profile))
            .perform(click())

        // Select Work profile
        onView(withId(app.focusphone.R.id.btn_profile_work))
            .perform(click())

        // Verify profile is Work
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(ProfilePreferences.PROFILE_WORK, ProfilePreferences.getProfile(context))

        // Open drawer again
        onView(withId(app.focusphone.R.id.drawer_layout))
            .perform(DrawerActions.open())

        // Open profile dialog again
        onView(withId(app.focusphone.R.id.tv_menu_profile))
            .perform(click())

        // Select Normal profile
        onView(withId(app.focusphone.R.id.btn_profile_normal))
            .perform(click())

        assertEquals(ProfilePreferences.PROFILE_NORMAL, ProfilePreferences.getProfile(context))
    }
}
