package app.focusphone.ui.widget

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class WidgetCatalogTest {

    private fun entry(pkg: String, cls: String, label: String, appLabel: String) = WidgetEntry(
        info = AppWidgetProviderInfo(),
        provider = ComponentName(pkg, "$pkg.$cls"),
        label = label,
        description = null,
        appLabel = appLabel,
        spanX = 4,
        spanY = 2,
        inUse = false
    )

    private val entries = listOf(
        entry("com.weather", "Now", "now", "Weather"),
        entry("com.clock", "Digital", "digital", "Clock"),
        entry("com.clock", "Analog", "analog", "Clock"),
        entry("com.weather", "Forecast", "forecast", "Weather")
    )

    @Test
    fun `rows are grouped by app in alphabetical order`() {
        val rows = WidgetCatalog.rows(entries, query = "")

        val labels = rows.map {
            when (it) {
                is WidgetPickerRow.AppHeader -> "#${it.appLabel}:${it.widgetCount}"
                is WidgetPickerRow.Widget -> it.entry.label
            }
        }
        assertEquals(listOf("#Clock:2", "analog", "digital", "#Weather:2", "forecast", "now"), labels)
    }

    @Test
    fun `a pinned app is listed first whatever its name`() {
        val rows = WidgetCatalog.rows(entries, query = "", pinnedPackage = "com.weather")

        val headers = rows.filterIsInstance<WidgetPickerRow.AppHeader>().map { it.appLabel }
        assertEquals(listOf("Weather", "Clock"), headers)
    }

    @Test
    fun `search matches widget names`() {
        val rows = WidgetCatalog.rows(entries, query = "fore")

        assertEquals(2, rows.size)
        assertEquals("forecast", (rows[1] as WidgetPickerRow.Widget).entry.label)
    }

    @Test
    fun `search matches app names and keeps all their widgets`() {
        val rows = WidgetCatalog.rows(entries, query = "clock")

        assertEquals(3, rows.size)
        assertTrue(rows.first() is WidgetPickerRow.AppHeader)
    }

    @Test
    fun `search without matches yields no rows`() {
        assertTrue(WidgetCatalog.rows(entries, query = "zzz").isEmpty())
    }

    @Test
    fun `blank query shows everything`() {
        assertEquals(6, WidgetCatalog.rows(entries, query = "   ").size)
    }
}
