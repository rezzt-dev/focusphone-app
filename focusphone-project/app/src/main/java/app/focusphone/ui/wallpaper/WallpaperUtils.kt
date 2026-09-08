package app.focusphone.ui.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.ImageView
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
     * Load a bitmap from a persisted content URI, scaled to avoid excessive memory use.
     */
    suspend fun loadBitmap(context: Context, uriString: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (uriString.isNullOrBlank()) return@withContext null
        try {
            val uri = Uri.parse(uriString)
            val (width, height) = context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                options.outWidth to options.outHeight
            } ?: return@withContext null

            val sampleSize = calculateInSampleSize(width, height, MAX_BITMAP_DIMENSION, MAX_BITMAP_DIMENSION)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load wallpaper from $uriString", e)
            null
        }
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
        val scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        return Bitmap.createScaledBitmap(scaled, source.width, source.height, true)
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
