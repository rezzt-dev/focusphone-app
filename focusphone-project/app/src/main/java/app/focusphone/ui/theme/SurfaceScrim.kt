package app.focusphone.ui.theme

import android.content.Context
import android.view.View
import app.focusphone.data.preferences.WallpaperPreferences

/**
 * The kind of surface a scrim protects. Each kind maps the user's single "legibility"
 * preference to its own alpha range, because the amount of help a surface needs depends
 * entirely on what it renders.
 *
 * @property floor alpha applied even at legibility 0, i.e. the non-negotiable minimum.
 * @property range extra alpha added as legibility grows to 100.
 */
enum class ScrimSurface(val floor: Float, val range: Float) {

    /**
     * The launcher home screen: a large clock and a handful of 21sp app names.
     * The wallpaper is the point of this screen, so it gets no forced minimum — a user
     * who sets legibility to 0 sees their photo untouched.
     */
    HOME(floor = 0f, range = 0.55f),

    /**
     * The side drawers (calendar + launcher options, notifications). Dense enough that a
     * busy photo behind 12sp weekday letters is unreadable, so a light floor is kept.
     */
    PANEL(floor = 0.28f, range = 0.62f),

    /**
     * Settings, app drawer, pickers and block screens: small body text, subtitles,
     * switches and sliders. These always keep a substantial floor.
     */
    CONTENT(floor = 0.40f, range = 0.55f)
}

/**
 * Paints an adaptive scrim between a wallpaper and the content drawn on top of it.
 *
 * Before this existed, scrim alpha was hard-coded per layout: the settings screens used a
 * fixed `0.85` (of the *base* theme colour, so custom themes tinted wrong) and both home
 * drawers used `0` — which is why calendar and menu text sat directly on the wallpaper.
 *
 * The scrim is always painted in the active palette's background colour, so the surface
 * behind the text is a dimmed version of the theme the user chose rather than a grey wash.
 */
object SurfaceScrim {

    /**
     * How much of the scrim a full-strength blur is allowed to replace. Blurring already
     * removes the high-frequency detail that makes text hard to read, so the two effects
     * would otherwise stack into an opaque panel.
     */
    private const val BLUR_RELIEF = 0.35f

    /**
     * Alpha for [surface], or 0 when no wallpaper is showing behind it.
     *
     * @param hasWallpaper whether a wallpaper image is actually visible on this surface.
     */
    fun alphaFor(context: Context, surface: ScrimSurface, hasWallpaper: Boolean): Float {
        if (!hasWallpaper || !WallpaperPreferences.isWallpaperEnabled(context)) return 0f
        return alphaFor(
            surface,
            legibility = WallpaperPreferences.getReadability(context),
            blurEnabled = WallpaperPreferences.isBlurEnabled(context),
            blurIntensity = WallpaperPreferences.getBlurIntensity(context)
        )
    }

    /**
     * Alpha for [surface] over a visible wallpaper with explicit settings, so a preview can
     * follow a slider while it is being dragged, before anything is stored.
     *
     * @param legibility 0-100, as stored by [WallpaperPreferences.setReadability].
     * @param blurIntensity 0-100, as stored by [WallpaperPreferences.setBlurIntensity].
     */
    fun alphaFor(surface: ScrimSurface, legibility: Int, blurEnabled: Boolean, blurIntensity: Int): Float {
        var alpha = surface.floor + surface.range * (legibility.coerceIn(0, 100) / 100f)
        if (blurEnabled) {
            alpha *= (1f - BLUR_RELIEF * (blurIntensity.coerceIn(0, 100) / 100f))
        }
        return alpha.coerceIn(0f, 1f)
    }

    /**
     * Applies the scrim to [view], hiding it entirely when no protection is needed so it
     * never costs a draw pass on the common no-wallpaper path.
     */
    fun apply(
        view: View?,
        context: Context,
        surface: ScrimSurface,
        palette: FocusPalette,
        hasWallpaper: Boolean
    ) {
        view ?: return
        val alpha = alphaFor(context, surface, hasWallpaper)
        if (alpha <= 0.01f) {
            view.visibility = View.GONE
            return
        }
        view.setBackgroundColor(palette.background)
        view.alpha = alpha
        view.visibility = View.VISIBLE
    }
}
