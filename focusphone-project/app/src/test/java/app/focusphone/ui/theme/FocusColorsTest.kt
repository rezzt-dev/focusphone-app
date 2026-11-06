package app.focusphone.ui.theme

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The contrast maths behind every FocusPhone surface. These are the guarantees that keep a
 * user-chosen theme/wallpaper combination readable, so they are pinned here rather than
 * verified by eye.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FocusColorsTest {

    private val black = Color.BLACK
    private val white = Color.WHITE

    // ── Contrast ratio ─────────────────────────────────────────────

    @Test
    fun `black on white is the maximum contrast ratio`() {
        assertEquals(21.0, FocusColors.contrastRatio(black, white), 0.05)
    }

    @Test
    fun `a colour has no contrast against itself`() {
        assertEquals(1.0, FocusColors.contrastRatio(0xFF3C7A55.toInt(), 0xFF3C7A55.toInt()), 0.001)
    }

    @Test
    fun `contrast ratio is symmetric`() {
        val a = 0xFF1E2A3A.toInt()
        val b = 0xFFD4C9B0.toInt()
        assertEquals(
            FocusColors.contrastRatio(a, b),
            FocusColors.contrastRatio(b, a),
            0.0001
        )
    }

    // ── ensureContrast ─────────────────────────────────────────────

    @Test
    fun `ensureContrast leaves an already readable colour untouched`() {
        // Slate's own pairing: #E0E0E0 on #181818 clears the body threshold comfortably.
        val fg = 0xFFE0E0E0.toInt()
        val bg = 0xFF181818.toInt()
        assertEquals(fg, FocusColors.ensureContrast(fg, bg, FocusColors.CONTRAST_BODY))
    }

    @Test
    fun `ensureContrast rescues a foreground that is invisible on its background`() {
        // The exact case a user can configure: near-black text on a near-black background.
        val fg = 0xFF232020.toInt()
        val bg = 0xFF181818.toInt()
        assertTrue(FocusColors.contrastRatio(fg, bg) < 1.5)

        val fixed = FocusColors.ensureContrast(fg, bg, FocusColors.CONTRAST_BODY)

        assertNotEquals(fg, fixed)
        assertTrue(
            "expected at least ${FocusColors.CONTRAST_BODY}:1",
            FocusColors.contrastRatio(fixed, bg) >= FocusColors.CONTRAST_BODY
        )
    }

    @Test
    fun `ensureContrast rescues white text on a white background`() {
        val fixed = FocusColors.ensureContrast(white, white, FocusColors.CONTRAST_BODY)
        assertTrue(FocusColors.contrastRatio(fixed, white) >= FocusColors.CONTRAST_BODY)
    }

    @Test
    fun `ensureContrast meets the requested ratio across the whole grey ramp`() {
        for (level in 0..255 step 5) {
            val bg = Color.rgb(level, level, level)
            for (start in 0..255 step 51) {
                val fg = Color.rgb(start, start, start)
                val fixed = FocusColors.ensureContrast(fg, bg, FocusColors.CONTRAST_LARGE)
                assertTrue(
                    "fg=$start bg=$level ratio=${FocusColors.contrastRatio(fixed, bg)}",
                    FocusColors.contrastRatio(fixed, bg) >= FocusColors.CONTRAST_LARGE - 0.01
                )
            }
        }
    }

    // ── bestOn ─────────────────────────────────────────────────────

    @Test
    fun `bestOn picks white for dark surfaces and black for light ones`() {
        assertEquals(white, FocusColors.bestOn(0xFF0A0E17.toInt()))
        assertEquals(black, FocusColors.bestOn(0xFFFAFAFA.toInt()))
    }

    // ── blend & alpha ──────────────────────────────────────────────

    @Test
    fun `blend at the extremes returns each input unchanged`() {
        val a = 0xFFFAFAFA.toInt()
        val b = 0xFF181818.toInt()
        assertEquals(a, FocusColors.blend(a, b, 1f))
        assertEquals(b, FocusColors.blend(a, b, 0f))
    }

    @Test
    fun `blend produces an opaque colour even from translucent inputs`() {
        val blended = FocusColors.blend(0x40FFFFFF, 0xFF000000.toInt(), 0.5f)
        assertEquals(255, Color.alpha(blended))
    }

    @Test
    fun `flatten composites a translucent colour onto its background`() {
        // 50% white over black is mid grey.
        val flattened = FocusColors.flatten(0x80FFFFFF.toInt(), black)
        assertEquals(255, Color.alpha(flattened))
        assertEquals(128, Color.red(flattened).toDouble(), 2.0)
    }

    @Test
    fun `withAlpha replaces alpha and keeps the rgb channels`() {
        val result = FocusColors.withAlpha(0xFF3C7A55.toInt(), 0.5f)
        assertEquals(127, Color.alpha(result))
        assertEquals(0x3C, Color.red(result))
        assertEquals(0x7A, Color.green(result))
        assertEquals(0x55, Color.blue(result))
    }

    @Test
    fun `withAlpha clamps out of range values`() {
        assertEquals(255, Color.alpha(FocusColors.withAlpha(black, 5f)))
        assertEquals(0, Color.alpha(FocusColors.withAlpha(black, -2f)))
    }

    // ── isDark ─────────────────────────────────────────────────────

    @Test
    fun `isDark classifies the four built-in theme backgrounds`() {
        assertTrue(FocusColors.isDark(0xFF181818.toInt()))   // slate
        assertTrue(FocusColors.isDark(0xFF0A0E17.toInt()))   // navy
        assertTrue(!FocusColors.isDark(0xFFFAFAFA.toInt()))  // off-white
        assertTrue(!FocusColors.isDark(0xFFFEFEFE.toInt()))  // warm beige
    }

    private fun assertEquals(expected: Int, actual: Double, delta: Double) =
        assertTrue(kotlin.math.abs(expected - actual) <= delta)
}
