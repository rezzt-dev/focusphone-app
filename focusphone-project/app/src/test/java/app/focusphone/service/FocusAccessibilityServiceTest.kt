package app.focusphone.service

import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FocusAccessibilityServiceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun getPackageName(): String = "app.focusphone"
        }
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            null
        )
    }

    @Test
    fun `disabled service returns false`() {
        assertFalse(FocusAccessibilityService.isEnabled(context))
    }

    @Test
    fun `full component form is recognized`() {
        setEnabled("app.focusphone/app.focusphone.service.FocusAccessibilityService")
        assertTrue(FocusAccessibilityService.isEnabled(context))
    }

    @Test
    fun `short component form used by OEMs is recognized`() {
        setEnabled("app.focusphone/.service.FocusAccessibilityService")
        assertTrue(FocusAccessibilityService.isEnabled(context))
    }

    private fun setEnabled(value: String) {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            value
        )
    }
}
