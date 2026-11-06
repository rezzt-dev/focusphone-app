package app.focusphone.ui.theme

import android.content.Context
import app.focusphone.data.preferences.WallpaperPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Scrim strength decides whether text survives being drawn over a wallpaper, so the mapping
 * from the user's single "legibility" preference to a per-surface alpha is pinned here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class SurfaceScrimTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("focusphone_wallpaper", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    @Test
    fun `no scrim is painted when this surface shows no wallpaper`() {
        assertEquals(
            0f,
            SurfaceScrim.alphaFor(context, ScrimSurface.CONTENT, hasWallpaper = false),
            0.0001f
        )
    }

    @Test
    fun `no scrim is painted when wallpapers are switched off entirely`() {
        WallpaperPreferences.setWallpaperEnabled(context, false)
        assertEquals(
            0f,
            SurfaceScrim.alphaFor(context, ScrimSurface.PANEL, hasWallpaper = true),
            0.0001f
        )
    }

    @Test
    fun `the home screen can be left completely unprotected but panels never are`() {
        WallpaperPreferences.setReadability(context, 0)

        assertEquals(
            "the wallpaper is the point of the home screen",
            0f,
            SurfaceScrim.alphaFor(context, ScrimSurface.HOME, hasWallpaper = true),
            0.0001f
        )
        assertTrue(
            "dense panels keep a floor so 12sp text stays readable",
            SurfaceScrim.alphaFor(context, ScrimSurface.PANEL, hasWallpaper = true) > 0.2f
        )
        assertTrue(
            SurfaceScrim.alphaFor(context, ScrimSurface.CONTENT, hasWallpaper = true) > 0.3f
        )
    }

    @Test
    fun `legibility raises every surface monotonically`() {
        ScrimSurface.entries.forEach { surface ->
            var previous = -1f
            for (level in 0..100 step 10) {
                WallpaperPreferences.setReadability(context, level)
                val alpha = SurfaceScrim.alphaFor(context, surface, hasWallpaper = true)
                assertTrue("$surface at $level went backwards", alpha >= previous)
                assertTrue("$surface at $level out of range", alpha in 0f..1f)
                previous = alpha
            }
        }
    }

    @Test
    fun `denser surfaces are always protected at least as much as lighter ones`() {
        for (level in 0..100 step 25) {
            WallpaperPreferences.setReadability(context, level)
            val home = SurfaceScrim.alphaFor(context, ScrimSurface.HOME, hasWallpaper = true)
            val panel = SurfaceScrim.alphaFor(context, ScrimSurface.PANEL, hasWallpaper = true)
            val content = SurfaceScrim.alphaFor(context, ScrimSurface.CONTENT, hasWallpaper = true)
            assertTrue("home=$home panel=$panel at $level", panel >= home)
            assertTrue("panel=$panel content=$content at $level", content >= panel)
        }
    }

    @Test
    fun `blur relieves the scrim because it already softens the image`() {
        WallpaperPreferences.setReadability(context, 60)
        val sharp = SurfaceScrim.alphaFor(context, ScrimSurface.CONTENT, hasWallpaper = true)

        WallpaperPreferences.setBlurEnabled(context, true)
        WallpaperPreferences.setBlurIntensity(context, 100)
        val blurred = SurfaceScrim.alphaFor(context, ScrimSurface.CONTENT, hasWallpaper = true)

        assertTrue("blur should lighten the scrim", blurred < sharp)
        assertTrue("but never remove it", blurred > 0f)
    }

    @Test
    fun `a disabled blur toggle does not relieve the scrim even at full intensity`() {
        WallpaperPreferences.setReadability(context, 60)
        WallpaperPreferences.setBlurIntensity(context, 100)
        WallpaperPreferences.setBlurEnabled(context, false)

        assertEquals(
            ScrimSurface.CONTENT.floor + ScrimSurface.CONTENT.range * 0.6f,
            SurfaceScrim.alphaFor(context, ScrimSurface.CONTENT, hasWallpaper = true),
            0.0001f
        )
    }

    @Test
    fun `live slider values give the same alpha as the stored settings`() {
        WallpaperPreferences.setReadability(context, 35)
        WallpaperPreferences.setBlurEnabled(context, true)
        WallpaperPreferences.setBlurIntensity(context, 70)

        ScrimSurface.entries.forEach { surface ->
            assertEquals(
                SurfaceScrim.alphaFor(context, surface, hasWallpaper = true),
                SurfaceScrim.alphaFor(surface, legibility = 35, blurEnabled = true, blurIntensity = 70),
                0.0001f
            )
        }
    }

    @Test
    fun `readability is clamped to a sane range`() {
        WallpaperPreferences.setReadability(context, 500)
        assertEquals(100, WallpaperPreferences.getReadability(context))
        WallpaperPreferences.setReadability(context, -20)
        assertEquals(0, WallpaperPreferences.getReadability(context))
    }
}
