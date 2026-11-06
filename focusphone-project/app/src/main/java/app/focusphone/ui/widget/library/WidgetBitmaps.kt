package app.focusphone.ui.widget.library

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Bitmaps the widgets draw themselves: progress rings and bars, and album artwork.
 *
 * RemoteViews travel through a binder transaction with a hard size limit, so rings and
 * bars are **alpha masks** ([Bitmap.Config.ALPHA_8], one byte per pixel) tinted by the
 * ImageView like any icon, and every bitmap is capped in size.
 */
object WidgetBitmaps {

    /** Largest side of any bitmap a widget sends, in pixels. */
    private const val MAX_SIDE_PX = 480
    private const val FULL_SWEEP = 360f
    private const val START_ANGLE = -90f

    /**
     * Ring geometry as a share of its side. `wg_ring_track.xml` draws the empty ring with
     * the same numbers (viewport 100: stroke 7.5, radius 45.25), so the arc sits exactly
     * on top of it at any size.
     */
    private const val RING_STROKE = 0.075f
    private const val RING_MARGIN = 0.01f

    /**
     * A progress arc starting at twelve o'clock, as an alpha mask [sidePx] square.
     * [fraction] is clamped to 0..1; a non-zero value always shows at least a dot.
     */
    fun ring(sidePx: Int, fraction: Float): Bitmap {
        val side = sidePx.coerceIn(1, MAX_SIDE_PX)
        val bitmap = createBitmap(side, side, Bitmap.Config.ALPHA_8)
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped <= 0f) return bitmap
        val strokePx = side * RING_STROKE
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            strokeCap = Paint.Cap.ROUND
            color = Color.BLACK
        }
        val inset = strokePx / 2f + side * RING_MARGIN
        val bounds = RectF(inset, inset, side - inset, side - inset)
        Canvas(bitmap).drawArc(bounds, START_ANGLE, max(clamped * FULL_SWEEP, 0.5f), false, paint)
        return bitmap
    }

    /** The filled part of a rounded progress bar, as an alpha mask. */
    fun bar(widthPx: Int, heightPx: Int, fraction: Float): Bitmap {
        val scale = if (widthPx > MAX_SIDE_PX * 2) MAX_SIDE_PX * 2f / widthPx else 1f
        val width = (widthPx * scale).roundToInt().coerceAtLeast(1)
        val height = (heightPx * scale).roundToInt().coerceAtLeast(1)
        val bitmap = createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped <= 0f) return bitmap
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        // Never narrower than the bar is tall, so a tiny value is a dot rather than a sliver.
        val filled = max(width * clamped, height.toFloat())
        Canvas(bitmap).drawRoundRect(RectF(0f, 0f, filled, height.toFloat()), height / 2f, height / 2f, paint)
        return bitmap
    }

    /**
     * [source] cropped to a square, scaled to [sidePx] and given rounded corners.
     * With [scrim], a dark gradient is laid over it so white text stays legible on any
     * artwork.
     */
    fun artwork(source: Bitmap, sidePx: Int, cornerPx: Float, scrim: Boolean = false): Bitmap {
        val side = sidePx.coerceIn(1, MAX_SIDE_PX)
        val output = createBitmap(side, side)
        val crop = minOf(source.width, source.height).toFloat()
        val scale = side / crop
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(-(source.width - crop) / 2f * scale, -(source.height - crop) / 2f * scale)
        }
        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(matrix) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
        val canvas = Canvas(output)
        val bounds = RectF(0f, 0f, side.toFloat(), side.toFloat())
        canvas.drawRoundRect(bounds, cornerPx, cornerPx, paint)
        if (scrim) {
            val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.shader = LinearGradient(
                    0f,
                    0f,
                    0f,
                    side.toFloat(),
                    intArrayOf(SCRIM_TOP, SCRIM_MIDDLE, SCRIM_BOTTOM),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(bounds, cornerPx, cornerPx, shade)
        }
        return output
    }

    private val SCRIM_TOP = Color.argb(70, 0, 0, 0)
    private val SCRIM_MIDDLE = Color.argb(110, 0, 0, 0)
    private val SCRIM_BOTTOM = Color.argb(190, 0, 0, 0)
}
