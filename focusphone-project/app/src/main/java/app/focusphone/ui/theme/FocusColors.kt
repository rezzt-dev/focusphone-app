package app.focusphone.ui.theme

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Colour maths shared by every FocusPhone surface.
 *
 * The launcher lets the user combine an arbitrary theme (including a fully custom
 * background/foreground pair) with an arbitrary wallpaper. Nothing guarantees that
 * such a combination stays readable, so the UI never trusts a raw colour: it runs it
 * through [ensureContrast] against the surface it will actually be painted on.
 *
 * Contrast ratios follow the WCAG 2.1 definition, which is the same model Material 3
 * uses for its tonal palettes.
 */
object FocusColors {

    /** WCAG minimum for body text (14sp regular). */
    const val CONTRAST_BODY = 4.5

    /** WCAG minimum for large text (>= 18sp, or 14sp bold) and meaningful UI shapes. */
    const val CONTRAST_LARGE = 3.0

    /** Comfortable target for secondary/supporting text on FocusPhone surfaces. */
    const val CONTRAST_SUPPORT = 4.0

    // ── Basic manipulation ─────────────────────────────────────────

    /** Returns [color] with its alpha replaced by [alpha] (0f..1f). */
    @ColorInt
    fun withAlpha(@ColorInt color: Int, @FloatRange(from = 0.0, to = 1.0) alpha: Float): Int =
        (color and 0x00FFFFFF) or ((alpha.coerceIn(0f, 1f) * 255f).toInt() shl 24)

    /**
     * Composites [foreground] over [background] at [ratio] and returns an opaque colour.
     *
     * Preferred over [withAlpha] whenever the result has to be measured for contrast:
     * a translucent colour has no measurable luminance on its own.
     */
    @ColorInt
    fun blend(
        @ColorInt foreground: Int,
        @ColorInt background: Int,
        @FloatRange(from = 0.0, to = 1.0) ratio: Float
    ): Int {
        val r = ratio.coerceIn(0f, 1f)
        val inv = 1f - r
        return Color.rgb(
            (Color.red(foreground) * r + Color.red(background) * inv).toInt().coerceIn(0, 255),
            (Color.green(foreground) * r + Color.green(background) * inv).toInt().coerceIn(0, 255),
            (Color.blue(foreground) * r + Color.blue(background) * inv).toInt().coerceIn(0, 255)
        )
    }

    /** Flattens a possibly translucent [color] onto an opaque [background]. */
    @ColorInt
    fun flatten(@ColorInt color: Int, @ColorInt background: Int): Int =
        blend(color, background, Color.alpha(color) / 255f)

    // ── Luminance & contrast ───────────────────────────────────────

    /** WCAG relative luminance of an opaque colour, in 0f..1f. */
    fun luminance(@ColorInt color: Int): Double {
        fun channel(value: Int): Double {
            val c = value / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(Color.red(color)) +
            0.7152 * channel(Color.green(color)) +
            0.0722 * channel(Color.blue(color))
    }

    /** true when [color] is dark enough that light content reads better on top of it. */
    fun isDark(@ColorInt color: Int): Boolean = luminance(color) < 0.5

    /** WCAG contrast ratio between two opaque colours, from 1.0 to 21.0. */
    fun contrastRatio(@ColorInt a: Int, @ColorInt b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Black or white — whichever is more readable on [background]. */
    @ColorInt
    fun bestOn(@ColorInt background: Int): Int =
        if (contrastRatio(Color.WHITE, background) >= contrastRatio(Color.BLACK, background)) {
            Color.WHITE
        } else {
            Color.BLACK
        }

    /**
     * Returns [foreground] pushed away from [background] until it reaches [minRatio].
     *
     * The hue is preserved as long as possible: the colour is first blended towards the
     * more readable of black/white in small steps, and only falls back to that extreme
     * when the target is otherwise unreachable. Returns [foreground] untouched when it
     * already meets the requirement, so themes that were designed carefully are never
     * altered.
     */
    @ColorInt
    fun ensureContrast(
        @ColorInt foreground: Int,
        @ColorInt background: Int,
        minRatio: Double = CONTRAST_BODY
    ): Int {
        val opaqueFg = flatten(foreground, background)
        if (contrastRatio(opaqueFg, background) >= minRatio) return opaqueFg

        val target = bestOn(background)
        var step = 0.05f
        while (step <= 1f) {
            val candidate = blend(target, opaqueFg, step)
            if (contrastRatio(candidate, background) >= minRatio) return candidate
            step += 0.05f
        }
        return target
    }

    /**
     * Alpha-composites [top] over [bottom] the way the framework draws overlapping views.
     * Used to reason about a scrim painted on top of a wallpaper.
     */
    @ColorInt
    fun compositeOver(@ColorInt top: Int, @ColorInt bottom: Int): Int {
        val ta = Color.alpha(top) / 255f
        if (ta >= 1f) return top
        if (ta <= 0f) return bottom
        return blend(top, bottom, ta)
    }
}
