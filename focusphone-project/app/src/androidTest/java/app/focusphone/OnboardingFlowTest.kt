package app.focusphone

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.ui.home.HomeActivity
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for the onboarding flow.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ThemePreferences.clearOnboardingComplete(context)
    }

    @Test
    fun onboarding_completes_all_3_steps_and_marks_onboarding_complete() {
        ActivityScenario.launch(HomeActivity::class.java)

        // Onboarding should start automatically
        onView(withId(app.focusphone.R.id.vp_onboarding))
            .check(matches(isDisplayed()))

        // Page 1: welcome
        onView(withText(app.focusphone.R.string.onboarding_step1_title))
            .check(matches(isDisplayed()))

        // Click next to page 2
        onView(withId(app.focusphone.R.id.btn_next))
            .perform(click())

        // Page 2: set launcher
        onView(withText(app.focusphone.R.string.onboarding_step2_title))
            .check(matches(isDisplayed()))

        // Click next to page 3
        onView(withId(app.focusphone.R.id.btn_next))
            .perform(click())

        // Page 3: pick apps
        onView(withText(app.focusphone.R.string.onboarding_step3_title))
            .check(matches(isDisplayed()))

        // Click get started to finish
        onView(withId(app.focusphone.R.id.btn_next))
            .perform(click())

        // Verify onboarding is marked complete
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertTrue(ThemePreferences.isOnboardingComplete(context))
    }
}
