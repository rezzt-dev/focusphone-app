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
class FontPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("focusphone_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `valid font sizes are preserved`() {
        assertEquals(8, FontPreferences.sanitizeFontSize(8))
        assertEquals(16, FontPreferences.sanitizeFontSize(16))
        assertEquals(24, FontPreferences.sanitizeFontSize(24))
    }

    @Test
    fun `corrupt font sizes recover to default`() {
        assertEquals(14, FontPreferences.sanitizeFontSize(-1))
        assertEquals(14, FontPreferences.sanitizeFontSize(100))
    }

    @Test
    fun `font size getter never writes while recovering corrupt storage`() {
        val preferences = context.getSharedPreferences("focusphone_prefs", Context.MODE_PRIVATE)
        preferences.edit().putInt("font_size_pt", 100).commit()

        assertEquals(14, FontPreferences.getFontSizePt(context))
        assertEquals(100, preferences.getInt("font_size_pt", -1))
    }

    @Test
    fun `font size setter sanitizes before writing`() {
        FontPreferences.setFontSizePt(context, 100)

        assertEquals(24, FontPreferences.getFontSizePt(context))
    }
}
