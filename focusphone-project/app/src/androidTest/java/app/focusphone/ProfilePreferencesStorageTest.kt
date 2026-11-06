package app.focusphone

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.focusphone.data.preferences.ProfilePreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilePreferencesStorageTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        context.getSharedPreferences("focusphone_profile", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun defaults_are_used_before_the_user_configures_keywords() {
        assertTrue(ProfilePreferences.getWorkKeywords(context).isNotEmpty())
    }

    @Test
    fun explicitly_empty_keywords_remain_empty_after_reload() {
        ProfilePreferences.setWorkKeywords(context, emptySet())
        ProfilePreferences.setProfile(context, ProfilePreferences.PROFILE_WORK)

        assertTrue(ProfilePreferences.getWorkKeywords(context).isEmpty())
        assertFalse(ProfilePreferences.isAppBlocked(context, "com.instagram.android"))
    }
}
