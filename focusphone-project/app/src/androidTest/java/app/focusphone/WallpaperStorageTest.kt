package app.focusphone

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.focusphone.data.preferences.WallpaperPreferences
import app.focusphone.ui.wallpaper.WallpaperStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * A picked wallpaper must keep working after the picker's grant is gone, which is what
 * happens on every cold start.
 */
@RunWith(AndroidJUnit4::class)
class WallpaperStorageTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        WallpaperPreferences.setUseSharedWallpaper(context, false)
        WallpaperPreferences.setSharedWallpaperUri(context, null)
        WallpaperPreferences.setHomeWallpaperUri(context, null)
        WallpaperPreferences.setNotificationsWallpaperUri(context, null)
        WallpaperPreferences.setSettingsWallpaperUri(context, null)
        File(context.filesDir, "wallpapers").deleteRecursively()
    }

    @Test
    fun a_picked_image_survives_losing_access_to_the_original() = runBlocking {
        val source = pickedImage("picked.png")

        val stored = WallpaperStorage.replace(context, Uri.fromFile(source)) {
            WallpaperPreferences.setHomeWallpaperUri(context, it)
        }
        assertTrue(stored)

        val reference = requireNotNull(WallpaperPreferences.getHomeWallpaperUri(context))
        assertTrue(reference.startsWith("file://"))
        assertTrue(reference.contains("/files/wallpapers/"))

        // The original going away stands in for the revoked Photo Picker grant.
        source.delete()
        assertTrue(WallpaperStorage.load(context, reference) is WallpaperStorage.LoadResult.Ready)
    }

    @Test
    fun replacing_a_wallpaper_deletes_the_previous_copy() = runBlocking {
        repeat(3) { index ->
            WallpaperStorage.replace(context, Uri.fromFile(pickedImage("picked$index.png"))) {
                WallpaperPreferences.setHomeWallpaperUri(context, it)
            }
        }
        assertEquals(1, File(context.filesDir, "wallpapers").listFiles()?.size)
    }

    @Test
    fun an_unreadable_legacy_reference_is_forgotten() = runBlocking {
        val legacy = "content://app.focusphone.missing/wallpaper.jpg"
        WallpaperPreferences.setHomeWallpaperUri(context, legacy)

        assertEquals(WallpaperStorage.LoadResult.Lost, WallpaperStorage.load(context, legacy))
        assertNull(WallpaperPreferences.getHomeWallpaperUri(context))
    }

    private fun pickedImage(name: String): File =
        File(context.cacheDir, name).apply {
            outputStream().use { out ->
                Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
                    .apply { eraseColor(Color.RED) }
                    .compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
}
