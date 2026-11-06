package app.focusphone.data.preferences

import android.content.Context
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
class GesturePreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("focusphone_gestures", Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun `isSwipeDownEnabled returns true by default`() {
        assertTrue(GesturePreferences.isSwipeDownEnabled(context))
    }

    @Test
    fun `setSwipeDownEnabled persists false`() {
        GesturePreferences.setSwipeDownEnabled(context, false)
        assertFalse(GesturePreferences.isSwipeDownEnabled(context))
    }

    @Test
    fun `setSwipeDownEnabled persists true after being false`() {
        GesturePreferences.setSwipeDownEnabled(context, false)
        GesturePreferences.setSwipeDownEnabled(context, true)
        assertTrue(GesturePreferences.isSwipeDownEnabled(context))
    }

    @Test
    fun `preferences are isolated per file`() {
        // Ensure we are not touching other preference files
        GesturePreferences.setSwipeDownEnabled(context, false)
        val otherPrefs = context.getSharedPreferences("focusphone_widget", Context.MODE_PRIVATE)
        assertTrue(otherPrefs.all.isEmpty())
    }
}
