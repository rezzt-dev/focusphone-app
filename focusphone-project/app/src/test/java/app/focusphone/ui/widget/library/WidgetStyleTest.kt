package app.focusphone.ui.widget.library

import android.graphics.Color
import app.focusphone.ui.theme.FocusColors
import app.focusphone.ui.theme.FocusPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class WidgetStyleTest {

    private fun palette(background: Int, text: Int, secondary: Int) = FocusPalette(
        background = background,
        surface = background,
        textPrimary = text,
        textSecondary = secondary,
        divider = background,
        accent = text,
        onAccent = background,
        isDark = FocusColors.isDark(background)
    )

    private val slate = palette(0xFF181818.toInt(), 0xFFE0E0E0.toInt(), 0xFF9E9E9E.toInt())
    private val offWhite = palette(0xFFFAFAFA.toInt(), 0xFF333333.toInt(), 0xFF757575.toInt())

    @Test
    fun `a dark theme gets a card slightly lighter than its background`() {
        val style = WidgetStyle.from(slate)

        assertTrue(style.isDark)
        assertTrue(FocusColors.luminance(style.card) > FocusColors.luminance(slate.background))
        assertTrue(FocusColors.luminance(style.chip) > FocusColors.luminance(style.card))
        assertTrue(FocusColors.luminance(style.track) > FocusColors.luminance(style.chip))
    }

    @Test
    fun `a light theme gets a card slightly darker than its background`() {
        val style = WidgetStyle.from(offWhite)

        assertTrue(!style.isDark)
        assertTrue(FocusColors.luminance(style.card) < FocusColors.luminance(offWhite.background))
        assertTrue(FocusColors.luminance(style.track) < FocusColors.luminance(style.chip))
    }

    @Test
    fun `the card keeps the tint of the theme's surface`() {
        // Warm beige: a neutral text colour over a warm surface must still give a warm card.
        val warm = palette(0xFFFEFEFE.toInt(), 0xFF4A4A4A.toInt(), 0xFF8A8078.toInt()).copy(surface = 0xFFF0EDE8.toInt())

        val card = WidgetStyle.from(warm).card

        assertTrue(Color.red(card) > Color.blue(card))
    }

    @Test
    fun `text and accent come straight from the launcher palette`() {
        val style = WidgetStyle.from(slate)

        assertEquals(slate.textPrimary, style.textPrimary)
        assertEquals(slate.textSecondary, style.textSecondary)
        assertEquals(slate.accent, style.accent)
        assertEquals(slate.onAccent, style.onAccent)
    }

    @Test
    fun `every text colour stays legible on the card`() {
        listOf(slate, offWhite).map(WidgetStyle::from).forEach { style ->
            assertTrue(FocusColors.contrastRatio(style.textPrimary, style.card) >= FocusColors.CONTRAST_BODY)
            assertTrue(FocusColors.contrastRatio(style.textSecondary, style.card) >= FocusColors.CONTRAST_SUPPORT)
            assertNotEquals(style.card, style.textTertiary)
            assertTrue(FocusColors.contrastRatio(style.onAccent, style.accent) >= FocusColors.CONTRAST_LARGE)
        }
    }
}
