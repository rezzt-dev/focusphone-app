package app.focusphone.data.preferences

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class WallpaperPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("focusphone_wallpaper", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `assignWallpaper stores the image of one surface only`() {
        WallpaperPreferences.assignWallpaper(context, WallpaperTarget.NOTIFICATIONS, HOME_COPY)

        assertEquals(HOME_COPY, WallpaperPreferences.getNotificationsWallpaperUri(context))
        assertNull(WallpaperPreferences.getHomeWallpaperUri(context))
        assertNull(WallpaperPreferences.getSettingsWallpaperUri(context))
    }

    @Test
    fun `assignWallpaper with null removes the image of that surface`() {
        WallpaperPreferences.assignWallpaper(context, WallpaperTarget.HOME, HOME_COPY)
        WallpaperPreferences.assignWallpaper(context, WallpaperTarget.SETTINGS, SETTINGS_COPY)

        WallpaperPreferences.assignWallpaper(context, WallpaperTarget.HOME, null)

        assertNull(WallpaperPreferences.getHomeWallpaperUri(context))
        assertEquals(SETTINGS_COPY, WallpaperPreferences.getSettingsWallpaperUri(context))
        assertEquals(setOf(SETTINGS_COPY), WallpaperPreferences.storedUris(context))
    }

    @Test
    fun `assignWallpaper changes the shared image while the shared mode is on`() {
        WallpaperPreferences.assignWallpaper(context, WallpaperTarget.HOME, HOME_COPY)
        WallpaperPreferences.setUseSharedWallpaper(context, true)

        WallpaperPreferences.assignWallpaper(context, WallpaperTarget.SETTINGS, SHARED_COPY)

        assertEquals(SHARED_COPY, WallpaperPreferences.getHomeWallpaperUri(context))
        assertEquals(SHARED_COPY, WallpaperPreferences.getNotificationsWallpaperUri(context))
        // The per-surface image is kept for when the shared mode is turned off again.
        WallpaperPreferences.setUseSharedWallpaper(context, false)
        assertEquals(HOME_COPY, WallpaperPreferences.getHomeWallpaperUri(context))
    }

    @Test
    fun `removing the shared image clears every surface in shared mode`() {
        WallpaperPreferences.setUseSharedWallpaper(context, true)
        WallpaperPreferences.assignWallpaper(context, WallpaperTarget.HOME, SHARED_COPY)

        WallpaperPreferences.assignWallpaper(context, WallpaperTarget.NOTIFICATIONS, null)

        assertNull(WallpaperPreferences.getHomeWallpaperUri(context))
        assertNull(WallpaperPreferences.getSettingsWallpaperUri(context))
        assertEquals(emptySet<String>(), WallpaperPreferences.storedUris(context))
    }

    private companion object {
        const val HOME_COPY = "file:///data/wallpapers/home.jpg"
        const val SETTINGS_COPY = "file:///data/wallpapers/settings.jpg"
        const val SHARED_COPY = "file:///data/wallpapers/shared.jpg"
    }
}
