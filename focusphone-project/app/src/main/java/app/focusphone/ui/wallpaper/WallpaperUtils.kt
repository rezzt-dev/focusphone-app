package app.focusphone.ui.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.scale
import androidx.core.net.toUri
import app.focusphone.data.preferences.WallpaperPreferences
import app.focusphone.data.preferences.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper for loading, blurring and applying user-selected wallpapers.
 *
 * On Android 12+ (API 31) blur is applied in real time via [RenderEffect]. On older
 * devices a fast bitmap downscale/upscale approximation is used so the UI remains
 * responsive.
 */
object WallpaperUtils {

    private const val TAG = "WallpaperUtils"
    private const val MAX_BITMAP_DIMENSION = 1920

    private data class CacheKey(
        val uri: String,
        val blurEnabled: Boolean,
        val blurRadiusTenths: Int
    )

    data class PreparedBitmap(
        val bitmap: Bitmap,
        val softwareBlurApplied: Boolean
    )

    /** At most one eighth of the process heap is retained for prepared wallpapers. */
    private val bitmapCache = object : LruCache<CacheKey, PreparedBitmap>(
        (Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt().coerceAtLeast(1)
    ) {
        override fun sizeOf(key: CacheKey, value: PreparedBitmap): Int =
            (value.bitmap.allocationByteCount / 1024).coerceAtLeast(1)
    }

    /**
     * Convert a 0-100 intensity value into a blur radius in pixels (0-25).
     */
    fun getBlurRadius(intensity: Int): Float = (intensity / 100f) * 25f

    fun getWallpaperUri(context: Context, target: WallpaperTarget): String? = when (target) {
        WallpaperTarget.HOME -> WallpaperPreferences.getHomeWallpaperUri(context)
        WallpaperTarget.NOTIFICATIONS -> WallpaperPreferences.getNotificationsWallpaperUri(context)
        WallpaperTarget.SETTINGS -> WallpaperPreferences.getSettingsWallpaperUri(context)
    }

    /**
     * Load a bitmap from a stored wallpaper URI, scaled to avoid excessive memory use.
     */
    suspend fun loadBitmap(
        context: Context,
        uriString: String?,
        blurEnabled: Boolean = false,
        blurRadius: Float = 0f
    ): PreparedBitmap? = withContext(Dispatchers.IO) {
        if (uriString.isNullOrBlank()) return@withContext null
        val key = CacheKey(uriString, blurEnabled, (blurRadius * 10f).toInt())
        synchronized(bitmapCache) {
            bitmapCache.get(key)?.takeUnless { it.bitmap.isRecycled }
        }?.let { return@withContext it }

        val decoded = decodeSampled(context, uriString.toUri()) ?: return@withContext null
        val useSoftwareBlur = blurEnabled && blurRadius > 0.5f && Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        val prepared = if (useSoftwareBlur) {
            val blurred = blurBitmap(decoded, blurRadius)
            if (blurred !== decoded) decoded.recycle()
            PreparedBitmap(blurred, softwareBlurApplied = true)
        } else {
            PreparedBitmap(decoded, softwareBlurApplied = false)
        }
        synchronized(bitmapCache) {
            // Blur is global, so variants for the same URI become stale as soon as the
            // setting changes. Keeping only the active one prevents slider previews from
            // filling the cache with dozens of radii.
            bitmapCache.snapshot().keys
                .filter { it.uri == uriString && it != key }
                .forEach(bitmapCache::remove)
            bitmapCache.put(key, prepared)
        }
        prepared
    }

    /** Removes every cached variant of a wallpaper that was deleted or replaced. */
    fun invalidate(uriString: String) {
        synchronized(bitmapCache) {
            bitmapCache.snapshot().keys
                .filter { it.uri == uriString }
                .forEach(bitmapCache::remove)
        }
    }

    /**
     * Decodes [uri] no larger than [MAX_BITMAP_DIMENSION] on either side, or returns null
     * when it cannot be read. Blocking; call from a background thread.
     */
    fun decodeSampled(context: Context, uri: Uri): Bitmap? = try {
        val (width, height) = context.contentResolver.openInputStream(uri)?.use { stream ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, options)
            options.outWidth to options.outHeight
        } ?: throw java.io.FileNotFoundException("No stream for $uri")

        val sampleSize = calculateInSampleSize(width, height, MAX_BITMAP_DIMENSION, MAX_BITMAP_DIMENSION)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to decode wallpaper from $uri", e)
        null
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var inSampleSize = 1
        while (width / inSampleSize > reqWidth || height / inSampleSize > reqHeight) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    /**
     * Apply a loaded wallpaper to an [ImageView], optionally blurring it.
     */
    suspend fun applyWallpaperToImageView(
        imageView: ImageView,
        bitmap: Bitmap?,
        blurRadius: Float,
        blurEnabled: Boolean
    ) = withContext(Dispatchers.Main) {
        if (bitmap == null) {
            imageView.setImageDrawable(null)
            imageView.setRenderEffectCompat(null)
            imageView.visibility = View.GONE
            return@withContext
        }

        imageView.visibility = View.VISIBLE
        if (blurEnabled && blurRadius > 0.5f) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                imageView.setImageBitmap(bitmap)
                imageView.setRenderEffectCompat(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP))
            } else {
                val blurred = withContext(Dispatchers.IO) { blurBitmap(bitmap, blurRadius) }
                imageView.setImageBitmap(blurred)
                imageView.setRenderEffectCompat(null)
            }
        } else {
            imageView.setImageBitmap(bitmap)
            imageView.setRenderEffectCompat(null)
        }
    }

    /**
     * Fast, software-only blur approximation for pre-Android 12 devices.
     */
    fun blurBitmap(source: Bitmap, radius: Float): Bitmap {
        val scaleFactor = (1 + radius / 8).coerceIn(1f, 16f)
        val scaledWidth = (source.width / scaleFactor).toInt().coerceAtLeast(1)
        val scaledHeight = (source.height / scaleFactor).toInt().coerceAtLeast(1)
        val scaled = source.scale(scaledWidth, scaledHeight)
        val result = Bitmap.createScaledBitmap(scaled, source.width, source.height, true)
        if (scaled !== source && scaled !== result) scaled.recycle()
        return result
    }

    /**
     * Re-blurs an image already on screen without decoding it again, so a preview can follow
     * the blur slider live. Only possible where blur is a render effect (Android 12+).
     *
     * @return false when this device blurs in software and the image must be reloaded.
     */
    fun updateLiveBlur(imageView: ImageView, blurRadius: Float, blurEnabled: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        imageView.setRenderEffectCompat(
            if (blurEnabled && blurRadius > 0.5f) {
                RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
            } else {
                null
            }
        )
        return true
    }

    /**
     * Clear a wallpaper [ImageView] and remove any blur effect.
     */
    fun clearWallpaperEffect(imageView: ImageView) {
        imageView.setImageDrawable(null)
        imageView.setRenderEffectCompat(null)
        imageView.visibility = View.GONE
    }

    private fun View.setRenderEffectCompat(effect: RenderEffect?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(effect)
        }
    }
}
