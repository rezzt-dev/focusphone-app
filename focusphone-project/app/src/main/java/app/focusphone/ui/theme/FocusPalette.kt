package app.focusphone.ui.theme

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import app.focusphone.data.preferences.ThemePreferences

/**
 * The resolved colour set of the currently active theme.
 *
 * FocusPhone supports four built-in themes plus a user-defined custom pair, and the
 * custom pair is applied at runtime rather than through a style. Before this type
 * existed every screen resolved theme attributes on its own — some through
 * `Theme.resolveAttribute().data`, which silently yields a resource id instead of a
 * colour whenever the attribute points at a `ColorStateList` — and the custom theme was
 * simply missing from most of those call sites.
 *
 * [of] is the single source of truth: it always returns opaque, contrast-checked colours
 * for whichever theme is active, custom included.
 */
data class FocusPalette(
    /** Opaque colour painted behind the whole screen. */
    @param:ColorInt val background: Int,
    /** Slightly raised background for cards, tiles and inputs. */
    @param:ColorInt val surface: Int,
    /** Primary text and icons. */
    @param:ColorInt val textPrimary: Int,
    /** Supporting text, subtitles and inactive icons. */
    @param:ColorInt val textSecondary: Int,
    /** Hairline separators. */
    @param:ColorInt val divider: Int,
    /** Fill used by selected states (today's date, active chips, switches). */
    @param:ColorInt val accent: Int,
    /** Readable content colour on top of [accent]. */
    @param:ColorInt val onAccent: Int,
    /** true when [background] is dark, i.e. light content belongs on top. */
    val isDark: Boolean
) {

    /** [textPrimary] dimmed to [fraction] while staying opaque and measurable. */
    @ColorInt
    fun primaryAt(fraction: Float): Int = FocusColors.blend(textPrimary, background, fraction)

    /**
     * A variant of this palette tuned for text drawn on a **partially transparent** scrim.
     *
     * A scrim below 100% opacity lets the wallpaper shift the surface away from
     * [background], and every FocusPhone colour was chosen against [background]. The shift
     * always reduces the distance between surface and text — a light theme over a dark photo
     * darkens the surface, a dark theme over a bright photo lightens it — so pulling the
     * supporting colours towards [textPrimary] restores the separation in either direction.
     *
     * The pull is proportional to how much of the wallpaper still shows through, so a nearly
     * opaque scrim leaves the palette untouched.
     *
     * @param scrimAlpha opacity of the scrim in front of the wallpaper, 0f..1f.
     */
    fun forScrim(scrimAlpha: Float): FocusPalette {
        if (scrimAlpha >= OPAQUE_ENOUGH) return this
        val pull = ((1f - scrimAlpha) * PULL_FACTOR).coerceIn(0f, MAX_PULL)
        return copy(
            textSecondary = FocusColors.blend(textPrimary, textSecondary, pull),
            divider = FocusColors.blend(textPrimary, background, 0.16f + 0.30f * pull)
        )
    }

    /** A translucent ripple/highlight colour appropriate for this palette. */
    @ColorInt
    fun ripple(): Int = FocusColors.withAlpha(textPrimary, 0.10f)

    companion object {

        /** Above this scrim opacity the wallpaper no longer affects legibility. */
        private const val OPAQUE_ENOUGH = 0.95f

        /** How aggressively supporting colours move towards [textPrimary]. */
        private const val PULL_FACTOR = 1.2f
        private const val MAX_PULL = 0.75f

        /** Fallbacks used only if a theme attribute is somehow missing. */
        private const val FALLBACK_DARK_BG = 0xFF181818.toInt()
        private const val FALLBACK_DARK_FG = 0xFFE0E0E0.toInt()

        /**
         * Resolves the palette for the theme currently applied to [context].
         *
         * Must be called with an Activity context (or any context whose theme has been
         * set), because built-in themes are read straight from the theme attributes.
         */
        fun of(context: Context): FocusPalette {
            val isCustom = ThemePreferences.getTheme(context) == ThemePreferences.THEME_CUSTOM
            return if (isCustom) fromCustom(context) else fromTheme(context)
        }

        // ── Built-in themes ────────────────────────────────────────

        private fun fromTheme(context: Context): FocusPalette {
            val background = colorAttr(context, android.R.attr.colorBackground, FALLBACK_DARK_BG)
            val primary = colorAttr(context, android.R.attr.textColorPrimary, FALLBACK_DARK_FG)
            val secondary = colorAttr(context, android.R.attr.textColorSecondary, primary)
            val surface = colorAttr(
                context,
                com.google.android.material.R.attr.colorSurface,
                FocusColors.blend(primary, background, 0.05f)
            )
            return build(
                background = opaque(background, FALLBACK_DARK_BG),
                surface = opaque(surface, background),
                primary = opaque(primary, FALLBACK_DARK_FG),
                secondary = opaque(secondary, primary)
            )
        }

        // ── Custom theme ───────────────────────────────────────────

        private fun fromCustom(context: Context): FocusPalette {
            val background = ThemePreferences.getCustomBackgroundColor(context)
            val primary = ThemePreferences.getCustomForegroundColor(context)
            return build(
                background = opaque(background, FALLBACK_DARK_BG),
                surface = FocusColors.blend(primary, background, 0.06f),
                primary = opaque(primary, FALLBACK_DARK_FG),
                secondary = FocusColors.blend(primary, background, 0.72f)
            )
        }

        // ── Shared construction ────────────────────────────────────

        private fun build(
            @ColorInt background: Int,
            @ColorInt surface: Int,
            @ColorInt primary: Int,
            @ColorInt secondary: Int
        ): FocusPalette {
            // A user can pick a foreground that is nearly invisible on their own
            // background. Rather than rendering an unusable launcher, nudge both text
            // roles until they clear the WCAG thresholds for their size.
            val safePrimary = FocusColors.ensureContrast(primary, background, FocusColors.CONTRAST_BODY)
            val safeSecondary =
                FocusColors.ensureContrast(secondary, background, FocusColors.CONTRAST_SUPPORT)
            val accent = safePrimary
            return FocusPalette(
                background = background,
                surface = surface,
                textPrimary = safePrimary,
                textSecondary = safeSecondary,
                divider = FocusColors.blend(safePrimary, background, 0.16f),
                accent = accent,
                onAccent = onAccentFor(accent, background),
                isDark = FocusColors.isDark(background)
            )
        }

        /**
         * Content colour for a filled [accent] shape. The theme background is preferred
         * so filled chips keep looking like cut-outs of the screen, but it is only used
         * when it is actually legible on the accent.
         */
        @ColorInt
        private fun onAccentFor(@ColorInt accent: Int, @ColorInt background: Int): Int =
            if (FocusColors.contrastRatio(background, accent) >= FocusColors.CONTRAST_LARGE) {
                background
            } else {
                FocusColors.bestOn(accent)
            }

        @ColorInt
        private fun opaque(@ColorInt color: Int, @ColorInt over: Int): Int =
            if (Color.alpha(color) == 255) color else FocusColors.flatten(color, over)

        /**
         * Reads a colour theme attribute safely.
         *
         * [android.content.res.TypedArray.getColor] resolves both plain colours and
         * `ColorStateList` references, which is exactly the case that
         * `Theme.resolveAttribute(...).data` gets wrong.
         */
        @ColorInt
        private fun colorAttr(context: Context, attr: Int, @ColorInt fallback: Int): Int {
            val typed = context.obtainStyledAttributes(intArrayOf(attr))
            return try {
                typed.getColor(0, fallback)
            } catch (_: UnsupportedOperationException) {
                fallback
            } finally {
                typed.recycle()
            }
        }
    }
}
