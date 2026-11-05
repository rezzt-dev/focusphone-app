package app.focusphone

import android.appwidget.AppWidgetHost
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import app.focusphone.data.preferences.HomeGridPreferences
import app.focusphone.domain.model.CellRect
import app.focusphone.domain.model.HomeGrid
import app.focusphone.domain.model.HomeGridItem
import app.focusphone.ui.home.grid.HomeGridStore
import app.focusphone.ui.widget.AppWidgetHostManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Widget IDs handed out to FocusPhone's host must be released when a widget leaves the home
 * grid; otherwise the provider never receives onDeleted() and IDs pile up forever.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26) // AppWidgetHost.getAppWidgetIds()
class HomeGridWidgetIdsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val hostManager = AppWidgetHostManager.getInstance(context)
    private val store = HomeGridStore(context)

    @Before
    fun setUp() {
        saveGrid()
        hostManager.releaseOrphanedIds(emptySet())
    }

    @After
    fun tearDown() {
        HomeGridPreferences.clear(context)
    }

    @Test
    fun removing_widgets_never_accumulates_ids() {
        val ids = List(3) { hostManager.allocateAppWidgetId() }
        saveGrid(*ids.toIntArray())

        ids.forEach(store::removeWidget)

        assertEquals(emptyList<Int>(), allocatedIds())
        assertEquals(emptySet<Int>(), store.widgetIds())
    }

    @Test
    fun ids_leaked_by_older_builds_are_released() {
        val kept = List(2) { hostManager.allocateAppWidgetId() }
        saveGrid(*kept.toIntArray())
        repeat(3) { hostManager.allocateAppWidgetId() }

        hostManager.releaseOrphanedIds(store.widgetIds())

        assertEquals(kept.sorted(), allocatedIds().sorted())
    }

    /** A 4 × 8 grid with the app list at the bottom and one row per widget above it. */
    private fun saveGrid(vararg widgetIds: Int) {
        val widgets = widgetIds.mapIndexed { row, id ->
            HomeGridItem(HomeGridItem.Type.WIDGET, CellRect(0, row, 4, 1), id, "app.focusphone/.Test")
        }
        val apps = HomeGridItem(HomeGridItem.Type.APPS, CellRect(0, widgetIds.size, 4, 8 - widgetIds.size))
        HomeGridPreferences.save(context, HomeGrid(4, 8, widgets + apps))
    }

    private fun allocatedIds(): List<Int> =
        AppWidgetHost(context, AppWidgetHostManager.HOST_ID).appWidgetIds.toList()
}
