package app.focusphone.data.preferences

import android.content.Context
import app.focusphone.domain.model.CellRect
import app.focusphone.domain.model.HomeGrid
import app.focusphone.domain.model.HomeGridItem
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
class HomeGridPreferencesTest {

    private lateinit var context: Context
    private val layoutPrefs get() = context.getSharedPreferences("focusphone_home_layout", Context.MODE_PRIVATE)
    private val widgetPrefs get() = context.getSharedPreferences("focusphone_widget", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        layoutPrefs.edit().clear().commit()
        widgetPrefs.edit().clear().commit()
        HomeGridPreferences.clear(context)
    }

    @Test
    fun `a saved grid is read back as it was`() {
        val grid = HomeGrid(
            5,
            9,
            listOf(
                HomeGridItem(HomeGridItem.Type.APPS, CellRect(0, 3, 5, 6)),
                HomeGridItem(HomeGridItem.Type.WIDGET, CellRect(0, 0, 5, 3), 12, "pkg/.W")
            )
        )
        assertNull(HomeGridPreferences.load(context))

        HomeGridPreferences.save(context, grid)

        assertEquals(grid, HomeGridPreferences.load(context))
    }

    @Test
    fun `a fresh install reads the classic order with no widget`() {
        val legacy = HomeGridPreferences.readLegacy(context)

        assertEquals(listOf(HomeGridItem.Type.CLOCK, HomeGridItem.Type.WIDGET, HomeGridItem.Type.APPS), legacy.order)
        assertTrue(legacy.clockEnabled)
        assertEquals(HomeGridItem.NO_WIDGET_ID, legacy.widgetId)
    }

    @Test
    fun `the pre-grid layout is carried over`() {
        layoutPrefs.edit()
            .putString("component_order", "apps,widget,clock")
            .putBoolean("clock_enabled", false)
            .putInt("app_spacing_dp", 12)
            .commit()
        widgetPrefs.edit()
            .putInt("widget_id", 42)
            .putBoolean("widget_enabled", true)
            .putString("widget_provider", "pkg/.W")
            .putInt("widget_height_dp", 200)
            .commit()

        val legacy = HomeGridPreferences.readLegacy(context)

        assertEquals(listOf(HomeGridItem.Type.APPS, HomeGridItem.Type.WIDGET, HomeGridItem.Type.CLOCK), legacy.order)
        assertFalse(legacy.clockEnabled)
        assertEquals(42, legacy.widgetId)
        assertEquals("pkg/.W", legacy.provider)
        assertEquals(200, legacy.widgetHeightDp)
    }

    @Test
    fun `a widget the old layout kept hidden is not carried over`() {
        layoutPrefs.edit().putBoolean("widget_enabled", false).commit()
        widgetPrefs.edit().putInt("widget_id", 42).putBoolean("widget_enabled", true).commit()

        assertEquals(HomeGridItem.NO_WIDGET_ID, HomeGridPreferences.readLegacy(context).widgetId)
    }

    @Test
    fun `clearing the legacy layout keeps the settings that still exist`() {
        layoutPrefs.edit()
            .putString("component_order", "clock,apps")
            .putBoolean("clock_enabled", true)
            .putInt("app_spacing_dp", 12)
            .commit()
        widgetPrefs.edit().putInt("widget_id", 42).commit()

        HomeGridPreferences.clearLegacy(context)

        assertEquals(mapOf("app_spacing_dp" to 12), layoutPrefs.all)
        assertTrue(widgetPrefs.all.isEmpty())
    }
}
