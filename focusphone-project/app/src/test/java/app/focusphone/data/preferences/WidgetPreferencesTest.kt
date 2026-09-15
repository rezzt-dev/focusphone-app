package app.focusphone.data.preferences

import android.content.Context
import org.junit.Assert.assertEquals
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
class WidgetPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("focusphone_widget", Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun `getWidgetId returns INVALID_WIDGET_ID by default`() {
        assertEquals(WidgetPreferences.INVALID_WIDGET_ID, WidgetPreferences.getWidgetId(context))
    }

    @Test
    fun `isWidgetEnabled returns false by default`() {
        assertFalse(WidgetPreferences.isWidgetEnabled(context))
    }

    @Test
    fun `setWidgetId persists value`() {
        WidgetPreferences.setWidgetId(context, 42)
        assertEquals(42, WidgetPreferences.getWidgetId(context))
    }

    @Test
    fun `setWidgetEnabled persists true`() {
        WidgetPreferences.setWidgetEnabled(context, true)
        assertTrue(WidgetPreferences.isWidgetEnabled(context))
    }

    @Test
    fun `clearWidget resets id and enabled flag`() {
        WidgetPreferences.setWidgetId(context, 42)
        WidgetPreferences.setWidgetEnabled(context, true)

        WidgetPreferences.clearWidget(context)

        assertEquals(WidgetPreferences.INVALID_WIDGET_ID, WidgetPreferences.getWidgetId(context))
        assertFalse(WidgetPreferences.isWidgetEnabled(context))
    }

    @Test
    fun `clearWidget is safe when no widget was set`() {
        // Should not throw even if preferences are empty
        WidgetPreferences.clearWidget(context)
        assertEquals(WidgetPreferences.INVALID_WIDGET_ID, WidgetPreferences.getWidgetId(context))
        assertFalse(WidgetPreferences.isWidgetEnabled(context))
    }

    @Test
    fun `setWidgetId with negative value is persisted correctly`() {
        // Edge case: negative IDs should be stored as-is; validation is caller's responsibility
        WidgetPreferences.setWidgetId(context, -5)
        assertEquals(-5, WidgetPreferences.getWidgetId(context))
    }

    @Test
    fun `preferences are isolated per file`() {
        WidgetPreferences.setWidgetId(context, 42)
        val otherPrefs = context.getSharedPreferences("focusphone_gestures", Context.MODE_PRIVATE)
        assertTrue(otherPrefs.all.isEmpty())
    }
}
