package app.focusphone.data.preferences

import android.content.ComponentName
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    private val provider = ComponentName("com.example.clock", "com.example.clock.ClockWidget")

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
    fun `no widget is assigned by default`() {
        assertFalse(WidgetPreferences.isWidgetEnabled(context))
        assertFalse(WidgetPreferences.hasWidget(context))
        assertNull(WidgetPreferences.getProvider(context))
        assertEquals(WidgetPreferences.HEIGHT_AUTO, WidgetPreferences.getHeightDp(context))
    }

    @Test
    fun `setWidget persists id, provider and enabled flag`() {
        WidgetPreferences.setWidget(context, 42, provider)

        assertEquals(42, WidgetPreferences.getWidgetId(context))
        assertTrue(WidgetPreferences.isWidgetEnabled(context))
        assertTrue(WidgetPreferences.hasWidget(context))
        assertEquals(provider, WidgetPreferences.getProvider(context))
    }

    @Test
    fun `setWidget without a provider keeps the slot usable`() {
        WidgetPreferences.setWidget(context, 42, provider)
        WidgetPreferences.setWidget(context, 43, null)

        assertEquals(43, WidgetPreferences.getWidgetId(context))
        assertNull(WidgetPreferences.getProvider(context))
    }

    @Test
    fun `a new widget starts at its provider's size`() {
        WidgetPreferences.setWidget(context, 42, provider)
        WidgetPreferences.setHeightDp(context, 200)

        WidgetPreferences.setWidget(context, 43, provider)

        assertEquals(WidgetPreferences.HEIGHT_AUTO, WidgetPreferences.getHeightDp(context))
    }

    @Test
    fun `setHeightDp persists the chosen height and never stores a negative one`() {
        WidgetPreferences.setHeightDp(context, 176)
        assertEquals(176, WidgetPreferences.getHeightDp(context))

        WidgetPreferences.setHeightDp(context, -10)
        assertEquals(WidgetPreferences.HEIGHT_AUTO, WidgetPreferences.getHeightDp(context))
    }

    @Test
    fun `clearWidget resets id, provider, height and enabled flag`() {
        WidgetPreferences.setWidget(context, 42, provider)
        WidgetPreferences.setHeightDp(context, 176)

        WidgetPreferences.clearWidget(context)

        assertEquals(WidgetPreferences.INVALID_WIDGET_ID, WidgetPreferences.getWidgetId(context))
        assertFalse(WidgetPreferences.isWidgetEnabled(context))
        assertFalse(WidgetPreferences.hasWidget(context))
        assertNull(WidgetPreferences.getProvider(context))
        assertEquals(WidgetPreferences.HEIGHT_AUTO, WidgetPreferences.getHeightDp(context))
    }

    @Test
    fun `clearWidget is safe when no widget was set`() {
        // Should not throw even if preferences are empty
        WidgetPreferences.clearWidget(context)
        assertEquals(WidgetPreferences.INVALID_WIDGET_ID, WidgetPreferences.getWidgetId(context))
        assertFalse(WidgetPreferences.isWidgetEnabled(context))
    }

    @Test
    fun `preferences are isolated per file`() {
        WidgetPreferences.setWidget(context, 42, provider)
        val otherPrefs = context.getSharedPreferences("focusphone_gestures", Context.MODE_PRIVATE)
        assertTrue(otherPrefs.all.isEmpty())
    }
}
