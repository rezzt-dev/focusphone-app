package app.focusphone

import android.appwidget.AppWidgetHost
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import app.focusphone.data.preferences.WidgetPreferences
import app.focusphone.ui.widget.AppWidgetHostManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Widget IDs handed out to FocusPhone's host must be released when a widget is removed or
 * replaced; otherwise the provider never receives onDeleted() and IDs pile up forever.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26) // AppWidgetHost.getAppWidgetIds()
class WidgetSlotTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val hostManager = AppWidgetHostManager.getInstance(context)

    @Before
    fun setUp() {
        WidgetPreferences.clearWidget(context)
        hostManager.releaseOrphanedIds(context)
    }

    @Test
    fun repeated_add_and_remove_never_accumulates_ids() {
        repeat(5) {
            hostManager.assignHomeWidget(context, hostManager.allocateAppWidgetId())
            hostManager.removeHomeWidget(context)
        }
        assertEquals(emptyList<Int>(), allocatedIds())
    }

    @Test
    fun replacing_a_widget_releases_the_previous_id() {
        hostManager.assignHomeWidget(context, hostManager.allocateAppWidgetId())
        val second = hostManager.allocateAppWidgetId()
        hostManager.assignHomeWidget(context, second)

        assertEquals(listOf(second), allocatedIds())
    }

    @Test
    fun ids_leaked_by_older_builds_are_released() {
        val kept = hostManager.allocateAppWidgetId()
        hostManager.assignHomeWidget(context, kept)
        repeat(3) { hostManager.allocateAppWidgetId() }

        hostManager.releaseOrphanedIds(context)

        assertEquals(listOf(kept), allocatedIds())
    }

    private fun allocatedIds(): List<Int> =
        AppWidgetHost(context, AppWidgetHostManager.HOST_ID).appWidgetIds.toList()
}
