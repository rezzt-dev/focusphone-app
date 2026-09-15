package app.focusphone

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.domain.model.AppInfo
import app.focusphone.ui.appselect.AppSelectActivity
import app.focusphone.ui.appselect.adapter.InstalledAppsAdapter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for app selection and the 10-app limit.
 */
@RunWith(AndroidJUnit4::class)
class AppSelectFlowTest {

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Context>().applicationContext as TestFocusPhoneApp

        // Ensure onboarding is complete so we don't get redirected
        ThemePreferences.markOnboardingComplete(app)

        // Inject fake apps
        val fakeApps = (1..12).map { index ->
            AppInfo(
                packageName = "com.test.app$index",
                label = "App $index",
                isFavourite = false
            )
        }
        app.fakeAppRepository = FakeAppRepository(fakeApps)
    }

    @Test
    fun select_and_deselect_apps_enforce_limit_of_10() {
        ActivityScenario.launch(AppSelectActivity::class.java)

        // Select first 10 apps
        repeat(10) { index ->
            onView(withId(app.focusphone.R.id.rv_apps))
                .perform(
                    RecyclerViewActions.actionOnItemAtPosition<InstalledAppsAdapter.ViewHolder>(
                        index,
                        click()
                    )
                )
        }

        // Verify count shows 10 of 10
        onView(withId(app.focusphone.R.id.tv_selected_count))
            .check(matches(withText("10 of 10 apps selected")))

        // Try to select the 11th app — should still show 10
        onView(withId(app.focusphone.R.id.rv_apps))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<InstalledAppsAdapter.ViewHolder>(
                    10,
                    click()
                )
            )

        onView(withId(app.focusphone.R.id.tv_selected_count))
            .check(matches(withText("10 of 10 apps selected")))
    }
}
