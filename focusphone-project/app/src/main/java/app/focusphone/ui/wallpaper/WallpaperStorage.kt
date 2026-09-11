package app.focusphone.ui.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import app.focusphone.data.preferences.WallpaperPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Owns the wallpaper images FocusPhone displays.
 *
 * The Android Photo Picker only grants read access for the lifetime of the process, so a
 * picked `content://` URI stops working after the next cold start. Every picked image is
 * therefore copied — downscaled and upright — into app-private storage, and the
 * preferences store the `file://` URI of that copy.
 *
 * Wallpapers chosen by builds that stored the `content://` URI directly are migrated the
 * first time they are displayed while the grant still works; once it is gone the
 * reference is removed and [LoadResult.Lost] tells the screen to inform the user, instead
 * of the wallpaper disappearing silently.
 *
 * Every write to the directory happens under one lock, so a clean-up never deletes a copy
 * that another import is about to assign.
 */
object WallpaperStorage {

    private const val TAG = "WallpaperStorage"
    private const val DIRECTORY = "wallpapers"
    private const val TEMP_SUFFIX = ".tmp"
    private const val JPEG_QUALITY = 92

    private val lock = Mutex()

    /** content:// URIs already migrated in this process, mapped to their private copy. */
    private val migrated = mutableMapOf<String, String>()

    sealed interface LoadResult {
        data class Ready(
            val bitmap: Bitmap,
            val softwareBlurApplied: Boolean
        ) : LoadResult

        /** The image can never be read again; its references have been removed. */
        data object Lost : LoadResult

        /** The image exists but could not be decoded this time. */
        data object Failed : LoadResult
    }

    /**
     * Copies [source] into private storage and hands the new reference to [assign], which
     * must store it in the preferences. Copies no longer referenced afterwards — such as
     * the one being replaced — are deleted.
     *
     * @return false when the image could not be read or copied; nothing is assigned then.
     */
    suspend fun replace(context: Context, source: Uri, assign: (String) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            lock.withLock {
                val stored = copyToPrivateStorage(appContext, source) ?: return@withLock false
                assign(stored)
                pruneUnreferenced(appContext)
                true
            }
        }

    /**
     * Runs [unassign], which must remove a reference from the preferences, and deletes the
     * copies nothing refers to any more. Holding the lock keeps a concurrent import from
     * seeing its fresh copy deleted.
     */
    suspend fun remove(context: Context, unassign: () -> Unit) =
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            lock.withLock {
                unassign()
                pruneUnreferenced(appContext)
            }
        }

    /** Loads the wallpaper stored as [uriString], migrating a legacy reference first. */
    suspend fun load(
        context: Context,
        uriString: String,
        blurEnabled: Boolean = false,
        blurRadius: Float = 0f
    ): LoadResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val privateUri = if (isPrivate(appContext, uriString)) {
            uriString
        } else {
            migrateLegacy(appContext, uriString) ?: return@withContext LoadResult.Lost
        }

        if (fileOf(privateUri)?.exists() != true) {
            Log.w(TAG, "Wallpaper copy is missing; forgetting it")
            WallpaperPreferences.removeUri(appContext, privateUri)
            WallpaperUtils.invalidate(privateUri)
            return@withContext LoadResult.Lost
        }
        WallpaperUtils.loadBitmap(appContext, privateUri, blurEnabled, blurRadius)
            ?.let { LoadResult.Ready(it.bitmap, it.softwareBlurApplied) }
            ?: LoadResult.Failed
    }

    /**
     * Copies a legacy `content://` wallpaper while its grant still works.
     *
     * @return the private copy, or null when the image could not be read any more — in
     * which case every reference to it has been removed.
     */
    private suspend fun migrateLegacy(context: Context, uriString: String): String? = lock.withLock {
        migrated[uriString]?.let { return@withLock it }

        val copy = copyToPrivateStorage(context, uriString.toUri())
        if (copy == null) {
            Log.w(TAG, "Legacy wallpaper is no longer readable; forgetting it")
            WallpaperPreferences.removeUri(context, uriString)
            return@withLock null
        }
        WallpaperPreferences.replaceUri(context, uriString, copy)
        migrated[uriString] = copy
        pruneUnreferenced(context)
        copy
    }

    // ── Files ──────────────────────────────────────────────────────

    private fun directory(context: Context): File = File(context.filesDir, DIRECTORY)

    private fun fileOf(uriString: String): File? =
        uriString.toUri().takeIf { it.scheme == "file" }?.path?.let(::File)

    private fun isPrivate(context: Context, uriString: String): Boolean {
        val file = fileOf(uriString) ?: return false
        return file.canonicalFile.parentFile == directory(context).canonicalFile
    }

    /** Must be called with [lock] held. */
    private fun copyToPrivateStorage(context: Context, source: Uri): String? {
        val decoded = WallpaperUtils.decodeSampled(context, source) ?: return null
        val upright = applyExifOrientation(context, source, decoded)
        val dir = directory(context).apply { mkdirs() }
        val target = File(dir, "wallpaper_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg")
        val temp = File(dir, target.name + TEMP_SUFFIX)

        return try {
            val written = temp.outputStream().use {
                upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
            }
            if (written && temp.renameTo(target)) Uri.fromFile(target).toString() else null
        } catch (e: Exception) {
            Log.e(TAG, "Could not store the wallpaper copy", e)
            null
        } finally {
            temp.delete()
            if (upright !== decoded) upright.recycle()
            decoded.recycle()
        }
    }

    /** Deletes every copy no preference refers to. Must be called with [lock] held. */
    private fun pruneUnreferenced(context: Context) {
        val referenced = WallpaperPreferences.storedUris(context)
            .mapNotNull { fileOf(it)?.canonicalFile }
            .toSet()
        directory(context).listFiles()?.forEach { file ->
            if (file.canonicalFile !in referenced) {
                WallpaperUtils.invalidate(Uri.fromFile(file).toString())
                file.delete()
            }
        }
    }

    /**
     * Returns [bitmap] rotated as its EXIF data asks. Camera photos are often stored
     * sideways with an orientation flag that [android.graphics.BitmapFactory] ignores.
     */
    private fun applyExifOrientation(context: Context, source: Uri, bitmap: Bitmap): Bitmap {
        val orientation = try {
            context.contentResolver.openInputStream(source)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> matrix.apply { setRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> matrix.apply { setRotate(-90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
