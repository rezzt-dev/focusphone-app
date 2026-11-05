package app.focusphone.data.preferences

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class LibraryWidgetPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("focusphone_widget_library", Context.MODE_PRIVATE).edit().clear().commit()
    }

    // ── Intention ──────────────────────────────────────────────────

    @Test
    fun `an intention is kept to one tidy line`() {
        assertEquals("read a book", LibraryWidgetPreferences.sanitizeIntention("  read\n\n a   book  "))
        assertEquals("", LibraryWidgetPreferences.sanitizeIntention(" \n\t "))
    }

    @Test
    fun `an intention is capped without a trailing space`() {
        val long = "word ".repeat(40)

        val clean = LibraryWidgetPreferences.sanitizeIntention(long)

        assertEquals(true, clean.length <= LibraryWidgetPreferences.MAX_INTENTION_LENGTH)
        assertEquals(clean.trim(), clean)
    }

    // ── World clock ────────────────────────────────────────────────

    @Test
    fun `a widget without saved cities shows the defaults`() {
        assertEquals(LibraryWidgetPreferences.DEFAULT_WORLD_ZONES, LibraryWidgetPreferences.getWorldZones(context, 7))
    }

    @Test
    fun `cities are stored per widget and released with it`() {
        LibraryWidgetPreferences.setWorldZones(context, 7, listOf("Asia/Tokyo", "Europe/Madrid"))
        LibraryWidgetPreferences.setWorldZones(context, 8, listOf("America/Chicago"))

        assertEquals(listOf("Asia/Tokyo", "Europe/Madrid"), LibraryWidgetPreferences.getWorldZones(context, 7))

        LibraryWidgetPreferences.forget(context, intArrayOf(7))

        assertEquals(LibraryWidgetPreferences.DEFAULT_WORLD_ZONES, LibraryWidgetPreferences.getWorldZones(context, 7))
        assertEquals(listOf("America/Chicago"), LibraryWidgetPreferences.getWorldZones(context, 8))
    }

    @Test
    fun `zone lists drop duplicates, blanks and anything past three`() {
        val raw = LibraryWidgetPreferences.serializeZones(
            listOf("Asia/Tokyo", " ", "Asia/Tokyo", "Europe/Paris", "bad,id", "Africa/Cairo", "Asia/Dubai")
        )

        assertEquals(listOf("Asia/Tokyo", "Europe/Paris", "Africa/Cairo"), LibraryWidgetPreferences.parseZones(raw))
    }

    @Test
    fun `a corrupted zone list is read defensively`() {
        assertEquals(listOf("Asia/Tokyo", "Europe/Paris"), LibraryWidgetPreferences.parseZones(",Asia/Tokyo,,Europe/Paris,Asia/Tokyo,"))
        assertEquals(emptyList<String>(), LibraryWidgetPreferences.parseZones(""))
    }
}
