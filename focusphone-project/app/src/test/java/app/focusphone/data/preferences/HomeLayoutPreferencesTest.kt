package app.focusphone.data.preferences

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class HomeLayoutPreferencesTest {

    private lateinit var context: Context
    private val rawPrefs get() = context.getSharedPreferences("focusphone_home_layout", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        rawPrefs.edit().clear().commit()
    }

    @Test
    fun `removeObsoleteKeys deletes retired layout keys and keeps the rest`() {
        rawPrefs.edit()
            .putInt("clock_weight_percent", 50)
            .putInt("widget_weight_percent", 30)
            .putBoolean("clock_fixed_top", true)
            .commit()
        // The pre-grid order is read once by HomeGridPreferences to carry the layout over.
        rawPrefs.edit().putString("component_order", "apps,clock").commit()

        HomeLayoutPreferences.removeObsoleteKeys(context)

        assertFalse(rawPrefs.contains("clock_weight_percent"))
        assertFalse(rawPrefs.contains("widget_weight_percent"))
        assertFalse(rawPrefs.contains("clock_fixed_top"))
        assertEquals("apps,clock", rawPrefs.getString("component_order", null))
    }

    @Test
    fun `removeObsoleteKeys leaves a clean file untouched`() {
        HomeLayoutPreferences.setAppSpacingDp(context, 12)

        HomeLayoutPreferences.removeObsoleteKeys(context)

        assertEquals(mapOf("app_spacing_dp" to 12), rawPrefs.all)
    }
}
